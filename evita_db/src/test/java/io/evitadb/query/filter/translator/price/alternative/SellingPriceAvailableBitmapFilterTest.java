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

package io.evitadb.query.filter.translator.price.alternative;

import io.evitadb.api.EntityCollection;
import io.evitadb.api.EvitaSession;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityStorageContainerAccessor;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.cache.NoCacheSupervisor;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.filter.translator.price.PriceBetweenTranslator;
import io.evitadb.query.response.QueryTelemetry;
import io.evitadb.query.response.QueryTelemetry.QueryPhase;
import io.evitadb.test.Entities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Currency;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Optional.ofNullable;

/**
 * This test verifies behaviour of {@link SellingPriceAvailableBitmapFilter}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
class SellingPriceAvailableBitmapFilterTest {
	private static final String PRICE_LIST_BASIC = "basic";
	private static final String PRICE_LIST_VIP = "vip";
	private static final String PRICE_LIST_REFERENCE = "reference";
	private static final Currency CZK = Currency.getInstance("CZK");
	private static final Currency EUR = Currency.getInstance("EUR");
	private static final DateTimeRange FAR_FUTURE = DateTimeRange.since(
		ZonedDateTime.now().plusYears(100)
	);
	private Map<Integer, Entity> entities;

	@BeforeEach
	void setUp() {
		entities = Stream.of(
				new InitialEntityBuilder(Entities.PRODUCT, 1)
					.setPrice(1, PRICE_LIST_REFERENCE, CZK, new BigDecimal("80"), new BigDecimal("21"), new BigDecimal("101"), false)
					.setPrice(2, PRICE_LIST_BASIC, CZK, new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("121"), true)
					.toInstance(),
				new InitialEntityBuilder(Entities.PRODUCT, 2)
					.setPrice(10, PRICE_LIST_REFERENCE, EUR, new BigDecimal("80"), new BigDecimal("21"), new BigDecimal("101"), false)
					.setPrice(11, PRICE_LIST_BASIC, EUR, new BigDecimal("200"), new BigDecimal("21"), new BigDecimal("241"), true)
					.toInstance(),
				new InitialEntityBuilder(Entities.PRODUCT, 3)
					.setPrice(20, PRICE_LIST_REFERENCE, CZK, new BigDecimal("80"), new BigDecimal("21"), new BigDecimal("101"), false)
					.setPrice(21, PRICE_LIST_VIP, CZK, new BigDecimal("100"), new BigDecimal("21"), new BigDecimal("121"), true)
					.toInstance(),
				new InitialEntityBuilder(Entities.PRODUCT, 4)
					.setPrice(30, PRICE_LIST_REFERENCE, CZK, new BigDecimal("80"), new BigDecimal("21"), new BigDecimal("101"), false)
					.setPrice(31, PRICE_LIST_BASIC, CZK, new BigDecimal("300"), new BigDecimal("21"), new BigDecimal("361"), true)
					.setPrice(32, PRICE_LIST_VIP, CZK, new BigDecimal("320"), new BigDecimal("21"), new BigDecimal("361"), true)
					.toInstance(),
				new InitialEntityBuilder(Entities.PRODUCT, 5)
					.setPrice(40, PRICE_LIST_REFERENCE, CZK, new BigDecimal("80"), new BigDecimal("21"), new BigDecimal("101"), FAR_FUTURE, false)
					.setPrice(41, PRICE_LIST_BASIC, CZK, new BigDecimal("300"), new BigDecimal("21"), new BigDecimal("361"), FAR_FUTURE, true)
					.setPrice(42, PRICE_LIST_VIP, CZK, new BigDecimal("320"), new BigDecimal("21"), new BigDecimal("361"), FAR_FUTURE, true)
					.toInstance()
			)
			.collect(
				Collectors.toMap(Entity::getPrimaryKey, Function.identity())
			);
	}

	@Test
	void shouldFilterEntitiesByCurrencyAndPriceList() {
		final SellingPriceAvailableBitmapFilter filter = new SellingPriceAvailableBitmapFilter(2);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				and(
					priceInCurrency(CZK),
					priceInPriceLists(PRICE_LIST_BASIC, PRICE_LIST_REFERENCE)
				)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		Assertions.assertArrayEquals(
			new int[]{1, 4, 5},
			result.getArray()
		);
	}

	@Test
	void shouldFilterEntitiesByCurrencyAndPriceListWithDifferentPriceLists() {
		final SellingPriceAvailableBitmapFilter filter = new SellingPriceAvailableBitmapFilter(2);
		final QueryContext queryContext = getQueryContext(
			Query.query(
				entities(Entities.PRODUCT),
				filterBy(
					and(
						priceInCurrency(CZK),
						priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_REFERENCE)
					)
				)
			)
		);

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		Assertions.assertArrayEquals(
			new int[]{3, 4, 5},
			result.getArray()
		);
	}

	@Test
	void shouldFilterEntitiesByCurrencyAndPriceListAndPriceFilter() {
		final SellingPriceAvailableBitmapFilter filter = new SellingPriceAvailableBitmapFilter(
			0, PriceBetweenTranslator.createPredicate(90, 130, QueryPriceMode.WITH_VAT, 0)
		);
		final QueryContext queryContext = getQueryContext(
			Query.query(
				entities(Entities.PRODUCT),
				filterBy(
					and(
						priceInCurrency(CZK),
						priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC, PRICE_LIST_REFERENCE),
						priceBetween(new BigDecimal("90"), new BigDecimal("130"))
					)
				)
			)
		);

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		Assertions.assertArrayEquals(
			new int[]{1, 3},
			result.getArray()
		);
	}

	@Test
	void shouldFilterEntitiesByCurrencyAndPriceListAndPriceFilterBasicFirst() {
		final SellingPriceAvailableBitmapFilter filter = new SellingPriceAvailableBitmapFilter(
			0, PriceBetweenTranslator.createPredicate(90, 130, QueryPriceMode.WITH_VAT, 0)
		);
		final QueryContext queryContext = getQueryContext(
			Query.query(
				entities(Entities.PRODUCT),
				filterBy(
					and(
						priceInCurrency(CZK),
						priceInPriceLists(PRICE_LIST_BASIC, PRICE_LIST_VIP, PRICE_LIST_REFERENCE),
						priceBetween(new BigDecimal("90"), new BigDecimal("130"))
					)
				)
			)
		);

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		Assertions.assertArrayEquals(
			new int[]{1, 3},
			result.getArray()
		);
	}

	@Test
	void shouldFilterEntitiesByCurrencyAndPriceListAndValidity() {
		final SellingPriceAvailableBitmapFilter filter = new SellingPriceAvailableBitmapFilter(0);
		final QueryContext queryContext = getQueryContext(
			Query.query(
				entities(Entities.PRODUCT),
				filterBy(
					and(
						priceInCurrency(CZK),
						priceInPriceLists(PRICE_LIST_BASIC, PRICE_LIST_VIP, PRICE_LIST_REFERENCE),
						priceValidIn(ZonedDateTime.now())
					)
				)
			)
		);

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		Assertions.assertArrayEquals(
			new int[]{1, 3, 4},
			result.getArray()
		);
	}

	@Test
	void shouldFilterEntitiesByCurrencyAndPriceListAndValidityInFarFuture() {
		final SellingPriceAvailableBitmapFilter filter = new SellingPriceAvailableBitmapFilter(0);
		final QueryContext queryContext = getQueryContext(
			Query.query(
				entities(Entities.PRODUCT),
				filterBy(
					and(
						priceInCurrency(CZK),
						priceInPriceLists(PRICE_LIST_BASIC, PRICE_LIST_VIP, PRICE_LIST_REFERENCE),
						priceValidIn(ZonedDateTime.now().plusYears(101))
					)
				)
			)
		);

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		Assertions.assertArrayEquals(
			new int[]{1, 3, 4, 5},
			result.getArray()
		);
	}

	@Nonnull
	private QueryContext getQueryContext(@Nonnull Query query) {
		final EntityCollection entityCollection = Mockito.mock(EntityCollection.class);
		final QueryContext queryContext = new QueryContext(
			entityCollection,
			Mockito.mock(EntityStorageContainerAccessor.class),
			Mockito.mock(EvitaSession.class),
			new EvitaRequest(
				query,
				ZonedDateTime.now()
			),
			new QueryTelemetry(QueryPhase.EXECUTION),
			serializable -> null,
			Collections.emptyMap(),
			NoCacheSupervisor.INSTANCE
		);

		Mockito.when(entityCollection.getSchema()).thenReturn(new EntitySchema(Entities.PRODUCT));
		Mockito.when(entityCollection.getEntity(ArgumentMatchers.anyInt(), ArgumentMatchers.any(EvitaRequest.class)))
			.thenAnswer(invocationOnMock -> {
				final Entity entity = this.entities.get(invocationOnMock.getArgument(0, Integer.class));
				final EvitaRequest evitaRequest = invocationOnMock.getArgument(1, EvitaRequest.class);
				return ofNullable(entity)
					.filter(Droppable::exists)
					.map(it -> Entity.decorate(
						entity,
						entity.getSchema(),
						new HierarchicalContractSerializablePredicate(),
						new AttributeValueSerializablePredicate(evitaRequest),
						new AssociatedDataValueSerializablePredicate(evitaRequest),
						new ReferenceContractSerializablePredicate(evitaRequest),
						new PriceContractSerializablePredicate(evitaRequest)
					))
					.orElse(null);
			});
		return queryContext;
	}
}