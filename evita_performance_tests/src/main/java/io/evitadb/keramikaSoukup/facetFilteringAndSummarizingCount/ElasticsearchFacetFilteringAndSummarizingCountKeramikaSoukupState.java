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

package io.evitadb.keramikaSoukup.facetFilteringAndSummarizingCount;

import io.evitadb.api.EsCatalog;
import io.evitadb.api.EsEntityCollection;
import io.evitadb.api.EsEvitaSession;
import io.evitadb.api.EsTransaction;
import io.evitadb.api.configuration.EsEvitaCatalogConfiguration;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.client.facetFilteringAndSummarizingCount.ClientFacetFilteringAndSummarizingCountState;
import io.evitadb.keramikaSoukup.KeramikaSoukupDataSource;
import io.evitadb.setup.ElasticReusableCatalogSetup;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Evita DB Elastic search implementation specific implementation of {@link ClientFacetFilteringAndSummarizingCountState}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@State(Scope.Benchmark)
public class ElasticsearchFacetFilteringAndSummarizingCountKeramikaSoukupState
	extends ClientFacetFilteringAndSummarizingCountState<EsEvitaRequest, EsEvitaCatalogConfiguration, EsEntityCollection, EsCatalog, EsTransaction, EsEvitaSession>
	implements ElasticReusableCatalogSetup, KeramikaSoukupDataSource {

	@Override
	public String getCatalogName() {
		return KeramikaSoukupDataSource.super.getCatalogName();
	}

}
