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

package io.evitadb.api.io.predicate;

import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.require.PriceFetchMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * This test verifies behaviour of {@link PriceContractSerializablePredicate}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
class PriceContractSerializablePredicateTest {

	@Test
	void shouldCreateRicherCopyForNoPrices() {
		final PriceContractSerializablePredicate noPricesRequired = new PriceContractSerializablePredicate(
			PriceFetchMode.NONE, null, null, null, Collections.emptySet()
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresPriceLists()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiresPriceLists()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresCurrency()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresPriceValidIn()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresEntityPrices()).thenReturn(PriceFetchMode.RESPECTING_FILTER);
		assertNotSame(noPricesRequired, noPricesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForNoPrices() {
		final PriceContractSerializablePredicate noAttributesRequired = new PriceContractSerializablePredicate(
			PriceFetchMode.NONE, null, null, null, Collections.emptySet()
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresPriceLists()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiresPriceLists()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresCurrency()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresPriceValidIn()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresEntityPrices()).thenReturn(PriceFetchMode.NONE);
		assertSame(noAttributesRequired, noAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForNoPricesWhenPricesPresent() {
		final PriceContractSerializablePredicate noAttributesRequired = new PriceContractSerializablePredicate(
			PriceFetchMode.RESPECTING_FILTER, null, null, null, Collections.emptySet()
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresPriceLists()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiresPriceLists()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresCurrency()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresPriceValidIn()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresEntityPrices()).thenReturn(PriceFetchMode.NONE);
		assertSame(noAttributesRequired, noAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldCreateRicherCopyForNoPricesRespectingFilter() {
		final PriceContractSerializablePredicate pricesRespectingFilterRequired = new PriceContractSerializablePredicate(
			PriceFetchMode.RESPECTING_FILTER, null, null, null, Collections.emptySet()
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresPriceLists()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiresPriceLists()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresCurrency()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresPriceValidIn()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresEntityPrices()).thenReturn(PriceFetchMode.ALL);
		assertNotSame(pricesRespectingFilterRequired, pricesRespectingFilterRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForRespectingFilter() {
		final PriceContractSerializablePredicate pricesRespectingFilterRequired = new PriceContractSerializablePredicate(
			PriceFetchMode.NONE, null, null, null, Collections.emptySet()
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresPriceLists()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiresPriceLists()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresCurrency()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresPriceValidIn()).thenReturn(null);
		Mockito.when(evitaRequest.getRequiresEntityPrices()).thenReturn(PriceFetchMode.NONE);
		assertSame(pricesRespectingFilterRequired, pricesRespectingFilterRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForAllPrices() {
		final PriceContractSerializablePredicate allPrices = new PriceContractSerializablePredicate(
			PriceFetchMode.ALL, null, null, null, Collections.emptySet()
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresPriceLists()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiresPriceLists()).thenReturn(new Serializable[]{"A"});
		Mockito.when(evitaRequest.getRequiresCurrency()).thenReturn(Currency.getInstance("CZK"));
		Mockito.when(evitaRequest.getRequiresPriceValidIn()).thenReturn(ZonedDateTime.now());
		Mockito.when(evitaRequest.getRequiresEntityPrices()).thenReturn(PriceFetchMode.RESPECTING_FILTER);
		assertSame(allPrices, allPrices.createRicherCopyWith(evitaRequest));
	}

}