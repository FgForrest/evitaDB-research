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

package io.evitadb.storage.model.storageParts;

import io.evitadb.api.schema.EntitySchema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * This class envelopes unknown Serializable and makes it comparable.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ComparableReferencedType implements Comparable<ComparableReferencedType>, Serializable {
	private static final long serialVersionUID = -3099841454995457261L;
	/**
	 * Refers to {@link EntitySchema#getName()}
	 */
	@Getter private final Serializable entityType;

	@Override
	public int compareTo(ComparableReferencedType o) {
		final Serializable thisReferencedEntity = getEntityType();
		final Serializable thatReferencedEntity = o.getEntityType();
		if (thisReferencedEntity.getClass().equals(o.getEntityType().getClass()) && this.getEntityType() instanceof Comparable) {
			//noinspection unchecked, rawtypes
			return ((Comparable) thisReferencedEntity).compareTo(thatReferencedEntity);
		} else {
			return thisReferencedEntity.toString().compareTo(thatReferencedEntity.toString());
		}
	}
}
