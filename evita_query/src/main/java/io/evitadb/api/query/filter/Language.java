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
import java.util.Locale;

/**
 * This `language` is constraint accepts single {@link Locale} argument.
 *
 * Function returns true if entity has at least one localized attribute or associated data that  targets specified locale.
 *
 * If require part of the query doesn't contain {@link io.evitadb.api.query.require.DataInLanguage} requirement that
 * would specify the requested data localization, this filtering constraint implicitly sets requirement to the passed
 * language argument. In other words if entity has two localizations: `en-US` and `cs-CZ` and `language('cs-CZ')` is
 * used in query, returned entity would have only Czech localization of attributes and associated data fetched along
 * with it (and also attributes that are locale agnostic).
 *
 * If query contains no language constraint filtering logic is applied only on "global" (i.e. language agnostic)
 * attributes.
 *
 * Only single `language` constraint can be used in the query.
 *
 * Example:
 *
 * ```
 * language('en-US')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Language extends AbstractFilterConstraintLeaf implements IndexUsingConstraint, FilterConstraint {
	private static final long serialVersionUID = 4716406488516855299L;

	private Language(Serializable... arguments) {
		super(arguments);
	}

	public Language(Locale locale) {
		super(locale);
	}

	/**
	 * Returns locale that should be used for localized attribute search.
	 */
	public Locale getLanguage() {
		return (Locale) getArguments()[0];
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 1;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Language(newArguments);
	}
}
