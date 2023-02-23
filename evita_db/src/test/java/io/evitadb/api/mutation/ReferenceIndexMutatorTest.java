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
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.data.mutation.reference.InsertReferenceMutation;
import io.evitadb.api.data.mutation.reference.ReferenceAttributesUpdateMutation;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.GlobalEntityIndex;
import io.evitadb.test.Entities;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.mutation.ReferenceIndexMutator.attributeUpdate;
import static io.evitadb.api.mutation.ReferenceIndexMutator.referenceInsert;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test verifies contract of {@link ReferenceIndexMutator} class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ReferenceIndexMutatorTest extends AbstractMutatorTestBase {
	private static final String ATTRIBUTE_BRAND_CODE = "brandCode";
	private static final String ATTRIBUTE_BRAND_EAN = "brandEan";
	private static final String ATTRIBUTE_VARIANT_COUNT = "variantCount";
	private static final String ATTRIBUTE_CHAR_ARRAY = "charArray";
	private final EntityIndex entityIndex = new GlobalEntityIndex(1, new EntityIndexKey(EntityIndexType.GLOBAL), () -> schema);
	private final EntityIndex referenceTypesIndex = new GlobalEntityIndex(1, new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, Entities.BRAND), () -> schema);
	private final EntityIndex referenceIndex = new GlobalEntityIndex(2, new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY, 1), () -> schema);

	@Override
	protected void alterProductSchema(EntitySchemaBuilder schema) {
		schema.withReferenceTo(Entities.BRAND, thatIs -> {
			thatIs.withAttribute(ATTRIBUTE_BRAND_CODE, String.class, whichIs -> whichIs.unique());
			thatIs.withAttribute(ATTRIBUTE_BRAND_EAN, String.class, whichIs -> whichIs.filterable());
			thatIs.withAttribute(ATTRIBUTE_VARIANT_COUNT, Integer.class, whichIs -> whichIs.sortable().filterable());
			thatIs.withAttribute(ATTRIBUTE_CHAR_ARRAY, Character[].class, whichIs -> whichIs.filterable());
		});
	}

	@Test
	void shouldInsertNewReference() {
		final InsertReferenceMutation referenceMutation = new InsertReferenceMutation(new Reference(schema, new EntityReference(Entities.BRAND, 10), null), schema);
		referenceInsert(
			executor, entityIndex, referenceTypesIndex, referenceIndex, referenceMutation.getCreatedReference()
		);
		assertArrayEquals(new int[]{10}, referenceTypesIndex.getAllPrimaryKeys().getArray());
		assertArrayEquals(new int[]{1}, referenceIndex.getAllPrimaryKeys().getArray());
	}

	@Test
	void shouldIndexAttributes() {
		final EntityReference referencedEntity = new EntityReference(Entities.BRAND, 10);
		final InsertReferenceMutation referenceMutation2 = new InsertReferenceMutation(new Reference(schema, referencedEntity, null), schema);
		referenceInsert(
			executor, entityIndex, referenceTypesIndex, referenceIndex, referenceMutation2.getCreatedReference()
		);
		final ReferenceAttributesUpdateMutation referenceMutation = new ReferenceAttributesUpdateMutation(referencedEntity, schema, new UpsertAttributeMutation(new AttributeKey(ATTRIBUTE_VARIANT_COUNT), 55));
		attributeUpdate(
			executor, referenceTypesIndex, referenceIndex, referenceMutation.getReferenceKey(), referenceMutation.getAttributeMutation()
		);
		final ReferenceAttributesUpdateMutation a = new ReferenceAttributesUpdateMutation(referencedEntity, schema, new UpsertAttributeMutation(new AttributeKey(ATTRIBUTE_BRAND_CODE), "A"));
		attributeUpdate(
			executor, referenceTypesIndex, referenceIndex, a.getReferenceKey(), a.getAttributeMutation()
		);
		final ReferenceAttributesUpdateMutation referenceMutation1 = new ReferenceAttributesUpdateMutation(referencedEntity, schema, new UpsertAttributeMutation(new AttributeKey(ATTRIBUTE_BRAND_EAN), "EAN-001"));
		attributeUpdate(
			executor, referenceTypesIndex, referenceIndex, referenceMutation1.getReferenceKey(), referenceMutation1.getAttributeMutation()
		);

		assertArrayEquals(new int[]{10}, referenceTypesIndex.getAllPrimaryKeys().getArray());
		assertArrayEquals(new int[]{1}, referenceIndex.getAllPrimaryKeys().getArray());
		assertEquals(10, referenceTypesIndex.getUniqueIndex(ATTRIBUTE_BRAND_CODE, null).getRecordIdByUniqueValue("A"));
		assertArrayEquals(new int[]{10}, referenceTypesIndex.getFilterIndex(ATTRIBUTE_BRAND_EAN, null).getRecordsEqualTo("EAN-001").getArray());
		assertEquals(1, referenceIndex.getUniqueIndex(ATTRIBUTE_BRAND_CODE, null).getRecordIdByUniqueValue("A"));
		assertArrayEquals(new int[]{1}, referenceIndex.getFilterIndex(ATTRIBUTE_BRAND_EAN, null).getRecordsEqualTo("EAN-001").getArray());
	}

}