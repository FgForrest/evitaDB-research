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

package io.evitadb.api.data;

/**
 * This interface allows to unite access to the full internals object comparison. This comparison is usually done in
 * {@link #equals(Object)} method but in Evita immutable objects are {@link #hashCode()} and {@link #equals(Object)}
 * optimized for sped and use only primary key and version. For tests and other purposes we need also full comparison
 * method that check entire contents for equality.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface ContentComparator<T> {

	/**
	 * Returns true if single item deep wise differs from the item of other object. This and otherObject must be of
	 * equal types.
	 *
	 * @param otherObject
	 * @return
	 */
	boolean differsFrom(T otherObject);

}