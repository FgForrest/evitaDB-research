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

/**
 * Contains SQL with clause ready to add to query. It contains with clause as well as auto joins for ctes inside
 * the with clause.
 *
 * @author Lukáš Hornych 2021
 */
@Data
public class SqlWithClause {

    public static final SqlWithClause EMPTY = new SqlWithClause(SqlPart.EMPTY, new StringBuilder());

    /**
     * With clause
     */
    private final SqlPart with;
    /**
     * Auto joins of selected ctes
     */
    private final StringBuilder withJoins;

    public boolean isEmpty() {
        return with.getSql().length() == 0;
    }
}
