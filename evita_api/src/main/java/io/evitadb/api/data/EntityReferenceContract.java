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

import io.evitadb.api.data.structure.Entity;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * This class represents reference to any Evita entity and can is returned by default for all
 * queries that don't require loading additional data.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface EntityReferenceContract<T extends Comparable<T> & EntityReferenceContract<T>> extends Serializable, Comparable<T> {

	/**
	 * Reference to {@link Entity#getType()} of the referenced entity. Might be also anything {@link Serializable}
	 * that identifies type some external resource not maintained by Evita.
	 */
	@Nonnull
	Serializable getType();

	/**
	 * Reference to {@link Entity#getPrimaryKey()} of the referenced entity. Might be also any integer
	 * that uniquely identifies some external resource of type {@link #getType()} not maintained by Evita.
	 */
	int getPrimaryKey();

	/**
	 * Default comparison function for EntityReferenceContracts.
	 */
	default int compareReferenceContract(EntityReferenceContract<T> o) {
		final int primaryComparison = Integer.compare(getPrimaryKey(), o.getPrimaryKey());
		if (primaryComparison == 0) {
			final Serializable thisReferencedEntity = getType();
			final Serializable thatReferencedEntity = o.getType();
			if (thisReferencedEntity.getClass().equals(o.getType().getClass()) && this.getType() instanceof Comparable) {
				//noinspection unchecked, rawtypes
				return ((Comparable) thisReferencedEntity).compareTo(thatReferencedEntity);
			} else {
				return thisReferencedEntity.toString().compareTo(thatReferencedEntity.toString());
			}
		} else {
			return primaryComparison;
		}
	}

}
