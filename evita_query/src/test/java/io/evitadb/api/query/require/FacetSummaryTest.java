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

import static io.evitadb.api.query.QueryConstraints.facetSummary;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link FacetSummary} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class FacetSummaryTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final FacetSummary facetSummary = facetSummary();
		assertNotNull(facetSummary);
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(facetSummary().isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final FacetSummary facetSummary = facetSummary();
		assertEquals(FacetStatisticsDepth.COUNTS, facetSummary.getFacetStatisticsDepth());
		assertEquals("facetSummary(COUNTS)", facetSummary.toString());
	}

	@Test
	void shouldToStringReturnExpectedFormatUsingArgument() {
		final FacetSummary facetSummary = facetSummary(FacetStatisticsDepth.IMPACT);
		assertEquals(FacetStatisticsDepth.IMPACT, facetSummary.getFacetStatisticsDepth());
		assertEquals("facetSummary(IMPACT)", facetSummary.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(facetSummary(), facetSummary());
		assertEquals(facetSummary(), facetSummary());
		assertNotEquals(facetSummary(), facetSummary(FacetStatisticsDepth.IMPACT));
		assertEquals(facetSummary().hashCode(), facetSummary().hashCode());
	}

}