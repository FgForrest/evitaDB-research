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

package io.evitadb.storage;

import io.evitadb.storage.exception.StorageException;
import io.evitadb.storage.kryo.ObservableInput;
import io.evitadb.storage.stream.RandomAccessFileInputStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * ReadOnlyHandle protects access to the {@link #exclusiveReadAccess}. No locking is required here - enveloping logic
 * is responsible for maintaining single threading access to the read only handle. Using {@link com.esotericsoftware.kryo.util.Pool}
 * is expected - this effectively excludes the possibility to use the resource in parallel. Locking in this class would
 * only add to latency.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ReadOnlyFileHandle {
	private final ExclusiveReadAccess exclusiveReadAccess;
	private final Runnable operationalCheck;

	public ReadOnlyFileHandle(Path targetFile, boolean computeCRC32C, Runnable operationalCheck) {
		this.operationalCheck = operationalCheck;
		try {
			final ObservableInput<RandomAccessFileInputStream> input = new ObservableInput<>(
				new RandomAccessFileInputStream(
					new RandomAccessFile(targetFile.toFile(), "r"),
					true
				)
			);
			final ObservableInput<RandomAccessFileInputStream> readFile = computeCRC32C ? input.computeCRC32() : input;
			this.exclusiveReadAccess = new ExclusiveReadAccess(readFile);
		} catch (FileNotFoundException ex) {
			throw new StorageException("Target file " + targetFile + " cannot be opened!", ex);
		}
	}

	/**
	 * This method executed the specified operation (logic) with getting read lock.
	 * @param logic
	 * @param <T>
	 * @return
	 */
	public <T> T execute(Function<ExclusiveReadAccess, T> logic) {
		operationalCheck.run();
		return logic.apply(this.exclusiveReadAccess);
	}

	/**
	 * This method executed the specified operation (logic) with getting read lock.
	 * Operational check is not used in this method.
	 *
	 * @param logic
	 * @param <T>
	 * @return
	 */
	public <T> T executeIgnoringOperationalCheck(Function<ExclusiveReadAccess, T> logic) {
		return logic.apply(this.exclusiveReadAccess);
	}

	/**
	 * This method closes the read handle ignoring the current lock.
	 */
	public void forceClose() {
		exclusiveReadAccess.close();
	}

	@RequiredArgsConstructor
	public static class ExclusiveReadAccess {
		@Getter private final ObservableInput<RandomAccessFileInputStream> readOnlyStream;

		public void close() {
			readOnlyStream.close();
		}
	}

}