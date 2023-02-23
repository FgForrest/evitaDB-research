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

package io.evitadb.api.io.predicate;

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataValue;
import io.evitadb.api.data.structure.SerializablePredicate;
import io.evitadb.api.io.EvitaRequestBase;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * This predicate allows to limit number of associated data visible to the client based on query constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class AssociatedDataValueSerializablePredicate implements SerializablePredicate<AssociatedDataValue> {
	public static final AssociatedDataValueSerializablePredicate DEFAULT_INSTANCE = new AssociatedDataValueSerializablePredicate(Collections.emptySet(), Collections.emptySet(), true);
	private static final long serialVersionUID = 85644932696677698L;
	@Nullable @Getter private final Set<Locale> languages;
	@Nonnull @Getter private final Set<String> associatedDataSet;
	@Getter private final boolean requiresEntityAssociatedData;

	public AssociatedDataValueSerializablePredicate() {
		this.languages = null;
		this.associatedDataSet = Collections.emptySet();
		this.requiresEntityAssociatedData = false;
	}

	public AssociatedDataValueSerializablePredicate(@Nonnull EvitaRequestBase evitaRequest) {
		this.languages = evitaRequest.getRequiredLanguages();
		this.associatedDataSet = evitaRequest.getEntityAssociatedDataSet();
		this.requiresEntityAssociatedData = evitaRequest.isRequiresEntityAssociatedData();
	}

	AssociatedDataValueSerializablePredicate(@Nullable Set<Locale> languages, @Nonnull Set<String> associatedDataSet, boolean requiresEntityAssociatedData) {
		this.languages = languages;
		this.associatedDataSet = associatedDataSet;
		this.requiresEntityAssociatedData = requiresEntityAssociatedData;
	}

	@Override
	public boolean test(@Nonnull AssociatedDataValue associatedDataValue) {
		if (requiresEntityAssociatedData) {
			final AssociatedDataKey key = associatedDataValue.getKey();
			return associatedDataValue.exists() &&
				(
					!key.isLocalized() ||
						(languages != null && (languages.isEmpty() || languages.contains(key.getLocale())))
				) &&
				(associatedDataSet.isEmpty() || associatedDataSet.contains(key.getAssociatedDataName()));
		} else {
			return false;
		}
	}

	public AssociatedDataValueSerializablePredicate createRicherCopyWith(@Nonnull EvitaRequestBase evitaRequest) {
		final Set<Locale> requiredLanguages = combineLanguages(evitaRequest);
		final Set<String> requiredAssociatedDataSet = combineAssociatedData(evitaRequest);
		if ((this.requiresEntityAssociatedData || this.requiresEntityAssociatedData == evitaRequest.isRequiresEntityAssociatedData()) &&
			Objects.equals(this.languages, requiredLanguages) &&
			Objects.equals(this.associatedDataSet, requiredAssociatedDataSet)) {
			return this;
		} else {
			return new AssociatedDataValueSerializablePredicate(
				requiredLanguages,
				requiredAssociatedDataSet,
				evitaRequest.isRequiresEntityAssociatedData() || this.requiresEntityAssociatedData
			);
		}
	}

	public boolean wasFetched(@Nonnull AssociatedDataKey associatedDataKey) {
		if (this.requiresEntityAssociatedData) {
			return this.associatedDataSet.contains(associatedDataKey.getAssociatedDataName()) &&
				(associatedDataKey.getLocale() == null || (this.languages != null && this.languages.contains(associatedDataKey.getLocale())));
		} else {
			return false;
		}
	}

	private Set<String> combineAssociatedData(@Nonnull EvitaRequestBase evitaRequest) {
		Set<String> requiredAssociatedDataSet;
		final Set<String> newlyRequiredAssociatedDataSet = evitaRequest.getEntityAssociatedDataSet();
		if (this.requiresEntityAssociatedData && evitaRequest.isRequiresEntityAssociatedData()) {
			if (this.associatedDataSet.isEmpty()) {
				requiredAssociatedDataSet = this.associatedDataSet;
			} else if (newlyRequiredAssociatedDataSet.isEmpty()) {
				requiredAssociatedDataSet = newlyRequiredAssociatedDataSet;
			} else {
				requiredAssociatedDataSet = new HashSet<>(this.associatedDataSet.size(), newlyRequiredAssociatedDataSet.size());
				requiredAssociatedDataSet.addAll(associatedDataSet);
				requiredAssociatedDataSet.addAll(newlyRequiredAssociatedDataSet);
			}
		} else if (this.requiresEntityAssociatedData) {
			requiredAssociatedDataSet = this.associatedDataSet;
		} else {
			requiredAssociatedDataSet = newlyRequiredAssociatedDataSet;
		}
		return requiredAssociatedDataSet;
	}

	@Nullable
	private Set<Locale> combineLanguages(@Nonnull EvitaRequestBase evitaRequest) {
		final Set<Locale> requiredLanguages;
		final Set<Locale> newlyRequiredLanguages = evitaRequest.getRequiredLanguages();
		if (languages == null) {
			requiredLanguages = newlyRequiredLanguages;
		} else if (newlyRequiredLanguages != null) {
			requiredLanguages = new HashSet<>(this.languages.size() + newlyRequiredLanguages.size());
			requiredLanguages.addAll(this.languages);
			requiredLanguages.addAll(newlyRequiredLanguages);
		} else {
			requiredLanguages = languages;
		}
		return requiredLanguages;
	}
}
