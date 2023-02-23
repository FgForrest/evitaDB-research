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

package io.evitadb.api.data.mutation.reference;

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link RemoveReferenceGroupMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class RemoveReferenceGroupMutationTest {
	private final EntitySchema entitySchema = new EntitySchema("PRODUCT");

	@Test
	void shouldRemoveExistingReferenceGroup() {
		final RemoveReferenceGroupMutation mutation = new RemoveReferenceGroupMutation(
			new EntityReference("brand", 5),
			entitySchema
		);
		final ReferenceContract reference = mutation.mutateLocal(
				new Reference(
						new EntitySchema("product"),
						new EntityReference("brand", 5),
						new GroupEntityReference("europe", 2)
				)
		);

		assertNotNull(reference);
		assertEquals(2, reference.getVersion());
		assertEquals("brand", reference.getReferencedEntity().getType());
		assertEquals(5, reference.getReferencedEntity().getPrimaryKey());
		assertNotNull(reference.getGroup());
		assertEquals("europe", reference.getGroup().getType());
		assertEquals(2, reference.getGroup().getPrimaryKey());
		assertEquals(2, reference.getGroup().getVersion());
		assertTrue(reference.getGroup().isDropped());
		assertFalse(reference.isDropped());
	}

	@Test
	void shouldFailToRemoveNonexistingReferenceGroup() {
		final RemoveReferenceGroupMutation mutation = new RemoveReferenceGroupMutation(
			new EntityReference("brand", 5),
			entitySchema
		);
		assertThrows(InvalidMutationException.class, () -> mutation.mutateLocal(null));
	}

	@Test
	void shouldFailToRemoveNonexistingReferenceGroupWhenAcceptingDroppedObject() {
		final RemoveReferenceGroupMutation mutation = new RemoveReferenceGroupMutation(
			new EntityReference("brand", 5),
			entitySchema
		);
		assertThrows(
				InvalidMutationException.class,
				() -> mutation.mutateLocal(
						new Reference(
								new EntitySchema("product"),
								2,
								new EntityReference("brand", 5),
								new GroupEntityReference(2, "europe", 2, true),
								false
						)
				)
		);
	}

}