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

import static io.evitadb.api.query.QueryConstraints.facet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Facet} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class FacetTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Facet facet = facet("brand", 1, 5, 7);
		assertEquals("brand", facet.getEntityType());
		assertArrayEquals(new int[] {1, 5, 7}, facet.getFacetIds());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new Facet(null).isApplicable());
		assertFalse(new Facet("brand").isApplicable());
		assertTrue(facet("brand", 1).isApplicable());
		assertTrue(facet("brand", 1, 5, 7).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Facet facet = facet("brand", 1, 5, 7);
		assertEquals("facet('brand',1,5,7)", facet.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(facet("brand", 1, 1, 5), facet("brand", 1, 1, 5));
		assertEquals(facet("brand", 1, 1, 5), facet("brand", 1, 1, 5));
		assertNotEquals(facet("brand", 1, 1, 5), facet("brand", 1, 1, 6));
		assertNotEquals(facet("brand", 1, 1, 5), facet("brand", 1, 1));
		assertNotEquals(facet("brand", 1, 1, 5), facet("brand", 2, 1, 5));
		assertNotEquals(facet("brand", 1, 1, 5), facet("category", 1, 1, 6));
		assertNotEquals(facet("brand", 1, 1, 5), facet("brand", 1, 1));
		assertEquals(facet("brand", 1, 1, 5).hashCode(), facet("brand", 1, 1, 5).hashCode());
		assertNotEquals(facet("brand", 1, 1, 5).hashCode(), facet("brand", 1, 1, 6).hashCode());
		assertNotEquals(facet("brand", 1, 1, 5).hashCode(), facet("brand", 1, 1).hashCode());
	}

}