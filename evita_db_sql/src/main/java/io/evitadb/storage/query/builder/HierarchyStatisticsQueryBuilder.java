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
import io.evitadb.storage.query.SqlWithClause;
import io.evitadb.storage.serialization.sql.HierarchyPlacement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Sql query builder for fetching {@link io.evitadb.api.io.extraResult.HierarchyStatistics} extra result.
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HierarchyStatisticsQueryBuilder {

    private static final String HIERARCHY_STATISTICS_TREE_LANGUAGE_CONSTRAINT = "? = any (entity.locales)";

    /**
     * Universal SQL query for fetching complete hierarchy statistics for single referenced collection.
     */
    public static SqlPart buildHierarchyStatisticsQuery(@Nullable Locale language,
                                                        @Nullable SqlWithClause withClause,
                                                        boolean requiresFullEntity,
                                                        @Nonnull String serializedQueriedEntityType,
                                                        @Nonnull String serializedRequiredHierarchyType,
                                                        @Nullable SqlPart treeWhereCondition,
                                                        boolean inSubtree,
                                                        boolean directRelation) {
        final String tempTableName = ComplexEntityQueryContextHolder.getContext().getTempTableName();

        // parents check sql
        final boolean requiresParentsCheck = language != null;

        // main sql
        final StringBuilder sqlBuilder = new StringBuilder()
                .append((withClause != null && !withClause.isEmpty()) ? withClause.getWith().getSql() + "," : "with ")
                // pre-fetch desired tree of hierarchy entities which will serve as main source for statistics
                .append("tree as (")
                .append("   select ").append(buildColumnList(requiresFullEntity)).append(", ")
                // count direct children
                .append("       (" +
                        "           select count(*) ")
                .append("           from ").append(tempTableName).append(" entityInHierarchy")
                .append("           join t_entityHierarchyPlacement hp on hp.entity_id = entityInHierarchy.entity_id");
        if (serializedRequiredHierarchyType.equals(serializedQueriedEntityType)) {
            sqlBuilder
                .append("           where entityInHierarchy.parentPrimaryKey = entity.primaryKey");
        } else {
            sqlBuilder
                .append("           where hp.type = entity.type and hp.primaryKey = entity.primaryKey::varchar(32)");
        }
        sqlBuilder
                .append("       ) as entityCount")
                .append("   from t_entity entity ").append((withClause != null && !withClause.isEmpty()) ? withClause.getWithJoins() : "")
                .append("   where entity.dropped = false " +
                        "       and entity.type = ? ")
                .append("       and ").append((treeWhereCondition != null && !treeWhereCondition.isEmpty()) ? treeWhereCondition.getSql() : SqlPart.TRUE.getSql())
                .append("       and ").append(language != null ? HIERARCHY_STATISTICS_TREE_LANGUAGE_CONSTRAINT : SqlPart.TRUE.getSql())
                .append(        buildParentCheckCondition(requiresParentsCheck, inSubtree))
                .append("   group by entity.entity_id" +
                        "   order by entity.entity_id" +
                        ")" +
                        // fetch existing tree with recursive cardinalities of entities
                        "select tree.*, " +
                        "   tree.level as level," +
                        "   cardinality.cardinality " +
                        "from tree ").append(withClause != null ? withClause.getWithJoins() : "").append(" ")
                .append(// compute recursive cardinalities
                        "join lateral (" +
                        "   select sum(cardinality.entityCount) as cardinality" +
                        "   from tree cardinality" +
                        "   where cardinality.leftBound >= tree.leftBound" +
                        "       and cardinality.rightBound <= tree.rightBound" +
                        ") cardinality on true " +
                        "where ").append(directRelation ? "tree.level = " + (ComplexEntityQueryContextHolder.getContext().getRootPlacement().getLevel() + 1) : SqlPart.TRUE.getSql())
                .append("   and (" +
                        "       cardinality.cardinality > 0 "); // returns only entities with some children
        if (serializedRequiredHierarchyType.equals(serializedQueriedEntityType)) {
            sqlBuilder
                .append("       or exists(" + // or, in case of self hierarchy tree, preserve entities without children which are found by main query
                        "           select 1 " +
                        "           from ").append(tempTableName).append(" entityInHierarchy ")
                .append("           where entityInHierarchy.type = tree.type " +
                        "               and entityInHierarchy.primaryKey = tree.primaryKey" +
                        "       )");
        }
        sqlBuilder
                .append("   )" +
                        "order by tree.leftBound asc");

        final List<Object> args = new LinkedList<>();
        if (withClause != null) {
            args.addAll(withClause.getWith().getArgs());
        }
        // tree args
        args.add(serializedRequiredHierarchyType);
        if (treeWhereCondition != null) {
            args.addAll(treeWhereCondition.getArgs());
        }
        if (language != null) {
            args.add(language.toString());
        }
        // parent args
        if (requiresParentsCheck) {
            args.add(language.toString());
        }

        return new SqlPart(sqlBuilder, args);
    }

    private static StringBuilder buildParentCheckCondition(boolean requiresParentsCheck, boolean inSubtree) {
        final StringBuilder parentCheckSqlBuilder;
        if (!requiresParentsCheck) {
            parentCheckSqlBuilder = new StringBuilder(0);
        } else {
            /*
            It checks if exists any parent of current tree entity that does not have requested language,
            if so tree entity have to be excluded from final tree (to exclude whole subtrees of tree entities that
            would create holes in final tree because of missing entities without requested language).
             */
            parentCheckSqlBuilder = new StringBuilder()
                    .append("and not(exists(" +
                            "   select 1" +
                            "   from t_entity parent" +
                            "   where parent.leftBound <= entity.leftBound" +
                            "       and parent.rightBound >= entity.rightBound" +
                            "       and ? != all (parent.locales)");
            if (inSubtree) {
                final HierarchyPlacement rootPlacement = ComplexEntityQueryContextHolder.getContext().getRootPlacement();
                parentCheckSqlBuilder
                    .append("       and parent.leftBound >= ").append(rootPlacement.getLeftBound())
                    .append("       and parent.rightBound <= ").append(rootPlacement.getRightBound());
            }
            parentCheckSqlBuilder
                    .append("))");
        }
        return parentCheckSqlBuilder;
    }

    private static String buildColumnList(boolean requiresFullEntity) {
        return new EntityColumnListBuilder("entity")
                .serializedEntity(requiresFullEntity)
                .primaryKey()
                .type()
                .hierarchyPlacement()
                .orderAmongSiblings()
                .build();
    }
}
