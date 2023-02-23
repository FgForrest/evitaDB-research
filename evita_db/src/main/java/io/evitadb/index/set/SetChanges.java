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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains combination of changes in a Set and removals made upon it. There is no other possible way
 * how to track removals in a map than to keep a set of keys that was removed in it.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
@NotThreadSafe
public class SetChanges<K> implements Serializable {
	private static final long serialVersionUID = -6370910459056592080L;
	private final Set<K> removedKeys = new HashSet<>();
	private final Set<K> createdKeys = new HashSet<>();

	public int getCreatedKeyCount() {
		return createdKeys.size();
	}

	void clearAll(Set<K> originalKeys) {
		this.createdKeys.clear();
		this.removedKeys.addAll(originalKeys);
	}

	boolean containsCreated(K key) {
		return createdKeys.contains(key);
	}

	void registerCreatedKey(K key) {
		this.createdKeys.add(key);
	}

	void removeCreatedKey(K key) {
		this.createdKeys.remove(key);
	}

	@Nonnull
	Set<K> getCreatedKeys() {
		return createdKeys;
	}

	void registerRemovedKey(K key) {
		this.removedKeys.add(key);
	}

	void removeRemovedKey(K key) {
		removedKeys.remove(key);
	}

	boolean containsRemoved(K key) {
		return removedKeys.contains(key);
	}

	int getRemovedKeyCount() {
		return removedKeys.size();
	}

	void copyState(SetChanges<K> layer) {
		layer.createdKeys.addAll(this.createdKeys);
		layer.removedKeys.addAll(this.removedKeys);
	}

}
