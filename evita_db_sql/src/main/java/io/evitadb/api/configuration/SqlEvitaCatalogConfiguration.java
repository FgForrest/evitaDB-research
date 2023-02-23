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

package io.evitadb.api.configuration;

import io.evitadb.api.SqlCatalog;
import io.evitadb.api.SqlEvita;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Configuration for {@link SqlCatalog}. Each catalog is represented by separate underlying PostgreSQL database,
 * so it is necessary to provide JDBC connection properties for catalog's database.
 *
 * @author Lukáš Hornych 2021
 */
@Getter
public class SqlEvitaCatalogConfiguration implements CatalogConfiguration {

    /**
     * Name of the {@link SqlCatalog} instance this configuration refers to. Name must be unique across all catalogs
     * inside same {@link SqlEvita} instance.
     */
    private final String name;

    /**
     * Configuration for connection to catalog's database
     */
    @Nonnull
    private final DatabaseConnectionConfiguration databaseConnection;

    /**
     * Configuration for entity collection hierarchies.
     */
    @Nonnull
    private final HierarchyConfiguration hierarchy;

    /**
     * Creates catalog configuration with default hierarchy configuration
     *
     * @param name name of the {@link SqlCatalog} instance this configuration refers to. Name must be unique across all catalogs
     *        inside same {@link SqlEvita} instance.
     * @param databaseConnection configuration for connection to catalog's database
     */
    public SqlEvitaCatalogConfiguration(@Nonnull String name,
                                        @Nonnull DatabaseConnectionConfiguration databaseConnection) {
        this(name, databaseConnection, new HierarchyConfiguration((short) 9, (short) 54));
    }

    /**
     * Creates catalog configuration
     *
     * @param name name of the {@link SqlCatalog} instance this configuration refers to. Name must be unique across all catalogs
     *        inside same {@link SqlEvita} instance.
     * @param databaseConnection configuration for connection to catalog's database
     * @param hierarchy configuration for entity collection hierarchies
     */
    public SqlEvitaCatalogConfiguration(@Nonnull String name,
                                        @Nonnull DatabaseConnectionConfiguration databaseConnection,
                                        @Nonnull HierarchyConfiguration hierarchy) {
        this.name = name;
        this.databaseConnection = databaseConnection;
        this.hierarchy = hierarchy;
    }
}
