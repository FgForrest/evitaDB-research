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

import io.evitadb.api.query.order.ReferenceAttribute;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.query.translate.order.OrderTranslatingContext;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link OrderConstraintTranslator} which converts constraint {@link ReferenceAttribute} to generic {@link SqlSortExpression}
 *
 * @author Jiří Bönsch, 2021
 * @author Lukáš Hornych 2021
 */
public class ReferenceAttributeTranslator implements OrderConstraintTranslator<ReferenceAttribute>{

    @Nonnull
    @Override
    public SqlSortExpression translate(@Nonnull ReferenceAttribute constraint, @Nonnull OrderTranslatingContext ctx) {
        final List<SqlSortExpression> innerExpressions = ctx.getCurrentLevelExpressions();

        final StringBuilder metadataSqlBuilder = new StringBuilder();
        final StringBuilder orderSqlBuilder = new StringBuilder();
        final List<Object> args = new LinkedList<>();

        final Iterator<SqlSortExpression> expressionIterator = innerExpressions.iterator();
        while (expressionIterator.hasNext()) {
            final SqlSortExpression expression = expressionIterator.next();
            metadataSqlBuilder.append(expression.getJoinSql());
            orderSqlBuilder.append(expression.getSql());
            args.addAll(expression.getArgs());

            if (expressionIterator.hasNext()) {
                metadataSqlBuilder.append(" ");
                orderSqlBuilder.append(", ");
            }
        }

        return new SqlSortExpression(metadataSqlBuilder, orderSqlBuilder, args);
    }
}