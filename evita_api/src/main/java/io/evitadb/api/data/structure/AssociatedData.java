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

import io.evitadb.api.data.AssociatedDataContract;
import io.evitadb.api.data.DataObjectConverter;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * Associated data carry additional data entries that are never used for filtering / sorting but may be needed to be fetched
 * along with entity in order to present data to the target consumer (i.e. user / API / bot). Associated data may be stored
 * in slower storage and may contain wide range of data types - from small ones (i.e. numbers, strings, dates) up to large
 * binary arrays representing entire files (i.e. pictures, documents).
 *
 * The search query must contain specific {@link io.evitadb.api.query.require.AssociatedData} requirement in order
 * associated data are fetched along with the entity. Associated data are stored and fetched separately by their name.
 *
 * Class is immutable on purpose - we want to support caching the entities in a shared cache and accessed by many threads.
 * For altering the contents use {@link io.evitadb.api.data.AssociatedDataEditor.AssociatedDataBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode
@Immutable
@ThreadSafe
public class AssociatedData implements AssociatedDataContract {
	private static final long serialVersionUID = 4916435515883999950L;
	/**
	 * Definition of the entity schema.
	 */
	final EntitySchema entitySchema;
	/**
	 * Contains locale insensitive associatedData values - simple key → value association map.
	 */
	final Map<AssociatedDataKey, AssociatedDataValue> associatedDataValues;
	/**
	 * Contains associatedData definition that is built up along way with associatedData adding or it may be directly filled
	 * in from the engine when entity with associated data is loaded from persistent storage.
	 */
	final Map<String, AssociatedDataSchema> associatedDataTypes;
	/**
	 * Reflection lookup is used for (de)serialization of {@link io.evitadb.api.dataType.ComplexDataObject}.
	 */
	@SuppressWarnings("TransientFieldNotInitialized")
	transient ReflectionLookup reflectionLookup;
	/**
	 * Optimization that ensures that expensive associatedData name resolving happens only once.
	 */
	private Set<String> associatedDataNames;
	/**
	 * Optimization that ensures that expensive associatedData locale resolving happens only once.
	 */
	private Set<Locale> associatedDataLocales;

	/**
	 * Constructor should be used only when associated data are loaded from persistent storage.
	 * Constructor is meant to be internal to the Evita engine.
	 */
	public AssociatedData(
		@Nonnull EntitySchema entitySchema,
		@Nonnull ReflectionLookup reflectionLookup,
		@Nonnull Set<AssociatedDataKey> associatedDataKeys,
		@Nullable Collection<AssociatedDataValue> associatedDataValues
	) {
		this.entitySchema = entitySchema;
		this.reflectionLookup = reflectionLookup;
		this.associatedDataValues = createHashMap(associatedDataKeys.size());
		for (AssociatedDataKey associatedDataKey : associatedDataKeys) {
			this.associatedDataValues.put(associatedDataKey, null);
		}
		if (associatedDataValues != null) {
			for (AssociatedDataValue associatedDataValue : associatedDataValues) {
				this.associatedDataValues.put(associatedDataValue.getKey(), associatedDataValue);
			}
		}
		this.associatedDataTypes = entitySchema.getAssociatedData();
	}

	/**
	 * Constructor should be used only when associated data are loaded from persistent storage.
	 * Constructor is meant to be internal to the Evita engine.
	 */
	public AssociatedData(
		@Nonnull EntitySchema entitySchema,
		@Nonnull ReflectionLookup reflectionLookup,
		@Nullable Collection<AssociatedDataValue> associatedDataValues
	) {
		this.entitySchema = entitySchema;
		this.reflectionLookup = reflectionLookup;
		this.associatedDataValues = ofNullable(associatedDataValues)
			.map(it -> it.stream().collect(Collectors.toMap(AssociatedDataValue::getKey, Function.identity())))
			.orElse(Collections.emptyMap());
		this.associatedDataTypes = entitySchema.getAssociatedData();
	}

	public AssociatedData(
		@Nonnull EntitySchema entitySchema,
		@Nonnull ReflectionLookup reflectionLookup
	) {
		this.entitySchema = entitySchema;
		this.reflectionLookup = reflectionLookup;
		this.associatedDataValues = Collections.emptyMap();
		this.associatedDataTypes = entitySchema.getAssociatedData();
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
		return associatedDataValues.get(new AssociatedDataKey(associatedDataName));
	}

	@Override
	@Nullable
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T) ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
			.map(AssociatedDataValue::getValue)
			.orElseGet(() -> getAssociatedData(associatedDataName));
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, Class<T> dtoType) {
		return ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
			.map(AssociatedDataValue::getValue)
			.map(it -> DataObjectConverter.getOriginalForm(it, dtoType, getReflectionLookup()))
			.orElseGet(() -> getAssociatedData(associatedDataName));
	}

	@Override
	@Nullable
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return (T[]) ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
			.map(AssociatedDataValue::getValue)
			.orElseGet(() -> getAssociatedData(associatedDataName));
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		return ofNullable(associatedDataValues.get(new AssociatedDataKey(associatedDataName, locale)))
			.orElseGet(() -> associatedDataValues.get(new AssociatedDataKey(associatedDataName)));
	}

	@Override
	@Nullable
	public AssociatedDataSchema getAssociatedDataSchema(@Nonnull String associatedDataName) {
		return associatedDataTypes.get(associatedDataName);
	}

	@Override
	@Nonnull
	public Set<String> getAssociatedDataNames() {
		if (this.associatedDataNames == null) {
			this.associatedDataNames = this.associatedDataValues
				.keySet()
				.stream()
				.map(AssociatedDataKey::getAssociatedDataName)
				.collect(Collectors.toSet());
		}
		return this.associatedDataNames;
	}

	@Nonnull
	@Override
	public Set<AssociatedDataKey> getAssociatedDataKeys() {
		return this.associatedDataValues.keySet();
	}

	/**
	 * Returns collection of all associatedDatas of the entity.
	 */
	@Override
	@Nonnull
	public Collection<AssociatedDataValue> getAssociatedDataValues() {
		return this.associatedDataValues
			.values()
			.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Collection<AssociatedDataValue> getAssociatedDataValues(@Nonnull String associatedDataName) {
		return associatedDataValues
			.entrySet()
			.stream().filter(it -> associatedDataName.equals(it.getKey().getAssociatedDataName()))
			.map(Entry::getValue)
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Set<Locale> getAssociatedDataLocales() {
		if (this.associatedDataLocales == null) {
			this.associatedDataLocales = this.associatedDataValues
				.keySet()
				.stream()
				.map(AssociatedDataKey::getLocale)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		}
		return this.associatedDataLocales;
	}

	/**
	 * Returns associatedData value for passed key.
	 */
	@Nonnull
	public AssociatedDataValue getAssociatedData(AssociatedDataKey associatedDataKey) {
		return this.associatedDataValues.get(associatedDataKey);
	}

	/**
	 * Returns true if there is no associated data set.
	 */
	public boolean isEmpty() {
		return this.associatedDataValues.isEmpty();
	}

	@Override
	public String toString() {
		return getAssociatedDataValues()
			.stream()
			.map(AssociatedDataValue::toString)
			.collect(Collectors.joining("; "));
	}

	private ReflectionLookup getReflectionLookup() {
		return ofNullable(reflectionLookup).orElseGet(() -> {
			this.reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);
			return this.reflectionLookup;
		});
	}

}
