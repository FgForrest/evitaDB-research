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
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.storage.serialization.sql.EntityCollectionDescriptorRowMapper;
import io.evitadb.test.SqlStorageTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.evitadb.test.SqlStorageTestSupport.CATALOG_TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CatalogRepository}.
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bönsch, 2021
 */
class CatalogRepositoryTest {

    public static final EntityCollectionDescriptorRowMapper ENTITY_COLLECTION_DESCRIPTOR_ROW_MAPPER = new EntityCollectionDescriptorRowMapper();

    private SqlEvita evita;
    private static JdbcTemplate jdbc;
    private static final String TEST_CATALOG = "testCatalog";
    CatalogRepository repo;
    CatalogContext catalogContext;

    @BeforeAll
    static void init() {
        jdbc = new JdbcTemplate(SqlStorageTestSupport.createStorageDatasource(SqlStorageTestSupport.getDbNameForCatalog(CATALOG_TEST_CATALOG)));
    }

    @BeforeEach
    void setUp() {
        // delete all db data
        SqlStorageTestSupport.deleteStoredData(SqlStorageTestSupport.CATALOG_TEST_CATALOG);

        catalogContext = SqlEvitaTestSupport.createTestCatalogContext();
        repo = new CatalogRepository(catalogContext);

        // create evita instance and configure test catalog
        evita = new SqlEvita(SqlEvitaTestSupport.createTestCatalogConfiguration());
    }

    @AfterEach
    void tearDown() {
        evita.close();
    }

    //tests
    @Test
    void shouldCreateNewCollection() {
        prepareTestData();

        final String collectionUid = repo.createNewCollection("type5");
        assertEquals("c_5", collectionUid);

        // test stored metadata
        final EntityCollectionDescriptor collection = jdbc.queryForObject(
                "select * from t_collection where uid = ?",
                ENTITY_COLLECTION_DESCRIPTOR_ROW_MAPPER,
                collectionUid
        );
        assertEquals(
                new EntityCollectionDescriptor("type5", "c_5", null),
                collection
        );

        // test created structure
        final List<String> createdTables = jdbc.queryForList(
                "select tablename from pg_catalog.pg_tables where schemaname = ? order by tablename",
                String.class,
                collectionUid
        );
        assertEquals(
                List.of("t_attributeindex", "t_priceindex", "t_referenceindex"),
                createdTables
        );
    }

    @Test
    void shouldFindAllCollections() {
        prepareTestData();

        final List<EntityCollectionDescriptor> collections = new ArrayList<>(repo.findAllCollections());
        collections.sort(Comparator.comparing(EntityCollectionDescriptor::getUid));
        assertEquals(new EntityCollectionDescriptor("type1", "c_1", null), collections.get(0));
        assertEquals(new EntityCollectionDescriptor("type2", "c_2", null), collections.get(1));
        assertEquals(new EntityCollectionDescriptor("type3", "c_3", null), collections.get(2));
        assertEquals(new EntityCollectionDescriptor(2, "c_4", null), collections.get(3));
    }

    @Test
    void shouldDeleteCollection() {
        prepareTestData();

        repo.deleteCollection("type1");
        repo.deleteCollection("type3");

        //check if record exists in entity
        String query = "SELECT * FROM t_entity WHERE type = ?";
        assertFalse(recordExist(query, "type1"));
        assertFalse(recordExist(query, "type3"));
        assertTrue(recordExist(query, "type2"));
        assertTrue(recordExist(query, "2"));
        //check if record exists in collection
        query = "SELECT * FROM t_collection WHERE name = ?";
        assertFalse(recordExist(query, "type1"));
        assertFalse(recordExist(query, "type3"));
        assertTrue(recordExist(query, "type2"));
        assertTrue(recordExist(query, "2"));
        //check if record exists in schema
        query = "SELECT * FROM t_schema WHERE entityType = ?";
        assertFalse(recordExist(query, "type1"));
        assertFalse(recordExist(query, "type3"));
        assertTrue(recordExist(query, "type2"));
        assertTrue(recordExist(query, "2"));
    }

    @Test
    void shouldGoLive() {
        repo.goLive(false);
        assertEquals("ALIVE", jdbc.queryForObject("select value from t_catalogAttribute where key = ?", String.class, "state"));
        assertTrue(jdbc.queryForObject("select count(*) from pg_indexes where indexname like 'ix_%'", Integer.class) > 0);
    }

    @Test
    void shouldRestoreLiveState() {
        final String indexCountQuery = "select count(*) from pg_indexes where indexname like 'ix_%'";

        prepareTestData();

        assertEquals("ALIVE", jdbc.queryForObject("select value from t_catalogAttribute where key = ?", String.class, "state"));
        assertTrue(jdbc.queryForObject(indexCountQuery, Integer.class) > 0);

        repo.goLive(true);
        assertEquals("ALIVE", jdbc.queryForObject("select value from t_catalogAttribute where key = ?", String.class, "state"));
        assertTrue(jdbc.queryForObject(indexCountQuery, Integer.class) > 0);
    }


    //helper methods
    private void prepareTestData() {
        evita.updateCatalog(
                TEST_CATALOG,
                session -> {
                    session.goLiveAndClose();
                }
        );
        evita.updateCatalog(
                TEST_CATALOG,
                session -> {
                    session.upsertEntity(new InitialEntityBuilder("type1", 1));
                    catalogContext.addEntityType("type1");
                    session.upsertEntity(new InitialEntityBuilder("type1", 2));
                    session.upsertEntity(new InitialEntityBuilder("type2", 1));
                    catalogContext.addEntityType("type2");
                    session.upsertEntity(new InitialEntityBuilder("type3", 1));
                    catalogContext.addEntityType("type3");
                    session.upsertEntity(new InitialEntityBuilder(2, 1));
                    catalogContext.addEntityType(2);
                }
        );
    }

    private boolean recordExist(String sql, @Nullable Object... args) {
        String query = "SELECT EXISTS(" + sql + ")";
        return jdbc.queryForObject(query, Boolean.class, args);
    }

}
