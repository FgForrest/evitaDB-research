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
 * This `orderBy` is container for ordering. It is mandatory container when any ordering is to be used. Ordering
 * process is as follows:
 *
 * - first ordering evaluated, entities missing requested attribute value are excluded to intermediate bucket
 * - next ordering is evaluated using entities present in an intermediate bucket, entities missing requested attribute
 *   are excluded to new intermediate bucket
 * - second step is repeated until all orderings are processed
 * - content of the last intermediate bucket is appended to the result ordered by the primary key in ascending order
 *
 * Entities with same (equal) values must not be subject to secondary ordering rules and may be sorted randomly within
 * the scope of entities with the same value (this is subject to change, currently this behaviour differs from the one
 * used by relational databases - but might be more performant).
 *
 * Example:
 *
 * ```
 * orderBy(
 *     ascending('code'),
 *     ascending('create'),
 *     priceDescending()
 * )
 * ```
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
public class OrderBy extends AbstractOrderConstraintContainer {
	private static final long serialVersionUID = 6352220342769661652L;

	public OrderBy(OrderConstraint... children) {
		super(children);
	}

	@Override
	public OrderConstraint getCopyWithNewChildren(OrderConstraint[] innerConstraints) {
		return new OrderBy(innerConstraints);
	}

	@Override
	public boolean isNecessary() {
		return isApplicable();
	}

	@Override
	public OrderConstraint cloneWithArguments(Serializable[] newArguments) {
		throw new UnsupportedOperationException("OrderBy ordering constraint has no arguments!");
	}

}
