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

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link InitialReferenceBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InitialReferenceBuilderTest {

	@Test
	void shouldCreateReference() {
		final EntitySchema schema = new EntitySchema("product");
		final ReferenceBuilder builder = new InitialReferenceBuilder(
				schema, new EntityReference("brand", 5), (entityType, attributeName) -> false
		)
				.setAttribute("brandPriority", 154L)
				.setAttribute("country", Locale.ENGLISH, "Great Britain")
				.setAttribute("country", Locale.CANADA, "Canada")
				.setGroup("group", 78);

		assertEquals(
				new EntityReference("brand", 5),
				builder.getReferencedEntity()
		);
		assertEquals(
				new GroupEntityReference(1, "group", 78, false),
				builder.getGroup()
		);
		assertEquals(154L, (Long)builder.getAttribute("brandPriority"));
		assertEquals("Great Britain", builder.getAttribute("country", Locale.ENGLISH));
		assertEquals("Canada", builder.getAttribute("country", Locale.CANADA));

		final ReferenceContract reference = builder.build();

		assertEquals(
				new EntityReference("brand", 5),
				reference.getReferencedEntity()
		);
		assertEquals(
				new GroupEntityReference(1, "group", 78, false),
				reference.getGroup()
		);
		assertEquals(154L, (Long) reference.getAttribute("brandPriority"));
		assertEquals("Great Britain", reference.getAttribute("country", Locale.ENGLISH));
		assertEquals("Canada", reference.getAttribute("country", Locale.CANADA));

		final AtomicReference<EntitySchema> schemaRef = new AtomicReference<>(schema);
		builder.buildChangeSet()
			.filter(it -> it instanceof SchemaEvolvingLocalMutation)
			.forEach(it -> ((SchemaEvolvingLocalMutation)it).verifyOrEvolveSchema(
				schemaRef.get(),
				schema1 -> {
					schemaRef.set(schema1); return schema1;
				})
			);

		final EntitySchema updatedSchema = schemaRef.get();
		final Map<String, AttributeSchema> brandRefAttributes = updatedSchema.getReference("brand").getAttributes();
		assertFalse(brandRefAttributes.isEmpty());
		final AttributeSchema brandPriority = brandRefAttributes.get("brandPriority");
		assertNotNull(brandPriority);
		assertEquals(Long.class, brandPriority.getType());
		assertFalse(brandPriority.isLocalized());

		final AttributeSchema brandCountry = brandRefAttributes.get("country");
		assertNotNull(brandCountry);
		assertEquals(String.class, brandCountry.getType());
		assertTrue(brandCountry.isLocalized());
	}

	@Test
	void shouldOverwriteReferenceData() {
		final EntitySchema schema = new EntitySchema("product");
		final ReferenceBuilder builder = new InitialReferenceBuilder(
				schema, new EntityReference("brand", 5), (entityType, attributeName) -> false
		)
				.setAttribute("brandPriority", 154L)
				.setAttribute("brandPriority", 155L)
				.setAttribute("country", Locale.ENGLISH, "Great Britain")
				.setAttribute("country", Locale.ENGLISH, "Great Britain #2")
				.setAttribute("country", Locale.CANADA, "Canada")
				.setAttribute("country", Locale.CANADA, "Canada #2")
				.setGroup("group", 78)
				.setGroup("group", 79);

		assertEquals(
				new EntityReference("brand", 5),
				builder.getReferencedEntity()
		);
		assertEquals(
				new GroupEntityReference(1, "group", 79, false),
				builder.getGroup()
		);
		assertEquals(155L, (Long)builder.getAttribute("brandPriority"));
		assertEquals("Great Britain #2", builder.getAttribute("country", Locale.ENGLISH));
		assertEquals("Canada #2", builder.getAttribute("country", Locale.CANADA));

		final ReferenceContract reference = builder.build();

		assertEquals(
				new EntityReference("brand", 5),
				reference.getReferencedEntity()
		);
		assertEquals(
				new GroupEntityReference(1, "group", 79, false),
				reference.getGroup()
		);
		assertEquals(155L, (Long) reference.getAttribute("brandPriority"));
		assertEquals("Great Britain #2", reference.getAttribute("country", Locale.ENGLISH));
		assertEquals("Canada #2", reference.getAttribute("country", Locale.CANADA));
	}

}
