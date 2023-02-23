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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.exception.UnknownClassOnDeserializationException;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Kryo serializer for {@link SerializationHeader} (de)serialization
 *
 * @author Jiří Bonch, 2021
 * @author Lukáš Hornych 2021
 */
public class SerializationHeaderSerializer extends Serializer<SerializationHeader> {

    @Override
    public void write(Kryo kryo, Output output, SerializationHeader header) {
        serializeKeys(header.getKeys(), output, kryo);
        serializeClassRegistrations(header.getRecordedClasses(), output);
    }

    @Override
    public SerializationHeader read(Kryo kryo, Input input, Class<? extends SerializationHeader> type) {
        Map<Integer, Object> keys = deserializeKeys(input, kryo);
        List<ClassId> recordedClasses = deserializeClassRegistrations(input);

        return new SerializationHeader(keys, recordedClasses);
    }

    private void serializeKeys(@Nonnull Map<Integer, Object> keys, Output output, Kryo kryo) {
        output.writeVarInt(keys.size(), true);
        for (Entry<Integer, Object> entry : keys.entrySet()) {
            output.writeVarInt(entry.getKey(), true);
            kryo.writeClassAndObject(output, entry.getValue());
        }
    }

    private Map<Integer, Object> deserializeKeys(Input input, Kryo kryo) {
        final Map<Integer, Object> keys = new LinkedHashMap<>();
        final int keyCount = input.readVarInt(true);
        for (int i = 1; i <= keyCount; i++) {
            keys.put(
                    input.readVarInt(true),
                    kryo.readClassAndObject(input)
            );
        }
        return keys;
    }

    private void serializeClassRegistrations(@Nonnull List<ClassId> classIds, @Nonnull Output output) {
        output.writeVarInt(classIds.size(), true);
        for (ClassId recordedClass : classIds) {
            final Class<?> type = recordedClass.getType();
            final int classId = recordedClass.getId();
            final boolean isArray = type.isArray();
            final String typeName = isArray ? type.getComponentType().getName() : type.getName();

            output.writeVarInt(classId, true);
            output.writeBoolean(isArray);
            output.writeString(typeName);
        }
    }

    @Nonnull
    private List<ClassId> deserializeClassRegistrations(@Nonnull Input input) {
        final int registrationCount = input.readVarInt(true);
        final List<ClassId> classIdList = new ArrayList<>(registrationCount);
        for (int i = 0; i < registrationCount; i++) {
            final int id = input.readVarInt(true);
            final boolean isArray = input.readBoolean();
            final String typeName = input.readString();

            final Class<?> type = isArray ?
                    Array.newInstance(getClassTypeSafely(typeName), 0).getClass() :
                    getClassTypeSafely(typeName);

            classIdList.add(new ClassId(id, type));
        }
        return classIdList;
    }

    @Nonnull
    private static Class<?> getClassTypeSafely(String className) {
        if (Character.isLowerCase(className.charAt(0))) {
            if ("boolean".equals(className)) {
                return boolean.class;
            } else if ("byte".equals(className)) {
                return byte.class;
            } else if ("short".equals(className)) {
                return short.class;
            } else if ("int".equals(className)) {
                return int.class;
            } else if ("long".equals(className)) {
                return long.class;
            } else if ("char".equals(className)) {
                return char.class;
            }
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new UnknownClassOnDeserializationException(
                    "Serialized class " + className + " not found by actual classloader!"
            );
        }
    }
}
