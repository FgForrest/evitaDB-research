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
import io.evitadb.api.utils.Assert;

import java.io.Serializable;

/**
 * This `prices` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
 * this requirement is used result contains [entity prices](entity_model.md).
 *
 * This requirement implicitly triggers {@link EntityBody} requirement because prices cannot be returned without entity.
 * When price constraints are used returned prices are filtered according to them by default. This behaviour might be
 * changed however.
 *
 * Accepts single {@link PriceFetchMode} parameter. When {@link PriceFetchMode#ALL} all prices of the entity are returned
 * regardless of the input query constraints otherwise prices are filtered by those constraints. Default is {@link PriceFetchMode#RESPECTING_FILTER}.
 *
 * Example:
 *
 * ```
 * prices() // defaults to respecting filter
 * prices(RESPECTING_FILTER)
 * prices(ALL)
 * prices(NONE)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Prices extends AbstractRequireConstraintLeaf implements EntityContentRequire {
	private static final long serialVersionUID = -8521118631539528009L;

	private Prices(Serializable... arguments) {
		super(arguments);
	}

	public Prices() {
		super(PriceFetchMode.RESPECTING_FILTER);
	}

	public Prices(PriceFetchMode fetchMode) {
		super(fetchMode);
	}

	/**
	 * Returns fetch mode for prices. Controls whether whether only those that comply with the filter constraint
	 * should be returned along with entity or all prices of the entity.
	 */
	public PriceFetchMode getFetchMode() {
		final Serializable argument = getArguments()[0];
		return argument instanceof PriceFetchMode ? (PriceFetchMode) argument : PriceFetchMode.valueOf(argument.toString());
	}

	@Override
	public boolean isEqualToOrWider(EntityContentRequire require) {
		return require instanceof Prices && ((Prices) require).getFetchMode().ordinal() <= getFetchMode().ordinal();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends EntityContentRequire> T combineWith(T anotherRequirement) {
		Assert.isTrue(anotherRequirement instanceof Prices, "Only Prices requirement can be combined with this one!");
		if (((Prices) anotherRequirement).getFetchMode().ordinal() >= getFetchMode().ordinal()) {
			return anotherRequirement;
		} else {
			return (T) this;
		}
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Prices(newArguments);
	}
}
