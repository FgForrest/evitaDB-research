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

import io.evitadb.api.CatalogState;
import io.evitadb.api.serialization.ClassId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Evita DB catalog header that contains all key information for loading / persisting Evita DB to disk.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
@RequiredArgsConstructor
public class CatalogHeader {
	/**
	 * Contains name of the {@link io.evitadb.api.Catalog}.
	 */
	@Getter private final String catalogName;
	/**
	 * Contains state of the {@link io.evitadb.api.Catalog}.
	 */
	@Getter private final CatalogState catalogState;
	/**
	 * Contains class index used by {@link com.esotericsoftware.kryo.ClassResolver} that is necessary for deserialization.
	 */
	@Getter private final List<ClassId> registeredClasses;
	/**
	 * Contains last committed transaction id for this catalog.
	 */
	@Getter private final long lastTransactionId;
	/**
	 * Contains index of all {@link io.evitadb.api.EntityCollection} headers that are necessary for accessing the entities.
	 */
	@Getter private final Map<Serializable, CatalogEntityHeader> entityTypesIndex;

	public CatalogHeader(String catalogName, CatalogState catalogState) {
		this.catalogName = catalogName;
		this.catalogState = catalogState;
		this.lastTransactionId = 0;
		this.registeredClasses = Collections.emptyList();
		this.entityTypesIndex = Collections.emptyMap();
	}

	public CatalogHeader(String catalogName, CatalogState catalogState, long lastTransactionId, List<ClassId> registeredClasses, Collection<CatalogEntityHeader> catalogEntityHeaders) {
		this.catalogName = catalogName;
		this.catalogState = catalogState;
		this.lastTransactionId = lastTransactionId;
		this.registeredClasses = registeredClasses;
		this.entityTypesIndex = catalogEntityHeaders
			.stream()
			.collect(
				Collectors.toMap(
					CatalogEntityHeader::getEntityType,
					Function.identity()
				)
			);
	}

	/**
	 * Returns list of all known entity types registered in this header.
	 */
	public Collection<Serializable> getEntityTypes() {
		return entityTypesIndex.keySet();
	}

	/**
	 * Returns list of all known entity types registered in this header.
	 */
	public Collection<CatalogEntityHeader> getEntityTypeHeaders() {
		return entityTypesIndex.values();
	}

}
