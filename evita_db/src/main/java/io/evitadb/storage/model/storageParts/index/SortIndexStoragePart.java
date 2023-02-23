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

package io.evitadb.storage.model.storageParts.index;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.storage.model.storageParts.RecordWithCompressedId;
import lombok.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

/**
 * Filter index container stores index for single {@link io.evitadb.api.schema.AttributeSchema} of the single
 * {@link io.evitadb.api.schema.EntitySchema}. This container object serves only as a storage carrier for
 * {@link io.evitadb.index.attribute.SortIndex} which is a live memory representation of the data stored in this
 * container.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@RequiredArgsConstructor
@AllArgsConstructor
@ToString(of = "attributeKey")
public class SortIndexStoragePart implements AttributeIndexStoragePart, RecordWithCompressedId<AttributeKey> {
	private static final long serialVersionUID = 6163295675316818632L;

	/**
	 * Unique id that identifies {@link io.evitadb.index.EntityIndex}.
	 */
	@Getter private final Integer entityIndexPrimaryKey;
	/**
	 * Contains name and locale of the indexed attribute.
	 */
	@Getter private final AttributeKey attributeKey;
	/**
	 * Contains type of the attribute.
	 */
	@Getter private final Class<? extends Comparable<?>> type;
	/**
	 * Contains record ids sorted by assigned values. The array is divided in so called record ids block that respects
	 * order in {@link #sortedRecordsValues}. Record ids within the same block are sorted naturally by their integer id.
	 */
	@Getter private final int[] sortedRecords;
	/**
	 * Contains comparable values sorted naturally by their {@link Comparable} characteristics.
	 */
	@Getter private final Comparable<?>[] sortedRecordsValues;
	/**
	 * Map contains only values with cardinalities greater than one. It is expected that records will have scarce values
	 * with low cardinality so this should save a lot of memory.
	 */
	@Getter private final Map<Comparable<?>, Integer> valueCardinalities;
	/**
	 * Id used for lookups in {@link io.evitadb.storage.MemTable} for this particular container.
	 */
	@Getter @Setter private Long uniquePartId;

	@Override
	public AttributeIndexType getIndexType() {
		return AttributeIndexType.SORT;
	}

	@Override
	public AttributeKey getStoragePartSourceKey() {
		return attributeKey;
	}

}
