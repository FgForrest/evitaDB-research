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

package io.evitadb.test;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Support for SQL storage testing.
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
@CommonsLog
public class SqlStorageTestSupport {

    public static final String CATALOG_TEST_CATALOG = "testCatalog";

    private static final String STORAGE_HOSTNAME_PARAM = "dbhost";
    private static final String STORAGE_PORT_PARAM = "dbport";

    public static final String STORAGE_DEFAULT_HOSTNAME = "postgres";
    public static final String STORAGE_DEFAULT_PORT = "5432";
    public static final String STORAGE_DBNAME_PREFIX = "catalog_";
    public static final String STORAGE_USER = "postgres";
    public static final String STORAGE_PASSWORD = "test";

    public static String getStorageConnectionHost() {
        final String customHostname = System.getProperty(STORAGE_HOSTNAME_PARAM);
        final String customPort = System.getProperty(STORAGE_PORT_PARAM);

        return String.format(
                "%s:%s",
                (customHostname != null) ? customHostname : STORAGE_DEFAULT_HOSTNAME,
                (customPort != null) ? customPort : STORAGE_DEFAULT_PORT
        );
    }

    /**
     * Returns instance of {@link DataSource} with ready connection to test catalog storage (database)
     *
     * @return data source
     */
    public static DataSource createStorageDatasource(String dbName) {
        final String host = getStorageConnectionHost();

        return new DriverManagerDataSource(
                "jdbc:postgresql://" + host + "/" + dbName,
                STORAGE_USER,
                STORAGE_PASSWORD
        );
    }

    public static String getDbNameForCatalog(String catalogName) {
        return STORAGE_DBNAME_PREFIX + catalogName;
    }

    /**
     * Safely clears stored data in test catalog storage so that the storage can be reused without reinitializing.
     */
    @SuppressWarnings("ConstantConditions")
    public static void deleteStoredData(String catalogName) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(createStorageDatasource(getDbNameForCatalog(catalogName)));

        if (jdbcTemplate.queryForObject("select exists(select 1 from information_schema.tables where table_name='t_collection')", boolean.class)) {
            final List<Map<String, Object>> collections = jdbcTemplate.queryForList("select uid from t_collection");
            for (Map<String, Object> collection : collections) {
                jdbcTemplate.execute("drop schema " + collection.get("uid") + " cascade");
            }
            jdbcTemplate.execute(
                    "drop table t_entityHierarchyPlacement;" +
                    "drop sequence t_entity_id_seq;" +
                    "drop table t_entity cascade;" +
                    "drop table t_catalogAttribute cascade;" +
                    "drop table t_schema cascade;"
            );
            jdbcTemplate.execute(
                    "drop sequence t_collection_uid_seq;" +
                    "drop table t_collection cascade; "
            );
        }
    }

    /**
     * Checks if there are any stored collection in catalog database
     */
    public static boolean hasAnyStoredData(String catalogName) {
        try {
            final JdbcTemplate jdbcTemplate = new JdbcTemplate(createStorageDatasource(getDbNameForCatalog(catalogName)));
            return jdbcTemplate.queryForObject("select count(*) from t_collection", Integer.class) > 0;
        } catch (Exception ex) {
            // database or tables may not exist
            return false;
        }
    }
}
