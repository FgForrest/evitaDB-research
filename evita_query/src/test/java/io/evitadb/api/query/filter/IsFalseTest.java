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

import static io.evitadb.api.query.QueryConstraints.isFalse;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link IsFalse} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class IsFalseTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final IsFalse isFalse = isFalse("married");
		assertEquals("married", isFalse.getAttributeName());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(isFalse("married").isApplicable());
		assertFalse(isFalse(null).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final IsFalse isFalse = isFalse("married");
		assertEquals("isFalse('married')", isFalse.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(isFalse("married"), isFalse("married"));
		assertEquals(isFalse("married"), isFalse("married"));
		assertNotEquals(isFalse("married"), isFalse("single"));
		assertNotEquals(isFalse("married"), isFalse(null));
		assertEquals(isFalse("married").hashCode(), isFalse("married").hashCode());
		assertNotEquals(isFalse("married").hashCode(), isFalse("single").hashCode());
		assertNotEquals(isFalse("married").hashCode(), isFalse(null).hashCode());
	}

}