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

import java.util.Arrays;

import static java.util.Optional.ofNullable;

/**
 * Ancestor for facet conditions having NOT relation
 *
 * @author Lukáš Hornych 2021
 */
public abstract class AbstractNotFacetCondition extends AbstractFacetCondition {

    protected AbstractNotFacetCondition(GroupReference group, String serializedEntityType, int[] facetIds) {
        super(group, serializedEntityType, facetIds);
    }

    @Override
    public String toString() {
        return getSerializedEntityType() + ofNullable(getGroup().toString()).map(it -> " " + it)
                .orElse("") + " (NOT):" + Arrays.toString(getFacetIds());
    }
}
