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
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.array.TransactionalComplexObjArray;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.histogram.suppliers.HistogramBitmapSupplier;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.VoidTransactionMemoryProducer;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.base.OrFormula;
import io.evitadb.query.algebra.deferred.DeferredFormula;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;

/**
 * Histogram index is based on <a href="https://en.wikipedia.org/wiki/Histogram">Histogram data structure</a>. It's
 * organized as a set of "buckets" ordered from minimal to maximal {@link Comparable} value. Each bucket has assigned
 * bitmap (ordered distinct set of primitive integer values) that are assigned to bucket {@link HistogramBucket#getValue()}.
 * <p>
 * Search in histogram is possible via. binary search with O(log n) complexity due its sorted nature. Set of records
 * are easily available as the set assigned to that value. Range look-ups are also available as boolean OR of all bitmaps
 * from / to looked up value threshold.
 * <p>
 * Histogram MUST NOT contain same record id in multiple buckets. This prerequisite is not checked internally by this
 * data structure and client code must this ensure by its internal logic! If this prerequisite is not met, histogram
 * may return confusing results.
 * <p>
 * Thread safety:
 * <p>
 * Histogram supports transaction memory. This means, that the histogram can be updated by multiple writers and also
 * multiple readers can read from it's original array without spotting the changes made in transactional access. Each
 * transaction is bound to the same thread and different threads doesn't see changes in another threads.
 * <p>
 * If no transaction is opened, changes are applied directly to the delegate array. In such case the class is not thread
 * safe for multiple writers!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@ThreadSafe
@EqualsAndHashCode
public class HistogramIndex<T extends Comparable<T>> implements VoidTransactionMemoryProducer<HistogramIndex<T>>, Serializable {
	private static final long serialVersionUID = 3019703951858227807L;

	/**
	 * This lambda lay out records by {@link HistogramBucket#getValue()} one after another.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static final BiFunction<Long, HistogramBucket[], Formula> UNSORTED_AGGREGATION_LAMBDA = (indexTransactionId, histogramBuckets) -> new DeferredFormula(
		new HistogramBitmapSupplier<>(histogramBuckets)
	);

	/**
	 * This lambda lay out records in natural ascending order.
	 */
	@SuppressWarnings("rawtypes")
	private static final BiFunction<Long, HistogramBucket[], Formula> SORTED_AGGREGATION_LAMBDA = (indexTransactionId, histogramBuckets) -> {
		final Bitmap[] bitmaps = new Bitmap[histogramBuckets.length];
		for (int i = 0; i < histogramBuckets.length; i++) {
			bitmaps[i] = histogramBuckets[i].getRecordIds();
		}
		if (bitmaps.length == 0) {
			return EmptyFormula.INSTANCE;
		} else if (bitmaps.length == 1) {
			return new ConstantFormula(bitmaps[0]);
		} else {
			return new OrFormula(new long[] {indexTransactionId}, bitmaps);
		}
	};

	private final TransactionalComplexObjArray<HistogramBucket<T>> buckets;

	/**
	 * Method verifies that {@link HistogramBucket#getValue()}s in passed set are monotonically increasing and contain
	 * no duplicities.
	 */
	private static <T extends Comparable<T>> void assertValueIsMonotonic(HistogramBucket<T>[] points) {
		T previous = null;
		for (HistogramBucket<T> bucket : points) {
			Assert.isTrue(
				previous == null || previous.compareTo(bucket.getValue()) < 0,
				"Histogram values are not monotonic - conflicting values: " + previous + ", " + bucket.getValue()
			);
			previous = bucket.getValue();
		}
	}

	public HistogramIndex() {
		//noinspection unchecked, rawtypes
		buckets = new TransactionalComplexObjArray<>(
			new HistogramBucket[0],
			HistogramBucket::add,
			HistogramBucket::remove,
			HistogramBucket::isEmpty,
			HistogramBucket::deepEquals
		);
	}

	public HistogramIndex(HistogramBucket<T>[] buckets) {
		// contract check
		assertValueIsMonotonic(buckets);
		this.buckets = new TransactionalComplexObjArray<>(
			buckets,
			HistogramBucket::add,
			HistogramBucket::remove,
			HistogramBucket::isEmpty,
			HistogramBucket::deepEquals
		);
	}

	/**
	 * Adds single record id into the bucket with specified `value`. If no bucket with this value exists, it is automatically
	 * created and first record id is assigned to it.
	 *
	 * @return position where insertion happened
	 */
	public int addRecord(@Nonnull T value, int recordId) {
		final HistogramBucket<T> bucket = new HistogramBucket<>(value, EmptyBitmap.INSTANCE);
		bucket.addRecord(recordId);
		return buckets.addReturningIndex(bucket);
	}

	/**
	 * Adds multiple records id into the bucket with specified `value`. If no bucket with this value exists, it is automatically
	 * created and first record ida are assigned to it.
	 */
	public void addRecord(@Nonnull T value, int... recordId) {
		Assert.isTrue(!ArrayUtils.isEmpty(recordId), "Record ids must be not null and non empty!");
		final HistogramBucket<T> bucket = new HistogramBucket<>(value, EmptyBitmap.INSTANCE);
		bucket.addRecord(recordId);
		buckets.add(bucket);
	}

	/**
	 * Removes one or multiple record ids from the bucket with specified `value`. If no bucket with this value exists,
	 * nothing happens. If the bucket contains no record id that match passed record id, nothing happens. If removal
	 * of the record ids leaves the bucket empty, it's entirely removed.
	 *
	 * @return position where removal occurred or -1 if no removal occurred
	 */
	public int removeRecord(@Nonnull T value, int... recordId) {
		Assert.isTrue(!ArrayUtils.isEmpty(recordId), "Record ids must be not null and non-empty!");
		return buckets.remove(new HistogramBucket<>(value, new BaseBitmap(recordId)));
	}

	/**
	 * Method returns ture if histogram contains no records (i.e. no, or empty buckets).
	 */
	public boolean isEmpty() {
		final Iterator<HistogramBucket<T>> it = buckets.iterator();
		while (it.hasNext()) {
			final HistogramBucket<T> bucket = it.next();
			if (!bucket.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if there is a bucket related to passed `value`.
	 */
	public boolean contains(@Nullable T value) {
		final HistogramBucket<T>[] pointsArray = buckets.getArray();
		final int index = Arrays.binarySearch(pointsArray, new HistogramBucket<>(value));
		return index >= 0;
	}

	/**
	 * Returns set of record ids that are present at bucket related to passed `value`.
	 *
	 * @return empty IntegerBitmap if bucket doesn't exist
	 */
	@Nonnull
	public Bitmap getRecordsAt(@Nullable T value) {
		final HistogramBucket<T>[] pointsArray = buckets.getArray();
		final int index = Arrays.binarySearch(pointsArray, new HistogramBucket<>(value));
		if (index >= 0) {
			return pointsArray[index].getRecordIds();
		} else {
			return EmptyBitmap.INSTANCE;
		}
	}

	/**
	 * Returns set of record ids that are present at bucket at the specific index.
	 */
	@Nonnull
	public Bitmap getRecordsAtIndex(int index) {
		final HistogramBucket<T>[] pointsArray = buckets.getArray();
		if (index >= 0) {
			return pointsArray[index].getRecordIds();
		} else {
			return EmptyBitmap.INSTANCE;
		}
	}

	/**
	 * Returns array of "buckets" ordered by {@link HistogramBucket#getValue()} that contain record ids assigned in them.
	 */
	@Nonnull
	public HistogramBucket<T>[] getBuckets() {
		return this.buckets.getArray();
	}

	/**
	 * Returns entire content of this histogram as "subset" that allows easy access to the record ids inside.
	 * Records returned by this {@link HistogramSubSet} are sorted by the order of the bucket
	 * {@link HistogramBucket#getValue()}.
	 * <p>
	 * This histogram:
	 * A: [1, 4]
	 * B: [2, 9]
	 * C: [3]
	 * <p>
	 * Will return subset providing record ids bitmap in form of: [3, 2, 9, 1, 4]
	 */
	@Nonnull
	public HistogramSubSet<T> getRecords() {
		return getRecords(null, null);
	}

	/**
	 * Returns subset of this histogram with buckets between `moreThanEq` and `lessThanEq` (i.e. inclusive subset).
	 * Records returned by this {@link HistogramSubSet} are sorted by the order of the bucket
	 * {@link HistogramBucket#getValue()}.
	 *
	 * @see #getRecords()
	 */
	public HistogramSubSet<T> getRecords(@Nullable T moreThanEq, @Nullable T lessThanEq) {
		final HistogramBucket<T>[] records = getRecordsInternal(moreThanEq, lessThanEq, BoundsHandling.INCLUSIVE);
		return convertToUnSortedResult(records);
	}

	/**
	 * Returns entire content of this histogram as "subset" that allows easy access to the record ids inside.
	 * Records returned by this {@link HistogramSubSet} are sorted by the order of the bucket {@link HistogramBucket#getValue()}.
	 * <p>
	 * This histogram:
	 * A: [1, 4]
	 * B: [2, 9]
	 * C: [3]
	 * <p>
	 * Will return subset providing record ids bitmap in form of: [1, 4, 2, 9, 3]
	 */
	@Nonnull
	public HistogramSubSet<T> getRecordsReversed() {
		return getRecordsReversed(null, null);
	}

	/**
	 * Returns subset of this histogram with buckets between `moreThanEq` and `lessThanEq` (i.e. inclusive subset).
	 * Records returned by this {@link HistogramSubSet} are sorted by the order of the bucket
	 * {@link HistogramBucket#getValue()}.
	 *
	 * @see #getRecordsReversed()
	 */
	public HistogramSubSet<T> getRecordsReversed(@Nullable T moreThanEq, @Nullable T lessThanEq) {
		final HistogramBucket<T>[] records = getRecordsInternal(moreThanEq, lessThanEq, BoundsHandling.INCLUSIVE);
		ArrayUtils.reverse(records);
		return convertToUnSortedResult(records);
	}

	/**
	 * Returns entire content of this histogram as "subset" that allows easy access to the record ids inside.
	 * Records returned by this {@link HistogramSubSet} are sorted by record id value.
	 * <p>
	 * This histogram:
	 * A: [1, 4]
	 * B: [2, 9]
	 * C: [3]
	 * <p>
	 * Will return subset providing record ids bitmap in form of: [1, 2, 3, 4, 9]
	 */
	public HistogramSubSet<T> getSortedRecords() {
		return getSortedRecords(null, null);
	}

	/**
	 * Returns subset of this histogram with buckets between `moreThanEq` and `lessThanEq` (i.e. inclusive subset).
	 * Records returned by this {@link HistogramSubSet} are sorted by record id value.
	 *
	 * @see #getSortedRecords()
	 */
	public HistogramSubSet<T> getSortedRecords(@Nullable T moreThanEq, @Nullable T lessThanEq) {
		final HistogramBucket<T>[] records = getRecordsInternal(moreThanEq, lessThanEq, BoundsHandling.INCLUSIVE);
		return convertToSortedResult(records);
	}

	/**
	 * Returns subset of this histogram with buckets between `moreThan` and `lessThan` (i.e. exclusive subset).
	 * Records returned by this {@link HistogramSubSet} are sorted by record id value.
	 *
	 * @see #getSortedRecords()
	 */
	public HistogramSubSet<T> getSortedRecordsExclusive(@Nullable T moreThan, @Nullable T lessThan) {
		final HistogramBucket<T>[] records = getRecordsInternal(moreThan, lessThan, BoundsHandling.EXCLUSIVE);
		return convertToSortedResult(records);
	}

	/**
	 * Returns index of the record id in the histogram.
	 * <p>
	 * Considering this histogram:
	 * A: [1, 4]
	 * B: [2, 9]
	 * C: [3]
	 * <p>
	 * findRecordIndex(1) returns 0
	 * findRecordIndex(9) returns 3
	 * findRecordIndex(10) returns -1
	 *
	 * @return index or -1 if no record id was found in histogram
	 */
	public int findRecordIndex(int recordId) {
		int missedCounts = 0;
		final HistogramBucket<T>[] histogramBuckets = this.buckets.getArray();
		for (final HistogramBucket<T> bucket : histogramBuckets) {
			final Bitmap recordIds = bucket.getRecordIds();
			if (recordId < recordIds.getFirst() || recordId > recordIds.getLast()) {
				// move quickly to next bucket
				missedCounts += recordIds.size();
			} else {
				final int pointIndex = recordIds.indexOf(recordId);
				if (pointIndex >= 0) {
					return missedCounts + pointIndex;
				} else {
					// recordId not found
					missedCounts += recordIds.size();
				}
			}
		}
		return -1;
	}

	/**
	 * Returns index of the record id in the histogram in the REVERSED order.
	 * <p>
	 * Considering this histogram:
	 * A: [1, 4]
	 * B: [2, 9]
	 * C: [3]
	 * <p>
	 * findRecordIndex(1) returns 4
	 * findRecordIndex(9) returns 1
	 * findRecordIndex(10) returns -1
	 *
	 * @return index or -1 if no record id was found in histogram
	 */
	public int findRecordIndexReversed(int recordId) {
		int missedCounts = 0;
		final HistogramBucket<T>[] histogramBuckets = this.buckets.getArray();
		for (int i = histogramBuckets.length - 1; i >= 0; i--) {
			final HistogramBucket<T> bucket = histogramBuckets[i];
			final Bitmap recordIds = bucket.getRecordIds();
			if (recordId < recordIds.getFirst() || recordId > recordIds.getLast()) {
				// move quickly to next bucket
				missedCounts += recordIds.size();
			} else {
				final int pointIndex = recordIds.indexOf(recordId);
				if (pointIndex >= 0) {
					return missedCounts + (recordIds.size() - (pointIndex + 1));
				} else {
					// recordId not found
					missedCounts += recordIds.size();
				}
			}
		}
		return -1;
	}

	/**
	 * Returns count of the buckets in the histogram.
	 */
	public int getBucketCount() {
		return buckets.getLength();
	}

	/**
	 * Returns count of all record ids in the histogram.
	 */
	public int getLength() {
		int count = 0;
		for (HistogramBucket<T> bucket : this.buckets.getArray()) {
			count += bucket.getRecordIds().size();
		}
		return count;
	}

	/*
		Implementation of TransactionalLayerProducer
	 */

	@Override
	public String toString() {
		return "HistogramIndex{" +
			"points=" + buckets +
			'}';
	}

	/*
		PRIVATE METHODS
	 */

	@Override
	public HistogramIndex<T> createCopyWithMergedTransactionalMemory(Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		return new HistogramIndex<>(transactionalLayer.getStateCopyWithCommittedChanges(buckets, transaction));
	}

	/**
	 * Returns subset that aggregates inner record ids by {@link HistogramBucket#getValue()} and thus the result may
	 * look unsorted on first look.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	private HistogramSubSet<T> convertToUnSortedResult(HistogramBucket<T>[] records) {
		return new HistogramSubSet(
			getId(),
			records,
			UNSORTED_AGGREGATION_LAMBDA
		);
	}

	/**
	 * Returns subset that aggregates inner record ids by natural ascending ordering.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	private HistogramSubSet<T> convertToSortedResult(HistogramBucket<T>[] records) {
		return new HistogramSubSet(
			getId(),
			records,
			SORTED_AGGREGATION_LAMBDA
		);
	}

	/**
	 * Searches histogram and select all buckets that fulfill the between `moreThanEq` and `lessThanEq` constraints.
	 * Returns array of all {@link HistogramBucket} in the range.
	 */
	@Nonnull
	private HistogramBucket<T>[] getRecordsInternal(@Nullable T moreThanEq, @Nullable T lessThanEq, @Nonnull BoundsHandling boundsHandling) {
		final HistogramBounds<T> histogramBounds = new HistogramBounds<>(buckets.getArray(), moreThanEq, lessThanEq, boundsHandling);
		@SuppressWarnings("unchecked") final HistogramBucket<T>[] result = new HistogramBucket[histogramBounds.getNormalizedEndIndex() - histogramBounds.getNormalizedStartIndex()];
		int index = -1;
		final Iterator<HistogramBucket<T>> it = buckets.iterator();
		while (it.hasNext()) {
			final HistogramBucket<T> bucket = it.next();
			index++;
			if (index >= histogramBounds.getNormalizedStartIndex() && index < histogramBounds.getNormalizedEndIndex()) {
				result[index - histogramBounds.getNormalizedStartIndex()] = bucket;
			}
			if (index >= histogramBounds.getNormalizedEndIndex()) {
				break;
			}
		}
		return result;
	}

	/**
	 * Represents search mode - i.e. whether records at the very bounds should be included in result or not.
	 */
	private enum BoundsHandling {

		EXCLUSIVE, INCLUSIVE

	}

	/**
	 * Class is used to search for the bucked bounds defined by `moreThanEq` and `lessThanEq` constraints.
	 */
	private static class HistogramBounds<T extends Comparable<T>> {
		/**
		 * Index of the first bucket to be included in search result.
		 */
		@Getter private final int normalizedStartIndex;
		/**
		 * Index of the last bucket to be included in search result.
		 */
		@Getter private final int normalizedEndIndex;

		HistogramBounds(@Nonnull HistogramBucket<T>[] points, @Nullable T moreThanEq, @Nullable T lessThanEq, @Nonnull BoundsHandling boundsHandling) {
			Assert.isTrue(
				moreThanEq == null || lessThanEq == null || moreThanEq.compareTo(lessThanEq) <= 0,
				"From must be lower than to: " + moreThanEq + " vs. " + lessThanEq
			);

			if (moreThanEq != null) {
				final int startIndex = Arrays.binarySearch(points, new HistogramBucket<>(moreThanEq));
				if (boundsHandling == BoundsHandling.EXCLUSIVE) {
					normalizedStartIndex = startIndex >= 0 ? startIndex + 1 : -1 * (startIndex) - 1;
				} else {
					normalizedStartIndex = startIndex >= 0 ? startIndex : -1 * (startIndex) - 1;
				}
			} else {
				normalizedStartIndex = 0;
			}

			if (lessThanEq != null) {
				final int endIndex = Arrays.binarySearch(points, new HistogramBucket<>(lessThanEq));
				if (boundsHandling == BoundsHandling.EXCLUSIVE) {
					normalizedEndIndex = endIndex >= 0 ? endIndex : (-1 * (endIndex) - 1);
				} else {
					normalizedEndIndex = endIndex >= 0 ? endIndex + 1 : (-1 * (endIndex) - 1);
				}
			} else {
				normalizedEndIndex = points.length;
			}
		}

	}
}