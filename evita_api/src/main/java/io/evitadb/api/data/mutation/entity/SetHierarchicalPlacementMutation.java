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

package io.evitadb.api.data.mutation.entity;

import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.HierarchicalPlacement;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.function.UnaryOperator;

import static java.util.Optional.ofNullable;

/**
 * This mutation allows to set {@link HierarchicalPlacement} in the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class SetHierarchicalPlacementMutation implements SchemaEvolvingLocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract> {
	private static final long serialVersionUID = 8277337397634643354L;
	@Delegate(types = HierarchicalPlacementContract.class) private final HierarchicalPlacement hierarchicalPlacement;

	public SetHierarchicalPlacementMutation(int orderAmongSiblings) {
		this.hierarchicalPlacement = new HierarchicalPlacement(orderAmongSiblings);
	}

	public SetHierarchicalPlacementMutation(int parentPrimaryKey, int orderAmongSiblings) {
		this.hierarchicalPlacement = new HierarchicalPlacement(parentPrimaryKey, orderAmongSiblings);
	}

	@Override
	public @Nonnull
	Class<HierarchicalPlacementContract> affects() {
		return HierarchicalPlacementContract.class;
	}

	@Nonnull
	@Override
	public HierarchicalPlacementContract mutateLocal(@Nullable HierarchicalPlacementContract value) {
		if (value == null) {
			return ofNullable(hierarchicalPlacement.getParentPrimaryKey())
				.map(it -> new HierarchicalPlacement(it, hierarchicalPlacement.getOrderAmongSiblings()))
				.orElseGet(() -> new HierarchicalPlacement(hierarchicalPlacement.getOrderAmongSiblings()));
		} else {
			return new HierarchicalPlacement(
				value.getVersion() + 1,
				hierarchicalPlacement.getParentPrimaryKey(),
				hierarchicalPlacement.getOrderAmongSiblings()
			);
		}
	}

	@Override
	public long getPriority() {
		return PRIORITY_UPSERT;
	}

	@Override
	public HierarchicalPlacementContract getComparableKey() {
		return hierarchicalPlacement;
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return HierarchicalPlacement.class;
	}

	@Override
	public @Nonnull
	EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		if (!schema.isWithHierarchy()) {
			if (schema.allows(EvolutionMode.ADDING_HIERARCHY)) {
				return schema.open(schemaUpdater).withHierarchy().applyChanges();
			} else {
				throw new InvalidMutationException("Entity type " + schema.getName() + " doesn't allow hierarchy placement!");
			}
		} else {
			return schema;
		}
	}
}
