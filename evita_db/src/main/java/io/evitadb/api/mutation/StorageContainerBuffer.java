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

import io.evitadb.api.EntityCollection;
import io.evitadb.api.EntityCollectionChanges;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.query.Query;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.serialization.exception.CompressionKeyUnknownException;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.storage.IOService;
import io.evitadb.storage.MemTable;
import io.evitadb.storage.model.memTable.MemTableRecordType;
import io.evitadb.storage.model.storageParts.RecordWithCompressedId;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.StoragePartKey;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.index.transactionalMemory.TransactionalMemory.getTransactionalMemoryLayer;
import static io.evitadb.index.transactionalMemory.TransactionalMemory.getTransactionalMemoryLayerIfExists;
import static java.util.Optional.ofNullable;

/**
 * StorageContainerBuffer represents volatile temporal memory between the {@link EntityCollection} and {@link MemTable}
 * that takes {@link io.evitadb.index.transactionalMemory.TransactionalMemory} into an account.
 *
 * All reads-writes are primarily targeting transactional memory if it's present for the current thread. If the value
 * is not found there it's located via {@link MemTable#get(long, Class)}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class StorageContainerBuffer {
	private final EntityCollection entityCollection;
	private final MemTable memTable;
	private final IOService ioService;
	/**
	 * DTO contains all trapped changes in this {@link StorageContainerBuffer}.
	 */
	private BufferedChangeSet bufferedChangeSet = new BufferedChangeSet();

	/**
	 * Collects information about dirty indexes that needs to be persisted. If transaction is opened, the changes
	 * are written only in the transactional layer and persisted at moment when transaction is committed. When
	 * transaction is not opened the changes are not immediately written to the {@link MemTable} but trapped in shared
	 * memory and will be written when buffer is {@link #exchangeBuffer() exchanged}. This is usually the case when
	 * entity index is just being created for the first time and the transactions were not yet enabled on it.
	 */
	public EntityIndex getOrCreateIndexForModification(@Nonnull EntityIndexKey entityIndexKey, @Nonnull Function<EntityIndexKey, EntityIndex> accessorWhenMissing) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayerIfExists(entityCollection);
		if (layer == null) {
			return bufferedChangeSet.getOrCreateIndexForModification(entityIndexKey, accessorWhenMissing);
		} else {
			return layer.getOrCreateIndexForModification(entityIndexKey, accessorWhenMissing);
		}
	}

	/**
	 * Returns {@link EntityIndex} by key if it already exists in change set. If the index is no present there
	 * `accessorWhenMissing` is executed to retrieve primary read-only index from the origin collection.
	 */
	public EntityIndex getIndexIfExists(EntityIndexKey entityIndexKey, @Nonnull Function<EntityIndexKey, EntityIndex> accessorWhenMissing) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayerIfExists(entityCollection);
		if (layer == null) {
			return bufferedChangeSet.getIndexIfExists(entityIndexKey, accessorWhenMissing);
		} else {
			return layer.getIndexIfExists(entityIndexKey, accessorWhenMissing);
		}
	}

	/**
	 * Removes {@link EntityIndex} from the change set. After removal (either successfully or unsuccessful)
	 * `removalPropagation` function is called to propagate deletion to the origin collection.
	 */
	public EntityIndex removeIndex(EntityIndexKey entityIndexKey, @Nonnull Function<EntityIndexKey, EntityIndex> removalPropagation) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayerIfExists(entityCollection);
		if (layer == null) {
			return bufferedChangeSet.removeIndex(entityIndexKey, removalPropagation);
		} else {
			return layer.removeIndex(entityIndexKey, removalPropagation);
		}
	}

	/**
	 * Reads container primarily from transactional memory and when the container is not present there (or transaction
	 * is not opened) reads it from the target {@link MemTable}.
	 */
	public <T extends StoragePart> T fetch(long primaryKey, @Nonnull Class<T> containerType) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayerIfExists(entityCollection);
		if (layer == null) {
			return memTable.get(primaryKey, containerType);
		} else if (layer.isRemoved(primaryKey, containerType)) {
			return null;
		} else {
			return ofNullable(layer.getContainer(primaryKey, containerType))
				.orElseGet(() -> memTable.get(primaryKey, containerType));
		}
	}

	/**
	 * Reads container primarily from transactional memory and when the container is not present there (or transaction
	 * is not opened) reads it from the target {@link MemTable}.
	 */
	public <T extends StoragePart, U extends Comparable<U>> T fetch(@Nonnull U originalKey, @Nonnull Class<T> containerType, @Nonnull BiFunction<KeyCompressor, U, Long> compressedKeyComputer) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayerIfExists(entityCollection);
		if (layer == null) {
			try {
				final long nonFlushedCompressedId = ofNullable(this.bufferedChangeSet.getNonFlushedCompressedId(originalKey))
					.orElseGet(() -> compressedKeyComputer.apply(memTable.getReadOnlyKeyCompressor(), originalKey));
				return memTable.get(nonFlushedCompressedId, containerType);
			} catch (CompressionKeyUnknownException ex) {
				// key wasn't yet assigned
				return null;
			}
		} else if (layer.isRemoved(originalKey, containerType)) {
			return null;
		} else {
			return ofNullable(layer.getContainer(originalKey, containerType))
				.orElseGet(() -> {
					try {
						final long nonFlushedCompressedId = ofNullable(this.bufferedChangeSet.getNonFlushedCompressedId(originalKey))
							.orElseGet(() -> compressedKeyComputer.apply(memTable.getReadOnlyKeyCompressor(), originalKey));
						return memTable.get(nonFlushedCompressedId, containerType);
					} catch (CompressionKeyUnknownException ex) {
						// key wasn't yet assigned
						return null;
					}
				});
		}
	}

	/**
	 * Removes container from the target storage. If transaction is open, it just marks the container as removed but
	 * doesn't really remove it.
	 */
	public <T extends StoragePart> boolean remove(long primaryKey, Class<T> entityClass) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayer(entityCollection);
		if (layer == null) {
			return memTable.remove(primaryKey, entityClass);
		} else {
			if (memTable.contains(primaryKey, entityClass)) {
				return layer.remove(primaryKey, entityClass);
			} else {
				return false;
			}
		}
	}

	/**
	 * Removes container from the transactional memory. This method should be used only for container, that has
	 * no uniqueId assigned so far (e.g. they haven't been stored yet).
	 */
	public <T extends StoragePart> boolean remove(Comparable<?> originalKey, Class<T> entityClass) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayer(entityCollection);
		if (layer == null) {
			return false;
		} else {
			return layer.remove(originalKey, entityClass);
		}
	}

	/**
	 * Inserts or updates container in the target storage. If transaction is opened, the changes are written only in
	 * the transactional layer and are not really written to the {@link MemTable}. Changes are written at the moment
	 * when transaction is committed.
	 */
	public <T extends StoragePart> void update(@Nonnull T value) {
		final EntityCollectionChanges layer = getTransactionalMemoryLayer(entityCollection);
		if (layer == null) {
			final long partId = memTable.put(0L, value);
			if (value instanceof RecordWithCompressedId) {
				this.bufferedChangeSet.setNonFlushedCompressedId(((RecordWithCompressedId<?>) value).getStoragePartSourceKey(), partId);
			}
		} else {
			boolean updated = false;
			final Long uniquePartId = value.getUniquePartId();
			if (uniquePartId != null) {
				layer.updateContainer((long) uniquePartId, value);
				updated = true;
			}
			if (value instanceof RecordWithCompressedId) {
				layer.updateContainer(((RecordWithCompressedId<?>) value).getStoragePartSourceKey(), value);
				updated = true;
			}
			Assert.isTrue(updated, "Stored value must either implement RecordWithCompressedId interface or provide uniquePartId! Object " + value + " does neither!");
		}
	}

	/**
	 * Returns count of entities of certain type in the target storage.
	 *
	 * <strong>Note:</strong> the count may not be accurate - it counts only already persisted containers to the
	 * {@link MemTable} and doesn't take transactional memory into an account.
	 */
	public <T extends StoragePart> int count(Class<T> containerClass) {
		return memTable.count(containerClass);

	}

	/**
	 * Returns iterator that goes through all containers of certain type in the target storage.
	 *
	 * <strong>Note:</strong> the list may not be accurate - it only goes through already persisted containers to the
	 * {@link MemTable} and doesn't take transactional memory into an account.
	 */
	public Iterator<Entity> entityIterator() {
		final EvitaRequest evitaRequest = new EvitaRequest(
			Query.query(
				entities(entityCollection.getSchema().getName()),
				require(fullEntity())
			),
			ZonedDateTime.now()
		);
		final byte recType = MemTableRecordType.idFor(EntityBodyStoragePart.class);
		return memTable
			.getEntries()
			.stream()
			.filter(it -> it.getKey().getRecordType() == recType)
			.map(it -> memTable.get(it.getValue(), EntityBodyStoragePart.class))
			.filter(Objects::nonNull)
			.map(it -> ioService.toEntity(evitaRequest, entityCollection.getSchema(), it, this))
			.iterator();

	}

	/**
	 * Method returns current buffer with trapped changes and creates new one, that starts fill in.
	 * This method doesn't take transactional memory into an account but contains only changes for trapped updates.
	 */
	public BufferedChangeSet exchangeBuffer() {
		// store current trapped updates
		final BufferedChangeSet oldChangeSet = this.bufferedChangeSet;
		// create new trapped updates containers - so that simultaneous updates from other threads doesn't affect this flush
		this.bufferedChangeSet = new BufferedChangeSet();
		// return old buffered change set
		return oldChangeSet;
	}

	/**
	 * DTO contains all trapped changes in this {@link StorageContainerBuffer}.
	 */
	@NotThreadSafe
	public static class BufferedChangeSet {
		/**
		 * This map contains compressed ids that were created when {@link StoragePart} was stored to the {@link MemTable},
		 * but the {@link MemTable} was not yet flushed so that compressed ids are still trapped in non-stored instance of
		 * the {@link io.evitadb.storage.model.ReadWriteKeyCompressor}.
		 *
		 * Data stored in {@link MemTable} are not considered durable until {@link MemTable} is flushed and stores also
		 * key to file-locations map and propagates itself to the header file.
		 */
		private final Map<Comparable<?>, Long> nonFlushedCompressedId = new ConcurrentHashMap<>();
		/**
		 * This map contains index of "dirty" entity indexes - i.e. subset of {@link EntityCollection#indexes} that were
		 * modified and not yet persisted.
		 */
		private final Map<EntityIndexKey, EntityIndex> dirtyEntityIndexes = new ConcurrentHashMap<>();

		/**
		 * Returns non-flushed compressed id by its key.
		 */
		public <U extends Comparable<U>> Long getNonFlushedCompressedId(U originalKey) {
			return nonFlushedCompressedId.get(originalKey);
		}

		/**
		 * Associates key with compressed id.
		 */
		public void setNonFlushedCompressedId(Comparable<?> originalKey, long compressedId) {
			nonFlushedCompressedId.put(originalKey, compressedId);
		}

		/**
		 * Returns set containing {@link StoragePartKey keys} that lead to the data structures in memory that were modified
		 * (are dirty) and needs to be persisted into the {@link MemTable}. This is performance optimization that minimizes
		 * I/O operations for frequently changed data structures such as indexes and these are stored once in a while in
		 * the moments when it has a sense.
		 */
		public Stream<StoragePart> getTrappedMemTableUpdates() {
			return dirtyEntityIndexes
				.values()
				.stream()
				.flatMap(it -> it.getModifiedStorageParts().stream());
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
			return ofNullable(dirtyEntityIndexes.get(entityIndexKey)).orElseGet(() -> accessorWhenMissing.apply(entityIndexKey));
		}

		/**
		 * Removes {@link EntityIndex} from the change set. After removal (either successfully or unsuccessful)
		 * `removalPropagation` function is called to propagate deletion to the origin collection.
		 */
		public EntityIndex removeIndex(EntityIndexKey entityIndexKey, Function<EntityIndexKey, EntityIndex> removalPropagation) {
			final EntityIndex dirtyIndexesRemoval = dirtyEntityIndexes.remove(entityIndexKey);
			final EntityIndex baseIndexesRemoval = removalPropagation.apply(entityIndexKey);
			return ofNullable(dirtyIndexesRemoval).orElse(baseIndexesRemoval);
		}
	}

}
