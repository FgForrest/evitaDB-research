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
import io.evitadb.api.data.*;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.test.Entities;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import io.evitadb.test.generator.DataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.Serializable;
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
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies whether entities can be fetched:
 *
 * - by primary key
 * - in required form of completeness
 * - without any additional data leakage
 * - filtering content according to specific filtering constraints in the query
 * - lazy loading of content
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita entity fetch by primary key functionality")
@Tag(FUNCTIONAL_TEST)
@Slf4j
public class EntityFetchingFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	protected static final String FIFTY_PRODUCTS = "FiftyProducts";
	private static final int SEED = 40;
	private static final Locale LOCALE_CZECH = CZECH_LOCALE;
	private final static BiFunction<SealedEntity, Serializable, int[]> REFERENCED_ID_EXTRACTOR =
		(entity, referencedType) -> entity.getReferences(referencedType)
			.stream()
			.mapToInt(it -> it.getReferencedEntity().getPrimaryKey())
			.toArray();
	private final DataGenerator dataGenerator = new DataGenerator();

	@DataSet(FIFTY_PRODUCTS)
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
					dataGenerator.getSampleProductSchema(session),
					randomEntityPicker,
					SEED
				)
				.limit(50)
				.map(session::upsertEntity)
				.collect(Collectors.toList());

			session.catalog.flush();

			return storedProducts.stream()
				.map(it -> session.getEntity(it.getType(), it.getPrimaryKey(), fullEntity()))
				.collect(Collectors.toList());
		});
	}

	@DisplayName("Should check existence of the entity")
	@Test
	void shouldReturnOnlyPrimaryKey(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(2)
						)
					),
					EntityReference.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());
				assertEquals(new EntityReference(Entities.PRODUCT, 2), productByPk.getRecordData().get(0));
				return null;
			}
		);
	}

	@DisplayName("Should check existence of multiple entities")
	@Test
	void shouldReturnOnlyPrimaryKeys(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<EntityReference> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(2, 4, 9, 10, 18, 16)
						),
						require(
							page(1, 4)
						)
					),
					EntityReference.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(6, productByPk.getTotalRecordCount());
				assertEquals(new EntityReference(Entities.PRODUCT, 2), productByPk.getRecordData().get(0));
				assertEquals(new EntityReference(Entities.PRODUCT, 4), productByPk.getRecordData().get(1));
				assertEquals(new EntityReference(Entities.PRODUCT, 9), productByPk.getRecordData().get(2));
				assertEquals(new EntityReference(Entities.PRODUCT, 10), productByPk.getRecordData().get(3));
				return null;
			}
		);
	}

	@DisplayName("Single entity by primary key should be found")
	@Test
	void shouldRetrieveSingleEntityByPrimaryKey(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(2)
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 2, false, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities by their primary keys should be found")
	@Test
	void shouldRetrieveMultipleEntitiesByPrimaryKey(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(2, 4, 9, 10, 18, 16)
						),
						require(
							entityBody(),
							page(1, 4)
						)
					),
					SealedEntity.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(6, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 2, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(1), 4, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(2), 9, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(3), 10, false, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities by negative query against defined set should be found")
	@Test
	void shouldRetrieveMultipleEntitiesByNotAgainstDefinedSetQuery(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(2, 4, 9, 10, 18, 16),
								not(primaryKey(9, 10))
							)
						),
						require(
							entityBody(),
							page(1, 4)
						)
					),
					SealedEntity.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(4, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 2, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(1), 4, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(2), 16, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(3), 18, false, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities by negative query against entire superset should be found")
	@Test
	void shouldRetrieveMultipleEntitiesByNotAgainstSupersetQuery(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							not(primaryKey(2, 4, 9, 10, 18, 16))
						),
						require(
							entityBody(),
							page(1, 4)
						)
					),
					SealedEntity.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(44, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 1, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(1), 3, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(2), 5, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(3), 6, false, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities by complex boolean query should be found")
	@Test
	void shouldRetrieveMultipleEntitiesByComplexBooleanQuery(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							or(
								primaryKey(2, 4, 9, 10, 18, 16),
								and(
									not(primaryKey(7, 32, 55)),
									primaryKey(7, 14, 32, 33)
								)
							)
						),
						require(
							entityBody(),
							page(2, 4)
						)
					),
					SealedEntity.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(8, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 14, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(1), 16, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(2), 18, false, false, false, false);
				assertProduct(productByPk.getRecordData().get(3), 33, false, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Single entity with attributes only by primary key should be found")
	@Test
	void shouldRetrieveSingleEntityWithAttributesByPrimaryKey(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(2)
						),
						require(
							entityBody(), attributes()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 2, true, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with attributes only by their primary keys should be found")
	@Test
	void shouldRetrieveMultipleEntitiesWithAttributesByPrimaryKey(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(2, 4, 9, 10, 18, 16)
						),
						require(
							entityBody(), attributes(),
							page(1, 4)
						)
					),
					SealedEntity.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(6, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 2, true, false, false, false);
				assertProduct(productByPk.getRecordData().get(1), 4, true, false, false, false);
				assertProduct(productByPk.getRecordData().get(2), 9, true, false, false, false);
				assertProduct(productByPk.getRecordData().get(3), 10, true, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Single entity with attributes in passed language only by primary key should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveSingleEntityWithAttributesInLanguageByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAttribute(ATTRIBUTE_NAME, LOCALE_CZECH) != null && it.getAttribute(ATTRIBUTE_URL, LOCALE_CZECH) != null &&
				it.getAttribute(ATTRIBUTE_NAME, Locale.ENGLISH) == null && it.getAttribute(ATTRIBUTE_URL, Locale.ENGLISH) == null
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody(), attributes()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProductHasAttributesInLocale(product, LOCALE_CZECH, ATTRIBUTE_NAME, ATTRIBUTE_URL);
				assertProductHasNotAttributesInLocale(product, Locale.ENGLISH, ATTRIBUTE_NAME, ATTRIBUTE_URL);
				return null;
			}
		);
	}

	@DisplayName("Single entity with attributes in multiple languages only by primary key should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveSingleEntityWithAttributesInMultipleLanguagesByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAttribute(ATTRIBUTE_NAME, LOCALE_CZECH) != null && it.getAttribute(ATTRIBUTE_URL, LOCALE_CZECH) != null &&
				it.getAttribute(ATTRIBUTE_NAME, Locale.ENGLISH) != null && it.getAttribute(ATTRIBUTE_URL, Locale.ENGLISH) != null
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody(), attributes(), dataInLanguage(LOCALE_CZECH, Locale.ENGLISH)
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProductHasAttributesInLocale(product, LOCALE_CZECH, ATTRIBUTE_NAME, ATTRIBUTE_URL);
				assertProductHasAttributesInLocale(product, Locale.ENGLISH, ATTRIBUTE_NAME, ATTRIBUTE_URL);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with attributes in passed language only by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithAttributesInLanguageByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] pks = originalProductEntities
			.stream()
			.filter(
				it ->
					(it.getAttribute(ATTRIBUTE_NAME, LOCALE_CZECH) != null && it.getAttribute(ATTRIBUTE_URL, LOCALE_CZECH) != null) &&
						!(it.getAttribute(ATTRIBUTE_NAME, Locale.ENGLISH) != null && it.getAttribute(ATTRIBUTE_URL, Locale.ENGLISH) != null)
			)
			.map(EntityContract::getPrimaryKey)
			.toArray(Integer[]::new);
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(pks),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody(), attributes()
						)
					),
					SealedEntity.class
				);

				for (SealedEntity product : productByPk.getRecordData()) {
					assertProductHasAttributesInLocale(product, LOCALE_CZECH, ATTRIBUTE_NAME, ATTRIBUTE_URL);
					assertProductHasNotAttributesInLocale(product, Locale.ENGLISH, ATTRIBUTE_NAME, ATTRIBUTE_URL);
				}
				return null;
			}
		);
	}

	@DisplayName("Single entity with associated data only by primary key should be found")
	@Test
	void shouldRetrieveSingleEntityWithAssociatedDataByPrimaryKey(@UseDataSet(FIFTY_PRODUCTS) EVITA evita) {
		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(3)
						),
						require(
							entityBody(), associatedData()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), 3, false, true, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with associated data only by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithAssociatedDataByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements)
						),
						require(
							entityBody(), associatedData(),
							page(1, 4)
						)
					),
					SealedEntity.class
				);
				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				assertProduct(productByPk.getRecordData().get(0), entitiesMatchingTheRequirements[0], false, true, false, false);
				assertProduct(productByPk.getRecordData().get(1), entitiesMatchingTheRequirements[1], false, true, false, false);
				assertProduct(productByPk.getRecordData().get(2), entitiesMatchingTheRequirements[2], false, true, false, false);
				assertProduct(productByPk.getRecordData().get(3), entitiesMatchingTheRequirements[3], false, true, false, false);
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with associated data in passed language only by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithAssociatedDataInLanguageDataByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) == null
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody(), associatedData()
						)
					),
					SealedEntity.class
				);

				for (SealedEntity product : productByPk.getRecordData()) {
					assertProductHasAssociatedDataInLocale(product, LOCALE_CZECH, ASSOCIATED_DATA_LABELS);
					assertProductHasNotAssociatedDataInLocale(product, Locale.ENGLISH, ASSOCIATED_DATA_LABELS);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with associated data in multiple language only by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithAssociatedDataInMultipleLanguageDataByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) != null
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody(), associatedData(), dataInLanguage(LOCALE_CZECH, Locale.ENGLISH)
						)
					),
					SealedEntity.class
				);

				for (SealedEntity product : productByPk.getRecordData()) {
					assertProductHasAssociatedDataInLocale(product, LOCALE_CZECH, ASSOCIATED_DATA_LABELS);
					assertProductHasAssociatedDataInLocale(product, Locale.ENGLISH, ASSOCIATED_DATA_LABELS);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with selected associated data only by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithNamedAssociatedDataByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) != null &&
				!it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements)
						),
						require(
							entityBody(),
							associatedData(ASSOCIATED_DATA_LABELS),
							dataInLanguage(LOCALE_CZECH, Locale.ENGLISH)
						)
					),
					SealedEntity.class
				);

				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertProductHasAssociatedDataInLocale(product, LOCALE_CZECH, ASSOCIATED_DATA_LABELS);
					assertProductHasAssociatedDataInLocale(product, Locale.ENGLISH, ASSOCIATED_DATA_LABELS);
					assertProductHasNotAssociatedData(product, ASSOCIATED_DATA_REFERENCED_FILES);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with selected associated data in passed language only by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithNamedAssociatedDataInLanguageByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) == null &&
				!it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody(),
							associatedData(ASSOCIATED_DATA_LABELS)
						)
					),
					SealedEntity.class
				);

				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertProductHasAssociatedDataInLocale(product, LOCALE_CZECH, ASSOCIATED_DATA_LABELS);
					assertProductHasNotAssociatedDataInLocale(product, Locale.ENGLISH, ASSOCIATED_DATA_LABELS);
					assertProductHasNotAssociatedData(product, ASSOCIATED_DATA_REFERENCED_FILES);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with all prices by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithAllPricesByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getPrices().stream().map(PriceContract::getCurrency).anyMatch(CURRENCY_EUR::equals) &&
				it.getPrices().stream().map(PriceContract::getCurrency).anyMatch(CURRENCY_USD::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_BASIC::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_B2B::equals)
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements)
						),
						require(
							entityBody(), allPrices()
						)
					),
					SealedEntity.class
				);

				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertHasPriceInCurrency(product, CURRENCY_GBP, CURRENCY_USD);
					assertHasPriceInPriceList(product, PRICE_LIST_BASIC, PRICE_LIST_INTRODUCTION);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with prices in selected currency by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithPricesInCurrencyByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> {
				final List<PriceContract> filteredPrices = it.getPrices()
					.stream()
					.filter(PriceContract::isSellable)
					.filter(price -> Objects.equals(price.getPriceList(), PRICE_LIST_SELLOUT))
					.collect(Collectors.toList());
				return filteredPrices.stream().map(PriceContract::getCurrency).anyMatch(CURRENCY_EUR::equals) &&
					filteredPrices.stream().map(PriceContract::getCurrency).noneMatch(CURRENCY_USD::equals);
			}
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements),
								priceInPriceLists(PRICE_LIST_SELLOUT),
								priceInCurrency(CURRENCY_EUR)
							)
						),
						require(
							entityBody(), prices()
						)
					),
					SealedEntity.class
				);
				assertEquals(Math.min(20, entitiesMatchingTheRequirements.length), productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertHasPriceInCurrency(product, CURRENCY_EUR);
					assertHasNotPriceInCurrency(product, CURRENCY_USD);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with prices in selected price lists by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithPricesInPriceListsByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getPrices(CURRENCY_USD).stream().filter(PriceContract::isSellable).map(PriceContract::getPriceList).anyMatch(PRICE_LIST_BASIC::equals) &&
				it.getPrices(CURRENCY_USD).stream().filter(PriceContract::isSellable).map(PriceContract::getPriceList)
					.noneMatch(pl ->
						pl.equals(PRICE_LIST_REFERENCE) &&
							pl.equals(PRICE_LIST_INTRODUCTION) &&
							pl.equals(PRICE_LIST_B2B) &&
							pl.equals(PRICE_LIST_VIP)
					)
		);

		assertTrue(
			entitiesMatchingTheRequirements.length > 0,
			"None entity match the filter, test would not work!"
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements),
								priceInCurrency(CURRENCY_USD),
								priceInPriceLists(PRICE_LIST_BASIC)
							)
						),
						require(
							entityBody(), prices()
						)
					),
					SealedEntity.class
				);

				assertEquals(Math.min(entitiesMatchingTheRequirements.length, 20), productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertHasPriceInPriceList(product, PRICE_LIST_BASIC);
					assertHasNotPriceInPriceList(product, PRICE_LIST_REFERENCE, PRICE_LIST_INTRODUCTION, PRICE_LIST_B2B, PRICE_LIST_VIP);
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with prices valid in specified time by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithPricesValidInTimeByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getPrices().stream().filter(PriceContract::isSellable).map(PriceContract::getValidity).anyMatch(validity -> validity == null || validity.isValidFor(theMoment))
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements),
								priceValidIn(theMoment)
							)
						),
						require(
							entityBody(), prices(),
							page(1, 100)
						)
					),
					SealedEntity.class
				);
				assertEquals(Math.min(100, entitiesMatchingTheRequirements.length), productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					for (PriceContract price : product.getPrices()) {
						assertTrue(
							price.getValidity() == null || price.getValidity().isValidFor(theMoment),
							"Listed price " + price + " which is not valid for the moment!"
						);
					}
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with references by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithReferencesByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getReferences().isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements)
						),
						require(
							entityBody(), references(), page(1, 4)
						)
					),
					SealedEntity.class
				);

				assertEquals(4, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertFalse(product.getReferences().isEmpty());
				}
				return null;
			}
		);
	}

	@DisplayName("Multiple entities with references by their primary keys should be found")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldRetrieveMultipleEntitiesWithReferencesByTypeAndByPrimaryKey(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getReferences(Entities.STORE).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements)
						),
						require(
							entityBody(), references(Entities.STORE)
						)
					),
					SealedEntity.class
				);

				assertEquals(20, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				for (SealedEntity product : productByPk.getRecordData()) {
					assertFalse(product.getReferences(Entities.STORE).isEmpty());
					assertTrue(product.getReferences(Entities.BRAND).isEmpty());
					assertTrue(product.getReferences(Entities.CATEGORY).isEmpty());
				}
				return null;
			}
		);
	}

	@DisplayName("Attributes can be lazy auto loaded")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAttributes(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getAttributeValues().isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements[0])
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProduct(product, entitiesMatchingTheRequirements[0], false, false, false, false);

				final SealedEntity enrichedProduct = session.enrichEntity(product, attributes());
				assertProduct(enrichedProduct, entitiesMatchingTheRequirements[0], true, false, false, false);
				return null;
			}
		);
	}

	@DisplayName("Attributes can be lazy auto loaded while respecting language")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAttributesButLanguageMustBeRespected(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAttribute(ATTRIBUTE_NAME, LOCALE_CZECH) != null &&
				it.getAttribute(ATTRIBUTE_NAME, Locale.ENGLISH) != null
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProduct(product, 1, false, false, false, false);

				final SealedEntity enrichedProduct = session.enrichEntity(product, attributes());
				assertProduct(enrichedProduct, 1, true, false, false, false);
				assertNotNull(enrichedProduct.getAttribute(ATTRIBUTE_NAME, LOCALE_CZECH));
				assertEquals((String) enrichedProduct.getAttribute(ATTRIBUTE_NAME, LOCALE_CZECH), enrichedProduct.getAttribute(ATTRIBUTE_NAME));
				assertNull(enrichedProduct.getAttribute(ATTRIBUTE_NAME, Locale.ENGLISH));
				return null;
			}
		);
	}

	@DisplayName("Associated data can be lazy auto loaded")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAssociatedData(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements[0])
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProduct(product, entitiesMatchingTheRequirements[0], false, false, false, false);
				final SealedEntity enrichedProduct = session.enrichEntity(product, associatedData());
				assertProduct(enrichedProduct, entitiesMatchingTheRequirements[0], false, true, false, false);
				return null;
			}
		);
	}

	@DisplayName("Associated data can be lazy auto loaded")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAssociatedDataButLanguageMustBeRespected(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) != null &&
				!it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProduct(product, entitiesMatchingTheRequirements[0], false, false, false, false);
				final SealedEntity enrichedProduct = session.enrichEntity(product, associatedData());
				assertNotNull(enrichedProduct.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH));
				assertEquals(enrichedProduct.getAssociatedDataValue(ASSOCIATED_DATA_LABELS, LOCALE_CZECH), enrichedProduct.getAssociatedDataValue(ASSOCIATED_DATA_LABELS));
				assertNull(enrichedProduct.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH));
				return null;
			}
		);
	}

	@DisplayName("Associated data can be lazy auto loaded in different languages lazily")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAssociatedDataWithIncrementallyAddingLanguages(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) != null &&
				!it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProduct(product, entitiesMatchingTheRequirements[0], false, false, false, false);

				final SealedEntity enrichedProduct = session.enrichEntity(product, associatedData());
				assertNotNull(enrichedProduct.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH));
				assertEquals(enrichedProduct.getAssociatedDataValue(ASSOCIATED_DATA_LABELS, LOCALE_CZECH), enrichedProduct.getAssociatedDataValue(ASSOCIATED_DATA_LABELS));
				assertNull(enrichedProduct.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH));

				final SealedEntity enrichedProductWithAdditionalLanguage = session.enrichEntity(enrichedProduct, dataInLanguage(Locale.ENGLISH));
				assertNotNull(enrichedProductWithAdditionalLanguage.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH));
				assertNotNull(enrichedProductWithAdditionalLanguage.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH));

				return null;
			}
		);
	}

	@DisplayName("Associated data can be lazy auto loaded incrementally by name")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAssociatedDataByNameIncrementally(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH) != null &&
				it.getAssociatedData(ASSOCIATED_DATA_LABELS, Locale.ENGLISH) != null &&
				!it.getAssociatedDataValues(ASSOCIATED_DATA_REFERENCED_FILES).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								language(LOCALE_CZECH)
							)
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertProduct(product, entitiesMatchingTheRequirements[0], false, false, false, false);

				final SealedEntity enrichedProduct1 = session.enrichEntity(product, associatedData(ASSOCIATED_DATA_LABELS));
				assertNotNull(enrichedProduct1.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH));
				assertNull(enrichedProduct1.getAssociatedData(ASSOCIATED_DATA_REFERENCED_FILES, LOCALE_CZECH));

				final SealedEntity enrichedProduct2 = session.enrichEntity(enrichedProduct1, associatedData(ASSOCIATED_DATA_REFERENCED_FILES));
				assertNotNull(enrichedProduct2.getAssociatedData(ASSOCIATED_DATA_LABELS, LOCALE_CZECH));
				assertNotNull(enrichedProduct2.getAssociatedData(ASSOCIATED_DATA_REFERENCED_FILES, LOCALE_CZECH));

				return null;
			}
		);
	}

	@DisplayName("Prices can be lazy auto loaded")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadAllPrices(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getPrices().stream().map(PriceContract::getCurrency).anyMatch(CURRENCY_GBP::equals) &&
				it.getPrices().stream().map(PriceContract::getCurrency).anyMatch(CURRENCY_USD::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_BASIC::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_VIP::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_REFERENCE::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_B2B::equals) &&
				it.getPrices().stream().map(PriceContract::getPriceList).anyMatch(PRICE_LIST_INTRODUCTION::equals)
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements[0])
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertTrue(product.getPrices().isEmpty());

				final SealedEntity enrichedProduct = session.enrichEntity(product, allPrices());
				assertHasPriceInCurrency(enrichedProduct, CURRENCY_GBP, CURRENCY_USD);
				assertHasPriceInPriceList(enrichedProduct, PRICE_LIST_BASIC, PRICE_LIST_VIP, PRICE_LIST_REFERENCE, PRICE_LIST_B2B, PRICE_LIST_INTRODUCTION);
				return null;
			}
		);
	}

	@DisplayName("Prices can be lazy auto loaded")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadFilteredPrices(EVITA evita, List<SealedEntity> originalProductEntities) {
		final ZonedDateTime theMoment = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> it.getPrices()
				.stream()
				.filter(PriceContract::isSellable)
				.anyMatch(
					price -> Objects.equals(CURRENCY_EUR, price.getCurrency()) &&
						Objects.equals(PRICE_LIST_B2B, price.getPriceList()) &&
						(price.getValidity() != null && price.getValidity().isValidFor(theMoment)))
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							and(
								primaryKey(entitiesMatchingTheRequirements[0]),
								priceInCurrency(CURRENCY_EUR),
								priceInPriceLists(PRICE_LIST_B2B),
								priceValidIn(theMoment)
							)
						),
						require(
							entityBody(),
							page(1, Integer.MAX_VALUE)
						)
					),
					SealedEntity.class
				);
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getRecordData().size());
				assertEquals(entitiesMatchingTheRequirements.length, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertTrue(product.getPrices().isEmpty());

				final SealedEntity enrichedProduct = session.enrichEntity(product, prices());
				assertHasPriceInCurrency(enrichedProduct, CURRENCY_EUR);
				assertHasPriceInPriceList(enrichedProduct, PRICE_LIST_BASIC);
				return null;
			}
		);
	}

	@DisplayName("References can be lazy auto loaded")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadReferences(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getReferences(Entities.CATEGORY).isEmpty() &&
				!it.getReferences(Entities.BRAND).isEmpty() &&
				!it.getReferences(Entities.STORE).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements[0])
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertTrue(product.getReferences().isEmpty());

				final SealedEntity theEntity = originalProductEntities
					.stream()
					.filter(it -> Objects.equals(it.getPrimaryKey(), entitiesMatchingTheRequirements[0]))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Should never happen!"));

				final SealedEntity enrichedProduct = session.enrichEntity(product, references());
				assertHasReferencesTo(enrichedProduct, Entities.CATEGORY, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.CATEGORY));
				assertHasReferencesTo(enrichedProduct, Entities.BRAND, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.BRAND));
				assertHasReferencesTo(enrichedProduct, Entities.STORE, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.STORE));
				return null;
			}
		);
	}

	@DisplayName("References can be lazy auto loaded in iterative fashion")
	@UseDataSet(FIFTY_PRODUCTS)
	@Test
	void shouldLazyLoadReferencesIteratively(EVITA evita, List<SealedEntity> originalProductEntities) {
		final Integer[] entitiesMatchingTheRequirements = getRequestedIdsByPredicate(
			originalProductEntities,
			it -> !it.getReferences(Entities.CATEGORY).isEmpty() &&
				!it.getReferences(Entities.BRAND).isEmpty() &&
				!it.getReferences(Entities.STORE).isEmpty()
		);

		evita.queryCatalog(
			TEST_CATALOG,
			session -> {
				final EvitaResponseBase<SealedEntity> productByPk = session.query(
					query(
						entities(Entities.PRODUCT),
						filterBy(
							primaryKey(entitiesMatchingTheRequirements[0])
						),
						require(
							entityBody()
						)
					),
					SealedEntity.class
				);
				assertEquals(1, productByPk.getRecordData().size());
				assertEquals(1, productByPk.getTotalRecordCount());

				final SealedEntity product = productByPk.getRecordData().get(0);
				assertTrue(product.getReferences().isEmpty());

				final SealedEntity theEntity = originalProductEntities
					.stream()
					.filter(it -> Objects.equals(it.getPrimaryKey(), entitiesMatchingTheRequirements[0]))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Should never happen!"));

				final SealedEntity enrichedProduct1 = session.enrichEntity(product, references(Entities.CATEGORY));
				assertHasReferencesTo(enrichedProduct1, Entities.CATEGORY, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.CATEGORY));
				assertHasReferencesTo(enrichedProduct1, Entities.BRAND);
				assertHasReferencesTo(enrichedProduct1, Entities.STORE);

				final SealedEntity enrichedProduct2 = session.enrichEntity(enrichedProduct1, references(Entities.BRAND));
				assertHasReferencesTo(enrichedProduct2, Entities.CATEGORY, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.CATEGORY));
				assertHasReferencesTo(enrichedProduct2, Entities.BRAND, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.BRAND));
				assertHasReferencesTo(enrichedProduct2, Entities.STORE);

				final SealedEntity enrichedProduct3 = session.enrichEntity(enrichedProduct2, references(Entities.STORE));
				assertHasReferencesTo(enrichedProduct3, Entities.CATEGORY, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.CATEGORY));
				assertHasReferencesTo(enrichedProduct2, Entities.BRAND, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.BRAND));
				assertHasReferencesTo(enrichedProduct3, Entities.STORE, REFERENCED_ID_EXTRACTOR.apply(theEntity, Entities.STORE));
				return null;
			}
		);
	}

	/*
		PRIVATE METHODS
	 */

	private static void assertProduct(SealedEntity product, int primaryKey, boolean hasAttributes, boolean hasAssociatedData, boolean hasPrices, boolean hasReferences) {
		assertEquals(primaryKey, (int) Objects.requireNonNull(product.getPrimaryKey()));

		if (hasAttributes) {
			assertFalse(product.getAttributeValues().isEmpty());
			assertNotNull(product.getAttribute(ATTRIBUTE_CODE));
		} else {
			assertTrue(product.getAttributeValues().isEmpty());
			assertNull(product.getAttribute(ATTRIBUTE_CODE));
		}

		if (hasAssociatedData) {
			assertFalse(product.getAssociatedDataValues().isEmpty());
			assertNotNull(product.getAssociatedData(ASSOCIATED_DATA_REFERENCED_FILES));
		} else {
			assertTrue(product.getAssociatedDataValues().isEmpty());
			assertNull(product.getAssociatedData(ASSOCIATED_DATA_REFERENCED_FILES));
		}

		if (hasPrices) {
			assertFalse(product.getPrices().isEmpty());
		} else {
			assertTrue(product.getPrices().isEmpty());
		}

		if (hasReferences) {
			assertFalse(product.getReferences().isEmpty());
		} else {
			assertTrue(product.getReferences().isEmpty());
		}
	}

	private void assertProductHasAttributesInLocale(SealedEntity product, Locale locale, String... attributes) {
		for (String attribute : attributes) {
			assertNotNull(
				product.getAttribute(attribute, locale),
				"Product " + product.getPrimaryKey() + " lacks attribute " + attribute
			);
		}
	}

	private void assertProductHasNotAttributesInLocale(SealedEntity product, Locale locale, String... attributes) {
		for (String attribute : attributes) {
			assertNull(
				product.getAttribute(attribute, locale),
				"Product " + product.getPrimaryKey() + " has attribute " + attribute
			);
		}
	}

	private void assertProductHasAssociatedDataInLocale(SealedEntity product, Locale locale, String... associatedDataName) {
		for (String associatedData : associatedDataName) {
			assertNotNull(
				product.getAssociatedData(associatedData, locale),
				"Product " + product.getPrimaryKey() + " lacks associated data " + associatedData
			);
		}
	}

	private void assertProductHasNotAssociatedDataInLocale(SealedEntity product, Locale locale, String... associatedDataName) {
		for (String associatedData : associatedDataName) {
			assertNull(
				product.getAssociatedData(associatedData, locale),
				"Product " + product.getPrimaryKey() + " has associated data " + associatedData
			);
		}
	}

	private void assertProductHasNotAssociatedData(SealedEntity product, String... associatedDataName) {
		for (String associatedData : associatedDataName) {
			assertNull(
				product.getAssociatedData(associatedData),
				"Product " + product.getPrimaryKey() + " has associated data " + associatedData
			);
		}
	}

	private void assertHasPriceInPriceList(SealedEntity product, Serializable... priceListName) {
		final Set<Serializable> foundPriceLists = new HashSet<>();
		for (PriceContract price : product.getPrices()) {
			foundPriceLists.add(price.getPriceList());
		}
		assertTrue(
			foundPriceLists.size() >= priceListName.length,
			"Expected price in price list " +
				Arrays.stream(priceListName)
					.filter(it -> !foundPriceLists.contains(it))
					.map(Object::toString)
					.collect(Collectors.joining(", ")) +
				" but was not found!"
		);
	}

	private void assertHasNotPriceInPriceList(SealedEntity product, Serializable... priceList) {
		final Set<Serializable> forbiddenCurrencies = new HashSet<>(Arrays.asList(priceList));
		final Set<Serializable> clashingCurrencies = new HashSet<>();
		for (PriceContract price : product.getPrices()) {
			if (forbiddenCurrencies.contains(price.getPriceList())) {
				clashingCurrencies.add(price.getPriceList());
			}
		}
		assertTrue(
			clashingCurrencies.isEmpty(),
			"Price in price list " +
				clashingCurrencies
					.stream()
					.map(Object::toString)
					.collect(Collectors.joining(", ")) +
				" was not expected but was found!"
		);
	}

	private void assertHasPriceInCurrency(SealedEntity product, Currency... currency) {
		final Set<Currency> foundCurrencies = new HashSet<>();
		for (PriceContract price : product.getPrices()) {
			foundCurrencies.add(price.getCurrency());
		}
		assertTrue(
			foundCurrencies.size() >= currency.length,
			"Expected price in currency " +
				Arrays.stream(currency)
					.filter(it -> !foundCurrencies.contains(it))
					.map(Object::toString)
					.collect(Collectors.joining(", ")) +
				" but was not found!"
		);
	}

	private void assertHasNotPriceInCurrency(SealedEntity product, Currency... currency) {
		final Set<Currency> forbiddenCurrencies = new HashSet<>(Arrays.asList(currency));
		final Set<Currency> clashingCurrencies = new HashSet<>();
		for (PriceContract price : product.getPrices()) {
			if (forbiddenCurrencies.contains(price.getCurrency())) {
				clashingCurrencies.add(price.getCurrency());
			}
		}
		assertTrue(
			clashingCurrencies.isEmpty(),
			"Price in currency " +
				clashingCurrencies
					.stream()
					.map(Object::toString)
					.collect(Collectors.joining(", ")) +
				" was not expected but was found!"
		);
	}

	private void assertHasReferencesTo(SealedEntity product, Entities entityType, int... primaryKeys) {
		final Collection<ReferenceContract> references = product.getReferences(entityType);
		final Set<Integer> expectedKeys = Arrays.stream(primaryKeys).boxed().collect(Collectors.toSet());
		assertEquals(primaryKeys.length, references.size());
		for (ReferenceContract reference : references) {
			assertEquals(entityType, reference.getReferencedEntity().getType());
			expectedKeys.remove(reference.getReferencedEntity().getPrimaryKey());
		}
		assertTrue(
			expectedKeys.isEmpty(),
			"Expected references to these " + entityType + ": " +
				expectedKeys.stream().map(Object::toString).collect(Collectors.joining(", ")) +
				" but were not found!"
		);
	}

	@Nonnull
	private static Integer[] getRequestedIdsByPredicate(List<SealedEntity> originalProductEntities, Predicate<SealedEntity> predicate) {
		final Integer[] entitiesMatchingTheRequirements = originalProductEntities
			.stream()
			.filter(predicate::test)
			.map(EntityContract::getPrimaryKey)
			.toArray(Integer[]::new);

		assertTrue(entitiesMatchingTheRequirements.length > 0, "There are no entities matching the requirements!");
		return entitiesMatchingTheRequirements;
	}

}
