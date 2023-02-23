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
 * This `isNotNull` is constraint that checks existence of value of the attribute with name passed in first argument.
 * First argument must be {@link String}.
 * Attribute must exist in order `isNotNull` function returns true.
 *
 * Function returns true if attribute exists.
 *
 * Example:
 *
 * ```
 * isNotNull('visible')
 * ```
 *
 * Function supports attribute arrays in the same way as plain values.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class IsNotNull extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = -9025138757534128925L;

	private IsNotNull(Serializable... arguments) {
		super(arguments);
	}

	public IsNotNull(String attributeName) {
		super(attributeName);
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 1;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new IsNotNull(newArguments);
	}

}
