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

package io.evitadb.storage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Holds {@link ComplexEntityQueryContext} for each thread separately during whole request processing.
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ComplexEntityQueryContextHolder {
    private static final ThreadLocal<ComplexEntityQueryContext> CONTEXT = new ThreadLocal<>();

    /**
     * Executes the supplier with available passed context and returns supplier result
     */
    public static <T> T executeWithinContext(@Nonnull Supplier<T> executable) {
        try {
            CONTEXT.set(new ComplexEntityQueryContext());
            return executable.get();
        } finally {
            CONTEXT.remove();
        }
    }

    /**
     * Returns context for current thread (if exists)
     */
    public static ComplexEntityQueryContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Checks if any context is present
     */
    public static boolean hasContext() {
        return getContext() != null;
    }
}
