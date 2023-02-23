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

package io.evitadb.api;

import com.github.javafaker.Faker;
import io.evitadb.api.configuration.CacheOptions;
import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.configuration.EvitaConfiguration;
import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.sequence.SequenceService;
import io.evitadb.test.Entities;
import io.evitadb.test.TestFileSupport;
import io.evitadb.test.generator.DataGenerator;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.fullEntity;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test contains various integration tests for {@link Evita}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@CommonsLog
class EvitaGenerationalTest implements TestFileSupport {
	private static final String TEST_CATALOG = "testCatalog";
	private static final long SEED = 10;
	/**
	 * Count of the product that will exist in the database BEFORE the test starts.
	 */
	private static final int INITIAL_COUNT_OF_PRODUCTS = 1000;
	/**
	 * Instance of the data generator that is used for randomizing artificial test data.
	 */
	protected final DataGenerator dataGenerator = new DataGenerator();
	/**
	 * Index of created entities that allows to retrieve referenced entities when creating product.
	 */
	protected final Map<Serializable, Integer> generatedEntities = new HashMap<>();
	/**
	 * Function allowing to pseudo randomly pick referenced entity for the product.
	 */
	protected final BiFunction<Serializable, Faker, Integer> randomEntityPicker = (entityType, faker) -> {
		final Integer entityCount = generatedEntities.computeIfAbsent(entityType, serializable -> 0);
		final int primaryKey = entityCount == 0 ? 0 : faker.random().nextInt(1, entityCount);
		return primaryKey == 0 ? null : primaryKey;
	};
	/**
	 * Seeded pseudo-randomizer.
	 */
	private final Random random = new Random(SEED);
	/**
	 * Created randomized product schema.
	 */
	protected EntitySchema productSchema;
	/**
	 * Iterator that infinitely produces new artificial products.
	 */
	protected Iterator<EntityBuilder> productIterator;
	/**
	 * Open read-write session that can be used for upserting data.
	 */
	@Getter protected EvitaSession session;
	/**
	 * Prepared product with randomized content ready to be upserted to DB.
	 */
	@Getter protected EntityBuilder product;
	/**
	 * Evita instance.
	 */
	private Evita evita;
	/**
	 * Functions allows to pseudo randomly modify existing product contents.
	 */
	private Function<SealedEntity, EntityBuilder> modificationFunction;
	/**
	 * Simple counter for measuring total product count updated in the database.
	 */
	private int updateCounter;

	/**
	 * Creates new product stream for the iteration.
	 */
	protected Stream<EntityBuilder> getProductStream() {
		return dataGenerator.generateEntities(
			productSchema,
			randomEntityPicker,
			SEED
		);
	}

	/**
	 * Creates new entity and inserts it into the index.
	 */
	protected void createEntity(@Nonnull EvitaSession session, @Nonnull Map<Serializable, Integer> generatedEntities, @Nonnull EntityBuilder it) {
		final EntityReferenceContract<?> insertedEntity = session.upsertEntity(it);
		generatedEntities.compute(
			insertedEntity.getType(),
			(serializable, existing) -> ofNullable(existing).orElse(0) + 1
		);
	}

	@BeforeEach
	void setUp() throws IOException {
		SequenceService.reset();
		cleanTestDirectory();
		this.dataGenerator.clear();
		this.generatedEntities.clear();
		// reset counter
		this.updateCounter = 0;
		final String catalogName = "testCatalog";
		// prepare database
		this.evita = new Evita(
			new EvitaConfiguration(),
			new EvitaCatalogConfiguration(
				TEST_CATALOG, getTestDirectory(), new StorageOptions(1), new CacheOptions()
			)
		);
		// create bunch or entities for referencing in products
		this.evita.updateCatalog(
			catalogName,
			session -> {
				this.dataGenerator.generateEntities(
						this.dataGenerator.getSampleBrandSchema(session),
						this.randomEntityPicker,
						SEED
					)
					.limit(5)
					.forEach(it -> createEntity(session, generatedEntities, it));

				this.dataGenerator.generateEntities(
						this.dataGenerator.getSampleCategorySchema(session),
						this.randomEntityPicker,
						SEED
					)
					.limit(10)
					.forEach(it -> createEntity(session, generatedEntities, it));

				this.dataGenerator.generateEntities(
						this.dataGenerator.getSamplePriceListSchema(session),
						this.randomEntityPicker,
						SEED
					)
					.limit(4)
					.forEach(it -> createEntity(session, generatedEntities, it));

				this.dataGenerator.generateEntities(
						this.dataGenerator.getSampleStoreSchema(session),
						this.randomEntityPicker,
						SEED
					)
					.limit(12)
					.forEach(it -> createEntity(session, generatedEntities, it));

				this.dataGenerator.generateEntities(
						this.dataGenerator.getSampleParameterGroupSchema(session),
						this.randomEntityPicker,
						SEED
					)
					.limit(20)
					.forEach(it -> createEntity(session, generatedEntities, it));

				this.dataGenerator.generateEntities(
						this.dataGenerator.getSampleParameterSchema(session),
						this.randomEntityPicker,
						SEED
					)
					.limit(200)
					.forEach(it -> createEntity(session, generatedEntities, it));

				this.productSchema = dataGenerator.getSampleProductSchema(
					session,
					entitySchemaBuilder -> entitySchemaBuilder.withReferenceToEntity(
						Entities.PARAMETER,
						thatIs -> thatIs.faceted().withGroupTypeRelatedToEntity(Entities.PARAMETER_GROUP)
					)
				);
				this.dataGenerator.generateEntities(
						this.productSchema,
						this.randomEntityPicker,
						SEED
					)
					.limit(INITIAL_COUNT_OF_PRODUCTS)
					.forEach(session::upsertEntity);

				session.goLiveAndClose();
			}
		);
		// create read/write bulk session
		this.session = evita.createReadWriteSession(catalogName);
		// create product modificator
		this.modificationFunction = dataGenerator.createModificationFunction(randomEntityPicker, random);
		// create product iterator
		this.productIterator = getProductStream().iterator();
	}

	@Disabled("This test tries to generate a bunch of products store them and load again to memory.")
	@Test
	void loadTest() {
		this.evita.close();

		this.evita = new Evita(
			new EvitaConfiguration(),
			new EvitaCatalogConfiguration(
				TEST_CATALOG, getTestDirectory(), new StorageOptions(1), new CacheOptions()
			)
		);

		assertNotNull(this.evita);
	}

	@Disabled("This infinite test performs random modification operations on existing entity and verifies consistency")
	@Test
	void generationalTransactionalModificationProofTest() {
		int generation = 0;
		try {
			do {
				try {
					this.session.openTransaction();
					final int iterations = random.nextInt(500);
					for (int i = 0; i < iterations; i++) {
						final int primaryKey = random.nextInt(INITIAL_COUNT_OF_PRODUCTS) + 1;
						final SealedEntity existingEntity = session.getEntity(
							Entities.PRODUCT,
							primaryKey,
							fullEntity()
						);
						this.product = this.modificationFunction.apply(existingEntity);
						this.session.upsertEntity(this.product);
						this.updateCounter++;
					}
				} catch (Exception ex) {
					fail("Failed to execute upsert: " + ex.getMessage(), ex);
				} finally {
					this.session.closeTransaction();
				}

				generation++;

				if (generation % 3 == 0) {
					System.out.println("Survived " + generation + " generations, size on disk is " + byteCountToDisplaySize(sizeOfDirectory(getTestDirectory().toFile())));

					// reload EVITA entirely
					this.evita.close();
					this.evita = new Evita(
						new EvitaConfiguration(),
						new EvitaCatalogConfiguration(
							TEST_CATALOG, getTestDirectory(), new StorageOptions(1),
							new CacheOptions()
						)
					);
					this.session = this.evita.createReadWriteSession(TEST_CATALOG);
				}
			} while (true);
		} finally {
			System.out.println("Failed at " + generation + " generations (" + this.updateCounter + " updates), size on disk is " + byteCountToDisplaySize(sizeOfDirectory(getTestDirectory().toFile())));
		}
	}

}