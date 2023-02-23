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

package io.evitadb.query.algebra.facet;

import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.require.FacetStatisticsDepth;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.Formula;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Interface marks all {@link Formula} that resolve facet filtering. This interface allows locating appropriate formulas
 * in the tree when {@link FacetStatisticsDepth#IMPACT} is requested to be computed and original requirements needs to
 * be altered in order to compute alternative searches.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface FacetGroupFormula extends Formula {

	/**
	 * Returns {@link Facet#getType()} of the facet that is targeted by this formula.
	 * This information is crucial for correct {@link io.evitadb.api.io.extraResult.FacetSummary} computation.
	 */
	Serializable getFacetType();

	/**
	 * Returns {@link GroupEntityReference#getPrimaryKey()} shared among all facets in {@link #getFacetIds()}.
	 */
	@Nullable
	Integer getFacetGroupId();

	/**
	 * Returns array of requested facet ids from {@link Facet#getFacetIds()} filtering constraint.
	 * This information is crucial for correct {@link io.evitadb.api.io.extraResult.FacetSummary} computation.
	 */
	int[] getFacetIds();

	/**
	 * Returns clone of the formula adding new facet to the formula along with `entityIds` that match this facet id.
	 */
	FacetGroupFormula getCloneWithFacet(int facetId, @Nonnull Bitmap... entityIds);

}
