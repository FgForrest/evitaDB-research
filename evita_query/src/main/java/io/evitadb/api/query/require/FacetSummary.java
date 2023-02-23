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

package io.evitadb.api.query.require;

import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.query.filter.UserFilter;

import java.io.Serializable;

/**
 * This `facetSummary` requirement usage triggers computing and adding an object to the result index. The object is
 * quite complex but allows rendering entire facet listing to e-commerce users. It contains information about all
 * facets present in current hierarchy view along with count of requested entities that have those facets assigned.
 *
 * Facet summary respects current query filtering constraints excluding the conditions inside {@link UserFilter}
 * container constraint.
 *
 * When this requirement is used an additional object {@link io.evitadb.api.io.extraResult.FacetSummary} is stored to result.
 *
 * Optionally accepts single enum argument:
 *
 * - COUNT: only counts of facets will be computed
 * - IMPACT: counts and selection impact for non-selected facets will be computed
 *
 * Example:
 *
 * ```
 * facetSummary()
 * facetSummary(COUNT) //same as previous row - default
 * facetSummary(IMPACT)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetSummary extends AbstractRequireConstraintLeaf implements ExtraResultRequireConstraint {
	private static final long serialVersionUID = 2377379601711709241L;

	private FacetSummary(Serializable... arguments) {
		super(arguments);
	}

	public FacetSummary() {
		super(FacetStatisticsDepth.COUNTS);
	}

	public FacetSummary(FacetStatisticsDepth statisticsDepth) {
		super(statisticsDepth);
	}

	/**
	 * The mode controls whether FacetSummary should contain only basic statistics about facets - e.g. count only,
	 * or whether the selection impact should be computed as well.
	 */
	public FacetStatisticsDepth getFacetStatisticsDepth() {
		return (FacetStatisticsDepth) getArguments()[0];
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new FacetSummary(newArguments);
	}
}
