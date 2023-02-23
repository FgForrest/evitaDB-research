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
import com.esotericsoftware.kryo.io.Input;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.common.ReadOnlyClassResolver;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.serialization.kryo.SerializationHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Retrieves and deserializes {@link Kryo}-serialized schema
 *
 * @author Lukáš Hornych 2022
 */
@RequiredArgsConstructor
public class EntitySchemaRowMapper implements RowMapper<EntitySchema> {

    private final EntityCollectionContext collectionCtx;

    @Override
    public EntitySchema mapRow(ResultSet rs, int rowNum) throws SQLException {
        final byte[] serializedSchema = rs.getBytes("detail");
        final byte[] serializedSerializationHeader = rs.getBytes("serializationHeader");

        final Input deserializedDataInput = new Input();

        final SerializationHeader header = collectionCtx.getEntitySchemaKryoManager().deserialize(new ReadOnlyClassResolver(), kryo -> {
            deserializedDataInput.setBuffer(serializedSerializationHeader);
            return kryo.readObject(deserializedDataInput, SerializationHeader.class);
        });

        final EntitySchema schema = collectionCtx.getEntitySchemaKryoManager().deserialize(new ReadOnlyClassResolver(header.getRecordedClasses()), kryo -> {
            deserializedDataInput.setBuffer(serializedSchema);
            return kryo.readObject(deserializedDataInput, EntitySchema.class);
        });

        deserializedDataInput.close();
        return schema;
    }
}
