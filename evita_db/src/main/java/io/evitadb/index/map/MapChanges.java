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

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Contains combination of changes in a Map and removals made upon it. There is no other possible way
 * how to track removals in a map than to keep a set of keys that was removed in it.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
@NotThreadSafe
public class MapChanges<K, V> implements Serializable {
	private static final long serialVersionUID = -6370910459056592080L;

	private final Set<K> removedKeys = Collections.newSetFromMap(new HashMap<>(8));
	private final Map<K, V> modifiedKeys = new HashMap<>(8);
	@Getter private int createdKeyCount;

	public void decreaseCreatedKeyCount() {
		createdKeyCount--;
	}

	@Nonnull
	Iterator<Entry<K, V>> getCreatedOrModifiedValuesIterator() {
		return getCreatedOrModifiedEntries().iterator();
	}

	@Nonnull
	Set<Entry<K, V>> getCreatedOrModifiedEntries() {
		return modifiedKeys.entrySet();
	}

	boolean containsRemoved(K key) {
		return removedKeys.contains(key);
	}

	boolean containsCreatedOrModified(K key) {
		return modifiedKeys.containsKey(key);
	}

	int getRemovedKeyCount() {
		return removedKeys.size();
	}

	boolean containsCreatedOrModifiedValue(V value) {
		return modifiedKeys.containsValue(value);
	}

	V getCreatedOrModifiedValue(K key) {
		return modifiedKeys.get(key);
	}

	boolean removeRemovedKey(K key) {
		return removedKeys.remove(key);
	}

	void clearAll(Set<K> originalKeys) {
		this.createdKeyCount = 0;
		this.modifiedKeys.clear();
		this.removedKeys.addAll(originalKeys);
	}

	V registerModifiedKey(K key, V value) {
		return this.modifiedKeys.put(key, value);
	}

	V registerCreatedKey(K key, V value) {
		final V previous = this.modifiedKeys.put(key, value);
		this.createdKeyCount++;
		return previous;
	}

	void registerRemovedKey(K key) {
		this.removedKeys.add(key);
	}

	V removeModifiedKey(K key) {
		return this.modifiedKeys.remove(key);
	}

	V removeCreatedKey(K key) {
		final V previous = this.modifiedKeys.remove(key);
		this.createdKeyCount--;
		return previous;
	}

	void copyState(MapChanges<K, V> layer) {
		layer.createdKeyCount = this.createdKeyCount;
		layer.removedKeys.addAll(this.removedKeys);
		layer.modifiedKeys.putAll(this.modifiedKeys);
	}

}
