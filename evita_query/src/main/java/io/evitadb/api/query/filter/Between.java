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
 * This `between` is constraint that compares value of the attribute with name passed in first argument with the value passed
 * in the second argument and value passed in third argument. First argument must be {@link String},
 * second and third argument may be any of {@link Comparable} type.
 * Type of the attribute value and second argument must be convertible one to another otherwise `between` function
 * returns false.
 *
 * Function returns true if value in a filterable attribute of such a name is greater than or equal to value in second argument
 * and lesser than or equal to value in third argument.
 *
 * Example:
 *
 * ```
 * between('age', 20, 25)
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `between` returns true if *any of attribute* values
 * is between the passed interval the value in the constraint. If we have the attribute `amount` with value `[1, 9]` all
 * these constraints will match:
 *
 * ```
 * between('amount', 0, 50)
 * between('amount', 0, 5)
 * between('amount', 8, 10)
 * ```
 *
 * If attribute is of `Range` type `between` constraint behaves like overlap - it returns true if examined range and
 * any of the attribute ranges (see previous paragraph about array types) share anything in common. All of following
 * constraints return true when we have the attribute `validity` with following `NumberRange` values: `[[2,5],[8,10]]`:
 *
 * ```
 * between(`validity`, 0, 3)
 * between(`validity`, 0, 100)
 * between(`validity`, 9, 10)
 * ```
 *
 * ... but these constraints will return false:
 *
 * ```
 * between(`validity`, 11, 15)
 * between(`validity`, 0, 1)
 * between(`validity`, 6, 7)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Between extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = 4684374310853295964L;

	private Between(Serializable... arguments) {
		super(arguments);
	}

	public <T extends Serializable & Comparable<?>> Between(String attributeName, T from, T to) {
		super(attributeName, from, to);
	}

	/**
	 * Returns lower bound of attribute value (inclusive).
	 * @param <T>
	 * @return
	 */
	public <T extends Serializable & Comparable<?>> T getFrom() {
		//noinspection unchecked
		return (T) getArguments()[1];
	}

	/**
	 * Returns upper bound of attribute value (inclusive).
	 * @param <T>
	 * @return
	 */
	public <T extends Serializable & Comparable<?>> T getTo() {
		//noinspection unchecked
		return (T) getArguments()[2];
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 3;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Between(newArguments);
	}
}
