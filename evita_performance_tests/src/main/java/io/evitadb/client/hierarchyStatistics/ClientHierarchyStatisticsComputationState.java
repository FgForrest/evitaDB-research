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

package io.evitadb.client.hierarchyStatistics;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.client.ClientDataFullDatabaseState;
import io.evitadb.generators.RandomQueryGenerator;
import io.evitadb.senesi.hierarchyStatistics.InMemoryHierarchyStatisticsComputationSenesiState;
import lombok.Getter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * Base state class for {@link io.evitadb.senesi.SenesiBenchmark#hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationSenesiState, Blackhole)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ClientHierarchyStatisticsComputationState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
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
	 * Set contains all `entityTypes` that are hierarchical and are referenced from product entity.
	 */
	private final Set<Serializable> referencedHierarchicalEntities = new LinkedHashSet<>();
	/**
	 * Set contains all `entityTypes` that are hierarchical.
	 */
	private final Set<Serializable> hierarchicalEntities = new HashSet<>();
	/**
	 * Query prepared for the measured invocation.
	 */
	@Getter protected Query query;

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		if (referencedHierarchicalEntities.isEmpty()) {
			productSchema.getReferences()
				.values()
				.forEach(it -> {
					if (it.isEntityTypeRelatesToEntity() && hierarchicalEntities.contains(it.getEntityType())) {
						referencedHierarchicalEntities.add(it.getEntityType());
					}
				});
			Assert.isTrue(!referencedHierarchicalEntities.isEmpty(), "No referenced entity is hierarchical!");
		}
		this.query = generateRandomParentSummaryQuery(
			random, productSchema, referencedHierarchicalEntities
		);
	}

	@Override
	protected void processSchema(@Nonnull EntitySchema schema) {
		if (schema.getName().equals(PRODUCT_ENTITY_TYPE)) {
			this.productSchema = schema;
		} else {
			if (schema.isWithHierarchy()) {
				hierarchicalEntities.add(schema.getName());
			}
		}
	}

}