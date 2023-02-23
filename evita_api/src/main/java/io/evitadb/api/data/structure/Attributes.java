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

import io.evitadb.api.data.AttributesContract;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Entity (global / relative) attributes allows defining set of data that are fetched in bulk along with the entity body.
 * Attributes may be indexed for fast filtering ({@link AttributeSchema#isFilterable()}) or can be used to sort along
 * ({@link AttributeSchema#isSortable()}). Attributes are not automatically indexed in order not to waste precious
 * memory space for data that will never be used in search queries.
 *
 * Filtering in attributes is executed by using constraints like {@link io.evitadb.api.query.filter.And},
 * {@link io.evitadb.api.query.filter.Not}, {@link io.evitadb.api.query.filter.Equals}, {@link io.evitadb.api.query.filter.Contains}
 * and many others. Sorting can be achieved with {@link io.evitadb.api.query.order.Ascending},
 * {@link io.evitadb.api.query.order.Descending} or others.
 *
 * Attributes are not recommended for bigger data as they are all loaded at once when {@link io.evitadb.api.query.require.Attributes}
 * requirement is used. Large data that are occasionally used store in {@link AssociatedData}.
 *
 * Class is immutable on purpose - we want to support caching the entities in a shared cache and accessed by many threads.
 * For altering the contents use {@link InitialAttributesBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode
@Immutable
@ThreadSafe
public class Attributes implements AttributesContract {
	private static final long serialVersionUID = -1474840271286135157L;

	/**
	 * Definition of the entity schema.
	 */
	final EntitySchema entitySchema;
	/**
	 * Contains locale insensitive attribute values - simple key → value association map.
	 */
	final Map<AttributeKey, AttributeValue> attributeValues;
	/**
	 * Contains attribute definition that is built up along way with attribute adding or it may be directly filled
	 * in from the engine when entity with attributes is loaded from persistent storage.
	 */
	final Map<String, AttributeSchema> attributeTypes;
	/**
	 * Optimization that ensures that expensive attribute name resolving happens only once.
	 */
	private Set<String> attributeNames;
	/**
	 * Contains set of all locales that has at least one localized attribute.
	 */
	@Getter private final Set<Locale> attributeLocales;

	/**
	 * Constructor should be used only when attributes are loaded from persistent storage.
	 * Constructor is meant to be internal to the Evita engine.
	 *  @param attributeValues
	 * @param attributeTypes
	 */
	public Attributes(
		@Nonnull EntitySchema entitySchema,
		@Nonnull Collection<AttributeValue> attributeValues,
		@Nonnull Set<Locale> attributeLocales,
		@Nonnull Map<String, AttributeSchema> attributeTypes
	) {
		this.entitySchema = entitySchema;
		this.attributeValues = attributeValues
			.stream()
			.collect(
				Collectors.toMap(
					AttributesContract.AttributeValue::getKey,
					Function.identity()
				)
			);
		this.attributeTypes = attributeTypes;
		this.attributeLocales = attributeLocales;
	}

	public Attributes(
		@Nonnull EntitySchema entitySchema,
		@Nonnull Collection<AttributeValue> attributeValues,
		@Nonnull Set<Locale> attributeLocales
	) {
		this.entitySchema = entitySchema;
		this.attributeValues = attributeValues
			.stream()
			.collect(
				Collectors.toMap(
					AttributesContract.AttributeValue::getKey,
					Function.identity(),
					(attributeValue, attributeValue2) -> {
						throw new IllegalArgumentException("Duplicated attribute " + attributeValue.getKey() + "!");
					}
				)
			);
		this.attributeTypes = attributeValues
			.stream()
			.map(it -> it.getKey().getAttributeName())
			.distinct()
			.map(entitySchema::getAttribute)
			.filter(Objects::nonNull)
			.collect(
				Collectors.toMap(
					AttributeSchema::getName,
					Function.identity()
				)
			);
		this.attributeLocales = attributeLocales;
	}

	public Attributes(@Nonnull EntitySchema entitySchema) {
		this.entitySchema = entitySchema;
		this.attributeValues = Collections.emptyMap();
		this.attributeTypes = entitySchema.getAttributes();
		this.attributeLocales = Collections.emptySet();
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName) {
		//noinspection unchecked
		return (T) ofNullable(attributeValues.get(new AttributeKey(attributeName)))
				.map(AttributesContract.AttributeValue::getValue)
				.orElse(null);
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName) {
		//noinspection unchecked
		return (T[]) ofNullable(attributeValues.get(new AttributeKey(attributeName)))
			.map(AttributesContract.AttributeValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName) {
		return attributeValues.get(new AttributeKey(attributeName));
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T) ofNullable(attributeValues.get(new AttributeKey(attributeName, locale)))
			.map(AttributesContract.AttributeValue::getValue)
			.orElseGet(() -> getAttribute(attributeName));
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T[]) ofNullable(attributeValues.get(new AttributeKey(attributeName, locale)))
			.map(AttributesContract.AttributeValue::getValue)
			.orElseGet(() -> getAttribute(attributeName));
	}

	@Nonnull
	@Override
	public Collection<AttributeValue> getAttributeValues(@Nonnull String attributeName) {
		return attributeValues
			.entrySet()
			.stream()
			.filter(it -> attributeName.equals(it.getKey().getAttributeName()))
			.map(Entry::getValue)
			.collect(Collectors.toList());
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName, @Nonnull Locale locale) {
		return ofNullable(attributeValues.get(new AttributeKey(attributeName, locale)))
			.orElseGet(() -> attributeValues.get(new AttributeKey(attributeName)));
	}

	@Override
	@Nullable
	public AttributeSchema getAttributeSchema(@Nonnull String attributeName) {
		return attributeTypes.get(attributeName);
	}

	@Override
	@Nonnull
	public Set<String> getAttributeNames() {
		if (this.attributeNames == null) {
			this.attributeNames = this.attributeValues
					.keySet()
					.stream()
					.map(AttributesContract.AttributeKey::getAttributeName)
					.collect(Collectors.toSet());
		}
		return this.attributeNames;
	}

	/**
	 * Returns set of all keys (combination of attribute name and locale) registered in this attribute set.
	 */
	@Nonnull
	@Override
	public Set<AttributeKey> getAttributeKeys() {
		return this.attributeValues.keySet();
	}

	/**
	 * Returns collection of all values present in this object.
	 * @return
	 */
	@Nonnull
	public Collection<AttributeValue> getAttributeValues() {
		return this.attributeValues.values();
	}

	/**
	 * Returns attribute value for passed key.
	 */
	@Nonnull
	public AttributeValue getAttributeValue(AttributeKey attributeKey) {
		return this.attributeValues.get(attributeKey);
	}

	/**
	 * Returns true if there is no attribute set.
	 * @return
	 */
	public boolean isEmpty() {
		return this.attributeValues.isEmpty();
	}

	@Override
	public String toString() {
		return getAttributeValues()
				.stream()
				.map(AttributeValue::toString)
				.collect(Collectors.joining("; "));
	}
}
