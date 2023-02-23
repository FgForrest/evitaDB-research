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

package io.evitadb.api.query.require;

import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.QueryConstraints.references;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link References} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 202"a"
 */
class ReferencesTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final References references = references("a", "b");
		assertArrayEquals(new String[] {"a", "b"}, references.getReferencedEntityType());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(references().isApplicable());
		assertTrue(references("a").isApplicable());
		assertTrue(references("a", "c").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final References references = references("a", "b");
		assertEquals("references('a','b')", references.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(references("a", "b"), references("a", "b"));
		assertEquals(references("a", "b"), references("a", "b"));
		assertNotEquals(references("a", "b"), references("a", "e"));
		assertNotEquals(references("a", "b"), references("a"));
		assertEquals(references("a", "b").hashCode(), references("a", "b").hashCode());
		assertNotEquals(references("a", "b").hashCode(), references("a", "e").hashCode());
		assertNotEquals(references("a", "b").hashCode(), references("a").hashCode());
	}

	@Test
	void shouldCombineWithAnotherConstraint() {
		assertEquals(references(), references("A").combineWith(references()));
		assertEquals(references(), references().combineWith(references("A")));
		assertEquals(references("A", "B"), references("A").combineWith(references("B")));
	}

}