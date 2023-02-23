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

import static io.evitadb.api.query.QueryConstraints.lessThanEquals;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link LessThanEquals} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class LessThanEqualsTest {

	@Test
	void shouldFailToUseInvalidDataType() {
		assertThrows(UnsupportedDataTypeException.class, () -> lessThanEquals("abc", new MockObject()));
	}

	@Test
	void shouldAutomaticallyConvertDataType() {
		assertEquals(new BigDecimal("1.0"), lessThanEquals("abc", 1f).getAttributeValue());
	}

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final LessThanEquals lessThanEquals = lessThanEquals("abc", "def");
		assertEquals("abc", lessThanEquals.getAttributeName());
		assertEquals("def", lessThanEquals.getAttributeValue());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new LessThanEquals("abc", null).isApplicable());
		assertFalse(new LessThanEquals(null, "abc").isApplicable());
		assertFalse(new LessThanEquals(null, null).isApplicable());
		assertTrue(lessThanEquals("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final LessThanEquals lessThanEquals = lessThanEquals("abc", "def");
		assertEquals("lessThanEquals('abc','def')", lessThanEquals.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(lessThanEquals("abc", "def"), lessThanEquals("abc", "def"));
		assertEquals(lessThanEquals("abc", "def"), lessThanEquals("abc", "def"));
		assertNotEquals(lessThanEquals("abc", "def"), lessThanEquals("abc", "defe"));
		assertNotEquals(lessThanEquals("abc", "def"), new LessThanEquals("abc", null));
		assertNotEquals(lessThanEquals("abc", "def"), new LessThanEquals(null, "abc"));
		assertEquals(lessThanEquals("abc", "def").hashCode(), lessThanEquals("abc", "def").hashCode());
		assertNotEquals(lessThanEquals("abc", "def").hashCode(), lessThanEquals("abc", "defe").hashCode());
		assertNotEquals(lessThanEquals("abc", "def").hashCode(), new LessThanEquals("abc", null).hashCode());
		assertNotEquals(lessThanEquals("abc", "def").hashCode(), new LessThanEquals(null, "abc").hashCode());
	}

}