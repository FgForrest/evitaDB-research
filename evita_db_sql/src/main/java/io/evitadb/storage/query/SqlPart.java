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

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents single generic SQL part (i.e. a > 5) translated from certain {@link io.evitadb.api.query.FilterConstraint}
 * for SQL where clause.
 *
 * @author Lukáš Hornych 2021
 */
@Data
public class SqlPart {

    public static final SqlPart TRUE = new SqlPart("true");
    public static final SqlPart FALSE = new SqlPart("false");
    public static final SqlPart EMPTY = new SqlPart();

    /**
     * SQL of condition
     */
    private final StringBuilder sql;
    /**
     * JDBC args used in {@link #sql}
     */
    private final List<Object> args;

    protected SqlPart() {
        this("");
    }

    /**
     * If possible use {@link #SqlPart(StringBuilder)}.
     */
    public SqlPart(@Nonnull String sql) {
        this(sql, List.of());
    }

    /**
     * If possible use {@link #SqlPart(StringBuilder, List)}.
     */
    public SqlPart(@Nonnull String sql, @Nonnull List<Object> args) {
        this(new StringBuilder(sql), args);
    }

    public SqlPart(@Nonnull StringBuilder sqlBuilder) {
        this(sqlBuilder, List.of());
    }

    public SqlPart(@Nonnull StringBuilder sqlBuilder, @Nonnull List<Object> args) {
        this.sql = sqlBuilder;
        this.args = args;
    }

    public boolean isEmpty() {
        return sql.length() == 0;
    }
}
