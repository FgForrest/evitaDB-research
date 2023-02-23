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

import io.evitadb.api.Transaction;
import io.evitadb.index.transactionalMemory.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;

/**
 * This class envelopes simple map and makes it transactional. This means, that the map contents can be updated
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
public class TransactionalMemoryMap<K, V> implements Map<K, V>, Serializable, Cloneable, TransactionalLayerCreator<MapChanges<K, V>>, TransactionalLayerProducer<MapChanges<K, V>, Map<K, V>> {
	private static final long serialVersionUID = 1111377458028103813L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	private final Map<K, V> mapDelegate;

	public TransactionalMemoryMap(Map<K, V> mapDelegate) {
		this.mapDelegate = mapDelegate;
	}

	/*
		TransactionalLayerCreator IMPLEMENTATION
	 */

	@Override
	public MapChanges<K, V> createLayer() {
		return new MapChanges<>();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Map<K, V> createCopyWithMergedTransactionalMemory(MapChanges<K, V> layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		// create new hash map of requested size
		final HashMap<K, V> copy = createHashMap(mapDelegate.size());
		// iterate original map and copy all values from it
		for (Entry<K, V> entry : mapDelegate.entrySet()) {
			K key = entry.getKey();
			// we need to always create copy - something in the referenced object might have changed
			// even the removed values need to be evaluated (in order to discard them from transactional memory set)
			if (key instanceof TransactionalLayerProducer) {
				key = (K) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) key, transaction);
			}
			V value = entry.getValue();
			if (value instanceof TransactionalLayerProducer) {
				value = (V) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) value, transaction);
			}
			// except those that were removed
			if (layer == null || !layer.containsRemoved(key)) {
				copy.put(key, value);
			}
		}

		// iterate over inserted or updated keys
		if (layer != null) {
			for (Entry<K, V> entry : layer.getCreatedOrModifiedEntries()) {
				K key = entry.getKey();
				// we need to always create copy - something in the referenced object might have changed
				if (key instanceof TransactionalLayerProducer) {
					key = (K) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) key, transaction);
				}
				V value = entry.getValue();
				if (value instanceof TransactionalLayerProducer) {
					value = (V) transactionalLayer.getStateCopyWithCommittedChanges((TransactionalLayerProducer) value, transaction);
				}
				// update the value
				copy.put(key, value);
			}
		}

		return copy;
	}

	/*
		MAP CONTRACT IMPLEMENTATION
	 */

	@Override
	public int size() {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.size();
		} else {
			return this.mapDelegate.size() - layer.getRemovedKeyCount() + layer.getCreatedKeyCount();
		}
	}

	@Override
	public boolean isEmpty() {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.isEmpty();
		} else {
			if (layer.getRemovedKeyCount() == 0 && layer.getCreatedKeyCount() == 0) {
				return mapDelegate.isEmpty();
			} else {
				return size() == 0;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.containsKey(key);
		} else {
			if (layer.containsCreatedOrModified((K) key)) {
				return true;
			} else if (layer.containsRemoved((K) key)) {
				return false;
			} else {
				return this.mapDelegate.containsKey(key);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsValue(Object value) {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.containsValue(value);
		} else {
			if (layer.containsCreatedOrModifiedValue((V) value)) {
				return true;
			} else {
				for (Entry<K, V> entry : mapDelegate.entrySet()) {
					if (Objects.equals(value, entry.getValue())) {
						return !layer.containsRemoved(entry.getKey());
					}
				}
				return false;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.get(key);
		} else {
			if (layer.containsRemoved((K) key)) {
				return null;
			} else if (layer.containsCreatedOrModified((K) key)) {
				return layer.getCreatedOrModifiedValue((K) key);
			} else {
				return this.mapDelegate.get(key);
			}
		}
	}

	@Override
	public V put(K key, V value) {
		// in some cases the default value from PropertyAccessor are set here -> result is NotSerializableException later
		if (value != null && value.getClass() == Object.class) {
			return null;
		}
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.mapDelegate.put(key, value);
		} else {
			return putInternal(key, value, layer);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			return this.mapDelegate.remove(key);
		} else {
			final V originalValue;
			final boolean existing = this.mapDelegate.containsKey(key);
			if (existing && layer.containsRemoved((K) key)) {
				// value has been already removed - report null and do nothing
				return null;
			}
			if (layer.containsCreatedOrModified((K) key)) {
				originalValue = existing ? layer.removeModifiedKey((K) key) : layer.removeCreatedKey((K) key);
			} else {
				originalValue = this.mapDelegate.get(key);
			}
			if (existing) {
				layer.registerRemovedKey((K) key);
			}
			return originalValue;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> t) {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.mapDelegate.putAll(t);
		} else {
			for (Entry<? extends K, ? extends V> entry : t.entrySet()) {
				putInternal(entry.getKey(), entry.getValue(), layer);
			}
		}
	}

	@Override
	public void clear() {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayer(this);
		if (layer == null) {
			this.mapDelegate.clear();
		} else {
			layer.clearAll(this.mapDelegate.keySet());
		}
	}

	@Override
	public Set<K> keySet() {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.keySet();
		} else {
			return new TransactionalMemoryKeySet();
		}
	}

	@Override
	public Collection<V> values() {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.values();
		} else {
			return new TransactionalMemoryValues();
		}
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer == null) {
			return this.mapDelegate.entrySet();
		} else {
			return new TransactionalMemoryEntrySet(layer);
		}
	}

	public int hashCode() {
		int h = 0;
		for (Entry<K, V> kvEntry : entrySet()) h += kvEntry.hashCode();
		return h;
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Map))
			return false;
		Map<K, V> m = (Map<K, V>) o;
		if (m.size() != size())
			return false;

		try {
			for (Entry<K, V> e : entrySet()) {
				K key = e.getKey();
				V value = e.getValue();
				if (value == null) {
					if (!(m.get(key) == null && m.containsKey(key)))
						return false;
				} else {
					if (!value.equals(m.get(key)))
						return false;
				}
			}
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}

		return true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		@SuppressWarnings("unchecked") final TransactionalMemoryMap<K, V> clone = (TransactionalMemoryMap<K, V>) super.clone();
		final MapChanges<K, V> layer = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		if (layer != null) {
			final MapChanges<K, V> clonedLayer = TransactionalMemory.getTransactionalMemoryLayer(clone);
			if (clonedLayer != null) {
				clonedLayer.copyState(layer);
			}
		}
		return clone;
	}

	public String toString() {
		final Iterator<Entry<K, V>> i = entrySet().iterator();
		if (!i.hasNext())
			return "{}";

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (; ; ) {
			Entry<K, V> e = i.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key == this ? "(this Map)" : key);
			sb.append('=');
			sb.append(value == this ? "(this Map)" : value);
			if (!i.hasNext())
				return sb.append('}').toString();
			sb.append(',').append(' ');
		}
	}

	/*
		INTERNALS
	 */

	private V putInternal(K key, V value, MapChanges<K, V> layer) {
		final V originalValue;
		if (layer.containsCreatedOrModified(key)) {
			originalValue = layer.registerModifiedKey(key, value);
		} else {
			originalValue = this.mapDelegate.get(key);
			if (this.mapDelegate.containsKey(key)) {
				layer.registerModifiedKey(key, value);
			} else {
				layer.registerCreatedKey(key, value);
			}
		}
		layer.removeRemovedKey(key);
		return originalValue;
	}

	/**
	 * Iterator implementation that aggregates values from the original map with modified data on transaction level.
	 */
	private static class TransactionalMemoryEntryAbstractIterator<K, V> implements Iterator<Entry<K, V>> {
		private final MapChanges<K, V> layer;
		private final Map<K, V> mapDelegate;
		private final Iterator<Entry<K, V>> layerIt;
		private final Iterator<Entry<K, V>> stateIt;

		private Entry<K, V> currentValue;
		private boolean fetched = true;
		private boolean endOfData;

		TransactionalMemoryEntryAbstractIterator(Map<K, V> mapDelegate, MapChanges<K, V> layer) {
			this.mapDelegate = mapDelegate;
			this.layer = layer;
			this.layerIt = layer.getCreatedOrModifiedValuesIterator();
			this.stateIt = mapDelegate.entrySet().iterator();
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
		public Entry<K, V> next() {
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

			final K key = currentValue.getKey();
			final boolean existing = this.mapDelegate.containsKey(key);
			boolean removedFromTransactionalMemory = !(currentValue instanceof TransactionalMemoryEntryWrapper);
			if (removedFromTransactionalMemory) {
				layerIt.remove();
				if (!existing) {
					layer.decreaseCreatedKeyCount();
				}
			}
			if (existing) {
				layer.registerRemovedKey(key);
			}
		}

		Entry<K, V> endOfData() {
			this.endOfData = true;
			return null;
		}

		Entry<K, V> computeNext() {
			if (endOfData) {
				return null;
			}
			if (layerIt.hasNext()) {
				return layerIt.next();
			} else if (stateIt.hasNext()) {
				Entry<K, V> adept;
				do {
					if (stateIt.hasNext()) {
						adept = stateIt.next();
					} else {
						return endOfData();
					}
				} while (layer.containsRemoved(adept.getKey()) || layer.containsCreatedOrModified(adept.getKey()));
				return new TransactionalMemoryEntryWrapper<>(layer, adept);
			} else {
				return endOfData();
			}
		}

	}

	@RequiredArgsConstructor
	private static class TransactionalMemoryEntryWrapper<K, V> implements Entry<K, V> {
		private final MapChanges<K, V> layer;
		private final Entry<K, V> delegate;

		@Override
		public K getKey() {
			return delegate.getKey();
		}

		@Override
		public V getValue() {
			return delegate.getValue();
		}

		@Override
		public V setValue(V value) {
			return layer.registerModifiedKey(delegate.getKey(), value);
		}

		@Override
		public int hashCode() {
			final V overwrittenValue = layer.getCreatedOrModifiedValue(delegate.getKey());
			return overwrittenValue != null ? overwrittenValue.hashCode() : delegate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!delegate.getClass().isInstance(obj)) {
				return false;
			}
			final V overwrittenValue = layer.getCreatedOrModifiedValue(delegate.getKey());
			return overwrittenValue != null ? overwrittenValue.equals(obj) : delegate.equals(obj);
		}

		@Override
		public String toString() {
			final V overwrittenValue = layer.getCreatedOrModifiedValue(delegate.getKey());
			return overwrittenValue != null ? overwrittenValue.toString() : delegate.toString();
		}
	}

	/**
	 * Basic implementation that maps key set in main class. Iterator is delegated to {@link TransactionalMemoryEntryAbstractIterator}.
	 */
	private class TransactionalMemoryKeySet extends AbstractSet<K> {

		@Nonnull
		@Override
		public Iterator<K> iterator() {
			return new Iterator<>() {
				private final Iterator<Entry<K, V>> i = TransactionalMemoryMap.this.entrySet().iterator();

				@Override
				public boolean hasNext() {
					return i.hasNext();
				}

				@Override
				public K next() {
					return i.next().getKey();
				}

				@Override
				public void remove() {
					i.remove();
				}
			};
		}

		@Override
		public int size() {
			return TransactionalMemoryMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return TransactionalMemoryMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object k) {
			return TransactionalMemoryMap.this.containsKey(k);
		}

		@Override
		public void clear() {
			TransactionalMemoryMap.this.clear();
		}
	}

	/**
	 * Basic implementation that maps value set in main class. Iterator is delegated to {@link TransactionalMemoryEntryAbstractIterator}.
	 */
	private class TransactionalMemoryValues extends AbstractCollection<V> {

		@Nonnull
		@Override
		public Iterator<V> iterator() {
			return new Iterator<>() {
				private final Iterator<Entry<K, V>> i = entrySet().iterator();

				@Override
				public boolean hasNext() {
					return i.hasNext();
				}

				@Override
				public V next() {
					return i.next().getValue();
				}

				@Override
				public void remove() {
					i.remove();
				}
			};
		}

		@Override
		public int size() {
			return TransactionalMemoryMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return TransactionalMemoryMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object v) {
			return TransactionalMemoryMap.this.containsValue(v);
		}

		@Override
		public void clear() {
			TransactionalMemoryMap.this.clear();
		}

	}

	/**
	 * Basic implementation that entry key set in main class. Iterator is delegated to {@link TransactionalMemoryEntryAbstractIterator}.
	 */
	private class TransactionalMemoryEntrySet extends AbstractSet<Entry<K, V>> {
		private final MapChanges<K, V> layer;

		public TransactionalMemoryEntrySet(@Nonnull MapChanges<K, V> layer) {
			this.layer = layer;
		}

		@Nonnull
		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new TransactionalMemoryEntryAbstractIterator<>(
				TransactionalMemoryMap.this.mapDelegate,
				layer
			);
		}

		@Override
		public int size() {
			return TransactionalMemoryMap.this.size();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			@SuppressWarnings("unchecked") TransactionalMemoryEntrySet that = (TransactionalMemoryEntrySet) o;
			return layer.equals(that.layer);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), layer);
		}
	}

}
