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

import io.evitadb.api.data.mutation.reference.ReferenceMutation;
import io.evitadb.api.data.structure.Reference;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Contract for classes that allow creating / updating or removing information in {@link Reference} instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface ReferenceEditor<W extends ReferenceEditor<W>> extends ReferenceContract, AttributesEditor<W> {

	/**
	 * Sets group to the reference. Group is composed of entity type and primary key of the referenced group entity.
	 * Group may or may not be Evita entity.
	 */
	@Nonnull
	W setGroup(@Nonnull Serializable referencedEntity, int primaryKey);

	/**
	 * Removes existing reference group.
	 */
	@Nonnull
	W removeGroup();

	/**
	 * Interface that simply combines writer and builder contracts together.
	 */
	interface ReferenceBuilder extends ReferenceEditor<ReferenceBuilder>, Builder<ReferenceContract> {

		@Nonnull
		Stream<? extends ReferenceMutation<?>> buildChangeSet();

	}

}
