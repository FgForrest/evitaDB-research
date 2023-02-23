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

package io.evitadb.api.io.extraResult;

import io.evitadb.api.io.extraResult.FacetSummary.FacetGroupStatistics;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.io.extraResult.FacetSummary.RequestImpact;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * This test verifies {@link FacetSummaryTest} contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class FacetSummaryTest {

	@Test
	void shouldBeEqual() {
		final FacetSummary one = createFacetSummary();
		final FacetSummary two = createFacetSummary();

		assertNotSame(one, two);
		assertEquals(one, two);
	}

	@Nonnull
	private FacetSummary createFacetSummary() {
		return new FacetSummary(
			Arrays.asList(
				new FacetGroupStatistics(
					"parameter", 1,
					Arrays.asList(
						new FacetStatistics(1, true, 5, null),
						new FacetStatistics(2, false, 6, new RequestImpact(6, 11)),
						new FacetStatistics(3, false, 3, new RequestImpact(3, 8))
					)
				),
				new FacetGroupStatistics(
					"parameter", 2,
					Arrays.asList(
						new FacetStatistics(4, true, 5, null),
						new FacetStatistics(5, false, 6, new RequestImpact(6, 11)),
						new FacetStatistics(6, false, 3, new RequestImpact(3, 8))
					)
				)
			)
		);
	}

}