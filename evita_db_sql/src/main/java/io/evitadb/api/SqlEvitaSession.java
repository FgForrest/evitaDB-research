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
import io.evitadb.api.query.Query;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * Implementation of {@link EvitaSessionBase} for PostgreSQL implementation of {@link SqlEvita}.
 *
 * @author Lukáš Hornych 2021
 */
public class SqlEvitaSession extends EvitaSessionBase<SqlEvitaRequest, SqlEvitaCatalogConfiguration, SqlEntityCollection, SqlCatalog, SqlTransaction> {

    protected SqlEvitaSession(SqlCatalog catalog) {
        super(catalog);
    }

    protected SqlEvitaSession(SqlCatalog catalog, Consumer<SqlCatalog> updatedCatalogCallback) {
        super(catalog, updatedCatalogCallback);
    }

    @Override
    public void clearCache() {
        // no cache is implemented yet
    }

    @Override
    protected SqlEvitaRequest createEvitaRequest(@Nonnull Query query, @Nonnull ZonedDateTime alignedNow) {
        return new SqlEvitaRequest(query, alignedNow);
    }

    @Override
    protected SqlTransaction createTransaction() {
        return new SqlTransaction();
    }
}
