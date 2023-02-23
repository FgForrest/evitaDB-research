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

import io.evitadb.api.data.EntityEditor;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.ExistingEntityBuilder;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.storage.CatalogContext;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.SqlEvitaTestSupport;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.storage.SqlEvitaTestSupport.PRODUCT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EntityBodySerializer}
 *
 * @author Lukáš Hornych 2022
 */
class EntityBodySerializerTest {

    private static final long ENTITY_ID = 1;
    private static final String ATTRIBUTE_NAME_A = "a";

    private SqlParameterSourceInsertBuffer insertBuffer;

    @BeforeEach
    void setup() {
        insertBuffer = new SqlParameterSourceInsertBuffer();
    }

    @Test
    void shouldSerializeEntityBodyToInsert() {
        insertEntityBody(null);
        assertInsertedEntity(null);

        insertBuffer.reset();

        insertEntityBody(100);
        assertInsertedEntity(100);
    }

    @Test
    void shouldSerializeEntityBodyToUpdate() {
        final SqlParameterSource entity1 = updateEntityBody(null);
        assertUpdatedEntity(entity1, null);

        final SqlParameterSource entity2 = updateEntityBody(100);
        assertUpdatedEntity(entity2, 100);
    }


    private EntityBodySerializer createSerializer(boolean withHierarchy) {
        final EntitySchemaBuilder schemaBuilder = new EntitySchemaBuilder(new EntitySchema(PRODUCT), schema -> schema)
                .withAttribute(ATTRIBUTE_NAME_A, Integer.class, whichIs -> whichIs.filterable().localized());
        if (withHierarchy) {
            schemaBuilder.withHierarchy();
        } else {
            schemaBuilder.withoutHierarchy();
        }

        final CatalogContext testCatalogContext = SqlEvitaTestSupport.createTestCatalogContext();
        testCatalogContext.addEntityType(PRODUCT);
        return new EntityBodySerializer(
                new EntityCollectionContext(
                        testCatalogContext,
                        "c_1",
                        PRODUCT,
                        new AtomicReference<>(schemaBuilder.applyChanges()),
                        new AtomicBoolean(true),
                        null
                )
        );
    }

    private Entity createNewEntity(Integer parentPrimaryKey) {
        final EntityEditor.EntityBuilder builder = new InitialEntityBuilder(PRODUCT, (int) ENTITY_ID)
                .setAttribute(ATTRIBUTE_NAME_A, Locale.ENGLISH, 1);
        if (parentPrimaryKey != null) {
            builder.setHierarchicalPlacement(parentPrimaryKey, 1);
        }
        return builder.toInstance();
    }

    private Entity createUpdatedEntity(Integer parentPrimaryKey) {
        return new ExistingEntityBuilder(createNewEntity(parentPrimaryKey)).toInstance();
    }

    private HierarchyItem createHierarchyPlacement() {
        return new HierarchyItemBase(PRODUCT, String.valueOf(ENTITY_ID), (short) 1, 1L, 2L, (short) 0, (short) 1, (short) 1);
    }

    private void insertEntityBody(Integer parentPrimaryKey) {
        final EntityBodySerializer serializer = createSerializer(parentPrimaryKey != null);
        serializer.serializeToInsert(
                insertBuffer,
                createNewEntity(parentPrimaryKey),
                (parentPrimaryKey != null) ? createHierarchyPlacement() : null,
                ENTITY_ID
        );
    }

    private SqlParameterSource updateEntityBody(Integer parentPrimaryKey) {
        final EntityBodySerializer serializer = createSerializer(parentPrimaryKey != null);
        return serializer.serializeToUpdate(
                createUpdatedEntity(parentPrimaryKey),
                (parentPrimaryKey != null) ? createHierarchyPlacement() : null,
                ENTITY_ID
        );
    }

    private void assertInsertedEntity(Integer parentPrimaryKey) {
        final Map<String, Object> entity = ((MapSqlParameterSource) insertBuffer.toArray()[0]).getValues();

        assertEquals(14, entity.size());
        assertEquals((int) ENTITY_ID, entity.get("primaryKey"));
        assertEquals(PRODUCT, entity.get("type"));
        assertEntityCommonPart(entity, parentPrimaryKey);
    }

    private void assertUpdatedEntity(SqlParameterSource entitySource, Integer parentPrimaryKey) {
        final Map<String, Object> entity = ((MapSqlParameterSource) entitySource).getValues();

        assertEquals(12, entity.size());
        assertEntityCommonPart(entity, parentPrimaryKey);
    }

    private void assertEntityCommonPart(Map<String, Object> entity, Integer parentPrimaryKey) {
        assertEquals(ENTITY_ID, entity.get("entityId"));
        assertEquals(1, entity.get("version"));
        assertEquals(false, entity.get("dropped"));
        assertArrayEquals(new String[] { "en" }, (String[]) entity.get("locales"));
        assertNotNull(entity.get("serializedEntity"));
        if (parentPrimaryKey != null) {
            assertEquals(parentPrimaryKey, entity.get("parentPrimaryKey"));
            assertEquals(1L, entity.get("leftBound"));
            assertEquals(2L, entity.get("rightBound"));
            assertEquals((short) 1, entity.get("level"));
            assertEquals((short) 0, entity.get("numberOfChildren"));
            assertEquals((short) 1, entity.get("orderAmongSiblings"));
            assertEquals((short) 1, entity.get("hierarchyBucket"));
        } else {
            assertNull(entity.get("leftBound"));
            assertNull(entity.get("rightBound"));
            assertNull(entity.get("level"));
            assertNull(entity.get("parentPrimaryKey"));
            assertNull(entity.get("numberOfChildren"));
            assertNull(entity.get("orderAmongSiblings"));
            assertNull(entity.get("hierarchyBucket"));
        }
    }

}