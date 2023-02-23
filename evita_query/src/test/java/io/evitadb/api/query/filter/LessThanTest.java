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

import static io.evitadb.api.query.QueryConstraints.lessThan;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link LessThan} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class LessThanTest {

	@Test
	void shouldFailToUseInvalidDataType() {
		assertThrows(UnsupportedDataTypeException.class, () -> lessThan("abc", new MockObject()));
	}

	@Test
	void shouldAutomaticallyConvertDataType() {
		assertEquals(new BigDecimal("1.0"), lessThan("abc", 1f).getAttributeValue());
	}

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final LessThan lessThan = lessThan("abc", "def");
		assertEquals("abc", lessThan.getAttributeName());
		assertEquals("def", lessThan.getAttributeValue());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new LessThan("abc", null).isApplicable());
		assertFalse(new LessThan(null, "abc").isApplicable());
		assertFalse(new LessThan(null, null).isApplicable());
		assertTrue(lessThan("abc", "def").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final LessThan lessThan = lessThan("abc", "def");
		assertEquals("lessThan('abc','def')", lessThan.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(lessThan("abc", "def"), lessThan("abc", "def"));
		assertEquals(lessThan("abc", "def"), lessThan("abc", "def"));
		assertNotEquals(lessThan("abc", "def"), lessThan("abc", "defe"));
		assertNotEquals(lessThan("abc", "def"), new LessThan("abc", null));
		assertNotEquals(lessThan("abc", "def"), new LessThan(null, "abc"));
		assertEquals(lessThan("abc", "def").hashCode(), lessThan("abc", "def").hashCode());
		assertNotEquals(lessThan("abc", "def").hashCode(), lessThan("abc", "defe").hashCode());
		assertNotEquals(lessThan("abc", "def").hashCode(), new LessThan("abc", null).hashCode());
		assertNotEquals(lessThan("abc", "def").hashCode(), new LessThan(null, "abc").hashCode());
	}

}