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

package io.evitadb.storage.query.translate;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.And;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link ConstraintRemovingVisitor}
 *
 * @author Lukáš Hornych 2022
 */
class ConstraintRemovingVisitorTest {

    @Test
    void shouldNotRemoveAnything() {
        final FilterConstraint originalConstraint = and(
            eq("a", 1),
            isFalse("b"),
            or(
                eq("c", "d")
            )
        );
        final FilterConstraint modifiedConstraint = visit(originalConstraint);
        assertEquals(originalConstraint, modifiedConstraint);
    }

    @Test
    void shouldRemoveForbiddenConstraint() {
        final FilterConstraint originalConstraint1 = and(isTrue("a"));
        final FilterConstraint modifiedConstraint1 = visit(originalConstraint1);
        assertEquals(new And(), modifiedConstraint1);

        final FilterConstraint originalConstraint2 = and(
                eq("a", 1),
                isTrue("b"),
                isFalse("c")
        );
        final FilterConstraint modifiedConstraint2 = visit(originalConstraint2);
        assertEquals(
                and(
                    eq("a", 1),
                    isFalse("c")
                ),
                modifiedConstraint2
        );

        final FilterConstraint originalConstraint3 = and(
                eq("a", 1),
                or(
                    isTrue("b"),
                    and(
                        eq("c", 2),
                        isTrue("d")
                    )
                )
        );
        final FilterConstraint modifiedConstraint3 = visit(originalConstraint3);
        assertEquals(
                and(
                    eq("a", 1),
                    or(
                        and(
                            eq("c", 2)
                        )
                    )
                ),
                modifiedConstraint3
        );
    }

    private FilterConstraint visit(FilterConstraint originalConstraint) {
        final ConstraintRemovingVisitor<FilterConstraint> visitor = new ConstraintRemovingVisitor<>(c -> c.getName().equals("isTrue"));
        visitor.visit(originalConstraint);
        return visitor.getModifiedTree();
    }
}