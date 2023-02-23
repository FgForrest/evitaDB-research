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

package io.evitadb.api.query.order;

import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.QueryConstraints.ascending;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Ascending} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class AscendingTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Ascending ascending = ascending("married");
		assertEquals("married", ascending.getAttributeName());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(ascending("married").isApplicable());
		assertFalse(ascending(null).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Ascending ascending = ascending("married");
		assertEquals("ascending('married')", ascending.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(ascending("married"), ascending("married"));
		assertEquals(ascending("married"), ascending("married"));
		assertNotEquals(ascending("married"), ascending("single"));
		assertNotEquals(ascending("married"), ascending(null));
		assertEquals(ascending("married").hashCode(), ascending("married").hashCode());
		assertNotEquals(ascending("married").hashCode(), ascending("single").hashCode());
		assertNotEquals(ascending("married").hashCode(), ascending(null).hashCode());
	}
	
}