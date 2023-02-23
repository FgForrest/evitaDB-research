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

package io.evitadb.api.data.structure;

import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link ExistingReferenceBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ExistingReferenceBuilderTest {
	private final EntitySchema schema = new EntitySchema("product");
	private ReferenceContract initialReference;

	@BeforeEach
	void setUp() {
		initialReference = new InitialReferenceBuilder(
				schema, new EntityReference("brand", 5), (entityType, attributeName) -> false
		)
				.setAttribute("brandPriority", 154L)
				.setAttribute("country", Locale.ENGLISH, "Great Britain")
				.setAttribute("country", Locale.CANADA, "Canada")
				.setGroup("group", 78)
				.build();
	}

	@Test
	void shouldModifyAttributes() {
		final ReferenceBuilder builder = new ExistingReferenceBuilder(initialReference, schema, (entityType, attributeName) -> false)
				.setAttribute("brandPriority", 155L)
				.removeAttribute("country", Locale.ENGLISH)
				.setAttribute("newAttribute", "Hi");

		assertEquals(155L, (Long)builder.getAttribute("brandPriority"));
		assertEquals("Canada", builder.getAttribute("country", Locale.CANADA));
		assertNull(builder.getAttribute("country", Locale.ENGLISH));
		assertEquals("Hi", builder.getAttribute("newAttribute"));

		final ReferenceContract reference = builder.build();

		assertEquals(155L, (Long) reference.getAttribute("brandPriority"));
		assertEquals("Canada", reference.getAttribute("country", Locale.CANADA));
		assertEquals("Great Britain", reference.getAttribute("country", Locale.ENGLISH));
		assertEquals("Hi", reference.getAttribute("newAttribute"));

		final AttributeValue gbCountry = reference.getAttributeValue("country", Locale.ENGLISH);
		assertNotNull(gbCountry);
		assertTrue(gbCountry.isDropped());
	}

	@Test
	void shouldModifyReferenceGroup() {
		final ReferenceBuilder builder = new ExistingReferenceBuilder(initialReference, schema, (entityType, attributeName) -> false)
				.setGroup("newGroup", 77);

		assertEquals(
				new GroupEntityReference(2, "newGroup", 77, false),
				builder.getGroup()
		);

		final ReferenceContract reference = builder.build();

		assertEquals(
				new GroupEntityReference(2, "newGroup", 77, false),
				reference.getGroup()
		);
	}

	@Test
	void shouldRemoveReferenceGroup() {
		final ReferenceBuilder builder = new ExistingReferenceBuilder(initialReference, schema, (entityType, attributeName) -> false)
				.removeGroup();

		assertNull(builder.getGroup());

		final ReferenceContract reference = builder.build();

		assertNull(reference.getGroup());
	}

	@Test
	void shouldReturnOriginalReferenceInstanceWhenNothingHasChanged() {
		final ReferenceContract reference = new ExistingReferenceBuilder(initialReference, schema, (entityType, attributeName) -> false)
				.setAttribute("brandPriority", 154L)
				.setAttribute("country", Locale.ENGLISH, "Great Britain")
				.setAttribute("country", Locale.CANADA, "Canada")
				.setGroup("group", 78)
				.build();

		assertSame(initialReference, reference);
	}
}
