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
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.require.PriceFetchMode;
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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static io.evitadb.api.query.QueryConstraints.attributes;
import static io.evitadb.api.query.QueryConstraints.prices;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test verifies whether entities can be filtered by prices.
 *
 * TOBEDONE JNO - create multiple functional tests - one run with enabled SelectionFormula, one with disabled
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita entity filtering by prices functionality - sum")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class SumPriceEntityByPriceFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> extends EntityByPriceFilteringFunctionalTest<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION, EVITA> {
	private static final String HUNDRED_PRODUCTS_WITH_SUM_PRICES = "HundredProductsWithSumPrices";

	private static final int SEED = 40;
	private final DataGenerator dataGenerator = new DataGenerator(faker -> PriceInnerRecordHandling.SUM);

	@DataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
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
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceList(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceList(evita, originalProductEntities);
	}

	@DisplayName("Should return products with prices including non sellable ones")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsIncludingNonSellablePrice(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsIncludingNonSellablePrice(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and certain currency and returning all prices")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListReturningAllPrices(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListReturningAllPrices(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in different price list and certain currency")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndDifferentPriceLists(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndDifferentPriceLists(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMoment(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMoment(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (with VAT)")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInInterval(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInInterval(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (without VAT)")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVAT(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVAT(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and certain currency ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListOrderByPriceAscending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and certain currency ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListOrderByPriceDescending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency within interval (with VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithVATOrderByPriceAscending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency within interval (without VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithoutVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithoutVATOrderByPriceAscending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency within interval (with VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithVATOrderByPriceDescending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency within interval (without VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithoutVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListInIntervalWithoutVATOrderByPriceDescending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (with VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithVATOrderByPriceAscending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (without VAT) ordered by price asc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVATOrderByPriceAscending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVATOrderByPriceAscending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (with VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithVATOrderByPriceDescending(evita, originalProductEntities);
	}

	@DisplayName("Should return products with price in price list and currency in specific moment within interval (without VAT) ordered by price desc")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVATOrderByPriceDescending(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndPriceListAtCertainMomentInIntervalWithoutVATOrderByPriceDescending(evita, originalProductEntities);
	}

	@DisplayName("Should return products having price in certain currency and any price list")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	@Override
	void shouldReturnProductsHavingPriceInCurrency(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrency(evita, originalProductEntities);
	}

	@DisplayName("Should return products having price in certain price list and any currency")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	@Override
	void shouldReturnProductsHavingPriceInPriceList(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInPriceList(evita, originalProductEntities);
	}

	@DisplayName("Should return products having price in any price list and any currency valid in certain moment")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	@Override
	void shouldReturnProductsHavingPriceValidIn(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceValidIn(evita, originalProductEntities);
	}

	@DisplayName("Should return products having price in currency valid in certain moment")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@DisabledIfSystemProperty(named = "implementation", matches = "es|sql")
	@Override
	void shouldReturnProductsHavingPriceInCurrencyAndValidIn(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnProductsHavingPriceInCurrencyAndValidIn(evita, originalProductEntities);
	}

	@DisplayName("Should return price histogram for returned products")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnPriceHistogram(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnPriceHistogram(evita, originalProductEntities);
	}

	@DisplayName("Should return price histogram for returned products excluding price between constraint")
	@UseDataSet(HUNDRED_PRODUCTS_WITH_SUM_PRICES)
	@Test
	@Override
	void shouldReturnPriceHistogramWithoutBeingAffectedByPriceFilter(EVITA evita, List<SealedEntity> originalProductEntities) {
		super.shouldReturnPriceHistogramWithoutBeingAffectedByPriceFilter(evita, originalProductEntities);
	}

	@Override
	protected void assertSellingPricesAreAsExpected(@Nonnull List<SealedEntity> resultToVerify, @Nonnull PriceFetchMode priceFetchMode, @Nonnull Currency currency, @Nullable ZonedDateTime validIn, @Nonnull Serializable[] priceLists) {
		final Set<Serializable> priceListsSet = Arrays.stream(priceLists).collect(Collectors.toSet());
		for (SealedEntity sealedEntity : resultToVerify) {
			final PriceContract sellingPrice = sealedEntity.getSellingPrice();
			assertNotNull(sellingPrice);
			checkReturnedPrices(priceFetchMode, currency, validIn, priceListsSet, sealedEntity);
		}
	}
}
