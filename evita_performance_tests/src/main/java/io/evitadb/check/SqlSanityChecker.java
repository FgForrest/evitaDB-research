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

package io.evitadb.check;

import io.evitadb.api.*;
import io.evitadb.api.configuration.DatabaseConnectionConfiguration;
import io.evitadb.api.configuration.SqlEvitaCatalogConfiguration;
import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.test.SqlStorageTestSupport;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;

import static io.evitadb.test.SqlStorageTestSupport.STORAGE_PASSWORD;
import static io.evitadb.test.SqlStorageTestSupport.STORAGE_USER;

/**
 * Concrete implementation of {@link SanityChecker} that targets in-memory implementation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class SqlSanityChecker extends SanityChecker<SqlEvitaRequest, SqlEvitaCatalogConfiguration, SqlEntityCollection, SqlCatalog, SqlTransaction, SqlEvitaSession> {

    public static void main(String... args) throws FileNotFoundException {
        new SqlSanityChecker().execute(args);
    }

    @Override
    protected SqlEvita createEvitaInstance(@Nonnull String catalogName) {
        return new SqlEvita(
                new SqlEvitaCatalogConfiguration(
                        catalogName,
                        new DatabaseConnectionConfiguration(
                                SqlStorageTestSupport.getStorageConnectionHost(),
                                SqlStorageTestSupport.getDbNameForCatalog(catalogName),
                                STORAGE_USER,
                                STORAGE_PASSWORD
                        )
                )
        );
    }
}
