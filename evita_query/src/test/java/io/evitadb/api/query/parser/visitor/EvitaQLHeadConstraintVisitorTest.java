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

import static io.evitadb.api.query.QueryConstraints.entities;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link EvitaQLHeadConstraintVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLHeadConstraintVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldParseEntitiesConstraint() {
        final Constraint<?> constraint1 = parseHeadConstraint("entities('product')");
        assertEquals(entities("product"), constraint1);

        final Constraint<?> constraint2 = parseHeadConstraint("entities(PRODUCT)");
        assertEquals(entities(EnumWrapper.fromString("PRODUCT")), constraint2);
    }

    @Test
    void shouldNotParseEntitiesConstraint() {
        assertThrows(RuntimeException.class, () -> parseHeadConstraint("entities"));
        assertThrows(RuntimeException.class, () -> parseHeadConstraint("entities()"));
        assertThrows(RuntimeException.class, () -> parseHeadConstraint("entities('product','variant')"));
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "headingConstraint"
     *
     * @param string string to parse
     * @return parsed constraint
     */
    private Constraint<?> parseHeadConstraint(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.headConstraint().accept(new EvitaQLHeadConstraintVisitor());
    }
}
