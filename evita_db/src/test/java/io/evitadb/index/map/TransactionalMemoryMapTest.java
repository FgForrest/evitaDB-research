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

package io.evitadb.index.map;

import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link TransactionalMemoryMap} implementation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
class TransactionalMemoryMapTest {
	private TransactionalMemoryMap<String, Integer> tested;

	@BeforeEach
	void setUp() {
		HashMap<String, Integer> underlyingData = new LinkedHashMap<>();
		underlyingData.put("a", 1);
		underlyingData.put("b", 2);
		tested = new TransactionalMemoryMap<>(underlyingData);
	}

	@Test
	void shouldNotModifyOriginalStateButCreateModifiedCopy() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("a", 3);
				original.put("c", 3);
				assertMapContains(original, new Tuple("a", 3), new Tuple("b", 2), new Tuple("c", 3));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("a", 3), new Tuple("b", 2), new Tuple("c", 3));
			}
		);
	}

	@Test
	void removalsShouldNotModifyOriginalState() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.remove("a");
				original.put("c", 3);
				assertMapContains(original, new Tuple("b", 2), new Tuple("c", 3));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("b", 2), new Tuple("c", 3));
			}
		);
	}

	@Test
	void verify() {
		/*
		* START: Q: 33,b: 29,S: 185,3: 86,c: 110,T: 181,e: 38,6: 91,J: 65
		* */

		tested.clear();
		tested.put("Q", 33);
		tested.put("b", 29);
		tested.put("S", 185);
		tested.put("3", 86);
		tested.put("c", 110);
		tested.put("T", 181);
		tested.put("e", 38);
		tested.put("6", 91);
		tested.put("J", 65);

		final HashMap<String, Integer> referenceMap = new HashMap<>(tested);

		assertStateAfterCommit(
			tested,
			original -> {

				/* +D:18#0+D:72 */

				original.put("D", 18);
				referenceMap.put("D", 18);
				final Iterator<Entry<String, Integer>> it = original.entrySet().iterator();
				final Entry<String, Integer> entry = it.next();
				it.remove();
				referenceMap.remove(entry.getKey());
				original.put("D", 72);
				referenceMap.put("D", 72);

				assertMapContains(original, referenceMap.entrySet().stream().map(x -> new Tuple(x.getKey(), x.getValue())).toArray(Tuple[]::new));
			},
			(original, committedVersion) -> {
				assertMapContains(committedVersion, referenceMap.entrySet().stream().map(x -> new Tuple(x.getKey(), x.getValue())).toArray(Tuple[]::new));
			}
		);
	}

	@Test
	void shouldMergeRemovalsAndUpdatesAndInsertionsOnTransactionCommit() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.remove("a");
				original.put("b", 3);
				original.put("c", 3);

				assertMapContains(original, new Tuple("b", 3), new Tuple("c", 3));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("b", 3), new Tuple("c", 3));
			}
		);
	}

	@Test
	void shouldInterpretIsEmptyCorrectly() {
		assertStateAfterCommit(
			tested,
			original -> {
				assertFalse(original.isEmpty());

				original.put("c", 3);
				assertFalse(original.isEmpty());

				original.remove("a");
				assertFalse(original.isEmpty());

				original.remove("c");
				assertFalse(original.isEmpty());

				original.remove("b");
				assertTrue(original.isEmpty());

				original.put("d", 4);
				assertFalse(original.isEmpty());

				original.remove("d");
				assertTrue(original.isEmpty());

				assertMapContains(original);
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion);
			}
		);
	}

	@Test
	void shouldProduceValidValueCollection() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);
				original.remove("b");

				final Set<Integer> result = new HashSet<>(original.values());
				assertEquals(2, result.size());
				assertTrue(result.contains(1));
				assertTrue(result.contains(3));
			},
			(original, committedVersion) -> {
				final Set<Integer> result = new HashSet<>(committedVersion.values());
				assertEquals(2, result.size());
				assertTrue(result.contains(1));
				assertTrue(result.contains(3));
			}
		);
	}

	@Test
	void shouldProduceValidKeySet() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);
				original.remove("b");

				final Set<String> result = new HashSet<>(original.keySet());
				assertEquals(2, result.size());
				assertTrue(result.contains("a"));
				assertTrue(result.contains("c"));
			},
			(original, committedVersion) -> {
				final Set<String> result = new HashSet<>(committedVersion.keySet());
				assertEquals(2, result.size());
				assertTrue(result.contains("a"));
				assertTrue(result.contains("c"));
			}
		);
	}

	@Test
	void shouldProduceValidEntrySet() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);
				original.remove("b");

				final Set<Entry<String, Integer>> entries = new HashSet<>(original.entrySet());
				assertEquals(2, entries.size());
				assertTrue(entries.contains(new SimpleEntry<>("a", 1)));
				assertTrue(entries.contains(new SimpleEntry<>("c", 3)));
			},
			(original, committedVersion) -> {
				final Set<Entry<String, Integer>> entries = new HashSet<>(committedVersion.entrySet());
				assertEquals(2, entries.size());
				assertTrue(entries.contains(new SimpleEntry<>("a", 1)));
				assertTrue(entries.contains(new SimpleEntry<>("c", 3)));
			}
		);
	}

	@Test
	void shouldNotModifyOriginalStateOnKeySetIteratorRemoval() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);

				final Iterator<String> it = original.keySet().iterator();
				//noinspection Java8CollectionRemoveIf
				while (it.hasNext()) {
					final String key = it.next();
					if (key.equals("b")) {
						it.remove();
					}
				}

				assertMapContains(original, new Tuple("a", 1), new Tuple("c", 3));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("a", 1), new Tuple("c", 3));
			}
		);
	}

	@Test
	void shouldNotModifyOriginalStateOnValuesIteratorRemoval() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);

				final Iterator<Integer> it = original.values().iterator();
				//noinspection Java8CollectionRemoveIf
				while (it.hasNext()) {
					final Integer value = it.next();
					if (value.equals(2)) {
						it.remove();
					}
				}

				assertMapContains(original, new Tuple("a", 1), new Tuple("c", 3));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("a", 1), new Tuple("c", 3));
			}
		);
	}

	@Test
	void shouldRemoveValuesWhileIteratingOverThem() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.clear();
				original.put("ac", 1);
				original.put("bc", 2);
				original.put("ad", 3);
				original.put("ae", 4);

				original.keySet().removeIf(key -> key.contains("a"));

				assertMapContains(original, new Tuple("bc", 2));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("bc", 2));
			}
		);
	}

	@Test
	void shouldMergeChangesInEntrySetIterator() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);

				final Iterator<Entry<String, Integer>> it = original.entrySet().iterator();
				//noinspection WhileLoopReplaceableByForEach
				while (it.hasNext()) {
					final Entry<String, Integer> entry = it.next();
					if ("b".equals(entry.getKey())) {
						entry.setValue(5);
					}
				}

				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 5), new Tuple("c", 3));
			},
			(original, committedVersion) -> {
				assertMapContains(original, new Tuple("a", 1), new Tuple("b", 2));
				assertMapContains(committedVersion, new Tuple("a", 1), new Tuple("b", 5), new Tuple("c", 3));
			}
		);
	}

	@Test
	void shouldKeepIteratorContract() {
		assertStateAfterCommit(
			tested,
			original -> {
				original.put("c", 3);

				final List<String> result = new ArrayList<>(3);

				final Iterator<Entry<String, Integer>> it = original.entrySet().iterator();
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next().getKey());
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next().getKey());
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next().getKey());
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
				original.put("c", 3);
				original.remove("b");

				final List<String> result = new ArrayList<>(3);

				final Iterator<Entry<String, Integer>> it = original.entrySet().iterator();
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next().getKey());
				for (int i = 0; i < 50; i++) {
					assertTrue(it.hasNext());
				}

				result.add(it.next().getKey());
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
		final Map<String, Integer> initialMap = generateRandomInitialMap(rnd, initialCount);
		final AtomicReference<TransactionalMemoryMap<String, Integer>> transactionalMap = new AtomicReference<>(new TransactionalMemoryMap<>(initialMap));
		final Map<String, Integer> referenceMap = new HashMap<>(initialMap);

		final StringBuilder sb = new StringBuilder();
		try {
			int iteration = 0;
			do {
				assertStateAfterCommit(
					transactionalMap.get(),
					original -> {
						sb.setLength(0);
						final TransactionalMemoryMap<String, Integer> txMap = transactionalMap.get();
						sb.append("\nSTART: " + txMap.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(",")) + "\n");

						final int operationsInTransaction = rnd.nextInt(5);
						for (int i = 0; i < operationsInTransaction; i++) {
							final int length = txMap.size();
							assertEquals(referenceMap.size(), length);
							final int operation = rnd.nextInt(4);
							if ((operation == 0 || length < 10) && length < 120) {
								// insert / update item
								final String newRecKey = String.valueOf((char)(40 + rnd.nextInt(64)));
								final Integer newRecId = rnd.nextInt(initialCount * 2);
								txMap.put(newRecKey, newRecId);
								referenceMap.put(newRecKey, newRecId);
								sb.append("+" + newRecKey + ":" + newRecId);
							} else if (operation == 1) {
								String recKey = null;
								final int index = rnd.nextInt(length);
								final Iterator<String> it = referenceMap.keySet().iterator();
								for (int j = 0; j <= index; j++) {
									final String key = it.next();
									if (j == index) {
										recKey = key;
									}
								}
								sb.append("-" + recKey);
								txMap.remove(recKey);
								referenceMap.remove(recKey);
							} else if (operation == 2) {
								// update existing item by iterator
								final int updateIndex = rnd.nextInt(length);
								final Integer updatedValue = rnd.nextInt(initialCount * 2);
								sb.append("!" + updateIndex + ":" + updatedValue);
								final Iterator<Entry<String, Integer>> it = txMap.entrySet().iterator();
								for (int j = 0; j <= updateIndex; j++) {
									final Entry<String, Integer> entry = it.next();
									if (j == updateIndex) {
										entry.setValue(updatedValue);
										referenceMap.put(entry.getKey(), updatedValue);
									}
								}
							} else {
								// remove existing item by iterator
								final int updateIndex = rnd.nextInt(length);
								sb.append("#" + updateIndex);
								final Iterator<Entry<String, Integer>> it = txMap.entrySet().iterator();
								for (int j = 0; j <= updateIndex; j++) {
									final Entry<String, Integer> entry = it.next();
									if (j == updateIndex) {
										it.remove();
										referenceMap.remove(entry.getKey());
									}
								}
							}
						}
						sb.append("\n");
					},
					(original, committed) -> {
						assertMapContains(committed, referenceMap.entrySet().stream().map(it -> new Tuple(it.getKey(), it.getValue())).toArray(Tuple[]::new));
						transactionalMap.set(new TransactionalMemoryMap<>(committed));
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

	private Map<String, Integer> generateRandomInitialMap(Random rnd, int count) {
		final Map<String, Integer> initialArray = new HashMap<>(count);
		for (int i = 0; i < count; i++) {
			final String recKey = String.valueOf((char)(40 + rnd.nextInt(64)));
			final int recId = rnd.nextInt(count * 2);
			initialArray.put(recKey, recId);
		}
		return initialArray;
	}

	@SuppressWarnings("WhileLoopReplaceableByForEach")
	private static void assertMapContains(Map<String, Integer> map, Tuple... data) {
		if (data.length == 0) {
			assertTrue(map.isEmpty());
		} else {
			assertFalse(map.isEmpty());
		}

		assertEquals(data.length, map.size());

		final Map<String, Integer> expectedMap = new HashMap<>(data.length);
		for (Tuple tuple : data) {
			expectedMap.put(tuple.getKey(), tuple.getValue());
			assertEquals(tuple.getValue(), map.get(tuple.getKey()));
			assertTrue(map.containsKey(tuple.getKey()));
			assertTrue(map.containsValue(tuple.getValue()));
		}

		final Iterator<Entry<String, Integer>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			final Entry<String, Integer> entry = it.next();
			assertEquals(expectedMap.get(entry.getKey()), entry.getValue());
		}

		final Iterator<String> keyIt = map.keySet().iterator();
		while (keyIt.hasNext()) {
			final String key = keyIt.next();
			assertTrue(expectedMap.containsKey(key));
		}

		final Iterator<Integer> valueIt = map.values().iterator();
		while (valueIt.hasNext()) {
			final Integer value = valueIt.next();
			assertTrue(expectedMap.containsValue(value));
		}
	}

	@Data
	private static class Tuple {
		private final String key;
		private final Integer value;

	}

}
