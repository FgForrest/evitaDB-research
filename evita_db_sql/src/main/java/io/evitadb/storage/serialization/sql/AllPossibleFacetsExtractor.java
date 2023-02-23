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

import io.evitadb.storage.query.builder.facet.AllPossibleFacets;
import io.evitadb.storage.query.builder.facet.FacetReference;
import io.evitadb.storage.query.builder.facet.GroupReference;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extracts all possible facet to group mappings. Used to calculate facet summary and statistics.
 *
 * @author Lukáš Hornych 2021
 */
public class AllPossibleFacetsExtractor implements ResultSetExtractor<AllPossibleFacets> {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    @Override
    public AllPossibleFacets extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (!rs.next()) {
            return null;
        }

        final Map<GroupReference, List<FacetReference>> groupedAllPossibleFacets = new TreeMap<>();
        final Map<FacetReference, GroupReference> facetToGroupMap = new TreeMap<>();

        do {
            final Integer referenceGroupPrimaryKey = rs.getObject("referenceGroupPrimaryKey", Integer.class);
            final String serializedReferenceGroupTypeDataType = rs.getObject("referenceGroupTypeDataType", String.class);
            final String serializedReferenceGroupType = rs.getObject("referenceGroupType", String.class);
            final Serializable referenceGroupType = STRING_TYPED_VALUE_SERIALIZER.deserialize(TypedValue.of(serializedReferenceGroupType, serializedReferenceGroupTypeDataType));

            final String serializedReferenceEntityTypeDataType = rs.getObject("referenceEntityTypeDataType", String.class);
            final String serializedReferenceEntityType = rs.getObject("referenceEntityType", String.class);
            final Serializable referenceEntityType = STRING_TYPED_VALUE_SERIALIZER.deserialize(TypedValue.of(serializedReferenceEntityType, serializedReferenceEntityTypeDataType));
            final int referenceEntityPrimaryKey = rs.getInt("referenceEntityPrimaryKey");
            final int referenceFacetCount = rs.getInt("referenceFacetCount");

            final GroupReference groupReference;
            if (referenceGroupPrimaryKey == null && referenceGroupType == null) {
                // use facet entity type as group entity type if facet is not in any group
                groupReference = new GroupReference(referenceEntityType, serializedReferenceEntityType, null);
            } else {
                groupReference = new GroupReference(referenceGroupType, serializedReferenceGroupType, referenceGroupPrimaryKey);
            }
            final FacetReference facetReference = new FacetReference(referenceEntityType, serializedReferenceEntityType, referenceEntityPrimaryKey, referenceFacetCount);

            final List<FacetReference> groupOfFacets = groupedAllPossibleFacets.computeIfAbsent(groupReference, k -> new LinkedList<>());
            groupOfFacets.add(facetReference);

            facetToGroupMap.put(facetReference, groupReference);
        } while (rs.next());

        return new AllPossibleFacets(groupedAllPossibleFacets, facetToGroupMap);
    }
}
