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

package one.edee.oss.pmptt.dao;

import one.edee.oss.pmptt.dao.memory.MemoryStorage;

/**
 * Extension of {@link MemoryStorage} to be used during catalog's {@link io.evitadb.api.CatalogState#WARMING_UP} state.
 * It exists to ensure that entities in buffer which are not flushed to database yet have access to whole hierarchy.
 * Main purpose of this extension is to mimic special behaviour (which does not correspond to library's contract but is
 * needed in order to be able to use it with Evita structure) of some methods used in {@link SqlStorage}.
 *
 * After state is switched to {@link io.evitadb.api.CatalogState#ALIVE} the {@link SqlStorage} should be used.
 *
 * @author Lukáš Hornych 2021
 */
public class WarmingUpMemoryStorage extends MemoryStorage {

    @Override
    public boolean removeHierarchy(String code) {
        // Removing of hierarchy currently means removing of whole collection which is handled by methods on collection.
        throw new IllegalStateException("Hierarchy for collection cannot be deleted separately. Use catalog methods to delete whole collections.");
    }
}
