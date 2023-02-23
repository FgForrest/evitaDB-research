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
import io.evitadb.api.data.PricesEditor.PricesBuilder;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.price.PriceInnerRecordHandlingMutation;
import io.evitadb.api.data.mutation.price.UpsertPriceMutation;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.exception.ContextMissingException;
import io.evitadb.api.query.require.QueryPriceMode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

/**
 * Builder that is used to create new {@link Price} instance.
 * Due to performance reasons (see {@link DirectWriteOrOperationLog} microbenchmark) there is special implementation
 * for the situation when entity is newly created. In this case we know everything is new and we don't need to closely
 * monitor the changes so this can speed things up.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InitialPricesBuilder implements PricesBuilder {
	private static final long serialVersionUID = 4752434728077797252L;

	private final Map<PriceKey, PriceContract> prices = new HashMap<>();
	@Getter private PriceInnerRecordHandling priceInnerRecordHandling = PriceInnerRecordHandling.NONE;

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		this.prices.put(
			priceKey,
			new Price(priceKey, null, priceWithoutVat, vat, priceWithVat, null, sellable)
		);
		return this;
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		this.prices.put(
			priceKey,
			new Price(priceKey, innerRecordId, priceWithoutVat, vat, priceWithVat, null, sellable)
		);
		return this;
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, DateTimeRange validity, boolean sellable) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		this.prices.put(
			priceKey,
			new Price(priceKey, null, priceWithoutVat, vat, priceWithVat, validity, sellable)
		);
		return this;
	}

	@Override
	public PricesBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		this.prices.put(
			priceKey,
			new Price(priceKey, innerRecordId, priceWithoutVat, vat, priceWithVat, validity, sellable)
		);
		return this;
	}

	@Override
	public PricesBuilder removePrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		final PriceKey priceKey = new PriceKey(priceId, priceList, currency);
		this.prices.remove(priceKey);
		return this;
	}

	@Override
	public PricesBuilder setPriceInnerRecordHandling(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		this.priceInnerRecordHandling = priceInnerRecordHandling;
		return this;
	}

	@Override
	public PricesBuilder removePriceInnerRecordHandling() {
		this.priceInnerRecordHandling = PriceInnerRecordHandling.NONE;
		return this;
	}

	@Override
	public PricesBuilder removeAllNonTouchedPrices() {
		throw new UnsupportedOperationException("This method has no sense when new entity is being created!");
	}

	@Override
	public PriceContract getPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		return getPrice(new PriceKey(priceId, priceList, currency));
	}

	@Nullable
	@Override
	public PriceContract getSellingPrice() throws ContextMissingException {
		throw new ContextMissingException();
	}

	@Nonnull
	@Override
	public List<PriceContract> getAllSellingPrices() {
		return getAllSellingPrices(null, null);
	}

	@Override
	public boolean hasPriceInInterval(@Nonnull BigDecimal from, @Nonnull BigDecimal to, @Nonnull QueryPriceMode queryPriceMode) throws ContextMissingException {
		throw new ContextMissingException();
	}

	@Nonnull
	@Override
	public Collection<PriceContract> getPrices() {
		return this.prices.values();
	}

	@Override
	public int getPricesVersion() {
		return 1;
	}

	public PriceContract getPrice(PriceKey priceKey) {
		return this.prices.get(priceKey);
	}

	@Nonnull
	@Override
	public Prices build() {
		return new Prices(
			1,
			prices.values(),
			priceInnerRecordHandling
		);
	}

	@Nonnull
	@Override
	public Stream<? extends LocalMutation<?, ?>> buildChangeSet() {
		return Stream.concat(
			priceInnerRecordHandling == null ? Stream.empty() : Stream.of(new PriceInnerRecordHandlingMutation(priceInnerRecordHandling)),
			prices.entrySet().stream().map(it -> new UpsertPriceMutation(it.getKey(), it.getValue()))
		);
	}

}
