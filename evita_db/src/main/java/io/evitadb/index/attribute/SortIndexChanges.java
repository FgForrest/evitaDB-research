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

import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ArrayUtils.InsertionPosition;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.index.array.TransactionalObjArray;
import io.evitadb.index.attribute.SortIndex.SortedRecordsSupplier;
import io.evitadb.index.map.TransactionalMemoryMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import static io.evitadb.index.attribute.SortIndex.invert;
import static java.util.Optional.ofNullable;

/**
 * Class contains intermediate computation data structures that speed up access to the {@link SortedRecordsSupplier}
 * implementations and also allow to modify contents of the {@link SortIndex} data. All data inside this class can be
 * safely thrown out and recreated from {@link SortIndex} internal data again.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
public class SortIndexChanges implements Serializable {
	private static final long serialVersionUID = -4791973822619493092L;

	/**
	 * Reference to the {@link SortIndex} this data structure is linked to.
	 */
	private final SortIndex sortIndex;
	/**
	 * Contains information about indexes of the record chunks that belong to {@link SortIndex#sortedRecordsValues}.
	 * This intermediate structure is used only when contents of the {@link SortIndex} are modified. Sort index itself
	 * doesn't contain this information in order to safe precious memory. Even the {@link SortIndex#valueCardinalities}
	 * hold only cardinalities bigger that one so the required space is minimized. This field contains computed data
	 * from {@link SortIndex#sortedRecordsValues} and {@link SortIndex#valueCardinalities} together in an unfolded form.
	 */
	private ValueStartIndex[] valueLocationIndex;
	/**
	 * Cached aggregation of "sorted" results in ascending order - computed as plain aggregation or all record ids
	 * in the histogram from left to right.
	 */
	private SortedRecordsSupplier recordIdToPositions;
	/**
	 * Cached aggregation of "sorted" results in descending order - computed as plain aggregation or all record ids
	 * in the histogram from right to left.
	 */
	private SortedRecordsSupplier recordIdToPositionsReversed;

	public SortIndexChanges(@Nonnull SortIndex sortIndex) {
		this.sortIndex = sortIndex;
	}

	/**
	 * Returns {@link SortedRecordsSupplier} that contains records ids sorted by value in ascending order.
	 * Result of the method is cached and additional calls obtain memoized result.
	 */
	public SortedRecordsSupplier getAscendingOrderRecordsSupplier() {
		return ofNullable(this.recordIdToPositions).orElseGet(() -> {
			this.recordIdToPositions = new SortedRecordsSupplier(
				this.sortIndex.sortedRecords.getArray(),
				this.sortIndex.sortedRecords.getRecordIds(),
				this.sortIndex.sortedRecords.getPositions()
			);
			return this.recordIdToPositions;
		});
	}

	/**
	 * Returns {@link SortedRecordsSupplier} that contains records ids sorted by value in descending order.
	 * Result of the method is cached and additional calls obtain memoized result.
	 */
	public SortedRecordsSupplier getDescendingOrderRecordsSupplier() {
		return ofNullable(this.recordIdToPositionsReversed).orElseGet(() -> {
			this.recordIdToPositionsReversed = new SortedRecordsSupplier(
				ArrayUtils.reverse(this.sortIndex.sortedRecords.getArray()),
				this.sortIndex.sortedRecords.getRecordIds(),
				invert(this.sortIndex.sortedRecords.getPositions())
			);
			return this.recordIdToPositionsReversed;
		});
	}

	/**
	 * Computes record id of the record id that should precede currently inserted record that is associated with passed
	 * `value`. When record id should be placed on the first index {@link Integer#MIN_VALUE} is returned. This aligns
	 * with {@link io.evitadb.index.array.TransactionalUnorderedIntArray#add(int, int)} contract.
	 */
	public int computePreviousRecord(Comparable<?> value, int recordId) {
		final ValueStartIndex[] valueIndex = getValueIndex(sortIndex.sortedRecordsValues, sortIndex.valueCardinalities);
		// compute index of the value in the value index
		final InsertionPosition valueInsertionPosition = ArrayUtils.computeInsertPositionOfObjInOrderedArray(new ValueStartIndex(value, -1), valueIndex);
		final int position = valueInsertionPosition.getPosition();
		// if the value is already part of the index
		if (valueInsertionPosition.isAlreadyPresent()) {
			// compute record id block of the value (block size is equal to value cardinality)
			final ValueStartIndex targetBlock = valueIndex[position];
			final int blockStart = targetBlock.getIndex();
			final int blockEnd = position + 1 < valueIndex.length ? valueIndex[position + 1].getIndex() : this.sortIndex.sortedRecords.getLength();
			final int[] allRecordIds = this.sortIndex.sortedRecords.getArray();
			final int[] recordIdsInBlock = Arrays.copyOfRange(allRecordIds, blockStart, blockEnd);
			// within the block record ids are sorted in natural integer order
			final InsertionPosition recordInsertionPosition = ArrayUtils.computeInsertPositionOfIntInOrderedArray(recordId, recordIdsInBlock);
			// compute the target record id position as block start + relative position in the block
			final int recordPosition = blockStart + recordInsertionPosition.getPosition() - 1;
			// if the record position is negative the record should be placed as first record of the sort index
			return recordPosition >= 0 ? allRecordIds[recordPosition] : Integer.MIN_VALUE;
		} else {
			if (position == 0) {
				// value is not in the index and should be placed as first
				return Integer.MIN_VALUE;
			} else if (position < valueIndex.length) {
				// value is not in the index and should be placed in the middle
				return sortIndex.sortedRecords.get(valueIndex[position].getIndex() - 1);
			} else {
				// value is not in the index and should be placed as last
				return sortIndex.sortedRecords.get(sortIndex.sortedRecords.getLength() - 1);
			}
		}
	}

	/**
	 * Method alters internal data structures when new value (that was not present before) is inserted in the {@link SortIndex}.
	 */
	public void valueAdded(Comparable<?> value) {
		this.recordIdToPositions = null;
		this.recordIdToPositionsReversed = null;
		final ValueStartIndex[] valueIndex = getValueIndex(sortIndex.sortedRecordsValues, sortIndex.valueCardinalities);
		// compute the insertion position in value index
		final InsertionPosition insertionPosition = ArrayUtils.computeInsertPositionOfObjInOrderedArray(new ValueStartIndex(value, -1), valueIndex);
		assertNotPresent(!insertionPosition.isAlreadyPresent(), value);
		// nod place the value in the value index with start position as previous block start + previous value cardinality
		final ValueStartIndex newValue = new ValueStartIndex(value, getStartPositionFor(valueIndex, insertionPosition.getPosition()));
		this.valueLocationIndex = ArrayUtils.insertRecordIntoArray(newValue, valueIndex, insertionPosition.getPosition());
		// update all values after the inserted one - their index should be greater by exactly one inserted record
		for (int i = insertionPosition.getPosition() + 1; i < this.valueLocationIndex.length; i++) {
			this.valueLocationIndex[i].increment();
		}
	}

	/**
	 * Method alters internal data structures when existing value cardinality is incremented in the {@link SortIndex}.
	 */
	public void valueCardinalityIncreased(Comparable<?> value) {
		this.recordIdToPositions = null;
		this.recordIdToPositionsReversed = null;
		final ValueStartIndex[] valueIndex = getValueIndex(sortIndex.sortedRecordsValues, sortIndex.valueCardinalities);
		// find the value in the index
		final int position = Arrays.binarySearch(valueIndex, new ValueStartIndex(value, -1));
		assertNotPresent(position >= 0, value);
		// update this and all values after it - their index should be greater by exactly one inserted record
		for (int i = position + 1; i < valueIndex.length; i++) {
			valueIndex[i].increment();
		}
	}

	/**
	 * Method prepares value index if it hasn't exist yet. It needs to be called before anything in {@link SortIndex}
	 * is changed.
	 */
	public void prepareForRemoval() {
		// force computation of the value index
		getValueIndex(sortIndex.sortedRecordsValues, sortIndex.valueCardinalities);
	}

	/**
	 * Method alters internal data structures when existing value is removed entirely from the {@link SortIndex}.
	 */
	public void valueRemoved(Comparable<?> value) {
		this.recordIdToPositions = null;
		this.recordIdToPositionsReversed = null;
		final ValueStartIndex[] valueIndex = getValueIndex(sortIndex.sortedRecordsValues, sortIndex.valueCardinalities);
		// find the value in the index
		final int position = Arrays.binarySearch(valueIndex, new ValueStartIndex(value, -1));
		assertNotPresent(position >= 0, value);
		// remove it from the value location index
		this.valueLocationIndex = ArrayUtils.removeRecordFromArrayOnIndex(valueIndex, position);
		// update all values after it - their index should be lesser by exactly one inserted record
		for (int i = position; i < this.valueLocationIndex.length; i++) {
			this.valueLocationIndex[i].decrement();
		}
	}

	/**
	 * Method alters internal data structures when existing value cardinality is decremented in the {@link SortIndex}.
	 */
	public void valueCardinalityDecreased(Comparable<?> value) {
		this.recordIdToPositions = null;
		this.recordIdToPositionsReversed = null;
		final ValueStartIndex[] valueIndex = getValueIndex(sortIndex.sortedRecordsValues, sortIndex.valueCardinalities);
		// find the value in the index
		final int position = Arrays.binarySearch(valueIndex, new ValueStartIndex(value, -1));
		assertNotPresent(position >= 0, value);
		// update it and all values after it - their index should be lesser by exactly one inserted record
		for (int i = position + 1; i < valueIndex.length; i++) {
			valueIndex[i].decrement();
		}
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * Computes value index if it hasn't exist yet. Result of this method is memoized. Method computes startung index
	 * (position) of the record ids block that belongs to specific value from {@link SortIndex#sortedRecordsValues} and
	 * {@link SortIndex#valueCardinalities} information.
	 */
	private ValueStartIndex[] getValueIndex(@Nonnull TransactionalObjArray<? extends Comparable<?>> sortedRecordsValues, @Nonnull TransactionalMemoryMap<? extends Comparable<?>, Integer> valueCardinalities) {
		if (this.valueLocationIndex == null) {
			final int valueCount = sortedRecordsValues.getLength();
			final ValueStartIndex[] theValueLocationIndex = new ValueStartIndex[valueCount];
			final Iterator<? extends Comparable<?>> it = sortedRecordsValues.iterator();
			int index = 0;
			int accumulator = 0;
			while (it.hasNext()) {
				final Comparable<?> value = it.next();
				theValueLocationIndex[index++] = new ValueStartIndex(value, accumulator);
				accumulator += ofNullable(valueCardinalities.get(value)).orElse(1);
			}
			this.valueLocationIndex = theValueLocationIndex;
		}
		return this.valueLocationIndex;
	}

	/**
	 * Computes start position for value at specified position in value index. The position is computed from previous
	 * value start position and previous value cardinality.
	 */
	private int getStartPositionFor(ValueStartIndex[] valueIndex, int position) {
		if (position == 0) {
			return 0;
		} else {
			final ValueStartIndex previousPosition = valueIndex[position - 1];
			final int previousPositionStart = previousPosition.getIndex();
			final Integer cardinality = ofNullable(this.sortIndex.valueCardinalities.get(previousPosition.getValue())).orElse(1);
			return previousPositionStart + cardinality;
		}
	}

	private void assertNotPresent(boolean present, @Nonnull Comparable<?> value) {
		Assert.isTrue(present, "Value `" + StringUtils.unknownToString(value) + "` unexpectedly found in value start index!");
	}

	/**
	 * Class that maintains information about record id block for certain value.
	 */
	@AllArgsConstructor
	private static class ValueStartIndex implements Comparable<ValueStartIndex>, Serializable {
		private static final long serialVersionUID = -4953895484396265436L;

		/**
		 * The comparable value.
		 */
		@Getter private final Comparable<?> value;
		/**
		 * Start index of the record id block in the {@link SortIndex#sortedRecords} for this value.
		 */
		@Getter private int index;

		/**
		 * Increments start index of the block.
		 */
		public void increment() {
			this.index++;
		}

		/**
		 * Decrements start index of the block.
		 */
		public void decrement() {
			this.index--;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public int compareTo(ValueStartIndex o) {
			return ((Comparable) value).compareTo(o.value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ValueStartIndex that = (ValueStartIndex) o;
			return value.equals(that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public String toString() {
			return value + ", " + index + '+';
		}

	}

}
