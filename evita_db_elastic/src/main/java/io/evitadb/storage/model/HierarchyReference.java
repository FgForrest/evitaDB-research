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

package io.evitadb.storage.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import one.edee.oss.pmptt.model.HierarchyItem;

import java.io.Serializable;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@Data
@NoArgsConstructor
public class HierarchyReference {

    private String externalId;
    private Long leftBound;
    private Long rightBound;
    private int level;
    private Serializable type;
    private boolean updated;

    public void updateBy(HierarchyItem hierarchyItem) {
        this.externalId = hierarchyItem.getCode();
        this.leftBound = hierarchyItem.getLeftBound();
        this.rightBound = hierarchyItem.getRightBound();
        this.level = hierarchyItem.getLevel();
    }
}
