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

package io.evitadb.index.list;

import io.evitadb.api.Transaction;
import io.evitadb.index.transactionalMemory.*;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * This class envelopes simple list and makes it transactional. This means, that the list contents can be updated
 * by multiple writers and also multiple readers can read from it's original list without spotting the changes made
 * in transactional access. Each transaction is bound to the same thread and different threads doesn't see changes in
 * another threads.
 *
 * If no transaction is opened, changes are applied directly to the delegate list. In such case the class is not thread
 * safe for multiple writers!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
@ThreadSafe
public class TransactionalMemoryList<V> implements List<V>, Serializable, Cloneable, TransactionalLayerCreator<ListChanges<V>>, TransactionalLayerProducer<ListChanges<V>, List<V>> {
	private static final long serialVersionUID = 7969800648176780425L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * Original immutable list.
	 */
	private final List<V> listDelegate;

	public TransactionalMemoryList(List<V> listDelegate) {
		this.listDelegate = listDelegate;
	}

	/*
		TransactionalLayerCreator IMPLEMENTATION
	 */

	@Override
	public ListChanges<V> createLayer() {
		return new ListChanges<>();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List<V> createCopyWithMergedTransactionalMemory(@Nullable ListChanges<V> layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		return createCopyWithMergedTransactionalMemory(
			layer,
			value -> (V) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) value, transaction)
		);
	}

	/*
		LIST CONTRACT IMPLEMENTATION
	 */

	@Override
	public int size() {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.size();
		} else {
			return this.listDelegate.size() - layer.getRemovedItems().size() + layer.getAddedItems().size();
		}
	}

	@Override
	public boolean isEmpty() {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.isEmpty();
		} else {
			return (this.listDelegate.size() - layer.getRemovedItems().size() == 0) && layer.getAddedItems().isEmpty();
		}
	}

	@Override
	public boolean contains(Object obj) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.contains(obj);
		} else {
			// scan original contents of the list and compare them
			for (int i = 0; i < listDelegate.size(); i++) {
				V examinedValue = listDelegate.get(i);
				// avoid items that are known to be removed
				if (!layer.getRemovedItems().contains(i) && Objects.equals(obj, examinedValue)) {
					return true;
				}
			}
			// scan newly added items of the list
			//noinspection SuspiciousMethodCalls
			return layer.getAddedItems().containsValue(obj);
		}
	}

	@Override
	public Iterator<V> iterator() {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.iterator();
		} else {
			// create copy of the list with all changes applied and iterate over it
			return new TransactionalMemoryEntryAbstractIterator(
				createCopyWithMergedTransactionalMemory(layer, value -> (V) value), 0
			);
		}
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[0]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(@Nonnull T[] array) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return (T[]) this.listDelegate.toArray();
		} else {
			// create copy of the list with all changes applied and convert it to the array
			return createCopyWithMergedTransactionalMemory(layer, value -> (V) value).toArray(array);
		}
	}

	@Override
	public boolean add(V v) {
		// add the element at the end
		this.add(size(), v);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.listDelegate.remove(o);
		} else {
			// find first position of the added item that equals to passed argument
			Integer addedNewPosition = null;
			for (Entry<Integer, V> entry : layer.getAddedItems().entrySet()) {
				if (Objects.equals(o, entry.getValue())) {
					addedNewPosition = entry.getKey();
					break;
				}
			}
			// find first position of the existing (non-removed) item that equals to passed argument - counting in added elements
			Integer indexToRemove = null;
			int removedExistingPosition = -1;
			for (int j = 0; j < size(); j++) {
				do {
					if (!layer.getAddedItems().containsKey(j)) {
						removedExistingPosition++;
					}
				} while (layer.getRemovedItems().contains(removedExistingPosition));
				if (removedExistingPosition > -1 && Objects.equals(o, this.listDelegate.get(removedExistingPosition))) {
					indexToRemove = j;
					break;
				}
			}

			if (addedNewPosition == null && indexToRemove == null) {
				// no match was found
				return false;
			} else if (indexToRemove == null || (addedNewPosition != null && addedNewPosition < indexToRemove)) {
				// added item was found first - just replace it on specified position
				layer.getAddedItems().remove(addedNewPosition);
				layer.lowerIndexesGreaterThan(addedNewPosition);
				return true;
			} else {
				// existing item was found first - add the proper position to removed and lower insertion indexes of new items after it
				layer.getRemovedItems().add(removedExistingPosition);
				layer.lowerIndexesGreaterThan(indexToRemove);
				return true;
			}
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		boolean modified = false;
		for (V e : c) {
			add(e);
			modified = true;
		}
		return modified;
	}

	@Override
	public boolean addAll(int index, Collection<? extends V> c) {
		boolean modified = false;
		for (V e : c) {
			add(index++, e);
			modified = true;
		}
		return modified;
	}

	@Override
	public boolean removeAll(@Nonnull Collection<?> collection) {
		boolean modified = false;
		final Iterator<?> it = iterator();
		while (it.hasNext()) {
			if (collection.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(@Nonnull Collection<?> c) {
		boolean modified = false;
		final Iterator<V> it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public void clear() {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.listDelegate.clear();
		} else {
			// remove all added elements
			layer.getAddedItems().clear();
			// add all list delegate elements to removed set
			final Set<Integer> removedItems = layer.getRemovedItems();
			removedItems.clear();
			for (int i = 0; i < listDelegate.size(); i++) {
				removedItems.add(i);
			}
		}
	}

	@Override
	public V get(int index) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.get(index);
		} else {
			// first try to find index in newly added elements
			final Map<Integer, V> addedItems = layer.getAddedItems();
			if (addedItems.containsKey(index)) {
				return addedItems.get(index);
			} else {
				// when not found iterate through original list
				int examinedIndex = -1;
				for (int j = 0; j <= index; j++) {
					// skip added items - these were already looked up
					if (addedItems.containsKey(j)) {
						continue;
					}
					// skip removed items as well
					do {
						examinedIndex++;
					} while (layer.getRemovedItems().contains(examinedIndex));
					// when arrived on proper index return element
					if (j == index) {
						return this.listDelegate.get(examinedIndex);
					}
				}
				return null;
			}
		}
	}

	@Override
	public V set(int index, V element) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.listDelegate.set(index, element);
		} else {
			// remove element and add on the same index new value
			final V result = remove(index);
			add(index, element);
			return result;
		}
	}

	@Override
	public void add(int index, V element) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.listDelegate.add(index, element);
		} else {
			if (index > size()) {
				throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
			}
			// increase indexes of all existing insertions after the modified index
			layer.increaseIndexesGreaterThanOrEquals(index);
			// and add new element at specified index
			layer.getAddedItems().put(index, element);
		}
	}

	@Override
	public V remove(int index) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.listDelegate.remove(index);
		} else {
			if (index > size()) {
				throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
			}

			// first find the position in the added elements
			if (layer.getAddedItems().containsKey(index)) {
				// if found remove it and lower indexes of all following new elements
				V result = layer.getAddedItems().remove(index);
				layer.lowerIndexesGreaterThan(index);
				return result;
			}

			// iterate through existing elements
			int examinedIndex = -1;
			for (int j = 0; j <= index; j++) {
				do {
					// increase existing index only when the new index doesn't match added element
					if (!layer.getAddedItems().containsKey(j)) {
						examinedIndex++;
					}
					// and skip already removed elements
				} while (layer.getRemovedItems().contains(examinedIndex));
				// if index was found (should be)
				if (j == index) {
					// add the index of the underlying delegate list to the set of removed indexes
					layer.getRemovedItems().add(examinedIndex);
					V result = this.listDelegate.get(examinedIndex);
					// lower all indexes of newly added elements greater than the new index
					layer.lowerIndexesGreaterThan(index);
					return result;
				}
			}
		}

		return null;
	}

	@Override
	public int indexOf(Object o) {
		// use simple iterator - this won't be much fast
		final ListIterator<V> it = listIterator();
		if (o == null) {
			while (it.hasNext())
				if (it.next() == null)
					return it.previousIndex();
		} else {
			while (it.hasNext())
				if (o.equals(it.next()))
					return it.previousIndex();
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		// use simple iterator - this won't be much fast
		final ListIterator<V> it = listIterator(size());
		if (o == null) {
			while (it.hasPrevious())
				if (it.previous() == null)
					return it.nextIndex();
		} else {
			while (it.hasPrevious())
				if (o.equals(it.previous()))
					return it.nextIndex();
		}
		return -1;
	}

	@Override
	public ListIterator<V> listIterator() {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.listIterator();
		} else {
			// create copy of new list with all changes merged - not entirely fast, but safe
			return new TransactionalMemoryEntryAbstractIterator(
				createCopyWithMergedTransactionalMemory(layer, value -> (V) value), 0
			);
		}
	}

	@Override
	public ListIterator<V> listIterator(int index) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.listIterator(index);
		} else {
			// create copy of new list with all changes merged - not entirely fast, but safe
			return new TransactionalMemoryEntryAbstractIterator(
				createCopyWithMergedTransactionalMemory(layer, value -> (V) value), index
			);
		}
	}

	@Override
	public List<V> subList(int fromIndex, int toIndex) {
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.listDelegate.subList(fromIndex, toIndex);
		} else {
			final List<V> sublist = new ArrayList<>(toIndex - fromIndex);
			// create copy of new list with all changes merged - not entirely fast, but safe
			final Iterator<V> it = iterator();
			int counter = -1;
			while (it.hasNext()) {
				counter++;
				if (counter >= fromIndex && counter < toIndex) {
					sublist.add(it.next());
				}
			}
			return sublist;
		}
	}

	public int hashCode() {
		int hashCode = 1;
		for (V e : this)
			hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
		return hashCode;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		ListIterator<V> e1 = listIterator();
		@SuppressWarnings({"unchecked", "rawtypes"}) ListIterator<V> e2 = ((List) o).listIterator();
		while (e1.hasNext() && e2.hasNext()) {
			V o1 = e1.next();
			Object o2 = e2.next();
			if (!(o1 == null ? o2 == null : o1.equals(o2)))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// clone transactional list contents with all recorded changes and create separate transactional memory piece for it
		@SuppressWarnings("unchecked") final TransactionalMemoryList<V> clone = (TransactionalMemoryList<V>) super.clone();
		final ListChanges<V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer != null) {
			final ListChanges<V> clonedLayer = TransactionalMemory.getTransactionalMemoryLayer(clone);
			if (clonedLayer != null) {
				clonedLayer.getRemovedItems().addAll(layer.getRemovedItems());
				clonedLayer.getAddedItems().putAll(layer.getAddedItems());
			}
		}
		return clone;
	}

	public String toString() {
		final Iterator<V> it = iterator();
		if (!it.hasNext())
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (; ; ) {
			V e = it.next();
			sb.append(e == this ? "(this Collection)" : e);
			if (!it.hasNext())
				return sb.append(']').toString();
			sb.append(',').append(' ');
		}
	}

	/**
	 * This method creates copy of the original list with all changes merged into it.
	 */
	@SuppressWarnings({"rawtypes"})
	private List<V> createCopyWithMergedTransactionalMemory(@Nullable ListChanges<V> layer, Function<TransactionalLayerProducer<?, ?>, V> transactionLayerExtractor) {
		// create new array list of requested size
		final ArrayList<V> copy = new ArrayList<>(size());
		// iterate original list and copy all values from it
		for (int i = 0; i < listDelegate.size(); i++) {
			V value = listDelegate.get(i);
			// we need to always create copy - something in the referenced object might have changed
			// even the removed values need to be evaluated (in order to discard them from transactional memory set)
			if (value instanceof TransactionalLayerProducer) {
				value = transactionLayerExtractor.apply((TransactionalLayerProducer) value);
			}
			// except those that were removed
			if (layer == null || !layer.getRemovedItems().contains(i)) {
				copy.add(value);
			}
		}
		// iterate over added items
		if (layer != null && !layer.getAddedItems().isEmpty()) {
			for (Integer updatedItem : layer.getAddedItems().keySet()) {
				V value = layer.getAddedItems().get(updatedItem);
				// we need to always create copy - something in the referenced object might have changed
				if (value instanceof TransactionalLayerProducer) {
					value = transactionLayerExtractor.apply((TransactionalLayerProducer) value);
				}
				// add the element in the result list
				copy.add(updatedItem, value);
			}
		}

		return copy;
	}

	/**
	 * List iterator implementation that supports modifications on the original list.
	 */
	private class TransactionalMemoryEntryAbstractIterator implements ListIterator<V> {
		private final List<V> currentList;
		private int currentPosition;
		private int previousPosition = -1;

		TransactionalMemoryEntryAbstractIterator(List<V> currentList, int initialIndex) {
			this.currentPosition = initialIndex;
			this.currentList = currentList;
		}

		@Override
		public boolean hasNext() {
			return currentList.size() > this.currentPosition;
		}

		@Override
		public V next() {
			if (this.currentList.size() > currentPosition) {
				previousPosition = currentPosition;
				return this.currentList.get(currentPosition++);
			} else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public boolean hasPrevious() {
			return this.currentPosition > 0;
		}

		@Override
		public V previous() {
			if (currentPosition <= 0) {
				throw new NoSuchElementException();
			}
			previousPosition = currentPosition;
			return this.currentList.get(--currentPosition);
		}

		@Override
		public int nextIndex() {
			return currentPosition;
		}

		@Override
		public int previousIndex() {
			return currentPosition - 1;
		}

		@Override
		public void remove() {
			if (previousPosition > -1) {
				currentPosition = previousPosition;
				TransactionalMemoryList.this.remove(previousPosition);
				currentList.remove(previousPosition);
			} else {
				throw new IllegalStateException();
			}
		}

		@Override
		public void set(V v) {
			if (currentPosition > 0) {
				TransactionalMemoryList.this.set(currentPosition - 1, v);
				currentList.set(currentPosition - 1, v);
			} else {
				throw new IllegalStateException();
			}
		}

		@Override
		public void add(V v) {
			TransactionalMemoryList.this.add(currentPosition, v);
			currentList.add(currentPosition, v);
		}

	}

}
