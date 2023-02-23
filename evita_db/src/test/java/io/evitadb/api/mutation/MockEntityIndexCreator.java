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

package io.evitadb.api.mutation;

import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexMaintainer;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This mock object is used in tests to provide entity index without necessity to load it from {@link io.evitadb.storage.MemTable}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
class MockEntityIndexCreator implements EntityIndexMaintainer {
	private final EntityIndex index;

	@Nonnull
	@Override
	public EntityIndex getOrCreateIndex(@Nonnull EntityIndexKey entityIndexKey) {
		return index;
	}

	@Nullable
	@Override
	public EntityIndex getIndexIfExists(@Nonnull EntityIndexKey entityIndexKey) {
		return index;
	}

	@Override
	public void removeIndex(@Nonnull EntityIndexKey entityIndexKey) {
		throw new UnsupportedOperationException("Method not supported.");
	}
}
