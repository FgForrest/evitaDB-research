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

import io.evitadb.api.query.filter.WithinRootHierarchy;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.query.filter.FilterConstraintTranslator;
import io.evitadb.storage.query.filter.TypeTranslator;
import io.evitadb.storage.utils.StringUtils;
import one.edee.oss.pmptt.model.Hierarchy;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.io.Serializable;
import java.util.Arrays;

import static io.evitadb.storage.query.filter.hierarchy.WithinHierarchyTranslator.PATHS_LEFT;
import static io.evitadb.storage.query.filter.hierarchy.WithinHierarchyTranslator.PATHS_RIGHT;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class WithinRootHierarchyTranslator implements FilterConstraintTranslator<WithinRootHierarchy>, HierarchyTranslator {

    @Override
    public EsConstraint apply(WithinRootHierarchy constraint, FilterByVisitor filterByVisitor) {


        Serializable entityType = constraint.getEntityType();
        Serializable serializableEntityType = entityType == null ? "CATEGORY" : entityType;
        Hierarchy hierarchy = PMPTTAccessor.getHierarchy(serializableEntityType);

        BoolQueryBuilder boolQuery = boolQuery();
        boolQuery
                .should(
                        boolQuery()
                                .must(matchQuery(PATHS_LEFT, 0))
                                .must(matchQuery(PATHS_RIGHT, 0))
                )
                .minimumShouldMatch(1);
        if (!constraint.isDirectRelation()) {
            boolQuery.should(
                    boolQuery()
                            .must(existsQuery(PATHS_LEFT))
                            .must(existsQuery(PATHS_RIGHT))
            ).minimumShouldMatch(1);
        }

        if (constraint.getEntityType() != null){
            TypeTranslator.enhanceWithType(boolQuery,constraint.getEntityType(),"paths.type");
        }

        int[] excludedChildrenIds = constraint.getExcludedChildrenIds();
        Arrays.stream(excludedChildrenIds)
                .mapToObj(i -> hierarchy.getItem(StringUtils.getUI(serializableEntityType, i)))
                .forEach(i -> boolQuery.mustNot(
                        boolQuery()
                                .must(rangeQuery(PATHS_LEFT).gte(i.getLeftBound()))
                                .must(rangeQuery(PATHS_RIGHT).lte(i.getRightBound()))
                ));
        BoolQueryBuilder pathQuery = boolQuery()
                .filter(
                        nestedQuery("paths",
                                boolQuery,
                                ScoreMode.Max
                        )
                );

        if (excludedChildrenIds.length > 0) {
            BoolQueryBuilder primaryKey = boolQuery()
                    .must(
                            termsQuery("primaryKey", excludedChildrenIds));

            if (constraint.getEntityType() != null){
                primaryKey
                        .must(
                                matchQuery("type.value.keyword", constraint.getEntityType().toString())
                        );
            }

            pathQuery = boolQuery()
                    .must(pathQuery)
                    .mustNot(primaryKey);
        }
        return new EsConstraint(pathQuery);
    }
}
