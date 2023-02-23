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

package io.evitadb.artificial.randomPageRead;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.artificial.ArtificialFullDatabaseState;
import io.evitadb.test.Entities;
import io.evitadb.test.generator.DataGenerator;
import lombok.Getter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.*;

/**
 * Base state class for {@link io.evitadb.artificial.ArtificialEntitiesBenchmark#paginatedEntityRead_InMemory(InMemoryPageReadArtificialState, Blackhole)}.
 * See benchmark description on the method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ArtificialPageReadState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ArtificialFullDatabaseState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {
	/**
	 * Pseudo-randomizer for picking random entities to fetch.
	 */
	private final Random random = new Random(SEED);
	/**
	 * Query prepared for the measured invocation.
	 */
	@Getter protected Query query;

	/**
	 * Prepares artificial product for the next operation that is measured in the benchmark.
	 */
	@Setup(Level.Invocation)
	public void prepareCall() {
		final Set<RequireConstraint> requirements = new HashSet<>();
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
			final Currency randomExistingCurrency = Arrays.stream(DataGenerator.CURRENCIES)
				.skip(random.nextInt(DataGenerator.CURRENCIES.length))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No currencies found!"));
			final String[] priceLists = Arrays.stream(DataGenerator.PRICE_LIST_NAMES)
				.filter(it -> random.nextBoolean())
				.toArray(String[]::new);

			priceConstraint = and(
				priceInCurrency(randomExistingCurrency),
				priceInPriceLists(priceLists),
				priceValidIn(ZonedDateTime.now())
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
						Stream.of(Entities.BRAND, Entities.CATEGORY, Entities.PRICE_LIST, Entities.STORE)
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

		requirements.add(
			page(random.nextInt(5) + 1, 20)
		);

		this.query = Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				and(
					primaryKey(
						Stream.iterate(
							random.nextInt(random.nextInt(PRODUCT_COUNT) + 1),
							aLong -> random.nextInt(PRODUCT_COUNT) + 1
						)
							.limit(100)
							.toArray(Integer[]::new)
					),
					language(randomExistingLocale),
					priceConstraint
				)
			),
			require(
				requirements.toArray(new RequireConstraint[0])
			)
		);
	}

}
