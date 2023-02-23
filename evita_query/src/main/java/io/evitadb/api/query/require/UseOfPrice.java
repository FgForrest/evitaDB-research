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
 * This `useOfPrice` require constraint can be used to control the form of prices that will be used for computation in
 * {@link io.evitadb.api.query.filter.PriceBetween} filtering, and {@link io.evitadb.api.query.order.PriceAscending},
 * {@link io.evitadb.api.query.order.PriceDescending} ordering. Also {@link PriceHistogram} is sensitive to this setting.
 *
 * By default, end customer form of price (e.g. price with VAT) is used in all above-mentioned constraints. This could
 * be changed by using this requirement constraint. It has single argument that can have one of the following values:
 *
 * - WITH_VAT
 * - WITHOUT_VAT
 *
 * Example:
 *
 * ```
 * useOfPrice(WITH_VAT)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UseOfPrice extends AbstractRequireConstraintLeaf {
	private static final long serialVersionUID = -7156758352138266166L;

	public UseOfPrice(QueryPriceMode queryPriceMode) {
		super(queryPriceMode);
	}

	/**
	 * Returns number of the items that should be omitted in the result.
	 */
	public QueryPriceMode getQueryPriceMode() {
		return (QueryPriceMode) getArguments()[0];
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 1;
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new UseOfPrice(getQueryPriceMode());
	}

}
