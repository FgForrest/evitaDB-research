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

package io.evitadb.api.dataType;

import io.evitadb.api.dataType.trie.Trie;
import io.evitadb.api.utils.MemoryMeasuringConstants;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;

/**
 * This class is used for (de)serialization of complex custom POJO classes in some generic form that doesn't change in time.
 * Client code will use {@link io.evitadb.api.data.DataObjectConverter} to serialize and deserialize objects of this type
 * to their respective POJO instances.
 *
 * Pojo properties are serialized in simple form:
 *
 * SIMPLE POJO PROPERTIES:
 * property=value
 *
 * NESTED POJO PROPERTIES:
 * property.subProperty=value
 *
 * ARRAY/LIST/SET PROPERTIES:
 * property[index]=value
 *
 * MAP PROPERTIES:
 * property[key]=value
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ComplexDataObject implements Serializable {
	public static final char INDEX_CHAR_START = '[';
	public static final char INDEX_CHAR_END = ']';
	public static final char SUB_PROPERTY_DELIMITER = '.';
	public static final String MAP_KEY_PREFIX = "#";
	private static final long serialVersionUID = -2394324801730957983L;
	final List<Serializable> keys;
	final Trie<Serializable> properties;
	private final Map<Serializable, Integer> keyIndex;

	public ComplexDataObject() {
		this.keyIndex = new HashMap<>();
		this.keys = new LinkedList<>();
		this.properties = new Trie<>(Serializable.class);
	}

	public ComplexDataObject(List<Serializable> keys) {
		this.keyIndex = createHashMap(keys.size());
		int index = 0;
		for (Serializable key : keys) {
			this.keyIndex.put(key, index++);
		}
		this.keys = keys;
		this.properties = new Trie<>(Serializable.class);
	}

	ComplexDataObject(List<Serializable> keys, Trie<Serializable> properties) {
		this.keyIndex = createHashMap(keys.size());
		int index = 0;
		for (Serializable key : keys) {
			this.keyIndex.put(key, index++);
		}
		this.keys = keys;
		this.properties = properties;
	}

	/**
	 * Returns identificator for passed map key. The identificator is then used as a placeholder instead of the key object
	 * contents in the property key. During reconstruction is the key is used for retrieving original contents back.
	 *
	 * @see #getKeyForId(int)
	 */
	public Integer registerMapKey(Serializable theKey) {
		return keyIndex.computeIfAbsent(
			theKey,
			it -> {
				final int position = keys.size();
				keys.add(theKey);
				return position;
			}
		);
	}

	/**
	 * Returns already assigned id for a key. Key must have been registered previously by calling {@link #registerMapKey(Serializable)}
	 * in order to return any value.
	 */
	public int getIdForKey(Serializable key) {
		return keyIndex.get(key);
	}

	/**
	 * Returns original key for id. Key must have been registered previously by calling {@link #registerMapKey(Serializable)}
	 * in order to return any value.
	 */
	public Serializable getKeyForId(int id) {
		return keys.size() > id && id >= 0 ? keys.get(id) : null;
	}

	/**
	 * Returns list of all registered map keys.
	 */
	public List<Serializable> getKeys() {
		return Collections.unmodifiableList(keys);
	}

	/**
	 * Stores new property value.
	 */
	public void setProperty(String name, Serializable value) {
		this.properties.insert(name, value);
	}

	/**
	 * Returns property value.
	 */
	@Nullable
	public Serializable getProperty(String name) {
		return this.properties.getValueForWord(name);
	}

	/**
	 * Returns set of all property names of the object.
	 */
	public Set<String> getPropertyNames() {
		return this.properties.getWords();
	}

	/**
	 * Returns set of all properties on nearest level under the prefix.
	 *
	 * Example properties:
	 *
	 * id
	 * name
	 * innerPojo.id
	 * innerPojo.name
	 *
	 * When calling this method with no prefix - this will be returned: id, name, innerPojo
	 * When calling this method with prefix `innerPojo` - this will be returned: id, name
	 */
	public Collection<String> getTopPropertyNames(String prefix) {
		return this.properties.getWordsBetweenOrStartingWith(prefix, SUB_PROPERTY_DELIMITER, INDEX_CHAR_START);
	}

	/**
	 * Returns true if there is single property starting with the prefix.
	 */
	public boolean hasAnyPropertyFor(String propertyNameStart) {
		return this.properties.containsWordStartingWith(propertyNameStart);
	}

	/**
	 * Returns set of keys for indexed property.
	 */
	public Set<String> getIndexStrings(String basePropertyName) {
		return this.properties.getWordsBetween(basePropertyName + INDEX_CHAR_START, INDEX_CHAR_END);
	}

	/**
	 * Returns count of properties in the object.
	 */
	public int size() {
		return this.properties.size();
	}

	@Override
	public int hashCode() {
		int result = keyIndex.hashCode();
		result = 31 * result + properties.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComplexDataObject that = (ComplexDataObject) o;

		if (!keyIndex.equals(that.keyIndex)) return false;
		return properties.equals(that.properties);
	}

	@Override
	public String toString() {
		final TreeMap<String, Serializable> orderedLines = new TreeMap<>();
		for (Entry<String, Serializable> entry : properties.wordToValueEntrySet()) {
			orderedLines.put(entry.getKey(), entry.getValue());
		}
		final StringBuilder sb = new StringBuilder();
		for (Entry<String, Serializable> entry : orderedLines.entrySet()) {
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	public int estimateSize() {
		return MemoryMeasuringConstants.OBJECT_HEADER_SIZE
			+ MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.computeLinkedListSize(keys)
			+ MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.computeHashMapSize(keyIndex)
			+ properties.estimateSize(value -> {
				if (value instanceof NullValue) {
					return MemoryMeasuringConstants.REFERENCE_SIZE;
				} else if (value instanceof EmptyValue) {
					return MemoryMeasuringConstants.REFERENCE_SIZE;
				} else {
					return EvitaDataTypes.estimateSize(value);
				}
		});
	}

	/**
	 * This class is used for marking NULL values.
	 */
	public static class NullValue implements Serializable {
		public static final NullValue INSTANCE = new NullValue();
		private static final long serialVersionUID = 3813952517361452162L;

		@Override
		public String toString() {
			return "<NULL>";
		}
	}

	/**
	 * This class is used for marking empty collections.
	 */
	public static class EmptyValue implements Serializable {
		public static final EmptyValue INSTANCE = new EmptyValue();
		private static final long serialVersionUID = 5930375668493204868L;

		@Override
		public String toString() {
			return "<EMPTY>";
		}
	}

}
