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

import io.evitadb.api.schema.AttributeSchemaBuilder;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link InitialEntityBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InitialEntityBuilderTest {
	private static final String SORTABLE_ATTRIBUTE = "toSort";
	private static final String BRAND_TYPE = "BRAND";

	@Test
	void shouldCreateNewEntity() {
		final InitialEntityBuilder builder = new InitialEntityBuilder("product");
		final Entity product = builder.toMutation().mutate(null);
		assertNotNull(product);
		assertEquals("product", product.getType());
		// no one has an opportunity to set the primary key (yet)
		assertNull(product.getPrimaryKey());
	}

	@Test
	void shouldFailToAddArrayAsSortableAttribute() {
		final EntitySchema schema = new EntitySchemaBuilder(
				new EntitySchema("product"),
				UnaryOperator.identity()
			)
			.withAttribute(SORTABLE_ATTRIBUTE, String.class, AttributeSchemaBuilder::sortable)
			.applyChanges();

		final InitialEntityBuilder builder = new InitialEntityBuilder(schema);
		assertThrows(IllegalArgumentException.class, () -> builder.setAttribute(SORTABLE_ATTRIBUTE, new String[] {"abc", "def"}));
	}

	@Test
	void shouldFailToAddMultipleSortableAttributesToSingleReferencedType() {
		final EntitySchema schema = new EntitySchemaBuilder(
			new EntitySchema("product"),
			UnaryOperator.identity()
		)
			.withReferenceTo(BRAND_TYPE, whichIs -> whichIs.withAttribute(SORTABLE_ATTRIBUTE, String.class, AttributeSchemaBuilder::sortable))
			.applyChanges();

		final InitialEntityBuilder builder = new InitialEntityBuilder(schema);
		builder.setReference(BRAND_TYPE, 1, thatIs -> thatIs.setAttribute(SORTABLE_ATTRIBUTE, "abc"));
		assertThrows(IllegalArgumentException.class, () -> builder.setReference(BRAND_TYPE, 3, thatIs -> thatIs.setAttribute(SORTABLE_ATTRIBUTE, "def")));
	}

}