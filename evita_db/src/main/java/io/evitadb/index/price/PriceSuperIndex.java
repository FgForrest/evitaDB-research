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
import io.evitadb.index.price.PriceSuperIndex.PriceIndexChanges;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.internalId.MinimalPriceInternalIdContainer;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.index.price.model.priceRecord.PriceRecordInnerRecordSpecific;
import io.evitadb.index.transactionalMemory.TransactionalContainerChanges;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Super index maintains references to {@link PriceListAndCurrencyPriceSuperIndex}, the main logic is part of
 * the abstract class this implementation extends from. Price super index (or its inner indexes) contain full price
 * dataset and is self-sufficient (on the contrary to {@link PriceRefIndex}).
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class PriceSuperIndex extends AbstractPriceIndex<PriceListAndCurrencyPriceSuperIndex> implements TransactionalLayerProducer<PriceIndexChanges, PriceSuperIndex> {
	private static final long serialVersionUID = 7596276815836027747L;
	/**
	 * Map of {@link PriceListAndCurrencyPriceSuperIndex indexes} that contains prices that relates to specific price-list
	 * and currency combination.
	 */
	protected final TransactionalMemoryMap<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> priceIndexes;
	/**
	 * Contains the sequence for assigning {@link PriceInternalIdContainer#getInternalPriceId()} to a newly encountered
	 * prices in the input data. See {@link PriceInternalIdContainer} to see the reasons behind it.
	 */
	private final AtomicInteger internalPriceIdSequence;

	public PriceSuperIndex() {
		this.internalPriceIdSequence = new AtomicInteger(0);
		this.priceIndexes = new TransactionalMemoryMap<>(new HashMap<>());
	}

	public PriceSuperIndex(int internalPriceIdSequenceSeed, @Nonnull Map<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> priceIndexes) {
		this.internalPriceIdSequence = new AtomicInteger(internalPriceIdSequenceSeed);
		this.priceIndexes = new TransactionalMemoryMap<>(priceIndexes);
	}

	private PriceSuperIndex(AtomicInteger internalPriceIdSequenceSeed, @Nonnull Map<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> priceIndexes) {
		this.internalPriceIdSequence = internalPriceIdSequenceSeed;
		this.priceIndexes = new TransactionalMemoryMap<>(priceIndexes);
	}

	/**
	 * Returns the last used id in the sequence for assigning {@link PriceInternalIdContainer#getInternalPriceId()} to
	 * a newly encountered prices in the input data. See {@link PriceInternalIdContainer} to see the reasons behind it.
	 */
	public int getLastAssignedInternalPriceId() {
		return internalPriceIdSequence.get();
	}

	/**
	 * Returns new, unique {@link PriceInternalIdContainer#getInternalPriceId()} from the sequence.
	 * See {@link PriceInternalIdContainer} to see the reasons behind it.
	 */
	public int nextInternalPriceId() {
		return internalPriceIdSequence.incrementAndGet();
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
	public PriceSuperIndex createCopyWithMergedTransactionalMemory(@Nullable PriceIndexChanges layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, @Nullable Transaction transaction) {
		final PriceSuperIndex priceIndex = new PriceSuperIndex(
			// we need to pass the atomic integer here because there may be updates pending, and we don't want to lose them
			// the sequences don't work transactionally
			internalPriceIdSequence,
			transactionalLayer.getStateCopyWithCommittedChanges(this.priceIndexes, transaction)
		);
		ofNullable(layer).ifPresent(it -> it.clean(transactionalLayer));
		return priceIndex;
	}

	/*
		PROTECTED METHODS
	 */

	@Nonnull
	@Override
	protected PriceListAndCurrencyPriceSuperIndex createNewPriceListAndCurrencyIndex(@Nonnull PriceIndexKey lookupKey) {
		final PriceListAndCurrencyPriceSuperIndex newPriceListIndex = new PriceListAndCurrencyPriceSuperIndex(lookupKey);
		ofNullable(TransactionalMemory.getTransactionalMemoryLayer(this))
			.ifPresent(it -> it.addCreatedItem(newPriceListIndex));
		return newPriceListIndex;
	}

	@Override
	protected void removeExistingIndex(@Nonnull PriceIndexKey lookupKey, @Nonnull PriceListAndCurrencyPriceSuperIndex priceListIndex) {
		super.removeExistingIndex(lookupKey, priceListIndex);
		ofNullable(TransactionalMemory.getTransactionalMemoryLayer(this))
			.ifPresent(it -> it.addRemovedItem(priceListIndex));
	}

	@Override
	protected PriceInternalIdContainer addPrice(
		@Nonnull PriceListAndCurrencyPriceSuperIndex priceListIndex, int entityPrimaryKey,
		@Nullable Integer internalPriceId, int priceId, @Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity, int priceWithoutVat, int priceWithVat
	) {
		final int usedInternalPriceId = ofNullable(internalPriceId).orElseGet(this::nextInternalPriceId);
		final PriceRecordContract priceRecord = innerRecordId == null ?
			new PriceRecord(usedInternalPriceId, priceId, entityPrimaryKey, priceWithVat, priceWithoutVat) :
			new PriceRecordInnerRecordSpecific(
				usedInternalPriceId, priceId, entityPrimaryKey, innerRecordId, priceWithVat, priceWithoutVat
			);
		priceListIndex.addPrice(priceRecord, validity);
		return new MinimalPriceInternalIdContainer(priceRecord.getInternalPriceId());
	}

	@Override
	protected void removePrice(
		@Nonnull PriceListAndCurrencyPriceSuperIndex priceListIndex, int entityPrimaryKey,
		int internalPriceId, int priceId, @Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity, int priceWithoutVat, int priceWithVat
	) {
		priceListIndex.removePrice(entityPrimaryKey, internalPriceId, validity);
	}

	@Override
	protected Map<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> getPriceIndexes() {
		return priceIndexes;
	}

	/**
	 * This class collects changes in {@link #priceIndexes} transactional map.
	 */
	public static class PriceIndexChanges {
		private final TransactionalContainerChanges<Void, PriceListAndCurrencyPriceSuperIndex, PriceListAndCurrencyPriceSuperIndex> collectedPriceIndexChanges = new TransactionalContainerChanges<>();

		public void addCreatedItem(PriceListAndCurrencyPriceSuperIndex priceIndex) {
			collectedPriceIndexChanges.addCreatedItem(priceIndex);
		}

		public void addRemovedItem(PriceListAndCurrencyPriceSuperIndex priceIndex) {
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
