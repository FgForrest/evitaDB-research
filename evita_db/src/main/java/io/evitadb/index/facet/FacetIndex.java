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

package io.evitadb.index.facet;

import io.evitadb.api.Transaction;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.function.TriFunction;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndexDataStructure;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.facet.FacetEntityTypeIndex.FacetEntityTypeIndexChanges;
import io.evitadb.index.facet.FacetEntityTypeIndex.NonTransactionalCopy;
import io.evitadb.index.facet.FacetIndex.FacetIndexChanges;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.index.set.TransactionalMemorySet;
import io.evitadb.index.transactionalMemory.*;
import io.evitadb.query.algebra.facet.FacetGroupFormula;
import io.evitadb.storage.MemTable;
import io.evitadb.storage.model.storageParts.index.FacetIndexStoragePart;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * Facet index provides fast O(1) access to the bitmaps of entity primary keys that refer to the faceted entity.
 * This index allows processing of {@link io.evitadb.api.query.filter.Facet} filtering constraint and is used to
 * generate {@link io.evitadb.api.query.require.FacetSummary} response.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetIndex implements FacetIndexContract, TransactionalLayerProducer<FacetIndexChanges, FacetIndex>, EntityIndexDataStructure, Serializable {
	private static final long serialVersionUID = 7909305391436069776L;

	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * Index that stores the key data needed for facet look-ups. Main key is {@link EntityReference#getType()}, secondary
	 * key is {@link EntityReference#getPrimaryKey()}. Values represent {@link Entity#getPrimaryKey()} that posses of
	 * this facet.
	 */
	private final TransactionalMemoryMap<Serializable, FacetEntityTypeIndex> facetingEntities;
	/**
	 * This simple set structure contains set of {@link EntityReference#getType()} that contain any changes in its index.
	 * This is required because {@link FacetIndexStoragePart} stores only index for single {@link EntityReference#getType()}.
	 * All flags gets cleared on transaction commit - i.e. when {@link #createCopyWithMergedTransactionalMemory(FacetIndexChanges, TransactionalLayerMaintainer, Transaction)}
	 * is called.
	 */
	private final TransactionalMemorySet<Serializable> dirtyIndexes;

	public FacetIndex() {
		this.facetingEntities = new TransactionalMemoryMap<>(new HashMap<>());
		this.dirtyIndexes = new TransactionalMemorySet<>(new HashSet<>());
	}

	public FacetIndex(Collection<FacetIndexStoragePart> facetIndexStorageParts) {
		final Map<Serializable, FacetEntityTypeIndex> baseIndex = createHashMap(facetIndexStorageParts.size());
		this.facetingEntities = new TransactionalMemoryMap<>(baseIndex);
		for (FacetIndexStoragePart facetIndexStoragePart : facetIndexStorageParts) {
			// we need to wrap non-transactional integer bitmap into a transactional one
			final Map<Integer, Map<Integer, Bitmap>> sourceGroupIndex = facetIndexStoragePart.getFacetingEntities();

			final List<FacetGroupIndex> indexes = new LinkedList<>();
			if (facetIndexStoragePart.getNoGroupFacetingEntities() != null) {
				final Map<Integer, Bitmap> sourceNoGroupFacetingEntities = facetIndexStoragePart.getNoGroupFacetingEntities();
				indexes.add(
					new FacetGroupIndex(
						sourceNoGroupFacetingEntities
							.entrySet()
							.stream()
							.map(it -> new FacetIdIndex(it.getKey(), it.getValue()))
							.collect(Collectors.toList())
					)
				);
			}
			for (Entry<Integer, Map<Integer, Bitmap>> groupEntry : sourceGroupIndex.entrySet()) {
				indexes.add(
					new FacetGroupIndex(
						groupEntry.getKey(),
						groupEntry.getValue()
							.entrySet()
							.stream()
							.map(it -> new FacetIdIndex(it.getKey(), it.getValue()))
							.collect(Collectors.toList())
					)
				);
			}
			final Serializable entityType = facetIndexStoragePart.getReferencedEntityType();
			final FacetEntityTypeIndex facetEntityTypeIndex = new FacetEntityTypeIndex(entityType, indexes);
			baseIndex.put(entityType, facetEntityTypeIndex);
		}
		this.dirtyIndexes = new TransactionalMemorySet<>(new HashSet<>());
	}

	private FacetIndex(Map<Serializable, NonTransactionalCopy> sourceFacetingEntities) {
		final HashMap<Serializable, FacetEntityTypeIndex> theFacetingEntities = createHashMap(sourceFacetingEntities.size());
		this.facetingEntities = new TransactionalMemoryMap<>(theFacetingEntities);
		for (Entry<Serializable, NonTransactionalCopy> facetingEntitiesIndexEntry : sourceFacetingEntities.entrySet()) {
			final NonTransactionalCopy nonTransactionalCopy = facetingEntitiesIndexEntry.getValue();
			final Map<Integer, Bitmap> noGroup = nonTransactionalCopy.getNoGroup();
			final Map<Integer, Map<Integer, Bitmap>> groups = nonTransactionalCopy.getGroups();
			final Map<Integer, int[]> facetToGroupIndex = nonTransactionalCopy.getFacetToGroupIndex();
			final Serializable entityType = facetingEntitiesIndexEntry.getKey();
			theFacetingEntities.put(
				entityType,
				new FacetEntityTypeIndex(entityType, noGroup, groups, facetToGroupIndex)
			);
		}
		this.dirtyIndexes = new TransactionalMemorySet<>(new HashSet<>());
	}

	@Override
	public void addFacet(@Nonnull EntityReferenceContract<?> referenceKey, @Nullable GroupEntityReference groupReference, int entityPrimaryKey) {
		final Serializable entityType = referenceKey.getType();
		// we need to keep track of created internal transactional memory related data structures
		final FacetIndexChanges txLayer = TransactionalMemory.getTransactionalMemoryLayer(this);
		// fetch or create index for referenced entity type
		final FacetEntityTypeIndex facetEntityTypeIndex = this.facetingEntities.computeIfAbsent(
			entityType,
			referencedEntityType -> {
				final FacetEntityTypeIndex fetIx = new FacetEntityTypeIndex(entityType);
				ofNullable(txLayer).ifPresent(it -> it.addCreatedItem(fetIx));
				return fetIx;
			});
		// now add the facet relation for entity primary key
		final Integer groupId = ofNullable(groupReference).filter(Droppable::exists).map(EntityReference::getPrimaryKey).orElse(null);
		final boolean added = facetEntityTypeIndex.addFacet(referenceKey.getPrimaryKey(), groupId, entityPrimaryKey);
		// if anything was changed mark the entity type index dirty
		if (added) {
			this.dirtyIndexes.add(entityType);
		}
	}

	@Override
	public void removeFacet(@Nonnull EntityReferenceContract<?> referenceKey, @Nullable Integer groupId, int entityPrimaryKey) {
		// fetch index for referenced entity type
		final FacetEntityTypeIndex facetEntityTypeIndex = this.facetingEntities.get(referenceKey.getType());
		Assert.notNull(facetEntityTypeIndex, "No facet found for referenced entity `" + referenceKey.getType() + "`!");
		boolean removed = facetEntityTypeIndex.removeFacet(referenceKey.getPrimaryKey(), groupId, entityPrimaryKey);
		// if facet was removed check whether there are any data left
		if (removed && facetEntityTypeIndex.isEmpty()) {
			// we need to keep track of removed internal transactional memory related data structures
			final FacetIndexChanges txLayer = TransactionalMemory.getTransactionalMemoryLayer(this);
			// remove the index entirely
			this.facetingEntities.remove(referenceKey.getType());
			ofNullable(txLayer).ifPresent(it -> it.addRemovedItem(facetEntityTypeIndex));
		}
		// if anything was changed mark the entity type index dirty
		this.dirtyIndexes.add(referenceKey.getType());
	}

	@Override
	public Set<Serializable> getReferencedEntities() {
		return this.facetingEntities.keySet();
	}

	@Override
	public List<FacetGroupFormula> getFacetReferencingEntityIdsFormula(@Nonnull Serializable entityType, @Nonnull TriFunction<Integer, int[], Bitmap[], FacetGroupFormula> formulaFactory, @Nonnull int... facetId) {
		// fetch index for referenced entity type
		final FacetEntityTypeIndex facetEntityTypeIndex = facetingEntities.get(entityType);
		// if not found or empty, or input parameter is empty - return empty result
		if (facetEntityTypeIndex == null || facetEntityTypeIndex.isEmpty()) {
			return Collections.emptyList();
		} else {
			return facetEntityTypeIndex.getFacetReferencingEntityIdsFormula(formulaFactory, facetId);
		}
	}

	@Override
	public boolean isFacetInGroup(@Nonnull Serializable entityType, int groupId, int facetId) {
		return ofNullable(facetingEntities.get(entityType))
			.map(it -> it.isFacetInGroup(groupId, facetId))
			.orElse(false);
	}

	@Override
	@Nonnull
	public Map<Serializable, FacetEntityTypeIndex> getFacetingEntities() {
		return this.facetingEntities;
	}

	@Override
	public int getSize() {
		return this.facetingEntities.values()
			.stream()
			.mapToInt(FacetEntityTypeIndex::size)
			.sum();
	}

	/**
	 * Returns collection of {@link FacetIndexStoragePart} that were modified and need persistence to the {@link MemTable}.
	 */
	public List<FacetIndexStoragePart> getModifiedStorageParts(int entityIndexPK) {
		return this.facetingEntities.entrySet()
			.stream()
			.filter(it -> this.dirtyIndexes.contains(it.getKey()))
			.map(it -> new FacetIndexStoragePart(entityIndexPK, it.getKey(), it.getValue().getNotGroupedFacetsAsMap(), it.getValue().getGroupsAsMap()))
			.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		this.facetingEntities.keySet().stream().sorted().forEach(it ->
			sb.append(it)
				.append(":\n")
				.append(this.facetingEntities.get(it).toString())
		);
		if (sb.length() > 0) {
			while (sb.charAt(sb.length() - 1) == '\n') {
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb.toString();
	}

	@Override
	public void resetDirty() {
		this.dirtyIndexes.clear();
	}

	@Override
	public void clearTransactionalMemory() {
		for (FacetEntityTypeIndex entityTypeIndex : facetingEntities.values()) {
			entityTypeIndex.clearTransactionalMemory();
		}

		final FacetIndexChanges changes = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		ofNullable(changes).ifPresent(it -> it.cleanAll(TransactionalMemory.getTransactionalMemoryLayer()));
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.dirtyIndexes);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.facetingEntities);
	}

	/*
		TransactionalLayerCreator implementation
	 */

	@Override
	public FacetIndexChanges createLayer() {
		return new FacetIndexChanges();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public FacetIndex createCopyWithMergedTransactionalMemory(@Nullable FacetIndexChanges layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, @Nullable Transaction transaction) {
		// we can safely throw away dirty flag now
		transactionalLayer.removeTransactionalMemoryLayerIfExists(this.dirtyIndexes);
		final FacetIndex facetIndex = new FacetIndex(
			// this is a HACK - facetingEntities id indexes produce NonTransactionalCopy instead of type than generics would suggest
			(Map) transactionalLayer.getStateCopyWithCommittedChanges(this.facetingEntities, transaction)
		);
		ofNullable(layer).ifPresent(it -> it.clean(transactionalLayer));
		return facetIndex;
	}

	/**
	 * This class collects changes in {@link #facetingEntities} transactional map and its sub structure.
	 */
	public static class FacetIndexChanges {
		private final TransactionalContainerChanges<FacetEntityTypeIndexChanges, NonTransactionalCopy, FacetEntityTypeIndex> facetGroupIndexChanges = new TransactionalContainerChanges<>();

		public void addCreatedItem(FacetEntityTypeIndex index) {
			facetGroupIndexChanges.addCreatedItem(index);
		}

		public void addRemovedItem(FacetEntityTypeIndex index) {
			facetGroupIndexChanges.addRemovedItem(index);
		}

		public void clean(TransactionalLayerMaintainer transactionalLayer) {
			facetGroupIndexChanges.clean(transactionalLayer);
		}

		public void cleanAll(TransactionalLayerMaintainer transactionalLayer) {
			facetGroupIndexChanges.cleanAll(transactionalLayer);
		}
	}

}
