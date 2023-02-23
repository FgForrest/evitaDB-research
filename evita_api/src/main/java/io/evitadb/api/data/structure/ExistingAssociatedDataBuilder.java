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

import io.evitadb.api.data.AssociatedDataEditor.AssociatedDataBuilder;
import io.evitadb.api.data.DataObjectConverter;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.RemoveAssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.UpsertAssociatedDataMutation;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.data.structure.InitialAssociatedDataBuilder.verifyAssociatedDataIsInSchemaAndTypeMatch;
import static java.util.Optional.ofNullable;

/**
 * Class supports intermediate mutable object that allows {@link AssociatedData} container rebuilding.
 * We need to closely monitor what associatedData is changed and how. These changes are wrapped in so called mutations
 * (see {@link AssociatedDataMutation} and its implementations) and mutations can be then processed transactionally by
 * the engine.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExistingAssociatedDataBuilder implements AssociatedDataBuilder {
	private static final long serialVersionUID = 3382748927871753611L;

	/**
	 * Initial set of associatedDatas that is going to be modified by this builder.
	 */
	private final AssociatedData baseAssociatedData;
	/**
	 * This predicate filters out associated data that were not fetched in query.
	 */
	@Getter private final SerializablePredicate<AssociatedDataValue> associatedDataPredicate;
	/**
	 * Reflection lookup is used for (de)serialization of {@link io.evitadb.api.dataType.ComplexDataObject}.
	 */
	@SuppressWarnings("TransientFieldNotInitialized")
	private transient ReflectionLookup reflectionLookup;
	/**
	 * Contains locale insensitive associatedData values - simple key → value association map.
	 */
	private final Map<AssociatedDataKey, AssociatedDataMutation> associatedDataMutations;

	/**
	 * AssociatedDataBuilder constructor that will be used for building brand new {@link AssociatedData} container.
	 */
	public ExistingAssociatedDataBuilder(AssociatedData baseAssociatedData) {
		this.associatedDataMutations = new HashMap<>();
		this.baseAssociatedData = baseAssociatedData;
		this.reflectionLookup = baseAssociatedData.reflectionLookup;
		this.associatedDataPredicate = Droppable::exists;
	}

	/**
	 * AssociatedDataBuilder constructor that will be used for building brand new {@link AssociatedData} container.
	 */
	public ExistingAssociatedDataBuilder(AssociatedData baseAssociatedData, SerializablePredicate<AssociatedDataValue> associatedDataPredicate) {
		this.associatedDataMutations = new HashMap<>();
		this.baseAssociatedData = baseAssociatedData;
		this.reflectionLookup = baseAssociatedData.reflectionLookup;
		this.associatedDataPredicate = associatedDataPredicate;
	}

	@Override
	@Nonnull
	public AssociatedDataBuilder removeAssociatedData(@Nonnull String associatedDataName) {
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName);
		associatedDataMutations.put(
				associatedDataKey,
				new RemoveAssociatedDataMutation(associatedDataKey)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T associatedDataValue) {
		final Serializable valueToStore = DataObjectConverter.getSerializableForm(associatedDataValue);
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName);
		verifyAssociatedDataIsInSchemaAndTypeMatch(baseAssociatedData.entitySchema, associatedDataName, valueToStore.getClass());
		associatedDataMutations.put(
				associatedDataKey,
				new UpsertAssociatedDataMutation(associatedDataKey, valueToStore)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T[] associatedDataValue) {
		final Serializable[] valueToStore = new Serializable[associatedDataValue.length];
		for (int i = 0; i < associatedDataValue.length; i++) {
			final T dataItem = associatedDataValue[i];
			valueToStore[i] = DataObjectConverter.getSerializableForm(dataItem);
		}
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName);
		verifyAssociatedDataIsInSchemaAndTypeMatch(baseAssociatedData.entitySchema, associatedDataName, valueToStore.getClass());
		associatedDataMutations.put(
				associatedDataKey,
				new UpsertAssociatedDataMutation(associatedDataKey, valueToStore)
		);
		return this;
	}

	@Nullable
	@Override
	public AssociatedDataSchema getAssociatedDataSchema(@Nonnull String associatedDataName) {
		return baseAssociatedData.getAssociatedDataSchema(associatedDataName);
	}

	@Nonnull
	@Override
	public Set<String> getAssociatedDataNames() {
		return getAssociatedDataValues()
				.stream()
				.map(it -> it.getKey().getAssociatedDataName())
				.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Set<AssociatedDataKey> getAssociatedDataKeys() {
		return getAssociatedDataValues()
			.stream()
			.map(AssociatedDataValue::getKey)
			.collect(Collectors.toSet());
	}

	@Nonnull
	public Stream<AssociatedDataKey> getAssociatedDataKeysWithoutPredicate() {
		return getAssociatedDataValuesWithoutPredicate()
			.map(AssociatedDataValue::getKey);
	}

	@Override
	@Nullable
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName) {
		//noinspection unchecked
		return (T) ofNullable(getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName)))
			.filter(associatedDataPredicate)
			.map(AssociatedDataValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, Class<T> dtoType) {
		final AssociatedDataValue associatedDataValue = getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName));
		if (associatedDataValue == null) {
			return null;
		} else {
			return DataObjectConverter.getOriginalForm(associatedDataValue.getValue(), dtoType, getReflectionLookup());
		}
	}

	@Override
	@Nullable
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName) {
		//noinspection unchecked
		return (T[]) ofNullable(getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName)))
			.filter(associatedDataPredicate)
			.map(AssociatedDataValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName) {
		return getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName));
	}

	@Nonnull
	@Override
	public Collection<AssociatedDataValue> getAssociatedDataValues(@Nonnull String associatedDataName) {
		return getAssociatedDataValues()
			.stream()
			.filter(it -> associatedDataName.equals(it.getKey().getAssociatedDataName()))
			.collect(Collectors.toList());
	}

	/*
		LOCALIZED AssociatedDataS
	 */

	@Override
	@Nonnull
	public AssociatedDataBuilder removeAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName, locale);
		associatedDataMutations.put(
				associatedDataKey,
				new RemoveAssociatedDataMutation(associatedDataKey)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T associatedDataValue) {
		final Serializable valueToStore = DataObjectConverter.getSerializableForm(associatedDataValue);
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName, locale);
		verifyAssociatedDataIsInSchemaAndTypeMatch(baseAssociatedData.entitySchema, associatedDataName, valueToStore.getClass(), locale);
		associatedDataMutations.put(
				associatedDataKey,
				new UpsertAssociatedDataMutation(associatedDataKey, valueToStore)
		);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T[] associatedDataValue) {
		final Serializable[] valueToStore = new Serializable[associatedDataValue.length];
		for (int i = 0; i < associatedDataValue.length; i++) {
			final T dataItem = associatedDataValue[i];
			valueToStore[i] = DataObjectConverter.getSerializableForm(dataItem);
		}
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName, locale);
		verifyAssociatedDataIsInSchemaAndTypeMatch(baseAssociatedData.entitySchema, associatedDataName, valueToStore.getClass(), locale);
		associatedDataMutations.put(
				associatedDataKey,
				new UpsertAssociatedDataMutation(associatedDataKey, valueToStore)
		);
		return this;
	}

	@Override
	@Nullable
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T) ofNullable(getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName, locale)))
			.filter(associatedDataPredicate)
			.map(AssociatedDataValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, Class<T> dtoType) {
		final AssociatedDataValue associatedDataValue = getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName, locale));
		if (associatedDataValue == null) {
			return null;
		} else {
			return DataObjectConverter.getOriginalForm(associatedDataValue.getValue(), dtoType, getReflectionLookup());
		}
	}

	@Override
	@Nullable
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T[]) ofNullable(getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName, locale)))
			.filter(associatedDataPredicate)
			.map(AssociatedDataValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		return getAssociatedDataValueInternal(new AssociatedDataKey(associatedDataName, locale));
	}

	/**
	 * Builds associatedData list based on registered mutations and previous state.
	 * @return
	 */
	@Override
	@Nonnull
	public Collection<AssociatedDataValue> getAssociatedDataValues() {
		return getAssociatedDataValuesWithoutPredicate()
			.filter(associatedDataPredicate)
			.collect(Collectors.toList());
	}

	/**
	 * Builds associatedData list based on registered mutations and previous state without using predicate.
	 * @return
	 */
	@Nonnull
	private Stream<AssociatedDataValue> getAssociatedDataValuesWithoutPredicate() {
		return Stream.concat(
			// process all original associatedData values - they will be: either kept intact if there is no mutation
			// or mutated by the mutation - i.e. updated or removed
			baseAssociatedData.associatedDataValues
				.entrySet()
				.stream()
				// use old associatedData, or apply mutation on the associatedData and return the mutated associatedData
				.map(it -> ofNullable(associatedDataMutations.get(it.getKey()))
					.map(mutation -> {
						final AssociatedDataValue originValue = it.getValue();
						final AssociatedDataValue mutatedAssociatedData = mutation.mutateLocal(originValue);
						return mutatedAssociatedData.differsFrom(originValue) ? mutatedAssociatedData : originValue;
					})
					.orElse(it.getValue())
				),
			// all mutations that doesn't hit existing associatedData probably produce new ones
			// we have to process them as well
			associatedDataMutations
				.values()
				.stream()
				// we want to process only those mutations that have no associatedData to mutate in the original set
				.filter(it -> !baseAssociatedData.getAssociatedDataKeys().contains(it.getAssociatedDataKey()))
				// apply mutation
				.map(it -> it.mutateLocal(null))
		);
	}

	@Nonnull
	public Set<Locale> getAssociatedDataLocales() {
		// this is quite expensive, but should not be called frequently
		return getAssociatedDataValues()
				.stream()
				.map(it -> it.getKey().getLocale())
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public AssociatedDataBuilder mutateAssociatedData(@Nonnull AssociatedDataMutation mutation) {
		associatedDataMutations.put(mutation.getAssociatedDataKey(), mutation);
		return this;
	}

	@Nonnull
	@Override
	public Stream<? extends AssociatedDataMutation> buildChangeSet() {
		return associatedDataMutations.values().stream();
	}

	@Nonnull
	@Override
	public AssociatedData build() {
		if (isThereAnyChangeInMutations()) {
			return new AssociatedData(
					baseAssociatedData.entitySchema,
					baseAssociatedData.reflectionLookup,
					getAssociatedDataKeysWithoutPredicate().collect(Collectors.toSet()),
					getAssociatedDataValuesWithoutPredicate().collect(Collectors.toList())
			);
		} else {
			return baseAssociatedData;
		}
	}

	/**
	 * Returns true if there is single mutation in the local mutations.
	 * @return
	 */
	private boolean isThereAnyChangeInMutations() {
		return Stream.concat(
				// process all original attribute values - they will be: either kept intact if there is no mutation
				// or mutated by the mutation - i.e. updated or removed
				baseAssociatedData.associatedDataValues
						.entrySet()
						.stream()
						// use old attribute, or apply mutation on the attribute and return the mutated attribute
						.map(it -> ofNullable(associatedDataMutations.get(it.getKey()))
								.map(mutation -> {
									final AssociatedDataValue originValue = it.getValue();
									final AssociatedDataValue mutatedAttribute = mutation.mutateLocal(originValue);
									return mutatedAttribute.differsFrom(originValue);
								})
								.orElse(false)
						),
				// all mutations that doesn't hit existing attribute probably produce new ones
				// we have to process them as well
				associatedDataMutations
						.values()
						.stream()
						// we want to process only those mutations that have no attribute to mutate in the original set
						.filter(it -> !baseAssociatedData.getAssociatedDataKeys().contains(it.getAssociatedDataKey()))
						// apply mutation
						.map(it -> true)
		)
				.anyMatch(it -> it);
	}

	/**
	 * Returns either unchanged associatedData value, or associatedData value with applied mutation or even new associatedData value
	 * that is produced by the mutation.
	 *
	 * @param associatedDataKey
	 * @return
	 */
	@Nullable
	private AssociatedDataValue getAssociatedDataValueInternal(AssociatedDataKey associatedDataKey) {
		final AssociatedDataValue associatedDataValue = ofNullable(this.baseAssociatedData.associatedDataValues.get(associatedDataKey))
			.map(it ->
				ofNullable(this.associatedDataMutations.get(associatedDataKey))
					.map(mut -> {
						final AssociatedDataValue mutatedValue = mut.mutateLocal(it);
						return mutatedValue.differsFrom(it) ? mutatedValue : it;
					})
					.orElse(it)
			)
			.orElseGet(() ->
				ofNullable(this.associatedDataMutations.get(associatedDataKey))
					.map(it -> it.mutateLocal(null))
					.orElse(null)
			);
		return associatedDataPredicate.test(associatedDataValue) ? associatedDataValue : null;
	}

	private ReflectionLookup getReflectionLookup() {
		return ofNullable(reflectionLookup).orElseGet(() -> {
			this.reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);
			return this.reflectionLookup;
		});
	}

}
