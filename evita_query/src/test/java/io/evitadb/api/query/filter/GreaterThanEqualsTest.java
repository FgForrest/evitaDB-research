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

import static io.evitadb.api.query.QueryConstraints.greaterThanEquals;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link GreaterThanEquals} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class GreaterThanEqualsTest {

	@Test
	void shouldFailToUseInvalidDataType() {
		assertThrows(UnsupportedDataTypeException.class, () -> greaterThanEquals("abc", new MockObject()));
	}

	@Test
	void shouldAutomaticallyConvertDataType() {
		assertEquals(new BigDecimal("1.0"), greaterThanEquals("abc", 1f).getAttributeValue());
	}

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final GreaterThanEquals greaterThanEquals = greaterThanEquals("abc", "def");
		assertEquals("abc", greaterThanEquals.getAttributeName());
		assertEquals("def", greaterThanEquals.getAttributeValue());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new GreaterThanEquals("abc", null).isApplicable());
		assertFalse(new GreaterThanEquals(null, "abc").isApplicable());
		assertFalse(new GreaterThanEquals(null, null).isApplicable());
		assertTrue(greaterThanEquals("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final GreaterThanEquals greaterThanEquals = greaterThanEquals("abc", "def");
		assertEquals("greaterThanEquals('abc','def')", greaterThanEquals.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(greaterThanEquals("abc", "def"), greaterThanEquals("abc", "def"));
		assertEquals(greaterThanEquals("abc", "def"), greaterThanEquals("abc", "def"));
		assertNotEquals(greaterThanEquals("abc", "def"), greaterThanEquals("abc", "defe"));
		assertNotEquals(greaterThanEquals("abc", "def"), new GreaterThanEquals("abc", null));
		assertNotEquals(greaterThanEquals("abc", "def"), new GreaterThanEquals(null, "abc"));
		assertEquals(greaterThanEquals("abc", "def").hashCode(), greaterThanEquals("abc", "def").hashCode());
		assertNotEquals(greaterThanEquals("abc", "def").hashCode(), greaterThanEquals("abc", "defe").hashCode());
		assertNotEquals(greaterThanEquals("abc", "def").hashCode(), new GreaterThanEquals("abc", null).hashCode());
		assertNotEquals(greaterThanEquals("abc", "def").hashCode(), new GreaterThanEquals(null, "abc").hashCode());
	}

}