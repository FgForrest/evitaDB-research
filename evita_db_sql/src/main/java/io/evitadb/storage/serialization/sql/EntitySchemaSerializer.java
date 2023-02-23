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
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.serialization.kryo.ReadOnlyKeyCompressor;
import io.evitadb.storage.serialization.kryo.SerializationHeader;
import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.model.Hierarchy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;

import static io.evitadb.api.serialization.KryoFactory.CLASSES_RESERVED_FOR_INTERNAL_USE;

/**
 * Serializer to serialize {@link EntitySchema} to SQL using Kryo serialization.
 *
 * @see EntitySchemaRowMapper
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class EntitySchemaSerializer {

    private final EntityCollectionContext collectionCtx;

    /**
     * Serializes {@link EntitySchema} to {@link org.springframework.jdbc.core.namedparam.SqlParameterSource} ready to be inserted.
     *
     * @param entitySchema schema to serialize
     * @param hierarchyDescriptor hierarchy description in case if collection is hierarchical
     * @return serialized schema with header
     */
    @Nonnull
    public SqlParameterSource serializeToInsert(@Nonnull EntitySchema entitySchema, @Nullable Hierarchy hierarchyDescriptor) {
        final WritableClassResolver classResolver = new WritableClassResolver(CLASSES_RESERVED_FOR_INTERNAL_USE);

        return collectionCtx.getEntitySchemaKryoManager().serialize(classResolver, kryo -> {
            final ByteArrayOutputStream serializedDataStream = new ByteArrayOutputStream();
            final Output serializedDataOutput = new Output(serializedDataStream);

            final MapSqlParameterSource args = new MapSqlParameterSource();

            final String serializedEntityType = collectionCtx.getCatalogCtx().getSerializedEntityType(entitySchema.getName());
            args.addValue("entityType", serializedEntityType);

            // serialized schema
            args.addValue("detail", serializeSchema(kryo, serializedDataStream, serializedDataOutput, entitySchema));
            args.addValue("serializationHeader", serializeSerializationHeader(kryo, serializedDataStream, serializedDataOutput, classResolver));

            // hierarchy metadata
            if (entitySchema.isWithHierarchy()) {
                args.addValue("withHierarchy", true);
                args.addValue("hierarchyLevels", hierarchyDescriptor.getLevels());
                args.addValue("hierarchySectionSize", hierarchyDescriptor.getSectionSize());
            } else {
                args.addValue("withHierarchy", false);
            }

            return args;
        });
    }

    /**
     * Serialize {@link EntitySchema} to {@link SqlParameterSource} ready to be updated.
     *
     * @param entitySchema entity schema to serialize
     * @return ready pss
     */
    public SqlParameterSource serializeToUpdate(@Nonnull EntitySchema entitySchema) {
        final WritableClassResolver classResolver = new WritableClassResolver(CLASSES_RESERVED_FOR_INTERNAL_USE);

        return collectionCtx.getEntitySchemaKryoManager().serialize(classResolver, kryo -> {
            final ByteArrayOutputStream serializedDataStream = new ByteArrayOutputStream();
            final Output serializedDataOutput = new Output(serializedDataStream);

            final byte[] serializedSchema = serializeSchema(kryo, serializedDataStream, serializedDataOutput, entitySchema);
            final byte[] serializedSerializationHeader = serializeSerializationHeader(kryo, serializedDataStream, serializedDataOutput, classResolver);
            final String serializedEntityType = collectionCtx.getCatalogCtx().getSerializedEntityType(entitySchema.getName());

            final MapSqlParameterSource args = new MapSqlParameterSource();
            args.addValue("detail", serializedSchema);
            args.addValue("serializationHeader", serializedSerializationHeader);
            args.addValue("entityType", serializedEntityType);
            return args;
        });
    }

    public SqlParameterSource serializeHierarchyDescriptorToInsert(@Nonnull Hierarchy hierarchyDescriptor) {
        final MapSqlParameterSource args = new MapSqlParameterSource();
        args.addValue("hierarchyLevels", hierarchyDescriptor.getLevels());
        args.addValue("hierarchySectionSize", hierarchyDescriptor.getSectionSize());
        args.addValue("entityType", hierarchyDescriptor.getCode());
        return args;
    }

    /**
     * Serializes entity schema using Kryo to binary
     *
     * @param kryo kryo instance configured for entity schema serialization
     * @param serializedDataStream stream where serialized data are cached during this serialization
     * @param serializedDataOutput kryo output where serialized data are cached during this serialization
     * @param entitySchema entity schema to serialize
     * @return kryo serialized entity schema
     */
    private byte[] serializeSchema(@Nonnull Kryo kryo,
                                   @Nonnull ByteArrayOutputStream serializedDataStream,
                                   @Nonnull Output serializedDataOutput,
                                   @Nonnull EntitySchema entitySchema) {
        kryo.writeObject(serializedDataOutput, entitySchema);
        serializedDataOutput.close();
        final byte[] serializedSchema = serializedDataStream.toByteArray();
        serializedDataStream.reset();

        return serializedSchema;
    }

    /**
     * Serializes serialization header using Kryo to binary. Have to be called after serialization of entity so
     * that {@code classResolver} is filled with appropriate data.
     *
     * @param kryo kryo instance configured for entity schema serialization
     * @param serializedDataStream stream where serialized data are cached during this serialization
     * @param serializedDataOutput kryo output where serialized data are cached during this serialization
     * @param classResolver class resolver used during serialization of entity schema, should be filled with registrations
     * @return kryo serialized serialization header
     */
    private byte[] serializeSerializationHeader(@Nonnull Kryo kryo,
                                                @Nonnull ByteArrayOutputStream serializedDataStream,
                                                @Nonnull Output serializedDataOutput,
                                                @Nonnull WritableClassResolver classResolver) {
        kryo.writeObject(serializedDataOutput, new SerializationHeader(
                new ReadOnlyKeyCompressor(),
                classResolver.listRecordedClasses()
        ));
        serializedDataOutput.close();
        final byte[] serializedSerializationHeader = serializedDataStream.toByteArray();
        serializedDataStream.reset();

        return serializedSerializationHeader;
    }
}
