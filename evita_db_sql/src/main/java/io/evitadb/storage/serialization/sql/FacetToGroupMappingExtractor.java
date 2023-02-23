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

import io.evitadb.storage.query.builder.facet.FacetCollection;
import io.evitadb.storage.query.builder.facet.GroupReference;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extracts facet to group mappings. Used to fetch entities by facets by selected facets.
 *
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class FacetToGroupMappingExtractor implements ResultSetExtractor<Map<GroupReference, FacetCollection>> {

    private final FilterTranslatingContext ctx;

    @Nullable
    @Override
    public Map<GroupReference, FacetCollection> extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (!rs.next()) {
            return null;
        }

        final Map<GroupReference, FacetCollection> groupedFacets = new TreeMap<>();

        GroupReference currentGroup = null;
        FacetCollection currentGroupFacetIds = null;
        boolean noMoreGroups = false;
        do {
            // group data
            final String serializedGroupType = rs.getObject("groupType", String.class);
            final String serializedGroupTypeDataType = rs.getObject("groupTypeDataType", String.class);
            final Serializable groupType = ctx.getStringTypedValueSerializer()
                    .deserialize(TypedValue.of(serializedGroupType, serializedGroupTypeDataType));
            final Integer groupPrimaryKey = rs.getObject("groupPrimaryKey", Integer.class);

            // facet data
            final String serializedEntityType = rs.getObject("entityType", String.class);
            final String serializedEntityTypeDataType = rs.getObject("entityTypeDataType", String.class);
            final Serializable entityType = ctx.getStringTypedValueSerializer()
                    .deserialize(TypedValue.of(serializedEntityType, serializedEntityTypeDataType));
            final int entityPrimaryKey = rs.getInt("entityPrimaryKey");

            if (!noMoreGroups) {
                // no more groups are available or none at all
                if (groupType == null) {
                    // pass logic
                    noMoreGroups = true;
                    // some group is present
                } else {
                    // extract group from current row
                    final GroupReference newGroup = new GroupReference(groupType, serializedGroupType, groupPrimaryKey);
                    // if group on current row is different from current, flush current and put new one to current
                    if (!newGroup.equals(currentGroup)) {
                        // flush current group if exists
                        if (currentGroup != null) {
                            groupedFacets.put(currentGroup, currentGroupFacetIds);
                        }
                        // reset current group
                        currentGroup = newGroup;
                        currentGroupFacetIds = new FacetCollection(serializedEntityType);
                    }
                }
            }

            if (noMoreGroups) {
                // extract "group from facet entity" type from current row
                final GroupReference newGroupFromFacetEntity = new GroupReference(entityType, serializedEntityType, null);
                // if group on current row is different from current, flush current and put new one to current
                if (!newGroupFromFacetEntity.equals(currentGroup)) {
                    // flush current group if exists
                    if (currentGroup != null) {
                        groupedFacets.put(currentGroup, currentGroupFacetIds);
                    }
                    // reset current group
                    currentGroup = newGroupFromFacetEntity;
                    currentGroupFacetIds = new FacetCollection(serializedEntityType);
                }
            }

            // add facet on current row to work collection
            currentGroupFacetIds.add(entityPrimaryKey);
        } while (rs.next());
        // flush current group that stayed after last row
        groupedFacets.put(currentGroup, currentGroupFacetIds);

        return groupedFacets;
    }
}
