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

package io.evitadb.storage.model.storageParts;

import lombok.Data;

/**
 * DTO that identifies the {@link StoragePart} already stored in {@link io.evitadb.storage.MemTable}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class PersistedStoragePartKey implements Comparable<PersistedStoragePartKey> {
	private final long primaryKey;
	private final Class<? extends StoragePart> containerClass;

	@Override
	public int compareTo(PersistedStoragePartKey o) {
		final int typeComparison = Integer.compare(System.identityHashCode(containerClass), System.identityHashCode(o.containerClass));
		if (typeComparison == 0) {
			return Long.compare(primaryKey, o.primaryKey);
		} else {
			return typeComparison;
		}
	}

}
