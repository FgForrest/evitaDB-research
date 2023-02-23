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
 * This test verifies whether entities can be filtered by hierarchy constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita entity filtering by hierarchy functionality")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class EntityByHierarchyFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	private static final String THOUSAND_CATEGORIES = "ThousandCategories";
	private static final int SEED = 40;
	private final DataGenerator dataGenerator = new DataGenerator();

	@DataSet(THOUSAND_CATEGORIES)
	DataCarrier setUp(EVITA evita) {
		return evita.updateCatalog(TEST_CATALOG, session -> {
			final BiFunction<Serializable, Faker, Integer> randomEntityPicker = (entityType, faker) -> {
				final int entityCount = session.getEntityCollectionSize(entityType);
				final int primaryKey = entityCount == 0 ? 0 : faker.random().nextInt(1, entityCount);
				return primaryKey == 0 ? null : primaryKey;
			};

			final List<EntityReferenceContract> storedCategories = dataGenerator.generateEntities(
					dataGenerator.getSampleCategorySchema(
						session,
						schemaBuilder -> schemaBuilder
							.withAttribute(ATTRIBUTE_PRIORITY, Long.class, whichIs -> whichIs.filterable().sortable())
					),
					randomEntityPicker,
					SEED
				)
				.limit(100)
				.map(session::upsertEntity)
				.collect(Collectors.toList());

			session.catalog.flush();
			final List<SealedEntity> categoriesAvailable = storedCategories.stream()
				.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), attributes(), references()))
				.collect(Collectors.toList());
			return new DataCarrier(
				"originalCategoryEntities",
				categoriesAvailable,
				"categoryHierarchy",
				dataGenerator.getHierarchy(Entities.CATEGORY)
			);
		});
	}

	@DisplayName("Should return root categories")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnRootCategories(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinRootHierarchy(directRelation())),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalCategoryEntities,
					sealedEntity ->
						// category has exact level one
						categoryHierarchy.getItem(Objects.requireNonNull(sealedEntity.getPrimaryKey()).toString()).getLevel() == 1,
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return all categories")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnAllCategories(EVITA evita, List<SealedEntity> originalCategoryEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinRootHierarchy()),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalCategoryEntities,
					sealedEntity -> true,
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return all categories except specified subtrees")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnAllCategoriesExceptSpecifiedSubtrees(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		final Set<Integer> excluded = new HashSet<>(Arrays.asList(1, 7, 13, 16, 40, 55));
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinRootHierarchy(excluding(excluded.toArray(new Integer[0])))),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalCategoryEntities,
					sealedEntity ->
						// is not directly excluded node
						!excluded.contains(sealedEntity.getPrimaryKey()) &&
							// has no parent node that is in excluded set
							categoryHierarchy.getParentItems(sealedEntity.getPrimaryKey().toString())
								.stream()
								.map(it -> Integer.parseInt(it.getCode()))
								.noneMatch(excluded::contains),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return subcategories of lower category")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnLowerLevelCategories(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinHierarchy(7, directRelation())),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				//noinspection ConstantConditions
				assertResultIs(
					originalCategoryEntities,
					sealedEntity ->
						// has direct parent node 7
						ofNullable(categoryHierarchy.getParentItem(sealedEntity.getPrimaryKey().toString()))
							.map(it -> Objects.equals(it.getCode(), String.valueOf(7)))
							.orElse(false),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return subcategories in selected subtree")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnAllChildCategories(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinHierarchy(7)),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				//noinspection ConstantConditions
				assertResultIs(
					originalCategoryEntities,
					sealedEntity ->
						// is either node 7
						Objects.equals(sealedEntity.getPrimaryKey().toString(), String.valueOf(7)) ||
							// or has parent node 7
							categoryHierarchy.getParentItems(sealedEntity.getPrimaryKey().toString())
								.stream()
								.anyMatch(it -> Objects.equals(it.getCode(), String.valueOf(7))),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return subcategories in selected subtree excluding root node")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnOnlyChildCategories(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinHierarchy(7, excludingRoot())),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				//noinspection ConstantConditions
				assertResultIs(
					originalCategoryEntities,
					sealedEntity ->
						// is not exactly node 7
						!Objects.equals(sealedEntity.getPrimaryKey().toString(), String.valueOf(7)) &&
							// but has parent node 7
							categoryHierarchy.getParentItems(sealedEntity.getPrimaryKey().toString())
								.stream()
								.anyMatch(it -> Objects.equals(it.getCode(), String.valueOf(7))),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return subtree categories except specified subtrees")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCategorySubtreeExceptSpecifiedSubtrees(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		final Set<Integer> excluded = new HashSet<>(Arrays.asList(2, 43, 34, 53));
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(withinHierarchy(1, excluding(excluded.toArray(new Integer[0])))),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalCategoryEntities,
					sealedEntity -> {
						final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(sealedEntity.getPrimaryKey().toString());
						return
							// is not directly excluded node
							!excluded.contains(sealedEntity.getPrimaryKey()) &&
								// has no excluded parent node
								parentItems
									.stream()
									.map(it -> Integer.parseInt(it.getCode()))
									.noneMatch(excluded::contains) &&
								// has parent node 1
								(
									Objects.equals(1, sealedEntity.getPrimaryKey()) ||
										parentItems
											.stream()
											.anyMatch(it -> Objects.equals(String.valueOf(1), it.getCode()))
								);
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return subtree categories except specified subtrees and matching attribute filter")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCategorySubtreeExceptSpecifiedSubtreesAndMatchingCertainConstraint(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		final Set<Integer> excluded = new HashSet<>(Arrays.asList(2, 34));
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(
							and(
								or(
									lessThan(ATTRIBUTE_PRIORITY, 25000),
									startsWith(ATTRIBUTE_CODE, "E")
								),
								withinHierarchy(1, excluding(excluded.toArray(new Integer[0])))
							)
						),
						require(page(1, Integer.MAX_VALUE))
					),
					EntityReference.class
				);

				assertResultIs(
					originalCategoryEntities,
					sealedEntity -> {
						final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(sealedEntity.getPrimaryKey().toString());
						return
							// attribute condition matches
							(
								ofNullable(sealedEntity.getAttribute(ATTRIBUTE_CODE)).map(it -> ((String) it).startsWith("E")).orElse(false) ||
									ofNullable(sealedEntity.getAttribute(ATTRIBUTE_PRIORITY)).map(it -> ((Long) it) < 25000).orElse(false)
							) &&
								// is not directly excluded node
								!excluded.contains(sealedEntity.getPrimaryKey()) &&
								// has no excluded parent node
								parentItems
									.stream()
									.map(it -> Integer.parseInt(it.getCode()))
									.noneMatch(excluded::contains) &&
								// has parent node 1
								(
									Objects.equals(1, sealedEntity.getPrimaryKey()) ||
										parentItems
											.stream()
											.anyMatch(it -> Objects.equals(String.valueOf(1), it.getCode()))
								);
					},
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return category parents for returned categories when only primary keys are returned")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCategoryParentsForReturnedCategoriesWhenOnlyPrimaryKeysAreReturned(EVITA evita, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						require(
							// wants obviously non-existed page to test evita that returns first page.
							page(94, Integer.MAX_VALUE),
							parents()
						)
					),
					EntityReference.class
				);

				assertFalse(result.getRecordData().isEmpty());

				final Parents parents = result.getAdditionalResults(Parents.class);
				assertNotNull(parents, "No parents DTO was returned!");
				final ParentsByType<Integer> categoryParents = parents.ofType(Entities.CATEGORY, Integer.class);

				// all results should start with same parents when we query by hierarchy
				for (EntityReference entityReference : result.getRecordData()) {
					final Integer[] relatedParents = categoryParents.getParentsFor(entityReference.getPrimaryKey());
					final Integer[] parentIds = categoryHierarchy
						.getParentItems(String.valueOf(entityReference.getPrimaryKey()))
						.stream()
						.map(it -> Integer.parseInt(it.getCode()))
						.toArray(Integer[]::new);
					if (relatedParents == null) {
						assertEquals(0, parentIds.length);
					} else {
						assertArrayEquals(
							parentIds,
							relatedParents
						);
					}
				}

				return null;
			}
		);
	}

	@DisplayName("Should return category parents for returned categories")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCategoryParentsForReturnedCategories(EVITA evita, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.CATEGORY),
						require(
							// wants obviously non-existed page to test evita that returns first page.
							page(94, Integer.MAX_VALUE),
							entityBody(),
							parents()
						)
					),
					SealedEntity.class
				);

				assertFalse(result.getRecordData().isEmpty());

				final Parents parents = result.getAdditionalResults(Parents.class);
				assertNotNull(parents, "No parents DTO was returned!");
				final ParentsByType<Integer> categoryParents = parents.ofType(Entities.CATEGORY, Integer.class);

				// all results should start with same parents when we query by hierarchy
				for (SealedEntity entity : result.getRecordData()) {
					final Integer[] relatedParents = categoryParents.getParentsFor(entity.getPrimaryKey());
					final Integer[] parentIds = categoryHierarchy
						.getParentItems(String.valueOf(entity.getPrimaryKey()))
						.stream()
						.map(it -> Integer.parseInt(it.getCode()))
						.toArray(Integer[]::new);
					if (relatedParents == null) {
						assertEquals(0, parentIds.length);
					} else {
						assertArrayEquals(
							parentIds,
							relatedParents
						);
					}
				}

				return null;
			}
		);
	}

	@DisplayName("Should return category parent bodies for returned categories")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCategoryParentBodiesForReturnedCategories(EVITA evita, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> result = session.query(
					query(
						entities(Entities.CATEGORY),
						require(
							fullEntityAnd(
								// wants obviously non-existed page to test evita that returns first page.
								page(94, Integer.MAX_VALUE),
								parents(entityBody())
							)
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
					final SealedEntity[] relatedParents = categoryParents.getParentsFor(entity.getPrimaryKey());
					final Integer[] parentIds = categoryHierarchy
						.getParentItems(String.valueOf(entity.getPrimaryKey()))
						.stream()
						.map(it -> Integer.parseInt(it.getCode()))
						.toArray(Integer[]::new);
					if (relatedParents == null) {
						assertEquals(0, parentIds.length);
					} else {
						final Integer[] relatedParentIds = Arrays.stream(relatedParents)
							.map(EntityContract::getPrimaryKey)
							.toArray(Integer[]::new);
						assertArrayEquals(
							parentIds,
							relatedParentIds
						);
					}
				}

				return null;
			}
		);
	}

	@DisplayName("Should return cardinalities for categories")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCardinalitiesForCategories(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(
							language(CZECH_LOCALE)
						),
						require(
							// we don't need the results whatsoever
							page(1, 0),
							// we need only data about cardinalities
							hierarchyStatistics(entityBody(), attributes())
						)
					),
					EntityReference.class
				);

				final Predicate<SealedEntity> languagePredicate = it -> it.getLocales().contains(CZECH_LOCALE);
				final HierarchyStatistics expectedStatistics = computeExpectedStatistics(
					session, null, categoryHierarchy, originalCategoryEntities,
					languagePredicate, languagePredicate
				);

				final HierarchyStatistics statistics = result.getAdditionalResults(HierarchyStatistics.class);
				assertNotNull(statistics);
				assertEquals(expectedStatistics, statistics);

				return null;
			}
		);
	}

	@DisplayName("Should return cardinalities for categories in subtree")
	@UseDataSet(THOUSAND_CATEGORIES)
	@Test
	void shouldReturnCardinalitiesForCategoriesWhenSubTreeIsRequested(EVITA evita, List<SealedEntity> originalCategoryEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.CATEGORY),
						filterBy(
							and(
								language(CZECH_LOCALE),
								withinHierarchy(Entities.CATEGORY, 2)
							)
						),
						require(
							// we don't need the results whatsoever
							page(1, 0),
							// we need only data about cardinalities
							hierarchyStatistics(entityBody(), attributes())
						)
					),
					EntityReference.class
				);

				final Predicate<SealedEntity> languagePredicate = it -> it.getLocales().contains(CZECH_LOCALE);
				final Predicate<SealedEntity> categoryPredicate = sealedEntity -> {
					final Integer categoryId = sealedEntity.getPrimaryKey();
					final String categoryIdAsString = String.valueOf(categoryId);
					final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(categoryIdAsString);
					// has parent node 2
					return (
						Objects.equals(2, categoryId) ||
							parentItems
								.stream()
								.anyMatch(it -> Objects.equals(String.valueOf(2), it.getCode()))
					);
				};
				final HierarchyStatistics expectedStatistics = computeExpectedStatistics(
					session, 2, categoryHierarchy, originalCategoryEntities,
					languagePredicate.and(categoryPredicate), languagePredicate
				);

				final HierarchyStatistics statistics = result.getAdditionalResults(HierarchyStatistics.class);
				assertNotNull(statistics);
				assertEquals(expectedStatistics, statistics);

				return null;
			}
		);
	}

	@Nonnull
	private HierarchyStatistics computeExpectedStatistics(SESSION session, Integer parentCategoryId, Hierarchy categoryHierarchy, List<SealedEntity> allCategories, Predicate<SealedEntity> filterPredicate, Predicate<SealedEntity> treePredicate) {
		final Map<Integer, SealedEntity> categoriesById = allCategories.stream()
			.collect(
				Collectors.toMap(
					EntityContract::getPrimaryKey,
					Function.identity()
				)
			);

		final Map<Integer, Integer> categoryCardinalities = new HashMap<>();
		for (SealedEntity category : allCategories) {
			if (filterPredicate.test(category)) {
				final boolean pathValid = categoryHierarchy.getParentItems(String.valueOf(category.getPrimaryKey()))
					.stream()
					.map(HierarchyItem::getCode)
					.map(Integer::parseInt)
					.map(categoriesById::get)
					.allMatch(treePredicate);
				if (pathValid) {
					final int categoryId = category.getPrimaryKey();
					final List<Integer> categoryPath = Stream.concat(
						categoryHierarchy.getParentItems(String.valueOf(categoryId))
							.stream()
							.map(it -> Integer.parseInt(it.getCode())),
						Stream.of(categoryId)
					).collect(Collectors.toList());
					for (int i = categoryPath.size() - 1; i >= 0; i--) {
						int cid = categoryPath.get(i);
						if (cid == categoryId) {
							categoryCardinalities.merge(cid, 0, Integer::sum);
						} else {
							categoryCardinalities.merge(cid, 1, Integer::sum);
						}
						if (parentCategoryId != null && cid == parentCategoryId) {
							// we have encountered requested parent
							break;
						}
					}
				}
			}
		}

		final Map<Serializable, List<LevelInfo<?>>> resultIndex = new HashMap<>();
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
		return session.query(
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
		).getRecordData().get(0);
	}

}
