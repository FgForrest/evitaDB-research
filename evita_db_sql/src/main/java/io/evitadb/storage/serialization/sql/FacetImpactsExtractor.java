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

import io.evitadb.api.io.extraResult.FacetSummary;
import io.evitadb.storage.query.builder.facet.FacetReference;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts impact for each facet.
 *
 * @author Tomáš Pozler 2022
 */
@RequiredArgsConstructor
public class FacetImpactsExtractor implements ResultSetExtractor<Map<FacetReference, FacetSummary.RequestImpact>> {

    private final Set<FacetReference> facetReferences;

    @Override
    public Map<FacetReference, FacetSummary.RequestImpact> extractData(ResultSet rs) throws SQLException, DataAccessException {
        final Map<FacetReference, FacetSummary.RequestImpact> fImpact = new HashMap<>();
        while (rs.next()) {
            final int entityPrimaryKey = rs.getInt("entityPrimaryKey");
            final String entityType = rs.getString("entityType");
            final int impact = rs.getInt("impact");
            final int count = rs.getInt("count");
            final Optional<FacetReference> reference = facetReferences.stream().filter(f -> f.getFacetId() == entityPrimaryKey && f.getSerializedEntityType().equals(entityType)).findFirst();
            reference.ifPresent(facetReference -> fImpact.put(facetReference, new FacetSummary.RequestImpact(impact, count)));
        }
        return fImpact;
    }
}
