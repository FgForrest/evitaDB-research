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

package io.evitadb.api.data.structure;

import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.io.predicate.AttributeValueSerializablePredicate;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.ReferenceSchema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Reference decorator class envelopes any {@link Reference} and allows to filter out properties that are not passing predicate
 * conditions. This allows us to reuse rich {@link Reference} / {@link Entity} objects from the cache even if clients requests thinner ones.
 * For example if we have full-blown entity in our cache and client asks for entity in English language, we can use
 * entity decorator to hide all attributes that refers to other languages than English one.
 *
 * We try to keep Evita DB responses consistent and provide only those type of data that were really requested in the query
 * and avoid inconsistent situations that richer data are returned just because the entity was found in cache in a form
 * that more than fulfills the request.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class ReferenceDecorator implements ReferenceContract {
	private static final long serialVersionUID = 1098992030664849469L;

	/**
	 * Contains reference to the (possibly richer than requested) entity object.
	 */
	@Getter private final ReferenceContract delegate;
	/**
	 * This predicate filters out attributes that were not fetched in query.
	 */
	@Getter private final AttributeValueSerializablePredicate attributePredicate;
	/**
	 * Optimization that ensures that expensive attributes filtering using predicates happens only once.
	 */
	private List<AttributeValue> filteredAttributes;
	/**
	 * Optimization that ensures that expensive attribute locale resolving happens only once.
	 */
	private Set<Locale> attributeLocales;

	@Override
	public boolean isDropped() {
		return delegate.isDropped();
	}

	@Nonnull
	@Override
	public EntityReference getReferencedEntity() {
		return delegate.getReferencedEntity();
	}

	@Nullable
	@Override
	public GroupEntityReference getGroup() {
		return ofNullable(delegate.getGroup())
			.filter(Droppable::exists)
			.orElse(null);
	}

	@Nullable
	@Override
	public ReferenceSchema getReferenceSchema() {
		return delegate.getReferenceSchema();
	}

	@Override
	public int getVersion() {
		return delegate.getVersion();
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName) {
		//noinspection unchecked
		return ofNullable(getAttributeValue(attributeName))
			.map(it -> (T) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName) {
		//noinspection unchecked
		return ofNullable(getAttributeValue(attributeName))
			.map(it -> (T[]) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName) {
		final Set<Locale> requestedLocales = attributePredicate.getLanguages();
		if (requestedLocales == null) {
			return delegate.getAttributeValue(attributeName);
		} else {
			AttributeValue result = delegate.getAttributeValue(attributeName);
			if (result == null) {
				Locale resultLocale = null;
				final Set<Locale> examinedLocales = requestedLocales.isEmpty() ? delegate.getAttributeLocales() : requestedLocales;
				for (Locale requestedLocale : examinedLocales) {
					final AttributeValue resultAdept = delegate.getAttributeValue(attributeName, requestedLocale);
					if (result == null) {
						result = resultAdept;
						resultLocale = requestedLocale;
					} else {
						throw new IllegalArgumentException("Attribute `" + attributeName + "` has multiple values for different locales: `" + resultLocale + "` and `" + requestedLocale + "`!");
					}
				}
			}
			return ofNullable(result)
				.filter(attributePredicate)
				.orElse(null);
		}
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return ofNullable(delegate.getAttributeValue(attributeName, locale))
			.filter(attributePredicate)
			.map(it -> (T) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return ofNullable(delegate.getAttributeValue(attributeName, locale))
			.filter(attributePredicate)
			.map(it -> (T[]) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName, @Nonnull Locale locale) {
		return ofNullable(delegate.getAttributeValue(attributeName, locale))
			.filter(attributePredicate)
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeSchema getAttributeSchema(@Nonnull String attributeName) {
		return delegate.getAttributeSchema(attributeName);
	}

	@Nonnull
	@Override
	public Set<String> getAttributeNames() {
		return getAttributeValues()
			.stream()
			.map(it -> it.getKey().getAttributeName())
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Set<AttributeKey> getAttributeKeys() {
		return getAttributeValues()
			.stream()
			.map(AttributeValue::getKey)
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Collection<AttributeValue> getAttributeValues() {
		if (filteredAttributes == null) {
			filteredAttributes = delegate.getAttributeValues()
				.stream()
				.filter(attributePredicate)
				.collect(Collectors.toList());
		}
		return filteredAttributes;
	}

	@Nonnull
	@Override
	public Collection<AttributeValue> getAttributeValues(@Nonnull String attributeName) {
		return getAttributeValues()
			.stream()
			.filter(it -> attributeName.equals(it.getKey().getAttributeName()))
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Set<Locale> getAttributeLocales() {
		if (this.attributeLocales == null) {
			this.attributeLocales = getAttributeValues()
				.stream()
				.map(AttributeValue::getKey)
				.map(AttributeKey::getLocale)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		}
		return this.attributeLocales;
	}


}
