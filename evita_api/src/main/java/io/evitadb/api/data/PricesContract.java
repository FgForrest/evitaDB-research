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

package io.evitadb.api.data;

import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.exception.ContextMissingException;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static io.evitadb.api.utils.CollectionUtils.createHashSet;
import static java.util.Optional.ofNullable;

/**
 * Contract for classes that allow reading information about prices in {@link Entity} instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface PricesContract extends Serializable {

	/**
	 * Computes a price for which the entity should be sold. Only indexed prices in requested currency, valid
	 * at the passed moment are taken into an account. Prices are also limited by the passed set of price lists and
	 * the first price found in the order of the requested price list ids will be returned.
	 */
	@Nullable
	static PriceContract computeSellingPrice(@Nonnull Collection<PriceContract> entityPrices, @Nonnull PriceInnerRecordHandling innerRecordHandling, @Nonnull Currency currency, @Nullable ZonedDateTime atTheMoment, @Nonnull Serializable[] priceListPriority, @Nonnull Predicate<PriceContract> filterPredicate) {
		final Map<Serializable, Integer> pLists = createHashMap(priceListPriority.length);
		for (int i = 0; i < priceListPriority.length; i++) {
			final Serializable pList = priceListPriority[i];
			pLists.put(pList, i);
		}
		final Stream<PriceContract> pricesStream = entityPrices
			.stream()
			.filter(PriceContract::exists)
			.filter(PriceContract::isSellable)
			.filter(it -> currency.equals(it.getCurrency()))
			.filter(it -> ofNullable(atTheMoment).map(mmt -> it.getValidity() == null || it.getValidity().isValidFor(mmt)).orElse(true))
			.filter(it -> pLists.containsKey(it.getPriceList()));

		switch (innerRecordHandling) {
			case NONE:
				return pricesStream
					.min(Comparator.comparing(o -> pLists.get(o.getPriceList())))
					.filter(filterPredicate)
					.orElse(null);
			case FIRST_OCCURRENCE: {
				final Map<Integer, List<PriceContract>> pricesByInnerId = pricesStream
					.collect(Collectors.groupingBy(it -> ofNullable(it.getInnerRecordId()).orElse(0)));
				return pricesByInnerId
					.values()
					.stream()
					.map(prices -> prices.stream()
						.min(Comparator.comparing(o -> pLists.get(o.getPriceList())))
						.orElse(null))
					.filter(Objects::nonNull)
					.filter(filterPredicate)
					.min(Comparator.comparing(PriceContract::getPriceWithVat))
					.orElse(null);
			}
			case SUM:
				final List<PriceContract> innerRecordPrices = pricesStream
					.collect(Collectors.groupingBy(it -> ofNullable(it.getInnerRecordId()).orElse(0)))
					.values()
					.stream()
					.map(prices -> prices.stream()
						.min(Comparator.comparing(o -> pLists.get(o.getPriceList())))
						.orElse(null))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
				if (innerRecordPrices.isEmpty()) {
					return null;
				} else {
					final PriceContract firstPrice = innerRecordPrices.get(0);
					// create virtual sum price
					final Price resultPrice = new Price(
						1, firstPrice.getPriceKey(), null,
						innerRecordPrices.stream().map(PriceContract::getPriceWithoutVat).reduce(BigDecimal::add).orElse(BigDecimal.ZERO),
						innerRecordPrices.stream().map(PriceContract::getVat).reduce((vat, vat2) -> {
							Assert.isTrue(vat.compareTo(vat2) == 0, "Prices have to have same VAT rate in order to compute selling price!");
							return vat;
						}).orElse(BigDecimal.ZERO),
						innerRecordPrices.stream().map(PriceContract::getPriceWithVat).reduce(BigDecimal::add).orElse(BigDecimal.ZERO),
						// computed virtual price has always no validity
						null,
						true
					);
					return filterPredicate.test(resultPrice) ? resultPrice : null;
				}
			default:
				throw new IllegalStateException("Unknown price inner record handling mode: " + innerRecordHandling);
		}
	}

	/**
	 * Returns true if single price differs between first and second instance.
	 */
	static boolean anyPriceDifferBetween(@Nonnull PricesContract first, @Nonnull PricesContract second) {
		final Collection<PriceContract> thisValues = first.getPrices();
		final Collection<PriceContract> otherValues = second.getPrices();

		if (thisValues.size() != otherValues.size()) {
			return true;
		} else {
			return thisValues
				.stream()
				.anyMatch(it -> it.differsFrom(second.getPrice(it.getPriceId(), it.getPriceList(), it.getCurrency())));
		}
	}

	/**
	 * Returns price by its business key identification.
	 *
	 * @param priceId   - identification of the price in the external systems
	 * @param priceList - identification of the price list (either external or internal)
	 * @param currency  - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 */
	@Nullable
	PriceContract getPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency);

	/**
	 * Returns all prices from the specified price list.
	 *
	 * @param priceList - identification of the price list (either external or internal)
	 */
	@Nonnull
	default Collection<PriceContract> getPrices(@Nonnull Serializable priceList) {
		return getPrices()
			.stream()
			.filter(it -> priceList.equals(it.getPriceList()))
			.collect(Collectors.toList());
	}

	/**
	 * Returns all prices from the specified currency.
	 *
	 * @param currency - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 */
	@Nonnull
	default Collection<PriceContract> getPrices(@Nonnull Currency currency) {
		return getPrices()
			.stream()
			.filter(it -> currency.equals(it.getCurrency()))
			.collect(Collectors.toList());
	}

	/**
	 * Returns all prices from the specified currency.
	 *
	 * @param currency  - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param priceList - identification of the price list (either external or internal)
	 */
	@Nonnull
	default Collection<PriceContract> getPrices(@Nonnull Currency currency, @Nonnull Serializable priceList) {
		return getPrices()
			.stream()
			.filter(it -> currency.equals(it.getCurrency()) && priceList.equals(it.getPriceList()))
			.collect(Collectors.toList());
	}

	/**
	 * Returns a price for which the entity should be sold. Only indexed prices in requested currency, valid
	 * at the passed moment are taken into an account. Prices are also limited by the passed set of price lists and
	 * the first price found in the order of the requested price list ids will be returned.
	 *
	 * @param currency          - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param atTheMoment       - identification of the moment when the entity is about to be sold
	 * @param priceListPriority - identification of the price list (either external or internal)
	 */
	@Nullable
	default PriceContract getSellingPrice(@Nonnull Currency currency, @Nullable ZonedDateTime atTheMoment, @Nonnull Serializable... priceListPriority) {
		return computeSellingPrice(getPrices(), getPriceInnerRecordHandling(), currency, atTheMoment, priceListPriority, Objects::nonNull);
	}

	/**
	 * Returns a price for which the entity should be sold. This method can be used only in context of a {@link Query}
	 * with price related constraints so that `currency` and `priceList` priority can be extracted from the query.
	 * The moment is either extracted from the query as well (if present) or current date and time is used.
	 *
	 * @throws ContextMissingException when entity is not related to any {@link Query} or the query
	 *                                 lacks price related constraints
	 */
	@Nullable
	PriceContract getSellingPrice() throws ContextMissingException;

	/**
	 * Returns all prices for which the entity could be sold. This method can be used in context of a {@link Query}
	 * with price related constraints so that `currency` and `priceList` priority can be extracted from the query.
	 * The moment is either extracted from the query as well (if present) or current date and time is used.
	 *
	 * The method differs from {@link #getSellingPrice()} in the sense of never returning {@link ContextMissingException}
	 * and returning list of all possibly matching selling prices (not only single one). Returned list may be also
	 * empty if there is no such price.
	 *
	 * @param currency          - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param atTheMoment       - identification of the moment when the entity is about to be sold
	 * @param priceListPriority - identification of the price list (either external or internal)
	 */
	@Nonnull
	default List<PriceContract> getAllSellingPrices(@Nullable Currency currency, @Nullable ZonedDateTime atTheMoment, @Nullable Serializable... priceListPriority) {
		final Set<Serializable> pLists;
		if (priceListPriority == null) {
			pLists = Collections.emptySet();
		} else {
			pLists = createHashSet(priceListPriority.length);
			pLists.addAll(Arrays.asList(priceListPriority));
		}
		return getPrices()
			.stream()
			.filter(PriceContract::exists)
			.filter(PriceContract::isSellable)
			.filter(it -> currency == null || currency.equals(it.getCurrency()))
			.filter(it -> ofNullable(atTheMoment).map(mmt -> it.getValidity() == null || it.getValidity().isValidFor(mmt)).orElse(true))
			.filter(it -> pLists.isEmpty() || pLists.contains(it.getPriceList()))
			.collect(Collectors.toList());
	}

	/**
	 * Returns all prices for which the entity could be sold. This method can be used in context of a {@link Query}
	 * with price related constraints so that `currency` and `priceList` priority can be extracted from the query.
	 * The moment is either extracted from the query as well (if present) or current date and time is used.
	 *
	 * The method differs from {@link #getSellingPrice()} in the sense of never returning {@link ContextMissingException}
	 * and returning list of all possibly matching selling prices (not only single one). Returned list may be also
	 * empty if there is no such price.
	 */
	@Nonnull
	List<PriceContract> getAllSellingPrices();

	/**
	 * Returns a price for which the entity should be sold. Only indexed prices in requested currency, valid
	 * at the passed moment are taken into an account. Prices are also limited by the passed set of price lists and
	 * the first price found in the order of the requested price list ids will be returned.
	 *
	 * @param from              - lower bound of the price (inclusive)
	 * @param to                - upper bound of the price (inclusive)
	 * @param currency          - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param queryPriceMode    - controls whether price with or without VAT is used
	 * @param atTheMoment       - identification of the moment when the entity is about to be sold
	 * @param priceListPriority - identification of the price list (either external or internal)
	 */
	default boolean hasPriceInInterval(@Nonnull BigDecimal from, @Nonnull BigDecimal to, @Nonnull QueryPriceMode queryPriceMode, @Nonnull Currency currency, @Nullable ZonedDateTime atTheMoment, @Nonnull Serializable... priceListPriority) {
		switch (getPriceInnerRecordHandling()) {
			case NONE:
			case SUM:
				return ofNullable(getSellingPrice(currency, atTheMoment, priceListPriority))
					.map(it -> queryPriceMode == QueryPriceMode.WITHOUT_VAT ? it.getPriceWithoutVat() : it.getPriceWithVat())
					.map(it -> from.compareTo(it) <= 0 && to.compareTo(it) >= 0)
					.orElse(false);
			case FIRST_OCCURRENCE:
				final Map<Serializable, Integer> pLists = createHashMap(priceListPriority.length);
				for (int i = 0; i < priceListPriority.length; i++) {
					final Serializable pList = priceListPriority[i];
					pLists.put(pList, i);
				}
				final Map<Integer, List<PriceContract>> pricesByInnerRecordId = getPrices()
					.stream()
					.filter(PriceContract::exists)
					.filter(PriceContract::isSellable)
					.filter(it -> currency.equals(it.getCurrency()))
					.filter(it -> ofNullable(atTheMoment).map(mmt -> it.getValidity() == null || it.getValidity().isValidFor(mmt)).orElse(true))
					.filter(it -> pLists.containsKey(it.getPriceList()))
					.collect(Collectors.groupingBy(it -> ofNullable(it.getInnerRecordId()).orElse(0)));
				return pricesByInnerRecordId
					.values()
					.stream()
					.anyMatch(prices -> prices.stream()
						.min(Comparator.comparing(o -> pLists.get(o.getPriceList())))
						.map(it -> queryPriceMode == QueryPriceMode.WITHOUT_VAT ? it.getPriceWithoutVat() : it.getPriceWithVat())
						.map(it -> from.compareTo(it) <= 0 && to.compareTo(it) >= 0)
						.orElse(null));
			default:
				throw new IllegalStateException("Unknown price inner record handling mode: " + getPriceInnerRecordHandling());
		}
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
	boolean hasPriceInInterval(@Nonnull BigDecimal from, @Nonnull BigDecimal to, @Nonnull QueryPriceMode queryPriceMode) throws ContextMissingException;

	/**
	 * Returns all prices of the entity.
	 */
	@Nonnull
	Collection<PriceContract> getPrices();

	/**
	 * Returns price inner record handling that controls how prices that share same `inner entity id` will behave during
	 * filtering and sorting.
	 */
	@Nonnull
	PriceInnerRecordHandling getPriceInnerRecordHandling();

	/**
	 * Returns version timestamp signalizing change in prices - namely in {@link #getPriceInnerRecordHandling()} because
	 * {@link PriceContract} is versioned separately.
	 */
	int getPricesVersion();

}
