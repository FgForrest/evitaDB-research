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

package io.evitadb.api.data.key;

import io.evitadb.api.data.structure.Price;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Currency;
import java.util.Objects;

/**
 * Abstract ancestor for price related compressed keys.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public abstract class PriceKey implements Serializable {
	private static final long serialVersionUID = -1790271502418416908L;

	@Getter protected final Currency currency;
	@Getter protected final int hashCode;
	@Getter protected final Serializable priceList;

	protected PriceKey(@Nonnull Price.PriceKey priceKey) {
		this.priceList = priceKey.getPriceList();
		this.currency = priceKey.getCurrency();
		this.hashCode = Objects.hash(priceList, currency);
	}

	protected PriceKey(@Nonnull Serializable priceList, @Nonnull Currency currency) {
		this.priceList = priceList;
		this.currency = currency;
		this.hashCode = Objects.hash(priceList, currency);
	}

	protected PriceKey(@Nonnull Serializable priceList, @Nonnull Currency currency, int hashCode) {
		this.priceList = priceList;
		this.currency = currency;
		this.hashCode = hashCode;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PriceKey that = (PriceKey) o;
		return hashCode == that.hashCode && priceList.equals(that.priceList) && currency.equals(that.currency);
	}

	@Override
	public String toString() {
		return priceList + "/" + currency;
	}
}
