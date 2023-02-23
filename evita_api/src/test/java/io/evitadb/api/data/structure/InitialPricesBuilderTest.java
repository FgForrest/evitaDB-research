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

package io.evitadb.api.data.structure;

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test verifies contract of {@link InitialEntityBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InitialPricesBuilderTest {
	public static final Currency CZK = Currency.getInstance("CZK");
	public static final Currency EUR = Currency.getInstance("EUR");
	private final InitialPricesBuilder builder = new InitialPricesBuilder();

	@Test
	void shouldCreateEntityWithPrices() {
		final Prices prices = builder.setPriceInnerRecordHandling(PriceInnerRecordHandling.FIRST_OCCURRENCE)
				.setPrice(1, "basic", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
				.setPrice(2, "reference", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
				.setPrice(3, "basic", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
				.setPrice(4, "reference", EUR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false)
				.build();
		assertEquals(PriceInnerRecordHandling.FIRST_OCCURRENCE, prices.getPriceInnerRecordHandling());
		assertEquals(4, prices.getPrices().size());
		assertPrice(prices.getPrice(1, "basic", CZK), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true);
		assertPrice(prices.getPrice(2, "reference", CZK), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false);
		assertPrice(prices.getPrice(3, "basic", EUR), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true);
		assertPrice(prices.getPrice(4, "reference", EUR), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, false);
	}

	@Test
	void shouldOverwriteIdenticalPrice() {
		final Prices prices = builder
				.setPrice(1, "basic", CZK, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, true)
				.setPrice(1, "basic", CZK, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true)
				.build();

		assertEquals(1, prices.getPrices().size());
		assertPrice(prices.getPrice(1, "basic", CZK), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, true);
	}

	public static void assertPrice(PriceContract price, BigDecimal priceWithoutVat, BigDecimal vat, BigDecimal priceWithVat, boolean indexed) {
		assertNotNull(price);
		assertEquals(priceWithoutVat, price.getPriceWithoutVat());
		assertEquals(vat, price.getVat());
		assertEquals(priceWithVat, price.getPriceWithVat());
		assertEquals(indexed, price.isSellable());
	}
}