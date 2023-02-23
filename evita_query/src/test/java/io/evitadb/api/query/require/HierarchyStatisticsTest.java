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

import static io.evitadb.api.query.QueryConstraints.hierarchyStatistics;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link HierarchyStatistics} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class HierarchyStatisticsTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final HierarchyStatistics hierarchyStatistics = hierarchyStatistics("brand");
		assertEquals("brand", hierarchyStatistics.getEntityType());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(hierarchyStatistics(null).isApplicable());
		assertTrue(hierarchyStatistics("brand").isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final HierarchyStatistics hierarchyStatistics = hierarchyStatistics("brand");
		assertEquals("hierarchyStatistics('brand')", hierarchyStatistics.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(hierarchyStatistics("brand"), hierarchyStatistics("brand"));
		assertEquals(hierarchyStatistics("brand"), hierarchyStatistics("brand"));
		assertNotEquals(hierarchyStatistics("brand"), hierarchyStatistics("category"));
		assertEquals(hierarchyStatistics("brand").hashCode(), hierarchyStatistics("brand").hashCode());
		assertNotEquals(hierarchyStatistics("brand").hashCode(), hierarchyStatistics("category").hashCode());
	}

}
