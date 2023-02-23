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

package io.evitadb.api;

import io.evitadb.api.utils.Assert;
import lombok.Getter;

/**
 * This class wraps access to the Transaction object specific for Evita DB implementation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class TransactionBase implements AutoCloseable {
	/**
	 * Rollback only flag.
	 */
	@Getter private boolean rollbackOnly;
	/**
	 * Flag that marks this instance closed and unusable. Once closed it can never be opened again.
	 */
	@Getter private boolean closed;

	/**
	 * Marks this transaction for rollback only. Ie. when transaction terminates, all changes will be rolled back.
	 */
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	@Override
	public void close() {
		Assert.isTrue(!closed, "Transaction was already closed!");
		closed = true;
	}

}
