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

/**
 * This `require` is container for listing all additional requirements for th equery. It is mandatory container when
 * any requirement constraint is to be used.
 *
 * Example:
 *
 * ```
 * require(
 *     page(1, 2),
 *     entityBody()
 * )
 * ```
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
public class Require extends AbstractRequireConstraintContainer {
	private static final long serialVersionUID = 6115101893250263038L;

	public Require(RequireConstraint... children) {
		super(children);
	}

	@Override
	public RequireConstraint getCopyWithNewChildren(RequireConstraint[] innerConstraints) {
		return new Require(innerConstraints);
	}

	@Override
	public boolean isNecessary() {
		return isApplicable();
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		throw new UnsupportedOperationException("Require require constraint has no arguments!");
	}
}
