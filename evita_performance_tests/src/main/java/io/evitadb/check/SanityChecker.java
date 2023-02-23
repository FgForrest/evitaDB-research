/*
 *                         _ _        ____  ____
 *               _____   _(_) |_ __ _|  _ \| __ )
 *              / _ \ \ / / | __/ _` | | | |  _ \
 *             |  __/\ V /| | || (_| | |_| | |_) |
 *              \___| \_/ |_|\__\__,_|____/|____/
 *
 *   Copyright (c) 2023
 *
 *   Licensed under the Business Source License, Version 1.1 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://github.com/FgForrest/evitaDB/blob/main/LICENSE
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.evitadb.check;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import io.evitadb.api.*;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.query.require.ExtraResultRequireConstraint;
import io.evitadb.api.query.visitor.FinderVisitor;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.KryoFactory.QuerySerializationKryoConfigurer;
import io.evitadb.api.utils.Assert;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This tool allows to iterate over all synthetic queries over real dataset and report count of returned records.
 * Execute the tool with following parameters:
 *
 * 1. catalog name
 * 2. absolute path to directory with queries (containing `queries.kryo` file)
 * 3. number of threads which will emit queries to the database
 * 4. number of queries that will be fetched from underlying data store at maximum
 *
 * The tool will load all synthetic queries and execute them against the target database. It records queries per second
 * digestion rate, total records matching, entity bodies and primary keys really returned.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public abstract class SanityChecker<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> {
	private static final int PRELOADED_QUERY_COUNT = 100_000;
	private final Object monitor = new Object();
	private int queryLimit;
	private int preloadedQueryCount;
	private Kryo kryo;
	private Input input;
	private boolean finished;
	private int queriesFetched;

	public void execute(String[] args) throws FileNotFoundException {
		Assert.isTrue(args.length >= 2, "Expected exactly two arguments! First: absolute path to data directory, second: absolute path to query directory");
		final String catalogName = args[0];
		final Path queryDirectory = getAndVerifyDirectory(args[1]);
		final int threadCount = args.length > 2 ? Integer.parseInt(args[2]) : Runtime.getRuntime().availableProcessors();
		this.queryLimit = args.length > 3 ? Integer.parseInt(args[3]) : Integer.MAX_VALUE;
		this.preloadedQueryCount = Math.min(queryLimit / threadCount, PRELOADED_QUERY_COUNT);
		this.input = new ByteBufferInput(new FileInputStream(queryDirectory.resolve("queries.kryo").toFile()), 8_192);
		this.kryo = KryoFactory.createKryo(new DefaultClassResolver(), QuerySerializationKryoConfigurer.INSTANCE);

		System.out.println("Loading evita database ...");
		try (final EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> evitaInstance = createEvitaInstance(catalogName)) {
			System.out.println("Evita database loaded. Starting workers with parallelization " + threadCount + " ...");

			final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
			final Statistics overallStatistics = new Statistics(threadCount);

			Stream.generate(() -> new Worker<>(this::fetchNewQueries, countDownLatch, catalogName, evitaInstance, overallStatistics))
				.limit(threadCount)
				.forEach(it -> new Thread(it).start());


			final Thread statusWriter = new Thread(new StatusWriter(overallStatistics));
			statusWriter.start();

			countDownLatch.await();
			statusWriter.interrupt();

			System.out.println("-".repeat(80));
			System.out.println(" T O T A L   R E S U L T S :");
			System.out.println("-".repeat(80));
			System.out.println(overallStatistics);
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates new Evita instance for specific implementation based on existing data from previous run.
	 */
	protected abstract EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> createEvitaInstance(@Nonnull String catalogName);

	@RequiredArgsConstructor
	private static class StatusWriter implements Runnable {
		private final Statistics overallStatistics;

		@Override
		public void run() {
			try {
				while (true) {
					synchronized (this) {
						System.out.println(overallStatistics.toString());
						Thread.sleep(10_000);
					}
				}
			} catch (InterruptedException e) {
				// finish
			}
		}

	}

	@RequiredArgsConstructor
	private static class Worker<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
		implements Runnable {
		private final Supplier<Deque<Query>> querySupplier;
		private final CountDownLatch countDownLatch;
		private final String catalogName;
		private final EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> evitaInstance;
		private final Statistics overallStatistics;
		private Deque<Query> preloadedQueries;

		@Override
		public void run() {
			try {
				this.preloadedQueries = this.querySupplier.get();
				while (!this.preloadedQueries.isEmpty()) {
					evitaInstance.queryCatalog(
						catalogName, session -> {
							while (!this.preloadedQueries.isEmpty()) {
								final Query theQuery = this.preloadedQueries.removeFirst();
								final boolean pkOnly = FinderVisitor.findConstraints(theQuery.getRequire(), EntityContentRequire.class::isInstance, ExtraResultRequireConstraint.class::isInstance).isEmpty();
								final long start = System.nanoTime();
								if (pkOnly) {
									final EvitaResponseBase<EntityReference> response = session.query(theQuery, EntityReference.class);
									overallStatistics.recordResponseEntityReference(System.nanoTime() - start, response);
								} else {
									final EvitaResponseBase<SealedEntity> response = session.query(theQuery, SealedEntity.class);
									overallStatistics.recordResponseSealedEntity(System.nanoTime() - start, response);
								}
							}
							return null;
						}
					);
					this.preloadedQueries = querySupplier.get();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				countDownLatch.countDown();
			}
		}

	}

	@Nonnull
	private static Path getAndVerifyDirectory(String absolutePath) {
		final Path directoryPath = Path.of(absolutePath);
		final File file = directoryPath.toFile();
		Assert.isTrue(file.exists(), "The directory " + absolutePath + " doesn't exist!");
		Assert.isTrue(file.isDirectory(), "The directory " + absolutePath + " doesn't exist!");
		return directoryPath;
	}

	private Deque<Query> fetchNewQueries() {
		synchronized (monitor) {
			final LinkedList<Query> fetchedQueries = new LinkedList<>();
			if (!finished) {
				for (int i = 0; i < preloadedQueryCount && queriesFetched++ < queryLimit; i++) {
					if (!input.canReadInt()) {
						this.input.close();
						finished = true;
						break;
					}
					fetchedQueries.add(kryo.readObject(input, Query.class));
				}
			}
			return fetchedQueries;
		}
	}

}
