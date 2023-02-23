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

package io.evitadb.storage;

import io.evitadb.api.CatalogState;
import lombok.Setter;
import one.edee.oss.pmptt.PMPTT;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.dao.SqlStorage;
import one.edee.oss.pmptt.dao.WarmingUpMemoryStorage;
import one.edee.oss.pmptt.model.Hierarchy;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.NoSuchElementException;

/**
 * Manager for managing entity collection hierarchies in {@link io.evitadb.api.SqlEvita}. It replaces the {@link PMPTT}
 * which tries to create hierarchy if not exists, which is not suitable for this use of this library as hierarchy creation
 * is handled together with entity schema store.
 *
 * It respects catalog's {@link CatalogState} and changes its behaviour depending on that state.
 *
 * @author Lukáš Hornych 2021
 */
public class HierarchyManager {

    @Setter
    private CatalogContext catalogContext;
    private HierarchyStorage hierarchyStorage;

    /**
     * Default levels assigned to every new hierarchy
     */
    private final short hierarchyLevels;
    /**
     * Default section size assigned to every new hierarchy
     */
    private final short hierarchySectionSize;

    /**
     * Creates new hierarchy manager in {@link CatalogState#WARMING_UP} state. This means that all hierarchy data are
     * stored in memory as well as in entity buffers. It is because sql hierarchy storage could not see hierarchy data in
     * database as database is not filled up.
     *
     * @param hierarchyLevels default levels assigned to every new hierarchy
     * @param hierarchySectionSize default section size assigned to every new hierarchy
     */
    public HierarchyManager(short hierarchyLevels, short hierarchySectionSize) {
        this.hierarchyLevels = hierarchyLevels;
        this.hierarchySectionSize = hierarchySectionSize;
        this.hierarchyStorage = new WarmingUpMemoryStorage(); // temporary storage for warming up state
    }

    /**
     * Creates new hierarchy descriptor ready to be stored. Should be used only when change entity schema not anywhere
     * else (for this use-case always use {@link #getHierarchy(Serializable)})
     *
     * @param entityType unique entity type of hierarchical collection
     * @return new hierarchy descriptor
     */
    public Hierarchy createHierarchyDescriptor(@Nonnull Serializable entityType) {
        final String serializedEntityType = catalogContext.getSerializedEntityType(entityType);
        final Hierarchy newHierarchy = new Hierarchy(serializedEntityType, hierarchyLevels, hierarchySectionSize);
        hierarchyStorage.createHierarchy(newHierarchy);
        return newHierarchy;
    }

    /**
     * Returns existing hierarchy of certain (unique) code.
     *
     * @param entityType entity type of hierarchical collection
     * @return hierarchy for collection looked up
     * @throws IllegalArgumentException when dimensions in the arguments don't match dimensions of already created hierarchy
     * @throws NoSuchElementException when no hierarchy exists
     */
    public Hierarchy getHierarchy(@Nonnull Serializable entityType) {
        final String serializedEntityType = catalogContext.getSerializedEntityType(entityType);
        final Hierarchy hierarchy = hierarchyStorage.getHierarchy(serializedEntityType);
        if (hierarchy == null) {
            throw new NoSuchElementException("Hierarchy for collection " + serializedEntityType + " does not exists.");
        }
        return hierarchy;
    }

    /**
     * Switched manager to {@link CatalogState#ALIVE} state. It changes hierarchy data storage to sql storage so that
     * querying and entity updates can query actual stored hierarchy data.
     */
    public void goLive() {
        // Start using permanent PMPTT storage for transactional state.
        // All data stored in temporary memory storage during warming up state are also stored in buffered entity data,
        // therefore no data is lost even with new empty storage initialization.
        this.hierarchyStorage = new SqlStorage(catalogContext);
    }
}
