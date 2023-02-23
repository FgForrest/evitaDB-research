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

import io.evitadb.api.query.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EvitaQLConstraintVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLConstraintVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldDelegateToHeadConstraintVisitor() {
        final Constraint<?> constraint = parseConstraint("entities('col')");
        assertTrue(HeadConstraint.class.isAssignableFrom(constraint.getClass()));
    }

    @Test
    void shouldDelegateToFilterConstraintVisitor() {
        final Constraint<?> constraint = parseConstraint("equals('name', 100)");
        assertTrue(FilterConstraint.class.isAssignableFrom(constraint.getClass()));
    }

    @Test
    void shouldDelegateToOrderConstraintVisitor() {
        final Constraint<?> constraint = parseConstraint("ascending('name')");
        assertTrue(OrderConstraint.class.isAssignableFrom(constraint.getClass()));
    }

    @Test
    void shouldDelegateToRequireConstraintVisitor() {
        final Constraint<?> constraint = parseConstraint("attributes()");
        assertTrue(RequireConstraint.class.isAssignableFrom(constraint.getClass()));
    }

    @Test
    void shouldNotDelegateToAnyVisitor() {
        assertThrows(RuntimeException.class, () -> parseConstraint("nonExistingConstraint()"));
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "constraint"
     *
     * @param string string to parse
     * @return parsed constraint
     */
    private Constraint<?> parseConstraint(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.constraint().accept(new EvitaQLConstraintVisitor());
    }
}
