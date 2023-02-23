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
 * This `and` is container constraint that contains two or more inner constraints which output is combined by
 * <a href="https://en.wikipedia.org/wiki/Logical_conjunction">logical AND</a>.
 *
 * Example:
 *
 * ```
 * and(
 *     isTrue('visible'),
 *     validInTime(2020-07-30T07:28:13+00:00)
 * )
 * ```
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
public class And extends AbstractFilterConstraintContainer {
	private static final long serialVersionUID = -3383976355275556890L;

	public And(FilterConstraint... children) {
		super(children);
	}

	@Override
	public FilterConstraint getCopyWithNewChildren(FilterConstraint[] innerConstraints) {
		return new And(innerConstraints);
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		throw new UnsupportedOperationException("And filtering constraint has no arguments!");
	}

}
