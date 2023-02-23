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

import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.structure.*;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.exception.ConcurrentSchemaUpdateException;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.exception.SchemaAlteringException;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.mutation.ContainerizedLocalMutationExecutor;
import io.evitadb.api.mutation.EntityIndexLocalMutationExecutor;
import io.evitadb.api.mutation.StorageContainerBuffer;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import io.evitadb.cache.CacheSupervisor;
import io.evitadb.index.*;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.index.price.PriceRefIndex;
import io.evitadb.index.price.PriceSuperIndex;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import io.evitadb.query.QueryExecutor;
import io.evitadb.query.QueryPlan;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.response.QueryTelemetry;
import io.evitadb.query.response.QueryTelemetry.QueryPhase;
import io.evitadb.sequence.SequenceService;
import io.evitadb.sequence.SequenceType;
import io.evitadb.storage.IOService;
import io.evitadb.storage.MemTable;
import io.evitadb.storage.ObservableOutputKeeper;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.CatalogHeader;
import io.evitadb.storage.model.memTable.MemTableDescriptor;
import io.evitadb.storage.model.storageParts.EntityCollectionUpdateInstruction;
import io.evitadb.storage.model.storageParts.PersistedStoragePartKey;
import io.evitadb.storage.model.storageParts.RecordWithCompressedId;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import io.evitadb.storage.model.storageParts.schema.EntitySchemaContainer;
import lombok.Getter;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Optional.ofNullable;

/**
 * EntityCollection is set of records of the same type. In the relational world it would represent a table (or a single
 * main table with several other tables containing records referring to that main table). Entity collection maintains
 * all entities of the same type (i.e. same {@link EntitySchema}).
 *
 * TOBEDONE JNO - when abstractions over different implementations gets flattened we need to pass EvitaSession references
 * to the methods of entity collection so we can ensure that the session is always not null in QueryContext!!!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EntityCollection extends EntityCollectionBase<EvitaRequest> implements TransactionalLayerProducer<EntityCollectionChanges, EntityCollection> {
	/**
	 * EntityIndex factory implementation.
	 */
	protected final EntityIndexMaintainer entityIndexCreator = new EntityIndexMaintainerImpl();
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * This field contains reference to the CURRENT {@link Catalog} instance allowing to access {@link EntityCollection}
	 * for any of entity types that are known to the catalog this collection is part of. Reference to other collections
	 * is used to access their schema or their indexes from this collection.
	 *
	 * The reference pointer is used because when transaction is committed and new catalog is created to atomically swap
	 * changes and left old readers finish with old catalog, the entity collection copy is created, and we need to init
	 * the reference to this function lazily when new catalog is instantiated (existence of the new collection precedes
	 * the creation of the catalog copy).
	 */
	private final AtomicReference<Catalog> catalogAccessor;
	/**
	 * Memory key-value store for entities.
	 */
	private final MemTable memTable;
	/**
	 * This holds sequence that allows automatic assigning monotonic primary keys to the entities.
	 */
	private final AtomicInteger pkSequence;
	/**
	 * This holds sequence that allows assigning monotonic primary keys to the entity indexes.
	 */
	private final AtomicInteger indexPkSequence;
	/**
	 * Service that contains I/O related methods.
	 */
	private final IOService ioService;
	/**
	 * Collection of search indexes prepared to handle queries.
	 */
	private final TransactionalMemoryMap<EntityIndexKey, EntityIndex> indexes;
	/**
	 * True if collection was already terminated. No other termination will be allowed.
	 */
	private final AtomicBoolean terminated = new AtomicBoolean(false);
	/**
	 * This instance is used to cover changes in transactional memory and {@link #memTable} reference.
	 */
	private final StorageContainerBuffer storageContainerBuffer;
	/**
	 * Formula supervisor is an entry point to the Evita cache. The idea is that each {@link Formula} can be identified
	 * by its {@link Formula#computeHash(LongHashFunction)} method and when the supervisor identifies that certain
	 * formula is frequently used in query formulas it moves its memoized results to the cache. The non-computed formula
	 * of the same hash will be exchanged in next query that contains it with the cached formula that already contains
	 * memoized result.
	 */
	private final CacheSupervisor cacheSupervisor;
	/**
	 * Contains current version of the catalog entity header which gets updated on flush.
	 */
	private CatalogEntityHeader catalogEntityHeader;
	/**
	 * Contains function that takes care of primary key generation if it's required by this {@link #getSchema()}.
	 */
	private Consumer<EntityMutation> primaryKeyHandler;

	public EntityCollection(
		@Nonnull Catalog catalog,
		@Nonnull EntitySchema entitySchema,
		@Nonnull CatalogHeader catalogHeader,
		@Nonnull CatalogEntityHeader entityHeader,
		@Nonnull Path storageDirectory,
		@Nonnull StorageOptions storageOptions,
		@Nonnull ObservableOutputKeeper observableOutputKeeper,
		@Nonnull IOService ioService,
		@Nonnull CacheSupervisor cacheSupervisor,
		boolean supportsTransactions
	) {
		this.ioService = ioService;
		this.cacheSupervisor = cacheSupervisor;
		this.pkSequence = SequenceService.getOrCreateSequence(
			catalogHeader.getCatalogName(), SequenceType.ENTITY, entityHeader.getEntityType(), entityHeader.getLastPrimaryKey()
		);
		this.indexPkSequence = SequenceService.getOrCreateSequence(
			catalogHeader.getCatalogName(), SequenceType.INDEX, entityHeader.getEntityType(), entityHeader.getLastEntityIndexPrimaryKey()
		);
		this.catalogEntityHeader = entityHeader;
		this.catalogAccessor = new AtomicReference<>(catalog);

		this.ioService.verifyDirectory(storageDirectory, false);
		this.ioService.verifyEntityType(
			catalogHeader.getEntityTypes().stream(),
			entitySchema.getName()
		);

		this.memTable = new MemTable(
			this.ioService.getPathForEntityType(storageDirectory, entityHeader.getEntityType()),
			new MemTableDescriptor(
				entityHeader,
				this.ioService.createTypeKryoInstance(this::getSchema),
				supportsTransactions
			),
			storageOptions,
			observableOutputKeeper
		);
		// initialize container buffer
		this.storageContainerBuffer = new StorageContainerBuffer(this, this.memTable, this.ioService);
		// initialize schema - still in constructor
		this.schema.set(entitySchema);
		// store newly created schema
		this.memTable.put(0L, new EntitySchemaContainer(entitySchema));
		// init new empty entity indexes
		this.indexes = new TransactionalMemoryMap<>(new HashMap<>());
	}

	public EntityCollection(
		@Nonnull Catalog catalog,
		@Nonnull CatalogHeader catalogHeader,
		@Nonnull CatalogEntityHeader entityHeader,
		@Nonnull Path storageDirectory,
		@Nonnull StorageOptions storageOptions,
		@Nonnull ObservableOutputKeeper observableOutputKeeper,
		@Nonnull IOService ioService,
		@Nonnull CacheSupervisor cacheSupervisor,
		boolean supportsTransactions
	) {
		this.ioService = ioService;
		this.cacheSupervisor = cacheSupervisor;
		this.pkSequence = SequenceService.getOrCreateSequence(
			catalogHeader.getCatalogName(), SequenceType.ENTITY, entityHeader.getEntityType(), entityHeader.getLastPrimaryKey()
		);
		this.indexPkSequence = SequenceService.getOrCreateSequence(
			catalogHeader.getCatalogName(), SequenceType.INDEX, entityHeader.getEntityType(), entityHeader.getLastEntityIndexPrimaryKey()
		);
		this.catalogEntityHeader = entityHeader;
		this.catalogAccessor = new AtomicReference<>(catalog);

		this.ioService.verifyDirectory(storageDirectory, false);

		this.memTable = new MemTable(
			this.ioService.getPathForEntityType(storageDirectory, entityHeader.getEntityType()),
			new MemTableDescriptor(
				entityHeader,
				this.ioService.createTypeKryoInstance(this::getSchema),
				supportsTransactions
			),
			storageOptions,
			observableOutputKeeper
		);
		// initialize container buffer
		this.storageContainerBuffer = new StorageContainerBuffer(this, this.memTable, this.ioService);
		// initialize schema - still in constructor
		this.schema.set(
			ofNullable(memTable.get(1, EntitySchemaContainer.class))
				.map(EntitySchemaContainer::getEntitySchema)
				.orElseGet(() -> new EntitySchema(entityHeader.getEntityType()))
		);
		// init entity indexes
		if (entityHeader.getGlobalEntityIndexId() == null) {
			Assert.isTrue(
				entityHeader.getUsedEntityIndexIds().isEmpty(),
				() -> new IllegalStateException(
					"Unexpected situation - global index doesn't exists but there are " +
						entityHeader.getUsedEntityIndexIds().size() + " reduced indexes!"
				)
			);
			this.indexes = new TransactionalMemoryMap<>(new HashMap<>());
		} else {
			this.indexes = loadIndexes(entityHeader);
		}
		// sanity check whether we deserialized the memtable we expect to
		Assert.isTrue(
			entityHeader.getEntityType().equals(getSchema().getName()),
			"Deserialized schema name differs from expected entity type - expected " + entityHeader.getEntityType() + " got " + getSchema().getName()
		);
	}

	private EntityCollection(
		@Nonnull EntitySchema entitySchema,
		@Nonnull CatalogEntityHeader entityHeader,
		@Nonnull MemTable memTable,
		@Nonnull AtomicInteger pkSequence,
		@Nonnull AtomicInteger indexPkSequence,
		@Nonnull IOService ioService,
		@Nonnull Map<EntityIndexKey, EntityIndex> indexes,
		@Nonnull CacheSupervisor cacheSupervisor
	) {
		super(entitySchema);
		this.memTable = memTable;
		this.catalogEntityHeader = entityHeader;
		this.catalogAccessor = new AtomicReference<>();
		this.pkSequence = pkSequence;
		this.ioService = ioService;
		this.indexPkSequence = indexPkSequence;
		this.storageContainerBuffer = new StorageContainerBuffer(this, memTable, ioService);
		this.indexes = new TransactionalMemoryMap<>(indexes);
		for (EntityIndex entityIndex : this.indexes.values()) {
			entityIndex.updateReferencesTo(this);
		}
		this.cacheSupervisor = cacheSupervisor;
	}

	@Nonnull
	@Override
	public <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull EvitaRequest evitaRequest) {
		// TOBEDONE JNO - session should be passed as argument
		return getEntities(evitaRequest, new EvitaSession(catalogAccessor.get()));
	}

	@Nullable
	@Override
	public SealedEntity getEntity(int primaryKey, @Nonnull EvitaRequest evitaRequest) {
		// retrieve current version of entity
		final SealedEntity entity = cacheSupervisor.analyse(
			new EvitaSession(catalogAccessor.get()),
			primaryKey,
			getSchema().getName(),
			evitaRequest.getRequiredContents(),
			() -> {
				final Entity internalEntity = ioService.readEntity(
					primaryKey,
					evitaRequest,
					getSchema(),
					storageContainerBuffer
				);
				if (internalEntity == null) {
					return null;
				} else {
					return Entity.decorate(
						internalEntity,
						getSchema(),
						new HierarchicalContractSerializablePredicate(),
						new AttributeValueSerializablePredicate(evitaRequest),
						new AssociatedDataValueSerializablePredicate(evitaRequest),
						new ReferenceContractSerializablePredicate(evitaRequest),
						new PriceContractSerializablePredicate(evitaRequest)
					);
				}
			},
			theEntity -> enrichEntity(theEntity, evitaRequest),
			theEntity -> limitEntity(theEntity, evitaRequest)
		);

		return ofNullable(entity)
			.filter(Droppable::exists)
			.orElse(null);
	}

	/**
	 * Method returns entity with limited scope of data visibility that matches the passed `evitaRequest`. This method
	 * is particularly useful for implementation of cache when the cache might contain fully loaded entities while
	 * the client requests (and expects) only small part of it. This method allows to "hide" the data that exists in
	 * the entity but that we don't want to reveal to the client.
	 *
	 * @param evitaRequest - request has no filter / order - only envelopes additional requirements for the loaded entity,
	 *                     so that utility methods in request can be reused
	 */
	public SealedEntity limitEntity(@Nonnull SealedEntity sealedEntity, @Nonnull EvitaRequest evitaRequest) {
		final EntityDecorator widerEntity = (EntityDecorator) sealedEntity;
		final AttributeValueSerializablePredicate newAttributePredicate = new AttributeValueSerializablePredicate(evitaRequest);
		final AssociatedDataValueSerializablePredicate newAssociatedDataPredicate = new AssociatedDataValueSerializablePredicate(evitaRequest);
		final ReferenceContractSerializablePredicate newReferenceContractPredicate = new ReferenceContractSerializablePredicate(evitaRequest);
		final PriceContractSerializablePredicate newPricePredicate = new PriceContractSerializablePredicate(evitaRequest, widerEntity.getPricePredicate());
		return new EntityDecorator(
			widerEntity.getDelegate(),
			// use original schema
			widerEntity.getSchema(),
			// show / hide hierarchical placement information
			widerEntity.getHierarchicalPlacementPredicate(),
			// show / hide attributes information
			newAttributePredicate,
			// show / hide associated data information
			newAssociatedDataPredicate,
			// show / hide references information
			newReferenceContractPredicate,
			// show / hide price information
			newPricePredicate
		);
	}

	@Override
	public SealedEntity enrichEntity(@Nonnull SealedEntity sealedEntity, @Nonnull EvitaRequest evitaRequest) {
		final EntityDecorator partiallyLoadedEntity = (EntityDecorator) sealedEntity;
		// return decorator that hides information not requested by original query
		final AttributeValueSerializablePredicate oldAttributePredicate = partiallyLoadedEntity.getAttributePredicate();
		final AttributeValueSerializablePredicate newAttributePredicate = oldAttributePredicate.createRicherCopyWith(evitaRequest);
		final AssociatedDataValueSerializablePredicate oldAssociatedDataPredicate = partiallyLoadedEntity.getAssociatedDataPredicate();
		final AssociatedDataValueSerializablePredicate newAssociatedDataPredicate = oldAssociatedDataPredicate.createRicherCopyWith(evitaRequest);
		final ReferenceContractSerializablePredicate oldReferencePredicate = partiallyLoadedEntity.getReferencePredicate();
		final ReferenceContractSerializablePredicate newReferenceContractPredicate = oldReferencePredicate.createRicherCopyWith(evitaRequest);
		final PriceContractSerializablePredicate oldPricePredicate = partiallyLoadedEntity.getPricePredicate();
		final PriceContractSerializablePredicate newPriceContractPredicate = oldPricePredicate.createRicherCopyWith(evitaRequest);

		if (newAttributePredicate == oldAttributePredicate &&
			newAssociatedDataPredicate == oldAssociatedDataPredicate &&
			newReferenceContractPredicate == oldReferencePredicate &&
			newPriceContractPredicate == oldPricePredicate
		) {
			// all new predicates are same as previous ones - we can return the same entity
			return sealedEntity;
		} else {
			// new predicates are richer that previous ones - we need to fetch additional data and create new entity
			return new EntityDecorator(
				// load all missing data according to current evita request
				this.ioService.enrichEntity(
					// use all data from existing entity
					partiallyLoadedEntity,
					newAttributePredicate,
					newAssociatedDataPredicate,
					newReferenceContractPredicate,
					newPriceContractPredicate,
					storageContainerBuffer
				),
				// use original schema
				partiallyLoadedEntity.getSchema(),
				// show / hide hierarchical placement information
				partiallyLoadedEntity.getHierarchicalPlacementPredicate(),
				// show / hide attributes information
				newAttributePredicate,
				// show / hide associated data information
				newAssociatedDataPredicate,
				// show / hide references information
				newReferenceContractPredicate,
				// show / hide price information
				newPriceContractPredicate
			);
		}
	}

	@Nonnull
	@Override
	public EntityReference upsertEntity(@Nonnull EntityMutation entityMutation) throws InvalidMutationException {
		// verify mutation against schema
		// it was already executed when mutation was created, but there are two reasons to do it again
		// - we don't trust clients - in future it may be some external JS application
		// - schema may changed between entity was provided to the client and the moment upsert was called
		final EntitySchema actualSchema = updateSchemaWithRetry(
			schema -> entityMutation.verifyOrEvolveSchema(schema, this::updateSchema)
		);
		getOrCreatePrimaryKeyHandler(entityMutation.getEntityPrimaryKey() != null)
			.accept(entityMutation);

		Assert.notNull(
			entityMutation.getEntityPrimaryKey(),
			"PK should be always present here."
		);

		final ContainerizedLocalMutationExecutor changeCollector = new ContainerizedLocalMutationExecutor(
			storageContainerBuffer,
			entityMutation.getEntityPrimaryKey(),
			entityMutation.expects(),
			this::getSchema
		);
		final EntityIndexLocalMutationExecutor entityIndexUpdater = new EntityIndexLocalMutationExecutor(
			changeCollector,
			entityMutation.getEntityPrimaryKey(),
			this.entityIndexCreator,
			this::getSchema,
			entityType -> this.catalogAccessor.get().getCollectionForEntityOrThrowException(entityType).getSchema()
		);

		EntitySerializationContext.executeWithSupplier(
			actualSchema,
			() -> processMutations(entityMutation, changeCollector, entityIndexUpdater)
		);

		changeCollector.getChangedEntityStorageParts()
			.forEach(this.storageContainerBuffer::update);

		return new EntityReference(
			entityMutation.getEntityType(),
			entityMutation.getEntityPrimaryKey()
		);
	}

	@Override
	public boolean deleteEntity(int primaryKey) {
		// fetch entire entity from the data store
		final SealedEntity entityToRemove = getEntity(primaryKey,
			new EvitaRequest(
				Query.query(
					entities(getSchema().getName()),
					require(fullEntity())
				),
				ZonedDateTime.now()
			));
		if (entityToRemove == null) {
			return false;
		}

		internalDeleteEntity(entityToRemove);
		return true;
	}

	@Override
	public int deleteEntityAndItsHierarchy(int primaryKey) {
		final EntityIndex globalIndex = getIndexByKeyIfExists(new EntityIndexKey(EntityIndexType.GLOBAL));
		if (globalIndex != null) {
			final Integer[] nodesToRemove = Arrays.stream(globalIndex.listHierarchyNodesFromParentIncludingItself(primaryKey).getArray())
				.boxed()
				.toArray(Integer[]::new);

			final EvitaResponseBase<SealedEntity> entitiesToRemove = getEntities(
				new EvitaRequest(
					Query.query(
						entities(getSchema().getName()),
						filterBy(primaryKey(nodesToRemove)),
						require(fullEntity())
					),
					ZonedDateTime.now()
				)
			);
			for (SealedEntity entityToRemove : entitiesToRemove.getRecordData()) {
				internalDeleteEntity(entityToRemove);
			}
			return entitiesToRemove.getTotalRecordCount();
		}
		return 0;
	}

	@Override
	public int deleteEntities(@Nonnull EvitaRequest evitaRequest) {
		final QueryTelemetry telemetry = new QueryTelemetry(QueryPhase.OVERALL);
		/* TOBEDONE JNO - session should be passed as argument */
		final QueryPlan queryPlan = createQueryPlan(evitaRequest, telemetry, new EvitaSession(catalogAccessor.get()));
		final DataChunk<Integer> primaryKeys = queryPlan.execute();
		return (int) primaryKeys.stream()
			.map(this::deleteEntity)
			.filter(it -> it)
			.count();
	}

	@Override
	public boolean isEmpty() {
		return this.storageContainerBuffer.count(EntityBodyStoragePart.class) == 0;
	}

	/**
	 * Returns count of all elements in the storage.
	 */
	@Override
	public int size() {
		return this.storageContainerBuffer.count(EntityBodyStoragePart.class);
	}

	@Override
	@Nonnull
	EntitySchema updateSchema(@Nonnull EntitySchema newSchema) throws SchemaAlteringException {
		final EntitySchema updatedSchema = super.updateSchema(newSchema);
		this.storageContainerBuffer.update(new EntitySchemaContainer(updatedSchema));
		return updatedSchema;
	}

	@Nonnull
	public <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull EvitaRequest evitaRequest, @Nonnull EvitaSession session) {
		final QueryTelemetry telemetry = new QueryTelemetry(QueryPhase.OVERALL);
		final QueryPlan queryPlan = createQueryPlan(evitaRequest, telemetry, session);
		return queryPlan.execute();
	}

	/**
	 * Returns catalog entity header version that is incremented with each update. Version is not stored on the disk,
	 * it serves only to distinguish whether there is any change made in the header and whether it needs to be persisted
	 * on disk.
	 */
	public long getVersion() {
		return catalogEntityHeader.getVersion();
	}

	/**
	 * This method writes all changed storage parts into the {@link MemTable} of this {@link EntityCollection} and then
	 * returns updated {@link CatalogEntityHeader}.
	 */
	public CatalogEntityHeader flush(long transactionId, List<EntityCollectionUpdateInstruction> storageParts) {
		for (final EntityCollectionUpdateInstruction instruction : storageParts) {
			if (instruction.isRemoval()) {
				final PersistedStoragePartKey removalKey = instruction.getRemovalKey();
				memTable.remove(removalKey.getPrimaryKey(), removalKey.getContainerClass());
			} else {
				memTable.put(transactionId, instruction.getStoragePart());
			}
		}
		final long previousMemTableVersion = memTable.getVersion();
		final MemTableDescriptor memTableDescriptor = memTable.flush(transactionId);
		// when versions are equal - nothing has changed and we can reuse old header
		if (memTableDescriptor.getVersion() != previousMemTableVersion) {
			this.catalogEntityHeader = new CatalogEntityHeader(
				this.getSchema().getName(),
				catalogEntityHeader.getVersion() + 1,
				size(),
				pkSequence.get(),
				indexPkSequence.get(),
				memTableDescriptor.getFileLocation(),
				memTableDescriptor.getCompressedKeys(),
				memTableDescriptor.getRegisteredClassIds(),
				ofNullable(this.indexes.get(new EntityIndexKey(EntityIndexType.GLOBAL)))
					.map(EntityIndex::getPrimaryKey)
					.orElse(null),
				this.indexes
					.values()
					.stream()
					.filter(it -> it.getEntityIndexKey().getType() != EntityIndexType.GLOBAL)
					.map(EntityIndex::getPrimaryKey)
					.collect(Collectors.toList())
			);
		}
		return catalogEntityHeader;
	}

	/**
	 * Flush operation persists immediately all information kept in non-transactional buffers to the disk. {@link MemTable}
	 * is fully synced with the disk file and will not contain any non-persisted data. Flush operation is ignored when
	 * there are no changes present in {@link MemTable}.
	 */
	@Nonnull
	public CatalogEntityHeader flush() {
		this.ioService.flushTrappedUpdates(
			this.storageContainerBuffer.exchangeBuffer(), this.memTable
		);
		final long previousMemTableVersion = memTable.getVersion();
		final MemTableDescriptor memTableDescriptor = memTable.flush(0L);
		// when versions are equal - nothing has changed, and we can reuse old header
		if (memTableDescriptor.getVersion() != previousMemTableVersion) {
			this.catalogEntityHeader = new CatalogEntityHeader(
				this.getSchema().getName(),
				catalogEntityHeader.getVersion() + 1,
				size(),
				pkSequence.get(),
				indexPkSequence.get(),
				memTableDescriptor.getFileLocation(),
				memTableDescriptor.getCompressedKeys(),
				memTableDescriptor.getRegisteredClassIds(),
				ofNullable(this.indexes.get(new EntityIndexKey(EntityIndexType.GLOBAL)))
					.map(EntityIndex::getPrimaryKey)
					.orElse(null),
				this.indexes
					.values()
					.stream()
					.filter(it -> it.getEntityIndexKey().getType() != EntityIndexType.GLOBAL)
					.map(EntityIndex::getPrimaryKey)
					.collect(Collectors.toList())
			);
		}
		return catalogEntityHeader;
	}

	/**
	 * Method terminates this instance of the {@link EntityCollection} and marks this instance as unusable to
	 * any following invocations. In bulk mode ({@link CatalogState#WARMING_UP}) the {@link #flush()} method should
	 * be called prior calling #terminate().
	 */
	public void terminate() {
		Assert.isTrue(
			this.terminated.compareAndSet(false, true),
			"Collection was already terminated!"
		);
		this.memTable.close();
	}

	/**
	 * Returns entity index by its key. If such index doesn't exist, NULL is returned.
	 */
	@Nullable
	public EntityIndex getIndexByKeyIfExists(EntityIndexKey entityIndexKey) {
		return this.storageContainerBuffer.getIndexIfExists(entityIndexKey, this.indexes::get);
	}

	/**
	 * Returns iterator that allows to iterate through all entities in the store.
	 */
	public Iterator<Entity> entityIterator() {
		return this.storageContainerBuffer.entityIterator();
	}

	@Override
	public EntityCollectionChanges createLayer() {
		return new EntityCollectionChanges();
	}

	@Override
	public EntityCollection createCopyWithMergedTransactionalMemory(@Nullable EntityCollectionChanges layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		final EntityCollectionChanges transactionalChanges = transactionalLayer.getTransactionalMemoryLayer(this);
		if (transactionalChanges != null) {
			final Serializable entityName = getName();

			final Collection<StoragePart> updatedParts = transactionalChanges.getModifiedStoragePartsToPersist();
			transaction.registerForPersistence(entityName, updatedParts);

			final Collection<PersistedStoragePartKey> removedPartKeys = transactionalChanges.getRemovedStoragePartsToPersist();
			transaction.registerForRemoval(entityName, removedPartKeys);

			// when we register all storage parts for persisting we can now release transactional memory
			transactionalLayer.removeTransactionalMemoryLayer(this);
			return new EntityCollection(
				this.getSchema(),
				this.catalogEntityHeader,
				/* TOBEDONE #25 - there needs to be kryo factory exchanged with pointer to new collection to isolate schema changes (Kryo factories contain Schema accessor) */
				this.memTable,
				this.pkSequence,
				this.indexPkSequence,
				this.ioService,
				transactionalLayer.getStateCopyWithCommittedChanges(this.indexes, transaction),
				cacheSupervisor
			);
		} else {
			// no changes present we can return self
			return this;
		}
	}

	/**
	 * Method returns {@link PriceSuperIndex}. This method is used when deserializing {@link PriceRefIndex} which
	 * looks up for prices in super index in order to save memory consumption.
	 */
	@Nonnull
	public PriceSuperIndex getPriceSuperIndex() {
		return getGlobalIndex().getPriceIndex();
	}

	/**
	 * Method returns {@link GlobalEntityIndex} or throws an exception if it hasn't yet exist.
	 */
	@Nonnull
	public GlobalEntityIndex getGlobalIndex() {
		final EntityIndex globalIndex = getIndexByKeyIfExists(new EntityIndexKey(EntityIndexType.GLOBAL));
		Assert.isTrue(globalIndex instanceof GlobalEntityIndex, () -> new IllegalStateException("Global index not found in entity collection of `" + getSchema().getName() + "`."));
		return (GlobalEntityIndex) globalIndex;
	}

	/**
	 * Generates new UNIQUE primary key for the entity. Calling this
	 *
	 * @return new unique primary key
	 */
	protected int getNextPrimaryKey() {
		// atomic integer takes care of concurrent access and producing unique monotonic sequence of numbers
		return pkSequence.incrementAndGet();
	}

	/**
	 * Returns function that ensures that the entity has unique primary key. Depending on {@link EntitySchema#isWithGeneratedPrimaryKey()}
	 * it either generates missing primary key by {@link #getNextPrimaryKey()} method or verifies that the primary key
	 * was provided from the outside.
	 */
	protected Consumer<EntityMutation> getOrCreatePrimaryKeyHandler(boolean pkInFirstEntityPresent) {
		return ofNullable(primaryKeyHandler)
			.orElseGet(() -> {
				final boolean entityCollectionIsEmpty = isEmpty();
				this.primaryKeyHandler = createPrimaryKeyHandler(
					entityCollectionIsEmpty ? pkInFirstEntityPresent : !getSchema().isWithGeneratedPrimaryKey()
				);
				return this.primaryKeyHandler;
			});
	}

	/*
		TransactionalLayerProducer implementation
	 */

	/**
	 * Creates function that ensures that the entity has unique primary key. Depending on {@link EntitySchema#isWithGeneratedPrimaryKey()}
	 * it either generates missing primary key by {@link #getNextPrimaryKey()} method or verifies that the primary key
	 * was provided from the outside.
	 */
	protected Consumer<EntityMutation> createPrimaryKeyHandler(boolean pkInFirstEntityPresent) {
		final EntitySchema currentSchema = this.getSchema();
		if (pkInFirstEntityPresent) {
			return entityMutation ->
				Assert.isTrue(
					!currentSchema.isWithGeneratedPrimaryKey(),
					"Entity of type " + currentSchema.getName() +
						" is expected to have primary key automatically generated by Evita!"

				);
		} else {
			Assert.isTrue(
				currentSchema.isWithGeneratedPrimaryKey() ||
					(currentSchema.allows(EvolutionMode.ADAPT_PRIMARY_KEY_GENERATION) &&
						isEmpty()),
				() -> new InvalidMutationException(
					"Entity of type " + currentSchema.getName() +
						" is expected to have primary key provided by external systems!"
				)
			);
			if (!currentSchema.isWithGeneratedPrimaryKey()) {
				updateSchemaWithRetry(s -> s.open(this::updateSchema).withGeneratedPrimaryKey().applyChanges());
			}
			return entityMutation -> {
				if (entityMutation.getEntityPrimaryKey() == null) {
					entityMutation.setEntityPrimaryKey(getNextPrimaryKey());
				}
			};
		}
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * This method replaces references in current instance that needs to work with information outside this entity
	 * collection. When transaction is committed new catalog instance is created after entity collection instances are
	 * recreated to encapsulate them. That means that all entity collections still point to the old catalog and when
	 * new one encapsulating them is created, all of them needs to update their "pointers".
	 */
	void updateReferenceToCatalog(@Nonnull Catalog catalog) {
		this.catalogAccessor.set(catalog);
	}

	/**
	 * Method processes all local mutations of passed `entityMutation` using passed `changeCollector`
	 * and `entityIndexUpdater`.
	 */
	private void processMutations(
		@Nonnull EntityMutation entityMutation,
		@Nonnull ContainerizedLocalMutationExecutor changeCollector,
		@Nonnull EntityIndexLocalMutationExecutor entityIndexUpdater
	) {
		final Collection<? extends LocalMutation<?, ?>> localMutations = entityMutation.getLocalMutations();
		for (LocalMutation<?, ?> localMutation : localMutations) {
			// first execute changes in search indexes
			entityIndexUpdater.applyMutation(localMutation);
			// second update the entity contents in mem table representation
			changeCollector.applyMutation(localMutation);
		}
	}

	/**
	 * Method loads all indexes mentioned in {@link CatalogEntityHeader#getGlobalEntityIndexId()} and
	 * {@link CatalogEntityHeader#getUsedEntityIndexIds()} into a transactional map indexed by their
	 * {@link EntityIndex#getEntityIndexKey()}.
	 */
	private TransactionalMemoryMap<EntityIndexKey, EntityIndex> loadIndexes(@Nonnull CatalogEntityHeader entityHeader) {
		// we need to load global index first, this is the only one index containing all data
		final GlobalEntityIndex globalIndex = (GlobalEntityIndex) this.ioService.readEntityIndex(
			entityHeader.getGlobalEntityIndexId(), memTable, this::getSchema,
			() -> {
				throw new IllegalStateException("Global index is currently loading!");
			},
			this::getPriceSuperIndex
		);
		return new TransactionalMemoryMap<>(
			// now join global index with all other reduced indexes into single key-value index
			Stream.concat(
					Stream.of(globalIndex),
					entityHeader.getUsedEntityIndexIds()
						.stream()
						.map(eid ->
							this.ioService.readEntityIndex(
								eid, memTable, this::getSchema,
								// this method is used just for `readEntityIndex` method to access global index until
								// it's available by `this::getPriceSuperIndex` (constructor must be finished first)
								globalIndex::getPriceIndex,
								// this method needs to be used from now on to access the super index
								this::getPriceSuperIndex
							)
						)
				)
				.collect(
					Collectors.toMap(
						EntityIndex::getEntityIndexKey,
						Function.identity()
					)
				)
		);
	}

	/**
	 * Method creates {@link QueryPlan} for passed {@link EvitaRequest} and fills data in the `telemetry` object.
	 */
	@Nonnull
	private QueryPlan createQueryPlan(@Nonnull EvitaRequest evitaRequest, @Nonnull QueryTelemetry telemetry, @Nonnull EvitaSession session) {
		final Function<Serializable, EntityCollection> externalEntityAccessor = entityType -> this.catalogAccessor.get().getCollectionForEntityOrThrowException(entityType);
		return QueryExecutor.planQuery(
			createQueryContext(evitaRequest, telemetry, session, externalEntityAccessor)
		);
	}

	/**
	 * Method creates {@link QueryContext} that is used for read operations.
	 */
	@Nonnull
	private QueryContext createQueryContext(@Nonnull EvitaRequest evitaRequest, @Nonnull QueryTelemetry telemetry, @Nonnull EvitaSession session, @Nonnull Function<Serializable, EntityCollection> externalEntityAccessor) {
		return new QueryContext(
			this,
			new ReadOnlyEntityStorageContainerAccessor(storageContainerBuffer, this::getSchema),
			session, evitaRequest, telemetry,
			externalEntityAccessor,
			indexes,
			cacheSupervisor
		);
	}

	/**
	 * This method will try to update existing schema but allows automatic retrying when there is race condition and
	 * some other process updates schema simultaneously but was a little bit faster.
	 */
	private EntitySchema updateSchemaWithRetry(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		int sanityCheck = 0;
		EntitySchema currentSchema = null;

		do {
			try {
				sanityCheck++;
				currentSchema = schemaUpdater.apply(getSchema());
			} catch (ConcurrentSchemaUpdateException ignored) {
				// someone was faster then us - retry with current schema
			}
		} while (currentSchema == null && sanityCheck < 10);

		return currentSchema;
	}

	/**
	 * Deletes passed entity both from indexes and the storage.
	 */
	private void internalDeleteEntity(SealedEntity entityToRemove) {
		// construct set of removal mutations
		final EntityMutation entityMutation = entityToRemove.open().toRemovalMutation();

		// prepare collectors
		final ContainerizedLocalMutationExecutor changeCollector = new ContainerizedLocalMutationExecutor(
			storageContainerBuffer,
			entityMutation.getEntityPrimaryKey(),
			entityMutation.expects(),
			this::getSchema
		);
		final EntityIndexLocalMutationExecutor entityIndexUpdater = new EntityIndexLocalMutationExecutor(
			changeCollector,
			entityMutation.getEntityPrimaryKey(),
			this.entityIndexCreator,
			this::getSchema,
			entityType -> this.catalogAccessor.get().getCollectionForEntityOrThrowException(entityType).getSchema()
		);

		// apply mutations leading to clearing storage containers
		EntitySerializationContext.executeWithSupplier(
			getSchema(),
			() -> processMutations(entityMutation, changeCollector, entityIndexUpdater)
		);

		// remove all parts from the underlying storage - after mutations applications they should be empty whatsoever
		changeCollector.getChangedEntityStorageParts()
			.forEach(part -> {
				if (part.getUniquePartId() == null) {
					Assert.isTrue(
						part instanceof RecordWithCompressedId,
						() -> new IllegalStateException("Removed container must have already its unique part id assigned or must be unsaved RecordWithCompressedId!")
					);
					Assert.isTrue(
						this.storageContainerBuffer.remove(
							((RecordWithCompressedId<?>) part).getStoragePartSourceKey(),
							part.getClass()
						),
						() -> new IllegalStateException("Removed container was expected to be in transactional memory but was not!")
					);
				} else {
					this.storageContainerBuffer.remove(
						(long) part.getUniquePartId(),
						part.getClass()
					);
				}
			});

		// remove the entity itself from the indexes
		entityIndexUpdater.removeEntity(entityToRemove.getPrimaryKey());
	}

	/**
	 * This implementation just manipulates with the set of EntityIndex in entity collection.
	 */
	private class EntityIndexMaintainerImpl implements EntityIndexMaintainer {

		/**
		 * Returns entity index by its key. If such index doesn't exist, it is automatically created.
		 */
		@Nonnull
		@Override
		public EntityIndex getOrCreateIndex(@Nonnull EntityIndexKey entityIndexKey) {
			return EntityCollection.this.storageContainerBuffer.getOrCreateIndexForModification(
				entityIndexKey,
				eik ->
					// if storage container buffer doesn't have index in "dirty" memory - retrieve index from collection
					EntityCollection.this.indexes.computeIfAbsent(
						eik,
						eikAgain -> {
							// if index doesn't exist even there create new one
							if (eikAgain.getType() == EntityIndexType.GLOBAL) {
								return new GlobalEntityIndex(indexPkSequence.incrementAndGet(), eikAgain, EntityCollection.this::getSchema);
							} else {
								final EntityIndex globalIndex = getIndexIfExists(new EntityIndexKey(EntityIndexType.GLOBAL));
								Assert.isTrue(
									globalIndex instanceof GlobalEntityIndex,
									() -> new IllegalStateException("When reduced index is created global one must already exist!")
								);
								return new ReducedEntityIndex(
									indexPkSequence.incrementAndGet(), eikAgain,
									EntityCollection.this::getSchema,
									((GlobalEntityIndex) globalIndex)::getPriceIndex
								);
							}
						}
					)
			);
		}

		/**
		 * Returns existing index for passed `entityIndexKey` or returns null.
		 */
		@Nullable
		@Override
		public EntityIndex getIndexIfExists(@Nonnull EntityIndexKey entityIndexKey) {
			return EntityCollection.this.getIndexByKeyIfExists(entityIndexKey);
		}

		/**
		 * Removes entity index by its key. If such index doesn't exist, exception is thrown.
		 *
		 * @throws IllegalArgumentException when entity index doesn't exist
		 */
		@Override
		public void removeIndex(@Nonnull EntityIndexKey entityIndexKey) {
			final EntityIndex removedIndex = EntityCollection.this.storageContainerBuffer.removeIndex(
				entityIndexKey, EntityCollection.this.indexes::remove
			);
			if (removedIndex == null) {
				throw new IllegalArgumentException("Entity index for key " + entityIndexKey + " doesn't exists!");
			} else {
				removedIndex.clearTransactionalMemory();
			}
		}

	}
}
