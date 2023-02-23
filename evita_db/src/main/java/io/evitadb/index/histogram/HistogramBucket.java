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

package io.evitadb.index.histogram;

import io.evitadb.api.Transaction;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.array.TransactionalObject;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.transactionalMemory.*;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Histogram point represents single "bucket" in {@link HistogramIndex} representing single {@link Comparable} {@link #value}
 * and bitmap (ordered and distinct) of record ids.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class HistogramBucket<T extends Comparable<T>> implements TransactionalObject<HistogramBucket<T>>,
	VoidTransactionMemoryProducer<HistogramBucket<T>>,
	TransactionalLayerProducer<Void, HistogramBucket<T>>,
	TransactionalCreatorMaintainer,
	Comparable<HistogramBucket<T>>,
	Serializable {
	private static final long serialVersionUID = 8584161806399686698L;
	private final T value;
	private final TransactionalBitmap recordIds;

	public HistogramBucket(T value) {
		this.value = value;
		this.recordIds = new TransactionalBitmap(EmptyBitmap.INSTANCE);
	}

	public HistogramBucket(T value, Bitmap recordIds) {
		this.value = value;
		this.recordIds = new TransactionalBitmap(recordIds);
	}

	public HistogramBucket(T value, TransactionalBitmap recordIds) {
		this.value = value;
		this.recordIds = recordIds;
	}

	public HistogramBucket(T value, int... recordIds) {
		this.value = value;
		this.recordIds = new TransactionalBitmap(new BaseBitmap(recordIds));
	}

	/**
	 * Returns comparable value that represents this bucket.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Registers new record ids for histogram point value.
	 * Already present record ids are silently skipped.
	 */
	public void addRecord(int... recordId) {
		this.recordIds.addAll(recordId);
	}

	/**
	 * Unregisters existing record ids from histogram point value.
	 * Non present record ids are silently skipped.
	 */
	public void removeRecord(int... recordId) {
		this.recordIds.removeAll(recordId);
	}

	/**
	 * Merges record ids of passed histogram point to this histogram point.
	 * Histogram point in the argument is required to have same value as this point.
	 */
	public void add(HistogramBucket<T> histogramBucket) {
		Assert.isTrue(value.compareTo(histogramBucket.value) == 0, "Values of the histogram point differs: " + value + " vs. " + histogramBucket.value);
		this.recordIds.addAll(histogramBucket.getRecordIds());
	}

	/**
	 * Subtracts record ids of passed histogram point from this histogram point.
	 * Histogram point in the argument is required to have same value as this point.
	 */
	public void remove(HistogramBucket<T> histogramBucket) {
		Assert.isTrue(value.compareTo(histogramBucket.value) == 0, "Values of the histogram point differs: " + value + " vs. " + histogramBucket.value);
		this.recordIds.removeAll(histogramBucket.getRecordIds());
	}

	/**
	 * Returns ordered array of distinct record ids of this histogram point.
	 */
	public TransactionalBitmap getRecordIds() {
		return this.recordIds;
	}

	/**
	 * Returns true if this histogram point contains no record ids.
	 */
	public boolean isEmpty() {
		return this.recordIds.isEmpty();
	}

	/**
	 * Compares {@link #value} of this and passed histogram point.
	 */
	@Override
	public int compareTo(@Nonnull HistogramBucket<T> o) {
		return value.compareTo(o.value);
	}

	public boolean deepEquals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		@SuppressWarnings("unchecked") final HistogramBucket<T> that = (HistogramBucket<T>) o;
		return value.equals(that.value) && recordIds.equals(that.recordIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		@SuppressWarnings("unchecked") final HistogramBucket<T> that = (HistogramBucket<T>) o;
		return value.compareTo(that.value) == 0;
	}

	@Override
	public String toString() {
		return "HistogramBucket{" +
			"value=" + value +
			", recordIds=" + recordIds +
			'}';
	}

	/*
		TransactionalObject implementation
	 */

	@Override
	public HistogramBucket<T> createCopyWithMergedTransactionalMemory(Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		return new HistogramBucket<>(
			value,
			transactionalLayer.getStateCopyWithCommittedChanges(recordIds, transaction)
		);
	}

	@Override
	public void removeFromTransactionalMemory() {
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(recordIds);
	}

	@Override
	public HistogramBucket<T> makeClone() {
		return new HistogramBucket<>(value, new BaseBitmap(recordIds));
	}

	@Override
	public Collection<TransactionalLayerCreator<?>> getMaintainedTransactionalCreators() {
		return Collections.singleton(recordIds);
	}
}
