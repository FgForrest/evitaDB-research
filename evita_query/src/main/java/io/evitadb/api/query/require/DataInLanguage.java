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
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * This `dataInLanguage` constraint is require constraint that accepts zero or more {@link Locale} arguments. When this
 * require constraint is used, result contains [entity attributes and associated data](../model/entity_model.md)
 * localized in required languages as well as global ones. If constraint contains no argument, global data and data
 * localized to all languages are returned. If constraint is not present in the query, only global attributes and
 * associated data are returned.
 *
 * **Note:** if {@link io.evitadb.api.query.filter.Language}is used in the filter part of the query and `dataInLanguage`
 * require constraint is missing, the system implicitly uses `dataInLanguage` matching the language in filter constraint.
 *
 * Only single `dataInLanguage` constraint can be used in the query.
 *
 * Example that fetches only global and `en-US` localized attributes and associated data (considering there are multiple
 * language localizations):
 *
 * ```
 * dataInLanguage('en-US')
 * ```
 *
 * Example that fetches all available global and localized data:
 *
 * ```
 * dataInLanguage()
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DataInLanguage extends AbstractRequireConstraintLeaf implements EntityContentRequire {
	private static final long serialVersionUID = 4716406488516855299L;

	private DataInLanguage(Serializable... arguments) {
		super(arguments);
	}

	public DataInLanguage(Locale... locale) {
		super(locale);
	}

	@Override
	public boolean isEqualToOrWider(EntityContentRequire require) {
		final Locale[] languages = getLanguages();
		return require instanceof DataInLanguage && (languages.length == 0 || ArrayUtils.contains(languages, ((DataInLanguage) require).getLanguages()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends EntityContentRequire> T combineWith(T anotherRequirement) {
		Assert.isTrue(anotherRequirement instanceof DataInLanguage, "Only DataInLanguage requirement can be combined with this one!");
		if (isAllRequested()) {
			return (T) this;
		} else if (((DataInLanguage) anotherRequirement).isAllRequested()) {
			return anotherRequirement;
		} else {
			return (T) new DataInLanguage(
				Stream.concat(
						Arrays.stream(getArguments()).map(Locale.class::cast),
						Arrays.stream(anotherRequirement.getArguments()).map(Locale.class::cast)
					)
					.distinct()
					.toArray(Locale[]::new)
			);
		}
	}

	/**
	 * Returns zero or more locales that should be used for retrieving localized data. Is no locale is returned all
	 * available localized data are expected to be returned.
	 */
	public Locale[] getLanguages() {
		return Arrays.stream(getArguments()).map(Locale.class::cast).toArray(Locale[]::new);
	}

	/**
	 * Returns TRUE if all available languages were requested to load.
	 */
	public boolean isAllRequested() {
		return ArrayUtils.isEmpty(getArguments());
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new DataInLanguage(newArguments);
	}
}
