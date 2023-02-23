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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.EsEntityCollection;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.api.io.extraResult.AttributeHistogram;
import io.evitadb.api.io.extraResult.FacetSummary;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.io.extraResult.HistogramContract.Bucket;
import io.evitadb.api.io.extraResult.PriceHistogram;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.filter.UserFilter;
import io.evitadb.api.query.require.FacetStatisticsDepth;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.query.require.UseOfPrice;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.exception.EvitaFacetComputationCheckedException;
import io.evitadb.storage.exception.EvitaGetException;
import io.evitadb.storage.model.FacetReferenceDto;
import io.evitadb.storage.query.EsQueryTranslator;
import io.evitadb.storage.query.PriceFiller;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.utils.FacetUtil;
import io.evitadb.storage.utils.StringUtils;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryConstraints.and;
import static io.evitadb.api.query.QueryConstraints.facet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
@CommonsLog
public class StatisticsComputer {
    public static final String FACETS = "facets";
    public static final String FACET_COUNT = "facetCount";
    public static final String EXC_MESSAGE_PREFIX = "Cannot get histogram query: ";
    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;
    private final EsEntityCollection esEntityCollection;
    private final String indexName;

    public FacetSummary computeFacetSummary(io.evitadb.api.query.require.FacetSummary facetSummary, EsEvitaRequest evitaRequest, EntitySchema schema, EsEntityCollection.EsSearchResult result) throws IOException {
        int totalCount = result.getTotalCount();
        Query query = evitaRequest.getQuery();
        FacetUtil.FacetGroups facetGroups = FacetUtil.getFacetGroupsFromQuery(query);
        Set<String> negatedGroups = facetGroups
                .getNegatedGroups()
                .stream()
                .map(i -> StringUtils.getUI(i.getEntityType(), i.getId()))
                .collect(Collectors.toSet());
        Set<String> disjunctionGroups = facetGroups
                .getDisjunctionGroups()
                .stream()
                .map(i -> StringUtils.getUI(i.getEntityType(), i.getId()))
                .collect(Collectors.toSet());
        UserFilter userFilter = QueryUtils.findFilter(query, UserFilter.class);
        Set<String> requestedFacets = ofNullable(userFilter)
                .map(FacetUtil::getRequestedFacets)
                .stream()
                .flatMap(Collection::stream)
                .map(i -> StringUtils.getUI(i.getEntityType(), i.getId()))
                .collect(Collectors.toSet());


        boolean computeImpact = facetSummary.getFacetStatisticsDepth().equals(FacetStatisticsDepth.IMPACT);
        SearchResponse searchResponse = client.search(
                getQueryForReferences(query, filterBy -> filterBy, FilterMode.BASELINE, evitaRequest, schema, true, indexName, objectMapper, client),
                RequestOptions.DEFAULT);

        int totalHits = ofNullable(searchResponse.getHits().getTotalHits()).map(i->(int)i.value).orElse(0);
        Map<String, Long> buckets = ((ParsedTerms) ((ParsedNested) searchResponse.getAggregations().get(FACETS))
                .getAggregations()
                .get(FACET_COUNT))
                .getBuckets()
                .stream()
                .collect(
                        toMap(
                                MultiBucketsAggregation.Bucket::getKeyAsString,
                                MultiBucketsAggregation.Bucket::getDocCount
                        )
                );

        Map<Serializable, FacetReferenceDto> cachedReferences = FacetUtil.getOrComputeCachedReferences(client, indexName, objectMapper);


        Map<Object, List<Tuple<FacetReferenceDto, Long>>> countMap = cachedReferences
                .values()
                .stream()
                .map(i -> new Tuple<>(i, buckets.get(StringUtils.getUI(i.getType(), i.getPrimaryKey()))))
                // remove with no impact (none of requested product has the facet)
                .filter(i -> i.v2() != null)
                .collect(groupingBy(i -> i.v1().getGroupId()));


        // now we have all the data we need, compute stats
        Set<CustomFacetStatistics> loadFacetStats = new HashSet<>();
        List<FacetSummary.FacetGroupStatistics> collect = cachedReferences
                .values()
                .stream()
                .map(i -> new Ref(ofNullable(i.getGroupType()).orElse(i.getType()), i.getGroupPrimaryKey(), i.getGroupId()))
                .distinct()
                .map(i -> {
                    Serializable type = i.getType();
                    Integer pk = i.getPk();
                    boolean isGroupNegated = negatedGroups.contains(StringUtils.getUI(type, pk));
                    Map<Integer, FacetSummary.FacetStatistics> map = new HashMap<>();
                    ofNullable(countMap.get(i.getGroupId()))
                            .orElse(Collections.emptyList())
                            .forEach(l -> {
                                FacetReferenceDto referencedEntity = l.v1();
                                int facetId = referencedEntity.getPrimaryKey();
                                int preComputedCount = l.v2().intValue();


                                CustomFacetStatistics facetStatistics = new CustomFacetStatistics(
                                        facetId,
                                        referencedEntity.getType(),
                                        requestedFacets.contains(StringUtils.getUI(referencedEntity.getType(), facetId)),
                                        isGroupNegated ? totalHits - preComputedCount : preComputedCount,
                                        preComputedCount,
                                        null,
                                        referencedEntity);

                                // do not count impact here, this will result in many and many reqs.
                                if (computeImpact) {
                                    loadFacetStats.add(facetStatistics);
                                }

                                map.put(
                                        facetId,
                                        facetStatistics);
                            });


                    return new FacetSummary.FacetGroupStatistics(type, pk, map);
                }).collect(Collectors.toList());

        // compute impact
        Map<FacetSummary.FacetStatistics, FacetSummary.RequestImpact> impactResults = new HashMap<>();
        if (!loadFacetStats.isEmpty()) {
            computeImpact(loadFacetStats, userFilter, cachedReferences, disjunctionGroups, negatedGroups, evitaRequest, schema, totalCount, impactResults, result);
        }

        collect = collect
                .stream()
                .map(i ->
                        new FacetSummary.FacetGroupStatistics(
                                i.getFacetType(),
                                i.getGroupId(),
                                i.getFacetStatistics()
                                        .stream()
                                        .map(l -> new FacetSummary.FacetStatistics(
                                                l.getFacetId(),
                                                l.isRequested(),
                                                l.getCount(),
                                                impactResults.get(l)))
                                        .collect(Collectors.toList())
                        )
                ).collect(Collectors.toList());

        return new FacetSummary(collect);
    }

    public AttributeHistogram computeAttributeHistogram(EsEvitaRequest evitaRequest) {
        Map<String, io.evitadb.api.io.extraResult.HistogramContract> histograms = new HashMap<>();
        io.evitadb.api.query.require.AttributeHistogram attributeHistogram = QueryUtils.findRequire(evitaRequest.getQuery(), io.evitadb.api.query.require.AttributeHistogram.class);
        if (attributeHistogram != null) {

            EsEntityCollection.EsSearchResult baseline;


            for (String attributeName : attributeHistogram.getAttributeNames()) {
                try {
                    baseline = esEntityCollection.searchElastic(evitaRequest, false, FilterMode.ATTRIBUTE_STATS, attributeName);
                } catch (IOException e) {
                    throw new EvitaGetException(EXC_MESSAGE_PREFIX + e.getMessage(), e);
                }
                Aggregations aggregations = baseline.getSearchResponse().getAggregations();
                // could be null in any of gets, but it should be there, so if NPE occurs, should be found the reason
                ParsedStats stats = (ParsedStats) getAttrStats(aggregations, attributeName);
                double max = stats.getMax();
                double min = stats.getMin();
                double dispersion = max - min;
                double interval = Math.max(dispersion / (evitaRequest.getAttributeHistogramBuckets()), 0.1);
                evitaRequest.getAttributeHistogramIntervals().put(attributeName, interval);
                evitaRequest.getAttributeHistogramMax().put(attributeName, max);
                evitaRequest.getAttributeHistogramMin().put(attributeName, min);
            }


            for (String attributeName : attributeHistogram.getAttributeNames()) {
                try {
                    baseline = esEntityCollection.searchElastic(evitaRequest, false, FilterMode.ATTRIBUTE_HISTOGRAM, attributeName);
                } catch (IOException e) {
                    throw new EvitaGetException(EXC_MESSAGE_PREFIX + e.getMessage(), e);
                }
                histograms.put(attributeName, computeHistogram(
                        getAttrStats(baseline.getSearchResponse().getAggregations(), attributeName),
                        evitaRequest.getAttributeHistogramMax().get(attributeName)));
            }
        }

        return new AttributeHistogram(histograms);
    }

    public PriceHistogram computePriceHistogram(EsEvitaRequest evitaRequest) {
        EsEntityCollection.EsSearchResult baseline;

        try {
            baseline = esEntityCollection.searchElastic(evitaRequest, false, FilterMode.PRICE_STATS);
        } catch (IOException e) {
            throw new EvitaGetException(EXC_MESSAGE_PREFIX + e.getMessage(), e);
        }

        ParsedStats priceStats = baseline.getSearchResponse().getAggregations().get("price_stats");
        if (priceStats.getCount() == 0) return null;


        double max = priceStats.getMax();
        double min = priceStats.getMin();
        double dispersion = max - min;
        double interval = dispersion / (evitaRequest.getPriceHistogramBuckets());
        evitaRequest.setPriceHistogramMax(max);
        evitaRequest.setPriceHistogramMin(min);

        try {
            evitaRequest.setPriceHistogramInterval(interval);
            baseline = esEntityCollection.searchElastic(evitaRequest, false, FilterMode.PRICE_HISTOGRAM);
        } catch (IOException e) {
            throw new EvitaGetException(EXC_MESSAGE_PREFIX + e.getMessage(), e);
        }

        Aggregation aggregation = ofNullable(baseline.getSearchResponse().getAggregations()).map(i -> i.getAsMap().get("price_histogram")).orElse(null);
        if (aggregation == null) return null;

        io.evitadb.api.io.extraResult.Histogram histogram = computeHistogram(
                aggregation,
                evitaRequest.getPriceHistogramMax());
        if (histogram == null) return null;

        return new PriceHistogram(histogram);
    }

    public static List<FacetReferenceDto> getReferencesFromResponse(SearchResponse searchResponse, ObjectMapper objectMapper) {

        return Arrays.stream(searchResponse.getHits().getHits())
                .distinct()
                .map(i -> {
                    try {
                        return objectMapper.readValue(i.getSourceAsString(), FacetReferenceDto.class);
                    } catch (JsonProcessingException e) {
                        throw new EvitaGetException(e.getMessage(), e);
                    }
                })
                .collect(Collectors.toList());
    }


    private void computeImpact(Set<CustomFacetStatistics> statistics, UserFilter userFilter, Map<Serializable, FacetReferenceDto> cachedReferences, Set<String> disjunctionGroups, Set<String> negatedGroups, EsEvitaRequest evitaRequest, EntitySchema schema, int totalCount, Map<FacetSummary.FacetStatistics, FacetSummary.RequestImpact> impactResults, EsEntityCollection.EsSearchResult result) throws IOException {
        LinkedList<MultiSearchResponse.Item> responsesList = new LinkedList<>();

        Query query = evitaRequest.getQuery();
        Set<String> groupsOfRequestedFacets;

        if (userFilter != null) {
            groupsOfRequestedFacets = FacetUtil.getRequestedFacets(userFilter)
                    .stream()
                    .map(l -> cachedReferences.get(StringUtils.getUI(l.getEntityType(), l.getId())))
                    .filter(Objects::nonNull)
                    .map(i -> StringUtils.getUI(i.getGroupType(), i.getGroupPrimaryKey()))
                    .collect(toSet());
        } else {
            groupsOfRequestedFacets = Collections.emptySet();
        }
        List<CustomFacetStatistics> needsToBeComputed = new LinkedList<>();
        List<CustomFacetStatistics> doNotCompute = new LinkedList<>();
        statistics
                .forEach(i -> {
                    FacetReferenceDto reference = i.getReferenceDto();
                    String ui = StringUtils.getUI(reference.getGroupType(), reference.getGroupPrimaryKey());
                    if (groupsOfRequestedFacets.contains(ui) || disjunctionGroups.contains(ui) || negatedGroups.contains(ui)) {
                        needsToBeComputed.add(i);
                    } else {
                        doNotCompute.add(i);
                    }
                });

        // for relations like 'or' with groups and 'and' within group we need to really compute counts in elastic
        // it is theoretically possible to be computed using info from computeFacetSummary method, but impact will not be high because of the count in lists
        // or count be optimized to only request one per group maybe?
        for (int i = 0; i < needsToBeComputed.size(); i += 30) {
            MultiSearchRequest request = new MultiSearchRequest();
            needsToBeComputed
                    .stream()
                    .skip(i)
                    .limit(30)
                    .forEach(k -> request.add(getQueryForImpact(query, k.getFacetId(), k.getType(), evitaRequest, schema)));

            responsesList.addAll(Arrays.stream(client.msearch(request, RequestOptions.DEFAULT).getResponses()).collect(Collectors.toList()));
        }

        for (int i = 0; i < responsesList.size(); i++) {
            CustomFacetStatistics customFacetStatistics = needsToBeComputed.get(i);
            MultiSearchResponse.Item response = responsesList.get(i);
            int countNow = ofNullable(response.getResponse()).map(SearchResponse::getHits).map(SearchHits::getTotalHits).map(l->l.value).orElse(0L).intValue();
            FacetSummary.RequestImpact requestImpact = new FacetSummary.RequestImpact(countNow - totalCount, countNow);
            impactResults.put(customFacetStatistics, requestImpact);
        }

        List<ParsedStringTerms.ParsedBucket> impactBuckets;
        try {
            impactBuckets = (List<ParsedStringTerms.ParsedBucket>) ((ParsedTerms) ((ParsedNested) result.getSearchResponse().getAggregations().get(FACETS))
                    .getAggregations()
                    .get(FACET_COUNT))
                    .getBuckets();
        } catch (Exception e) {
            throw new EvitaGetException(e.getMessage(), e);
        }

        List<ParsedStringTerms.ParsedBucket> withoutDisjunctiveBuckets = Collections.emptyList();
        int totalWithoutDisjunctive = 0;
        if (!disjunctionGroups.isEmpty()) {

            try {
                SearchRequest queryForDisjunctive = getQueryForReferences(
                        query,
                        filterBy -> filterBy,
                        FilterMode.DISJUNCTIVE_FACETS_AND,
                        evitaRequest, schema, false, indexName, objectMapper, client);
                SearchResponse disjunctiveResult = client.search(queryForDisjunctive, RequestOptions.DEFAULT);
                withoutDisjunctiveBuckets = (List<ParsedStringTerms.ParsedBucket>) ((ParsedTerms) ((ParsedNested) disjunctiveResult.getAggregations().get(FACETS))
                        .getAggregations()
                        .get(FACET_COUNT))
                        .getBuckets();

                queryForDisjunctive = getQueryForReferences(
                        query,
                        filterBy -> filterBy,
                        FilterMode.DISJUNCTIVE_FACETS,
                        evitaRequest, schema, false, indexName, objectMapper, client);
                disjunctiveResult = client.search(queryForDisjunctive, RequestOptions.DEFAULT);
                totalWithoutDisjunctive = ofNullable(disjunctiveResult.getHits().getTotalHits()).map(l->l.value).orElse(0L).intValue();
            } catch (EvitaFacetComputationCheckedException e) {
                // checked
            }
        }

        if (impactBuckets != null) {
            List<ParsedStringTerms.ParsedBucket> finalWithoutDisjunctiveBuckets = withoutDisjunctiveBuckets;
            boolean hasDisjunctionResults = !disjunctionGroups.isEmpty() && !finalWithoutDisjunctiveBuckets.isEmpty();
            int finallyTotal = hasDisjunctionResults ? totalWithoutDisjunctive : totalCount;
            doNotCompute
                    .forEach(i -> {
                        String ui = StringUtils.getUI(i.getType(), i.getFacetId());
                        int count = impactBuckets
                                .stream()
                                .filter(k -> Objects.equals(k.getKeyAsString(), ui))
                                .findAny()
                                .map(parsedBucket -> (int) parsedBucket.getDocCount())
                                .orElse(0);
                        if (hasDisjunctionResults) {
                            count -= (int) finalWithoutDisjunctiveBuckets
                                    .stream()
                                    .filter(m -> Objects.equals(m.getKeyAsString(), ui))
                                    .mapToLong(ParsedMultiBucketAggregation.ParsedBucket::getDocCount)
                                    .sum();

                        }

                        int difference = count - finallyTotal;
                        FacetSummary.RequestImpact requestImpact = new FacetSummary.RequestImpact(difference, hasDisjunctionResults ? difference + totalCount : count);
                        impactResults.put(i, requestImpact);
                    });
        }

    }

    public static SearchRequest getQueryForReferences(
            Query query,
            UnaryOperator<FilterBy> filterCreator,
            FilterMode filterMode,
            EsEvitaRequest evitaRequest,
            EntitySchema schema,
            boolean trackTotalHints,
            String indexName,
            ObjectMapper objectMapper,
            RestHighLevelClient client) {


        ZonedDateTime now = evitaRequest.getAlignedNow();
        Locale locale = evitaRequest.getLanguage();

        QueryPriceMode priceMode = ofNullable(QueryUtils.findRequire(query, UseOfPrice.class)).map(UseOfPrice::getQueryPriceMode).orElse(QueryPriceMode.WITH_VAT);
        final EsQueryTranslator.FilterByVisitor filterByVisitor = new EsQueryTranslator.FilterByVisitor(now, filterMode, schema, locale, objectMapper, priceMode, query, client, null, indexName);
        FilterBy filterBy = filterCreator.apply(query.getFilterBy());
        ofNullable(filterBy).ifPresent(fb -> fb.accept(filterByVisitor));

        final EsQueryTranslator.PriceVisitor priceVisitor = new EsQueryTranslator.PriceVisitor(now, filterMode, schema, locale, objectMapper, priceMode, query, client);
        ofNullable(filterBy).ifPresent(fb -> fb.accept(priceVisitor));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder initQuery = boolQuery();
        BoolQueryBuilder visitorQuery = filterByVisitor.getEsQuery();
        if (visitorQuery != null) initQuery.must(filterByVisitor.getEsQuery());
        searchSourceBuilder.query(EsQueryTranslator.enhanceWithTypeConstraint(initQuery, priceVisitor.getEsQuery(), query));
        searchSourceBuilder.fetchSource(false);

        if (filterBy != null) new PriceFiller(query, priceMode, filterMode).apply(searchSourceBuilder);

        if (filterMode == FilterMode.DISJUNCTIVE_FACETS_AND || filterMode == FilterMode.BASELINE) {
            FacetUtil.prepareAggForFacet(searchSourceBuilder);
        }
        if (filterMode == FilterMode.HIERARCHY) {
            searchSourceBuilder
                    .aggregation(
                            AggregationBuilders.nested("hierarchy", "paths")
                                    .subAggregation(
                                            AggregationBuilders.terms("hierarchyCounts")
                                                    .size(Integer.MAX_VALUE)
                                                    .field("paths.ui"))
                    );
        }

        searchSourceBuilder.size(0);
        searchSourceBuilder.from(0);
        if (trackTotalHints) {
            searchSourceBuilder.trackTotalHitsUpTo(Integer.MAX_VALUE);
        }
        return new SearchRequest(indexName).source(searchSourceBuilder);
    }

    private UserFilter createNewUserFilter(int facetId, Serializable type, @Nullable UserFilter userFilter) {
        FilterConstraint[] children =
                Stream.concat(
                        Arrays.stream(ofNullable(userFilter).map(i -> userFilter.getConstraints()).orElse(new FilterConstraint[0])),
                        Stream.of(facet(type, facetId))
                ).toArray(FilterConstraint[]::new);
        return new FacetUserFilter(children);
    }

    private SearchRequest getQueryForImpact(Query query, int facetId, Serializable type, EsEvitaRequest evitaRequest, EntitySchema schema) {
        return getQueryForReferences(
                query,
                filterBy ->
                        (FilterBy) filterBy.getCopyWithNewChildren(
                                new FilterConstraint[]{
                                        and(
                                                Stream.concat(
                                                                Arrays.stream(filterBy.getConstraints()),
                                                                Stream.of(createNewUserFilter(facetId, type, QueryUtils.findFilter(filterBy, UserFilter.class))))
                                                        .toArray(FilterConstraint[]::new)
                                        )
                                }

                        ),
                FilterMode.BASELINE,
                evitaRequest, schema, false, indexName, objectMapper, client);

    }

    private ParsedAggregation getAttrStats(Aggregations aggregations, String attributeName) {
        return ((ParsedFilter) ((ParsedNested) aggregations.get(attributeName)).getAggregations().get("filter_" + attributeName)).getAggregations().get(attributeName);
    }

    private io.evitadb.api.io.extraResult.Histogram computeHistogram(Aggregation aggregation, Double max) {

        List<? extends Histogram.Bucket> returnedBucket = ((ParsedHistogram) aggregation).getBuckets();
        int size = returnedBucket.size();
        List<HistogramContract.Bucket> finalBuckets = new ArrayList<>(size);
        for (int i = 0; i < size - 1; i++) {
            ParsedMultiBucketAggregation.ParsedBucket bucket = (ParsedMultiBucketAggregation.ParsedBucket) returnedBucket.get(i);
            int docCount = (int) bucket.getDocCount();
            if (i == size - 2) docCount += (int) returnedBucket.get(i + 1).getDocCount();
            if (docCount != 0) {
                finalBuckets.add(new HistogramContract.Bucket(i, BigDecimal.valueOf((Double) bucket.getKey()), docCount));
            }

        }

        if (finalBuckets.isEmpty()) {
            return null;
        }

        return new io.evitadb.api.io.extraResult.Histogram(finalBuckets.toArray(new Bucket[0]), BigDecimal.valueOf(max));
    }

    public static class FacetUserFilter extends UserFilter {
        @Getter
        @Setter
        private Facet customFacet;

        public FacetUserFilter(FilterConstraint... children) {
            super(children);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            FacetUserFilter that = (FacetUserFilter) o;
            return Objects.equals(customFacet, that.customFacet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), customFacet);
        }
    }

    @AllArgsConstructor
    @Data
    private static class Ref {
        private Serializable type;
        private Integer pk;
        private String groupId;

    }

    public static class CustomFacetStatistics extends FacetSummary.FacetStatistics {

        @Getter
        private final int preComputedCount;
        @Getter
        private final Serializable type;
        @Getter
        private final transient FacetReferenceDto referenceDto;

        public CustomFacetStatistics(int facetId, Serializable type, boolean requested, int count, int preComputedCount, @Nullable FacetSummary.RequestImpact impact, FacetReferenceDto referenceDto) {
            super(facetId, requested, count, impact);
            this.preComputedCount = preComputedCount;
            this.type = type;
            this.referenceDto = referenceDto;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            CustomFacetStatistics that = (CustomFacetStatistics) o;
            return preComputedCount == that.preComputedCount && Objects.equals(type, that.type) && Objects.equals(referenceDto, that.referenceDto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), preComputedCount, type, referenceDto);
        }
    }
}
