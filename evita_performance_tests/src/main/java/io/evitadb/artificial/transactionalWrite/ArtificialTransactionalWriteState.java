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

package io.evitadb.artificial.transactionalWrite;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.artificial.ArtificialState;
import io.evitadb.test.Entities;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Random;
import java.util.function.Function;

import static io.evitadb.api.query.QueryConstraints.fullEntity;

/**
 * Base state class for {@link io.evitadb.artificial.ArtificialEntitiesBenchmark#transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteArtificialState)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ArtificialTransactionalWriteState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> extends ArtificialState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {
	/**
	 * Count of the product that will exist in the database BEFORE the test starts.
	 */
	private static final int INITIAL_COUNT_OF_PRODUCTS = 10000;
	/**
	 * Seeded pseudo-randomizer.
	 */
	private final Random random = new Random(SEED);
	/**
	 * Functions allows to pseudo randomly modify existing product contents.
	 */
	private Function<SealedEntity, EntityBuilder> modificationFunction;
	/**
	 * Simple counter for measuring total product count inserted into the database.
	 */
	private int insertCounter;
	/**
	 * Simple counter for measuring total product count updated in the database.
	 */
	private int updateCounter;

	/**
	 * Method is invoked before each benchmark iteration.
	 * Method creates bunch of brand, categories, price lists and stores that cen be referenced in products.
	 * Method also prepares infinite iterator for creating new products for insertion.
	 */
	@Setup(Level.Iteration)
	public void setUp() {
		this.dataGenerator.clear();
		this.generatedEntities.clear();
		// reset counter
		this.insertCounter = 0;
		this.updateCounter = 0;
		final String catalogName = getCatalogName();
		// prepare database
		this.evita = createEmptyEvitaInstance(catalogName);
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

				this.productSchema = dataGenerator.getSampleProductSchema(session);
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
		// create product modificator
		this.modificationFunction = dataGenerator.createModificationFunction(randomEntityPicker, random);
		// create product iterator
		this.productIterator = getProductStream().iterator();
	}

	/**
	 * We need writable sessions here.
	 */
	@Override
	public SESSION getSession() {
		return getSession(() -> evita.createReadWriteSession(getCatalogName()));
	}

	/**
	 * Returns name of the test catalog.
	 */
	protected String getCatalogName() {
		return TEST_CATALOG + "_transactionalWrite";
	}

	/**
	 * Method is called when benchmark iteration is finished, closes session, evita instance and prints statistics.
	 */
	@TearDown(Level.Iteration)
	public void closeEvita() {
		this.evita.close();
		System.out.println("\nInitial database size was " + INITIAL_COUNT_OF_PRODUCTS + " of records.");
		System.out.println("Inserted " + insertCounter + " records in iteration.");
		System.out.println("Updated " + updateCounter + " records in iteration.");
	}

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		final SESSION session = getSession();
		session.openTransaction();
		// there is 50% on update instead of insert
		if (random.nextBoolean()) {
			final SealedEntity existingEntity = session.getEntity(
				Entities.PRODUCT,
				random.nextInt(INITIAL_COUNT_OF_PRODUCTS + this.insertCounter) + 1,
				fullEntity()
			);
			this.product = this.modificationFunction.apply(existingEntity);
			this.updateCounter++;
		} else {
			this.product = productIterator.next();
			this.insertCounter++;
		}
	}

	@TearDown(Level.Invocation)
	public void finishCall() {
		getSession().closeTransaction();
	}

}
