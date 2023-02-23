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

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.structure.SerializablePredicate;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.require.PriceFetchMode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Optional.ofNullable;

/**
 * This predicate allows to limit number of prices visible to the client based on query constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceContractSerializablePredicate implements SerializablePredicate<PriceContract> {
	public static final PriceContractSerializablePredicate DEFAULT_INSTANCE = new PriceContractSerializablePredicate();
	private static final long serialVersionUID = -7100489955953977035L;
	@Getter private final PriceFetchMode priceFetchMode;
	@Getter private final Set<Serializable> priceListsAsSet;
	@Getter private final Currency currency;
	@Getter private final ZonedDateTime validIn;
	@Getter private final Serializable[] priceLists;

	public PriceContractSerializablePredicate() {
		this.priceFetchMode = PriceFetchMode.ALL;
		this.currency = null;
		this.validIn = null;
		this.priceLists = null;
		this.priceListsAsSet = Collections.emptySet();
	}

	public PriceContractSerializablePredicate(@Nonnull EvitaRequestBase evitaRequest) {
		this.priceFetchMode = evitaRequest.getRequiresEntityPrices();
		this.currency = evitaRequest.getRequiresCurrency();
		this.validIn = evitaRequest.getRequiresPriceValidIn();
		this.priceLists = evitaRequest.getRequiresPriceLists();
		this.priceListsAsSet = new HashSet<>(Arrays.asList(priceLists));
	}

	public PriceContractSerializablePredicate(@Nonnull EvitaRequestBase evitaRequest, @Nonnull PriceContractSerializablePredicate predicate) {
		this.priceFetchMode = evitaRequest.getRequiresEntityPrices();
		this.currency = predicate.currency;
		this.validIn = predicate.validIn;
		this.priceLists = predicate.priceLists;
		this.priceListsAsSet = predicate.priceListsAsSet;
	}

	PriceContractSerializablePredicate(@Nonnull PriceFetchMode priceFetchMode, @Nullable Currency currency, @Nullable ZonedDateTime validIn, @Nullable Serializable[] priceLists, @Nonnull Set<Serializable> priceListsAsSet) {
		this.priceFetchMode = priceFetchMode;
		this.currency = currency;
		this.validIn = validIn;
		this.priceLists = priceLists;
		this.priceListsAsSet = priceListsAsSet;
	}

	@Override
	public boolean test(@Nonnull PriceContract priceContract) {
		switch (priceFetchMode) {
			case NONE:
				return false;
			case ALL:
				return priceContract.exists();
			case RESPECTING_FILTER:
				return priceContract.exists() &&
					(currency == null || Objects.equals(currency, priceContract.getCurrency())) &&
					(priceListsAsSet.isEmpty() || priceListsAsSet.contains(priceContract.getPriceList())) &&
					(validIn == null || ofNullable(priceContract.getValidity()).map(it -> it.isValidFor(validIn)).orElse(true));
			default:
				throw new IllegalStateException("Unknown price fetch mode: " + priceFetchMode + "!");
		}
	}

	public PriceContractSerializablePredicate createRicherCopyWith(@Nonnull EvitaRequestBase evitaRequest) {
		final PriceFetchMode requiresEntityPrices = evitaRequest.getRequiresEntityPrices();
		if (this.priceFetchMode.ordinal() >= requiresEntityPrices.ordinal()) {
			// this predicate cannot change since everything is taken from the filter and this cannot change in time
			return this;
		} else {
			return new PriceContractSerializablePredicate(
				requiresEntityPrices, this.currency, this.validIn, this.priceLists, this.priceListsAsSet
			);
		}
	}
}
