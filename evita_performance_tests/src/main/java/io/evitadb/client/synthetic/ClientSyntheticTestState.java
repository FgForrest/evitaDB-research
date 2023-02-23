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

package io.evitadb.client.synthetic;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.query.require.ExtraResultRequireConstraint;
import io.evitadb.api.query.visitor.FinderVisitor;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.KryoFactory.QuerySerializationKryoConfigurer;
import io.evitadb.client.ClientDataFullDatabaseState;
import io.evitadb.generators.RandomQueryGenerator;
import io.evitadb.senesi.synthetic.InMemorySyntheticTestSenesiState;
import lombok.Data;
import lombok.Getter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;

import static java.util.Optional.ofNullable;

/**
 * Base state class for {@link io.evitadb.senesi.SenesiBenchmark#syntheticTest_InMemory(InMemorySyntheticTestSenesiState, Blackhole)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ClientSyntheticTestState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ClientDataFullDatabaseState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>
	implements RandomQueryGenerator {

	private static final int PRELOADED_QUERY_COUNT = 100_000;
	private final Object monitor = new Object();

	private Deque<Query> preloadedQueries = new LinkedList<>();
	private Path inputFolder;
	private Kryo kryo;
	private Input input;
	private int fetchedQueryCount;
	private long lastFetch;

	/**
	 * Query prepared for the measured invocation.
	 */
	@Getter protected QueryWithExpectedType queryWithExpectedType;

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Iteration)
	public void prepareQueries() {
		this.inputFolder = getDataDirectory().resolve(getCatalogName() + "_queries/queries.kryo");
		try {
			this.input = new ByteBufferInput(new FileInputStream(inputFolder.toFile()), 8_192);
			this.kryo = KryoFactory.createKryo(new DefaultClassResolver(), QuerySerializationKryoConfigurer.INSTANCE);
			this.preloadedQueries = fetchNewQueries(PRELOADED_QUERY_COUNT);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Cannot access input folder: " + inputFolder);
		}
	}

	@TearDown(Level.Iteration)
	public void tearDown() {
		this.input.close();
		this.input = null;
		this.kryo = null;
	}

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		this.queryWithExpectedType = ofNullable(preloadedQueries.pollFirst()).map(QueryWithExpectedType::new).orElse(null);
		while (this.queryWithExpectedType == null) {
			this.preloadedQueries = fetchNewQueries(PRELOADED_QUERY_COUNT);
			this.queryWithExpectedType = new QueryWithExpectedType(preloadedQueries.pollFirst());
		}
	}

	private Deque<Query> fetchNewQueries(int queryCount) {
		final LinkedList<Query> fetchedQueries = new LinkedList<>();
		synchronized (this.monitor) {
			for (int i = 0; i < queryCount; i++) {
				try {
					if (!input.canReadInt()) {
						System.out.println("Wrapping around the file.");
						// reopen the same file again and read from start
						this.input.close();
						this.input = new ByteBufferInput(new FileInputStream(this.inputFolder.toFile()), 8_192);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot access input folder: " + inputFolder);
				}
				fetchedQueries.add(kryo.readObject(input, Query.class));
			}
		}
		final long duration = (System.nanoTime() - lastFetch);
		System.out.println(
			"Fetched " + fetchedQueries.size() + " (total " + fetchedQueryCount + ") queries " +
				(lastFetch > 0 ? " - avg. qps " + new BigDecimal(fetchedQueries.size()).divide(new BigDecimal(duration).divide(new BigDecimal(1_000_000_000L), 1_000, RoundingMode.HALF_UP), 0, RoundingMode.HALF_UP) : "")
		);
		fetchedQueryCount += fetchedQueries.size();
		lastFetch = System.nanoTime();
		return fetchedQueries;
	}

	@Data
	public static class QueryWithExpectedType {
		private final Query query;
		private final Class<? extends Serializable> expectedResult;

		public QueryWithExpectedType(Query query) {
			this.query = query;
			this.expectedResult = FinderVisitor.findConstraints(this.query.getRequire(), EntityContentRequire.class::isInstance, ExtraResultRequireConstraint.class::isInstance).isEmpty()
				? EntityReferenceContract.class : SealedEntity.class;
		}

	}

}
