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

import io.evitadb.api.exception.UnsupportedDataTypeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.evitadb.api.query.QueryConstraints.eq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Equals} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EqualsTest {

	@Test
	void shouldFailToUseInvalidDataType() {
		assertThrows(UnsupportedDataTypeException.class, () -> eq("abc", new MockObject()));
	}

	@Test
	void shouldAutomaticallyConvertDataType() {
		assertEquals(new BigDecimal("1.0"), eq("abc", 1f).getAttributeValue());
	}

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Equals eq = eq("abc", "def");
		assertEquals("abc", eq.getAttributeName());
		assertEquals("def", eq.getAttributeValue());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new Equals("abc", null).isApplicable());
		assertFalse(new Equals(null, "abc").isApplicable());
		assertFalse(new Equals(null, null).isApplicable());
		assertTrue(eq("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Equals eq = eq("abc", "def");
		assertEquals("equals('abc','def')", eq.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(eq("abc", "def"), eq("abc", "def"));
		assertEquals(eq("abc", "def"), eq("abc", "def"));
		assertNotEquals(eq("abc", "def"), eq("abc", "defe"));
		assertNotEquals(eq("abc", "def"), new Equals("abc", null));
		assertNotEquals(eq("abc", "def"), new Equals(null, "abc"));
		assertEquals(eq("abc", "def").hashCode(), eq("abc", "def").hashCode());
		assertNotEquals(eq("abc", "def").hashCode(), eq("abc", "defe").hashCode());
		assertNotEquals(eq("abc", "def").hashCode(), new Equals("abc", null).hashCode());
		assertNotEquals(eq("abc", "def").hashCode(), new Equals(null, "abc").hashCode());
	}

}