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

import io.evitadb.api.Transaction;
import io.evitadb.index.transactionalMemory.*;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * This class envelopes simple set and makes it transactional. This means, that the map contents can be updated
 * by multiple writers and also multiple readers can read from its original map without spotting the changes made
 * in transactional access. Each transaction is bound to the same thread and different threads doesn't see changes in
 * another threads.
 *
 * If no transaction is opened, changes are applied directly to the delegate map. In such case the class is not thread
 * safe for multiple writers!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
@ThreadSafe
public class TransactionalMemorySet<K> implements Set<K>, Serializable, Cloneable, TransactionalLayerCreator<SetChanges<K>>, TransactionalLayerProducer<SetChanges<K>, Set<K>> {
	private static final long serialVersionUID = 6678551073928034251L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	private final Set<K> setDelegate;

	public TransactionalMemorySet(Set<K> setDelegate) {
		this.setDelegate = setDelegate;
	}

	/*
		TransactionalLayerCreator IMPLEMENTATION
	 */

	@Override
	public SetChanges<K> createLayer() {
		return new SetChanges<>();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Set<K> createCopyWithMergedTransactionalMemory(SetChanges<K> layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		// create new hash set of requested size
		final HashSet<K> copy = new HashSet<>(setDelegate.size());
		// iterate original map and copy all values from it
		for (K key : setDelegate) {
			// we need to always create copy - something in the referenced object might have changed
			// even the removed values need to be evaluated (in order to discard them from transactional memory set)
			if (key instanceof TransactionalLayerProducer) {
				key = (K) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) key, transaction);
			}
			// except those that were removed
			if (layer == null || !layer.containsRemoved(key)) {
				copy.add(key);
			}
		}

		// iterate over inserted or updated keys
		if (layer != null) {
			for (K key : layer.getCreatedKeys()) {
				// we need to always create copy - something in the referenced object might have changed
				if (key instanceof TransactionalLayerProducer) {
					key = (K) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) key, transaction);
				}
				// update the value
				copy.add(key);
			}
		}

		return copy;
	}

	/*
		SET CONTRACT IMPLEMENTATION
	 */

	@Override
	public int size() {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.setDelegate.size();
		} else {
			return this.setDelegate.size() - layer.getRemovedKeyCount() + layer.getCreatedKeyCount();
		}
	}

	@Override
	public boolean isEmpty() {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.setDelegate.isEmpty();
		} else {
			if (layer.getRemovedKeyCount() == 0 && layer.getCreatedKeyCount() == 0) {
				return setDelegate.isEmpty();
			} else {
				return size() == 0;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object o) {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.setDelegate.contains(o);
		} else {
			if (layer.containsCreated((K) o)) {
				return true;
			} else if (layer.containsRemoved((K) o)) {
				return false;
			} else {
				return this.setDelegate.contains(o);
			}
		}
	}

	@Override
	public Iterator<K> iterator() {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return setDelegate.iterator();
		} else {
			return new TransactionalMemorySetIterator<>(setDelegate, layer);
		}
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public <T> T[] toArray(T[] a) {
		//noinspection unchecked
		return (T[]) Array.newInstance(a.getClass().getComponentType(), 0);
	}

	@Override
	public boolean add(K key) {
		// in some cases the default value from PropertyAccessor are set here -> result is NotSerializableException later
		if (key != null && key.getClass() == Object.class) {
			return false;
		}
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.setDelegate.add(key);
		} else {
			return putInternal(key, layer);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object key) {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.setDelegate.remove(key);
		} else {
			final boolean originalContained = this.setDelegate.contains(key);
			if (originalContained && layer.containsRemoved((K) key)) {
				// value has been already removed - report false and do nothing
				return false;
			}
			final boolean wasRemoved;
			if (layer.containsCreated((K) key)) {
				layer.removeCreatedKey((K) key);
				wasRemoved = true;
			} else if (originalContained) {
				layer.registerRemovedKey((K) key);
				wasRemoved = true;
			} else {
				wasRemoved = false;
			}
			return wasRemoved;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.setDelegate.addAll(c);
		} else {
			boolean modified = false;
			for (K key : c) {
				modified |= putInternal(key, layer);
			}
			return modified;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.setDelegate.clear();
		} else {
			layer.clearAll(this.setDelegate);
		}
	}

	public int hashCode() {
		int h = 0;
		for (K key : this) {
			h += key.hashCode();
		}
		return h;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Map))
			return false;
		Set<K> m = (Set<K>) o;
		if (m.size() != size())
			return false;

		try {
			for (K key : this) {
				if (!(m.contains(key)))
					return false;
			}
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}

		return true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		@SuppressWarnings("unchecked") final TransactionalMemorySet<K> clone = (TransactionalMemorySet<K>) super.clone();
		final SetChanges<K> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer != null) {
			final SetChanges<K> clonedLayer = TransactionalMemory.getTransactionalMemoryLayer(clone);
			if (clonedLayer != null) {
				clonedLayer.copyState(layer);
			}
		}
		return clone;
	}

	public String toString() {
		final Iterator<K> i = iterator();
		if (!i.hasNext())
			return "{}";

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (; ; ) {
			K key = i.next();
			sb.append(key == this ? "(this Set)" : key);
			if (!i.hasNext())
				return sb.append('}').toString();
			sb.append(',').append(' ');
		}
	}

	/*
		INTERNALS
	 */

	private boolean putInternal(K key, SetChanges<K> layer) {
		final boolean wasAdded;
		if (layer.containsCreated(key)) {
			wasAdded = false;
		} else {
			final boolean isPartOfOriginal = this.setDelegate.contains(key);
			if (isPartOfOriginal) {
				wasAdded = false;
			} else {
				layer.registerCreatedKey(key);
				wasAdded = true;
			}
		}
		layer.removeRemovedKey(key);
		return wasAdded;
	}

	/**
	 * Iterator implementation that aggregates values from the original map with modified data on transaction level.
	 */
	private static class TransactionalMemorySetIterator<K> implements Iterator<K> {
		private final SetChanges<K> layer;
		private final Set<K> mapDelegate;
		private final Iterator<K> layerIt;
		private final Iterator<K> stateIt;

		private K currentValue;
		private boolean fetched = true;
		private boolean endOfData;

		TransactionalMemorySetIterator(Set<K> setDelegate, SetChanges<K> layer) {
			this.mapDelegate = setDelegate;
			this.layer = layer;
			this.layerIt = layer.getCreatedKeys().iterator();
			this.stateIt = setDelegate.iterator();
		}

		@Override
		public boolean hasNext() {
			if (fetched) {
				currentValue = computeNext();
				fetched = false;
			}
			return !endOfData;
		}

		@Override
		public K next() {
			if (endOfData) {
				throw new NoSuchElementException();
			}
			if (fetched) {
				currentValue = computeNext();
			}
			fetched = true;
			return currentValue;
		}

		@Override
		public void remove() {
			if (currentValue == null) {
				throw new IllegalStateException();
			}

			final K key = currentValue;
			final boolean existing = this.mapDelegate.contains(key);
			final boolean removedFromTransactionalMemory = this.layer.getCreatedKeys().contains(key);
			if (removedFromTransactionalMemory) {
				layerIt.remove();
				if (!existing) {
					layer.removeCreatedKey(key);
				}
			}
			if (existing) {
				layer.registerRemovedKey(key);
			}
		}

		K endOfData() {
			this.endOfData = true;
			return null;
		}

		K computeNext() {
			if (endOfData) {
				return null;
			}
			if (layerIt.hasNext()) {
				return layerIt.next();
			} else if (stateIt.hasNext()) {
				K adept;
				do {
					if (stateIt.hasNext()) {
						adept = stateIt.next();
					} else {
						return endOfData();
					}
				} while (layer.containsRemoved(adept) || layer.containsCreated(adept));
				return adept;
			} else {
				return endOfData();
			}
		}

	}

}
