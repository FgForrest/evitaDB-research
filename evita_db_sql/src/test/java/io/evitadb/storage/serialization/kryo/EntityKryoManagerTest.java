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
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.schema.EntitySchema;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EntityKryoManager}
 *
 * @author Lukáš Hornych 2022
 */
class EntityKryoManagerTest {

    @Test
    void shouldSerializeEntityAndThenDeserializeEntity() {
        final EntitySchema schema = new EntitySchema("product");
        final EntityKryoManager manager = new EntityKryoManager(new AtomicReference<>(schema), null);

        final Entity entity = new InitialEntityBuilder(schema, 1)
                .setAttribute("code", 2)
                .toInstance();

        final byte[] serializedEntity = manager.serialize(kryo -> {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final Output output = new Output(outputStream);

            kryo.writeObject(output, entity);

            output.close();
            return outputStream.toByteArray();
        });
        final Entity deserializedEntity = manager.deserialize(kryo -> {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedEntity);
            final Input input = new Input(inputStream);

            final Entity e = kryo.readObject(input, Entity.class);

            input.close();
            return e;
        });

        assertEquals(entity, deserializedEntity);
        assertTrue(manager.getSerializedSerializationHeader().length > 0);
    }
}