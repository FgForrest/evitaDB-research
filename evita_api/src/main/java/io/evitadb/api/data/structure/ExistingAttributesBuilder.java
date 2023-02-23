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

import io.evitadb.api.data.AttributesEditor.AttributesBuilder;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation;
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.data.structure.InitialAttributesBuilder.verifyAttributeIsInSchemaAndTypeMatch;
import static java.util.Optional.ofNullable;

/**
 * Class supports intermediate mutable object that allows {@link Attributes} container rebuilding.
 * We need to closely monitor what attribute is changed and how. These changes are wrapped in so called mutations
 * (see {@link AttributeMutation} and its implementations) and mutations can be then processed transactionally by
 * the engine.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExistingAttributesBuilder implements AttributesBuilder {
	private static final long serialVersionUID = 3382748927871753611L;

	/**
	 * Initial set of attributes that is going to be modified by this builder.
	 */
	private final Attributes baseAttributes;
	/**
	 * When this flag is set to true - verification on store is suppressed. It can be set to true only when verification
	 * is ensured by calling logic.
	 */
	private final boolean suppressVerification;
	/**
	 * Contains locale insensitive attribute values - simple key → value association map.
	 */
	private final Map<AttributeKey, AttributeMutation> attributeMutations;
	/**
	 * This predicate filters out attributes that were not fetched in query.
	 */
	private final SerializablePredicate<AttributeValue> attributePredicate;

	/**
	 * AttributesBuilder constructor that will be used for building brand new {@link Attributes} container.
	 */
	public ExistingAttributesBuilder(Attributes baseAttributes) {
		this.attributeMutations = new HashMap<>();
		this.baseAttributes = baseAttributes;
		this.suppressVerification = false;
		this.attributePredicate = Droppable::exists;
	}

	/**
	 * AttributesBuilder constructor that will be used for building brand new {@link Attributes} container.
	 */
	public ExistingAttributesBuilder(EntitySchema entitySchema, Collection<AttributeValue> attributes, Set<Locale> attributeLocales) {
		this.attributeMutations = new HashMap<>();
		this.baseAttributes = new Attributes(entitySchema, attributes, attributeLocales);
		this.suppressVerification = false;
		this.attributePredicate = Droppable::exists;
	}

	/**
	 * AttributesBuilder constructor that will be used for building brand new {@link Attributes} container.
	 */
	public ExistingAttributesBuilder(Attributes baseAttributes, SerializablePredicate<AttributeValue> attributePredicate) {
		this.attributeMutations = new HashMap<>();
		this.baseAttributes = baseAttributes;
		this.suppressVerification = false;
		this.attributePredicate = attributePredicate;
	}

	/**
	 * AttributesBuilder constructor that will be used for building brand new {@link Attributes} container.
	 */
	ExistingAttributesBuilder(EntitySchema entitySchema, Collection<AttributeValue> attributes, Set<Locale> attributeLocales, boolean suppressVerification) {
		this.attributeMutations = new HashMap<>();
		this.baseAttributes = new Attributes(entitySchema, attributes, attributeLocales);
		this.suppressVerification = suppressVerification;
		this.attributePredicate = Droppable::exists;
	}

	/**
	 * AttributesBuilder constructor that will be used for building brand new {@link Attributes} container.
	 */
	ExistingAttributesBuilder(Attributes baseAttributes, boolean suppressVerification) {
		this.attributeMutations = new HashMap<>();
		this.baseAttributes = baseAttributes;
		this.suppressVerification = suppressVerification;
		this.attributePredicate = Droppable::exists;
	}

	@Override
	@Nonnull
	public AttributesBuilder removeAttribute(@Nonnull String attributeName) {
		final AttributeKey attributeKey = new AttributeKey(attributeName);
		attributeMutations.put(
				attributeKey,
				new RemoveAttributeMutation(attributeKey)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable & Comparable<?>> AttributesBuilder setAttribute(@Nonnull String attributeName, @Nonnull T attributeValue) {
		final AttributeKey attributeKey = new AttributeKey(attributeName);
		if (!suppressVerification) {
			verifyAttributeIsInSchemaAndTypeMatch(baseAttributes.entitySchema, attributeName, attributeValue.getClass());
		}
		attributeMutations.put(
				attributeKey,
				new UpsertAttributeMutation(attributeKey, attributeValue)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable & Comparable<?>> AttributesBuilder setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue) {
		final AttributeKey attributeKey = new AttributeKey(attributeName);
		if (!suppressVerification) {
			verifyAttributeIsInSchemaAndTypeMatch(baseAttributes.entitySchema, attributeName, attributeValue.getClass());
		}
		attributeMutations.put(
				attributeKey,
				new UpsertAttributeMutation(attributeKey, attributeValue)
		);
		return this;
	}

	@Nullable
	@Override
	public AttributeSchema getAttributeSchema(@Nonnull String attributeName) {
		return baseAttributes.getAttributeSchema(attributeName);
	}

	@Nonnull
	@Override
	public Set<String> getAttributeNames() {
		return getAttributeValues()
				.stream()
				.filter(attributePredicate)
				.map(it -> it.getKey().getAttributeName())
				.collect(Collectors.toSet());
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName) {
		//noinspection unchecked
		return (T) ofNullable(getAttributeValueInternal(new AttributeKey(attributeName)))
			.map(AttributeValue::getValue)
			.orElse(null);
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName) {
		//noinspection unchecked
		return (T[]) ofNullable(getAttributeValueInternal(new AttributeKey(attributeName)))
			.map(AttributeValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName) {
		return getAttributeValueInternal(new AttributeKey(attributeName));
	}

	@Nonnull
	@Override
	public Collection<AttributeValue> getAttributeValues(@Nonnull String attributeName) {
		return getAttributeValues()
			.stream()
			.filter(it -> attributeName.equals(it.getKey().getAttributeName()))
			.collect(Collectors.toList());
	}

	/*
		LOCALIZED ATTRIBUTES
	 */

	@Override
	@Nonnull
	public AttributesBuilder removeAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		final AttributeKey attributeKey = new AttributeKey(attributeName, locale);
		attributeMutations.put(
				attributeKey,
				new RemoveAttributeMutation(attributeKey)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable & Comparable<?>> AttributesBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T attributeValue) {
		final AttributeKey attributeKey = new AttributeKey(attributeName, locale);
		if (!suppressVerification) {
			verifyAttributeIsInSchemaAndTypeMatch(baseAttributes.entitySchema, attributeName, attributeValue.getClass(), locale);
		}
		attributeMutations.put(
				attributeKey,
				new UpsertAttributeMutation(attributeKey, attributeValue)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable & Comparable<?>> AttributesBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue) {
		final AttributeKey attributeKey = new AttributeKey(attributeName, locale);
		if (!suppressVerification) {
			verifyAttributeIsInSchemaAndTypeMatch(baseAttributes.entitySchema, attributeName, attributeValue.getClass(), locale);
		}
		attributeMutations.put(
				attributeKey,
				new UpsertAttributeMutation(attributeKey, attributeValue)
		);
		return this;
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T) ofNullable(getAttributeValueInternal(new AttributeKey(attributeName, locale)))
			.map(AttributeValue::getValue)
			.orElse(null);
	}

	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T[]) ofNullable(getAttributeValueInternal(new AttributeKey(attributeName, locale)))
			.map(AttributeValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName, @Nonnull Locale locale) {
		return getAttributeValueInternal(new AttributeKey(attributeName, locale));
	}

	@Nonnull
	public Set<Locale> getAttributeLocales() {
		// this is quite expensive, but should not be called frequently
		return getAttributeValues()
				.stream()
				.map(it -> it.getKey().getLocale())
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public AttributesBuilder mutateAttribute(@Nonnull AttributeMutation mutation) {
		attributeMutations.put(mutation.getAttributeKey(), mutation);
		return this;
	}

	@Nonnull
	@Override
	public Stream<? extends AttributeMutation> buildChangeSet() {
		return attributeMutations.values().stream();
	}

	@Nonnull
	@Override
	public Attributes build() {
		if (isThereAnyChangeInMutations()) {
			final Collection<AttributeValue> newAttributeValues = getAttributeValuesWithoutPredicate().collect(Collectors.toList());
			final Map<String, AttributeSchema> newAttributeTypes = Stream.concat(
					baseAttributes.attributeTypes.values().stream(),
					// we don't check baseAttributes.allowUnknownAttributeTypes here because it gets checked on adding a mutation
					newAttributeValues
							.stream()
							// filter out new attributes that has no type yet
							.filter(it -> !baseAttributes.attributeTypes.containsKey(it.getKey().getAttributeName()))
							// create definition for them on the fly
							.map(this::createImplicitSchema)
			)
					.collect(
							Collectors.toMap(
								AttributeSchema::getName,
								Function.identity(),
								(attributeSchema, attributeSchema2) -> {
									Assert.isTrue(
										attributeSchema.equals(attributeSchema2),
										"Attribute " + attributeSchema.getName() + " has incompatible types in the same entity!"
									);
									return attributeSchema;
								}
							)
					);
			return new Attributes(
				baseAttributes.entitySchema,
				newAttributeValues,
				newAttributeValues
					.stream()
					.map(it -> it.getKey().getLocale())
					.filter(Objects::nonNull)
					.collect(Collectors.toSet()), newAttributeTypes
			);
		} else {
			return baseAttributes;
		}
	}

	@Nonnull
	@Override
	public Set<AttributeKey> getAttributeKeys() {
		return getAttributeValues()
				.stream()
				.map(AttributeValue::getKey)
				.collect(Collectors.toSet());
	}

	/**
	 * Builds attribute list based on registered mutations and previous state.
	 * @return
	 */
	@Nonnull
	public Collection<AttributeValue> getAttributeValues() {
		return getAttributeValuesWithoutPredicate()
				.filter(attributePredicate)
				.collect(Collectors.toList());
	}

	/**
	 * Builds attribute list based on registered mutations and previous state.
	 * @return
	 */
	@Nonnull
	private Stream<AttributeValue> getAttributeValuesWithoutPredicate() {
		return Stream.concat(
			// process all original attribute values - they will be: either kept intact if there is no mutation
			// or mutated by the mutation - i.e. updated or removed
			baseAttributes.attributeValues
				.entrySet()
				.stream()
				// use old attribute, or apply mutation on the attribute and return the mutated attribute
				.map(it -> ofNullable(attributeMutations.get(it.getKey()))
					.map(mutation -> {
						final AttributeValue originValue = it.getValue();
						final AttributeValue mutatedAttribute = mutation.mutateLocal(originValue);
						return mutatedAttribute.differsFrom(originValue) ? mutatedAttribute : originValue;
					})
					.orElse(it.getValue())
				),
			// all mutations that doesn't hit existing attribute probably produce new ones
			// we have to process them as well
			attributeMutations
				.values()
				.stream()
				// we want to process only those mutations that have no attribute to mutate in the original set
				.filter(it -> !baseAttributes.getAttributeKeys().contains(it.getAttributeKey()))
				// apply mutation
				.map(it -> it.mutateLocal(null))
		);
	}

	/**
	 * Returns true if there is single mutation in the local mutations.
	 * @return
	 */
	boolean isThereAnyChangeInMutations() {
		return Stream.concat(
				// process all original attribute values - they will be: either kept intact if there is no mutation
				// or mutated by the mutation - i.e. updated or removed
				baseAttributes.attributeValues
						.entrySet()
						.stream()
						// use old attribute, or apply mutation on the attribute and return the mutated attribute
						.map(it -> ofNullable(attributeMutations.get(it.getKey()))
								.map(mutation -> {
									final AttributeValue originValue = it.getValue();
									final AttributeValue mutatedAttribute = mutation.mutateLocal(originValue);
									return mutatedAttribute.differsFrom(originValue);
								})
								.orElse(false)
						),
				// all mutations that doesn't hit existing attribute probably produce new ones
				// we have to process them as well
				attributeMutations
						.values()
						.stream()
						// we want to process only those mutations that have no attribute to mutate in the original set
						.filter(it -> !baseAttributes.getAttributeKeys().contains(it.getAttributeKey()))
						// apply mutation
						.map(it -> true)
		)
				.anyMatch(it -> it);
	}

	/**
	 * Returns either unchanged attribute value, or attribute value with applied mutation or even new attribute value
	 * that is produced by the mutation.
	 *
	 * @param attributeKey
	 * @return
	 */
	@Nullable
	private AttributeValue getAttributeValueInternal(AttributeKey attributeKey) {
		final AttributeValue attributeValue = ofNullable(this.baseAttributes.attributeValues.get(attributeKey))
			.map(it ->
				ofNullable(this.attributeMutations.get(attributeKey))
					.map(mut -> {
						final AttributeValue mutatedValue = mut.mutateLocal(it);
						return mutatedValue.differsFrom(it) ? mutatedValue : it;
					})
					.orElse(it)
			)
			.orElseGet(() ->
				ofNullable(this.attributeMutations.get(attributeKey))
					.map(it -> it.mutateLocal(null))
					.orElse(null)
			);
		return attributePredicate.test(attributeValue) ? attributeValue : null;
	}

}
