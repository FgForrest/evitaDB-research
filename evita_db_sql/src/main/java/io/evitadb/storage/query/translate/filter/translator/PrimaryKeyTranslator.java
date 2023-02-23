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

package io.evitadb.storage.query.translate.filter.translator;

import io.evitadb.api.query.filter.PrimaryKey;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link PrimaryKey} to generic {@link SqlPart}
 *
 * @author Lukáš Hornych 2021
 */
public class PrimaryKeyTranslator implements FilterConstraintTranslator<PrimaryKey> {

    @Override
    public SqlPart translate(@Nonnull PrimaryKey constraint, @Nonnull FilterTranslatingContext ctx) {
        final String sql;
        if (ctx.getReferenceSchema() == null) {
            sql = "entity.primaryKey = any (?)";
        } else {
            sql = "reference.entityPrimaryKey = any (?)";
        }

        return new SqlPart(sql, List.of(constraint.getPrimaryKeys()));
    }
}
