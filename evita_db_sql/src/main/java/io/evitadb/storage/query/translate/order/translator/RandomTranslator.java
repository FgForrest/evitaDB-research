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

import io.evitadb.api.query.order.Random;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.query.translate.order.OrderTranslatingContext;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation of {@link OrderConstraintTranslator} which converts constraint {@link Random} to generic {@link SqlSortExpression}
 *
 * @author Lukáš Hornych 2021
 */
public class RandomTranslator implements OrderConstraintTranslator<Random> {

    @Nonnull
    @Override
    public SqlSortExpression translate(@Nonnull Random constraint, @Nonnull OrderTranslatingContext ctx) {
        return new SqlSortExpression("", "random()", List.of());
    }
}
