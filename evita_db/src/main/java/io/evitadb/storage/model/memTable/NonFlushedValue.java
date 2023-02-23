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

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * This DTO allows to keep the object that was written to the {@link io.evitadb.storage.MemTable} but its location
 * was not yet flushed to the disk.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class NonFlushedValue implements Serializable {
	private static final long serialVersionUID = -4467999274212489366L;

	/**
	 * Contains primary key of the stored container.
	 */
	private final long primaryKey;
	/**
	 * Contains type of the record stored on specified position.
	 */
	private final byte recordType;
	/**
	 * Contains coordinates to the space in the file that is occupied by this record.
	 */
	private final FileLocation fileLocation;

	/**
	 * Returns true if this non flushed value represents removal of the record.
	 * @return
	 */
	public boolean isRemoval() {
		return recordType < 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NonFlushedValue that = (NonFlushedValue) o;
		return primaryKey == that.primaryKey && Math.abs(recordType) == Math.abs(that.recordType) && fileLocation.equals(that.fileLocation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(primaryKey, Math.abs(recordType), fileLocation);
	}
}
