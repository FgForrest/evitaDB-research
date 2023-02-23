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

package io.evitadb.storage.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.EsEntityCollection;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.api.io.extraResult.HierarchyStatistics;
import io.evitadb.api.io.extraResult.HierarchyStatistics.LevelInfo;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.exception.EvitaGetException;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * DOCME SKA doc me and add more comments!!!!
 *
 * @author Štěpán Kameník (kamenik@fg.cz), FG Forrest a.s. (c) 2022
 **/
@RequiredArgsConstructor
public class HierarchyStatisticsComputer {

    private final EsEntityCollection esEntityCollection;

    public HierarchyStatistics computeHierarchyStatistics(EsEvitaRequest evitaRequest, RestHighLevelClient client, EntitySchema schema, String indexName, ObjectMapper objectMapper) {
        io.evitadb.api.query.require.HierarchyStatistics hierarchyStatistics = evitaRequest.getRequireHierarchyStatistics();
        WithinHierarchy withinHierarchyFilter = evitaRequest.getWithinHierarchyFilter();
        Locale language = evitaRequest.getLanguage();
        Integer parentId = ofNullable(withinHierarchyFilter).map(WithinHierarchy::getParentId).orElse(null);

        Serializable entityType = ofNullable(hierarchyStatistics.getEntityType()).orElse(evitaRequest.getEntityType());
        Hierarchy hierarchy = PMPTTAccessor.getHierarchy(entityType);
        List<HierarchyItem> leafs = hierarchy.getLeafItems(StringUtils.getUI(entityType, parentId));

        final Predicate<SealedEntity> languagePredicate = it -> language == null || it.getLocales().contains(language);
        final Predicate<SealedEntity> categoryPredicate = sealedEntity -> {
            final Integer categoryId = sealedEntity.getPrimaryKey();
            return parentId == null || (
                    Objects.equals(parentId, categoryId) ||
                            hierarchy
                                    .getParentItems(Objects.requireNonNull(StringUtils.getUI(entityType, categoryId)))
                                    .stream()
                                    .anyMatch(it -> Objects.equals(StringUtils.getUI(entityType, parentId), it.getCode()))
            );
        };
        EntityContentRequire[] requirements = hierarchyStatistics.getRequirements();
        final Map<Integer, SealedEntity> allExaminedCategoriesById = leafs
                .stream()
                .flatMap(i -> Stream.concat(hierarchy.getParentItems(i.getCode()).stream(), Stream.of(i)))
                .distinct()
                .map(it -> esEntityCollection.getEntity(
                        StringUtils.cleanFromType(it.getCode(), entityType),
                        new EsEvitaRequest(entityType, evitaRequest.getLanguage(), requirements, evitaRequest.getAlignedNow())))
                .filter(Objects::nonNull)
                .filter(i -> i.getPrimaryKey() != null)
                .collect(
                        Collectors.toMap(
                                EntityContract::getPrimaryKey,
                                Function.identity()
                        )
                );

        Map<String, Long> aggs = new HashMap<>();
        try {
            SearchResponse searchResponse = client.search(
                    StatisticsComputer.getQueryForReferences(
                            evitaRequest.getQuery(),
                            filterBy -> filterBy,
                            FilterMode.HIERARCHY,
                            evitaRequest,
                            schema,
                            false,
                            indexName,
                            objectMapper,
                            client),
                    RequestOptions.DEFAULT);
            aggs = ((ParsedStringTerms) ((ParsedNested) searchResponse
                    .getAggregations()
                    .get("hierarchy"))
                    .getAggregations()
                    .get("hierarchyCounts"))
                    .getBuckets()
                    .stream()
                    .collect(Collectors.toMap(MultiBucketsAggregation.Bucket::getKeyAsString, MultiBucketsAggregation.Bucket::getDocCount));
        } catch (IOException e) {
            throw new EvitaGetException("Cannot ge hierarchy stats : " + e.getMessage(),e);
        }

        final Map<Integer, Integer> cardinalities = new HashMap<>();
        for (SealedEntity category : allExaminedCategoriesById.values()) {
            if (languagePredicate.and(categoryPredicate).test(category) &&
                hierarchy
                    .getParentItems(Objects.requireNonNull(StringUtils.getUI(entityType, category.getPrimaryKey())))
                    .stream()
                    .map(HierarchyItem::getCode)
                    .map(s -> StringUtils.cleanFromType(s, entityType))
                    .map(allExaminedCategoriesById::get)
                    .allMatch(languagePredicate)) {

                final int categoryId = category.getPrimaryKey();
                final List<Integer> categoryPath = Stream.concat(
                        hierarchy.getParentItems(StringUtils.getUI(entityType, categoryId))
                                .stream()
                                .map(it -> StringUtils.cleanFromType(it.getCode(), entityType)),
                        Stream.of(categoryId)
                ).collect(Collectors.toList());
                for (int i = categoryPath.size() - 1; i >= 0; i--) {
                    int examinedCategoryId = categoryPath.get(i);

                    Integer countOfRelations = 1;
                    if (!Objects.equals(entityType, evitaRequest.getEntityType())) {
                        countOfRelations = ofNullable(aggs.get(StringUtils.getUI(entityType,categoryId))).orElse(0L).intValue();
                    }
                    if (examinedCategoryId == categoryId) {
                        cardinalities.put(categoryId, cardinalities.getOrDefault(categoryId, 0) + (Objects.equals(entityType, evitaRequest.getEntityType()) ? 0 : countOfRelations));
                    } else {
                        cardinalities.put(examinedCategoryId, cardinalities.getOrDefault(examinedCategoryId, 0) + countOfRelations);
                    }
                    if (parentId != null && examinedCategoryId == parentId) {
                        break;
                    }
                }
            }
        }

        final LinkedList<LevelInfo<? extends Serializable>> levelInfo = new LinkedList<>();
        for (HierarchyItem rootItem : (parentId == null ? hierarchy.getRootItems() : Collections.singletonList(hierarchy.getItem(StringUtils.getUI(entityType, parentId))))) {
            createLevelInfoFromEntity(rootItem, entityType, cardinalities, levelInfo, evitaRequest, requirements, hierarchy);
        }
        return new HierarchyStatistics(Collections.singletonMap(entityType, levelInfo));
    }

    private void createLevelInfoFromEntity(HierarchyItem item, Serializable entityType, Map<Integer, Integer> categoryCardinalities, LinkedList<LevelInfo<? extends Serializable>> levelInfo, EsEvitaRequest evitaRequest, EntityContentRequire[] requirements, Hierarchy categoryHierarchy) {
        final Integer categoryId = StringUtils.cleanFromType(item.getCode(), entityType);
        final Integer cardinality = categoryCardinalities.get(categoryId);
        if (cardinality != null && (Objects.equals(entityType, evitaRequest.getEntityType()) || cardinality != 0)) {
            final SealedEntity category = esEntityCollection.getEntity(categoryId, new EsEvitaRequest(entityType, evitaRequest.getLanguage(), requirements, evitaRequest.getAlignedNow()));
            levelInfo.add(new LevelInfo(category, cardinality, getChildInfo(categoryId, categoryHierarchy, categoryCardinalities, evitaRequest, requirements, entityType)));
        }
    }

    private List<LevelInfo<? extends Serializable>> getChildInfo(int parentId, Hierarchy categoryHierarchy, Map<Integer, Integer> categoryCardinalities, EsEvitaRequest evitaRequest, EntityContentRequire[] requirements, Serializable entityType) {
        final LinkedList<LevelInfo<? extends Serializable>> levelInfo = new LinkedList<>();
        for (HierarchyItem item : categoryHierarchy.getChildItems(StringUtils.getUI(entityType, parentId))) {
            createLevelInfoFromEntity(item, entityType, categoryCardinalities, levelInfo, evitaRequest, requirements, categoryHierarchy);
        }
        return levelInfo;
    }

}
