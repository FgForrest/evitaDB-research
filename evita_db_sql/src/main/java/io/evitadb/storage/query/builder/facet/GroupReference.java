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

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

import static java.util.Optional.ofNullable;

/**
 * Internal data structure for referencing nullable groups.
 */
@Data
@EqualsAndHashCode(exclude = "serializedEntityType")
public class GroupReference implements Comparable<GroupReference> {

    private final Serializable entityType;
    private final String serializedEntityType;
    private final Integer groupId;

    @Override
    public int compareTo(GroupReference o) {
        final int first = serializedEntityType.compareTo(o.serializedEntityType);
        if (first != 0) {
            return first;
        }
        return ofNullable(groupId)
                .map(it -> ofNullable(o.groupId).map(it::compareTo).orElse(-1))
                .orElseGet(() -> o.groupId != null ? 1 : 0);
    }
}
