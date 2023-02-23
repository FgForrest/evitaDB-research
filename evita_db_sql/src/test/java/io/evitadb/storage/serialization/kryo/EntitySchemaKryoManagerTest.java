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

package io.evitadb.storage.serialization.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.common.WritableClassResolver;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static io.evitadb.api.serialization.KryoFactory.CLASSES_RESERVED_FOR_INTERNAL_USE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link EntitySchemaKryoManager}
 *
 * @author Lukáš Hornych 2022
 */
class EntitySchemaKryoManagerTest {

    @Test
    void shouldSerializedAndThenDeserializeSchema() {
        final EntitySchemaKryoManager manager = new EntitySchemaKryoManager();

        final EntitySchema schema = new EntitySchema("product");
        final WritableClassResolver classResolver = new WritableClassResolver(CLASSES_RESERVED_FOR_INTERNAL_USE);
        final byte[] serializedSchema = manager.serialize(classResolver, kryo -> {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final Output output = new Output(outputStream);

            kryo.writeObject(output, schema);

            output.close();
            return outputStream.toByteArray();
        });
        final EntitySchema deserializedSchema = manager.deserialize(classResolver, kryo -> {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedSchema);
            final Input input = new Input(inputStream);

            final EntitySchema s = kryo.readObject(input, EntitySchema.class);

            input.close();
            return s;
        });

        assertEquals(schema, deserializedSchema);
    }
}