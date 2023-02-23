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
 * If you use `excludingRoot` sub-constraint in `withinHierarchy` parent, response will contain only children of the
 * entity specified in `withinHierarchy` or entities related to those children entities - if the `withinHierarchy` targets
 * different entity types.
 *
 * Let's have following category tree:
 *
 * - TV (1)
 *     - Crt (2)
 *     - LCD (3)
 *
 * These categories are related by following products:
 *
 * - TV (1):
 *     - Product Philips 32"
 *     - Product Samsung 24"
 *     - Crt (2):
 *         - Product Ilyiama 15"
 *         - Product Panasonic 17"
 *     - LCD (3):
 *         - Product BenQ 32"
 *         - Product LG 28"
 *
 * When using this query:
 *
 * ```
 * query(
 *    entities('PRODUCT'),
 *    filterBy(
 *       withinHierarchy('CATEGORY', 1)
 *    )
 * )
 * ```
 *
 * All products will be returned.
 * When this query is used:
 *
 * ```
 * query(
 *    entities('PRODUCT'),
 *    filterBy(
 *       withinHierarchy('CATEGORY', 1, excludingRoot())
 *    )
 * )
 * ```
 *
 * Only products related to sub-categories of the TV category will be returned - i.e.: Ilyiama 15", Panasonic 17" and
 * BenQ 32", LG 28". The products related directly to TV category will not be returned.
 *
 * As you can see {@link ExcludingRoot} and {@link DirectRelation} are mutually exclusive.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExcludingRoot extends AbstractFilterConstraintLeaf implements HierarchySpecificationFilterConstraint {
	private static final long serialVersionUID = 3965082821350063527L;

	private ExcludingRoot(Serializable... arguments) {
		super(arguments);
	}

	public ExcludingRoot() {
		super();
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new ExcludingRoot(newArguments);
	}
}