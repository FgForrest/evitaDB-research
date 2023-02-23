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

package io.evitadb.index.facet;

import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.function.TriFunction;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.facet.FacetGroupFormula;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FacetIndexContract describes the API of {@link FacetIndex} that maintains data structures for fast accessing
 * faceted {@link io.evitadb.api.data.ReferenceContract references} of the entities.
 *
 * Purpose of this contract interface is to ease using {@link @lombok.experimental.Delegate} annotation
 * in {@link io.evitadb.index.EntityIndex} and minimize the amount of the code in this complex class by automatically
 * delegating all {@link FacetIndexContract} methods to the {@link FacetIndex} implementation that is part
 * of this index.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface FacetIndexContract {

	/**
	 * Registers new facet to `referenceKey` for passed `entityPrimaryKey`.
	 */
	void addFacet(@Nonnull EntityReferenceContract<?> referenceKey, @Nullable GroupEntityReference groupReference, int entityPrimaryKey);

	/**
	 * Unregisters existing facet to `referenceKey` for passed `entityPrimaryKey`. Method automatically removes all
	 * indexes that become empty and useless after this operation.
	 *
	 * @throws IllegalArgumentException when `entityPrimaryKey` is not present in the index for passed facet
	 */
	void removeFacet(@Nonnull EntityReferenceContract<?> referenceKey, @Nullable Integer groupId, int entityPrimaryKey);

	/**
	 * Returns set of {@link EntityReference#getType()} that has at least one facet indexed.
	 */
	Set<Serializable> getReferencedEntities();

	/**
	 * Method returns formula that allows computation of all entity primary keys that have at least one
	 * of `facetId` of passed `entityType` as its faceted reference.
	 */
	List<FacetGroupFormula> getFacetReferencingEntityIdsFormula(
		@Nonnull Serializable entityType,
		@Nonnull TriFunction<Integer, int[], Bitmap[], FacetGroupFormula> formulaFactory,
		@Nonnull int... facetId
	);

	/**
	 * Method returns true if facet id is part of the passed group id for specified `entityType`.
	 */
	boolean isFacetInGroup(@Nonnull Serializable entityType, int groupId, int facetId);

	/**
	 * Returns index of {@link FacetEntityTypeIndex} where key is the {@link EntityReference#getType()} of the facet
	 * referred by the entity.
	 */
	@Nonnull
	Map<Serializable, FacetEntityTypeIndex> getFacetingEntities();

	/**
	 * Returns count of all indexed entity primary keys in this index.
	 */
	int getSize();

}
