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

package io.evitadb.api.query.parser.visitor;

import io.evitadb.api.query.Query;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link EvitaQLQueryVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLQueryVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldParseQueryWithDifferentCountsOfArguments() {
        assertEquals(
                query(
                        entities("a")
                ),
                parseQuery("query(entities('a'))")
        );

        assertEquals(
                query(
                        entities("a"),
                        filterBy(eq("a", 1L))
                ),
                parseQuery("query(entities('a'),filterBy(equals('a',1)))")
        );

        assertEquals(
                query(
                        entities("a"),
                        orderBy(ascending("c"))
                ),
                parseQuery("query(entities('a'),orderBy(ascending('c')))")
        );

        assertEquals(
                query(
                        entities("a"),
                        require(attributes())
                ),
                parseQuery("query(entities('a'),require(attributes()))")
        );

        assertEquals(
                query(
                        entities("a"),
                        filterBy(eq("a", 1L)),
                        orderBy(ascending("c")),
                        require(attributes())
                ),
                parseQuery("query(require(attributes()),entities('a'),orderBy(ascending('c')),filterBy(equals('a',1)))")
        );

        assertEquals(
                query(
                        entities("a"),
                        filterBy(
                                and(
                                        isTrue("b"),
                                        eq("c", 5L)
                                )
                        ),
                        orderBy(
                                ascending("c"),
                                priceAscending()
                        ),
                        require(attributes())
                ),
                parseQuery(
                        query(
                                entities("a"),
                                filterBy(
                                        and(
                                                isTrue("b"),
                                                eq("c", 5L)
                                        )
                                ),
                                orderBy(
                                        ascending("c"),
                                        priceAscending()
                                ),
                                require(attributes())
                        ).prettyPrint()
                )
        );

        assertEquals(
                query(
                        entities("a"),
                        filterBy(
                                and(
                                        isTrue("b"),
                                        eq("c", 5L)
                                )
                        ),
                        orderBy(
                                ascending("c"),
                                priceAscending()
                        ),
                        require(attributes())
                ),
                parseQuery(
                        query(
                                entities("a"),
                                filterBy(
                                        and(
                                                isTrue("b"),
                                                eq("c", 5L)
                                        )
                                ),
                                orderBy(
                                        ascending("c"),
                                        priceAscending()
                                ),
                                require(attributes())
                        ).toString()
                )
        );
    }

    @Test
    void shouldNotParseQuery() {
        assertThrows(RuntimeException.class, () -> parseQuery("query"));
        assertThrows(RuntimeException.class, () -> parseQuery("query()"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(filterBy(equals('a',1)))"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(entities('a'),equals('b',1))"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(entities('a'),ascending('b'))"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(entities('a'),attributes())"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(entities('a'),filterBy(equals('b',1)),equals('c',1))"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(entities('a'),orderBy(ascending('c')),ascending('b'))"));
        assertThrows(RuntimeException.class, () -> parseQuery("query(entities('a'),require(entityBody()),attributes())"));
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "query"
     *
     * @param string string to parse
     * @return query
     */
    private Query parseQuery(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.query().accept(new EvitaQLQueryVisitor());
    }
}
