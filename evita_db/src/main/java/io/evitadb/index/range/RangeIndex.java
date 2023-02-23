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

package io.evitadb.index.range;

import io.evitadb.api.Transaction;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.array.TransactionalComplexObjArray;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.VoidTransactionMemoryProducer;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * RangeIndex has following structure:
 * <p>
 * [long - threshold 1]: starts [ recordId1, recordId2 ], ends []
 * [long - threshold 2]: starts [ recordId3 ], ends []
 * [long - threshold 3]: starts [], ends [ recordId3 ]
 * [long - threshold 4]: starts [], ends [ recordId1, recordId2 ]
 * <p>
 * And allows to compute which record ids are valid at the certain point (or at the virtual point between points),
 * which records are valid from certain point forwards, which records are valid until certain point and so on.
 * See methods on this data structure.
 * <p>
 * Beware - single record id may have multiple ranges in this data structure, but client code must ensure that
 * from/to combinations for the record are unique - i.e. that the single record id doesn't share same border.
 * Avoid following combinations for ranges of the SAME record:
 * <p>
 * (2-10)(10-20) - ten is shared
 * (2-20)(2-40) - second is shared
 * (2-10)(5-10) - ten is shared
 * <p>
 * This situation will lead to problems when such record is removed because on removal it removes the shared border
 * information for all ranges.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
public class RangeIndex implements VoidTransactionMemoryProducer<RangeIndex>, Serializable {
	private static final long serialVersionUID = -6580254774575839798L;

	/**
	 * Function combines two {@link TransactionalRangePoint}s to new one that will hold combination of their starts
	 * and ends. Threshold is not checked by the function and caller must ensure this function produces sane result.
	 */
	private static final BiConsumer<TransactionalRangePoint, TransactionalRangePoint> INT_RANGE_POINT_PRODUCER = (target, source) -> {
		target.addStarts(source.getStarts().getArray());
		target.addEnds(source.getEnds().getArray());
	};
	/**
	 * Function subtracts second argument {@link TransactionalRangePoint} from first {@link TransactionalRangePoint}.
	 * So that first point contains no record id in the start/end set that is present in the second point.
	 * Threshold is not checked by the function and caller must ensure this function produces sane result.
	 */
	private static final BiConsumer<TransactionalRangePoint, TransactionalRangePoint> INT_RANGE_POINT_REDUCER = (target, source) -> {
		target.removeStarts(source.getStarts().getArray());
		target.removeEnds(source.getEnds().getArray());
	};
	/**
	 * Predicate will return true if point has no sense because it contains no data (no starts, no ends). Predicate will
	 * never return true for full range border points (MIN/MAX) even if empty.
	 */
	private static final Predicate<TransactionalRangePoint> INT_RANGE_POINT_OBSOLETE_CHECKER =
		point -> point.getThreshold() != Long.MIN_VALUE && point.getThreshold() != Long.MAX_VALUE && point.getStarts().isEmpty() && point.getEnds().isEmpty();

	/**
	 * Predicate will return true if both range points are deeply equals
	 */
	private static final BiPredicate<TransactionalRangePoint, TransactionalRangePoint> INT_RANGE_POINT_DEEP_COMPARATOR =
		TransactionalRangePoint::deepEquals;

	/**
	 * Contains range information sorted by {@link RangePoint#getThreshold()} in ascending order.
	 * At least two points are always present for MIN and MAX point of the range.
	 */
	final TransactionalComplexObjArray<TransactionalRangePoint> ranges;

	public RangeIndex(TransactionalRangePoint[] ranges) {
		Assert.isTrue(ranges.length >= 2, "At least two ranges are expected!");
		Assert.isTrue(ranges[0].getThreshold() == Long.MIN_VALUE, "First range should have threshold Long.MIN_VALUE!");
		Assert.isTrue(ranges[ranges.length - 1].getThreshold() == Long.MAX_VALUE, "Last range should have threshold Long.MAX_VALUE!");
		assertThresholdIsMonotonic(ranges);
		this.ranges = new TransactionalComplexObjArray<>(
			ranges,
			INT_RANGE_POINT_PRODUCER,
			INT_RANGE_POINT_REDUCER,
			INT_RANGE_POINT_OBSOLETE_CHECKER,
			INT_RANGE_POINT_DEEP_COMPARATOR
		);
	}

	public RangeIndex() {
		this.ranges = new TransactionalComplexObjArray<>(
			new TransactionalRangePoint[]{
				new TransactionalRangePoint(Long.MIN_VALUE),
				new TransactionalRangePoint(Long.MAX_VALUE)
			},
			INT_RANGE_POINT_PRODUCER,
			INT_RANGE_POINT_REDUCER,
			INT_RANGE_POINT_OBSOLETE_CHECKER,
			INT_RANGE_POINT_DEEP_COMPARATOR
		);
	}

	public RangeIndex(long from, long to, int[] recordIds) {
		this.ranges = new TransactionalComplexObjArray<>(
			new TransactionalRangePoint[]{
				new TransactionalRangePoint(Long.MIN_VALUE),
				new TransactionalRangePoint(Long.MAX_VALUE)
			},
			INT_RANGE_POINT_PRODUCER,
			INT_RANGE_POINT_REDUCER,
			INT_RANGE_POINT_OBSOLETE_CHECKER,
			INT_RANGE_POINT_DEEP_COMPARATOR
		);
		for (int recordId : recordIds) {
			addRecord(from, to, recordId);
		}
	}

	/**
	 * Returns all ranges registered in this index.
	 */
	public RangePoint<?>[] getRanges() {
		return this.ranges.getArray();
	}

	/**
	 * Adds new record with the interval from/to to the range.
	 */
	public void addRecord(long from, long to, int recordId) {
		final Bitmap recArray = new BaseBitmap(recordId);
		this.ranges.add(new TransactionalRangePoint(from, recArray, EmptyBitmap.INSTANCE));
		this.ranges.add(new TransactionalRangePoint(to, EmptyBitmap.INSTANCE, recArray));
	}

	/**
	 * Removes record with the interval from/to from the range.
	 */
	public void removeRecord(long start, long end, int recordId) {
		final Bitmap recArray = new BaseBitmap(recordId);
		this.ranges.remove(new TransactionalRangePoint(start, recArray, EmptyBitmap.INSTANCE));
		this.ranges.remove(new TransactionalRangePoint(end, EmptyBitmap.INSTANCE, recArray));
	}

	/**
	 * Returns true if the range contains passed record id anywhere in its {@link #ranges}.
	 */
	public boolean contains(int recordId) {
		final Iterator<TransactionalRangePoint> it = this.ranges.iterator();
		while (it.hasNext()) {
			final TransactionalRangePoint point = it.next();
			if (point.getStarts().contains(recordId)) {
				return true;
			}
			if (point.getEnds().contains(recordId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns formula that computes records that are valid since the passed point = threshold in range (inclusive).
	 * This method handles even multiple validity spans for single record id providing that they don't overlap.
	 * The computation is based on starts and end of their validity ranges. Record is valid when there is single
	 * end threshold and not even single start for the same record.
	 * <p>
	 * We also need to avoid situation when there is another full range after the actual one. This situation is solved
	 * by combining {@link JoinFormula} - which is something like OR join that leaves duplicate record ids in place.
	 * After that {@link DisentangleFormula} excludes all record ids that are in both bitmaps on the same place. This
	 * operation will exclude all ranges that both start and ends after examined range.
	 */
	public Formula getRecordsFrom(long threshold) {
		final int index = ranges.indexOf(new TransactionalRangePoint(threshold));
		final int startIndex = index >= 0 ? index : -1 * (index) - 1;

		final StartsEndsDTO startsEndsDTO = collectsStartsAndEnds(startIndex, ranges.getLength() - 1, ranges);
		return new DisentangleFormula(
			new JoinFormula(getId(), startsEndsDTO.getRangeEndsAsBitmapArray()),
			new JoinFormula(getId(), startsEndsDTO.getRangeStartsAsBitmapArray())
		);
	}

	/**
	 * Returns formula that computes records that are valid until passed point = threshold in range (inclusive).
	 * This method handles even multiple validity spans for single record id providing that they don't overlap.
	 * The computation is based on starts and end of their validity ranges. Record is valid when there is single
	 * start threshold and not even single end for the same record.
	 * <p>
	 * We also need to avoid situation when there is another full range before the actual one. This situation is solved
	 * by combining {@link JoinFormula} - which is something like OR join that leaves duplicate record ids in place.
	 * After that {@link DisentangleFormula} excludes all record ids that are in both bitmaps on the same place. This
	 * operation will exclude all ranges that both start and ends after examined range.
	 */
	public Formula getRecordsTo(long threshold) {
		final int index = ranges.indexOf(new TransactionalRangePoint(threshold));
		final int startIndex = index >= 0 ? index : -1 * (index) - 2;

		final StartsEndsDTO startsEndsDTO = collectsStartsAndEnds(0, startIndex, ranges);
		return new DisentangleFormula(
			new JoinFormula(getId(), startsEndsDTO.getRangeStartsAsBitmapArray()),
			new JoinFormula(getId(), startsEndsDTO.getRangeEndsAsBitmapArray())
		);
	}

	/**
	 * Method returns formula that computes all records which range is fully contained by passed enclosing range. It is
	 * optimized for using with MPTT algorithm where ranges never overlap and are fully contained one in another - creating
	 * form of range tree.
	 * <p>
	 * Computation doesn't support records with multiple ranges and also will not return record ids whose range only starts
	 * or ends within the passed range. Both start and end must be within in order record id is returned.
	 * <p>
	 * Formula returns also records that are directly bound to the `from`/`to` range. That means that in MPTT structure
	 * it returns the records tied to the parent node and all children nodes.
	 */
	public Formula getRecordsWithRangesWithinRangeIncludingDirectBounds(long from, long to) {
		final RangeLookup rangeLookup = new RangeLookup(ranges, from, to);
		final StartsEndsDTO startsEndsDTO = collectsStartsAndEnds(rangeLookup.getStartIndex(), rangeLookup.getEndIndex(), ranges);
		return collectRecordsWithRangesWithin(rangeLookup, true, startsEndsDTO);
	}

	/**
	 * Method returns formula that computes all records which range is fully contained by passed enclosing range. It is
	 * optimized for using with MPTT algorithm where ranges never overlap and are fully contained one in another - creating
	 * form of range tree.
	 * <p>
	 * Computation doesn't support records with multiple ranges and also will not return record ids whose range only starts
	 * or ends within the passed range. Both start and end must be within in order record id is returned.
	 * <p>
	 * Formula doesn't return records that are directly bound to the `from`/`to` range. That means that in MPTT structure
	 * it returns only the record tied to the children nodes but not the records tied directly to the parent node which
	 * range is passed in the `from`/`to` arguments.
	 */
	public Formula getRecordsWithRangesWithinRangeExcludingDirectBounds(long from, long to) {
		final RangeLookup rangeLookup = new RangeLookup(ranges, from, to);
		final StartsEndsDTO startsEndsDTO = collectsStartsAndEnds(rangeLookup.getStartIndex(), rangeLookup.getEndIndex(), ranges);
		return collectRecordsWithRangesWithin(rangeLookup, false, startsEndsDTO);
	}

	/**
	 * Method returns formula that computes records which range is exactly equal to passed bounds `from`/`to`. It is
	 * computed as simple AND of records which start range is registered for `from` and end range for `to`.
	 */
	public Formula getRecordsWithExactRange(long from, long to) {
		final RangeLookup rangeLookup = new RangeLookup(ranges, from, to);
		return new AndFormula(
			new long[] {getId()},
			rangeLookup.getExactStarts(),
			rangeLookup.getExactEnds()
		);
	}

	/**
	 * Method returns formula that computes all records which range fully envelopes passed range with `from` and `to` bounds.
	 * It is optimized for using with MPTT algorithm where ranges never overlap and are fully contained one in another -
	 * creating form of range tree. This method can find all parents of the node inside MPTT tree.
	 *
	 * This method doesn't support multiple ranges for single record id.
	 *
	 * Method finds all records which start range is before `from` and end range is after `to` argument. Records with
	 * the exact range of `from`/`to` are not part of the result.
	 */
	public Formula getRecordsWithRangesOutsideMpttExclusiveBounds(long from, long to) {
		final RangeLookup rangeLookup = new RangeLookup(ranges, from, to);

		final StartsEndsDTO lesserBoundsDTO = collectsStartsAndEnds(0, rangeLookup.getStartIndex(), ranges);
		final StartsEndsDTO greaterBoundsDTO = collectsStartsAndEnds(rangeLookup.getEndIndex(), ranges.getLength() - 1, ranges);

		return new NotFormula(
			new AndFormula(
				new long[] {getId()},
				rangeLookup.getExactStarts(),
				rangeLookup.getExactEnds()
			),
			new AndFormula(
				new NotFormula(
					lesserBoundsDTO.getRangeEnds(),
					lesserBoundsDTO.getRangeStarts()
				),
				new NotFormula(
					greaterBoundsDTO.getRangeStarts(),
					greaterBoundsDTO.getRangeEnds()
				)
			)
		);
	}

	/**
	 * Method returns formula that computes all records which range fully envelopes passed range with `from` and `to` bounds.
	 *
	 * This method supports multiple ranges for the same id.
	 *
	 * Method finds all records which start range is before `from` and end range is after `to` argument. Records with
	 * the exact range of `from`/`to` are part of the result.
	 */
	public Formula getRecordsWithRangesOutsideInclusive(long from, long to) {
		final RangeLookup rangeLookup = new RangeLookup(ranges, from, to);

		final int startIndex = rangeLookup.isStartThresholdFound() ? rangeLookup.getStartIndex() : rangeLookup.getStartIndex() - 1;
		final int endIndex = rangeLookup.isEndThresholdFound() ? rangeLookup.getEndIndex() + 1 : rangeLookup.getEndIndex();

		final StartsEndsDTO lesserBoundsDTO = startIndex >= 0 ?
			collectsStartsAndEnds(0, startIndex, ranges) : new StartsEndsDTO();
		final StartsEndsDTO greaterBoundsDTO = endIndex < ranges.getLength() ?
			collectsStartsAndEnds(endIndex, ranges.getLength() - 1, ranges) : new StartsEndsDTO();

		return new AndFormula(
			new DisentangleFormula(
				new JoinFormula(getId(), lesserBoundsDTO.getRangeStartsAsBitmapArray()),
				new JoinFormula(getId(), lesserBoundsDTO.getRangeEndsAsBitmapArray())
			),
			new DisentangleFormula(
				new JoinFormula(getId(), greaterBoundsDTO.getRangeEndsAsBitmapArray()),
				new JoinFormula(getId(), greaterBoundsDTO.getRangeStartsAsBitmapArray())
			)
		);
	}

	/**
	 * Method returns formula that computes all records which range overlap (have points in common)	passed range with
	 * `from` and `to` bounds.
	 *
	 * Method finds all records which start range is before `from` and ends after or equal to `from` or
	 * which ends after `from` but before or equal to `to`.
	 */
	public Formula getRecordsWithRangesOverlapping(long from, long to) {
		final RangeLookup rangeLookup = new RangeLookup(ranges, from, to);
		final StartsEndsDTO between = collectsStartsAndEnds(rangeLookup.getStartIndex(), rangeLookup.getEndIndex(), ranges);
		final StartsEndsDTO before = collectsStartsAndEnds(0, Math.min(rangeLookup.getStartIndex(), rangeLookup.getEndIndex()), ranges);
		final StartsEndsDTO after = collectsStartsAndEnds(Math.max(rangeLookup.getStartIndex(), rangeLookup.getEndIndex()), ranges.getLength() - 1, ranges);

		return new OrFormula(
			between.getRangeStarts(),
			between.getRangeEnds(),
			new AndFormula(
				new DisentangleFormula(
					new JoinFormula(getId(), before.getRangeStartsAsBitmapArray()),
					new JoinFormula(getId(), before.getRangeEndsAsBitmapArray())
				),
				new DisentangleFormula(
					new JoinFormula(getId(), after.getRangeEndsAsBitmapArray()),
					new JoinFormula(getId(), after.getRangeStartsAsBitmapArray())
				)
			)
		);
	}

	/**
	 * Returns record ids of all records in this index.
	 */
	public Bitmap getAllRecords() {
		final StartsEndsDTO all = collectsStartsAndEnds(0, ranges.getLength() - 1, ranges);
		return new AndFormula(all.getRangeStarts(), all.getRangeEnds()).compute();
	}

	/**
	 * Returns count of record ids in range index.
	 */
	public int size() {
		return getAllRecords().size();
	}

	/*
		TRANSACTIONAL MEMORY implementation
	 */

	@Override
	public RangeIndex createCopyWithMergedTransactionalMemory(Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		return new RangeIndex(transactionalLayer.getStateCopyWithCommittedChanges(ranges, transaction));
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * Method collects all starts and ends from ranges between fromIndex and toIndex (inclusive) and returns them collected
	 * in simple DTO.
	 */
	static StartsEndsDTO collectsStartsAndEnds(int fromIndex, int toIndex, TransactionalComplexObjArray<TransactionalRangePoint> ranges) {
		final StartsEndsDTO result = new StartsEndsDTO();
		for (int i = fromIndex; i <= toIndex; i++) {
			final RangePoint<?> rangePoint = ranges.get(i);
			result.addStart(rangePoint.getStarts());
			result.addEnd(rangePoint.getEnds());
		}
		return result;
	}

	/**
	 * Collects all starts and end record ids arrays inside the `rangeLookup`, respecting `includingParent` (inclusion)
	 * settings.
	 */
	private Formula collectRecordsWithRangesWithin(RangeLookup rangeLookup, boolean includingParent, StartsEndsDTO startsEndsDTO) {
		if (includingParent || !rangeLookup.hasBothPoints()) {
			return new AndFormula(
				startsEndsDTO.getRangeStarts(),
				startsEndsDTO.getRangeEnds()
			);
		} else {
			return new OrFormula(
				new AndFormula(
					startsEndsDTO.getRangeStartsSubset(1, startsEndsDTO.getRangeStartsSize()),
					startsEndsDTO.getRangeEnds()
				),
				new AndFormula(
					startsEndsDTO.getRangeStarts(),
					startsEndsDTO.getRangeEndsSubset(0, startsEndsDTO.getRangeEndsSize() - 1)
				)
			);
		}
	}

	/**
	 * Method throws {@link IllegalArgumentException} when ranges are not in ascending order or contains duplicate threshold.
	 */
	private static void assertThresholdIsMonotonic(RangePoint<?>[] ranges) {
		Long previous = null;
		for (RangePoint<?> point : ranges) {
			Assert.isTrue(
				previous == null || previous < point.getThreshold(),
				"Range values are not monotonic - conflicting values: " + previous + ", " + point.getThreshold()
			);
			previous = point.getThreshold();
		}
	}

	/**
	 * DTO for passing sets of bitmap starts and ends from index structure to computational logic.
	 */
	@NoArgsConstructor
	static class StartsEndsDTO {
		private static final Formula[] EMPTY_ARRAY = new Formula[0];
		private final List<Formula> rangeStarts = new LinkedList<>();
		private final List<Formula> rangeEnds = new LinkedList<>();

		StartsEndsDTO(List<Bitmap> starts, List<Bitmap> ends) {
			for (Bitmap start : starts) {
				addStart(start);
			}
			for (Bitmap end : ends) {
				addEnd(end);
			}
		}

		/**
		 * Returns formula that computes bitmap of distinct record ids that are present at collected start ranges.
		 */
		public Formula getRangeStarts() {
			if (rangeStarts.isEmpty()) {
				return EmptyFormula.INSTANCE;
			} else if (rangeStarts.size() == 1) {
				return rangeStarts.get(0);
			} else {
				return new OrFormula(
					rangeStarts.toArray(EMPTY_ARRAY)
				);
			}
		}

		/**
		 * Returns formula that computes bitmap of distinct record ids that are present at collected start ranges in
		 * part (sub array) specified by indexes in argument.
		 */
		public Formula getRangeStartsSubset(int fromIndex, int toIndex) {
			final List<Formula> subList = rangeStarts.subList(fromIndex, toIndex);
			if (subList.isEmpty()) {
				return EmptyFormula.INSTANCE;
			} else if (subList.size() == 1) {
				return subList.get(0);
			} else {
				return new OrFormula(
					subList.toArray(EMPTY_ARRAY)
				);
			}
		}

		/**
		 * Returns count of collected start ranges.
		 */
		public int getRangeStartsSize() {
			return rangeStarts.size();
		}

		/**
		 * Returns formula that computes bitmap of distinct record ids that are present at collected end ranges.
		 */
		public Formula getRangeEnds() {
			if (rangeEnds.isEmpty()) {
				return EmptyFormula.INSTANCE;
			} else if (rangeEnds.size() == 1) {
				return rangeEnds.get(0);
			} else {
				return new OrFormula(
					rangeEnds.toArray(EMPTY_ARRAY)
				);
			}
		}

		/**
		 * Returns formula that computes bitmap of distinct record ids that are present at collected end ranges in
		 * part (sub array) specified by indexes in argument.
		 */
		public Formula getRangeEndsSubset(int fromIndex, int toIndex) {
			final List<Formula> subList = rangeEnds.subList(fromIndex, toIndex);
			if (subList.isEmpty()) {
				return EmptyFormula.INSTANCE;
			} else if (subList.size() == 1) {
				return subList.get(0);
			} else {
				return new OrFormula(
					subList.toArray(EMPTY_ARRAY)
				);
			}
		}

		/**
		 * Returns count of collected end ranges.
		 */
		public int getRangeEndsSize() {
			return rangeEnds.size();
		}

		/**
		 * Returns array of bitmaps of distinct record ids that are present at collected start ranges. All added formulas
		 * so far must be of simple {@link ConstantFormula} type otherwise this method returns {@link IllegalArgumentException}
		 *
		 * @throws IllegalArgumentException when {@link #addStart(Formula)} was called with complex formula
		 */
		public Bitmap[] getRangeStartsAsBitmapArray() {
			return this.rangeStarts
				.stream()
				.map(it -> {
					if (it instanceof EmptyFormula) {
						return EmptyBitmap.INSTANCE;
					} else {
						Assert.isTrue(it instanceof ConstantFormula, "StartsEndsDTO is expected to contain only ConstantFormula when indistinct values are required. Encountered " + it.getClass());
						return ((ConstantFormula) it).getDelegate();
					}
				})
				.toArray(Bitmap[]::new);
		}

		/**
		 * Returns array of bitmaps of distinct record ids that are present at collected end ranges. All added formulas
		 * so far must be of simple {@link ConstantFormula} type otherwise this method returns {@link IllegalArgumentException}
		 *
		 * @throws IllegalArgumentException when {@link #addEnd(Bitmap)} was called with complex formula
		 */
		public Bitmap[] getRangeEndsAsBitmapArray() {
			return this.rangeEnds
				.stream()
				.map(it -> {
					if (it instanceof EmptyFormula) {
						return EmptyBitmap.INSTANCE;
					} else {
						Assert.isTrue(it instanceof ConstantFormula, "StartsEndsDTO is expected to contain only ConstantFormula when indistinct values are required. Encountered " + it.getClass());
						return ((ConstantFormula) it).getDelegate();
					}
				})
				.toArray(Bitmap[]::new);
		}

		/**
		 * Returns true if StartsEndsDTO is contents wise effectively equal to passed one.
		 */
		public boolean effectivelyEquals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			StartsEndsDTO that = (StartsEndsDTO) o;
			final int[][] thisStarts = rangeStarts.stream().map(it -> it.compute().getArray()).toArray(int[][]::new);
			final int[][] thatStarts = that.rangeStarts.stream().map(it -> it.compute().getArray()).toArray(int[][]::new);
			if (thisStarts.length != thatStarts.length) {
				return false;
			}
			for (int i = 0; i < thisStarts.length; i++) {
				int[] thisStart = thisStarts[i];
				int[] thatStart = thatStarts[i];
				if (!Arrays.equals(thisStart, thatStart)) {
					return false;
				}
			}
			final int[][] thisEnds = rangeEnds.stream().map(it -> it.compute().getArray()).toArray(int[][]::new);
			final int[][] thatEnds = that.rangeEnds.stream().map(it -> it.compute().getArray()).toArray(int[][]::new);
			if (thisEnds.length != thatEnds.length) {
				return false;
			}
			for (int i = 0; i < thisEnds.length; i++) {
				int[] thisEnd = thisEnds[i];
				int[] thatEnd = thatEnds[i];
				if (!Arrays.equals(thisEnd, thatEnd)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			return Objects.hash(rangeStarts, rangeEnds);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			StartsEndsDTO that = (StartsEndsDTO) o;
			return rangeStarts.equals(that.rangeStarts) && rangeEnds.equals(that.rangeEnds);
		}

		@Override
		public String toString() {
			final Function<List<Formula>, String> cnv = ints -> ints.stream()
				.map(it -> "[" + it.toString() + "]")
				.collect(Collectors.joining(","));
			return "StartsEndsDTO{" +
				"rangeStarts=" + cnv.apply(rangeStarts) +
				", rangeEnds=" + cnv.apply(rangeEnds) +
				'}';
		}

		/**
		 * Adds new formula to the set of start ranges.
		 */
		void addStart(Formula starts) {
			this.rangeStarts.add(starts);
		}

		/**
		 * Adds new formula to the set of end ranges.
		 */
		void addEnd(Formula ends) {
			this.rangeEnds.add(ends);
		}

		/**
		 * Adds new bitmap as simple {@link ConstantFormula} to the set of start ranges.
		 */
		void addStart(Bitmap starts) {
			if (starts.isEmpty()) {
				this.rangeStarts.add(EmptyFormula.INSTANCE);
			} else {
				this.rangeStarts.add(new ConstantFormula(starts));
			}
		}

		/**
		 * Adds new bitmap as simple {@link ConstantFormula} to the set of end ranges.
		 */
		void addEnd(Bitmap ends) {
			if (ends.isEmpty()) {
				this.rangeEnds.add(EmptyFormula.INSTANCE);
			} else {
				this.rangeEnds.add(new ConstantFormula(ends));
			}
		}
	}

	/**
	 * Range lookup will find and return positions of the `from` / `to` ranges in the `ranges` array. It computes their
	 * indexes and will provide access to the set of records in form of {@link TransactionalRangePoint} at those indexes
	 * for access to directly assigned records at these bounds.
	 */
	@Data
	static class RangeLookup {
		private final int startIndex;
		private final TransactionalRangePoint startPoint;
		private final int endIndex;
		private final TransactionalRangePoint endPoint;

		RangeLookup(TransactionalComplexObjArray<TransactionalRangePoint> ranges, long from, long to) {
			final int indexFrom = ranges.indexOf(new TransactionalRangePoint(from));
			if (indexFrom >= 0) {
				startIndex = indexFrom;
				startPoint = ranges.get(indexFrom);
			} else {
				startIndex = -1 * (indexFrom) - 1;
				startPoint = null;
			}

			if (from == to) {
				endIndex = startIndex;
				endPoint = startPoint;
			} else {
				final int indexTo = ranges.indexOf(new TransactionalRangePoint(to));
				if (indexTo >= 0) {
					endIndex = indexTo;
					endPoint = ranges.get(indexTo);
				} else {
					endIndex = -1 * (indexTo) - 2;
					endPoint = null;
				}
			}
		}

		/**
		 * Returns true if both points - start and end were found in the index.
		 */
		boolean hasBothPoints() {
			return startPoint != null && endPoint != null;
		}

		/**
		 * Returns true if start point was found in the index.
		 */
		boolean isStartThresholdFound() {
			return startPoint != null;
		}

		/**
		 * Returns true if end point was found in the index.
		 */
		boolean isEndThresholdFound() {
			return endPoint != null;
		}

		/**
		 * Returns bitmap of records that starts exactly at the start of the looked up range.
		 */
		Bitmap getExactStarts() {
			return startPoint == null ? EmptyBitmap.INSTANCE : startPoint.getStarts();
		}

		/**
		 * Returns bitmap of records that ends exactly at the end of the looked up range.
		 */
		Bitmap getExactEnds() {
			return endPoint == null ? EmptyBitmap.INSTANCE : endPoint.getEnds();
		}
	}

}
