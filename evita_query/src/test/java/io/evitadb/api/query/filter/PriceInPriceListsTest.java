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

import java.io.Serializable;

import static io.evitadb.api.query.QueryConstraints.priceInPriceLists;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link PriceInPriceLists} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class PriceInPriceListsTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final PriceInPriceLists priceInPriceLists = priceInPriceLists("basic", "reference");
		assertArrayEquals(new Serializable[] {"basic", "reference"}, priceInPriceLists.getPriceLists());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new PriceInPriceLists().isApplicable());
		assertTrue(priceInPriceLists(1).isApplicable());
		assertTrue(priceInPriceLists(1, 2).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final PriceInPriceLists priceInPriceLists = priceInPriceLists("basic", "reference");
		assertEquals("priceInPriceLists('basic','reference')", priceInPriceLists.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(priceInPriceLists("basic", "reference"), priceInPriceLists("basic", "reference"));
		assertEquals(priceInPriceLists("basic", "reference"), priceInPriceLists("basic", "reference"));
		assertNotEquals(priceInPriceLists("basic", "reference"), priceInPriceLists("basic", "action"));
		assertNotEquals(priceInPriceLists("basic", "reference"), priceInPriceLists("basic"));
		assertEquals(priceInPriceLists("basic", "reference").hashCode(), priceInPriceLists("basic", "reference").hashCode());
		assertNotEquals(priceInPriceLists("basic", "reference").hashCode(), priceInPriceLists("basic", "action").hashCode());
		assertNotEquals(priceInPriceLists("basic", "reference").hashCode(), priceInPriceLists("basic").hashCode());
	}

}