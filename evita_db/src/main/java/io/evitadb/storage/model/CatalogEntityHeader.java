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

package io.evitadb.storage.model;

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.index.EntityIndexType;
import io.evitadb.storage.model.memTable.FileLocation;
import lombok.Getter;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class holds read only information about the {@link io.evitadb.api.EntityCollection} backed by {@link io.evitadb.storage.MemTable}
 * instance. In this header class there are all information necessary to read the MemTable contents on the disk.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
public class CatalogEntityHeader {
	/**
	 * Type of the entity - {@link EntitySchema#getName()}.
	 */
	@Getter private final Serializable entityType;
	/**
	 * Catalog entity header incremented with each update. Version is not stored on the disk, it serves only to distinguish
	 * whether there is any change made in the header and whether it needs to be persisted on disk.
	 */
	@Getter private final long version;
	/**
	 * Contains information about the number of entities in the collection. Servers for informational purposes.
	 */
	@Getter private final int recordCount;
	/**
	 * Contains key index extracted from {@link io.evitadb.api.serialization.KeyCompressor} that is necessary for
	 * bootstraping {@link io.evitadb.api.serialization.KeyCompressor} used for MemTable deserialization.
	 */
	@Getter private final Map<Integer, Object> idToKeyIndex;
	/**
	 * Contains class index used by {@link com.esotericsoftware.kryo.ClassResolver} that is necessary for deserialization.
	 */
	@Getter private final List<ClassId> registeredClasses;
	/**
	 * Contains {@link io.evitadb.index.EntityIndex} id that belongs to the {@link EntityIndexType#GLOBAL} and is
	 * stored in {@link io.evitadb.storage.MemTable}.
	 */
	@Getter private final Integer globalEntityIndexId;
	/**
	 * Contains list of unique {@link io.evitadb.index.EntityIndex} ids that are stored in {@link io.evitadb.storage.MemTable}.
	 */
	@Getter private final List<Integer> usedEntityIndexIds;
	/**
	 * Contains location of the last MemTable fragment for this version of the header / collection.
	 */
	@Getter private final FileLocation memTableLocation;
	/**
	 * Contains last ID used in {@link #registeredClasses} or seed id if there are no classes registered yet. Newly
	 * registered classes will obtain ID = `lastUsedClassId` + 1.
	 */
	@Getter private final int lastUsedClassId;
	/**
	 * Contains last primary key used by {@link io.evitadb.api.EntityCollection} - but only in case that Evita assignes
	 * new primary keys to the entities. New entity will obtain PK = `lastPrimaryKey` + 1.
	 */
	@Getter private final int lastPrimaryKey;
	/**
	 * Contains last primary key used by {@link io.evitadb.index.EntityIndex}. New entity indexes will obtain
	 * PK = `lastPrimaryKey` + 1.
	 */
	@Getter private final int lastEntityIndexPrimaryKey;
	/**
	 * Contains last assigned id in {@link #idToKeyIndex}. Newly registered key will obtain ID = `lastKeyId` + 1.
	 */
	@Getter private final int lastKeyId;

	public CatalogEntityHeader(Serializable entityType) {
		this(
			entityType, 1L, 0, 0, 0, null,
			Collections.emptyMap(), Collections.emptyList(), null, Collections.emptyList()
		);
	}

	public CatalogEntityHeader(Serializable entityType, long version, int recordCount, int lastPrimaryKey, int lastEntityIndexPrimaryKey, FileLocation memTableLocation, Map<Integer, Object> keys, List<ClassId> registeredClasses, Integer globalIndexId, List<Integer> entityIndexIds) {
		this.entityType = entityType;
		this.version = version;
		this.recordCount = recordCount;
		this.memTableLocation = memTableLocation;
		this.registeredClasses = registeredClasses;
		this.globalEntityIndexId = globalIndexId;
		this.usedEntityIndexIds = entityIndexIds;
		this.idToKeyIndex = Collections.unmodifiableMap(keys);
		this.lastPrimaryKey = lastPrimaryKey;
		this.lastEntityIndexPrimaryKey = lastEntityIndexPrimaryKey;
		this.lastKeyId = keys.keySet().stream().max(Comparator.comparingInt(o -> o)).orElse(1);
		this.lastUsedClassId = registeredClasses
			.stream()
			.max(Comparator.comparingInt(ClassId::getId))
			.map(ClassId::getId)
			.orElse(KryoFactory.CLASSES_RESERVED_FOR_ENTITY_USE);
	}

}
