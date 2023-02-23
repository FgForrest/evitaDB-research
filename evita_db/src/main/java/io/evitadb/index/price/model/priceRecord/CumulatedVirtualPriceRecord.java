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

package io.evitadb.index.price.model.priceRecord;

import io.evitadb.api.query.require.QueryPriceMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a virtual record, that is created an "on the fly" record with computed prices. This record is created for
 * {@link io.evitadb.api.data.PriceInnerRecordHandling#SUM sum price computation strategy}. After price records was
 * returned to the client it is garbage collected and stored nowhere in the indexes.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class CumulatedVirtualPriceRecord implements PriceRecordContract {
	private static final long serialVersionUID = -8702849059439375941L;
	private static final Comparator<PriceRecordContract> FULL_COMPARATOR =
		Comparator.comparing(PriceRecordContract::getEntityPrimaryKey)
			.thenComparing(PriceRecordContract::getPriceWithoutVat)
			.thenComparing(PriceRecordContract::getPriceWithVat);

	@Getter private final int entityPrimaryKey;
	@Getter private final int price;
	@Getter private final QueryPriceMode priceMode;

	@Override
	public int getPriceWithVat() {
		return priceMode == QueryPriceMode.WITH_VAT ? price : 0;
	}

	@Override
	public int getPriceWithoutVat() {
		return priceMode == QueryPriceMode.WITHOUT_VAT ? price : 0;
	}

	@Override
	public int getInnerRecordId() {
		return 0;
	}

	@Override
	public int getInternalPriceId() {
		return 0;
	}

	@Override
	public int getPriceId() {
		return 0;
	}

	@Override
	public boolean isInnerRecordSpecific() {
		return false;
	}

	@Override
	public int compareTo(PriceRecordContract o) {
		return FULL_COMPARATOR.compare(this, o);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CumulatedVirtualPriceRecord that = (CumulatedVirtualPriceRecord) o;
		return entityPrimaryKey == that.entityPrimaryKey && price == that.price && priceMode == that.priceMode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entityPrimaryKey, price, priceMode);
	}

	@Override
	public String toString() {
		return "CumulatedVirtualPriceRecord{" +
			"entityPrimaryKey=" + entityPrimaryKey +
			", price=" + price +
			", priceMode=" + priceMode +
			'}';
	}
}
