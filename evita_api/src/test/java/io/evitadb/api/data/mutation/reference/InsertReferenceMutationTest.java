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
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link InsertReferenceMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InsertReferenceMutationTest {
	private final EntitySchema schema = new EntitySchema("product");

	@Test
	void shouldInsertNewReference() {
		final InsertReferenceMutation mutation = new InsertReferenceMutation(
			new Reference(
					schema,
					new EntityReference("brand", 5),
					new GroupEntityReference("europe", 2)
			),
			schema
		);

		final ReferenceContract reference = mutation.mutateLocal(null);
		assertNotNull(reference);
		assertEquals(1, reference.getVersion());
		assertEquals("brand", reference.getReferencedEntity().getType());
		assertEquals(5, reference.getReferencedEntity().getPrimaryKey());
		assertNotNull(reference.getGroup());
		assertEquals("europe", reference.getGroup().getType());
		assertEquals(2, reference.getGroup().getPrimaryKey());
		assertFalse(reference.isDropped());
	}

	@Test
	void shouldReturnSameSkipToken() {
		assertEquals(
				new InsertReferenceMutation(
					new Reference(
							schema,
							new EntityReference("brand", 5),
							new GroupEntityReference("europe", 2)
					),
					schema
				).getSkipToken(),
				new InsertReferenceMutation(
					new Reference(
							schema,
							new EntityReference("brand", 10),
							new GroupEntityReference("asia", 8)
					),
					schema
				).getSkipToken()
		);
	}

	@Test
	void shouldReturnDifferentSkipToken() {
		assertNotEquals(
				new InsertReferenceMutation(
					new Reference(
							schema,
							new EntityReference("brand", 5),
							new GroupEntityReference("europe", 2)
					),
					schema
				).getSkipToken(),
				new InsertReferenceMutation(
					new Reference(
							schema,
							new EntityReference("category", 10),
							new GroupEntityReference("europe", 2)
					),
					schema
				).getSkipToken()
		);
	}

}