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

package io.evitadb.setup;

import io.evitadb.api.EsEvita;
import io.evitadb.storage.configuration.ElasticClientUtil;

/**
 * Base implementation for Elasticsearch tests that allow catalog recurring usage.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface ElasticReusableCatalogSetup
	extends ElasticCatalogSetup {

	@Override
	default EsEvita createEvitaInstanceFromExistingData(String catalogName) {
		EsEvita instance = getInstance(catalogName);
		instance.updateCatalog(catalogName, esEvitaSession -> {esEvitaSession.goLiveAndClose();});
		return instance;
	}

	@Override
	default boolean isCatalogAvailable(String catalogName) {
		return ElasticClientUtil.isCatalogInitialized(catalogName);
	}

	@Override
	default boolean shouldStartFromScratch() {
		return false;
	}
}
