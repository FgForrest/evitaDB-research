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
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.index.EntityIndexDataStructure;
import io.evitadb.index.array.TransactionalObjArray;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.bool.TransactionalBoolean;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.entityPrices.EntityPrices;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.index.range.RangeIndex;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import io.evitadb.index.transactionalMemory.VoidTransactionMemoryProducer;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.price.priceIndex.PriceIdContainerFormula;
import io.evitadb.query.algebra.price.priceIndex.PriceIndexContainerFormula;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.index.PriceListAndCurrencyRefIndexStoragePart;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Index contains information used for filtering by price that is related to specific price list and currency combination.
 * Real world use-cases usually filter entities by price in certain currency in set of price lists, and we can greatly
 * minimize the working set by separating price indexes by this combination.
 *
 * RefIndex attempts to store minimal data set in order to save memory on heap. For memory expensive objects such as
 * {@link PriceRecord} and {@link EntityPrices} it looks up via {@link #superIndexAccessor} lambda to
 * {@link PriceListAndCurrencyPriceSuperIndex} where the records are located.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceListAndCurrencyPriceRefIndex implements VoidTransactionMemoryProducer<PriceListAndCurrencyPriceRefIndex>, EntityIndexDataStructure, Serializable, PriceListAndCurrencyPriceIndex {
	private static final long serialVersionUID = 182980639981206272L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * This is internal flag that tracks whether the index contents became dirty and needs to be persisted.
	 */
	private final TransactionalBoolean dirty;
	/**
	 * Unique identification of this index - contains price list name and currency combination.
	 */
	@Getter private final PriceIndexKey priceIndexKey;
	/**
	 * Bitmap contains all entity ids known to this index. This bitmap represents superset of all inner bitmaps.
	 */
	private final TransactionalBitmap indexedPriceEntityIds;
	/**
	 * Field contains condensed bitmap of all {@link #priceTriples} {@link PriceRecordContract#getInternalPriceId()}
	 * for the sake of the faster search for appropriate {@link PriceRecordContract} by the internal price id.
	 */
	private final TransactionalBitmap indexedPriceIds;
	/**
	 * Range index contains date-time validity information for each indexed price id. This index is used to process
	 * the {@link io.evitadb.api.query.filter.PriceValidIn} filtering constraint.
	 */
	private final RangeIndex validityIndex;
	/**
	 * Array contains complete information about prices sorted by {@link PriceContract#getPriceId()} allowing translation
	 * of price id to {@link Entity#getPrimaryKey()} using binary search algorithm.
	 */
	private final TransactionalObjArray<PriceRecordContract> priceTriples;
	/**
	 * Lambda providing access to the main {@link PriceListAndCurrencyPriceSuperIndex} that keeps memory expensive
	 * objects.
	 */
	private Function<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> superIndexAccessor;
	/**
	 * Contains cached result of {@link TransactionalBitmap#getArray()} call.
	 */
	private int[] memoizedIndexedPriceIds;

	public PriceListAndCurrencyPriceRefIndex(
		@Nonnull PriceIndexKey priceIndexKey,
		@Nonnull Function<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> superIndexAccessor
	) {
		this.dirty = new TransactionalBoolean();
		this.indexedPriceEntityIds = new TransactionalBitmap();
		this.indexedPriceIds = new TransactionalBitmap();
		this.priceIndexKey = priceIndexKey;
		this.validityIndex = new RangeIndex();
		this.priceTriples = new TransactionalObjArray<>(new PriceRecordContract[0]);
		this.superIndexAccessor = superIndexAccessor;
	}

	public PriceListAndCurrencyPriceRefIndex(
		@Nonnull PriceIndexKey priceIndexKey,
		@Nonnull RangeIndex validityIndex,
		@Nonnull int[] priceIds,
		@Nonnull Function<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> superIndexAccessor
	) {
		this.dirty = new TransactionalBoolean();
		this.priceIndexKey = priceIndexKey;
		this.validityIndex = validityIndex;
		this.superIndexAccessor = superIndexAccessor;
		this.indexedPriceIds = new TransactionalBitmap(priceIds);
		this.memoizedIndexedPriceIds = priceIds;
		final PriceRecordContract[] priceRecords = superIndexAccessor.apply(priceIndexKey).getPriceRecords(indexedPriceIds);
		this.priceTriples = new TransactionalObjArray<>(priceRecords);

		final int[] entityIds = new int[priceRecords.length];
		for (int i = 0; i < priceRecords.length; i++) {
			final PriceRecordContract priceRecord = priceRecords[i];
			entityIds[i] = priceRecord.getEntityPrimaryKey();
		}
		this.indexedPriceEntityIds = new TransactionalBitmap(entityIds);
	}

	private PriceListAndCurrencyPriceRefIndex(
		@Nonnull PriceIndexKey priceIndexKey,
		@Nonnull Bitmap indexedPriceEntityIds,
		@Nonnull Bitmap priceIds,
		@Nonnull RangeIndex validityIndex,
		@Nonnull Function<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> superIndexAccessor
	) {
		this.dirty = new TransactionalBoolean();
		this.priceIndexKey = priceIndexKey;
		this.indexedPriceEntityIds = new TransactionalBitmap(indexedPriceEntityIds);
		this.indexedPriceIds = new TransactionalBitmap(priceIds);
		this.validityIndex = validityIndex;
		this.superIndexAccessor = superIndexAccessor;
		final PriceRecordContract[] priceRecords = getPriceSuperIndex().getPriceRecords(this.indexedPriceIds);
		this.priceTriples = new TransactionalObjArray<>(priceRecords);
	}

	/**
	 * This method replaces super index accessor with new one. This needs to be done when transaction is committed and
	 * PriceListAndCurrencyPriceRefIndex is created with link to the original transactional
	 * {@link PriceListAndCurrencyPriceSuperIndex} but finally new {@link io.evitadb.api.EntityCollection} is created
	 * along with new {@link PriceSuperIndex} and reference needs to be exchanged.
	 */
	public void updateReferencesTo(@Nonnull Function<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> superIndexAccessor) {
		this.superIndexAccessor = superIndexAccessor;
	}

	/**
	 * Indexes inner record id or entity primary key into the price index with passed values.
	 */
	public void addPrice(@Nonnull PriceRecordContract priceRecord, @Nullable DateTimeRange validity) {
		// index the presence of the record
		this.indexedPriceEntityIds.add(priceRecord.getEntityPrimaryKey());
		this.indexedPriceIds.add(priceRecord.getInternalPriceId());
		// index validity
		if (validity != null) {
			this.validityIndex.addRecord(validity.getFrom(), validity.getTo(), priceRecord.getInternalPriceId());
		} else {
			this.validityIndex.addRecord(Long.MIN_VALUE, Long.MAX_VALUE, priceRecord.getInternalPriceId());
		}
		// add price to the translation triple
		this.priceTriples.add(priceRecord);
		// make index dirty
		this.dirty.setToTrue();
		this.memoizedIndexedPriceIds = null;
	}

	/**
	 * Removes inner record id or entity primary key of passed values from the price index.
	 */
	public void removePrice(@Nonnull PriceRecordContract priceRecord, @Nullable DateTimeRange validity, @Nonnull EntityPrices updatedEntityPrices) {
		// remove price to the translation triple
		this.priceTriples.remove(priceRecord);

		// remove the presence of the record
		this.indexedPriceIds.remove(priceRecord.getInternalPriceId());

		if (!updatedEntityPrices.containsAnyOf(this.priceTriples.getArray())) {
			// remove the presence of the record
			this.indexedPriceEntityIds.remove(priceRecord.getEntityPrimaryKey());
		}
		// remove validity
		if (validity != null) {
			this.validityIndex.removeRecord(validity.getFrom(), validity.getTo(), priceRecord.getInternalPriceId());
		} else {
			this.validityIndex.removeRecord(Long.MIN_VALUE, Long.MAX_VALUE, priceRecord.getInternalPriceId());
		}
		// make index dirty
		this.dirty.setToTrue();
		this.memoizedIndexedPriceIds = null;
	}

	/**
	 * Method returns condensed bitmap of all {@link #priceTriples} {@link PriceRecordContract#getInternalPriceId()}
	 * that can be used for the faster search for appropriate {@link PriceRecordContract} by the internal price id.
	 */
	@Nonnull
	public int[] getIndexedPriceIds() {
		// if there is transaction open, there might be changes in the histogram data, and we can't easily use cache
		if (TransactionalMemory.isTransactionalMemoryAvailable()) {
			return this.indexedPriceIds.getArray();
		} else {
			if (memoizedIndexedPriceIds == null) {
				memoizedIndexedPriceIds = this.indexedPriceIds.getArray();
			}
			return memoizedIndexedPriceIds;
		}
	}

	@Nonnull
	@Override
	public Bitmap getIndexedPriceEntityIds() {
		return indexedPriceEntityIds;
	}

	@Nonnull
	@Override
	public Formula getIndexedPriceEntityIdsFormula() {
		if (indexedPriceEntityIds.isEmpty()) {
			return EmptyFormula.INSTANCE;
		} else {
			return new ConstantFormula(indexedPriceEntityIds);
		}
	}

	@Nonnull
	@Override
	public PriceIdContainerFormula getIndexedRecordIdsValidInFormula(ZonedDateTime theMoment) {
		final long thePoint = DateTimeRange.toComparableLong(theMoment);
		return new PriceIdContainerFormula(
			this, this.validityIndex.getRecordsWithRangesOutsideInclusive(thePoint, thePoint)
		);
	}

	@Nullable
	@Override
	public int[] getInternalPriceIdsForEntity(int entityId) {
		return getPriceSuperIndex().getInternalPriceIdsForEntity(entityId);
	}

	@Override
	@Nullable
	public PriceRecordContract[] getLowestPriceRecordsForEntity(int entityId) {
		return getPriceSuperIndex().getLowestPriceRecordsForEntity(entityId);
	}

	@Nonnull
	@Override
	public PriceRecordContract[] getPriceRecords() {
		return this.priceTriples.getArray();
	}

	@Nonnull
	@Override
	public Formula createPriceIndexFormulaWithAllRecords() {
		return new PriceIndexContainerFormula(this, this.getIndexedPriceEntityIdsFormula());
	}

	@Override
	public boolean isEmpty() {
		return this.indexedPriceEntityIds.isEmpty();
	}

	@Nullable
	@Override
	public StoragePart createStoragePart(int entityIndexPrimaryKey) {
		if (this.dirty.isTrue()) {
			final int[] priceIds = new int[priceTriples.getLength()];
			final Iterator<PriceRecordContract> it = priceTriples.iterator();
			int index = 0;
			while (it.hasNext()) {
				final PriceRecordContract priceRecord = it.next();
				priceIds[index++] = priceRecord.getInternalPriceId();
			}
			return new PriceListAndCurrencyRefIndexStoragePart(
				entityIndexPrimaryKey, priceIndexKey, validityIndex, priceIds
			);
		} else {
			return null;
		}
	}

	/**
	 * Method returns reference to the {@link PriceListAndCurrencyPriceSuperIndex} that maintains the original, full
	 * {@link PriceRecordContract} data. Because the price data are quite big the only place where we store them is
	 * the {@link PriceListAndCurrencyPriceSuperIndex} and other indexes only reference the existing objects in it.
	 */
	@Nonnull
	public PriceListAndCurrencyPriceSuperIndex getPriceSuperIndex() {
		return Objects.requireNonNull(superIndexAccessor.apply(priceIndexKey));
	}

	@Override
	public String toString() {
		return priceIndexKey.toString();
	}

	@Override
	public void resetDirty() {
		this.dirty.reset();
	}

	@Override
	public void clearTransactionalMemory() {
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.dirty);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.indexedPriceEntityIds);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.indexedPriceIds);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.validityIndex);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.priceTriples);
	}

	/*
		TransactionalLayerCreator implementation
	 */

	@Override
	public PriceListAndCurrencyPriceRefIndex createCopyWithMergedTransactionalMemory(@Nullable Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, @Nullable Transaction transaction) {
		// we can safely throw away dirty flag now
		transactionalLayer.removeTransactionalMemoryLayerIfExists(this.dirty);
		return new PriceListAndCurrencyPriceRefIndex(
			priceIndexKey,
			transactionalLayer.getStateCopyWithCommittedChanges(this.indexedPriceEntityIds, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.indexedPriceIds, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.validityIndex, transaction),
			this.superIndexAccessor
		);
	}

}
