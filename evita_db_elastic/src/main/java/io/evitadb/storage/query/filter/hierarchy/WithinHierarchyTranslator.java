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

package io.evitadb.storage.query.filter.hierarchy;

import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.query.filter.FilterConstraintTranslator;
import io.evitadb.storage.query.filter.TypeTranslator;
import io.evitadb.storage.utils.StringUtils;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.io.Serializable;
import java.util.Arrays;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class WithinHierarchyTranslator implements FilterConstraintTranslator<WithinHierarchy>, HierarchyTranslator {
    public static final String PATHS_RIGHT = "paths.rightBound";
    public static final String PATHS_LEFT = "paths.leftBound";
    @Override
    public EsConstraint apply(WithinHierarchy constraint, FilterByVisitor filterByVisitor) {

        if (constraint.isDirectRelation() && constraint.isExcludingRoot()) {
            throw new IllegalArgumentException("Something is wrong, there cannot be both, excludingRoot and directRelation");
        }

        Serializable entityType = constraint.getEntityType();
        Serializable serializableEntityType = entityType == null ? "CATEGORY" : entityType;
        Hierarchy hierarchy = PMPTTAccessor.getHierarchy(serializableEntityType);

        int parentId = constraint.getParentId();
        HierarchyItem item = hierarchy.getItem(StringUtils.getUI(serializableEntityType, parentId));


        Integer primaryKeyToFetchWithResult = null;
        BoolQueryBuilder must;

        if (constraint.isDirectRelation()) {
            must = boolQuery()
                    .must(matchQuery(PATHS_LEFT, item.getLeftBound()))
                    .must(matchQuery(PATHS_RIGHT, item.getRightBound()));
        } else {
            primaryKeyToFetchWithResult = parentId;
            if (constraint.isExcludingRoot() && entityType != null && !entityType.equals(filterByVisitor.getEntitySchema().getName())) {
                must = boolQuery()
                        .must(rangeQuery(PATHS_LEFT).gt(item.getLeftBound()))
                        .must(rangeQuery(PATHS_RIGHT).lt(item.getRightBound()));
            } else {
                must = boolQuery()
                        .must(rangeQuery(PATHS_LEFT).gte(item.getLeftBound()))
                        .must(rangeQuery(PATHS_RIGHT).lte(item.getRightBound()));
            }
        }

        if (entityType != null){
            TypeTranslator.enhanceWithType(must, entityType,"paths.type");
        }

        int[] excludedChildrenIds = constraint.getExcludedChildrenIds();
        Arrays.stream(excludedChildrenIds)
                .mapToObj(i -> hierarchy.getItem(StringUtils.getUI(serializableEntityType, i)))
                .forEach(i ->
                        must
                            .mustNot(
                                boolQuery()
                                        .must(rangeQuery(PATHS_LEFT).gte(i.getLeftBound()))
                                        .must(rangeQuery(PATHS_RIGHT).lte(i.getRightBound())))
                            );
        BoolQueryBuilder pathQuery = boolQuery()
                .should(
                        boolQuery()
                                .filter(
                                        nestedQuery("paths",
                                                must,
                                                ScoreMode.Max
                                        )
                                )).minimumShouldMatch(1);

        if (primaryKeyToFetchWithResult != null) {
            BoolQueryBuilder primaryKey = boolQuery()
                    .must(
                            matchQuery("primaryKey", primaryKeyToFetchWithResult));

            if (entityType != null){
                primaryKey
                        .must(
                                matchQuery("type.value.keyword", entityType.toString())
                        );
            }

            pathQuery = pathQuery.should(
                    primaryKey
            );
        }
        if (excludedChildrenIds.length > 0) {

            BoolQueryBuilder primaryKey = boolQuery()
                    .must(
                            termsQuery("primaryKey", excludedChildrenIds));

            if (entityType != null){
                primaryKey
                        .must(
                                matchQuery("type.value.keyword", entityType.toString())
                        );
            }

            pathQuery = boolQuery()
                    .must(pathQuery)
                    .mustNot(primaryKey);
        }
        if (constraint.isExcludingRoot()) {
            pathQuery = boolQuery()
                    .must(pathQuery)
                    .mustNot(
                            boolQuery()
                                    .filter(
                                            nestedQuery("paths",
                                                    boolQuery()
                                                            .must(matchQuery(PATHS_LEFT, 0))
                                                            .must(matchQuery(PATHS_RIGHT, 0)),
                                                    ScoreMode.Max
                                            )
                                    )

                    );
        }
        return new EsConstraint(pathQuery);
    }

}
