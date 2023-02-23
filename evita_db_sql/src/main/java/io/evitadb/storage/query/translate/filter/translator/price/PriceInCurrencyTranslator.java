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
import io.evitadb.api.query.filter.PriceInCurrency;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterConstraintTranslatingVisitor;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.PriceFilterConstraintsTranslatingVisitor;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;

import javax.annotation.Nonnull;

/**
 * Implementation of {@link FilterConstraintTranslator} for {@link PriceInCurrency} constraint.
 *
 * This implementation actually does nothing because price in currency logic is currently handled separately of
 * {@link FilterConstraintTranslatingVisitor} by {@link PriceFilterConstraintsTranslatingVisitor} with other price filter
 * constraints (besides {@link PriceBetween}).
 *
 * However filter visitor still needs to know about some translator for {@link PriceInCurrency} so this implementation
 * acts as placeholder.
 *
 * @author Lukáš Hornych 2021
 */
public class PriceInCurrencyTranslator implements FilterConstraintTranslator<PriceInCurrency> {

    @Override
    public SqlPart translate(@Nonnull PriceInCurrency constraint, @Nonnull FilterTranslatingContext ctx) {
        // should not generate any standalone sql to actual query because PriceFilterConstraintsTranslatingVisitor
        // handles the sql
        return null;
    }
}
