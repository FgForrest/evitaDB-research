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

import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Transactional overlay data object for {@link TransactionalMemoryList} that keeps track of removed and added items
 * in the list.
 *
 * Contains combination of changes in a List and removals made upon it. There is no other possible way
 * how to track removals in a list than to keep an collection of removed ones.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2017
 */
@NotThreadSafe
class ListChanges<V> implements Serializable {
	private static final long serialVersionUID = -4217133814767167202L;
	/**
	 * Set of removed positions in a list (indexes).
	 */
	@Getter private final TreeSet<Integer> removedItems = new TreeSet<>();
	/**
	 * Map of added items on certain indexes.
	 */
	@Getter private final Map<Integer, V> addedItems = new TreeMap<>();

	/**
	 * Decreases indexes of all items above (excluding) passed position by one.
	 * @param position
	 */
	void lowerIndexesGreaterThan(Integer position) {
		final Map<Integer, V> items = new HashMap<>();
		final Iterator<Entry<Integer, V>> it = addedItems.entrySet().iterator();
		while (it.hasNext()) {
			final Entry<Integer, V> removedPosition = it.next();
			if (removedPosition.getKey() > position) {
				Assert.isTrue(
					removedPosition.getKey() - 1 > -1,
					"Illegal state - attempt to lower index of element that is at the start of the list!"
				);
				items.put(removedPosition.getKey() - 1, removedPosition.getValue());
				it.remove();
			}
		}
		addedItems.putAll(items);
	}

	/**
	 * Increased indexes of all items above (including) passed position by one.
	 * @param position
	 */
	void increaseIndexesGreaterThanOrEquals(Integer position) {
		final Map<Integer, V> items = new HashMap<>();
		final Iterator<Entry<Integer, V>> it = addedItems.entrySet().iterator();
		while (it.hasNext()) {
			final Entry<Integer, V> removedPosition = it.next();
			if (removedPosition.getKey() >= position) {
				items.put(removedPosition.getKey() + 1, removedPosition.getValue());
				it.remove();
			}
		}
		addedItems.putAll(items);
	}
}
