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

import io.evitadb.api.data.structure.Entity;
import io.evitadb.storage.EntityCollectionContext;
import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.annotation.Nonnull;

import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_PRIMARY_KEY;
import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_TYPE;

/**
 * Serializer for serializing hierarchy placements extracted from {@link HierarchyItem}s for db storage
 *
 * @author Lukáš Hornych 2022
 */
@RequiredArgsConstructor
public class HierarchyPlacementSerializer {

    private final EntityCollectionContext entityCollectionCtx;

    /**
     * Serializes placement to insert buffer
     *
     * @param insertBuffer insert buffer for hierarchy placements
     * @param hierarchyItem item to extract placement from to serialize
     * @param entity owning entity
     * @param entityId internal id of owning entity
     */
    public void serializeToInsert(@Nonnull SqlParameterSourceInsertBuffer insertBuffer,
                                  @Nonnull HierarchyItem hierarchyItem,
                                  @Nonnull Entity entity,
                                  long entityId) {
        final MapSqlParameterSource args = new MapSqlParameterSource();

        args.addValue("entity_id", entityId);
        final String placementEntityType;
        final String placementPrimaryKey;
        if (hierarchyItem.getHierarchyCode().equals(entityCollectionCtx.getSerializedEntityType()) &&
            hierarchyItem.getCode().equals(String.valueOf(entity.getPrimaryKey()))) {
            placementEntityType = SELF_TYPE;
            placementPrimaryKey = SELF_PRIMARY_KEY;
        } else {
            placementEntityType = hierarchyItem.getHierarchyCode();
            placementPrimaryKey = hierarchyItem.getCode();
        }
        args.addValue("type", placementEntityType);
        args.addValue("primaryKey", placementPrimaryKey);
        args.addValue("leftBound", hierarchyItem.getLeftBound());
        args.addValue("rightBound", hierarchyItem.getRightBound());
        args.addValue("level", hierarchyItem.getLevel());

        insertBuffer.add(args);
    }
}
