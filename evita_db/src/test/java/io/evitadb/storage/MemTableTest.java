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

package io.evitadb.storage;

import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KryoFactory.EntityKryoConfigurer;
import io.evitadb.api.serialization.KryoFactory.SchemaKryoConfigurer;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.storage.MemTable.MemTableFileStatistics;
import io.evitadb.storage.kryo.VersionedKryo;
import io.evitadb.storage.kryo.VersionedKryoFactory;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.memTable.MemTableDescriptor;
import io.evitadb.storage.model.memTable.VersionedKryoKeyInputs;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import io.evitadb.storage.serialization.StoragePartConfigurer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies functionality of {@link MemTable} operations.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
class MemTableTest {
	public static final String ENTITY_TYPE = "whatever";
	private final Path targetFile = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "memtable.kryo");

	@BeforeEach
	void setUp() {
		targetFile.toFile().delete();
	}

	@AfterEach
	void tearDown() {
		targetFile.toFile().delete();
	}

	@DisplayName("Hundreds entities should be stored in MemTable and retrieved intact.")
	@Test
	void shouldSerializeAndReconstructBigMemTable() {
		final StorageOptions options = new StorageOptions(1);
		final ObservableOutputKeeper observableOutputKeeper = new ObservableOutputKeeper(options);
		observableOutputKeeper.prepare();

		final MemTable memTable = new MemTable(
			targetFile,
			new MemTableDescriptor(
				new CatalogEntityHeader(ENTITY_TYPE),
				createKryo(),
				false
			),
			options,
			observableOutputKeeper
		);
		final int recordCount = 600;

		final long transactionId = 1;
		for (int i = 1; i <= recordCount; i++) {
			memTable.put(transactionId, new EntityBodyStoragePart(i));
		}

		log.info("Flushing table (" + transactionId + ")");
		final MemTableDescriptor memTableInfo = memTable.flush(transactionId);
		final MemTable loadedMemTable = new MemTable(
			targetFile,
			new MemTableDescriptor(
				memTableInfo.getFileLocation(),
				memTableInfo
			),
			options,
			observableOutputKeeper
		);

		long duration = 0L;
		for (int i = 1; i <= recordCount; i++) {
			long start = System.nanoTime();
			final EntityBodyStoragePart actual = memTable.get(i, EntityBodyStoragePart.class);
			duration += System.nanoTime() - start;
			assertEquals(
				new EntityBodyStoragePart(i),
				actual
			);
		}

		observableOutputKeeper.free();

		assertTrue(memTable.memTableEquals(loadedMemTable));
		/* 600 records +1 record for th MemTable itself */
		assertEquals(601, memTable.verifyContents().getRecordCount());
		log.info("Average reads: " + StringUtils.formatRequestsPerSec(recordCount, duration));
	}

	@DisplayName("Existing record can be removed")
	@Test
	void shouldRemoveRecord() {
		final StorageOptions options = new StorageOptions(1);
		final ObservableOutputKeeper observableOutputKeeper = new ObservableOutputKeeper(options);
		observableOutputKeeper.prepare();

		final MemTable memTable = new MemTable(
			targetFile,
			new MemTableDescriptor(
				new CatalogEntityHeader(ENTITY_TYPE),
				createKryo(),
				false
			),
			options,
			observableOutputKeeper
		);

		MemTableDescriptor memTableInfo = null;
		// store 300 records in multiple chunks,
		final int recordCount = 50;
		final int removedRecords = 10;
		final int iterationCount = 6;

		long transactionId = 0;
		for (int j = 0; j < iterationCount; j++) {
			transactionId++;
			if (j > 0) {
				for (int i = 1; i < removedRecords; i++) {
					final int primaryKey = i + (j - 1) * recordCount;
					log.info("Removal of rec with PK:   " + primaryKey);
					memTable.remove(primaryKey, EntityBodyStoragePart.class);
				}
			}
			for (int i = 1; i <= recordCount; i++) {
				final int primaryKey = j * recordCount + i;
				log.info("Insertion of rec with PK (tx " + transactionId + "): " + primaryKey);
				memTable.put(
					transactionId,
					new EntityBodyStoragePart(primaryKey));
			}

			log.info("Flushing table (tx " + transactionId + ")");
			memTableInfo = memTable.flush(transactionId);
		}

		final MemTable loadedMemTable = new MemTable(
			targetFile,
			new MemTableDescriptor(
				memTableInfo.getFileLocation(),
				memTableInfo
			),
			options,
			observableOutputKeeper
		);

		for (int i = 1; i <= recordCount * iterationCount; i++) {
			final EntityBodyStoragePart actual = memTable.get(i, EntityBodyStoragePart.class);
			if (i < recordCount * (iterationCount - 1) && i % recordCount < removedRecords && i % recordCount > 0) {
				assertNull(actual);
			} else {
				assertEquals(
					new EntityBodyStoragePart(i),
					actual
				);
			}
		}

		observableOutputKeeper.free();

		assertTrue(memTable.memTableEquals(loadedMemTable));
		/* 300 records +6 record for th MemTable itself */
		assertEquals(306, memTable.verifyContents().getRecordCount());
	}

	@DisplayName("No operation should be allowed after close")
	@Test
	void shouldRefuseOperationAfterClose() {
		final StorageOptions options = new StorageOptions(1);
		final ObservableOutputKeeper outputKeeper = new ObservableOutputKeeper(options);
		outputKeeper.prepare();

		final MemTable memTable = new MemTable(
			targetFile,
			new MemTableDescriptor(
				new CatalogEntityHeader(ENTITY_TYPE),
				createKryo(),
				false
			),
			options,
			outputKeeper
		);
		memTable.put(0L, new EntityBodyStoragePart(1));
		memTable.close();
		outputKeeper.free();

		assertThrows(IllegalStateException.class, () -> memTable.get(1, EntityBodyStoragePart.class));
		assertThrows(IllegalStateException.class, () -> memTable.put(0L, new EntityBodyStoragePart(2)));
		assertThrows(IllegalStateException.class, memTable::getEntries);
		assertThrows(IllegalStateException.class, memTable::getKeys);
		assertThrows(IllegalStateException.class, memTable::getFileLocations);
		assertThrows(IllegalStateException.class, () -> memTable.flush(0L));
	}

	@Disabled("This infinite test performs random operations on MemTable verifying its consistency")
	@Test
	void generationalProofTest() {
		final Random random = new Random(123456);
		final StorageOptions options = new StorageOptions(1);
		final ObservableOutputKeeper observableOutputKeeper = new ObservableOutputKeeper(options);
		observableOutputKeeper.prepare();

		final MemTable memTable = new MemTable(
			targetFile,
			new MemTableDescriptor(
				new CatalogEntityHeader(ENTITY_TYPE),
				createKryo(),
				false
			),
			options,
			observableOutputKeeper
		);

		observableOutputKeeper.free();

		long transactionId = 1L;
		Set<Integer> recordIds = new HashSet<>();
		int roundTrip = 0;

		do {
			observableOutputKeeper.prepare();

			final int recordCount = random.nextInt(10_000);
			final List<RecordOperation> plannedOps = new ArrayList<>(recordCount);
			final Set<Integer> touchedInThisRound = new HashSet<>();
			for (int i = 1; i <= recordCount; i++) {
				final int rndOp = random.nextInt(3);
				final RecordOperation operation;
				if (recordIds.isEmpty() || (rndOp == 0 && recordIds.size() < 100_000)) {
					operation = new RecordOperation(getNonExisting(recordIds, touchedInThisRound, random), Operation.INSERT);
					recordIds.add(operation.getRecordId());
				} else if (recordIds.size() - touchedInThisRound.size() > 10_000 && rndOp == 1) {
					operation = new RecordOperation(getExisting(recordIds, touchedInThisRound, random), Operation.UPDATE);
				} else if (recordIds.size() - touchedInThisRound.size() > 10_000 && rndOp == 2) {
					operation = new RecordOperation(getExisting(recordIds, touchedInThisRound, random), Operation.REMOVE);
					recordIds.remove(operation.getRecordId());
				} else {
					continue;
				}
				touchedInThisRound.add(operation.getRecordId());
				plannedOps.add(operation);
			}

			for (RecordOperation plannedOp : plannedOps) {
				switch (plannedOp.getOperation()) {
					case INSERT: {
						memTable.put(transactionId, new EntityBodyStoragePart(plannedOp.getRecordId()));
						plannedOp.setVersion(1);
						break;
					}
					case UPDATE: {
						final EntityBodyStoragePart existingContainer = memTable.get(plannedOp.getRecordId(), EntityBodyStoragePart.class);
						Assert.notNull(existingContainer, "Container with id " + plannedOp.getRecordId() + " unexpectedly not found!");
						memTable.put(transactionId, new EntityBodyStoragePart(existingContainer.getVersion() + 1, existingContainer.getPrimaryKey(), existingContainer.getHierarchicalPlacement(), existingContainer.getLocales(), existingContainer.getAttributeLocales(), existingContainer.getAssociatedDataKeys()));
						plannedOp.setVersion(existingContainer.getVersion() + 1);
						break;
					}
					case REMOVE: {
						memTable.remove(plannedOp.getRecordId(), EntityBodyStoragePart.class);
						break;
					}
				}
			}

			final MemTableDescriptor memTableInfo = memTable.flush(transactionId++);

			long start = System.nanoTime();
			final MemTable loadedMemTable = new MemTable(
				targetFile,
				new MemTableDescriptor(
					memTableInfo.getFileLocation(),
					memTableInfo
				),
				options,
				observableOutputKeeper
			);
			long end = System.nanoTime();

			observableOutputKeeper.free();

			assertTrue(memTable.memTableEquals(loadedMemTable));

			final MemTableFileStatistics stats = memTable.verifyContents();
			for (RecordOperation plannedOp : plannedOps) {
				final EntityBodyStoragePart entityStorageContainer = memTable.get(plannedOp.getRecordId(), EntityBodyStoragePart.class);
				if (plannedOp.getOperation() == Operation.REMOVE) {
					Assert.isTrue(entityStorageContainer == null, "Cnt " + plannedOp.getRecordId() + " should be null but was not!");
				} else {
					Assert.notNull(entityStorageContainer, "Cnt " + plannedOp.getRecordId() + " was not found!");
					assertEquals(plannedOp.getVersion(), entityStorageContainer.getVersion());
				}
			}

			System.out.println("Round trip #" + ++roundTrip + " (loaded in " + StringUtils.formatNano(end - start) + ", " + loadedMemTable.count() + " living recs. / " + stats.getRecordCount() + " total recs.)");
		} while (true);
	}

	private int getNonExisting(Set<Integer> recordIds, Set<Integer> touchedInThisRound, Random random) {
		int recPrimaryKey;
		do {
			recPrimaryKey = Math.abs(random.nextInt());
		} while (recPrimaryKey != 0 && (recordIds.contains(recPrimaryKey) || touchedInThisRound.contains(recPrimaryKey)));
		return recPrimaryKey;
	}

	private int getExisting(Set<Integer> recordIds, Set<Integer> touchedInThisRound, Random random) {
		final Iterator<Integer> it = recordIds.iterator();
		final int bound = recordIds.size() - 1;
		if (bound > 0) {
			final int steps = random.nextInt(bound);
			for (int i = 0; i < steps; i++) {
				it.next();
			}
		}
		final Integer adept = it.next();
		// retry if this id was picked already in this round
		return touchedInThisRound.contains(adept) ? getExisting(recordIds, touchedInThisRound, random) : adept;
	}

	@Nonnull
	private Function<VersionedKryoKeyInputs, VersionedKryo> createKryo() {
		return (keyInputs) -> VersionedKryoFactory.createKryo(
			keyInputs.getVersion(),
			keyInputs.getClassResolver(),
			SchemaKryoConfigurer.INSTANCE
				.andThen(
					new EntityKryoConfigurer(
						() -> new EntitySchema(ENTITY_TYPE),
						new ReflectionLookup(ReflectionCachingBehaviour.CACHE),
						keyInputs.getKeyCompressor()
					)
				)
				.andThen(
					new StoragePartConfigurer(
						DefaultKryoSerializationHelper.INSTANCE,
						keyInputs.getKeyCompressor()
					)
				)
		);
	}

	private enum Operation {
		INSERT, UPDATE, REMOVE
	}

	@Data
	private static class RecordOperation {
		private final int recordId;
		private final Operation operation;
		private int version;

	}

}