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

import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.HierarchicalPlacement;
import io.evitadb.api.data.structure.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Contract for classes that allow creating / updating or removing information in {@link Entity} instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface EntityEditor<W extends EntityEditor<W>> extends EntityContract, AttributesEditor<W>, AssociatedDataEditor<W>, PricesEditor<W> {

	/**
	 * Sets hierarchy information of the entity. Hierarchy information allows to compose hierarchy tree composed of entities
	 * of the same type. Referenced entity is always entity of the same type. Referenced entity must be already present
	 * in the Evita DB and must also have hierarchy placement set. This method is used to set "root" hierarchical placement.
	 * That means, that the entity is one of the root entities without parent. To create children entities use overloaded
	 * method {@link #setHierarchicalPlacement(int, int)}
	 *
	 * @param orderAmongSiblings
	 * @return
	 */
	W setHierarchicalPlacement(int orderAmongSiblings);

	/**
	 * Sets hierarchy information of the entity. Hierarchy information allows to compose hierarchy tree composed of entities
	 * of the same type. Referenced entity is always entity of the same type. Referenced entity must be already present
	 * in the Evita DB and must also have hierarchy placement set.
	 *
	 * @param parentPrimaryKey
	 * @param orderAmongSiblings
	 * @return
	 */
	W setHierarchicalPlacement(int parentPrimaryKey, int orderAmongSiblings);

	/**
	 * Removes existing hierarchical placement of the entity. If there are other entities, that refer transitively via
	 * {@link HierarchicalPlacement#getParentPrimaryKey()} this entity their hierarchical placement will be removed
	 * as well.
	 *
	 * @return
	 */
	W removeHierarchicalPlacement();

	/**
	 * Method creates or updates reference of the entity. Reference represents reference to another Evita entity or may by
	 * also any external source identified by `referencedEntityType`.
	 *
	 * @param referencedEntityType
	 * @param referencedPrimaryKey
	 * @return
	 */
	W setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey);

	/**
	 * Method creates or updates reference of the entity. Reference represents reference to another Evita entity or may by
	 * also any external source identified by `referencedEntityType`. Third argument accepts consumer, that allows
	 * to set additional information on the reference such as its {@link Reference#getAttributes()} or grouping information.
	 *
	 * @param referencedEntityType
	 * @param referencedPrimaryKey
	 * @param whichIs
	 * @return
	 */
	W setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey, @Nullable Consumer<ReferenceBuilder> whichIs);

	/**
	 * Removes existing reference of specified type and primary key.
	 *
	 * @param referencedEntityType
	 * @param referencedPrimaryKey
	 * @return
	 */
	W removeReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey);

	/**
	 * Interface that simply combines writer and builder entity contracts together.
	 * Builder produces either {@link EntityMutation} that describes all changes to be made on {@link Entity} instance
	 * to get it to "up-to-date" state or can provide already built {@link Entity} that may not represent globally
	 * "up-to-date" state because it is based on the version of the entity known when builder was created. Evita
	 * interface accepts only first version of the object for updates: {@link io.evitadb.api.EvitaSessionBase#upsertEntity(EntityMutation)}
	 *
	 * Mutation allows Evita to perform surgical updates on the most latest version of the {@link Entity} object that
	 * is in the database at the time update request arrives.
	 */
	interface EntityBuilder extends EntityEditor<EntityBuilder> {

		/**
		 * Returns object that contains set of {@link io.evitadb.api.data.mutation.LocalMutation} instances describing
		 * what changes occurred in the builder and which should be applied on the existing {@link Entity} version.
		 * Each mutation increases {@link Versioned#getVersion()} of the modified object and allows to detect race conditions
		 * based on "optimistic locking" mechanism in very granular way.
		 *
		 * @return
		 */
		EntityMutation toMutation();

		/**
		 * Returns object that contains set of {@link io.evitadb.api.data.mutation.LocalMutation} instances describing
		 * what local mutations needs to be executed in so that entity gets fully wiped out.
		 *
		 * @return
		 */
		EntityMutation toRemovalMutation();

		/**
		 * Returns built "local up-to-date" {@link Entity} instance that may not represent globally "up-to-date" state
		 * because it is based on the version of the entity known when builder was created. Evita interface accepts
		 * only first version of the object for updates: {@link io.evitadb.api.EvitaSessionBase#upsertEntity(EntityMutation)}
		 *
		 * Mutation allows Evita to perform surgical updates on the most latest version of the {@link Entity} object that
		 * is in the database at the time update request arrives.
		 *
		 * This method is useful for tests.
		 *
		 * @return
		 */
		Entity toInstance();

	}

}
