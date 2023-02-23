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

import io.evitadb.query.sort.SortedRecordsSupplierFactory.SortedRecordsSupplier;
import lombok.Data;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link SortIndex}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class SortIndexTest {

	@Test
	void shouldCreateIndexWithDifferentCardinalities() {
		final SortIndex sortIndex = createIndexWithBaseCardinalities();
		assertNull(sortIndex.valueCardinalities.get("Z"));
		assertNull(sortIndex.valueCardinalities.get("A"));
		assertEquals(2, sortIndex.valueCardinalities.get("B"));
		assertEquals(3, sortIndex.valueCardinalities.get("C"));
		assertArrayEquals(new String[] {"A", "B", "C"}, sortIndex.sortedRecordsValues.getArray());
		assertArrayEquals(new int[] {6, 4, 5, 1, 2, 3}, sortIndex.sortedRecords.getArray());
	}

	@Test
	void shouldAlterIndexWithDifferentCardinalities() {
		final SortIndex sortIndex = createIndexWithBaseCardinalities();
		sortIndex.removeRecord("A", 6);
		sortIndex.removeRecord("B", 4);
		sortIndex.removeRecord("C", 1);
		assertNull(sortIndex.valueCardinalities.get("Z"));
		assertNull(sortIndex.valueCardinalities.get("A"));
		assertNull(sortIndex.valueCardinalities.get("B"));
		assertEquals(2, sortIndex.valueCardinalities.get("C"));
		assertArrayEquals(new String[] {"B", "C"}, sortIndex.sortedRecordsValues.getArray());
		assertArrayEquals(new int[] {5, 2, 3}, sortIndex.sortedRecords.getArray());
	}

	@Test
	void shouldIndexRecordsAndReturnInAscendingOrder() {
		final SortIndex sortIndex = new SortIndex(Integer.class);
		sortIndex.addRecord(7, 2);
		sortIndex.addRecord(3, 4);
		sortIndex.addRecord(4, 3);
		sortIndex.addRecord(9, 1);
		sortIndex.addRecord(1, 5);
		final SortedRecordsSupplier ascendingOrderRecordsSupplier = sortIndex.getAscendingOrderRecordsSupplier();
		assertArrayEquals(
			new int[] {5, 4, 3, 2, 1},
			ascendingOrderRecordsSupplier.getSortedRecordIds()
		);
	}

	@Test
	void shouldIndexRecordsAndReturnInDescendingOrder() {
		final SortIndex sortIndex = new SortIndex(Integer.class);
		sortIndex.addRecord(7, 2);
		sortIndex.addRecord(3, 4);
		sortIndex.addRecord(4, 3);
		sortIndex.addRecord(9, 1);
		sortIndex.addRecord(1, 5);
		final SortedRecordsSupplier ascendingOrderRecordsSupplier = sortIndex.getDescendingOrderRecordsSupplier();
		assertArrayEquals(
			new int[] {1, 2, 3, 4, 5},
			ascendingOrderRecordsSupplier.getSortedRecordIds()
		);
	}

	@Test
	void shouldPassGenerationalTest1() {
		final SortIndex sortIndex = new SortIndex(String.class);
		sortIndex.addRecord("W" ,49);
		sortIndex.addRecord("Z" ,150);
		sortIndex.addRecord("[" ,175);
		sortIndex.addRecord("E" ,26);
		sortIndex.addRecord("I" ,141);
		sortIndex.addRecord("T" ,131);
		sortIndex.addRecord("G" ,186);
		sortIndex.addRecord("X" ,139);
		sortIndex.addRecord("C" ,177);
		sortIndex.addRecord("L" ,126);

		assertArrayEquals(
			new int[] {177, 26, 186, 141, 126, 131, 49, 139, 150, 175},
			sortIndex.getAscendingOrderRecordsSupplier().getSortedRecordIds()
		);
	}

	@Disabled("This infinite test performs random operations sort index and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random();
		final int initialCount = 100;
		final TreeSet<ValueRecord> setToCompare = new TreeSet<>();
		final Set<Integer> currentRecordSet = new HashSet<>();
		final AtomicReference<SortIndex> transactionalSortIndex = new AtomicReference<>(new SortIndex(String.class));
		final StringBuilder ops = new StringBuilder("final SortIndex sortIndex = new SortIndex(String.class);\n");

		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalSortIndex.get(),
				original -> {
					try {
						final int operationsInTransaction = rnd.nextInt(100);
						for (int i = 0; i < operationsInTransaction; i++) {
							final SortIndex sortIndex = transactionalSortIndex.get();
							final int length = sortIndex.size();
							if ((rnd.nextBoolean() || length < 10) && length < 50) {
								// insert new item
								final String newValue = Character.toString(65 + rnd.nextInt(28));
								int newRecId;
								do {
									newRecId = rnd.nextInt(initialCount * 2);
								} while (currentRecordSet.contains(newRecId));
								setToCompare.add(new ValueRecord(newValue, newRecId));
								currentRecordSet.add(newRecId);

								ops.append("sortIndex.addRecord(\"").append(newValue).append("\",").append(newRecId).append(");\n");
								sortIndex.addRecord(newValue, newRecId);
							} else {
								// remove existing item
								final Iterator<ValueRecord> it = setToCompare.iterator();
								ValueRecord valueToRemove = null;
								for (int j = 0; j < rnd.nextInt(length) + 1; j++) {
									valueToRemove = it.next();
								}
								it.remove();
								currentRecordSet.remove(valueToRemove.getRecordId());

								ops.append("sortIndex.removeRecord(\"").append(valueToRemove.getValue()).append("\",").append(valueToRemove.getRecordId()).append(");\n");
								sortIndex.removeRecord(valueToRemove.getValue(), valueToRemove.getRecordId());
							}
						}
					} catch (Exception ex) {
						fail("\n" + ops, ex);
					}
				},
				(original, committed) -> {
					final int[] expected = setToCompare.stream().mapToInt(ValueRecord::getRecordId).toArray();
					assertArrayEquals(
						expected,
						committed.getAscendingOrderRecordsSupplier().getSortedRecordIds(),
						"\nExpected: " + Arrays.toString(expected) + "\n" +
							"Actual:  " + Arrays.toString(committed.getAscendingOrderRecordsSupplier().getSortedRecordIds()) + "\n\n" +
							ops
					);

					transactionalSortIndex.set(
						new SortIndex(
							committed.getType(),
							committed.sortedRecords.getArray(),
							committed.sortedRecordsValues.getArray(),
							committed.valueCardinalities
						)
					);
					ops.setLength(0);
					ops.append("final SortIndex sortIndex = new SortIndex(String.class);\n")
						.append(setToCompare.stream().map(it -> "sortIndex.addRecord(\"" + it.getValue() + "\"," + it.getRecordId() +");").collect(Collectors.joining("\n")));
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

	@Nonnull
	private SortIndex createIndexWithBaseCardinalities() {
		final SortIndex sortIndex = new SortIndex(String.class);
		sortIndex.addRecord("B", 5);
		sortIndex.addRecord("A", 6);
		sortIndex.addRecord("C", 3);
		sortIndex.addRecord("C", 2);
		sortIndex.addRecord("B", 4);
		sortIndex.addRecord("C", 1);
		return sortIndex;
	}

	@Data
	private static class ValueRecord implements Comparable<ValueRecord> {
		private final String value;
		private final int recordId;

		@Override
		public int compareTo(ValueRecord o) {
			final int cmp1 = value.compareTo(o.value);
			return cmp1 == 0 ? Integer.compare(recordId, o.recordId) : cmp1;
		}

	}
}