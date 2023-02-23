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

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.mutation.StorageContainerBuffer;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.model.storageParts.entity.*;
import io.evitadb.storage.model.storageParts.entity.AssociatedDataStoragePart.EntityAssociatedDataKey;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart.EntityAttributesSetKey;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * This abstract class centralizes the logic for accessing entity storage containers / storage parts stored in the
 * {@link io.evitadb.storage.MemTable}, using {@link StorageContainerBuffer} as a mean for accessing it and getting
 * advantage of data trapped in volatile memory.
 *
 * See descendants of this class to get an idea about use-cases of this abstract class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public abstract class AbstractEntityStorageContainerAccessor implements EntityStorageContainerAccessor {
	/**
	 * Contains CURRENT storage buffer that traps transactional and intermediate volatile data.
	 */
	private final StorageContainerBuffer storageContainerBuffer;
	/**
	 * Function returns CURRENT {@link EntitySchema} to be used for deserialized objects.
	 */
	private final Supplier<EntitySchema> schemaAccessor;

	@Nonnull
	@Override
	public EntityBodyStoragePart getEntityStorageContainer(int entityPrimaryKey, EntityExistence expects) {
		// if entity container is already present - return it quickly
		return ofNullable(getCachedEntityStorageContainer(entityPrimaryKey))
			// when not
			.orElseGet(() -> {
				final EntitySchema schema = schemaAccessor.get();
				return EntitySerializationContext.executeWithSupplier(
					schema,
					() -> {
						final Serializable entityType = schema.getName();
						// read it from mem table
						return cacheEntityStorageContainer(
							entityPrimaryKey,
							ofNullable(storageContainerBuffer.fetch(entityPrimaryKey, EntityBodyStoragePart.class))
								.map(it -> {
									// if it was found, verify whether it was expected
									if (expects == EntityExistence.MUST_NOT_EXIST) {
										throw new InvalidMutationException(
											"There is already entity " + entityType + " with primary key " +
												entityPrimaryKey + " present! Please fetch this entity and perform update " +
												"operation on top of it."
										);
									}
									return it;
								})
								.orElseGet(() -> {
									// if it was not found, verify whether it was expected
									if (expects == EntityExistence.MUST_EXIST) {
										throw new InvalidMutationException(
											"There is no entity " + entityType + " with primary key " +
												entityPrimaryKey + " present! This means, that you're probably trying to update " +
												"entity that has been already removed!"
										);
									} else {
										// create new container for the entity
										return new EntityBodyStoragePart(entityPrimaryKey);
									}
								})
						);
					});
			});
	}

	@Nonnull
	@Override
	public AttributesStoragePart getAttributeStorageContainer(int entityPrimaryKey) {
		// if attributes container is already present - return it quickly
		return ofNullable(getCachedAttributeStorageContainer(entityPrimaryKey))
			// when not
			.orElseGet(
				() -> EntitySerializationContext.executeWithSupplier(
					schemaAccessor.get(),
					() -> {
						// try to compute container id (keyCompressor must already recognize the EntityAttributesSetKey)
						final EntityAttributesSetKey globalAttributeSetKey = new EntityAttributesSetKey(entityPrimaryKey, null);
						return cacheAttributeStorageContainer(
							entityPrimaryKey,
							ofNullable(storageContainerBuffer.fetch(globalAttributeSetKey, AttributesStoragePart.class, AttributesStoragePart::computeUniquePartId))
								// when not found in storage - create new container
								.orElseGet(() -> new AttributesStoragePart(entityPrimaryKey))
						);
					})
			);
	}

	@Nonnull
	@Override
	public AttributesStoragePart getAttributeStorageContainer(int entityPrimaryKey, @Nonnull Locale locale) {
		// check existence locale specific attributes index
		final Map<Locale, AttributesStoragePart> attributesContainer = getOrCreateCachedLocalizedAttributesStorageContainer(entityPrimaryKey, locale);
		// if attributes container is already present in the index - return it quickly
		return attributesContainer.computeIfAbsent(
			locale,
			language -> EntitySerializationContext.executeWithSupplier(
				schemaAccessor.get(),
				() -> {
					// try to compute container id (keyCompressor must already recognize the EntityAttributesSetKey)
					final EntityAttributesSetKey localeSpecificAttributeSetKey = new EntityAttributesSetKey(entityPrimaryKey, language);
					return ofNullable(storageContainerBuffer.fetch(localeSpecificAttributeSetKey, AttributesStoragePart.class, AttributesStoragePart::computeUniquePartId))
						// when not found in storage - create new container
						.orElseGet(() -> new AttributesStoragePart(entityPrimaryKey, locale));
				}
			)
		);
	}

	@Nonnull
	@Override
	public AssociatedDataStoragePart getAssociatedDataStorageContainer(int entityPrimaryKey, @Nonnull AssociatedDataKey key) {
		// check existence locale specific associated data index
		final Map<AssociatedDataKey, AssociatedDataStoragePart> associatedDataContainer = getOrCreateCachedAssociatedDataStorageContainer(entityPrimaryKey, key);
		// if associated data container is already present in the index - return it quickly
		return associatedDataContainer.computeIfAbsent(
			key,
			associatedDataKey -> EntitySerializationContext.executeWithSupplier(
				schemaAccessor.get(),
				() -> {
					// try to compute container id (keyCompressor must already recognize the EntityAssociatedDataKey)
					final EntityAssociatedDataKey entityAssociatedDataKey = new EntityAssociatedDataKey(entityPrimaryKey, key.getAssociatedDataName(), key.getLocale());
					return ofNullable(storageContainerBuffer.fetch(entityAssociatedDataKey, AssociatedDataStoragePart.class, AssociatedDataStoragePart::computeUniquePartId))
						// when not found in storage - create new container
						.orElseGet(() -> new AssociatedDataStoragePart(entityPrimaryKey, associatedDataKey));
				})
		);
	}

	@Nonnull
	@Override
	public ReferencesStoragePart getReferencesStorageContainer(int entityPrimaryKey) {
		// if reference container is already present - return it quickly
		return ofNullable(getCachedReferenceStorageContainer(entityPrimaryKey))
			//when not
			.orElseGet(
				() -> EntitySerializationContext.executeWithSupplier(
					schemaAccessor.get(),
					// read it from mem table
					() -> cacheReferencesStorageContainer(
						entityPrimaryKey,
						ofNullable(storageContainerBuffer.fetch(entityPrimaryKey, ReferencesStoragePart.class))
							// and when not found even there create new container
							.orElseGet(() -> new ReferencesStoragePart(entityPrimaryKey))
					)
				));
	}

	@Nonnull
	@Override
	public PricesStoragePart getPriceStorageContainer(int entityPrimaryKey) {
		// if price container is already present - return it quickly
		return ofNullable(getCachedPricesStorageContainer(entityPrimaryKey))
			//when not
			.orElseGet(
				() -> EntitySerializationContext.executeWithSupplier(
					schemaAccessor.get(),
					// read it from mem table
					() -> cachePricesStorageContainer(
						entityPrimaryKey,
						ofNullable(storageContainerBuffer.fetch(entityPrimaryKey, PricesStoragePart.class))
							// and when not found even there create new container
							.orElseGet(() -> new PricesStoragePart(entityPrimaryKey))
					)
				));
	}

	protected abstract EntityBodyStoragePart getCachedEntityStorageContainer(int entityPrimaryKey);

	protected abstract EntityBodyStoragePart cacheEntityStorageContainer(int entityPrimaryKey, @Nonnull EntityBodyStoragePart entityStorageContainer);

	protected abstract AttributesStoragePart getCachedAttributeStorageContainer(int entityPrimaryKey);

	protected abstract AttributesStoragePart cacheAttributeStorageContainer(int entityPrimaryKey, @Nonnull AttributesStoragePart attributesStorageContainer);

	protected abstract Map<Locale, AttributesStoragePart> getOrCreateCachedLocalizedAttributesStorageContainer(int entityPrimaryKey, @Nonnull Locale locale);

	protected abstract Map<AssociatedDataKey, AssociatedDataStoragePart> getOrCreateCachedAssociatedDataStorageContainer(int entityPrimaryKey, @Nonnull AssociatedDataKey key);

	protected abstract ReferencesStoragePart getCachedReferenceStorageContainer(int entityPrimaryKey);

	protected abstract ReferencesStoragePart cacheReferencesStorageContainer(int entityPrimaryKey, @Nonnull ReferencesStoragePart referencesStorageContainer);

	protected abstract PricesStoragePart getCachedPricesStorageContainer(int entityPrimaryKey);

	protected abstract PricesStoragePart cachePricesStorageContainer(int entityPrimaryKey, @Nonnull PricesStoragePart pricesStorageContainer);

}
