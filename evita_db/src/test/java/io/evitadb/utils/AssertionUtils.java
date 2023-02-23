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

package io.evitadb.utils;

import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import io.evitadb.query.algebra.Formula;

import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class contains methods that ease assertions in JUnit tests.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class AssertionUtils {

	/**
	 * Compares computation result of the formula with the expected contents and reports failure when the contents
	 * differ from expected ones.
	 */
	public static void assertFormulaResultsIn(Formula formula, int[] expectedContents) {
		assertIteratorContains(formula.compute().iterator(), expectedContents);
	}

	/**
	 * Compares iterator contents with the expected contents and reports failure when the contents
	 * differ from expected ones.
	 */
	public static <T> void assertIteratorContains(Iterator<T> it, T[] expectedContents) {
		int index = -1;
		while (it.hasNext()) {
			final T nextObj = it.next();
			assertTrue(expectedContents.length > index + 1);
			assertEquals(expectedContents[++index], nextObj);
		}
		assertEquals(
			expectedContents.length, index + 1,
			"There are more expected objects than int array produced by iterator!"
		);
	}

	/**
	 * Compares iterator contents with the expected contents and reports failure when the contents
	 * differ from expected ones.
	 */
	public static void assertIteratorContains(OfInt it, int[] expectedContents) {
		int index = -1;
		while (it.hasNext()) {
			final int nextInt = it.next();
			assertTrue(expectedContents.length > index + 1);
			assertEquals(expectedContents[++index], nextInt);
		}
		assertEquals(
			expectedContents.length, index + 1,
			"There are more expected objects than int array produced by iterator!"
		);
	}

	/**
	 * This method executes operation in lambda `doInTransaction` on `tested` instance and verifies the results visible
	 * after transactional memory is committed in `verifyAfterCommit` lambda.
	 */
	public static <S, X, T extends TransactionalLayerProducer<X, S>> void assertStateAfterCommit(T tested, Consumer<T> doInTransaction, BiConsumer<T, S> verifyAfterCommit) {
		assertFalse(TransactionalMemory.isTransactionalMemoryAvailable());
		TransactionalMemory.open();

		AtomicReference<S> copyHolder = new AtomicReference<>();
		TransactionalMemory.addTransactionCommitHandler(transactionalLayer -> {
			final S stateCopyWithCommittedChanges = transactionalLayer.getStateCopyWithCommittedChanges(tested, null);
			copyHolder.set(stateCopyWithCommittedChanges);
		});

		try {
			doInTransaction.accept(tested);
			TransactionalMemory.commit();
		} catch (Throwable ex) {
			TransactionalMemory.rollback();
			throw ex;
		}

		final S committedCopy = copyHolder.get();
		verifyAfterCommit.accept(tested, committedCopy);
	}

	/**
	 * This method executes operation in lambda `doInTransaction` on `tested` instance and verifies the results visible
	 * after transactional memory is rollbacked in `verifyAfterRollback` lambda.
	 */
	public static <S, X, T extends TransactionalLayerProducer<X, S>> void assertStateAfterRollback(T tested, Consumer<T> doInTransaction, BiConsumer<T, S> verifyAfterRollback) {
		assertFalse(TransactionalMemory.isTransactionalMemoryAvailable());
		TransactionalMemory.open();

		AtomicReference<S> copyHolder = new AtomicReference<>();
		TransactionalMemory.addTransactionCommitHandler(transactionalLayer -> {
			final S stateCopyWithCommittedChanges = transactionalLayer.getStateCopyWithCommittedChanges(tested, null);
			copyHolder.set(stateCopyWithCommittedChanges);
		});

		doInTransaction.accept(tested);

		TransactionalMemory.rollback();

		final S committedCopy = copyHolder.get();
		verifyAfterRollback.accept(tested, committedCopy);
	}

}
