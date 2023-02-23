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

package io.evitadb.storage.query.util;

import io.evitadb.api.query.filter.UserFilter;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public enum FilterMode {
    /**
     * Translates whole filter constraint tree
     */
    DEFAULT,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    BASELINE,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    PRICE_HISTOGRAM,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    PRICE_STATS,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    ATTRIBUTE_HISTOGRAM,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    ATTRIBUTE_STATS,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    DISJUNCTIVE_FACETS,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    DISJUNCTIVE_FACETS_AND,

    /**
     * Translates whole filter constraint tree without {@link UserFilter} sub-tree
     */
    HIERARCHY
}
