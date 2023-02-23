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

import io.evitadb.api.dataType.NumberRange;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies {@link FilterIndex} contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class FilterIndexTest {
	private final FilterIndex stringAttribute = new FilterIndex(String.class);
	private final FilterIndex rangeAttribute = new FilterIndex(NumberRange.class);

	@Test
	void shouldInsertNewStringRecordId() {
		stringAttribute.addRecord(1, "A");
		stringAttribute.addRecord(2, new String[] {"A", "B"});
		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertEquals(2, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldInsertNewStringRecordIdInTheMiddle() {
		stringAttribute.addRecord(1, "A");
		stringAttribute.addRecord(3, "C");
		assertArrayEquals(new int[] {1}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {3}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertEquals(2, stringAttribute.getAllRecords().size());
		stringAttribute.addRecord(2, "B");
		assertArrayEquals(new int[] {1}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertArrayEquals(new int[] {3}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertEquals(3, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldInsertNewStringRecordIdInTheBeginning() {
		stringAttribute.addRecord(1, "C");
		stringAttribute.addRecord(2, "B");
		stringAttribute.addRecord(3, "A");

		assertArrayEquals(new int[] {3}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertArrayEquals(new int[] {1}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertEquals(3, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldInsertNewRangeRecord() {
		rangeAttribute.addRecord(1, NumberRange.between(5, 10));
		rangeAttribute.addRecord(2, NumberRange.between(11, 20));
		rangeAttribute.addRecord(3, NumberRange.between(5, 15));

		assertArrayEquals(new int[] {1}, rangeAttribute.getRecordsEqualTo(NumberRange.between(5, 10)).getArray());
		assertArrayEquals(new int[] {2}, rangeAttribute.getRecordsEqualTo(NumberRange.between(11, 20)).getArray());
		assertArrayEquals(new int[] {3}, rangeAttribute.getRecordsEqualTo(NumberRange.between(5, 15)).getArray());
		assertEquals(3, rangeAttribute.getAllRecords().size());
	}

	@Test
	void shouldRemoveStringRecordId() {
		fillStringAttribute();
		stringAttribute.removeRecord(1, new String[] {"A", "C"});

		assertArrayEquals(new int[] {2}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertArrayEquals(new int[] {3}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertArrayEquals(new int[] {4}, stringAttribute.getRecordsEqualTo("D").getArray());
		assertEquals(4, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldRemoveStringRecordIdRemovingFirstBucket() {
		fillStringAttribute();
		stringAttribute.removeRecord(1, "A");
		stringAttribute.removeRecord(2, "A");

		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertArrayEquals(new int[] {1, 3}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertArrayEquals(new int[] {4}, stringAttribute.getRecordsEqualTo("D").getArray());
		assertEquals(4, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldRemoveStringRecordIdRemovingLastBucket() {
		fillStringAttribute();
		stringAttribute.removeRecord(4, "D");

		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertArrayEquals(new int[] {1, 3}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertEquals(3, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldRemoveStringRecordIdRemovingMiddleBuckets() {
		fillStringAttribute();
		stringAttribute.removeRecord(1, new String[] {"B", "C"});
		stringAttribute.removeRecord(2, "B");
		stringAttribute.removeRecord(3, "C");

		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[] {4}, stringAttribute.getRecordsEqualTo("D").getArray());
		assertEquals(3, stringAttribute.getAllRecords().size());
	}

	@Test
	void shouldRemoveRangeRecord() {
		fillRangeAttribute();
		rangeAttribute.removeRecord(1, NumberRange.between(5, 10));

		assertArrayEquals(new int[] {1}, rangeAttribute.getRecordsEqualTo(NumberRange.between(50, 90)).getArray());
		assertArrayEquals(new int[] {2}, rangeAttribute.getRecordsEqualTo(NumberRange.between(11, 20)).getArray());
		assertArrayEquals(new int[] {3}, rangeAttribute.getRecordsEqualTo(NumberRange.between(5, 15)).getArray());
		assertEquals(3, rangeAttribute.getAllRecords().size());
	}

	@Test
	void shouldRemoveRangeRecordRemovingBucket() {
		fillRangeAttribute();
		rangeAttribute.removeRecord(1, new NumberRange[] {NumberRange.between(5, 10), NumberRange.between(50, 90)});
		rangeAttribute.removeRecord(3, NumberRange.between(5, 15));

		assertArrayEquals(new int[] {2}, rangeAttribute.getRecordsEqualTo(NumberRange.between(11, 20)).getArray());
		assertEquals(1, rangeAttribute.getAllRecords().size());
	}

	@Test
	void shouldReturnAllRecords() {
		fillStringAttribute();
		assertArrayEquals(new int[] {1, 2, 3, 4}, stringAttribute.getAllRecords().getArray());
	}

	@Test
	void shouldReturnRecordsGreaterThan() {
		fillStringAttribute();
		assertArrayEquals(new int[] {1, 3, 4}, stringAttribute.getRecordsGreaterThan("B").getArray());
	}

	@Test
	void shouldReturnRecordsGreaterThanEq() {
		fillStringAttribute();
		assertArrayEquals(new int[] {1, 3, 4}, stringAttribute.getRecordsGreaterThanEq("C").getArray());
	}

	@Test
	void shouldReturnRecordsLesserThan() {
		fillStringAttribute();
		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsLesserThan("C").getArray());
	}

	@Test
	void shouldReturnRecordsLesserThanEq() {
		fillStringAttribute();
		assertArrayEquals(new int[] {1, 2}, stringAttribute.getRecordsLesserThanEq("B").getArray());
	}

	@Test
	void shouldReturnRecordsBetween() {
		fillStringAttribute();
		assertArrayEquals(new int[]{1, 3, 4}, stringAttribute.getRecordsBetween("C", "D").getArray());
	}

	@Test
	void shouldReturnRecordsValidIn() {
		fillRangeAttribute();
		assertArrayEquals(new int[]{1, 3}, rangeAttribute.getRecordsValidIn(8L).getArray());
	}

	@Disabled("This infinite test performs random operations sort index and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random();
		final int initialCount = 100;
		final Map<NumberRange, Integer> mapToCompare = new HashMap<>();
		final Set<Integer> currentRecordSet = new HashSet<>();
		final Set<NumberRange> uniqueValues = new HashSet<>();
		final AtomicReference<FilterIndex> transactionalFilterIndex = new AtomicReference<>(new FilterIndex(NumberRange.class));
		final StringBuilder ops = new StringBuilder("final FilterIndex filterIndex = new FilterIndex(String.class);\n");

		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalFilterIndex.get(),
				original -> {
					try {
						final int operationsInTransaction = rnd.nextInt(100);
						for (int i = 0; i < operationsInTransaction; i++) {
							final FilterIndex filterIndex = transactionalFilterIndex.get();
							final int length = filterIndex.size();
							if ((rnd.nextBoolean() || length < 10) && length < 50) {
								// insert new item
								NumberRange range;
								do {
									final int from = rnd.nextInt(initialCount * 2);
									final int to = rnd.nextInt(initialCount * 2);
									range = NumberRange.between(Math.min(from, to), Math.max(from, to));
								} while (uniqueValues.contains(range));

								int newRecId;
								do {
									newRecId = rnd.nextInt(initialCount * 2);
								} while (currentRecordSet.contains(newRecId));
								mapToCompare.put(range, newRecId);
								currentRecordSet.add(newRecId);
								uniqueValues.add(range);

								ops.append("filterIndex.addRecord(\"").append(newRecId).append("\",").append(range).append(");\n");
								filterIndex.addRecord(newRecId, range);
							} else {
								// remove existing item
								final Iterator<Entry<NumberRange, Integer>> it = mapToCompare.entrySet().iterator();
								Entry<NumberRange, Integer> valueToRemove = null;
								for (int j = 0; j < rnd.nextInt(length) + 1; j++) {
									valueToRemove = it.next();
								}
								it.remove();
								currentRecordSet.remove(valueToRemove.getValue());
								uniqueValues.remove(valueToRemove.getKey());

								ops.append("filterIndex.removeRecord(\"").append(valueToRemove.getValue()).append("\",").append(valueToRemove.getKey()).append(");\n");
								filterIndex.removeRecord(valueToRemove.getValue(), valueToRemove.getKey());
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
						committed.getAllRecords().getArray(),
						"\nExpected: " + Arrays.toString(expected) + "\n" +
							"Actual:  " + Arrays.toString(committed.getAllRecords().getArray()) + "\n\n" +
							ops
					);

					transactionalFilterIndex.set(
						new FilterIndex(
							committed.getHistogram(),
							committed.getRangeIndex()
						)
					);
					ops.setLength(0);
					ops.append("final FilterIndex filterIndex = new FilterIndex(String.class);\n")
						.append(mapToCompare.entrySet().stream().map(it -> "filterIndex.addRecord(\"" + it.getValue() + "\"," + it.getKey() + ");").collect(Collectors.joining("\n")));
					ops.append("\nOps:\n");
				}
			);
			if (iteration++ % 100 == 0) {
				System.out.print(".");
				System.out.flush();
			}
			if (iteration % 5_000 == 0) {
				System.out.print("\n");
				System.out.flush();
			}
		} while (true);
	}

	private void fillStringAttribute() {
		stringAttribute.addRecord(1, new String[]{"A", "B", "C"});
		stringAttribute.addRecord(2, new String[]{"A", "B"});
		stringAttribute.addRecord(3, "C");
		stringAttribute.addRecord(4, "D");
		assertArrayEquals(new int[]{1, 2}, stringAttribute.getRecordsEqualTo("A").getArray());
		assertArrayEquals(new int[]{1, 2}, stringAttribute.getRecordsEqualTo("B").getArray());
		assertArrayEquals(new int[]{1, 3}, stringAttribute.getRecordsEqualTo("C").getArray());
		assertArrayEquals(new int[]{4}, stringAttribute.getRecordsEqualTo("D").getArray());
		assertFalse(stringAttribute.isEmpty());
	}

	private void fillRangeAttribute() {
		rangeAttribute.addRecord(1, new NumberRange[] {NumberRange.between(5, 10), NumberRange.between(50, 90)});
		rangeAttribute.addRecord(2, NumberRange.between(11, 20));
		rangeAttribute.addRecord(3, NumberRange.between(5, 15));
		assertArrayEquals(new int[] {1}, rangeAttribute.getRecordsEqualTo(NumberRange.between(5, 10)).getArray());
		assertArrayEquals(new int[] {1}, rangeAttribute.getRecordsEqualTo(NumberRange.between(50, 90)).getArray());
		assertArrayEquals(new int[] {2}, rangeAttribute.getRecordsEqualTo(NumberRange.between(11, 20)).getArray());
		assertArrayEquals(new int[] {3}, rangeAttribute.getRecordsEqualTo(NumberRange.between(5, 15)).getArray());
		assertEquals(3, rangeAttribute.getAllRecords().size());
		assertFalse(rangeAttribute.isEmpty());
	}

}