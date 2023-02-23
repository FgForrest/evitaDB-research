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

import io.evitadb.api.configuration.SqlEvitaCatalogConfiguration;
import io.evitadb.api.io.SqlEvitaRequest;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static io.evitadb.api.utils.Assert.notNull;

/**
 * Implementation of {@link EvitaBase} which uses PostgreSQL as underlying database system for storing and searching data.
 *
 * Touch 2022-06-21 08:43
 *
 * @author Lukáš Hornych 2021
 */
@Slf4j
public class SqlEvita extends EvitaBase<SqlEvitaRequest, SqlEvitaCatalogConfiguration, SqlEntityCollection, SqlCatalog, SqlTransaction, SqlEvitaSession> {

    /**
     * Creates new configured {@link SqlEvita} instance with necessary sources.
     *
     * @param catalogConfigurations configurations of individual catalogs
     */
    public SqlEvita(@Nonnull final SqlEvitaCatalogConfiguration... catalogConfigurations) {
        super(catalogConfigurations);
    }

    @Override
    protected SqlCatalog createCatalog(@Nonnull final SqlEvitaCatalogConfiguration configuration) {
        notNull(configuration,"In createCatalog, configuration cannot be Null");
        return new SqlCatalog(configuration);
    }

    @Override
    protected SqlEvitaSession createReadWriteSession(@Nonnull SqlCatalog catalog, @Nonnull Consumer<SqlCatalog> updatedCatalogCallback) {
        notNull(catalog,"In createSession, catalog cannot be Null");
        return new SqlEvitaSession(catalog, updatedCatalogCallback);
    }

    @Override
    protected SqlEvitaSession createReadOnlySession(@Nonnull SqlCatalog catalog) {
        notNull(catalog,"In createSession, catalog cannot be Null");
        return new SqlEvitaSession(catalog);
    }
}
