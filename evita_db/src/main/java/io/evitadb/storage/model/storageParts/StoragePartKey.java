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

import io.evitadb.api.EntityCollection;
import lombok.Data;

/**
 * StoragePartKey represents a key, that can be any time converted via
 * {@link EntityCollection#createStoragePartFor(io.evitadb.storage.model.storageParts.StoragePartKey)} to the
 * {@link StoragePart} that can be stored via {@link io.evitadb.storage.MemTable#put(long, long, StoragePart)}.
 *
 * Storage keys represents a way how to postpone persistence of frequently modified data structures such as
 * {@link io.evitadb.index.EntityIndex EntityIndexes} so that multiple entities can alter them in a single or multiple
 * serially executed transactions and persist them at the end so that I/O is minimized.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class StoragePartKey implements Comparable<StoragePartKey> {
	/**
	 * The original key must uniquely identify the record among all other records both of the same type. You need to be able
	 * to fully compute {@link StoragePart#getUniquePartId()} based on information stored in this key. The key
	 * must have proper equals and hashCode implementation and also {@link Comparable} interface is recommended to be
	 * implemented by the key.
	 *
	 * @return
	 */
	private final Comparable<?> originalKey;
	/**
	 * The type of the storage part that distinguishes record of this type from other StorageParts.
	 */
	private final Class<? extends StoragePart> containerClass;


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public int compareTo(StoragePartKey o) {
		final int typeComparison = Integer.compare(System.identityHashCode(containerClass), System.identityHashCode(o.containerClass));
		if (typeComparison == 0) {
			return ((Comparable) originalKey).compareTo(o.originalKey);
		} else {
			return typeComparison;
		}
	}

}
