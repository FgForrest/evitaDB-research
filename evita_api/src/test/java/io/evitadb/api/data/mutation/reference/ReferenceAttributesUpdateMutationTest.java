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

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link ReferenceAttributesUpdateMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ReferenceAttributesUpdateMutationTest {
	private final EntitySchema entitySchema = new EntitySchema("PRODUCT");

	@Test
	void shouldUpdateReferenceAttributes() {
		final ReferenceAttributesUpdateMutation mutation = new ReferenceAttributesUpdateMutation(
				new EntityReference("category", 5),
				entitySchema,
				new UpsertAttributeMutation(new AttributeKey("categoryPriority"), 145L)
		);

		final ReferenceContract reference = mutation.mutateLocal(
				new Reference(
						new EntitySchema("product"),
						new EntityReference("category", 5),
						null
				)
		);

		assertNotNull(reference);
		assertEquals(2, reference.getVersion());
		assertEquals("category", reference.getReferencedEntity().getType());
		assertEquals(5, reference.getReferencedEntity().getPrimaryKey());

		final Collection<AttributeValue> attributeValues = reference.getAttributeValues();
		assertEquals(1, attributeValues.size());

		final AttributeValue attributeValue = attributeValues.iterator().next();
		assertEquals(1, attributeValue.getVersion());
		assertEquals("categoryPriority", attributeValue.getKey().getAttributeName());
		assertEquals(145L, attributeValue.getValue());

		assertNull(reference.getGroup());
		assertFalse(reference.isDropped());
	}

	@Test
	void shouldReturnSameSkipToken() {
		assertEquals(
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 5),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc"), "B")
				).getSkipToken(),
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 10),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc"), "C")
				).getSkipToken()
		);
		assertEquals(
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 5),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc", Locale.ENGLISH), "B")
				).getSkipToken(),
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 10),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc", Locale.ENGLISH), "C")
				).getSkipToken()
		);
	}

	@Test
	void shouldReturnDifferentSkipToken() {
		assertNotEquals(
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 5),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc"), "B")
				).getSkipToken(),
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 10),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abe"), "C")
				).getSkipToken()
		);
		assertNotEquals(
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 5),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc"), "B")
				).getSkipToken(),
				new ReferenceAttributesUpdateMutation(
						new EntityReference("product", 5),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc"), "B")
				).getSkipToken()
		);
		assertNotEquals(
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 5),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc", Locale.ENGLISH), "B")
				).getSkipToken(),
				new ReferenceAttributesUpdateMutation(
						new EntityReference("category", 10),
						entitySchema,
						new UpsertAttributeMutation(new AttributeKey("abc", Locale.GERMAN), "C")
				).getSkipToken()
		);
	}

}