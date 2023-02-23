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
 * This tests verifies basic properties of {@link WithinHierarchy} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class WithinHierarchyTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final WithinHierarchy withinHierarchy = withinHierarchy("brand", 1);
		assertEquals("brand", withinHierarchy.getEntityType());
		assertEquals(1, withinHierarchy.getParentId());
		assertArrayEquals(new int[0], withinHierarchy.getExcludedChildrenIds());
		assertFalse(withinHierarchy.isDirectRelation());
		assertFalse(withinHierarchy.isExcludingRoot());
	}

	@Test
	void shouldCreateViaFactoryUsingExcludingSubConstraintClassWorkAsExpected() {
		final WithinHierarchy withinHierarchy = withinHierarchy("brand", 1, excluding(5, 7));
		assertEquals("brand", withinHierarchy.getEntityType());
		assertEquals(1, withinHierarchy.getParentId());
		assertArrayEquals(new int[] {5, 7}, withinHierarchy.getExcludedChildrenIds());
		assertFalse(withinHierarchy.isDirectRelation());
		assertFalse(withinHierarchy.isExcludingRoot());
	}

	@Test
	void shouldCreateViaFactoryUsingExcludingRootSubConstraintClassWorkAsExpected() {
		final WithinHierarchy withinHierarchy = withinHierarchy("brand", 1, excludingRoot());
		assertEquals("brand", withinHierarchy.getEntityType());
		assertEquals(1, withinHierarchy.getParentId());
		assertArrayEquals(new int[0], withinHierarchy.getExcludedChildrenIds());
		assertFalse(withinHierarchy.isDirectRelation());
		assertTrue(withinHierarchy.isExcludingRoot());
	}

	@Test
	void shouldCreateViaFactoryWithoutEntityTypeClassWorkAsExpected() {
		final WithinHierarchy withinHierarchy = withinHierarchy(1, excludingRoot());
		assertNull(withinHierarchy.getEntityType());
		assertEquals(1, withinHierarchy.getParentId());
		assertArrayEquals(new int[0], withinHierarchy.getExcludedChildrenIds());
		assertFalse(withinHierarchy.isDirectRelation());
		assertTrue(withinHierarchy.isExcludingRoot());
	}

	@Test
	void shouldCreateViaFactoryUsingDirectRelationSubConstraintClassWorkAsExpected() {
		final WithinHierarchy withinHierarchy = withinHierarchy("brand", 1, directRelation());
		assertEquals("brand", withinHierarchy.getEntityType());
		assertEquals(1, withinHierarchy.getParentId());
		assertArrayEquals(new int[0], withinHierarchy.getExcludedChildrenIds());
		assertTrue(withinHierarchy.isDirectRelation());
		assertFalse(withinHierarchy.isExcludingRoot());
	}

	@Test
	void shouldCreateViaFactoryUsingAllPossibleSubConstraintClassWorkAsExpected() {
		final WithinHierarchy withinHierarchy = withinHierarchy(
			"brand", 1,
			excludingRoot(),
			excluding(3, 7)
		);
		assertEquals("brand", withinHierarchy.getEntityType());
		assertEquals(1, withinHierarchy.getParentId());
		assertArrayEquals(new int[] {3, 7}, withinHierarchy.getExcludedChildrenIds());
		assertFalse(withinHierarchy.isDirectRelation());
		assertTrue(withinHierarchy.isExcludingRoot());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new WithinHierarchy(null, (Integer) null).isApplicable());
		assertFalse(new WithinHierarchy("brand", null).isApplicable());
		assertTrue(withinHierarchy("brand", 1).isApplicable());
		assertTrue(withinHierarchy("brand", 1, excluding(5, 7)).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final WithinHierarchy withinHierarchy = withinHierarchy("brand", 1, excluding(5, 7));
		assertEquals("withinHierarchy('brand',1,excluding(5,7))", withinHierarchy.toString());
	}

	@Test
	void shouldToStringReturnExpectedFormatWhenUsingMultipleSubConstraints() {
		final WithinHierarchy withinHierarchy = withinHierarchy(
			"brand", 1,
			excludingRoot(),
			excluding(3, 7)
		);;
		assertEquals("withinHierarchy('brand',1,excludingRoot(),excluding(3,7))", withinHierarchy.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("brand", 1,excluding(1, 5)));
		assertEquals(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("brand", 1,excluding(1, 5)));
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("brand", 1,excluding(1, 6)));
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("brand", 1, excluding(1)));
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("brand", 2,excluding(1, 5)));
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("category", 1,excluding(1, 6)));
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)), withinHierarchy("brand", 1, excluding(1)));
		assertEquals(withinHierarchy("brand", 1,excluding(1, 5)).hashCode(), withinHierarchy("brand", 1,excluding(1, 5)).hashCode());
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)).hashCode(), withinHierarchy("brand", 1,excluding(1, 6)).hashCode());
		assertNotEquals(withinHierarchy("brand", 1,excluding(1, 5)).hashCode(), withinHierarchy("brand", 1, excluding(1)).hashCode());
	}

}
