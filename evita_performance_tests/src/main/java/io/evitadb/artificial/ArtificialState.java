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

package io.evitadb.artificial;

import com.github.javafaker.Faker;
import io.evitadb.api.*;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.setup.CatalogSetup;
import io.evitadb.test.TestConstants;
import io.evitadb.test.generator.DataGenerator;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Base state class for all artifical based benchmarks.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ArtificialState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	implements TestConstants, CatalogSetup<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {
	/**
	 * Fixed seed allows to replay the same randomized data multiple times.
	 */
	public static final long SEED = 42;
	/**
	 * Default name of the test catalog.
	 */
	public static final String TEST_CATALOG = "testCatalog";
	/**
	 * Instance of the data generator that is used for randomizing artificial test data.
	 */
	protected final DataGenerator dataGenerator = new DataGenerator(faker -> {
		final int selectedOption = faker.random().nextInt(10);
		if (selectedOption < 6) {
			// 60% of basic products
			return PriceInnerRecordHandling.NONE;
		} else if (selectedOption < 9) {
			// 30% of variant products
			return PriceInnerRecordHandling.FIRST_OCCURRENCE;
		} else {
			// 10% of set products
			return PriceInnerRecordHandling.SUM;
		}
	});
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
	 * Open read-write session that can be used for upserting data.
	 */
	protected final ThreadLocal<SESSION> session = new ThreadLocal<>();
	/**
	 * Created randomized product schema.
	 */
	protected EntitySchema productSchema;
	/**
	 * Iterator that infinitely produces new artificial products.
	 */
	protected Iterator<EntityBuilder> productIterator;
	/**
	 * Open Evita instance.
	 */
	@Getter protected EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> evita;
	/**
	 * Prepared product with randomized content ready to be upserted to DB.
	 */
	@Getter protected EntityBuilder product;

	/**
	 * Returns an existing session unique for the thread or creates new one.
	 */
	public SESSION getSession() {
		return getSession(() -> this.evita.createReadOnlySession(getCatalogName()));
	}

	/**
	 * Returns an existing session unique for the thread or creates new one.
	 */
	public SESSION getSession(Supplier<SESSION> creatorFct) {
		final SESSION session = this.session.get();
		if (session == null || !session.isActive()) {
			final SESSION createdSession = creatorFct.get();
			this.session.set(createdSession);
			return createdSession;
		} else {
			return session;
		}
	}

	/**
	 * Returns name of the test catalog.
	 */
	protected String getCatalogName() {
		return TEST_CATALOG;
	}

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
	protected void createEntity(@Nonnull SESSION session, @Nonnull Map<Serializable, Integer> generatedEntities, @Nonnull EntityBuilder it) {
		final EntityReferenceContract<?> insertedEntity = session.upsertEntity(it);
		generatedEntities.compute(
			insertedEntity.getType(),
			(serializable, existing) -> ofNullable(existing).orElse(0) + 1
		);
	}

}
