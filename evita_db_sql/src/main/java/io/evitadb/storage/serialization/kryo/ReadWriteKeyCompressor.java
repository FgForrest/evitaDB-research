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

import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This implementation of {@link KeyCompressor} is used for accessing and creating new mappings between keys and integer
 * ids that are used in persisted (serialized) form to minimize space occupied by the Evita DB records.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
public class ReadWriteKeyCompressor implements KeyCompressor {
    /**
     * Contains key index extracted from {@link io.evitadb.api.serialization.KeyCompressor} that is necessary for
     * bootstraping {@link io.evitadb.api.serialization.KeyCompressor} used for MemTable deserialization.
     */
    @Getter
    private final Map<Integer, Object> idToKeyIndex;
    /**
     * Reverse lookup index to {@link #idToKeyIndex}
     */
    private final Map<Object, Integer> keyToIdIndex;
    /**
     * Sequence used for generating new monotonic ids for registered keys.
     */
    private final AtomicInteger sequence;
    /**
     * Contains TRUE when there are new keys registered in this instance.
     */
    private final AtomicBoolean dirty = new AtomicBoolean();

    public ReadWriteKeyCompressor(Map<Integer, Object> keys) {
        int peek = 0;
        this.idToKeyIndex = new HashMap<>(keys.size());
        this.keyToIdIndex = new HashMap<>(keys.size());
        for (Map.Entry<Integer, Object> entry : keys.entrySet()) {
            this.idToKeyIndex.put(entry.getKey(), entry.getValue());
            this.keyToIdIndex.put(entry.getValue(), entry.getKey());
            if (entry.getKey() > peek) {
                peek = entry.getKey();
            }
        }
        this.sequence = new AtomicInteger(peek);
    }

    public ReadWriteKeyCompressor() {
        this(new HashMap<>());
    }

    /**
     * Method returns TRUE if there were any changes in this instance since last reset or creation.
     * @return
     */
    public boolean resetDirtyFlag() {
        return dirty.getAndSet(false);
    }

    @Override
    public @Nonnull
    Map<Integer, Object> getKeys() {
        return idToKeyIndex;
    }

    @Override
    public <T extends Comparable<T>> int getId(@Nonnull T key) {
        return keyToIdIndex.computeIfAbsent(key, o -> {
            final int id = sequence.incrementAndGet();
            idToKeyIndex.put(id, o);
            dirty.compareAndSet(false, true);
            return id;
        });
    }

    @Nullable
    @Override
    public <T extends Comparable<T>> Integer getIdIfExists(@Nonnull T key) {
        return keyToIdIndex.get(key);
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>> T getKeyForId(int id) {
        final Object key = idToKeyIndex.get(id);
        Assert.notNull(key, "There is no key for id " + id + "!");
        //noinspection unchecked
        return (T) key;
    }
}