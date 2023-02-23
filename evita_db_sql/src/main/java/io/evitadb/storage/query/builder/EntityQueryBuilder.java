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
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.query.SqlWithClause;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Sql query builder for creating queries to fetch entities (usually in conjunction with translated filter query and so on,
 * it does not translate anything).
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityQueryBuilder {

    public static SqlPart buildFindEntityByPKQuery(@Nonnull String serializedEntityType,
                                                   int primaryKey) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("select ").append(new EntityColumnListBuilder("entity").serializedEntity().build())
                .append(" from t_entity entity " +
                        "where dropped = false and type = ? and primaryKey = ?");

        final List<Object> args = new LinkedList<>();
        args.add(serializedEntityType);
        args.add(primaryKey);

        return new SqlPart(sqlBuilder, args);
    }

    public static SqlPart buildFindEntitiesByQueryDirectly(@Nonnull String serializedEntityType,
                                                           @Nonnull SqlEvitaRequest request,
                                                           @Nonnull SqlWithClause with,
                                                           @Nonnull SqlPart where,
                                                           @Nonnull SqlPart prices,
                                                           @Nonnull SqlSortExpression sort,
                                                           int firstRecordOffset,
                                                           int limit) {
        final boolean hasOrdering = !sort.isEmpty();

        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(with.getWith().getSql()).append(" ")
                .append("select ")
                .append(new EntityColumnListBuilder("sortedFilteredEntity").all().build())
                .append(" from (");
        sqlBuilder
                .append("select ")
                .append(new EntityColumnListBuilder("filteredEntity").all().build())
                .append(", row_number() over (order by ");
        if (hasOrdering) {
            sqlBuilder.append(sort.getSql());
        } else {
            sqlBuilder.append("filteredEntity.primaryKey asc ");
        }
        sqlBuilder
                .append(") as rowNumber")
                .append(" from (")
                .append("select ").append(buildColumnListForDirectFind(request, hasOrdering))
                .append(" from t_entity entity ").append(with.getWithJoins()).append(" ")
                .append(prices.getSql())
                .append(" where entity.dropped = false and entity.type = ? and ")
                .append(where.getSql())
                .append(") filteredEntity ");
        if (hasOrdering) {
            sqlBuilder
                    .append(sort.getJoinSql());
        }

        sqlBuilder.append(") sortedFilteredEntity " +
                "where rowNumber > compute_offset(sortedFilteredEntity.totalRecordCount, ?, ?) and rowNumber <= compute_offset(sortedFilteredEntity.totalRecordCount, ?, ?) + ?");

        final List<Object> args = new LinkedList<>();
        args.addAll(with.getWith().getArgs());
        args.addAll(prices.getArgs());
        args.add(serializedEntityType);
        args.addAll(where.getArgs());
        args.addAll(sort.getArgs());
        args.add(limit);
        args.add(firstRecordOffset);
        args.add(limit);
        args.add(firstRecordOffset);
        args.add(limit);

        return new SqlPart(sqlBuilder, args);
    }

    public static SqlPart buildFindEntitiesByQueryFromTempTable(@Nonnull SqlEvitaRequest request,
                                                                @Nonnull SqlPart where,
                                                                @Nonnull SqlWithClause with,
                                                                @Nonnull SqlSortExpression sort,
                                                                int firstRecordOffset,
                                                                int limit) {
        final boolean hasOrdering = !sort.isEmpty();

        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(with.getWith().getSql()).append(" ")
                .append("select ")
                .append(new EntityColumnListBuilder("sortedFilteredEntity").all().build())
                .append(" from (");
        sqlBuilder
                .append("select ")
                .append(new EntityColumnListBuilder("filteredEntity").all().build())
                .append(", row_number() over (order by ");
        if (hasOrdering) {
            sqlBuilder.append(sort.getSql());
        } else {
            sqlBuilder.append("filteredEntity.primaryKey asc ");
        }

        sqlBuilder
                .append(") as rowNumber")
                .append(" from (")
                .append("select ").append(buildColumnListForTempTableFind(request, hasOrdering))
                .append(" from ").append(ComplexEntityQueryContextHolder.getContext().getTempTableName()).append(" entity ").append(with.getWithJoins()).append(" ")
                .append(" where ").append(where.getSql())
                .append(") filteredEntity ");
        if (hasOrdering) {
            sqlBuilder
                    .append(sort.getJoinSql());
        }

        sqlBuilder.append(") sortedFilteredEntity " +
                "where rowNumber > compute_offset(sortedFilteredEntity.totalRecordCount, ?, ?) and rowNumber <= compute_offset(sortedFilteredEntity.totalRecordCount, ?, ?) + ?");

        final List<Object> args = new LinkedList<>();
        args.addAll(with.getWith().getArgs());
        args.addAll(where.getArgs());
        args.addAll(sort.getArgs());
        args.add(limit);
        args.add(firstRecordOffset);
        args.add(limit);
        args.add(firstRecordOffset);
        args.add(limit);

        return new SqlPart(sqlBuilder, args);
    }

    public static SqlPart buildFindEntitiesByQueryDirectly(@Nonnull String serializedEntityType,
                                                           @Nonnull SqlEvitaRequest request,
                                                           @Nonnull SqlWithClause with,
                                                           @Nonnull SqlPart where,
                                                           @Nonnull SqlPart prices,
                                                           @Nonnull SqlSortExpression sort) {
        final boolean hasOrdering = !sort.isEmpty();

        final StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(with.getWith().getSql()).append(" ");
        if (hasOrdering) {
            sqlBuilder
                    .append("select ")
                    .append(new EntityColumnListBuilder("filteredEntity").all().build())
                    .append(" from (");
        }
        sqlBuilder
                .append("select ").append(buildColumnListForDirectFind(request, hasOrdering))
                .append(" from t_entity entity ").append(with.getWithJoins()).append(" ")
                .append(prices.getSql())
                .append(" where entity.dropped = false and entity.type = ? and ")
                .append(where.getSql());
        if (hasOrdering) {
            sqlBuilder
                    .append(") filteredEntity ")
                    .append(sort.getJoinSql());
        }
        sqlBuilder.append(" order by ");
        if (hasOrdering) {
            sqlBuilder.append(sort.getSql());
        } else {
            sqlBuilder.append("entity.primaryKey asc ");
        }

        final List<Object> args = new LinkedList<>();
        args.addAll(with.getWith().getArgs());
        args.addAll(prices.getArgs());
        args.add(serializedEntityType);
        args.addAll(where.getArgs());
        args.addAll(sort.getArgs());

        return new SqlPart(sqlBuilder, args);
    }

    public static SqlPart buildFindEntitiesByQueryFromTempTable(@Nonnull SqlEvitaRequest request,
                                                                @Nonnull SqlPart where,
                                                                @Nonnull SqlWithClause with,
                                                                @Nonnull SqlSortExpression sort) {
        final boolean hasOrdering = !sort.isEmpty();

        final StringBuilder sqlBuilder = new StringBuilder()
                .append(with.getWith().getSql()).append(" ");
        if (hasOrdering) {
            sqlBuilder
                    .append("select ")
                    .append(new EntityColumnListBuilder("filteredEntity").all().build())
                    .append(" from (");
        }
        sqlBuilder
                .append("select ").append(buildColumnListForTempTableFind(request, hasOrdering))
                .append(" from ").append(ComplexEntityQueryContextHolder.getContext().getTempTableName()).append(" entity ").append(with.getWithJoins()).append(" ")
                .append(" where ").append(where.getSql());
        if (hasOrdering) {
            sqlBuilder
                    .append(") filteredEntity ")
                    .append(sort.getJoinSql());
        }
        sqlBuilder.append(" order by ");
        if (hasOrdering) {
            sqlBuilder.append(sort.getSql());
        } else {
            sqlBuilder.append("entity.primaryKey asc ");
        }

        final List<Object> args = new LinkedList<>();
        args.addAll(with.getWith().getArgs());
        args.addAll(where.getArgs());
        args.addAll(sort.getArgs());

        return new SqlPart(sqlBuilder, args);
    }

    private static String buildColumnListForDirectFind(@Nonnull SqlEvitaRequest request, boolean hasOrdering) {
        final boolean needsIds = !request.isRequiresEntityBody() || hasOrdering;
        return new EntityColumnListBuilder("entity")
                .serializedEntity(request.isRequiresEntityBody())
                .entityId(needsIds)
                .primaryKey()
                .prices(request.isRequiresPrices())
                .count()
                .build();
    }

    private static String buildColumnListForTempTableFind(@Nonnull SqlEvitaRequest request, boolean hasOrdering) {
        final boolean needsIds = !request.isRequiresEntityBody() || hasOrdering;
        return new EntityColumnListBuilder("entity")
                .serializedEntity(request.isRequiresEntityBody())
                .entityId(needsIds)
                .primaryKey(needsIds)
                .prices(request.isRequiresPriceOrdering())
                .count()
                .build();
    }
}
