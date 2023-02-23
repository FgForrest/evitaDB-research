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
import io.evitadb.api.EntityByAttributeFilteringFunctionalTest.Market;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.test.Entities;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.extension.DataCarrier;
import io.evitadb.test.generator.DataGenerator;
import io.evitadb.utils.AssertionUtils;
import one.edee.oss.pmptt.model.Hierarchy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static io.evitadb.test.generator.DataGenerator.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class CombinedEntityFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	private static final String THREE_HUNDRED_PRODUCTS_WITH_ALL_DATA = "HundredProductsWithAllData";
	private static final String ATTRIBUTE_SIZE = "size";
	private static final String ATTRIBUTE_CREATED = "created";
	private static final String ATTRIBUTE_MANUFACTURED = "manufactured";
	private static final String ATTRIBUTE_COMBINED_PRIORITY = "combinedPriority";
	private static final String ATTRIBUTE_TARGET_MARKET = "targetMarket";
	private static final String ATTRIBUTE_LOCATED_AT = "locatedAt";
	private static final String ATTRIBUTE_MARKET_SHARE = "marketShare";
	private static final String ATTRIBUTE_FOUNDED = "founded";
	private static final String ATTRIBUTE_CAPACITY = "capacity";

	private static final int SEED = 40;
	private final DataGenerator dataGenerator = new DataGenerator(faker -> {
		final int rndPIRH = faker.random().nextInt(10);
		if (rndPIRH < 6) {
			return PriceInnerRecordHandling.NONE;
		} else if (rndPIRH < 8) {
			return PriceInnerRecordHandling.FIRST_OCCURRENCE;
		} else {
			return PriceInnerRecordHandling.SUM;
		}
	});

	@DataSet(THREE_HUNDRED_PRODUCTS_WITH_ALL_DATA)
	DataCarrier setUp(EVITA evita) {
		return evita.updateCatalog(TEST_CATALOG, session -> {
			final BiFunction<Serializable, Faker, Integer> randomEntityPicker = (entityType, faker) -> {
				final int entityCount = session.getEntityCollectionSize(entityType);
				final int primaryKey = entityCount == 0 ? 0 : faker.random().nextInt(1, entityCount);
				return primaryKey == 0 ? null : primaryKey;
			};

			dataGenerator.generateEntities(
					dataGenerator.getSampleBrandSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(5)
				.forEach(session::upsertEntity);

			dataGenerator.generateEntities(
					dataGenerator.getSampleCategorySchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(10)
				.forEach(session::upsertEntity);

			dataGenerator.generateEntities(
					dataGenerator.getSamplePriceListSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(4)
				.forEach(session::upsertEntity);

			dataGenerator.generateEntities(
					dataGenerator.getSampleStoreSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(12)
				.forEach(session::upsertEntity);

			final List<EntityReferenceContract> storedProducts = dataGenerator.generateEntities(
					dataGenerator.getSampleProductSchema(
						session,
						schemaBuilder -> schemaBuilder
							.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique().sortable())
							.withAttribute(ATTRIBUTE_QUANTITY, BigDecimal.class, whichIs -> whichIs.filterable().sortable().indexDecimalPlaces(2))
							.withAttribute(ATTRIBUTE_PRIORITY, Long.class, whichIs -> whichIs.sortable().filterable())
							.withAttribute(ATTRIBUTE_SIZE, NumberRange[].class, whichIs -> whichIs.filterable())
							.withAttribute(ATTRIBUTE_CREATED, ZonedDateTime.class, whichIs -> whichIs.filterable().sortable())
							.withAttribute(ATTRIBUTE_MANUFACTURED, LocalDate.class, whichIs -> whichIs.filterable().sortable())
							.withAttribute(ATTRIBUTE_COMBINED_PRIORITY, Multiple.class, whichIs -> whichIs.filterable().sortable())
							.withAttribute(ATTRIBUTE_TARGET_MARKET, Market.class, whichIs -> whichIs.filterable())
							.withReferenceToEntity(Entities.BRAND,
								whichIs -> whichIs
									.withAttribute(ATTRIBUTE_LOCATED_AT, Market.class, thatIs -> thatIs.filterable())
									.withAttribute(ATTRIBUTE_MARKET_SHARE, BigDecimal.class, thatIs -> thatIs.filterable().sortable())
									.withAttribute(ATTRIBUTE_FOUNDED, ZonedDateTime.class, thatIs -> thatIs.filterable().sortable())
							)
							.withReferenceToEntity(Entities.STORE,
								whichIs -> whichIs
									.withAttribute(ATTRIBUTE_LOCATED_AT, Market.class, thatIs -> thatIs.filterable())
									.withAttribute(ATTRIBUTE_CAPACITY, Long.class, thatIs -> thatIs.filterable().sortable())
							)
					),
					randomEntityPicker,
					SEED
				)
				.limit(300)
				.map(session::upsertEntity)
				.collect(Collectors.toList());

			session.catalog.flush();
			return new DataCarrier(
				"originalProductEntities",
				storedProducts.stream()
					.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), fullEntity()))
					.collect(Collectors.toList()),
				"categoryHierarchy",
				dataGenerator.getHierarchy(Entities.CATEGORY)
			);
		});
	}

	@DisplayName("Should return products having price in currency and hierarchy location")
	@UseDataSet(THREE_HUNDRED_PRODUCTS_WITH_ALL_DATA)
	@Test
	void shouldReturnProductsHavingBothPriceAndHierarchyConstraints(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		final BigDecimal from = new BigDecimal("80");
		final BigDecimal to = new BigDecimal("150");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_BASIC),
								priceBetween(from, to),
								withinHierarchy(Entities.CATEGORY, 4)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);

				AssertionUtils.assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final boolean hasPrice = sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_CZK, null, PRICE_LIST_BASIC);
						final boolean isWithinCategory = sealedEntity
							.getReferences(Entities.CATEGORY)
							.stream()
							.anyMatch(category -> {
								final String categoryId = String.valueOf(category.getReferencedEntity().getPrimaryKey());
								// is either category 4
								return Objects.equals(categoryId, String.valueOf(4)) ||
									// or has parent category 4
									categoryHierarchy.getParentItems(categoryId)
										.stream()
										.anyMatch(it -> Objects.equals(it.getCode(), String.valueOf(4)));
							});
						return hasPrice && isWithinCategory;
					},
					result.getRecordData()
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products having price in currency and referenced entity")
	@UseDataSet(THREE_HUNDRED_PRODUCTS_WITH_ALL_DATA)
	@Test
	void shouldReturnProductsHavingBothPriceAndReferencedEntity(EVITA evita, List<SealedEntity> originalProductEntities) {
		final BigDecimal from = new BigDecimal("80");
		final BigDecimal to = new BigDecimal("150");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_BASIC),
								priceBetween(from, to),
								referenceHavingAttribute(Entities.BRAND, primaryKey(4))
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);

				AssertionUtils.assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final boolean hasPrice = sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_CZK, null, PRICE_LIST_BASIC);
						final boolean isReferencingBrand = sealedEntity.getReference(Entities.BRAND, 4) != null;
						return hasPrice && isReferencingBrand;
					},
					result.getRecordData()
				);

				return null;
			}
		);
	}

	@DisplayName("Should return products having price in currency and hierarchy location and referenced entity")
	@UseDataSet(THREE_HUNDRED_PRODUCTS_WITH_ALL_DATA)
	@Test
	void shouldReturnProductsHavingBothPriceAndHierarchyLocationAndReferencedEntity(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		final BigDecimal from = new BigDecimal("80");
		final BigDecimal to = new BigDecimal("150");

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								priceInCurrency(CURRENCY_CZK),
								priceInPriceLists(PRICE_LIST_BASIC),
								priceBetween(from, to),
								withinHierarchy(Entities.CATEGORY, 4),
								referenceHavingAttribute(Entities.BRAND, primaryKey(4))
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);

				AssertionUtils.assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final boolean hasPrice = sealedEntity.hasPriceInInterval(from, to, QueryPriceMode.WITH_VAT, CURRENCY_CZK, null, PRICE_LIST_BASIC);
						final boolean isReferencingBrand = sealedEntity.getReference(Entities.BRAND, 4) != null;
						final boolean isWithinCategory = sealedEntity
							.getReferences(Entities.CATEGORY)
							.stream()
							.anyMatch(category -> {
								final String categoryId = String.valueOf(category.getReferencedEntity().getPrimaryKey());
								// is either category 4
								return Objects.equals(categoryId, String.valueOf(4)) ||
									// or has parent category 4
									categoryHierarchy.getParentItems(categoryId)
										.stream()
										.anyMatch(it -> Objects.equals(it.getCode(), String.valueOf(4)));
							});
						return hasPrice && isReferencingBrand && isWithinCategory;
					},
					result.getRecordData()
				);

				return null;
			}
		);
	}

}
