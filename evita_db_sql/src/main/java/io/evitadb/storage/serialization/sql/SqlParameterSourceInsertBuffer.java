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

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Generic insert buffer for objects that are serialized to {@link SqlParameterSource} and should be all insert in single
 * batch.
 *
 * @author Lukáš Hornych 2021
 */
public class SqlParameterSourceInsertBuffer {

    /**
     * Holds actual serialized objects.
     */
    private final List<SqlParameterSource> data = new LinkedList<>();

    /**
     * Adds new serialized object to buffer
     *
     * @param item serialized object
     */
    public void add(@Nonnull SqlParameterSource item) {
        data.add(item);
    }

    /**
     * Resets the buffer to initial state so that the buffer can be reused.
     */
    public void reset() {
        data.clear();
    }

    /**
     * @return if buffer is empty (no data buffered)
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * @return size of data in buffer
     */
    public int size() {
        return data.size();
    }

    /**
     * Returns buffer data as array.
     *
     * @return array of data
     */
    public SqlParameterSource[] toArray() {
        return data.toArray(SqlParameterSource[]::new);
    }
}
