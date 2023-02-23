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
 * This `equals` is constraint that compares value of the attribute with name passed in first argument with the value passed
 * in the second argument. First argument must be {@link String}, second argument may be any of {@link Comparable} type.
 * Type of the attribute value and second argument must be convertible one to another otherwise `equals` function
 * returns false.
 *
 * Function returns true if both values are equal.
 *
 * Example:
 *
 * ```
 * equals('code', 'abc')
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `equals` returns true if *any of attribute* values
 * equals the value in the constraint. If we have the attribute `code` with value `['A','B','C']` all these constraints will
 * match:
 *
 * ```
 * equals('code','A')
 * equals('code','B')
 * equals('code','C')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Equals extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = 3928023999412612529L;

	private Equals(Serializable... arguments) {
		super(arguments);
	}

	public <T extends Serializable & Comparable<?>> Equals(String attributeName, T attributeValue) {
		super(attributeName, attributeValue);
	}

	/**
	 * Returns value that must be equals to attribute value.
	 * @param <T>
	 * @return
	 */
	public <T extends Serializable & Comparable<?>> T getAttributeValue() {
		//noinspection unchecked
		return (T) getArguments()[1];
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 2;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Equals(newArguments);
	}

}
