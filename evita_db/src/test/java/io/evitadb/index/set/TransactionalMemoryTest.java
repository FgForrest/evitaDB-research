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

package io.evitadb.index.set;

import io.evitadb.index.map.TransactionalMemoryMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link TransactionalMemoryMap} implementation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
class TransactionalMemoryTest {
	private TransactionalMemorySet<String> tested;

	@BeforeEach
	void setUp() {
		HashSet<String> underlyingData = new LinkedHashSet<>();
		underlyingData.add("a");
		underlyingData.add("b");
		tested = new TransactionalMemorySet<>(underlyingData);
	}

	@Test
	void shouldNotModifyOriginalStateButCreateModifiedCopy() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.add("a");
				original.add("c");
				assertSetContains(original, "a", "b", "c");
			},
			(original, committedVersion) -> {
				assertSetContains(original, "a", "b");
				assertSetContains(committedVersion, "a", "b", "c");
			}
		);
	}

	@Test
	void removalsShouldNotModifyOriginalState() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.remove("a");
				original.add("c");
				assertSetContains(original, "b", "c");
			},
			(original, committedVersion) -> {
				assertSetContains(original, "a", "b");
				assertSetContains(committedVersion, "b", "c");
			}
		);
	}

	@Test
	void verify() {
		/*
		 * START: Q,b,S,3,c,T,e,6,J
		 * */

		tested.clear();
		tested.add("Q");
		tested.add("b");
		tested.add("S");
		tested.add("3");
		tested.add("c");
		tested.add("T");
		tested.add("e");
		tested.add("6");
		tested.add("J");

		final HashSet<String> referenceMap = new HashSet<>(tested);

		assertStateAfterCommit(
			tested,
			original -> {

				/* +D#0+D */

				original.add("D");
				referenceMap.add("D");
				final Iterator<String> it = original.iterator();
				final String entry = it.next();
				it.remove();
				referenceMap.remove(entry);
				original.add("D");
				referenceMap.add("D");

				assertSetContains(original, referenceMap.toArray(String[]::new));
			},
			(original, committedVersion) -> {
				assertSetContains(committedVersion, referenceMap.toArray(String[]::new));
			}
		);
	}

	@Test
	void shouldMergeRemovalsAndUpdatesAndInsertionsOnTransactionCommit() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.remove("a");
				original.add("b");
				original.add("c");

				assertSetContains(original, "b", "c");
			},
			(original, committedVersion) -> {
				assertSetContains(original, "a", "b");
				assertSetContains(committedVersion, "b", "c");
			}
		);
	}

	@Test
	void shouldInterpretIsEmptyCorrectly() {
		assertStateAfterCommit(
			tested,
			original -> {
				assertFalse(original.isEmpty());

				original.add("c");
				assertFalse(original.isEmpty());

				original.remove("a");
				assertFalse(original.isEmpty());

				original.remove("c");
				assertFalse(original.isEmpty());

				original.remove("b");
				assertTrue(original.isEmpty());

				original.add("d");
				assertFalse(original.isEmpty());

				original.remove("d");
				assertTrue(original.isEmpty());

				assertSetContains(original);
			},
			(original, committedVersion) -> {
				assertSetContains(original, "a", "b");
				assertSetContains(committedVersion);
			}
		);
	}

	@Test
	void shouldNotModifyOriginalStateOnKeySetIteratorRemoval() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.add("c");

				final Iterator<String> it = original.iterator();
				//noinspection Java8CollectionRemoveIf
				while (it.hasNext()) {
					final String key = it.next();
					if (key.equals("b")) {
						it.remove();
					}
				}

				assertSetContains(original, "a", "c");
			},
			(original, committedVersion) -> {
				assertSetContains(original, "a", "b");
				assertSetContains(committedVersion, "a", "c");
			}
		);
	}

	@Test
	void shouldRemoveValuesWhileIteratingOverThem() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.clear();
				original.add("ac");
				original.add("bc");
				original.add("ad");
				original.add("ae");

				original.removeIf(key -> key.contains("a"));

				assertSetContains(original, "bc");
			},
			(original, committedVersion) -> {
				assertSetContains(original, "a", "b");
				assertSetContains(committedVersion, "bc");
			}
		);
	}

	@Test
	void shouldKeepIteratorContract() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.add("c");

				final List<String> result = new ArrayList<>(3);

				final Iterator<String> it = original.iterator();
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next());
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next());
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next());
				for (int i = 0; i < 50; i++) {
					assertFalse(it.hasNext());
				}

				assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), new HashSet<>(result));

				try {
					it.next();
					fail("Exception expected!");
				} catch (NoSuchElementException ex) {
					//ok
				}
			},
			(original, committedVersion) -> {
				// do nothing
			}
		);
	}

	@Test
	void shouldKeepIteratorContractWhenItemsRemoved() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.add("c");
				original.remove("b");

				final List<String> result = new ArrayList<>(3);

				final Iterator<String> it = original.iterator();
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next());
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next());
				for (int i = 0; i < 50; i++) {
					assertFalse(it.hasNext());
				}

				assertEquals(new HashSet<>(Arrays.asList("a", "c")), new HashSet<>(result));

				try {
					it.next();
					fail("Exception expected!");
				} catch (NoSuchElementException ex) {
					//ok
				}
			},
			(original, committedVersion) -> {
				// do nothing
			}
		);
	}

	@Disabled("This infinite test performs random operations on trans. list and normal list and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random();
		final int initialCount = 100;
		final Set<String> initialMap = generateRandomInitialSet(rnd, initialCount);
		final AtomicReference<TransactionalMemorySet<String>> transactionalMap = new AtomicReference<>(new TransactionalMemorySet<>(initialMap));
		final Set<String> referenceMap = new HashSet<>(initialMap);

		final StringBuilder sb = new StringBuilder();
		try {
			int iteration = 0;
			do {
				assertStateAfterCommit(
					transactionalMap.get(),
					original -> {
						sb.setLength(0);
						final TransactionalMemorySet<String> txMap = transactionalMap.get();
						sb.append("\nSTART: ").append(String.join(",", txMap)).append("\n");

						final int operationsInTransaction = rnd.nextInt(5);
						for (int i = 0; i < operationsInTransaction; i++) {
							final int length = txMap.size();
							assertEquals(referenceMap.size(), length);
							final int operation = rnd.nextInt(3);
							if ((operation == 0 || length < 10) && length < 120) {
								// insert / update item
								final String newRecKey = String.valueOf((char) (40 + rnd.nextInt(64)));
								txMap.add(newRecKey);
								referenceMap.add(newRecKey);
								sb.append("+").append(newRecKey);
							} else if (operation == 1) {
								String recKey = null;
								final int index = rnd.nextInt(length);
								final Iterator<String> it = referenceMap.iterator();
								for (int j = 0; j <= index; j++) {
									final String key = it.next();
									if (j == index) {
										recKey = key;
									}
								}
								sb.append("-").append(recKey);
								txMap.remove(recKey);
								referenceMap.remove(recKey);
							} else {
								// remove existing item by iterator
								final int updateIndex = rnd.nextInt(length);
								sb.append("#").append(updateIndex);
								final Iterator<String> it = txMap.iterator();
								for (int j = 0; j <= updateIndex; j++) {
									final String entry = it.next();
									if (j == updateIndex) {
										it.remove();
										referenceMap.remove(entry);
									}
								}
							}
						}
						sb.append("\n");
					},
					(original, committed) -> {
						assertSetContains(committed, referenceMap.toArray(String[]::new));
						transactionalMap.set(new TransactionalMemorySet<>(committed));
					}
				);
				if (iteration++ % 1000 == 0) {
					System.out.print(".");
					System.out.flush();
				}
				if (iteration % 50_000 == 0) {
					System.out.print("\n");
					System.out.flush();
				}
			} while (true);
		} catch (Throwable ex) {
			System.out.println(sb);
			throw ex;
		}
	}

	private Set<String> generateRandomInitialSet(Random rnd, int count) {
		final Set<String> initialArray = new HashSet<>(count);
		for (int i = 0; i < count; i++) {
			final String recKey = String.valueOf((char) (40 + rnd.nextInt(64)));
			initialArray.add(recKey);
		}
		return initialArray;
	}

	@SuppressWarnings("WhileLoopReplaceableByForEach")
	private static void assertSetContains(Set<String> set, String... data) {
		if (data.length == 0) {
			assertTrue(set.isEmpty());
		} else {
			assertFalse(set.isEmpty());
		}

		assertEquals(data.length, set.size());

		final Set<String> expectedSet = new HashSet<>(data.length);
		for (String dataItem : data) {
			expectedSet.add(dataItem);
			assertTrue(set.contains(dataItem));
		}

		final Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			final String entry = it.next();
			assertTrue(expectedSet.contains(entry));
		}
	}

}
