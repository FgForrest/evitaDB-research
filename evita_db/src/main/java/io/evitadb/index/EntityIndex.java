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

package io.evitadb.index;

import io.evitadb.api.EntityCollection;
import io.evitadb.api.data.Versioned;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.attribute.AttributeIndex;
import io.evitadb.index.attribute.AttributeIndexContract;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.bool.TransactionalBoolean;
import io.evitadb.index.facet.FacetIndex;
import io.evitadb.index.facet.FacetIndexContract;
import io.evitadb.index.hierarchy.HierarchyIndex;
import io.evitadb.index.hierarchy.HierarchyIndexContract;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.index.price.PriceIndexContract;
import io.evitadb.index.price.PriceListAndCurrencyPriceIndex;
import io.evitadb.index.price.PriceSuperIndex;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStorageKey;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStoragePart.AttributeIndexType;
import io.evitadb.storage.model.storageParts.index.EntityIndexStoragePart;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * This class represents main data structure that keeps all information connected with entity data, that could be used
 * for searching, sorting or another computational task upon these data.
 *
 * There may be multiple {@link EntityIndex} instances with different slices of the original data. There will be always
 * single {@link GlobalEntityIndex} index that contains all the data, but also several thinner
 * {@link ReducedEntityIndex indexes} that would contain only part of these. We aim to choose the smallest index
 * possible that can still provide correct answer for the input query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class EntityIndex implements PriceIndexContract, Versioned, EntityIndexDataStructure {
	/**
	 * This part of index collects information about filterable/unique/sortable attributes of the entities. It provides
	 * data that are necessary for constructing {@link Formula} tree for the constraints
	 * related to the attributes.
	 */
	@Delegate(types = AttributeIndexContract.class)
	protected final AttributeIndex attributeIndex;
	/**
	 * This is internal flag that tracks whether the index contents became dirty and needs to be persisted.
	 */
	protected final TransactionalBoolean dirty;
	/**
	 * IntegerBitmap contains all entity ids known to this index. This bitmap represents superset of all inner bitmaps.
	 */
	protected final TransactionalBitmap entityIds;
	/**
	 * Map contains entity ids by their supported language.
	 */
	protected final TransactionalMemoryMap<Locale, TransactionalBitmap> entityIdsByLanguage;
	/**
	 * Type of the index.
	 */
	@Getter protected final EntityIndexKey entityIndexKey;
	/**
	 * This part of index collects information about facets in entities. It provides data that are necessary for
	 * constructing {@link Formula} tree for the constraints related to the facets.
	 */
	@Delegate(types = FacetIndexContract.class)
	protected final FacetIndex facetIndex;
	/**
	 * This part of index collection information about hierarchy placement of the entities. It provides data that are
	 * necessary for constructing {@link Formula} tree for the constraints related to the hierarchy.
	 */
	@Delegate(types = HierarchyIndexContract.class)
	protected final HierarchyIndex hierarchyIndex;
	/**
	 * Unique id that identifies this instance of {@link EntityIndex}.
	 */
	@Getter protected final int primaryKey;
	/**
	 * Version of the entity index that gets increased with each atomic change in the index (incremented by one when
	 * transaction is committed and anything in this index was changed).
	 */
	@Getter protected final int version;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * Lambda that provides access to the current schema.
	 */
	protected Supplier<EntitySchema> schemaAccessor;

	protected EntityIndex(int primaryKey, @Nonnull EntityIndexKey entityIndexKey, @Nonnull Supplier<EntitySchema> schemaAccessor) {
		this.primaryKey = primaryKey;
		this.version = 1;
		this.dirty = new TransactionalBoolean();
		this.entityIndexKey = entityIndexKey;
		this.schemaAccessor = schemaAccessor;
		this.entityIds = new TransactionalBitmap();
		this.entityIdsByLanguage = new TransactionalMemoryMap<>(new HashMap<>());
		this.attributeIndex = new AttributeIndex();
		this.hierarchyIndex = new HierarchyIndex();
		this.facetIndex = new FacetIndex();
	}

	protected EntityIndex(int primaryKey, @Nonnull EntityIndexKey entityIndexKey, int version, @Nonnull Supplier<EntitySchema> schemaAccessor, @Nonnull Bitmap entityIds, @Nonnull Map<Locale, TransactionalBitmap> entityIdsByLanguage, @Nonnull AttributeIndex attributeIndex, @Nonnull HierarchyIndex hierarchyIndex, @Nonnull FacetIndex facetIndex) {
		this.primaryKey = primaryKey;
		this.entityIndexKey = entityIndexKey;
		this.version = version;
		this.schemaAccessor = schemaAccessor;
		this.dirty = new TransactionalBoolean();
		this.entityIds = new TransactionalBitmap(entityIds);

		final Map<Locale, TransactionalBitmap> txMap = createHashMap(entityIdsByLanguage.size());
		for (Entry<Locale, TransactionalBitmap> entry : entityIdsByLanguage.entrySet()) {
			txMap.put(entry.getKey(), new TransactionalBitmap(entry.getValue()));
		}
		this.entityIdsByLanguage = new TransactionalMemoryMap<>(txMap);
		this.attributeIndex = attributeIndex;
		this.hierarchyIndex = hierarchyIndex;
		this.facetIndex = facetIndex;
	}

	/**
	 * Registers new entity primary key to the superset of entity ids of this entity index.
	 */
	public boolean insertPrimaryKeyIfMissing(int entityPrimaryKey) {
		final boolean added = entityIds.add(entityPrimaryKey);
		if (added) {
			this.dirty.setToTrue();
		}
		return added;
	}

	/**
	 * Removes existing from the superset of entity ids of this entity index.
	 */
	public boolean removePrimaryKey(int entityPrimaryKey) {
		final boolean removed = entityIds.remove(entityPrimaryKey);
		if (removed) {
			this.dirty.setToTrue();
		}
		return removed;
	}

	/**
	 * Returns superset of all entity ids known to this entity index.
	 */
	public Formula getAllPrimaryKeysFormula() {
		return entityIds.isEmpty() ? EmptyFormula.INSTANCE : new ConstantFormula(entityIds);
	}

	/**
	 * Returns superset of all entity ids known to this entity index.
	 */
	public Bitmap getAllPrimaryKeys() {
		return entityIds;
	}

	/**
	 * Replaces reference to the schema accessor lambda in new collection. This needs to be done when transaction is
	 * committed and new EntityIndex is created with link to the original transactional EntityIndex but finally
	 * new {@link io.evitadb.api.EntityCollection} is created and the new indexes linking old collection needs to be
	 * migrated to new entity collection.
	 */
	public void updateReferencesTo(EntityCollection newCollection) {
		this.schemaAccessor = newCollection::getSchema;
	}

	/**
	 * Provides access to the entity schema via passed lambda.
	 */
	public EntitySchema getEntitySchema() {
		return schemaAccessor.get();
	}

	/**
	 * Inserts information that entity with `recordId` has localized attribute / associated data of passed `language`.
	 * If such information is already present no changes are made.
	 */
	public void upsertLanguage(Locale language, int recordId) {
		final boolean added = this.entityIdsByLanguage
			.computeIfAbsent(language, loc -> new TransactionalBitmap())
			.add(recordId);
		if (added) {
			this.dirty.setToTrue();
		}
	}

	/**
	 * Removed information that entity with `recordId` has no longer any localized attribute / associated data of passed `language`.
	 */
	public void removeLanguage(Locale locale, int recordId) {
		final TransactionalBitmap recordIdsWithLanguage = this.entityIdsByLanguage.get(locale);
		Assert.isTrue(
			recordIdsWithLanguage != null && recordIdsWithLanguage.remove(recordId),
			"Entity `" + recordId + "` has unexpectedly not indexed localized data for language `" + locale + "`!"
		);
		if (recordIdsWithLanguage.isEmpty()) {
			this.entityIdsByLanguage.remove(locale);
			this.dirty.setToTrue();
			// remove the changes container - the bitmap got removed entirely
			TransactionalMemory.removeTransactionalMemoryLayerIfExists(recordIdsWithLanguage);
		}
	}

	/**
	 * Returns formula that computes all record ids in this index that has at least one localized attribute / associated
	 * data in passed `language`.
	 */
	public Formula getRecordsWithLanguageFormula(Locale language) {
		return ofNullable(this.entityIdsByLanguage.get(language))
			.map(it -> (Formula) new ConstantFormula(it))
			.orElse(EmptyFormula.INSTANCE);
	}

	/**
	 * Returns bitmap (naturally ordered distinct array) that computes all record ids in this index that has at least
	 * one localized attribute / associated data in passed `language`.
	 */
	public Bitmap getRecordsWithLanguage(Locale language) {
		return ofNullable((Bitmap) this.entityIdsByLanguage.get(language)).orElse(EmptyBitmap.INSTANCE);
	}

	/**
	 * Returns collection of all languages that are present in this {@link EntityIndex}.
	 */
	public Collection<Locale> getLanguages() {
		return this.entityIdsByLanguage.keySet();
	}

	/**
	 * Returns true if index contains no data whatsoever.
	 */
	public boolean isEmpty() {
		return entityIds.isEmpty() &&
			attributeIndex.isAttributeIndexEmpty() &&
			hierarchyIndex.isHierarchyIndexEmpty();
	}

	/**
	 * Method returns collection of all modified parts of this index that were modified and needs to be stored.
	 */
	public Collection<StoragePart> getModifiedStorageParts() {
		final List<StoragePart> dirtyList = new LinkedList<>();
		if (dirty.isTrue()) {
			dirtyList.add(createStoragePart());
		}
		ofNullable(hierarchyIndex.createStoragePart(primaryKey))
			.ifPresent(dirtyList::add);
		dirtyList.addAll(attributeIndex.getModifiedStorageParts(primaryKey));
		dirtyList.addAll(facetIndex.getModifiedStorageParts(primaryKey));
		return dirtyList;
	}

	@Override
	public void resetDirty() {
		this.dirty.reset();
		this.hierarchyIndex.resetDirty();
		this.attributeIndex.resetDirty();
		this.facetIndex.resetDirty();
	}

	@Override
	public void clearTransactionalMemory() {
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.dirty);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.entityIds);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.entityIdsByLanguage);
		this.attributeIndex.clearTransactionalMemory();
		this.hierarchyIndex.clearTransactionalMemory();
		this.facetIndex.clearTransactionalMemory();
	}

	public abstract <S extends PriceIndexContract> S getPriceIndex();

	/*
		PRIVATE METHODS
	 */

	/**
	 * Method creates container that is possible to serialize with {@link com.esotericsoftware.kryo.Kryo} and store
	 * into {@link io.evitadb.storage.MemTable} storage.
	 */
	private StoragePart createStoragePart() {
		final PriceIndexContract priceIndex = getPriceIndex();
		return new EntityIndexStoragePart(
			primaryKey, version, entityIndexKey, entityIds, entityIdsByLanguage,
			Stream.of(
					attributeIndex.getUniqueIndexes().stream().map(it -> new AttributeIndexStorageKey(entityIndexKey, AttributeIndexType.UNIQUE, it)),
					attributeIndex.getFilterIndexes().stream().map(it -> new AttributeIndexStorageKey(entityIndexKey, AttributeIndexType.FILTER, it)),
					attributeIndex.getSortIndexes().stream().map(it -> new AttributeIndexStorageKey(entityIndexKey, AttributeIndexType.SORT, it))
				)
				.flatMap(it -> it)
				.collect(Collectors.toSet()),
			priceIndex instanceof PriceSuperIndex ? ((PriceSuperIndex)priceIndex).getLastAssignedInternalPriceId() : null,
			priceIndex
				.getPriceListAndCurrencyIndexes()
				.stream()
				.map(PriceListAndCurrencyPriceIndex::getPriceIndexKey)
				.collect(Collectors.toSet()),
			!hierarchyIndex.isHierarchyIndexEmpty(),
			facetIndex.getReferencedEntities()
		);
	}

}
