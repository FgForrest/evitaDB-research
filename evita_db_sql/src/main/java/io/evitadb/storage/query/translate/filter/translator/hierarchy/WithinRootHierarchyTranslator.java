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

import io.evitadb.api.query.filter.Excluding;
import io.evitadb.api.query.filter.WithinRootHierarchy;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link WithinRootHierarchy} to generic {@link SqlPart}.
 * Currently, it also handles conversion of inner constraint {@link Excluding}.
 *
 * @author Jiří Bonsch
 * @author Tomáš Pozler
 * @author Lukáš Hornych 2021
 */
public class WithinRootHierarchyTranslator extends HierarchyFilterConstraintTranslator implements FilterConstraintTranslator<WithinRootHierarchy> {

    @Override
    public SqlPart translate(@Nonnull WithinRootHierarchy constraint, @Nonnull FilterTranslatingContext ctx) {
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

        final SqlPart sqlPart;
        if (constraint.isDirectRelation()) {
            sqlPart = buildDirectRelationSqlPart(
                    constraint.getExcludedChildrenIds()
            );
        } else {
            sqlPart = buildWithinRootHierarchySqlPart(
                    ctx,
                    hierarchyEntityType,
                    constraint.getExcludedChildrenIds(),
                    referenced
            );
        }

        return sqlPart;
    }

    /**
     * Creates sql which finds all entities (PKs) that are within root hierarchy.
     *
     * @param ctx translating context
     * @param hierarchyEntityType entity type of target hierarchy to search within
     * @param excludedChildrenIds ids of excluded subtrees
     * @param referenced inner flag stating if we work with reference or not
     * @return sql part
     */
    private SqlPart buildWithinRootHierarchySqlPart(@Nonnull FilterTranslatingContext ctx,
                                                    @Nonnull String hierarchyEntityType,
                                                    @Nonnull int[] excludedChildrenIds,
                                                    boolean referenced) {
        // if not "referenced" we don't need any constraint as we're already searching through desired collection only
        final StringBuilder referencedCategoriesPlacementBuilder = new StringBuilder();
        if (referenced) {
            referencedCategoriesPlacementBuilder
                    .append("exists(" +
                            "   select 1" +
                            "   from t_entityHierarchyPlacement placement" +
                            "   where placement.entity_id = entity.entity_id " +
                            "       and placement.type = ?" +
                            ")");
        }
        final List<Object> args = new LinkedList<>();
        if (referenced) {
            args.add(hierarchyEntityType);
        }

        final SqlPart mainCondition = new SqlPart(referencedCategoriesPlacementBuilder, args);
        return buildFinalSqlPart(ctx, mainCondition, hierarchyEntityType, null, referenced, excludedChildrenIds);
    }

    /**
     * Creates sql which finds all entities (PKs) that are directly related virtual (null) root
     *
     * @param excludedChildrenIds ids of excluded subtrees
     * @return sql part
     */
    private SqlPart buildDirectRelationSqlPart(@Nonnull int[] excludedChildrenIds) {
        final SqlPart excludingSqlPart = buildDirectRelationExcludingSqlPart(excludedChildrenIds);

        final StringBuilder sqlBuilder = new StringBuilder()
                .append(" entity.parentPrimaryKey is null ")
                .append(excludingSqlPart.getSql())
                .append(" ");
        final List<Object> args = new LinkedList<>(excludingSqlPart.getArgs());

        return new SqlPart(sqlBuilder, args);
    }

    /**
     * Creates sql part from passed array of excluded children ids
     */
    private SqlPart buildDirectRelationExcludingSqlPart(@Nonnull int[] excludedChildrenIds) {
        if (excludedChildrenIds.length == 0) {
            return SqlPart.EMPTY;
        }

        return new SqlPart(
                " and entity.primaryKey != all (?) ",
                List.of(excludedChildrenIds)
        );
    }

}
