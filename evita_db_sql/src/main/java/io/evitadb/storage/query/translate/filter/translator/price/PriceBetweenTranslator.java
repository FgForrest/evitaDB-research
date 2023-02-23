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

package io.evitadb.storage.query.translate.filter.translator.price;

import io.evitadb.api.query.filter.PriceBetween;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.PriceFilterConstraintsTranslatingVisitor;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts {@link PriceBetween} constraint to generic
 * {@link SqlPart}.
 *
 * It expects generated sql by {@link PriceFilterConstraintsTranslatingVisitor} to be present in final sql query, thus
 * the used alias {@code price}.
 *
 * @author Lukáš Hornych 2021
 */
public class PriceBetweenTranslator implements FilterConstraintTranslator<PriceBetween> {

    @Override
    public SqlPart translate(@Nonnull PriceBetween constraint, @Nonnull FilterTranslatingContext ctx) {
        // "prices" is used without prefix because in case of using temp table the "prices" column is placed directly
        // in temp table but in normal query the "prices" column is placed in join table with prefix "price"
        return new SqlPart(
                "numrange(?, ?, '[]') @> any (prices)",
                List.of(constraint.getFrom(), constraint.getTo())
        );
    }
}
