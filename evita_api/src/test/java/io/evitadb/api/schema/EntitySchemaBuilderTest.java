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

package io.evitadb.api.schema;

import io.evitadb.api.dataType.ComplexDataObject;
import io.evitadb.api.dataType.DateTimeRange;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies the process of Evita DB schema update.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EntitySchemaBuilderTest {
	private final EntitySchema productSchema = new EntitySchema(Entities.PRODUCT);
	private final EntitySchema categorySchema = new EntitySchema(Entities.CATEGORY);

	@Test
	void shouldDefineProductSchema() {
		final EntitySchemaBuilder schemaBuilder = new EntitySchemaBuilder(productSchema, updatedSchema -> {
			assertSchemaContents(updatedSchema);
			return updatedSchema;
		});

		constructExampleSchema(schemaBuilder);
	}

	@Test
	void shouldUpdateBuildExactlySameProductSchema() {
		final AtomicReference<EntitySchema> updatedSchemaRef = new AtomicReference<>();

		final EntitySchemaBuilder schemaBuilder = new EntitySchemaBuilder(productSchema, updatedSchema -> {
			assertSchemaContents(updatedSchema);

			updatedSchemaRef.set(updatedSchema);
			return updatedSchema;
		});

		constructExampleSchema(schemaBuilder);

		new EntitySchemaBuilder(updatedSchemaRef.get(), schemaAgain -> {
			assertSchemaContents(schemaAgain);

			return schemaAgain;
		}).applyChanges();
	}

	@SuppressWarnings("Convert2MethodRef")
	@Test
	void shouldDefineCategorySchema() {
		final EntitySchemaBuilder schemaBuilder = new EntitySchemaBuilder(categorySchema, updatedSchema -> {
			assertTrue(updatedSchema.getEvolutionMode().isEmpty());
			assertTrue(updatedSchema.isWithHierarchy());

			assertFalse(updatedSchema.isWithPrice());

			assertTrue(updatedSchema.getLocales().contains(Locale.ENGLISH));
			assertTrue(updatedSchema.getLocales().contains(new Locale("cs", "CZ")));

			assertEquals(5, updatedSchema.getAttributes().size());
			assertAttribute(updatedSchema.getAttribute("code"), true, false, false, false, 0, String.class);
			assertAttribute(updatedSchema.getAttribute("oldEntityUrls"), false, true, false, true, 0, String[].class);
			assertAttribute(updatedSchema.getAttribute("priority"), false, false, true, false, 0, Long.class);

			assertEquals(1, updatedSchema.getAssociatedData().size());
			assertAssociatedData(updatedSchema.getAssociatedData("labels"), true, ComplexDataObject.class);

			assertTrue(updatedSchema.getReferences().isEmpty());
			return updatedSchema;
		});

		schemaBuilder
			/* all is strictly verified for categories */
			.verifySchemaStrictly()
			/* categories are organized in a tree manner */
			.withHierarchy()
			/* categories don't have prices, we can also omit this line */
			.withoutPrice()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, new Locale("cs", "CZ"))
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute("code", String.class, whichIs -> whichIs.unique())
			.withAttribute("url", String.class, whichIs -> whichIs.unique().localized())
			.withAttribute("oldEntityUrls", String[].class, whichIs -> whichIs.filterable().localized())
			.withAttribute("name", String.class, whichIs -> whichIs.filterable().sortable())
			.withAttribute("priority", Long.class, whichIs -> whichIs.sortable())
			/* here we define set of associated data, that can be stored along with entity */
			.withAssociatedData("labels", Labels.class, whichIs -> whichIs.localized())
			/* finally apply schema changes */
			.applyChanges();
	}

	private void assertSchemaContents(EntitySchema updatedSchema) {
		assertTrue(updatedSchema.allows(EvolutionMode.ADDING_ASSOCIATED_DATA));
		assertTrue(updatedSchema.allows(EvolutionMode.ADDING_REFERENCES));

		assertFalse(updatedSchema.isWithHierarchy());

		assertTrue(updatedSchema.isWithPrice());

		assertTrue(updatedSchema.getLocales().contains(Locale.ENGLISH));
		assertTrue(updatedSchema.getLocales().contains(new Locale("cs", "CZ")));

		assertEquals(9, updatedSchema.getAttributes().size());
		assertAttribute(updatedSchema.getAttribute("code"), true, false, false, false, 0, String.class);
		assertAttribute(updatedSchema.getAttribute("oldEntityUrls"), false, true, false, true, 0, String[].class);
		assertAttribute(updatedSchema.getAttribute("quantity"), false, true, false, false, 2, BigDecimal.class);
		assertAttribute(updatedSchema.getAttribute("priority"), false, false, true, false, 0, Long.class);

		assertEquals(2, updatedSchema.getAssociatedData().size());
		assertAssociatedData(updatedSchema.getAssociatedData("referencedFiles"), false, ComplexDataObject.class);
		assertAssociatedData(updatedSchema.getAssociatedData("labels"), true, ComplexDataObject.class);

		assertEquals(3, updatedSchema.getReferences().size());

		final ReferenceSchema categoryReference = updatedSchema.getReference(Entities.CATEGORY);
		assertReference(categoryReference, false);
		assertEquals(1, categoryReference.getAttributes().size());
		assertAttribute(categoryReference.getAttribute("categoryPriority"), false, false, true, false, 0, Long.class);

		assertReference(updatedSchema.getReference(Entities.BRAND), true);
		assertReference(updatedSchema.getReference("stock"), true);
	}

	private void assertReference(ReferenceSchema reference, boolean indexed) {
		assertNotNull(reference);
		assertEquals(indexed, reference.isFaceted());
	}

	private void assertAttribute(AttributeSchema attributeSchema, boolean unique, boolean filterable, boolean sortable, boolean localized, int indexedDecimalPlaces, Class<? extends Serializable> ofType) {
		assertNotNull(attributeSchema);
		assertEquals(unique, attributeSchema.isUnique());
		assertEquals(filterable, attributeSchema.isFilterable());
		assertEquals(sortable, attributeSchema.isSortable());
		assertEquals(localized, attributeSchema.isLocalized());
		assertEquals(ofType, attributeSchema.getType());
		assertEquals(indexedDecimalPlaces, attributeSchema.getIndexedDecimalPlaces());
	}

	private void assertAssociatedData(AssociatedDataSchema associatedDataSchema, boolean localized, Class<? extends Serializable> ofType) {
		assertNotNull(associatedDataSchema);
		assertEquals(localized, associatedDataSchema.isLocalized());
		assertEquals(ofType, associatedDataSchema.getType());
	}

	@SuppressWarnings("Convert2MethodRef")
	private static void constructExampleSchema(EntitySchemaBuilder schemaBuilder) {
		schemaBuilder
			/* all is strictly verified but associated data and references can be added on the fly */
			.verifySchemaButAllow(EvolutionMode.ADDING_ASSOCIATED_DATA, EvolutionMode.ADDING_REFERENCES)
			/* product are not organized in the tree */
			.withoutHierarchy()
			/* prices are referencing another entity stored in Evita */
			.withPrice()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, new Locale("cs", "CZ"))
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute("code", String.class, whichIs -> whichIs.unique())
			.withAttribute("url", String.class, whichIs -> whichIs.unique().localized())
			.withAttribute("oldEntityUrls", String[].class, whichIs -> whichIs.filterable().localized())
			.withAttribute("name", String.class, whichIs -> whichIs.filterable().sortable())
			.withAttribute("ean", String.class, whichIs -> whichIs.filterable())
			.withAttribute("priority", Long.class, whichIs -> whichIs.sortable())
			.withAttribute("validity", DateTimeRange.class, whichIs -> whichIs.filterable())
			.withAttribute("quantity", BigDecimal.class, whichIs -> whichIs.filterable().indexDecimalPlaces(2))
			.withAttribute("alias", Boolean.class, whichIs -> whichIs.filterable())
			/* here we define set of associated data, that can be stored along with entity */
			.withAssociatedData("referencedFiles", ReferencedFileSet.class)
			.withAssociatedData("labels", Labels.class, whichIs -> whichIs.localized())
			/* here we define references that relate to another entities stored in Evita */
			.withReferenceToEntity(
				Entities.CATEGORY,
				whichIs ->
					/* we can specify special attributes on relation */
					whichIs.withAttribute("categoryPriority", Long.class, thatIs -> thatIs.sortable())
			)
			/* for faceted references we can compute "counts" */
			.withReferenceToEntity(Entities.BRAND, whichIs -> whichIs.faceted())
			/* references may be also represented be entities unknown to Evita */
			.withReferenceTo("stock", whichIs -> whichIs.faceted())
			/* finally apply schema changes */
			.applyChanges();
	}

	public enum Entities {
		PRODUCT, CATEGORY, BRAND, PRICE_LIST
	}

	public static class ReferencedFileSet implements Serializable {
		private static final long serialVersionUID = -1355676966187183143L;
	}

	public static class Labels implements Serializable {
		private static final long serialVersionUID = 1121150156843379388L;
	}

}