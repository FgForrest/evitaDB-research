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
import io.evitadb.api.data.structure.HierarchicalPlacement;
import io.evitadb.api.exception.InvalidMutationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link RemoveHierarchicalPlacementMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class RemoveHierarchicalPlacementMutationTest {

	@Test
	void shouldRemoveExistingHierarchicalPlacement() {
		final RemoveHierarchicalPlacementMutation mutation = new RemoveHierarchicalPlacementMutation();
		final HierarchicalPlacementContract hierarchyPlacement = mutation.mutateLocal(
				new HierarchicalPlacement(2, 3)
		);
		assertNotNull(hierarchyPlacement);
		assertEquals(2, hierarchyPlacement.getVersion());
		assertEquals(2, hierarchyPlacement.getParentPrimaryKey());
		assertEquals(3, hierarchyPlacement.getOrderAmongSiblings());
		assertTrue(hierarchyPlacement.isDropped());
	}

	@Test
	void shouldFailToRemoveNonexistingHierarchicalPlacement() {
		final RemoveHierarchicalPlacementMutation mutation = new RemoveHierarchicalPlacementMutation();
		assertThrows(InvalidMutationException.class, () -> mutation.mutateLocal(null));
	}

	@Test
	void shouldFailToRemoveNonexistingPriceWhenAcceptingDroppedObject() {
		final RemoveHierarchicalPlacementMutation mutation = new RemoveHierarchicalPlacementMutation();
		assertThrows(
				InvalidMutationException.class,
				() -> mutation.mutateLocal(
						new HierarchicalPlacement(2, 2, 3, true)
				)
		);
	}
}