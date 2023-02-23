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

import io.evitadb.api.data.*;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.exception.ContextMissingException;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.filter.PriceInPriceLists;
import io.evitadb.api.query.require.PriceHistogram;
import io.evitadb.api.query.require.QueryPriceMode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entity prices container allows defining set of prices of the entity.
 * Attributes may be indexed for fast filtering ({@link Price#isSellable()}). Prices are not automatically indexed
 * in order not to waste precious memory space for data that will never be used in search queries.
 * <p>
 * Filtering in prices is executed by using constraints like {@link io.evitadb.api.query.filter.PriceBetween},
 * {@link io.evitadb.api.query.filter.PriceValidIn}, {@link PriceInPriceLists} or
 * {@link QueryPriceMode}.
 * <p>
 * Class is immutable on purpose - we want to support caching the entities in a shared cache and accessed by many threads.
 * For altering the contents use {@link InitialPricesBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode(of = "version")
public class Prices implements PricesContract, Versioned, ContentComparator<Prices> {
	private static final long serialVersionUID = -2717054691549391374L;

	/**
	 * Contains version of this object and gets increased with any (direct) entity update. Allows to execute
	 * optimistic locking i.e. avoiding parallel modifications.
	 */
	@Getter final int version;
	/**
	 * Prices are specific to a very few entities, but because correct price computation is very complex in e-commerce
	 * systems and highly affects performance of the entities filtering and sorting, they deserve first class support
	 * in entity model. It is pretty common in B2B systems single product has assigned dozens of prices for the different
	 * customers.
	 * <p>
	 * Specifying prices on entity allows usage of {@link io.evitadb.api.query.filter.PriceValidIn},
	 * {@link io.evitadb.api.query.filter.PriceBetween}, {@link QueryPriceMode}
	 * and {@link PriceInPriceLists} filtering constraints and also {@link io.evitadb.api.query.order.PriceAscending},
	 * {@link io.evitadb.api.query.order.PriceDescending} ordering of the entities. Additional requirements
	 * {@link PriceHistogram}, {@link io.evitadb.api.query.require.Prices}
	 * can be used in query as well.
	 */
	final Map<PriceKey, PriceContract> priceIndex;
	/**
	 * Price inner record handling controls how prices that share same `inner entity id` will behave during filtering and sorting.
	 */
	@Getter final PriceInnerRecordHandling priceInnerRecordHandling;

	public Prices(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		this.version = 1;
		this.priceIndex = new HashMap<>();
		this.priceInnerRecordHandling = priceInnerRecordHandling;
	}

	public Prices(int version, @Nonnull Collection<PriceContract> prices, @Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		this.version = version;
		this.priceIndex = prices.stream().collect(Collectors.toUnmodifiableMap(PriceContract::getPriceKey, Function.identity()));
		this.priceInnerRecordHandling = priceInnerRecordHandling;
	}

	/**
	 * Returns version of this object.
	 */
	@Override
	public int getPricesVersion() {
		return version;
	}

	/**
	 * Returns price by its business key identification.
	 */
	@Nullable
	public PriceContract getPrice(PriceKey priceKey) {
		return priceIndex.get(priceKey);
	}

	/**
	 * Returns price by its business key identification.
	 */
	@Nullable
	public PriceContract getPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		return priceIndex.get(new PriceKey(priceId, priceList, currency));
	}

	/**
	 * Returns a price for which the entity should be sold. This method can be used only in context of a {@link Query}
	 * with price related constraints so that `currency` and `priceList` priority can be extracted from the query.
	 * The moment is either extracted from the query as well (if present) or current date and time is used.
	 */
	@Nullable
	@Override
	public PriceContract getSellingPrice() throws ContextMissingException {
		throw new ContextMissingException();
	}

	/**
	 * Returns a price for which the entity should be sold. This method can be used in context of a {@link Query}
	 * with price related constraints so that `currency` and `priceList` priority can be extracted from the query.
	 * The moment is either extracted from the query as well (if present) or current date and time is used.
	 *
	 * The method differs from {@link #getSellingPrice()} in the sense of never returning {@link ContextMissingException}
	 * and returning list of all possibly matching selling prices.
	 */
	@Nonnull
	@Override
	public List<PriceContract> getAllSellingPrices() {
		return getAllSellingPrices(null, null);
	}

	/**
	 * Returns a price for which the entity should be sold. Only indexed prices in requested currency, valid
	 * at the passed moment are taken into an account. Prices are also limited by the passed set of price lists and
	 * the first price found in the order of the requested price list ids will be returned.
	 *
	 * @param from           - lower bound of the price (inclusive)
	 * @param to             - upper bound of the price (inclusive)
	 * @param queryPriceMode - controls whether price with or without VAT is used
	 * @throws ContextMissingException when entity is not related to any {@link Query} or the query
	 *                                 lacks price related constraints
	 */
	@Override
	public boolean hasPriceInInterval(@Nonnull BigDecimal from, @Nonnull BigDecimal to, @Nonnull QueryPriceMode queryPriceMode) throws ContextMissingException {
		throw new ContextMissingException();
	}

	/**
	 * Returns all prices of the entity.
	 */
	@Nonnull
	public Collection<PriceContract> getPrices() {
		return priceIndex.values();
	}

	/**
	 * Returns all prices indexed by key.
	 */
	@Nonnull
	public Map<PriceKey, PriceContract> getPriceIndex() {
		return priceIndex;
	}

	/**
	 * Returns true when there is no single price defined.
	 */
	public boolean isEmpty() {
		return priceIndex.isEmpty();
	}

	/**
	 * Method returns true if any prices inner data differs from other prices object.
	 */
	@Override
	public boolean differsFrom(@Nullable Prices otherPrices) {
		if (this == otherPrices) return false;
		if (otherPrices == null) return true;

		if (version != otherPrices.version) return true;
		if (priceInnerRecordHandling != otherPrices.priceInnerRecordHandling) return true;
		if (priceIndex.size() != otherPrices.priceIndex.size()) return true;

		for (Entry<PriceKey, PriceContract> entry : priceIndex.entrySet()) {
			final PriceContract otherPrice = otherPrices.getPrice(entry.getKey());
			if (otherPrice == null || entry.getValue().differsFrom(otherPrice)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		final Collection<PriceContract> prices = getPrices();
		return "selects " + priceInnerRecordHandling + " from: " +
			(
				prices.isEmpty() ?
					"no price" :
					prices
						.stream()
						.map(Object::toString)
						.collect(Collectors.joining(", "))
			);
	}
}
