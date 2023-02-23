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

import static io.evitadb.api.query.QueryConstraints.isNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link IsNull} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class IsNullTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final IsNull isNull = isNull("married");
		assertEquals("married", isNull.getAttributeName());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(isNull("married").isApplicable());
		assertFalse(isNull(null).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final IsNull isNull = isNull("married");
		assertEquals("isNull('married')", isNull.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(isNull("married"), isNull("married"));
		assertEquals(isNull("married"), isNull("married"));
		assertNotEquals(isNull("married"), isNull("single"));
		assertNotEquals(isNull("married"), isNull(null));
		assertEquals(isNull("married").hashCode(), isNull("married").hashCode());
		assertNotEquals(isNull("married").hashCode(), isNull("single").hashCode());
		assertNotEquals(isNull("married").hashCode(), isNull(null).hashCode());
	}

}