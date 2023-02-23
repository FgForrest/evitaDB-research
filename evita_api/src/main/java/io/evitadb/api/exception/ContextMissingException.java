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

package io.evitadb.api.exception;

import io.evitadb.api.data.PricesContract;
import io.evitadb.api.query.Query;

/**
 * This exception is thrown when {@link PricesContract#getSellingPrice()} is called and there is no {@link Query} known
 * that would provide sufficient data - or the query lacks price related constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ContextMissingException extends IllegalArgumentException {
	private static final long serialVersionUID = 2082957443834244040L;

	public ContextMissingException() {
		super("Query context is missing. You need to use method getSellingPrice(Currency, ZonedDateTime, Serializable...) and provide the context on your own.");
	}

}
