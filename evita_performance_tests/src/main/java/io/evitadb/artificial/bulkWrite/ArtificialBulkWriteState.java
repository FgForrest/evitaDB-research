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

package io.evitadb.artificial.bulkWrite;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.artificial.ArtificialState;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Base state class for {@link io.evitadb.artificial.ArtificialEntitiesBenchmark#bulkInsertThroughput_InMemory(InMemoryBulkWriteArtificialState)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ArtificialBulkWriteState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> extends ArtificialState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {
	/**
	 * Simple counter for measuring total product count inserted into the database.
	 */
	private int counter;

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
		this.counter = 0;
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
			}
		);
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
		return TEST_CATALOG + "_bulkWrite";
	}

	/**
	 * Method is called when benchmark iteration is finished, closes session and prints statistics.
	 */
	@TearDown(Level.Iteration)
	public void closeEvita() {
		this.evita.close();
		System.out.println("\nInserted " + counter + " records in iteration.");
	}

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		this.product = productIterator.next();
		counter++;
	}

}
