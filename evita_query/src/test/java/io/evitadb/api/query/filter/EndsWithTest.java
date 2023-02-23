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

import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.QueryConstraints.endsWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link EndsWith} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EndsWithTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final EndsWith endsWith = endsWith("abc", "def");
		assertEquals("abc", endsWith.getAttributeName());
		assertEquals("def", endsWith.getTextToSearch());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new EndsWith("abc", null).isApplicable());
		assertFalse(new EndsWith(null, "abc").isApplicable());
		assertFalse(new EndsWith(null, null).isApplicable());
		assertTrue(endsWith("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final EndsWith endsWith = endsWith("abc", "def");
		assertEquals("endsWith('abc','def')", endsWith.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(endsWith("abc", "def"), endsWith("abc", "def"));
		assertEquals(endsWith("abc", "def"), endsWith("abc", "def"));
		assertNotEquals(endsWith("abc", "def"), endsWith("abc", "defe"));
		assertNotEquals(endsWith("abc", "def"), new EndsWith("abc", null));
		assertNotEquals(endsWith("abc", "def"), new EndsWith(null, "abc"));
		assertEquals(endsWith("abc", "def").hashCode(), endsWith("abc", "def").hashCode());
		assertNotEquals(endsWith("abc", "def").hashCode(), endsWith("abc", "defe").hashCode());
		assertNotEquals(endsWith("abc", "def").hashCode(), new EndsWith("abc", null).hashCode());
		assertNotEquals(endsWith("abc", "def").hashCode(), new EndsWith(null, "abc").hashCode());
	}

}