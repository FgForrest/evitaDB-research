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

package io.evitadb.api;

import com.github.javafaker.Faker;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.dataType.PaginatedList;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.io.extraResult.HistogramContract.Bucket;
import io.evitadb.api.io.extraResult.PriceHistogram;
import io.evitadb.api.query.require.Page;
import io.evitadb.api.query.require.PriceFetchMode;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.test.Entities;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.generator.DataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static io.evitadb.test.generator.DataGenerator.*;
import static io.evitadb.utils.AssertionUtils.assertSortedResultEquals;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.summingInt;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies whether entities can be filtered by prices.
 *
 * TOBEDONE JNO - create multiple functional tests - one run with enabled SelectionFormula, one with disabled
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita entity filtering by prices functionality")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class EntityByPriceFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	private static final String HUNDRED_PRODUCTS_WITH_PRICES = "HundredProductsWithPrices";

	private static final int SEED = 40;
	private final DataGenerator dataGenerator = new DataGenerator();

	@DataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	List<SealedEntity> setUp(EVITA evita) {
		return evita.updateCatalog(TEST_CATALOG, session -> {
			final BiFunction<Serializable, Faker, Integer> randomEntityPicker = (entityType, faker) -> null;

			final List<EntityReferenceContract> storedProducts = dataGenerator.generateEntities(
					dataGenerator.getSampleProductSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(100)
				.map(session::upsertEntity)
				.collect(Collectors.toList());
			session.catalog.flush();
			return storedProducts.stream()
				.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), attributes(), prices()))
				.collect(Collectors.toList());
		});
	}

	@DisplayName("Should return products with price in price list and certain currency")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceList(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_VIP) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_BASIC),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_CZK,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);
				assertResultContainOnlyPricesFrom(
					result.getRecordData(),
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with prices including non sellable ones")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsIncludingNonSellablePrice(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_BASIC, PRICE_LIST_REFERENCE)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_BASIC) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_REFERENCE),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_CZK,
					null,
					PRICE_LIST_BASIC, PRICE_LIST_REFERENCE
				);
				assertResultContainProductWithNonSellablePriceFrom(
					result.getRecordData(),
					PRICE_LIST_REFERENCE
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and certain currency and returning all prices")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListReturningAllPrices(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							allPrices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_VIP) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_BASIC),
					result.getRecordData(),
					PriceFetchMode.ALL,
					CURRENCY_CZK,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);
				final Set<Serializable> priceListsReturned = result.getRecordData()
					.stream()
					.flatMap(it -> it.getPrices().stream())
					.map(PriceContract::getPriceList)
					.collect(Collectors.toSet());
				assertTrue(priceListsReturned.size() > 2);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in different price list and certain currency")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndDifferentPriceLists(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_B2B, PRICE_LIST_SELLOUT, PRICE_LIST_BASIC)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_B2B) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_SELLOUT) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_BASIC),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_CZK,
					null,
					PRICE_LIST_B2B, PRICE_LIST_SELLOUT, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMoment(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_VIP, theMoment) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_CZK, PRICE_LIST_BASIC, theMoment),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_CZK,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (with VAT)")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInInterval(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		final BigDecimal from = new BigDecimal("80");
		final BigDecimal to = new BigDecimal("150");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment),
								priceBetween(from, to)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_CZK, theMoment, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_CZK,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (without VAT)")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVAT(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		final BigDecimal from = new BigDecimal("80");
		final BigDecimal to = new BigDecimal("150");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment),
								priceBetween(from, to)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices(),
							useOfPrice(QueryPriceMode.WITHOUT_VAT)
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITHOUT_VAT, CURRENCY_EUR, theMoment, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and certain currency ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC)
							)
						),
						require(
							page(1, 10),
							prices()
						),
						orderBy(
							priceAscending()
						)
					),
					SealedEntity.class
				);
				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_VIP) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithVat),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and certain currency ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC)
							)
						),
						require(
							page(1, 10),
							prices()
						),
						orderBy(
							priceDescending()
						)
					),
					SealedEntity.class
				);
				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_VIP) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithVat).reversed(),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency within interval (with VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceBetween(from, to)
							)
						),
						require(
							page(1, 10),
							prices()
						),
						orderBy(
							priceAscending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithVat),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency within interval (without VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithoutVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceBetween(from, to)
							)
						),
						require(
							page(1, 10),
							prices(),
							useOfPrice(QueryPriceMode.WITHOUT_VAT)
						),
						orderBy(
							priceAscending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITHOUT_VAT, CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithoutVat),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency within interval (with VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceBetween(from, to)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						),
						orderBy(
							priceDescending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithVat).reversed(),
					page(1, Integer.MAX_VALUE),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency within interval (without VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithoutVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceBetween(from, to)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices(),
							useOfPrice(QueryPriceMode.WITHOUT_VAT)
						),
						orderBy(
							priceDescending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITHOUT_VAT, CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithoutVat).reversed(),
					page(1, Integer.MAX_VALUE),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (with VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment),
								priceBetween(from, to)
							)
						),
						require(
							page(1, 10),
							prices()
						),
						orderBy(
							priceAscending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_EUR, theMoment, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithVat),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (without VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment),
								priceBetween(from, to)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices(),
							useOfPrice(QueryPriceMode.WITHOUT_VAT)
						),
						orderBy(
							priceAscending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITHOUT_VAT, CURRENCY_EUR, theMoment, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithoutVat),
					page(1, Integer.MAX_VALUE),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (with VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment),
								priceBetween(from, to)
							)
						),
						require(
							page(1, 10),
							prices()
						),
						orderBy(
							priceDescending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_EUR, theMoment, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithVat).reversed(),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (without VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		final BigDecimal from = new BigDecimal("30");
		final BigDecimal to = new BigDecimal("60");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								priceValidIn(theMoment),
								priceBetween(from, to)
							)
						),
						require(
							page(1, 10),
							prices(),
							useOfPrice(QueryPriceMode.WITHOUT_VAT)
						),
						orderBy(
							priceDescending()
						)
					),
					SealedEntity.class
				);

				assertSortedResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITHOUT_VAT, CURRENCY_EUR, theMoment, PRICE_LIST_VIP, PRICE_LIST_BASIC),
					result.getRecordData(),
					Comparator.comparing(PriceContract::getPriceWithoutVat).reversed(),
					page(1, 10),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					theMoment,
					PRICE_LIST_VIP, PRICE_LIST_BASIC
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products having price in certain currency and any price list")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	void shouldReturnProductsHavingPriceInCurrency(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							priceInCurrency(CURRENCY_EUR)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_EUR),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products having price in certain price list and any currency")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	void shouldReturnProductsHavingPriceInPriceList(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							priceInPriceLists(PRICE_LIST_SELLOUT)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, PRICE_LIST_SELLOUT),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products having price in any price list and any currency valid in certain moment")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	void shouldReturnProductsHavingPriceValidIn(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							priceValidIn(theMoment)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, theMoment),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					null,
					null
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products having price in currency valid in certain moment")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	void shouldReturnProductsHavingPriceInCurrencyAndValidIn(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2010, 5, 5, 0, 0, 0, 0, ZoneId.systemDefault());
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceValidIn(theMoment),
								priceInCurrency(CURRENCY_EUR)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							prices()
						)
					),
					SealedEntity.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_EUR, theMoment),
					result.getRecordData(),
					PriceFetchMode.RESPECTING_FILTER,
					CURRENCY_EUR,
					null
				);

				return null;
			}
		);
	}

	@DisplayName("Should return price histogram for returned products")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnPriceHistogram(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							entityBody(),
							priceHistogram(20)
						)
					),
					SealedEntity.class
				);

				final List<SealedEntity> filteredProducts = originalProductEntities
					.stream()
					.filter(sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_VIP) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_BASIC))
					.collect(Collectors.toList());

				assertHistogramIntegrity(result, filteredProducts);

				return null;
			}
		);
	}

	@DisplayName("Should return price histogram for returned products excluding price between constraint")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_PRICES)
	@Test
	void shouldReturnPriceHistogramWithoutBeingAffectedByPriceFilter(EVITA evita, List<SealedEntity> originalProductEntities) {
		final BigDecimal from = new BigDecimal("80");
		final BigDecimal to = new BigDecimal("150");
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_VIP, PRICE_LIST_BASIC),
								userFilter(
									priceBetween(from, to)
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							entityBody(),
							priceHistogram(20)
						)
					),
					SealedEntity.class
				);

				final List<SealedEntity> filteredProducts = originalProductEntities
					.stream()
					.filter(sealedEntity -> hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_VIP) ||
						hasAnySellablePrice(sealedEntity, CURRENCY_EUR, PRICE_LIST_BASIC))
					.collect(Collectors.toList());

				// verify our test works
				final Predicate<SealedEntity> sellingPriceBetweenPredicate = it -> {
					final BigDecimal price = it.getSellingPrice(CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC).getPriceWithVat();
					return price.compareTo(from) >= 0 && price.compareTo(to) <= 0;
				};
				assertTrue(
					filteredProducts.size() > filteredProducts.stream().filter(sellingPriceBetweenPredicate).count(),
					"Price between constraint didn't filter out any products. Test is not testing anything!"
				);

				// the price between constraint must be ignored while computing price histogram
				assertHistogramIntegrity(result, filteredProducts);

				return null;
			}
		);
	}

	/*
		ASSERTIONS
	 */

	void assertSellingPricesAreAsExpected(@Nonnull List<SealedEntity> resultToVerify, @Nonnull PriceFetchMode priceFetchMode, @Nonnull Currency currency, @Nullable ZonedDateTime validIn, @Nonnull Serializable[] priceLists) {
		final Set<Serializable> priceListsSet = Arrays.stream(priceLists).collect(Collectors.toSet());

		for (SealedEntity sealedEntity : resultToVerify) {
			final PriceContract sellingPrice = sealedEntity.getSellingPrice();
			assertNotNull(sellingPrice);
			for (Serializable priceList : priceLists) {
				if (priceList.equals(sellingPrice.getPriceList())) {
					break;
				} else {
					assertTrue(
						sealedEntity.getPrices(currency, priceList)
							.stream()
							.filter(PriceContract::isSellable)
							// for first occurrence strategy the price with more prioritized list might be found but is skipped, because is bigger than other inner record price
							.filter(it -> Objects.equals(it.getInnerRecordId(), sellingPrice.getInnerRecordId()) || it.getPriceWithVat().compareTo(sellingPrice.getPriceWithVat()) <= 0)
							.noneMatch(it -> it.getValidity() == null || validIn == null || it.getValidity().isValidFor(validIn)),
						"There must be no price for more prioritized price lists! But is for: " + priceList
					);
				}
			}
			checkReturnedPrices(priceFetchMode, currency, validIn, priceListsSet, sealedEntity);
		}
	}

	/**
	 * Method checks whether the returned prices conform to the requested fetch mode.
	 */
	void checkReturnedPrices(@Nonnull PriceFetchMode priceFetchMode, @Nonnull Currency currency, ZonedDateTime validIn, Set<Serializable> priceListsSet, SealedEntity sealedEntity) {
		if (priceFetchMode == PriceFetchMode.NONE) {
			// no prices should be returned at all
			assertTrue(sealedEntity.getPrices().isEmpty());
		} else if (priceFetchMode == PriceFetchMode.RESPECTING_FILTER) {
			// only prices that match input filter can be returned
			assertTrue(
				sealedEntity
					.getPrices()
					.stream()
					.allMatch(
						price -> Objects.equals(price.getCurrency(), currency) &&
							ofNullable(price.getValidity()).map(it -> validIn == null || it.isValidFor(validIn)).orElse(true) &&
							priceListsSet.contains(price.getPriceList())
					)
			);
		} else {
			// all - also not matching prices can be returned
			assertFalse(sealedEntity.getPrices().isEmpty());
		}
	}

	/**
	 * Verifies histogram integrity against source entities.
	 */
	private static void assertHistogramIntegrity(EvitaResponseBase<SealedEntity> result, List<SealedEntity> filteredProducts) {
		final PriceHistogram priceHistogram = result.getAdditionalResults(PriceHistogram.class);
		assertNotNull(priceHistogram);
		assertTrue(priceHistogram.getBuckets().length <= 20);

		assertEquals(filteredProducts.size(), priceHistogram.getOverallCount());
		final List<BigDecimal> sellingPrices = filteredProducts
			.stream()
			.map(it -> it.getSellingPrice(CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC))
			.filter(Objects::nonNull)
			.map(PriceContract::getPriceWithVat)
			.collect(Collectors.toList());

		assertEquals(sellingPrices.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO), priceHistogram.getMin());
		assertEquals(sellingPrices.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO), priceHistogram.getMax());

		// verify bucket occurrences
		final Map<Integer, Integer> expectedOccurrences = filteredProducts
			.stream()
			.collect(
				Collectors.groupingBy(
					it -> findIndexInHistogram(it, priceHistogram),
					summingInt(entity -> 1)
				)
			);

		final Bucket[] buckets = priceHistogram.getBuckets();
		for (int i = 0; i < buckets.length; i++) {
			final Bucket bucket = priceHistogram.getBuckets()[i];
			assertEquals(
				ofNullable(expectedOccurrences.get(i)).orElse(0),
				bucket.getOccurrences()
			);
		}
	}

	/**
	 * Finds appropriate index in the histogram according to histogram thresholds.
	 */
	private static int findIndexInHistogram(SealedEntity entity, HistogramContract histogram) {
		final BigDecimal entityPrice = entity.getSellingPrice(CURRENCY_EUR, null, PRICE_LIST_VIP, PRICE_LIST_BASIC).getPriceWithVat();
		final Bucket[] buckets = histogram.getBuckets();
		for (int i = buckets.length - 1; i >= 0; i--) {
			final Bucket bucket = buckets[i];
			final int priceCompared = entityPrice.compareTo(bucket.getThreshold());
			if (priceCompared >= 0) {
				return i;
			}
		}
		fail("Histogram span doesn't match current entity price: " + entityPrice);
		return -1;
	}

	/**
	 * Returns true if there is any indexed price for passed currency.
	 */
	private static boolean hasAnySellablePrice(@Nonnull SealedEntity entity, @Nonnull Currency currency) {
		return entity.getPrices(currency).stream().anyMatch(PriceContract::isSellable);
	}

	/**
	 * Returns true if there is any indexed price for passed price list.
	 */
	private static boolean hasAnySellablePrice(@Nonnull SealedEntity entity, @Nonnull String priceList) {
		return entity.getPrices(priceList).stream().anyMatch(PriceContract::isSellable);
	}

	/**
	 * Returns true if there is any indexed price for passed currency and price list.
	 */
	private static boolean hasAnySellablePrice(@Nonnull SealedEntity entity, @Nonnull ZonedDateTime atTheMoment) {
		return entity.getPrices().stream().filter(PriceContract::isSellable).anyMatch(it -> it.getValidity() == null || it.getValidity().isValidFor(atTheMoment));
	}

	/**
	 * Returns true if there is any indexed price for passed currency and price list.
	 */
	private static boolean hasAnySellablePrice(@Nonnull SealedEntity entity, @Nonnull Currency currency, @Nonnull ZonedDateTime atTheMoment) {
		return entity.getPrices(currency).stream().filter(PriceContract::isSellable).anyMatch(it -> it.getValidity() == null || it.getValidity().isValidFor(atTheMoment));
	}

	/**
	 * Returns true if there is any indexed price for passed currency and price list.
	 */
	private static boolean hasAnySellablePrice(@Nonnull SealedEntity entity, @Nonnull Currency currency, @Nonnull String priceList) {
		return entity.getPrices(currency, priceList).stream().anyMatch(PriceContract::isSellable);
	}

	/**
	 * Returns true if there is any indexed price for passed currency and price list.
	 */
	private static boolean hasAnySellablePrice(@Nonnull SealedEntity entity, @Nonnull Currency currency, @Nonnull String priceList, @Nonnull ZonedDateTime atTheMoment) {
		return entity.getPrices(currency, priceList).stream().filter(PriceContract::isSellable).anyMatch(it -> it.isValid(atTheMoment));
	}

	/**
	 * Verifies that `originalEntities` filtered by `predicate` match exactly contents of the `resultToVerify`.
	 */
	private void assertResultIs(List<SealedEntity> originalEntities, Predicate<SealedEntity> predicate, List<SealedEntity> resultToVerify, PriceFetchMode priceFetchMode, Currency currency, ZonedDateTime validIn, Serializable... priceLists) {
		@SuppressWarnings("ConstantConditions") final int[] expectedResult = originalEntities.stream().filter(predicate).mapToInt(EntityContract::getPrimaryKey).toArray();
		assertFalse(ArrayUtils.isEmpty(expectedResult), "Expected result should never be empty - this would cause false positive tests!");
		assertResultEquals(
			resultToVerify,
			expectedResult
		);

		if (priceLists.length > 0) {
			assertSellingPricesAreAsExpected(resultToVerify, priceFetchMode, currency, validIn, priceLists);
		}
	}

	/**
	 * Verifies that `record` primary keys exactly match passed `reference` ids. Both lists are sorted naturally before
	 * the comparison is executed.
	 */
	private static void assertResultEquals(@Nonnull List<SealedEntity> records, @Nonnull int... reference) {
		final List<Integer> recordsCopy = records.stream().map(SealedEntity::getPrimaryKey).sorted().collect(Collectors.toList());
		Arrays.sort(reference);

		assertSortedResultEquals(recordsCopy, reference);
	}

	/**
	 * Verifies that result contains only prices in specified price lists.
	 */
	private static void assertResultContainOnlyPricesFrom(@Nonnull List<SealedEntity> recordData, @Nonnull Serializable... priceLists) {
		final Set<Serializable> allowedPriceLists = Set.of(priceLists);
		for (SealedEntity entity : recordData) {
			assertTrue(
				entity.getPrices().stream().allMatch(price -> allowedPriceLists.contains(price.getPriceList()))
			);
		}
	}

	/**
	 * Verifies that result contains at least one product with non-sellable price from passed price list.
	 */
	private static void assertResultContainProductWithNonSellablePriceFrom(@Nonnull List<SealedEntity> recordData, @Nonnull Serializable... priceLists) {
		final Set<Serializable> allowedPriceLists = Set.of(priceLists);
		for (SealedEntity entity : recordData) {
			if (entity.getPrices().stream().anyMatch(price -> allowedPriceLists.contains(price.getPriceList()) && !price.isSellable())) {
				return;
			}
		}
		fail("There is product that contains price from price lists: " + Arrays.stream(priceLists).map(Object::toString).collect(Collectors.joining(", ")));
	}

	/**
	 * Verifies that `originalEntities` filtered by `predicate` match exactly contents of the `resultToVerify`.
	 */
	private void assertSortedResultIs(@Nonnull List<SealedEntity> originalEntities, @Nonnull Predicate<SealedEntity> predicate, @Nonnull List<SealedEntity> resultToVerify, @Nonnull Comparator<PriceContract> priceComparator, @Nonnull Page page, @Nonnull PriceFetchMode priceFetchMode, @Nonnull Currency currency, @Nullable ZonedDateTime validIn, @Nonnull Serializable... priceLists) {
		@SuppressWarnings("ConstantConditions") final int[] expectedResult = originalEntities
			.stream()
			.filter(predicate)
			// consider only entities that has valid selling price
			.filter(it -> it.getSellingPrice(currency, validIn, priceLists) != null)
			.sorted((o1, o2) -> priceComparator.compare(o1.getSellingPrice(currency, validIn, priceLists), o2.getSellingPrice(currency, validIn, priceLists)))
			.mapToInt(EntityContract::getPrimaryKey)
			.skip(PaginatedList.getFirstItemNumberForPage(page.getPageNumber(), page.getPageSize()))
			.limit(page.getPageSize())
			.toArray();

		assertFalse(ArrayUtils.isEmpty(expectedResult), "Expected result should never be empty - this would cause false positive tests!");
		final List<Integer> recordsCopy = resultToVerify
			.stream()
			.map(SealedEntity::getPrimaryKey)
			.collect(Collectors.toList());

		assertSortedResultEquals(
			recordsCopy,
			expectedResult
		);

		assertSellingPricesAreAsExpected(resultToVerify, priceFetchMode, currency, validIn, priceLists);
	}

}
