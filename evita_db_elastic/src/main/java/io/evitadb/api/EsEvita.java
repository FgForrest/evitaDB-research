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


import io.evitadb.api.configuration.EsEvitaCatalogConfiguration;
import io.evitadb.api.io.EsEvitaRequest;

import javax.annotation.Nonnull;
import javax.xml.catalog.Catalog;
import java.time.ZoneId;
import java.util.function.Consumer;

/**
 * This is main entrance to the Evita DB. All basic methods are specified in the superclass {@link EvitaBase}.
 * Implementations just need to implement access to the {@link Catalog} and work with {@link EvitaSession}.
 * <p>
 * Currently it references simplistic memory implementation - each implementation should go its way.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EsEvita extends EvitaBase<EsEvitaRequest, EsEvitaCatalogConfiguration, EsEntityCollection, EsCatalog, EsTransaction, EsEvitaSession> {

	public EsEvita(EsEvitaCatalogConfiguration... configs) {
		super(configs);
	}

	@Override
	protected EsCatalog createCatalog(@Nonnull EsEvitaCatalogConfiguration config) {
		return new EsCatalog(config);
	}

	@Override
	protected EsEvitaSession createReadWriteSession(@Nonnull EsCatalog catalog, @Nonnull Consumer<EsCatalog> updatedCatalogCallback) {
		return new EsEvitaSession(catalog, ZoneId.systemDefault(), updatedCatalogCallback);
	}

	@Override
	protected EsEvitaSession createReadOnlySession(@Nonnull EsCatalog catalog) {
		return new EsEvitaSession(catalog, ZoneId.systemDefault());
	}


}
