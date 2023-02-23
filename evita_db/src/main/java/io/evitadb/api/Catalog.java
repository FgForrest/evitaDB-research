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

import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.exception.InvalidSchemaMutationException;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.cache.CacheSupervisor;
import io.evitadb.cache.HeapMemoryCacheSupervisor;
import io.evitadb.cache.NoCacheSupervisor;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.scheduling.Scheduler;
import io.evitadb.sequence.SequenceService;
import io.evitadb.sequence.SequenceType;
import io.evitadb.storage.IOService;
import io.evitadb.storage.MemTable;
import io.evitadb.storage.ObservableOutputKeeper;
import io.evitadb.storage.kryo.ObservableOutput;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.CatalogHeader;
import io.evitadb.storage.model.storageParts.EntityCollectionUpdateInstruction;
import lombok.Getter;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * Catalog is an abstraction for "database" in the sense of relational databases. Catalog contains all entities and data
 * connected with single client. In the e-commerce world catalog means "single e-shop" although it may not be the truth
 * in every case. Catalog manages set of {@link EntityCollection} uniquely identified by their {@link EntityCollection#getName()}.
 *
 * Each entity is stored in separate {@link MemTable} data store.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Catalog extends CatalogBase<EvitaRequest, EvitaCatalogConfiguration, EntityCollection> {
	/**
	 * Memory store for catalogs.
	 */
	final TransactionalMemoryMap<Serializable, EntityCollection> entityCollections;
	/**
	 * This instance keeps references to the {@link ObservableOutput} instances that internally keep large buffers in
	 * {@link ObservableOutput#getBuffer()} to use them for serialization. There buffers are not necessary when there are
	 * no updates to the catalog / collection so it's wise to get rid of them if there is no actual need.
	 */
	final ObservableOutputKeeper observableOutputKeeper;
	/**
	 * Contains count of concurrently opened read-write sessions connected with this catalog.
	 * This information is used to control lifecycle of {@link #observableOutputKeeper} object.
	 */
	final AtomicInteger readWriteSessionCount;
	/**
	 * Contains last given transaction id - it represents sequence number that allows to generate unique number across
	 * multiple clients.
	 */
	final AtomicLong txPkSequence;
	/**
	 * Service containing I/O related methods.
	 */
	final IOService ioService;
	/**
	 * This field contains flag with TRUE value if catalog is being switched to {@link CatalogState#ALIVE} state.
	 */
	final AtomicBoolean goingLive = new AtomicBoolean();
	/**
	 * Formula supervisor is an entry point to the Evita cache. The idea is that each {@link Formula} can be identified by
	 * its {@link Formula#computeHash(LongHashFunction)} method and when the supervisor identifies that certain formula
	 * is frequently used in query formulas it moves its memoized results to the cache. The non-computed formula
	 * of the same hash will be exchanged in next query that contains it with the cached formula that already contains
	 * memoized result.
	 */
	final CacheSupervisor cacheSupervisor;
	/**
	 * Contains id of the transaction ({@link Transaction#getId()}) that was successfully committed to the disk.
	 */
	@Getter long lastCommittedTransactionId;

	public Catalog(@Nonnull EvitaCatalogConfiguration configuration, @Nonnull Scheduler executorService, @Nonnull ReflectionLookup reflectionLookup) {
		super(configuration);
		this.ioService = new IOService(reflectionLookup);
		this.ioService.verifyDirectory(configuration.getStorageDirectory(), configuration.getStorageOptions().isBootEmpty());
		this.observableOutputKeeper = new ObservableOutputKeeper(configuration.getStorageOptions());

		final CatalogHeader catalogHeader = ofNullable(this.ioService.readHeader(configuration.getStorageDirectory(), configuration.getName()))
			.orElseGet(() -> new CatalogHeader(configuration.getName(), getCatalogState()));
		this.setCatalogState(catalogHeader.getCatalogState());
		this.readWriteSessionCount = new AtomicInteger(0);
		this.lastCommittedTransactionId = catalogHeader.getLastTransactionId();
		this.txPkSequence = SequenceService.getOrCreateSequence(getName(), SequenceType.TRANSACTION, this.lastCommittedTransactionId);
		this.cacheSupervisor = configuration.getCacheOptions().isEnableFormulaCache() ?
			new HeapMemoryCacheSupervisor(configuration.getCacheOptions(), executorService) : NoCacheSupervisor.INSTANCE;

		final Map<Serializable, EntityCollection> collections = createHashMap(catalogHeader.getEntityTypeHeaders().size());
		for (CatalogEntityHeader entityHeader : catalogHeader.getEntityTypeHeaders()) {
			collections.put(
				entityHeader.getEntityType(),
				new EntityCollection(
					this,
					catalogHeader,
					entityHeader,
					configuration.getStorageDirectory(),
					configuration.getStorageOptions(),
					observableOutputKeeper,
					ioService,
					cacheSupervisor,
					supportsTransaction()
				)
			);
		}
		this.entityCollections = new TransactionalMemoryMap<>(collections);
		this.cacheSupervisor.checkFreeMemory();
	}

	Catalog(
		@Nonnull EvitaCatalogConfiguration configuration,
		@Nonnull CatalogState catalogState,
		@Nonnull IOService ioService,
		@Nonnull CacheSupervisor cacheSupervisor,
		@Nonnull ObservableOutputKeeper observableOutputKeeper,
		@Nonnull AtomicInteger readWriteSessionCount,
		@Nonnull AtomicLong txPkSequence,
		long lastCommittedTransactionId,
		@Nonnull Map<Serializable, EntityCollection> entityCollections
	) {
		super(configuration, catalogState);
		this.ioService = ioService;
		this.cacheSupervisor = cacheSupervisor;
		this.observableOutputKeeper = observableOutputKeeper;
		this.readWriteSessionCount = readWriteSessionCount;
		this.txPkSequence = txPkSequence;
		this.lastCommittedTransactionId = lastCommittedTransactionId;
		this.entityCollections = new TransactionalMemoryMap<>(entityCollections);
	}

	/**
	 * Returns {@link EntitySchema} for passed `entityType` or throws {@link IllegalArgumentException} if schema for
	 * this type is not yet known.
	 */
	@Nullable
	public EntitySchema getEntitySchema(@Nonnull Serializable entityType) {
		return ofNullable(entityCollections.get(entityType))
			.map(EntityCollectionBase::getSchema)
			.orElse(null);
	}

	/**
	 * Returns {@link EntitySchema} for passed `entityType` or throws {@link IllegalArgumentException} if schema for
	 * this type is not yet known.
	 */
	@Nullable
	public EntityIndex getEntityIndexIfExists(@Nonnull Serializable entityType, @Nonnull EntityIndexKey indexKey) {
		final EntityCollection targetCollection = ofNullable(entityCollections.get(entityType))
			.orElseThrow(() -> new IllegalArgumentException("Entity collection of type " + entityType + " doesn't exist!"));
		return targetCollection.getIndexByKeyIfExists(indexKey);
	}

	/**
	 * Returns next unique transaction id for the catalog.
	 */
	public long getNextTransactionId() {
		return txPkSequence.incrementAndGet();
	}

	/**
	 * Increases number of read and write sessions that are currently talking with this catalog.
	 */
	public void increaseReadWriteSessionCount() {
		if (readWriteSessionCount.getAndIncrement() == 0) {
			observableOutputKeeper.prepare();
		}
	}

	/**
	 * Decreases number of read and write sessions that are currently talking with this catalog.
	 * When session count reaches zero - opened output buffers are released to free memory.
	 */
	public void decreaseReadWriteSessionCount() {
		if (readWriteSessionCount.decrementAndGet() == 0) {
			observableOutputKeeper.free();
		}
	}

	@Override
	public Set<Serializable> getEntityTypes() {
		return entityCollections.keySet();
	}

	@Nullable
	@Override
	public EntityCollection getCollectionForEntity(@Nonnull Serializable entityType) {
		return entityCollections.get(entityType);
	}

	@Nonnull
	@Override
	public EntityCollection getOrCreateCollectionForEntity(@Nonnull Serializable entityType) {
		return entityCollections.computeIfAbsent(
			entityType,
			entityTypeSeed -> {
				this.ioService.verifyEntityType(
					getEntityTypes().stream(),
					entityType
				);
				return new EntityCollection(
					this,
					new CatalogHeader(getName(), getCatalogState()),
					new CatalogEntityHeader(entityType),
					getConfiguration().getStorageDirectory(),
					getConfiguration().getStorageOptions(),
					observableOutputKeeper,
					ioService,
					cacheSupervisor,
					supportsTransaction()
				);
			}
		);
	}

	@Nonnull
	@Override
	public EntityCollection createCollectionForEntity(@Nonnull EntitySchema entitySchema) throws InvalidSchemaMutationException {
		final EntityCollection newEntityCollection = new EntityCollection(
			this,
			entitySchema,
			new CatalogHeader(getName(), getCatalogState()),
			new CatalogEntityHeader(entitySchema.getName()),
			getConfiguration().getStorageDirectory(),
			getConfiguration().getStorageOptions(),
			observableOutputKeeper,
			ioService,
			cacheSupervisor,
			supportsTransaction()
		);
		final EntityCollection entityCollection = entityCollections.computeIfAbsent(
			entitySchema.getName(),
			serializable -> newEntityCollection
		);
		if (newEntityCollection != entityCollection) {
			throw new InvalidSchemaMutationException("Schema for entity type " + entitySchema.getName() + " already exists!");
		}
		return entityCollection;
	}

	@Override
	public boolean deleteCollectionOfEntity(@Nonnull Serializable entityType) {
		return entityCollections.remove(entityType) != null;
	}

	@Override
	boolean goLive() {
		try {
			Assert.isTrue(
				goingLive.compareAndSet(false, true),
				"Concurrent call of `goLive` method is not supported!"
			);
			return super.goLive();
		} finally {
			goingLive.set(false);
		}
	}

	/**
	 * This method stores {@link CatalogEntityHeader} in case there were any changes in the {@link MemTable} executed
	 * in BULK / non-transactional mode.
	 */
	@Override
	public void flush() {
		boolean changeOccurred = false;
		Assert.isTrue(
			getCatalogState() == CatalogState.WARMING_UP,
			() -> new IllegalStateException("Cannot flush catalog that is in transactional mode. Any changes could occur only in transactions!")
		);
		final List<CatalogEntityHeader> entityHeaders = new ArrayList<>(this.entityCollections.size());
		for (EntityCollection entityCollection : entityCollections.values()) {
			final long lastSeenVersion = entityCollection.getVersion();
			entityHeaders.add(entityCollection.flush());
			changeOccurred |= entityCollection.getVersion() != lastSeenVersion;
		}

		if (changeOccurred) {
			this.ioService.storeHeader(
				getConfiguration().getStorageDirectory(),
				getName(),
				goingLive.get() ? CatalogState.ALIVE : getCatalogState(),
				lastCommittedTransactionId,
				entityHeaders
			);
		}
	}

	@Override
	void terminate() {
		final List<CatalogEntityHeader> entityHeaders;
		boolean changeOccurred = false;
		try {
			observableOutputKeeper.prepare();
			final boolean warmingUpState = getCatalogState() == CatalogState.WARMING_UP;
			entityHeaders = new ArrayList<>(this.entityCollections.size());
			for (EntityCollection entityCollection : entityCollections.values()) {
				// in warmup state try to persist all changes in volatile memory
				if (warmingUpState) {
					final long lastSeenVersion = entityCollection.getVersion();
					entityHeaders.add(entityCollection.flush());
					changeOccurred |= entityCollection.getVersion() != lastSeenVersion;
				}
				// in all states terminate collection operations
				entityCollection.terminate();
			}
		} finally {
			observableOutputKeeper.free();
		}

		// if any change occurred (this may happen only in warm up state)
		if (changeOccurred) {
			// store catalog header
			this.ioService.storeHeader(
				getConfiguration().getStorageDirectory(),
				getName(), getCatalogState(),
				lastCommittedTransactionId,
				entityHeaders
			);
		}
		// close all resources here, here we just hand all objects to GC
		entityCollections.clear();
	}

	/**
	 * This method writes all changed storage parts into the {@link MemTable} of {@link EntityCollection} and then stores
	 * {@link CatalogHeader} marking transactionId as committed.
	 */
	public void flush(long transactionId, @Nonnull Map<Serializable, List<EntityCollectionUpdateInstruction>> storagePartsToPersist) {
		boolean changeOccurred = false;
		final List<CatalogEntityHeader> entityHeaders = new ArrayList<>(this.entityCollections.size());
		for (EntityCollection entityCollection : entityCollections.values()) {
			final Serializable entityType = entityCollection.getSchema().getName();
			final long lastSeenVersion = entityCollection.getVersion();
			final List<EntityCollectionUpdateInstruction> entityStoragePartsToPersist = ofNullable(storagePartsToPersist.get(entityType)).orElse(Collections.emptyList());
			entityHeaders.add(entityCollection.flush(transactionId, entityStoragePartsToPersist));
			changeOccurred |= entityCollection.getVersion() != lastSeenVersion;
		}

		if (changeOccurred) {
			this.ioService.storeHeader(
				getConfiguration().getStorageDirectory(),
				getName(), getCatalogState(),
				transactionId, entityHeaders
			);
			this.lastCommittedTransactionId = transactionId;
		}
	}

}
