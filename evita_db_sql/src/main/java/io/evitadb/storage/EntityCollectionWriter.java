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

package io.evitadb.storage;

import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.SqlEntityCollection;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.exception.UniqueValueViolationException;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.exception.ConcurrentEntityWriteException;
import io.evitadb.storage.serialization.LocaleParser;
import io.evitadb.storage.serialization.sql.*;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.evitadb.api.utils.Assert.notNull;
import static java.util.Optional.ofNullable;

/**
 * <p>Writer for {@link SqlEntityCollection}'s SQL persistence storage. Contains all collection write operations</p>
 *
 * <h3>Bulk indexing</h3>
 * <p>This implementation also supports a bulk inserting. The actual behaviour is then controlled via flag {@code transactionalMode}.
 * If in bulk insert mode, buffer are automatically flushed when they are full or can be manually flushed at any time
 * using {@link #flushInsertBuffers()}. Note that insert buffer for data other than entities (i.e. attributes, prices...) are
 * used even in transactional mode to speed up insert but are flushed on every entity store call.</p>
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
@Slf4j
public class EntityCollectionWriter {

    /**
     * When entity insert buffer reach this size, all insert buffers should be flushed to storage.
     * Postgres and JDBC are able to handle more but this was tested to be ideal value. Bigger values showed no
     * added performance and lot bigger values Java could not handle with default settings.
     */
    static final int BULK_ENTITY_INSERT_BUFFER_MAX_SIZE = 10000;
    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();
    private static final Pattern SQL_INSERT_DUPLICATE_VALUE_DATA_PATTERN = Pattern.compile("(\\w+)(, )\\{(.+)}");

    // repository state and configuration
    private final EntityCollectionContext ctx;
    private final String serializedEntityType;

    // serialization support
    private final EntitySchemaSerializer entitySchemaSerializer;
    private final EntityBodySerializer entityBodySerializer;
    private final HierarchyPlacementSerializer hierarchyPlacementSerializer;
    private final AttributeSerializer attributeSerializer;
    private final ReferenceSerializer referenceSerializer;
    private final PriceSerializer priceSerializer;

    // insert buffers
    final Queue<Long> freeEntityIds;
    final Queue<Long> freeReferenceIds;
    final SqlParameterSourceInsertBuffer entityBodyInsertBuffer;
    final SqlParameterSourceInsertBuffer hierarchyPlacementInsertBuffer;
    final SqlParameterSourceInsertBuffer referenceInsertBuffer;
    final SqlParameterSourceInsertBuffer attributeInsertBuffer;
    final SqlParameterSourceInsertBuffer priceInsertBuffer;

    private final Lock exclusiveWriteAccess;

    // scripts
    private final String collectionIndexesInitScript;


    /**
     * Creates new storage handler for entity collection
     *
     * @param entityCollectionContext context including all necessary things to operate collection repository, such as db connection, schema, entityType
     */
    public EntityCollectionWriter(@Nonnull EntityCollectionContext entityCollectionContext) {
        notNull(entityCollectionContext, "Database connection is required.");

        this.ctx = entityCollectionContext;

        this.serializedEntityType = ctx.getSerializedEntityType();
        this.entitySchemaSerializer = new EntitySchemaSerializer(entityCollectionContext);
        this.entityBodySerializer = new EntityBodySerializer(entityCollectionContext);
        this.hierarchyPlacementSerializer = new HierarchyPlacementSerializer(entityCollectionContext);
        this.attributeSerializer = new AttributeSerializer();
        this.referenceSerializer = new ReferenceSerializer();
        this.priceSerializer = new PriceSerializer();

        this.freeEntityIds = new ConcurrentLinkedQueue<>();
        this.freeReferenceIds = new ConcurrentLinkedQueue<>();
        this.entityBodyInsertBuffer = new SqlParameterSourceInsertBuffer();
        this.hierarchyPlacementInsertBuffer = new SqlParameterSourceInsertBuffer();
        this.referenceInsertBuffer = new SqlParameterSourceInsertBuffer();
        this.attributeInsertBuffer = new SqlParameterSourceInsertBuffer();
        this.priceInsertBuffer = new SqlParameterSourceInsertBuffer();

        this.exclusiveWriteAccess = new ReentrantLock();

        collectionIndexesInitScript = loadSqlScript(new ClassPathResource("/database/indexes/collection.sql"), Map.of("collectionUid", ctx.getUid()));
    }


    /**
     * <p>Stores new or updated entity.</p>
     *
     * <p>If not in transactional mode, entity is not stored in storage immediately, rather it
     * is added to buffer which is flushed when enough entities are in buffer or can be manually flushed with {@link #flushInsertBuffers()}.</p>
     *
     * @param entity entity to store
     */
    public void storeEntity(@Nonnull Entity entity) {
        notNull(entity, "Cannot store null entity.");
        notNull(entity.getPrimaryKey(), "Entity has to have primary key.");

        try {
            if (exclusiveWriteAccess.tryLock(5, TimeUnit.SECONDS)) {
                try {
                    boolean referencesChanged = false;

                    Long entityId = findEntityId(entity.getPrimaryKey());
                    if (entityId == null) {
                        entityId = insertEntityBody(entityBodyInsertBuffer, entityId, entity);

                        if (!entity.isDropped()) {
                            for (AttributeValue attributeValue : entity.getAttributeValues()) {
                                insertAttribute(attributeValue, entityId, null, null);
                            }

                            for (PriceContract price : entity.getPrices()) {
                                insertPrice(price, entity.getPriceInnerRecordHandling(), entityId);
                            }

                            for (ReferenceContract reference : entity.getReferences()) {
                                final ReferenceSchema referenceSchema = ctx.getEntitySchema().get().getReference(reference.getReferencedEntity().getType());
                                insertReference(reference, referenceSchema, entityId);
                            }
                        }
                    } else {
                        Assert.isTrue(ctx.getTransactionMode().get(), () -> new IllegalStateException("Cannot update entity in warming up state."));
                        updateEntityBody(entity, entityId);
                        upsertAttributes(entity.getAttributeValues(), entityId, entity, null, null);
                        upsertPrices(entity.getPrices(), entity.getPriceInnerRecordHandling(), entityId);
                        referencesChanged = upsertReferences(entity.getReferences(), entityId, entity);
                    }

                    if (shouldFlushInsertBuffers()) {
                        flushInsertBuffers();

                        if (referencesChanged) {
                            refreshFacetIndex();
                        }
                    }
                } finally {
                    exclusiveWriteAccess.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentEntityWriteException("New exclusive write access couldn't have been obtained due to interrupt!");
        }
    }

    /**
     * Flushes all not-stored data from insert buffers to persistence storage.
     */
    public void flush() {
        flushInsertBuffers();
        refreshFacetIndex();
    }

    /**
     * @return count of all entities in insert buffers ready to be inserted to db
     */
    @SuppressWarnings("ConstantConditions")
    public int countBufferEntities() {
        if (ctx.getTransactionMode().get()) {
            return 0;
        }

        return entityBodyInsertBuffer.size();
    }

    /**
     * Stores new or updated entity schema for entity collection defined when repository was created.
     *
     * @param schema schema to store
     */
    public void storeSchema(@Nonnull EntitySchema schema) {
        notNull(schema, "Entity schema cannot be null when storing.");

        Optional<Map<String, Object>> existingSchema;
        try {
            existingSchema = Optional.of(ctx.getCatalogCtx().getNpJdbcTemplate().queryForMap(
                    "select withHierarchy from t_schema where entityType = :entityType",
                    Collections.singletonMap("entityType", schema.getName().toString())
            ));
        } catch (EmptyResultDataAccessException ex) {
            existingSchema = Optional.empty();
        }

        if (existingSchema.isEmpty()) {
            Hierarchy hierarchyDescriptor = null;
            if (schema.isWithHierarchy()) {
                hierarchyDescriptor = ctx.getCatalogCtx().getHierarchyManager().createHierarchyDescriptor(schema.getName());
            }

            new SimpleJdbcInsert(ctx.getCatalogCtx().getJdbcTemplate())
                    .withTableName("t_schema")
                    .execute(entitySchemaSerializer.serializeToInsert(schema, hierarchyDescriptor));

            if (schema.isWithGeneratedPrimaryKey() || schema.allows(EvolutionMode.ADAPT_PRIMARY_KEY_GENERATION)) {
                createSequence();
            }

            if (ctx.getTransactionMode().get()) {
                // we want to create indexes only in transactional mode, otherwise it is to expensive
                createIndexes();
            }
        } else {
            ctx.getCatalogCtx().getNpJdbcTemplate().update(
                    "update t_schema " +
                            "set detail = :detail, " +
                            "   serializationHeader = :serializationHeader " +
                            "where entityType = :entityType",
                    entitySchemaSerializer.serializeToUpdate(schema)
            );

            // insert hierarchy metadata if hierarchy has been created after entity schema (evolution of schema)
            final boolean alreadyHasHierarchy = (boolean) existingSchema.get().get("withHierarchy");
            if (schema.isWithHierarchy() && !alreadyHasHierarchy) {
                final Hierarchy hierarchyDescriptor = ctx.getCatalogCtx().getHierarchyManager().createHierarchyDescriptor(schema.getName());
                ctx.getCatalogCtx().getNpJdbcTemplate().update(
                        "update t_schema " +
                                "set withHierarchy = true, " +
                                "   hierarchyLevels = :hierarchyLevels, " +
                                "   hierarchySectionSize = :hierarchySectionSize " +
                                "where entityType = :entityType",
                        entitySchemaSerializer.serializeHierarchyDescriptorToInsert(hierarchyDescriptor)
                );
            }
        }
    }

    /**
     * Stores updated Kryo's serialization header for this collection
     */
    public void storeSerializationHeader(@Nonnull byte[] serializedSerializationHeader) {
        ctx.getCatalogCtx().getJdbcTemplate().update(
                "update t_collection set serializationHeader = ? where name = ?",
                serializedSerializationHeader,
                serializedEntityType
        );
    }

    /**
     * Generates next primary key from sequence bound to this entity collection. Used for entities without primary keys set
     * by client.
     *
     * @return generated next sequence value
     */
    @SuppressWarnings("ConstantConditions")
    public int generateNextPrimaryKey() {
        return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select nextVal(?)",
                Integer.class,
                ctx.getUid() + ".schemaSequence_" + serializedEntityType
        );
    }

    /**
     * Switches collection to alive state
     *
     * @param restore if restoring previous alive state
     */
    public void goLive(boolean restore) {
        if (!restore) {
            createIndexes();
        }
    }


    /**
     * Finds internal entity id by its type and primary key
     *
     * @param primaryKey primary key of finding entity
     * @return id or null if entity could not be found
     */
    private Long findEntityId(int primaryKey) {
        try {
            return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                    "select entity_id from t_entity where type = ? and primaryKey = ?",
                    Long.class,
                    serializedEntityType,
                    primaryKey
            );
        } catch (IncorrectResultSizeDataAccessException ex) {
            return null;
        }
    }

    /*
     * --- Entity storing ---
     */

    /**
     * <h3>In transactional mode</h3>
     * <p>Inserts body of new entity (base metadata and cached entity data) directly to database and returns generated internal id.</p>
     *
     * <h3>In bulk insert mode</h3>
     * <p>Inserts body of new entity (base metadata and cached entity data) to insert buffer to be
     * later flushed to database in bulk with other entities. Returns generated internal id.</p>
     *
     * @param entity new entity to insert
     * @return newly generated internal id of entity
     */
    private long insertEntityBody(@Nonnull SqlParameterSourceInsertBuffer entityBodyInsertBuffer,
                                  @Nullable Long entityId,
                                  @Nonnull Entity entity) {
        if (entityId == null) {
            entityId = generateEntityId();
        }

        final List<HierarchyItem> associatedHierarchyPlacements = getAssociatedHierarchyPlacements(entity);

        HierarchyItem selfHierarchyPlacement = null;
        if (ctx.getEntitySchema().get().isWithHierarchy()) {
            selfHierarchyPlacement = associatedHierarchyPlacements.get(0);
        }
        entityBodySerializer.serializeToInsert(entityBodyInsertBuffer, entity, selfHierarchyPlacement, entityId);

        insertAssociatedHierarchyPlacements(entityId, entity, associatedHierarchyPlacements);

        return entityId;
    }

    /**
     * Updates single previously stored body of entity (base metadata and cached entity). It does not check version.
     *
     * @param entity   entity to update in database
     * @param entityId internal id of the entity
     */
    private void updateEntityBody(@Nonnull Entity entity, long entityId) {
        final List<HierarchyItem> associatedHierarchyPlacements = getAssociatedHierarchyPlacements(entity);

        HierarchyItem selfHierarchyPlacement = null;
        if (ctx.getEntitySchema().get().isWithHierarchy()) {
            selfHierarchyPlacement = associatedHierarchyPlacements.get(0);
        }
        final SqlParameterSource updatedEntityBodyArgs = entityBodySerializer.serializeToUpdate(entity, selfHierarchyPlacement, entityId);

        ctx.getCatalogCtx().getNpJdbcTemplate().update(
                "update t_entity set " +
                        "   version = :version," +
                        "   dropped = :dropped," +
                        "   parentPrimaryKey = :parentPrimaryKey," +
                        "   leftBound = :leftBound, " +
                        "   rightBound = :rightBound, " +
                        "   level = :level, " +
                        "   numberOfChildren = :numberOfChildren, " +
                        "   orderAmongSiblings = :orderAmongSiblings, " +
                        "   hierarchyBucket = :hierarchyBucket, " +
                        "   locales = :locales," +
                        "   serializedEntity = :serializedEntity " +
                        " where entity_id = :entityId",
                updatedEntityBodyArgs
        );

        deleteAssociatedHierarchyPlacements(entityId);
        insertAssociatedHierarchyPlacements(entityId, entity, associatedHierarchyPlacements);
    }

    /**
     * Inserts new associated hierarchy placement found for specified entity
     */
    private void insertAssociatedHierarchyPlacements(@Nonnull Long entityId, @Nonnull Entity entity, List<HierarchyItem> associatedHierarchyPlacements) {
        for (HierarchyItem item : associatedHierarchyPlacements) {
            hierarchyPlacementSerializer.serializeToInsert(hierarchyPlacementInsertBuffer, item, entity, entityId);
        }
    }

    /**
     * Deletes all associated hierarchy placements for specified entity
     */
    private void deleteAssociatedHierarchyPlacements(long entityId) {
        ctx.getCatalogCtx().getJdbcTemplate().update(
                "delete from t_entityHierarchyPlacement where entity_id = ?",
                entityId
        );
    }

    /*
     * --- Attribute storing ---
     */

    @Nonnull
    private Map<AttributeKey, ExistingRecord> findExistingAttributes(long entityId) {
        //noinspection ConstantConditions
        return ctx.getCatalogCtx().getJdbcTemplate().query(
                "select name, locale, attribute_id, version " +
                    "from " + ctx.getUid() + ".t_attributeIndex " +
                    "where entity_id = ?",
                new MapResultSetExtractor<>(
                        (rs, n) -> new AttributeKey(
                                rs.getString("name"),
                                LocaleParser.parse(rs.getString("locale"))
                        ),
                        (rs, n) -> new ExistingRecord(rs.getLong("attribute_id"), rs.getInt("version"))
                ),
                entityId
        );
    }

    /**
     * Checks each attribute and decides whether it should be inserted or updated and delegates the work accordingly.
     * It decides what to do depending on stored version of each attribute.
     *
     * @param attributeValues       attribute values to process
     * @param entityId              internal id of owning entity (in case of entity attribute, otherwise {@code null}) or id of entity owning reference (in case of reference attribute)
     * @param referenceId           internal id of owning reference (in case of reference attribute, otherwise {@code null})
     * @param reference             owning reference (in case of reference attribute)
     */
    private void upsertAttributes(@Nonnull Collection<AttributeValue> attributeValues,
                                  long entityId,
                                  @Nonnull Entity entity,
                                  @Nullable Long referenceId,
                                  @Nullable ReferenceContract reference) {
        Assert.isTrue(
                (referenceId != null) == (reference != null),
                "Both reference id and reference entity type have to be present or none."
        );

        final Map<AttributeKey, ExistingRecord> existingAttributes = findExistingAttributes(entityId);

        for (AttributeValue attributeValue : attributeValues) {
            final ExistingRecord current = existingAttributes.get(attributeValue.getKey());

            if (current == null) {
                insertAttribute(attributeValue, entityId, referenceId, reference);
            } else if (attributeValue.getVersion() > current.getVersion()) {
                if (attributeValue.isDropped()) {
                    // new version is dropped, so we do not want it in index anymore
                    deleteAttribute(current.getInternalId());
                } else {
                    updateAttribute(attributeValue, entity, reference, current.getInternalId());
                }
            }
        }
    }

    /**
     * Adds new attribute to insert buffer to be later inserted to database if the attribute is either filterable or sortable.
     * Also, if attribute is sortable it's value cannot be array.
     *
     * @param attributeValue        new attribute to insert
     * @param entityId              internal id of owning entity (in case of entity attribute) or id of entity owning reference (in case of reference attribute)
     * @param referenceId           internal id of owning reference (in case of reference attribute)
     * @param reference             owning reference (in case of reference attribute)
     */
    private void insertAttribute(@Nonnull AttributeValue attributeValue,
                                 long entityId,
                                 @Nullable Long referenceId,
                                 @Nullable ReferenceContract reference) {
        if (attributeValue.isDropped()) {
            return;
        }

        Assert.isTrue(
                (referenceId != null) == (reference != null),
                "Both reference id and reference entity type have to be present or none."
        );

        final AttributeSchema attributeSchema = getAttributeSchema(attributeValue, reference);
        if (!attributeSchema.isFilterable() && !attributeSchema.isSortable() && !attributeSchema.isUnique()) {
            return;
        }
        if (attributeSchema.isSortable() && attributeSchema.getType().isArray()) {
            throw new IllegalArgumentException("Attribute " + attributeSchema.getName() + " is sortable and can't hold arrays of " + attributeValue.getValue().getClass().getSimpleName() + "!");
        }

        attributeSerializer.serializeToInsert(
                attributeInsertBuffer,
                attributeValue,
                attributeSchema,
                entityId,
                referenceId,
                reference
        );
    }

    /**
     * Updates single previously stored attribute. It does not check version.
     *
     * @param attributeValue attribute to update in database
     * @param entity         owning or parent entity
     * @param reference      owning reference
     * @param attributeId    internal id of attribute
     */
    private void updateAttribute(@Nonnull AttributeValue attributeValue,
                                 @Nonnull Entity entity,
                                 @Nullable ReferenceContract reference,
                                 long attributeId) {

        final AttributeSchema attributeSchema = getAttributeSchema(attributeValue, reference);
        final SqlParameterSource updatedAttributeValueArgs = attributeSerializer.serializeToUpdate(attributeValue, attributeSchema, attributeId);

        final StringBuilder sqlBuilder = new StringBuilder()
                .append("update " + ctx.getUid() + ".t_attributeIndex set " +
                        "version = :version,")
                .append(AttributeTypedValue.STRINGS_COLUMN).append(" = :").append(AttributeTypedValue.STRINGS_COLUMN).append(",")
                .append(AttributeTypedValue.INTS_COLUMN).append(" = :").append(AttributeTypedValue.INTS_COLUMN).append(",")
                .append(AttributeTypedValue.INT_RANGES_COLUMN).append(" = :").append(AttributeTypedValue.INT_RANGES_COLUMN)
                .append(" where attribute_id = :attributeId");

        try {
            ctx.getCatalogCtx().getNpJdbcTemplate().update(
                    sqlBuilder.toString(),
                    updatedAttributeValueArgs
            );
        } catch (DuplicateKeyException duplicateKeyException) {
            throwUniqueValueViolationException(entity, attributeValue);
        }
    }

    /**
     * Deletes attribute from index by internal id
     */
    private void deleteAttribute(long attributeId) {
        ctx.getCatalogCtx().getJdbcTemplate().update(
                "delete from " + ctx.getUid() + ".t_attributeIndex where attribute_id = ?",
                attributeId
        );
    }

    /*
     * --- Price storing ---
     */

    @Nonnull
    private Map<PriceKey, ExistingRecord> findExistingPrices(long entityId) {
        //noinspection ConstantConditions
        return ctx.getCatalogCtx().getJdbcTemplate().query(
                "select primaryKey, priceList, priceListDataType, currency, price_id, version " +
                    "from " + ctx.getUid() + ".t_priceIndex " +
                    "where entity_id = ?",
                new MapResultSetExtractor<>(
                        (rs, n) -> new PriceKey(
                                rs.getInt("primaryKey"),
                                STRING_TYPED_VALUE_SERIALIZER.deserialize(TypedValue.of(rs.getString("priceList"), rs.getString("priceListDataType"))),
                                STRING_TYPED_VALUE_SERIALIZER.deserialize(TypedValue.of(rs.getString("currency"), Currency.class))
                        ),
                        (rs, n) -> new ExistingRecord(rs.getLong("price_id"), rs.getInt("version"))
                ),
                entityId
        );
    }

    /**
     * Checks each price and decides whether it should be inserted or updated and delegates the work accordingly.
     * It decides what to do depending on already stored version of each price.
     *
     * @param prices                   prices to process
     * @param priceInnerRecordHandling price inner record handling of passed collection of prices
     * @param entityId                 internal id of owning entity
     */
    private void upsertPrices(@Nonnull Collection<PriceContract> prices,
                              @Nonnull PriceInnerRecordHandling priceInnerRecordHandling,
                              long entityId) {
        final Map<PriceKey, ExistingRecord> existingPrices = findExistingPrices(entityId);

        for (PriceContract price : prices) {
            final ExistingRecord current = existingPrices.get(price.getPriceKey());

            if (current == null) {
                insertPrice(price, priceInnerRecordHandling, entityId);
            } else if (price.getVersion() > current.getVersion()) {
                if (price.isDropped()) {
                    // new version is dropped, so we do not want it in index anymore
                    deletePrice(current.getInternalId());
                } else {
                    updatePrice(price, priceInnerRecordHandling, current.getInternalId());
                }
            }
        }
    }

    /**
     * Adds new price to insert buffer to be later inserted to database.
     *
     * @param price                    new price to insert
     * @param priceInnerRecordHandling price inner record handling of prices collection of the price
     * @param entityId                 internal id of owning entity
     */
    private void insertPrice(@Nonnull PriceContract price,
                             @Nonnull PriceInnerRecordHandling priceInnerRecordHandling,
                             long entityId) {
        if (price.isDropped() || !price.isSellable()) {
            return;
        }

        priceSerializer.serializeToInsert(priceInsertBuffer, price, priceInnerRecordHandling, entityId);
    }

    /**
     * Updates single previously stored price. It does not check version.
     *
     * @param price                    price to update in database
     * @param priceInnerRecordHandling price inner record handling of prices collection of the price
     * @param priceId                 internal id of price
     */
    private void updatePrice(@Nonnull PriceContract price,
                             @Nonnull PriceInnerRecordHandling priceInnerRecordHandling,
                             long priceId) {
        if (price.isDropped() || !price.isSellable()) {
            return;
        }

        final SqlParameterSource updatedPriceArgs = priceSerializer.serializeToUpdate(price, priceInnerRecordHandling, priceId);

        ctx.getCatalogCtx().getNpJdbcTemplate().update(
                "update " + ctx.getUid() + ".t_priceIndex set " +
                    "   version = :version, " +
                    "   validity = :validity, " +
                    "   innerRecordHandling = :innerRecordHandling, " +
                    "   innerRecordId = :innerRecordId, " +
                    "   priceWithoutVat = :priceWithoutVat, " +
                    "   priceWithVat = :priceWithVat " +
                    "where price_id = :priceId",
                updatedPriceArgs
        );
    }

    /**
     * Deletes price from index by internal id
     */
    private void deletePrice(long priceId) {
        ctx.getCatalogCtx().getJdbcTemplate().update(
                "delete from " + ctx.getUid() + ".t_priceIndex where price_id = ?",
                priceId
        );
    }

    /*
     * --- Reference storing ---
     */

    @Nonnull
    private Map<ReferenceKey, ExistingRecord> findExistingReferences(long entityId) {
        //noinspection ConstantConditions
        return ctx.getCatalogCtx().getJdbcTemplate().query(
                "select entityPrimaryKey, entityType, entityTypeDataType, reference_id, version " +
                    "from " + ctx.getUid() + ".t_referenceIndex " +
                    "where entity_id = ?",
                new MapResultSetExtractor<>(
                        (rs, n) -> new ReferenceKey(
                                rs.getInt("entityPrimaryKey"),
                                STRING_TYPED_VALUE_SERIALIZER.deserialize(TypedValue.of(
                                        rs.getString("entityType"),
                                        rs.getString("entityTypeDataType")
                                ))
                        ),
                        (rs, n) -> new ExistingRecord(rs.getLong("reference_id"), rs.getInt("version"))
                ),
                entityId
        );
    }

    /**
     * Checks each reference and decides where it should be inserted or updated and delegates the work accordingly.
     * It decides what to do depending on already stored version of each reference.
     *
     * @param references            references to process
     * @param entityId              owning entity internal id
     * @return if any changes
     */
    private boolean upsertReferences(@Nonnull Collection<ReferenceContract> references,
                                  long entityId,
                                  @Nonnull Entity entity) {
        boolean referencesChanged = false;

        final Map<ReferenceKey, ExistingRecord> existingReferences = findExistingReferences(entityId);

        for (ReferenceContract reference : references) {
            final ExistingRecord current = existingReferences.get(new ReferenceKey(reference));
            final ReferenceSchema referenceSchema = ctx.getEntitySchema().get().getReference(reference.getReferencedEntity().getType());

            if (current == null) {
                insertReference(reference, referenceSchema, entityId);
                referencesChanged = true;
            } else if (reference.getVersion() > current.getVersion()) {
                if (reference.isDropped()) {
                    // new version is dropped, so we do not want it in index anymore
                    deleteReference(current.getInternalId());
                } else {
                    updateReference(reference, referenceSchema, entityId, entity, current.getInternalId());
                }
                referencesChanged = true;
            }
        }

        return referencesChanged;
    }

    /**
     * Adds new reference to insert buffer to be later inserted to database if the reference is faceted.
     *
     * @param reference             new reference to be inserted
     * @param referenceSchema       schema of inserted reference
     * @param entityId              internal id of owning entity
     */
    private void insertReference(@Nonnull ReferenceContract reference,
                                 @Nonnull ReferenceSchema referenceSchema,
                                 long entityId) {
        if (reference.isDropped() || !referenceSchema.isIndexed()) {
            return;
        }

        final long generatedReferenceId = generateReferenceId();
        referenceSerializer.serializeToInsert(referenceInsertBuffer, reference, referenceSchema, generatedReferenceId, entityId);

        for (AttributeValue attributeValue : reference.getAttributeValues()) {
            insertAttribute(attributeValue, entityId, generatedReferenceId, reference);
        }
    }

    /**
     * Updates single previously stored reference. It does not check version.
     *
     * @param reference             reference to update in database
     * @param referenceSchema       schema of updated reference
     * @param referenceId           internal id of updating reference for correct linking of reference attributes
     * @param entityId              internal id of owning entity
     */
    private void updateReference(@Nonnull ReferenceContract reference,
                                 @Nonnull ReferenceSchema referenceSchema,
                                 long entityId,
                                 @Nonnull Entity entity,
                                 long referenceId) {
        if (reference.isDropped() || !referenceSchema.isIndexed()) {
            return;
        }

        final SqlParameterSource updatedReferenceArgs = referenceSerializer.serializeToUpdate(reference, referenceSchema, referenceId);

        ctx.getCatalogCtx().getNpJdbcTemplate().update(
                "update " + ctx.getUid() + ".t_referenceIndex set " +
                    "   version = :version, " +
                    "   groupPrimaryKey = :groupPrimaryKey, " +
                    "   groupType = :groupType " +
                    "where reference_id = :referenceId",
                updatedReferenceArgs
        );

        upsertAttributes(reference.getAttributeValues(), entityId, entity, referenceId, reference);
    }

    /**
     * Deletes reference from index by internal id
     */
    private void deleteReference(long referenceId) {
        ctx.getCatalogCtx().getJdbcTemplate().update(
                "delete from " + ctx.getUid() + ".t_referenceIndex where reference_id = ?",
                referenceId
        );
    }

    /*
     * --- Misc helpers ---
     */

    /**
     * Loads SQL script from resources directory to string
     *
     * @param scriptResource script descriptor
     * @param params insertable params
     * @return loaded script
     */
    private String loadSqlScript(@Nonnull Resource scriptResource, @Nonnull Map<String, String> params) {
        final StringBuilder scriptBuilder = new StringBuilder();

        try (final BufferedReader scriptReader = new BufferedReader(new InputStreamReader(scriptResource.getInputStream()))) {
            String scriptLine;
            while ((scriptLine = scriptReader.readLine()) != null) {
                // replace params
                for (Map.Entry<String, String> param : params.entrySet()) {
                    scriptLine = scriptLine.replaceAll("\\$\\{" + param.getKey() + "}", param.getValue());
                }

                scriptBuilder.append(scriptLine).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return scriptBuilder.toString();
    }

    /**
     * Executes loaded SQL script with passed arguments
     */
    private void executeSqlScript(@Nonnull String script, @Nonnull Map<String, Object> args) {
        // i know there is sql injection but JDBC could not find any passed parameters to "create index" clause so...
        for (Map.Entry<String, Object> arg : args.entrySet()) {
            final String serializedArg = arg.getValue() instanceof String ? "'" + arg.getValue() + "'" : arg.getValue().toString();
            script = script.replaceAll(":" + arg.getKey(), serializedArg);
        }
        ctx.getCatalogCtx().getJdbcTemplate().execute(script);
    }

    /**
     * Creates all indexes after all bulk data has been inserted
     */
    private void createIndexes() {
        log.info("Creating indexes for collection " + ctx.getUid() + "...");
        executeSqlScript(collectionIndexesInitScript, Map.of());
        log.info("Indexes for collection " + ctx.getUid() + " created.");
    }

    private boolean shouldFlushInsertBuffers() {
        return ctx.getTransactionMode().get() || (entityBodyInsertBuffer.size() >= BULK_ENTITY_INSERT_BUFFER_MAX_SIZE);
    }

    /**
     * Flushes all data from insert buffers to persistence storage.
     */
    private void flushInsertBuffers() {
        try {
            if (exclusiveWriteAccess.tryLock(5, TimeUnit.SECONDS)) {
                try {
                    // flush new entities buffer
                    if (!entityBodyInsertBuffer.isEmpty()) {
                        new SimpleJdbcInsert(ctx.getCatalogCtx().getJdbcTemplate())
                                .withTableName("t_entity")
                                .executeBatch(entityBodyInsertBuffer.toArray());
                    }

                    // flush entity hierarchy placements buffer
                    if (!hierarchyPlacementInsertBuffer.isEmpty()) {
                        new SimpleJdbcInsert(ctx.getCatalogCtx().getJdbcTemplate())
                                .withTableName("t_entityHierarchyPlacement")
                                .executeBatch(hierarchyPlacementInsertBuffer.toArray());
                    }

                    // flush references buffer
                    if (!referenceInsertBuffer.isEmpty()) {
                        new SimpleJdbcInsert(ctx.getCatalogCtx().getJdbcTemplate())
                                .withSchemaName(ctx.getUid())
                                .withTableName("t_referenceIndex")
                                .executeBatch(referenceInsertBuffer.toArray());
                    }

                    // flush attributes buffer
                    if (!attributeInsertBuffer.isEmpty()) {
                        try {
                            new SimpleJdbcInsert(ctx.getCatalogCtx().getJdbcTemplate())
                                    .withSchemaName(ctx.getUid())
                                    .withTableName("t_attributeIndex")
                                    .usingGeneratedKeyColumns("attribute_id")
                                    .executeBatch(attributeInsertBuffer.toArray());
                        } catch (DuplicateKeyException duplicateKeyException) {
                            throwUniqueValueViolationException(duplicateKeyException);
                        }
                    }

                    // flush prices buffer
                    if (!priceInsertBuffer.isEmpty()) {
                        new SimpleJdbcInsert(ctx.getCatalogCtx().getJdbcTemplate())
                                .withSchemaName(ctx.getUid())
                                .withTableName("t_priceIndex")
                                .usingGeneratedKeyColumns("price_id")
                                .executeBatch(priceInsertBuffer.toArray());
                    }

                    // reset buffers for reuse
                    entityBodyInsertBuffer.reset();
                    hierarchyPlacementInsertBuffer.reset();
                    referenceInsertBuffer.reset();
                    attributeInsertBuffer.reset();
                    priceInsertBuffer.reset();
                } finally {
                    exclusiveWriteAccess.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentEntityWriteException("New exclusive write access couldn't have been obtained due to interrupt!");
        }
    }

    /**
     * Refreshes facet index for this collection as it is materialized view
     */
    private void refreshFacetIndex() {
        ctx.getCatalogCtx().getJdbcTemplate().execute("refresh materialized view " + ctx.getUid() + ".t_facetIndex");
    }

    /**
     * Find schema for entity attribute or reference attribute
     *
     * @param attributeValue attribute
     * @param reference      reference in case of reference attribute
     * @return attribute schema
     */
    private AttributeSchema getAttributeSchema(@Nonnull AttributeValue attributeValue, ReferenceContract reference) {
        if (reference == null) {
            return ctx.getEntitySchema().get().getAttribute(attributeValue.getKey().getAttributeName());
        } else {
            return ctx.getEntitySchema().get()
                    .getReference(reference.getReferencedEntity().getType())
                    .getAttribute(attributeValue.getKey().getAttributeName());
        }
    }

    /**
     * Returns true if referenced entity is hierarchical entity.
     */
    private boolean isReferencingHierarchicalEntity(@Nonnull Serializable referencedType,
                                                    @Nonnull ReferenceSchema referenceSchema) {
        return referenceSchema.isEntityTypeRelatesToEntity() &&
                ofNullable(ctx.getCatalogCtx().getCatalog().getCollectionForEntity(referencedType))
                        .map(EntityCollectionBase::getSchema)
                        .map(EntitySchema::isWithHierarchy)
                        .orElseThrow(() -> new IllegalArgumentException("Referenced schema for entity " + referencedType + " was not found!"));
    }

    /**
     * Returns list of associated hierarchy placements (items) with passed entity. Associated placement can be either self-placement
     * (in case entity is hierarchical and has its own placement) or placement of referenced hierarchy entity to speed up search.
     *
     * @param entity entity for which to find items
     * @return list of found items
     */
    private List<HierarchyItem> getAssociatedHierarchyPlacements(Entity entity) {
        final List<HierarchyItem> associatedHierarchyPlacements = new LinkedList<>();

        // create hierarchy placement if entity is hierarchical
        if (ctx.getEntitySchema().get().isWithHierarchy()) {
            final Hierarchy hierarchy = ctx.getCatalogCtx().getHierarchyManager().getHierarchy(entity.getType());

            final Integer parentPrimaryKey = entity.getHierarchicalPlacement().getParentPrimaryKey();
            final HierarchyItem selfPlacement;
            if (parentPrimaryKey == null) {
                selfPlacement = hierarchy.createRootItem(String.valueOf(entity.getPrimaryKey()));
            } else {
                selfPlacement = hierarchy.createItem(
                        String.valueOf(entity.getPrimaryKey()),
                        String.valueOf(parentPrimaryKey)
                );
            }
            associatedHierarchyPlacements.add(selfPlacement);
        }

        // find referenced hierarchy items to speed up search
        entity.getReferences().forEach(r -> {
            final EntityReference referencedEntity = r.getReferencedEntity();
            if (referencedEntity.getType().equals(entity.getType()) && referencedEntity.getPrimaryKey() == entity.getPrimaryKey()) {
                // ignore reference to itself
                return;
            }

            final ReferenceSchema referenceSchema = ctx.getEntitySchema().get().getReference(referencedEntity.getType());
            final boolean isHierarchyIndex = isReferencingHierarchicalEntity(referencedEntity.getType(), referenceSchema);
            if (!isHierarchyIndex) {
                return;
            }

            final Hierarchy hierarchy = ctx.getCatalogCtx().getHierarchyManager().getHierarchy(referenceSchema.getEntityType());
            final HierarchyItem referencedPlacement = hierarchy.getItem(String.valueOf(referencedEntity.getPrimaryKey()));
            associatedHierarchyPlacements.add(referencedPlacement);
        });

        return associatedHierarchyPlacements;
    }

    /**
     * Generates new internal entity id upfront for newly inserted entity. If not in transactional mode, multiple ids are
     * generated at once (in size of max insert buffer size) and cached.
     *
     * @return new unique internal reference id
     */
    private long generateEntityId() {
        if (!ctx.getTransactionMode().get()) {
            if (freeEntityIds.isEmpty()) {
                final List<Long> generatedIds = ctx.getCatalogCtx().getJdbcTemplate().queryForList(
                        "select nextVal('t_entity_id_seq') from generate_series(1, ?)",
                        long.class,
                        BULK_ENTITY_INSERT_BUFFER_MAX_SIZE
                );
                freeEntityIds.addAll(generatedIds);
            }

            return freeEntityIds.remove();
        }

        //noinspection ConstantConditions
        return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select nextVal('t_entity_id_seq')",
                long.class
        );
    }

    /**
     * Generates new internal reference id upfront for newly inserted reference. If not in transactional mode, multiple ids are
     * generated at once (in size of max insert buffer size) and cached.
     *
     * @return new unique internal entity id
     */
    private long generateReferenceId() {
        if (!ctx.getTransactionMode().get()) {
            if (freeReferenceIds.isEmpty()) {
                final List<Long> generatedIds = ctx.getCatalogCtx().getJdbcTemplate().queryForList(
                        "select nextVal('" + ctx.getUid() + ".t_reference_id_seq') from generate_series(1, ?)",
                        long.class,
                        BULK_ENTITY_INSERT_BUFFER_MAX_SIZE
                );
                freeReferenceIds.addAll(generatedIds);
            }

            return freeReferenceIds.remove();
        }

        //noinspection ConstantConditions
        return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select nextVal('" + ctx.getUid() + ".t_reference_id_seq')",
                long.class
        );
    }

    /**
     * Creates new sequence for specific schema. If sequence already exists by passed entity type the cursor will only be resetted to 0.
     */
    @SuppressWarnings("ConstantConditions")
    private void createSequence() {
        final String sequenceName = ctx.getUid() + ".schemaSequence_" + serializedEntityType;

        final boolean sequenceExists = ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select exists(select relname from pg_class where relkind = 'S' and relname = ?)",
                boolean.class,
                sequenceName.toLowerCase()
        );

        if (sequenceExists) {
            ctx.getCatalogCtx().getJdbcTemplate().update("alter sequence " + sequenceName + " restart");
        } else {
            ctx.getCatalogCtx().getJdbcTemplate().update("create sequence " + sequenceName);
        }
    }

    /**
     * Translates db's {@link DuplicateKeyException} on unique attribute to Evita's {@link UniqueValueViolationException}
     *
     * @param duplicateKeyException original db exception
     */
    private void throwUniqueValueViolationException(@Nonnull DuplicateKeyException duplicateKeyException) {
        final Matcher duplicateValueDataMather = SQL_INSERT_DUPLICATE_VALUE_DATA_PATTERN.matcher(duplicateKeyException.getCause().getMessage());

        if (!duplicateValueDataMather.find()) {
            // if information about violated attribute could not be found, just rethrow original exception
            throw new DuplicateKeyException("Unique attribute value constraint violated: ", duplicateKeyException);
        }
        final String attributeName = duplicateValueDataMather.group(1);
        final String attributeValue = duplicateValueDataMather.group(3);
        throw new UniqueValueViolationException(attributeName, attributeValue, -1, -1);
    }

    /**
     * Throws Evita's {@link UniqueValueViolationException} from specific attribute data
     *
     * @param entity         owning entity of attribute
     * @param attributeValue attribute with incorrect value
     */
    private void throwUniqueValueViolationException(@Nonnull Entity entity, @Nonnull AttributeValue attributeValue) {
        throw new UniqueValueViolationException(attributeValue.getKey().getAttributeName(), attributeValue.getValue(), -1, entity.getPrimaryKey());
    }

    @Value
    @RequiredArgsConstructor
    private static class ReferenceKey {
        int primaryKey;
        Serializable type;

        ReferenceKey(@Nonnull ReferenceContract reference) {
            this.primaryKey = reference.getReferencedEntity().getPrimaryKey();
            this.type = reference.getReferencedEntity().getType();
        }
    }

    /**
     * Descriptor of existing stored record (attribute/price/reference). Used for updating existing records
     */
    @Value
    private static class ExistingRecord {
        long internalId;
        int version;
    }
}
