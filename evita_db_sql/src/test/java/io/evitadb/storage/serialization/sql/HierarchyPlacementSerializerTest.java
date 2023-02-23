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

import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.storage.SqlEvitaTestSupport;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.annotation.Nonnull;
import java.util.Map;

import static io.evitadb.storage.SqlEvitaTestSupport.PRODUCT;
import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_PRIMARY_KEY;
import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link HierarchyPlacementSerializer}
 *
 * @author Lukáš Hornych 2022
 */
class HierarchyPlacementSerializerTest {

    private static final String CATEGORY = "category";
    public static final String CATEGORY_ID = "100";
    private static final long ENTITY_ID = 1;

    private SqlParameterSourceInsertBuffer insertBuffer;
    private HierarchyPlacementSerializer hierarchyPlacementSerializer;

    @BeforeEach
    void setup() {
        insertBuffer = new SqlParameterSourceInsertBuffer();
        hierarchyPlacementSerializer = new HierarchyPlacementSerializer(SqlEvitaTestSupport.createTestEntityCollectionContext());
    }

    @Test
    void shouldSerializeHierarchyPlacementToInsert() {
        insertHierarchyPlacement(createSelfHierarchyPlacement());
        assertInsertedAttribute(SELF_TYPE, SELF_PRIMARY_KEY);

        insertBuffer.reset();

        insertHierarchyPlacement(createReferencedHierarchyPlacement());
        assertInsertedAttribute(CATEGORY, CATEGORY_ID);
    }


    private HierarchyItem createSelfHierarchyPlacement() {
        return new HierarchyItemBase(PRODUCT, String.valueOf(ENTITY_ID), (short) 1, 10L, 20L, (short) 0, (short) 1, (short) 1);
    }

    private HierarchyItem createReferencedHierarchyPlacement() {
        return new HierarchyItemBase(CATEGORY, CATEGORY_ID, (short) 1, 10L, 20L, (short) 0, (short) 1, (short) 1);
    }

    private Entity createEntity() {
        return new InitialEntityBuilder(PRODUCT, (int) ENTITY_ID).toInstance();
    }

    private void insertHierarchyPlacement(@Nonnull HierarchyItem hierarchyItem) {
        hierarchyPlacementSerializer.serializeToInsert(insertBuffer, hierarchyItem, createEntity(), ENTITY_ID);
    }

    private void assertInsertedAttribute(String type, String primaryKey) {
        final Map<String, Object> placement = ((MapSqlParameterSource) insertBuffer.toArray()[0]).getValues();

        assertEquals(6, placement.size());
        assertEquals(ENTITY_ID, placement.get("entity_id"));
        assertEquals(type, placement.get("type"));
        assertEquals(primaryKey, placement.get("primaryKey"));
        assertEquals(10L, placement.get("leftBound"));
        assertEquals(20L, placement.get("rightBound"));
        assertEquals((short) 1, placement.get("level"));
    }
}