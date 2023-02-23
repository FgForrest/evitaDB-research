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

import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.mutation.LocalMutation;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * Sealed entity is read only form of entity that contains seal-breaking actions such as opening its contents to write
 * actions using {@link EntityBuilder} or accepting mutations that create {@link EntityMutation} objects. All seal
 * breaking actions don't modify SealedEntity contents and only create new objects upon it. This keeps this class
 * immutable and thread safe.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@ThreadSafe
@Immutable
public interface SealedEntity extends EntityContract {

	/**
	 * Opens entity for update - returns {@link EntityBuilder} that allows modification of the entity internals and
	 * fabricates new immutable copy of the entity with altered data. Returned EntityBuilder is NOT THREAD SAFE.
	 *
	 * EntityBuilder doesn't alter contents of SealedEntity but allows to create new version based on the version that
	 * is represented by this sealed entity.
	 */
	@Nonnull
	EntityBuilder open();

	/**
	 * Creates entity mutation that aggregates passed local mutations. Applying this mutation upon actual entity contents
	 * can create new version of the entity. Mutations can be passed to Evita DB to execute the changes.
	 *
	 * EntityMutation doesn't alter contents of SealedEntity and is immutable and thread safe by itself.
	 */
	@Nonnull
	EntityMutation withMutations(LocalMutation<?, ?>... localMutations);

	/**
	 * Creates entity mutation that aggregates passed local mutations. Applying this mutation upon actual entity contents
	 * can create new version of the entity. Mutations can be passed to Evita DB to execute the changes.
	 *
	 * EntityMutation doesn't alter contents of SealedEntity and is immutable and thread safe by itself.
	 */
	@Nonnull
	EntityMutation withMutations(Collection<LocalMutation<?, ?>> localMutations);

}
