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

package io.evitadb.index.array;

import javax.annotation.concurrent.ThreadSafe;

/**
 * This interface must be implemented by all objects in order to be placed inside {@link TransactionalComplexObjArray}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@ThreadSafe
public interface TransactionalObject<T> {

	/**
	 * Method implementation must remove entire diff memory from the current transaction. If object maintains inner
	 * objects, their memory must be removed as well.
	 */
	void removeFromTransactionalMemory();

	/**
	 * Method implementation must create deep clone of the object itself and all transactionally active inner objects.
	 * @return
	 */
	T makeClone();

}
