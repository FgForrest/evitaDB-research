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

package io.evitadb.api.query;

import java.io.Serializable;

/**
 * Transforms string representation of EvitaQL queries to actual objects.
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public interface QueryParser {

    /**
     * Creates {@link Query} corresponding to string representation in {@code query};
     *
     * @param query string representation of query in specific format
     * @return parsed {@link Query}
     */
    Query parseQuery(String query);

    /**
     * Creates {@link Constraint} corresponding to string representation in {@code constraint}
     *
     * @param constraint string representation of constraint in specific format
     * @return parsed {@link Constraint}
     */
    Constraint<?> parseConstraint(String constraint);

    /**
     * Creates actual value for any supported literal in string representation in {@code literal}
     *
     * @param literal string representation of literal value
     * @param <T> parsed literal type
     * @return parsed literal value
     */
    <T extends Serializable> T parseLiteral(String literal);
}
