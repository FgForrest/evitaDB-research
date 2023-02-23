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

package io.evitadb.client.bulkWrite;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.structure.CopyExistingEntityBuilder;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.client.ClientDataState;
import io.evitadb.senesi.bulkWrite.InMemoryBulkWriteSenesiState;
import io.evitadb.test.snapshot.EntityConsumer;
import io.evitadb.test.snapshot.GenericSerializedCatalogReader;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Base state class for {@link io.evitadb.senesi.SenesiBenchmark#bulkInsertThroughput_InMemory(InMemoryBulkWriteSenesiState)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ClientBulkWriteState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ClientDataState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {

	/**
	 * Senesi entity type of product.
	 */
	public static final String PRODUCT_ENTITY_TYPE = "Product";
	/**
	 * Simple counter for measuring total product count inserted into the database.
	 */
	private int counter;
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
		// reset counter
		this.counter = 0;
		final String catalogName = getCatalogName();
		final String writeCatalogName = catalogName + "_bulkWrite";
		// prepare database
		this.evita = createEmptyEvitaInstance(writeCatalogName);
		// create reader instance
		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader(entityType -> !PRODUCT_ENTITY_TYPE.equalsIgnoreCase(entityType.toString()));
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
							session.upsertEntity(new CopyExistingEntityBuilder(entity));
							return true;
						}

						@Override
						public void close() {

						}
					}
				);
			}
		);
		// create read/write bulk session
		this.session = evita.createReadWriteSession(writeCatalogName);
		// create product iterator
		initProductIterator(CopyExistingEntityBuilder::new);
	}

	/**
	 * Method is called when benchmark iteration is finished, closes session and prints statistics.
	 */
	@TearDown(Level.Iteration)
	public void closeSession() {
		this.session.close();
		this.evita.close();
		System.out.println("\nInserted " + counter + " records in iteration.");
	}

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		if (productIterator.hasNext()) {
			this.product = productIterator.next();
			// keep track of already assigned primary keys (may have gaps, may be in random order)
			if (this.product.getPrimaryKey() > this.pkPeek) {
				this.pkPeek = this.product.getPrimaryKey();
			}
		} else {
			// when products are exhausted - start again from scratch
			initProductIterator(it -> new CopyExistingEntityBuilder(it, ++pkPeek));
			// initialize first product from the new round
			this.product = productIterator.next();
		}
		counter++;
	}

	/**
	 * Initialized product iterator.
	 */
	private void initProductIterator(Function<Entity, EntityBuilder> entityBuilderFactory) {
		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		this.productIterator = StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
					reader.read(getCatalogName(), getDataDirectory().resolve(getCatalogName()), PRODUCT_ENTITY_TYPE),
					Spliterator.DISTINCT & Spliterator.IMMUTABLE & Spliterator.NONNULL
				), false
			)
			// but assign new primary keys starting from highest observed primary key so far plus one
			.map(entityBuilderFactory)
			.iterator();
	}

}
