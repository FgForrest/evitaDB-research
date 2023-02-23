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

package io.evitadb.api.storage;

import io.evitadb.api.Catalog;
import io.evitadb.api.CatalogState;
import io.evitadb.api.EntityCollection;
import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.exception.InvalidFileNameException;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.storage.exception.DuplicateFileNameException;
import io.evitadb.api.storage.exception.UnexpectedCatalogContentsException;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.cache.NoCacheSupervisor;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.storage.IOService;
import io.evitadb.storage.ObservableOutputKeeper;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.CatalogHeader;
import io.evitadb.test.Entities;
import io.evitadb.test.TestFileSupport;
import io.evitadb.test.generator.DataGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This test verifies contract of {@link IOService}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class IOServiceTest implements TestFileSupport {
	private static final String TEST_CATALOG_NAME = "test";

	private final DataGenerator dataGenerator = new DataGenerator();
	private final IOService ioService = new IOService(
		new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE)
	);

	@BeforeEach
	public void setUp() throws IOException {
		prepareEmptyTestDirectory();
	}

	@AfterEach
	public void tearDown() throws IOException {
		cleanTestDirectory();
	}

	@Test
	void shouldSerializeAndDeserializeCatalogHeader() {
		final ObservableOutputKeeper observableOutputKeeper = new ObservableOutputKeeper(getStorageOptions());
		observableOutputKeeper.prepare();

		final EntityCollection brandCollection = constructEntityCollectionWithSomeEntities(
			dataGenerator.getSampleBrandSchema(UnaryOperator.identity()), ioService, observableOutputKeeper
		);
		final EntityCollection storeCollection = constructEntityCollectionWithSomeEntities(
			dataGenerator.getSampleStoreSchema(UnaryOperator.identity()), ioService, observableOutputKeeper
		);
		final EntityCollection productCollection = constructEntityCollectionWithSomeEntities(
			dataGenerator.getSampleProductSchema(UnaryOperator.identity()), ioService, observableOutputKeeper
		);

		final List<CatalogEntityHeader> entityHeaders = new ArrayList<>(3);
		entityHeaders.add(productCollection.flush());
		entityHeaders.add(brandCollection.flush());
		entityHeaders.add(storeCollection.flush());

		// try to serialize
		ioService.storeHeader(getTestDirectory(), TEST_CATALOG_NAME, CatalogState.WARMING_UP, 0, entityHeaders);

		// release buffers
		observableOutputKeeper.free();

		// try to deserialize again
		final CatalogHeader catalogHeader = ioService.readHeader(getTestDirectory(), TEST_CATALOG_NAME);

		assertNotNull(catalogHeader);
		final Map<Serializable, CatalogEntityHeader> entityTypesIndex = catalogHeader.getEntityTypesIndex();
		assertEntityCollectionsHasIdenticalContent(brandCollection, catalogHeader, entityTypesIndex.get(Entities.BRAND));
		assertEntityCollectionsHasIdenticalContent(storeCollection, catalogHeader, entityTypesIndex.get(Entities.STORE));
		assertEntityCollectionsHasIdenticalContent(productCollection, catalogHeader, entityTypesIndex.get(Entities.PRODUCT));
	}

	@Test
	void shouldDetectInvalidCatalogContents() {
		final ObservableOutputKeeper observableOutputKeeper = new ObservableOutputKeeper(getStorageOptions());
		observableOutputKeeper.prepare();

		final EntityCollection productCollection = constructEntityCollectionWithSomeEntities(
			dataGenerator.getSampleProductSchema(UnaryOperator.identity()), ioService, observableOutputKeeper
		);
		final EntityCollection brandCollection = constructEntityCollectionWithSomeEntities(
			dataGenerator.getSampleBrandSchema(UnaryOperator.identity()), ioService, observableOutputKeeper
		);
		final EntityCollection storeCollection = constructEntityCollectionWithSomeEntities(
			dataGenerator.getSampleStoreSchema(UnaryOperator.identity()), ioService, observableOutputKeeper
		);

		// try to serialize
		ioService.storeHeader(
			getTestDirectory(),
			TEST_CATALOG_NAME,
			CatalogState.WARMING_UP,
			0,
			Arrays.asList(
				productCollection.flush(),
				brandCollection.flush(),
				storeCollection.flush()
			)
		);

		// release buffers
		observableOutputKeeper.free();

		assertThrows(
			UnexpectedCatalogContentsException.class,
			() -> ioService.readHeader(getTestDirectory(), "somethingElse")
		);
	}

	@Test
	void shouldSignalizeInvalidEntityNames() {
		assertThrows(
			InvalidFileNameException.class,
			() -> ioService.verifyEntityType(
				Stream.empty(),
				"→"
			)
		);
	}

	@Test
	void shouldSignalizeConflictingEntityNames() {
		assertThrows(
			DuplicateFileNameException.class,
			() -> ioService.verifyEntityType(
				Stream.of("á"),
				"A"
			)
		);
	}

	@Test
	void shouldReturnNullOnEmptyDirectory() {
		assertNull(ioService.readHeader(getTestDirectory(), TEST_CATALOG_NAME));
	}

	/*
		PRIVATE METHODS
	 */

	@Nonnull
	private EntityCollection constructEntityCollectionWithSomeEntities(@Nonnull EntitySchema schema, @Nonnull IOService ioService, @Nonnull ObservableOutputKeeper outputKeeper) {
		final EntityCollection entityCollection = new EntityCollection(
			getMockCatalog(schema),
			schema,
			new CatalogHeader(TEST_CATALOG_NAME, CatalogState.WARMING_UP),
			new CatalogEntityHeader(schema.getName()),
			getTestDirectory(),
			getStorageOptions(),
			outputKeeper,
			ioService,
			NoCacheSupervisor.INSTANCE,
			false
		);

		dataGenerator.generateEntities(
				schema,
				(serializable, faker) -> null,
				40
			)
			.limit(10)
			.forEach(it -> entityCollection.upsertEntity(it.toMutation()));

		return entityCollection;
	}

	@Nonnull
	private StorageOptions getStorageOptions() {
		return new StorageOptions(1);
	}

	private void assertEntityCollectionsHasIdenticalContent(EntityCollection entityCollection, CatalogHeader catalogHeader, CatalogEntityHeader collectionHeader) {
		assertEquals(entityCollection.size(), collectionHeader.getRecordCount());
		final ObservableOutputKeeper outputKeeper = new ObservableOutputKeeper(getStorageOptions());
		outputKeeper.prepare();

		final EntitySchema schema = entityCollection.getSchema();
		final EntityCollection collection = new EntityCollection(
			getMockCatalog(schema),
			catalogHeader, collectionHeader, getTestDirectory(), getStorageOptions(),
			outputKeeper, ioService, NoCacheSupervisor.INSTANCE, false
		);

		final Iterator<Entity> it = entityCollection.entityIterator();
		while (it.hasNext()) {
			final Entity originEntity = it.next();
			final EvitaResponseBase<Serializable> response = collection.getEntities(
				new EvitaRequest(
					query(
						entities(entityCollection.getSchema().getName()),
						filterBy(primaryKey(originEntity.getPrimaryKey())),
						require(fullEntity())
					),
					ZonedDateTime.now()
				)
			);
			assertEquals(1, response.getRecordData().size());
			final SealedEntity deserializedEntity = (SealedEntity) response.getRecordData().get(0);
			assertFalse(originEntity.differsFrom(deserializedEntity));
		}

		outputKeeper.free();
	}

	@Nonnull
	private Catalog getMockCatalog(@Nonnull EntitySchema schema) {
		final Catalog mockCatalog = mock(Catalog.class);
		when(mockCatalog.getEntitySchema(schema.getName())).thenReturn(schema);
		when(mockCatalog.getEntityIndexIfExists(Mockito.eq(schema.getName()), any(EntityIndexKey.class))).thenReturn(null);
		return mockCatalog;
	}

}