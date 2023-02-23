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

package io.evitadb.storage.pmptt;

import io.evitadb.api.EsEntityCollection;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.query.require.Parents;
import io.evitadb.api.query.require.ParentsOfType;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.utils.StringUtils;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.stream.Collectors.toMap;

/**
 * DOCME SKA doc me and add more comments!!!!
 *
 * @author Štěpán Kameník (kamenik@fg.cz), FG Forrest a.s. (c) 2022
 **/
@RequiredArgsConstructor
public class ParentsComputer {

    private final EsEntityCollection esEntityCollection;

    public io.evitadb.api.io.extraResult.Parents getParentsOfEntity(Parents parentsOfType, EvitaRequestBase evitaRequest, List<String> primaryKeys) {
        Serializable entityType = evitaRequest.getEntityType();
        Map<Serializable, io.evitadb.api.io.extraResult.Parents.ParentsByType<?>> map =
                Stream.of(entityType)
                        .collect(
                                toMap(
                                        type->type,
                                        type -> {
                                            Collector<EntityReference, ?, Map<Integer, Serializable[]>> parentEntityCollector = getCollector(false, entityType, parentsOfType.getRequirements(),evitaRequest.getLanguage(), evitaRequest.getAlignedNow());
                                            return new io.evitadb.api.io.extraResult.Parents.ParentsByType<>(
                                                    entityType,
                                                    primaryKeys
                                                            .stream()
                                                            .map(i-> StringUtils.cleanFromType(i,type))
                                                            .collect(
                                                                    toMap(
                                                                            i->i,
                                                                            i -> Stream.of(i)
                                                                                    .map(p -> new EntityReference(entityType, p))
                                                                                    .collect(parentEntityCollector)
                                                                    )
                                                            )
                                            );
                                        })
                        );
        return new io.evitadb.api.io.extraResult.Parents(map);
    }


    public io.evitadb.api.io.extraResult.Parents getParentsOfTypes(ParentsOfType parentsOfType, EvitaRequestBase evitaRequest, List<String> primaryKeys) {
        EvitaResponseBase<Serializable> entities = esEntityCollection.getEntities(new EsEvitaRequest(
                query(
                        entities(evitaRequest.getEntityType()),
                        filterBy(
                                primaryKey(primaryKeys.stream().map(i->StringUtils.cleanFromType(i,evitaRequest.getEntityType())).toArray(Integer[]::new))
                        ),
                        require(
                                page(1, Integer.MAX_VALUE),
                                references()
                        )
                ),
                evitaRequest.getAlignedNow()
        ));


        Serializable[] types = parentsOfType.getEntityTypes();
        Map<Serializable, io.evitadb.api.io.extraResult.Parents.ParentsByType<?>> map = Arrays
                .stream(types)
                .collect(
                        toMap(
                                type->type,
                                type -> {
                                    Collector<EntityReference, ?, Map<Integer, Serializable[]>> parentEntityCollector = getCollector(true, type, parentsOfType.getRequirements(), evitaRequest.getLanguage(), evitaRequest.getAlignedNow());
                                    return new io.evitadb.api.io.extraResult.Parents.ParentsByType<>(
                                            type,
                                            entities.getRecordData()
                                                    .stream()
                                                    .map(SealedEntity.class::cast)
                                                    .collect(
                                                            toMap(
                                                                    EntityContract::getPrimaryKey,
                                                                    i -> {
                                                                        if (Objects.equals(type, i.getType())) {
                                                                            return Stream.of(i)
                                                                                    .map(p -> new EntityReference(p.getType(), p.getPrimaryKey()))
                                                                                    .collect(parentEntityCollector);
                                                                        } else {
                                                                            return i
                                                                                    .getReferences(type)
                                                                                    .stream()
                                                                                    .map(ReferenceContract::getReferencedEntity)
                                                                                    .collect(parentEntityCollector);
                                                                        }
                                                                    }
                                                            )
                                                    )
                                    );
                                })
                );
        return new io.evitadb.api.io.extraResult.Parents(map);
    }


    private Collector<EntityReference, ?, Map<Integer, Serializable[]>> getCollector(boolean includingSelf, Serializable entityType, EntityContentRequire[] requirements, Locale language, ZonedDateTime alignedNow) {
        return Collectors.toMap(
                EntityReference::getPrimaryKey,
                entity -> {
                    Integer[] parentItems = PMPTTAccessor
                            .getHierarchy(entity.getType())
                            .getParentItems(StringUtils.getUI(entity.getType(), entity.getPrimaryKey()))
                            .stream()
                            .map(i->StringUtils.cleanFromType(i.getCode(),entity.getType()))
                            .toArray(Integer[]::new);
                    Integer[] parents;
                    if (includingSelf) {

                        List<Integer> list = new LinkedList<>(Arrays.asList(parentItems));
                        list.add(entity.getPrimaryKey());
                        parents = list.toArray(i -> new Integer[0]);

                    } else {
                        parents = parentItems;
                    }

                    return !ArrayUtils.isEmpty(requirements) ?
                            Arrays.stream(parents)
                                    .map(it -> esEntityCollection.getEntity(it, new EsEvitaRequest(entityType, language, requirements, alignedNow)))
                                    .toArray(SealedEntity[]::new) :
                            parents;
                }
        );
    }
}
