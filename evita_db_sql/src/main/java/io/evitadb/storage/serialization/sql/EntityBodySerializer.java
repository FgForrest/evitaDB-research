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

package io.evitadb.storage.serialization.sql;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.EntityCollectionContext;
import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Serializer for inserting/updating {@link Entity} bodies to database.
 * It does not serialize whole entity but rather its basic metadata and cached {@link Kryo-serialized original entity for
 * quick retrieval.
 *
 * @see EntityRowMapper
 * @see EntityReferenceRowMapper
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class EntityBodySerializer {

    private final EntityCollectionContext entityCollectionCtx;

    /**
     * Serializes {@link Entity} body to {@link org.springframework.jdbc.core.namedparam.SqlParameterSource} ready to be inserted.
     *
     * @param insertBuffer insert buffer for new entities
     * @param entity entity to serialize
     * @param hierarchyPlacement self hierarchy placement in case of hierarchical entity
     * @param entityId globally unique internal entity id
     */
    public void serializeToInsert(@Nonnull SqlParameterSourceInsertBuffer insertBuffer,
                                  @Nonnull Entity entity,
                                  @Nullable HierarchyItem hierarchyPlacement,
                                  long entityId) {
        final MapSqlParameterSource queryArgs = serializeCommonAttributes(
                entity,
                entityId,
                hierarchyPlacement
        );

        queryArgs.addValue("primaryKey", entity.getPrimaryKey());
        queryArgs.addValue("type", entityCollectionCtx.getSerializedEntityType());

        insertBuffer.add(queryArgs);
    }

    /**
     * Serialize {@link Entity} to {@link PreparedStatementSetter} ready to be updated.f
     *
     * @param entity entity to serialize
     * @param hierarchyPlacement self hierarchy placement in case of hierarchical entity
     * @param entityId internal id of entity
     * @return ready source
     */
    public SqlParameterSource serializeToUpdate(@Nonnull Entity entity,
                                                @Nullable HierarchyItem hierarchyPlacement,
                                                long entityId) {
        return serializeCommonAttributes(
                entity,
                entityId,
                hierarchyPlacement
        );
    }

    /**
     * Serializes common entity data for its inserting and updating
     *
     * @param entity entity to be inserted / updated
     * @param entityId internal id of the entity
     * @param hierarchyPlacement self hierarchy placement in case of hierarchical entity
     * @return sql parameter map
     */
    private MapSqlParameterSource serializeCommonAttributes(@Nonnull Entity entity,
                                                            long entityId,
                                                            @Nullable HierarchyItem hierarchyPlacement) {
        final ByteArrayOutputStream serializedDataStream = new ByteArrayOutputStream();
        final Output serializedDataOutput = new Output(serializedDataStream);

        final MapSqlParameterSource args = new MapSqlParameterSource();

        args.addValue("entityId", entityId);
        args.addValue("version", entity.getVersion());
        args.addValue("dropped", entity.isDropped());

        args.addValue("locales", entity.getLocales().stream().map(Locale::toString).toArray(String[]::new));

        if (hierarchyPlacement != null) {
            Assert.notNull(hierarchyPlacement, "Entity is hierarchical but not placement present.");

            // only to be able to reconstruct entity back
            args.addValue("parentPrimaryKey", entity.getHierarchicalPlacement().getParentPrimaryKey());

            // in case of hierarchy entity, there is expected to be single self item
            args.addValue("leftBound", hierarchyPlacement.getLeftBound());
            args.addValue("rightBound", hierarchyPlacement.getRightBound());
            args.addValue("level", hierarchyPlacement.getLevel());
            args.addValue("numberOfChildren", hierarchyPlacement.getNumberOfChildren());
            args.addValue("orderAmongSiblings", hierarchyPlacement.getOrder());
            args.addValue("hierarchyBucket", hierarchyPlacement.getBucket());
        } else {
            args.addValue("leftBound", null);
            args.addValue("rightBound", null);
            args.addValue("level", null);
            args.addValue("parentPrimaryKey", null);
            args.addValue("numberOfChildren", null);
            args.addValue("orderAmongSiblings", null);
            args.addValue("hierarchyBucket", null);
        }

        entityCollectionCtx.getEntityKryoManager().serialize(entityKryo ->  {
            args.addValue("serializedEntity", serializeEntityToCache(entityKryo, serializedDataStream, serializedDataOutput, entity));
            return null;
        });

        return args;
    }

    /**
     * Serializes whole entity to cache form for quick entity retrieval
     *
     * @param kryo kryo instance configured for entity serialization
     * @param serializedDataStream stream where serialized data are cached during this serialization
     * @param serializedDataOutput kryo output where serialized data are cached during this serialization
     * @param entity entity which is being serialized
     * @return kryo serialized locales
     */
    private byte[] serializeEntityToCache(@Nonnull Kryo kryo,
                                           @Nonnull ByteArrayOutputStream serializedDataStream,
                                           @Nonnull Output serializedDataOutput,
                                           @Nonnull Entity entity) {
        kryo.writeObject(serializedDataOutput, entity);
        serializedDataOutput.close();
        final byte[] serializedEntity = serializedDataStream.toByteArray();
        serializedDataStream.reset();

        return serializedEntity;
    }
}
