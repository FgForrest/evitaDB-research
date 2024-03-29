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

import io.evitadb.api.Transaction;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.index.price.PriceRefIndex.PriceIndexChanges;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.entityPrices.EntityPrices;
import io.evitadb.index.price.model.internalId.MinimalPriceInternalIdContainer;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.index.transactionalMemory.TransactionalContainerChanges;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * Price index contains data structures that allow processing price related filtering and sorting constraints such as
 * {@link io.evitadb.api.query.filter.PriceBetween}, {@link io.evitadb.api.query.filter.PriceValidIn},
 * {@link io.evitadb.api.query.order.PriceAscending} or {@link io.evitadb.api.query.order.PriceDescending}.
 *
 * For each combination of {@link PriceContract#getPriceList()} and {@link PriceContract#getCurrency()} it maintains
 * separate filtering index. Pre-sorted indexes are maintained for all prices regardless of their price list
 * relation because there is no guarantee that there will be currency or price list part of the query.
 *
 * Ref index maintains references to {@link PriceListAndCurrencyPriceRefIndex}, the main logic is part of
 * the abstract class this implementation extends from. PriceRefIndex contains reduced set of data - we try to avoid
 * excessive memory consumption by maintaining reusing the existing {@link PriceRecord} and {@link EntityPrices}
 * objects in {@link PriceSuperIndex}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class PriceRefIndex extends AbstractPriceIndex<PriceListAndCurrencyPriceRefIndex> implements TransactionalLayerProducer<PriceIndexChanges, PriceRefIndex> {
	private static final long serialVersionUID = 7596276815836027747L;
	/**
	 * Map of {@link PriceListAndCurrencyPriceSuperIndex indexes} that contains prices that relates to specific price-list
	 * and currency combination.
	 */
	@Getter protected final TransactionalMemoryMap<PriceIndexKey, PriceListAndCurrencyPriceRefIndex> priceIndexes;
	/**
	 * Lambda providing access to the main {@link PriceSuperIndex} that keeps memory expensive
	 * objects.
	 */
	private Supplier<PriceSuperIndex> superIndexAccessor;

	public PriceRefIndex(@Nonnull Supplier<PriceSuperIndex> superIndexAccessor) {
		this.priceIndexes = new TransactionalMemoryMap<>(new HashMap<>());
		this.superIndexAccessor = superIndexAccessor;
	}

	public PriceRefIndex(@Nonnull Map<PriceIndexKey, PriceListAndCurrencyPriceRefIndex> priceIndexes, @Nonnull Supplier<PriceSuperIndex> superIndexAccessor) {
		this.priceIndexes = new TransactionalMemoryMap<>(priceIndexes);
		this.superIndexAccessor = superIndexAccessor;
	}

	@Override
	public void clearTransactionalMemory() {
		for (PriceListAndCurrencyPriceIndex priceListAndCurrencyPriceIndex : priceIndexes.values()) {
			priceListAndCurrencyPriceIndex.clearTransactionalMemory();
		}

		final PriceIndexChanges changes = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		ofNullable(changes).ifPresent(it -> it.cleanAll(TransactionalMemory.getTransactionalMemoryLayer()));
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.priceIndexes);
	}

	@Override
	public PriceIndexChanges createLayer() {
		return new PriceIndexChanges();
	}

	@Override
	public PriceRefIndex createCopyWithMergedTransactionalMemory(@Nullable PriceIndexChanges layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, @Nullable Transaction transaction) {
		final PriceRefIndex priceIndex = new PriceRefIndex(
			transactionalLayer.getStateCopyWithCommittedChanges(this.priceIndexes, transaction),
			this.superIndexAccessor
		);
		ofNullable(layer).ifPresent(it -> it.clean(transactionalLayer));
		return priceIndex;
	}

	/**
	 * This method replaces super index accessor with new one. This needs to be done when transaction is committed and
	 * PriceRefIndex is created with link to the original transactional {@link PriceSuperIndex} but finally new
	 * {@link io.evitadb.api.EntityCollection} is created along with new {@link PriceSuperIndex} and reference needs
	 * to be exchanged.
	 */
	public void updateReferencesTo(@Nonnull Supplier<PriceSuperIndex> priceSuperIndexAccessor) {
		this.superIndexAccessor = priceSuperIndexAccessor;
		for (PriceListAndCurrencyPriceRefIndex index : priceIndexes.values()) {
			index.updateReferencesTo(priceIndexKey -> superIndexAccessor.get().getPriceIndex(priceIndexKey));
		}
	}

	/*
		PROTECTED METHODS
	 */

	@Nonnull
	protected PriceListAndCurrencyPriceRefIndex createNewPriceListAndCurrencyIndex(@Nonnull PriceIndexKey lookupKey) {
		final PriceListAndCurrencyPriceRefIndex newPriceListIndex = new PriceListAndCurrencyPriceRefIndex(
			lookupKey, priceIndexKey -> this.superIndexAccessor.get().getPriceIndex(priceIndexKey)
		);
		ofNullable(TransactionalMemory.getTransactionalMemoryLayer(this))
			.ifPresent(it -> it.addCreatedItem(newPriceListIndex));
		return newPriceListIndex;
	}

	@Override
	protected void removeExistingIndex(@Nonnull PriceIndexKey lookupKey, @Nonnull PriceListAndCurrencyPriceRefIndex priceListIndex) {
		super.removeExistingIndex(lookupKey, priceListIndex);
		ofNullable(TransactionalMemory.getTransactionalMemoryLayer(this))
			.ifPresent(it -> it.addRemovedItem(priceListIndex));
	}

	@Override
	protected PriceInternalIdContainer addPrice(
		@Nonnull PriceListAndCurrencyPriceRefIndex priceListIndex, int entityPrimaryKey,
		@Nullable Integer internalPriceId, int priceId,
		@Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity, int priceWithoutVat, int priceWithVat
	) {
		final PriceListAndCurrencyPriceSuperIndex superIndex = Objects.requireNonNull(superIndexAccessor.get().getPriceIndex(priceListIndex.getPriceIndexKey()));
		final PriceRecordContract priceRecord = superIndex.getPriceRecord(Objects.requireNonNull(internalPriceId));
		priceListIndex.addPrice(priceRecord, validity);
		return new MinimalPriceInternalIdContainer(priceRecord.getInternalPriceId());
	}

	@Override
	protected void removePrice(
		@Nonnull PriceListAndCurrencyPriceRefIndex priceListIndex, int entityPrimaryKey,
		int internalPriceId, int priceId,
		@Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity, int priceWithoutVat, int priceWithVat
	) {
		final PriceListAndCurrencyPriceSuperIndex superIndex = Objects.requireNonNull(superIndexAccessor.get().getPriceIndex(priceListIndex.getPriceIndexKey()));
		final PriceRecordContract priceRecord = superIndex.getPriceRecord(internalPriceId);
		final EntityPrices entityPrices = superIndex.getEntityPrices(priceRecord.getEntityPrimaryKey());
		priceListIndex.removePrice(priceRecord, validity, entityPrices);
	}

	/**
	 * This class collects changes in {@link #priceIndexes} transactional map.
	 */
	public static class PriceIndexChanges {
		private final TransactionalContainerChanges<Void, PriceListAndCurrencyPriceRefIndex, PriceListAndCurrencyPriceRefIndex> collectedPriceIndexChanges = new TransactionalContainerChanges<>();

		public void addCreatedItem(PriceListAndCurrencyPriceRefIndex priceIndex) {
			collectedPriceIndexChanges.addCreatedItem(priceIndex);
		}

		public void addRemovedItem(PriceListAndCurrencyPriceRefIndex priceIndex) {
			collectedPriceIndexChanges.addRemovedItem(priceIndex);
		}

		public void clean(TransactionalLayerMaintainer transactionalLayer) {
			collectedPriceIndexChanges.clean(transactionalLayer);
		}

		public void cleanAll(TransactionalLayerMaintainer transactionalLayer) {
			collectedPriceIndexChanges.cleanAll(transactionalLayer);
		}
	}

}
