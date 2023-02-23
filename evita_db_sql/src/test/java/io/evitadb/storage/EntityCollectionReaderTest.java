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

import io.evitadb.api.SqlEvita;
import io.evitadb.api.SqlEvitaSession;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.test.SqlStorageTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.test.SqlStorageTestSupport.CATALOG_TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for {@link EntityCollectionWriter}
 *
 * @author Tomáš Pozler, 2021
 * @author Jiří Bönsch, 2021
 */
class EntityCollectionReaderTest {

	private SqlEvita evita;
	private EntityCollectionContext collectionCtx;
	private EntityCollectionReader reader;

	@BeforeEach
	void setUp() {
		// delete all db data
		SqlStorageTestSupport.deleteStoredData(CATALOG_TEST_CATALOG);

		// create evita instance and configure test catalog
		evita = new SqlEvita(SqlEvitaTestSupport.createTestCatalogConfiguration());
		collectionCtx = SqlEvitaTestSupport.createTestEntityCollectionContext();
		reader = new EntityCollectionReader(collectionCtx);

		switchToAlive();
	}

	@AfterEach
	void tearDown() {
		// close evita and clear data
		evita.close();
	}


	@Test
	void shouldFindSchema() {
		Optional<EntitySchema> schema = reader.findSchema();
		assertFalse(schema.isPresent());

		updateCatalog(session -> {
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 1));
		});

		schema = reader.findSchema();
		assertEquals(new EntitySchema(collectionCtx.getEntityType()), schema.get());
	}

	@Test
	void shouldFindEntityByPrimaryKey() {
		updateCatalog(session -> {
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 1));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 2));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 3));
		});

		final Optional<Entity> entity = reader.findEntityByPrimaryKey(2);
		assertEquals(createEmptyEntity(collectionCtx.getEntityType(), 2).toInstance(), entity.get());
	}

	@Test
	void shouldFindEntitiesByRequest() {
		updateCatalog(session -> {
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 1));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 2));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 3));
		});

		// whole entity
		DataChunk<Serializable> entities = reader.findEntitiesByRequest(new SqlEvitaRequest(
				query(
					entities(collectionCtx.getEntityType()),
					filterBy(
						primaryKey(2)
					),
					require(fullEntity())
				),
				ZonedDateTime.now()
		));
		assertEquals(1, entities.getTotalRecordCount());
		assertEquals(createEmptyEntity(collectionCtx.getEntityType(), 2).toInstance(), entities.getData().get(0));

		// entity reference
		entities = reader.findEntitiesByRequest(new SqlEvitaRequest(
				query(
					entities(collectionCtx.getEntityType()),
					filterBy(
						primaryKey(2)
					)
				),
				ZonedDateTime.now()
		));
		assertEquals(1, entities.getTotalRecordCount());
		assertEquals(createEntityReference(2), entities.getData().get(0));
	}

	@Test
	void shouldFindEntitiesByRequestInComplexContext() {
		updateCatalog(session -> {
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 1));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 2));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), 3));
		});

		// whole entity
		final SqlEvitaRequest requestWholeEntity = new SqlEvitaRequest(
				query(
					entities(collectionCtx.getEntityType()),
					filterBy(
						primaryKey(2)
					),
					require(fullEntity())
				),
				ZonedDateTime.now()
		);
		DataChunk<Serializable> entities = reader.fetchComplex(
				requestWholeEntity,
				() -> reader.findEntitiesByRequest(requestWholeEntity)
		);
		assertEquals(1, entities.getTotalRecordCount());
		assertEquals(createEmptyEntity(collectionCtx.getEntityType(), 2).toInstance(), entities.getData().get(0));

		// entity reference
		final SqlEvitaRequest requestEntityReference = new SqlEvitaRequest(
				query(
					entities(collectionCtx.getEntityType()),
					filterBy(
						primaryKey(2)
					)
				),
				ZonedDateTime.now()
		);
		entities = reader.fetchComplex(
				requestEntityReference,
				() -> reader.findEntitiesByRequest(requestEntityReference)
		);
		assertEquals(1, entities.getTotalRecordCount());
		assertEquals(createEntityReference(2), entities.getData().get(0));
	}

	@Test
	void shouldCountStoredEntities() {
		assertEquals(0, reader.countStoredEntities());

		updateCatalog(session -> {
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), null));
		});
		assertEquals(1, reader.countStoredEntities());

		updateCatalog(session -> {
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), null));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), null));
			session.upsertEntity(createEmptyEntity(collectionCtx.getEntityType(), null));
		});
		assertEquals(4, reader.countStoredEntities());
	}


	private void updateCatalog(Consumer<SqlEvitaSession> updater) {
		evita.updateCatalog(CATALOG_TEST_CATALOG, updater);
	}

	private void switchToAlive() {
		evita.updateCatalog(
			CATALOG_TEST_CATALOG,
			session -> {
				session.goLiveAndClose();
			}
		);
	}

	private EntityBuilder createEmptyEntity(Serializable type, Integer primaryKey) {
		return new InitialEntityBuilder(type, primaryKey);
	}

	private EntityReference createEntityReference(int primaryKey) {
		return new EntityReference(collectionCtx.getEntityType(), primaryKey);
	}
}
