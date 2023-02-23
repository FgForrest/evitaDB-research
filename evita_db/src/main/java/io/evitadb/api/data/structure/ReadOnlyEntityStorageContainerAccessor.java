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

import com.esotericsoftware.kryo.util.IntMap;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.mutation.StorageContainerBuffer;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.model.storageParts.entity.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * This accessor serves as a cached accessor to entity containers / storage parts. The accessor is not thread safe and
 * is meant to be instantiated when paginated entity result is retrieved and ad-hoc data needs to be read from
 * the {@link io.evitadb.storage.MemTable}. All read containers are kept cached in internal data structures so repeated
 * reads of the same container type for the same entity primary key don't involve an I/O operation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@NotThreadSafe
public class ReadOnlyEntityStorageContainerAccessor extends AbstractEntityStorageContainerAccessor {
	/**
	 * Cache for the {@link EntityBodyStoragePart} by the entity primary key.
	 */
	protected IntMap<EntityBodyStoragePart> entityContainer;
	/**
	 * Cache for the {@link AttributesStoragePart} by the entity primary key.
	 */
	protected IntMap<AttributesStoragePart> globalAttributesStorageContainer;
	/**
	 * Cache for the localized {@link AttributesStoragePart} by the entity primary key.
	 */
	protected IntMap<Map<Locale, AttributesStoragePart>> languageSpecificAttributesContainer;
	/**
	 * Cache for the {@link AssociatedDataStoragePart} by the entity primary key.
	 */
	protected IntMap<Map<AssociatedDataKey, AssociatedDataStoragePart>> associatedDataContainers;
	/**
	 * Cache for the {@link PricesStoragePart} by the entity primary key.
	 */
	protected IntMap<PricesStoragePart> pricesContainer;
	/**
	 * Cache for the {@link ReferencesStoragePart} by the entity primary key.
	 */
	protected IntMap<ReferencesStoragePart> referencesStorageContainer;

	public ReadOnlyEntityStorageContainerAccessor(StorageContainerBuffer storageContainerBuffer, Supplier<EntitySchema> schemaAccessor) {
		super(storageContainerBuffer, schemaAccessor);
	}

	@Override
	protected EntityBodyStoragePart getCachedEntityStorageContainer(int entityPrimaryKey) {
		return getEntityStorageContainerHolder().get(entityPrimaryKey);
	}

	@Override
	protected EntityBodyStoragePart cacheEntityStorageContainer(int entityPrimaryKey, @Nonnull EntityBodyStoragePart entityStorageContainer) {
		getEntityStorageContainerHolder().put(entityPrimaryKey, entityStorageContainer);
		return entityStorageContainer;
	}

	@Override
	protected AttributesStoragePart getCachedAttributeStorageContainer(int entityPrimaryKey) {
		return getAttributesStorageContainerHolder().get(entityPrimaryKey);
	}

	@Override
	protected AttributesStoragePart cacheAttributeStorageContainer(int entityPrimaryKey, @Nonnull AttributesStoragePart attributesStorageContainer) {
		getAttributesStorageContainerHolder().put(entityPrimaryKey, attributesStorageContainer);
		return attributesStorageContainer;
	}

	@Override
	protected Map<Locale, AttributesStoragePart> getOrCreateCachedLocalizedAttributesStorageContainer(int entityPrimaryKey, @Nonnull Locale locale) {
		final IntMap<Map<Locale, AttributesStoragePart>> holder = ofNullable(this.languageSpecificAttributesContainer).orElseGet(() -> {
			this.languageSpecificAttributesContainer = new IntMap<>();
			return this.languageSpecificAttributesContainer;
		});
		return ofNullable(holder.get(entityPrimaryKey))
			.orElseGet(() -> {
				final HashMap<Locale, AttributesStoragePart> localeSpecificMap = new HashMap<>();
				holder.put(entityPrimaryKey, localeSpecificMap);
				return localeSpecificMap;
			});
	}

	@Override
	protected Map<AssociatedDataKey, AssociatedDataStoragePart> getOrCreateCachedAssociatedDataStorageContainer(int entityPrimaryKey, @Nonnull AssociatedDataKey key) {
		final IntMap<Map<AssociatedDataKey, AssociatedDataStoragePart>> holder = ofNullable(this.associatedDataContainers).orElseGet(() -> {
			this.associatedDataContainers = new IntMap<>();
			return this.associatedDataContainers;
		});
		return ofNullable(holder.get(entityPrimaryKey))
			.orElseGet(() -> {
				final HashMap<AssociatedDataKey, AssociatedDataStoragePart> associatedDataMap = new HashMap<>();
				holder.put(entityPrimaryKey, associatedDataMap);
				return associatedDataMap;
			});
	}

	@Override
	protected ReferencesStoragePart getCachedReferenceStorageContainer(int entityPrimaryKey) {
		return getReferencesStorageContainerHolder().get(entityPrimaryKey);
	}

	@Override
	protected ReferencesStoragePart cacheReferencesStorageContainer(int entityPrimaryKey, @Nonnull ReferencesStoragePart referencesStorageContainer) {
		getReferencesStorageContainerHolder().put(entityPrimaryKey, referencesStorageContainer);
		return referencesStorageContainer;
	}

	@Override
	protected PricesStoragePart getCachedPricesStorageContainer(int entityPrimaryKey) {
		return getPricesStorageContainerHolder().get(entityPrimaryKey);
	}

	@Override
	protected PricesStoragePart cachePricesStorageContainer(int entityPrimaryKey, @Nonnull PricesStoragePart pricesStorageContainer) {
		getPricesStorageContainerHolder().put(entityPrimaryKey, pricesStorageContainer);
		return pricesStorageContainer;
	}

	/*
		PRIVATE METHODS
	 */

	private IntMap<EntityBodyStoragePart> getEntityStorageContainerHolder() {
		return ofNullable(this.entityContainer).orElseGet(() -> {
			this.entityContainer = new IntMap<>();
			return this.entityContainer;
		});
	}

	private IntMap<AttributesStoragePart> getAttributesStorageContainerHolder() {
		return ofNullable(this.globalAttributesStorageContainer).orElseGet(() -> {
			this.globalAttributesStorageContainer = new IntMap<>();
			return this.globalAttributesStorageContainer;
		});
	}

	private IntMap<PricesStoragePart> getPricesStorageContainerHolder() {
		return ofNullable(this.pricesContainer).orElseGet(() -> {
			this.pricesContainer = new IntMap<>();
			return this.pricesContainer;
		});
	}

	private IntMap<ReferencesStoragePart> getReferencesStorageContainerHolder() {
		return ofNullable(this.referencesStorageContainer).orElseGet(() -> {
			this.referencesStorageContainer = new IntMap<>();
			return this.referencesStorageContainer;
		});
	}

}
