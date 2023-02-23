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

package io.evitadb.storage.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents single generic SQL sort expression (i.e. a > 5) translated from certain {@link io.evitadb.api.query.OrderConstraint}
 * for SQL order by clause.
 *
 * @author Lukáš Hornych 2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SqlSortExpression extends SqlPart {

    public static final SqlSortExpression EMPTY = new SqlSortExpression();

    /**
     * SQL of join clause used to join attributes to sort by
     */
    private final StringBuilder joinSql;

    /**
     * Creates empty sort expression
     */
    private SqlSortExpression() {
        super();
        this.joinSql = new StringBuilder();
    }

    /**
     * If possible use {@link #SqlSortExpression(StringBuilder, StringBuilder, List)} instead.
     */
    public SqlSortExpression(@Nonnull String joinSql, @Nonnull String sql, @Nonnull List<Object> args) {
        this(new StringBuilder(joinSql), new StringBuilder(sql), args);
    }

    public SqlSortExpression(@Nonnull StringBuilder joinSqlBuilder,
                             @Nonnull StringBuilder sqlBuilder,
                             @Nonnull List<Object> args) {
        super(sqlBuilder, args);
        this.joinSql = joinSqlBuilder;
    }
}
