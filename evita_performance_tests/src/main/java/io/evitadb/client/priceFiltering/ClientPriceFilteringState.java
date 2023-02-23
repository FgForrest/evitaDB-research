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

package io.evitadb.client.priceFiltering;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.client.ClientDataFullDatabaseState;
import io.evitadb.generators.RandomQueryGenerator;
import io.evitadb.senesi.priceFiltering.InMemoryPriceFilteringSenesiState;
import lombok.Getter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Base state class for {@link io.evitadb.senesi.SenesiBenchmark#priceFiltering_InMemory(InMemoryPriceFilteringSenesiState, Blackhole)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ClientPriceFilteringState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ClientDataFullDatabaseState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>
	implements RandomQueryGenerator {

	/**
	 * Senesi entity type of product.
	 */
	public static final String PRODUCT_ENTITY_TYPE = "Product";
	/**
	 * Pseudo-randomizer for picking random entities to fetch.
	 */
	private final Random random = new Random(SEED);
	/**
	 * Map contains set of all filterable attributes with statistics about them, that could be used to create random queries.
	 */
	private final GlobalPriceStatistics priceStatistics = new GlobalPriceStatistics();
	/**
	 * Query prepared for the measured invocation.
	 */
	@Getter protected Query query;

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		this.query = generateRandomPriceQuery(random, productSchema, priceStatistics);
	}

	@Override
	protected void processSchema(@Nonnull EntitySchema schema) {
		if (schema.getName().equals(PRODUCT_ENTITY_TYPE)) {
			this.productSchema = schema;
		}
	}

	@Override
	protected void processEntity(@Nonnull SealedEntity entity) {
		if (entity.getType().equals(PRODUCT_ENTITY_TYPE)) {
			updatePriceStatistics(entity, random, priceStatistics);
		}
	}

}
