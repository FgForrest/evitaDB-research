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

package one.edee.oss.pmptt.dao;

import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.SqlEvita;
import io.evitadb.api.SqlEvitaSession;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.storage.SqlEvitaTestSupport;
import io.evitadb.test.Entities;
import io.evitadb.test.SqlStorageTestSupport;
import lombok.Getter;
import one.edee.oss.pmptt.model.*;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.evitadb.test.SqlStorageTestSupport.CATALOG_TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SqlStorage}
 *
 * @author Lukáš Hornych 2021
 */
class SqlStorageTest {

	private final PuppetHierarchyChangeListener puppetListener = new PuppetHierarchyChangeListener();
	private final JdbcTemplate jdbcTemplate = new JdbcTemplate(SqlStorageTestSupport.createStorageDatasource(SqlStorageTestSupport.getDbNameForCatalog(CATALOG_TEST_CATALOG)));
	private final SqlStorage storage = new SqlStorage(SqlEvitaTestSupport.createTestCatalogContext());
	private SqlEvita evita;

	@BeforeEach
	void setUp() {
		// delete all db data
		SqlStorageTestSupport.deleteStoredData(CATALOG_TEST_CATALOG);

		// initialize evita to be able to insert entities (hierarchy items)
		evita = new SqlEvita(SqlEvitaTestSupport.createTestCatalogConfiguration());
		evita.updateCatalog(
			CATALOG_TEST_CATALOG,
			(Consumer<SqlEvitaSession>) EvitaSessionBase::goLiveAndClose
		);
		evita.updateCatalog(
			CATALOG_TEST_CATALOG,
			session -> {
				new EntitySchemaBuilder(
					new EntitySchema(Entities.CATEGORY),
					entitySchema -> {
						session.defineSchema(entitySchema);
						return entitySchema;
					}
				)
					/* all is strictly verified */
					.verifySchemaStrictly()
					/* let Evita generates the key */
					.withoutGeneratedPrimaryKey()
					/* brands are not organized in the tree */
					.withHierarchy()
					/* finally apply schema changes */
					.applyChanges();

				new EntitySchemaBuilder(
					new EntitySchema(Entities.PRODUCT),
					entitySchema -> {
						session.defineSchema(entitySchema);
						return entitySchema;
					}
				)
					.verifySchemaStrictly()
					.withoutGeneratedPrimaryKey()
					.withoutHierarchy()
					.withReferenceToEntity(Entities.CATEGORY)
					.applyChanges();
			}
		);
		insertTestTree();

		storage.registerChangeListener(puppetListener);
		puppetListener.clear();
	}

	@Test
	void shouldNotCreateHierarchy() {
		storage.createHierarchy(new Hierarchy("test", (short) 1, (short) 1));
		assertNull(storage.getHierarchy("test"));
	}

	@Test
	void shouldReturnHierarchy() {
		final Hierarchy expected = new Hierarchy(Entities.CATEGORY.toString(), (short) 9, (short) 54);
		expected.setStorage(storage);
		assertEquals(expected, storage.getHierarchy(Entities.CATEGORY.toString()));
	}

	@Test
	void shouldNotRemoveHierarchy() {
		assertThrows(IllegalStateException.class, () -> {
			storage.removeHierarchy(Entities.CATEGORY.toString());
		});
	}

	@Test
	void shouldMockCreateItem() {
		storage.createItem(
			new HierarchyItemBase(Entities.CATEGORY.toString(), "130", (short) 2, 2L, 3L, (short) 1),
			new HierarchyItemBase(Entities.CATEGORY.toString(), "100", (short) 1, 1L, 4L, (short) 1)
		);

		assertItemsCreated("130");
		assertNull(storage.getItem(Entities.CATEGORY.toString(), "130"));
	}

	@Test
	void shouldUpdateItem() {
		final HierarchyItem item = storage.getItem(Entities.CATEGORY.toString(), "111");
		assertItem(item, 3, 1, 0);

		item.setLeftBound(1L);
		item.setRightBound(10L);
		item.setLevel((short) 2);
		item.setOrder((short) 2);
		item.setNumberOfChildren((short) 10);
		item.setBucket((short) 100);
		storage.updateItem(item);

		assertItemReferencesUpdated();
		assertItemsUpdated("111");
		final HierarchyItem updatedItem = storage.getItem(Entities.CATEGORY.toString(), "111");
		assertItem(updatedItem, 1L, 10L, 2, 2, 10);
	}

	@Test
	void shouldMockRemoveItem() {
		storage.removeItem(storage.getItem(Entities.CATEGORY.toString(), "111"));
		assertItemReferencesDeleted();
		assertItemsRemoved("111");
		assertItem(storage.getItem(Entities.CATEGORY.toString(), "111"), 3, 1, 0);
	}

	@Test
	void shouldGetItem() {
		final HierarchyItem item = storage.getItem(Entities.CATEGORY.toString(), "110");
		assertItem(item, 2L, 83790324380788L, 2, 1, 2);
	}

	@Test
	void shouldGetParentItem() {
		final HierarchyItem parent = storage.getParentItem(storage.getItem(Entities.CATEGORY.toString(), "112"));
		assertItem(parent, 2, 1, 2);
	}

	@Test
	void shouldGetParentsOfItem() {
		final List<HierarchyItem> parents = storage.getParentsOfItem(storage.getItem(Entities.CATEGORY.toString(), "112"));
		assertEquals(2, parents.size());
		assertItem(parents.get(0), 1, 1, 2);
		assertItem(parents.get(1), 2, 1, 2);
	}

	@Test
	void shouldGetRootItems() {
		final List<HierarchyItem> roots = storage.getRootItems(Entities.CATEGORY.toString());
		assertEquals(2, roots.size());
		assertItem(roots.get(0), 1, 1, 2);
		assertItem(roots.get(1), 1, 2, 0);
	}

	@Test
	void shouldGetChildItems() {
		final List<HierarchyItem> children = storage.getChildItems(storage.getItem(Entities.CATEGORY.toString(), "110"));
		assertEquals(2, children.size());
		assertItem(children.get(0), 3, 1, 0);
		assertItem(children.get(1), 3, 2, 0);
	}

	@Test
	void shouldGetAllChildrenItems() {
		final List<HierarchyItem> children = storage.getAllChildrenItems(storage.getItem(Entities.CATEGORY.toString(), "100"));
		assertEquals(5, children.size());
		assertItem(children.get(0), 2, 1, 2);
		assertItem(children.get(1), 2, 2, 1);
		assertItem(children.get(2), 3, 1, 0);
		assertItem(children.get(3), 3, 1, 0);
		assertItem(children.get(4), 3, 2, 0);
	}

	@Test
	void shouldGetLeadItemsOfParent() {
		final List<HierarchyItem> children = storage.getLeafItems(storage.getItem(Entities.CATEGORY.toString(), "110"));
		assertEquals(2, children.size());
		assertItem(children.get(0), 3, 1, 0);
		assertItem(children.get(1), 3, 2, 0);
	}

	@Test
	void shouldGetLeadItemsOfWholeHierarchy() {
		final List<HierarchyItem> children = storage.getLeafItems(Entities.CATEGORY.toString());
		assertEquals(4, children.size());
		assertItem(children.get(0), 3, 1, 0);
		assertItem(children.get(1), 3, 2, 0);
		assertItem(children.get(2), 3, 1, 0);
		assertItem(children.get(3), 1, 2, 0);
	}

	@Test
	void shouldGetFirstEmptySectionOfRoot() {
		final long sectionSizeForLevel = Section.getSectionSizeForLevel((short) 55, (short) 1, (short) 10);
		final SectionWithBucket section = storage.getFirstEmptySection(Entities.CATEGORY.toString(), sectionSizeForLevel, (short) 3);
		assertEquals((short) 3, section.getBucket());
	}

	@Test
	void shouldGetFirstEmptySectionOfParent() {
		final long sectionSizeForLevel = Section.getSectionSizeForLevel((short) 55, (short) 3, (short) 10);
		final SectionWithBucket section = storage.getFirstEmptySection(
			Entities.CATEGORY.toString(),
			sectionSizeForLevel,
			(short) 3,
			storage.getItem(Entities.CATEGORY.toString(), "120")
		);
		assertEquals((short) 2, section.getBucket());
	}

	@AfterEach
	void tearDown() {
		// close evita and clear data
		evita.close();
	}

	private void insertTestTree() {
		evita.updateCatalog(
			CATALOG_TEST_CATALOG,
			session -> {
                    /*
                        categories:
                        > root 100
                            > 110
                                > 111 (has 2 products)
                                > 112
                            > 120
                                > 121
                        > root 200
                     */

				// categories
				session.upsertEntity(createRootCategoryEntity(100, 1));
				session.upsertEntity(createRootCategoryEntity(200, 2));

				session.upsertEntity(createCategoryEntity(110, 100, 1));
				session.upsertEntity(createCategoryEntity(111, 110, 1));
				session.upsertEntity(createCategoryEntity(112, 110, 2));

				session.upsertEntity(createCategoryEntity(120, 100, 2));
				session.upsertEntity(createCategoryEntity(121, 120, 1));

				// products
				session.upsertEntity(createProductEntity(1000, 111));
				session.upsertEntity(createProductEntity(1001, 111));
			}
		);
	}

	private EntityMutation createRootCategoryEntity(int primaryKey, int orderAmongSiblings) {
		return new InitialEntityBuilder(Entities.CATEGORY, primaryKey).setHierarchicalPlacement(orderAmongSiblings).toMutation();
	}

	private EntityMutation createCategoryEntity(int primaryKey, int parentPrimaryKey, int orderAmongSiblings) {
		return new InitialEntityBuilder(Entities.CATEGORY, primaryKey).setHierarchicalPlacement(parentPrimaryKey, orderAmongSiblings).toMutation();
	}

	private EntityMutation createProductEntity(int primaryKey, int categoryPrimaryKey) {
		return new InitialEntityBuilder(Entities.PRODUCT, primaryKey).setReference(Entities.CATEGORY, categoryPrimaryKey).toMutation();
	}

	private void assertItem(HierarchyItem item, Integer level, Integer order, Integer numberOfChildren) {
		assertEquals(Short.valueOf(level.shortValue()), item.getLevel(), "Level doesn't match!");
		assertEquals(Short.valueOf(order.shortValue()), item.getOrder(), "Order doesn't match!");
		assertEquals(Short.valueOf(numberOfChildren.shortValue()), item.getNumberOfChildren(), "Number of children doesn't match!");
	}

	private void assertItem(HierarchyItem item, long leftBound, long rightBound, Integer level, Integer order, Integer numberOfChildren) {
		assertEquals(leftBound, item.getLeftBound(), "Left bound doesn't match!");
		assertEquals(rightBound, item.getRightBound(), "Right bound doesn't match!");
		assertEquals(Short.valueOf(level.shortValue()), item.getLevel(), "Level doesn't match!");
		assertEquals(Short.valueOf(order.shortValue()), item.getOrder(), "Order doesn't match!");
		assertEquals(Short.valueOf(numberOfChildren.shortValue()), item.getNumberOfChildren(), "Number of children doesn't match!");
	}

	private void assertItemsCreated(String... expected) {
		assertEquals(
			StringUtils.arrayToCommaDelimitedString(expected),
			StringUtils.collectionToCommaDelimitedString(puppetListener.getCreated())
		);
	}

	private void assertItemsUpdated(String... expected) {
		assertEquals(
			StringUtils.arrayToCommaDelimitedString(expected),
			StringUtils.collectionToCommaDelimitedString(puppetListener.getUpdated())
		);
	}

	private void assertItemsRemoved(String... expected) {
		assertEquals(
			StringUtils.arrayToCommaDelimitedString(expected),
			StringUtils.collectionToCommaDelimitedString(puppetListener.getRemoved())
		);
	}

	private void assertItemReferencesUpdated() {
		final String boundQuery =
				"select placement.leftBound, placement.rightBound, placement.level " +
				"from t_entity entity " +
				"join t_entityHierarchyPlacement placement on placement.entity_id = entity.entity_id " +
				"where entity.type = ?" +
				"	and entity.primaryKey = ?" +
				"	and placement.type = ?" +
				"	and placement.primaryKey = ?";

		final Map<String, Object> bounds1 = jdbcTemplate.queryForMap(
				boundQuery,
				Entities.PRODUCT.toString(), 1000, "CATEGORY", "111"
		);
		assertEquals(1, (long) bounds1.get("leftBound"));
		assertEquals(10, (long) bounds1.get("rightBound"));
		assertEquals(2, bounds1.get("level"));

		final Map<String, Object> bounds2 = jdbcTemplate.queryForMap(
				boundQuery,
				Entities.PRODUCT.toString(), 1001, "CATEGORY", "111"
		);
		assertEquals(1, (long) bounds2.get("leftBound"));
		assertEquals(10, (long) bounds2.get("rightBound"));
		assertEquals(2, bounds2.get("level"));
	}

	private void assertItemReferencesDeleted() {
		final String itemCountQuery =
				"select count(*) " +
				"from t_entity entity " +
				"join t_entityHierarchyPlacement placement on placement.entity_id = entity.entity_id " +
				"where entity.type = ?" +
				"	and entity.primaryKey = ?" +
				"	and placement.type = ?" +
				"	and placement.primaryKey = ?";

		assertEquals(0, jdbcTemplate.queryForObject(
				itemCountQuery,
				int.class,
				Entities.PRODUCT.toString(), 1000, "CATEGORY", "111"
		));
		assertEquals(0, jdbcTemplate.queryForObject(
				itemCountQuery,
				int.class,
				Entities.PRODUCT.toString(), 1001, "CATEGORY", "111"
		));
	}

	private static class PuppetHierarchyChangeListener implements HierarchyChangeListener {
		@Getter
		private final List<String> created = new LinkedList<>();
		@Getter private final List<String> updated = new LinkedList<>();
		@Getter private final List<String> removed = new LinkedList<>();

		@Override
		public void itemCreated(HierarchyItem createdItem) {
			this.created.add(createdItem.getCode());
		}

		@Override
		public void itemUpdated(HierarchyItem updatedItem, HierarchyItem originalItem) {
			this.updated.add(updatedItem.getCode());
		}

		@Override
		public void itemRemoved(HierarchyItem removeItem) {
			this.removed.add(removeItem.getCode());
		}

		void clear() {
			this.created.clear();
			this.updated.clear();
			this.removed.clear();
		}

	}
}