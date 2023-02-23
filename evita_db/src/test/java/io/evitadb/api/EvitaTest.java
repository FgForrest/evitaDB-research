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

import io.evitadb.api.configuration.CacheOptions;
import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.configuration.EvitaConfiguration;
import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.ReferenceSchemaBuilder;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.sequence.SequenceService;
import io.evitadb.test.Entities;
import io.evitadb.test.TestFileSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test contains various integration tests for {@link Evita}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EvitaTest implements TestFileSupport {
	public static final String ATTRIBUTE_EAN = "ean";
	public static final String ATTRIBUTE_CATEGORY_PRIORITY = "categoryPriority";
	private static final String TEST_CATALOG = "testCatalog";
	private static final Currency CURRENCY_CZK = Currency.getInstance("CZK");
	private static final Currency CURRENCY_EUR = Currency.getInstance("EUR");
	private static final String PRICE_LIST_BASIC = "basic";
	private static final String PRICE_LIST_VIP = "vip";
	private Evita evita;

	@BeforeEach
	void setUp() throws IOException {
		SequenceService.reset();
		cleanTestDirectory();
		evita = new Evita(
			new EvitaConfiguration(),
			new EvitaCatalogConfiguration(
				TEST_CATALOG, getTestDirectory(),
				new StorageOptions(1),
				new CacheOptions()
			)
		);
	}

	@Test
	void shouldCreateDeleteAndRecreateReferencedEntityWithSameAttribute() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {

				session
					.defineSchema(Entities.CATEGORY)
					.applyChanges();

				session
					.defineSchema(Entities.PRODUCT)
					.withReferenceTo(
						Entities.CATEGORY,
						whichIs -> whichIs.withAttribute(
							ATTRIBUTE_CATEGORY_PRIORITY,
							Long.class,
							thatIs -> thatIs.sortable()
						)
					)
					.applyChanges();

				final EntityBuilder product = session.createNewEntity(Entities.PRODUCT, 1)
					.setReference(
						Entities.CATEGORY, 1,
						thatIs -> thatIs.setAttribute(ATTRIBUTE_CATEGORY_PRIORITY, 1L)
					);

				session.upsertEntity(product);

				session.upsertEntity(
					session.getEntity(Entities.PRODUCT, 1, fullEntity())
						.open()
						.removeReference(Entities.CATEGORY, 1)
				);

				session.upsertEntity(
					session.getEntity(Entities.PRODUCT, 1, fullEntity())
						.open()
						.setReference(
							Entities.CATEGORY, 8,
							thatIs -> thatIs.setAttribute(ATTRIBUTE_CATEGORY_PRIORITY, 5L)
						)
				);

				final SealedEntity loadedEntity = session.getEntity(Entities.PRODUCT, 1, fullEntity());
				assertNull(
					loadedEntity.getReference(Entities.CATEGORY, 1)
				);
				assertEquals(
					5L,
					(Long) loadedEntity
						.getReference(Entities.CATEGORY, 8)
						.getAttribute(ATTRIBUTE_CATEGORY_PRIORITY)
				);
			}
		);
	}

	@Test
	void shouldCreateDeleteAndRecreateSortableAttributeForReferencedEntity() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {

				session
					.defineSchema(Entities.CATEGORY)
					.applyChanges();

				session
					.defineSchema(Entities.PRODUCT)
					.withReferenceTo(
						Entities.CATEGORY,
						whichIs -> whichIs.withAttribute(
							ATTRIBUTE_CATEGORY_PRIORITY,
							Long.class,
							thatIs -> thatIs.sortable()
						)
					)
					.applyChanges();

				final EntityBuilder product = session.createNewEntity(Entities.PRODUCT, 1)
					.setReference(
						Entities.CATEGORY, 1,
						thatIs -> thatIs.setAttribute(ATTRIBUTE_CATEGORY_PRIORITY, 1L)
					);

				session.upsertEntity(product);

				final EntityBuilder updatedProduct = session.getEntity(Entities.PRODUCT, 1, fullEntity())
					.open()
					.setReference(
						Entities.CATEGORY, 1,
						thatIs -> thatIs.removeAttribute(ATTRIBUTE_CATEGORY_PRIORITY)
					);
				session.upsertEntity(updatedProduct);

				session.upsertEntity(
					session.getEntity(Entities.PRODUCT, 1, fullEntity())
						.open()
						.setReference(
							Entities.CATEGORY, 1,
							thatIs -> thatIs.setAttribute(ATTRIBUTE_CATEGORY_PRIORITY, 5L)
						)
				);

				final SealedEntity loadedEntity = session.getEntity(Entities.PRODUCT, 1, fullEntity());
				assertEquals(
					5L,
					(Long) loadedEntity
						.getReference(Entities.CATEGORY, 1)
						.getAttribute(ATTRIBUTE_CATEGORY_PRIORITY)
				);
			}
		);
	}

	@Test
	void shouldChangePriceInnerRecordHandlingAndRemovePrice() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session
					.defineSchema(Entities.PRODUCT)
					.withPrice()
					.applyChanges();

				final EntityBuilder product = session.createNewEntity(Entities.PRODUCT, 1)
					.setPriceInnerRecordHandling(PriceInnerRecordHandling.NONE)
					.setPrice(1, PRICE_LIST_BASIC, CURRENCY_CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
					.setPrice(2, PRICE_LIST_BASIC, CURRENCY_EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
					.setPrice(3, PRICE_LIST_VIP, CURRENCY_CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
					.setPrice(4, PRICE_LIST_VIP, CURRENCY_EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true);

				session.upsertEntity(product);

				final EntityBuilder updatedProduct = session.getEntity(Entities.PRODUCT, 1, fullEntity())
					.open()
					.setPriceInnerRecordHandling(PriceInnerRecordHandling.FIRST_OCCURRENCE);
				session.upsertEntity(updatedProduct);

				session.upsertEntity(
					session.getEntity(Entities.PRODUCT, 1, fullEntity())
						.open()
						.removePrice(1, PRICE_LIST_BASIC, CURRENCY_CZK)
						.removePrice(3, PRICE_LIST_VIP, CURRENCY_CZK)
				);

				final SealedEntity loadedEntity = session.getEntity(Entities.PRODUCT, 1, fullEntity());
				assertEquals(
					2,
					loadedEntity
						.getPrices()
						.size()
				);
			}
		);
	}

	@Test
	void shouldChangePriceSellability() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session
					.defineSchema(Entities.PRODUCT)
					.withPrice()
					.applyChanges();

				final EntityBuilder product = session.createNewEntity(Entities.PRODUCT, 1)
					.setPriceInnerRecordHandling(PriceInnerRecordHandling.NONE)
					.setPrice(1, PRICE_LIST_BASIC, CURRENCY_CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
					.setPrice(2, PRICE_LIST_BASIC, CURRENCY_EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false);

				session.upsertEntity(product);

				assertEquals(1, countProductsWithPriceListCurrencyCombination(session, PRICE_LIST_BASIC, CURRENCY_CZK));
				assertEquals(0, countProductsWithPriceListCurrencyCombination(session, PRICE_LIST_BASIC, CURRENCY_EUR));

				final EntityBuilder updatedProduct = session.getEntity(Entities.PRODUCT, 1, fullEntity())
					.open()
					.setPrice(1, PRICE_LIST_BASIC, CURRENCY_CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
					.setPrice(2, PRICE_LIST_BASIC, CURRENCY_EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true);
				session.upsertEntity(updatedProduct);

				assertEquals(0, countProductsWithPriceListCurrencyCombination(session, PRICE_LIST_BASIC, CURRENCY_CZK));
				assertEquals(1, countProductsWithPriceListCurrencyCombination(session, PRICE_LIST_BASIC, CURRENCY_EUR));
			}
		);
	}

	@Test
	void shouldRemoveDeepStructureOfHierarchicalEntity() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session
					.defineSchema(Entities.CATEGORY)
					.withHierarchy()
					.applyChanges();

				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 1).setHierarchicalPlacement(1));
				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 2).setHierarchicalPlacement(2));
				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 3).setHierarchicalPlacement(1, 1));
				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 4).setHierarchicalPlacement(1, 2));
				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 5).setHierarchicalPlacement(3, 1));
				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 6).setHierarchicalPlacement(3, 2));

				assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, getAllCategories(session));
				session.deleteEntityAndItsHierarchy(Entities.CATEGORY, 3);
				assertArrayEquals(new int[]{1, 2, 4}, getAllCategories(session));
				session.deleteEntityAndItsHierarchy(Entities.CATEGORY, 1);
				assertArrayEquals(new int[]{2}, getAllCategories(session));
			}
		);
	}

	@Test
	void shouldIndexAllAttributesAndPricesAfterReferenceToHierarchicalEntityIsSet() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session
					.defineSchema(Entities.CATEGORY)
					.withHierarchy()
					.applyChanges();

				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 1).setHierarchicalPlacement(1));

				session
					.defineSchema(Entities.BRAND)
					.applyChanges();

				session.upsertEntity(session.createNewEntity(Entities.BRAND, 1));

				session
					.defineSchema(Entities.PRODUCT)
					.withAttribute(ATTRIBUTE_EAN, String.class, thatIs -> thatIs.unique().sortable())
					.withPrice()
					.withReferenceToEntity(Entities.CATEGORY, ReferenceSchemaBuilder::faceted)
					.withReferenceToEntity(Entities.BRAND, ReferenceSchemaBuilder::faceted)
					.applyChanges();

				final EntityBuilder product = session.createNewEntity(Entities.PRODUCT, 1)
					.setPriceInnerRecordHandling(PriceInnerRecordHandling.NONE)
					.setPrice(1, PRICE_LIST_BASIC, CURRENCY_CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
					.setPrice(2, PRICE_LIST_BASIC, CURRENCY_EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
					.setAttribute(ATTRIBUTE_EAN, "123_ABC");

				// first create entity without references
				session.upsertEntity(product);

				// check there are no specialized entity indexes
				final EntityCollection productCollection = session.catalog.getCollectionForEntity(Entities.PRODUCT);
				assertNull(getHierarchyIndex(productCollection, Entities.CATEGORY, 1));
				assertNull(getReferencedEntityIndex(productCollection, Entities.BRAND, 1));

				// load it and add references
				session.upsertEntity(
					session.getEntity(Entities.PRODUCT, 1, fullEntity())
						.open()
						.setReference(Entities.BRAND, 1)
						.setReference(Entities.CATEGORY, 1)
				);

				// assert data from global index were propagated
				assertNull(getReferencedEntityIndex(productCollection, Entities.CATEGORY, 1));
				final EntityIndex categoryIndex = getHierarchyIndex(productCollection, Entities.CATEGORY, 1);
				assertDataWasPropagated(categoryIndex, 1);

				assertNull(getHierarchyIndex(productCollection, Entities.BRAND, 1));
				final EntityIndex brandIndex = getReferencedEntityIndex(productCollection, Entities.BRAND, 1);
				assertDataWasPropagated(brandIndex, 1);

				// load it and remove references
				session.upsertEntity(
					session.getEntity(Entities.PRODUCT, 1, fullEntity())
						.open()
						.removeReference(Entities.BRAND, 1)
						.removeReference(Entities.CATEGORY, 1)
				);

				// assert indexes were emptied and removed
				assertNull(getHierarchyIndex(productCollection, Entities.CATEGORY, 1));
				assertNull(getReferencedEntityIndex(productCollection, Entities.BRAND, 1));
			}
		);
	}

	@Test
	void shouldAvoidCreatingIndexesForNonIndexedReferences() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session
					.defineSchema(Entities.CATEGORY)
					.withHierarchy()
					.applyChanges();

				session.upsertEntity(session.createNewEntity(Entities.CATEGORY, 1).setHierarchicalPlacement(1));

				session
					.defineSchema(Entities.BRAND)
					.applyChanges();

				session.upsertEntity(session.createNewEntity(Entities.BRAND, 1));

				session
					.defineSchema(Entities.PRODUCT)
					.withAttribute(ATTRIBUTE_EAN, String.class, thatIs -> thatIs.unique().sortable())
					.withPrice()
					.withReferenceToEntity(Entities.CATEGORY, thatIs -> thatIs.withAttribute(ATTRIBUTE_CATEGORY_PRIORITY, Long.class))
					.withReferenceToEntity(Entities.BRAND)
					.applyChanges();

				final EntityBuilder product = session.createNewEntity(Entities.PRODUCT, 1)
					.setPriceInnerRecordHandling(PriceInnerRecordHandling.NONE)
					.setPrice(1, PRICE_LIST_BASIC, CURRENCY_CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
					.setPrice(2, PRICE_LIST_BASIC, CURRENCY_EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
					.setAttribute(ATTRIBUTE_EAN, "123_ABC")
					.setReference(Entities.BRAND, 1)
					.setReference(Entities.CATEGORY, 1, thatIs -> thatIs.setAttribute(ATTRIBUTE_CATEGORY_PRIORITY, 1L));

				// first create entity without references
				session.upsertEntity(product);

				// check there are no specialized entity indexes
				final EntityCollection productCollection = session.catalog.getCollectionForEntity(Entities.PRODUCT);
				assertNull(getHierarchyIndex(productCollection, Entities.CATEGORY, 1));
				assertNull(getReferencedEntityIndex(productCollection, Entities.BRAND, 1));
			}
		);
	}

	@Test
	void shouldHandleQueryingEmptyCollection() {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session
					.defineSchema(Entities.BRAND)
					.applyChanges();

				final EvitaResponseBase<SealedEntity> entities = session.query(
					Query.query(
						entities(Entities.BRAND)
					),
					SealedEntity.class
				);

				// result is expected to be empty
				assertEquals(0, entities.getTotalRecordCount());
				assertTrue(entities.getRecordData().isEmpty());
			}
		);
	}

	private static void assertDataWasPropagated(EntityIndex categoryIndex, int recordId) {
		assertNotNull(categoryIndex);
		assertTrue(categoryIndex.getUniqueIndex(new AttributeKey(ATTRIBUTE_EAN)).getRecordIds().contains(recordId));
		assertTrue(categoryIndex.getFilterIndex(new AttributeKey(ATTRIBUTE_EAN)).getAllRecords().contains(recordId));
		assertTrue(ArrayUtils.contains(categoryIndex.getSortIndex(new AttributeKey(ATTRIBUTE_EAN)).getSortedRecords(), recordId));
		assertTrue(categoryIndex.getPriceIndex(PRICE_LIST_BASIC, CURRENCY_CZK, PriceInnerRecordHandling.NONE).getIndexedPriceEntityIds().contains(recordId));
		// EUR price is not sellable
		assertNull(categoryIndex.getPriceIndex(PRICE_LIST_BASIC, CURRENCY_EUR, PriceInnerRecordHandling.NONE));
	}

	@Nullable
	private EntityIndex getHierarchyIndex(EntityCollection productCollection, Entities entityType, int recordId) {
		return productCollection.entityIndexCreator.getIndexIfExists(
			new EntityIndexKey(
				EntityIndexType.REFERENCED_HIERARCHY_NODE,
				new EntityReference(entityType, recordId)
			)
		);
	}

	@Nullable
	private EntityIndex getReferencedEntityIndex(EntityCollection productCollection, Entities entityType, int recordId) {
		return productCollection.entityIndexCreator.getIndexIfExists(
			new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY, new EntityReference(entityType, recordId))
		);
	}

	private int[] getAllCategories(EvitaSession session) {
		return session.query(
				Query.query(
					entities(Entities.CATEGORY)
				),
				EntityReferenceContract.class
			)
			.getRecordData()
			.stream()
			.mapToInt(EntityReferenceContract::getPrimaryKey)
			.toArray();
	}

	private int countProductsWithPriceListCurrencyCombination(EvitaSession session, String priceList, Currency currency) {
		return session.query(
				Query.query(
					entities(Entities.PRODUCT),
					filterBy(
						and(
							priceInPriceLists(priceList),
							priceInCurrency(currency)
						)
					)
				),
				EntityReferenceContract.class
			)
			.getTotalRecordCount();
	}

}