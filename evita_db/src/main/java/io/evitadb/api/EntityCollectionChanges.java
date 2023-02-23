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

package io.evitadb.api;

import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.storage.model.storageParts.PersistedStoragePartKey;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.StoragePartKey;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * This class keeps transactional layer changes for {@link EntityCollection}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EntityCollectionChanges {
	/**
	 * This map contains {@link io.evitadb.api.data.structure.Entity} body storage parts that needs to be persisted.
	 */
	private final Map<PersistedStoragePartKey, StoragePart> pendingStorageParts = new HashMap<>();
	/**
	 * This map contains {@link io.evitadb.api.data.structure.Entity} body compressed storage parts that needs to be persisted.
	 */
	private final Map<StoragePartKey, StoragePart> pendingStoragePartKeys = new HashMap<>();
	/**
	 * This map contains reference to all {@link EntityIndex} modified in this transaction.
	 */
	private final Map<EntityIndexKey, EntityIndex> dirtyEntityIndexes = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <T extends StoragePart> T getContainer(long primaryKey, @Nonnull Class<T> containerType) {
		return (T) pendingStorageParts.get(new PersistedStoragePartKey(primaryKey, containerType));
	}

	public <T extends StoragePart> boolean isRemoved(long primaryKey, @Nonnull Class<T> containerType) {
		return pendingStorageParts.get(new PersistedStoragePartKey(primaryKey, containerType)) instanceof RemovedStoragePart;
	}

	public <T extends StoragePart> boolean remove(long primaryKey, @Nonnull Class<T> entityClass) {
		return pendingStorageParts.put(new PersistedStoragePartKey(primaryKey, entityClass), RemovedStoragePart.INSTANCE) != null;
	}

	public <T extends StoragePart> void updateContainer(long primaryKey, @Nonnull T value) {
		this.pendingStorageParts.put(new PersistedStoragePartKey(primaryKey, value.getClass()), value);
	}

	@SuppressWarnings("unchecked")
	public <T extends StoragePart> T getContainer(@Nonnull Comparable<?> originalKey, @Nonnull Class<T> containerType) {
		return (T) pendingStoragePartKeys.get(new StoragePartKey(originalKey, containerType));
	}

	public <T extends StoragePart> boolean isRemoved(@Nonnull Comparable<?> originalKey, @Nonnull Class<T> containerType) {
		return pendingStoragePartKeys.get(new StoragePartKey(originalKey, containerType)) instanceof RemovedStoragePart;
	}

	public <T extends StoragePart> boolean remove(@Nonnull Comparable<?> originalKey, @Nonnull Class<T> containerType) {
		return this.pendingStoragePartKeys.remove(new StoragePartKey(originalKey, containerType)) != null;
	}

	public <T extends StoragePart> void updateContainer(@Nonnull Comparable<?> originalKey, @Nonnull T value) {
		pendingStoragePartKeys.put(new StoragePartKey(originalKey, value.getClass()), value);
	}

	public Collection<StoragePart> getModifiedStoragePartsToPersist() {
		return Stream.concat(
				Stream.concat(
					pendingStoragePartKeys
						.values()
						.stream(),
					pendingStorageParts
						.values()
						.stream()
						.filter(it -> !(it instanceof RemovedStoragePart))
				),
				dirtyEntityIndexes
					.values()
					.stream()
					.flatMap(it -> it.getModifiedStorageParts().stream())
			)
			.collect(Collectors.toList());
	}

	public Collection<PersistedStoragePartKey> getRemovedStoragePartsToPersist() {
		return pendingStorageParts
			.entrySet()
			.stream()
			.filter(it -> it.getValue() instanceof RemovedStoragePart)
			.map(Entry::getKey)
			.collect(Collectors.toList());
	}

	/**
	 * Method checks and returns the requested index from the local "dirty" memory. If it isn't there, it's fetched
	 * using `accessorWhenMissing` lambda and stores into the "dirty" memory before returning.
	 */
	public EntityIndex getOrCreateIndexForModification(@Nonnull EntityIndexKey entityIndexKey, @Nonnull Function<EntityIndexKey, EntityIndex> accessorWhenMissing) {
		return dirtyEntityIndexes.computeIfAbsent(
			entityIndexKey, accessorWhenMissing
		);
	}

	/**
	 * Method checks and returns the requested index from the local "dirty" memory. If it isn't there, it's fetched
	 * using `accessorWhenMissing` and returned without adding to "dirty" memory.
	 */
	public EntityIndex getIndexIfExists(@Nonnull EntityIndexKey entityIndexKey, @Nonnull Function<EntityIndexKey, EntityIndex> accessorWhenMissing) {
		return ofNullable(dirtyEntityIndexes.get(entityIndexKey))
			.orElseGet(() -> accessorWhenMissing.apply(entityIndexKey));
	}

	/**
	 * Removes {@link EntityIndex} from the change set. After removal (either successfully or unsuccessful)
	 * `removalPropagation` function is called to propagate deletion to the origin collection.
	 */
	public EntityIndex removeIndex(@Nonnull EntityIndexKey entityIndexKey, @Nonnull Function<EntityIndexKey, EntityIndex> removalPropagation) {
		final EntityIndex dirtyIndexesRemoval = dirtyEntityIndexes.remove(entityIndexKey);
		final EntityIndex baseIndexesRemoval = removalPropagation.apply(entityIndexKey);
		return ofNullable(dirtyIndexesRemoval).orElse(baseIndexesRemoval);
	}

	/**
	 * This class marks removed {@link StoragePart} in the transactional memory.
	 */
	private static class RemovedStoragePart implements StoragePart {
		private static final RemovedStoragePart INSTANCE = new RemovedStoragePart();
		private static final long serialVersionUID = 2485318464734970542L;

		@Override
		public Long getUniquePartId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
			throw new UnsupportedOperationException();
		}
	}

}
