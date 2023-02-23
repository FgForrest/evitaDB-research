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

package io.evitadb.index.array;

import io.evitadb.api.Transaction;
import io.evitadb.api.utils.ArrayUtils.*;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.VoidTransactionMemoryProducer;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.ArrayUtils.*;
import static io.evitadb.utils.AssertionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies transactional behaviour of {@link TransactionalComplexObjArray}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
class TransactionalComplexObjArrayTest {

	@Test
	void shouldCorrectlyAddItemsOnFirstLastAndMiddlePositionsAndRollback() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 5, 10));

		assertStateAfterRollback(
			array,
			original -> {
				original.add(new TransactionalInteger(11));
				original.add(new TransactionalInteger(0));
				original.add(new TransactionalInteger(6));

				assertTransactionalObjArray(createIntegerArray(0, 1, 5, 6, 10, 11), original);
			},
			(original, committed) -> {
				assertNull(committed);
				assertTransactionalObjArray(createIntegerArray(1, 5, 10), original);
			}
		);

	}

	@Test
	void shouldCorrectlyRemoveItemsOnFirstLastAndMiddlePositionsAndRollback() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 5, 10));

		assertStateAfterRollback(
			array,
			original -> {
				original.remove(new TransactionalInteger(5));

				assertTransactionalObjArray(createIntegerArray(1, 10), original);
			},
			(original, committed) -> {
				assertNull(committed);
				assertTransactionalObjArray(createIntegerArray(1, 5, 10), original);
			}
		);
	}

	@Test
	void shouldCorrectlyAddItemsOnFirstLastAndMiddlePositionsAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 5, 10));

		assertStateAfterCommit(
			array,
			original -> {
				original.add(new TransactionalInteger(11));
				original.add(new TransactionalInteger(11));
				original.add(new TransactionalInteger(0));
				original.add(new TransactionalInteger(6));

				assertTransactionalObjArray(createIntegerArray(0, 1, 5, 6, 10, 11), original);
			},
			(original, committed) -> {
				assertArrayEquals(committed, createIntegerArray(0, 1, 5, 6, 10, 11));
				assertTransactionalObjArray(createIntegerArray(1, 5, 10), original);
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveItemsFromFirstLastAndMiddlePositionsAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(11));
				original.remove(new TransactionalInteger(1));
				original.remove(new TransactionalInteger(5));

				assertTransactionalObjArray(createIntegerArray(2, 6, 10), original);
			},
			(original, committed) -> {
				assertArrayEquals(committed, createIntegerArray(2, 6, 10));
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
			}
		);
	}

	@Test
	void shouldAddAndRemoveEverythingAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(new TransactionalInteger[0]);

		assertStateAfterCommit(
			array,
			original -> {
				original.add(new TransactionalInteger(1));
				original.add(new TransactionalInteger(5));
				original.remove(new TransactionalInteger(1));
				original.remove(new TransactionalInteger(5));

				assertTransactionalObjArray(new TransactionalInteger[0], original);
			},
			(original, committed) -> {
				assertArrayEquals(new TransactionalInteger[0], committed);
				assertTransactionalObjArray(new TransactionalInteger[0], original);
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveMultipleItemsInARowAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(6));
				original.remove(new TransactionalInteger(5));
				original.remove(new TransactionalInteger(2));

				assertTransactionalObjArray(createIntegerArray(1, 10, 11), original);
			},
			(original, committed) -> {
				assertArrayEquals(committed, createIntegerArray(1, 10, 11));
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveMultipleItemsInARowTillTheEndAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(6));
				original.remove(new TransactionalInteger(5));
				original.remove(new TransactionalInteger(10));
				original.remove(new TransactionalInteger(11));

				assertTransactionalObjArray(createIntegerArray(1, 2), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
				assertArrayEquals(committed, createIntegerArray(1, 2));
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveMultipleItemsInARowFromTheBeginningAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(2));
				original.remove(new TransactionalInteger(6));
				original.remove(new TransactionalInteger(5));
				original.remove(new TransactionalInteger(1));
				original.remove(new TransactionalInteger(10));

				assertTransactionalObjArray(createIntegerArray(11), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
				assertArrayEquals(committed, createIntegerArray(11));
			}
		);
	}

	@Test
	void shouldAddNothingAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 5, 10));

		assertStateAfterCommit(
			array,
			original -> {
				assertTransactionalObjArray(createIntegerArray(1, 5, 10), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 5, 10), original);
				assertArrayEquals(committed, createIntegerArray(1, 5, 10));
			}
		);
	}

	@Test
	void shouldCorrectlyAddMultipleItemsOnSamePositionsAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 5, 10));

		assertStateAfterCommit(
			array,
			original -> {
				original.add(new TransactionalInteger(11));
				original.add(new TransactionalInteger(6));
				original.add(new TransactionalInteger(0));
				original.add(new TransactionalInteger(3));
				original.add(new TransactionalInteger(3));
				original.add(new TransactionalInteger(7));
				original.add(new TransactionalInteger(12));
				original.add(new TransactionalInteger(2));
				original.add(new TransactionalInteger(8));

				assertTransactionalObjArray(createIntegerArray(0, 1, 2, 3, 5, 6, 7, 8, 10, 11, 12), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 5, 10), original);
				assertArrayEquals(committed, createIntegerArray(0, 1, 2, 3, 5, 6, 7, 8, 10, 11, 12));
			}
		);
	}

	@Test
	void shouldCorrectlyAddAndRemoveOnNonOverlappingPositionsAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.add(new TransactionalInteger(4));
				original.add(new TransactionalInteger(3));
				original.remove(new TransactionalInteger(10));
				original.remove(new TransactionalInteger(6));
				original.add(new TransactionalInteger(15));

				assertTransactionalObjArray(createIntegerArray(1, 2, 3, 4, 5, 11, 15), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
				assertArrayEquals(committed, createIntegerArray(1, 2, 3, 4, 5, 11, 15));
			}
		);
	}

	@Test
	void shouldCorrectlyAddAndRemoveSameNumberAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.add(new TransactionalInteger(4));
				original.remove(new TransactionalInteger(4));

				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
				assertArrayEquals(committed, createIntegerArray(1, 2, 5, 6, 10, 11));
			}
		);
	}

	@Test
	void shouldCorrectlyRemoveAndAddSameNumberAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(5));
				original.remove(new TransactionalInteger(10));
				original.remove(new TransactionalInteger(11));
				original.add(new TransactionalInteger(10));
				original.add(new TransactionalInteger(11));
				original.add(new TransactionalInteger(5));

				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
				assertArrayEquals(committed, createIntegerArray(1, 2, 5, 6, 10, 11));
			}
		);

	}

	@Test
	void shouldCorrectlyAddAndRemoveOnOverlappingBoundaryPositionsAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2, 5, 6, 10, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(1));
				original.remove(new TransactionalInteger(11));
				original.add(new TransactionalInteger(0));
				original.add(new TransactionalInteger(12));

				assertTransactionalObjArray(createIntegerArray(0, 2, 5, 6, 10, 12), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2, 5, 6, 10, 11), original);
				assertArrayEquals(committed, createIntegerArray(0, 2, 5, 6, 10, 12));
			}
		);
	}

	@Test
	void shouldCorrectlyAddAndRemoveOnOverlappingMiddlePositionsAndCommit() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 5, 8, 11));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(5));
				original.remove(new TransactionalInteger(8));
				original.add(new TransactionalInteger(6));
				original.add(new TransactionalInteger(7));
				original.add(new TransactionalInteger(8));
				original.add(new TransactionalInteger(9));
				original.add(new TransactionalInteger(10));

				assertTransactionalObjArray(createIntegerArray(1, 6, 7, 8, 9, 10, 11), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 5, 8, 11), original);
				assertArrayEquals(committed, createIntegerArray(1, 6, 7, 8, 9, 10, 11));
			}
		);
	}

	@Test
	void shouldProperlyHandleChangesOnSinglePositionWithInt() {
		final TransactionalComplexObjArray<TransactionalInteger> array = new TransactionalComplexObjArray<>(createIntegerArray(1, 2));

		assertStateAfterCommit(
			array,
			original -> {
				original.remove(new TransactionalInteger(1));
				original.remove(new TransactionalInteger(2));
				original.add(new TransactionalInteger(2));
				original.add(new TransactionalInteger(4));
				original.remove(new TransactionalInteger(2));
				original.add(new TransactionalInteger(5));

				assertTransactionalObjArray(createIntegerArray(4, 5), original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(createIntegerArray(1, 2), original);
				assertArrayEquals(committed, createIntegerArray(4, 5));
			}
		);
	}

	@Test
	void shouldCorrectlyMergeInnerValuesOnAddToEmptyArray() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[0],
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2, 3, 4),
				new DistinctValueHolder("B", 8),
				new DistinctValueHolder("C", 5, 6)
			},
			original -> {
				original.add(new DistinctValueHolder("A", 1, 2));
				original.add(new DistinctValueHolder("B", 8));
				original.add(new DistinctValueHolder("A", 3, 4));
				original.add(new DistinctValueHolder("C", 5, 6));
			}
		);
	}

	@Test
	void shouldCorrectlyMergeInnerValuesOnAddToFilledArray() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 8)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2, 3, 4),
				new DistinctValueHolder("B", 8),
				new DistinctValueHolder("C", 5, 6)
			},
			original -> {
				original.add(new DistinctValueHolder("A", 3, 4));
				original.add(new DistinctValueHolder("C", 5, 6));
			}
		);
	}

	@Test
	void shouldCorrectlyMergeInnerValuesOnRemovalFromFilledArray() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2, 3),
				new DistinctValueHolder("B", 4),
				new DistinctValueHolder("C", 5, 6)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 3),
				new DistinctValueHolder("B", 4)
			},
			original -> {
				original.remove(new DistinctValueHolder("A", 1));
				original.remove(new DistinctValueHolder("A", 2));
				original.remove(new DistinctValueHolder("C", 5, 6));
			}
		);
	}

	@Test
	void shouldCorrectlyMergeInnerValuesOnRemovalFromFilledArrayWithAddedValues() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3),
				new DistinctValueHolder("C", 4)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 3),
				new DistinctValueHolder("B", 3),
				new DistinctValueHolder("C", 5, 6),
				new DistinctValueHolder("E", 8),
				new DistinctValueHolder("F", 9),
			},
			original -> {
				original.add(new DistinctValueHolder("A", 3));
				original.add(new DistinctValueHolder("C", 5, 6));
				original.add(new DistinctValueHolder("D", 7));
				original.add(new DistinctValueHolder("E", 8));
				original.add(new DistinctValueHolder("F", 9));
				original.remove(new DistinctValueHolder("A", 1));
				original.remove(new DistinctValueHolder("A", 2));
				original.remove(new DistinctValueHolder("C", 4));
				original.remove(new DistinctValueHolder("D", 7));
			}
		);
	}

	@Test
	void shouldCorrectlyMergeInnerValuesOnRemovalFromFilledArrayWithAddedValuesOnSamePosition() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("B", 1, 2),
				new DistinctValueHolder("C", 4)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", -1)
			},
			original -> {
				original.add(new DistinctValueHolder("B", 3));
				original.add(new DistinctValueHolder("A", -1));
				original.remove(new DistinctValueHolder("B", 1, 2, 3));
				original.remove(new DistinctValueHolder("C", 4));
			}
		);
	}

	@Test
	void shouldCorrectlyMergeInnerValuesOnRemovalFromFilledArrayWithAddedValuesOnSamePositionVariant2() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("B", 1, 2),
				new DistinctValueHolder("C", 4)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", -1),
				new DistinctValueHolder("B", 1)
			},
			original -> {
				original.add(new DistinctValueHolder("B", 3));
				original.add(new DistinctValueHolder("A", -1));
				original.remove(new DistinctValueHolder("B", 2, 3));
				original.remove(new DistinctValueHolder("C", 4));
			}
		);
	}

	@Test
	void shouldLeaveArrayEmptyWhenInsertionsAndRemovalsAreMatching() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[0],
			new DistinctValueHolder[0],
			original -> {
				original.add(new DistinctValueHolder("A", 1, 2));
				original.remove(new DistinctValueHolder("A", 1));
				original.remove(new DistinctValueHolder("A", 2));
				original.add(new DistinctValueHolder("B", 3));
				original.remove(new DistinctValueHolder("B", 3));
				original.add(new DistinctValueHolder("C", 4, 5));
				original.remove(new DistinctValueHolder("C", 4, 5));
				original.add(new DistinctValueHolder("D", 4));
				original.remove(new DistinctValueHolder("D", 4));
			}
		);
	}

	@Test
	void shouldProperlyHandleChangesOnSinglePosition() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3, 4)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("D", 3),
				new DistinctValueHolder("E", 4)
			},
			original -> {
				original.remove(new DistinctValueHolder("A", 1));
				original.remove(new DistinctValueHolder("A", 2));
				original.remove(new DistinctValueHolder("B", 3, 4));
				original.add(new DistinctValueHolder("B", 5));
				original.add(new DistinctValueHolder("D", 3));
				original.remove(new DistinctValueHolder("B", 5));
				original.add(new DistinctValueHolder("E", 4));
			}
		);
	}

	@Test
	void shouldAddMoreItemsToTheBeginning() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("C", 5, 6),
				new DistinctValueHolder("D", 7)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3, 4, 5),
				new DistinctValueHolder("C", 5, 6),
				new DistinctValueHolder("D", 7)
			},
			original -> {
				original.add(new DistinctValueHolder("A", 1));
				original.add(new DistinctValueHolder("A", 2));
				original.add(new DistinctValueHolder("B", 3, 4));
				original.add(new DistinctValueHolder("B", 5));
			}
		);
	}

	@Test
	void shouldAddMoreItemsToTheBeginningWithBeginningRemoval() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("C", 5, 6),
				new DistinctValueHolder("D", 7)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3, 4, 5),
				new DistinctValueHolder("D", 7)
			},
			original -> {
				original.add(new DistinctValueHolder("A", 1));
				original.add(new DistinctValueHolder("A", 2));
				original.remove(new DistinctValueHolder("C", 5, 6));
				original.add(new DistinctValueHolder("B", 3, 4));
				original.add(new DistinctValueHolder("B", 5));
			}
		);
	}

	@Test
	void shouldAddMoreItemsToTheEnd() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3),
				new DistinctValueHolder("C", 1, 2),
				new DistinctValueHolder("D", 3, 4, 5)
			},
			original -> {
				original.add(new DistinctValueHolder("C", 1));
				original.add(new DistinctValueHolder("D", 5));
				original.add(new DistinctValueHolder("D", 3, 4));
				original.add(new DistinctValueHolder("C", 2));
			}
		);
	}

	@Test
	void shouldAddMoreItemsToTheEndWithEndRemoval() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("B", 3)
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
				new DistinctValueHolder("C", 1, 2),
				new DistinctValueHolder("D", 3, 4, 5)
			},
			original -> {
				original.add(new DistinctValueHolder("C", 1));
				original.add(new DistinctValueHolder("D", 5));
				original.add(new DistinctValueHolder("D", 3, 4));
				original.add(new DistinctValueHolder("C", 2));
				original.remove(new DistinctValueHolder("B", 3));
			}
		);
	}

	@Test
	void shouldAddAndRemoveAllItems() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[0],
			new DistinctValueHolder[0],
			original -> {
				original.add(new DistinctValueHolder("A", 1));
				original.add(new DistinctValueHolder("B", 2));
				original.remove(new DistinctValueHolder("A", 1));
				original.remove(new DistinctValueHolder("B", 2));
			}
		);
	}

	@Test
	void shouldCorrectlyDoNothingOnRemoveAndAddFinallySameItemsAndCommit() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
			},
			original -> {
				original.remove(new DistinctValueHolder("A", 1, 2));
				original.add(new DistinctValueHolder("A", 2));
				original.add(new DistinctValueHolder("A", 1));
			}
		);
	}

	@Test
	void shouldCorrectlyDoNothingOnRemoveAndAddSameItemsAndCommit() {
		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("A", 1, 2),
			},
			original -> {
				original.remove(new DistinctValueHolder("A", 1, 2));
				original.add(new DistinctValueHolder("A", 1, 2));
			}
		);
	}

	@Test
	void verify() {

		assertDistinctValueStateAfterCommit(
			new DistinctValueHolder[]{
				new DistinctValueHolder("(", 2, 4, 5),
				new DistinctValueHolder(")", 3, 7),
				new DistinctValueHolder("*", 0, 1, 2, 3, 5, 6, 7),
				new DistinctValueHolder("+", 0, 3, 6),
				new DistinctValueHolder(",", 0, 1, 2, 3, 4, 5, 6),
				new DistinctValueHolder("-", 7),
				new DistinctValueHolder(".", 0, 1, 2, 4, 5),
				new DistinctValueHolder("/", 1, 2),
				new DistinctValueHolder("0", 0, 1, 2, 3, 4, 5, 7),
				new DistinctValueHolder("1", 0, 1, 2, 3, 5),
			},
			new DistinctValueHolder[]{
				new DistinctValueHolder("(", 0, 1, 2, 3, 4, 5),
				new DistinctValueHolder("*", 0, 1, 2, 3, 5, 6, 7),
				new DistinctValueHolder("+", 0, 3, 6),
				new DistinctValueHolder(",", 0, 1, 2, 3, 4, 5, 6),
				new DistinctValueHolder("-", 0, 4, 5, 7),
				new DistinctValueHolder(".", 1, 2, 4, 5),
				new DistinctValueHolder("/", 2),
				new DistinctValueHolder("0", 0, 1, 2, 3, 4, 5, 7),
				new DistinctValueHolder("1", 0, 2, 3),
			},
			original -> {
				original.add(new DistinctValueHolder("1", 2, 3));
				original.remove(new DistinctValueHolder("/", 1));
				original.remove(new DistinctValueHolder(".", 0));
				original.remove(new DistinctValueHolder("1", 1, 5));
				original.remove(new DistinctValueHolder("-"));
				original.remove(new DistinctValueHolder(")", 3, 7));
				original.add(new DistinctValueHolder("(", 0, 1, 3));
				original.add(new DistinctValueHolder("-", 0, 4, 5));
			}
		);
	}

	@Disabled("This infinite test performs random operations on trans. list and normal list and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random();
		final int initialCount = 20;
		final int subCount = 30;
		final DistinctValueHolder[] initialArray = generateRandomInitialArray(rnd, initialCount, subCount);
		final AtomicReference<TransactionalComplexObjArray<DistinctValueHolder>> transactionalArray = new AtomicReference<>(
			new TransactionalComplexObjArray<>(
				initialArray,
				DistinctValueHolder::combineWith,
				DistinctValueHolder::subtract,
				DistinctValueHolder::isEmpty,
				DistinctValueHolder::equals
			)
		);
		final AtomicReference<DistinctValueHolder[]> nextArrayToCompare = new AtomicReference<>(initialArray);

		final StringBuilder sb = new StringBuilder();
		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalArray.get(),
				original -> {

					sb.setLength(0);
					sb.append("\nSTART:\n" + Arrays.stream(nextArrayToCompare.get()).map(DistinctValueHolder::toString).collect(Collectors.joining("\n")) + "\n\n");

					final TransactionalComplexObjArray<DistinctValueHolder> txArray = transactionalArray.get();
					final int operationsInTransaction = rnd.nextInt(10);
					for (int i = 0; i < operationsInTransaction; i++) {
						if (rnd.nextBoolean() || txArray.getLength() < 10) {
							// upsert new item
							final String recKey = String.valueOf((char) (40 + rnd.nextInt(initialCount * 2)));
							final DistinctValueHolder upsertItem = new DistinctValueHolder(recKey, generateRandomArray(rnd, rnd.nextInt(subCount)));
							sb.append("+ " + upsertItem + "\n");
							final int txPosition = txArray.addReturningIndex(upsertItem);
							final DistinctValueHolder[] referenceArray = nextArrayToCompare.get();
							final InsertionPosition position = computeInsertPositionOfObjInOrderedArray(upsertItem, referenceArray);
							if (position.isAlreadyPresent()) {
								referenceArray[position.getPosition()] = mergeArrays(upsertItem, referenceArray[position.getPosition()]);
							} else if (!upsertItem.getValues().isEmpty()) {
								nextArrayToCompare.set(insertRecordIntoArray(upsertItem, referenceArray, position.getPosition()));
							}
							if (!upsertItem.getValues().isEmpty()) {
								assertEquals(position.getPosition(), txPosition, sb.toString());
							}
						} else {
							// remove existing item
							final int position = rnd.nextInt(txArray.getLength());
							final DistinctValueHolder removedRecId = txArray.get(position);
							final DistinctValueHolder removedItem = new DistinctValueHolder(removedRecId.getKey(), pickSomethingRandomlyFrom(rnd, removedRecId.getValues()));
							sb.append("- " + removedItem + "\n");
							txArray.remove(removedItem);
							final Integer[] restArray = sutbractArrays(removedItem.getValues(), removedRecId.getValues());
							final DistinctValueHolder[] existingArray = nextArrayToCompare.get();
							if (isEmpty(restArray)) {
								nextArrayToCompare.set(removeRecordFromOrderedArray(removedRecId, existingArray));
							} else {
								existingArray[position] = new DistinctValueHolder(removedRecId.getKey(), restArray);
							}
						}
					}

					// after operations the transactional array must match expected array
					assertTransactionalObjArray(nextArrayToCompare.get(), txArray);
				},
				(original, committed) -> {
					sb.append("\nEXPECTED:\n" + Arrays.stream(nextArrayToCompare.get()).map(DistinctValueHolder::toString).collect(Collectors.joining("\n")) + "\n");
					sb.append("\nGOT:\n" + Arrays.stream(committed).map(DistinctValueHolder::toString).collect(Collectors.joining("\n")) + "\n");
					assertArrayEquals(nextArrayToCompare.get(), committed, sb.toString());
					transactionalArray.set(
						new TransactionalComplexObjArray<>(
							committed,
							DistinctValueHolder::combineWith,
							DistinctValueHolder::subtract,
							DistinctValueHolder::isEmpty,
							DistinctValueHolder::equals
						)
					);
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

	private Integer[] sutbractArrays(TreeSet<Integer> subtractedArray, TreeSet<Integer> baseArray) {
		final TreeSet<Integer> baseArrayCopy = new TreeSet<>(baseArray);
		baseArrayCopy.removeAll(subtractedArray);
		return baseArrayCopy.toArray(new Integer[0]);
	}

	private Integer[] pickSomethingRandomlyFrom(Random rnd, TreeSet<Integer> values) {
		final TreeSet<Integer> newSet = new TreeSet<>(values);
		newSet.removeIf(it -> rnd.nextBoolean());
		return newSet.toArray(new Integer[0]);
	}

	private DistinctValueHolder mergeArrays(DistinctValueHolder upsertItem, DistinctValueHolder existingItem) {
		final Set<Integer> mergedValues = new TreeSet<>(existingItem.getValues());
		mergedValues.addAll(upsertItem.getValues());
		final Integer[] values = mergedValues.toArray(new Integer[0]);
		return new DistinctValueHolder(existingItem.getKey(), values);
	}

	private DistinctValueHolder[] generateRandomInitialArray(Random rnd, int count, int subCount) {
		final Set<String> uniqueSet = new HashSet<>();
		final DistinctValueHolder[] initialArray = new DistinctValueHolder[count];
		for (int i = 0; i < count; i++) {
			boolean added;
			do {
				final String recKey = String.valueOf((char) (40 + rnd.nextInt(count * 2)));
				added = uniqueSet.add(recKey);
				if (added) {
					final Integer[] values = generateRandomArray(rnd, subCount);
					if (ArrayUtils.isNotEmpty(values)) {
						initialArray[i] = new DistinctValueHolder(recKey, values);
					} else {
						added = false;
					}
				}
			} while (!added);
		}
		Arrays.sort(initialArray);
		return initialArray;
	}

	private Integer[] generateRandomArray(Random rnd, int count) {
		final Set<Integer> uniqueSet = new HashSet<>();
		final Integer[] initialArray = new Integer[count];
		for (int i = 0; i < count; i++) {
			boolean added;
			do {
				final int recId = rnd.nextInt(count * 2);
				added = uniqueSet.add(recId);
				if (added) {
					initialArray[i] = recId;
				}
			} while (!added);
		}
		Arrays.sort(initialArray);
		return initialArray;
	}

	private static void assertTransactionalObjArray(TransactionalInteger[] expectedContents, TransactionalComplexObjArray<TransactionalInteger> array) {
		if (isEmpty(expectedContents)) {
			assertTrue(array.isEmpty());
		} else {
			assertFalse(array.isEmpty());
		}
		assertArrayEquals(expectedContents, array.getArray());
		assertEquals(expectedContents.length, array.getLength());

		for (int i = 0; i < expectedContents.length; i++) {
			assertEquals(expectedContents[i], array.get(i));
			assertEquals(i, array.indexOf(expectedContents[i]));
		}

		assertIteratorContains(array.iterator(), expectedContents);
	}

	private static void assertTransactionalObjArray(DistinctValueHolder[] expectedContents, TransactionalComplexObjArray<DistinctValueHolder> array) {
		if (isEmpty(expectedContents)) {
			assertTrue(array.isEmpty());
		} else {
			assertFalse(array.isEmpty());
		}
		assertArrayEquals(expectedContents, array.getArray());
		assertIteratorContains(array.iterator(), expectedContents);

		assertEquals(expectedContents.length, array.getLength());

		for (int i = 0; i < expectedContents.length; i++) {
			assertEquals(expectedContents[i], array.get(i));
			assertEquals(i, array.indexOf(expectedContents[i]));
		}
	}

	private static void assertDistinctValueStateAfterCommit(DistinctValueHolder[] startValue, DistinctValueHolder[] expectedValue, Consumer<TransactionalComplexObjArray<DistinctValueHolder>> doInTransaction) {
		final TransactionalComplexObjArray<DistinctValueHolder> array = new TransactionalComplexObjArray<>(
			startValue,
			DistinctValueHolder::combineWith,
			DistinctValueHolder::subtract,
			DistinctValueHolder::isEmpty,
			DistinctValueHolder::equals
		);

		assertStateAfterCommit(
			array,
			original -> {
				doInTransaction.accept(original);

				assertTransactionalObjArray(expectedValue, original);
			},
			(original, committed) -> {
				assertTransactionalObjArray(startValue, original);
				assertArrayEquals(committed, expectedValue);
			}
		);
	}

	@Nonnull
	private static TransactionalInteger[] createIntegerArray(int... integers) {
		TransactionalInteger[] result = new TransactionalInteger[integers.length];
		for (int i = 0; i < integers.length; i++) {
			int integer = integers[i];
			result[i] = new TransactionalInteger(integer);
		}
		return result;
	}

	@Data
	private static class TransactionalInteger implements TransactionalObject<TransactionalInteger>, VoidTransactionMemoryProducer<TransactionalInteger>, Comparable<TransactionalInteger> {
		private final Integer object;

		@Override
		public TransactionalInteger createCopyWithMergedTransactionalMemory(Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
			return this;
		}

		@Override
		public int compareTo(@Nonnull TransactionalInteger o) {
			return Integer.compare(object, o.object);
		}

		@Override
		public void removeFromTransactionalMemory() {

		}

		@Override
		public TransactionalInteger makeClone() {
			return new TransactionalInteger(object);
		}
	}

	@Data
	private static class DistinctValueHolder implements TransactionalObject<DistinctValueHolder>, VoidTransactionMemoryProducer<DistinctValueHolder>, Comparable<DistinctValueHolder> {
		private final String key;
		private final TreeSet<Integer> values = new TreeSet<>();

		DistinctValueHolder(String key, Integer... values) {
			this.key = key;
			Collections.addAll(this.values, values);
		}

		@Override
		public DistinctValueHolder createCopyWithMergedTransactionalMemory(Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
			return this;
		}

		@Override
		public int compareTo(@Nonnull DistinctValueHolder o) {
			return key.compareTo(o.key);
		}

		@Override
		public void removeFromTransactionalMemory() {

		}

		@Override
		public DistinctValueHolder makeClone() {
			return new DistinctValueHolder(key, values.toArray(new Integer[0]));
		}

		@Override
		public String toString() {
			return key + ":" + values.stream().map(Object::toString).collect(Collectors.joining(","));
		}

		void combineWith(DistinctValueHolder otherHolder) {
			Assert.isTrue(key.equals(otherHolder.getKey()), "Keys are expected to be equal!");
			this.values.addAll(otherHolder.getValues());
		}

		boolean isEmpty() {
			return values.isEmpty();
		}

		void subtract(DistinctValueHolder otherHolder) {
			Assert.isTrue(key.equals(otherHolder.getKey()), "Keys are expected to be equal!");
			this.values.removeAll(otherHolder.getValues());
		}
	}

}