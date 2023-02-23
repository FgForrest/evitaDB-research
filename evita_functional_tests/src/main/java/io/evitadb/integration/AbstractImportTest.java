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

package io.evitadb.integration;

import io.evitadb.api.*;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.test.TestConstants;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.snapshot.EntityConsumer;
import io.evitadb.test.snapshot.GenericSerializedCatalogReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test imports data from production database into a tested implementation and verifies that all data are persisted
 * successfully and when re-reading all of them they pass the equality check with the original.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
@Tag(TestConstants.INTEGRATION_TEST)
abstract class AbstractImportTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> implements TestConstants {
	protected static final String CUSTOMER_DATA_SET = "CustomerDataSet";

	@DataSet(CUSTOMER_DATA_SET)
	void setUp(EVITA evita, String catalogName) {
		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting writing process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final EntityConsumer consumer = getEntityCollectionContentsConsumer(evita, catalogName);
		reader.read(catalogName, getDataDirectory().resolve(catalogName), consumer);
		consumer.close();
	}

	/**
	 * This test reads contents of the Evita catalog from the generic binary serialized form and imports all entities
	 * to the specific Evita DB implementation.
	 */
	@DisplayName("Import and verify contents of production database")
	@Test
	void shouldDeserializeCatalog(@UseDataSet(CUSTOMER_DATA_SET) EVITA evita, String catalogName) {
		log.info("Starting verification process ...");
		// verify stored data
		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		final ReadAndVerifyEntityConsumer consumer = new ReadAndVerifyEntityConsumer(
			evita, catalogName, schema -> getEntityCount(evita, catalogName, schema.getName())
		);
		reader.read(catalogName, getDataDirectory().resolve(catalogName), consumer);
		consumer.close();
		log.info(
			"Sequential get by id of {} entities took in average {}.",
			consumer.getEntityCount(), StringUtils.formatNano(consumer.getDuration() / consumer.getEntityCount())
		);
	}

	/**
	 * This method should return count of all entities stored in the collection of the `entityType` in catalog with name
	 * of `catalogName`.
	 */
	protected abstract int getEntityCount(EVITA evita, String catalogName, Serializable entityType);

	/**
	 * This method should return schema stored for the collection of the `entityType` in catalog with name
	 * of `catalogName`.
	 */
	@Nonnull
	protected abstract EntitySchema getSchema(EVITA evita, String catalogName, Serializable entityType);

	/**
	 * This method should find entity of type `entityType` in catalog with name of `catalogName` by its primary key.
	 */
	@Nonnull
	protected abstract EntityContract getEntity(EVITA evita, String catalogName, Serializable entityType, Integer primaryKey);

	/**
	 * This method should return an implementation lambda that will insert schema and entities into the Evita DB
	 * implementation for the catalog with name `catalogName`.
	 */
	@Nonnull
	protected abstract EntityConsumer getEntityCollectionContentsConsumer(EVITA evita, String catalogName);

	private class ReadAndVerifyEntityConsumer implements EntityConsumer {
		private final EVITA evita;
		private final String catalogName;
		private final Function<EntitySchema, Integer> realRecordCount;
		private final Map<EntitySchema, Integer> entityCounters = new HashMap<>();
		@Getter private long duration;

		public ReadAndVerifyEntityConsumer(EVITA evita, String catalogName, Function<EntitySchema, Integer> realRecordCount) {
			this.evita = evita;
			this.catalogName = catalogName;
			this.realRecordCount = realRecordCount;
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			assertExactlyEquals(schema, getSchema(evita, catalogName, schema.getName()), "Schema is different!");
			this.entityCounters.put(schema, 0);
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			long start = System.nanoTime();
			final EntityContract serializedEntity = getEntity(evita, catalogName, schema.getName(), entity.getPrimaryKey());
			duration += (System.nanoTime() - start);
			assertNotNull(serializedEntity, "Entity with id " + entity.getPrimaryKey() + " is not found after serialization!");
			assertExactlyEquals(entity, serializedEntity, "Entity with id " + entity.getPrimaryKey() + " was corrupted!");
			this.entityCounters.computeIfPresent(schema, (entitySchema, count) -> count + 1);
			return true;
		}

		public long getEntityCount() {
			return entityCounters.values().stream().mapToInt(it -> it).sum();
		}

		@Override
		public void close() {
			for (Entry<EntitySchema, Integer> entry : entityCounters.entrySet()) {
				assertEquals(
					entry.getValue(), realRecordCount.apply(entry.getKey()),
					"Expected " + entry.getValue() + " of " + entry.getKey().getName() + " entities but was " + realRecordCount.apply(entry.getKey())
				);
			}
		}
	}
}
