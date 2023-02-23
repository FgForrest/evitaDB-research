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

import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.storage.model.memTable.StorageRecord;

/**
 * This interface marks all {@link StorageRecord} - or their payloads which keys are computed with the help of {@link KeyCompressor}.
 * This fact complicates access to the record id in case {@link KeyCompressor} is not flushed to the disk and the assigned
 * IDs are still present in volatile memory that cannot be easily accessed in thread safe manner. These ids are accessible
 * via {@link io.evitadb.api.mutation.StorageContainerBuffer#nonFlushedCompressedId} field and in temporary memory.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 * @see io.evitadb.api.mutation.StorageContainerBuffer
 */
public interface RecordWithCompressedId<T extends Comparable<T>> {

	/**
	 * Returns instance of the source key, that is required for unique part id computation.
	 * The key must uniquely identify the record among all other records both of the same type. You need to be able
	 * to fully compute {@link EntityStoragePart#getUniquePartId()} based on information stored in this key. The key
	 * must have proper equals and hashCode implementation and also {@link Comparable} interface is recommended to be
	 * implemented by the key.
	 */
	T getStoragePartSourceKey();

}
