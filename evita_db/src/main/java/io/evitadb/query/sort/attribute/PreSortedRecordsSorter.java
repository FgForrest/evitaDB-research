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

package io.evitadb.query.sort.attribute;

import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.sort.SortedRecordsSupplierFactory.SortedRecordsSupplier;
import io.evitadb.query.sort.Sorter;
import io.evitadb.query.sort.utils.SortUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.roaringbitmap.BatchIterator;
import org.roaringbitmap.RoaringBatchIterator;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.RoaringBitmapWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * This sorter sorts {@link AbstractFormula#compute()} result according to {@link SortedRecordsSupplier} that contains information
 * about record ids in already sorted fashion. This sorter executes following operation:
 *
 * - creates mask of positions in presorted array that refer to the {@link AbstractFormula#compute()} result
 * - copies slice of record ids in the presorted array that conform to the mask (in the order of presorted array)
 * the slice respects requested start and end index
 * - if the result is complete it is returned, if there is space left and there were record ids that were not found
 * in presorted array append these to the tail of the result until end index is reached or the set is exhausted
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class PreSortedRecordsSorter implements Sorter {
	private static final int[] EMPTY_RESULT = new int[0];
	/**
	 * This instance will be used by this sorter to access pre sorted arrays of entities.
	 */
	private final SortedRecordsSupplier sortedRecordsSupplier;
	/**
	 * This sorter instance will be used for sorting entities, that cannot be sorted by this sorter.
	 */
	private final Sorter unknownRecordIdsSorter;

	public PreSortedRecordsSorter(SortedRecordsSupplier sortedRecordsSupplier) {
		this.sortedRecordsSupplier = sortedRecordsSupplier;
		this.unknownRecordIdsSorter = null;
	}

	@Nonnull
	@Override
	public Sorter andThen(Sorter sorterForUnknownRecords) {
		return new PreSortedRecordsSorter(
			sortedRecordsSupplier,
			sorterForUnknownRecords
		);
	}

	@Nullable
	@Override
	public Sorter getNextSorter() {
		return unknownRecordIdsSorter;
	}

	@Override
	public int[] sortAndSlice(@Nonnull Formula input, int startIndex, int endIndex) {
		final Bitmap selectedRecordIds = input.compute();
		if (selectedRecordIds.isEmpty()) {
			return EMPTY_RESULT;
		} else {
			final MaskResult positions = getMask(selectedRecordIds);
			final SortResult sortPartialResult = fetchSlice(selectedRecordIds.size(), positions.getMask(), startIndex, endIndex);
			return returnResultAppendingUnknown(positions, sortPartialResult, startIndex, endIndex);
		}
	}

	/**
	 * Maps positions to the record ids in presorted set respecting start and end index.
	 */
	@Nonnull
	private SortResult fetchSlice(int recordsFound, RoaringBitmap positions, int startIndex, int endIndex) {
		final int[] buffer = new int[512];

		final RoaringBatchIterator batchIterator = positions.getBatchIterator();
		final int[] preSortedRecordIds = sortedRecordsSupplier.getSortedRecordIds();
		final int length = Math.min(endIndex - startIndex, recordsFound);
		if (length < 0) {
			throw new IndexOutOfBoundsException("Index: " + startIndex + ", Size: " + sortedRecordsSupplier.getRecordCount());
		}
		final int[] sortedResult = new int[length];
		int inputPeak = 0;
		int previousInputPeak;
		int outputPeak = 0;
		while (batchIterator.hasNext()) {
			final int read = batchIterator.nextBatch(buffer);
			if (read == 0) {
				// no more results break early
				break;
			}
			previousInputPeak = inputPeak;
			inputPeak += read;
			// skip previous pages quickly
			if (inputPeak >= startIndex) {
				// copy records for page
				for (int i = startIndex - previousInputPeak; i < read && i < (endIndex - previousInputPeak); i++) {
					sortedResult[outputPeak++] = preSortedRecordIds[buffer[i]];
				}
				startIndex = inputPeak;
			}
			// finish - page was read
			if (inputPeak >= endIndex) {
				break;
			}
		}
		return new SortResult(sortedResult, outputPeak);
	}

	/**
	 * Returns mask of the positions in the presorted array that matched the computational result
	 * Mask also contains record ids not found in presorted record index.
	 */
	private MaskResult getMask(Bitmap selectedRecordIds) {
		final int[] bufferA = new int[512];
		final int[] bufferB = new int[512];

		final int selectedRecordCount = selectedRecordIds.size();
		final Bitmap unsortedRecordIds = sortedRecordsSupplier.getAllRecords();
		final int[] recordPositions = sortedRecordsSupplier.getRecordPositions();

		final RoaringBitmapWriter<RoaringBitmap> mask = RoaringBitmapBackedBitmap.buildWriter();
		final RoaringBitmapWriter<RoaringBitmap> notFound = RoaringBitmapBackedBitmap.buildWriter();

		final BatchIterator unsortedRecordIdsIt = RoaringBitmapBackedBitmap.getRoaringBitmap(unsortedRecordIds).getBatchIterator();
		final BatchIterator selectedRecordIdsIt = RoaringBitmapBackedBitmap.getRoaringBitmap(selectedRecordIds).getBatchIterator();

		int matchesFound = 0;
		int peakA = -1;
		int limitA = -1;
		int peakB = -1;
		int limitB = -1;
		int accA = 1;
		do {
			if (peakA == limitA && limitA != 0) {
				accA += limitA;
				limitA = unsortedRecordIdsIt.nextBatch(bufferA);
				peakA = 0;
			}
			if (peakB == limitB) {
				limitB = selectedRecordIdsIt.nextBatch(bufferB);
				peakB = 0;
			}
			if (peakA < limitA && bufferA[peakA] == bufferB[peakB]) {
				mask.add(recordPositions[accA + peakA]);
				matchesFound++;
				peakB++;
				peakA++;
			} else if (peakB < limitB && (peakA >= limitA || bufferA[peakA] > bufferB[peakB])) {
				notFound.add(bufferB[peakB]);
				peakB++;
			} else {
				peakA++;
			}
		} while (matchesFound < selectedRecordCount && limitB > 0);

		return new MaskResult(mask.get(), notFound.get());
	}

	/**
	 * Completes the result by cutting the result smaller than requested count but appending all not found record ids
	 * before the cut.
	 */
	@Nonnull
	private int[] returnResultAppendingUnknown(MaskResult positions, SortResult sortPartialResult, int startIndex, int endIndex) {
		final int[] sortedResult = sortPartialResult.getResult();
		final int sortedResultPeak = sortPartialResult.getPeak();
		final int finalResultPeak;
		final RoaringBitmap notFoundRecords = positions.getNotFoundRecords();
		if (sortedResultPeak < sortedResult.length && !notFoundRecords.isEmpty()) {
			final int recomputedStartIndex = Math.max(0, startIndex - sortedResultPeak);
			final int recomputedEndIndex = Math.max(0, endIndex - sortedResultPeak);
			if (unknownRecordIdsSorter == null) {
				finalResultPeak = SortUtils.appendNotFoundResult(
					sortedResult, sortedResultPeak, recomputedStartIndex, recomputedEndIndex, notFoundRecords
				);
			} else {
				final int[] rest = unknownRecordIdsSorter.sortAndSlice(
					new ConstantFormula(new BaseBitmap(notFoundRecords)),
					recomputedStartIndex, recomputedEndIndex
				);
				System.arraycopy(rest, 0, sortedResult, sortedResultPeak, rest.length);
				finalResultPeak = sortedResultPeak + rest.length;
			}
		} else {
			finalResultPeak = sortedResultPeak;
		}
		return finalResultPeak < sortedResult.length ? Arrays.copyOf(sortedResult, finalResultPeak) : sortedResult;
	}

	@Data
	private static class MaskResult {
		/**
		 * IntegerBitmap of positions of record ids in presorted set.
		 */
		private final RoaringBitmap mask;
		/**
		 * IntegerBitmap of record ids not found in presorted set at all.
		 */
		private final RoaringBitmap notFoundRecords;
	}

	@Data
	private static class SortResult {
		/**
		 * Sorted result of record ids (may contain padding - see {@link #peak})
		 */
		private final int[] result;
		/**
		 * Peak index position in the result (delimits real record ids and empty space)
		 */
		private final int peak;
	}

}
