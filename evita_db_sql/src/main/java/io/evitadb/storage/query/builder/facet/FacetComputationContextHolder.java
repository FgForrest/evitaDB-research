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

package io.evitadb.storage.query.builder.facet;

import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.require.FacetGroupsConjunction;
import io.evitadb.api.query.require.FacetGroupsDisjunction;
import io.evitadb.api.query.require.FacetGroupsNegation;
import io.evitadb.storage.EntityCollectionContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Holds {@link FacetComputationContext} for each thread separately and makes it statically available in that thread
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FacetComputationContextHolder {
    private static final ThreadLocal<FacetComputationContext> CONTEXT = new ThreadLocal<>();

    /**
     * Executes the supplier with available passed context and returns supplier result
     */
    public static <T> T executeWithinContext(@Nonnull EntityCollectionContext collectionContext,
                                             @Nonnull List<FacetGroupsConjunction> facetGroupsConjunction,
                                             @Nonnull List<FacetGroupsDisjunction> facetGroupsDisjunction,
                                             @Nonnull List<FacetGroupsNegation> facetGroupsNegation,
                                             @Nullable FilterBy filterBy,
                                             @Nonnull Function<FacetComputationContext, T> executable) {
        try {
            final FacetComputationContext context = new FacetComputationContext(
                    collectionContext,
                    facetGroupsConjunction,
                    facetGroupsDisjunction,
                    facetGroupsNegation,
                    filterBy
            );
            CONTEXT.set(context);
            return executable.apply(context);
        } finally {
            CONTEXT.remove();
        }
    }

    /**
     * Returns context for current thread (if exists)
     */
    public static FacetComputationContext getContext() {
        return CONTEXT.get();
    }
}
