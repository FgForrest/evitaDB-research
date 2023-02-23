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

package io.evitadb.api.query.filter;

import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.FilterConstraint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.evitadb.api.query.QueryConstraints.and;
import static io.evitadb.api.query.QueryConstraints.eq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link And} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class AndTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final ConstraintContainer<FilterConstraint> and =
				and(
					eq("abc", "def"),
					eq("abc", "xyz")
				);
		assertNotNull(and);
		assertEquals(2, and.getConstraintCount());
		assertEquals("abc", ((Equals)and.getConstraints()[0]).getAttributeName());
		assertEquals("def", ((Equals)and.getConstraints()[0]).getAttributeValue());
		assertEquals("abc", ((Equals)and.getConstraints()[1]).getAttributeName());
		assertEquals("xyz", ((Equals)and.getConstraints()[1]).getAttributeValue());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(new And(eq("abc", "def")).isApplicable());
		assertFalse(new And().isApplicable());
	}

	@Test
	void shouldRecognizeNecessity() {
		assertTrue(new And(eq("abc", "def"), eq("xyz", "def")).isNecessary());
		assertFalse(new And(eq("abc", "def")).isNecessary());
		assertFalse(new And().isNecessary());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final ConstraintContainer<FilterConstraint> and =
				and(
						eq("abc", '\''),
						eq("abc", 'x')
				);
		assertNotNull(and);
		assertEquals("and(equals('abc','\\\''),equals('abc','x'))", and.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(createAndConstraint("abc", "def"), createAndConstraint("abc", "def"));
		assertEquals(createAndConstraint("abc", "def"), createAndConstraint("abc", "def"));
		assertNotEquals(createAndConstraint("abc", "def"), createAndConstraint("abc", "defe"));
		assertNotEquals(createAndConstraint("abc", "def"), createAndConstraint("abc", null));
		assertNotEquals(createAndConstraint("abc", "def"), createAndConstraint(null, "abc"));
		assertEquals(createAndConstraint("abc", "def").hashCode(), createAndConstraint("abc", "def").hashCode());
		assertNotEquals(createAndConstraint("abc", "def").hashCode(), createAndConstraint("abc", "defe").hashCode());
		assertNotEquals(createAndConstraint("abc", "def").hashCode(), createAndConstraint("abc", null).hashCode());
		assertNotEquals(createAndConstraint("abc", "def").hashCode(), createAndConstraint(null, "abc").hashCode());
	}

	private static And createAndConstraint(String... values) {
		return and(
				Arrays.stream(values)
						.map(it -> eq("abc", it))
						.toArray(FilterConstraint[]::new)
		);
	}
	
}