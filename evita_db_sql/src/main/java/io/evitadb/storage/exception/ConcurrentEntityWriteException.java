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

/**
 * Exception thrown when multiple threads try to write entity at the same time
 */
public class ConcurrentEntityWriteException extends RuntimeException {

    private static final long serialVersionUID = -5684990375812847945L;

    public ConcurrentEntityWriteException() {
    }

    public ConcurrentEntityWriteException(String message) {
        super(message);
    }

    public ConcurrentEntityWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentEntityWriteException(Throwable cause) {
        super(cause);
    }

    public ConcurrentEntityWriteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
