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

package io.evitadb.storage.query.builder.facet.summary;

import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.builder.facet.AbstractAndFacetCondition;
import io.evitadb.storage.query.builder.facet.FacetCondition;
import io.evitadb.storage.query.builder.facet.GroupReference;

import java.util.*;

/**
 * Facet condition with AND relation for computing facet summary and statistics
 */
public class SummaryAndFacetCondition extends AbstractAndFacetCondition {

    public SummaryAndFacetCondition(GroupReference group, String serializedEntityType, int... facetIds) {
        super(group, serializedEntityType, facetIds);
    }

    @Override
    public SqlPart get() {
        final StringBuilder sqlBuilder = new StringBuilder();
        final List<Object> args = new LinkedList<>();

        final PrimitiveIterator.OfInt facetIdIterator = Arrays.stream(getFacetIds()).iterator();
        while (facetIdIterator.hasNext()) {
            final SqlPart condition = buildSingleFacetSqlCondition(facetIdIterator.next());
            sqlBuilder.append(condition.getSql());
            args.addAll(condition.getArgs());

            if (facetIdIterator.hasNext()) {
                sqlBuilder.append(" and ");
            }
        }

        return new SqlPart(sqlBuilder, args);
    }

    @Override
    public FacetCondition combine(GroupReference group, String serializedEntityType, int... facetIds) {
        Assert.isTrue(getSerializedEntityType().equals(serializedEntityType), "Sanity check!");
        Assert.isTrue(Objects.equals(getGroup(), group), "Sanity check!");
        return new SummaryAndFacetCondition(
                getGroup(),
                serializedEntityType,
                ArrayUtils.mergeArrays(getFacetIds(), facetIds)
        );
    }
}
