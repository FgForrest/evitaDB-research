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

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.schema.AttributeSchemaBuilder;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;
import java.util.Locale;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link ExistingEntityBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ExistingEntityBuilderTest {
	public static final Currency CZK = Currency.getInstance("CZK");
	public static final Currency EUR = Currency.getInstance("EUR");
	private static final String SORTABLE_ATTRIBUTE = "toSort";
	private static final String BRAND_TYPE = "BRAND";
	private Entity initialEntity;
	private ExistingEntityBuilder builder;

	public static void assertPrice(Entity updatedInstance, int priceId, Serializable priceList, Currency currency, BigDecimal priceWithoutVat, BigDecimal vat, BigDecimal priceWithVat, boolean indexed) {
		final PriceContract price = updatedInstance.getPrice(priceId, priceList, currency);
		assertEquals(priceWithoutVat, price.getPriceWithoutVat());
		assertEquals(vat, price.getVat());
		assertEquals(priceWithVat, price.getPriceWithVat());
		assertEquals(indexed, price.isSellable());
	}

	@BeforeEach
	void setUp() {
		initialEntity = new InitialEntityBuilder("product")
			.setHierarchicalPlacement(5, 1)
			.setPriceInnerRecordHandling(PriceInnerRecordHandling.FIRST_OCCURRENCE)
			.setPrice(1, "basic", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
			.setPrice(2, "reference", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
			.setPrice(3, "basic", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
			.setPrice(4, "reference", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
			.setAttribute("string", "string")
			.setAttribute("int", Locale.ENGLISH, 1)
			.setAttribute("bigDecimal", Locale.ENGLISH, BigDecimal.ONE)
			.setAssociatedData("string", "string")
			.setAssociatedData("int", Locale.ENGLISH, 1)
			.setAssociatedData("bigDecimal", Locale.ENGLISH, BigDecimal.ONE)
			.toInstance();
		this.builder = new ExistingEntityBuilder(initialEntity);
	}

	@Test
	void shouldRemoveHierarchicalPlacement() {
		builder.removeHierarchicalPlacement();
		assertNull(builder.getHierarchicalPlacement());

		final Entity updatedEntity = builder.toMutation().mutate(initialEntity);
		assertNotNull(updatedEntity.getHierarchicalPlacement());
		assertFalse(updatedEntity.getHierarchicalPlacement().exists());
	}

	@Test
	void shouldDefineFacetGroup() {
		builder.setReference(BRAND_TYPE, 1, whichIs -> whichIs.setGroup("Whatever", 8));

		final EntityMutation entityMutation = builder.toMutation();
		final Collection<? extends LocalMutation<?, ?>> localMutations = entityMutation.getLocalMutations();
		assertEquals(1, localMutations.size());

		final Entity updatedEntity = entityMutation.mutate(initialEntity);
		final ReferenceContract reference = updatedEntity.getReference(BRAND_TYPE, 1);
		assertNotNull(reference);
		assertEquals(new GroupEntityReference("Whatever", 8), reference.getGroup());
	}

	@Test
	void shouldRemovePriceInnerRecordHandling() {
		builder.removePriceInnerRecordHandling();
		assertEquals(PriceInnerRecordHandling.NONE, builder.getPriceInnerRecordHandling());

		final Entity updatedEntity = builder.toMutation().mutate(initialEntity);
		assertEquals(PriceInnerRecordHandling.NONE, updatedEntity.getPriceInnerRecordHandling());
	}

	@Test
	void shouldOverwriteHierarchicalPlacement() {
		builder.setHierarchicalPlacement(78, 5);
		assertEquals(new HierarchicalPlacement(2, 78, 5), builder.getHierarchicalPlacement());

		final Entity updatedEntity = builder.toMutation().mutate(initialEntity);
		assertEquals(new HierarchicalPlacement(2, 78, 5), updatedEntity.getHierarchicalPlacement());
	}

	@Test
	void shouldOverwritePrices() {
		final Entity updatedInstance = builder
			.setPrice(1, "basic", CZK, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true)
			.removePrice(2, "reference", CZK)
			.setPrice(5, "basic", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
			.toInstance();

		assertEquals(5, updatedInstance.getPrices().size());
		assertTrue(updatedInstance.getPrice(2, "reference", CZK).isDropped());
		assertPrice(updatedInstance, 1, "basic", CZK, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true);
		assertPrice(updatedInstance, 5, "basic", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true);
	}

	@Test
	void shouldFailToAddArrayAsSortableAttribute() {
		final EntitySchema schema = new EntitySchemaBuilder(
			new EntitySchema("product"),
			UnaryOperator.identity()
		)
			.withAttribute(SORTABLE_ATTRIBUTE, String.class, AttributeSchemaBuilder::sortable)
			.applyChanges();

		final ExistingEntityBuilder existingEntityBuilder = new ExistingEntityBuilder(new InitialEntityBuilder(schema).toInstance());
		assertThrows(IllegalArgumentException.class, () -> existingEntityBuilder.setAttribute(SORTABLE_ATTRIBUTE, new String[]{"abc", "def"}));
	}

	@Test
	void shouldFailToAddMultipleSortableAttributesToSingleReferencedType() {
		final EntitySchema schema = new EntitySchemaBuilder(
			new EntitySchema("product"),
			UnaryOperator.identity()
		)
			.withReferenceTo(BRAND_TYPE, whichIs -> whichIs.withAttribute(SORTABLE_ATTRIBUTE, String.class, AttributeSchemaBuilder::sortable))
			.applyChanges();

		final ExistingEntityBuilder existingEntityBuilder = new ExistingEntityBuilder(new InitialEntityBuilder(schema).toInstance());
		existingEntityBuilder.setReference(BRAND_TYPE, 1, thatIs -> thatIs.setAttribute(SORTABLE_ATTRIBUTE, "abc"));
		assertThrows(IllegalArgumentException.class, () -> existingEntityBuilder.setReference(BRAND_TYPE, 3, thatIs -> thatIs.setAttribute(SORTABLE_ATTRIBUTE, "def")));
	}

	@Test
	void shouldReturnOriginalEntityInstanceWhenNothingHasChanged() {
		final Entity newEntity = new ExistingEntityBuilder(initialEntity)
			.setHierarchicalPlacement(5, 1)
			.setPriceInnerRecordHandling(PriceInnerRecordHandling.FIRST_OCCURRENCE)
			.setPrice(1, "basic", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
			.setPrice(2, "reference", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
			.setPrice(3, "basic", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
			.setPrice(4, "reference", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
			.setAttribute("string", "string")
			.setAttribute("int", Locale.ENGLISH, 1)
			.setAttribute("bigDecimal", Locale.ENGLISH, BigDecimal.ONE)
			.setAssociatedData("string", "string")
			.setAssociatedData("int", Locale.ENGLISH, 1)
			.setAssociatedData("bigDecimal", Locale.ENGLISH, BigDecimal.ONE)
			.toInstance();

		assertSame(initialEntity, newEntity);
	}

}