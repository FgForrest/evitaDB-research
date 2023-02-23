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

package io.evitadb.index.transactionalMemory;

import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * TransactionalMemory is central object for all {@link TransactionalLayerCreator} implementations. Transactional memory
 * allows to make changes to state objects that are visible only in the transaction. All accesses outside the transaction
 * (ie. from other threads to the same state objects) don't see the changes until they are committed.
 *
 * The work with transaction is expected in following form:
 *
 * ``` java
 * TransactionalMemory.open()
 * try {
 * TransactionalMemory.commit()
 * // do some work
 * } catch (Exception ex) {
 * TransactionalMemory.rollback()
 * }
 * ```
 *
 * All changes made with objects participating in transaction (all must implement {@link TransactionalLayerCreator} or
 * {@link TransactionalLayerProducer} interface) must be captured in change objects and must not affect original state.
 * Changes must create separate copy in {@link TransactionalLayerProducer#createCopyWithMergedTransactionalMemory(Object, TransactionalLayerMaintainer, io.evitadb.api.Transaction)}
 * method.
 *
 * All copies created by {@link TransactionalLayerProducer#createCopyWithMergedTransactionalMemory(Object, TransactionalLayerMaintainer, io.evitadb.api.Transaction)}
 * must be consumed by registered {@link TransactionalLayerConsumer#collectTransactionalChanges(TransactionalLayerMaintainer)} so
 * that no changes end in the void.
 *
 * Transactional memory is bound to current thread. Single thread may open multiple simultaneous transactions, but accessible
 * is only the last one created. Changes made in one transaction are not visible in other transactions (currently).
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
public class TransactionalMemory {
	private static final ThreadLocal<Deque<TransactionalMemory>> TRANSACTIONAL_MEMORY = new ThreadLocal<>();
	private static final ThreadLocal<Deque<Set<TransactionalLayerCreator<?>>>> SUPPRESSED_CREATORS = new ThreadLocal<>();
	private final TransactionalLayerMaintainer transactionalLayer;

	private TransactionalMemory() {
		this.transactionalLayer = new TransactionalLayerMaintainer();
	}

	private TransactionalMemory(TransactionalMemory existingLayer) {
		this.transactionalLayer = new TransactionalLayerMaintainer(existingLayer.transactionalLayer);
	}

	/**
	 * Opens a new layer of transactional states upon {@link TransactionalLayerCreator} object.
	 */
	public static void open() {
		open(null);
	}

	/**
	 * Opens a new layer of transactional states upon {@link TransactionalLayerCreator} object.
	 * Allows to pass initial data of transactional memory.
	 */
	public static void open(@Nullable TransactionalMemory transactionalMemory) {
		final Deque<TransactionalMemory> threadStack = getTransactionalMemoryStack();
		threadStack.push(transactionalMemory == null ? new TransactionalMemory() : transactionalMemory);
	}

	/**
	 * Opens a new layer of transactional states upon {@link TransactionalLayerCreator} object.
	 * Uses all transactional data of existing transactional layer. If no transactional layer exists, new is created.
	 * Operations in nested transaction are not entirely isolated:
	 *
	 * - operation that targets existing transactional state objects from upper transaction layer will modify contents
	 * of upper transaction layer even if nested transaction is rolled back
	 * - operation that creates new transactional state will be captured in nested transaction and will be reverted
	 * in case of nested transaction rollback
	 * - nested transaction rollback doesn't affect the parent transaction
	 */
	public static void openNested() {
		final Deque<TransactionalMemory> threadStack = getTransactionalMemoryStack();
		threadStack.push(threadStack.isEmpty() ? new TransactionalMemory() : new TransactionalMemory(threadStack.peek()));
	}

	/**
	 * Propagates changes in states made in transactional layer down to real "state" in {@link TransactionalLayerCreator}
	 * which may be stored in longer living state object.
	 *
	 * Destroys current transactional layer in the process - if you want to keep current transactional layer
	 * for additional usage please use {@link #flush()}
	 */
	public static void commit() {
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			try {
				//we get current transactional memory
				final TransactionalMemory transactionalMemory = stack.peek();
				// execute commit - all transactional object can still access their transactional memories during
				// entire commit phase
				transactionalMemory.transactionalLayer.commit();
			} finally {
				// now we remove the transactional memory - no object will see it transactional memory from now on
				stack.removeFirst();
				// if stack is empty, get rid of it
				if (stack.isEmpty()) {
					TRANSACTIONAL_MEMORY.remove();
				}
			}
		}
	}

	/**
	 * Propagates changes in states made in transactional layer down to real "state" in {@link TransactionalLayerCreator}
	 * which may be stored in longer living state object.
	 *
	 * Keeps current transactional layer active - additional changes will be monitored on.
	 */
	public static void flush() {
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			final TransactionalMemory transactionalMemory = stack.peek();
			transactionalMemory.transactionalLayer.commit();
		}
	}

	/**
	 * Rollbacks all transactional changes (i.e. dereferences them).
	 */
	@Nullable
	public static TransactionalMemory rollback() {
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			final TransactionalMemory transactionalMemory = stack.pop();
			if (stack.isEmpty()) {
				TRANSACTIONAL_MEMORY.remove();
			}
			return transactionalMemory;
		}
		return null;
	}

	/**
	 * Registers transaction commit handler for current transaction. Implementation of {@link TransactionalLayerConsumer}
	 * may withdraw multiple {@link TransactionalLayerProducer#createCopyWithMergedTransactionalMemory(Object, TransactionalLayerMaintainer, io.evitadb.api.Transaction)} 4
	 * and use their results to swap certain internal state atomically.
	 *
	 * All withdrawn objects will be considered as committed.
	 */
	public static boolean addTransactionCommitHandler(@Nonnull TransactionalLayerConsumer consumer) {
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			final TransactionalMemory transactionalMemory = stack.peek();
			transactionalMemory.transactionalLayer.addLayerConsumer(consumer);
			return true;
		}
		return false;
	}

	/**
	 * Returns read-only list of registered transaction commit handlers.
	 */
	@Nonnull
	public static List<TransactionalLayerConsumer> getTransactionCommitHandlers() {
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null) {
			final TransactionalMemory transactionalMemory = stack.peek();
			Assert.notNull(transactionalMemory, "Transactional memory is unexpectedly null!");
			return Collections.unmodifiableList(transactionalMemory.transactionalLayer.getLayerConsumers());
		}
		return Collections.emptyList();
	}

	/**
	 * Returns true if transactional memory is present and usable.
	 */
	public static boolean isTransactionalMemoryAvailable() {
		return TRANSACTIONAL_MEMORY.get() != null && !TRANSACTIONAL_MEMORY.get().isEmpty();
	}

	/**
	 * Returns transactional layer for states, that is isolated for this thread.
	 */
	@Nullable
	public static <T> T getTransactionalMemoryLayerIfExists(@Nonnull TransactionalLayerCreator<T> layerCreator) {
		// we may safely do this because transactionalLayer is stored in ThreadLocal and
		// thus won't be accessed by multiple threads at once
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			final TransactionalMemory transactionalMemory = stack.peek();
			return transactionalMemory.transactionalLayer.getTransactionalMemoryLayerIfExists(layerCreator);
		} else {
			return null;
		}
	}

	/**
	 * Returns transactional states for passed layer creator object, that is isolated for this thread.
	 */
	@Nullable
	public static <T> T getTransactionalMemoryLayer(@Nonnull TransactionalLayerCreator<T> layerCreator) {
		// we may safely do this because transactionalLayer is stored in ThreadLocal and
		// thus won't be accessed by multiple threads at once
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		final Deque<Set<TransactionalLayerCreator<?>>> suppressedObjects = getSuppressedCreatorStackIfExists();
		if (stack != null && !stack.isEmpty() && (suppressedObjects == null || suppressedObjects.isEmpty() || !suppressedObjects.peek().contains(layerCreator))) {
			final TransactionalMemory transactionalMemory = stack.peek();
			return transactionalMemory.transactionalLayer.getTransactionalMemoryLayer(layerCreator);
		} else {
			return null;
		}
	}

	/**
	 * Returns transactional layer for states, that is isolated for this thread.
	 */
	@Nullable
	public static TransactionalLayerMaintainer getTransactionalMemoryLayer() {
		// we may safely do this because transactionalLayer is stored in ThreadLocal and
		// thus won't be accessed by multiple threads at once
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			final TransactionalMemory transactionalMemory = stack.peek();
			return transactionalMemory.transactionalLayer;
		} else {
			return null;
		}
	}

	/**
	 * This method will suppress creation of new transactional layer for passed `object` when it is asked for inside
	 * the `objectConsumer` lambda. This makes the object effectively transactional-less for the scope of the lambda
	 * function.
	 */
	public static <T> void suppressTransactionalMemoryLayerFor(@Nonnull T object, @Nonnull Consumer<T> objectConsumer) {
		suppressTransactionalMemoryLayerForWithResult(
			object, it -> {
				objectConsumer.accept(it);
				return null;
			});
	}

	/**
	 * This method will suppress creation of new transactional layer for passed `object` when it is asked for inside
	 * the `objectConsumer` lambda. This makes the object effectively transactional-less for the scope of the lambda
	 * function.
	 */
	public static <T, U> U suppressTransactionalMemoryLayerForWithResult(@Nonnull T object, @Nonnull Function<T, U> objectConsumer) {
		Assert.isTrue(object instanceof TransactionalLayerCreator, "Object " + object.getClass() + " doesn't implement TransactionalLayerCreator interface!");
		Assert.isTrue(getTransactionalMemoryLayerIfExists((TransactionalLayerCreator<?>) object) == null, "There already exists transactional memory for passed creator!");
		final Deque<Set<TransactionalLayerCreator<?>>> deque = ofNullable(SUPPRESSED_CREATORS.get()).orElseGet(() -> {
			final LinkedList<Set<TransactionalLayerCreator<?>>> newDequeue = new LinkedList<>();
			SUPPRESSED_CREATORS.set(newDequeue);
			return newDequeue;
		});
		try {
			final Set<TransactionalLayerCreator<?>> suppressedSet = new HashSet<>();
			suppressedSet.add((TransactionalLayerCreator<?>) object);
			if (object instanceof TransactionalCreatorMaintainer) {
				suppressedSet.addAll(((TransactionalCreatorMaintainer) object).getMaintainedTransactionalCreators());
			}
			deque.push(suppressedSet);
			return objectConsumer.apply(object);
		} finally {
			deque.pop();
			if (deque.isEmpty()) {
				SUPPRESSED_CREATORS.remove();
			}
		}
	}

	/**
	 * Removes transactional layer for passed layer creator.
	 */
	@Nullable
	public static <T> T removeTransactionalMemoryLayerIfExists(@Nonnull TransactionalLayerCreator<T> layerCreator) {
		// we may safely do this because transactionalLayer is stored in ThreadLocal and
		// thus won't be accessed by multiple threads at once
		final Deque<TransactionalMemory> stack = getTransactionalMemoryStackIfExists();
		if (stack != null && !stack.isEmpty()) {
			final TransactionalMemory transactionalMemory = stack.peek();
			return transactionalMemory.transactionalLayer.removeTransactionalMemoryLayerIfExists(layerCreator);
		} else {
			return null;
		}
	}

	@Nullable
	private static Deque<TransactionalMemory> getTransactionalMemoryStackIfExists() {
		return TRANSACTIONAL_MEMORY.get();
	}

	@Nullable
	private static Deque<Set<TransactionalLayerCreator<?>>> getSuppressedCreatorStackIfExists() {
		return SUPPRESSED_CREATORS.get();
	}

	@Nonnull
	private static Deque<TransactionalMemory> getTransactionalMemoryStack() {
		Deque<TransactionalMemory> threadStack = TRANSACTIONAL_MEMORY.get();
		if (threadStack == null) {
			threadStack = new LinkedList<>();
			TRANSACTIONAL_MEMORY.set(threadStack);
		}
		return threadStack;
	}

}
