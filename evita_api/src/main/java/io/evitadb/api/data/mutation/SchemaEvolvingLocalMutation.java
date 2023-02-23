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

package io.evitadb.api.data.mutation;

import io.evitadb.api.schema.EntitySchema;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;

/**
 * This is local alternative for {@link SchemaEvolvingMutation} that verifies / evolves schema on local mutation level.
 * Entire {@link SchemaEvolvingMutation} is composed of set of {@link SchemaEvolvingLocalMutation local mutations}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
public interface SchemaEvolvingLocalMutation<T, S extends Comparable<S>> extends SchemaEvolvingMutation, LocalMutation<T, S> {

	/**
	 * Skip token is used to quickly skip analogous local mutations to speed verification / evolution process up.
	 * For example when there are several mutations to prices we need to check only first mutation because
	 * {@link EntitySchema} defines only {@link EntitySchema#isWithPrice()} behaviour. Other price mutations don't
	 * need to check this information again.
	 */
	@Nonnull
	Serializable getSkipToken();

}
