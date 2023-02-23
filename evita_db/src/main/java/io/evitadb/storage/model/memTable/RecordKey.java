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

package io.evitadb.storage.model.memTable;

import io.evitadb.storage.MemTable;
import lombok.Data;

import java.io.Serializable;

/**
 * Each record that is stored via {@link StorageRecord} and maintained by {@link MemTable}
 * must be uniquely identified by this key.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class RecordKey implements Serializable, Comparable<RecordKey> {
	private static final long serialVersionUID = 7212147121525140183L;
	/**
	 * Id of the record type gathered from {@link MemTableRecordType#idFor(Class)}
	 */
	private final byte recordType;
	/**
	 * Primary key of the record.
	 */
	private final long primaryKey;

	/**
	 * Comparable keys are optimal for HashMaps handling.
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(RecordKey o) {
		final int result = Byte.compare(recordType, o.recordType);
		return result == 0 ? Long.compare(primaryKey, o.primaryKey) : result;
	}

}
