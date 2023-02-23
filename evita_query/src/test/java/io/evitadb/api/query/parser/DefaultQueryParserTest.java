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

package io.evitadb.api.query.parser;

import io.evitadb.api.dataType.EnumWrapper;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultQueryParser}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class DefaultQueryParserTest {

    private final DefaultQueryParser parser = new DefaultQueryParser();

    @Test
    void shouldGetInstance() {
        final DefaultQueryParser queryParser1 = DefaultQueryParser.getInstance();
        final DefaultQueryParser queryParser2 = DefaultQueryParser.getInstance();
        assertSame(queryParser1, queryParser2);
    }

    @Test
    void shouldParseQueryString() {
        assertEquals(
                query(
                        entities("a")
                ),
                parser.parseQuery("query(entities('a'))")
        );

        assertEquals(
                query(
                        entities("a"),
                        filterBy(isTrue("b"))
                ),
                parser.parseQuery("query(entities('a'),filterBy(isTrue('b')))")
        );

        assertEquals(
                query(
                        entities("a"),
                        filterBy(isTrue("b")),
                        orderBy(random()),
                        require(attributes())
                ),
                parser.parseQuery("query(entities('a'),filterBy(isTrue('b')),orderBy(random()),require(attributes()))")
        );
    }

    @Test
    void shouldNotParseQueryString() {
        assertThrows(RuntimeException.class, () -> parser.parseQuery(""));
        assertThrows(RuntimeException.class, () -> parser.parseQuery("'b'"));
        assertThrows(RuntimeException.class, () -> parser.parseQuery("isTrue('a')"));
        assertThrows(RuntimeException.class, () -> parser.parseQuery("query(entities('a')) query(entities('b'))"));
    }

    @Test
    void shouldParseConstraintString() {
        assertEquals(
                entities("a"),
                parser.parseConstraint("entities('a')")
        );

        assertEquals(
                filterBy(
                        isTrue("a")
                ),
                parser.parseConstraint("filterBy(isTrue('a'))")
        );

        assertEquals(
                orderBy(
                        random()
                ),
                parser.parseConstraint("orderBy(random())")
        );

        assertEquals(
                require(
                        attributes()
                ),
                parser.parseConstraint("require(attributes())")
        );
    }

    @Test
    void shouldNotParseConstraintString() {
        assertThrows(RuntimeException.class, () -> parser.parseConstraint(""));
        assertThrows(RuntimeException.class, () -> parser.parseConstraint("'b'"));
        assertThrows(RuntimeException.class, () -> parser.parseConstraint("isTrue('a') isTrue('a')"));
        assertThrows(RuntimeException.class, () -> parser.parseConstraint("query(entities('a'))"));
    }

    @Test
    void shouldParseLiteralString() {
        assertEquals("a", parser.parseLiteral("'a'"));
        assertEquals(123L, (Long) parser.parseLiteral("123"));
        assertEquals(EnumWrapper.fromString("SOME_ENUM"), parser.parseLiteral("SOME_ENUM"));
    }

    @Test
    void shouldNotParseLiteralString() {
        assertThrows(RuntimeException.class, () -> parser.parseLiteral(""));
        assertThrows(RuntimeException.class, () -> parser.parseLiteral("_"));
        assertThrows(RuntimeException.class, () -> parser.parseLiteral("isTrue('a')"));
        assertThrows(RuntimeException.class, () -> parser.parseLiteral("12 24"));
        assertThrows(RuntimeException.class, () -> parser.parseLiteral("query(entities('a'))"));
    }
}
