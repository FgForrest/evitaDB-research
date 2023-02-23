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

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link WithinRootHierarchy} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class WithinRootHierarchyTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final WithinRootHierarchy withinRootHierarchy = withinRootHierarchy("brand");
		assertEquals("brand", withinRootHierarchy.getEntityType());
		assertArrayEquals(new int[0], withinRootHierarchy.getExcludedChildrenIds());
		assertFalse(withinRootHierarchy.isDirectRelation());
	}

	@Test
	void shouldCreateViaFactoryUsingExcludingSubConstraintClassWorkAsExpected() {
		final WithinRootHierarchy withinRootHierarchy = withinRootHierarchy("brand", excluding(5, 7));
		assertEquals("brand", withinRootHierarchy.getEntityType());
		assertArrayEquals(new int[] {5, 7}, withinRootHierarchy.getExcludedChildrenIds());
		assertFalse(withinRootHierarchy.isDirectRelation());
	}

	@Test
	void shouldCreateViaFactoryUsingDirectRelationSubConstraintClassWorkAsExpected() {
		final WithinRootHierarchy withinRootHierarchy = withinRootHierarchy(directRelation());
		assertNull(withinRootHierarchy.getEntityType());
		assertArrayEquals(new int[0], withinRootHierarchy.getExcludedChildrenIds());
		assertTrue(withinRootHierarchy.isDirectRelation());
	}

	@Test
	void shouldFailToCreateViaFactoryUsingDirectOnSelfReferencingConstraint() {
		assertThrows(IllegalArgumentException.class, () -> withinRootHierarchy("brand", directRelation()));
	}

	@Test
	void shouldCreateViaFactoryUsingAllPossibleSubConstraintClassWorkAsExpected() {
		final WithinRootHierarchy withinRootHierarchy = withinRootHierarchy(
			excluding(3, 7)
		);
		assertNull(withinRootHierarchy.getEntityType());
		assertArrayEquals(new int[] {3, 7}, withinRootHierarchy.getExcludedChildrenIds());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(withinRootHierarchy().isApplicable());
		assertTrue(withinRootHierarchy("brand").isApplicable());
		assertTrue(withinRootHierarchy("brand").isApplicable());
		assertTrue(withinRootHierarchy("brand", excluding(5, 7)).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final WithinRootHierarchy withinRootHierarchy = withinRootHierarchy("brand", excluding(5, 7));
		assertEquals("withinRootHierarchy('brand',excluding(5,7))", withinRootHierarchy.toString());
	}

	@Test
	void shouldToStringReturnExpectedFormatWhenUsingMultipleSubConstraints() {
		final WithinRootHierarchy withinRootHierarchy = withinRootHierarchy(
			"brand",
			excluding(3, 7)
		);;
		assertEquals("withinRootHierarchy('brand',excluding(3,7))", withinRootHierarchy.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(withinRootHierarchy("brand", excluding(1, 5)), withinRootHierarchy("brand", excluding(1, 5)));
		assertEquals(withinRootHierarchy("brand", excluding(1, 5)), withinRootHierarchy("brand", excluding(1, 5)));
		assertNotEquals(withinRootHierarchy("brand", excluding(1, 5)), withinRootHierarchy("brand", excluding(1, 6)));
		assertNotEquals(withinRootHierarchy("brand", excluding(1, 5)), withinRootHierarchy("brand", excluding(1)));
		assertNotEquals(withinRootHierarchy("brand", excluding(1, 5)), withinRootHierarchy("category", excluding(1, 6)));
		assertNotEquals(withinRootHierarchy("brand", excluding(1, 5)), withinRootHierarchy("brand", excluding(1)));
		assertEquals(withinRootHierarchy("brand", excluding(1, 5)).hashCode(), withinRootHierarchy("brand", excluding(1, 5)).hashCode());
		assertNotEquals(withinRootHierarchy("brand", excluding(1, 5)).hashCode(), withinRootHierarchy("brand", excluding(1, 6)).hashCode());
		assertNotEquals(withinRootHierarchy("brand", excluding(1, 5)).hashCode(), withinRootHierarchy("brand", excluding(1)).hashCode());
	}

}
