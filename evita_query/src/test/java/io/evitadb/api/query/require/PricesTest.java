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

import static io.evitadb.api.query.QueryConstraints.allPrices;
import static io.evitadb.api.query.QueryConstraints.prices;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Prices} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class PricesTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		assertEquals(PriceFetchMode.RESPECTING_FILTER, prices().getFetchMode());
		assertEquals(PriceFetchMode.ALL, allPrices().getFetchMode());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(prices().isApplicable());
		assertTrue(allPrices().isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		assertEquals("prices(RESPECTING_FILTER)", prices().toString());
		assertEquals("prices(ALL)", allPrices().toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(prices(), prices());
		assertEquals(prices(), prices());
		assertNotEquals(prices(), allPrices());
		assertEquals(prices().hashCode(), prices().hashCode());
		assertNotEquals(prices().hashCode(), allPrices().hashCode());
	}

	@Test
	void shouldCombineWithAnotherConstraint() {
		assertEquals(prices(), prices().combineWith(prices()));
		assertEquals(allPrices(), prices().combineWith(allPrices()));
		assertEquals(allPrices(), allPrices().combineWith(prices()));
	}

}