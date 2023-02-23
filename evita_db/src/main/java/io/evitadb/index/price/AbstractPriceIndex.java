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

package io.evitadb.index.price;

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.index.EntityIndexDataStructure;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import io.evitadb.storage.model.storageParts.StoragePart;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.utils.Assert.notNull;

/**
 * Price index contains data structures that allow processing price related filtering and sorting constraints such as
 * {@link io.evitadb.api.query.filter.PriceBetween}, {@link io.evitadb.api.query.filter.PriceValidIn},
 * {@link io.evitadb.api.query.order.PriceAscending} or {@link io.evitadb.api.query.order.PriceDescending}.
 *
 * For each combination of {@link PriceContract#getPriceList()} and {@link PriceContract#getCurrency()} it maintains
 * separate filtering index. Pre-sorted indexes are maintained for all prices regardless of their price list
 * relation because there is no guarantee that there will be currency or price list part of the query.
 *
 * This abstract class unifies base logic both for {@link PriceSuperIndex} that works with full data set and
 * {@link PriceRefIndex} that works with slimmed down data referencing the original ones in super index.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
abstract class AbstractPriceIndex<T extends PriceListAndCurrencyPriceIndex> implements EntityIndexDataStructure, Serializable, PriceIndexContract {
	private static final long serialVersionUID = 7715100845881804377L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();

	@Nonnull
	@Override
	public Collection<? extends PriceListAndCurrencyPriceIndex> getPriceListAndCurrencyIndexes() {
		return getPriceIndexes().values();
	}

	@Nonnull
	@Override
	public Stream<? extends PriceListAndCurrencyPriceIndex> getPriceIndexesStream(@Nonnull Currency currency, @Nonnull PriceInnerRecordHandling innerRecordHandling) {
		return getPriceIndexes()
			.values()
			.stream()
			.filter(it -> {
				final PriceIndexKey priceIndexKey = it.getPriceIndexKey();
				return innerRecordHandling.equals(priceIndexKey.getRecordHandling()) &&
					currency.equals(priceIndexKey.getCurrency());
			});
	}

	@Nonnull
	@Override
	public Stream<? extends PriceListAndCurrencyPriceIndex> getPriceIndexesStream(@Nonnull Serializable priceListName, @Nonnull PriceInnerRecordHandling innerRecordHandling) {
		return getPriceIndexes()
			.values()
			.stream()
			.filter(it -> {
				final PriceIndexKey priceIndexKey = it.getPriceIndexKey();
				return innerRecordHandling.equals(priceIndexKey.getRecordHandling()) &&
					priceListName.equals(priceIndexKey.getPriceList());
			});
	}

	@Nonnull
	@Override
	public PriceInternalIdContainer addPrice(
		int entityPrimaryKey,
		@Nullable Integer internalPriceId, @Nonnull PriceKey priceKey,
		@Nonnull PriceInnerRecordHandling innerRecordHandling,
		@Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity,
		int priceWithoutVat, int priceWithVat
	) {
		final T priceListIndex = this.getPriceIndexes().computeIfAbsent(
			new PriceIndexKey(priceKey.getPriceList(), priceKey.getCurrency(), innerRecordHandling),
			this::createNewPriceListAndCurrencyIndex
		);
		return addPrice(
			priceListIndex, entityPrimaryKey,
			internalPriceId, priceKey.getPriceId(), innerRecordId,
			validity, priceWithoutVat, priceWithVat
		);
	}

	@Override
	public void priceRemove(
		int entityPrimaryKey,
		int internalPriceId, @Nonnull PriceKey priceKey,
		@Nonnull PriceInnerRecordHandling innerRecordHandling,
		@Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity,
		int priceWithoutVat, int priceWithVat
	) {
		final PriceIndexKey lookupKey = new PriceIndexKey(priceKey.getPriceList(), priceKey.getCurrency(), innerRecordHandling);
		final T priceListIndex = this.getPriceIndexes().get(lookupKey);
		notNull(priceListIndex, "Price index for price list " + priceKey.getPriceList() + " and currency " + priceKey.getCurrency() + " not found!");

		removePrice(
			priceListIndex, entityPrimaryKey,
			internalPriceId, priceKey.getPriceId(), innerRecordId,
			validity, priceWithoutVat, priceWithVat
		);

		if (priceListIndex.isEmpty()) {
			removeExistingIndex(lookupKey, priceListIndex);
		}
	}

	@Override
	public T getPriceIndex(@Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull PriceInnerRecordHandling innerRecordHandling) {
		return getPriceIndex(new PriceIndexKey(priceList, currency, innerRecordHandling));
	}

	@Override
	public T getPriceIndex(@Nonnull PriceIndexKey priceListAndCurrencyKey) {
		return this.getPriceIndexes().get(priceListAndCurrencyKey);
	}

	@Override
	public boolean isPriceIndexEmpty() {
		return this.getPriceIndexes().isEmpty();
	}

	/**
	 * Method returns collection of all modified parts of this index that were modified and needs to be stored.
	 */
	public Collection<StoragePart> getModifiedStorageParts(int entityIndexPrimaryKey) {
		return this.getPriceIndexes()
			.values()
			.stream()
			.map(it -> it.createStoragePart(entityIndexPrimaryKey))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Override
	public void resetDirty() {
		for (PriceListAndCurrencyPriceIndex priceIndex : getPriceIndexes().values()) {
			priceIndex.resetDirty();
		}
	}

	/*
		PROTECTED METHODS
	 */

	@Nonnull
	protected abstract T createNewPriceListAndCurrencyIndex(@Nonnull PriceIndexKey lookupKey);

	protected void removeExistingIndex(@Nonnull PriceIndexKey lookupKey, @Nonnull T priceListIndex) {
		getPriceIndexes().remove(lookupKey);
	}

	protected abstract PriceInternalIdContainer addPrice(
		@Nonnull T priceListIndex, int entityPrimaryKey,
		@Nullable Integer internalPriceId, int priceId, @Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity,
		int priceWithoutVat, int priceWithVat
	);

	protected abstract void removePrice(
		@Nonnull T priceListIndex, int entityPrimaryKey,
		int internalPriceId, int priceId, @Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity,
		int priceWithoutVat, int priceWithVat
	);

	protected abstract Map<PriceIndexKey, T> getPriceIndexes();

}
