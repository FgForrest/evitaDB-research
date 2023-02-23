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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.storage.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * No extra information provided, if you see this, code is my very best work,
 * so each method is self-explanatory and description would be useless.
 * If this code is not masterpiece, there wasn't time to write proper code
 * and not even documentation, so so sorry.
 *
 * @author Štěpán Kameník (kamenik@fg.cz), FG Forrest a.s. (c) 2022
 **/
@AllArgsConstructor
@Data
public class FacetReferenceDto implements Serializable{
    public FacetReferenceDto(ReferenceContract referenceContract) {
        EntityReference referencedEntity = referenceContract.getReferencedEntity();
        Optional<ReferenceContract.GroupEntityReference> group = Optional.ofNullable(referenceContract.getGroup());
        this.type = referencedEntity.getType();
        this.primaryKey = referencedEntity.getPrimaryKey();
        this.groupType = group.map(EntityReference::getType).orElse(null);
        this.groupPrimaryKey = group.map(EntityReference::getPrimaryKey).orElse(null);
        this.groupOrType = Optional.ofNullable(groupType).orElse(type);
        this.groupId = StringUtils.join("_",groupOrType,groupPrimaryKey);
        this.referenceId = StringUtils.join("_",type, primaryKey, groupType, groupPrimaryKey);
    }

    public FacetReferenceDto(Serializable type, int primaryKey, @Nullable Serializable groupType, @Nullable Integer groupPrimaryKey, Set<String> usedInEntities) {
        this.type = type;
        this.primaryKey = primaryKey;
        this.groupType = groupType;
        this.groupPrimaryKey = groupPrimaryKey;
        this.usedInEntities = usedInEntities;
        this.groupOrType = Optional.ofNullable(groupType).orElse(type);
        this.groupId = StringUtils.join("_",groupOrType,groupPrimaryKey);
        this.referenceId = StringUtils.join("_",type, primaryKey, groupType, groupPrimaryKey);
    }

    private final Serializable type;

    private final int primaryKey;

    @Nullable
    private final Serializable groupType;


    @Nullable
    private final Integer groupPrimaryKey;

    @Nullable
    @JsonIgnore
    private final Serializable groupOrType;
    @JsonIgnore
    private final String groupId;
    @JsonIgnore
    private final String referenceId;

    private Set<String> usedInEntities = new HashSet<>();

}
