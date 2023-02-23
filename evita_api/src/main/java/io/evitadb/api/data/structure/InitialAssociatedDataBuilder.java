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
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.exception.InvalidDataTypeMutationException;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Class supports intermediate mutable object that allows {@link AssociatedData} container rebuilding.
 * Due to performance reasons (see {@link DirectWriteOrOperationLog} microbenchmark) there is special implementation
 * for the situation when entity is newly created. In this case we know everything is new and we don't need to closely
 * monitor the changes so this can speed things up.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InitialAssociatedDataBuilder implements AssociatedDataBuilder {
	private static final long serialVersionUID = 7714436064799237939L;
	/**
	 * Entity schema if available.
	 */
	private final EntitySchema entitySchema;
	/**
	 * Reflection lookup is used for (de)serialization of {@link io.evitadb.api.dataType.ComplexDataObject}.
	 */
	private transient ReflectionLookup reflectionLookup;
	/**
	 * Contains locale insensitive associatedData values - simple key → value association map.
	 */
	private final Map<AssociatedDataKey, AssociatedDataValue> associatedDataValues;

	/**
	 * AssociatedDataBuilder constructor that will be used for building brand new {@link AssociatedData} container.
	 */
	InitialAssociatedDataBuilder(EntitySchema entitySchema) {
		this.entitySchema = entitySchema;
		this.associatedDataValues = new HashMap<>();
	}

	@Nullable
	@Override
	public AssociatedDataSchema getAssociatedDataSchema(@Nonnull String associatedDataName) {
		return this.entitySchema.getAssociatedData(associatedDataName);
	}

	@Nonnull
	@Override
	public Set<String> getAssociatedDataNames() {
		return this.associatedDataValues
				.keySet()
				.stream()
				.map(AssociatedDataKey::getAssociatedDataName)
				.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Set<AssociatedDataKey> getAssociatedDataKeys() {
		return this.associatedDataValues.keySet();
	}

	@Nonnull
	@Override
	public Collection<AssociatedDataValue> getAssociatedDataValues() {
		return this.associatedDataValues.values();
	}

	@Override
	@Nonnull
	public AssociatedDataBuilder removeAssociatedData(@Nonnull String associatedDataName) {
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName);
		associatedDataValues.remove(associatedDataKey);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T associatedDataValue) {
		final Serializable valueToStore = DataObjectConverter.getSerializableForm(associatedDataValue);
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName);
		verifyAssociatedDataIsInSchemaAndTypeMatch(entitySchema, associatedDataName, valueToStore.getClass());
		associatedDataValues.put(associatedDataKey, new AssociatedDataValue(associatedDataKey, valueToStore));
		return this;
	}

	@Override
	@Nonnull
	@SuppressWarnings("unchecked")
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T[] associatedDataValue) {
		final Serializable valueToStore = DataObjectConverter.getSerializableForm(associatedDataValue);
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName);
		verifyAssociatedDataIsInSchemaAndTypeMatch(entitySchema, associatedDataName, valueToStore.getClass());
		associatedDataValues.put(associatedDataKey, new AssociatedDataValue(associatedDataKey, valueToStore));
		return this;
	}

	@Override
	@Nullable
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName) {
		//noinspection unchecked
		return (T) ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName)))
				.map(AssociatedDataValue::getValue)
				.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, Class<T> dtoType) {
		return ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName)))
				.map(AssociatedDataValue::getValue)
				.map(it -> DataObjectConverter.getOriginalForm(it, dtoType, getReflectionLookup()))
				.orElse(null);
	}

	@Override
	@Nullable
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName) {
		//noinspection unchecked
		return (T[]) ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName)))
			.map(AssociatedDataValue::getValue)
			.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName) {
		return ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName)))
			.orElse(null);
	}

	@Nonnull
	@Override
	public Collection<AssociatedDataValue> getAssociatedDataValues(@Nonnull String associatedDataName) {
		return associatedDataValues
			.entrySet()
			.stream()
			.filter(it -> associatedDataName.equals(it.getKey().getAssociatedDataName()))
			.map(Entry::getValue)
			.collect(Collectors.toList());
	}

	/*
		LOCALIZED AssociatedDataS
	 */

	@Override
	@Nonnull
	public AssociatedDataBuilder removeAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName, locale);
		this.associatedDataValues.remove(associatedDataKey);
		return this;
	}

	@Override
	@Nonnull
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T associatedDataValue) {
		final Serializable valueToStore = DataObjectConverter.getSerializableForm(associatedDataValue);
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName, locale);
		verifyAssociatedDataIsInSchemaAndTypeMatch(entitySchema, associatedDataName, valueToStore.getClass(), locale);
		this.associatedDataValues.put(associatedDataKey, new AssociatedDataValue(associatedDataKey, valueToStore));
		return this;
	}

	@Override
	@Nonnull
	@SuppressWarnings("unchecked")
	public <T extends Serializable> AssociatedDataBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T[] associatedDataValue) {
		final Serializable valueToStore = DataObjectConverter.getSerializableForm(associatedDataValue);
		final AssociatedDataKey associatedDataKey = new AssociatedDataKey(associatedDataName, locale);
		verifyAssociatedDataIsInSchemaAndTypeMatch(entitySchema, associatedDataName, valueToStore.getClass(), locale);
		this.associatedDataValues.put(associatedDataKey, new AssociatedDataValue(associatedDataKey, valueToStore));
		return this;
	}

	@Override
	@Nullable
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T) ofNullable(this.associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
				.map(AssociatedDataValue::getValue)
				.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, Class<T> dtoType) {
		return ofNullable(this.associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
				.map(AssociatedDataValue::getValue)
				.map(it -> DataObjectConverter.getOriginalForm(it, dtoType, getReflectionLookup()))
				.orElse(null);
	}

	@Override
	@Nullable
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T[]) ofNullable(this.associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
				.map(AssociatedDataValue::getValue)
				.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		return ofNullable(this.associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
			.orElse(null);
	}

	@Nonnull
	public Set<Locale> getAssociatedDataLocales() {
		return this.associatedDataValues
				.keySet()
				.stream()
				.map(AssociatedDataKey::getLocale)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public AssociatedDataBuilder mutateAssociatedData(@Nonnull AssociatedDataMutation mutation) {
		throw new UnsupportedOperationException("You cannot apply mutation when entity is just being created!");
	}

	@Nonnull
	@Override
	public Stream<? extends AssociatedDataMutation> buildChangeSet() {
		throw new UnsupportedOperationException("Initial entity creation doesn't support change monitoring - it has no sense.");
	}

	@Nonnull
	@Override
	public AssociatedData build() {
		// let's check whether there are compatible attributes
		// noinspection ResultOfMethodCallIgnored
		this.associatedDataValues
			.values()
			.stream()
			.map(this::createImplicitSchema)
			.collect(
				Collectors.toMap(
					AssociatedDataSchema::getName,
					Function.identity(),
					(associatedDataType, associatedDataType2) -> {
						Assert.isTrue(
							Objects.equals(associatedDataType, associatedDataType2),
							"Ambiguous situation - there are two associated data with the same name and different definition:\n" +
								associatedDataType + "\n" +
								associatedDataType2
						);
						return associatedDataType;
					}
				)
			);

		return new AssociatedData(
				this.entitySchema,
				getReflectionLookup(),
				this.associatedDataValues.keySet(),
				this.associatedDataValues.values()
		);
	}

	private ReflectionLookup getReflectionLookup() {
		return ofNullable(reflectionLookup).orElseGet(() -> {
			this.reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);
			return this.reflectionLookup;
		});
	}

	static void verifyAssociatedDataIsInSchemaAndTypeMatch(@Nonnull EntitySchema entitySchema, @Nonnull String associatedDataName, @Nullable Class<? extends Serializable> aClass) {
		final AssociatedDataSchema associatedDataSchema = entitySchema.getAssociatedData(associatedDataName);
		verifyAssociatedDataIsInSchemaAndTypeMatch(entitySchema, associatedDataName, aClass, null, associatedDataSchema);
	}

	static void verifyAssociatedDataIsInSchemaAndTypeMatch(@Nonnull EntitySchema entitySchema, @Nonnull String associatedDataName, @Nullable Class<? extends Serializable> aClass, @Nonnull Locale locale) {
		final AssociatedDataSchema associatedDataSchema = entitySchema.getAssociatedData(associatedDataName);
		verifyAssociatedDataIsInSchemaAndTypeMatch(entitySchema, associatedDataName, aClass, locale, associatedDataSchema);
	}


	static void verifyAssociatedDataIsInSchemaAndTypeMatch(@Nonnull EntitySchema entitySchema, @Nonnull String associatedDataName, @Nullable Class<? extends Serializable> aClass, Locale locale, AssociatedDataSchema associatedDataSchema) {
		Assert.isTrue(
				associatedDataSchema != null || entitySchema.allows(EvolutionMode.ADDING_ASSOCIATED_DATA),
				() -> new InvalidMutationException(
						"AssociatedData " + associatedDataName + " is not configured in entity " + entitySchema.getName() +
								" schema and automatic evolution is not enabled for associated data!"
				)
		);
		if (associatedDataSchema != null) {
			if (aClass != null) {
				Assert.isTrue(
						associatedDataSchema.getType().isAssignableFrom(aClass),
						() -> new InvalidDataTypeMutationException(
								"AssociatedData " + associatedDataName + " accepts only type " + associatedDataSchema.getType().getName() +
										" - value type is different: " + aClass.getName() + "!",
								associatedDataSchema.getType(), aClass
						)
				);
			}
			if (locale == null) {
				Assert.isTrue(
						!associatedDataSchema.isLocalized(),
						() -> new InvalidMutationException(
								"AssociatedData " + associatedDataName + " is localized and doesn't accept non-localized associated data!"
						)
				);
			} else {
				Assert.isTrue(
						associatedDataSchema.isLocalized(),
						() -> new InvalidMutationException(
								"AssociatedData " + associatedDataName + " is not localized and doesn't accept localized associated data!"
						)
				);
				Assert.isTrue(
						entitySchema.supportsLocale(locale) || entitySchema.allows(EvolutionMode.ADDING_LOCALES),
						() -> new InvalidMutationException(
								"AssociatedData " + associatedDataName + " is localized, but schema doesn't support locale " + locale + "! " +
										"Supported locales are: " +
										entitySchema.getLocales().stream().map(Locale::toString).collect(Collectors.joining(", "))
						)
				);
			}
		} else if (locale != null) {
			// at least verify supported locale
			Assert.isTrue(
					entitySchema.supportsLocale(locale) || entitySchema.allows(EvolutionMode.ADDING_LOCALES),
					() -> new InvalidMutationException(
							"AssociatedData " + associatedDataName + " is localized, but schema doesn't support locale " + locale + "! " +
									"Supported locales are: " +
									entitySchema.getLocales().stream().map(Locale::toString).collect(Collectors.joining(", "))
					)
			);
		}
	}

}
