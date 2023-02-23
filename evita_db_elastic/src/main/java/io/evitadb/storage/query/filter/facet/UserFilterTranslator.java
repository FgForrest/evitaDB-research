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

package io.evitadb.storage.query.filter.facet;


import io.evitadb.api.query.filter.UserFilter;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.exception.EvitaFacetComputationCheckedException;
import io.evitadb.storage.model.FacetReferenceDto;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.query.filter.FilterConstraintTranslator;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.statistics.StatisticsComputer.FacetUserFilter;
import io.evitadb.storage.utils.FacetUtil;
import io.evitadb.storage.utils.FacetUtil.Ref;
import io.evitadb.storage.utils.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@CommonsLog
public class UserFilterTranslator implements FilterConstraintTranslator<UserFilter> {
    public static final String NONE_TYPE = "noneGroup";

    @SneakyThrows
    @Override
    public EsConstraint apply(UserFilter constraint, FilterByVisitor visitor) {
        // ignore user filter subtree if only baseline part of query is desired
        if (visitor.getFilterMode() == FilterMode.BASELINE && !(constraint instanceof FacetUserFilter)) {
            return null;
        }

        // gather inner conditions that are not facets
        final List<EsConstraint> innerConditions = visitor.getCurrentLevelConstraints();

        // gather facet constraints that are direct children of user filter
        final Set<Ref> facetConstraints = FacetUtil.getRequestedFacets(constraint);

        if (innerConditions.isEmpty() && facetConstraints.isEmpty()) {
            return null;
        }
        FacetUtil.FacetGroups facetGroups = FacetUtil.getFacetGroupsFromQuery(visitor.getQuery());

        Map<Serializable, FacetReferenceDto> cachedReferences = FacetUtil.getOrComputeCachedReferences(visitor.getClient(), visitor.getIndexName(),visitor.getObjectMapper());

        Map<String,FacetReferenceDto> referenceContracts =
                facetConstraints
                        .stream()
                        .distinct()
                        .filter(Objects::nonNull)
                        .map(key->StringUtils.getUI(key.getEntityType(),key.getId()))
                        .collect(Collectors.toMap(key -> key, cachedReferences::get));

        Map<Serializable, List<FacetReferenceDto>> groupToReference =
                referenceContracts.values().stream().collect(Collectors.groupingBy(i -> ofNullable(i.getGroupId()).orElse(NONE_TYPE)));


        List<BoolQueryBuilder> conjunctionEsConstraints = buildFacetGroup(facetGroups.getConjunctionGroups(), facetConstraints, groupToReference, (a, b) -> a.should(b).minimumShouldMatch(1), false);
        List<BoolQueryBuilder> disjunctionEsConstraints = buildFacetGroup(facetGroups.getDisjunctionGroups(), facetConstraints, groupToReference, (a, b) -> a.should(b).minimumShouldMatch(1), true);
        List<BoolQueryBuilder> negatedEsConstraints = buildFacetGroup(facetGroups.getNegatedGroups(), facetConstraints, groupToReference, BoolQueryBuilder::must, false);

        Map<String, List<FacetReferenceDto>> facets = facetConstraints
                .stream()
                .map(i -> {
                    FacetReferenceDto facetReferenceDto = referenceContracts.get(StringUtils.getUI(i.getEntityType(), i.getId()));
                    Assert.notNull(facetReferenceDto,"Cannot find reference for facet: " + i.getId() + " type: " + i.getEntityType());
                    return facetReferenceDto;})
                .collect(Collectors.groupingBy(i -> i.getGroupId() != null ? i.getGroupId() : NONE_TYPE));

        List<BoolQueryBuilder> facetEsConstraints = buildFacetGroup(facets, (a, b) -> a.should(b).minimumShouldMatch(1));

        BoolQueryBuilder boolQueryBuilder = boolQuery();
        if (visitor.getFilterMode() != FilterMode.DISJUNCTIVE_FACETS_AND) {
            if (!conjunctionEsConstraints.isEmpty()) conjunctionEsConstraints.forEach(boolQueryBuilder::must);
            if (!negatedEsConstraints.isEmpty()) negatedEsConstraints.forEach(boolQueryBuilder::mustNot);
            if (!facetEsConstraints.isEmpty()) facetEsConstraints.forEach(boolQueryBuilder::must);
        }

        if (visitor.getFilterMode() == FilterMode.DISJUNCTIVE_FACETS_AND) {
            if (disjunctionEsConstraints.isEmpty())
                throw new EvitaFacetComputationCheckedException("There is no disjunctive facets, leave this query");
            BoolQueryBuilder disQuery = boolQuery();
            disjunctionEsConstraints.forEach(disQuery::must);
            boolQueryBuilder = boolQueryBuilder.hasClauses() ? disQuery.must(boolQueryBuilder) : disQuery;
        } else if (visitor.getFilterMode() != FilterMode.DISJUNCTIVE_FACETS && !disjunctionEsConstraints.isEmpty()) {
            BoolQueryBuilder disQuery = boolQuery().minimumShouldMatch(1);
            disjunctionEsConstraints.forEach(disQuery::should);
            BoolQueryBuilder should = boolQuery().should(disQuery).minimumShouldMatch(1);
            boolQueryBuilder = boolQueryBuilder.hasClauses() ? should.should(boolQueryBuilder) : should;
        }


        innerConditions.stream()
                .map(EsConstraint::getQueryBuilder)
                .filter(Objects::nonNull)
                .forEach(boolQueryBuilder::must);

        return new EsConstraint(boolQueryBuilder);
    }

    private List<BoolQueryBuilder> buildFacetGroup(Set<Ref> set, Set<Ref> facetConstraints, Map<Serializable, List<FacetReferenceDto>> groupToReference, BiConsumer<BoolQueryBuilder, QueryBuilder> consumer, boolean groupConstraints) {
        List<FacetReferenceDto> facets = new LinkedList<>();
        List<BoolQueryBuilder> constraints = new LinkedList<>();
        for (Ref groupRef : set) {
            String key = groupRef.getEntityType() + "_" + groupRef.getId();
            List<FacetReferenceDto> referenceContractsOfGroup = groupToReference.get(key);
            if (referenceContractsOfGroup != null) {

                List<FacetReferenceDto> references = referenceContractsOfGroup
                        .stream()
                        .filter(i ->
                                new HashSet<>(facetConstraints)
                                        .stream()
                                        .filter(k -> k.getEntityType().equals(i.getType())
                                                && i.getPrimaryKey() == k.getId())
                                        .peek(facetConstraints::remove)
                                        .findAny()
                                        .isPresent()
                        )
                        .collect(Collectors.toList());

                if (groupConstraints) {
                    facets.addAll(references);
                } else {
                    references.forEach(j -> buildFacetQuery(Collections.singletonList(j), constraints, consumer));
                }
            }
        }
        if (groupConstraints) {
            buildFacetQuery(facets, constraints, consumer);
        }
        return constraints;
    }

    private List<BoolQueryBuilder> buildFacetGroup(Map<String, List<FacetReferenceDto>> facetConstraints, BiConsumer<BoolQueryBuilder, QueryBuilder> consumer) {

        List<BoolQueryBuilder> constraints = new LinkedList<>();
        for (Map.Entry<String, List<FacetReferenceDto>> groupRef : facetConstraints.entrySet()) {
            buildFacetQuery(groupRef.getValue(), constraints, consumer);
        }
        return constraints;
    }

    private void buildFacetQuery(List<FacetReferenceDto> facets, List<BoolQueryBuilder> facetEsConstraints, BiConsumer<BoolQueryBuilder, QueryBuilder> consumer) {

        if (facets.isEmpty()) return;

        BoolQueryBuilder queryBuilder = boolQuery();
        facets
                .stream()
                .collect(Collectors.groupingBy(FacetReferenceDto::getType))
                .forEach((entityType, value) -> {

                    BoolQueryBuilder typeFilter = boolQuery()
                            .filter(
                                    nestedQuery(
                                            "references.referencedEntityType",
                                            matchQuery("references.referencedEntityType.value.keyword", entityType),
                                            ScoreMode.None
                                    )
                            );
                    value
                            .forEach(facet ->
                                    consumer.accept(
                                            typeFilter,
                                            matchQuery("references.referencedEntityPrimaryKey", facet.getPrimaryKey())));

                    BoolQueryBuilder facetQuery = boolQuery()
                            .filter(
                                    nestedQuery(
                                            "references",
                                            typeFilter,
                                            ScoreMode.Max
                                    )
                            );
                    queryBuilder.must(facetQuery);
                });
        facetEsConstraints.add(queryBuilder);
    }


}
