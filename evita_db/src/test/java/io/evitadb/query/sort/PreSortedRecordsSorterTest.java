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

package io.evitadb.query.sort;

import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.sort.SortedRecordsSupplierFactory.SortedRecordsSupplier;
import io.evitadb.query.sort.attribute.PreSortedRecordsSorter;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies {@link PreSortedRecordsSorter} behaviour.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class PreSortedRecordsSorterTest {

	@Test
	void shouldReturnFullResultInExpectedOrderOnSmallData() {
		final PreSortedRecordsSorter sorter = new PreSortedRecordsSorter(
			new MockSortedRecordsSupplier(7, 2, 4, 1, 3, 8, 5, 9, 6)
		);
		assertArrayEquals(
			new int[] {2, 4, 1, 3},
			sorter.sortAndSlice(makeFormula(1, 2, 3, 4), 0, 100)
		);
		assertArrayEquals(
			new int[] {1, 3},
			sorter.sortAndSlice(makeFormula(1, 2, 3, 4, 5, 6, 7, 8, 9), 3, 5)
		);
		assertArrayEquals(
			new int[] {7, 8, 9},
			sorter.sortAndSlice(makeFormula(7, 8, 9), 0, 3)
		);
	}

	@Test
	void shouldReturnSortedResultEvenForMissingData() {
		final PreSortedRecordsSorter sorter = new PreSortedRecordsSorter(
			new MockSortedRecordsSupplier(7, 2, 4, 1, 3, 8, 5, 9, 6)
		);
		final int[] actual = sorter.sortAndSlice(makeFormula(0, 1, 2, 3, 4, 12, 13), 0, 100);
		assertArrayEquals(
			new int[]{2, 4, 1, 3, 0, 12, 13},
			actual
		);
	}

	@Test
	void shouldReturnSortedResultEvenForMissingDataWithAdditionalSorter() {
		final PreSortedRecordsSorter sorter = new PreSortedRecordsSorter(
			new MockSortedRecordsSupplier(7, 2, 4, 1, 3, 8, 5, 9, 6),
			new PreSortedRecordsSorter(
				new MockSortedRecordsSupplier(13, 0, 12)
			)
		);
		final int[] actual = sorter.sortAndSlice(makeFormula(0, 1, 2, 3, 4, 12, 13), 0, 100);
		assertArrayEquals(
			new int[]{2, 4, 1, 3, 13, 0, 12},
			actual
		);
	}

	@Test
	void shouldReturnFullResultInExpectedOrderOnLargeData() {
		final int[] sortedRecordIds = generateRandomSortedRecords(2500);
		final MockSortedRecordsSupplier sortedRecordsSupplier = new MockSortedRecordsSupplier(sortedRecordIds);

		assertArrayEquals(
			sortedRecordIds,
			sortedRecordsSupplier.getSortedRecordIds()
		);

		final PreSortedRecordsSorter sorter = new PreSortedRecordsSorter(sortedRecordsSupplier);

		for (int i = 0; i < 5; i++) {
			int[] recIds = pickRandomResults(sortedRecordIds, 500);
			assertPageIsConsistent(sortedRecordIds, sorter, recIds, 0, 50);
			assertPageIsConsistent(sortedRecordIds, sorter, recIds, 75, 125);
			assertPageIsConsistent(sortedRecordIds, sorter, recIds, 100, 500);
		}
	}

	private void assertPageIsConsistent(int[] sortedRecordIds, PreSortedRecordsSorter sorter, int[] recIds, int startIndex, int endIndex) {
		final int[] sortedSlice = sorter.sortAndSlice(makeFormula(recIds), startIndex, endIndex);
		assertEquals(endIndex - startIndex, sortedSlice.length);
		int lastPosition = -1;
		for (int recId : sortedSlice) {
			assertTrue(Arrays.binarySearch(recIds, recId) >= 0, "Record must be part of filter result!");
			int positionInSortedSet = findPosition(sortedRecordIds, recId);
			assertTrue(positionInSortedSet >= lastPosition, "Order must be monotonic!");
		}
	}

	private int findPosition(int[] sortedRecordIds, int recId) {
		for (int i = 0; i < sortedRecordIds.length; i++) {
			int sortedRecordId = sortedRecordIds[i];
			if (sortedRecordId == recId) {
				return i;
			}
		}
		return -1;
	}

	private int[] pickRandomResults(int[] sortedRecordIds, int count) {
		final Random random = new Random();
		final int[] recs = new int[count];
		final Set<Integer> picked = new HashSet<>(count);
		int peak = 0;
		do {
			final int randomRecordId = sortedRecordIds[random.nextInt(sortedRecordIds.length)];
			if (picked.add(randomRecordId)) {
				recs[peak++] = randomRecordId;
			}
		} while (peak < count);
		Arrays.sort(recs);
		return recs;
	}

	private int[] generateRandomSortedRecords(int recCount) {
		final Random random = new Random();
		final Set<Integer> randomRecordIds = new TreeSet<>();
		final int[] sortedRecordIds = new int[recCount];
		int peak = 0;
		do {
			final int rndRecId = random.nextInt(recCount * 2);
			if (randomRecordIds.add(rndRecId)) {
				sortedRecordIds[peak++] = rndRecId;
			}
		} while (peak < recCount);
		return sortedRecordIds;
	}

	@Nonnull
	private ConstantFormula makeFormula(int... recordIds) {
		return new ConstantFormula(new BaseBitmap(recordIds));
	}

	private static class MockSortedRecordsSupplier implements SortedRecordsSupplier {
		@Getter private final RoaringBitmapBackedBitmap allRecords;
		@Getter private final int[] sortedRecordIds;
		@Getter private final int[] recordPositions;

		public MockSortedRecordsSupplier(int... sortedRecordIds) {
			this.sortedRecordIds = sortedRecordIds;
			this.allRecords = new BaseBitmap(sortedRecordIds);
			this.recordPositions = new int[sortedRecordIds.length];
			for (int i = 0; i < sortedRecordIds.length; i++) {
				this.recordPositions[i] = i;
			}
			ArrayUtils.sortSecondAlongFirstArray(this.sortedRecordIds, this.recordPositions);
		}

		@Override
		public int getRecordCount() {
			return sortedRecordIds.length;
		}

	}
}