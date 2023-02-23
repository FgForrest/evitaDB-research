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

package io.evitadb.api.serialization.utils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * This helper class allows to safely unwrap otherwise unwrappable instances of {@link Collections#unmodifiableMap(Map)}
 * and similar methods that are used in immutable classes to protect changes in inner collections. Unwrapped implementations
 * are necessary in Kryo serialization process.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UnmodifiableCollectionsUnwrapper {
	private static final Field SOURCE_COLLECTION_FIELD;
	private static final Field SOURCE_MAP_FIELD;
	private static final Set<?> EMPTY_REAL_SET = new HashSet<>(0);
	private static final Set<?> EMPTY_SET = Collections.emptySet();
	private static final Map<?,?> EMPTY_REAL_MAP = new HashMap<>(0);
	private static final Map<?,?> EMPTY_MAP = Collections.emptyMap();
	private static final List<?> EMPTY_REAL_LIST = new ArrayList<>(0);
	private static final List<?> EMPTY_LIST = Collections.emptyList();

	static {
		try {
			SOURCE_COLLECTION_FIELD = Class.forName("java.util.Collections$UnmodifiableCollection").getDeclaredField( "c" );
			SOURCE_COLLECTION_FIELD.setAccessible(true);

			SOURCE_MAP_FIELD = Class.forName("java.util.Collections$UnmodifiableMap").getDeclaredField( "m" );
			SOURCE_MAP_FIELD.setAccessible(true);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Could not access source collection  field in java.util.Collections$UnmodifiableCollection.", e
			);
		}
	}

	private UnmodifiableCollectionsUnwrapper() {}

	public static <K,V> Map<K, V> unwrap(final Map<K, V> object) {
		try {
			final UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType(object.getClass());
			//noinspection unchecked
			final Map<K, V> unwrapped = (Map<K, V>) unmodifiableCollection.sourceCollectionField.get(object);
			//noinspection unchecked
			return unwrapped == EMPTY_MAP ? (Map<K, V>) EMPTY_REAL_MAP : unwrapped;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot unwrap unmodifiable map!", e);
		}
	}

	public static <V> Set<V> unwrap(final Set<V> object) {
		try {
			final UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType(object.getClass());
			//noinspection unchecked
			final Set<V> unwrapped = (Set<V>) unmodifiableCollection.sourceCollectionField.get(object);
			//noinspection unchecked
			return unwrapped == EMPTY_SET ? (Set<V>) EMPTY_REAL_SET : unwrapped;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot unwrap unmodifiable set!", e);
		}
	}

	public static <V> List<V> unwrap(final List<V> object) {
		try {
			final UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType(object.getClass());
			//noinspection unchecked
			final List<V> unwrapped = (List<V>) unmodifiableCollection.sourceCollectionField.get(object);
			//noinspection unchecked
			return unwrapped == EMPTY_LIST ? (List<V>) EMPTY_REAL_LIST : unwrapped;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot unwrap unmodifiable list!", e);
		}
	}

	public static <V> Collection<V> unwrap(final Collection<V> object) {
		try {
			final UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType(object.getClass());
			//noinspection unchecked
			final Collection<V> unwrapped = (Collection<V>) unmodifiableCollection.sourceCollectionField.get(object);
			if (unwrapped == EMPTY_LIST) {
				//noinspection unchecked
				return (Collection<V>) EMPTY_REAL_LIST;
			} else if (unwrapped == EMPTY_SET) {
				//noinspection unchecked
				return (Collection<V>) EMPTY_REAL_SET;
			} else {
				return unwrapped;
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot unwrap unmodifiable collection!", e);
		}
	}

	private enum UnmodifiableCollection {
		COLLECTION( Collections.unmodifiableCollection(Arrays.asList("")).getClass(), SOURCE_COLLECTION_FIELD ),
		RANDOM_ACCESS_LIST( Collections.unmodifiableList( new ArrayList<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ),
		LIST( Collections.unmodifiableList( new LinkedList<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ),
		SET( Collections.unmodifiableSet( new HashSet<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ),
		SORTED_SET( Collections.unmodifiableSortedSet( new TreeSet<Void>() ).getClass(), SOURCE_COLLECTION_FIELD ),
		MAP( Collections.unmodifiableMap( new HashMap<Void, Void>() ).getClass(), SOURCE_MAP_FIELD ),
		SORTED_MAP( Collections.unmodifiableSortedMap( new TreeMap<Void, Void>() ).getClass(), SOURCE_MAP_FIELD );

		private final Class<?> type;
		private final Field sourceCollectionField;

		UnmodifiableCollection(final Class<?> type, final Field sourceCollectionField) {
			this.type = type;
			this.sourceCollectionField = sourceCollectionField;
		}

		static UnmodifiableCollection valueOfType( final Class<?> type ) {
			for( final UnmodifiableCollection item : values() ) {
				if ( item.type.equals( type ) ) {
					return item;
				}
			}
			throw new IllegalArgumentException( "The type " + type + " is not supported." );
		}

	}

}