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

package io.evitadb.storage.query.builder;

import io.evitadb.storage.ComplexEntityQueryContextHolder;
import io.evitadb.storage.query.SqlPart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_PRIMARY_KEY;
import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_TYPE;

/**
 * Sql query builder for fetching {@link io.evitadb.api.io.extraResult.Parents} extra result.
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParentsQueryBuilder {

    /**
     * Universal SQL query for fetching complete parents for multiple referenced collections.
     */
    public static SqlPart buildParentsQuery(boolean requiresFullEntity,
                                            @Nullable String[] parentTypes) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("select case " +
                        "       when placement.type = '" + SELF_TYPE + "' then entity.type " +
                        "       else placement.type " +
                        "   end as parentsType, " +
                        "   entity.primaryKey as entityPrimaryKey, " +
                        "   case " +
                        "       when placement.primaryKey = '" + SELF_PRIMARY_KEY + "' then entity.primaryKey " +
                        "       else placement.primaryKey::int " +
                        "   end as referencedEntityPrimaryKey, " +
                        "   ").append(buildColumnList(requiresFullEntity))
                // search through cached entities only
                .append(" from ").append(ComplexEntityQueryContextHolder.getContext().getTempTableName()).append(" entity ")
                // populate cached entity's referenced hierarchy placements
                .append("join t_entityHierarchyPlacement placement" +
                        "   on placement.entity_id = entity.entity_id");
        if (parentTypes != null) {
            sqlBuilder
                .append("       and placement.type = any (?)");
        }
        sqlBuilder
                .append(// find parents for each referenced hierarchy placement
                        " join t_entity parent" +
                        "   on" +
                        "       case " +
                        "           when placement.type = '" + SELF_TYPE + "' then parent.type = entity.type and parent.primaryKey != entity.primaryKey " + // also exclude self if searching in own hierarchy
                        "           else parent.type = placement.type " +
                        "       end " +
                        "       and parent.leftBound <= placement.leftBound" +
                        "       and parent.rightBound >= placement.rightBound " +
                        "order by placement.type, entity.primaryKey, placement.primaryKey, parent.level asc");

        final List<Object> args = new LinkedList<>();
        if (parentTypes != null) {
            args.add(parentTypes);
        }

        return new SqlPart(sqlBuilder, args);
    }

    private static String buildColumnList(boolean requiresFullEntity) {
        return new EntityColumnListBuilder("parent")
                .serializedEntity(requiresFullEntity)
                .primaryKey(!requiresFullEntity)
                .hierarchyPlacement()
                .build();
    }
}
