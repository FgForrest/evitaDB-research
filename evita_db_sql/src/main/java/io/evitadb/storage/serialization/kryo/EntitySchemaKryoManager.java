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

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.common.SerialVersionBasedSerializer;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * Provides support for serializing and deserializing {@link EntitySchema} using {@link Kryo}
 *
 * @author Lukáš Hornych 2022
 */
public class EntitySchemaKryoManager {

    /**
     * Used to serialize entity schema. Can be used only in single thread.
     *
     * @param function serialization function
     */
    public <R> R serialize(@Nonnull ClassResolver classResolver, @Nonnull Function<Kryo, R> function) {
        final Kryo kryo = createKryo(classResolver);
        return function.apply(kryo);
    }

    /**
     * Used to deserialize entity schema.
     *
     * @param function deserialization function
     */
    public <R> R deserialize(@Nonnull ClassResolver classResolver, @Nonnull Function<Kryo, R> function) {
        final Kryo kryo = createKryo(classResolver);
        return function.apply(kryo);
    }

    /**
     * Obtains ready {@link Kryo} instance configured for {@link EntitySchema} (de)serialization
     *
     * @param classResolver some class resolver to be used in the kryo instance
     * @return ready configured kryo instance
     */
    private Kryo createKryo(ClassResolver classResolver) {
        return KryoFactory.createKryo(
                classResolver,
                KryoFactory.SchemaKryoConfigurer.INSTANCE
                        .andThen(kryo -> kryo.register(
                                SerializationHeader.class,
                                new SerialVersionBasedSerializer<>(new SerializationHeaderSerializer(), SerializationHeader.class),
                                250
                        ))
        );
    }
}
