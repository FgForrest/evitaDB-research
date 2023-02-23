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

package io.evitadb.sequence;

import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This class represents unique sequence key, that distinguishes particular sequence from other sequences.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class SequenceKey {
	/**
	 * Catalog name this sequence is attached to.
	 */
	@Nonnull private final String catalog;
	/**
	 * Type of the sequence in the {@link io.evitadb.api.EntityCollection}.
	 */
	@Nonnull private final SequenceType type;
	/**
	 * Entity collection type this sequence is attached to.
	 * Null in case the sequence targets entire catalog.
	 */
	@Nullable private final Serializable entityType;

}
