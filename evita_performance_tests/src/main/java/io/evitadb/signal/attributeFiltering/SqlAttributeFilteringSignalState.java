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

package io.evitadb.signal.attributeFiltering;

import io.evitadb.api.SqlCatalog;
import io.evitadb.api.SqlEntityCollection;
import io.evitadb.api.SqlEvitaSession;
import io.evitadb.api.SqlTransaction;
import io.evitadb.api.configuration.SqlEvitaCatalogConfiguration;
import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.client.attributeFiltering.ClientAttributeFilteringState;
import io.evitadb.setup.SqlCatalogReusableSetup;
import io.evitadb.signal.SignalDataSource;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Evita DB PostgreSQL implementation specific implementation of {@link ClientAttributeFilteringState}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@State(Scope.Benchmark)
public class SqlAttributeFilteringSignalState
	extends ClientAttributeFilteringState<SqlEvitaRequest, SqlEvitaCatalogConfiguration, SqlEntityCollection, SqlCatalog, SqlTransaction, SqlEvitaSession>
	implements SqlCatalogReusableSetup, SignalDataSource {

	@Override
	public String getCatalogName() {
		return SignalDataSource.super.getCatalogName();
	}

}
