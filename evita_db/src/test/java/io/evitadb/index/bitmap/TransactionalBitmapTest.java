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

package io.evitadb.index.bitmap;

import io.evitadb.api.utils.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static io.evitadb.utils.AssertionUtils.assertStateAfterRollback;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link TransactionalBitmap}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class TransactionalBitmapTest {

	@Test
	void shouldCorrectlyAddItemsOnFirstLastAndMiddlePositionsAndRollback() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 5, 10);

		assertStateAfterRollback(
			bitmap,
			original -> {
				original.addAll(11, 0, 6);
				assertTransactionalBitmapIs(new int[]{0, 1, 5, 6, 10, 11}, bitmap);
			},
			(original, committed) -> {
				assertNull(committed);
				assertTransactionalBitmapIs(new int[] {1,5,10}, original);
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveItemsOnFirstLastAndMiddlePositionsAndRollback() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 5, 10);

		assertStateAfterRollback(
			bitmap,
			original -> {
				original.remove(5);
				assertTransactionalBitmapIs(new int[] {1,10}, original);
			},
			(original, committed) -> {
				assertNull(committed);
				assertTransactionalBitmapIs(new int[] {1,5,10}, original);
			}
		);

	}

	@Test
	void shouldCorrectlyAddItemsOnFirstLastAndMiddlePositionsAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 5, 10);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.add(11);
				original.addAll(11, 0, 6);

				assertTransactionalBitmapIs(new int[]{0, 1, 5, 6, 10, 11}, bitmap);
				assertFalse(bitmap.contains(2));
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 5, 10}, original);
				assertArrayEquals(new int[]{0, 1, 5, 6, 10, 11}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveItemsFromFirstLastAndMiddlePositionsAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.removeAll(11, 1, 5);
				assertTransactionalBitmapIs(new int[] {2, 6, 10}, bitmap);
				assertFalse(bitmap.contains(11));
				assertFalse(bitmap.contains(1));
				assertFalse(bitmap.contains(5));
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {2, 6, 10}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveMultipleItemsInARowAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.removeAll(6, 5, 2);

				assertTransactionalBitmapIs(new int[] {1, 10, 11}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {1, 10, 11}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveMultipleItemsInARowTillTheEndAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.remove(6);
				original.removeAll(6, 5, 10, 11);

				assertTransactionalBitmapIs(new int[] {1, 2}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {1, 2}, committed.getArray());
			}
		);

	}

	@Test
	void shouldCorrectlyRemoveMultipleItemsInARowFromTheBeginningAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.removeAll(2, 6, 5, 1, 10);

				assertTransactionalBitmapIs(new int[] {11}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {11}, committed.getArray());
			}
		);
	}

	@Test
	void shouldAddNothingAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 5, 10);

		assertStateAfterCommit(
			bitmap,
			original -> {
				assertTransactionalBitmapIs(new int[] {1,5,10}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1,5,10}, bitmap);
				assertArrayEquals(new int[] {1,5,10}, committed.getArray());
			}
		);
	}

	@Test
	void shouldAddAndRemoveEverythingAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(new int[0]);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.add(1);
				original.add(5);
				original.remove(1);
				original.remove(5);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[0], bitmap);
				assertArrayEquals(new int[0], committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyAddMultipleItemsOnSamePositionsAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 5, 10);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.addAll(11, 6, 0, 3, 7, 12, 2, 8);
				original.add(3);

				assertTransactionalBitmapIs(new int[] {0,1,2,3,5,6,7,8,10,11,12}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 5, 10}, original);
				assertArrayEquals(new int[] {0,1,2,3,5,6,7,8,10,11,12}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyAddAndRemoveOnNonOverlappingPositionsAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.add(4);
				original.add(3);
				original.remove(10);
				original.remove(6);
				original.add(15);

				assertTransactionalBitmapIs(new int[] {1, 2, 3, 4, 5, 11, 15}, original);
				assertFalse(bitmap.contains(10));
				assertFalse(bitmap.contains(6));
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {1, 2, 3, 4, 5, 11, 15}, committed.getArray());
			}
		);

	}

	@Test
	void shouldCorrectlyAddAndRemoveSameNumberAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.add(4);
				original.remove(4);

				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertFalse(bitmap.contains(4));
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {1, 2, 5, 6, 10, 11}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveAndAddSameNumberAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.remove(5);
				original.remove(10);
				original.remove(11);
				original.add(10);
				original.add(11);
				original.add(5);

				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {1, 2, 5, 6, 10, 11}, committed.getArray());
			}
		);

	}

	@Test
	void shouldCorrectlyAddAndRemoveOnOverlappingBoundaryPositionsAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2, 5, 6, 10, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.remove(1);
				original.remove(11);
				original.add(0);
				original.add(12);

				assertTransactionalBitmapIs(new int[] {0, 2, 5, 6, 10, 12}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2, 5, 6, 10, 11}, original);
				assertArrayEquals(new int[] {0, 2, 5, 6, 10, 12}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyAddAndRemoveOnOverlappingMiddlePositionsAndCommit() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 5, 8, 11);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.remove(5);
				original.remove(8);
				original.add(6);
				original.add(7);
				original.add(8);
				original.add(9);
				original.add(10);

				assertTransactionalBitmapIs(new int[] {1, 6, 7, 8, 9, 10, 11}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 5, 8, 11}, original);
				assertArrayEquals(new int[] {1, 6, 7, 8, 9, 10, 11}, committed.getArray());
			}
		);
	}

	@Test
	void shouldProperlyHandleChangesOnSinglePosition() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(1, 2);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.remove(1);
				original.remove(2);
				original.add(2);
				original.add(4);
				original.remove(2);
				original.add(5);

				assertTransactionalBitmapIs(new int[] {4, 5}, original);
			},
			(original, committed) -> {
				assertTransactionalBitmapIs(new int[] {1, 2}, original);
				assertArrayEquals(new int[] {4, 5}, committed.getArray());
			}
		);
	}

	@Test
	void shouldCorrectlyWipeAll() {
		final TransactionalBitmap bitmap = new TransactionalBitmap(36, 59, 179);

		assertStateAfterCommit(
			bitmap,
			original -> {
				original.add(31);
				original.remove(31);
				original.addAll(140, 115);
				original.removeAll(179, 36, 140);
				original.add(58);
				original.removeAll(58, 115, 59);
				original.addAll(156, 141);
				original.remove(141);
				original.add(52);
				original.removeAll(52, 156);

				assertTransactionalBitmapIs(new int[0], bitmap);
			},
			(original, committed) -> assertArrayEquals(new int[0], committed.getArray())
		);
	}

	@Disabled("This infinite test performs random operations on trans. list and normal list and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random();
		final int initialCount = 100;
		final int[] initialBitmap = generateRandomInitialBitmap(rnd, initialCount);
		final AtomicReference<TransactionalBitmap> transactionalBitmap = new AtomicReference<>(new TransactionalBitmap(initialBitmap));
		final AtomicReference<int[]> nextBitmapToCompare = new AtomicReference<>(initialBitmap);

		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalBitmap.get(),
				original -> {
					final int operationsInTransaction = rnd.nextInt(100);
					final TransactionalBitmap txBitmap = transactionalBitmap.get();
					for (int i = 0; i < operationsInTransaction; i++) {
						final int length = txBitmap.size();
						if (rnd.nextBoolean() || length < 10) {
							// insert new item
							final int newRecId = rnd.nextInt(initialCount * 2);
							txBitmap.add(newRecId);
							nextBitmapToCompare.set(ArrayUtils.insertIntIntoOrderedArray(newRecId, nextBitmapToCompare.get()));
						} else {
							// remove existing item
							final int removedRecId = txBitmap.get(rnd.nextInt(length));
							txBitmap.remove(removedRecId);
							nextBitmapToCompare.set(ArrayUtils.removeIntFromOrderedArray(removedRecId, nextBitmapToCompare.get()));
						}
					}

					assertTransactionalBitmapIs(nextBitmapToCompare.get(), txBitmap);
				},
				(original, committed) -> {
					assertArrayEquals(nextBitmapToCompare.get(), committed.getArray());
					transactionalBitmap.set(new TransactionalBitmap(committed));
				}
			);
			if (iteration++ % 10_000 == 0) {
				System.out.print(".");
				System.out.flush();
			}
			if (iteration % 500_000 == 0) {
				System.out.print("\n");
				System.out.flush();
			}
		} while (true);
	}

	private int[] generateRandomInitialBitmap(Random rnd, int count) {
		final Set<Integer> uniqueSet = new HashSet<>();
		final int[] initialBitmap = new int[count];
		for (int i = 0; i < count; i++) {
			boolean added;
			do {
				final int recId = rnd.nextInt(count * 2);
				added = uniqueSet.add(recId);
				if (added) {
					initialBitmap[i] = recId;
				}
			} while (!added);
		}
		Arrays.sort(initialBitmap);
		return initialBitmap;
	}

	private static void assertTransactionalBitmapIs(int[] expectedResult, TransactionalBitmap bitmap) {
		if (ArrayUtils.isEmpty(expectedResult)) {
			assertTrue(bitmap.isEmpty());
		} else {
			assertFalse(bitmap.isEmpty());
		}

		for (int recordId : expectedResult) {
			assertTrue(bitmap.contains(recordId), "IntegerBitmap should contain " + recordId + ", but does not!");
		}

		assertArrayEquals(expectedResult, bitmap.getArray());
		assertEquals(expectedResult.length, bitmap.size());

		final OfInt it = bitmap.iterator();
		int index = -1;
		while (it.hasNext()) {
			final int nextInt = it.next();
			assertTrue(expectedResult.length > index + 1);
			assertEquals(expectedResult[++index], nextInt);
		}
		assertEquals(
			expectedResult.length, index + 1,
			"There are more expected ints than int bitmap produced by iterator!"
		);
	}

}