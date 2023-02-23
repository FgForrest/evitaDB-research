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
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.io.extraResult.HierarchyStatistics;
import io.evitadb.api.io.extraResult.HierarchyStatistics.LevelInfo;
import io.evitadb.api.io.extraResult.Parents;
import io.evitadb.api.io.extraResult.Parents.ParentsByType;
import io.evitadb.test.Entities;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.extension.DataCarrier;
import io.evitadb.test.generator.DataGenerator;
import lombok.extern.slf4j.Slf4j;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static io.evitadb.test.generator.DataGenerator.*;
import static io.evitadb.utils.AssertionUtils.assertResultIs;
import static java.util.Optional.ofNullable;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies whether entities that reference other - hierarchical entity can be filtered by hierarchy constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita referenced entity filtering by hierarchy functionality")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class ReferencingEntityByHierarchyFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	private static final String THOUSAND_PRODUCTS = "ThousandProducts";
	private static final String ATTRIBUTE_MARKET_SHARE = "marketShare";
	private static final String ATTRIBUTE_FOUNDED = "founded";
	private static final String ATTRIBUTE_CAPACITY = "capacity";
	private static final int SEED = 40;
	private final DataGenerator dataGenerator = new DataGenerator();

	@DataSet(THOUSAND_PRODUCTS)
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

			final List<EntityReferenceContract> storedCategories = dataGenerator.generateEntities(
					dataGenerator.getSampleCategorySchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(100)
				.map(session::upsertEntity)
				.collect(Collectors.toList());

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
							.withReferenceToEntity(Entities.BRAND,
								whichIs -> whichIs.indexed()
									.withAttribute(ATTRIBUTE_MARKET_SHARE, BigDecimal.class, thatIs -> thatIs.filterable().sortable())
									.withAttribute(ATTRIBUTE_FOUNDED, ZonedDateTime.class, thatIs -> thatIs.filterable().sortable())
							)
							.withReferenceToEntity(Entities.STORE,
								whichIs -> whichIs.withAttribute(ATTRIBUTE_CAPACITY, Long.class, thatIs -> thatIs.filterable().sortable())
							)
					),
					randomEntityPicker,
					SEED
				)
				.limit(1000)
				.map(session::upsertEntity)
				.collect(Collectors.toList());
			session.catalog.flush();
			return new DataCarrier(
				"originalProductEntities",
				storedProducts.stream()
					.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), attributes(), references()))
					.collect(Collectors.toList()),
				"originalCategoryEntities",
				storedCategories.stream()
					.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), attributes(), references()))
					.collect(Collectors.toList()),
				"categoryHierarchy",
				dataGenerator.getHierarchy(Entities.CATEGORY)
			);
		});
	}

	@DisplayName("Should return all product linked to any category")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnAllProductsLinkedToAnyCategory(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinRootHierarchy(Entities.CATEGORY)),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> !sealedEntity.getReferences(Entities.CATEGORY).isEmpty(),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return all products in categories except specified category subtrees")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnAllProductsInCategoriesExceptSpecifiedSubtrees(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		final Set<Integer> excluded = new HashSet<>(Arrays.asList(1, 7, 13, 16, 40, 55));
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinRootHierarchy(Entities.CATEGORY, excluding(excluded.toArray(new Integer[0])))),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> {
							// is not directly excluded node
							return !excluded.contains(category.getReferencedEntity().getPrimaryKey()) &&
								// has no parent node that is in excluded set
								categoryHierarchy.getParentItems(String.valueOf(category.getReferencedEntity().getPrimaryKey()))
									.stream()
									.map(it -> Integer.parseInt(it.getCode()))
									.noneMatch(excluded::contains);
						}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products in category")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnProductsInCategory(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 7, directRelation())),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> {
							// is directly referencing category 7
							return Objects.equals(category.getReferencedEntity().getPrimaryKey(), 7);
						}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products in selected category subtree")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnProductsInCategorySubtree(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 7)),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> {
							final String categoryId = String.valueOf(category.getReferencedEntity().getPrimaryKey());
							// is either category 7
							return Objects.equals(categoryId, String.valueOf(7)) ||
								// or has parent category 7
								categoryHierarchy.getParentItems(categoryId)
									.stream()
									.anyMatch(it -> Objects.equals(it.getCode(), String.valueOf(7)));
						}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products in selected category subtree except directly related products")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnProductsInCategorySubtreeExceptDirectlyRelated(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 7, excludingRoot())),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> {
							final String categoryId = String.valueOf(category.getReferencedEntity().getPrimaryKey());
							// is not exactly category 7
							return !Objects.equals(categoryId, String.valueOf(7)) &&
								// but has parent category 7
								categoryHierarchy.getParentItems(categoryId)
									.stream()
									.anyMatch(it -> Objects.equals(it.getCode(), String.valueOf(7)));
						}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products in category subtree except specified subtrees")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnProductsInCategorySubtreeExceptSpecifiedSubtrees(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		final Set<Integer> excluded = new HashSet<>(Arrays.asList(2, 43, 34, 53));
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 1, excluding(excluded.toArray(new Integer[0])))),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> {
							final int categoryId = category.getReferencedEntity().getPrimaryKey();
							final String categoryIdAsString = String.valueOf(categoryId);
							final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(categoryIdAsString);
							return
								// is not directly excluded node
								!excluded.contains(categoryId) &&
									// has no excluded parent node
									parentItems
										.stream()
										.map(it -> Integer.parseInt(it.getCode()))
										.noneMatch(excluded::contains) &&
									// has parent node 1
									(
										Objects.equals(1, categoryId) ||
											parentItems
												.stream()
												.anyMatch(it -> Objects.equals(String.valueOf(1), it.getCode()))
									);
						}),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products in category subtree except specified subtrees and matching attribute filter")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnProductsInCategorySubtreeExceptSpecifiedSubtreesAndMatchingCertainConstraint(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		final Set<Integer> excluded = new HashSet<>(Arrays.asList(2, 34));
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								or(
									lessThan(ATTRIBUTE_PRIORITY, 25000),
									startsWith(ATTRIBUTE_CODE, "E")
								),
								referenceHavingAttribute(
									Entities.BRAND,
									greaterThan(ATTRIBUTE_MARKET_SHARE, 50)
								),
								referenceHavingAttribute(
									Entities.STORE,
									greaterThan(ATTRIBUTE_CAPACITY, 50000)
								),
								withinHierarchy(Entities.CATEGORY, 1, excluding(excluded.toArray(new Integer[0])))
							)
						),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalProductEntities,
					sealedEntity -> {
						return
							// direct attribute condition matches
							(
								ofNullable(sealedEntity.getAttribute(ATTRIBUTE_CODE)).map(it -> ((String) it).startsWith("E")).orElse(false) ||
									ofNullable(sealedEntity.getAttribute(ATTRIBUTE_PRIORITY)).map(it -> ((Long) it) < 25000).orElse(false)
							) &&
								// constraint on referenced entity BRAND attribute matches
								(
									sealedEntity.getReferences(Entities.BRAND)
										.stream()
										.anyMatch(brand -> {
											// brand market share is bigger than
											return ofNullable(brand.getAttribute(ATTRIBUTE_MARKET_SHARE))
												.map(it -> ((BigDecimal) it).compareTo(new BigDecimal("50")) > 0)
												.orElse(false);
										})
								) &&
								// constraint on referenced entity STORE attribute matches
								(
									sealedEntity.getReferences(Entities.STORE)
										.stream()
										.anyMatch(brand -> {
											// brand market share is bigger than
											return ofNullable(brand.getAttribute(ATTRIBUTE_CAPACITY))
												.map(it -> ((Long) it) > 50000L)
												.orElse(false);
										})
								) &&
								// category hierarchy constraint is fulfilled
								sealedEntity
									.getReferences(Entities.CATEGORY)
									.stream()
									.anyMatch(category -> {
										final int categoryId = category.getReferencedEntity().getPrimaryKey();
										final String categoryIdAsString = String.valueOf(categoryId);
										final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(categoryIdAsString);
										// is not directly excluded node
										return !excluded.contains(categoryId) &&
											// has no excluded parent node
											parentItems
												.stream()
												.map(it -> Integer.parseInt(it.getCode()))
												.noneMatch(excluded::contains) &&
											// has parent node 1
											(
												Objects.equals(1, categoryId) ||
													parentItems
														.stream()
														.anyMatch(it -> Objects.equals(String.valueOf(1), it.getCode()))
											);
									});
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return category parents for returned products when only primary keys are returned")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnCategoryParentsForReturnedProductsWhenOnlyPrimaryKeysAreReturned(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 94)),
						require(
							page(1, Integer.MAX_VALUE),
							parentsOfType(Entities.CATEGORY)
						)
					),
					EntityReference.class
				);

				assertFalse(result.getRecordData().isEmpty());

				final Parents parents = result.getAdditionalResults(Parents.class);
				assertNotNull(parents, "No parents DTO was returned!");
				final ParentsByType<Integer> categoryParents = parents.ofType(Entities.CATEGORY, Integer.class);

				final Map<Integer, SealedEntity> originalProductIndex = originalProductEntities.stream()
					.collect(
						Collectors.toMap(
							EntityContract::getPrimaryKey,
							Function.identity()
						)
					);

				for (EntityReference entity : result.getRecordData()) {
					final SealedEntity originalEntity = originalProductIndex.get(entity.getPrimaryKey());
					final Collection<ReferenceContract> references = originalEntity.getReferences(Entities.CATEGORY);
					assertFalse(references.isEmpty());
					references.forEach(relatedCategory -> {
						final int referencedCategoryId = relatedCategory.getReferencedEntity().getPrimaryKey();
						final Integer[] relatedParentIds = categoryParents.getParentsFor(entity.getPrimaryKey(), referencedCategoryId);
						assertArrayEquals(
							getParentIds(categoryHierarchy, referencedCategoryId),
							relatedParentIds
						);
					});
				}

				return null;
			}
		);
	}

	@DisplayName("Should return category parents for returned products")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnCategoryParentsForReturnedProducts(EVITA evita, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 94)),
						require(
							page(1, Integer.MAX_VALUE),
							entityBody(),
							references(Entities.CATEGORY),
							parentsOfType(Entities.CATEGORY)
						)
					),
					SealedEntity.class
				);

				assertFalse(result.getRecordData().isEmpty());

				final Parents parents = result.getAdditionalResults(Parents.class);
				assertNotNull(parents, "No parents DTO was returned!");
				final ParentsByType<Integer> categoryParents = parents.ofType(Entities.CATEGORY, Integer.class);

				for (SealedEntity entity : result.getRecordData()) {
					final Collection<ReferenceContract> references = entity.getReferences(Entities.CATEGORY);
					assertFalse(references.isEmpty());
					references.forEach(relatedCategory -> {
						final int referencedCategoryId = relatedCategory.getReferencedEntity().getPrimaryKey();
						final Integer[] relatedParentIds = categoryParents.getParentsFor(entity.getPrimaryKey(), referencedCategoryId);
						final Integer[] parentIds = getParentIds(categoryHierarchy, referencedCategoryId);
						assertArrayEquals(parentIds, relatedParentIds);
					});
				}

				return null;
			}
		);
	}

	@DisplayName("Should return category parent bodies for returned products")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnCategoryParentBodiesForReturnedProducts(EVITA evita, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(withinHierarchy(Entities.CATEGORY, 94)),
						require(
							page(1, Integer.MAX_VALUE),
							entityBody(),
							references(Entities.CATEGORY),
							parentsOfType(Entities.CATEGORY, entityBody())
						)
					),
					SealedEntity.class
				);

				assertFalse(result.getRecordData().isEmpty());

				final Parents parents = result.getAdditionalResults(Parents.class);
				assertNotNull(parents, "No parents DTO was returned!");
				final ParentsByType<SealedEntity> categoryParents = parents.ofType(Entities.CATEGORY, SealedEntity.class);

				// all results should start with same parents when we query by hierarchy
				for (SealedEntity entity : result.getRecordData()) {
					final Collection<ReferenceContract> references = entity.getReferences(Entities.CATEGORY);
					assertFalse(references.isEmpty());
					references.forEach(relatedCategory -> {
						final int referencedCategoryId = relatedCategory.getReferencedEntity().getPrimaryKey();
						final SealedEntity[] relatedParents = categoryParents.getParentsFor(entity.getPrimaryKey(), referencedCategoryId);
						final Integer[] relatedParentIds = Arrays.stream(relatedParents).map(EntityContract::getPrimaryKey).toArray(Integer[]::new);
						assertArrayEquals(
							getParentIds(categoryHierarchy, referencedCategoryId),
							relatedParentIds
						);
					});
				}

				return null;
			}
		);
	}

	@DisplayName("Should return cardinalities for products")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnCardinalitiesForProducts(EVITA evita, List<SealedEntity> originalProductEntities, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								eq(ATTRIBUTE_ALIAS, true),
								language(CZECH_LOCALE)
							)
						),
						require(
							// we don't need the results whatsoever
							page(1, 0),
							// we need only data about cardinalities
							hierarchyStatistics(Entities.CATEGORY, entityBody(), attributes())
						)
					),
					EntityReference.class
				);

				final Predicate<SealedEntity> languagePredicate = it -> it.getLocales().contains(CZECH_LOCALE);
				final Predicate<SealedEntity> aliasPredicate = it -> ofNullable((Boolean) it.getAttribute(ATTRIBUTE_ALIAS)).orElse(false);
				final HierarchyStatistics expectedStatistics = computeExpectedStatistics(
					session, null, categoryHierarchy, originalProductEntities, originalCategoryEntities,
					languagePredicate.and(aliasPredicate), languagePredicate
				);

				final HierarchyStatistics statistics = result.getAdditionalResults(HierarchyStatistics.class);
				assertNotNull(statistics);
				assertEquals(expectedStatistics, statistics);

				return null;
			}
		);
	}

	@DisplayName("Should return cardinalities for products in subtree")
	@UseDataSet(THOUSAND_PRODUCTS)
	@Test
	void shouldReturnCardinalitiesForProductsWhenSubTreeIsRequested(EVITA evita, List<SealedEntity> originalProductEntities, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								eq(ATTRIBUTE_ALIAS, true),
								language(CZECH_LOCALE),
								withinHierarchy(Entities.CATEGORY, 2)
							)
						),
						require(
							// we don't need the results whatsoever
							page(1, 0),
							// we need only data about cardinalities
							hierarchyStatistics(Entities.CATEGORY, entityBody(), attributes())
						)
					),
					EntityReference.class
				);

				final Predicate<SealedEntity> languagePredicate = it -> it.getLocales().contains(CZECH_LOCALE);
				final Predicate<SealedEntity> aliasPredicate = it -> ofNullable((Boolean) it.getAttribute(ATTRIBUTE_ALIAS)).orElse(false);
				final Predicate<SealedEntity> categoryPredicate = sealedEntity -> sealedEntity
					.getReferences(Entities.CATEGORY)
					.stream()
					.anyMatch(category -> {
						final int categoryId = category.getReferencedEntity().getPrimaryKey();
						final String categoryIdAsString = String.valueOf(categoryId);
						final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(categoryIdAsString);
						return // has parent node 2
							(
								Objects.equals(2, categoryId) ||
									parentItems
										.stream()
										.anyMatch(it -> Objects.equals(String.valueOf(2), it.getCode()))
							);
					});

				final HierarchyStatistics expectedStatistics = computeExpectedStatistics(
					session, 2, categoryHierarchy, originalProductEntities, originalCategoryEntities,
					languagePredicate.and(aliasPredicate).and(categoryPredicate), languagePredicate
				);

				final HierarchyStatistics statistics = result.getAdditionalResults(HierarchyStatistics.class);
				assertNotNull(statistics);
				assertEquals(expectedStatistics, statistics);

				return null;
			}
		);
	}

	@Nonnull
	private Integer[] getParentIds(Hierarchy categoryHierarchy, int categoryId) {
		return Stream.concat(
				categoryHierarchy
					.getParentItems(String.valueOf(categoryId))
					.stream()
					.map(it -> Integer.parseInt(it.getCode())),
				Stream.of(categoryId)
			)
			.toArray(Integer[]::new);
	}

	@Nonnull
	private HierarchyStatistics computeExpectedStatistics(SESSION session, Integer parentCategoryId, Hierarchy categoryHierarchy, List<SealedEntity> allProducts, List<SealedEntity> allCategories, Predicate<SealedEntity> filterPredicate, Predicate<SealedEntity> treePredicate) {
		final Map<Integer, SealedEntity> categoriesById = allCategories.stream()
			.collect(
				Collectors.toMap(
					EntityContract::getPrimaryKey,
					Function.identity()
				)
			);

		final Map<Integer, Integer> categoryCardinalities = new HashMap<>();
		for (SealedEntity product : allProducts) {
			if (filterPredicate.test(product)) {
				final Collection<ReferenceContract> categoryReferences = product.getReferences(Entities.CATEGORY);
				for (ReferenceContract category : categoryReferences) {
					final boolean pathValid = categoryHierarchy.getParentItems(String.valueOf(category.getReferencedEntity().getPrimaryKey()))
						.stream()
						.map(HierarchyItem::getCode)
						.map(Integer::parseInt)
						.map(categoriesById::get)
						.allMatch(treePredicate) &&
						treePredicate.test(categoriesById.get(category.getReferencedEntity().getPrimaryKey()));
					if (pathValid) {
						final int categoryId = category.getReferencedEntity().getPrimaryKey();
						final List<Integer> categoryPath = Stream.concat(
							categoryHierarchy.getParentItems(String.valueOf(categoryId))
								.stream()
								.map(it -> Integer.parseInt(it.getCode())),
							Stream.of(categoryId)
						).collect(Collectors.toList());
						for (int i = categoryPath.size() - 1; i >= 0; i--) {
							int cid = categoryPath.get(i);
							categoryCardinalities.merge(cid, 1, Integer::sum);
							if (parentCategoryId != null && cid == parentCategoryId) {
								// we have encountered requested parent
								break;
							}
						}
					}
				}
			}
		}

		final Map<Serializable, List<LevelInfo<?>>> resultIndex = new TreeMap<>();
		final LinkedList<LevelInfo<?>> levelInfo = new LinkedList<>();
		resultIndex.put(Entities.CATEGORY, levelInfo);
		final List<HierarchyItem> items = parentCategoryId == null ?
			categoryHierarchy.getRootItems() :
			Collections.singletonList(categoryHierarchy.getItem(String.valueOf(parentCategoryId)));

		for (HierarchyItem rootItem : items) {
			final int categoryId = Integer.parseInt(rootItem.getCode());
			final Integer cardinality = categoryCardinalities.get(categoryId);
			if (cardinality != null) {
				final SealedEntity category = fetchHierarchyStatisticsEntity(session, categoryId);
				levelInfo.add(new LevelInfo<>(category, cardinality, fetchLevelInfo(session, categoryId, categoryHierarchy, categoryCardinalities)));
			}
		}
		return new HierarchyStatistics(resultIndex);
	}

	private List<LevelInfo<SealedEntity>> fetchLevelInfo(SESSION session, int parentCategoryId, Hierarchy categoryHierarchy, Map<Integer, Integer> categoryCardinalities) {
		final LinkedList<LevelInfo<SealedEntity>> levelInfo = new LinkedList<>();
		for (HierarchyItem item : categoryHierarchy.getChildItems(String.valueOf(parentCategoryId))) {
			final int categoryId = Integer.parseInt(item.getCode());
			final Integer cardinality = categoryCardinalities.get(categoryId);
			if (cardinality != null) {
				final SealedEntity category = fetchHierarchyStatisticsEntity(session, categoryId);
				levelInfo.add(new LevelInfo<>(category, cardinality, fetchLevelInfo(session, categoryId, categoryHierarchy, categoryCardinalities)));
			}
		}
		return levelInfo;
	}

	private SealedEntity fetchHierarchyStatisticsEntity(SESSION session, int categoryId) {
		final List<SealedEntity> result = session.query(
			query(
				entities(Entities.CATEGORY),
				filterBy(
					and(
						language(CZECH_LOCALE),
						primaryKey(categoryId)
					)
				),
				require(entityBody(), attributes())
			),
			SealedEntity.class
		).getRecordData();
		assertEquals(1, result.size(), "Category with id " + categoryId + " was not found in czech locale!");
		return result.get(0);
	}

}
