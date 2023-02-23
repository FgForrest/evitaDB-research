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

package io.evitadb.index.attribute;

import io.evitadb.api.Transaction;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.dataType.Range;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndexDataStructure;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.bool.TransactionalBoolean;
import io.evitadb.index.histogram.HistogramBucket;
import io.evitadb.index.histogram.HistogramIndex;
import io.evitadb.index.histogram.HistogramSubSet;
import io.evitadb.index.range.RangeIndex;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import io.evitadb.index.transactionalMemory.VoidTransactionMemoryProducer;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.index.FilterIndexStoragePart;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static io.evitadb.api.utils.StringUtils.unknownToString;
import static java.util.Optional.ofNullable;

/**
 * Filter index maintains information about single filterable attribute - its value to record id relation.
 * It uses several data structures to allow filtration - see fields description.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FilterIndex implements VoidTransactionMemoryProducer<FilterIndex>, EntityIndexDataStructure, Serializable {
	private static final long serialVersionUID = -6813305126746774103L;
	public static final String ERROR_RANGE_TYPE_NOT_SUPPORTED = "This filter index doesn't handle Range type!";
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * This is internal flag that tracks whether the index contents became dirty and needs to be persisted.
	 */
	private final TransactionalBoolean dirty;
	/**
	 * Histogram is the main data structure that holds the information about value to record ids relation.
	 */
	@Nonnull @Getter private final HistogramIndex<? extends Comparable<?>> histogram;
	/**
	 * Range index is used only for attribute types that are assignable to {@link Range} and can answer questions like:
	 * <p>
	 * - what records are valid at precise moment
	 * - what records are valid until certain moment
	 * - what records are valid after certain moment
	 */
	@Nullable @Getter private final RangeIndex rangeIndex;
	/**
	 * Value index is auxiliary data structure that allows fast O(1) access to the records of specified value.
	 * Is created on demand on first use.
	 */
	private transient Map<? extends Comparable<?>, Integer> valueIndex;
	/**
	 * This field speeds up all requests for all data in this index (which happens quite often). This formula can be
	 * computed anytime by calling `((HistogramIndex) this.histogram).getSortedRecords(null, null)`. Original operation
	 * needs to perform costly join of all internally held bitmaps and that's why we memoize the result.
	 */
	private transient Formula memoizedAllRecordsFormula;

	private static void verifyValueArray(@Nonnull Object value) {
		isTrue(Serializable.class.isAssignableFrom(value.getClass().getComponentType()), () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Serializable, but it is not!"));
		isTrue(Comparable.class.isAssignableFrom(value.getClass().getComponentType()), () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Comparable, but it is not!"));
	}

	private static void verifyValue(@Nonnull Object value) {
		isTrue(value instanceof Serializable, () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Serializable, but it is not!"));
		isTrue(value instanceof Comparable, () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Comparable, but it is not!"));
	}

	public FilterIndex(Class<?> attributeType) {
		this.dirty = new TransactionalBoolean();
		this.histogram = new HistogramIndex<>();
		this.rangeIndex = Range.class.isAssignableFrom(attributeType) ? new RangeIndex() : null;
	}

	public <T extends Comparable<T>> FilterIndex(@Nonnull HistogramIndex<T> histogram, @Nullable RangeIndex rangeIndex) {
		this.dirty = new TransactionalBoolean();
		this.histogram = histogram;
		this.rangeIndex = rangeIndex;
	}

	/**
	 * Returns count of records in this index.
	 */
	public int size() {
		return histogram.getRecords().getRecordIds().size();
	}

	/**
	 * Returns true if this {@link FilterIndex} instance contains range index.
	 */
	public boolean hasRangeIndex() {
		return this.rangeIndex != null;
	}

	/**
	 * Returns sorted (ascending, natural) collection of all distinct values present in the filter index.
	 */
	public <T extends Comparable<T>> Collection<T> getValues() {
		//noinspection unchecked
		return (Collection<T>) getValueIndex().keySet();
	}

	/**
	 * Registers new record id for passed attribute value.
	 */
	public <T extends Comparable<T>> void addRecord(int recordId, Object value) {
		// if current attribute is Range based assign record also to range index
		if (rangeIndex != null) {
			if (value instanceof Object[]) {
				isTrue(
					Range.class.isAssignableFrom(value.getClass().getComponentType()),
					() -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Range but it is not!")
				);
				final Range[] consolidatedRanges = Range.consolidateRange((Range[]) value);
				for (Range consolidatedRange : consolidatedRanges) {
					rangeIndex.addRecord(consolidatedRange.getFrom(), consolidatedRange.getTo(), recordId);
				}
			} else {
				isTrue(value instanceof Range, () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Range but it is not!"));
				final Range range = (Range) value;
				rangeIndex.addRecord(range.getFrom(), range.getTo(), recordId);
			}
		}

		if (value instanceof Object[]) {
			verifyValueArray(value);
			final Object[] valueArray = (Object[]) value;
			for (Object valueItem : valueArray) {
				addRecordToHistogramAndValueIndex(recordId, (T) valueItem);
			}
		} else {
			verifyValue(value);
			addRecordToHistogramAndValueIndex(recordId, (T) value);
		}

		if (!TransactionalMemory.isTransactionalMemoryAvailable()) {
			this.memoizedAllRecordsFormula = null;
		}
		this.dirty.setToTrue();
	}

	/**
	 * Unregisters record id from passed attribute value.
	 *
	 * @throws IllegalArgumentException when the removed record is not actually registered for the attribute
	 */
	public <T extends Comparable<T>> void removeRecord(int recordId, Object value) {
		// if current attribute is Range based assign record also to range index
		if (this.rangeIndex != null) {
			if (value instanceof Object[]) {
				isTrue(
					Range.class.isAssignableFrom(value.getClass().getComponentType()),
					() -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Range but it is not!")
				);
				final Range[] consolidatedRanges = Range.consolidateRange((Range[]) value);
				for (Range consolidatedRange : consolidatedRanges) {
					this.rangeIndex.removeRecord(consolidatedRange.getFrom(), consolidatedRange.getTo(), recordId);
				}
			} else {
				isTrue(value instanceof Range, () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Range but it is not!"));
				final Range range = (Range) value;
				this.rangeIndex.removeRecord(range.getFrom(), range.getTo(), recordId);
			}
		}

		if (value instanceof Object[]) {
			verifyValueArray(value);
			final Object[] valueArray = (Object[]) value;
			for (Object valueItem : valueArray) {
				removeRecordFromHistogramAndValueIndex(recordId, (T) valueItem);
			}
		} else {
			verifyValue(value);
			removeRecordFromHistogramAndValueIndex(recordId, (T) value);
		}

		if (!TransactionalMemory.isTransactionalMemoryAvailable()) {
			this.memoizedAllRecordsFormula = null;
		}
		this.dirty.setToTrue();
	}

	/**
	 * Returns true if filter index contains no records.
	 */
	public boolean isEmpty() {
		return this.histogram.isEmpty();
	}

	/**
	 * Returns bitmap of all record ids connected with the value in the argument
	 */
	public <T extends Comparable<T>> Bitmap getRecordsEqualTo(T attributeValue) {
		return ofNullable(getValueIndex().get(attributeValue))
			.map(histogram::getRecordsAtIndex)
			.orElse(EmptyBitmap.INSTANCE);
	}

	/**
	 * Returns bitmap of all record ids connected with the value in the argument
	 */
	public <T extends Comparable<T>> Formula getRecordsEqualToFormula(T attributeValue) {
		return ofNullable(getValueIndex().get(attributeValue))
			.map(it -> (Formula) new ConstantFormula(histogram.getRecordsAtIndex(it)))
			.orElse(EmptyFormula.INSTANCE);
	}

	/**
	 * Returns all records present in filter index in the form of {@link HistogramSubSet}.
	 */
	public <T extends Comparable<T>> HistogramSubSet<T> getHistogramOfAllRecords() {
		return ((HistogramIndex) this.histogram).getSortedRecords(null, null);
	}

	/**
	 * Returns all records present in filter index as {@link Bitmap}.
	 */
	public Bitmap getAllRecords() {
		return getAllRecordsFormula().compute();
	}

	/**
	 * Returns all records present in filter index as {@link AbstractFormula}.
	 */
	public Formula getAllRecordsFormula() {
		// if there is transaction open, there might be changes in the histogram data, and we can't easily use cache
		if (TransactionalMemory.isTransactionalMemoryAvailable()) {
			return getHistogramOfAllRecords().getFormula();
		} else {
			if (this.memoizedAllRecordsFormula == null) {
				this.memoizedAllRecordsFormula = getHistogramOfAllRecords().getFormula();
			}
			return this.memoizedAllRecordsFormula;
		}
	}

	/**
	 * Returns all records lesser than or equals attribute value passed in the argument in the form of {@link HistogramSubSet}.
	 */
	public <T extends Comparable<T>> HistogramSubSet<T> getHistogramOfRecordsLesserThanEq(T comparable) {
		return ((HistogramIndex) this.histogram).getSortedRecords(null, comparable);
	}

	/**
	 * Returns all records lesser than or equals attribute value passed in the argument in the form of {@link Bitmap}.
	 */
	public <T extends Comparable<T>> Bitmap getRecordsLesserThanEq(T comparable) {
		return getRecordsLesserThanEqFormula(comparable).compute();
	}

	/**
	 * Returns all records lesser than or equals attribute value passed in the argument in the form of {@link Bitmap}.
	 */
	public <T extends Comparable<T>> Formula getRecordsLesserThanEqFormula(T comparable) {
		return getHistogramOfRecordsLesserThanEq(comparable).getFormula();
	}

	/**
	 * Returns all records greater than or equals attribute value passed in the argument in the form of {@link HistogramSubSet}.
	 */
	public <T extends Comparable<T>> HistogramSubSet<T> getHistogramOfRecordsGreaterThanEq(T comparable) {
		return ((HistogramIndex) this.histogram).getSortedRecords(comparable, null);
	}

	/**
	 * Returns all records greater than or equals attribute value passed in the argument in the form of {@link Bitmap}.
	 */
	public <T extends Comparable<T>> Bitmap getRecordsGreaterThanEq(T comparable) {
		return getRecordsGreaterThanEqFormula(comparable).compute();
	}

	/**
	 * Returns all records greater than or equals attribute value passed in the argument in the form of {@link AbstractFormula}.
	 */
	public <T extends Comparable<T>> Formula getRecordsGreaterThanEqFormula(T comparable) {
		return getHistogramOfRecordsGreaterThanEq(comparable).getFormula();
	}

	/**
	 * Returns all records lesser than attribute value passed in the argument in the form of {@link HistogramSubSet}.
	 */
	public <T extends Comparable<T>> HistogramSubSet<T> getHistogramOfRecordsLesserThan(T comparable) {
		return ((HistogramIndex) this.histogram).getSortedRecordsExclusive(null, comparable);
	}

	/**
	 * Returns all records lesser than attribute value passed in the argument in the form of {@link Bitmap}.
	 */
	public <T extends Comparable<T>> Bitmap getRecordsLesserThan(T comparable) {
		return getRecordsLesserThanFormula(comparable).compute();
	}

	/**
	 * Returns all records lesser than attribute value passed in the argument in the form of {@link AbstractFormula}.
	 */
	public <T extends Comparable<T>> Formula getRecordsLesserThanFormula(T comparable) {
		return getHistogramOfRecordsLesserThan(comparable).getFormula();
	}

	/**
	 * Returns all records greater than attribute value passed in the argument in the form of {@link HistogramSubSet}.
	 */
	public <T extends Comparable<T>> HistogramSubSet<T> getHistogramOfRecordsGreaterThan(T comparable) {
		return ((HistogramIndex) this.histogram).getSortedRecordsExclusive(comparable, null);
	}

	/**
	 * Returns all records greater than attribute value passed in the argument in the form of {@link Bitmap}.
	 */
	public <T extends Comparable<T>> Bitmap getRecordsGreaterThan(T comparable) {
		return getRecordsGreaterThanFormula(comparable).compute();
	}

	/**
	 * Returns all records greater than attribute value passed in the argument in the form of {@link AbstractFormula}.
	 */
	public <T extends Comparable<T>> Formula getRecordsGreaterThanFormula(T comparable) {
		return getHistogramOfRecordsGreaterThan(comparable).getFormula();
	}

	/**
	 * Returns all records with attribute values between `from` and `to` (inclusive) passed in the argument
	 * in the form of {@link HistogramSubSet}.
	 */
	public <T extends Comparable<T>> HistogramSubSet<T> getHistogramOfRecordsBetween(T from, T to) {
		return ((HistogramIndex) this.histogram).getSortedRecords(from, to);
	}

	/**
	 * Returns all records with attribute values between `from` and `to` (inclusive) passed in the argument
	 * in the form of {@link Bitmap}.
	 */
	public <T extends Comparable<T>> Bitmap getRecordsBetween(T from, T to) {
		return getRecordsBetweenFormula(from, to).compute();
	}

	/**
	 * Returns all records with attribute values between `from` and `to` (inclusive) passed in the argument
	 * in the form of {@link AbstractFormula}.
	 */
	public <T extends Comparable<T>> Formula getRecordsBetweenFormula(T from, T to) {
		return getHistogramOfRecordsBetween(from, to).getFormula();
	}

	/**
	 * Returns all records valid at the moment in the form of {@link Bitmap}.
	 * This method can be used only when the attribute type is of the {@link Range} type.
	 */
	public Bitmap getRecordsValidIn(long thePoint) {
		Assert.notNull(this.rangeIndex, ERROR_RANGE_TYPE_NOT_SUPPORTED);
		return getRecordsValidInFormula(thePoint).compute();
	}

	/**
	 * Returns all records valid at the moment in the form of {@link AbstractFormula}.
	 * This method can be used only when the attribute type is of the {@link Range} type.
	 */
	public Formula getRecordsValidInFormula(long thePoint) {
		Assert.notNull(this.rangeIndex, ERROR_RANGE_TYPE_NOT_SUPPORTED);
		return this.rangeIndex.getRecordsWithRangesOutsideInclusive(thePoint, thePoint);
	}

	/**
	 * Returns all records which range overlaps the passed range in the form of {@link Bitmap}.
	 * This method can be used only when the attribute type is of the {@link Range} type.
	 *
	 * @param from * @param to
	 */
	public Bitmap getRecordsOverlapping(long from, long to) {
		Assert.notNull(this.rangeIndex, ERROR_RANGE_TYPE_NOT_SUPPORTED);
		return getRecordsOverlappingFormula(from, to).compute();
	}

	/**
	 * Returns all records which range overlaps the passed range in the form of {@link AbstractFormula}.
	 * This method can be used only when the attribute type is of the {@link Range} type.
	 */
	public Formula getRecordsOverlappingFormula(long from, long to) {
		Assert.notNull(this.rangeIndex, ERROR_RANGE_TYPE_NOT_SUPPORTED);
		return this.rangeIndex.getRecordsWithRangesOverlapping(from, to);
	}

	/**
	 * Method creates container for storing filter index from memory to the persistent storage.
	 */
	@Nullable
	public StoragePart createStoragePart(int entityIndexPrimaryKey, AttributeKey attribute) {
		if (this.dirty.isTrue()) {
			return new FilterIndexStoragePart(entityIndexPrimaryKey, attribute, histogram, rangeIndex);
		} else {
			return null;
		}
	}

	/*
		TransactionalLayerProducer implementation
	 */

	@Override
	public void resetDirty() {
		this.dirty.reset();
	}

	/*
		PRIVATE METHODS
	 */

	@Override
	public void clearTransactionalMemory() {
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.dirty);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.histogram);
		ofNullable(this.rangeIndex).ifPresent(TransactionalMemory::removeTransactionalMemoryLayerIfExists);
	}

	@Override
	public FilterIndex createCopyWithMergedTransactionalMemory(@Nullable Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		// we can safely throw away dirty flag now
		transactionalLayer.removeTransactionalMemoryLayerIfExists(this.dirty);
		return new FilterIndex(
			transactionalLayer.getStateCopyWithCommittedChanges(histogram, transaction),
			rangeIndex == null ? null : transactionalLayer.getStateCopyWithCommittedChanges(rangeIndex, transaction)
		);
	}

	private Map<? extends Comparable<?>, Integer> getValueIndex() {
		if (valueIndex == null) {
			final Map<Comparable<?>, Integer> bucketValueToPositionIndex = createHashMap(histogram.getBucketCount());
			final HistogramBucket<? extends Comparable<?>>[] buckets = histogram.getBuckets();
			for (int i = 0; i < buckets.length; i++) {
				HistogramBucket<? extends Comparable<?>> bucket = buckets[i];
				bucketValueToPositionIndex.put(bucket.getValue(), i);
			}
			this.valueIndex = bucketValueToPositionIndex;
		}
		return valueIndex;
	}

	private <T extends Comparable<T>> void addRecordToHistogramAndValueIndex(int recordId, T value) {
		((HistogramIndex) this.histogram).addRecord(value, recordId);
		this.valueIndex = null;
	}

	private <T extends Comparable<T>> void removeRecordFromHistogramAndValueIndex(int recordId, T value) {
		final int removalIndex = ((HistogramIndex) this.histogram).removeRecord(value, recordId);
		isTrue(removalIndex >= 0, "Sanity check - record not found!");
		this.valueIndex = null;
	}

}
