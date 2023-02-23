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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.AssociatedDataContract;
import io.evitadb.api.data.AttributesContract;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.key.AssociatedDataKeySerializer;
import io.evitadb.api.data.key.AttributeKeySerializer;
import io.evitadb.api.data.key.CompressiblePriceKey;
import io.evitadb.api.data.key.CompressiblePriceKeySerializer;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.common.ReadOnlyClassResolver;
import io.evitadb.api.serialization.common.SerialVersionBasedSerializer;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.api.utils.ReflectionLookup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.evitadb.api.serialization.KryoFactory.CLASSES_RESERVED_FOR_INTERNAL_USE;

/**
 * Manager for more efficient serialization with {@link Kryo}. This should be used everywhere where entity is to be
 * serialized/deserialized instead of directly using new Kryo instance.
 *
 * Manager is tied to single entity schema thus single instance can be used only in one entity collection.
 *
 * @author Tomáš Pozler
 * @author Lukáš Hornych 2021
 */
public class EntityKryoManager {

    private final AtomicReference<EntitySchema> entitySchema;

    private final ReadWriteKeyCompressor writableKeyCompressor;
    private final WritableClassResolver writableClassResolver;
    /**
     * There is only one instance of writable kryo because we do not support multi-threaded writes.
     */
    private final Kryo writeKryo;

    private final VersionedKryoPool readKryoPool;

    /**
     * @param entitySchema schema which will be passed to created Kryo instances.
     * @param serializationHeader serialized header containing registered classes and cached keys
     */
    public EntityKryoManager(@Nonnull AtomicReference<EntitySchema> entitySchema, @Nullable byte[] serializationHeader) {
        this.entitySchema = entitySchema;

        if (serializationHeader == null) {
            this.writableKeyCompressor = new ReadWriteKeyCompressor();
            this.writableClassResolver = new WritableClassResolver(CLASSES_RESERVED_FOR_INTERNAL_USE);
        } else {
            final Kryo headerKryo = createKryo(1, new ReadOnlyClassResolver(), new ReadOnlyKeyCompressor());

            final Input input = new Input();
            input.setBuffer(serializationHeader);
            final SerializationHeader header = headerKryo.readObject(input, SerializationHeader.class);

            this.writableKeyCompressor = new ReadWriteKeyCompressor(header.getKeys());
            this.writableClassResolver = new WritableClassResolver(header.getRecordedClasses());
        }

        this.writeKryo = createKryo(1, writableClassResolver, writableKeyCompressor);
        this.readKryoPool = new VersionedKryoPool(
                version -> createKryo(
                        version,
                        new ReadOnlyClassResolver(writableClassResolver.listRecordedClasses()),
                        new ReadOnlyKeyCompressor(writableKeyCompressor.getKeys())
                )
        );
    }

    /**
     * Used to serialize entity data. Can be used only in single thread.
     *
     * @param function serialization function
     */
    public <R> R serialize(@Nonnull Function<Kryo, R> function) {
        final R result = function.apply(writeKryo);
        if (writableKeyCompressor.resetDirtyFlag() | writableClassResolver.resetDirtyFlag()) {
            this.readKryoPool.expireAllPreviouslyCreated();
        }
        return result;
    }

    /**
     * Used to deserialize entity data.
     *
     * @param function deserialization function
     */
    public <R> R deserialize(@Nonnull Function<VersionedKryo, R> function) {
        return readKryoPool.borrowAndExecute(function);
    }

    /**
     * Returns serialized current version of serialization header.
     */
    public byte[] getSerializedSerializationHeader() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Output output = new Output(outputStream);
        writeKryo.writeObject(output, new SerializationHeader(
                writableKeyCompressor,
                writableClassResolver.listRecordedClasses()
        ));
        output.close();
        return outputStream.toByteArray();
    }

    /**
     * Obtains ready {@link Kryo} instance configured for {@link Entity} (de)serialization
     *
     * @param classResolver some class resolver to be used in the kryo instance
     * @param keyCompressor some key compressor to capture cached keys
     * @return ready configured kryo instance
     */
    private VersionedKryo createKryo(long version, @Nonnull ClassResolver classResolver, @Nonnull KeyCompressor keyCompressor) {
        final KryoSerializationHelper serializationHelper = new DefaultKryoSerializationHelper();

        return VersionedKryoFactory.createKryo(version, classResolver, new KryoFactory.EntityKryoConfigurer(
                entitySchema::get,
                new ReflectionLookup(ReflectionCachingBehaviour.CACHE),
                keyCompressor
        ).andThen((kryo -> {
            kryo.register(AttributesContract.AttributeKey.class, new SerialVersionBasedSerializer<>(new AttributeKeySerializer(), AttributesContract.AttributeKey.class), 450);
            kryo.register(AssociatedDataContract.AssociatedDataKey.class, new SerialVersionBasedSerializer<>(new AssociatedDataKeySerializer(), AssociatedDataContract.AssociatedDataKey.class), 451);
            kryo.register(CompressiblePriceKey.class, new SerialVersionBasedSerializer<>(new CompressiblePriceKeySerializer(serializationHelper), CompressiblePriceKey.class), 452);
            kryo.register(SerializationHeader.class, new SerialVersionBasedSerializer<>(new SerializationHeaderSerializer(), SerializationHeader.class), 453);
        })));
    }
}
