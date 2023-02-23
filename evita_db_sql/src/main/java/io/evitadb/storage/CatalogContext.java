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

import com.zaxxer.hikari.HikariDataSource;
import io.evitadb.api.SqlCatalog;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all data needed throughout evita catalog instance (such as database connection and so on).
 *
 * @author Lukáš Hornych 2021
 */
@Data
public class CatalogContext {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    /**
     * Catalog owning this context
     */
    private final SqlCatalog catalog;

    /**
     * If catalog is in debug mode
     */
    private final boolean inDebugMode;

    @Getter(AccessLevel.NONE)
    private final DataSource dataSource;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    private final HierarchyManager hierarchyManager;

    /**
     * Mapping of serialized types of collection to original types for easier reconstruction from db
     */
    @Getter(AccessLevel.NONE)
    private final Map<String, Serializable> serializedEntityTypesToOriginalEntityTypes = new ConcurrentHashMap<>();
    /**
     * Mapping of original types of collection to serialized types
     */
    @Getter(AccessLevel.NONE)
    private final Map<Serializable, String> originalEntityTypesToSerializedEntityTypes = new ConcurrentHashMap<>();

    public CatalogContext(SqlCatalog catalog,
                          DataSource dataSource,
                          JdbcTemplate jdbcTemplate,
                          NamedParameterJdbcTemplate npJdbcTemplate,
                          PlatformTransactionManager transactionManager,
                          HierarchyManager hierarchyManager) {
        this.catalog = catalog;
        this.inDebugMode = catalog.isInDebugMode();
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
        this.transactionManager = transactionManager;
        this.hierarchyManager = hierarchyManager;
        this.hierarchyManager.setCatalogContext(this);
    }

    /**
     * Adds collection's entity type to registry with its serialized version
     *
     * @return serialized entity type
     */
    public TypedValue<String> addEntityType(@Nonnull Serializable entityType) {
        final TypedValue<String> serializedEntityType = STRING_TYPED_VALUE_SERIALIZER.serialize(entityType);
        serializedEntityTypesToOriginalEntityTypes.put(serializedEntityType.getSerializedValue(), entityType);
        originalEntityTypesToSerializedEntityTypes.put(entityType, serializedEntityType.getSerializedValue());

        return serializedEntityType;
    }

    /**
     * Returns original entity type for collection which corresponds to serialized entity type
     */
    public Serializable getOriginalEntityType(@Nonnull String serializedEntityType) {
        return serializedEntityTypesToOriginalEntityTypes.get(serializedEntityType);
    }

    /**
     * Returns already serialized entity type for original entity type
     */
    public String getSerializedEntityType(@Nonnull Serializable originalEntityType) {
        return originalEntityTypesToSerializedEntityTypes.get(originalEntityType);
    }

    public void close() {
        ((HikariDataSource) dataSource).close();
    }
}
