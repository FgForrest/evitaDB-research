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

package io.evitadb.storage.serialization.typedValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * <p>DTO that holds serialized value with original type.
 *
 * @param <SV> type of serialized form
 *
 * @see StringTypedValueSerializer
 * @see AttributeTypedValueSerializer
 * @author Lukáš Hornych 2021
 */
@Getter
@EqualsAndHashCode
@ToString
public class TypedValue<SV> {

    private final SV serializedValue;
    private final Class<?> type;

    /**
     * @param serializedValue serialized value
     * @param type original type of serialized value
     */
    protected TypedValue(SV serializedValue, Class<?> type) {
        this.serializedValue = serializedValue;
        this.type = type;
    }

    /**
     * @param serializedValue serialized value
     * @param type original type of serialized value
     * @param <SV> type of serialized value
     * @return new typed value
     */
    public static <SV> TypedValue<SV> of(SV serializedValue, Class<?> type) {
        return new TypedValue<>(serializedValue, type);
    }

    /**
     * @param serializedValue serialized value
     * @param serializedType original serialized type of serialized value in format {@link Class#getName()}
     * @param <SV> type of serialized value
     * @return new typed value
     */
    public static <SV> TypedValue<SV> of(SV serializedValue, String serializedType) {
        if (serializedType == null) {
            return of(serializedValue, (Class<?>) null);
        }

        try {
            final Class<?> deserializedType = Class.forName(serializedType);
            return of(serializedValue, deserializedType);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class for serialized type \"" + serializedType + "\".");
        }
    }

    /**
     * Special typed values representing {@code null}/unknown value of unknown type.
     *
     * @param <SV> type of serialized value
     * @return new typed value
     */
    public static <SV> TypedValue<SV> nullValue() {
        return of(null, (Class<?>) null);
    }

    public String getSerializedType() {
        return type.getName();
    }
}
