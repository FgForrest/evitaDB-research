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

package io.evitadb.client.randomSingleRead;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.client.ClientDataFullDatabaseState;
import io.evitadb.generators.RandomQueryGenerator;
import io.evitadb.senesi.randomSingleRead.InMemorySingleReadSenesiState;
import lombok.Getter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.*;

/**
 * Base state class for {@link io.evitadb.senesi.SenesiBenchmark#singleEntityRead_InMemory(InMemorySingleReadSenesiState, Blackhole)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ClientSingleReadState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ClientDataFullDatabaseState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> implements RandomQueryGenerator {

	/**
	 * Client entity type of product.
	 */
	public static final String PRODUCT_ENTITY_TYPE = "Product";
	/**
	 * Client entity type of brand.
	 */
	public static final String BRAND_ENTITY_TYPE = "Brand";
	/**
	 * Client entity type of category.
	 */
	public static final String CATEGORY_ENTITY_TYPE = "Category";
	/**
	 * Client entity type of group.
	 */
	public static final String GROUP_ENTITY_TYPE = "Group";
	/**
	 * Pseudo-randomizer for picking random entities to fetch.
	 */
	private final Random random = new Random(SEED);
	/**
	 * Query prepared for the measured invocation.
	 */
	@Getter protected Query query;
	/**
	 * Map contains set of all filterable attributes with statistics about them, that could be used to create random queries.
	 */
	private final GlobalPriceStatistics priceStatistics = new GlobalPriceStatistics();

	@Override
	public void setUp() {
		super.setUp();
		try (final SESSION session = this.evita.createReadOnlySession(getCatalogName())) {
			this.productSchema = session.getEntitySchema(PRODUCT_ENTITY_TYPE);
		}
	}

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		final Set<EntityContentRequire> requirements = new HashSet<>();
		/* always fetch product body */
		requirements.add(entityBody());
		/* 75% times fetch attributes */
		if (random.nextInt(4) != 0) {
			requirements.add(attributes());
		}
		/* 75% times fetch associated data */
		if (random.nextInt(4) != 0) {
			requirements.add(
				associatedData(
					this.productSchema
						.getAssociatedData()
						.keySet()
						.stream()
						.filter(it -> random.nextInt(4) != 0)
						.toArray(String[]::new)
				)
			);
		}
		/* 50% times fetch prices */
		final FilterConstraint priceConstraint;
		if (random.nextBoolean()) {
			priceConstraint = createRandomPriceFilterBy(
				random, priceStatistics, productSchema.getIndexedPricePlaces()
			);

			/* 75% only filtered prices */
			if (random.nextInt(4) != 0) {
				requirements.add(prices());
			} else {
				requirements.add(allPrices());
			}
		} else {
			priceConstraint = null;
		}

		/* 25% times load references */
		if (random.nextInt(4) == 0) {
			/* 50% times load all references */
			if (random.nextBoolean()) {
				requirements.add(references());
			} else {
				/* 50% select only some of them */
				requirements.add(
					references(
						Stream.of(BRAND_ENTITY_TYPE, CATEGORY_ENTITY_TYPE, GROUP_ENTITY_TYPE)
							.filter(it -> random.nextBoolean())
							.toArray(Serializable[]::new)
					)
				);
			}
		}

		final Locale randomExistingLocale = this.productSchema
			.getLocales()
			.stream()
			.skip(random.nextInt(this.productSchema.getLocales().size()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No locales found!"));

		final List<Integer> productIds = generatedEntities.get(PRODUCT_ENTITY_TYPE);
		this.query = Query.query(
			entities(PRODUCT_ENTITY_TYPE),
			filterBy(
				and(
					primaryKey(productIds.get(random.nextInt(productIds.size()))),
					language(randomExistingLocale),
					priceConstraint
				)
			),
			require(requirements.toArray(new EntityContentRequire[0]))
		);
	}

	@Override
	protected void processEntity(@Nonnull SealedEntity entity) {
		entity.getPrices().forEach(it -> this.priceStatistics.updateValue(it, random));
	}

}
