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
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.io.extraResult.AttributeHistogram;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.io.extraResult.HistogramContract.Bucket;
import io.evitadb.test.Entities;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.generator.DataGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static io.evitadb.test.generator.DataGenerator.*;
import static io.evitadb.utils.AssertionUtils.*;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.summingInt;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies whether entities can be filtered by attributes.
 *
 * TOBEDONE JNO - create multiple functional tests - one run with enabled SelectionFormula, one with disabled
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita entity filtering by attributes functionality")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class EntityByAttributeFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	private static final String HUNDRED_PRODUCTS = "HundredProducts";
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
	private final DataGenerator dataGenerator = new DataGenerator();

	@DataSet(HUNDRED_PRODUCTS)
	List<SealedEntity> setUp(EVITA evita) {
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
				.limit(100)
				.map(session::upsertEntity)
				.collect(Collectors.toList());
			session.catalog.flush();
			return storedProducts.stream()
				.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), fullEntity()))
				.collect(Collectors.toList());
		});
	}

	@DisplayName("Should return single entity by equals to attribute (String)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToString(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_CODE, "Heavy-Duty-Marble-Bench-2-36")
						),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> "Heavy-Duty-Marble-Bench-2-36".equals(sealedEntity.getAttribute(ATTRIBUTE_CODE)),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by equals to attribute (Long)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToNumber(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_PRIORITY, 60797)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> Long.valueOf(60797L).equals(sealedEntity.getAttribute(ATTRIBUTE_PRIORITY)),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by equals to attribute (BigDecimal)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToBigDecimal(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_QUANTITY, new BigDecimal("313.21"))
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((BigDecimal) sealedEntity.getAttribute(ATTRIBUTE_QUANTITY))
						.map(it -> new BigDecimal("313.21").compareTo(it) == 0)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by equals to attribute (BigDecimal)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeEqualToBoolean(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_ALIAS, true)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable(sealedEntity.getAttribute(ATTRIBUTE_ALIAS))
						.map(Boolean.TRUE::equals)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by equals to attribute (ZonedDateTime)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToZonedDateTime(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final ZonedDateTime theMoment = ZonedDateTime.of(2003, 6, 18, 4, 14, 17, 0, ZoneId.systemDefault());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_CREATED, theMoment)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable(sealedEntity.getAttribute(ATTRIBUTE_CREATED))
						.map(theMoment::equals)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by equals to attribute (LocalDate)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToLocalDate(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final LocalDate theMoment = LocalDate.of(2011, 9, 15);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_MANUFACTURED, theMoment)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity ->
						ofNullable(sealedEntity.getAttribute(ATTRIBUTE_MANUFACTURED))
							.map(theMoment::equals)
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by equals to attribute (Enum)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeEqualToEnum(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_TARGET_MARKET, Market.ASIA)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity ->
						ofNullable(sealedEntity.getAttribute(ATTRIBUTE_TARGET_MARKET))
							.map(it -> Market.ASIA == it)
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by equals to attribute (NumberRange)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToNumberRange(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_SIZE, NumberRange.between(7, 95))
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity ->
						ofNullable((NumberRange[]) sealedEntity.getAttributeArray(ATTRIBUTE_SIZE))
							.map(it -> Arrays.stream(it).anyMatch(x -> NumberRange.between(7, 95).equals(x)))
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by equals to attribute (DateTimeRange)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeEqualToDateTimeRange(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final DateTimeRange theRange = DateTimeRange.between(LocalDateTime.of(2010, 1, 1, 0, 0), LocalDateTime.of(2012, 12, 31, 0, 0), ZoneId.systemDefault());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_VALIDITY, theRange)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity ->
						ofNullable(sealedEntity.getAttribute(ATTRIBUTE_VALIDITY))
							.map(theRange::equals)
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by equals to attribute (Multiple)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeEqualToMultiple(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							eq(ATTRIBUTE_COMBINED_PRIORITY, new Multiple(6051, 7107))
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity ->
						ofNullable(sealedEntity.getAttribute(ATTRIBUTE_COMBINED_PRIORITY))
							.map(it -> new Multiple(6051, 7107).equals(it))
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by localized attribute equals to")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByLocalizedAttributeEqualsTo(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								eq(ATTRIBUTE_NAME, "Practical Silk Hat"),
								language(CZECH_LOCALE)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable(sealedEntity.getAttribute(ATTRIBUTE_NAME, CZECH_LOCALE))
						.map("Practical Silk Hat"::equals)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity by global attribute equals to with language")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByGlobalAttributeEqualsToWithLanguage(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				// code attribute is not localized attribute
				final SealedEntity firstEntityWithSomeCzechAttribute = originalProductEntities
					.stream()
					.filter(e -> e.getLocales().contains(CZECH_LOCALE) && e.getAttribute(ATTRIBUTE_EAN) != null)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("There is no entity with attribute localized to Czech!"));
				// now retrieve non-localized attribute for this entity
				final String firstEntityEan = firstEntityWithSomeCzechAttribute.getAttribute(ATTRIBUTE_EAN);
				// and filter the entity by both language and non-localized attribute - it should match
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								eq(ATTRIBUTE_EAN, firstEntityEan),
								language(CZECH_LOCALE)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable(sealedEntity.getAttribute(ATTRIBUTE_EAN))
						.map(v -> v.equals(firstEntityEan))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by equals to attribute joined by or")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntityByAttributeEqualToJoinedByOr(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							or(
								eq(ATTRIBUTE_CODE, "Awesome-Rubber-Bottle-9"),
								eq(ATTRIBUTE_CODE, "Gorgeous-Rubber-Wallet-2-15")
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable(sealedEntity.getAttribute(ATTRIBUTE_CODE))
						.map(it -> "Awesome-Rubber-Bottle-9".equals(it) || "Gorgeous-Rubber-Wallet-2-15".equals(it))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by equals to attribute joined by and")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntityByAttributeEqualToJoinedByAnd(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								eq(ATTRIBUTE_PRIORITY, 9047),
								eq(ATTRIBUTE_ALIAS, true)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> 9047 == it)
						.orElse(false) &&
						ofNullable((Boolean) sealedEntity.getAttribute(ATTRIBUTE_ALIAS))
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by equals to attribute wrapped in negated container")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeNotEqualTo(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							not(
								eq(ATTRIBUTE_CODE, "Practical-Silk-Hat-11")
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable(sealedEntity.getAttribute(ATTRIBUTE_CODE))
						.map(it -> !"Practical-Silk-Hat-11".equals(it))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by complex and/not/or container composition")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByComplexAndOrNotComposition(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								or(
									and(
										eq(ATTRIBUTE_ALIAS, true),
										eq(ATTRIBUTE_PRIORITY, 22983)
									),
									and(
										eq(ATTRIBUTE_ALIAS, true),
										eq(ATTRIBUTE_PRIORITY, 57679)
									),
									and(
										eq(ATTRIBUTE_ALIAS, false),
										inSet(ATTRIBUTE_PRIORITY, 42982, 62815, 16747, 61091)
									)
								),
								not(
									eq(ATTRIBUTE_CODE, "Small-Bronze-Gloves-25")
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final boolean isAlias = ofNullable((Boolean) sealedEntity.getAttribute(ATTRIBUTE_ALIAS)).orElse(false);
						final long priority = ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY)).orElse(Long.MIN_VALUE);
						final String code = sealedEntity.getAttribute(ATTRIBUTE_CODE);
						return ((isAlias && (priority == 22983 || priority == 57679)) || (!isAlias && priority == 42982 || priority == 62815 || priority == 16747 || priority == 61091))
							&& !("Small-Bronze-Gloves-25".equals(code));
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by attribute in set of values")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeIn(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							inSet(ATTRIBUTE_CODE, "Sleek-Rubber-Table-2-5", "Awesome-Rubber-Bottle-9", "Enormous-Wooden-Table-18", "Non-Existing-Code")
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final String code = sealedEntity.getAttribute(ATTRIBUTE_CODE);
						return "Sleek-Rubber-Table-2-5".equals(code) ||
							"Awesome-Rubber-Bottle-9".equals(code) ||
							"Enormous-Wooden-Table-18".equals(code);
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by is true attribute (Boolean)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeIsTrue(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isTrue(ATTRIBUTE_ALIAS)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Boolean) sealedEntity.getAttribute(ATTRIBUTE_ALIAS)).orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by is false attribute (Boolean)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeIsFalse(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isFalse(ATTRIBUTE_ALIAS)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Boolean) sealedEntity.getAttribute(ATTRIBUTE_ALIAS))
						.map(it -> !it)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute between (Number)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeBetween(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							between(ATTRIBUTE_PRIORITY, 15000, 45000)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final long priority = ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY)).orElse(Long.MAX_VALUE);
						return priority >= 15000 && priority <= 45000;
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute between (ZonedDateTime)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeBetweenZonedDateTime(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final ZonedDateTime from = ZonedDateTime.of(2007, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
				final ZonedDateTime to = ZonedDateTime.of(2007, 12, 31, 23, 59, 59, 0, ZoneId.systemDefault());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							between(ATTRIBUTE_CREATED, from, to)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final ZonedDateTime created = ofNullable((ZonedDateTime) sealedEntity.getAttribute(ATTRIBUTE_CREATED)).orElse(ZonedDateTime.now());
						return created.isAfter(from) && created.isBefore(to);
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute between (DateTimeRange) - overlap")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeBetweenDateTimeRange(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final ZonedDateTime from = ZonedDateTime.of(2007, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
				final ZonedDateTime to = ZonedDateTime.of(2008, 12, 31, 23, 59, 59, 0, ZoneId.systemDefault());
				final DateTimeRange lookedUpRange = DateTimeRange.between(from, to);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							between(ATTRIBUTE_VALIDITY, from, to)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final DateTimeRange validity = sealedEntity.getAttribute(ATTRIBUTE_VALIDITY);
						return validity != null && validity.overlaps(lookedUpRange);
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute between (NumberRange) - overlap")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeBetweenNumberRange(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final NumberRange lookedUpRange = NumberRange.between(4, 6);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							between(ATTRIBUTE_SIZE, 4, 6)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						final NumberRange[] validity = sealedEntity.getAttributeArray(ATTRIBUTE_SIZE);
						return validity != null && Arrays.stream(validity).anyMatch(it -> it.overlaps(lookedUpRange));
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by string attribute contains")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeContains(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							contains(ATTRIBUTE_CODE, "Hat")
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((String) sealedEntity.getAttribute(ATTRIBUTE_CODE))
						.map(it -> it.contains("Hat"))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by string attribute starts with")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeStartsWith(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							startsWith(ATTRIBUTE_CODE, "Practical")
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((String) sealedEntity.getAttribute(ATTRIBUTE_CODE))
						.map(it -> it.startsWith("Practical"))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by string attribute ends with")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeEndsWith(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							endsWith(ATTRIBUTE_CODE, "8")
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((String) sealedEntity.getAttribute(ATTRIBUTE_CODE))
						.map(it -> it.endsWith("8"))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute lessThan")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeLessThan(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_PRIORITY, 4500)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it < 4500)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by attribute lessThan (ZonedDateTime)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeLessThanZonedDateTime(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final ZonedDateTime theMoment = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_CREATED, theMoment)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((ZonedDateTime) sealedEntity.getAttribute(ATTRIBUTE_CREATED))
						.map(it -> it.isBefore(theMoment))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by attribute lessThan (LocalDate)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeLessThanLocalDate(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final LocalDate theMoment = LocalDate.of(2003, 1, 1);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_MANUFACTURED, theMoment)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((LocalDate) sealedEntity.getAttribute(ATTRIBUTE_MANUFACTURED))
						.map(it -> it.isBefore(theMoment))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute lessThanEquals")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeLessThanOrEqualsTo(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThanEquals(ATTRIBUTE_PRIORITY, 2472)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it <= 2472)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute greaterThan")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeGreaterThan(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							greaterThan(ATTRIBUTE_PRIORITY, 90822)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it > 90822)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number attribute greaterThanEquals")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeGreaterThanOrEqualsTo(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							greaterThanEquals(ATTRIBUTE_PRIORITY, 90822)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it >= 90822)
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by date time range")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeDateTimeInRange(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final ZonedDateTime theMoment = ZonedDateTime.of(LocalDateTime.of(2015, 1, 1, 0, 0), ZoneId.systemDefault());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							inRange(ATTRIBUTE_VALIDITY, theMoment)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((DateTimeRange) sealedEntity.getAttribute(ATTRIBUTE_VALIDITY))
						.map(it -> it.isValidFor(theMoment))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by date time range (now)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeDateTimeInRangeNow(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final ZonedDateTime theMoment = ZonedDateTime.now();
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							inRangeNow(ATTRIBUTE_VALIDITY)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((DateTimeRange) sealedEntity.getAttribute(ATTRIBUTE_VALIDITY))
						.map(it -> it.isValidFor(theMoment))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by number range")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeNumberInRange(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = getByAttributeSize(session, 43);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> ofNullable((NumberRange[]) sealedEntity.getAttributeArray(ATTRIBUTE_SIZE))
						.map(it -> Arrays.stream(it).anyMatch(x -> x.isWithin(43)))
						.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return single entity that has by multiple number ranges")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnSingleEntityByAttributeNumberInRangeUsingMultipleRanges(EVITA evita, List<SealedEntity> originalProductEntities) {
		final AtomicInteger updatedProductId = new AtomicInteger();
		final AtomicReference<NumberRange[]> formerAttribute = new AtomicReference<>();
		try {
			evita.updateCatalog(
				TEST_CATALOG,
				session -> {
					final SealedEntity alteredProduct = originalProductEntities.get(0);
					formerAttribute.set(alteredProduct.getAttributeArray(ATTRIBUTE_SIZE));
					updatedProductId.set(alteredProduct.getPrimaryKey());
					session.upsertEntity(
						alteredProduct.open().setAttribute(
							ATTRIBUTE_SIZE,
							new NumberRange[]{
								NumberRange.between(879, 926),
								NumberRange.between(1250, 1780)
							}
						)
					);
				}
			);
			evita.queryCatalog(
				TEST_CATALOG,
				session -> {
					final EvitaResponseBase<EntityReference> result = getByAttributeSize(session, 900);
					assertEquals(1, result.getTotalRecordCount());
					assertEquals(updatedProductId.get(), result.getRecordData().get(0).getPrimaryKey());

					final EvitaResponseBase<EntityReference> anotherResult = getByAttributeSize(session, 1400);
					assertEquals(1, anotherResult.getTotalRecordCount());
					assertEquals(updatedProductId.get(), anotherResult.getRecordData().get(0).getPrimaryKey());
					return null;
				}
			);
		} finally {
			// revert changes
			evita.updateCatalog(
				TEST_CATALOG,
				session -> {
					final SealedEntity alteredProduct = originalProductEntities.get(0);
					updatedProductId.set(alteredProduct.getPrimaryKey());
					session.upsertEntity(
						alteredProduct.open().setAttribute(
							ATTRIBUTE_SIZE,
							formerAttribute.get()
						)
					);
				}
			);
		}
	}

	@DisplayName("Should return multiple entities by having attribute null (not defined)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeIsNull(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNull(ATTRIBUTE_PRIORITY)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_PRIORITY) == null,
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by having attribute not null")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeIsNotNull(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_PRIORITY)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_PRIORITY) != null,
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by referenced entity of particular id")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByReferencedEntityId(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							referenceHavingAttribute(
								Entities.BRAND,
								primaryKey(1)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.BRAND)
						.stream()
						.anyMatch(brand -> brand.getReferencedEntity().getPrimaryKey() == 1),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by having attribute set on referenced entity")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeSetOnReferencedEntity(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							referenceHavingAttribute(
								Entities.BRAND,
								and(
									eq(ATTRIBUTE_LOCATED_AT, Market.EUROPE),
									greaterThan(ATTRIBUTE_MARKET_SHARE, new BigDecimal("150.45"))
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.BRAND)
						.stream()
						.anyMatch(brand -> {
							final boolean marketMatch = ofNullable(brand.getAttribute(ATTRIBUTE_LOCATED_AT))
								.map(it -> Market.EUROPE == it)
								.orElse(false);
							final boolean shareMatch = ofNullable((BigDecimal) brand.getAttribute(ATTRIBUTE_MARKET_SHARE))
								.map(it -> new BigDecimal("150.45").compareTo(it) < 0)
								.orElse(false);
							return marketMatch && shareMatch;
						}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by having attribute set on referenced entity and also global attribute")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeSetOnReferencedEntityAndAlsoGlobalAttribute(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								isNotNull(ATTRIBUTE_PRIORITY),
								referenceHavingAttribute(
									Entities.BRAND,
									and(
										eq(ATTRIBUTE_LOCATED_AT, Market.EUROPE),
										greaterThan(ATTRIBUTE_MARKET_SHARE, new BigDecimal("150.45"))
									)
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_PRIORITY) != null &&
						sealedEntity
							.getReferences(Entities.BRAND)
							.stream()
							.anyMatch(brand -> {
								final boolean marketMatch = ofNullable(brand.getAttribute(ATTRIBUTE_LOCATED_AT))
									.map(it -> Market.EUROPE == it)
									.orElse(false);
								final boolean shareMatch = ofNullable((BigDecimal) brand.getAttribute(ATTRIBUTE_MARKET_SHARE))
									.map(it -> new BigDecimal("150.45").compareTo(it) < 0)
									.orElse(false);
								return marketMatch && shareMatch;
							}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by having attribute set on two referenced entities (AND)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeSetOnTwoReferencedEntitiesByAndRelation(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								referenceHavingAttribute(
									Entities.BRAND,
									and(
										eq(ATTRIBUTE_LOCATED_AT, Market.EUROPE),
										greaterThan(ATTRIBUTE_MARKET_SHARE, new BigDecimal("150.45"))
									)
								),
								referenceHavingAttribute(
									Entities.STORE,
									eq(ATTRIBUTE_LOCATED_AT, Market.EUROPE)
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.BRAND)
						.stream()
						.anyMatch(brand -> {
							final boolean marketMatch = ofNullable(brand.getAttribute(ATTRIBUTE_LOCATED_AT))
								.map(it -> Market.EUROPE == it)
								.orElse(false);
							final boolean shareMatch = ofNullable((BigDecimal) brand.getAttribute(ATTRIBUTE_MARKET_SHARE))
								.map(it -> new BigDecimal("150.45").compareTo(it) < 0)
								.orElse(false);
							return marketMatch && shareMatch;
						}) &&
						sealedEntity
							.getReferences(Entities.STORE)
							.stream()
							.anyMatch(store -> ofNullable(store.getAttribute(ATTRIBUTE_LOCATED_AT))
								.map(it -> Market.EUROPE == it)
								.orElse(false)),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return multiple entities by having attribute set on two referenced entities (OR)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnMultipleEntitiesByAttributeSetOnTwoReferencedEntitiesByOrRelation(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							or(
								referenceHavingAttribute(
									Entities.BRAND,
									and(
										eq(ATTRIBUTE_LOCATED_AT, Market.EUROPE),
										greaterThan(ATTRIBUTE_MARKET_SHARE, new BigDecimal("600"))
									)
								),
								referenceHavingAttribute(
									Entities.STORE,
									and(
										eq(ATTRIBUTE_LOCATED_AT, Market.EUROPE),
										lessThan(ATTRIBUTE_CAPACITY, 25000L)
									)
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertResultIs(
					originalProductEntities,
					sealedEntity ->
						sealedEntity.getReferences(Entities.BRAND)
							.stream()
							.anyMatch(brand -> {
								final boolean marketMatch = ofNullable(brand.getAttribute(ATTRIBUTE_LOCATED_AT))
									.map(it -> Market.EUROPE == it)
									.orElse(false);
								final boolean shareMatch = ofNullable((BigDecimal) brand.getAttribute(ATTRIBUTE_MARKET_SHARE))
									.map(it -> new BigDecimal("600").compareTo(it) < 0)
									.orElse(false);
								return marketMatch && shareMatch;
							}) ||
							sealedEntity
								.getReferences(Entities.STORE)
								.stream()
								.anyMatch(store -> {
									final boolean marketMatch = ofNullable(store.getAttribute(ATTRIBUTE_LOCATED_AT))
										.map(it -> Market.EUROPE == it)
										.orElse(false);
									final boolean capacityMath = ofNullable((Long) store.getAttribute(ATTRIBUTE_CAPACITY))
										.map(it -> 25000L > it)
										.orElse(false);
									return marketMatch && capacityMath;
								}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities randomly")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesRandomly(EVITA evita, List<SealedEntity> originalProductEntities) {
		final int[][] results = new int[2][];
		for (int i = 0; i < 2; i++) {
			results[i] = evita.queryCatalog(
				TEST_CATALOG,
				session -> {
					final EvitaResponseBase<EntityReference> result = session.query(
						query(
							entities(Entities.PRODUCT),
							filterBy(
								lessThan(ATTRIBUTE_PRIORITY, 35000L)
							),
							orderBy(
								random()
							),
							require(
								page(1, Integer.MAX_VALUE)
							)
						),
						EntityReference.class
					);
					return result
						.getRecordData()
						.stream()
						.mapToInt(EntityReference::getPrimaryKey)
						.toArray();
				}
			);
		}
		assertArrayAreDifferent(results[0], results[1]);
		Arrays.sort(results[0]);
		Arrays.sort(results[1]);
		assertArrayEquals(results[0], results[1], "After sorting arrays should be equal.");
	}

	@DisplayName("Should return entities sorted by String attribute (combined with filtering)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToStringAttribute(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_PRIORITY, 35000L)
						),
						orderBy(
							descending(ATTRIBUTE_CODE)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it < 35000L)
						.orElse(false),
					(sealedEntityA, sealedEntityB) -> sealedEntityB.getAttribute(ATTRIBUTE_CODE).compareTo(sealedEntityA.getAttribute(ATTRIBUTE_CODE))
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by two attributes")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToTwoAttributes(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_PRIORITY, 35000L)
						),
						orderBy(
							descending(ATTRIBUTE_CREATED),
							ascending(ATTRIBUTE_MANUFACTURED)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it < 35000L)
						.orElse(false),
					new PredicateWithComparatorTuple(
						(sealedEntity) -> sealedEntity.getAttribute(ATTRIBUTE_CREATED) != null,
						(sealedEntityA, sealedEntityB) -> sealedEntityB.getAttribute(ATTRIBUTE_CREATED).compareTo(sealedEntityA.getAttribute(ATTRIBUTE_CREATED))
					),
					new PredicateWithComparatorTuple(
						(sealedEntity) -> sealedEntity.getAttribute(ATTRIBUTE_MANUFACTURED) != null,
						Comparator.comparing(sealedEntity -> (LocalDate) sealedEntity.getAttribute(ATTRIBUTE_MANUFACTURED))
					)
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by Number attribute (combined with filtering)")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToNumberAttribute(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_PRIORITY, 14000L)
						),
						orderBy(
							descending(ATTRIBUTE_PRIORITY)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> ofNullable((Long) sealedEntity.getAttribute(ATTRIBUTE_PRIORITY))
						.map(it -> it < 14000L)
						.orElse(false),
					(sealedEntityA, sealedEntityB) -> sealedEntityB.getAttribute(ATTRIBUTE_PRIORITY).compareTo(sealedEntityA.getAttribute(ATTRIBUTE_PRIORITY))
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by BigDecimal attribute ")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToBigDecimalAttribute(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_QUANTITY, new BigDecimal("250"))
						),
						orderBy(
							ascending(ATTRIBUTE_QUANTITY)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> ofNullable((BigDecimal) sealedEntity.getAttribute(ATTRIBUTE_QUANTITY))
						.map(it -> it.compareTo(new BigDecimal("250")) < 0)
						.orElse(false),
					Comparator.comparing(sealedEntityA -> (BigDecimal) sealedEntityA.getAttribute(ATTRIBUTE_QUANTITY))
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by ZonedDateTime attribute ")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToZonedDateTimeAttribute(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2003, 6, 10, 14, 24, 32, 0, ZoneId.systemDefault());
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							lessThan(ATTRIBUTE_CREATED, theMoment)
						),
						orderBy(
							ascending(ATTRIBUTE_CREATED)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> ofNullable((ZonedDateTime) sealedEntity.getAttribute(ATTRIBUTE_CREATED))
						.map(theMoment::isAfter)
						.orElse(false),
					Comparator.comparing(sealedEntityA -> (ZonedDateTime) sealedEntityA.getAttribute(ATTRIBUTE_CREATED))
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by Multiple attribute ")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToMultipleAttribute(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_COMBINED_PRIORITY)
						),
						orderBy(
							descending(ATTRIBUTE_COMBINED_PRIORITY)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_COMBINED_PRIORITY) != null,
					(sealedEntityA, sealedEntityB) -> sealedEntityB.getAttribute(ATTRIBUTE_COMBINED_PRIORITY).compareTo(sealedEntityA.getAttribute(ATTRIBUTE_COMBINED_PRIORITY))
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by attribute defined on referenced entity")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToAttributeOnReferencedEntity(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							referenceHavingAttribute(
								Entities.BRAND,
								isNotNull(ATTRIBUTE_MARKET_SHARE)
							)
						),
						orderBy(
							referenceAttribute(
								Entities.BRAND,
								descending(ATTRIBUTE_MARKET_SHARE)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getReferences(Entities.BRAND).stream().anyMatch(it -> it.getAttribute(ATTRIBUTE_MARKET_SHARE) != null),
					(sealedEntityA, sealedEntityB) -> {
						final BigDecimal marketShareB = sealedEntityB.getReferences(Entities.BRAND).stream().map(it -> (BigDecimal) it.getAttribute(ATTRIBUTE_MARKET_SHARE)).findFirst().orElseThrow(IllegalStateException::new);
						final BigDecimal marketShareA = sealedEntityA.getReferences(Entities.BRAND).stream().map(it -> (BigDecimal) it.getAttribute(ATTRIBUTE_MARKET_SHARE)).findFirst().orElseThrow(IllegalStateException::new);
						return marketShareB.compareTo(marketShareA);
					}
				);
				return null;
			}
		);
	}

	@DisplayName("Should return entities sorted by multiple attributes defined on different referenced entities")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldSortEntitiesAccordingToMultipleAttributesOnReferencedEntity(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						orderBy(
							referenceAttribute(
								Entities.BRAND,
								descending(ATTRIBUTE_MARKET_SHARE)
							),
							referenceAttribute(
								Entities.STORE,
								ascending(ATTRIBUTE_CAPACITY)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);
				assertSortedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> true,
					new PredicateWithComparatorTuple(
						sealedEntity -> sealedEntity.getReferences(Entities.BRAND).stream().anyMatch(it -> it.getAttribute(ATTRIBUTE_MARKET_SHARE) != null),
						(sealedEntityA, sealedEntityB) -> {
							final BigDecimal marketShareB = sealedEntityB.getReferences(Entities.BRAND).stream().map(it -> (BigDecimal) it.getAttribute(ATTRIBUTE_MARKET_SHARE)).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
							final BigDecimal marketShareA = sealedEntityA.getReferences(Entities.BRAND).stream().map(it -> (BigDecimal) it.getAttribute(ATTRIBUTE_MARKET_SHARE)).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
							return marketShareB.compareTo(marketShareA);
						}
					),
					new PredicateWithComparatorTuple(
						sealedEntity -> sealedEntity.getReferences(Entities.STORE).stream().anyMatch(it -> it.getAttribute(ATTRIBUTE_CAPACITY) != null),
						(sealedEntityA, sealedEntityB) -> {
							final Long capacityA = sealedEntityA.getReferences(Entities.STORE).stream().map(it -> (Long) it.getAttribute(ATTRIBUTE_CAPACITY)).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
							final Long capacityB = sealedEntityB.getReferences(Entities.STORE).stream().map(it -> (Long) it.getAttribute(ATTRIBUTE_CAPACITY)).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
							return capacityA.compareTo(capacityB);
						}
					)
				);
				return null;
			}
		);
	}

	@DisplayName("Should return only first page of filtered and sorted entities")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnFirstPageOfEntities(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_ALIAS)
						),
						orderBy(
							ascending(ATTRIBUTE_CODE)
						),
						require(
							page(1, 3)
						)
					),
					EntityReference.class
				);
				assertEquals(89, result.getTotalRecordCount());
				assertSortedAndPagedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null,
					Comparator.comparing(sealedEntity -> (String) sealedEntity.getAttribute(ATTRIBUTE_CODE)),
					0, 3
				);
				return null;
			}
		);
	}

	@DisplayName("Should return only fifth page of filtered and sorted entities")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnFifthPageOfEntities(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_ALIAS)
						),
						orderBy(
							ascending(ATTRIBUTE_CODE)
						),
						require(
							page(5, 3)
						)
					),
					EntityReference.class
				);
				assertEquals(89, result.getTotalRecordCount());
				assertSortedAndPagedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null,
					Comparator.comparing(sealedEntity -> (String) sealedEntity.getAttribute(ATTRIBUTE_CODE)),
					4 * 3, 3
				);
				return null;
			}
		);
	}

	@DisplayName("Should return only first page with offset of filtered and sorted entities")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnFirstPageOfEntitiesWithOffset(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_ALIAS)
						),
						orderBy(
							ascending(ATTRIBUTE_CODE)
						),
						require(
							strip(0, 2)
						)
					),
					EntityReference.class
				);
				assertEquals(89, result.getTotalRecordCount());
				assertSortedAndPagedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null,
					Comparator.comparing(sealedEntity -> (String) sealedEntity.getAttribute(ATTRIBUTE_CODE)),
					0, 2
				);
				return null;
			}
		);
	}

	@DisplayName("Should return only fifth page with offset of filtered and sorted entities")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnFifthPageOfEntitiesWithOffset(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_ALIAS)
						),
						orderBy(
							ascending(ATTRIBUTE_CODE)
						),
						require(
							strip(11, 30)
						)
					),
					EntityReference.class
				);
				assertEquals(89, result.getTotalRecordCount());
				assertSortedAndPagedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null,
					Comparator.comparing(sealedEntity -> (String) sealedEntity.getAttribute(ATTRIBUTE_CODE)),
					11, 30
				);
				return null;
			}
		);
	}

	@DisplayName("Should return first page when there is not enough results")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnFirstPageOfEntitiesWhenThereIsNoRequiredPageAfterFiltering(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_ALIAS)
						),
						orderBy(
							ascending(ATTRIBUTE_CODE)
						),
						require(
							page(11, 10)
						)
					),
					EntityReference.class
				);
				assertEquals(89, result.getTotalRecordCount());
				assertSortedAndPagedResultIs(
					originalProductEntities,
					result.getRecordData(),
					sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null,
					Comparator.comparing(sealedEntity -> (String) sealedEntity.getAttribute(ATTRIBUTE_CODE)),
					0, 10
				);
				return null;
			}
		);
	}

	@DisplayName("Should return attribute histogram for returned products")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnAttributeHistogram(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							isNotNull(ATTRIBUTE_ALIAS)
						),
						require(
							page(1, Integer.MAX_VALUE),
							entityBody(),
							attributeHistogram(20, ATTRIBUTE_QUANTITY, ATTRIBUTE_PRIORITY)
						)
					),
					SealedEntity.class
				);

				final List<SealedEntity> filteredProducts = originalProductEntities
					.stream()
					.filter(sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null)
					.collect(Collectors.toList());

				assertHistogramIntegrity(result, filteredProducts, ATTRIBUTE_QUANTITY);
				assertHistogramIntegrity(result, filteredProducts, ATTRIBUTE_PRIORITY);

				return null;
			}
		);
	}

	@DisplayName("Should return attribute histogram for returned products excluding constraints targeting that attribute")
	@UseDataSet(HUNDRED_PRODUCTS)
	@Test
	void shouldReturnAttributeHistogramWithoutBeingAffectedByAttributeFilter(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Predicate<SealedEntity> priorityAttributePredicate = it -> ofNullable((Long) it.getAttribute(ATTRIBUTE_PRIORITY))
			.map(attr -> attr >= 15000L && attr <= 90000L)
			.orElse(false);
		final Predicate<SealedEntity> quantityAttributePredicate = it -> ofNullable((BigDecimal) it.getAttribute(ATTRIBUTE_QUANTITY))
			.map(attr -> attr.compareTo(new BigDecimal("100")) >= 0 && attr.compareTo(new BigDecimal("900")) <= 0)
			.orElse(false);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								isNotNull(ATTRIBUTE_ALIAS),
								userFilter(
									between(ATTRIBUTE_QUANTITY, 100, 900),
									between(ATTRIBUTE_PRIORITY, 15000, 90000)
								)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							entityBody(),
							attributeHistogram(20, ATTRIBUTE_QUANTITY, ATTRIBUTE_PRIORITY)
						)
					),
					SealedEntity.class
				);

				final List<SealedEntity> filteredProducts = originalProductEntities
					.stream()
					.filter(sealedEntity -> sealedEntity.getAttribute(ATTRIBUTE_ALIAS) != null)
					.collect(Collectors.toList());

				// verify our test works
				final Predicate<SealedEntity> attributePredicate = priorityAttributePredicate.or(quantityAttributePredicate);
				assertTrue(
					filteredProducts.size() > filteredProducts.stream().filter(attributePredicate).count(),
					"Price between constraint didn't filter out any products. Test is not testing anything!"
				);

				// the attribute `between(ATTRIBUTE_QUANTITY, 100, 900)` constraint must be ignored while computing its histogram
				assertHistogramIntegrity(
					result,
					filteredProducts.stream().filter(priorityAttributePredicate).collect(Collectors.toList()),
					ATTRIBUTE_QUANTITY
				);

				// the attribute `between(ATTRIBUTE_PRIORITY, 15000, 90000)` constraint must be ignored while computing its histogram
				assertHistogramIntegrity(
					result,
					filteredProducts.stream().filter(quantityAttributePredicate).collect(Collectors.toList()),
					ATTRIBUTE_PRIORITY
				);

				return null;
			}
		);
	}

	private EvitaResponseBase<EntityReference> getByAttributeSize(SESSION session, int size) {
		return session.query(
			query(
				entities(Entities.PRODUCT),
				filterBy(
					inRange(ATTRIBUTE_SIZE, size)
				),
				require(
					page(1, Integer.MAX_VALUE)
				)
			),
			EntityReference.class
		);
	}

	/*
		HELPER METHODS AND ASSERTIONS
	 */

	/**
	 * Verifies histogram integrity against source entities.
	 */
	private static void assertHistogramIntegrity(EvitaResponseBase<SealedEntity> result, List<SealedEntity> filteredProducts, String attributeName) {
		final AttributeHistogram histogramPacket = result.getAdditionalResults(AttributeHistogram.class);
		assertNotNull(histogramPacket);
		final HistogramContract histogram = histogramPacket.getHistogram(attributeName);
		assertTrue(histogram.getBuckets().length <= 20);

		assertEquals(
			filteredProducts.stream().filter(it -> it.getAttribute(attributeName) != null).count(),
			histogram.getOverallCount()
		);

		final List<BigDecimal> attributeValues = filteredProducts
			.stream()
			.map(it -> convertToBigDecimal(attributeName, it))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		//noinspection SimplifiableAssertion
		assertTrue(
			attributeValues.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO).compareTo(histogram.getMin()) == 0,
			"Min value is not equal."
		);
		//noinspection SimplifiableAssertion
		assertTrue(
			attributeValues.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO).compareTo(histogram.getMax()) == 0,
			"Max value is not equal."
		);

		// verify bucket occurrences
		final Map<Integer, Integer> expectedOccurrences = filteredProducts
			.stream()
			.map(it -> convertToBigDecimal(attributeName, it))
			.filter(Objects::nonNull)
			.collect(
				Collectors.groupingBy(
					it -> findIndexInHistogram(it, histogram),
					summingInt(entity -> 1)
				)
			);

		final Bucket[] buckets = histogram.getBuckets();
		for (int i = 0; i < buckets.length; i++) {
			final Bucket bucket = histogram.getBuckets()[i];
			assertEquals(
				ofNullable(expectedOccurrences.get(i)).orElse(0), bucket.getOccurrences(),
				"Expected " + expectedOccurrences.get(i) + " occurrences in bucket " + i + ", but got " + bucket.getOccurrences() + "!"
			);
		}
	}

	/**
	 * Converts numeric value to BigDecimal.
	 */
	private static BigDecimal convertToBigDecimal(String attributeName, SealedEntity it) {
		final Object value = it.getAttribute(attributeName);
		if (value == null) {
			return null;
		}

		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		} else {
			return BigDecimal.valueOf((long) value);
		}
	}

	/**
	 * Finds appropriate index in the histogram according to histogram thresholds.
	 */
	private static int findIndexInHistogram(BigDecimal attributeValue, HistogramContract histogram) {
		final Bucket[] buckets = histogram.getBuckets();
		for (int i = buckets.length - 1; i >= 0; i--) {
			final Bucket bucket = buckets[i];
			final int valueCompared = attributeValue.compareTo(bucket.getThreshold());
			if (valueCompared >= 0) {
				return i;
			}
		}
		fail("Histogram span doesn't match current entity attribute value: " + attributeValue);
		return -1;
	}

	private void assertSortedAndPagedResultIs(List<SealedEntity> originalProductEntities, List<EntityReference> records, Predicate<SealedEntity> predicate, Comparator<SealedEntity> comparator, int skip, int limit) {
		assertSortedResultEquals(
			records.stream().map(EntityReference::getPrimaryKey).collect(Collectors.toList()),
			originalProductEntities.stream()
				.filter(predicate)
				.sorted(comparator)
				.mapToInt(EntityContract::getPrimaryKey)
				.skip(Math.max(skip, 0))
				.limit(skip >= 0 ? limit : limit + skip)
				.toArray()
		);
	}

	private void assertSortedResultIs(List<SealedEntity> originalProductEntities, List<EntityReference> records, Predicate<SealedEntity> predicate, Comparator<SealedEntity> comparator) {
		assertSortedResultIs(originalProductEntities, records, predicate, new PredicateWithComparatorTuple(predicate, comparator));
	}

	private void assertSortedResultIs(List<SealedEntity> originalProductEntities, List<EntityReference> records, Predicate<SealedEntity> filteringPredicate, PredicateWithComparatorTuple... sortVector) {
		final List<Predicate<SealedEntity>> previousPredicateAcc = new ArrayList<>();
		final int[] expectedSortedRecords = Stream.concat(
				Arrays.stream(sortVector)
					.flatMap(it -> {
						final List<SealedEntity> subResult = originalProductEntities
							.stream()
							.filter(filteringPredicate)
							.filter(entity -> previousPredicateAcc.stream().noneMatch(predicate -> predicate.test(entity)))
							.filter(it.getPredicate())
							.sorted(it.getComparator())
							.collect(Collectors.toList());
						previousPredicateAcc.add(it.getPredicate());
						return subResult.stream();
					}),
				// append entities that don't match any predicate
				originalProductEntities
					.stream()
					.filter(filteringPredicate)
					.filter(entity -> previousPredicateAcc.stream().noneMatch(predicate -> predicate.test(entity)))
			)
			.mapToInt(EntityContract::getPrimaryKey)
			.toArray();

		assertSortedResultEquals(
			records.stream().map(EntityReference::getPrimaryKey).collect(Collectors.toList()),
			expectedSortedRecords
		);
	}

	public enum Market {
		EUROPE, NORTH_AMERICA, ASIA
	}

	@Data
	public static class PredicateWithComparatorTuple {
		private final Predicate<SealedEntity> predicate;
		private final Comparator<SealedEntity> comparator;

	}

}
