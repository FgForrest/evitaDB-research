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

import static io.evitadb.api.query.QueryConstraints.startsWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link StartsWith} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class StartsWithTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final StartsWith startsWith = startsWith("abc", "def");
		assertEquals("abc", startsWith.getAttributeName());
		assertEquals("def", startsWith.getTextToSearch());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new StartsWith("abc", null).isApplicable());
		assertFalse(new StartsWith(null, "abc").isApplicable());
		assertFalse(new StartsWith(null, null).isApplicable());
		assertTrue(startsWith("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final StartsWith startsWith = startsWith("abc", "def");
		assertEquals("startsWith('abc','def')", startsWith.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(startsWith("abc", "def"), startsWith("abc", "def"));
		assertEquals(startsWith("abc", "def"), startsWith("abc", "def"));
		assertNotEquals(startsWith("abc", "def"), startsWith("abc", "defe"));
		assertNotEquals(startsWith("abc", "def"), new StartsWith("abc", null));
		assertNotEquals(startsWith("abc", "def"), new StartsWith(null, "abc"));
		assertEquals(startsWith("abc", "def").hashCode(), startsWith("abc", "def").hashCode());
		assertNotEquals(startsWith("abc", "def").hashCode(), startsWith("abc", "defe").hashCode());
		assertNotEquals(startsWith("abc", "def").hashCode(), new StartsWith("abc", null).hashCode());
		assertNotEquals(startsWith("abc", "def").hashCode(), new StartsWith(null, "abc").hashCode());
	}

}