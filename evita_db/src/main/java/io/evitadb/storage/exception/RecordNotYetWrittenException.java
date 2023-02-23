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

package io.evitadb.storage.exception;

import io.evitadb.storage.MemTable;

/**
 * Exception is thrown when record was written to the {@link io.evitadb.storage.MemTable} but {@link MemTable#flush(long)}
 * method was not yet called and record is present in intermediate buffers and was not yet fully written to the disk.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class RecordNotYetWrittenException extends IllegalArgumentException {
	private static final long serialVersionUID = -9102021006732628881L;

	public RecordNotYetWrittenException(Throwable cause) {
		super(cause);
	}

}
