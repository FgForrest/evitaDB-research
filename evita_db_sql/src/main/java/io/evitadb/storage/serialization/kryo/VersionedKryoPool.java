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
import com.esotericsoftware.kryo.util.Pool;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * Kryo pool supporting {@link VersionedKryo}. Version is used to discard all borrowed Kryo instances which have
 * obsolete data (registered classed and keys).
 *
 * @author Tomáš Pozler
 * @author Lukáš Hornych 2021
 */
public class VersionedKryoPool extends Pool<VersionedKryo> {

    /**
     * Function allows creating new instance of {@link VersionedKryo} with current Pool version.
     */
    private final Function<Long, VersionedKryo> kryoCreator;

    /**
     * Version increases only by calling {@link #expireAllPreviouslyCreated()} method and allows to discard all
     * obsolete {@link VersionedKryo} instances when they are about to be returned back to pool.
     */
    private long version = 1L;

    public VersionedKryoPool(@Nonnull Function<Long, VersionedKryo> kryoCreator) {
        super(true, false, 64);
        this.kryoCreator = kryoCreator;
    }

    /**
     * Method allowing safe way for obtaining {@link Kryo} instance and returning it back to the pool.
     */
    public <T> T borrowAndExecute(Function<VersionedKryo, T> logic) {
        final VersionedKryo kryo = this.obtain();
        try {
            return logic.apply(kryo);
        } finally {
            this.free(kryo);
        }
    }

    /**
     * This method will increase version of this pool which makes all previously created {@link VersionedKryo}
     * instances obsolete. Borrowed instances will still work but when they are returned back by {@link #free(VersionedKryo)}
     * method they are not accepted back to pool and they are going to be garbage collected. New {@link VersionedKryo}
     * instances will be created on their place and these new versions will possibly have new configuration of key
     * internal inputs.
     */
    public void expireAllPreviouslyCreated() {
        this.version++;
        this.clear();
    }

    /**
     * Creates new instance of {@link VersionedKryo} with current configuration.
     */
    @Override
    protected VersionedKryo create() {
        return kryoCreator.apply(version);
    }

    /**
     * Returns borrowed instance back to the pool.
     */
    @Override
    public void free(VersionedKryo object) {
        // if object version is the same as actual version, accept it,
        // otherwise it would be discarded and garbage collected
        if (object.getVersion() == version) {
            super.free(object);
        }
    }
}
