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

import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.PricesEditor.PricesBuilder;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.price.PriceInnerRecordHandlingMutation;
import io.evitadb.api.data.mutation.price.PriceMutation;
import io.evitadb.api.data.mutation.price.RemovePriceMutation;
import io.evitadb.api.data.mutation.price.UpsertPriceMutation;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.exception.ContextMissingException;
import io.evitadb.api.io.predicate.PriceContractSerializablePredicate;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Builder that is used to alter existing {@link Prices}. Prices is immutable object so there is need for another object
 * that would simplify the process of updating its contents. This is why the builder class exists.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExistingPricesBuilder implements PricesBuilder {
	private static final long serialVersionUID = 5366182867172493114L;

	/**
	 * This predicate filters out prices that were not fetched in query.
	 */
	@Getter private final PriceContractSerializablePredicate pricePredicate;
	private final Map<PriceKey, PriceMutation> priceMutations;
	private final Prices basePrices;
	private LocalMutation<Prices, PriceInnerRecordHandling> priceInnerRecordHandlingEntityMutation;
	private boolean removeAllNonModifiedPrices;

	public ExistingPricesBuilder(Prices prices) {
		this.basePrices = prices;
		this.priceMutations = new HashMap<>();
		this.pricePredicate = new PriceContractSerializablePredicate();
	}

	public ExistingPricesBuilder(Prices prices, PriceContractSerializablePredicate pricePredicate) {
		this.basePrices = prices;
		this.priceMutations = new HashMap<>();
		this.pricePredicate = pricePredicate;
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		return setPrice(priceId, priceList, currency, null, priceWithoutVat, vat, priceWithVat, null, sellable);
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		return setPrice(priceId, priceList, currency, innerRecordId, priceWithoutVat, vat, priceWithVat, null, sellable);
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		return setPrice(priceId, priceList, currency, null, priceWithoutVat, vat, priceWithVat, validity, sellable);
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		final UpsertPriceMutation mutation = new UpsertPriceMutation(priceKey, innerRecordId, priceWithoutVat, vat, priceWithVat, validity, sellable);
		this.priceMutations.put(priceKey, mutation);
		return this;
	}

	@Override
	public PricesBuilder removePrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		Assert.notNull(basePrices.getPrice(priceKey), "Price " + priceKey + " doesn't exist!");
		final RemovePriceMutation mutation = new RemovePriceMutation(priceKey);
		this.priceMutations.put(priceKey, mutation);
		return this;
	}

	@Override
	public PricesBuilder setPriceInnerRecordHandling(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		this.priceInnerRecordHandlingEntityMutation = new PriceInnerRecordHandlingMutation(priceInnerRecordHandling);
		return this;
	}

	@Override
	public PricesBuilder removePriceInnerRecordHandling() {
		Assert.isTrue(
			basePrices.getPriceInnerRecordHandling() != PriceInnerRecordHandling.NONE,
			"Price inner record handling is already set to NONE!"
		);
		this.priceInnerRecordHandlingEntityMutation = new PriceInnerRecordHandlingMutation(PriceInnerRecordHandling.NONE);
		return this;
	}

	@Override
	public PricesBuilder removeAllNonTouchedPrices() {
		this.removeAllNonModifiedPrices = true;
		return this;
	}

	@Override
	public PriceContract getPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		return getPriceInternal(new PriceKey(priceId, priceList, currency));
	}

	@Nullable
	@Override
	public PriceContract getSellingPrice() throws ContextMissingException {
		return getSellingPrice(
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists()
		);
	}

	@Nonnull
	@Override
	public List<PriceContract> getAllSellingPrices() {
		return getAllSellingPrices(
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists()
		);
	}

	@Override
	public boolean hasPriceInInterval(@Nonnull BigDecimal from, @Nonnull BigDecimal to, @Nonnull QueryPriceMode queryPriceMode) throws ContextMissingException {
		return hasPriceInInterval(
			from, to, queryPriceMode,
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists()
		);
	}

	@Nonnull
	@Override
	public Collection<PriceContract> getPrices() {
		return getPricesWithoutPredicate()
			.filter(pricePredicate)
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public PriceInnerRecordHandling getPriceInnerRecordHandling() {
		return ofNullable(priceInnerRecordHandlingEntityMutation)
			.map(it -> it.mutateLocal(basePrices).getPriceInnerRecordHandling())
			.orElseGet(basePrices::getPriceInnerRecordHandling);
	}

	@Override
	public int getPricesVersion() {
		return basePrices.getPricesVersion();
	}

	@Nonnull
	@Override
	public Stream<? extends LocalMutation<?, ?>> buildChangeSet() {
		return Stream.concat(
			removeAllNonModifiedPrices ?
				Stream.concat(
					Stream.of(priceInnerRecordHandlingEntityMutation),
					basePrices.getPrices().stream().filter(Droppable::exists).map(PriceContract::getPriceKey).map(RemovePriceMutation::new)
				)
				:
				Stream.of(priceInnerRecordHandlingEntityMutation),
			priceMutations.values().stream()
		).filter(Objects::nonNull);
	}

	@Nonnull
	@Override
	public Prices build() {
		final Collection<PriceContract> newPrices = getPricesWithoutPredicate().collect(Collectors.toList());
		final Map<PriceKey, PriceContract> newPriceIndex = newPrices
			.stream()
			.collect(
				Collectors.toMap(
					PriceContract::getPriceKey,
					Function.identity()
				)
			);
		final PriceInnerRecordHandling newPriceInnerRecordHandling = getPriceInnerRecordHandling();
		if (!Objects.equals(basePrices.getPriceIndex(), newPriceIndex) || basePrices.getPriceInnerRecordHandling() != newPriceInnerRecordHandling) {
			return new Prices(
				basePrices.getVersion() + 1,
				newPrices,
				newPriceInnerRecordHandling
			);
		} else {
			return basePrices;
		}
	}

	@Nonnull
	private Stream<PriceContract> getPricesWithoutPredicate() {
		return Stream.concat(
			basePrices
				.getPrices()
				.stream()
				.map(it ->
					ofNullable(priceMutations.get(it.getPriceKey()))
						.map(mut -> {
							final PriceContract mutatedValue = mut.mutateLocal(it);
							return mutatedValue.differsFrom(it) ? mutatedValue : it;
						})
						.orElseGet(() -> removeAllNonModifiedPrices ? null : it)
				)
				.filter(Objects::nonNull)
				.filter(PriceContract::exists),
			priceMutations
				.values()
				.stream()
				.filter(it -> basePrices.getPrice(it.getPriceKey()) == null)
				.map(it -> it.mutateLocal(null))
		);
	}

	private PriceContract getPriceInternal(PriceKey priceKey) {
		final PriceContract price = ofNullable(basePrices.getPrice(priceKey))
			.map(it -> ofNullable(priceMutations.get(priceKey))
				.map(x -> x.mutateLocal(it))
				.orElse(it)
			)
			.orElseGet(
				() -> ofNullable(priceMutations.get(priceKey))
					.map(it -> it.mutateLocal(null))
					.orElse(null)
			);
		return ofNullable(price).filter(pricePredicate).orElse(null);
	}

}
