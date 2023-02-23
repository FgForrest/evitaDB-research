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

import io.evitadb.api.query.filter.Facet;
import io.evitadb.storage.ComplexEntityQueryContextHolder;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.builder.facet.filter.FilterAndFacetCondition;
import io.evitadb.storage.query.builder.facet.filter.FilterNotFacetCondition;
import io.evitadb.storage.query.builder.facet.filter.FilterOrFacetCondition;
import io.evitadb.storage.query.builder.facet.summary.SummaryAndFacetCondition;
import io.evitadb.storage.query.builder.facet.summary.SummaryNotFacetCondition;
import io.evitadb.storage.query.builder.facet.summary.SummaryOrFacetCondition;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Sql query builder for fetching whole facet summary and statistics
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FacetSqlQueryBuilder {

    public static SqlPart buildFindAllPossibleFacetsQuery(@Nonnull String collectionUid) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("select reference.entityType as referenceEntityType, " +
                        "   reference.entityTypeDataType as referenceEntityTypeDataType, " +
                        "   reference.groupType as referenceGroupType, " +
                        "   reference.groupTypeDataType as referenceGroupTypeDataType, " +
                        "   reference.groupPrimaryKey as referenceGroupPrimaryKey, " +
                        "   reference.entityPrimaryKey as referenceEntityPrimaryKey, " +
                        "   count(reference.entityPrimaryKey) as referenceFacetCount " +
                        "from ").append(ComplexEntityQueryContextHolder.getContext().getTempTableName()).append(" as entity ")
                .append(" inner join ").append(collectionUid).append(".t_referenceIndex reference" +
                        "   on entity.entity_id = reference.entity_id" +
                        "       and reference.faceted = true " + "")
                .append(" group by reference.entityType, reference.entityTypeDataType, reference.groupType, reference.groupTypeDataType, reference.groupPrimaryKey, reference.entityPrimaryKey " +
                        "order by reference.entityType, reference.entityTypeDataType, reference.groupType, reference.groupTypeDataType, reference.groupPrimaryKey, reference.entityPrimaryKey");

        return new SqlPart(sqlBuilder);
    }

    /**
     * Builds query for finding groups for user passed facets
     */
    public static SqlPart buildFindFacetToGroupMappingQuery(@Nonnull FilterTranslatingContext ctx,
                                                            @Nonnull List<Facet> facetConstraints) {
        final List<Object> args = new LinkedList<>();

        final StringBuilder facetConditionsSqlBuilder = new StringBuilder(SqlPart.FALSE.getSql());
        facetConstraints.forEach(fc -> {
            args.add(ctx.getStringTypedValueSerializer()
                    .serialize(fc.getEntityType())
                    .getSerializedValue());
            args.add(fc.getFacetIds());

            facetConditionsSqlBuilder.append(" or (entityType = ? and entityPrimaryKey = any (?))");
        });

        final StringBuilder sqlBuilder = new StringBuilder()
                .append("select distinct on (groupType, groupPrimaryKey, entityType, entityPrimaryKey) groupType, groupTypeDataType, groupPrimaryKey, entityType, entityTypeDataType, entityPrimaryKey " +
                        "from ").append(ctx.getCollectionUid()).append(".t_referenceIndex " +
                        "where faceted = true" +
                        "   and (").append(facetConditionsSqlBuilder).append(") ")
                .append("order by groupType, groupPrimaryKey, entityType, entityPrimaryKey");


        return new SqlPart(sqlBuilder, args);
    }

    @Nonnull
    public static FacetCondition buildSummaryFacetConditionByGroupRelation(@Nonnull GroupReference group,
                                                                           @Nonnull String serializedEntityType,
                                                                           int... facetIds) {
        final FacetComputationContext context = getComputationContext();
        if (context.getConjugatedGroups().contains(group)) {
            return new SummaryAndFacetCondition(group, serializedEntityType, facetIds);
        } else if (context.getNegatedGroups().contains(group)) {
            return new SummaryNotFacetCondition(group, serializedEntityType, facetIds);
        } else {
            return new SummaryOrFacetCondition(group, serializedEntityType, facetIds);
        }
    }

    @Nonnull
    public static FacetCondition buildFilterFacetConditionByGroupRelation(@Nonnull FilterTranslatingContext filterTranslatingCtx,
                                                                          @Nonnull GroupReference group,
                                                                          @Nonnull String serializedEntityType,
                                                                          int... facetIds) {
        final FacetComputationContext context = FacetComputationContextHolder.getContext();
        if (context.getConjugatedGroups().contains(group)) {
            return new FilterAndFacetCondition(filterTranslatingCtx, group, serializedEntityType, facetIds);
        } else if (context.getNegatedGroups().contains(group)) {
            return new FilterNotFacetCondition(filterTranslatingCtx, group, serializedEntityType, facetIds);
        } else {
            return new FilterOrFacetCondition(filterTranslatingCtx, group, serializedEntityType, facetIds);
        }
    }

    @Nonnull
    public static SqlPart combineFacetConditions(List<FacetCondition> facetConditions) {
        final FacetComputationContext context = getComputationContext();

        final Optional<SqlPart> conjugatedConditions = facetConditions
                .stream()
                .filter(it -> !context.getDisjugatedGroups().contains(it.getGroup()))
                .map(Supplier::get)
                .reduce((sqlPart1, sqlPart2) -> {
                    final List<Object> args = new LinkedList<>();
                    args.addAll(sqlPart1.getArgs());
                    args.addAll(sqlPart2.getArgs());

                    final StringBuilder sqlBuilder = new StringBuilder()
                            .append("(")
                            .append(sqlPart1.getSql())
                            .append(") and (")
                            .append(sqlPart2.getSql())
                            .append(")");

                    return new SqlPart(sqlBuilder, args);
                });
        final Optional<SqlPart> disjugatedConditions = facetConditions
                .stream()
                .filter(it -> context.getDisjugatedGroups().contains(it.getGroup()))
                .map(Supplier::get)
                .reduce((sqlPart1, sqlPart2) -> {
                    final List<Object> args = new LinkedList<>();
                    args.addAll(sqlPart1.getArgs());
                    args.addAll(sqlPart2.getArgs());

                    final StringBuilder sqlBuilder = new StringBuilder()
                            .append("(")
                            .append(sqlPart1.getSql())
                            .append(") or (")
                            .append(sqlPart2.getSql())
                            .append(")");

                    return new SqlPart(sqlBuilder, args);
                });

        SqlPart resultCondition = null;
        if (conjugatedConditions.isPresent()) {
            resultCondition = conjugatedConditions.get();
        }
        if (disjugatedConditions.isPresent()) {
            if (resultCondition == null) {
                resultCondition = disjugatedConditions.get();
            } else {
                final List<Object> args = new LinkedList<>();
                args.addAll(resultCondition.getArgs());
                args.addAll(disjugatedConditions.get().getArgs());

                final StringBuilder sqlBuilder = new StringBuilder()
                        .append("(")
                        .append(resultCondition.getSql())
                        .append(") or (")
                        .append(disjugatedConditions.get().getSql())
                        .append(")");

                resultCondition = new SqlPart(sqlBuilder, args);
            }
        }

        if (resultCondition != null) {
            return resultCondition;
        }
        return SqlPart.TRUE;
    }

    @Nonnull
    public static SqlPart createTestFacetCondition(GroupReference group, FacetReference facet) {
        final List<FacetCondition> facetConditions = FacetComputationContextHolder.getContext().getFacetConditions();

        // create brand new predicate
        final Predicate<FacetCondition> matchTypeAndGroup = it -> Objects.equals(facet.getSerializedEntityType(), it.getSerializedEntityType()) &&
                Objects.equals(group, it.getGroup());

        final List<FacetCondition> combinedConditions = Stream.concat(
                // use all previous facet conditions that doesn't match this facet type and group
                facetConditions
                        .stream()
                        .filter(matchTypeAndGroup.negate()),
                // alter existing facet condition by adding new OR facet id or create new facet condition for current facet
                Stream.of(
                        facetConditions
                                .stream()
                                .filter(matchTypeAndGroup)
                                .findFirst()
                                .map(it -> it.combine(group, facet.getSerializedEntityType(), facet.getFacetId()))
                                .orElseGet(() -> buildSummaryFacetConditionByGroupRelation(group, facet.getSerializedEntityType(), facet.getFacetId()))
                )
        ).collect(toList());
        // now create AND condition upon it
        return combineFacetConditions(combinedConditions);
    }

    private static FacetComputationContext getComputationContext() {
        return FacetComputationContextHolder.getContext();
    }
}
