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

import io.evitadb.api.CatalogState;
import io.evitadb.api.SqlCatalog;
import io.evitadb.storage.serialization.sql.EntityCollectionDescriptorRowMapper;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.*;

/**
 * Repository for accessing {@link SqlCatalog}'s persistence storage which uses
 * {@link org.springframework.jdbc.core.JdbcTemplate} as communication tool between repository and DB.
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
@Slf4j
public class CatalogRepository {

    private final CatalogContext ctx;
    private final EntityCollectionDescriptorRowMapper entityCollectionDescriptorRowMapper;

    // scripts
    private String commonIndexesInitScript;

    public CatalogRepository(@Nonnull CatalogContext catalogContext) {
        this.ctx = catalogContext;
        this.entityCollectionDescriptorRowMapper = new EntityCollectionDescriptorRowMapper();

        commonIndexesInitScript = loadSqlScript(new ClassPathResource("/database/indexes/common.sql"));
    }

    /**
     * Creates new db schema with need structure for new collection and registers reference to that collection-
     *
     * @param entityType entity type of new collection
     * @return uid of new collection
     */
    public String createNewCollection(@Nonnull Serializable entityType) {
        final TypedValue<String> serializedEntityType = ctx.addEntityType(entityType);

        // generate new uid
        final String newUid = ctx.getJdbcTemplate().queryForObject(
                "select nextVal('t_collection_uid_seq') as n",
                (rs, rowNum) -> "c_" + rs.getLong("n")
        );
        if (newUid == null) {
            throw new NullPointerException("Database did not generate new UID for collection");
        }
        if (newUid.length() > 12) {
            throw new RuntimeException("You have reached limit of number of collections. Cannot create another collection.");
        }

        // schema structure initialization
        ctx.getJdbcTemplate().execute("create schema " + newUid);

        final ClassPathResource initCollectionStructureScriptFile = new ClassPathResource("/database/structure/collection.sql");
        final StringBuilder initCollectionStructureScript = new StringBuilder();
        try (final BufferedReader scriptReader = new BufferedReader(new InputStreamReader(initCollectionStructureScriptFile.getInputStream()))) {
            String scriptLine;
            while ((scriptLine = scriptReader.readLine()) != null) {
                scriptLine = scriptLine.replace("${collectionUid}", newUid);
                scriptLine = scriptLine.replace("${collectionType}", serializedEntityType.getSerializedValue());
                initCollectionStructureScript.append(scriptLine).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ctx.getJdbcTemplate().execute(initCollectionStructureScript.toString());

        // register reference
        final Map<String, Object> collectionArgs = new HashMap<>();
        collectionArgs.put("name", serializedEntityType.getSerializedValue());
        collectionArgs.put("nameType", serializedEntityType.getSerializedType());
        collectionArgs.put("uid", newUid);
        new SimpleJdbcInsert(ctx.getJdbcTemplate())
                .withTableName("t_collection")
                .execute(collectionArgs);

        return newUid;
    }

    /**
     * Finds all entity collections stored for this catalog
     *
     * @return all entity types of collections
     */
    @Nonnull
    public Set<EntityCollectionDescriptor> findAllCollections() {
        final List<EntityCollectionDescriptor> foundTypes = ctx.getJdbcTemplate().query(
                "select name, nameType, uid, serializationHeader from t_collection",
                entityCollectionDescriptorRowMapper
        );
        return new LinkedHashSet<>(foundTypes);
    }

    /**
     * Deletes entire entity collection with its data by type of collection
     *
     * @param entityType entity type to delete collection by
     */
    public void deleteCollection(@Nonnull Serializable entityType) {
        final String serializedEntityType = ctx.getSerializedEntityType(entityType);

        final String uid = ctx.getJdbcTemplate().queryForObject(
                "select uid from t_collection where name = ?",
                String.class,
                serializedEntityType
        );

        ctx.getJdbcTemplate().execute("drop schema " + uid + " cascade");

        ctx.getJdbcTemplate().update(
                "delete from t_collection where name = ?",
                serializedEntityType
        );
    }

    public void createCatalogStructure() {
        final boolean collectionExists = ctx.getJdbcTemplate().queryForObject("select exists(select 1 from information_schema.tables where table_name='t_collection')", boolean.class);
        if (collectionExists) {
            return;
        }
        final String initCommonStructureScript = loadSqlScript(new ClassPathResource("/database/structure/common.sql"));
        ctx.getJdbcTemplate().execute(initCommonStructureScript);
    }


    /**
     * Switches underlying storage to alive state
     *
     * @param restore if restoring previous alive state
     */
    public void goLive(boolean restore) {
        if (isCatalogAlive()) {
            return;
        }

        if (!restore) {
            ctx.getJdbcTemplate().update(
                    "update t_catalogAttribute set value = ? where key = 'state';",
                    CatalogState.ALIVE.toString()
            );
            createIndexes();
            ctx.getJdbcTemplate().update("analyze;");
        }
    }

    public boolean isCatalogAlive() {
        try {
            final CatalogState currentState = ctx.getJdbcTemplate().queryForObject(
                    "select value from t_catalogAttribute where key='state'",
                    CatalogState.class
            );
            return CatalogState.ALIVE.equals(currentState);
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }


    /**
     * Loads SQL script from resources directory to string
     *
     * @param scriptResource script descriptor
     * @return loaded script
     */
    private String loadSqlScript(@Nonnull Resource scriptResource) {
        final StringBuilder scriptBuilder = new StringBuilder();

        try (final BufferedReader scriptReader = new BufferedReader(new InputStreamReader(scriptResource.getInputStream()))) {
            String scriptLine;
            while ((scriptLine = scriptReader.readLine()) != null) {
                scriptBuilder.append(scriptLine).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return scriptBuilder.toString();
    }

    /**
     * Executes loaded SQL script with passed arguments
     */
    private void executeSqlScript(@Nonnull String script) {
        ctx.getJdbcTemplate().execute(script);
    }

    /**
     * Creates indexes after warming up state
     */
    private void createIndexes() {
        log.info("Creating common indexes...");
        executeSqlScript(commonIndexesInitScript);
        log.info("Common indexes created.");
    }
}
