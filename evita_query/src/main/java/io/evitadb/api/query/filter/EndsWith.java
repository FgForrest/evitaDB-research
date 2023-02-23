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
 * This `endsWith` is constraint that searches value of the attribute with name passed in first argument for presence of the
 * {@link String} value passed in the second argument.
 *
 * Function returns true if attribute value contains secondary argument (using reverse lookup from last position).
 * InSet other words attribute value ends with string passed in second argument. Function is case sensitive and comparison
 * is executed using `UTF-8` encoding (Java native).
 *
 * Example:
 *
 * ```
 * endsWith('code', 'ida')
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `endsWith` returns true if *any of attribute* values
 * ends with the value in the constraint. If we have the attribute `code` with value `['cat','mouse','dog']` all these
 * constraints will match:
 *
 * ```
 * contains('code','at')
 * contains('code','og')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EndsWith extends AbstractAttributeFilterConstraintLeaf {
	private static final long serialVersionUID = -8551542903236177197L;

	private EndsWith(Serializable... arguments) {
		super(arguments);
	}

	public EndsWith(String attributeName, String textToSearch) {
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
		return new EndsWith(newArguments);
	}
}
