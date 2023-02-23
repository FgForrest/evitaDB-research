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

import io.evitadb.api.dataType.EnumWrapper;
import io.evitadb.api.query.Constraint;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link EvitaQLOrderConstraintVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLOrderConstraintVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldParseOrderByConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("orderBy(ascending('a'))");
        assertEquals(orderBy(ascending("a")), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("orderBy(ascending('a'),descending('b'))");
        assertEquals(orderBy(ascending("a"), descending("b")), constraint2);

        final Constraint<?> constraint3 = parseOrderConstraint("orderBy( ascending('a') ,  descending('b') )");
        assertEquals(orderBy(ascending("a"), descending("b")), constraint3);
    }

    @Test
    void shouldNotParseOrderByConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("orderBy"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("orderBy()"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("orderBy(equals('a',1))"));
    }

    @Test
    void shouldParseAscendingConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("ascending('a')");
        assertEquals(ascending("a"), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("ascending( 'a'  )");
        assertEquals(ascending("a"), constraint2);
    }

    @Test
    void shouldNotParseAscendingConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("ascending"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("ascending()"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("ascending(10)"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("ascending('a', 'b')"));
    }

    @Test
    void shouldParseDescendingConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("descending('a')");
        assertEquals(descending("a"), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("descending( 'a'  )");
        assertEquals(descending("a"), constraint2);
    }

    @Test
    void shouldNotParseDescendingConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("descending"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("descending()"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("descending(10)"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("descending('a', 'b')"));
    }

    @Test
    void shouldParsePriceAscendingConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("priceAscending()");
        assertEquals(priceAscending(), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("priceAscending(  )");
        assertEquals(priceAscending(), constraint2);
    }

    @Test
    void shouldNotParsePriceAscendingConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("priceAscending"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("priceAscending('a')"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("priceAscending('a', 'b')"));
    }

    @Test
    void shouldParsePriceDescendingConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("priceDescending()");
        assertEquals(priceDescending(), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("priceDescending(  )");
        assertEquals(priceDescending(), constraint2);
    }

    @Test
    void shouldNotParsePriceDescendingConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("priceDescending"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("priceDescending('a')"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("priceDescending('a', 'b')"));
    }

    @Test
    void shouldParseRandomConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("random()");
        assertEquals(random(), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("random (  )");
        assertEquals(random(), constraint2);
    }

    @Test
    void shouldNotParseRandomConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("random"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("random('a')"));
    }

    @Test
    void shouldParseReferenceAttributeConstraint() {
        final Constraint<?> constraint1 = parseOrderConstraint("referenceAttribute('a',ascending('b'))");
        assertEquals(referenceAttribute("a", ascending("b")), constraint1);

        final Constraint<?> constraint2 = parseOrderConstraint("referenceAttribute('a',ascending('b'),descending('c'))");
        assertEquals(
                referenceAttribute("a", ascending("b"), descending("c")),
                constraint2
        );

        final Constraint<?> constraint3 = parseOrderConstraint("referenceAttribute ( 'a' , ascending('b')  , descending('c') )");
        assertEquals(
                referenceAttribute("a", ascending("b"), descending("c")),
                constraint3
        );

        final Constraint<?> constraint4 = parseOrderConstraint("referenceAttribute(SOME_ENUM,ascending('b'))");
        assertEquals(referenceAttribute(EnumWrapper.fromString("SOME_ENUM"), ascending("b")), constraint4);
    }

    @Test
    void shouldNotParseReferenceAttributeConstraint() {
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("referenceAttribute"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("referenceAttribute()"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("referenceAttribute('a')"));
        assertThrows(RuntimeException.class, () -> parseOrderConstraint("referenceAttribute('a',1)"));
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "orderConstraint"
     *
     * @param string string to parse
     * @return parsed constraint
     */
    private Constraint<?> parseOrderConstraint(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.orderConstraint().accept(new EvitaQLOrderConstraintVisitor());
    }
}
