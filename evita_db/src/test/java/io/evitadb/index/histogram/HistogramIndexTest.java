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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.storage.serialization.index.internal.HistogramBucketSerializer;
import io.evitadb.storage.serialization.index.internal.HistogramIndexSerializer;
import io.evitadb.storage.serialization.index.internal.TransactionalIntegerBitmapSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link HistogramIndex} data structure.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
class HistogramIndexTest {
	private final HistogramIndex<Integer> tested = new HistogramIndex<>();

	@BeforeEach
	void setUp() {
		tested.addRecord(5, 1);
		tested.addRecord(5, 20);
		tested.addRecord(10, 3);
		tested.addRecord(15, 2);
		tested.addRecord(15, 4);
		tested.addRecord(20, 5);
	}

	@Test
	void shouldReturnRecordsAt() {
		assertArrayEquals(new int[]{1, 20}, tested.getRecordsAt(5).getArray());
		assertArrayEquals(new int[]{3}, tested.getRecordsAt(10).getArray());
		assertArrayEquals(new int[]{2, 4}, tested.getRecordsAt(15).getArray());
		assertArrayEquals(new int[]{5}, tested.getRecordsAt(20).getArray());
		assertArrayEquals(new int[0], tested.getRecordsAt(12).getArray());
	}

	@Test
	void shouldAddTransactionalItemsAndRollback() {
		assertStateAfterRollback(
			tested,
			original -> {
				original.addRecord(5, 7);
				original.addRecord(12, 18);
				original.addRecord(1, 10);
				original.addRecord(20, 11);

				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(1, 10),
						new HistogramBucket<>(5, 1, 7, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(12, 18),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5, 11)
					},
					original.getBuckets()
				);
			},
			(original, committed) -> {
				assertNull(committed);
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					original.getBuckets()
				);
			}
		);
	}

	@Test
	void shouldAddSingleNewTransactionalItemAndCommit() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.addRecord(55, 78);

				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5),
						new HistogramBucket<>(55, 78)
					},
					original.getBuckets()
				);
			},
			(original, committed) -> {
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					original.getBuckets()
				);
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5),
						new HistogramBucket<>(55, 78)
					},
					committed.getBuckets()
				);
			}
		);
	}

	@Test
	void shouldRemoveSingleTransactionalItemAndCommit() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.removeRecord(10, 3);

				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					original.getBuckets()
				);
			},
			(original, committed) -> {
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					original.getBuckets()
				);
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					committed.getBuckets()
				);
			}
		);
	}

	@Test
	void shouldAddTransactionalItemsAndCommit() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.addRecord(5, 7);
				original.addRecord(12, 18);
				original.addRecord(1, 10);
				original.addRecord(20, 11);

				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(1, 10),
						new HistogramBucket<>(5, 1, 7, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(12, 18),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5, 11)
					},
					original.getBuckets()
				);
			},
			(original, committed) -> {
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					original.getBuckets()
				);
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(1, 10),
						new HistogramBucket<>(5, 1, 7, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(12, 18),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5, 11)
					},
					committed.getBuckets()
				);
			}
		);
	}

	@Test
	void shouldAddAndRemoveItemsInTransaction() {
		assertStateAfterCommit(
			new HistogramIndex<Integer>(),
			original -> {
				original.addRecord(5, 7);
				original.addRecord(12, 18);
				original.removeRecord(5, 7);
				original.removeRecord(12, 18);

				assertArrayEquals(
					new HistogramBucket[0],
					original.getBuckets()
				);
			},
			(original, committed) -> {
				assertArrayEquals(
					new HistogramBucket[0],
					original.getBuckets()
				);
				assertArrayEquals(
					new HistogramBucket[0],
					committed.getBuckets()
				);
			}
		);
	}

	@Test
	void shouldShrinkHistogramOnRemovingItems() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.removeRecord(5, 1);
				original.removeRecord(10, 3);
				original.removeRecord(20, 5);

				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 20),
						new HistogramBucket<>(15, 2, 4)
					},
					original.getBuckets()
				);
			},
			(original, committed) -> {
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 1, 20),
						new HistogramBucket<>(10, 3),
						new HistogramBucket<>(15, 2, 4),
						new HistogramBucket<>(20, 5)
					},
					original.getBuckets()
				);
				assertArrayEquals(
					new HistogramBucket[]{
						new HistogramBucket<>(5, 20),
						new HistogramBucket<>(15, 2, 4)
					},
					committed.getBuckets()
				);
			}
		);
	}

	@Test
	void shouldReportEmptyStateEveInTransaction() {
		assertStateAfterCommit(
			tested,
			original -> {
				assertFalse(original.isEmpty());

				original.removeRecord(5, 1);
				original.removeRecord(5, 20);
				original.removeRecord(10, 3);
				original.removeRecord(15, 2);
				original.removeRecord(15, 4);

				assertFalse(original.isEmpty());

				original.removeRecord(20, 5);

				assertTrue(original.isEmpty());
			},
			(original, committed) -> {
				assertFalse(original.isEmpty());
				assertTrue(committed.isEmpty());
			}
		);
	}

	@Test
	void shouldSerializeAndDeserialize() {
		final Kryo kryo = new Kryo();

		kryo.register(HistogramIndex.class, new HistogramIndexSerializer());
		kryo.register(HistogramBucket.class, new HistogramBucketSerializer());
		kryo.register(TransactionalBitmap.class, new TransactionalIntegerBitmapSerializer());

		final Output output = new Output(1024, -1);
		kryo.writeObject(output, tested);
		output.flush();

		byte[] bytes = output.getBuffer();

		final HistogramIndex deserializedTested = kryo.readObject(new Input(bytes), HistogramIndex.class);
		assertEquals(tested, deserializedTested);
	}

	@Test
	void shouldReturnRecordIndex() {
		assertEquals(0, tested.findRecordIndex(1));
		assertEquals(1, tested.findRecordIndex(20));
		assertEquals(2, tested.findRecordIndex(3));
		assertEquals(3, tested.findRecordIndex(2));
		assertEquals(4, tested.findRecordIndex(4));
		assertEquals(5, tested.findRecordIndex(5));
		assertEquals(-1, tested.findRecordIndex(6));
	}

	@Test
	void shouldReturnRecordIndexReversed() {
		assertEquals(5, tested.findRecordIndexReversed(1));
		assertEquals(4, tested.findRecordIndexReversed(20));
		assertEquals(3, tested.findRecordIndexReversed(3));
		assertEquals(2, tested.findRecordIndexReversed(2));
		assertEquals(1, tested.findRecordIndexReversed(4));
		assertEquals(0, tested.findRecordIndexReversed(5));
		assertEquals(-1, tested.findRecordIndex(6));
	}

	@Test
	void shouldReportEmptyState() {
		assertFalse(tested.isEmpty());

		tested.removeRecord(5, 1);
		tested.removeRecord(5, 20);
		tested.removeRecord(10, 3);
		tested.removeRecord(15, 2);
		tested.removeRecord(15, 4);

		assertFalse(tested.isEmpty());

		tested.removeRecord(20, 5);

		assertTrue(tested.isEmpty());
	}

	@Test
	void shouldReturnSortedAllValues() {
		assertIteratorContains(tested.getSortedRecords().getRecordIds().iterator(), new int[]{1, 2, 3, 4, 5, 20});
	}

	@Test
	void shouldReturnSortedValuesFromLowerBoundUp() {
		assertIteratorContains(tested.getSortedRecords(10, null).getRecordIds().iterator(), new int[]{2, 3, 4, 5});
	}

	@Test
	void shouldReturnSortedValuesFromLowerBoundUpNotExact() {
		assertIteratorContains(tested.getSortedRecords(11, null).getRecordIds().iterator(), new int[]{2, 4, 5});
	}

	@Test
	void shouldReturnSortedValuesFromUpperBoundDown() {
		assertIteratorContains(tested.getSortedRecords(null, 15).getRecordIds().iterator(), new int[]{1, 2, 3, 4, 20});
	}

	@Test
	void shouldReturnSortedValuesFromUpperBoundDownNotExact() {
		assertIteratorContains(tested.getSortedRecords(null, 14).getRecordIds().iterator(), new int[]{1, 3, 20});
	}

	@Test
	void shouldReturnSortedValuesBetweenBounds() {
		assertIteratorContains(tested.getSortedRecords(10, 15).getRecordIds().iterator(), new int[]{2, 3, 4});
	}

	@Test
	void shouldReturnSortedValuesBetweenBoundsNotExact() {
		assertIteratorContains(tested.getSortedRecords(11, 14).getRecordIds().iterator(), new int[0]);
		assertIteratorContains(tested.getSortedRecords(14, 16).getRecordIds().iterator(), new int[]{2, 4});
		assertIteratorContains(tested.getSortedRecords(15, 15).getRecordIds().iterator(), new int[]{2, 4});
	}

	/* NOT SORTED */

	@Test
	void shouldReturnAllValues() {
		assertIteratorContains(tested.getRecords().getRecordIds().iterator(), new int[]{1, 20, 3, 2, 4, 5});
	}

	@Test
	void shouldReturnValuesFromLowerBoundUp() {
		assertIteratorContains(tested.getRecords(10, null).getRecordIds().iterator(), new int[]{3, 2, 4, 5});
	}

	@Test
	void shouldReturnValuesFromLowerBoundUpNotExact() {
		assertIteratorContains(tested.getRecords(11, null).getRecordIds().iterator(), new int[]{2, 4, 5});
	}

	@Test
	void shouldReturnValuesFromUpperBoundDown() {
		assertIteratorContains(tested.getRecords(null, 15).getRecordIds().iterator(), new int[]{1, 20, 3, 2, 4});
	}

	@Test
	void shouldReturnValuesFromUpperBoundDownNotExact() {
		assertIteratorContains(tested.getRecords(null, 14).getRecordIds().iterator(), new int[]{1, 20, 3});
	}

	@Test
	void shouldReturnValuesBetweenBounds() {
		assertIteratorContains(tested.getRecords(10, 15).getRecordIds().iterator(), new int[]{3, 2, 4});
	}

	@Test
	void shouldReturnValuesBetweenBoundsNotExact() {
		assertIteratorContains(tested.getRecords(11, 14).getRecordIds().iterator(), new int[0]);
		assertIteratorContains(tested.getRecords(14, 16).getRecordIds().iterator(), new int[]{2, 4});
		assertIteratorContains(tested.getRecords(15, 15).getRecordIds().iterator(), new int[]{2, 4});
	}

	/* NOT SORTED - REVERSED */

	@Test
	void shouldReversedReturnAllValues() {
		assertIteratorContains(tested.getRecordsReversed().getRecordIds().iterator(), new int[]{5, 2, 4, 3, 1, 20});
	}

	@Test
	void shouldReversedReturnValuesFromLowerBoundUp() {
		assertIteratorContains(tested.getRecordsReversed(10, null).getRecordIds().iterator(), new int[]{5, 2, 4, 3});
	}

	@Test
	void shouldReversedReturnValuesFromLowerBoundUpNotExact() {
		assertIteratorContains(tested.getRecordsReversed(11, null).getRecordIds().iterator(), new int[]{5, 2, 4});
	}

	@Test
	void shouldReversedReturnValuesFromUpperBoundDown() {
		assertIteratorContains(tested.getRecordsReversed(null, 15).getRecordIds().iterator(), new int[]{2, 4, 3, 1, 20});
	}

	@Test
	void shouldReversedReturnValuesFromUpperBoundDownNotExact() {
		assertIteratorContains(tested.getRecordsReversed(null, 14).getRecordIds().iterator(), new int[]{3, 1, 20});
	}

	@Test
	void shouldReversedReturnValuesBetweenBounds() {
		assertIteratorContains(tested.getRecordsReversed(10, 15).getRecordIds().iterator(), new int[]{2, 4, 3});
	}

	@Test
	void shouldReversedReturnValuesBetweenBoundsNotExact() {
		assertIteratorContains(tested.getRecordsReversed(11, 14).getRecordIds().iterator(), new int[0]);
		assertIteratorContains(tested.getRecordsReversed(14, 16).getRecordIds().iterator(), new int[]{2, 4});
		assertIteratorContains(tested.getRecordsReversed(15, 15).getRecordIds().iterator(), new int[]{2, 4});
	}

	@Test
	void shouldGenerationalTestPass() {
		final HistogramIndex<Long> histogram = new HistogramIndex<>();
		histogram.addRecord(64L, 36, 47);
		histogram.addRecord(0L, 10);
		histogram.addRecord(65L, 90);
		histogram.addRecord(2L, 89);
		histogram.addRecord(67L, 9);
		histogram.addRecord(4L, 31, 22);
		histogram.addRecord(5L, 87);
		histogram.addRecord(6L, 5);
		histogram.addRecord(7L, 40);
		histogram.addRecord(74L, 7);
		histogram.addRecord(10L, 54);
		histogram.addRecord(12L, 16);
		histogram.addRecord(76L, 97);
		histogram.addRecord(77L, 56);
		histogram.addRecord(13L, 82);
		histogram.addRecord(15L, 67);
		histogram.addRecord(16L, 55);
		histogram.addRecord(82L, 32);
		histogram.addRecord(18L, 53, 76);
		histogram.addRecord(22L, 45, 37);
		histogram.addRecord(87L, 94, 83);
		histogram.addRecord(88L, 46, 44);
		histogram.addRecord(25L, 99);
		histogram.addRecord(26L, 98, 49);
		histogram.addRecord(92L, 0);
		histogram.addRecord(93L, 1);
		histogram.addRecord(31L, 57);
		histogram.addRecord(95L, 85);
		histogram.addRecord(97L, 66);
		histogram.addRecord(41L, 11);
		histogram.addRecord(44L, 51);
		histogram.addRecord(46L, 81, 3, 41);
		histogram.addRecord(49L, 26);
		histogram.addRecord(51L, 96);
		histogram.addRecord(54L, 8);
		histogram.addRecord(56L, 34);
		histogram.addRecord(57L, 62);
		histogram.addRecord(61L, 78);

		assertStateAfterCommit(
			histogram,
			original -> {
				histogram.removeRecord(65L, 90);
				histogram.removeRecord(51L, 96);
				histogram.removeRecord(22L, 37);
				histogram.addRecord(0L, 75);
				histogram.removeRecord(7L, 40);
				histogram.removeRecord(26L, 49);
				histogram.removeRecord(0L, 75);
				histogram.addRecord(92L, 71);
				histogram.addRecord(31L, 88);
				histogram.addRecord(16L, 59);
				histogram.addRecord(93L, 70);
				histogram.addRecord(74L, 84);
				histogram.removeRecord(64L, 47);
				histogram.addRecord(85L, 69);
				histogram.addRecord(78L, 28);
				histogram.addRecord(71L, 40);
				histogram.addRecord(37L, 43);
				histogram.removeRecord(97L, 66);
				histogram.addRecord(9L, 50);
				histogram.removeRecord(67L, 9);
				histogram.addRecord(45L, 73);
				histogram.removeRecord(13L, 82);
				histogram.removeRecord(92L, 0);
				histogram.removeRecord(93L, 1);
				histogram.addRecord(67L, 17);
				histogram.removeRecord(77L, 56);
				histogram.addRecord(66L, 23);
				histogram.addRecord(98L, 56);
				histogram.addRecord(29L, 48);
				histogram.removeRecord(88L, 44);
				histogram.addRecord(75L, 49);
				histogram.removeRecord(31L, 57);
				histogram.removeRecord(5L, 87);
				histogram.addRecord(65L, 64);
				histogram.removeRecord(71L, 40);
				histogram.removeRecord(4L, 22);
				histogram.removeRecord(61L, 78);
				histogram.addRecord(11L, 12);
				histogram.removeRecord(46L, 81);
				histogram.addRecord(0L, 2);
				histogram.addRecord(42L, 15);
				histogram.addRecord(37L, 25);
				histogram.removeRecord(75L, 49);
				histogram.removeRecord(54L, 8);
				histogram.addRecord(74L, 61);
				histogram.removeRecord(37L, 25);
				histogram.addRecord(16L, 30);
				histogram.addRecord(96L, 72);
				histogram.addRecord(65L, 39);
				histogram.removeRecord(18L, 53);
				histogram.removeRecord(56L, 34);
				histogram.removeRecord(45L, 73);
				histogram.removeRecord(0L, 2);
				histogram.removeRecord(95L, 85);
				histogram.addRecord(85L, 78);
				histogram.addRecord(80L, 18);
				histogram.addRecord(88L, 8);
				histogram.removeRecord(74L, 84);
				histogram.addRecord(96L, 1);
				histogram.addRecord(54L, 38);
				histogram.addRecord(33L, 93);
				histogram.removeRecord(16L, 59);
				histogram.removeRecord(57L, 62);
				histogram.addRecord(64L, 60);
				histogram.addRecord(94L, 75);
				histogram.removeRecord(25L, 99);
				histogram.removeRecord(37L, 43);
				histogram.removeRecord(42L, 15);
				histogram.removeRecord(10L, 54);
				histogram.removeRecord(85L, 78);
				histogram.addRecord(19L, 2);
				histogram.addRecord(81L, 90);
				histogram.addRecord(21L, 95);
				histogram.removeRecord(64L, 60);
				histogram.addRecord(87L, 42);
				histogram.removeRecord(46L, 41);
				histogram.removeRecord(82L, 32);
				histogram.removeRecord(74L, 61);
				histogram.addRecord(42L, 73);
				histogram.removeRecord(78L, 28);
				histogram.removeRecord(16L, 30);
				histogram.removeRecord(98L, 56);
				histogram.addRecord(64L, 47);
				histogram.removeRecord(87L, 83);
				histogram.removeRecord(42L, 73);
				histogram.removeRecord(22L, 45);
				histogram.addRecord(35L, 19);
				histogram.removeRecord(81L, 90);
				histogram.removeRecord(54L, 38);
				histogram.addRecord(64L, 60);
			},
			(original, committed) -> {
				final int[] expected = {1, 2, 3, 5, 7, 8, 10, 11, 12, 16, 17, 18, 19, 23, 26, 31, 36, 39, 42, 46, 47, 48, 50, 51, 55, 60, 64, 67, 69, 70, 71, 72, 75, 76, 88, 89, 93, 94, 95, 97, 98};
				assertArrayEquals(
					expected,
					committed.getSortedRecords().getRecordIds().getArray(),
					"\nExpected: " + Arrays.toString(expected) + "\n" +
						"Actual:   " + Arrays.toString(committed.getSortedRecords().getRecordIds().getArray()) + "\n"
				);
			}
		);
	}

	@Disabled("This infinite test performs random operations sort index and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random(15);
		final int initialCount = 100;
		final Map<Long, List<Integer>> mapToCompare = new HashMap<>();
		final Set<Integer> currentRecordSet = new HashSet<>();
		final Set<Long> uniqueValues = new TreeSet<>();
		final AtomicReference<HistogramIndex<Long>> transactionalHistogram = new AtomicReference<>(new HistogramIndex<>());
		final StringBuilder ops = new StringBuilder("final HistogramIndex<Long> histogram = new HistogramIndex<>();\n");

		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalHistogram.get(),
				original -> {
					try {
						final int operationsInTransaction = rnd.nextInt(100);
						for (int i = 0; i < operationsInTransaction; i++) {
							final HistogramIndex<Long> histogram = transactionalHistogram.get();
							final int length = histogram.getRecords().getRecordIds().size();
							if ((rnd.nextBoolean() || length < 10) && length < 50) {
								// insert new item
								final Long newValue = (long) rnd.nextInt(initialCount);

								int newRecId;
								do {
									newRecId = rnd.nextInt(initialCount);
								} while (currentRecordSet.contains(newRecId));

								mapToCompare.computeIfAbsent(newValue, aLong -> new ArrayList<>()).add(newRecId);
								currentRecordSet.add(newRecId);
								uniqueValues.add(newValue);

								ops.append("histogram.addRecord(").append(newValue).append("L,").append(newRecId).append(");\n");
								histogram.addRecord(newValue, newRecId);
							} else {
								// remove existing item
								final Iterator<Entry<Long, List<Integer>>> it = mapToCompare.entrySet().iterator();
								Long valueToRemove = null;
								Integer recordToRemove = null;
								final int removePosition = rnd.nextInt(length);
								int cnt = 0;
								finder:
								for (int j = 0; j < mapToCompare.size() + 1; j++) {
									final Entry<Long, List<Integer>> entry = it.next();
									final Iterator<Integer> valIt = entry.getValue().iterator();
									while (valIt.hasNext()) {
										final Integer recordId = valIt.next();
										if (removePosition == cnt++) {
											valueToRemove = entry.getKey();
											recordToRemove = recordId;
											valIt.remove();
											break finder;
										}
									}
								}
								currentRecordSet.remove(recordToRemove);
								final int expectedIndex = indexOf(uniqueValues, valueToRemove);
								if (mapToCompare.get(valueToRemove).isEmpty()) {
									uniqueValues.remove(valueToRemove);
									mapToCompare.remove(valueToRemove);
								}

								ops.append("histogram.removeRecord(").append(valueToRemove).append("L,").append(recordToRemove).append(");\n");
								final int removedAtIndex = histogram.removeRecord(valueToRemove, recordToRemove);

								assertEquals(expectedIndex, removedAtIndex);
							}
						}
					} catch (Exception ex) {
						fail("\n" + ops, ex);
					}
				},
				(original, committed) -> {
					final int[] expected = currentRecordSet.stream().mapToInt(it -> it).sorted().toArray();
					assertArrayEquals(
						expected,
						committed.getSortedRecords().getRecordIds().getArray(),
						"\nExpected: " + Arrays.toString(expected) + "\n" +
							"Actual:   " + Arrays.toString(committed.getSortedRecords().getRecordIds().getArray()) + "\n\n" +
							ops
					);

					transactionalHistogram.set(
						new HistogramIndex<>(
							committed.getBuckets()
						)
					);
					ops.setLength(0);
					ops.append("final HistogramIndex<Long> histogram = new HistogramIndex<>();\n")
						.append(mapToCompare.entrySet().stream().map(it -> "histogram.addRecord(" + it.getKey() + "L," + it.getValue().stream().map(Object::toString).collect(Collectors.joining(", ")) + ");").collect(Collectors.joining("\n")));
					ops.append("\nOps:\n");
				}
			);
			if (iteration++ % 1_000 == 0) {
				System.out.print(".");
				System.out.flush();
			}
			if (iteration % 50_000 == 0) {
				System.out.print("\n");
				System.out.flush();
			}
		} while (true);
	}

	private static int indexOf(Set<Long> values, Long valueToFind) {
		int result = -1;
		for (Long value : values) {
			result++;
			if (valueToFind.equals(value)) {
				return result;
			}
		}
		return result;
	}

}