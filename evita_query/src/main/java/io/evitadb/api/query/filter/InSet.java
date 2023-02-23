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
import java.util.Arrays;

/**
 * This `inSet` is constraint that compares value of the attribute with name passed in first argument with all the values passed
 * in the second, third and additional arguments. First argument must be {@link String},
 * additional arguments may be any of {@link Comparable} type.
 * Type of the attribute value and additional arguments must be convertible one to another otherwise `in` function
 * skips value comparison and ultimately returns false.
 *
 * Function returns true if attribute value is equal to at least one of additional values.
 *
 * Example:
 *
 * ```
 * inSet('level', 1, 2, 3)
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `inSet` returns true if *any of attribute* values
 * equals the value in the constraint. If we have the attribute `code` with value `['A','B','C']` all these constraints will
 * match:
 *
 * ```
 * inSet('code','A','D')
 * inSet('code','A', 'B')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InSet extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = 500395477991778874L;

	private InSet(Serializable... arguments) {
		super(arguments);
	}

	public <T extends Serializable & Comparable<?>> InSet(String attributeName, T... set) {
		super(concat(attributeName, set));
	}

	/**
	 * Returns set of {@link Comparable} values that attribute value must be part of.
	 * @return
	 */
	public Comparable<?>[] getSet() {
		//noinspection SuspiciousToArrayCall
		return Arrays.stream(getArguments())
				.skip(1)
				.toArray(Comparable<?>[]::new);
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length >= 2;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new InSet(newArguments);
	}
}
