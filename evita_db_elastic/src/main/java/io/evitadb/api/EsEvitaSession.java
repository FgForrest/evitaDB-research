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
import io.evitadb.api.query.Query;

import javax.annotation.Nonnull;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * Session are created by the clients to envelope a "piece of work" with Evita DB. In web environment it's a good idea
 * to have session per request, in batch processing it's recommended to keep session per "record page" or "transaction".
 * There may be multiple {@link Transaction transactions} during single session instance life but there is no support
 * for transactional overlap - there may be at most single transaction open in single session. Session also caches
 * response to the queries - when client reads twice the same entity within the session (in different queries) he/she
 * should obtain the same reference to that entity (i.e. same in the sense of ==). Once transaction is committed cached
 * results are purged.
 * <p>
 * Note for implementors: transactional access doesn't need to be implemented in the early stages
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EsEvitaSession extends EvitaSessionBase<EsEvitaRequest, EsEvitaCatalogConfiguration, EsEntityCollection, EsCatalog, EsTransaction> {

	protected EsEvitaSession(EsCatalog catalog, ZoneId zoneId) {
		super(catalog);
	}

	protected EsEvitaSession(EsCatalog catalog, ZoneId zoneId, Consumer<EsCatalog> updatedCatalogCallback) {
		super(catalog, updatedCatalogCallback);
	}

	@Override
	public void clearCache() {
		// no cache in the simplistic implementation
	}

	@Override
	protected EsEvitaRequest createEvitaRequest(@Nonnull Query query, @Nonnull ZonedDateTime alignedNow) {
		return new EsEvitaRequest(query, alignedNow);
	}


	@Override
	protected EsTransaction createTransaction() {
		return new EsTransaction();
	}


}
