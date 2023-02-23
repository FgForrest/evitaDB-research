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

package io.evitadb.api.query.visitor;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test verifies contract of {@link ConstraintCloneVisitor} class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
class ConstraintCloneVisitorTest {
	private FilterConstraint constraint;

	@BeforeEach
	void setUp() {
		constraint = and(
			eq("a", "b"),
			or(
				isNotNull("def"),
				isTrue("xev"),
				between("c", 1, 78),
				not(
					isTrue("utr")
				)
			)
		);
	}

	@Test
	void shouldCloneFilteringConstraintReplacingIsTrue() {
		final FilterConstraint clone = ConstraintCloneVisitor.clone(constraint, (visitor, examined) -> {
			if (examined instanceof IsTrue) {
				return new IsFalse(((IsTrue) examined).getAttributeName());
			} else {
				return examined;
			}
		});

		assertEquals("\tand(\n" +
			"\t\tequals('a', 'b'),\n" +
			"\t\tor(\n" +
			"\t\t\tisNotNull('def'),\n" +
			"\t\t\tisFalse('xev'),\n" +
			"\t\t\tbetween('c', 1, 78),\n" +
			"\t\t\tnot(\n" +
			"\t\t\t\tisFalse('utr')\n" +
			"\t\t\t)\n" +
			"\t\t)\n" +
			"\t)",
			PrettyPrintingVisitor.toString(clone, 1)
		);
	}

	@Test
	void shouldCloneFilteringConstraintReplacingIsTrueWithNull() {
		final FilterConstraint clone = ConstraintCloneVisitor.clone(constraint, (visitor, examined) -> {
			if (examined instanceof IsTrue) {
				return null;
			} else {
				return examined;
			}
		});

		assertEquals("\tand(\n" +
				"\t\tequals('a', 'b'),\n" +
				"\t\tor(\n" +
				"\t\t\tisNotNull('def'),\n" +
				"\t\t\tbetween('c', 1, 78)\n" +
				"\t\t)\n" +
				"\t)",
			PrettyPrintingVisitor.toString(clone, 1)
		);
	}

	@Test
	void shouldCloneFilteringConstraintReplacingBetweenWithNull() {
		final FilterConstraint clone = ConstraintCloneVisitor.clone(constraint, (visitor, examined) -> {
			if (examined instanceof Between) {
				return null;
			} else {
				return examined;
			}
		});

		assertEquals("\tand(\n" +
				"\t\tequals('a', 'b'),\n" +
				"\t\tor(\n" +
				"\t\t\tisNotNull('def'),\n" +
				"\t\t\tisTrue('xev'),\n" +
				"\t\t\tnot(\n" +
				"\t\t\t\tisTrue('utr')\n" +
				"\t\t\t)\n" +
				"\t\t)\n" +
				"\t)",
			PrettyPrintingVisitor.toString(clone, 1)
		);
	}

	@Test
	void shouldCloneFilteringConstraintReplacingNotWithAnd() {
		final FilterConstraint clone = ConstraintCloneVisitor.clone(constraint, (visitor, examined) -> {
			if (examined instanceof Not) {
				return new And(
					Stream.concat(
						visitor.analyseChildren((Not) examined).stream(),
						Stream.of(new IsTrue("added"))
					).toArray(FilterConstraint[]::new)
				);
			} else {
				return examined;
			}
		});

		assertEquals("\tand(\n" +
				"\t\tequals('a', 'b'),\n" +
				"\t\tor(\n" +
				"\t\t\tisNotNull('def'),\n" +
				"\t\t\tisTrue('xev'),\n" +
				"\t\t\tbetween('c', 1, 78),\n" +
				"\t\t\tand(\n" +
				"\t\t\t\tisTrue('utr'),\n" +
				"\t\t\t\tisTrue('added')\n" +
				"\t\t\t)\n" +
				"\t\t)\n" +
				"\t)",
			PrettyPrintingVisitor.toString(clone, 1)
		);
	}
}