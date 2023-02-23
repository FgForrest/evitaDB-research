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

package io.evitadb.api.query.order;

import io.evitadb.api.query.OrderConstraint;

import java.io.Serializable;

/**
 * This `referenceAttribute` container is ordering that sorts returned entities by reference attributes. Ordering is
 * specified by inner constraints. Price related orderings cannot be used here, because references don't posses of prices.
 *
 * Example:
 *
 * ```
 * referenceAttribute(
 *    'CATEGORY',
 *    ascending('categoryPriority')
 * )
 * ```
 *
 * or
 *
 * ```
 * referenceAttribute(
 *    'CATEGORY',
 *    ascending('categoryPriority'),
 *    descending('stockPriority')
 * )
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceAttribute extends AbstractOrderConstraintContainer {
	private static final long serialVersionUID = -8564699361608364992L;

	private ReferenceAttribute(Serializable[] arguments, OrderConstraint... children) {
		super(arguments, children);
	}

	public ReferenceAttribute(Serializable entityType, OrderConstraint... children) {
		super(entityType, children);
	}

	/**
	 * Returns entity type of the facet that should be used for applying for ordering according to children constraints.
	 * @return
	 */
	public Serializable getEntityType() {
		return getArguments()[0];
	}

	@Override
	public OrderConstraint getCopyWithNewChildren(OrderConstraint[] innerConstraints) {
		return new ReferenceAttribute(
				getEntityType(),
				innerConstraints
		);
	}

	@Override
	public OrderConstraint cloneWithArguments(Serializable[] newArguments) {
		return new ReferenceAttribute(newArguments, getConstraints());
	}

	@Override
	public boolean isNecessary() {
		return getConstraints().length >= 1;
	}
}
