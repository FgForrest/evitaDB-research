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
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.function.TriFunction;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndexDataStructure;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.facet.FacetEntityTypeIndex.FacetEntityTypeIndexChanges;
import io.evitadb.index.facet.FacetEntityTypeIndex.NonTransactionalCopy;
import io.evitadb.index.facet.FacetGroupIndex.FacetGroupIndexChanges;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.index.transactionalMemory.*;
import io.evitadb.query.algebra.facet.FacetGroupFormula;
import lombok.Data;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * FacetEntityTypeIndex contains information about all entity ids that use facet that is of this {@link #entityType} as
 * their {@link io.evitadb.api.data.structure.Entity#getReference(Serializable, int)}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetEntityTypeIndex implements TransactionalLayerProducer<FacetEntityTypeIndexChanges, NonTransactionalCopy>, EntityIndexDataStructure {
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * Contains {@link EntityReference#getType()} of the facets in this index.
	 */
	@Getter private final Serializable entityType;
	/**
	 * Represents index of facet to group relation - if none exists facet is either unknown or not assigned to any group.
	 * TOBEDONE JNO - add consistency check that at the end of transaction, there is simple 1:1 relation in this sub index
	 */
	private final TransactionalMemoryMap<Integer, int[]> facetToGroupIndex;
	/**
	 * Represents index of {@link FacetGroupIndex}, the key is {@link FacetGroupIndex#getGroupId()}.
	 */
	private final TransactionalMemoryMap<Integer, FacetGroupIndex> groupedFacets;
	/**
	 * Represents index for all facets that are not organized in any group (has no relation to group).
	 */
	@Getter @Nullable private FacetGroupIndex notGroupedFacets;

	public FacetEntityTypeIndex(@Nonnull Serializable entityType) {
		this.entityType = entityType;
		this.notGroupedFacets = null;
		this.groupedFacets = new TransactionalMemoryMap<>(new HashMap<>());
		this.facetToGroupIndex = new TransactionalMemoryMap<>(new HashMap<>());
	}

	public FacetEntityTypeIndex(@Nonnull Serializable entityType, @Nonnull Collection<FacetGroupIndex> groupIndexes) {
		this.entityType = entityType;
		FacetGroupIndex noGroup = null;
		final Map<Integer, FacetGroupIndex> internalMap = new HashMap<>();
		final Map<Integer, int[]> facetToGroup = new HashMap<>();
		for (FacetGroupIndex groupIndex : groupIndexes) {
			final Integer groupId = groupIndex.getGroupId();
			if (groupId == null) {
				Assert.isTrue(noGroup == null, "There is only single group without group id allowed!");
				noGroup = groupIndex;
			} else {
				internalMap.put(groupId, groupIndex);
				for (FacetIdIndex facetIdIndex : groupIndex.getFacetIdIndexes().values()) {
					facetToGroup.merge(
						facetIdIndex.getFacetId(),
						new int[]{groupId},
						(oldValues, newValues) -> ArrayUtils.insertIntIntoOrderedArray(newValues[0], oldValues)
					);
				}
			}
		}
		this.notGroupedFacets = noGroup;
		this.groupedFacets = new TransactionalMemoryMap<>(internalMap);
		this.facetToGroupIndex = new TransactionalMemoryMap<>(facetToGroup);
	}

	FacetEntityTypeIndex(Serializable entityType, Map<Integer, Bitmap> noGroup, Map<Integer, Map<Integer, Bitmap>> groups, Map<Integer, int[]> facetToGroupIndex) {
		this.entityType = entityType;
		final Function<Map<Integer, Bitmap>, Map<Integer, FacetIdIndex>> facetIdIndexFct = map -> map.entrySet()
			.stream()
			.map(it -> new FacetIdIndex(it.getKey(), it.getValue()))
			.collect(
				Collectors.toMap(FacetIdIndex::getFacetId, Function.identity())
			);

		this.notGroupedFacets = ofNullable(noGroup)
			.map(it -> new FacetGroupIndex(null, facetIdIndexFct.apply(it)))
			.orElse(null);
		final Map<Integer, FacetGroupIndex> baseGroupMap = new HashMap<>();
		for (Entry<Integer, Map<Integer, Bitmap>> entry : groups.entrySet()) {
			final Map<Integer, FacetIdIndex> facetIndexes = facetIdIndexFct.apply(entry.getValue());
			baseGroupMap.put(
				entry.getKey(),
				new FacetGroupIndex(entry.getKey(), facetIndexes)
			);
		}
		this.groupedFacets = new TransactionalMemoryMap<>(baseGroupMap);
		this.facetToGroupIndex = new TransactionalMemoryMap<>(facetToGroupIndex);
	}

	/**
	 * Returns collection of {@link FacetGroupIndex} that contain information about entity ids linked to facets of
	 * particular group.
	 */
	@Nonnull
	public Collection<FacetGroupIndex> getGroupedFacets() {
		return groupedFacets.values();
	}

	/**
	 * Adds new entity primary key to facet index of `facetPrimaryKey` and group identified by `groupId`.
	 *
	 * @return true if entity id was really added
	 */
	public boolean addFacet(int facetPrimaryKey, @Nullable Integer groupId, int entityPrimaryKey) {
		final FacetEntityTypeIndexChanges txLayer = TransactionalMemory.getTransactionalMemoryLayer(this);
		final FacetGroupIndex facetGroupIndex;
		if (groupId == null) {
			if (this.notGroupedFacets == null) {
				this.notGroupedFacets = new FacetGroupIndex();
				ofNullable(txLayer).ifPresent(it -> it.addCreatedItem(this.notGroupedFacets));
			}
			facetGroupIndex = this.notGroupedFacets;
		} else {
			facetToGroupIndex.merge(
				facetPrimaryKey,
				new int[]{groupId},
				(oldValues, newValues) -> ArrayUtils.insertIntIntoOrderedArray(newValues[0], oldValues)
			);
			// fetch or create index for referenced entity id (inside correct type)
			facetGroupIndex = this.groupedFacets.computeIfAbsent(groupId, gPK -> {
				final FacetGroupIndex fgIx = new FacetGroupIndex(gPK);
				ofNullable(txLayer).ifPresent(it -> it.addCreatedItem(fgIx));
				return fgIx;
			});
		}

		return facetGroupIndex.addFacet(facetPrimaryKey, entityPrimaryKey);
	}

	/**
	 * Removes entity primary key from index of `facetPrimaryKey` facet and group identified by `groupId`.
	 *
	 * @return true if entity id was really removed
	 */
	public boolean removeFacet(int facetPrimaryKey, @Nullable Integer groupId, int entityPrimaryKey) {
		final FacetGroupIndex facetGroupIndex;
		if (groupId == null) {
			facetGroupIndex = this.notGroupedFacets;
		} else {
			// fetch or create index for referenced entity id (inside correct type)
			facetGroupIndex = this.groupedFacets.get(groupId);
		}
		// fetch index for referenced entity type
		Assert.notNull(facetGroupIndex, "Facet `" + facetPrimaryKey + "` not found in index (group: `" + groupId + "`)!");
		boolean removed = facetGroupIndex.removeFacet(facetPrimaryKey, entityPrimaryKey);

		// remove facet to group mapping
		if (groupId != null) {
			final int[] groups = facetToGroupIndex.get(facetPrimaryKey);
			int[] cleanedGroups = groups;
			for (int group : groups) {
				final FacetGroupIndex examinedGroupIndex = this.groupedFacets.get(group);
				// there is no facet index present any more
				if (examinedGroupIndex.getFacetIdIndex(facetPrimaryKey) == null) {
					cleanedGroups = ArrayUtils.removeIntFromOrderedArray(groupId, cleanedGroups);
				}
			}
			if (ArrayUtils.isEmpty(cleanedGroups)) {
				facetToGroupIndex.remove(facetPrimaryKey);
			} else {
				facetToGroupIndex.put(facetPrimaryKey, cleanedGroups);
			}
		}

		// if facet was removed check whether there are any data left
		if (removed && facetGroupIndex.isEmpty()) {
			// we need to keep track of removed internal transactional memory related data structures
			final FacetEntityTypeIndexChanges txLayer = TransactionalMemory.getTransactionalMemoryLayer(this);
			// remove the index entirely
			if (groupId == null) {
				this.notGroupedFacets = null;
			} else {
				this.groupedFacets.remove(groupId);
			}
			ofNullable(txLayer).ifPresent(it -> it.addRemovedItem(facetGroupIndex));
		}
		return removed;
	}

	/**
	 * Returns true if there is no entity id linked to any facet of this `entityType` and the entire index is useless.
	 */
	public boolean isEmpty() {
		if (this.notGroupedFacets != null && !this.notGroupedFacets.isEmpty()) {
			return false;
		}
		return this.groupedFacets
			.values()
			.stream()
			.allMatch(FacetGroupIndex::isEmpty);
	}

	/**
	 * Returns count of all entity ids referring to all facets of this `entityType`.
	 */
	public int size() {
		return ofNullable(this.notGroupedFacets).map(FacetGroupIndex::size).orElse(0) +
			this.groupedFacets.values().stream().mapToInt(FacetGroupIndex::size).sum();
	}

	/**
	 * Returns stream of all {@link FacetGroupIndex} in this index. It combines both non-grouped and grouped indexes.
	 */
	@Nonnull
	public Stream<FacetGroupIndex> getFacetGroupIndexesAsStream() {
		final Stream<FacetGroupIndex> groupStream = this.groupedFacets
			.values()
			.stream();
		return this.notGroupedFacets == null ?
			groupStream :
			Stream.concat(
				Stream.of(this.notGroupedFacets),
				groupStream
			);
	}

	/**
	 * Returns {@link FacetGroupIndex} for passed group id.
	 */
	@Nullable
	public FacetGroupIndex getFacetsInGroup(@Nullable Integer groupId) {
		return groupId == null ? notGroupedFacets : this.groupedFacets.get(groupId);
	}

	/**
	 * Method returns formula that allows computation of all entity primary keys that have at least one
	 * of `facetId` as its faceted reference.
	 */
	public List<FacetGroupFormula> getFacetReferencingEntityIdsFormula(@Nonnull TriFunction<Integer, int[], Bitmap[], FacetGroupFormula> formulaFactory, int... facetId) {
		final Map<FacetGroupIndex, List<Integer>> facetsByGroup = Arrays.stream(facetId)
			.mapToObj(fId -> ofNullable(facetToGroupIndex.get(fId))
				.map(groupIds -> Arrays.stream(groupIds).mapToObj(groupId -> new GroupFacetIdDTO(groupedFacets.get(groupId), fId)))
				.orElseGet(() -> Stream.of(new GroupFacetIdDTO(notGroupedFacets, fId)))
			)
			.flatMap(Function.identity())
			.filter(it -> it.getGroupIndex() != null)
			.collect(
				Collectors.groupingBy(
					GroupFacetIdDTO::getGroupIndex,
					Collectors.mapping(GroupFacetIdDTO::getFacetId, Collectors.toList())
				)
			);
		return facetsByGroup
			.entrySet()
			.stream()
			.map(entry -> {
				final FacetGroupIndex groupIndex = entry.getKey();
				if (groupIndex == null) {
					return null;
				} else {
					final int[] groupFacets = entry.getValue().stream().mapToInt(it -> it).toArray();
					return formulaFactory.apply(
						groupIndex.getGroupId(), groupFacets, groupIndex.getFacetIdIndexesAsArray(groupFacets)
					);
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * Method returns true if facet id is part of the passed group id for specified `entityType`.
	 */
	public boolean isFacetInGroup(int groupId, int facetId) {
		return ofNullable(facetToGroupIndex.get(facetId))
			.map(it -> Arrays.binarySearch(it, groupId) >= 0)
			.orElse(false);
	}

	/**
	 * Returns contents of non-grouped facet index as plain non-transactional map.
	 */
	@Nullable
	public Map<Integer, Bitmap> getNotGroupedFacetsAsMap() {
		if (this.notGroupedFacets != null) {
			return this.notGroupedFacets.getAsMap();
		} else {
			return null;
		}
	}

	/**
	 * Returns contents of grouped facet indexes as plain non-transactional map.
	 */
	@Nonnull
	public Map<Integer, Map<Integer, Bitmap>> getGroupsAsMap() {
		final Map<Integer, Map<Integer, Bitmap>> result = createHashMap(this.groupedFacets.size());
		for (Entry<Integer, FacetGroupIndex> entry : this.groupedFacets.entrySet()) {
			result.put(entry.getKey(), entry.getValue().getAsMap());
		}
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (this.notGroupedFacets != null) {
			sb.append("\t").append(this.notGroupedFacets).append("\n");
		}
		this.groupedFacets
			.keySet()
			.stream()
			.sorted()
			.forEach(group -> sb.append("\t").append(this.groupedFacets.get(group)));
		return sb.toString();
	}

	@Override
	public void resetDirty() {
		// do nothing here
	}

	@Override
	public void clearTransactionalMemory() {
		ofNullable(this.notGroupedFacets).ifPresent(EntityIndexDataStructure::clearTransactionalMemory);
		for (FacetGroupIndex facetGroupIndex : groupedFacets.values()) {
			facetGroupIndex.clearTransactionalMemory();
		}

		final FacetEntityTypeIndexChanges changes = TransactionalMemory.getTransactionalMemoryLayerIfExists(this);
		ofNullable(changes).ifPresent(it -> it.cleanAll(TransactionalMemory.getTransactionalMemoryLayer()));
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.facetToGroupIndex);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.groupedFacets);
	}

	/*
		Implementation of TransactionalLayerProducer
	 */

	@Override
	public FacetEntityTypeIndexChanges createLayer() {
		return new FacetEntityTypeIndexChanges();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public NonTransactionalCopy createCopyWithMergedTransactionalMemory(@Nullable FacetEntityTypeIndexChanges layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, @Nullable Transaction transaction) {
		final Map<Integer, Bitmap> noGroupCopy = this.notGroupedFacets == null ? null : transactionalLayer.getStateCopyWithCommittedChanges(this.notGroupedFacets, transaction);
		// this is a HACK - facet id indexes produce IntegerBitmap instead of type than generics would suggest
		final Map<Integer, Map<Integer, Bitmap>> groupCopy = (Map) transactionalLayer.getStateCopyWithCommittedChanges(this.groupedFacets, transaction);
		final Map<Integer, int[]> facetToGroupCopy = transactionalLayer.getStateCopyWithCommittedChanges(this.facetToGroupIndex, transaction);
		ofNullable(layer).ifPresent(it -> it.clean(transactionalLayer));
		return new NonTransactionalCopy(noGroupCopy, groupCopy, facetToGroupCopy);
	}

	@Data
	public static class NonTransactionalCopy {
		private Map<Integer, Bitmap> noGroup;
		private Map<Integer, Map<Integer, Bitmap>> groups;
		private Map<Integer, int[]> facetToGroupIndex;

		public NonTransactionalCopy(Map<Integer, Bitmap> noGroupCopy, Map<Integer, Map<Integer, Bitmap>> groupCopy, Map<Integer, int[]> facetToGroupCopy) {
			this.noGroup = noGroupCopy;
			this.groups = groupCopy;
			this.facetToGroupIndex = facetToGroupCopy;
		}

	}

	/**
	 * This class collects changes in {@link #groupedFacets} transactional map and its sub structure.
	 */
	public static class FacetEntityTypeIndexChanges {
		private final TransactionalContainerChanges<FacetGroupIndexChanges, Map<Integer, Bitmap>, FacetGroupIndex> items = new TransactionalContainerChanges<>();

		public void addCreatedItem(FacetGroupIndex baseIndex) {
			items.addCreatedItem(baseIndex);
		}

		public void addRemovedItem(FacetGroupIndex baseIndex) {
			items.addRemovedItem(baseIndex);
		}

		public void clean(TransactionalLayerMaintainer transactionalLayer) {
			items.clean(transactionalLayer);
		}

		public void cleanAll(TransactionalLayerMaintainer transactionalLayer) {
			items.cleanAll(transactionalLayer);
		}
	}

	@Data
	private static class GroupFacetIdDTO {
		private final FacetGroupIndex groupIndex;
		private final int facetId;
	}

}
