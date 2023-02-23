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

import org.postgresql.util.PGobject;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Represents postgresql's array of intrange data type
 *
 * @author Lukáš Hornych 2021
 */
public class PGIntRangeArray extends PGObject {

    private static final long serialVersionUID = -5116755978194183923L;

    public PGIntRangeArray(@Nonnull PGobject... intRanges) {
        super(
                "int8range[]",
                Arrays.stream(intRanges)
                        .map(range -> (range == null) ? "null" : "\"" + range.getValue() + "\"")
                        .collect(Collectors.joining(",", "{", "}"))
        );
    }
}
