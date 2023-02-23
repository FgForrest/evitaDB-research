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
import io.evitadb.api.dataType.Range;
import io.evitadb.index.histogram.HistogramIndex;
import io.evitadb.index.range.RangeIndex;
import io.evitadb.storage.model.storageParts.RecordWithCompressedId;
import lombok.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Filter index container stores index for single {@link io.evitadb.api.schema.AttributeSchema} of the single
 * {@link io.evitadb.api.schema.EntitySchema}. This container object serves only as a storage carrier for
 * {@link io.evitadb.index.attribute.FilterIndex} which is a live memory representation of the data stored in this
 * container.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
@AllArgsConstructor
@ToString(of = {"attributeKey", "entityIndexPrimaryKey"})
public class FilterIndexStoragePart implements AttributeIndexStoragePart, RecordWithCompressedId<AttributeKey> {
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
	 * Histogram is the main data structure that holds the information about value to record ids relation.
	 */
	@Nonnull @Getter private final HistogramIndex<? extends Comparable<?>> histogram;
	/**
	 * Range index is used only for attribute types that are assignable to {@link Range} and can answer questions like:
	 * <p>
	 * - what records are valid at precise moment
	 * - what records are valid until certain moment
	 * - what records are valid after certain moment
	 */
	@Nullable @Getter private final RangeIndex rangeIndex;
	/**
	 * Id used for lookups in {@link io.evitadb.storage.MemTable} for this particular container.
	 */
	@Getter @Setter private Long uniquePartId;

	@Override
	public AttributeIndexType getIndexType() {
		return AttributeIndexType.FILTER;
	}

	@Override
	public AttributeKey getStoragePartSourceKey() {
		return attributeKey;
	}

}
