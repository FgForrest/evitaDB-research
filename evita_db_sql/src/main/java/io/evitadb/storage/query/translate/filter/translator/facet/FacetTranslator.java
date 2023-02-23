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

package io.evitadb.storage.query.translate.filter.translator.facet;

import io.evitadb.api.query.filter.Facet;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link FilterConstraintTranslator} for {@link Facet} constraint.
 * This implementation actually does nothing because facets are handled by {@link UserFilterTranslator} due to additional
 * complexity of facet grouping.
 * But visitor needs to know about some translator for {@link Facet} so this implementation
 * acts as placeholder.
 *
 * @author Lukáš Hornych 2021
 */
public class FacetTranslator implements FilterConstraintTranslator<Facet>  {

    @Nullable
    @Override
    public SqlPart translate(@Nonnull Facet constraint, @Nonnull FilterTranslatingContext ctx) {
        // should not generate any standalone sql to actual query, whole facet translation handled by user filter translator
        // due to additional complexity of facet grouping
        return null;
    }
}
