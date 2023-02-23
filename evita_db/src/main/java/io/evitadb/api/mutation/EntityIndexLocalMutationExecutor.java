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

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataValue;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.*;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.LocalMutationExecutor;
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.RemoveAssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.UpsertAssociatedDataMutation;
import io.evitadb.api.data.mutation.attribute.ApplyDeltaAttributeMutation;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation;
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.data.mutation.entity.RemoveHierarchicalPlacementMutation;
import io.evitadb.api.data.mutation.entity.SetHierarchicalPlacementMutation;
import io.evitadb.api.data.mutation.price.PriceInnerRecordHandlingMutation;
import io.evitadb.api.data.mutation.price.PriceMutation;
import io.evitadb.api.data.mutation.price.RemovePriceMutation;
import io.evitadb.api.data.mutation.price.UpsertPriceMutation;
import io.evitadb.api.data.mutation.reference.*;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.data.structure.WritableEntityStorageContainerAccessor;
import io.evitadb.api.mutation.AttributeIndexMutator.*;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.index.*;
import io.evitadb.index.price.model.internalId.PriceWithInternalIds;
import io.evitadb.storage.model.storageParts.entity.PricesStoragePart;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.function.*;

import static io.evitadb.api.mutation.AssociatedDataIndexMutator.executeAssociatedDataRemoval;
import static io.evitadb.api.mutation.AssociatedDataIndexMutator.executeAssociatedDataUpsert;
import static io.evitadb.api.mutation.AttributeIndexMutator.*;
import static io.evitadb.api.mutation.HierarchyPlacementMutator.removeHierarchyPlacement;
import static io.evitadb.api.mutation.HierarchyPlacementMutator.setHierarchyPlacement;
import static io.evitadb.api.mutation.ReferenceIndexMutator.*;
import static io.evitadb.api.utils.Assert.isTrue;

/**
 * This class applies changes in {@link LocalMutation} to one or multiple {@link EntityIndex} so that changes are reflected
 * in next filtering / sorting query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class EntityIndexLocalMutationExecutor implements LocalMutationExecutor {
	@Getter private final WritableEntityStorageContainerAccessor containerAccessor;
	private final LinkedList<ToIntFunction<IndexType>> entityPrimaryKey = new LinkedList<>();
	private final EntityIndexMaintainer indexCreatingAccessor;
	private final Supplier<EntitySchema> schemaAccessor;
	private final Function<Serializable, EntitySchema> otherEntitiesSchemaAccessor;

	public EntityIndexLocalMutationExecutor(
		@Nonnull WritableEntityStorageContainerAccessor containerAccessor,
		int entityPrimaryKey,
		@Nonnull EntityIndexMaintainer indexCreatingAccessor,
		@Nonnull Supplier<EntitySchema> schemaAccessor,
		@Nonnull Function<Serializable, EntitySchema> otherEntitiesSchemaAccessor
	) {
		this.containerAccessor = containerAccessor;
		this.entityPrimaryKey.add(anyType -> entityPrimaryKey);
		this.indexCreatingAccessor = indexCreatingAccessor;
		this.schemaAccessor = schemaAccessor;
		this.otherEntitiesSchemaAccessor = otherEntitiesSchemaAccessor;
	}

	/**
	 * Method inserts language for entity if entity lacks information about used language.
	 */
	public void upsertEntityLanguage(@Nonnull EntityIndex index, @Nonnull Locale locale) {
		final EntitySchema schema = getEntitySchema();
		final Set<Locale> allowedLocales = schema.getLocales();
		isTrue(
			allowedLocales.contains(locale) || schema.getEvolutionMode().contains(EvolutionMode.ADDING_LOCALES),
			() -> new IllegalArgumentException("Locale " + locale + " is not allowed by the schema!")
		);
		index.upsertLanguage(locale, getPrimaryKeyToIndex(IndexType.ENTITY_INDEX));
	}

	/**
	 * Method removes language for entity.
	 */
	public void removeEntityLanguage(@Nonnull EntityIndex index, @Nonnull Locale locale) {
		index.removeLanguage(locale, getPrimaryKeyToIndex(IndexType.ENTITY_INDEX));
	}

	/**
	 * Removes entity itself from indexes.
	 */
	public void removeEntity(int primaryKey) {
		final EntityIndex index = getOrCreateIndex(new EntityIndexKey(EntityIndexType.GLOBAL));
		index.removePrimaryKey(primaryKey);
	}

	@Override
	public void applyMutation(@Nonnull LocalMutation<?, ?> localMutation) {
		final Class<?> affectsProperty = localMutation.affects();
		final EntityIndex index = getOrCreateIndex(new EntityIndexKey(EntityIndexType.GLOBAL));
		final int theEntityPrimaryKey = getPrimaryKeyToIndex(IndexType.ENTITY_INDEX);

		index.insertPrimaryKeyIfMissing(theEntityPrimaryKey);

		if (affectsProperty.equals(Prices.class)) {
			final PriceInnerRecordHandlingMutation priceHandlingMutation = (PriceInnerRecordHandlingMutation) localMutation;
			updatePriceHandlingForEntity(priceHandlingMutation, index);
		} else if (affectsProperty.equals(PriceContract.class)) {
			final PriceMutation priceMutation = (PriceMutation) localMutation;
			final Consumer<EntityIndex> priceUpdateApplicator = theIndex -> updatePriceIndex(priceMutation, theIndex);
			if (priceMutation instanceof RemovePriceMutation) {
				// removal must first occur on the reduced indexes, because they consult the super index
				executeWithReferenceIndexes(this, priceUpdateApplicator);
				priceUpdateApplicator.accept(index);
			} else {
				// upsert must first occur on super index, because reduced indexed rely on information in super index
				priceUpdateApplicator.accept(index);
				executeWithReferenceIndexes(this, priceUpdateApplicator);
			}
		} else if (affectsProperty.equals(HierarchicalPlacementContract.class)) {
			//noinspection unchecked
			updateHierarchyPlacement((LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract>) localMutation, index);
		} else if (affectsProperty.equals(ReferenceContract.class)) {
			final ReferenceMutation<?> referenceMutation = (ReferenceMutation<?>) localMutation;
			final ReferenceSchema referenceSchema = getEntitySchema().getReferenceOrThrowException(referenceMutation.getReferenceKey().getType());
			if (referenceSchema.isIndexed()) {
				updateReferences(referenceMutation, index);
				executeWithReferenceIndexes(
					this,
					referenceIndex -> updateReferencesInReferenceIndex(referenceMutation, referenceIndex),
					// avoid indexing the referenced index that got updated by updateReferences method
					referenceContract -> !referenceMutation.getReferenceKey().equals(referenceContract.getReferencedEntity())
				);
			}
		} else if (affectsProperty.equals(AttributeValue.class)) {
			final AttributeMutation attributeMutation = (AttributeMutation) localMutation;
			final ExistingAttributeAccessor existingAttributeAccessor = new ExistingAttributeAccessor(theEntityPrimaryKey, this, attributeMutation.getAttributeKey());
			final BiConsumer<Boolean, EntityIndex> attributeUpdateApplicator = (updateLanguage, targetIndex) -> updateAttributes(
				attributeMutation,
				it -> getEntitySchema().getAttribute(it),
				existingAttributeAccessor,
				targetIndex,
				updateLanguage
			);
			attributeUpdateApplicator.accept(true, index);
			executeWithReferenceIndexes(this, entityIndex -> attributeUpdateApplicator.accept(true, entityIndex), Droppable::exists);
		} else if (affectsProperty.equals(AssociatedDataValue.class)) {
			final AssociatedDataMutation associatedDataMutation = (AssociatedDataMutation) localMutation;
			updateAssociatedData(associatedDataMutation, index);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + localMutation.getClass());
		}
	}

	/**
	 * Method removes existing index from the existing set.
	 */
	public void removeIndex(EntityIndexKey entityIndexKey) {
		indexCreatingAccessor.removeIndex(entityIndexKey);
	}

	/**
	 * Method returns existing index or creates new and adds it to the changed set of indexes that needs persisting.
	 */
	public EntityIndex getIndexIfExists(EntityIndexKey entityIndexKey) {
		return indexCreatingAccessor.getIndexIfExists(entityIndexKey);
	}

	/*
		FRIENDLY METHODS
	 */

	/**
	 * Returns current entity schema.
	 */
	@Nonnull
	EntitySchema getEntitySchema() {
		return schemaAccessor.get();
	}

	/**
	 * Returns current entity schema.
	 */
	@Nullable
	EntitySchema getEntitySchema(Serializable entityType) {
		return otherEntitiesSchemaAccessor.apply(entityType);
	}

	/**
	 * Returns primary key that should be indexed by certain {@link IndexType}. Argument of index type is necessary
	 * because for example for {@link EntityIndexType#REFERENCED_ENTITY_TYPE} we need to index referenced entity id for
	 * {@link IndexType#ATTRIBUTE_FILTER_INDEX} and {@link IndexType#ATTRIBUTE_UNIQUE_INDEX}, but entity
	 * id for {@link IndexType#ATTRIBUTE_SORT_INDEX}.
	 */
	int getPrimaryKeyToIndex(IndexType indexType) {
		isTrue(!entityPrimaryKey.isEmpty(), () -> new IllegalStateException("Should not ever happen."));
		//noinspection ConstantConditions
		return entityPrimaryKey.peek().applyAsInt(indexType);
	}

	/**
	 * Method allows overloading default implementation that returns entity primary key for all {@link IndexType} values.
	 */
	void executeWithDifferentPrimaryKeyToIndex(ToIntFunction<IndexType> entityPrimaryKeyResolver, Runnable runnable) {
		try {
			this.entityPrimaryKey.push(entityPrimaryKeyResolver);
			runnable.run();
		} finally {
			this.entityPrimaryKey.pop();
		}
	}

	/**
	 * Method returns existing index or creates new and adds it to the changed set of indexes that needs persisting.
	 */
	EntityIndex getOrCreateIndex(EntityIndexKey entityIndexKey) {
		return indexCreatingAccessor.getOrCreateIndex(entityIndexKey);
	}

	/**
	 * Method processes all mutations that targets entity attributes - e.g. {@link AttributeMutation}.
	 */
	void updateAttributes(@Nonnull AttributeMutation attributeMutation, @Nonnull Function<String, AttributeSchema> schemaProvider, @Nonnull Supplier<AttributeValue> existingValueSupplier, @Nonnull EntityIndex index, boolean updateLanguage) {
		final AttributeKey affectedAttribute = attributeMutation.getAttributeKey();

		if (attributeMutation instanceof UpsertAttributeMutation) {
			final Serializable attributeValue = ((UpsertAttributeMutation) attributeMutation).getAttributeValue();
			executeAttributeUpsert(
				this, schemaProvider, existingValueSupplier,
				index, affectedAttribute, attributeValue, updateLanguage
			);
		} else if (attributeMutation instanceof RemoveAttributeMutation) {
			executeAttributeRemoval(
				this, schemaProvider, existingValueSupplier,
				index, affectedAttribute, updateLanguage
			);
		} else if (attributeMutation instanceof ApplyDeltaAttributeMutation) {
			final Number attributeValue = ((ApplyDeltaAttributeMutation) attributeMutation).getAttributeValue();
			executeAttributeDelta(
				this, schemaProvider, existingValueSupplier,
				index, affectedAttribute, attributeValue
			);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + attributeMutation.getClass());
		}
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * Method processes all mutations that target entity attributes - e.g. {@link AttributeMutation}.
	 */
	private void updateAssociatedData(@Nonnull AssociatedDataMutation associatedDataMutation, @Nonnull EntityIndex index) {
		final AssociatedDataKey affectedAssociatedData = associatedDataMutation.getAssociatedDataKey();
		if (associatedDataMutation instanceof UpsertAssociatedDataMutation) {
			executeAssociatedDataUpsert(this, index, affectedAssociatedData);
		} else if (associatedDataMutation instanceof RemoveAssociatedDataMutation) {
			executeAssociatedDataRemoval(this, index, affectedAssociatedData);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + associatedDataMutation.getClass());
		}
	}

	/**
	 * Method processes all mutations that target entity references - e.g. {@link ReferenceMutation}. This method
	 * alters contents of the primary indexes - i.e. global index, reference type and referenced entity index for
	 * the particular referenced entity.
	 */
	private void updateReferences(@Nonnull ReferenceMutation<?> referenceMutation, @Nonnull EntityIndex entityIndex) {
		final EntityReferenceContract<?> referenceKey = referenceMutation.getReferenceKey();
		final int theEntityPrimaryKey = getPrimaryKeyToIndex(IndexType.ENTITY_INDEX);

		if (referenceMutation instanceof UpsertReferenceGroupMutation) {
			final EntityIndex referenceIndex = getReferencedEntityIndex(this, referenceKey);
			setFacetGroupInIndex(
				entityIndex, referenceMutation.getReferenceKey(),
				((UpsertReferenceGroupMutation) referenceMutation).getGroupReference(),
				this,
				theEntityPrimaryKey
			);
			setFacetGroupInIndex(
				referenceIndex, referenceMutation.getReferenceKey(),
				((UpsertReferenceGroupMutation) referenceMutation).getGroupReference(),
				this,
				theEntityPrimaryKey
			);
		} else if (referenceMutation instanceof RemoveReferenceGroupMutation) {
			final EntityIndex referenceIndex = getReferencedEntityIndex(this, referenceKey);
			removeFacetGroupInIndex(
				entityIndex,
				referenceMutation.getReferenceKey(),
				this,
				theEntityPrimaryKey
			);
			removeFacetGroupInIndex(
				referenceIndex,
				referenceMutation.getReferenceKey(),
				this,
				theEntityPrimaryKey
			);
		} else if (referenceMutation instanceof ReferenceAttributesUpdateMutation) {
			final AttributeMutation attributeMutation = ((ReferenceAttributesUpdateMutation) referenceMutation).getAttributeMutation();
			final EntityIndex referenceTypeIndex = getOrCreateIndex(new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, referenceKey.getType()));
			final EntityIndex referenceIndex = getReferencedEntityIndex(this, referenceKey);
			attributeUpdate(
				this, referenceTypeIndex, referenceIndex, referenceMutation.getReferenceKey(), attributeMutation
			);
		} else if (referenceMutation instanceof InsertReferenceMutation) {
			final ReferenceContract createdReference = ((InsertReferenceMutation) referenceMutation).getCreatedReference();
			final EntityIndex referenceTypeIndex = getOrCreateIndex(new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, referenceKey.getType()));
			final EntityIndex referenceIndex = getReferencedEntityIndex(this, referenceKey);
			referenceInsert(
				this, entityIndex, referenceTypeIndex, referenceIndex, createdReference
			);
		} else if (referenceMutation instanceof RemoveReferenceMutation) {
			final EntityIndexKey referencedTypeIndexKey = new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, referenceKey.getType());
			final EntityIndex referenceTypeIndex = getOrCreateIndex(referencedTypeIndexKey);
			final EntityIndex referenceIndex = getReferencedEntityIndex(this, referenceKey);
			referenceRemoval(
				this, entityIndex, referenceTypeIndex, referenceIndex, referenceKey
			);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + referenceMutation.getClass());
		}
	}

	/**
	 * Method processes all mutations that target entity references - e.g. {@link ReferenceMutation}. This method
	 * alters contents of the secondary indexes - i.e. all referenced entity indexes that are used in the main entity
	 * except the referenced entity index that directly connects to {@link ReferenceMutation#getReferenceKey()} because
	 * this is altered in {@link #updateReferences(ReferenceMutation, EntityIndex)} method.
	 */
	private void updateReferencesInReferenceIndex(@Nonnull ReferenceMutation<?> referenceMutation, @Nonnull EntityIndex targetIndex) {
		final EntityIndexType targetIndexType = targetIndex.getEntityIndexKey().getType();
		final int theEntityPrimaryKey;
		if (targetIndexType == EntityIndexType.REFERENCED_HIERARCHY_NODE) {
			theEntityPrimaryKey = getPrimaryKeyToIndex(IndexType.HIERARCHY_INDEX);
		} else if (targetIndexType == EntityIndexType.REFERENCED_ENTITY) {
			theEntityPrimaryKey = getPrimaryKeyToIndex(IndexType.REFERENCE_INDEX);
		} else {
			throw new IllegalStateException("Unexpected type of index: " + targetIndexType);
		}
		if (referenceMutation instanceof UpsertReferenceGroupMutation) {
			setFacetGroupInIndex(
				targetIndex, referenceMutation.getReferenceKey(),
				((UpsertReferenceGroupMutation) referenceMutation).getGroupReference(),
				this,
				theEntityPrimaryKey
			);
		} else if (referenceMutation instanceof RemoveReferenceGroupMutation) {
			removeFacetGroupInIndex(
				targetIndex,
				referenceMutation.getReferenceKey(),
				this,
				theEntityPrimaryKey
			);
		} else if (referenceMutation instanceof ReferenceAttributesUpdateMutation) {
			// do nothing - attributes are not indexed in hierarchy index
		} else if (referenceMutation instanceof InsertReferenceMutation) {
			final ReferenceContract createdReference = ((InsertReferenceMutation) referenceMutation).getCreatedReference();
			addFacetToIndex(
				targetIndex,
				createdReference.getReferencedEntity(),
				createdReference.getGroup(),
				this,
				theEntityPrimaryKey
			);
		} else if (referenceMutation instanceof RemoveReferenceMutation) {
			removeFacetInIndex(
				targetIndex,
				referenceMutation.getReferenceKey(),
				this,
				theEntityPrimaryKey
			);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + referenceMutation.getClass());
		}
	}

	/**
	 * Method switches inner handling strategy for the entity - e.g. {@link PriceInnerRecordHandlingMutation}
	 */
	private void updatePriceHandlingForEntity(
		@Nonnull PriceInnerRecordHandlingMutation priceHandlingMutation,
		@Nonnull EntityIndex index
	) {
		final PricesStoragePart priceStorageContainer = getContainerAccessor().getPriceStorageContainer(getPrimaryKeyToIndex(IndexType.PRICE_INDEX));
		final PriceInnerRecordHandling originalInnerRecordHandling = priceStorageContainer.getPriceInnerRecordHandling();
		final PriceInnerRecordHandling newPriceInnerRecordHandling = priceHandlingMutation.getPriceInnerRecordHandling();

		if (originalInnerRecordHandling != newPriceInnerRecordHandling) {

			final Consumer<EntityIndex> pricesRemoval = theIndex -> {
				for (PriceWithInternalIds price : priceStorageContainer.getPrices()) {
					PriceIndexMutator.priceRemove(
						this, theIndex, price.getPriceKey(),
						price,
						originalInnerRecordHandling
					);
				}
			};

			final Consumer<EntityIndex> pricesInsertion = theIndex -> {
				for (PriceWithInternalIds price : priceStorageContainer.getPrices()) {
					PriceIndexMutator.priceUpsert(
						this, theIndex, price.getPriceKey(),
						price.getInnerRecordId(),
						price.getValidity(),
						price.getPriceWithoutVat(),
						price.getPriceWithVat(),
						price.isSellable(),
						null,
						newPriceInnerRecordHandling,
						PriceIndexMutator.createPriceProvider(price)
					);
				}
			};

			// first remove data from reduced indexes
			executeWithReferenceIndexes(this, pricesRemoval);

			// now we can safely remove the data from super index
			pricesRemoval.accept(index);

			// next we need to add data to super index first
			pricesInsertion.accept(index);

			// and then we can add data to reduced indexes
			executeWithReferenceIndexes(this, pricesInsertion);
		}
	}

	/**
	 * Method processes all mutations that targets entity prices - e.g. {@link PriceMutation}.
	 */
	private void updatePriceIndex(@Nonnull PriceMutation priceMutation, @Nonnull EntityIndex index) {
		final PriceKey priceKey = priceMutation.getPriceKey();

		if (priceMutation instanceof UpsertPriceMutation) {
			final UpsertPriceMutation upsertPriceMutation = (UpsertPriceMutation) priceMutation;
			final int theEntityPrimaryKey = this.getPrimaryKeyToIndex(IndexType.PRICE_INDEX);
			PriceIndexMutator.priceUpsert(
				this, index, theEntityPrimaryKey, priceKey,
				upsertPriceMutation.getInnerRecordId(),
				upsertPriceMutation.getValidity(),
				upsertPriceMutation.getPriceWithoutVat(),
				upsertPriceMutation.getPriceWithVat(),
				upsertPriceMutation.isSellable(),
				(thePriceKey, theInnerRecordId) -> containerAccessor.findExistingInternalIds(theEntityPrimaryKey, thePriceKey, theInnerRecordId)
			);
		} else if (priceMutation instanceof RemovePriceMutation) {
			PriceIndexMutator.priceRemove(
				this, index, priceKey
			);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + priceMutation.getClass());
		}
	}

	/**
	 * Method processes all mutations that targets hierachy placement - e.g. {@link SetHierarchicalPlacementMutation}
	 * and {@link RemoveHierarchicalPlacementMutation}.
	 */
	private void updateHierarchyPlacement(LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract> hierarchyMutation, EntityIndex index) {
		if (hierarchyMutation instanceof SetHierarchicalPlacementMutation) {
			final SetHierarchicalPlacementMutation setMutation = (SetHierarchicalPlacementMutation) hierarchyMutation;
			setHierarchyPlacement(
				this, index,
				getPrimaryKeyToIndex(IndexType.HIERARCHY_INDEX),
				setMutation.getParentPrimaryKey(),
				setMutation.getOrderAmongSiblings()
			);
		} else if (hierarchyMutation instanceof RemoveHierarchicalPlacementMutation) {
			removeHierarchyPlacement(
				this, index,
				getPrimaryKeyToIndex(IndexType.HIERARCHY_INDEX)
			);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + hierarchyMutation.getClass());
		}
	}

}