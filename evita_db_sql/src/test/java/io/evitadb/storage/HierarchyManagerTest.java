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

package io.evitadb.storage;

import io.evitadb.api.SqlEvita;
import io.evitadb.test.Entities;
import io.evitadb.test.SqlStorageTestSupport;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.dao.SqlStorage;
import one.edee.oss.pmptt.dao.WarmingUpMemoryStorage;
import one.edee.oss.pmptt.model.Hierarchy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static io.evitadb.test.SqlStorageTestSupport.CATALOG_TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HierarchyManager}
 *
 * @author Lukáš Hornych 2021
 */
class HierarchyManagerTest {

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(SqlStorageTestSupport.createStorageDatasource(SqlStorageTestSupport.getDbNameForCatalog(CATALOG_TEST_CATALOG)));
    private SqlEvita evita;

    private HierarchyManager warmupManager;
    private HierarchyManager aliveManager;

    @BeforeEach
    void setUp() {
        // delete all db data
        SqlStorageTestSupport.deleteStoredData(CATALOG_TEST_CATALOG);

        // initialize evita to have database structure
        evita = new SqlEvita(SqlEvitaTestSupport.createTestCatalogConfiguration());
        final CatalogContext testCatalogContext = SqlEvitaTestSupport.createTestCatalogContext();
        testCatalogContext.addEntityType(Entities.CATEGORY);
        testCatalogContext.addEntityType(Entities.PRODUCT);

        warmupManager = new HierarchyManager((short) 9, (short) 54);
        warmupManager.setCatalogContext(testCatalogContext);
        aliveManager = new HierarchyManager((short) 9, (short) 54);
        aliveManager.setCatalogContext(testCatalogContext);
        aliveManager.goLive();
    }

    @AfterEach
    void tearDown() {
        // close evita and clear data
        evita.close();
    }

    @Test
    void shouldCreateHierarchyDescriptor() {
        // warming up state
        final Hierarchy categoryHierarchyDescriptor = warmupManager.createHierarchyDescriptor(Entities.CATEGORY);
        assertHierarchy("CATEGORY", WarmingUpMemoryStorage.class, categoryHierarchyDescriptor);
        assertHierarchyExistsInMemory(Entities.CATEGORY);
        assertNotHierarchyExistInDb(Entities.CATEGORY.toString());

        // alive state
        final Hierarchy productHierarchyDescriptor = aliveManager.createHierarchyDescriptor(Entities.PRODUCT);
        assertHierarchy("PRODUCT", null, productHierarchyDescriptor);
        assertNotHierarchyExistsInMemory(Entities.PRODUCT);
        assertNotHierarchyExistInDb(Entities.PRODUCT.toString());
    }

    @Test
    void shouldNotReturnNonExistingHierarchy() {
        // warming up state
        assertThrows(NoSuchElementException.class, () -> warmupManager.getHierarchy(Entities.CATEGORY));
        // alive state
        assertThrows(NoSuchElementException.class, () -> aliveManager.getHierarchy(Entities.CATEGORY));
    }

    @Test
    void shouldReturnExistingHierarchy() {
        // warming up state
        warmupManager.createHierarchyDescriptor(Entities.CATEGORY);
        assertHierarchy(Entities.CATEGORY.toString(), WarmingUpMemoryStorage.class, warmupManager.getHierarchy(Entities.CATEGORY));

        // alive state
        jdbcTemplate.execute("create schema c_1");
        final Map<String, Object> collectionInsertArgs = new HashMap<>();
        collectionInsertArgs.put("name", Entities.CATEGORY.toString());
        collectionInsertArgs.put("nameType", "class");
        collectionInsertArgs.put("uid", "c_1");
        new SimpleJdbcInsert(jdbcTemplate.getDataSource())
                .withTableName("t_collection")
                .execute(collectionInsertArgs);

        final Map<String, Object> hierarchyInsertArgs = new HashMap<>();
        hierarchyInsertArgs.put("entityType", Entities.CATEGORY.toString());
        hierarchyInsertArgs.put("detail", new byte[0]);
        hierarchyInsertArgs.put("serializationHeader", new byte[0]);
        hierarchyInsertArgs.put("withHierarchy", true);
        hierarchyInsertArgs.put("hierarchyLevels", 10);
        hierarchyInsertArgs.put("hierarchySectionSize", 55);
        new SimpleJdbcInsert(jdbcTemplate.getDataSource())
                .withTableName("t_schema")
                .execute(hierarchyInsertArgs);
        assertHierarchy(Entities.CATEGORY.toString(), SqlStorage.class, aliveManager.getHierarchy(Entities.CATEGORY));
    }

    private void assertHierarchy(String expectedEntityType, Class<? extends HierarchyStorage> expectedStorageType, Hierarchy hierarchy) {
        assertEquals(expectedEntityType, hierarchy.getCode());
        assertEquals(10, hierarchy.getLevels());
        assertEquals(55, hierarchy.getSectionSize());
        if (expectedStorageType == null) {
            assertNull(hierarchy.getStorage());
        } else {
            assertEquals(expectedStorageType, hierarchy.getStorage().getClass());
        }
    }

    private void assertNotHierarchyExistInDb(String entityType) {
        assertFalse(jdbcTemplate.queryForObject("select exists(select 1 from t_schema where entityType = ?)", boolean.class, entityType));
    }

    private void assertHierarchyExistsInMemory(Serializable entityType) {
        assertNotNull(warmupManager.getHierarchy(entityType));
    }

    private void assertNotHierarchyExistsInMemory(Serializable entityType) {
        assertThrows(NoSuchElementException.class, () -> warmupManager.getHierarchy(entityType));
    }
}