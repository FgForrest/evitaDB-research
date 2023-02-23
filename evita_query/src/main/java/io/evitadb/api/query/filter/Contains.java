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
 * This `contains` is constraint that searches value of the attribute with name passed in first argument for presence of the
 * {@link String} value passed in the second argument.
 *
 * Function returns true if attribute value contains secondary argument (starting with any position). Function is case
 * sensitive and comparison is executed using `UTF-8` encoding (Java native).
 *
 * Example:
 *
 * ```
 * contains('code', 'eve')
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `contains` returns true if *any of attribute* values
 * contains the value in the constraint. If we have the attribute `code` with value `['cat','mouse','dog']` all these constraints will
 * match:
 *
 * ```
 * contains('code','mou')
 * contains('code','o')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Contains extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = 5307621598413172503L;

	private Contains(Serializable... arguments) {
		super(arguments);
	}

	public Contains(String attributeName, String textToSearch) {
		super(attributeName, textToSearch);
	}

	/**
	 * Returns part of attribute value that needs to be looked up for.
	 * @return
	 */
	public String getTextToSearch() {
		return (String) getArguments()[1];
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 2;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Contains(newArguments);
	}

}
