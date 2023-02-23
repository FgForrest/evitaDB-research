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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SqlParameterSourceInsertBuffer}
 *
 * @author Lukáš Hornych 2022
 */
class SqlParameterSourceInsertBufferTest {

    private static final SqlParameterSource[] ITEMS = new SqlParameterSource[] {
            new MapSqlParameterSource("item1", "value1"),
            new MapSqlParameterSource("item1", "value1"),
            new MapSqlParameterSource("item1", "value1")
    };

    private SqlParameterSourceInsertBuffer insertBuffer;

    @BeforeEach
    void setup() {
        insertBuffer = new SqlParameterSourceInsertBuffer();
    }

    @Test
    void shouldStoreAndRetrieveBufferedItems() {
        insertItems();

        final SqlParameterSource[] bufferedItems = insertBuffer.toArray();
        assertArrayEquals(ITEMS, bufferedItems);
    }

    @Test
    void shouldCountBufferedItems() {
        assertTrue(insertBuffer.isEmpty());
        assertEquals(0, insertBuffer.size());

        insertItems();

        assertFalse(insertBuffer.isEmpty());
        assertEquals(3, insertBuffer.size());
    }

    @Test
    void shouldResetBuffer() {
        insertItems();

        assertFalse(insertBuffer.isEmpty());

        insertBuffer.reset();
        assertTrue(insertBuffer.isEmpty());
        assertEquals(0, insertBuffer.size());
    }


    private void insertItems() {
        insertBuffer.add(ITEMS[0]);
        insertBuffer.add(ITEMS[1]);
        insertBuffer.add(ITEMS[2]);
    }
}