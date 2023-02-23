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

import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.storage.ComplexEntityQueryContextHolder;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlWithClause;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Sql builder to create temp table with cached entities
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TempTableBuilder {

    public static SqlPart buildCreateTempTableSql(@Nonnull EntityCollectionContext ctx,
                                                  @Nonnull SqlEvitaRequest request,
                                                  @Nonnull String serializedEntityType,
                                                  @Nonnull SqlWithClause withClause,
                                                  @Nonnull SqlPart prices,
                                                  @Nonnull SqlPart baseline) {
        final String tableName = ComplexEntityQueryContextHolder.getContext().getTempTableName();

        final StringBuilder sqlBuilder = new StringBuilder();
        if (ctx.getCatalogCtx().isInDebugMode()) {
            sqlBuilder
                .append("create table ");
        } else {
            sqlBuilder
                .append("create temporary table ");
        }
        sqlBuilder
                .append(tableName)
                .append(" as (")
                .append(withClause.getWith().getSql())
                .append("   select ").append(buildColumnList(request))
                .append("   from t_entity as entity ").append(withClause.getWithJoins()).append("   ").append(prices.getSql());
        if (request.isRequiresFacetSummary()) {
            sqlBuilder
                .append("   left join ").append(ctx.getUid()).append(".t_facetIndex facet on entity.entity_id = facet.entity_id ");
        }
        sqlBuilder
                .append("   where entity.dropped = false and entity.type = ? and ").append(baseline.getSql())
                .append(");");
        if (request.isRequiresFacetSummary()) {
            sqlBuilder
                .append("create index ix_").append(tableName).append("_facets on ").append(tableName).append(" using gin (facets jsonb_path_ops);");
        }
        if (request.isRequiresHierarchyStatistics()) {
            sqlBuilder
                .append("create index ix_").append(tableName).append("_forHierarchyStatistics on ").append(tableName).append(" (entity_id, (primaryKey::varchar(32)));" +
                        "analyze ").append(tableName).append(";");
        }

        final List<Object> args = new LinkedList<>();
        args.addAll(withClause.getWith().getArgs());
        args.addAll(prices.getArgs());
        args.add(serializedEntityType);
        args.addAll(baseline.getArgs());

        return new SqlPart(sqlBuilder, args);
    }

    private static String buildColumnList(@Nonnull SqlEvitaRequest request) {
        return new EntityColumnListBuilder("entity")
                .serializedEntity(request.isRequiresEntityBody())
                .entityId()
                .primaryKey()
                .type(request.isRequiresParents() || request.isRequiresHierarchyStatistics())
                .parentPrimaryKey(request.isRequiresHierarchyStatistics())
                .hierarchyPlacement(request.isRequiresParents() || request.isRequiresHierarchyStatistics())
                .prices(request.isRequiresPrices())
                .facets(request.isRequiresFacetSummary())
                .build();
    }
}
