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

package io.evitadb.storage.model.storageParts.index;

import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.index.hierarchy.HierarchyNode;
import io.evitadb.storage.model.storageParts.StoragePart;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Map;

/**
 * Hierarchy index collocates information about hierarchical tree structure of the entities. This container object
 * serves only as a storage carrier for {@link io.evitadb.index.hierarchy.HierarchyIndex} which is a live memory
 * representation of the data stored in this container.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@ToString(of = "entityIndexPrimaryKey")
public class HierarchyIndexStoragePart implements StoragePart {
	private static final long serialVersionUID = -3223754922135567923L;

	/**
	 * Unique id that identifies {@link io.evitadb.index.EntityIndex}.
	 */
	@Getter private final int entityIndexPrimaryKey;
	/**
	 * Index contains information about every entity that has {@link HierarchicalPlacementContract} specified no matter
	 * whether it's part of the tree reachable from the {@link #roots} or {@link #orphans}. Key of the index is
	 * {@link HierarchyNode#getEntityPrimaryKey()}.
	 */
	@Getter private final Map<Integer, HierarchyNode> itemIndex;
	/**
	 * List contains entity primary keys of all entities that have hierarchy placement set to root level (i.e. without
	 * any parent). List contains ids sorted by {@link HierarchicalPlacementContract#getOrderAmongSiblings()}.
	 */
	@Getter private final List<Integer> roots;
	/**
	 * Index contains information about children of all entities having {@link HierarchicalPlacementContract} specified.
	 * Every entity in {@link #itemIndex} has also record in this entity but only in case they are reachable from
	 * {@link #roots} - either with empty array or array of its children sorted by their
	 * {@link HierarchicalPlacementContract#getOrderAmongSiblings()}. If the entity is not reachable from any root
	 * entity it's places into {@link #orphans} and is not present in this index.
	 */
	@Getter private final Map<Integer, int[]> levelIndex;
	/**
	 * Array contains entity primary keys of all entities that are not reachable from {@link #roots}. This simple list
	 * contains also children of orphan parents - i.e. primary keys of all unreachable entities that have
	 * {@link HierarchicalPlacementContract} specified.
	 */
	@Getter private final int[] orphans;

	public HierarchyIndexStoragePart(int entityIndexPrimaryKey, Map<Integer, HierarchyNode> itemIndex, List<Integer> roots, Map<Integer, int[]> levelIndex, int[] orphans) {
		this.entityIndexPrimaryKey = entityIndexPrimaryKey;
		this.itemIndex = itemIndex;
		this.roots = roots;
		this.levelIndex = levelIndex;
		this.orphans = orphans;
	}

	@Nullable
	@Override
	public Long getUniquePartId() {
		return (long) entityIndexPrimaryKey;
	}

	@Override
	public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
		return entityIndexPrimaryKey;
	}

}
