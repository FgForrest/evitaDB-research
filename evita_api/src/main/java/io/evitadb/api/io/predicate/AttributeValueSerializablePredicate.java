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

import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.structure.SerializablePredicate;
import io.evitadb.api.io.EvitaRequestBase;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * This predicate allows limiting number of attributes visible to the client based on query constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class AttributeValueSerializablePredicate implements SerializablePredicate<AttributeValue> {
	public static final AttributeValueSerializablePredicate DEFAULT_INSTANCE = new AttributeValueSerializablePredicate(Collections.emptySet(), true);
	private static final long serialVersionUID = 2628834850476260927L;
	@Nullable @Getter private final Set<Locale> languages;
	@Getter private final boolean requiresEntityAttributes;

	public AttributeValueSerializablePredicate() {
		this.requiresEntityAttributes = false;
		this.languages = null;
	}

	public AttributeValueSerializablePredicate(@Nonnull EvitaRequestBase evitaRequest) {
		this.languages = evitaRequest.getRequiredLanguages();
		this.requiresEntityAttributes = evitaRequest.isRequiresEntityAttributes();
	}

	AttributeValueSerializablePredicate(@Nullable Set<Locale> languages, boolean requiresEntityAttributes) {
		this.languages = languages;
		this.requiresEntityAttributes = requiresEntityAttributes;
	}

	@Override
	public boolean test(@Nonnull AttributeValue attributeValue) {
		if (requiresEntityAttributes) {
			return attributeValue.exists() &&
				(
					!attributeValue.getKey().isLocalized() ||
						(this.languages != null && (this.languages.isEmpty() || this.languages.contains(attributeValue.getKey().getLocale())))
				);
		} else {
			return false;
		}
	}

	public AttributeValueSerializablePredicate createRicherCopyWith(@Nonnull EvitaRequestBase evitaRequest) {
		final Set<Locale> requiredLanguages = combineLanguages(evitaRequest);
		if (Objects.equals(this.languages, requiredLanguages) && (this.requiresEntityAttributes || this.requiresEntityAttributes == evitaRequest.isRequiresEntityAttributes())) {
			return this;
		} else {
			return new AttributeValueSerializablePredicate(
				requiredLanguages,
				evitaRequest.isRequiresEntityAttributes() || this.requiresEntityAttributes
			);
		}
	}

	@Nullable
	private Set<Locale> combineLanguages(@Nonnull EvitaRequestBase evitaRequest) {
		final Set<Locale> requiredLanguages;
		final Set<Locale> newlyRequiredLanguages = evitaRequest.getRequiredLanguages();
		if (this.languages == null) {
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
