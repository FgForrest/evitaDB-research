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

import io.evitadb.api.exception.UniqueValueViolationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test verifies contract of {@link UniqueIndex}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class UniqueIndexTest {
	private final UniqueIndex tested = new UniqueIndex("whatever", String.class, new HashMap<>());

	@Test
	void shouldRegisterUniqueValueAndRetrieveItBack() {
		tested.registerUniqueKey("A", 1);
		assertEquals(1, tested.getRecordIdByUniqueValue("A"));
		assertNull(tested.getRecordIdByUniqueValue("B"));
	}

	@Test
	void shouldFailToRegisterDuplicateValues() {
		tested.registerUniqueKey("A", 1);
		assertThrows(UniqueValueViolationException.class, () -> tested.registerUniqueKey("A", 2));
	}

	@Test
	void shouldUnregisterPreviouslyRegisteredValue() {
		tested.registerUniqueKey("A", 1);
		assertEquals(1, tested.unregisterUniqueKey("A", 1));
		assertNull(tested.getRecordIdByUniqueValue("A"));
	}

	@Test
	void shouldFailToUnregisterUnknownValue() {
		assertThrows(IllegalArgumentException.class, () -> tested.unregisterUniqueKey("B", 1));
	}

	@Test
	void shouldRegisterAndPartialUnregisterValues() {
		tested.registerUniqueKey(new String[]{"A", "B", "C"}, 1);
		assertEquals(1, tested.getRecordIdByUniqueValue("A"));
		assertEquals(1, tested.getRecordIdByUniqueValue("B"));
		assertEquals(1, tested.getRecordIdByUniqueValue("C"));
		tested.unregisterUniqueKey(new String[]{"B", "C"}, 1);
		assertEquals(1, tested.getRecordIdByUniqueValue("A"));
		assertNull(tested.getRecordIdByUniqueValue("B"));
		assertNull(tested.getRecordIdByUniqueValue("C"));
	}

	@Disabled("This infinite test performs random operations sort index and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random();
		final int initialCount = 100;
		final Map<String, Integer> mapToCompare = new HashMap<>();
		final Set<Integer> currentRecordSet = new HashSet<>();
		final AtomicReference<UniqueIndex> transactionalUniqueIndex = new AtomicReference<>(new UniqueIndex("code", String.class));
		final StringBuilder ops = new StringBuilder("final UniqueIndex uniqueIndex = new UniqueIndex(\"code\", String.class);\n");

		int iteration = 0;
		do {
			int finalIteration = iteration;
			assertStateAfterCommit(
				transactionalUniqueIndex.get(),
				original -> {
					try {
						final int operationsInTransaction = rnd.nextInt(100);
						for (int i = 0; i < operationsInTransaction; i++) {
							final UniqueIndex uniqueIndex = transactionalUniqueIndex.get();
							final int length = uniqueIndex.size();
							if ((rnd.nextBoolean() || length < 10) && length < 50) {
								// insert new item
								final String newValue = Character.toString(65 + rnd.nextInt(28)) + "_" + ((finalIteration * 100) + i);
								int newRecId;
								do {
									newRecId = rnd.nextInt(initialCount * 2);
								} while (currentRecordSet.contains(newRecId));
								mapToCompare.put(newValue, newRecId);
								currentRecordSet.add(newRecId);

								ops.append("uniqueIndex.registerUniqueKey(\"").append(newValue).append("\",").append(newRecId).append(");\n");
								uniqueIndex.registerUniqueKey(newValue, newRecId);
							} else {
								// remove existing item
								final Iterator<Entry<String, Integer>> it = mapToCompare.entrySet().iterator();
								Entry<String, Integer> valueToRemove = null;
								for (int j = 0; j < rnd.nextInt(length) + 1; j++) {
									valueToRemove = it.next();
								}
								it.remove();
								currentRecordSet.remove(valueToRemove.getValue());

								ops.append("uniqueIndex.unregisterUniqueKey(\"").append(valueToRemove.getKey()).append("\",").append(valueToRemove.getValue()).append(");\n");
								uniqueIndex.unregisterUniqueKey(valueToRemove.getKey(), valueToRemove.getValue());
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
						committed.getRecordIds().getArray(),
						"\nExpected: " + Arrays.toString(expected) + "\n" +
							"Actual:  " + Arrays.toString(committed.getRecordIds().getArray()) + "\n\n" +
							ops
					);

					transactionalUniqueIndex.set(
						new UniqueIndex(
							committed.getName(),
							committed.getType(),
							committed.getUniqueValueToRecordId(),
							committed.getRecordIds()
						)
					);
					ops.setLength(0);
					ops.append("final UniqueIndex uniqueIndex = new UniqueIndex(\"code\", String.class);\n")
						.append(mapToCompare.entrySet().stream().map(it -> "uniqueIndex.registerUniqueKey(\"" + it.getKey() + "\"," + it.getValue() + ");").collect(Collectors.joining("\n")));
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

}