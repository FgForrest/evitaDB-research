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
import io.evitadb.api.io.extraResult.FacetSummary;
import io.evitadb.api.io.extraResult.FacetSummary.FacetGroupStatistics;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.io.extraResult.FacetSummary.RequestImpact;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.require.FacetGroupsConjunction;
import io.evitadb.api.query.require.FacetGroupsDisjunction;
import io.evitadb.api.query.require.FacetGroupsNegation;
import io.evitadb.api.query.require.FacetStatisticsDepth;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.schema.ReferenceSchemaBuilder;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.test.Entities;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.extension.DataCarrier;
import io.evitadb.test.generator.DataGenerator;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.api.query.QueryUtils.*;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static io.evitadb.test.generator.DataGenerator.ATTRIBUTE_QUANTITY;
import static io.evitadb.utils.AssertionUtils.assertResultIs;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies whether entities can be filtered by facets.
 *
 * TOBEDONE JNO - add tests that contains also priceBetween / other attribute filter inside user filter and check that
 * they're affecting impact counts
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita entity filtering by facets functionality")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class EntityByFacetFilteringFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	private static final String THOUSAND_PRODUCTS_WITH_FACETS = "ThousandsProductsWithFacets";
	private static final int SEED = 40;
	private final DataGenerator dataGenerator = new DataGenerator();

	@DataSet(THOUSAND_PRODUCTS_WITH_FACETS)
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

			dataGenerator.generateEntities(
					dataGenerator.getSampleParameterGroupSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(15)
				.forEach(session::upsertEntity);

			dataGenerator.generateEntities(
					dataGenerator.getSampleParameterSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(200)
				.forEach(session::upsertEntity);

			final EntitySchema productSchema = dataGenerator.getSampleProductSchema(
				session,
				schemaBuilder -> schemaBuilder
					.withReferenceToEntity(Entities.BRAND, ReferenceSchemaBuilder::faceted)
					.withReferenceToEntity(Entities.STORE, ReferenceSchemaBuilder::faceted)
					.withReferenceToEntity(Entities.CATEGORY, ReferenceSchemaBuilder::faceted)
					.withReferenceToEntity(Entities.PARAMETER, thatIs -> thatIs.faceted().withGroupTypeRelatedToEntity(Entities.PARAMETER_GROUP))
			);
			final List<EntityReferenceContract> storedProducts = dataGenerator.generateEntities(
					productSchema,
					randomEntityPicker,
					SEED
				)
				.limit(1000)
				.map(session::upsertEntity)
				.collect(toList());
			session.catalog.flush();

			return new DataCarrier(
				"originalCategoryEntities",
				storedProducts.stream()
					.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), attributes(), references()))
					.collect(toList()),
				"categoryHierarchy",
				dataGenerator.getHierarchy(Entities.CATEGORY),
				"productSchema",
				productSchema,
				"parameterGroupMapping",
				dataGenerator.getParameterIndex()
			);
		});
	}

	@DisplayName("Should return products matching random facet")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetInEntireSet(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Random rnd = new Random(SEED);
				for (Entities entityType : EnumSet.of(Entities.CATEGORY, Entities.BRAND, Entities.STORE)) {
					final int entityCount = session.getEntityCollectionSize(entityType);
					// for each entity execute 100 pseudo random queries
					for (int i = 0; i < 100; i++) {
						final int numberOfSelectedFacets = 1 + rnd.nextInt(5);
						final Integer[] facetIds = new Integer[numberOfSelectedFacets];
						for (int j = 0; j < numberOfSelectedFacets; j++) {
							final int primaryKey = rnd.nextInt(entityCount - 1) + 1;
							facetIds[j] = primaryKey;
						}

						final EvitaResponseBase<EntityReference> result = session.query(
							query(
								entities(Entities.PRODUCT),
								filterBy(
									userFilter(
										facet(entityType, facetIds)
									)
								),
								require(
									page(1, Integer.MAX_VALUE)
								)
							),
							EntityReference.class
						);

						final Set<Integer> selectedIdsAsSet = new HashSet<>(Arrays.asList(facetIds));
						assertResultIs(
							"Querying " + entityType + " facets: " + Arrays.toString(facetIds),
							originalProductEntities,
							sealedEntity -> sealedEntity
								.getReferences(entityType)
								.stream()
								.map(it -> it.getReferencedEntity().getPrimaryKey())
								.anyMatch(selectedIdsAsSet::contains),
							result.getRecordData()
						);
					}
				}
				return null;
			}
		);
	}

	@DisplayName("Should return products matching group AND combination of facet")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetGroupAndCombinationInEntireSet(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Integer[] parameters = getParametersWithDifferentGroups(originalProductEntities, new HashSet<>());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							userFilter(
								facet(Entities.PARAMETER, parameters)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);

				final Set<Integer> selectedIdsAsSet = Arrays.stream(parameters).collect(Collectors.toSet());
				assertResultIs(
					"Querying " + Entities.PARAMETER + " facets: " + Arrays.toString(parameters),
					originalProductEntities,
					sealedEntity -> selectedIdsAsSet
						.stream()
						.allMatch(parameterId -> sealedEntity.getReference(Entities.PARAMETER, parameterId) != null),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products matching group OR combination of facet")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetGroupOrCombinationInEntireSet(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final HashSet<Integer> groups = new HashSet<>();
				final Integer[] parameters = getParametersWithDifferentGroups(originalProductEntities, groups);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							userFilter(
								facet(Entities.PARAMETER, parameters)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							facetGroupsDisjunction(Entities.PARAMETER_GROUP, groups.toArray(new Integer[0]))
						)
					),
					EntityReference.class
				);

				final Set<Integer> selectedIdsAsSet = Arrays.stream(parameters).collect(Collectors.toSet());
				assertResultIs(
					"Querying " + Entities.PARAMETER + " facets: " + Arrays.toString(parameters),
					originalProductEntities,
					sealedEntity -> selectedIdsAsSet
						.stream()
						.anyMatch(parameterId -> sealedEntity.getReference(Entities.PARAMETER, parameterId) != null),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products matching group NOT combination of facet")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetGroupNotCombinationInEntireSet(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Set<Integer> groups = getGroupsWithGaps(originalProductEntities);
				final Integer[] parameters = getParametersInGroups(originalProductEntities, groups);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							userFilter(
								facet(Entities.PARAMETER, parameters)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							facetGroupsNegation(Entities.PARAMETER_GROUP, groups.toArray(new Integer[0]))
						)
					),
					EntityReference.class
				);

				final Set<Integer> selectedIdsAsSet = Arrays.stream(parameters).collect(Collectors.toSet());
				assertResultIs(
					"Querying " + Entities.PARAMETER + " facets: " + Arrays.toString(parameters),
					originalProductEntities,
					sealedEntity -> selectedIdsAsSet
						.stream()
						.noneMatch(parameterId -> sealedEntity.getReference(Entities.PARAMETER, parameterId) != null),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products matching AND combination of facet")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetAndCombinationInEntireSet(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final HashSet<Integer> groups = new HashSet<>();
				final Integer[] parameters = getParametersWithSameGroup(originalProductEntities, groups);
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							userFilter(
								facet(Entities.PARAMETER, parameters)
							)
						),
						require(
							page(1, Integer.MAX_VALUE),
							facetGroupsConjunction(Entities.PARAMETER_GROUP, groups.toArray(new Integer[0]))
						)
					),
					EntityReference.class
				);

				final Set<Integer> selectedIdsAsSet = Arrays.stream(parameters).collect(Collectors.toSet());
				assertResultIs(
					"Querying " + Entities.PARAMETER + " facets: " + Arrays.toString(parameters),
					originalProductEntities,
					sealedEntity -> selectedIdsAsSet
						.stream()
						.allMatch(parameterId -> sealedEntity.getReference(Entities.PARAMETER, parameterId) != null),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products matching OR combination of facet")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetOrCombinationInEntireSet(EVITA evita, List<SealedEntity> originalProductEntities) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Integer[] parameters = getParametersWithSameGroup(originalProductEntities, new HashSet<>());
				final EvitaResponseBase<EntityReference> result = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							userFilter(
								facet(Entities.PARAMETER, parameters)
							)
						),
						require(
							page(1, Integer.MAX_VALUE)
						)
					),
					EntityReference.class
				);

				final Set<Integer> selectedIdsAsSet = Arrays.stream(parameters).collect(Collectors.toSet());
				assertResultIs(
					"Querying " + Entities.PARAMETER + " facets: " + Arrays.toString(parameters),
					originalProductEntities,
					sealedEntity -> selectedIdsAsSet
						.stream()
						.anyMatch(parameterId -> sealedEntity.getReference(Entities.PARAMETER, parameterId) != null),
					result.getRecordData()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return products matching random facet within hierarchy tree")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnProductsWithSpecifiedFacetInHierarchyTree(EVITA evita, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Random rnd = new Random(SEED);
				final int categoryCount = session.getEntityCollectionSize(Entities.CATEGORY);
				for (Entities entityType : EnumSet.of(Entities.CATEGORY, Entities.BRAND, Entities.STORE)) {
					final int entityCount = session.getEntityCollectionSize(entityType);
					// for each entity execute 100 pseudo random queries
					for (int i = 0; i < 100; i++) {
						final int numberOfSelectedFacets = rnd.nextInt(entityCount - 1) + 1;
						final Integer[] facetIds = new Integer[numberOfSelectedFacets];
						for (int j = 0; j < numberOfSelectedFacets; j++) {
							int primaryKey;
							do {
								primaryKey = rnd.nextInt(entityCount - 1) + 1;
							} while (ArrayUtils.contains(facetIds, primaryKey));
							facetIds[j] = primaryKey;
						}

						final int hierarchyRoot = rnd.nextInt(categoryCount - 1) + 1;
						final EvitaResponseBase<EntityReference> result = session.query(
							query(
								entities(Entities.PRODUCT),
								filterBy(
									and(
										withinHierarchy(Entities.CATEGORY, hierarchyRoot),
										userFilter(
											facet(entityType, facetIds)
										)
									)
								),
								require(
									page(1, Integer.MAX_VALUE)
								)
							),
							EntityReference.class
						);

						final Set<Integer> selectedIdsAsSet = new HashSet<>(Arrays.asList(facetIds));
						assertResultIs(
							"Iteration #" + (i + 1) + ". Querying " + entityType + " facets in hierarchy root " + hierarchyRoot + ": " + Arrays.toString(facetIds),
							originalProductEntities,
							sealedEntity -> {
								// is within requested hierarchy
								final boolean isWithinHierarchy = sealedEntity.getReferences(Entities.CATEGORY)
									.stream()
									.anyMatch(it -> it.getReferencedEntity().getPrimaryKey() == hierarchyRoot ||
										categoryHierarchy.getParentItems(String.valueOf(it.getReferencedEntity().getPrimaryKey()))
											.stream()
											.anyMatch(catId -> hierarchyRoot == Integer.parseInt(catId.getCode()))
									);
								// has the facet
								final boolean hasFacet = sealedEntity.getReferences(entityType)
									.stream()
									.map(it -> it.getReferencedEntity().getPrimaryKey())
									.anyMatch(selectedIdsAsSet::contains);
								return isWithinHierarchy && hasFacet;
							},
							result.getRecordData()
						);
					}
				}
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary for entire set")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForEntireSet(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Query query = query(
					entities(Entities.PRODUCT),
					require(
						facetSummary()
					)
				);
				final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
				final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);

				final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
					productSchema,
					originalProductEntities,
					null,
					query,
					FacetStatisticsDepth.COUNTS,
					parameterGroupMapping
				);

				assertEquals(
					expectedSummary.getFacetSummary(),
					actualFacetSummary,
					"Filtered entity count: " + expectedSummary.getEntityCount()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary for filtered set")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForFilteredSet(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Query query = query(
					entities(Entities.PRODUCT),
					filterBy(
						greaterThan(ATTRIBUTE_QUANTITY, 970)
					),
					require(
						facetSummary()
					)
				);
				final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
				final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);

				final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
					productSchema,
					originalProductEntities,
					it -> ofNullable((BigDecimal) it.getAttribute(ATTRIBUTE_QUANTITY))
						.map(attr -> attr.compareTo(new BigDecimal("970")) > 0)
						.orElse(false),
					query,
					FacetStatisticsDepth.COUNTS,
					parameterGroupMapping
				);

				assertEquals(
					expectedSummary.getFacetSummary(),
					actualFacetSummary,
					"Filtered entity count: " + expectedSummary.getEntityCount()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary for facet filtered set")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForFacetFilteredSet(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Query query = query(
					entities(Entities.PRODUCT),
					filterBy(
						and(
							greaterThan(ATTRIBUTE_QUANTITY, 950),
							userFilter(
								facet(Entities.BRAND, 1, 2, 3),
								facet(Entities.STORE, 5, 6, 7, 8),
								facet(Entities.CATEGORY, 8)
							)
						)
					),
					require(
						facetSummary()
					)
				);
				final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
				final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);

				final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
					productSchema,
					originalProductEntities,
					it -> ofNullable((BigDecimal) it.getAttribute(ATTRIBUTE_QUANTITY))
						.map(attr -> attr.compareTo(new BigDecimal("950")) > 0)
						.orElse(false),
					query,
					FacetStatisticsDepth.COUNTS,
					parameterGroupMapping
				);

				assertEquals(
					expectedSummary.getFacetSummary(),
					actualFacetSummary,
					"Filtered entity count: " + expectedSummary.getEntityCount()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary for hierarchy tree")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTree(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Integer[] excludedSubTrees = {2, 10};
				final Query query = query(
					entities(Entities.PRODUCT),
					filterBy(
						and(
							withinHierarchy(Entities.CATEGORY, 1, excluding(excludedSubTrees)),
							userFilter(
								facet(Entities.BRAND, 1),
								facet(Entities.STORE, 5, 6, 7, 8),
								facet(Entities.CATEGORY, 8, 9)
							)
						)
					),
					require(
						facetSummary()
					)
				);
				final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
				final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);
				final Set<Integer> excluded = new HashSet<>(Arrays.asList(excludedSubTrees));

				final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
					productSchema,
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
					query,
					FacetStatisticsDepth.COUNTS,
					parameterGroupMapping
				);

				assertEquals(
					expectedSummary.getFacetSummary(),
					actualFacetSummary,
					"Filtered entity count: " + expectedSummary.getEntityCount()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary for hierarchy with statistics")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTreeWithStatistics(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Query query = query(
					entities(Entities.PRODUCT),
					filterBy(
						and(
							withinHierarchy(Entities.CATEGORY, 2),
							userFilter(
								facet(Entities.BRAND, 1),
								facet(Entities.STORE, 5)
							)
						)
					),
					require(
						facetSummary(FacetStatisticsDepth.IMPACT)
					)
				);
				final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
				final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);

				final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
					productSchema,
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> isWithinHierarchy(categoryHierarchy, category, 2)),
					query,
					FacetStatisticsDepth.IMPACT,
					parameterGroupMapping
				);

				assertEquals(
					expectedSummary.getFacetSummary(),
					actualFacetSummary,
					"Filtered entity count: " + expectedSummary.getEntityCount()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary with parameter selection for hierarchy with statistics")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTreeAndParameterFacetWithStatistics(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final int allParametersWithinOneGroupResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 4, 31, 100
				);
				final int parametersInDifferentGroupsResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 4, 31, 100, 102
				);
				assertTrue(
					parametersInDifferentGroupsResult < allParametersWithinOneGroupResult,
					"When parameter from different group is selected - result count must decrease."
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary for hierarchy with statistics and inverted inter facet relation")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTreeWithStatisticsAndInvertedInterFacetRelation(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final Query query = query(
					entities(Entities.PRODUCT),
					filterBy(
						and(
							withinHierarchy(Entities.CATEGORY, 2),
							userFilter(
								facet(Entities.STORE, 5)
							)
						)
					),
					require(
						facetSummary(FacetStatisticsDepth.IMPACT),
						facetGroupsConjunction(Entities.STORE, 5)
					)
				);
				final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
				final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);

				final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
					productSchema,
					originalProductEntities,
					sealedEntity -> sealedEntity
						.getReferences(Entities.CATEGORY)
						.stream()
						.anyMatch(category -> isWithinHierarchy(categoryHierarchy, category, 2)),
					query,
					FacetStatisticsDepth.IMPACT,
					parameterGroupMapping
				);

				assertEquals(
					expectedSummary.getFacetSummary(),
					actualFacetSummary,
					"Filtered entity count: " + expectedSummary.getEntityCount()
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary with parameter selection for hierarchy with statistics with inverted inter facet relation")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTreeAndParameterFacetWithStatisticsAndInvertedInterFacetRelation(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final int singleParameterSelectedResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 1
				);
				final int twoParametersFromSameGroupResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 1, 51
				);
				assertTrue(
					twoParametersFromSameGroupResult > singleParameterSelectedResult,
					"When selecting multiple parameters from same group it should increase the result"
				);
				final int singleParameterSelectedResultInverted = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, facetGroupsConjunction(Entities.PARAMETER_GROUP, 1), 1
				);
				final int twoParametersFromSameGroupResultInverted = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, facetGroupsConjunction(Entities.PARAMETER_GROUP, 1), 1, 51
				);
				assertTrue(
					twoParametersFromSameGroupResultInverted < singleParameterSelectedResultInverted,
					"When certain parameter group relation is inverted to AND, selecting multiple parameters from it should decrease the result"
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary with parameter selection for hierarchy with statistics with inverted facet group relation")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTreeAndParameterFacetWithStatisticsAndInvertedFacetGroupRelation(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final int singleParameterSelectedResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 1
				);
				final int twoParametersFromSameGroupResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 1, 7
				);
				assertTrue(
					twoParametersFromSameGroupResult < singleParameterSelectedResult,
					"When selecting multiple parameters from their groups should decrease the result"
				);
				final int singleParameterSelectedResultWithOr = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, facetGroupsDisjunction(Entities.PARAMETER_GROUP, 2), 1
				);
				final int twoParametersFromSameGroupResultWithOr = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, facetGroupsDisjunction(Entities.PARAMETER_GROUP, 2), 1, 7
				);
				assertTrue(
					twoParametersFromSameGroupResultWithOr > singleParameterSelectedResultWithOr,
					"When certain parameter group relation is inverted to OR, selecting multiple parameters from their groups should increase the result"
				);
				return null;
			}
		);
	}

	@DisplayName("Should return facet summary with parameter selection for hierarchy with statistics with negated meaning of group")
	@UseDataSet(THOUSAND_PRODUCTS_WITH_FACETS)
	@Test
	void shouldReturnFacetSummaryForHierarchyTreeAndParameterFacetWithStatisticsAndNegatedGroupImpact(EVITA evita, EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final int singleParameterSelectedResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, null, 1
				);
				final int twoParametersFromSameGroupResult = queryParameterFacets(
					productSchema, originalProductEntities, categoryHierarchy, parameterGroupMapping, session, facetGroupsNegation(Entities.PARAMETER_GROUP, 1), 1
				);
				assertTrue(
					twoParametersFromSameGroupResult > singleParameterSelectedResult,
					"When same parameter constraint is inverted to negative fashion, it must return more results"
				);
				return null;
			}
		);
	}

	/**
	 * Simplification method that executes query with facet computation and returns how many record matches the query
	 * that filters over input parameter facet ids.
	 */
	private int queryParameterFacets(EntitySchema productSchema, List<SealedEntity> originalProductEntities, Hierarchy categoryHierarchy, Map<Integer, Integer> parameterGroupMapping, SESSION session, RequireConstraint additionalRequirement, Integer... facetIds) {
		final Query query = query(
			entities(Entities.PRODUCT),
			filterBy(
				and(
					withinHierarchy(Entities.CATEGORY, 2),
					userFilter(
						facet(Entities.PARAMETER, facetIds)
					)
				)
			),
			require(
				facetSummary(FacetStatisticsDepth.IMPACT),
				additionalRequirement
			)
		);
		final EvitaResponseBase<EntityReference> result = session.query(query, EntityReference.class);
		final FacetSummary actualFacetSummary = result.getAdditionalResults(FacetSummary.class);

		final FacetSummaryWithResultCount expectedSummary = computeFacetSummary(
			productSchema,
			originalProductEntities,
			sealedEntity -> sealedEntity
				.getReferences(Entities.CATEGORY)
				.stream()
				.anyMatch(category -> isWithinHierarchy(categoryHierarchy, category, 2)),
			query,
			FacetStatisticsDepth.IMPACT,
			parameterGroupMapping
		);

		assertEquals(expectedSummary.getEntityCount(), result.getTotalRecordCount());
		assertEquals(
			expectedSummary.getFacetSummary(),
			actualFacetSummary,
			"Filtered entity count: " + expectedSummary.getEntityCount()
		);

		return result.getTotalRecordCount();
	}

	private boolean isWithinHierarchy(Hierarchy categoryHierarchy, ReferenceContract category, int requestedCategoryId) {
		final int categoryId = category.getReferencedEntity().getPrimaryKey();
		final String categoryIdAsString = String.valueOf(categoryId);
		final List<HierarchyItem> parentItems = categoryHierarchy.getParentItems(categoryIdAsString);
		// has parent node or requested category id
		return Objects.equals(requestedCategoryId, categoryId) ||
			parentItems
				.stream()
				.anyMatch(it -> Objects.equals(String.valueOf(requestedCategoryId), it.getCode()));
	}

	/**
	 * Computes facet summary by streamed fashion.
	 */
	private static FacetSummaryWithResultCount computeFacetSummary(@Nonnull EntitySchema schema, @Nonnull List<SealedEntity> entities, @Nullable Predicate<SealedEntity> entityFilter, @Nonnull Query query, @Nonnull FacetStatisticsDepth statisticsDepth, @Nonnull Map<Integer, Integer> parameterGroupMapping) {
		// this context allows us to create facet filtering predicates in correct way
		final FacetComputationalContext fcc = new FacetComputationalContext(schema, query, parameterGroupMapping);

		// filter entities by mandatory predicate
		final List<SealedEntity> filteredEntities = ofNullable(entityFilter)
			.map(it -> entities.stream().filter(it))
			.orElseGet(entities::stream)
			.collect(toList());

		// collect set of faceted reference types
		final Set<Serializable> facetedEntities = schema.getReferences()
			.values()
			.stream()
			.filter(ReferenceSchema::isFaceted)
			.map(ReferenceSchema::getEntityType)
			.collect(Collectors.toSet());

		// group facets by their entity type / group
		final Map<GroupReference, Map<EntityReferenceContract, Integer>> groupedFacets = filteredEntities
			.stream()
			.flatMap(it -> it.getReferences().stream())
			// filter out not faceted entity types
			.filter(it -> facetedEntities.contains(it.getReferencedEntity().getType()))
			.collect(
				groupingBy(
					// create referenced entity type + referenced entity group id key
					it -> new GroupReference(
						ofNullable(schema.getReference(it.getReferencedEntity().getType()).getGroupType()).orElseGet(() -> it.getReferencedEntity().getType()),
						ofNullable(it.getGroup()).map(EntityReference::getPrimaryKey).orElse(null)
					),
					TreeMap::new,
					// compute facet count
					groupingBy(
						ReferenceContract::getReferencedEntity,
						TreeMap::new,
						summingInt(facet -> 1)
					)
				)
			);

		// filter entities by facets in input query (even if part of user filter) - use AND for different entity types, and OR for facet ids
		final Set<Integer> facetFilteredEntityIds = filteredEntities
			.stream()
			.filter(fcc.createBaseFacetPredicate())
			.map(EntityContract::getPrimaryKey)
			.collect(Collectors.toSet());

		// if there facet group negation - invert the facet counts
		if (fcc.isAnyFacetGroupNegated()) {
			groupedFacets.entrySet()
				.stream()
				.filter(it -> fcc.isFacetGroupNegated(it.getKey()))
				.forEach(it ->
					// invert the results
					it.getValue()
						.entrySet()
						.forEach(facetCount -> facetCount.setValue(filteredEntities.size() - facetCount.getValue()))
				);
		}

		return new FacetSummaryWithResultCount(
			facetFilteredEntityIds.size(),
			new FacetSummary(
				groupedFacets
					.entrySet()
					.stream()
					.map(it ->
						new FacetGroupStatistics(
							it.getKey().getEntityType(),
							it.getKey().getGroupId(),
							it.getValue()
								.entrySet()
								.stream()
								.map(facet -> {
									// compute whether facet was part of input filter by
									final boolean requested = fcc.wasFacetRequested(facet.getKey());
									// create facet statistics
									return new FacetStatistics(
										facet.getKey().getPrimaryKey(),
										requested,
										facet.getValue(),
										statisticsDepth == FacetStatisticsDepth.IMPACT ?
											computeImpact(filteredEntities, facetFilteredEntityIds, facet.getKey(), fcc) : null
									);
								})
								.collect(toList())
						)
					)
					.collect(toList())
			)
		);
	}

	private static RequestImpact computeImpact(@Nonnull List<SealedEntity> filteredEntities, @Nonnull Set<Integer> filteredEntityIds, @Nonnull EntityReferenceContract facet, @Nonnull FacetComputationalContext fcc) {
		// on already filtered entities
		final Set<Integer> newResult = filteredEntities.stream()
			// apply newly created predicate with added current facet constraint
			.filter(fcc.createTestFacetPredicate(facet))
			// we need only primary keys
			.map(EntityContract::getPrimaryKey)
			// in set
			.collect(Collectors.toSet());

		return new RequestImpact(
			// compute difference with base result
			newResult.size() - filteredEntityIds.size(),
			// pass new result count
			newResult.size()
		);
	}

	@Nonnull
	private Integer[] getParametersWithDifferentGroups(List<SealedEntity> originalProductEntities, Set<Integer> groups) {
		final SealedEntity exampleProduct = originalProductEntities
			.stream()
			.filter(it -> it.getReferences(Entities.PARAMETER)
				.stream()
				.filter(x -> x.getGroup() != null)
				.map(x -> x.getGroup().getPrimaryKey())
				.distinct()
				.count() >= 2
			)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("There is no product with two references to parameters in different groups!"));
		return exampleProduct.getReferences(Entities.PARAMETER)
			.stream()
			.filter(it -> it.getGroup() != null)
			.filter(it -> groups.add(it.getGroup().getPrimaryKey()))
			.map(it -> it.getReferencedEntity().getPrimaryKey())
			.toArray(Integer[]::new);
	}

	@Nonnull
	private Integer[] getParametersWithSameGroup(List<SealedEntity> originalProductEntities, Set<Integer> groups) {
		return originalProductEntities
			.stream()
			.map(it -> {
				final Integer groupWithMultipleItems = it.getReferences(Entities.PARAMETER)
					.stream()
					.filter(x -> x.getGroup() != null)
					.collect(groupingBy(x -> x.getGroup().getPrimaryKey(), Collectors.counting()))
					.entrySet()
					.stream()
					.filter(x -> x.getValue() > 2L)
					.map(Entry::getKey)
					.findFirst()
					.orElse(null);
				if (groupWithMultipleItems == null) {
					return null;
				} else {
					groups.add(groupWithMultipleItems);
					return it.getReferences(Entities.PARAMETER)
						.stream()
						.filter(x -> x.getGroup() != null && x.getGroup().getPrimaryKey() == groupWithMultipleItems)
						.map(x -> x.getReferencedEntity().getPrimaryKey())
						.toArray(Integer[]::new);
				}
			})
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("There is no product with two references to parameters in same group!"));
	}

	private Set<Integer> getGroupsWithGaps(List<SealedEntity> originalProductEntities) {
		final Set<Integer> allGroupsPresent = originalProductEntities.stream()
			.flatMap(it -> it.getReferences(Entities.PARAMETER).stream())
			.filter(it -> it.getGroup() != null)
			.map(it -> it.getGroup().getPrimaryKey())
			.collect(Collectors.toSet());
		final Set<Integer> groupsWithGaps = new HashSet<>();
		for (SealedEntity product : originalProductEntities) {
			final Set<Integer> groupsPresentOnProduct = product.getReferences(Entities.PARAMETER).stream()
				.filter(it -> it.getGroup() != null)
				.map(it -> it.getGroup().getPrimaryKey())
				.collect(Collectors.toSet());
			allGroupsPresent
				.stream()
				.filter(it -> !groupsWithGaps.contains(it) && !groupsPresentOnProduct.contains(it))
				.forEach(groupsWithGaps::add);
		}
		return groupsWithGaps;
	}

	private Integer[] getParametersInGroups(List<SealedEntity> originalProductEntities, Set<Integer> groups) {
		return originalProductEntities.stream()
			.flatMap(it -> it.getReferences(Entities.PARAMETER).stream())
			.filter(it -> it.getGroup() != null)
			.filter(it -> groups.contains(it.getGroup().getPrimaryKey()))
			.map(it -> it.getReferencedEntity().getPrimaryKey())
			.distinct()
			.toArray(Integer[]::new);
	}

	private interface FacetPredicate extends Predicate<SealedEntity> {

		Serializable getEntityType();

		Serializable getFacetGroupEntityType();

		Integer getFacetGroupId();

		int[] getFacetIds();

		FacetPredicate combine(Serializable entityType, Integer facetGroupId, int... facetIds);

	}

	private static class AndFacetPredicate implements FacetPredicate {
		@Getter private final Serializable entityType;
		@Getter private final Serializable facetGroupEntityType;
		@Getter private final Integer facetGroupId;
		@Getter private final int[] facetIds;

		public AndFacetPredicate(Serializable entityType, Serializable facetGroupEntityType, Integer facetGroupId, int... facetIds) {
			this.entityType = entityType;
			this.facetGroupEntityType = facetGroupEntityType;
			this.facetGroupId = facetGroupId;
			this.facetIds = facetIds;
		}

		@Override
		public boolean test(SealedEntity entity) {
			final Set<Integer> referenceSet = entity.getReferences(entityType)
				.stream()
				.map(it -> it.getReferencedEntity().getPrimaryKey())
				.collect(Collectors.toSet());
			// has the facet
			return Arrays.stream(facetIds).allMatch(referenceSet::contains);
		}

		@Override
		public FacetPredicate combine(Serializable entityType, Integer facetGroupId, int... facetIds) {
			Assert.isTrue(this.entityType.equals(entityType), "Sanity check!");
			Assert.isTrue(Objects.equals(this.facetGroupId, facetGroupId), "Sanity check!");
			return new AndFacetPredicate(
				entityType,
				facetGroupEntityType,
				getFacetGroupId(),
				ArrayUtils.mergeArrays(getFacetIds(), facetIds)
			);
		}

		@Override
		public String toString() {
			return getEntityType() + ofNullable(facetGroupId).map(it -> " " + it).orElse("") + " (AND):" + Arrays.toString(getFacetIds());
		}

	}

	private static class OrFacetPredicate implements FacetPredicate {
		@Getter private final Serializable entityType;
		@Getter private final Serializable facetGroupEntityType;
		@Getter private final Integer facetGroupId;
		@Getter private final int[] facetIds;

		public OrFacetPredicate(Serializable entityType, Serializable facetGroupEntityType, Integer facetGroupId, int... facetIds) {
			this.entityType = entityType;
			this.facetGroupEntityType = facetGroupEntityType;
			this.facetGroupId = facetGroupId;
			this.facetIds = facetIds;
		}

		@Override
		public boolean test(SealedEntity entity) {
			final Set<Integer> referenceSet = entity.getReferences(entityType)
				.stream()
				.map(it -> it.getReferencedEntity().getPrimaryKey())
				.collect(Collectors.toSet());
			// has the facet
			return Arrays.stream(facetIds).anyMatch(referenceSet::contains);
		}

		@Override
		public FacetPredicate combine(Serializable entityType, Integer facetGroupId, int... facetIds) {
			Assert.isTrue(this.entityType.equals(entityType), "Sanity check!");
			Assert.isTrue(Objects.equals(this.facetGroupId, facetGroupId), "Sanity check!");
			return new OrFacetPredicate(
				entityType,
				facetGroupEntityType,
				getFacetGroupId(),
				ArrayUtils.mergeArrays(getFacetIds(), facetIds)
			);
		}

		@Override
		public String toString() {
			return getEntityType() + ofNullable(facetGroupId).map(it -> " " + it).orElse("") + " (OR):" + Arrays.toString(getFacetIds());
		}

	}

	private static class NotFacetPredicate implements FacetPredicate {
		@Getter private final Serializable entityType;
		@Getter private final Serializable facetGroupEntityType;
		@Getter private final Integer facetGroupId;
		@Getter private final int[] facetIds;

		public NotFacetPredicate(Serializable entityType, Serializable facetGroupEntityType, Integer facetGroupId, int... facetIds) {
			this.entityType = entityType;
			this.facetGroupEntityType = facetGroupEntityType;
			this.facetGroupId = facetGroupId;
			this.facetIds = facetIds;
		}

		@Override
		public boolean test(SealedEntity entity) {
			final Set<Integer> referenceSet = entity.getReferences(entityType)
				.stream()
				.map(it -> it.getReferencedEntity().getPrimaryKey())
				.collect(Collectors.toSet());
			// has the facet
			return Arrays.stream(facetIds).noneMatch(referenceSet::contains);
		}

		@Override
		public FacetPredicate combine(Serializable entityType, Integer facetGroupId, int... facetIds) {
			Assert.isTrue(this.entityType.equals(entityType), "Sanity check!");
			Assert.isTrue(Objects.equals(this.facetGroupId, facetGroupId), "Sanity check!");
			return new NotFacetPredicate(
				entityType,
				facetGroupEntityType,
				getFacetGroupId(),
				ArrayUtils.mergeArrays(getFacetIds(), facetIds)
			);
		}

		@Override
		public String toString() {
			return getEntityType() + ofNullable(facetGroupId).map(it -> " " + it).orElse("") + " (NOT):" + Arrays.toString(getFacetIds());
		}

	}

	/**
	 * Internal data structure for referencing nullable groups.
	 */
	@Data
	private static class GroupReference implements Comparable<GroupReference> {
		private final Serializable entityType;
		private final Integer groupId;

		@Override
		public int compareTo(GroupReference o) {
			final int first = entityType.toString().compareTo(o.entityType.toString());
			return first == 0 ? ofNullable(groupId).map(it -> ofNullable(o.groupId).map(it::compareTo).orElse(-1)).orElseGet(() -> o.groupId != null ? 1 : 0) : first;
		}
	}

	@Data
	private static class FacetSummaryWithResultCount {
		private final int entityCount;
		private final FacetSummary facetSummary;
	}

	private static class FacetComputationalContext {
		private final EntitySchema entitySchema;
		private final Query query;
		private final BiFunction<Serializable, Integer, Predicate<FilterConstraint>> facetPredicateFactory;
		private final Map<Integer, Integer> parameterGroupMapping;
		private final List<FacetPredicate> existingFacetPredicates;
		private final Set<GroupReference> conjugatedGroups;
		private final Set<GroupReference> disjugatedGroups;
		private final Set<GroupReference> negatedGroups;

		public FacetComputationalContext(@Nonnull EntitySchema entitySchema, @Nonnull Query query, @Nonnull Map<Integer, Integer> parameterGroupMapping) {
			this.entitySchema = entitySchema;
			this.query = query;
			this.parameterGroupMapping = parameterGroupMapping;
			this.conjugatedGroups = findRequires(query, FacetGroupsConjunction.class)
				.stream()
				.flatMap(it -> {
					if (ArrayUtils.isEmpty(it.getFacetGroups())) {
						return Stream.of(new GroupReference(it.getEntityType(), null));
					} else {
						return Arrays.stream(it.getFacetGroups())
							.mapToObj(x -> new GroupReference(it.getEntityType(), x));
					}
				})
				.collect(Collectors.toSet());
			this.disjugatedGroups = findRequires(query, FacetGroupsDisjunction.class)
				.stream()
				.flatMap(it -> {
					if (ArrayUtils.isEmpty(it.getFacetGroups())) {
						return Stream.of(new GroupReference(it.getEntityType(), null));
					} else {
						return Arrays.stream(it.getFacetGroups())
							.mapToObj(x -> new GroupReference(it.getEntityType(), x));
					}
				})
				.collect(Collectors.toSet());
			this.negatedGroups = findRequires(query, FacetGroupsNegation.class)
				.stream()
				.flatMap(it -> {
					if (ArrayUtils.isEmpty(it.getFacetGroups())) {
						return Stream.of(new GroupReference(it.getEntityType(), null));
					} else {
						return Arrays.stream(it.getFacetGroups())
							.mapToObj(x -> new GroupReference(it.getEntityType(), x));
					}
				})
				.collect(Collectors.toSet());
			// create function that allows to create predicate that returns true if specified facet was part of input query filter
			this.facetPredicateFactory =
				(refType, refPk) ->
					fc -> fc instanceof Facet &&
						Objects.equals(((Facet) fc).getEntityType(), refType) &&
						ArrayUtils.contains(((Facet) fc).getFacetIds(), refPk);

			// create predicates that can filter along facet constraints in current query
			this.existingFacetPredicates = computeExistingFacetPredicates(query.getFilterBy(), entitySchema);
		}

		@Nonnull
		public Predicate<? super SealedEntity> createBaseFacetPredicate() {
			return combineFacetsIntoPredicate(existingFacetPredicates);
		}

		@Nonnull
		public Predicate<? super SealedEntity> createTestFacetPredicate(@Nonnull EntityReferenceContract facet) {
			// create brand new predicate
			final Predicate<FacetPredicate> matchTypeAndGroup = it -> Objects.equals(facet.getType(), it.getEntityType()) &&
				Objects.equals(getGroup(facet), it.getFacetGroupId());

			final List<FacetPredicate> combinedPredicates = Stream.concat(
				// use all previous facet predicates that doesn't match this facet type and group
				existingFacetPredicates
					.stream()
					.filter(matchTypeAndGroup.negate()),
				// alter existing facet predicate by adding new OR facet id or create new facet predicate for current facet
				Stream.of(
					existingFacetPredicates
						.stream()
						.filter(matchTypeAndGroup)
						.findFirst()
						.map(it -> it.combine(facet.getType(), getGroup(facet), facet.getPrimaryKey()))
						.orElseGet(() ->
							createFacetGroupPredicate(
								facet.getType(),
								ofNullable(entitySchema.getReference(facet.getType()).getGroupType()).orElse(facet.getType()),
								getGroup(facet),
								facet.getPrimaryKey()
							)
						)
				)
			).collect(toList());
			// now create and predicate upon it
			return combineFacetsIntoPredicate(combinedPredicates);
		}

		public boolean wasFacetRequested(EntityReferenceContract facet) {
			return ofNullable(query.getFilterBy())
				.map(fb -> {
					final Predicate<FilterConstraint> predicate = facetPredicateFactory.apply(
						facet.getType(),
						facet.getPrimaryKey()
					);
					return findFilter(fb, predicate) != null;
				})
				.orElse(false);
		}

		public boolean isAnyFacetGroupNegated() {
			return !negatedGroups.isEmpty();
		}

		public boolean isFacetGroupNegated(GroupReference groupReference) {
			return negatedGroups.contains(groupReference);
		}

		@Nullable
		private Integer getGroup(EntityReferenceContract facet) {
			return Entities.PARAMETER == facet.getType() ? parameterGroupMapping.get(facet.getPrimaryKey()) : null;
		}

		@Nonnull
		private List<FacetPredicate> computeExistingFacetPredicates(@Nullable FilterBy filterBy, @Nonnull EntitySchema entitySchema) {
			final List<FacetPredicate> userFilterPredicates = new LinkedList<>();
			if (filterBy != null) {
				for (Facet facetFilter : findFilters(filterBy, Facet.class)) {
					if (Entities.PARAMETER == facetFilter.getEntityType()) {
						final Map<Integer, List<Integer>> groupedFacets = Arrays.stream(facetFilter.getFacetIds())
							.boxed()
							.collect(
								groupingBy(parameterGroupMapping::get)
							);
						groupedFacets
							.forEach((facetGroupId, facetIdList) -> {
								final int[] facetIds = facetIdList.stream().mapToInt(it -> it).toArray();
								userFilterPredicates.add(
									createFacetGroupPredicate(
										facetFilter.getEntityType(),
										ofNullable(entitySchema.getReference(facetFilter.getEntityType()).getGroupType()).orElse(facetFilter.getEntityType()),
										facetGroupId,
										facetIds
									)
								);
							});
					} else {
						userFilterPredicates.add(
							createFacetGroupPredicate(
								facetFilter.getEntityType(),
								ofNullable(entitySchema.getReference(facetFilter.getEntityType()).getGroupType()).orElse(facetFilter.getEntityType()),
								null,
								facetFilter.getFacetIds()
							)
						);
					}
				}
			}
			return userFilterPredicates;
		}

		@Nonnull
		private FacetPredicate createFacetGroupPredicate(Serializable entityType, Serializable facetGroupEntityType, Integer facetGroupId, int... facetIds) {
			final GroupReference groupReference = new GroupReference(facetGroupEntityType, facetGroupId);
			if (conjugatedGroups.contains(groupReference)) {
				return new AndFacetPredicate(entityType, facetGroupEntityType, facetGroupId, facetIds);
			} else if (negatedGroups.contains(groupReference)) {
				return new NotFacetPredicate(entityType, facetGroupEntityType, facetGroupId, facetIds);
			} else {
				return new OrFacetPredicate(entityType, facetGroupEntityType, facetGroupId, facetIds);
			}
		}

		@Nonnull
		private Predicate<SealedEntity> combineFacetsIntoPredicate(List<FacetPredicate> predicates) {
			Predicate<SealedEntity> resultPredicate = entity -> true;
			final Optional<Predicate<SealedEntity>> disjugatedPredicates = predicates
				.stream()
				.filter(it -> disjugatedGroups.contains(new GroupReference(it.getFacetGroupEntityType(), it.getFacetGroupId())))
				.map(it -> (Predicate<SealedEntity>) it)
				.reduce(Predicate::or);
			final Optional<Predicate<SealedEntity>> conjugatedPredicates = predicates
				.stream()
				.filter(it -> !disjugatedGroups.contains(new GroupReference(it.getFacetGroupEntityType(), it.getFacetGroupId())))
				.map(it -> (Predicate<SealedEntity>) it)
				.reduce(Predicate::and);

			if (conjugatedPredicates.isPresent()) {
				resultPredicate = resultPredicate.and(conjugatedPredicates.get());
			}
			if (disjugatedPredicates.isPresent()) {
				resultPredicate = resultPredicate.or(disjugatedPredicates.get());
			}

			return resultPredicate;
		}

	}

}
