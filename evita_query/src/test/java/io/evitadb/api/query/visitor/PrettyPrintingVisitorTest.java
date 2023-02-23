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

import io.evitadb.api.query.require.EntityBody;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test verifies expected behaviour of {@link PrettyPrintingVisitor}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class PrettyPrintingVisitorTest {

	@Test
	void shouldPrettyPrintSimpleConstraint() {
		assertEquals("entityBody()", PrettyPrintingVisitor.toString(new EntityBody(), 0));
	}

	@Test
	void shouldPrettyPrintComplexConstraint() {
		assertEquals(
			"and(\n" +
				"\tequals('a', 'b'),\n" +
				"\tor(\n" +
				"\t\tisNotNull('def'),\n" +
				"\t\tbetween('c', 1, 78),\n" +
				"\t\tnot(\n" +
				"\t\t\tisTrue('utr')\n" +
				"\t\t),\n" +
				"\t\twithinHierarchy(\n" +
				"\t\t\t'd',\n" +
				"\t\t\t1,\n" +
				"\t\t\tdirectRelation()\n" +
				"\t\t),\n" +
				"\t\twithinHierarchy('e', 1)\n" +
				"\t)\n" +
				")",
			PrettyPrintingVisitor.toString(
				requireNonNull(
					and(
						eq("a", "b"),
						or(
							isNotNull("def"),
							between("c", 1, 78),
							not(
								isTrue("utr")
							),
							withinHierarchy("d", 1, directRelation()),
							withinHierarchy("e", 1)
						)
					)
				), 0
			)
		);
	}

}