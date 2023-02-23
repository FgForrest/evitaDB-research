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

import static io.evitadb.api.query.QueryConstraints.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link GreaterThan} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class GreaterThanTest {

	@Test
	void shouldFailToUseInvalidDataType() {
		assertThrows(UnsupportedDataTypeException.class, () -> greaterThan("abc", new MockObject()));
	}

	@Test
	void shouldAutomaticallyConvertDataType() {
		assertEquals(new BigDecimal("1.0"), greaterThan("abc", 1f).getAttributeValue());
	}

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final GreaterThan greaterThan = greaterThan("abc", "def");
		assertEquals("abc", greaterThan.getAttributeName());
		assertEquals("def", greaterThan.getAttributeValue());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new GreaterThan("abc", null).isApplicable());
		assertFalse(new GreaterThan(null, "abc").isApplicable());
		assertFalse(new GreaterThan(null, null).isApplicable());
		assertTrue(greaterThan("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final GreaterThan greaterThan = greaterThan("abc", "def");
		assertEquals("greaterThan('abc','def')", greaterThan.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(greaterThan("abc", "def"), greaterThan("abc", "def"));
		assertEquals(greaterThan("abc", "def"), greaterThan("abc", "def"));
		assertNotEquals(greaterThan("abc", "def"), greaterThan("abc", "defe"));
		assertNotEquals(greaterThan("abc", "def"), new GreaterThan("abc", null));
		assertNotEquals(greaterThan("abc", "def"), new GreaterThan(null, "abc"));
		assertEquals(greaterThan("abc", "def").hashCode(), greaterThan("abc", "def").hashCode());
		assertNotEquals(greaterThan("abc", "def").hashCode(), greaterThan("abc", "defe").hashCode());
		assertNotEquals(greaterThan("abc", "def").hashCode(), new GreaterThan("abc", null).hashCode());
		assertNotEquals(greaterThan("abc", "def").hashCode(), new GreaterThan(null, "abc").hashCode());
	}

}