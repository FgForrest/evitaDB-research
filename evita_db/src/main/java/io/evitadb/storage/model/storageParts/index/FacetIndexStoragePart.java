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

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.NumberUtils;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.storage.model.storageParts.ComparableReferencedType;
import io.evitadb.storage.model.storageParts.StoragePart;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.Map;

/**
 * Facet index collocates information about facets in entities. This container object serves only as a storage carrier
 * for {@link io.evitadb.index.facet.FacetIndex} which is a live memory representation of the data stored in this
 * container.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@ToString(of = {"referencedEntityType", "entityIndexPrimaryKey"})
public class FacetIndexStoragePart implements StoragePart {
	private static final long serialVersionUID = -2348533783771242845L;

	/**
	 * Unique id that identifies {@link io.evitadb.index.EntityIndex}.
	 */
	@Getter private final int entityIndexPrimaryKey;
	/**
	 * Refers to {@link EntitySchema#getName()}
	 */
	@Getter private final Serializable referencedEntityType;
	/**
	 * Refers to facets that are not assigned to any group.
	 */
	@Getter private final Map<Integer, Bitmap> noGroupFacetingEntities;
	/**
	 * Contains information about referenced entities facet.
	 */
	@Getter private final Map<Integer, Map<Integer, Bitmap>> facetingEntities;
	/**
	 * Id used for lookups in {@link io.evitadb.storage.MemTable} for this particular container.
	 */
	@Getter @Setter private Long uniquePartId;

	public FacetIndexStoragePart(int entityIndexPrimaryKey, @Nonnull Serializable referencedEntityType, @Nullable Map<Integer, Bitmap> noGroupFacetingEntities, @Nonnull Map<Integer, Map<Integer, Bitmap>> facetingEntities) {
		this.entityIndexPrimaryKey = entityIndexPrimaryKey;
		this.referencedEntityType = referencedEntityType;
		this.noGroupFacetingEntities = noGroupFacetingEntities;
		this.facetingEntities = facetingEntities;
	}

	public static long computeUniquePartId(Integer entityIndexPrimaryKey, Serializable referencedEntityType, KeyCompressor keyCompressor) {
		return NumberUtils.join(entityIndexPrimaryKey, keyCompressor.getId(new ComparableReferencedType(referencedEntityType)));
	}

	@Override
	public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
		final long computedUniquePartId = computeUniquePartId(entityIndexPrimaryKey, referencedEntityType, keyCompressor);
		final Long theUniquePartId = getUniquePartId();
		if (theUniquePartId == null) {
			setUniquePartId(computedUniquePartId);
		} else {
			Assert.isTrue(theUniquePartId == computedUniquePartId, "Unique part ids must never differ!");
		}
		return computedUniquePartId;
	}

}
