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

import io.evitadb.storage.serialization.sql.HierarchyPlacement;
import lombok.Data;

/**
 * Context of processing single whole complex evita request. It can hold any data needed globally (in scope of single request).
 * Complex mean we want to fetch more than just list of entities.
 *
 * @see ComplexEntityQueryContextHolder
 * @author Lukáš Hornych 2021
 */
@Data
public class ComplexEntityQueryContext {

    /**
     * Name of generated temporary table that has been created to cache intermediate results for computation additional
     * results.
     */
    private String tempTableName;

    /**
     * Hierarchy placement of single root hierarchy entity
     */
    private HierarchyPlacement rootPlacement;
}
