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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link SetHierarchicalPlacementMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class SetHierarchicalPlacementMutationTest {

	@Test
	void shouldCreateNewHierarchicalPlacement() {
		final SetHierarchicalPlacementMutation mutation = new SetHierarchicalPlacementMutation(2, 3);
		final HierarchicalPlacementContract hierarchyPlacement = mutation.mutateLocal(null);

		assertNotNull(hierarchyPlacement);
		assertEquals(1, hierarchyPlacement.getVersion());
		assertEquals(2, hierarchyPlacement.getParentPrimaryKey());
		assertEquals(3, hierarchyPlacement.getOrderAmongSiblings());
		assertFalse(hierarchyPlacement.isDropped());
	}

	@Test
	void shouldUpdateExistingHierarchicalPlacement() {
		final SetHierarchicalPlacementMutation mutation = new SetHierarchicalPlacementMutation(6, 7);
		final HierarchicalPlacementContract hierarchyPlacement = mutation.mutateLocal(
				new HierarchicalPlacement(2, 3)
		);

		assertNotNull(hierarchyPlacement);
		assertEquals(2, hierarchyPlacement.getVersion());
		assertEquals(6, hierarchyPlacement.getParentPrimaryKey());
		assertEquals(7, hierarchyPlacement.getOrderAmongSiblings());
		assertFalse(hierarchyPlacement.isDropped());
	}

	@Test
	void shouldReturnSameSkipToken() {
		assertEquals(
				new SetHierarchicalPlacementMutation(7, 9).getSkipToken(),
				new SetHierarchicalPlacementMutation(4, 9).getSkipToken()
		);
	}

}