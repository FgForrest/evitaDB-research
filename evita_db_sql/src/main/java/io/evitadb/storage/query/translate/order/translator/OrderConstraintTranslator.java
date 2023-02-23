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

package io.evitadb.storage.query.translate.order.translator;

import io.evitadb.api.query.OrderConstraint;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.query.translate.order.OrderTranslatingContext;

import javax.annotation.Nonnull;

/**
 * Interface for translating order constraints to SQL sort expressions.
 *
 * @param <CONSTRAINT> translating constraint
 * @author Lukáš Hornych 2021
 */
public interface OrderConstraintTranslator<CONSTRAINT extends OrderConstraint> {

    /**
     * Translates {@link OrderConstraint} to corresponding generic {@link SqlSortExpression}.
     *
     * @param constraint constraint to translate
     * @param ctx translating context
     * @return translated constraint
     */
    @Nonnull
    SqlSortExpression translate(@Nonnull CONSTRAINT constraint, @Nonnull OrderTranslatingContext ctx);
}
