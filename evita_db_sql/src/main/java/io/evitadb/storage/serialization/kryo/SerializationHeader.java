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

import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.KeyCompressor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import static io.evitadb.api.utils.Assert.notNull;

/**
 * Generic metadata header for Kryo serialized data (i.e. {@link Entity}s, {@link io.evitadb.api.schema.EntitySchema}).
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 */
@Getter
@RequiredArgsConstructor
public class SerializationHeader {

    private static final long serialVersionUID = -4178440526921280066L;

    /**
     * Recorded keys from {@link KeyCompressor}
     */
    private final
    Map<Integer, Object> keys;

    /**
     * Recorded registrations from {@link com.esotericsoftware.kryo.ClassResolver}
     */
    private final List<ClassId> recordedClasses;

    public SerializationHeader(@Nonnull KeyCompressor keyCompressor, @Nonnull List<ClassId> recordedClasses) {
        notNull(keyCompressor, "Key compressor is required in header.");
        notNull(recordedClasses, "List of recorded classes during serialization is required in header.");

        this.keys = keyCompressor.getKeys();
        this.recordedClasses = recordedClasses;
    }
}
