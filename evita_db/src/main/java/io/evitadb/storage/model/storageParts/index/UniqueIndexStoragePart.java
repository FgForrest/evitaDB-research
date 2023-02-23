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
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.storage.model.storageParts.RecordWithCompressedId;
import lombok.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.Map;

/**
 * Filter index container stores index for single {@link io.evitadb.api.schema.AttributeSchema} of the single
 * {@link io.evitadb.api.schema.EntitySchema}. This container object serves only as a storage carrier for
 * {@link io.evitadb.index.attribute.UniqueIndex} which is a live memory representation of the data stored in this
 * container.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@RequiredArgsConstructor
@AllArgsConstructor
@ToString(of = "attributeKey")
public class UniqueIndexStoragePart implements AttributeIndexStoragePart, RecordWithCompressedId<AttributeKey> {
	private static final long serialVersionUID = -4095785894036417656L;

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
	@Getter private final Class<? extends Serializable> type;
	/**
	 * Keeps the unique value to record id mappings. Fairly large HashMap is expected here.
	 */
	@Getter private final Map<Serializable, Integer> uniqueValueToRecordId;
	/**
	 * Keeps information about all record ids resent in this index.
	 */
	@Getter private final Bitmap recordIds;
	/**
	 * Id used for lookups in {@link io.evitadb.storage.MemTable} for this particular container.
	 */
	@Getter @Setter private Long uniquePartId;

	@Override
	public AttributeIndexType getIndexType() {
		return AttributeIndexType.UNIQUE;
	}

	@Override
	public AttributeKey getStoragePartSourceKey() {
		return attributeKey;
	}

}
