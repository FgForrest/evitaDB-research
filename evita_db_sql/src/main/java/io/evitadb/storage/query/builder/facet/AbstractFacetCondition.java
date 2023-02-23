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

package io.evitadb.storage.query.builder.facet;

import io.evitadb.storage.query.SqlPart;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Ancestor for all facet conditions with base data and helper methods.
 *
 * @author Lukáš Hornych 2021
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractFacetCondition implements FacetCondition {

    private final GroupReference group;
    private final String serializedEntityType;
    private final int[] facetIds;

    protected SqlPart buildSingleFacetSqlCondition(int facetId) {
        return new SqlPart(
                "facets @@ ('exists($.' || ? || '[*] ? (@ == ' || ? || '))')::jsonpath",
                List.of(serializedEntityType, facetId)
        );
    }
}
