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

package io.evitadb.client.transactionalWrite;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.CopyExistingEntityBuilder;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.utils.Assert;
import io.evitadb.client.ClientDataState;
import io.evitadb.senesi.transactionalWrite.InMemoryTransactionalWriteSenesiState;
import io.evitadb.test.snapshot.EntityConsumer;
import io.evitadb.test.snapshot.GenericSerializedCatalogReader;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static io.evitadb.api.query.QueryConstraints.fullEntity;

/**
 * Base state class for {@link io.evitadb.senesi.SenesiBenchmark#transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteSenesiState)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ClientTransactionalWriteState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ClientDataState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {

	/**
	 * Senesi entity type of product.
	 */
	public static final String PRODUCT_ENTITY_TYPE = "Product";
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
	 * Highest observed product primary key.
	 */
	private int pkPeek;

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
		final String writeCatalogName = catalogName + "_transactionalWrite";
		this.evita = createEmptyEvitaInstance(writeCatalogName);
		// create reader instance
		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		final AtomicInteger counter = new AtomicInteger();
		// create bunch or entities for referencing in products
		evita.updateCatalog(
			writeCatalogName,
			session -> {
				reader.read(
					catalogName,
					getDataDirectory().resolve(catalogName),
					new EntityConsumer() {
						@Override
						public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
							session.defineSchema(schema);
						}

						@Override
						public boolean accept(EntitySchema schema, Entity entity) {
							final EntityReference createdEntity = createEntity(session, new CopyExistingEntityBuilder(entity));
							if (PRODUCT_ENTITY_TYPE.equals(schema.getName())) {
								addToGeneratedEntities(schema, createdEntity.getPrimaryKey());
							}
							return !PRODUCT_ENTITY_TYPE.equals(schema.getName()) || counter.incrementAndGet() < INITIAL_COUNT_OF_PRODUCTS;
						}

						@Override
						public void close() {

						}
					}
				);

				session.goLiveAndClose();
			}
		);
		// create read/write bulk session
		this.session = evita.createReadWriteSession(writeCatalogName);
		// create product modificator
		this.modificationFunction = dataGenerator.createModificationFunction(randomEntityPicker, random);
		// create product iterator
		this.productIterator = StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
					reader.read(catalogName, getDataDirectory().resolve(getCatalogName()), PRODUCT_ENTITY_TYPE),
					Spliterator.DISTINCT & Spliterator.IMMUTABLE & Spliterator.NONNULL
				), false
			)
			.skip(INITIAL_COUNT_OF_PRODUCTS)
			.map(it -> (EntityBuilder) new CopyExistingEntityBuilder(it))
			.iterator();
		this.productSchema = session.getEntitySchema(PRODUCT_ENTITY_TYPE);
	}

	/**
	 * Method is called when benchmark iteration is finished, closes session and prints statistics.
	 */
	@TearDown(Level.Iteration)
	public void closeSession() {
		this.session.close();
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
		this.session.openTransaction();
		// there is 50% on update instead of insert
		if (random.nextBoolean()) {
			final List<Integer> existingProductIds = generatedEntities.get(PRODUCT_ENTITY_TYPE);
			final int index = random.nextInt(existingProductIds.size());
			final Integer primaryKey = existingProductIds.get(index);
			final SealedEntity existingEntity = session.getEntity(
				PRODUCT_ENTITY_TYPE,
				primaryKey,
				fullEntity()
			);
			Assert.notNull(existingEntity, "Entity with id " + primaryKey + " unexpectedly not found!");
			this.product = this.modificationFunction.apply(existingEntity);
			this.updateCounter++;
		} else {
			if (productIterator.hasNext()) {
				this.product = productIterator.next();
				addToGeneratedEntities(productSchema, this.product.getPrimaryKey());
				// keep track of already assigned primary keys (may have gaps, may be in random order)
				if (this.product.getPrimaryKey() > this.pkPeek) {
					this.pkPeek = this.product.getPrimaryKey();
				}
			} else {
				// when products are exhausted - start again from scratch
				final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
				this.productIterator = StreamSupport.stream(
						Spliterators.spliteratorUnknownSize(
							reader.read(getCatalogName(), getDataDirectory().resolve(getCatalogName()), PRODUCT_ENTITY_TYPE),
							Spliterator.DISTINCT & Spliterator.IMMUTABLE & Spliterator.NONNULL
						), false
					)
					// but assign new primary keys starting from highest observed primary key so far plus one
					.map(it -> (EntityBuilder) new CopyExistingEntityBuilder(it, ++pkPeek))
					.iterator();
				// initialize first product from the new round
				this.product = productIterator.next();
				addToGeneratedEntities(this.productSchema, this.product.getPrimaryKey());
			}
			this.insertCounter++;
		}
	}

	@TearDown(Level.Invocation)
	public void finishCall() {
		this.session.closeTransaction();
	}

	private void addToGeneratedEntities(EntitySchema productSchema, Integer primaryKey) {
		generatedEntities
			.computeIfAbsent(productSchema.getName(), serializable -> new LinkedList<>())
			.add(primaryKey);
	}

}
