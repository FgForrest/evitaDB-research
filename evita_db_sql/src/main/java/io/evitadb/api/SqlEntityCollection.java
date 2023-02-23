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

import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityDecorator;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.exception.ConcurrentSchemaUpdateException;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.exception.SchemaAlteringException;
import io.evitadb.api.io.*;
import io.evitadb.api.io.extraResult.PriceHistogram;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.CatalogContext;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.EntityCollectionReader;
import io.evitadb.storage.EntityCollectionWriter;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.Assert.notNull;
import static java.util.Optional.ofNullable;

/**
 * Implementation of {@link EntityCollectionBase} for PostgreSQL implementation of {@link SqlEvita}.
 *
 * @author Lukáš Hornych 2021
 */
public class SqlEntityCollection extends EntityCollectionBase<SqlEvitaRequest> {

    @Nonnull @Getter private final EntityCollectionContext ctx;
    @Nonnull private final EntityCollectionWriter writer;
    @Nonnull private final EntityCollectionReader reader;

    /**
     * Contains function that takes care of primary key generation if it's required by this {@link #getSchema()}.
     */
    private Consumer<EntityMutation> primaryKeyHandler;
    /**
     * True if collection was already terminated. No other termination will be allowed.
     */
    private final AtomicBoolean terminated = new AtomicBoolean(false);


    /**
     * Creates new instance representing some collection in storage
     *
     * @param catalogContext       connection to storage
     * @param uid                   internal collection id
     * @param entityType               type of collection
     * @param serializationHeader   serialized header of kryo
     * @param initialTransactionalMode if this instance should start in transactional or bulk mode
     */
    public SqlEntityCollection(@Nonnull CatalogContext catalogContext,
                               @Nonnull String uid,
                               @Nonnull Serializable entityType,
                               @Nullable byte[] serializationHeader,
                               boolean initialTransactionalMode) {
        notNull(catalogContext, "In EntityCollection Constructor, evitaContext cannot be Null");
        notNull(entityType, "In EntityCollection Constructor, entityType cannot be Null");

        this.ctx = new EntityCollectionContext(
                catalogContext,
                uid,
                entityType,
                this.schema,
                new AtomicBoolean(initialTransactionalMode),
                serializationHeader
        );
        this.reader = new EntityCollectionReader(ctx);
        this.writer = new EntityCollectionWriter(ctx);
        this.schema.set(getOrCreateEntitySchema(entityType));
    }

    /**
     * Creates new collection
     *
     * @param catalogContext       connection to storage
     * @param uid                   internal collection id
     * @param entitySchema             new schema of new collection
     * @param initialTransactionalMode if this instance should start in transactional or bulk mode
     */
    public SqlEntityCollection(@Nonnull CatalogContext catalogContext,
                               @Nonnull String uid,
                               @Nonnull EntitySchema entitySchema,
                               boolean initialTransactionalMode) {
        notNull(catalogContext, "In EntityCollection Constructor, evitaContext cannot be Null");
        notNull(entitySchema, "In EntityCollection Constructor, entitySchema cannot be Null");

        this.ctx = new EntityCollectionContext(
                catalogContext,
                uid,
                entitySchema.getName(),
                this.schema,
                new AtomicBoolean(initialTransactionalMode),
                null
        );
        this.reader = new EntityCollectionReader(ctx);
        this.writer = new EntityCollectionWriter(ctx);
        this.schema.set(storeNewEntitySchema(entitySchema));
    }

    /**
     * Returns existing stored schema. If it does not exist creates new schema in storage.
     *
     * @param entityType entity type of desired schema
     * @return existing or new (and stored) schema
     */
    @Nonnull
    protected EntitySchema getOrCreateEntitySchema(@Nonnull Serializable entityType) {
        notNull(entityType, "In getOrCreateEntitySchema, entityType cannot be Null");

        final Optional<EntitySchema> schema = reader.findSchema();
        if (schema.isPresent()) {
            return schema.get();
        }

        final EntitySchema newSchema = new EntitySchema(entityType);
        writer.storeSchema(newSchema);

        return newSchema;
    }

    /**
     * Stores specific entity schema that was created outside of this collection.
     *
     * @param entitySchema new schema to store
     * @return stored schema
     */
    @Nonnull
    protected EntitySchema storeNewEntitySchema(@Nonnull EntitySchema entitySchema) {
        notNull(entitySchema, "Entity schema is required to be able to store it.");

        final boolean schemaExists = reader.findSchema().isPresent();
        isTrue(!schemaExists, "Entity schema with name \"" + entitySchema.getName() + "\" already exists.");

        writer.storeSchema(entitySchema);

        return entitySchema;
    }

    @Nonnull
    @Override
    public <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull SqlEvitaRequest evitaRequest) {
        notNull(evitaRequest, "In getEntities, evitaRequest cannot be Null");

        final DataChunk<S> entities;
        final List<EvitaResponseExtraResult> extraResults = new LinkedList<>();
        if (evitaRequest.isRequiresAnyExtraResult()) {
            entities = reader.fetchComplex(evitaRequest, () -> {
                final DataChunk<S> foundEntities = reader.findEntitiesByRequest(evitaRequest);
                if (evitaRequest.isRequiresFacetSummary()) {
                    extraResults.add(reader.computeFacetSummary(evitaRequest));
                }
                if (evitaRequest.isRequiresAttributeHistogram()) {
                    extraResults.add(reader.computeAttributeHistograms(evitaRequest));
                }
                if (evitaRequest.isRequiresPriceHistogram()) {
                    final PriceHistogram priceHistogram = reader.computePriceHistogram(evitaRequest);
                    if (priceHistogram != null) {
                        extraResults.add(priceHistogram);
                    }
                }
                if (evitaRequest.isRequiresParents()) {
                    extraResults.add(reader.findParents(evitaRequest));
                }
                if (evitaRequest.isRequiresHierarchyStatistics()) {
                    extraResults.add(reader.computeHierarchyStatistics(evitaRequest));
                }

                return foundEntities;
            });
        } else {
            entities = reader.findEntitiesByRequest(evitaRequest);
        }

        if (evitaRequest.isRequiresEntityBody()) {
            final DataChunk<EntityContract> decoratedEntities = entities.replaceData(
                    entities.getData().stream()
                            .map(entity -> decorateEntity((Entity) entity, evitaRequest))
                            .collect(Collectors.toList())
            );
            // noinspection unchecked
            return (T) new SqlEvitaEntityResponse(
                    evitaRequest.getQuery(),
                    decoratedEntities,
                    extraResults.toArray(EvitaResponseExtraResult[]::new)
            );
        } else {
            // noinspection unchecked
            return (T) new SqlEvitaEntityReferenceResponse(
                    evitaRequest.getQuery(),
                    (DataChunk<EntityReference>) entities,
                    extraResults.toArray(EvitaResponseExtraResult[]::new)
            );
        }
    }

    @Nullable
    @Override
    public SealedEntity getEntity(int primaryKey, @Nonnull SqlEvitaRequest request) {
        return reader.findEntityByPrimaryKey(primaryKey)
                .filter(Droppable::exists)
                .map(entity -> decorateEntity(entity, request))
                .orElse(null);
    }

    @Override
    public SealedEntity enrichEntity(@Nonnull SealedEntity sealedEntity, @Nonnull SqlEvitaRequest evitaRequest) {
        final EntityDecorator partiallyLoadedEntity = (EntityDecorator) sealedEntity;
        // return decorator that hides information not requested by original query
        final AttributeValueSerializablePredicate newAttributePredicate = partiallyLoadedEntity.getAttributePredicate().createRicherCopyWith(evitaRequest);
        final AssociatedDataValueSerializablePredicate newAssociatedDataPredicate = partiallyLoadedEntity.getAssociatedDataPredicate().createRicherCopyWith(evitaRequest);
        final ReferenceContractSerializablePredicate newReferencePredicate = partiallyLoadedEntity.getReferencePredicate().createRicherCopyWith(evitaRequest);
        final PriceContractSerializablePredicate newPricePredicate = partiallyLoadedEntity.getPricePredicate().createRicherCopyWith(evitaRequest);
        return new EntityDecorator(
            // load all missing data according to current evita request
            reader.findEntityByPrimaryKey(partiallyLoadedEntity.getDelegate().getPrimaryKey()).orElseThrow(),
            // use original schema
            partiallyLoadedEntity.getSchema(),
            // show / hide hierarchical placement information
            partiallyLoadedEntity.getHierarchicalPlacementPredicate(),
            // show / hide attributes information
            newAttributePredicate,
            // show / hide associated data information
            newAssociatedDataPredicate,
            // show / hide references information
            newReferencePredicate,
            // show / hide price information
            newPricePredicate
        );
    }

    @Nonnull
    @Override
    public EntityReference upsertEntity(@Nonnull EntityMutation entityMutation) throws InvalidMutationException {
        notNull(entityMutation, "In upsertEntity, entityMutation cannot be Null");

        // verify mutation against schema
        // it was already executed when mutation was created, but there are two reasons to do it again
        // - we don't trust clients - in future it may be some external JS application
        // - schema may changed between entity was provided to the client and the moment upsert was called
        updateSchemaWithRetry(
                s -> entityMutation.verifyOrEvolveSchema(s, this::updateSchema)
        );

        final Entity currentEntity = ofNullable(entityMutation.getEntityPrimaryKey())
                .flatMap(reader::findEntityByPrimaryKey)
                .orElse(null);

        getOrCreatePrimaryKeyHandler(entityMutation.getEntityPrimaryKey() != null)
                .accept(entityMutation);

        final Entity newVersionOfEntity = entityMutation.mutate(currentEntity);
        writer.storeEntity(newVersionOfEntity);

        //noinspection ConstantConditions
        return new EntityReference(newVersionOfEntity.getType(), newVersionOfEntity.getPrimaryKey());
    }

    @Override
    public boolean deleteEntity(int primaryKey) {
        final Optional<Entity> entityToDelete = reader.findEntityByPrimaryKey(primaryKey);
        if (entityToDelete.isEmpty()) {
            return false;
        }

        final EntityMutation deleteMutation = ((SealedEntity) entityToDelete.get()).open().toRemovalMutation();
        writer.storeEntity(deleteMutation.mutate(entityToDelete.get()));
        return true;
    }

    @Override
    public int deleteEntityAndItsHierarchy(int primaryKey) {
        final List<RequireConstraint> require = Arrays.asList(fullEntity());
        require.add(strip(0, Integer.MAX_VALUE));
        final DataChunk<Entity> entitiesToDelete = reader.findEntitiesByRequest(new SqlEvitaRequest(
                Query.query(
                        entities(getSchema().getName()),
                        filterBy(
                            or(
                                primaryKey(primaryKey),
                                withinHierarchy(primaryKey)
                            )
                        ),
                        require(require.toArray(RequireConstraint[]::new))
                ),
                ZonedDateTime.now()
        ));

        for (Entity entity : entitiesToDelete) {
            final EntityMutation deleteMutation = entity.open().toRemovalMutation();
            writer.storeEntity(deleteMutation.mutate(entity));
        }

        return entitiesToDelete.getTotalRecordCount();
    }

    @Override
    public int deleteEntities(@Nonnull SqlEvitaRequest request) {
        notNull(request, "In deleteEntities, request cannot be Null");

        final DataChunk<Entity> entitiesToDelete = reader.findEntitiesByRequest(new SqlEvitaRequest(
                Query.query(
                        entities(getSchema().getName()),
                        request.getQuery().getFilterBy(),
                        require(strip(0, Integer.MAX_VALUE))
                ),
                ZonedDateTime.now()
        ));

        for (Entity entity : entitiesToDelete) {
            final EntityMutation deleteMutation = entity.open().toRemovalMutation();
            writer.storeEntity(deleteMutation.mutate(entity));
        }

        return entitiesToDelete.getTotalRecordCount();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return reader.countStoredEntities() + writer.countBufferEntities();
    }

    /**
     * Switches collection to alive mode
     *
     * @param restore if restoring previous alive state
     */
    public void goLive(boolean restore) {
        writer.goLive(restore);
    }

    /**
     * Flushes any unsaved changes to storage.
     */
    public void flush() {
        if (ctx.getTransactionMode().compareAndSet(false, true)) {
            writer.flush();
        }
        final byte[] serializationHeader = ctx.getEntityKryoManager().getSerializedSerializationHeader();
        writer.storeSerializationHeader(serializationHeader);
    }

    /**
     * Method terminates this instance of the {@link SqlEntityCollection}. Executes {@link #flush()} operations so that all
     * data gets persisted to the storage and marks this instance as unusable to any following invocations.
     */
    public void terminate() {
        Assert.isTrue(
                this.terminated.compareAndSet(false, true),
                "Collection was already terminated!"
        );
        flush();
    }


    @Override
    @Nonnull
    EntitySchema updateSchema(@Nonnull EntitySchema newSchema) throws SchemaAlteringException {
        final EntitySchema updatedSchema = super.updateSchema(newSchema);
        writer.storeSchema(updatedSchema);

        return updatedSchema;
    }

    /**
     * This method will try to update existing schema but allows automatic retrying when there is race condition and
     * some other process updates schema simultaneously but was a little bit faster.
     *
     * @param schemaUpdater how schema will be updated
     */
    private void updateSchemaWithRetry(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
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
    }

    /**
     * Returns function that ensures that the entity has unique primary key. Depending on {@link EntitySchema#isWithGeneratedPrimaryKey()}
     * it either generates missing primary key by  method or verifies that the primary key
     * was provided from the outside.
     */
    private Consumer<EntityMutation> getOrCreatePrimaryKeyHandler(boolean pkInFirstEntityPresent) {
        return ofNullable(primaryKeyHandler).orElseGet(() -> {
            this.primaryKeyHandler = createPrimaryKeyHandler(pkInFirstEntityPresent);
            return this.primaryKeyHandler;
        });
    }

    /**
     * Creates function that ensures that the entity has unique primary key. Depending on {@link EntitySchema#isWithGeneratedPrimaryKey()}
     * it either generates missing primary key by  method or verifies that the primary key
     * was provided from the outside.
     */
    private Consumer<EntityMutation> createPrimaryKeyHandler(boolean pkInFirstEntityPresent) {
        if (pkInFirstEntityPresent) {
            return entityMutation ->
                    Assert.isTrue(
                            !this.getSchema().isWithGeneratedPrimaryKey(),
                            "Entity of type " + this.getSchema().getName() +
                                    " already has primary key. But schema expects to have primary key automatically generated by Evita!"
                    );
        } else {
            Assert.isTrue(
                    this.getSchema().isWithGeneratedPrimaryKey() ||
                            (this.getSchema().allows(EvolutionMode.ADAPT_PRIMARY_KEY_GENERATION) &&
                                    isEmpty()),
                    () -> new InvalidMutationException(
                            "Entity of type " + this.getSchema().getName() +
                                    " does not have primary key. But schema expects to have primary key provided by external systems!"
                    )
            );
            if (!this.getSchema().isWithGeneratedPrimaryKey()) {
                updateSchemaWithRetry(s -> s.open(this::updateSchema).withGeneratedPrimaryKey().applyChanges());
            }
            return entityMutation -> {
                if (entityMutation.getEntityPrimaryKey() == null) {
                    entityMutation.setEntityPrimaryKey(writer.generateNextPrimaryKey());
                }
            };
        }
    }

    private SealedEntity decorateEntity(Entity entity, SqlEvitaRequest request) {
        return Entity.decorate(
                entity,
                getSchema(),
                new HierarchicalContractSerializablePredicate(),
                new AttributeValueSerializablePredicate(request),
                new AssociatedDataValueSerializablePredicate(request),
                new ReferenceContractSerializablePredicate(request),
                new PriceContractSerializablePredicate(request)
        );
    }
}
