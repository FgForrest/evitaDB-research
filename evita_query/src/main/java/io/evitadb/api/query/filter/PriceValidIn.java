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

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * This `priceValidIn` is constraint accepts single {@link ZonedDateTime}
 * argument that represents the moment in time for which entity price must be valid.
 * If argument is not passed - current date and time (now) is used.
 *
 * Function returns true if entity has at least one price which validity start (valid from) is lesser or equal to passed
 * date and time and validity end (valid to) is greater or equal to passed date and time. This function is also affected by
 * {@link PriceInCurrency} and {@link PriceInPriceLists} functions that limits the examined prices as well.
 * When this constraint is used in the query returned entities will contain only prices which validity settings match
 * specified date and time.
 *
 * Only single `priceValidIn` constraint can be used in the query. Validity of the prices will not be taken into an account
 * when `priceValidIn` is not used in the query.
 *
 * Example:
 *
 * ```
 * priceValidIn(2020-07-30T20:37:50+00:00)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceValidIn extends AbstractFilterConstraintLeaf implements IndexUsingConstraint, FilterConstraint {
	private static final long serialVersionUID = -3041416427283645494L;

	private PriceValidIn(Serializable... arguments) {
		super(arguments);
	}

	public PriceValidIn() {
		super();
	}

	public PriceValidIn(ZonedDateTime theMoment) {
		super(theMoment);
	}

	/**
	 * Returns {@link ZonedDateTime} that should be verified whether is within the range (inclusive) of price validity.
	 */
	@Nullable
	public ZonedDateTime getTheMoment() {
		return getArguments().length == 0 ? null : (ZonedDateTime) getArguments()[0];
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new PriceValidIn(newArguments);
	}
}
