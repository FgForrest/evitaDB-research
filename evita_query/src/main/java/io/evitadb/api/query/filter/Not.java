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
 * This `not` is container constraint that contains single inner constraint which output is negated. Behaves as
 * <a href="https://en.wikipedia.org/wiki/Negation">logical NOT</a>.
 *
 * Example:
 *
 * ```
 * not(
 *     primaryKey(1,2,3)
 * )
 * ```
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
public class Not extends AbstractFilterConstraintContainer {
	private static final long serialVersionUID = 7151549459608672988L;

	public Not(FilterConstraint children) {
		super(children);
	}

	@Override
	public boolean isNecessary() {
		return getConstraints().length > 0;
	}

	@Override
	public FilterConstraint getCopyWithNewChildren(FilterConstraint[] innerConstraints) {
		return new Not(innerConstraints.length == 0 ? null : innerConstraints[0]);
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		throw new UnsupportedOperationException("Not filtering constraint has no arguments!");
	}

}