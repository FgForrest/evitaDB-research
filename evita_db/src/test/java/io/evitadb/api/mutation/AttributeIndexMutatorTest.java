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

package io.evitadb.api.mutation;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.mutation.AttributeIndexMutator.*;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.index.attribute.UniqueIndex;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStoragePart;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStoragePart.AttributeIndexType;
import io.evitadb.storage.model.storageParts.index.FilterIndexStoragePart;
import io.evitadb.storage.model.storageParts.index.SortIndexStoragePart;
import io.evitadb.storage.model.storageParts.index.UniqueIndexStoragePart;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;

import static io.evitadb.api.mutation.AttributeIndexMutator.*;
import static io.evitadb.test.generator.DataGenerator.ATTRIBUTE_CODE;
import static io.evitadb.test.generator.DataGenerator.ATTRIBUTE_EAN;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies {@link AttributeIndexMutator} contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class AttributeIndexMutatorTest extends AbstractMutatorTestBase {
	private static final String ATTRIBUTE_VARIANT_COUNT = "variantCount";
	private static final String ATTRIBUTE_CHAR_ARRAY = "charArray";

	@Override
	protected void alterProductSchema(EntitySchemaBuilder schema) {
		schema.withAttribute(ATTRIBUTE_VARIANT_COUNT, Integer.class, whichIs -> whichIs.sortable().filterable());
		schema.withAttribute(ATTRIBUTE_CHAR_ARRAY, Character[].class, whichIs -> whichIs.filterable());
	}

	@Test
	void shouldInsertNewAttribute() {
		final AttributeKey codeAttr = new AttributeKey(ATTRIBUTE_CODE);
		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, codeAttr),
			index, codeAttr, "A",
			false
		);
		final AttributeKey eanAttr = new AttributeKey(ATTRIBUTE_EAN);
		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, eanAttr),
			index, eanAttr, "EAN-001",
			false
		);
		assertEquals(1, index.getUniqueIndex(ATTRIBUTE_CODE, null).getRecordIdByUniqueValue("A"));
		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_EAN, null).getRecordsEqualTo("EAN-001").getArray());

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(3, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.UNIQUE, ATTRIBUTE_CODE);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_CODE);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_EAN);
	}

	@Test
	void shouldInsertNewAttributeWithAutomaticConversion() {
		final AttributeKey variantCountAttr = new AttributeKey(ATTRIBUTE_VARIANT_COUNT);
		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, variantCountAttr),
			index, variantCountAttr, "115",
			false
		);

		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_VARIANT_COUNT, null).getRecordsEqualTo(115).getArray());
		assertTrue(Arrays.binarySearch(index.getSortIndex(ATTRIBUTE_VARIANT_COUNT, null).getSortedRecordValues(), 115) >= 0);

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(2, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_VARIANT_COUNT);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.SORT, ATTRIBUTE_VARIANT_COUNT);
	}

	@Test
	void shouldInsertAndThenUpdateNewAttribute() {
		shouldInsertNewAttribute();

		final AttributeKey codeAttributeKey = new AttributeKey(ATTRIBUTE_CODE);
		final AttributeSchema codeSchema = new AttributeSchema(ATTRIBUTE_CODE, String.class, false);
		containerAccessor.getAttributeStorageContainer(1)
			.upsertAttribute(codeAttributeKey, codeSchema, attributeValue -> new AttributeValue(codeAttributeKey, "A"));

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, codeAttributeKey),
			index, codeAttributeKey, "B",
			false
		);

		final AttributeKey eanAttributeKey = new AttributeKey(ATTRIBUTE_EAN);
		final AttributeSchema eanSchema = new AttributeSchema(ATTRIBUTE_EAN, String.class, false);
		containerAccessor.getAttributeStorageContainer(1)
			.upsertAttribute(eanAttributeKey, eanSchema, attributeValue -> new AttributeValue(eanAttributeKey, "EAN-001"));

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, eanAttributeKey),
			index, eanAttributeKey, "EAN-002",
			false
		);

		final UniqueIndex uniqueIndex = index.getUniqueIndex(ATTRIBUTE_CODE, null);
		assertNull(uniqueIndex.getRecordIdByUniqueValue("A"));
		assertEquals(1, uniqueIndex.getRecordIdByUniqueValue("B"));

		final FilterIndex filterIndex = index.getFilterIndex(ATTRIBUTE_EAN, null);
		assertArrayEquals(new int[0], filterIndex.getRecordsEqualTo("EAN-001").getArray());
		assertArrayEquals(new int[]{1}, filterIndex.getRecordsEqualTo("EAN-002").getArray());

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(3, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.UNIQUE, ATTRIBUTE_CODE);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_CODE);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_EAN);
	}

	@Test
	void shouldInsertSimpleAndThenUpdateWithArrayAttribute() {
		final AttributeKey charArrayAttr = new AttributeKey(ATTRIBUTE_CHAR_ARRAY);
		final AttributeSchema charArraySchema = new AttributeSchema(ATTRIBUTE_CHAR_ARRAY, Character[].class, false);

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, charArrayAttr),
			index, charArrayAttr, 'A',
			false
		);
		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_CHAR_ARRAY, null).getRecordsEqualTo('A').getArray());

		containerAccessor.getAttributeStorageContainer(1)
			.upsertAttribute(charArrayAttr, charArraySchema, attributeValue -> new AttributeValue(charArrayAttr, new Character[]{'A'}));

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, charArrayAttr),
			index, charArrayAttr, new Character[]{'C', 'D'},
			false
		);

		final FilterIndex filterIndex = index.getFilterIndex(ATTRIBUTE_CHAR_ARRAY, null);
		assertArrayEquals(new int[0], filterIndex.getRecordsEqualTo('A').getArray());
		assertArrayEquals(new int[]{1}, filterIndex.getRecordsEqualTo('C').getArray());
		assertArrayEquals(new int[]{1}, filterIndex.getRecordsEqualTo('D').getArray());

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(1, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_CHAR_ARRAY);
	}

	@Test
	void shouldInsertAndThenUpdateNewArrayAttribute() {
		final AttributeKey charArrayAttr = new AttributeKey(ATTRIBUTE_CHAR_ARRAY);
		final AttributeSchema charArraySchema = new AttributeSchema(ATTRIBUTE_CHAR_ARRAY, Character[].class, false);

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, charArrayAttr),
			index, charArrayAttr, new Character[]{'A', 'B'},
			false
		);
		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_CHAR_ARRAY, null).getRecordsEqualTo('A').getArray());
		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_CHAR_ARRAY, null).getRecordsEqualTo('B').getArray());

		containerAccessor.getAttributeStorageContainer(1)
			.upsertAttribute(charArrayAttr, charArraySchema, attributeValue -> new AttributeValue(charArrayAttr, new Character[]{'A', 'B'}));

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, charArrayAttr),
			index, charArrayAttr, new Character[]{'C', 'D'},
			false
		);

		final FilterIndex filterIndex = index.getFilterIndex(ATTRIBUTE_CHAR_ARRAY, null);
		assertArrayEquals(new int[0], filterIndex.getRecordsEqualTo('A').getArray());
		assertArrayEquals(new int[0], filterIndex.getRecordsEqualTo('B').getArray());
		assertArrayEquals(new int[]{1}, filterIndex.getRecordsEqualTo('C').getArray());
		assertArrayEquals(new int[]{1}, filterIndex.getRecordsEqualTo('D').getArray());

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(1, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_CHAR_ARRAY);
	}

	@Test
	void shouldReuseUniqueCode() {
		shouldInsertAndThenUpdateNewAttribute();
		index.resetDirty();

		final AttributeKey attrCode = new AttributeKey(ATTRIBUTE_CODE);
		final AttributeSchema codeSchema = new AttributeSchema(ATTRIBUTE_CODE, String.class, false);

		containerAccessor.reset();

		executeAttributeUpsert(
			new EntityIndexLocalMutationExecutor(
				containerAccessor, 2, new MockEntityIndexCreator(index),
				() -> schema,
				entityType -> entityType.equals(schema.getName()) ? schema : null
			),
			schema::getAttribute,
			new ExistingAttributeAccessor(2, executor, attrCode),
			index, attrCode, "A",
			false
		);

		final UniqueIndex uniqueIndex = index.getUniqueIndex(ATTRIBUTE_CODE, null);
		assertEquals(2, uniqueIndex.getRecordIdByUniqueValue("A"));
		assertEquals(1, uniqueIndex.getRecordIdByUniqueValue("B"));

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(2, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.UNIQUE, ATTRIBUTE_CODE);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_CODE);
	}

	@Test
	void shouldRemoveAttribute() {
		shouldInsertNewAttribute();
		index.resetDirty();

		final AttributeKey attributeCode = new AttributeKey(ATTRIBUTE_CODE);
		final AttributeSchema codeSchema = new AttributeSchema(ATTRIBUTE_CODE, String.class, false);

		containerAccessor.getAttributeStorageContainer(1)
			.upsertAttribute(attributeCode, codeSchema, attributeValue -> new AttributeValue(attributeCode, "A"));

		executeAttributeRemoval(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, attributeCode), index, attributeCode,
			false
		);
		assertNull(index.getUniqueIndex(ATTRIBUTE_CODE, null));
		assertNull(index.getFilterIndex(ATTRIBUTE_CODE, null));

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(0, modifiedStorageParts.size());
	}

	@Test
	void shouldApplyDeltaToAttribute() {
		final AttributeKey attrVariantCount = new AttributeKey(ATTRIBUTE_VARIANT_COUNT);
		final AttributeSchema variantSchema = new AttributeSchema(ATTRIBUTE_VARIANT_COUNT, Integer.class, false);

		executeAttributeUpsert(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, attrVariantCount),
			index, attrVariantCount, 10,
			false
		);
		executeAttributeUpsert(
			new EntityIndexLocalMutationExecutor(
				containerAccessor, 2, new MockEntityIndexCreator(index),
				() -> schema,
				entityType -> entityType.equals(schema.getName()) ? schema : null
			),
			schema::getAttribute,
			new ExistingAttributeAccessor(2, executor, attrVariantCount),
			index, attrVariantCount, 9,
			false
		);

		assertNull(index.getUniqueIndex(ATTRIBUTE_VARIANT_COUNT, null));
		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_VARIANT_COUNT, null).getRecordsEqualTo(10).getArray());
		final int position = findInArray(index.getSortIndex(ATTRIBUTE_VARIANT_COUNT, null).getAscendingOrderRecordsSupplier().getSortedRecordIds(), 1);
		assertTrue(position >= 0);

		containerAccessor.getAttributeStorageContainer(1)
			.upsertAttribute(attrVariantCount, variantSchema, attributeValue -> new AttributeValue(attrVariantCount, 10));

		executeAttributeDelta(
			executor, schema::getAttribute,
			new ExistingAttributeAccessor(1, executor, attrVariantCount),
			index, attrVariantCount, -3
		);

		assertArrayEquals(new int[]{1}, index.getFilterIndex(ATTRIBUTE_VARIANT_COUNT, null).getRecordsEqualTo(7).getArray());
		assertArrayEquals(new int[0], index.getFilterIndex(ATTRIBUTE_VARIANT_COUNT, null).getRecordsEqualTo(10).getArray());
		assertTrue(findInArray(index.getSortIndex(ATTRIBUTE_VARIANT_COUNT, null).getAscendingOrderRecordsSupplier().getSortedRecordIds(), 1) < position);

		final Collection<StoragePart> modifiedStorageParts = index.getModifiedStorageParts();
		assertEquals(2, modifiedStorageParts.size());
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.FILTER, ATTRIBUTE_VARIANT_COUNT);
		assertContainsChangedPart(modifiedStorageParts, AttributeIndexType.SORT, ATTRIBUTE_VARIANT_COUNT);
	}

	private void assertContainsChangedPart(Collection<StoragePart> changedStorageParts, AttributeIndexType type, String attributeName) {
		final Class<? extends StoragePart> containerType;
		switch (type) {
			case FILTER:
				containerType = FilterIndexStoragePart.class;
				break;
			case UNIQUE:
				containerType = UniqueIndexStoragePart.class;
				break;
			case SORT:
				containerType = SortIndexStoragePart.class;
				break;
			default:
				throw new IllegalArgumentException();
		}
		for (StoragePart changedStoragePart : changedStorageParts) {
			if (changedStoragePart instanceof AttributeIndexStoragePart) {
				final AttributeIndexStoragePart aisp = (AttributeIndexStoragePart) changedStoragePart;
				if (containerType.isInstance(changedStoragePart)) {
					final AttributeKey attributeKey = aisp.getAttributeKey();
					if (attributeName.equals(attributeKey.getAttributeName())) {
						return;
					}
				}
			}
		}
		fail("Expected " + type + " storage part for attribute " + attributeName + " was not found!");
	}

	private static int findInArray(int[] ids, int id) {
		for (int i = 0; i < ids.length; i++) {
			int examinedId = ids[i];
			if (examinedId == id) {
				return i;
			}
		}
		return -1;
	}

}