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

import io.evitadb.api.query.filter.DirectRelation;
import io.evitadb.api.query.filter.Excluding;
import io.evitadb.api.query.filter.ExcludingRoot;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.storage.ComplexEntityQueryContext;
import io.evitadb.storage.ComplexEntityQueryContextHolder;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.sql.HierarchyPlacement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link WithinHierarchy} to generic {@link SqlPart}.
 * Currently, it also handles conversion of inner constraints ({@link DirectRelation}, {@link Excluding} and {@link ExcludingRoot}).
 *
 * @author Jiří Bonsch
 * @author Tomáš Pozler
 * @author Lukáš Hornych 2021
 */
public class WithinHierarchyTranslator extends HierarchyFilterConstraintTranslator implements FilterConstraintTranslator<WithinHierarchy> {

    @Override
    public SqlPart translate(@Nonnull WithinHierarchy constraint, @Nonnull FilterTranslatingContext ctx) {
        if(constraint.isDirectRelation() && constraint.isExcludingRoot()){
            throw new IllegalStateException("Within hierarchy cannot have both ExcludingRoot and DirectRelation constraints.");
        }
        final String collectionEntityType = ctx.getStringTypedValueSerializer().serialize(ctx.getEntitySchema().getName()).getSerializedValue();

        boolean referenced = false;
        final String hierarchyEntityType;
        if (constraint.getEntityType() == null) {
            hierarchyEntityType = collectionEntityType;
        } else {
            hierarchyEntityType = ctx.getStringTypedValueSerializer().serialize(constraint.getEntityType()).getSerializedValue();
            if (!hierarchyEntityType.equals(collectionEntityType)) {
                referenced = true;
            }
        }

        // obtain root placement
        final HierarchyPlacement rootPlacement = findRootPlacement(ctx, hierarchyEntityType, constraint.getParentId());
        if (ComplexEntityQueryContextHolder.hasContext()) {
            final ComplexEntityQueryContext complexCtx = ComplexEntityQueryContextHolder.getContext();
            complexCtx.setRootPlacement(rootPlacement);
        }

        // build main query
        final SqlPart sqlPart;
        if (constraint.isDirectRelation()) {
            sqlPart = buildDirectRelationSqlPart(
                    ctx,
                    hierarchyEntityType,
                    rootPlacement,
                    constraint.getExcludedChildrenIds(),
                    referenced
            );
        } else {
            sqlPart = buildWithinHierarchySqlPart(
                    ctx,
                    hierarchyEntityType,
                    rootPlacement,
                    constraint.isExcludingRoot(),
                    constraint.getExcludedChildrenIds(),
                    referenced
            );
        }

        return sqlPart;
    }

    /**
     * Creates sql which finds all entities (PKs) that are within hierarchy of {@code rootId}.
     *
     * @param ctx translating context
     * @param hierarchyEntityType entity type of target hierarchy to search within
     * @param rootPlacement placement of queried root hierarchy entity
     * @param excludingRoot flag, whether root node should be excluded from results
     * @param excludedChildrenIds ids of excluded subtrees
     * @param referenced inner flag stating if we work with reference or not
     * @return sql part
     */
    private SqlPart buildWithinHierarchySqlPart(@Nonnull FilterTranslatingContext ctx,
                                                @Nonnull String hierarchyEntityType,
                                                @Nonnull HierarchyPlacement rootPlacement,
                                                boolean excludingRoot,
                                                @Nonnull int[] excludedChildrenIds,
                                                boolean referenced) {
        final SqlPart placementMatchSqlPart = buildPlacementMatchSqlPart(
                ctx,
                referenced ? hierarchyEntityType : null,
                rootPlacement,
                excludingRoot ? ">" : ">=",
                excludingRoot ? "<" : "<=",
                false
        );
        return buildFinalSqlPart(ctx, placementMatchSqlPart, hierarchyEntityType, rootPlacement, referenced, excludedChildrenIds);
    }

    /**
     * Creates sql which finds all entities (PKs) that are directly related to specified root
     *
     * @param ctx translating context
     * @param hierarchyEntityType entity type of target hierarchy to search within
     * @param rootPlacement placement of queried root hierarchy entity
     * @param excludedChildrenIds ids of excluded subtrees
     * @param referenced inner flag stating if we work with reference or not
     * @return sql part
     */
    private SqlPart buildDirectRelationSqlPart(@Nonnull FilterTranslatingContext ctx,
                                               @Nonnull String hierarchyEntityType,
                                               @Nonnull HierarchyPlacement rootPlacement,
                                               @Nonnull int[] excludedChildrenIds,
                                               boolean referenced) {
        final SqlPart placementMatchSqlPart;
        if (referenced) {
            placementMatchSqlPart = buildPlacementMatchSqlPart(ctx, hierarchyEntityType, rootPlacement, ">=", "<=", true);
        } else {
            placementMatchSqlPart = buildPlacementMatchSqlPart(ctx, null, rootPlacement, ">", "<", true);
        }
        return buildFinalSqlPart(ctx, placementMatchSqlPart, hierarchyEntityType, rootPlacement, referenced, excludedChildrenIds);
    }

    /**
     * Builds SQL condition to check hierarchy placements against root bounds
     *
     * @param referencedHierarchyEntityType entity type of referenced hierarchy, if searching in own hierarchy then it has to be null
     * @param rootPlacement placement of queried root hierarchy entity
     * @param leftBoundOperator operator for comparing left bounds
     * @param rightBoundOperator operator for comparing right bounds
     * @param directRelation if it should check only directly related entities
     */
    private SqlPart buildPlacementMatchSqlPart(@Nonnull FilterTranslatingContext ctx,
                                               @Nullable String referencedHierarchyEntityType,
                                               @Nonnull HierarchyPlacement rootPlacement,
                                               @Nonnull String leftBoundOperator,
                                               @Nonnull String rightBoundOperator,
                                               boolean directRelation) {
        if (referencedHierarchyEntityType == null) {
            return buildPlacementMatchSqlForSelfHierarchy(rootPlacement, leftBoundOperator, rightBoundOperator, directRelation);
        } else {
            return buildPlacementMatchSqlForReferencedHierarchy(ctx, referencedHierarchyEntityType, rootPlacement, leftBoundOperator, rightBoundOperator, directRelation);
        }
    }

    private SqlPart buildPlacementMatchSqlForSelfHierarchy(@Nonnull HierarchyPlacement rootPlacement,
                                                           @Nonnull String leftBoundOperator,
                                                           @Nonnull String rightBoundOperator,
                                                           boolean directRelation) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("(" +
                        "   leftBound ").append(leftBoundOperator).append(" ?")
                .append("   and rightBound ").append(rightBoundOperator).append(" ?");
        if (directRelation) {
            sqlBuilder
                .append("   and level = ?");
        }
        sqlBuilder
                .append(")");

        final List<Object> args = new LinkedList<>();
        args.add(rootPlacement.getLeftBound());
        args.add(rootPlacement.getRightBound());
        if (directRelation) {
            args.add(rootPlacement.getLevel() + 1);
        }

        return new SqlPart(sqlBuilder, args);
    }

    private SqlPart buildPlacementMatchSqlForReferencedHierarchy(@Nonnull FilterTranslatingContext ctx,
                                                                 @Nonnull String referencedHierarchyEntityType,
                                                                 @Nonnull HierarchyPlacement rootPlacement,
                                                                 @Nonnull String leftBoundOperator,
                                                                 @Nonnull String rightBoundOperator,
                                                                 boolean directRelation) {
        final StringBuilder foundEntitiesSqlBuilder = new StringBuilder()
                .append("select entity_id " +
                        "from t_entityHierarchyPlacement " +
                        "where type = ?")
                .append("   and leftBound ").append(leftBoundOperator).append(" ?" +
                        "   and rightBound ").append(rightBoundOperator).append(" ?");
        if (directRelation) {
            foundEntitiesSqlBuilder
                .append("   and level = ?");
        }

        final List<Object> foundEntitiesArgs = new LinkedList<>();
        foundEntitiesArgs.add(referencedHierarchyEntityType);
        foundEntitiesArgs.add(rootPlacement.getLeftBound());
        foundEntitiesArgs.add(rootPlacement.getRightBound());
        if (directRelation) {
            foundEntitiesArgs.add(rootPlacement.getLevel());
        }

        final String foundEntitiesCteAlias = ctx.addWithCte(new SqlPart(foundEntitiesSqlBuilder, foundEntitiesArgs), false);
        return new SqlPart("entity.entity_id = any (select entity_id from " + foundEntitiesCteAlias + ")");
    }
}
