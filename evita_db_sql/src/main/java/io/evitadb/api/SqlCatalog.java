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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.evitadb.api.configuration.DatabaseConnectionConfiguration;
import io.evitadb.api.configuration.HierarchyConfiguration;
import io.evitadb.api.configuration.SqlEvitaCatalogConfiguration;
import io.evitadb.api.exception.InvalidSchemaMutationException;
import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.CatalogContext;
import io.evitadb.storage.CatalogRepository;
import io.evitadb.storage.EntityCollectionDescriptor;
import io.evitadb.storage.HierarchyManager;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.evitadb.api.utils.Assert.notNull;

/**
 * Implementation of {@link CatalogBase} for PostgreSQL implementation of {@link SqlEvita}
 *
 * @author Lukáš Hornych 2021
 */
public class SqlCatalog extends CatalogBase<SqlEvitaRequest, SqlEvitaCatalogConfiguration, SqlEntityCollection> {

    private static final String DEBUG_MODE_PROPERTY = "sql.catalog.debug";
    /**
     * If catalog is in debug mode
     */
    @Getter
    private final boolean inDebugMode;

    @Getter
    @Nonnull
    protected final CatalogContext ctx;

    @Nonnull
    protected final CatalogRepository catalogRepository;

    /**
     * Memory store for catalogs.
     */
    private final Map<Serializable, SqlEntityCollection> entityCollections = new ConcurrentHashMap<>();


    public SqlCatalog(@Nonnull final SqlEvitaCatalogConfiguration configuration) {
        super(configuration);

        this.inDebugMode = parseDebugModeProperty();
        this.ctx = initContext(configuration);
        this.catalogRepository = new CatalogRepository(ctx);

        //todo delete - this is for testing purposes
//        System.out.println("***************************** Database settings *********************************");
//        List<DatabaseSetting> databaseSettings = ctx.getNpJdbcTemplate()
//                .query("show all", (rs, rowNum) ->
//                        new DatabaseSetting(rs.getString("name"), rs.getString("setting")));
//
//        databaseSettings.forEach(s -> System.out.println(s.getName() +" = "+ s.getSetting()));
        //todo end of delete

        // database structure initialization
        catalogRepository.createCatalogStructure();

        initEntityCollections();

        // switch to alive state if previous instance already was in alive state
        if (this.catalogRepository.isCatalogAlive()) {
            goLive(true);
        }
    }

    //todo delete - this class is for testing purposes
    @Data
    private static class DatabaseSetting {
        private final String name;
        private final String setting;
    }

    @Override
    public Set<Serializable> getEntityTypes() {
        return entityCollections.keySet();
    }

    @Nullable
    @Override
    public SqlEntityCollection getCollectionForEntity(@Nonnull Serializable entityType) {
        return entityCollections.get(entityType);
    }

    @Nonnull
    @Override
    public SqlEntityCollection getOrCreateCollectionForEntity(@Nonnull Serializable entityType) {
        notNull(entityType,"In getOrCreateCollectionForEntity, entityType cannot be Null");

        return entityCollections.computeIfAbsent(entityType, t -> {
            final String uid = catalogRepository.createNewCollection(t);
            return new SqlEntityCollection(ctx, uid, t, null, supportsTransaction());
        });
    }

    @Nonnull
    @Override
    public SqlEntityCollection createCollectionForEntity(@Nonnull EntitySchema entitySchema) throws InvalidSchemaMutationException {
        notNull(entitySchema, "Entity schema is required to create new collection of entities.");

        final String uid = catalogRepository.createNewCollection(entitySchema.getName());
        //noinspection ConstantConditions
        return entityCollections.put(entitySchema.getName(), new SqlEntityCollection(ctx, uid, entitySchema, supportsTransaction()));
    }

    @Override
    public boolean deleteCollectionOfEntity(@NonNull Serializable entityType) {
        notNull(entityType,"In deleteCollectionOfEntity, entityType cannot be Null");

        catalogRepository.deleteCollection(entityType);
        entityCollections.remove(entityType);

        return true;
    }

    @Override
    boolean goLive() {
        return goLive(false);
    }

    @Override
    void flush() {
        entityCollections.values().forEach(SqlEntityCollection::flush);
    }

    @Override
    void terminate() {
        entityCollections.values().forEach(SqlEntityCollection::terminate);
        entityCollections.clear();

        ctx.close();
    }


    /**
     * Founds out if catalog is configured to be in debug mode
     */
    private boolean parseDebugModeProperty() {
        final String debugModeProperty = System.getProperty(DEBUG_MODE_PROPERTY);
        if (debugModeProperty == null) {
            return false;
        }
        return Boolean.parseBoolean(debugModeProperty);
    }

    /**
     * Initializes complete catalog context: connection to underlying database, hierarchy and other helpers.
     *
     * @param configuration catalog configuration
     * @return ready context
     */
    private CatalogContext initContext(@Nonnull SqlEvitaCatalogConfiguration configuration) {
        // database connection
        final DatabaseConnectionConfiguration databaseConnectionConfiguration = configuration.getDatabaseConnection();
        final HikariConfig dataSourceConfig = new HikariConfig();
        dataSourceConfig.setJdbcUrl(databaseConnectionConfiguration.getJdbcUrl());
        dataSourceConfig.setUsername(databaseConnectionConfiguration.getDbUsername());
        dataSourceConfig.setPassword(databaseConnectionConfiguration.getDbPassword());
        dataSourceConfig.setMaximumPoolSize(32);
        final DataSource dataSource = new HikariDataSource(dataSourceConfig);

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final NamedParameterJdbcTemplate npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        final JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);

        // hierarchy management initialization
        final HierarchyConfiguration hierarchyConfiguration = configuration.getHierarchy();
        final HierarchyManager hierarchyManager = new HierarchyManager(
                hierarchyConfiguration.getLevels(),
                hierarchyConfiguration.getSectionSize()
        );

        return new CatalogContext(
                this,
                dataSource,
                jdbcTemplate,
                npJdbcTemplate,
                transactionManager,
                hierarchyManager
        );
    }

    /**
     * Initializes entity collection instances for stored collections
     */
    private void initEntityCollections() {
        for (EntityCollectionDescriptor descriptor : catalogRepository.findAllCollections()) {
            ctx.addEntityType(descriptor.getType());
            entityCollections.put(
                    descriptor.getType(),
                    new SqlEntityCollection(
                            ctx,
                            descriptor.getUid(),
                            descriptor.getType(),
                            descriptor.getSerializationHeader(),
                            supportsTransaction())
            );
        }
    }

    /**
     * Changes state of the catalog from {@link CatalogState#WARMING_UP} to {@link CatalogState#ALIVE}.
     *
     * @param restore if restoring previous alive state
     * @return if state was switched
     */
    private boolean goLive(boolean restore) {
        final boolean switched = super.goLive();
        if (switched) {
            catalogRepository.goLive(restore);
            ctx.getHierarchyManager().goLive();
            entityCollections.values().forEach(c -> c.goLive(restore));
        }

        return switched;
    }
}
