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

import static io.evitadb.api.query.QueryConstraints.isTrue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link IsTrue} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class IsTrueTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final IsTrue isTrue = isTrue("married");
		assertEquals("married", isTrue.getAttributeName());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(isTrue("married").isApplicable());
		assertFalse(isTrue(null).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final IsTrue isTrue = isTrue("married");
		assertEquals("isTrue('married')", isTrue.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(isTrue("married"), isTrue("married"));
		assertEquals(isTrue("married"), isTrue("married"));
		assertNotEquals(isTrue("married"), isTrue("single"));
		assertNotEquals(isTrue("married"), isTrue(null));
		assertEquals(isTrue("married").hashCode(), isTrue("married").hashCode());
		assertNotEquals(isTrue("married").hashCode(), isTrue("single").hashCode());
		assertNotEquals(isTrue("married").hashCode(), isTrue(null).hashCode());
	}

}