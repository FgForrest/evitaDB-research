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

package io.evitadb.signal.facetAndHierarchyFiltering;

import io.evitadb.api.EsCatalog;
import io.evitadb.api.EsEntityCollection;
import io.evitadb.api.EsEvitaSession;
import io.evitadb.api.EsTransaction;
import io.evitadb.api.configuration.EsEvitaCatalogConfiguration;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.client.facetAndHierarchyFiltering.ClientFacetAndHierarchyFilteringState;
import io.evitadb.setup.ElasticReusableCatalogSetup;
import io.evitadb.signal.SignalDataSource;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Evita DB Elastic search implementation specific implementation of {@link ClientFacetAndHierarchyFilteringState}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@State(Scope.Benchmark)
public class ElasticsearchFacetAndHierarchyFilteringSignalState
	extends ClientFacetAndHierarchyFilteringState<EsEvitaRequest, EsEvitaCatalogConfiguration, EsEntityCollection, EsCatalog, EsTransaction, EsEvitaSession>
	implements ElasticReusableCatalogSetup, SignalDataSource {

	@Override
	public String getCatalogName() {
		return SignalDataSource.super.getCatalogName();
	}

}