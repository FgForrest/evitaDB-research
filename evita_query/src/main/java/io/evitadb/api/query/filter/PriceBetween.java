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
import io.evitadb.api.query.require.UseOfPrice;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This `priceBetween` constraint accepts two {@link BigDecimal} arguments that represents lower and higher price
 * bounds (inclusive).
 *
 * Function returns true if entity has sellable price in most prioritized price list according to {@link PriceInPriceLists}
 * constraint greater than or equal to passed lower bound and lesser than or equal to passed higher bound. This function
 * is also affected by other price related constraints such as {@link PriceInCurrency} functions that limits the examined
 * prices as well.
 *
 * Most prioritized price term relates to [price computation algorithm](price_computation.md) described in special article.
 *
 * By default, price with VAT is used for filtering, you can change this by using {@link UseOfPrice} require constraint.
 * Non-sellable prices doesn't participate in the filtering at all.
 *
 * Only single `priceBetween` constraint can be used in the query.
 *
 * Example:
 *
 * ```
 * priceBetween(150.25, 220.0)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceBetween extends AbstractFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = -4134467514999931163L;

	private PriceBetween(Serializable... arguments) {
		super(arguments);
	}

	public PriceBetween(BigDecimal from, BigDecimal to) {
		super(from, to);
	}

	/**
	 * Returns lower bound of price (inclusive).
	 * @return
	 */
	public BigDecimal getFrom() {
		return (BigDecimal) getArguments()[0];
	}

	/**
	 * Returns upper bound of price (inclusive).
	 * @return
	 */
	public BigDecimal getTo() {
		return (BigDecimal) getArguments()[1];
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 2;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new PriceBetween(newArguments);
	}
}
