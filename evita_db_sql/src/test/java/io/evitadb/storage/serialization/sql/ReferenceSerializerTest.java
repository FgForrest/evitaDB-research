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

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Map;
import java.util.Optional;

import static io.evitadb.storage.SqlEvitaTestSupport.PRODUCT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReferenceSerializer}
 *
 * @author Lukáš Hornych 2022
 */
class ReferenceSerializerTest {

    private static final String REFERENCE_TYPE = "category";
    private static final String GROUP_TYPE = "brand";
    public static final int REFERENCE_PRIMARY_KEY = 100;
    public static final long REFERENCE_ID = 10;
    public static final long ENTITY_ID = 1;

    private SqlParameterSourceInsertBuffer insertBuffer;
    private ReferenceSerializer referenceSerializer;

    @BeforeEach
    void setup() {
        insertBuffer = new SqlParameterSourceInsertBuffer();
        referenceSerializer = new ReferenceSerializer();
    }

    @Test
    void shouldSerializeReferenceToInsert() {
        // ignore non-indexable reference
        insertReference(200, false, false, false);
        assertTrue(insertBuffer.isEmpty());

        // ignore dropped reference
        insertReference(200, true, true, true);
        assertTrue(insertBuffer.isEmpty());

        insertReference(null, true, false, false);
        assertInsertedReference(null, false);

        insertBuffer.reset();

        insertReference(200, true, false, false);
        assertInsertedReference(200, false);

        insertBuffer.reset();

        insertReference(200, true, true, false);
        assertInsertedReference(200, true);
    }

    @Test
    void shouldSerializeReferenceToUpdate() {
        // ignore non-indexable value
        assertNull(updateReference(null, false, false));

        // ignore dropped value
        assertNull(updateReference(null, true, true));

        final SqlParameterSource reference1 = updateReference(null, true, false);
        assertUpdatedReference(reference1, null);

        final SqlParameterSource reference2 = updateReference(210, true, false);
        assertUpdatedReference(reference2, 210);
    }


    private ReferenceContract createReference(int version, boolean dropped, Integer groupPrimaryKey) {
        return new Reference(
                new EntitySchema(PRODUCT),
                version,
                new EntityReference(REFERENCE_TYPE, REFERENCE_PRIMARY_KEY),
                Optional.ofNullable(groupPrimaryKey).map(pk -> new GroupEntityReference(GROUP_TYPE, pk)).orElse(null),
                dropped
        );
    }

    private ReferenceSchema createSchema(boolean indexed, boolean faceted) {
        return new ReferenceSchema(REFERENCE_TYPE, false, GROUP_TYPE, false, indexed, faceted);
    }

    private void insertReference(Integer groupPrimaryKey, boolean indexed, boolean faceted, boolean dropped) {
        referenceSerializer.serializeToInsert(
                insertBuffer,
                createReference(1, dropped, groupPrimaryKey),
                createSchema(indexed, faceted),
                REFERENCE_ID,
                ENTITY_ID
        );
    }

    private SqlParameterSource updateReference(Integer groupPrimaryKey, boolean indexed, boolean dropped) {
        return referenceSerializer.serializeToUpdate(
                createReference(2, dropped, groupPrimaryKey),
                createSchema(indexed, false),
                REFERENCE_ID
        );
    }

    private void assertInsertedReference(Integer groupPrimaryKey, boolean faceted) {
        final Map<String, Object> reference = ((MapSqlParameterSource) insertBuffer.toArray()[0]).getValues();

        assertEquals((groupPrimaryKey != null) ? 10 : 7, reference.size());
        assertEquals(REFERENCE_ID, reference.get("reference_id"));
        assertEquals(ENTITY_ID, reference.get("entity_id"));
        assertEquals(1, reference.get("version"));
        assertEquals(REFERENCE_PRIMARY_KEY, reference.get("entityPrimaryKey"));
        assertEquals(REFERENCE_TYPE, reference.get("entityType"));
        assertEquals("java.lang.String", reference.get("entityTypeDataType"));
        assertEquals(faceted, reference.get("faceted"));
        if (groupPrimaryKey != null) {
            assertEquals(groupPrimaryKey, reference.get("groupPrimaryKey"));
            assertEquals(GROUP_TYPE, reference.get("groupType"));
            assertEquals("java.lang.String", reference.get("groupTypeDataType"));
        } else {
            assertNull(reference.get("groupPrimaryKey"));
            assertNull(reference.get("groupType"));
            assertNull(reference.get("groupTypeDataType"));
        }
    }

    private void assertUpdatedReference(SqlParameterSource referenceSource, Integer groupPrimaryKey) {
        final Map<String, Object> reference = ((MapSqlParameterSource) referenceSource).getValues();

        assertEquals(4, reference.size());
        assertEquals(REFERENCE_ID, reference.get("referenceId"));
        assertEquals(2, reference.get("version"));
        if (groupPrimaryKey != null) {
            assertEquals(groupPrimaryKey, reference.get("groupPrimaryKey"));
            assertEquals(GROUP_TYPE, reference.get("groupType"));
        } else {
            assertNull(reference.get("groupPrimaryKey"));
            assertNull(reference.get("groupType"));
        }
    }
}