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

import static io.evitadb.api.query.QueryConstraints.between;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Between} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class BetweenTest {

	@Test
	void shouldFailToUseInvalidDataType() {
		assertThrows(UnsupportedDataTypeException.class, () -> between("abc", new MockObject(), new MockObject()));
	}

	@Test
	void shouldAutomaticallyConvertDataType() {
		assertEquals(new BigDecimal("1.0"), between("abc", 1f, 2f).getFrom());
		assertEquals(new BigDecimal("2.0"), between("abc", 1f, 2f).getTo());
	}

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Between between = between("abc", 1, 2);
		assertEquals("abc", between.getAttributeName());
		assertEquals(Integer.valueOf(1), between.getFrom());
		assertEquals(Integer.valueOf(2), between.getTo());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(between("abc", null, null).isApplicable());
		assertFalse(between(null, "abc", "abc").isApplicable());
		assertFalse(between(null, null, null).isApplicable());
		assertTrue(between("abc", "def", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Between between = between("abc", "def", "def");
		assertEquals("between('abc','def','def')", between.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(between("abc", "def", "def"), between("abc", "def", "def"));
		assertEquals(between("abc", "def", "def"), between("abc", "def", "def"));
		assertNotEquals(between("abc", "def", "def"), between("abc", "defe", "defe"));
		assertNotEquals(between("abc", "def", "def"), between("abc", null, null));
		assertNotEquals(between("abc", "def", "def"), between(null, "abc", "abc"));
		assertEquals(between("abc", "def", "def").hashCode(), between("abc", "def", "def").hashCode());
		assertNotEquals(between("abc", "def", "def").hashCode(), between("abc", "defe", "defe").hashCode());
		assertNotEquals(between("abc", "def", "def").hashCode(), between("abc", null, null).hashCode());
		assertNotEquals(between("abc", "def", "def").hashCode(), between(null, "abc", "abc").hashCode());
	}

}