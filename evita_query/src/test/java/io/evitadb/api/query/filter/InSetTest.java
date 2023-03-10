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

import static io.evitadb.api.query.QueryConstraints.inSet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link InSet} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InSetTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final InSet inSet = inSet("refs", 1, 5);
		assertArrayEquals(new Comparable<?>[] {1, 5}, inSet.getSet());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new InSet(null).isApplicable());
		assertFalse(new InSet("refs").isApplicable());
		assertTrue(inSet("refs", 1).isApplicable());
		assertTrue(inSet("refs", 1, 2).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final InSet inSet = inSet("refs", 1, 5);
		assertEquals("inSet('refs',1,5)", inSet.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(inSet("refs", 1, 5), inSet("refs", 1, 5));
		assertEquals(inSet("refs", 1, 5), inSet("refs", 1, 5));
		assertNotEquals(inSet("refs", 1, 5), inSet("refs", 1, 6));
		assertNotEquals(inSet("refs", 1, 5), inSet("refs", 1));
		assertNotEquals(inSet("refs", 1, 5), inSet("def", 1, 5));
		assertEquals(inSet("refs", 1, 5).hashCode(), inSet("refs", 1, 5).hashCode());
		assertNotEquals(inSet("refs", 1, 5).hashCode(), inSet("refs", 1, 6).hashCode());
		assertNotEquals(inSet("refs", 1, 5).hashCode(), inSet("refs", 1).hashCode());
	}

}