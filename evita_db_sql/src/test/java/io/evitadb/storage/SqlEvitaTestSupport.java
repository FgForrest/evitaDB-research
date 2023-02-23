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

import io.evitadb.api.SqlCatalog;
import io.evitadb.api.configuration.DatabaseConnectionConfiguration;
import io.evitadb.api.configuration.SqlEvitaCatalogConfiguration;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.test.SqlStorageTestSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.test.SqlStorageTestSupport.*;
import static org.mockito.Mockito.mock;

/**
 * Utils for sql evita functional tests. It is extension of {@link SqlStorageTestSupport}.
 *
 * @author Lukáš Hornych 2021
 */
public class SqlEvitaTestSupport {

    public static final String PRODUCT = "product";

    public static SqlEvitaCatalogConfiguration createTestCatalogConfiguration() {
        final DatabaseConnectionConfiguration databaseConnectionConfiguration = new DatabaseConnectionConfiguration(
                SqlStorageTestSupport.getStorageConnectionHost(),
                SqlStorageTestSupport.getDbNameForCatalog(CATALOG_TEST_CATALOG),
                STORAGE_USER,
                STORAGE_PASSWORD
        );
        return new SqlEvitaCatalogConfiguration(
                CATALOG_TEST_CATALOG,
                databaseConnectionConfiguration
        );
    }

    public static CatalogContext createTestCatalogContext() {
        final DatabaseConnectionConfiguration databaseConnectionConfiguration = new DatabaseConnectionConfiguration(
                SqlStorageTestSupport.getStorageConnectionHost(),
                SqlStorageTestSupport.getDbNameForCatalog(CATALOG_TEST_CATALOG),
                STORAGE_USER,
                STORAGE_PASSWORD
        );
        final SqlEvitaCatalogConfiguration catalogConfiguration = new SqlEvitaCatalogConfiguration(
                CATALOG_TEST_CATALOG,
                databaseConnectionConfiguration
        );

        final DataSource dataSource = new DriverManagerDataSource(
                databaseConnectionConfiguration.getJdbcUrl(),
                databaseConnectionConfiguration.getDbUsername(),
                databaseConnectionConfiguration.getDbPassword()
        );

        final CatalogContext catalogContext = new CatalogContext(
                mock(SqlCatalog.class),
                dataSource,
                new JdbcTemplate(dataSource),
                new NamedParameterJdbcTemplate(dataSource),
                new JdbcTransactionManager(dataSource),
                new HierarchyManager(
                        catalogConfiguration.getHierarchy().getLevels(),
                        catalogConfiguration.getHierarchy().getSectionSize()
                )
        );
        return catalogContext;
    }

    public static EntityCollectionContext createTestEntityCollectionContext() {
        final CatalogContext testCatalogContext = createTestCatalogContext();
        testCatalogContext.addEntityType(PRODUCT);

        return new EntityCollectionContext(
                testCatalogContext,
                "c_1",
                PRODUCT,
                new AtomicReference<>(new EntitySchema(PRODUCT)),
                new AtomicBoolean(),
                null
        );
    }
}
