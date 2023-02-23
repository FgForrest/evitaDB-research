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

package io.evitadb.storage.query.translate.filter.translator.facet;

import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.UserFilter;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.builder.facet.*;
import io.evitadb.storage.query.translate.filter.FilterConstraintTranslatingVisitor;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.sql.FacetToGroupMappingExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.evitadb.storage.query.builder.facet.FacetSqlQueryBuilder.*;

/**
 * Translates {@link UserFilter} constraint to generic {@link SqlPart}. Also handles translation of {@link Facet}
 * constraints inside user filter as facet are needed to be handled in bulk due to their complex relations.
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch 2021
 * @author Tomáš Pozler 2021
 */
public class UserFilterTranslator implements FilterConstraintTranslator<UserFilter> {

    @Nullable
    @Override
    public SqlPart translate(@Nonnull UserFilter constraint, @Nonnull FilterTranslatingContext ctx) {
        // ignore user filter subtree if only baseline part of query is desired
        if (ctx.getMode() == FilterConstraintTranslatingVisitor.Mode.BASELINE) {
            return null;
        }

        // gather inner conditions that are not facets
        final List<SqlPart> innerConditions = ctx.getCurrentLevelConditions();

        // gather facet constraints that are direct children of user filter
        final List<Facet> facetConstraints = Arrays.stream(constraint.getConstraints())
                .filter(c -> Facet.class.isAssignableFrom(c.getClass()))
                .map(Facet.class::cast)
                .collect(Collectors.toList());

        if (innerConditions.isEmpty() && facetConstraints.isEmpty()) {
            return null;
        }

        final Function<FacetComputationContext, SqlPart> executable = context -> {
            // translate facets
            final Map<GroupReference, FacetCollection> groupedUserFacets = mapUserFacetsToGroups(ctx, facetConstraints);
            if (groupedUserFacets != null) {
                // translate facet mappings to actual sql conditions
                final List<FacetCondition> facetConditions = translateFacetGroupMappingsToSql(ctx, groupedUserFacets);
                // add translated facets to other inner constrains
                innerConditions.add(combineFacetConditions(facetConditions));
            }

            // join all inner constraints
            final List<Object> args = new LinkedList<>();
            final StringBuilder sqlBuilder = new StringBuilder("(");

            final Iterator<SqlPart> innerSqlIterator = innerConditions.iterator();
            while (innerSqlIterator.hasNext()) {
                final SqlPart sqlPart = innerSqlIterator.next();
                sqlBuilder.append(sqlPart.getSql());
                args.addAll(sqlPart.getArgs());

                if (innerSqlIterator.hasNext()) {
                    sqlBuilder.append(" and ");
                }
            }
            sqlBuilder.append(")");

            return new SqlPart(sqlBuilder, args);
        };
        return FacetComputationContextHolder.executeWithinContext(
                ctx.getCollectionCtx(),
                ctx.getFacetGroupsConjunction(),
                ctx.getFacetGroupsDisjunction(),
                ctx.getFacetGroupsNegation(),
                null,
                executable
        );
    }

    /**
     * Creates user passed facet -> group mappings
     */
    private Map<GroupReference, FacetCollection> mapUserFacetsToGroups(@Nonnull FilterTranslatingContext ctx,
                                                                       @Nonnull List<Facet> facetConstraints) {
        final SqlPart findFacetToGroupMappingsSqlCondition = buildFindFacetToGroupMappingQuery(ctx, facetConstraints);
        return ctx.getCollectionCtx().getCatalogCtx()
                .getJdbcTemplate()
                .query(
                        findFacetToGroupMappingsSqlCondition.getSql().toString(),
                        new FacetToGroupMappingExtractor(ctx),
                        findFacetToGroupMappingsSqlCondition.getArgs().toArray()
                );
    }

    /**
     * Translates facet -> group mappings to correct sql conditions
     */
    private List<FacetCondition> translateFacetGroupMappingsToSql(FilterTranslatingContext ctx,
                                                                  Map<GroupReference, FacetCollection> groupedUserFacets) {
        return groupedUserFacets.entrySet().stream()
                .map(groupToFacetIdsMapping -> {
                    final int[] facetIds = groupToFacetIdsMapping.getValue().getIds().stream().mapToInt(it -> it).toArray();
                    return buildFilterFacetConditionByGroupRelation(
                            ctx,
                            groupToFacetIdsMapping.getKey(),
                            groupToFacetIdsMapping.getValue().getSerializedEntityType(),
                            facetIds
                    );
                })
                .collect(Collectors.toList());
    }
}
