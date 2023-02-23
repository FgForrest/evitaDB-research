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

package io.evitadb.storage.serialization.sql;

import lombok.Value;

/**
 * Represents single hierarchy entity placement in hierarchy. Used for specifying referenced hierarchy entities or itself.
 * Basically, represents part of {@link one.edee.oss.pmptt.model.HierarchyItem} where only placement data are needed
 * (usually where hierarchy type and PK is already known like querying).
 *
 * @author Lukáš Hornych 2021
 */
@Value
public class HierarchyPlacement {

    /**
     * Used only by hierarchy entity to reference its own placement
     */
    public static final String SELF_TYPE = "__self__";
    /**
     * Used only by hierarchy entity to reference its own placement
     */
    public static final String SELF_PRIMARY_KEY = "__self__";

    long leftBound;
    long rightBound;
    short level;
}
