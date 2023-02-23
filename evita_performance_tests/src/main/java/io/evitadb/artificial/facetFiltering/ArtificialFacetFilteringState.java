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

package io.evitadb.artificial.facetFiltering;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.artificial.ArtificialFullDatabaseState;
import io.evitadb.generators.RandomQueryGenerator;
import io.evitadb.test.Entities;
import lombok.Getter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.Serializable;
import java.util.*;

/**
 * Base state class for {@link io.evitadb.artificial.ArtificialEntitiesBenchmark#facetFiltering_InMemory(InMemoryFacetFilteringArtificialState, Blackhole)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ArtificialFacetFilteringState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ArtificialFullDatabaseState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>
	implements RandomQueryGenerator {

	/**
	 * Pseudo-randomizer for picking random entities to fetch.
	 */
	private final Random random = new Random(SEED);
	/**
	 * Map contains set of all faceted referenced entities, that could be used to create random queries.
	 */
	private final Map<Serializable, Set<Integer>> facetedReferences = new LinkedHashMap<>();
	/**
	 * Map contains relation between facet and its group for all faceted entity types.
	 */
	private final Map<Serializable, Map<Integer, Integer>> facetGroupsIndex = new LinkedHashMap<>();
	/**
	 * Query prepared for the measured invocation.
	 */
	@Getter protected Query query;

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		this.query = generateRandomFacetQuery(random, productSchema, facetedReferences);
	}

	@Override
	protected void processSchema(EntitySchema schema) {
		if (schema.getName().equals(Entities.PRODUCT)) {
			this.productSchema = schema;
			schema.getReferences()
				.values()
				.forEach(it -> {
					if (it.isFaceted()) {
						facetedReferences.put(it.getEntityType(), new HashSet<>());
						facetGroupsIndex.put(it.getEntityType(), new HashMap<>());
					}
				});
		}
	}

	@Override
	protected void processEntity(SealedEntity entity) {
		if (entity.getType().equals(Entities.PRODUCT)) {
			updateFacetStatistics(entity, facetedReferences, facetGroupsIndex);
		}
	}

}