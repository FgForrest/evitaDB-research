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

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price.PriceKey;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Price record envelopes single price of the entity. This data structure allows translating price ids and inner record
 * ids to entity primary key. Also price amounts are used for sorting by price. Price indexes don't use original
 * {@link PriceContract} to minimize memory consumption (this class contains only primitive types).
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@ToString
public class PriceRecord implements PriceRecordContract {
	private static final long serialVersionUID = 474325852345436993L;

	/**
	 * Contains internal id for {@link PriceContract#getPriceId()}. The is unique for the price identified
	 * by {@link PriceKey} inside single entity. The id is different for two prices sharing same {@link PriceKey}
	 * but are present in different entities.
	 */
	@Getter private final int internalPriceId;
	/**
	 * Refers to {@link PriceContract#getPriceId()}. The price is unique only within the scope of the primary key.
	 */
	@Getter private final int priceId;
	/**
	 * Refers to {@link Entity#getPrimaryKey()}.
	 */
	@Getter private final int entityPrimaryKey;
	/**
	 * Refers to {@link PriceContract#getPriceWithVat()}.
	 */
	@Getter private final int priceWithVat;
	/**
	 * Refers to {@link PriceContract#getPriceWithoutVat()}.
	 */
	@Getter private final int priceWithoutVat;

	public PriceRecord(int internalPriceId, int priceId, int entityPrimaryKey, int priceWithVat, int priceWithoutVat) {
		this.internalPriceId = internalPriceId;
		this.priceId = priceId;
		this.entityPrimaryKey = entityPrimaryKey;
		this.priceWithVat = priceWithVat;
		this.priceWithoutVat = priceWithoutVat;
	}

	@Override
	public boolean isInnerRecordSpecific() {
		return false;
	}

	@Override
	public int getInnerRecordId() {
		return 0;
	}

	@Override
	public int compareTo(@Nonnull PriceRecordContract other) {
		return PRICE_RECORD_COMPARATOR.compare(this, other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.internalPriceId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final PriceRecord that = (PriceRecord) o;
		return internalPriceId == that.internalPriceId;
	}
}
