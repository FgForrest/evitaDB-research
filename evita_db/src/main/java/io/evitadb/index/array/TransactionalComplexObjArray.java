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
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ArrayUtils.InsertionPosition;
import io.evitadb.index.iterator.ConstantObjIterator;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static io.evitadb.index.transactionalMemory.TransactionalMemory.*;

/**
 * This class envelopes simple object array and makes it transactional. This means, that the array can be updated
 * by multiple writers and also multiple readers can read from it's original array without spotting the changes made
 * in transactional access. Each transaction is bound to the same thread and different threads doesn't see changes in
 * another threads.
 *
 * Object handled by this {@link TransactionalComplexObjArray} are expected to be also {@link TransactionalObject} themselves
 * and support internal transactional memory handling.
 *
 * If no transaction is opened, changes are applied directly to the delegate array. In such case the class is not thread
 * safe for multiple writers!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class TransactionalComplexObjArray<T extends TransactionalObject<T> & Comparable<T>> implements TransactionalLayerProducer<ComplexObjArrayChanges<T>, T[]> {
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	private final Class<T> objectType;
	private final boolean transactionalLayerProducer;
	private final BiConsumer<T, T> producer;
	private final BiConsumer<T, T> reducer;
	private final BiPredicate<T, T> deepComparator;
	private final Predicate<T> obsoleteChecker;
	private T[] delegate;

	public TransactionalComplexObjArray(@Nonnull T[] delegate) {
		//noinspection unchecked
		this.objectType = (Class<T>) delegate.getClass().getComponentType();
		this.transactionalLayerProducer = TransactionalLayerProducer.class.isAssignableFrom(objectType);
		this.delegate = delegate;
		this.producer = null;
		this.reducer = null;
		this.obsoleteChecker = null;
		this.deepComparator = null;
	}

	public TransactionalComplexObjArray(@Nonnull T[] delegate, @Nonnull BiConsumer<T, T> producer, @Nonnull BiConsumer<T, T> reducer, @Nonnull Predicate<T> obsoleteChecker, @Nonnull BiPredicate<T, T> deepComparator) {
		//noinspection unchecked
		this.objectType = (Class<T>) delegate.getClass().getComponentType();
		this.transactionalLayerProducer = TransactionalLayerProducer.class.isAssignableFrom(objectType);
		this.delegate = delegate;
		this.producer = producer;
		this.reducer = reducer;
		this.obsoleteChecker = obsoleteChecker;
		this.deepComparator = deepComparator;
	}

	/**
	 * Method returns record on specified index of the array.
	 */
	public T get(int index) {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.delegate[index];
		} else {
			return layer.getMergedArray()[index];
		}
	}

	/**
	 * Method returns the underlying array or records.
	 */
	public T[] getArray() {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.delegate;
		} else {
			return layer.getMergedArray();
		}
	}

	/**
	 * Method adds new record to the array.
	 *
	 * @return position when insertion happened
	 */
	public void add(T recordId) {
		if (obsoleteChecker != null && obsoleteChecker.test(recordId)) {
			return;
		}

		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		final InsertionPosition position = ArrayUtils.computeInsertPositionOfObjInOrderedArray(recordId, this.delegate);
		if (layer == null) {
			if (position.isAlreadyPresent()) {
				if (producer != null) {
					T original = this.delegate[position.getPosition()];
					producer.accept(original, recordId);
				}
			} else {
				this.delegate = ArrayUtils.insertRecordIntoArray(recordId, this.delegate, position.getPosition());
			}
		} else {
			layer.addRecordOnPosition(recordId, position.getPosition());
		}
	}

	/**
	 * Method adds new record to the array.
	 *
	 * @return position when insertion happened
	 */
	public int addReturningIndex(T recordId) {
		if (obsoleteChecker != null && obsoleteChecker.test(recordId)) {
			return -1;
		}

		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		final InsertionPosition position = ArrayUtils.computeInsertPositionOfObjInOrderedArray(recordId, this.delegate);
		if (layer == null) {
			if (position.isAlreadyPresent()) {
				if (producer != null) {
					T original = this.delegate[position.getPosition()];
					producer.accept(original, recordId);
				}
			} else {
				this.delegate = ArrayUtils.insertRecordIntoArray(recordId, this.delegate, position.getPosition());
			}
			return position.getPosition();
		} else {
			return layer.addRecordOnPositionComputingIndex(recordId, position.getPosition());
		}
	}

	/**
	 * Method adds multiple records to the array.
	 */
	public void addAll(T[] recordIds) {
		for (T recordId : recordIds) {
			add(recordId);
		}
	}

	/**
	 * Method removes record from the array.
	 *
	 * @return position where removal occurred or -1 if no removal occurred
	 */
	public int remove(T recordId) {
		if (obsoleteChecker != null && obsoleteChecker.test(recordId)) {
			return -1;
		}

		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		final InsertionPosition position = ArrayUtils.computeInsertPositionOfObjInOrderedArray(recordId, this.delegate);
		if (layer == null) {
			if (position.isAlreadyPresent()) {
				T original = this.delegate[position.getPosition()];
				if (reducer != null) {
					reducer.accept(original, recordId);
				}
				if (obsoleteChecker != null) {
					if (obsoleteChecker.test(original)) {
						this.delegate = ArrayUtils.removeRecordFromOrderedArray(recordId, this.delegate);
					}
				} else {
					this.delegate = ArrayUtils.removeRecordFromOrderedArray(recordId, this.delegate);
				}
				return position.getPosition();
			}
		} else {
			return layer.removeRecordOnPosition(recordId, position.getPosition(), position.isAlreadyPresent());
		}

		return -1;
	}

	/**
	 * Method removes multiple record from the array.
	 */
	public void removeAll(T[] recordIds) {
		for (T recordId : recordIds) {
			remove(recordId);
		}
	}

	/**
	 * Returns length of the array.
	 * This operation might be costly because it requires final array computation.
	 */
	public int getLength() {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.delegate.length;
		} else {
			return layer.getMergedArray().length;
		}
	}

	/**
	 * Returns true if array contain no records.
	 * This operation might be costly because it requires final array computation.
	 */
	public boolean isEmpty() {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return ArrayUtils.isEmpty(this.delegate);
		} else {
			return layer.getMergedArray().length == 0;
		}
	}

	/**
	 * Returns index (position) of the record in the array.
	 *
	 * @return negative value when record is not found, positive if found
	 */
	public int indexOf(T recordId) {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return Arrays.binarySearch(this.delegate, recordId);
		} else {
			return Arrays.binarySearch(getArray(), recordId);
		}
	}

	/**
	 * Returns true if record is part of the array.
	 */
	public boolean contains(T recordId) {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return Arrays.binarySearch(this.delegate, recordId) >= 0;
		} else {
			return layer.contains(recordId);
		}
	}

	/**
	 * Returns iterator that allows to iterate through all record of the array.
	 */
	public Iterator<T> iterator() {
		final ComplexObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return new ConstantObjIterator<>(this.delegate);
		} else {
			return new TransactionalComplexObjArrayIterator<>(this.delegate, layer, producer, reducer, obsoleteChecker);
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(getArray());
	}

	/*
		TRANSACTIONAL OBJECT IMPLEMENTATION
	 */

	@Override
	public T[] createCopyWithMergedTransactionalMemory(@Nullable ComplexObjArrayChanges<T> layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		if (layer == null) {
			@SuppressWarnings("unchecked") final T[] copy = (T[]) Array.newInstance(objectType, delegate.length);
			for (int i = 0; i < delegate.length; i++) {
				T item = delegate[i];
				if (transactionalLayerProducer) {
					@SuppressWarnings("unchecked") final TransactionalLayerProducer<ComplexObjArrayChanges<T>, ?> theProducer = (TransactionalLayerProducer<ComplexObjArrayChanges<T>, ?>) item;
					//noinspection unchecked
					item = (T) theProducer.createCopyWithMergedTransactionalMemory(
						null, transactionalLayer, transaction
					);
				}
				copy[i] = item;
			}
			return copy;
		} else {
			return layer.getMergedArray(transactionalLayer);
		}
	}

	@Override
	public ComplexObjArrayChanges<T> createLayer() {
		if (producer != null) {
			return isTransactionalMemoryAvailable() ? new ComplexObjArrayChanges<>(objectType, delegate, producer, reducer, obsoleteChecker, deepComparator) : null;
		} else {
			return isTransactionalMemoryAvailable() ? new ComplexObjArrayChanges<>(objectType, delegate) : null;
		}
	}

}
