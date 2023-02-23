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
 * This `isFalse` is constraint that compares value of the attribute with name passed in first argument with boolean FALSE value.
 * First argument must be {@link String}.
 * Type of the attribute value must be convertible to Boolean otherwise `isFalse` function returns false.
 *
 * Function returns true if attribute value equals to {@link Boolean#FALSE}.
 *
 * Example:
 *
 * ```
 * isFalse('visible')
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `isTrue` returns true if *any of attribute* values
 * starts with the value in the constraint. If we have the attribute `dead` with value `['true','false']` both `isTrue`
 * and `isFalse` match. Hence, we can call this attribute Schrödinger one - both dead and undead. In other words - this
 * constraint will work even if it has not much of a sense.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class IsFalse extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = 7669831492521056081L;

	private IsFalse(Serializable... arguments) {
		super(arguments);
	}

	public IsFalse(String attributeName) {
		super(attributeName);
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 1;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new IsFalse(newArguments);
	}

}
