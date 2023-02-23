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

package io.evitadb.storage.query.translate.filter.translator.attribute;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Ancestor for translators of container constraints combining multiple attribute filter constraints into one.
 *
 * @param <CONSTRAINT> container constraint
 * @author Lukáš Hornych 2022
 */
public abstract class MultipleAttributeFilterConstraintsContainerTranslator<CONSTRAINT extends FilterConstraint> extends AttributeFilterConstraintTranslator<CONSTRAINT> {

    @Override
    public SqlPart translate(@Nonnull CONSTRAINT constraint, @Nonnull FilterTranslatingContext ctx) {
        final List<SqlPart> innerConditions = ctx.getCurrentLevelConditions();
        if (innerConditions.isEmpty()) {
            return null;
        }

        final List<Object> args = new LinkedList<>();
        final StringBuilder sqlBuilder = new StringBuilder("(");

        final Iterator<SqlPart> innerSqlIterator = innerConditions.iterator();
        while (innerSqlIterator.hasNext()) {
            final SqlPart sqlPart = innerSqlIterator.next();
            sqlBuilder.append(sqlPart.getSql());
            args.addAll(sqlPart.getArgs());

            if (innerSqlIterator.hasNext()) {
                sqlBuilder.append(" ").append(getConditionsDelimiter()).append(" ");
            }
        }
        sqlBuilder.append(")");

        return new SqlPart(sqlBuilder, args);
    }

    protected abstract String getConditionsDelimiter();
}
