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

package io.evitadb.api.serialization;

import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;

/**
 * Generic read-write catalog header that contains all key information for loading / persisting Evita records to disk.
 * Class is used only for implementation agnostic use-cases.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class MutableCatalogEntityHeader implements KeyCompressor {
	/**
	 * Type of the entity - {@link io.evitadb.api.schema.EntitySchema#getName()}.
	 */
	@Getter private final Serializable entityType;
	/**
	 * Contains key index extracted from {@link io.evitadb.api.serialization.KeyCompressor} that is necessary for
	 * bootstraping {@link io.evitadb.api.serialization.KeyCompressor} used for MemTable deserialization.
	 */
	@Getter private final Map<Integer, Object> idToKeyIndex;
	/**
	 * Reverse lookup index to {@link #idToKeyIndex}
	 */
	private final Map<Object, Integer> keyToIdIndex;
	/**
	 * Implementation of {@link com.esotericsoftware.kryo.ClassResolver} that allows adding new entries during write.
	 */
	@Getter private final WritableClassResolver classResolver;
	/**
	 * Sequence used for generating new monotonic ids for registered keys.
	 */
	private final AtomicInteger keySequence;
	/**
	 * Contains information about the number of entities in the collection. Servers for informational purposes.
	 */
	@Getter @Setter private int recordCount;

	public MutableCatalogEntityHeader(Serializable entityType, WritableClassResolver classResolver, int recordCount, Map<Integer, Object> keys) {
		this.entityType = entityType;
		this.classResolver = classResolver;
		this.recordCount = recordCount;
		int peek = 0;
		this.idToKeyIndex = createHashMap(keys.size());
		this.keyToIdIndex = createHashMap(keys.size());
		for (Entry<Integer, Object> entry : keys.entrySet()) {
			this.idToKeyIndex.put(entry.getKey(), entry.getValue());
			this.keyToIdIndex.put(entry.getValue(), entry.getKey());
			if (entry.getKey() > peek) {
				peek = entry.getKey();
			}
		}
		this.keySequence = new AtomicInteger(peek);
	}

	/**
	 * This method will create list of all classes currently registered in Kryo instance. Result is created dynamically
	 * and takes some effort so it is advised to call this method with caution and is expected to be called only once
	 * at the end of the serialization process.
	 *
	 * @return list of {@link ClassId} that allows to recreate Kryo instance to the state that will be able to deserialize
	 * serialized contents in consistent way (id and class names must match)
	 */
	public List<ClassId> listRecordedClasses() {
		return classResolver.listRecordedClasses();
	}

	@Nonnull
	@Override
	public Map<Integer, Object> getKeys() {
		return idToKeyIndex;
	}

	@Override
	public <T extends Comparable<T>> int getId(@Nonnull T key) {
		return keyToIdIndex.computeIfAbsent(key, o -> {
			final int id = keySequence.incrementAndGet();
			idToKeyIndex.put(id, o);
			return id;
		});
	}

	@Nullable
	@Override
	public <T extends Comparable<T>> Integer getIdIfExists(@Nonnull T key) {
		return keyToIdIndex.get(key);
	}

	@Nonnull
	@Override
	public <T extends Comparable<T>> T getKeyForId(int id) {
		final Object key = idToKeyIndex.get(id);
		Assert.notNull(key, "There is no key for id " + id + "!");
		//noinspection unchecked
		return (T) key;
	}

	@Override
	public int hashCode() {
		int result = entityType.hashCode();
		result = 31 * result + idToKeyIndex.hashCode();
		result = 31 * result + keyToIdIndex.hashCode();
		result = 31 * result + classResolver.listRecordedClasses().hashCode();
		result = 31 * result + Integer.hashCode(keySequence.get());
		result = 31 * result + recordCount;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MutableCatalogEntityHeader that = (MutableCatalogEntityHeader) o;

		if (recordCount != that.recordCount) return false;
		if (!entityType.equals(that.entityType)) return false;
		if (!idToKeyIndex.equals(that.idToKeyIndex)) return false;
		if (!keyToIdIndex.equals(that.keyToIdIndex)) return false;
		if (!classResolver.listRecordedClasses().equals(that.classResolver.listRecordedClasses())) return false;
		return keySequence.get() == that.keySequence.get();
	}
}
