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

package io.evitadb.api.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * Configuration options related to the key-value storage.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
@AllArgsConstructor
public class StorageOptions {
	/**
	 * This timeout represents a time in seconds that is tolerated to wait for lock acquiring. Locks are used to get
	 * handle to open file. Set of open handles is limited to {@link #maxOpenedReadHandles} for read operations and
	 * single write handle for write operations (only single thread is expected to append to a file).
	 */
	@Getter private final long lockTimeoutSeconds;
	/**
	 * This timeout represents a time that will MemTable wait for processes to release their read handles to files.
	 * After this timeout files will be closed by force and processes may experience an exception.
	 */
	@Getter private final long waitOnCloseSeconds;
	/**
	 * Output buffer size determines how big buffer will be held in memory for output purposes. The size of the buffer
	 * also limits the maximum size of the single record, that can be stored in MemTable.
	 */
	@Getter private final int outputBufferSize;
	/**
	 * Maximum number of simultaneously opened {@link java.io.InputStream} to MemTable file.
	 */
	@Getter private final int maxOpenedReadHandles;
	/**
	 * Contains setting that determined whether CRC32C checksums will be computed for written records and also whether
	 * the CRC32C checksum will be checked on record read.
	 */
	@Getter private final boolean computeCRC32C;
	/**
	 * If set to true MemTable will ignore existing target file and deletes it along with all contents and starts empty.
	 * BEWARE!!! This is destructive option - use with care!
	 */
	@Getter private final boolean bootEmpty;
	/**
	 * If set to true MemTable will be booted from existing file but when reading error occurs the file is deleted and
	 * MemTable starts empty - but it starts and doesn't kill the process.
	 */
	@Getter private final boolean bootEmptyOnError;

	/**
	 * Recommended settings constructor.
	 */
	public StorageOptions(int maxOpenedReadHandles) {
		this.lockTimeoutSeconds = 5;
		this.waitOnCloseSeconds = 5;
		this.outputBufferSize = 2_097_152;
		this.computeCRC32C = true;
		this.maxOpenedReadHandles = maxOpenedReadHandles;
		this.bootEmpty = false;
		this.bootEmptyOnError = false;
	}
}
