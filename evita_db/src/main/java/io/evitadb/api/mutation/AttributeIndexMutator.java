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

package io.evitadb.api.mutation;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.api.data.structure.EntityStorageContainerAccessor;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.NumberUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.IndexType;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import io.evitadb.storage.model.storageParts.entity.ReferencesStoragePart;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * This interface is used to co-locate attribute mutating routines which are rather procedural and long to avoid excessive
 * amount of code in {@link EntityIndexLocalMutationExecutor}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface AttributeIndexMutator {

	/**
	 * Handles {@link io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation} and alters {@link EntityIndex}
	 * data according this mutation.
	 */
	static void executeAttributeUpsert(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull Function<String, AttributeSchema> schemaProvider,
		@Nonnull Supplier<AttributeValue> existingValueSupplier,
		@Nonnull EntityIndex index,
		@Nonnull AttributeKey attributeKey,
		@Nonnull Serializable attributeValue,
		boolean updateLanguage
	) {
		final AttributeSchema attributeDefinition = schemaProvider.apply(attributeKey.getAttributeName());
		Assert.notNull(attributeDefinition, "Attribute `" + attributeKey.getAttributeName() + "` not defined in schema!");

		if (updateLanguage) {
			ofNullable(attributeKey.getLocale())
				.ifPresent(it -> executor.upsertEntityLanguage(index, it));
		}

		if (attributeDefinition.isUnique() || attributeDefinition.isFilterable() || attributeDefinition.isSortable()) {
			final Object newValue = EvitaDataTypes.toTargetType(attributeValue, attributeDefinition.getType(), attributeDefinition.getIndexedDecimalPlaces());
			insertValue(executor, index, attributeDefinition, existingValueSupplier, attributeKey, newValue);
		}
	}

	/**
	 * Handles {@link io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation} and alters {@link EntityIndex}
	 * data according this mutation.
	 */
	static <T extends Serializable & Comparable<T>> void executeAttributeRemoval(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull Function<String, AttributeSchema> schemaProvider,
		@Nonnull Supplier<AttributeValue> existingValueSupplier,
		@Nonnull EntityIndex index,
		@Nonnull AttributeKey attributeKey,
		boolean updateLanguage
	) {
		final EntitySchema entitySchema = executor.getEntitySchema();
		final AttributeSchema attributeDefinition = schemaProvider.apply(attributeKey.getAttributeName());
		Assert.notNull(attributeDefinition, "Attribute `" + attributeKey.getAttributeName() + "` not defined in schema!");

		final Set<Locale> allowedLocales = entitySchema.getLocales();
		final Locale locale = attributeKey.getLocale();

		if (attributeDefinition.isUnique() || attributeDefinition.isFilterable() || attributeDefinition.isSortable()) {
			final AttributeValue existingValue = existingValueSupplier.get();
			Assert.notNull(existingValue, "Attribute `" + attributeDefinition.getName() + "` is unexpectedly not found in container for entity `" + entitySchema.getName() + "` record " + executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_INDEX) + "!");
			@SuppressWarnings("unchecked") final T oldValue = (T) existingValue.getValue();

			if (attributeDefinition.isUnique()) {
				index.removeUniqueAttribute(attributeDefinition, allowedLocales, locale, oldValue, executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_UNIQUE_INDEX));
				if (String.class.equals(attributeDefinition.getType()) && !attributeDefinition.isFilterable()) {
					// TOBEDONE JNO this should be replaced with RadixTree
					index.removeFilterAttribute(attributeDefinition, allowedLocales, locale, oldValue, executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_FILTER_INDEX));
				}
			}
			if (attributeDefinition.isFilterable()) {
				index.removeFilterAttribute(attributeDefinition, allowedLocales, locale, oldValue, executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_FILTER_INDEX));
			}
			if (attributeDefinition.isSortable()) {
				index.removeSortAttribute(attributeDefinition, allowedLocales, locale, oldValue, executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_SORT_INDEX));
			}
		}

		if (updateLanguage && locale != null) {
			final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_INDEX);
			if (!entityHasEffectivelyLocalizedValueExcept(executor, entityPrimaryKey, locale, attributeDefinition.getName(), null)) {
				executor.removeEntityLanguage(index, locale);
			}
		}
	}

	/**
	 * Returns true if there is single localized value of particular `language` in the entity.
	 */
	static boolean entityHasEffectivelyLocalizedValueExcept(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		int entityPrimaryKey,
		@Nonnull Locale language,
		@Nullable String exceptAttributeName,
		@Nullable String exceptAssociatedDataName
	) {
		final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
		final EntityBodyStoragePart entityStorageContainer = containerAccessor.getEntityStorageContainer(entityPrimaryKey, EntityExistence.MUST_EXIST);
		if (entityStorageContainer.getAssociatedDataKeys()
			.stream()
			.filter(it -> Objects.equals(language, it.getLocale()))
			.anyMatch(it -> exceptAssociatedDataName == null || !exceptAssociatedDataName.equals(it.getAssociatedDataName()))
		) {
			return true;
		}
		final AttributesStoragePart localizedAttributesCnt = containerAccessor.getAttributeStorageContainer(entityPrimaryKey, language);
		final AttributeValue[] localizedAttributes = localizedAttributesCnt.getAttributes();
		if ((exceptAttributeName == null && !ArrayUtils.isEmpty(localizedAttributes))
			|| Arrays.stream(localizedAttributes).anyMatch(it -> !Objects.equals(it.getKey().getAttributeName(), exceptAttributeName))) {
			return true;
		}
		final ReferencesStoragePart references = containerAccessor.getReferencesStorageContainer(entityPrimaryKey);
		return Arrays.stream(references.getReferences())
			.flatMap(it -> it.getAttributeValues().stream())
			.anyMatch(it -> Objects.equals(it.getKey().getLocale(), language));
	}

	/**
	 * Handles {@link io.evitadb.api.data.mutation.attribute.ApplyDeltaAttributeMutation} and alters {@link EntityIndex}
	 * data according this mutation.
	 */
	static <T extends Serializable & Comparable<T>> void executeAttributeDelta(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull Function<String, AttributeSchema> schemaProvider,
		@Nonnull Supplier<AttributeValue> existingValueSupplier,
		@Nonnull EntityIndex index,
		@Nonnull AttributeKey attributeKey,
		@Nonnull Number delta
	) {
		final EntitySchema entitySchema = executor.getEntitySchema();
		final AttributeSchema attributeDefinition = schemaProvider.apply(attributeKey.getAttributeName());

		if (attributeDefinition.isUnique() || attributeDefinition.isFilterable() || attributeDefinition.isSortable()) {
			final Locale locale = attributeKey.getLocale();
			final Set<Locale> allowedLocales = entitySchema.getLocales();

			final AttributeValue existingValue = existingValueSupplier.get();
			Assert.notNull(existingValue, "Attribute `" + attributeDefinition.getName() + "` is unexpectedly not found in indexes for entity `" + entitySchema.getName() + "` record " + executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_INDEX) + "!");
			@SuppressWarnings("unchecked") final T oldValue = (T) existingValue.getValue();
			Assert.isTrue(oldValue instanceof Number, "Attribute `" + attributeDefinition.getName() + "` in entity `" + entitySchema.getName() + "` is not a number type!");
			@SuppressWarnings("unchecked") final T result = (T) NumberUtils.sum((Number) oldValue, (Number) EvitaDataTypes.toTargetType(delta, attributeDefinition.getType(), attributeDefinition.getIndexedDecimalPlaces()));

			if (attributeDefinition.isUnique()) {
				final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_UNIQUE_INDEX);
				index.removeUniqueAttribute(attributeDefinition, allowedLocales, locale, oldValue, entityPrimaryKey);
				index.insertUniqueAttribute(attributeDefinition, allowedLocales, locale, result, entityPrimaryKey);
			}
			if (attributeDefinition.isFilterable()) {
				final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_FILTER_INDEX);
				index.removeFilterAttribute(attributeDefinition, allowedLocales, locale, oldValue, entityPrimaryKey);
				index.insertFilterAttribute(attributeDefinition, allowedLocales, locale, result, entityPrimaryKey);
			}
			if (attributeDefinition.isSortable()) {
				final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_SORT_INDEX);
				index.removeSortAttribute(attributeDefinition, allowedLocales, locale, oldValue, entityPrimaryKey);
				index.insertSortAttribute(attributeDefinition, allowedLocales, locale, result, entityPrimaryKey);
			}
		}
	}

	/**
	 * Inserts or updates value from/to indexes. Arrays of objects are automatically unwrapped and handled as a set of discrete
	 * values.
	 */
	private static void insertValue(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex index,
		@Nonnull AttributeSchema attributeDefinition,
		@Nonnull Supplier<AttributeValue> existingValueSupplier,
		@Nonnull AttributeKey attributeKey,
		@Nonnull Object valueToInsert
	) {
		final Set<Locale> allowedLocales = executor.getEntitySchema().getLocales();
		final Locale locale = attributeKey.getLocale();

		if (attributeDefinition.isUnique()) {
			final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_UNIQUE_INDEX);
			final AttributeValue existingValue = existingValueSupplier.get();
			if (existingValue != null) {
				index.removeUniqueAttribute(attributeDefinition, allowedLocales, locale, existingValue.getValue(), entityPrimaryKey);
				if (String.class.equals(attributeDefinition.getType()) && !attributeDefinition.isFilterable()) {
					// TOBEDONE JNO this should be replaced with RadixTree
					index.removeFilterAttribute(attributeDefinition, allowedLocales, locale, existingValue.getValue(), entityPrimaryKey);
				}
			}
			index.insertUniqueAttribute(attributeDefinition, allowedLocales, locale, valueToInsert, entityPrimaryKey);
			if (String.class.equals(attributeDefinition.getType()) && !attributeDefinition.isFilterable()) {
				// TOBEDONE JNO this should be replaced with RadixTree
				index.insertFilterAttribute(attributeDefinition, allowedLocales, locale, valueToInsert, entityPrimaryKey);
			}
		}
		if (attributeDefinition.isFilterable()) {
			final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_FILTER_INDEX);
			final AttributeValue existingValue = existingValueSupplier.get();
			if (existingValue != null) {
				index.removeFilterAttribute(attributeDefinition, allowedLocales, locale, existingValue.getValue(), entityPrimaryKey);
			}
			index.insertFilterAttribute(attributeDefinition, allowedLocales, locale, valueToInsert, entityPrimaryKey);
		}
		if (attributeDefinition.isSortable()) {
			final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ATTRIBUTE_SORT_INDEX);
			final AttributeValue existingValue = existingValueSupplier.get();
			if (existingValue != null) {
				index.removeSortAttribute(attributeDefinition, allowedLocales, locale, existingValue.getValue(), entityPrimaryKey);
			}
			index.insertSortAttribute(attributeDefinition, allowedLocales, locale, valueToInsert, entityPrimaryKey);
		}
	}

	/**
	 * This is auxiliary class that allows lazily fetch attribute container from persistent storage and remember it
	 * for potential future requests.
	 */
	@RequiredArgsConstructor
	class ExistingAttributeAccessor implements Supplier<AttributeValue> {
		private final int entityPrimaryKey;
		private final EntityIndexLocalMutationExecutor executor;
		private final AttributeKey affectedAttribute;
		private AtomicReference<AttributeValue> memoizedValue;

		@Override
		public AttributeValue get() {
			if (memoizedValue == null) {
				final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
				final AttributesStoragePart currentAttributes = ofNullable(affectedAttribute.getLocale())
					.map(it -> containerAccessor.getAttributeStorageContainer(entityPrimaryKey, it))
					.orElseGet(() -> containerAccessor.getAttributeStorageContainer(entityPrimaryKey));

				this.memoizedValue = new AtomicReference<>(
					Optional.of(currentAttributes)
						.map(it -> it.findAttribute(affectedAttribute))
						.filter(Droppable::exists)
						.orElse(null)
				);
			}
			return memoizedValue.get();
		}
	}

}
