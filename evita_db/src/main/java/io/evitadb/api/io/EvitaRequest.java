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

package io.evitadb.api.io;

import io.evitadb.api.query.Query;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;

/**
 * {@link EvitaRequest} is internal class (Evita accepts simple {@link Query} object - see {@link io.evitadb.api.EvitaSession#query(Query, Class)})
 * that envelopes the input query. Evita request can be used to implement methods that extract crucial informations
 * from the input query and cache those extracted information to avoid paying parsing costs twice in single request.
 * See examples in {@link EvitaResponseBase} super class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EvitaRequest extends EvitaRequestBase {

	public EvitaRequest(@Nonnull Query query, @Nonnull ZonedDateTime alignedNow) {
		super(query, alignedNow);
	}

}
