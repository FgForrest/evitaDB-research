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

import java.util.function.Supplier;

/**
 * Sql condition representing group of facets grouped by current inter/inner group relations
 *
 * @author Lukáš Hornych 2021
 */
public interface FacetCondition extends Supplier<SqlPart> {

    /**
     * Reference to group which this condition represents
     */
    GroupReference getGroup();

    /**
     * Entity type of facets
     */
    String getSerializedEntityType();

    /**
     * Ids of facets in group
     */
    int[] getFacetIds();

    /**
     * Combines this condition with passed parameters. Group and entity type have to be same for both conditions
     */
    FacetCondition combine(GroupReference group, String serializedEntityType, int... facetIds);
}
