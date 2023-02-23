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

package io.evitadb.api.data.structure;

import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.utils.MemoryMeasuringConstants;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;

/**
 * This class is used as nested object in {@link Reference} to reference external entities that are represented by reference
 * itself or its reference group. Also may represent reference to any Evita entity and can is returned by default for all
 * queries that don't require loading additional data.
 *
 * Class is immutable on purpose - we want to support caching the entities in a shared cache and accessed by many threads.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
@Immutable
@ThreadSafe
public class EntityReference implements EntityReferenceContract<EntityReference> {
	private static final long serialVersionUID = 7432447904441796055L;

	/**
	 * Reference to {@link Entity#getType()} of the referenced entity. Might be also anything {@link Serializable}
	 * that identifies type some external resource not maintained by Evita.
	 */
	@Nonnull
	private final Serializable type;
	/**
	 * Reference to {@link Entity#getPrimaryKey()} of the referenced entity. Might be also any integer
	 * that uniquely identifies some external resource of type {@link #getType()} not maintained by Evita.
	 */
	private final int primaryKey;

	/**
	 * Constructor.
	 */
	public EntityReference(@Nonnull Serializable type, int primaryKey) {
		this.type = type;
		this.primaryKey = primaryKey;
	}

	@Override
	public int compareTo(@Nonnull EntityReference o) {
		return compareReferenceContract(o);
	}

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	public int estimateSize() {
		return MemoryMeasuringConstants.OBJECT_HEADER_SIZE +
			// type
			EvitaDataTypes.estimateSize(type) +
			// primary key
			MemoryMeasuringConstants.INT_SIZE;
	}

	@Override
	public String toString() {
		return type + ": " + primaryKey;
	}
}
