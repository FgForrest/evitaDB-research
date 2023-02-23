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
 * This `page` constraint controls count of the entities in the query output. It allows specifying 2 arguments in following order:
 *
 * - **[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) pageNumber**: number of the page of
 * results that are expected to be returned, starts with 1, must be greater than zero (mandatory)
 * - **[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) pageSize**: number of entities on
 * a single page, must be greater than zero (mandatory)
 *
 * Example - return first page with 24 items:
 *
 * ```
 * page(1, 24)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Page extends AbstractRequireConstraintLeaf {
	private static final long serialVersionUID = 1300354074537839696L;

	private Page(Serializable... arguments) {
		super(arguments);
	}

	public Page(Integer pageNumber, Integer pageSize) {
		super(pageNumber, pageSize);
	}

	/**
	 * Returns page number to return in the response.
	 */
	public int getPageNumber() {
		return getArguments().length > 0 ? (Integer) getArguments()[0] : 1;
	}

	/**
	 * Returns page size to return in the response.
	 */
	public int getPageSize() {
		return getArguments().length > 1 ? (Integer) getArguments()[1] : 20;
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length > 1;
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Page(newArguments);
	}

}
