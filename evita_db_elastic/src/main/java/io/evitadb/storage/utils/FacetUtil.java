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

package io.evitadb.storage.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.UserFilter;
import io.evitadb.api.query.require.FacetGroupsConjunction;
import io.evitadb.api.query.require.FacetGroupsDisjunction;
import io.evitadb.api.query.require.FacetGroupsNegation;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.storage.model.FacetReferenceDto;
import io.evitadb.storage.statistics.StatisticsComputer;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryUtils.findRequires;
import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class FacetUtil {
    private FacetUtil() {
    }

    @Getter
    private static final Map<Serializable, FacetReferenceDto> cachedReferences = new HashMap<>();

    public static Map<Serializable, FacetReferenceDto> getOrComputeCachedReferences(RestHighLevelClient client, String indexName, ObjectMapper om) throws IOException {

        if (cachedReferences.isEmpty()) {

            BoolQueryBuilder references = boolQuery().must(matchAllQuery());

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(references);
            searchSourceBuilder.fetchSource(true);
            searchSourceBuilder.size(Integer.MAX_VALUE);

            SearchResponse searchResponse = client.search(new SearchRequest(indexName + "-reference").source(searchSourceBuilder), RequestOptions.DEFAULT);

            List<FacetReferenceDto> obtainedReferences = StatisticsComputer.getReferencesFromResponse(searchResponse, om);

            cachedReferences.putAll(
                    obtainedReferences
                            .stream()
                            .collect(Collectors.toMap(i -> StringUtils.getUI(i.getType(), i.getPrimaryKey()), j -> j,(k,v)->{
                                if (k.getUsedInEntities().size() > v.getUsedInEntities().size()){
                                    k.getUsedInEntities().addAll(v.getUsedInEntities());
                                    return k;
                                }else {
                                    v.getUsedInEntities().addAll(k.getUsedInEntities());
                                    return v;
                                }
                            })));
        }
        return cachedReferences;

    }

    public static Set<Ref> getRequestedFacets(UserFilter constraint) {
        return Arrays.stream(constraint.getConstraints())
                .filter(c -> Facet.class.isAssignableFrom(c.getClass()))
                .map(Facet.class::cast)
                .flatMap(i -> Arrays.stream(i.getFacetIds()).mapToObj(p -> new Ref(i.getEntityType(), p)))
                .collect(Collectors.toSet());
    }

    public static FacetGroups getFacetGroupsFromQuery(Query query) {

        List<FacetGroupsConjunction> conjunction = findRequires(query, FacetGroupsConjunction.class);
        List<FacetGroupsDisjunction> disjunctions = findRequires(query, FacetGroupsDisjunction.class);
        List<FacetGroupsNegation> negated = findRequires(query, FacetGroupsNegation.class);


        // find custom inner and inter facet groups configuration from query
        final Set<Ref> conjunctGroups = conjunction
                .stream()
                .flatMap(it -> {
                    if (ArrayUtils.isEmpty(it.getFacetGroups())) {
                        return Stream.of(new Ref(it.getEntityType(), null));
                    } else {
                        return Arrays.stream(it.getFacetGroups())
                                .mapToObj(x -> new Ref(it.getEntityType(), x));
                    }
                })
                .collect(java.util.stream.Collectors.toSet());
        final Set<Ref> disjunctiveGroups = disjunctions
                .stream()
                .flatMap(it -> {
                    if (ArrayUtils.isEmpty(it.getFacetGroups())) {
                        return Stream.of(new Ref(it.getEntityType(), null));
                    } else {
                        return Arrays.stream(it.getFacetGroups())
                                .mapToObj(x -> new Ref(it.getEntityType(), x));
                    }
                })
                .collect(java.util.stream.Collectors.toSet());
        final Set<Ref> negatedGroups = negated
                .stream()
                .flatMap(it -> {
                    if (ArrayUtils.isEmpty(it.getFacetGroups())) {
                        return Stream.of(new Ref(it.getEntityType(), null));
                    } else {
                        return Arrays.stream(it.getFacetGroups())
                                .mapToObj(x -> new Ref(it.getEntityType(), x));
                    }
                })
                .collect(Collectors.toSet());
        return new FacetGroups(conjunctGroups, disjunctiveGroups, negatedGroups);
    }

    public static void prepareAggForFacet(SearchSourceBuilder searchSourceBuilder){
        searchSourceBuilder
                .aggregation(
                        AggregationBuilders.nested("facets", "references")
                                .subAggregation(
                                        AggregationBuilders.terms("facetCount")
                                                .size(Integer.MAX_VALUE)
                                                .field("references.referencedEntity"))
                );
    }


    @Data
    @RequiredArgsConstructor
    public static class FacetGroups {
        private final Set<Ref> conjunctionGroups;
        private final Set<Ref> disjunctionGroups;
        private final Set<Ref> negatedGroups;
    }

    @Data
    public static class Ref implements Comparable<Ref> {
        private final Serializable entityType;
        private final Integer id;

        @Override
        public int compareTo(Ref o) {
            int first = entityType.toString().compareTo(o.entityType.toString());
            if (first == 0){
                first = ofNullable(id).map(it -> ofNullable(o.id).map(it::compareTo).orElse(-1))
                        .orElseGet(() -> o.id != null ? 1 : 0);
            }
            return first;
        }
    }

}
