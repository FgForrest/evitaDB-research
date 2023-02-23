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

package io.evitadb.storage.serialization.sql;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import static io.evitadb.storage.SqlEvitaTestSupport.PRODUCT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AttributeSerializer}
 *
 * @author Lukáš Hornych 2022
 */
class AttributeSerializerTest {

    private static final String ATTRIBUTE_NAME_A = "a";

    private SqlParameterSourceInsertBuffer insertBuffer;
    private AttributeSerializer attributeSerializer;

    @BeforeEach
    void setup() {
        insertBuffer = new SqlParameterSourceInsertBuffer();
        attributeSerializer = new AttributeSerializer();
    }

    @Test
    void shouldSerializeAttributeToInsert() {
        // ignore dropped attribute
        insertAttribute(null, int.class, 1, true, null, null);
        assertTrue(insertBuffer.isEmpty());

        // global int attribute
        insertAttribute(null, int.class, 1, false, null, null);
        assertEquals(1, insertBuffer.size());
        assertInsertedAttribute(null, null, null, null, new Long[] { 1L }, null);

        insertBuffer.reset();

        // localized string attribute
        insertAttribute(Locale.ENGLISH, String.class, "b", false, null, null);
        assertEquals(1, insertBuffer.size());
        assertInsertedAttribute(null, null, "en", new String[] { "b" }, null, null);

        insertBuffer.reset();

        // reference range attribute
        insertAttribute(null, NumberRange.class, new NumberRange[] { NumberRange.between(10L, 20L) }, false, 10L, createReference());
        assertEquals(1, insertBuffer.size());
        assertInsertedAttribute(10L, "category", null, null, null, "{\"[10,20]\"}");
    }

    @Test
    void shouldSerializeAttributeToUpdate() {
        // ignore dropped value
        assertNull(updateAttribute(int.class, 1, true));

        final SqlParameterSource attribute1 = updateAttribute(String.class, "b", false);
        assertUpdatedAttribute(attribute1, new String[] { "b" }, null, null);

        final SqlParameterSource attribute2 = updateAttribute(int.class, 1, false);
        assertUpdatedAttribute(attribute2, null, new Long[] { 1L }, null);

        final SqlParameterSource attribute3 = updateAttribute(NumberRange.class, NumberRange.between(10L, 20L), false);
        assertUpdatedAttribute(attribute3, null, null, "{\"[10,20]\"}");
    }


    private AttributeValue createAttribute(@Nullable Locale locale, int version, @Nonnull Serializable value, boolean dropped) {
        return new AttributeValue(
                version,
                new AttributeKey(ATTRIBUTE_NAME_A, locale),
                value,
                dropped
        );
    }

    private AttributeSchema createSchema(@Nullable Locale locale, @Nonnull Class<? extends Serializable> type) {
        return new AttributeSchema(ATTRIBUTE_NAME_A, type, locale != null);
    }

    private ReferenceContract createReference() {
        return new Reference(new EntitySchema(PRODUCT), new EntityReference("category", 20), new GroupEntityReference("brand", 30));
    }

    private void insertAttribute(@Nullable Locale locale,
                                 @Nonnull Class<? extends Serializable> type,
                                 @Nonnull Serializable value,
                                 boolean dropped,
                                 @Nullable Long referenceId,
                                 @Nullable ReferenceContract reference) {
        attributeSerializer.serializeToInsert(
                insertBuffer,
                createAttribute(locale, 1, value, dropped),
                createSchema(locale, type),
                1,
                referenceId,
                reference
        );
    }

    private SqlParameterSource updateAttribute(@Nonnull Class<? extends Serializable> type,
                                               @Nonnull Serializable value,
                                               boolean dropped) {
        return attributeSerializer.serializeToUpdate(
                createAttribute(null, 2, value, dropped),
                createSchema(null, type),
                1
        );
    }

    private void assertInsertedAttribute(Long referenceId, String referenceType, String locale, String[] stringValues, Long[] intValues, String intRangeValues) {
        final Map<String, Object> attribute = ((MapSqlParameterSource) insertBuffer.toArray()[0]).getValues();

        int mapSize = 6;
        if (referenceId != null) {
            mapSize += 2;
        }
        if (locale != null) {
            mapSize++;
        }
        assertEquals(mapSize, attribute.size());
        assertEquals(1L, attribute.get("entity_id"));
        assertEquals(referenceId, attribute.get("reference_id"));
        assertEquals(referenceType, attribute.get("reference_entityType"));
        assertEquals(ATTRIBUTE_NAME_A, attribute.get("name"));
        assertEquals(locale, attribute.get("locale"));
        assertEquals(1, attribute.get("version"));
        assertEquals(false, attribute.get("sortable"));
        assertEquals(false, attribute.get("uniq"));
        if (stringValues != null) {
            assertArrayEquals(stringValues, (String[]) attribute.get("stringValues"));
        } else {
            assertNull(attribute.get("stringValues"));
        }
        if (intValues != null) {
            assertArrayEquals(intValues, (Long[]) attribute.get("intValues"));
        } else {
            assertNull(attribute.get("intValues"));
        }
        if (intRangeValues != null) {
            assertEquals(intRangeValues, attribute.get("intRangeValues").toString());
        } else {
            assertNull(attribute.get("intRangeValues"));
        }
    }

    private void assertUpdatedAttribute(SqlParameterSource attributeSource, String[] stringValues, Long[] intValues, String intRangeValues) {
        final Map<String, Object> attribute = ((MapSqlParameterSource) attributeSource).getValues();

        assertEquals(5, attribute.size());
        assertEquals(2, attribute.get("version"));
        assertEquals(1L, attribute.get("attributeId"));
        if (stringValues != null) {
            assertArrayEquals(stringValues, (String[]) attribute.get("stringValues"));
        } else {
            assertNull(attribute.get("stringValues"));
        }
        if (intValues != null) {
            assertArrayEquals(intValues, (Long[]) attribute.get("intValues"));
        } else {
            assertNull(attribute.get("intValues"));
        }
        if (intRangeValues != null) {
            assertEquals(intRangeValues, attribute.get("intRangeValues").toString());
        } else {
            assertNull(attribute.get("intRangeValues"));
        }
    }

}