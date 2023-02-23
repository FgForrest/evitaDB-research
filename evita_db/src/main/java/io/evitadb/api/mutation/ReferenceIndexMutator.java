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
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.EntityStorageContainerAccessor;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.IndexType;
import io.evitadb.index.price.model.internalId.PriceWithInternalIds;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import io.evitadb.storage.model.storageParts.entity.PricesStoragePart;
import io.evitadb.storage.model.storageParts.entity.ReferencesStoragePart;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.evitadb.api.utils.Assert.isTrue;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * This interface is used to co-locate reference mutating routines which are rather procedural and long to avoid excessive
 * amount of code in {@link EntityIndexLocalMutationExecutor}.
 *
 * References are indexed in two special indexes:
 *
 * ## Reference type entity index
 *
 * For each referenced entity TYPE (like brand, category and so on) there is special index that contains all attributes
 * that were used in combination with that type and instead of primary key of the entity, contain primary key of the
 * REFERENCED entity.
 *
 * This referenced entity primary key along with type leads us to the second type of the index:
 *
 * ## Referenced entity index
 *
 * For each referenced entity TYPE and PRIMARY KEY there is special index that contains all attributes that were used
 * in entities linked to this same referenced entity instance. This kind of index is optimal to use for queries that
 * try to list all entities of particular brand/category etc., because the index contains information just about that.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface ReferenceIndexMutator {

	Supplier<AttributeValue> NON_EXISTING_SUPPLIER = () -> null;

	/**
	 * Method allows to process all attribute mutations in referenced entities of the entity. Method uses
	 * {@link EntityIndexLocalMutationExecutor#updateAttributes(AttributeMutation, Function, Supplier, EntityIndex, boolean)}
	 * to do that.
	 */
	static void attributeUpdate(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex referenceTypeIndex,
		@Nullable EntityIndex referenceIndex,
		@Nonnull EntityReferenceContract<?> referenceKey,
		@Nonnull AttributeMutation attributeMutation
	) {
		final EntitySchema entitySchema = executor.getEntitySchema();
		final ReferenceSchema referenceSchema = entitySchema.getReference(referenceKey.getType());
		Assert.notNull(referenceSchema, "Reference of type `" + referenceKey.getType() + "` is not present in schema of `" + entitySchema.getName() + "` entity.");

		// use different existing attribute value accessor - attributes needs to be looked up in ReferencesStoragePart
		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ENTITY_INDEX);
		final Supplier<AttributeValue> existingValueAccessorFactory = new ExistingReferenceAttributeAccessor(
			executor, entityPrimaryKey, referenceKey, attributeMutation.getAttributeKey()
		);
		// we need to index referenced entity primary key into the reference type index for all attributes
		executor.executeWithDifferentPrimaryKeyToIndex(
			indexType -> {
				if (indexType == IndexType.ATTRIBUTE_SORT_INDEX) {
					// only sort indexes here target primary key of the main entity - we need monotonic row of all entity primary keys for this type
					return entityPrimaryKey;
				} else {
					// all other indexes target referenced entity primary key - we use them for looking up referenced type indexes (type + referenced entity key)
					return referenceKey.getPrimaryKey();
				}
			},
			() -> executor.updateAttributes(attributeMutation, referenceSchema::getAttribute, existingValueAccessorFactory, referenceTypeIndex, false)
		);

		if (referenceIndex != null) {
			// we need to index entity primary key into the referenced entity index for all attributes
			executor.updateAttributes(attributeMutation, referenceSchema::getAttribute, existingValueAccessorFactory, referenceIndex, false);
		}
	}

	/**
	 * Methods creates reference to the reference type and referenced entity indexes.
	 */
	static void referenceInsert(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex entityIndex,
		@Nonnull EntityIndex referenceTypeIndex,
		@Nullable EntityIndex referenceIndex,
		@Nonnull ReferenceContract entityReference
	) {
		// we need to index referenced entity primary key into the reference type index
		final int referencedPrimaryKey = entityReference.getReferencedEntity().getPrimaryKey();
		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.REFERENCE_INDEX);
		referenceTypeIndex.insertPrimaryKeyIfMissing(referencedPrimaryKey);

		// add facet to global index
		addFacetToIndex(entityIndex, entityReference.getReferencedEntity(), entityReference.getGroup(), executor, entityPrimaryKey);

		if (referenceIndex != null) {
			// we need to index entity primary key into the referenced entity index
			referenceIndex.insertPrimaryKeyIfMissing(entityPrimaryKey);
			addFacetToIndex(referenceIndex, entityReference.getReferencedEntity(), entityReference.getGroup(), executor, entityPrimaryKey);

			// we need to index all previously added global entity attributes and prices
			indexAllExistingData(executor, referenceIndex, entityPrimaryKey);
		}
	}

	/**
	 * Methods removes reference from the reference type and referenced entity indexes.
	 */
	static void referenceRemoval(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex entityIndex,
		@Nonnull EntityIndex referenceTypeIndex,
		@Nullable EntityIndex referenceIndex,
		@Nonnull EntityReferenceContract<?> referenceKey
	) {
		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ENTITY_INDEX);
		// we need to remove referenced entity primary key from the reference type index
		referenceTypeIndex.removePrimaryKey(referenceKey.getPrimaryKey());

		// remove facet from global and index
		removeFacetInIndex(entityIndex, referenceKey, executor, entityPrimaryKey);

		if (referenceIndex != null) {
			// we need to remove entity primary key from the referenced entity index
			referenceIndex.removePrimaryKey(entityPrimaryKey);

			// remove all attributes that are present on the reference relation
			final ReferencesStoragePart referencesStorageContainer = executor.getContainerAccessor().getReferencesStorageContainer(entityPrimaryKey);
			Assert.notNull(referencesStorageContainer, "References storage container for entity " + entityPrimaryKey + " was unexpectedly not found!");
			final ReferenceContract theReference = Arrays.stream(referencesStorageContainer.getReferences())
				.filter(Droppable::exists)
				.filter(it -> Objects.equals(it.getReferencedEntity(), referenceKey))
				.findFirst()
				.orElse(null);
			Assert.notNull(theReference, "Reference " + referenceKey + " for entity " + entityPrimaryKey + " was unexpectedly not found!");
			for (AttributeValue attributeValue : theReference.getAttributeValues()) {
				if (attributeValue.exists()) {
					attributeUpdate(
						executor, referenceTypeIndex, referenceIndex, referenceKey,
						new RemoveAttributeMutation(attributeValue.getKey())
					);
				}
			}

			// if referenced type index is empty remove it completely
			if (referenceTypeIndex.isEmpty()) {
				executor.removeIndex(referenceTypeIndex.getEntityIndexKey());
			}

			// remove all global entity attributes and prices
			removeAllExistingData(executor, referenceIndex, entityPrimaryKey);
		}
	}

	/**
	 * Method indexes all existing indexable data for passed entity / referenced entity combination in passed indexes.
	 */
	static void indexAllExistingData(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex targetIndex,
		int entityPrimaryKey
	) {
		indexFacets(executor, targetIndex, entityPrimaryKey);

		final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
		final EntityBodyStoragePart entityCnt = containerAccessor.getEntityStorageContainer(entityPrimaryKey, EntityExistence.MUST_EXIST);
		final AttributesStoragePart attributeCnt = containerAccessor.getAttributeStorageContainer(entityPrimaryKey);
		for (AttributeValue attribute : attributeCnt.getAttributes()) {
			if (attribute.exists()) {
				AttributeIndexMutator.executeAttributeUpsert(
					executor,
					attributeName -> executor.getEntitySchema().getAttribute(attributeName),
					NON_EXISTING_SUPPLIER,
					targetIndex,
					attribute.getKey(), attribute.getValue(), false
				);
			}
		}
		for (Locale locale : entityCnt.getAttributeLocales()) {
			final AttributesStoragePart localizedAttributeCnt = containerAccessor.getAttributeStorageContainer(entityPrimaryKey, locale);
			for (AttributeValue attribute : localizedAttributeCnt.getAttributes()) {
				if (attribute.exists()) {
					AttributeIndexMutator.executeAttributeUpsert(
						executor,
						attributeName -> executor.getEntitySchema().getAttribute(attributeName),
						NON_EXISTING_SUPPLIER,
						targetIndex,
						attribute.getKey(), attribute.getValue(), false
					);
				}
			}
		}
		final PricesStoragePart priceContainer = containerAccessor.getPriceStorageContainer(entityPrimaryKey);
		for (PriceWithInternalIds price : priceContainer.getPrices()) {
			if (price.exists()) {
				PriceIndexMutator.priceUpsert(
					executor, targetIndex,
					price.getPriceKey(),
					price.getInnerRecordId(),
					price.getValidity(),
					price.getPriceWithoutVat(),
					price.getPriceWithVat(),
					price.isSellable(),
					null,
					priceContainer.getPriceInnerRecordHandling(),
					PriceIndexMutator.createPriceProvider(price)
				);
			}
		}
	}

	/**
	 * Method indexes all facets data for passed entity in passed index.
	 */
	static void indexFacets(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex targetIndex,
		int entityPrimaryKey
	) {
		final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
		final ReferencesStoragePart referencesStorageContainer = containerAccessor.getReferencesStorageContainer(entityPrimaryKey);

		for (ReferenceContract reference : referencesStorageContainer.getReferences()) {
			final EntityReference referencedEntity = reference.getReferencedEntity();
			final GroupEntityReference groupReference = reference.getGroup();
			if (reference.exists() && isFacetedReference(referencedEntity, executor)) {
				targetIndex.addFacet(referencedEntity, groupReference, entityPrimaryKey);
			}
		}
	}

	/**
	 * Method removes all indexed data for passed entity / referenced entity combination in passed indexes.
	 */
	static void removeAllExistingData(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex targetIndex,
		int entityPrimaryKey
	) {
		removeFacets(executor, targetIndex, entityPrimaryKey);

		final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
		final EntityBodyStoragePart entityCnt = containerAccessor.getEntityStorageContainer(entityPrimaryKey, EntityExistence.MUST_EXIST);
		final AttributesStoragePart attributeCnt = containerAccessor.getAttributeStorageContainer(entityPrimaryKey);
		for (AttributeValue attribute : attributeCnt.getAttributes()) {
			if (attribute.exists()) {
				AttributeIndexMutator.executeAttributeRemoval(
					executor,
					attributeName -> executor.getEntitySchema().getAttribute(attributeName),
					() -> attribute,
					targetIndex,
					attribute.getKey(), false
				);
			}
		}
		for (Locale locale : entityCnt.getLocales()) {
			final AttributesStoragePart localizedAttributeCnt = containerAccessor.getAttributeStorageContainer(entityPrimaryKey, locale);
			for (AttributeValue attribute : localizedAttributeCnt.getAttributes()) {
				if (attribute.exists()) {
					AttributeIndexMutator.executeAttributeRemoval(
						executor,
						attributeName -> executor.getEntitySchema().getAttribute(attributeName),
						() -> attribute,
						targetIndex,
						attribute.getKey(), false
					);
				}
			}
		}
		final PricesStoragePart priceContainer = containerAccessor.getPriceStorageContainer(entityPrimaryKey);
		for (PriceContract price : priceContainer.getPrices()) {
			if (price.exists()) {
				PriceIndexMutator.priceRemove(
					executor, targetIndex,
					price.getPriceKey()
				);
			}
		}

		// if target index is empty, remove it completely
		if (targetIndex.isEmpty()) {
			executor.removeIndex(targetIndex.getEntityIndexKey());
		}
	}

	/**
	 * Method removes all facets for passed entity from passed index.
	 */
	static void removeFacets(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex targetIndex,
		int entityPrimaryKey
	) {
		final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
		final ReferencesStoragePart referencesStorageContainer = containerAccessor.getReferencesStorageContainer(entityPrimaryKey);

		for (ReferenceContract reference : referencesStorageContainer.getReferences()) {
			final EntityReference referencedEntity = reference.getReferencedEntity();
			if (reference.exists() && isFacetedReference(referencedEntity, executor)) {
				final Integer groupId = ofNullable(reference.getGroup()).filter(Droppable::exists).map(EntityReference::getPrimaryKey).orElse(null);
				targetIndex.removeFacet(referencedEntity, groupId, entityPrimaryKey);
			}
		}
	}

	/**
	 * Returns appropriate {@link EntityIndex} for passed `referencedEntity`. If the entity refers to the another Evita
	 * entity which has hierarchical structure {@link EntityIndexType#REFERENCED_HIERARCHY_NODE} is returned otherwise
	 * {@link EntityIndexType#REFERENCED_ENTITY} index is returned.
	 */
	static EntityIndex getReferencedEntityIndex(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityReferenceContract<?> referencedEntity
	) {
		final Serializable referencedEntityType = referencedEntity.getType();
		final ReferenceSchema referenceSchema = executor.getEntitySchema().getReferenceOrThrowException(referencedEntityType);
		final boolean referencesHierarchy;
		if (referenceSchema.isEntityTypeRelatesToEntity()) {
			final EntitySchema referencedEntitySchema = executor.getEntitySchema(referencedEntityType);
			isTrue(referencedEntitySchema != null, () -> new IllegalStateException("Referenced entity `" + referencedEntityType + "` schema was not found!"));
			referencesHierarchy = referencedEntitySchema.isWithHierarchy();
		} else {
			referencesHierarchy = false;
		}
		// in order to save memory the data are indexed either to hierarchical or referenced entity index
		final EntityIndexKey entityIndexKey;
		if (referencesHierarchy) {
			entityIndexKey = new EntityIndexKey(EntityIndexType.REFERENCED_HIERARCHY_NODE, referencedEntity);
		} else {
			entityIndexKey = new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY, referencedEntity);
		}
		return executor.getOrCreateIndex(entityIndexKey);
	}

	/**
	 * Method executes logic in `referenceIndexConsumer` in new specific type of {@link EntityIndex} of type
	 * {@link EntityIndexType#REFERENCED_ENTITY} for all entities that are currently referenced.
	 */
	static void executeWithReferenceIndexes(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull Consumer<EntityIndex> referenceIndexConsumer
	) {
		executeWithReferenceIndexes(executor, referenceIndexConsumer, referenceContract -> true);
	}

	/**
	 * Method executes logic in `referenceIndexConsumer` in new specific type of {@link EntityIndex} of type
	 * {@link EntityIndexType#REFERENCED_ENTITY} for all entities that are currently referenced.
	 */
	static void executeWithReferenceIndexes(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull Consumer<EntityIndex> referenceIndexConsumer,
		@Nonnull Predicate<ReferenceContract> referencePredicate
	) {
		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ENTITY_INDEX);
		final ReferencesStoragePart referencesStorageContainer = executor.getContainerAccessor().getReferencesStorageContainer(entityPrimaryKey);
		for (ReferenceContract reference : referencesStorageContainer.getReferences()) {
			if (reference.exists() && isIndexed(reference) && referencePredicate.test(reference)) {
				final EntityIndex targetIndex = getReferencedEntityIndex(executor, reference.getReferencedEntity());
				referenceIndexConsumer.accept(targetIndex);
			}
		}
	}

	/**
	 * Returns true if reference schema is configured and indexed.
	 */
	static boolean isIndexed(ReferenceContract reference) {
		final ReferenceSchema referenceSchema = reference.getReferenceSchema();
		return referenceSchema != null && referenceSchema.isIndexed();
	}

	/**
	 * Registers new facet for the passed `referencedEntity` and `entityPrimaryKey` but only if the referenced entity
	 * type is marked as `faceted` in reference schema.
	 */
	static void addFacetToIndex(
		@Nonnull EntityIndex index,
		@Nonnull EntityReference referencedEntity,
		@Nullable GroupEntityReference groupEntityReference,
		@Nonnull EntityIndexLocalMutationExecutor executor,
		int entityPrimaryKey
	) {
		if (isFacetedReference(referencedEntity, executor)) {
			index.addFacet(
				referencedEntity,
				groupEntityReference,
				entityPrimaryKey
			);
		}
	}

	/**
	 * Sets group in existing facet in reference to the passed `referencedEntity` and `entityPrimaryKey` but only if
	 * the referenced entity type is marked as `faceted` in reference schema.
	 */
	static void setFacetGroupInIndex(
		@Nonnull EntityIndex index,
		@Nonnull EntityReference referencedEntity,
		@Nonnull GroupEntityReference groupEntityReference,
		@Nonnull EntityIndexLocalMutationExecutor executor,
		int entityPrimaryKey
	) {
		if (isFacetedReference(referencedEntity, executor)) {
			final ReferenceContract existingReference = executor.getContainerAccessor().getReferencesStorageContainer(entityPrimaryKey)
				.findReferenceOrThrowException(referencedEntity);
			index.removeFacet(
				existingReference.getReferencedEntity(),
				ofNullable(existingReference.getGroup()).filter(Droppable::exists).map(EntityReference::getPrimaryKey).orElse(null),
				entityPrimaryKey
			);
			index.addFacet(
				referencedEntity,
				groupEntityReference,
				entityPrimaryKey
			);
		}
	}

	/**
	 * Removes existing facet for the passed `referencedEntity` and `entityPrimaryKey` but only if the referenced entity
	 * type is marked as `faceted` in reference schema.
	 */
	static void removeFacetInIndex(
		@Nonnull EntityIndex index,
		@Nonnull EntityReferenceContract<?> referencedEntity,
		@Nonnull EntityIndexLocalMutationExecutor executor,
		int entityPrimaryKey
	) {
		if (isFacetedReference(referencedEntity, executor)) {
			final ReferenceContract existingReference = executor.getContainerAccessor().getReferencesStorageContainer(entityPrimaryKey)
				.findReferenceOrThrowException(referencedEntity);
			index.removeFacet(
				existingReference.getReferencedEntity(),
				ofNullable(existingReference.getGroup()).filter(Droppable::exists).map(EntityReference::getPrimaryKey).orElse(null),
				entityPrimaryKey
			);
			if (index.isEmpty()) {
				// if the result index is empty, we should drop track of it in global entity index
				// it was probably registered before, and it has been emptied by the consumer lambda just now
				executor.removeIndex(index.getEntityIndexKey());
			}
		}
	}

	/**
	 * Removes group in existing facet in reference to the passed `referencedEntity` and `entityPrimaryKey` but only if
	 * the referenced entity type is marked as `faceted` in reference schema.
	 */
	static void removeFacetGroupInIndex(
		@Nonnull EntityIndex index,
		@Nonnull EntityReference referencedEntity,
		@Nonnull EntityIndexLocalMutationExecutor executor,
		int entityPrimaryKey
	) {
		if (isFacetedReference(referencedEntity, executor)) {
			final ReferenceContract existingReference = executor.getContainerAccessor().getReferencesStorageContainer(entityPrimaryKey)
				.findReferenceOrThrowException(referencedEntity);
			isTrue(
				existingReference.getGroup() != null && existingReference.getGroup().exists(),
				() -> new IllegalStateException("Group is expected to be non-null when RemoveReferenceGroupMutation is about to be executed.")
			);
			index.removeFacet(
				existingReference.getReferencedEntity(),
				existingReference.getGroup().getPrimaryKey(),
				entityPrimaryKey
			);
			index.addFacet(referencedEntity, null, entityPrimaryKey);
		}
	}

	/**
	 * Returns TRUE if `referencedEntity` is marked as `faceted` in the entity schema.
	 */
	static boolean isFacetedReference(
		@Nonnull EntityReferenceContract<?> referencedEntity,
		@Nonnull EntityIndexLocalMutationExecutor executor
	) {
		final ReferenceSchema referenceSchema = executor.getEntitySchema().getReferenceOrThrowException(referencedEntity.getType());
		return referenceSchema.isFaceted();
	}

	/**
	 * This implementation of attribute accessor looks up for attribute in {@link ReferencesStoragePart}.
	 */
	@Data
	class ExistingReferenceAttributeAccessor implements Supplier<AttributeValue> {
		private final EntityIndexLocalMutationExecutor executor;
		private final int entityPrimaryKey;
		private final EntityReferenceContract<?> referenceKey;
		private final AttributeKey affectedAttribute;
		private AtomicReference<AttributeValue> memoizedValue;

		@Override
		public AttributeValue get() {
			if (memoizedValue == null) {
				final EntityStorageContainerAccessor containerAccessor = executor.getContainerAccessor();
				final ReferencesStoragePart referencesStorageContainer = containerAccessor.getReferencesStorageContainer(entityPrimaryKey);

				this.memoizedValue = new AtomicReference<>(
					of(referencesStorageContainer)
						.flatMap(it -> it.getReferencesAsCollection().stream().filter(ref -> Objects.equals(ref.getReferencedEntity(), referenceKey)).findFirst())
						.map(it -> ofNullable(affectedAttribute.getLocale()).map(loc -> it.getAttributeValue(affectedAttribute.getAttributeName(), loc)).orElseGet(() -> it.getAttributeValue(affectedAttribute.getAttributeName())))
						.filter(Droppable::exists)
						.orElse(null)
				);
			}
			return memoizedValue.get();
		}
	}
}
