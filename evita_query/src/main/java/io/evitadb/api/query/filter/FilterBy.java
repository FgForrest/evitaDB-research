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

package io.evitadb.api.query.filter;

import io.evitadb.api.query.FilterConstraint;

import java.io.Serializable;

/**
 * This `filterBy` is container for filtering constraints. It is mandatory container when any filtering is to be used.
 * This container allows only one children container with the filtering condition.
 *
 * Example:
 *
 * ```
 * filterBy(
 *     and(
 *        isNotNull('code'),
 *        or(
 *           equals('code', 'ABCD'),
 *           startsWith('title', 'Knife')
 *        )
 *     )
 * )
 * ```
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
public class FilterBy extends AbstractFilterConstraintContainer {
	private static final long serialVersionUID = -2294600717092701351L;

	FilterBy() {
		super();
	}

	public FilterBy(FilterConstraint children) {
		super(children);
	}

	@Override
	public FilterConstraint getCopyWithNewChildren(FilterConstraint[] innerConstraints) {
		return innerConstraints.length > 0 ? new FilterBy(innerConstraints[0]) : new FilterBy();
	}

	@Override
	public boolean isNecessary() {
		return isApplicable();
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		throw new UnsupportedOperationException("FilterBy filtering constraint has no arguments!");
	}

}
