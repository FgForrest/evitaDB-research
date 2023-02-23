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
import io.evitadb.api.configuration.EsEvitaCatalogConfiguration;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.test.TestFileSupport;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;

/**
 * Concrete implementation of {@link SanityChecker} that targets es implementation.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 */
public class ElasticsearchSanityChecker extends SanityChecker<EsEvitaRequest, EsEvitaCatalogConfiguration, EsEntityCollection, EsCatalog, EsTransaction, EsEvitaSession>
        implements TestFileSupport {

    public static void main(String[] args) throws FileNotFoundException {
        new ElasticsearchSanityChecker().execute(args);
    }

    @Override
    protected EsEvita createEvitaInstance(@Nonnull String catalogName) {
        EsEvita esEvita = new EsEvita(new EsEvitaCatalogConfiguration(catalogName));
        esEvita.updateCatalog(catalogName, esEvitaSession -> {
            esEvitaSession.goLiveAndClose();
        });
        return esEvita;
    }
}
