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
import io.evitadb.index.iterator.ConstantObjIterator;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.ToIntBiFunction;

import static io.evitadb.index.transactionalMemory.TransactionalMemory.getTransactionalMemoryLayer;
import static io.evitadb.index.transactionalMemory.TransactionalMemory.isTransactionalMemoryAvailable;

/**
 * This array keeps unique (distinct) Comparable values in strictly ordered fashion (naturally ordered - ascending).
 *
 * This class envelopes simple primitive int array and makes it transactional. This means, that the array can be updated
 * by multiple writers and also multiple readers can read from it's original array without spotting the changes made
 * in transactional access. Each transaction is bound to the same thread and different threads doesn't see changes in
 * another threads.
 *
 * If no transaction is opened, changes are applied directly to the delegate array. In such case the class is not thread
 * safe for multiple writers!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@ThreadSafe
public class TransactionalObjArray<T extends Comparable<T>> implements TransactionalLayerProducer<ObjArrayChanges<T>, T[]>, Serializable {
	private static final long serialVersionUID = 3207853222537134300L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	private T[] delegate;

	public TransactionalObjArray(T[] delegate) {
		this.delegate = delegate;
	}

	/**
	 * Method returns record id on specified index of the array.
	 */
	public T get(int index) {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.delegate[index];
		} else {
			return layer.getMergedArray()[index];
		}
	}

	/**
	 * Method returns the underlying array or record ids.
	 */
	public T[] getArray() {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.delegate;
		} else {
			return layer.getMergedArray();
		}
	}

	/**
	 * Method adds new record to the array.
	 */
	public void add(T recordId) {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.delegate = ArrayUtils.insertRecordIntoOrderedArray(recordId, this.delegate);
		} else {
			layer.addRecordId(recordId);
		}
	}

	/**
	 * Method adds multiple record ids to the array.
	 */
	public void addAll(T[] recordIds) {
		for (T recordId : recordIds) {
			add(recordId);
		}
	}

	/**
	 * Method removes record id from the array.
	 */
	public void remove(T recordId) {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.delegate = ArrayUtils.removeRecordFromOrderedArray(recordId, this.delegate);
		} else {
			layer.removeRecordId(recordId);
		}
	}

	/**
	 * Method removes multiple record ids from the array.
	 */
	public void removeAll(T[] recordIds) {
		for (T recordId : recordIds) {
			remove(recordId);
		}
	}

	/**
	 * Returns length of the array.
	 */
	public int getLength() {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.delegate.length;
		} else {
			return layer.getMergedLength();
		}
	}

	/**
	 * Returns true if array contain no record ids.
	 */
	public boolean isEmpty() {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return ArrayUtils.isEmpty(this.delegate);
		} else {
			return layer.getMergedLength() == 0;
		}
	}

	/**
	 * Returns index (position) of the record id in the array.
	 *
	 * @return negative value when record is not found, positive if found
	 */
	public int indexOf(T recordId) {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return Arrays.binarySearch(this.delegate, recordId, Comparable::compareTo);
		} else {
			return layer.indexOf(recordId);
		}
	}

	/**
	 * Returns true if record id is part of the array.
	 */
	public boolean contains(T recordId) {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return Arrays.binarySearch(this.delegate, recordId, Comparable::compareTo) >= 0;
		} else {
			return layer.contains(recordId);
		}
	}

	/**
	 * Returns index (position) of the record id in the array.
	 *
	 * @return negative value when record is not found, positive if found
	 */
	public <U> boolean contains(U recordId, ToIntBiFunction<T, U> idExtractor) {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return ArrayUtils.binarySearch(this.delegate, recordId, idExtractor) >= 0;
		} else {
			return layer.contains(recordId, idExtractor);
		}
	}

	/**
	 * Returns iterator that allows to iterate through all record ids of the array.
	 */
	public Iterator<T> iterator() {
		final ObjArrayChanges<T> layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return new ConstantObjIterator<>(this.delegate);
		} else {
			return new TransactionalObjArrayIterator<>(this.delegate, layer);
		}
	}

	@Override
	public int hashCode() {
		/* we deliberately want Object.hashCode() default implementation */
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		/* we deliberately want Object.equals() default implementation */
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return Arrays.toString(getArray());
	}

	/*
		TRANSACTIONAL OBJECT IMPLEMENTATION
	 */

	@Override
	public T[] createCopyWithMergedTransactionalMemory(@Nullable ObjArrayChanges<T> layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		if (layer == null) {
			return this.delegate;
		} else {
			return layer.getMergedArray();
		}
	}

	@Override
	public ObjArrayChanges<T> createLayer() {
		return isTransactionalMemoryAvailable() ? new ObjArrayChanges<>(this.delegate) : null;
	}

}
