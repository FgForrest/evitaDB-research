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

import com.esotericsoftware.kryo.Registration;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class maintains meta information necessary for proper deserialization of the Kryo serialized data. Contains
 * compressed key information, used class ids.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
public class MutableCatalogHeader {
	@Getter private final String catalogName;
	@Getter private final WritableClassResolver classResolver;
	@Getter private final Map<Serializable, MutableCatalogEntityHeader> entityTypesIndex;

	public MutableCatalogHeader(String catalogName, WritableClassResolver classResolver, Serializable... entityTypes) {
		this.catalogName = catalogName;
		this.classResolver = classResolver;
		this.entityTypesIndex = Arrays.stream(entityTypes)
			.sorted()
			.collect(
				Collectors.toMap(
					Function.identity(),
					entityType -> new MutableCatalogEntityHeader(
						entityType,
						new WritableClassResolver(KryoFactory.CLASSES_RESERVED_FOR_ENTITY_USE),
						0,
						Collections.emptyMap()
					),
					(header1, header2) -> {
						throw new IllegalArgumentException("Duplicated entity types " + header1.getEntityType());
					},
					LinkedHashMap::new
				)
			);
	}

	public MutableCatalogHeader(String catalogName, WritableClassResolver classResolver, Collection<MutableCatalogEntityHeader> entityHeaders) {
		this.catalogName = catalogName;
		this.classResolver = classResolver;
		this.entityTypesIndex = entityHeaders
			.stream()
			.collect(
				Collectors.toMap(
					MutableCatalogEntityHeader::getEntityType,
					it -> it,
					(header1, header2) -> {
						throw new IllegalArgumentException("Duplicated entity types " + header1.getEntityType());
					},
					LinkedHashMap::new
				)
			);
	}

	/**
	 * Adds new entity type to this catalog.
	 */
	public void addEntityType(MutableCatalogEntityHeader entityHeader) {
		Assert.isTrue(
			!this.entityTypesIndex.containsKey(entityHeader.getEntityType()),
			"This catalog already contains entity of type " + entityHeader.getEntityType()
		);
		this.entityTypesIndex.put(entityHeader.getEntityType(), entityHeader);
	}

	/**
	 * Returns list of all known entity types registered in this header.
	 */
	public Collection<Serializable> getEntityTypes() {
		return entityTypesIndex.keySet();
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

	/**
	 * Returns catalog entity header for particular entity type (if present).
	 */
	@Nullable
	public MutableCatalogEntityHeader getEntityTypeHeader(@Nonnull Serializable entityType) {
		return entityTypesIndex.get(entityType);
	}

	@Override
	public int hashCode() {
		int result = catalogName.hashCode();
		result = 31 * result + entityTypesIndex.hashCode();
		result = 31 * result + classResolver.hashCode();
		result = 31 * result + entityTypesIndex.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MutableCatalogHeader that = (MutableCatalogHeader) o;

		if (!catalogName.equals(that.catalogName)) return false;
		if (!entityTypesIndex.equals(that.entityTypesIndex)) return false;

		for (Registration thisRegistration : classResolver.getRegistrations()) {
			final Registration thatRegistration = that.classResolver.getRegistration(thisRegistration.getId());
			if (thatRegistration == null) return false;
			if (!thisRegistration.getType().equals(thatRegistration.getType())) return false;
		}

		return entityTypesIndex.equals(that.entityTypesIndex);
	}

}
