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

package io.evitadb.storage.query.builder.facet.filter;

import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.builder.facet.AbstractAndFacetCondition;
import io.evitadb.storage.query.builder.facet.FacetCondition;
import io.evitadb.storage.query.builder.facet.GroupReference;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Facet condition with AND relation for finding entities by selected facets
 *
 * @author Lukáš Hornych 2021
 */
public class FilterAndFacetCondition extends AbstractAndFacetCondition {

    private final FilterTranslatingContext ctx;

    public FilterAndFacetCondition(FilterTranslatingContext ctx, GroupReference group, String serializedEntityType, int... facetIds) {
        super(group, serializedEntityType, facetIds);
        this.ctx = ctx;
    }

    @Override
    public SqlPart get() {
        final StringBuilder cteSqlBuilder = new StringBuilder()
                .append("select reference.entity_id " +
                        "from ").append(ctx.getCollectionUid()).append(".t_referenceIndex reference " +
                        "where reference.entityType = ?" +
                        "   and reference.entityPrimaryKey = any (?) " +
                        "group by reference.entity_id " +
                        "having count(*) = ?"); // simulates AND by checking the overall count of found references

        final List<Object> cteArgs = new LinkedList<>();
        cteArgs.add(getSerializedEntityType());
        cteArgs.add(getFacetIds());
        cteArgs.add(getFacetIds().length);

        final String cteAlias = ctx.addWithCte(new SqlPart(cteSqlBuilder, cteArgs), false);
        return new SqlPart("entity.entity_id = any (select entity_id from " + cteAlias + ")");
    }

    @Override
    public FacetCondition combine(GroupReference group, String serializedEntityType, int... facetIds) {
        Assert.isTrue(getSerializedEntityType().equals(serializedEntityType), "Sanity check!");
        Assert.isTrue(Objects.equals(getGroup(), group), "Sanity check!");
        return new FilterAndFacetCondition(
                ctx,
                getGroup(),
                serializedEntityType,
                ArrayUtils.mergeArrays(getFacetIds(), facetIds)
        );
    }
}
