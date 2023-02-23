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

package io.evitadb.storage.query.translate.filter.translator.hierarchy;

import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.serialization.sql.HierarchyPlacement;
import io.evitadb.storage.serialization.sql.HierarchyPlacementRowMapper;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

import static io.evitadb.storage.serialization.sql.HierarchyPlacement.SELF_TYPE;

/**
 * Ancestor for filter constraint translators translating hierarchy related constraints. It provides common helper methods.
 *
 * @author Tomáš Pozler
 * @author Jiří Bonsch
 */
public abstract class HierarchyFilterConstraintTranslator {

    private static final String EXCLUDED_CHILDREN_CTE_ALIAS = "excludedChildren";

    private static final HierarchyPlacementRowMapper HIERARCHY_PLACEMENT_ROW_MAPPER = new HierarchyPlacementRowMapper();

    /**
     * Tries to find placement of queried root hierarchy entity
     */
    HierarchyPlacement findRootPlacement(FilterTranslatingContext ctx, @Nonnull String hierarchyEntityType, int rootId) {
        final SqlPart prefetchBoundsSqlPart = buildHierarchyEntitiesQuery(hierarchyEntityType, true, rootId);

        final HierarchyPlacement rootPlacement;
        try {
            rootPlacement = ctx.getCollectionCtx().getCatalogCtx().getJdbcTemplate().queryForObject(
                    prefetchBoundsSqlPart.getSql().toString(),
                    HIERARCHY_PLACEMENT_ROW_MAPPER,
                    prefetchBoundsSqlPart.getArgs().toArray()
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("Parent entity with id " + rootId + " does not exist.");
        }
        return rootPlacement;
    }

    /**
     * Adds to main condition excluded children condition if needed.
     *
     * @param ctx translating context
     * @param mainCondition main search condition
     * @param hierarchyEntityType entity type of searched hierarchy
     * @param referenced is hierarchy referenced
     * @param excludedChildrenIds excluded subtrees
     * @return final sql part
     */
    SqlPart buildFinalSqlPart(@Nonnull FilterTranslatingContext ctx,
                              @Nonnull SqlPart mainCondition,
                              @Nonnull String hierarchyEntityType,
                              @Nullable HierarchyPlacement rootPlacement,
                              boolean referenced,
                              @Nonnull int[] excludedChildrenIds) {
        final SqlPart excludedChildrenSqlPart = buildExcludedChildrenSqlPart(ctx, hierarchyEntityType, rootPlacement, referenced, excludedChildrenIds);

        final List<Object> args = new LinkedList<>();
        args.addAll(mainCondition.getArgs());
        args.addAll(excludedChildrenSqlPart.getArgs());

        final StringBuilder sqlBuilder = mainCondition.getSql();
        sqlBuilder
                .append(" ")
                .append(excludedChildrenSqlPart.getSql())
                .append(" ");

        return new SqlPart(sqlBuilder, args);
    }

    /**
     * Creates subquery returning hierarchy entities metadata
     *
     * @param entityType hierarchy of entities
     * @param level fetch level or not
     * @param primaryKeys PKs of entities to fetch
     */
    SqlPart buildHierarchyEntitiesQuery(@Nonnull String entityType,
                                        boolean level,
                                        int... primaryKeys) {
        final StringBuilder subqueryBuilder = new StringBuilder()
                .append("select leftBound, " +
                        "   rightBound ");
        if (level) {
            subqueryBuilder
                .append(", level ");
        }
        subqueryBuilder
                .append("from t_entity " +
                        "where dropped = false and type = ? and primaryKey = any (?)");

        final List<Object> args = new LinkedList<>();
        args.add(entityType);
        args.add(primaryKeys);

        return new SqlPart(subqueryBuilder, args);
    }

    /**
     * Builds sql part which excluded entity if it is in excluded subtree
     *
     * @param ctx translating context
     * @param hierarchyEntityType entity type of search hierarchy
     * @param rootPlacement placement of queried root hierarchy entity
     * @param referenced if hierarchy is referenced
     * @param excludedChildrenIds excluded subtrees
     * @return excluded children sql part
     */
    private SqlPart buildExcludedChildrenSqlPart(@Nonnull FilterTranslatingContext ctx,
                                                 @Nonnull String hierarchyEntityType,
                                                 @Nullable HierarchyPlacement rootPlacement,
                                                 boolean referenced,
                                                 int[] excludedChildrenIds) {
        // exclude entity if is in referenced excluded subtree
        if (excludedChildrenIds.length > 0) {
            // create excluded hierarchy entities cte
            ctx.addWithCte(
                    EXCLUDED_CHILDREN_CTE_ALIAS,
                    buildHierarchyEntitiesQuery(hierarchyEntityType, false, excludedChildrenIds),
                    false
            );

            final StringBuilder sqlBuilder = new StringBuilder();
            if ((rootPlacement != null) || referenced) {
                sqlBuilder.append(" and ");
            }
            sqlBuilder
                    .append("exists(" +
                            "   select 1 " +
                            "   from (" +
                            "      select " + EXCLUDED_CHILDREN_CTE_ALIAS + ".leftBound as excludedLeftBound" +
                            "      from t_entityHierarchyPlacement referencedEntity" +
                            "      left join " + EXCLUDED_CHILDREN_CTE_ALIAS +
                            "          on referencedEntity.leftBound >= " + EXCLUDED_CHILDREN_CTE_ALIAS + ".leftBound and referencedEntity.rightBound <= " + EXCLUDED_CHILDREN_CTE_ALIAS + ".rightBound" +
                            "      where referencedEntity.entity_id = entity.entity_id" +
                            "          and referencedEntity.type = ?");
            if (rootPlacement != null) {
                sqlBuilder
                    .append("          and referencedEntity.leftBound >= ").append(rootPlacement.getLeftBound())
                    .append("          and referencedEntity.rightBound <= ").append(rootPlacement.getRightBound());
            }
            sqlBuilder
                    .append("   ) referencedEntity " +
                            "   where referencedEntity.excludedLeftBound is null" +
                            ")");

            final String placementsEntityType = referenced ? hierarchyEntityType : SELF_TYPE;
            return new SqlPart(sqlBuilder, List.of(placementsEntityType));
        }
        return SqlPart.EMPTY;
    }
}
