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

import java.io.Serializable;
import java.util.Arrays;

/**
 * This `facetGroupsNegation` requirement allows specifying facet relation inside facet groups of certain primary ids. Negative facet
 * groups results in omitting all entities that have requested facet in query result. First mandatory argument specifies
 * entity type of the facet group, secondary argument allows to define one more facet group ids that should be considered
 * negative.
 *
 * Example:
 *
 * ```
 * facetGroupsNegation('parameterType', 1, 8, 15)
 * ```
 *
 * This statement means, that facets in 'parameterType' groups `1`, `8`, `15` will be joined with boolean AND NOT relation
 * when selected.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetGroupsNegation extends AbstractRequireConstraintLeaf {
	private static final long serialVersionUID = 3993873252481237893L;

	private FacetGroupsNegation(Serializable... arguments) {
		super(arguments);
	}

	public FacetGroupsNegation(Serializable entityType, Integer... facetGroups) {
		super(concat(entityType, facetGroups));
	}

	/**
	 * Returns name of the entity this hierarchy constraint relates to.
	 */
	public Serializable getEntityType() {
		return getArguments()[0];
	}

	/**
	 * Returns ids of facet groups.
	 */
	public int[] getFacetGroups() {
		return Arrays.stream(getArguments())
			.skip(1)
			.mapToInt(Integer.class::cast)
			.toArray();
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length >= 1;
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new FacetGroupsNegation(newArguments);
	}
}
