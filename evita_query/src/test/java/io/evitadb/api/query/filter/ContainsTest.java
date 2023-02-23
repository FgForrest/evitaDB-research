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

import static io.evitadb.api.query.QueryConstraints.contains;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Contains} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ContainsTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Contains contains = contains("abc", "def");
		assertEquals("abc", contains.getAttributeName());
		assertEquals("def", contains.getTextToSearch());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new Contains("abc", null).isApplicable());
		assertFalse(new Contains(null, "abc").isApplicable());
		assertFalse(new Contains(null, null).isApplicable());
		assertTrue(contains("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Contains contains = contains("abc", "def");
		assertEquals("contains('abc','def')", contains.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(contains("abc", "def"), contains("abc", "def"));
		assertEquals(contains("abc", "def"), contains("abc", "def"));
		assertNotEquals(contains("abc", "def"), contains("abc", "defe"));
		assertNotEquals(contains("abc", "def"), new Contains("abc", null));
		assertNotEquals(contains("abc", "def"), new Contains(null, "abc"));
		assertEquals(contains("abc", "def").hashCode(), contains("abc", "def").hashCode());
		assertNotEquals(contains("abc", "def").hashCode(), contains("abc", "defe").hashCode());
		assertNotEquals(contains("abc", "def").hashCode(), new Contains("abc", null).hashCode());
		assertNotEquals(contains("abc", "def").hashCode(), new Contains(null, "abc").hashCode());
	}

}