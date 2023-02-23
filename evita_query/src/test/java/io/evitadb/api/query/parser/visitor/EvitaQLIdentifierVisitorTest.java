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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link EvitaQLIdentifierVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLIdentifierVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldParseIdentifier() {
        final String identifier1 = parseIdentifier("'name'");
        assertEquals("name", identifier1);

        final String identifier2 = parseIdentifier("'name or something'");
        assertEquals("name or something", identifier2);

        final String identifier3 = parseIdentifier("'name-or_something'");
        assertEquals("name-or_something", identifier3);
    }

    @Test
    void shouldNotParseIdentifier() {
        assertThrows(RuntimeException.class, () -> parseIdentifier("100"));
        assertThrows(RuntimeException.class, () -> parseIdentifier("100.588"));
        assertThrows(RuntimeException.class, () -> parseIdentifier("NAME"));
        assertThrows(RuntimeException.class, () -> parseIdentifier("name"));
        assertThrows(RuntimeException.class, () -> parseIdentifier("2020-20-11"));
        assertThrows(RuntimeException.class, () -> parseIdentifier("2020-20-11T15:16:30"));
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "identifier"
     *
     * @param string string to parse
     * @return parsed identifier
     */
    private String parseIdentifier(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.identifier().accept(new EvitaQLIdentifierVisitor());
    }
}
