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
import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.LocalMutationExecutor;
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.price.PriceMutation;
import io.evitadb.api.data.mutation.reference.ReferenceAttributesUpdateMutation;
import io.evitadb.api.data.mutation.reference.ReferenceMutation;
import io.evitadb.api.data.structure.AbstractEntityStorageContainerAccessor;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.data.structure.WritableEntityStorageContainerAccessor;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.price.model.internalId.MinimalPriceInternalIdContainer;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;
import io.evitadb.storage.model.storageParts.entity.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Evita DB organizes entity data in storage in following way:
 *
 * - basic entity data, set of locales, set of associated data keys is stored in {@link EntityBodyStoragePart}
 * - attributes are divided into several groups and stored in multiple {@link AttributesStoragePart}
 * - global attributes (i.e. language agnostic)
 * - localized attributes (i.e. tied to specific {@link Locale})
 * - associated data are stored one by one using {@link AssociatedDataStoragePart}
 * - prices are all stored in single container {@link PricesStoragePart} - even if they can be requested
 * price list by price list
 * - references are all stored in single container {@link ReferencesStoragePart} - even if they can be requested
 * by different entity types
 *
 * Containers are modified separately and updated only when really changes. When reading containers - only minimal set
 * of containers that fulfills the requested query is really read from the storage.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
public class ContainerizedLocalMutationExecutor extends AbstractEntityStorageContainerAccessor implements LocalMutationExecutor, WritableEntityStorageContainerAccessor {
	public static final String ERROR_SAME_KEY_EXPECTED = "Expected same primary key here!";
	private final EntityExistence requiresExisting;
	private final int entityPrimaryKey;
	private final Supplier<EntitySchema> schemaAccessor;
	protected EntityBodyStoragePart entityContainer;
	protected PricesStoragePart pricesContainer;
	protected ReferencesStoragePart referencesStorageContainer;
	protected AttributesStoragePart globalAttributesStorageContainer;
	protected Map<Locale, AttributesStoragePart> languageSpecificAttributesContainer;
	protected Map<AssociatedDataKey, AssociatedDataStoragePart> associatedDataContainers;
	private Map<PriceKey, Integer> assignedInternalPriceIdIndex;

	public ContainerizedLocalMutationExecutor(StorageContainerBuffer storageContainerBuffer, int entityPrimaryKey, EntityExistence requiresExisting, Supplier<EntitySchema> schemaAccessor) {
		super(storageContainerBuffer, schemaAccessor);
		this.entityPrimaryKey = entityPrimaryKey;
		this.entityContainer = getEntityStorageContainer(entityPrimaryKey, requiresExisting);
		this.requiresExisting = requiresExisting;
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void applyMutation(@Nonnull LocalMutation<?, ?> localMutation) {
		final Class<?> affectsProperty = localMutation.affects();
		if (affectsProperty.equals(Prices.class)) {
			//noinspection unchecked
			updatePrices((LocalMutation<Prices, PriceKey>) localMutation);
		} else if (affectsProperty.equals(PriceContract.class)) {
			updatePriceIndex((PriceMutation) localMutation);
		} else if (affectsProperty.equals(HierarchicalPlacementContract.class)) {
			//noinspection unchecked
			updateHierarchyPlacement((LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract>) localMutation);
		} else if (affectsProperty.equals(ReferenceContract.class)) {
			updateReferences(localMutation);
		} else if (affectsProperty.equals(AttributeValue.class)) {
			updateAttributes((AttributeMutation) localMutation);
		} else if (affectsProperty.equals(AssociatedDataValue.class)) {
			updateAssociatedData((AssociatedDataMutation) localMutation);
		} else {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Unknown mutation: " + localMutation.getClass());
		}
	}

	/**
	 * Returns stream of containers that were touched and modified by applying mutations. Existing containers are
	 * automatically fetched from the underlying storage and modified, new containers are created on the fly.
	 */
	public Stream<? extends io.evitadb.storage.model.storageParts.EntityStoragePart> getChangedEntityStorageParts() {
		final EntityBodyStoragePart entityStorageContainer = ofNullable(this.entityContainer)
			// if entity represents first version we need to forcefully create entity container object
			.orElseGet(() -> new EntityBodyStoragePart(entityPrimaryKey));

		// now return all affected containers
		return Stream.of(
				Stream.of(
					entityStorageContainer,
					this.pricesContainer,
					this.globalAttributesStorageContainer,
					this.referencesStorageContainer
				),
				ofNullable(this.languageSpecificAttributesContainer).stream().flatMap(it -> it.values().stream()),
				ofNullable(this.associatedDataContainers).stream().flatMap(it -> it.values().stream())
			)
			.flatMap(it -> it)
			.filter(Objects::nonNull)
			/* return only parts that have been changed */
			.filter(io.evitadb.storage.model.storageParts.EntityStoragePart::isDirty);
	}

	@Override
	public void registerAssignedPriceId(int entityPrimaryKey, @Nonnull PriceKey priceKey, @Nullable Integer innerRecordId, @Nonnull PriceInternalIdContainer priceId) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		if (assignedInternalPriceIdIndex == null) {
			assignedInternalPriceIdIndex = new HashMap<>();
		}
		assignedInternalPriceIdIndex.compute(
			priceKey,
			(thePriceKey, existingInternalPriceId) -> {
				final Integer newPriceId = Objects.requireNonNull(priceId.getInternalPriceId());
				Assert.isTrue(
					existingInternalPriceId == null || Objects.equals(existingInternalPriceId, newPriceId),
					() -> new IllegalStateException("Attempt to change already assigned price id!")
				);
				return newPriceId;
			}
		);
	}

	@Nonnull
	@Override
	public PriceInternalIdContainer findExistingInternalIds(int entityPrimaryKey, @Nonnull PriceKey priceKey, @Nullable Integer innerRecordId) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		Integer internalPriceId = assignedInternalPriceIdIndex == null ? null : assignedInternalPriceIdIndex.get(priceKey);
		if (internalPriceId == null) {
			final PricesStoragePart priceStorageContainer = getPriceStorageContainer(entityPrimaryKey);
			final PriceInternalIdContainer existingInternalIds = priceStorageContainer.findExistingInternalIds(priceKey);
			return new MinimalPriceInternalIdContainer(existingInternalIds.getInternalPriceId());
		} else {
			return new MinimalPriceInternalIdContainer(internalPriceId);
		}
	}

	/*
		PROTECTED METHODS
	 */

	@Override
	protected EntityBodyStoragePart getCachedEntityStorageContainer(int entityPrimaryKey) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		return this.entityContainer;
	}

	@Override
	protected EntityBodyStoragePart cacheEntityStorageContainer(int entityPrimaryKey, @Nonnull EntityBodyStoragePart entityStorageContainer) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		this.entityContainer = entityStorageContainer;
		return this.entityContainer;
	}

	@Override
	protected AttributesStoragePart getCachedAttributeStorageContainer(int entityPrimaryKey) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		return this.globalAttributesStorageContainer;
	}

	@Override
	protected AttributesStoragePart cacheAttributeStorageContainer(int entityPrimaryKey, @Nonnull AttributesStoragePart attributesStorageContainer) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		this.globalAttributesStorageContainer = attributesStorageContainer;
		return this.globalAttributesStorageContainer;
	}

	@Override
	protected Map<Locale, AttributesStoragePart> getOrCreateCachedLocalizedAttributesStorageContainer(int entityPrimaryKey, @Nonnull Locale locale) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		return ofNullable(this.languageSpecificAttributesContainer)
			.orElseGet(() -> {
				// when not available lazily instantiate it
				this.languageSpecificAttributesContainer = new HashMap<>();
				return this.languageSpecificAttributesContainer;
			});
	}

	@Override
	protected Map<AssociatedDataKey, AssociatedDataStoragePart> getOrCreateCachedAssociatedDataStorageContainer(int entityPrimaryKey, @Nonnull AssociatedDataKey key) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		return ofNullable(this.associatedDataContainers)
			.orElseGet(() -> {
				// when not available lazily instantiate it
				this.associatedDataContainers = new HashMap<>();
				return this.associatedDataContainers;
			});
	}

	@Override
	protected ReferencesStoragePart getCachedReferenceStorageContainer(int entityPrimaryKey) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		return this.referencesStorageContainer;
	}

	@Override
	protected ReferencesStoragePart cacheReferencesStorageContainer(int entityPrimaryKey, @Nonnull ReferencesStoragePart referencesStorageContainer) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		this.referencesStorageContainer = referencesStorageContainer;
		return this.referencesStorageContainer;
	}

	@Override
	protected PricesStoragePart getCachedPricesStorageContainer(int entityPrimaryKey) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		return this.pricesContainer;
	}

	@Override
	protected PricesStoragePart cachePricesStorageContainer(int entityPrimaryKey, @Nonnull PricesStoragePart pricesStorageContainer) {
		Assert.isTrue(entityPrimaryKey == this.entityPrimaryKey, () -> new IllegalStateException(ERROR_SAME_KEY_EXPECTED));
		this.pricesContainer = pricesStorageContainer;
		return this.pricesContainer;
	}

	/*
		PRIVATE METHODS
	 */

	private void updateAttributes(AttributeMutation localMutation) {
		final AttributeKey attributeKey = localMutation.getAttributeKey();
		final EntitySchema entitySchema = schemaAccessor.get();
		final AttributeSchema attributeDefinition = entitySchema.getAttribute(attributeKey.getAttributeName());
		Assert.notNull(attributeDefinition, "Attribute `" + attributeKey.getAttributeName() + "` is not known for entity `" + entitySchema.getName() + "`.");
		final AttributesStoragePart attributesStorageContainer = ofNullable(attributeKey.getLocale())
			.map(it -> {
				// if associated data is localized - enrich the set of entity locales
				entityContainer.addAttributeLocale(it);
				// get or create locale specific attributes container
				return getAttributeStorageContainer(entityPrimaryKey, it);
			})
			// get or create locale agnostic container (global one)
			.orElseGet(() -> getAttributeStorageContainer(entityPrimaryKey));

		// now replace the attribute contents in the container
		attributesStorageContainer.upsertAttribute(attributeKey, attributeDefinition, localMutation::mutateLocal);

		// TOBEDONE JNO when mutation is removal locales should be recomputed and optionally removed if all localized attributes and associated data are dropped!!
	}

	private void updateAssociatedData(AssociatedDataMutation localMutation) {
		final AssociatedDataKey associatedDataKey = localMutation.getAssociatedDataKey();
		// get or create associated data container
		final AssociatedDataStoragePart associatedDataStorageContainer = getAssociatedDataStorageContainer(entityPrimaryKey, associatedDataKey);
		// add associated data key to entity set to allow lazy fetching by the key
		this.entityContainer.addAssociatedDataKey(associatedDataKey);
		// now replace the associated data in the container
		associatedDataStorageContainer.replaceAssociatedData(
			localMutation.mutateLocal(associatedDataStorageContainer.getValue())
		);

		// TOBEDONE JNO when mutation is removal locales should be recomputed and optionally removed if all localized attributes and associated data are dropped!!
	}

	private void updateReferences(@Nonnull LocalMutation<?, ?> localMutation) {
		// get or create references container
		final ReferencesStoragePart referencesStorageCnt = getReferencesStorageContainer(entityPrimaryKey);
		final ReferenceMutation<?> referenceContractMutation = (ReferenceMutation<?>) localMutation;
		// replace or add the mutated reference in the container
		referencesStorageCnt.replaceOrAddReference(referenceContractMutation.getReferenceKey(), referenceContractMutation::mutateLocal);
		// when mutation affects inner attributes of the reference
		if (localMutation instanceof ReferenceAttributesUpdateMutation) {
			final AttributeKey attributeKey = ((ReferenceAttributesUpdateMutation) localMutation).getAttributeKey();
			// and attribute is localized - enrich the set of attribute locales
			ofNullable(attributeKey.getLocale()).ifPresent(it -> this.entityContainer.addAttributeLocale(it));
			// TOBEDONE JNO when mutation is removal locales should be recomputed and optionally removed if all localized attributes and associated data are dropped!!
		}
	}

	private void updateHierarchyPlacement(LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract> localMutation) {
		// get entity container
		final EntityBodyStoragePart entityStorageContainer = getEntityStorageContainer(entityPrimaryKey, requiresExisting);
		// update hierarchical placement there
		entityStorageContainer.setHierarchicalPlacement(
			localMutation.mutateLocal(entityStorageContainer.getHierarchicalPlacement())
		);
	}

	private void updatePriceIndex(PriceMutation localMutation) {
		// get or create prices container
		final PricesStoragePart pricesStorageContainer = getPriceStorageContainer(entityPrimaryKey);
		// add or replace price in the container
		pricesStorageContainer.replaceOrAddPrice(
			localMutation.getPriceKey(),
			localMutation::mutateLocal,
			priceKey -> ofNullable(assignedInternalPriceIdIndex).map(it -> it.get(priceKey)).orElse(null)
		);
	}

	private void updatePrices(LocalMutation<Prices, PriceKey> localMutation) {
		// get or create prices container
		final PricesStoragePart pricesStorageContainer = getPriceStorageContainer(entityPrimaryKey);
		// update price inner record handling in it - we have to mock the Prices virtual container for this operation
		final Prices mutatedPrices = localMutation.mutateLocal(
			new Prices(
				pricesStorageContainer.getVersion(),
				Collections.emptyList(),
				pricesStorageContainer.getPriceInnerRecordHandling()
			)
		);
		pricesStorageContainer.setPriceInnerRecordHandling(mutatedPrices.getPriceInnerRecordHandling());
	}

}
