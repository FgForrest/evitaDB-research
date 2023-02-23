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

import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.dataType.DateTimeRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.stream.Stream;

/**
 * Contract for classes that allow creating / updating or removing information about prices in {@link Entity} instance.
 * This interface is usually connected with {@link PricesBuilder} contract because {@link Entity} is designed
 * to be immutable.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface PricesEditor<W extends PricesEditor<W>> extends PricesContract {

	/**
	 * Creates or updates price with key properties: priceId, priceList, currency.
	 * Beware! If priceId and currency stays the same, but only price list changes (although it's unlikely), you need
	 * to {@link #removePrice(int, Serializable, Currency)} first and set it with new price list.
	 *
	 * @param priceId         - identification of the price in the external systems
	 * @param priceList       - identification of the price list (either external or internal {@link Entity#getPrimaryKey()}
	 * @param currency        - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param priceWithoutVat - price without VAT tax
	 * @param vat             - VAT percentage (i.e. for 19% it'll be 19.00)
	 * @param priceWithVat    - price with VAT tax
	 * @param sellable        - controls whether price is subject to filtering / sorting logic ({@see Price#sellable})
	 * @return builder instance to allow command chaining
	 */
	W setPrice(
		int priceId,
		@Nonnull Serializable priceList,
		@Nonnull Currency currency,
		@Nonnull BigDecimal priceWithoutVat,
		@Nonnull BigDecimal vat,
		@Nonnull BigDecimal priceWithVat,
		boolean sellable
	);

	/**
	 * Creates or updates price with key properties: priceId, priceList, currency.
	 * Beware! If priceId and currency stays the same, but only price list changes (although it's unlikely), you need
	 * to {@link #removePrice(int, Serializable, Currency)} first and set it with new price list.
	 *
	 * @param priceId         - identification of the price in the external systems
	 * @param priceList       - identification of the price list (either external or internal {@link Entity#getPrimaryKey()}
	 * @param currency        - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param innerRecordId   - sub-record identification {@see Price#innerRecordId}, must be positive value
	 * @param priceWithoutVat - price without VAT tax
	 * @param vat             - VAT percentage (i.e. for 19% it'll be 19.00)
	 * @param priceWithVat    - price with VAT tax
	 * @param sellable        - controls whether price is subject to filtering / sorting logic ({@see Price#sellable})
	 * @return builder instance to allow command chaining
	 */
	W setPrice(
		int priceId,
		@Nonnull Serializable priceList,
		@Nonnull Currency currency,
		@Nullable Integer innerRecordId,
		@Nonnull BigDecimal priceWithoutVat,
		@Nonnull BigDecimal vat,
		@Nonnull BigDecimal priceWithVat,
		boolean sellable
	);

	/**
	 * Creates or updates price with key properties: priceId, priceList, currency.
	 * Beware! If priceId and currency stays the same, but only price list changes (although it's unlikely), you need
	 * to {@link #removePrice(int, Serializable, Currency)} first and set it with new price list.
	 *
	 * @param priceId         - identification of the price in the external systems
	 * @param priceList       - identification of the price list (either external or internal {@link Entity#getPrimaryKey()}
	 * @param currency        - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param priceWithoutVat - price without VAT tax
	 * @param vat             - VAT percentage (i.e. for 19% it'll be 19.00)
	 * @param priceWithVat    - price with VAT tax
	 * @param validity        - date and time interval for which the price is valid (inclusive)
	 * @param sellable        - controls whether price is subject to filtering / sorting logic ({@see Price#sellable})
	 * @return builder instance to allow command chaining
	 */
	W setPrice(
		int priceId,
		@Nonnull Serializable priceList,
		@Nonnull Currency currency,
		@Nonnull BigDecimal priceWithoutVat,
		@Nonnull BigDecimal vat,
		@Nonnull BigDecimal priceWithVat,
		@Nullable DateTimeRange validity,
		boolean sellable
	);

	/**
	 * Creates or updates price with key properties: priceId, priceList, currency.
	 * Beware! If priceId and currency stays the same, but only price list changes (although it's unlikely), you need
	 * to {@link #removePrice(int, Serializable, Currency)} first and set it with new price list.
	 *
	 * @param priceId         - identification of the price in the external systems
	 * @param priceList       - identification of the price list (either external or internal {@link Entity#getPrimaryKey()}
	 * @param currency        - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @param innerRecordId   - sub-record identification {@see Price#innerRecordId}, must be positive value
	 * @param priceWithoutVat - price without VAT tax
	 * @param vat             - VAT percentage (i.e. for 19% it'll be 19.00)
	 * @param priceWithVat    - price with VAT tax
	 * @param validity        - date and time interval for which the price is valid (inclusive)
	 * @param sellable        - controls whether price is subject to filtering / sorting logic ({@see Price#sellable})
	 * @return builder instance to allow command chaining
	 */
	W setPrice(
		int priceId,
		@Nonnull Serializable priceList,
		@Nonnull Currency currency,
		@Nullable Integer innerRecordId,
		@Nonnull BigDecimal priceWithoutVat,
		@Nonnull BigDecimal vat,
		@Nonnull BigDecimal priceWithVat,
		@Nullable DateTimeRange validity,
		boolean sellable
	);

	/**
	 * Removes existing price by specifying key properties.
	 *
	 * @param priceId   - identification of the price in the external systems
	 * @param priceList - identification of the price list (either external or internal {@link Entity#getPrimaryKey()}
	 * @param currency  - identification of the currency. Three-letter form according to [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217)
	 * @return builder instance to allow command chaining
	 */
	W removePrice(
		int priceId,
		@Nonnull Serializable priceList,
		@Nonnull Currency currency
	);

	/**
	 * Sets behaviour for prices that has {@link Price#getInnerRecordId()} set in terms of computing the "selling" price.
	 */
	W setPriceInnerRecordHandling(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling);

	/**
	 * Removes previously set behaviour for prices with {@link Price#getInnerRecordId()}. You should ensure that
	 * the entity has no prices with non-null {@link Price#getInnerRecordId()}.
	 */
	W removePriceInnerRecordHandling();

	/**
	 * This is helper method that allows to purge all methods, that were not overwritten
	 * (i.e. {@link #setPrice(int, Serializable, Currency, Integer, BigDecimal, BigDecimal, BigDecimal, boolean)}
	 * by instance of this editor/builder class. It's handy if you know that whenever any price is updated in the entity
	 * you also update all other prices (i.e. all prices are rewritten). By using this method you don't need to care about
	 * purging the previous set of superfluous prices.
	 *
	 * This method is analogical to following process:
	 *
	 * - clear all prices
	 * - set them all from scratch
	 *
	 * Now you can simply:
	 *
	 * - set all prices
	 * - remove all non "touched" prices
	 *
	 * Even if you set the price exactly the same (i.e. in reality it doesn't change), it'll remain - because it was
	 * "touched". This mechanism is here because we want to avoid price removal and re-insert due to optimistic locking
	 * which is supported on by-price level.
	 */
	W removeAllNonTouchedPrices();

	/**
	 * Interface that simply combines writer and builder contracts together.
	 */
	interface PricesBuilder extends PricesEditor<PricesEditor.PricesBuilder>, Builder<Prices> {

		@Nonnull
		@Override
		Stream<? extends LocalMutation<?, ?>> buildChangeSet();

	}

}
