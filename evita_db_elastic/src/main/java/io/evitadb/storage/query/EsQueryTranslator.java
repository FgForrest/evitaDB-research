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

package io.evitadb.storage.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.api.query.*;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.order.Random;
import io.evitadb.api.query.order.*;
import io.evitadb.api.query.require.*;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.storage.query.filter.*;
import io.evitadb.storage.query.filter.attribute.*;
import io.evitadb.storage.query.filter.facet.FacetTranslator;
import io.evitadb.storage.query.filter.facet.UserFilterTranslator;
import io.evitadb.storage.query.filter.hierarchy.*;
import io.evitadb.storage.query.filter.price.*;
import io.evitadb.storage.query.order.*;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.statistics.StatisticsComputer;
import io.evitadb.storage.utils.FacetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;

import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@CommonsLog
public class EsQueryTranslator {

    private EsQueryTranslator() {
    }

    public static SearchSourceBuilder translateQuery(@Nonnull ZonedDateTime now, @Nonnull EntitySchema entitySchema, EsEvitaRequest evitaRequest, ObjectMapper objectMapper, Boolean respectPage, FilterMode filterMode, RestHighLevelClient client, String forbiddenAttribute, String indexName) {
        Locale locale = evitaRequest.getLanguage();
        Query query = evitaRequest.getQuery();

        QueryPriceMode priceMode = ofNullable(QueryUtils.findRequire(query, UseOfPrice.class)).map(UseOfPrice::getQueryPriceMode).orElse(QueryPriceMode.WITH_VAT);
        final FilterByVisitor filterByVisitor = new FilterByVisitor(now, filterMode, entitySchema, locale, objectMapper, priceMode, query, client,forbiddenAttribute,indexName);
        FilterBy filterBy = query.getFilterBy();
        ofNullable(filterBy).ifPresent(fb -> fb.accept(filterByVisitor));

        final PriceVisitor priceVisitor = new PriceVisitor(now, filterMode, entitySchema, locale, objectMapper, priceMode, query, client);
        ofNullable(filterBy).ifPresent(fb -> fb.accept(priceVisitor));

        PriceInPriceLists priceInPriceLists = ofNullable(filterBy)
                .map(fb -> QueryUtils.findFilter(fb, PriceInPriceLists.class))
                .orElse(null);

        final OrderByVisitor orderByVisitor = new OrderByVisitor(entitySchema, filterMode, priceInPriceLists, priceMode);
        ofNullable(query.getOrderBy()).ifPresent(orderBy -> orderBy.accept(orderByVisitor));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        PriceHistogram priceHistogram = QueryUtils.findRequire(query, PriceHistogram.class);
        AttributeHistogram attributeHistogram = QueryUtils.findRequire(query, AttributeHistogram.class);
        if (priceHistogram != null) {
            if (filterMode.equals(FilterMode.PRICE_HISTOGRAM) && evitaRequest.getPriceHistogramInterval() != 0) {
                searchSourceBuilder
                        .aggregation(AggregationBuilders.histogram("price_histogram")
                                .interval(evitaRequest.getPriceHistogramInterval())
                                .offset(evitaRequest.getPriceHistogramMin())
                                .field("finalPrice")
                        );
            } else if (filterMode.equals(FilterMode.PRICE_STATS)) {
                searchSourceBuilder.aggregation(AggregationBuilders.stats("price_stats").field("finalPrice"));
            }
        }
        if (attributeHistogram != null) {
            if (filterMode.equals(FilterMode.ATTRIBUTE_HISTOGRAM)) {
                Double interval = evitaRequest.getAttributeHistogramIntervals().get(forbiddenAttribute);
                searchSourceBuilder
                        .aggregation(
                                AggregationBuilders.nested(forbiddenAttribute, "attribute")
                                        .subAggregation(
                                                AggregationBuilders.filter("filter_" + forbiddenAttribute,
                                                        new BoolQueryBuilder()
                                                                .filter(matchQuery("attribute.attributeName", forbiddenAttribute))
                                                ).subAggregation(
                                                        AggregationBuilders.histogram(forbiddenAttribute)
                                                                .interval(interval <= 0 ? 1 : interval)
                                                                .offset(evitaRequest.getAttributeHistogramMin().get(forbiddenAttribute))
                                                                .field("attribute.numberValue")
                                                ))
                        );
            } else if (filterMode.equals(FilterMode.ATTRIBUTE_STATS)) {
                for (String attributeName : attributeHistogram.getAttributeNames()) {
                    searchSourceBuilder
                            .aggregation(
                                    AggregationBuilders.nested(attributeName, "attribute")
                                            .subAggregation(
                                                    AggregationBuilders.filter("filter_" + attributeName,
                                                                    new BoolQueryBuilder()
                                                                            .filter(matchQuery("attribute.attributeName", attributeName))
                                                            )
                                                            .subAggregation(
                                                                    AggregationBuilders.stats(attributeName)
                                                                            .field("attribute.numberValue")
                                                            ))
                            );
                }
            }
        }

        FacetSummary facetSummary = evitaRequest.getFacetSummary();
        if (filterByVisitor.getFilterMode() == FilterMode.DISJUNCTIVE_FACETS_AND || filterByVisitor.getFilterMode() == FilterMode.DISJUNCTIVE_FACETS || (facetSummary != null && Objects.equals(facetSummary.getFacetStatisticsDepth(), FacetStatisticsDepth.IMPACT))) {
            FacetUtil.prepareAggForFacet(searchSourceBuilder);
        }

        searchSourceBuilder.query(enhanceWithTypeConstraint(filterByVisitor.getEsQuery(), priceVisitor.getEsQuery(), query));
        searchSourceBuilder.fetchSource(false);

        if (filterBy != null) new PriceFiller(query, priceMode, filterMode).apply(searchSourceBuilder);

        if (filterMode.equals(FilterMode.DEFAULT)) {
            Page page = QueryUtils.findRequire(query, Page.class);
            if (page != null && respectPage) {
                searchSourceBuilder.size(Math.min(page.getPageSize(), 10000));
                int from = (page.getPageNumber() - 1) * page.getPageSize();
                searchSourceBuilder.from( from > 10000*1000 ? 0 : from);
            } else if (page != null) {
                searchSourceBuilder.size(Math.min(page.getPageSize(), 10000));
                searchSourceBuilder.from(0);
            } else {
                searchSourceBuilder.size(10000);
                searchSourceBuilder.from(0);

            }
        } else {
            searchSourceBuilder.size(0);
            searchSourceBuilder.from(0);
        }

        searchSourceBuilder.sort(orderByVisitor.getEsOrdering());
        return searchSourceBuilder;
    }

    @Getter
    private abstract static class AbstractConstraintVisitor<S,
            C extends Constraint<C>,
            T extends BiFunction<? extends C, S, X>,
            X> implements ConstraintVisitor<C> {

        protected static final Map<Class<? extends Constraint<?>>, BiFunction<? extends Constraint<?>, ? extends ConstraintVisitor<? extends Constraint<?>>, ?>> CONSTRAINT_TRANSLATORS = new HashMap<>();

        protected final EntitySchema entitySchema;
        private final FilterMode filterMode;
        protected ReferenceSchema referenceSchema;
        @Setter
        protected PriceInPriceLists priceInPriceListsConstraint;
        protected QueryPriceMode priceMode;

        protected final Deque<List<X>> esConstraintStack = new ArrayDeque<>();

        protected AbstractConstraintVisitor(EntitySchema entitySchema, FilterMode filterMode) {
            this.entitySchema = entitySchema;
            this.filterMode = filterMode;
            esConstraintStack.push(new LinkedList<>());
        }

        @SuppressWarnings("unchecked")
        protected T getConstraintTranslator(C constraint) {
            return (T) ofNullable(CONSTRAINT_TRANSLATORS
                    .get(constraint.getClass()))
                    .orElseThrow(() -> new IllegalStateException("Could not find translator for constraint \"" + constraint.getName() + "\"."));
        }

        public List<X> getCurrentLevelConstraints() {
            return esConstraintStack.peek();
        }

        public String getPathPrefix() {
            return referenceSchema != null ? ReferenceHavingAttributeTranslator.REFERENCES_PREFIX + "." : "";
        }
    }

    @Getter
    public static class FilterByVisitor extends AbstractConstraintVisitor<FilterByVisitor, FilterConstraint, FilterConstraintTranslator<? extends FilterConstraint>, EsConstraint> {

        private static final Map<Class<? extends FilterConstraint>, FilterConstraintTranslator<? extends FilterConstraint>> CONSTRAINT_TRANSLATORS = new HashMap<>();

        static {
            CONSTRAINT_TRANSLATORS.put(FilterBy.class, new FilterByTranslator());
            CONSTRAINT_TRANSLATORS.put(And.class, new AndTranslator());
            CONSTRAINT_TRANSLATORS.put(Or.class, new OrTranslator());
            CONSTRAINT_TRANSLATORS.put(Not.class, new NotTranslator());
            CONSTRAINT_TRANSLATORS.put(PrimaryKey.class, new PrimaryKeyTranslator());
            CONSTRAINT_TRANSLATORS.put(Equals.class, new EqualsTranslator());
            CONSTRAINT_TRANSLATORS.put(GreaterThan.class, new GreaterThanTranslator());
            CONSTRAINT_TRANSLATORS.put(GreaterThanEquals.class, new GreaterThanEqualsTranslator());
            CONSTRAINT_TRANSLATORS.put(LessThan.class, new LessThanTranslator());
            CONSTRAINT_TRANSLATORS.put(LessThanEquals.class, new LessThanEqualsTranslator());
            CONSTRAINT_TRANSLATORS.put(Between.class, new BetweenTranslator());
            CONSTRAINT_TRANSLATORS.put(InSet.class, new InSetTranslator());
            CONSTRAINT_TRANSLATORS.put(Contains.class, new ContainsTranslator());
            CONSTRAINT_TRANSLATORS.put(StartsWith.class, new StartsWithTranslator());
            CONSTRAINT_TRANSLATORS.put(EndsWith.class, new EndsWithTranslator());
            CONSTRAINT_TRANSLATORS.put(IsTrue.class, new IsTrueTranslator());
            CONSTRAINT_TRANSLATORS.put(IsFalse.class, new IsFalseTranslator());
            CONSTRAINT_TRANSLATORS.put(IsNull.class, new IsNullTranslator());
            CONSTRAINT_TRANSLATORS.put(IsNotNull.class, new IsNotNullTranslator());
            CONSTRAINT_TRANSLATORS.put(InRange.class, new InRangeTranslator());
            CONSTRAINT_TRANSLATORS.put(Language.class, new LanguageTranslator());
            CONSTRAINT_TRANSLATORS.put(ReferenceHavingAttribute.class, new ReferenceHavingAttributeTranslator());
            CONSTRAINT_TRANSLATORS.put(PriceInCurrency.class, new PriceInCurrencyTranslator());
            CONSTRAINT_TRANSLATORS.put(PriceInPriceLists.class, new PriceInPriceListsTranslator());
            CONSTRAINT_TRANSLATORS.put(PriceValidIn.class, new PriceValidInTranslator());
            CONSTRAINT_TRANSLATORS.put(PriceBetween.class, new PriceBetweenTranslator());
            CONSTRAINT_TRANSLATORS.put(DirectRelation.class, new DirectRelationTranslator());
            CONSTRAINT_TRANSLATORS.put(ExcludingRoot.class, new ExcludingRootTranslator());
            CONSTRAINT_TRANSLATORS.put(Excluding.class, new ExcludingTranslator());
            CONSTRAINT_TRANSLATORS.put(WithinHierarchy.class, new WithinHierarchyTranslator());
            CONSTRAINT_TRANSLATORS.put(WithinRootHierarchy.class, new WithinRootHierarchyTranslator());
            CONSTRAINT_TRANSLATORS.put(UserFilter.class, new UserFilterTranslator());
            CONSTRAINT_TRANSLATORS.put(StatisticsComputer.FacetUserFilter.class, new UserFilterTranslator());
            CONSTRAINT_TRANSLATORS.put(Facet.class, new FacetTranslator());
        }

        @Getter
        private final ZonedDateTime now;
        @Getter
        private final Locale locale;
        @Getter
        private final ObjectMapper objectMapper;
        @Getter
        private final RestHighLevelClient client;
        @Getter
        private final Query query;
        @Getter
        private final String forbiddenAttribute;
        @Getter
        private final String indexName;

        protected EsQuery esQuery = null;


        public FilterByVisitor(
                @Nonnull ZonedDateTime now,
                FilterMode filterMode,
                @Nonnull EntitySchema entitySchema,
                @Nullable Locale locale,
                ObjectMapper objectMapper,
                QueryPriceMode priceMode,
                @Nullable Query query,
                RestHighLevelClient client,
                String forbiddenAttribute,
                String indexName) {
            super(entitySchema, filterMode);
            this.priceInPriceListsConstraint = null;
            this.now = now;
            this.locale = locale;
            this.objectMapper = objectMapper;
            this.priceMode = priceMode;
            this.client = client;
            this.query = query;
            this.forbiddenAttribute = forbiddenAttribute;
            this.indexName = indexName;
        }

        @Override
        public void visit(@Nonnull FilterConstraint constraint) {
            @SuppressWarnings("unchecked") final FilterConstraintTranslator<FilterConstraint> translator = (FilterConstraintTranslator<FilterConstraint>) ofNullable(CONSTRAINT_TRANSLATORS
                    .get(constraint.getClass()))
                    .orElseThrow(() -> new IllegalStateException("Could not find translator for constraint \"" + constraint.getName() + "\"."));


            if (!canApply(translator, constraint)) return;
            final EsConstraint esConstraint;

            if (constraint instanceof ConstraintContainer) {
                @SuppressWarnings("unchecked") final ConstraintContainer<FilterConstraint> container = (ConstraintContainer<FilterConstraint>) constraint;

                // create new level for translated constraints
                esConstraintStack.push(new LinkedList<>());

                // set up context for reference attributes filtering
                if (container instanceof ReferenceHavingAttribute) {
                    if (referenceSchema != null) {
                        throw new IllegalStateException("There cannot be nested referenceHavingAttribute constraints.");
                    }
                    referenceSchema = entitySchema.getReference(((ReferenceHavingAttribute) container).getEntityType());
                }

                // translate inner constraints
                for (FilterConstraint innerConstraint : container.getConstraints()) {
                    innerConstraint.accept(this);
                }

                // discard context for reference attributes filtering
                if (container instanceof ReferenceHavingAttribute) {
                    referenceSchema = null;
                }

                esConstraint = translator.apply(constraint, this);

                esConstraintStack.pop();
            } else if (constraint instanceof ConstraintLeaf) {
                esConstraint = translator.apply(constraint, this);
            } else {
                throw new IllegalStateException("Unknown constraint type. Should never happen!");
            }


            if (esConstraint == null) {
                return;
            }

            if (constraint instanceof FilterBy) {
                esQuery = new EsQuery(esConstraint.getQueryBuilder());
            } else {
                getCurrentLevelConstraints().add(esConstraint);
            }
        }

        /**
         * @param constraint This value may be use for further computation in overriding classes
         */
        protected boolean canApply(FilterConstraintTranslator<?> innerConstraint, FilterConstraint constraint) {
            return !(innerConstraint instanceof AbstractPriceTranslator);
        }

        public BoolQueryBuilder getEsQuery() {
            return ofNullable(esQuery).map(EsQuery::getQueryBuilder).orElse(null);
        }
    }

    public static class PriceVisitor extends FilterByVisitor {

        public PriceVisitor(@Nonnull ZonedDateTime now, FilterMode filterMode, @Nonnull EntitySchema entitySchema, @Nullable Locale locale, ObjectMapper objectMapper, QueryPriceMode priceMode, Query query, RestHighLevelClient client) {
            super(now, filterMode, entitySchema, locale, objectMapper, priceMode, query, client,null, null);
        }

        @Override
        public BoolQueryBuilder getEsQuery() {
            if (super.getEsQuery() == null)
                return null;

            return boolQuery()
                    .filter(
                            nestedQuery("prices",
                                    super.getEsQuery(),
                                    ScoreMode.Max
                            )
                    );
        }

        @Override
        protected boolean canApply(FilterConstraintTranslator<?> translator, FilterConstraint constraint) {
            return translator instanceof AbstractPriceTranslator || (constraint instanceof ConstraintContainer && !(translator instanceof HierarchyTranslator || translator instanceof UserFilterTranslator));
        }
    }


    public static class OrderByVisitor extends AbstractConstraintVisitor<OrderByVisitor, OrderConstraint, OrderConstraintTranslator<? extends OrderConstraint>, EsOrderConstraint> {

        static {
            CONSTRAINT_TRANSLATORS.put(OrderBy.class, new OrderByTranslator());
            CONSTRAINT_TRANSLATORS.put(Ascending.class, new AscendingTranslator());
            CONSTRAINT_TRANSLATORS.put(Descending.class, new DescendingTranslator());
            CONSTRAINT_TRANSLATORS.put(ReferenceAttribute.class, new ReferenceAttributeTranslator());
            CONSTRAINT_TRANSLATORS.put(Random.class, new RandomTranslator());
            CONSTRAINT_TRANSLATORS.put(PriceAscending.class, new PriceAscendingTranslator());
            CONSTRAINT_TRANSLATORS.put(PriceDescending.class, new PriceDescendingTranslator());
        }

        private final List<SortBuilder<?>> esOrdering = new LinkedList<>();

        public OrderByVisitor(@Nonnull EntitySchema entitySchema, FilterMode filterMode, PriceInPriceLists priceVisitor, QueryPriceMode priceMode) {
            super(entitySchema, filterMode);
            this.priceInPriceListsConstraint = priceVisitor;
            this.priceMode = priceMode;
        }

        @Override
        public void visit(@Nonnull OrderConstraint constraint) {
            @SuppressWarnings("unchecked") final OrderConstraintTranslator<OrderConstraint> translator = (OrderConstraintTranslator<OrderConstraint>) getConstraintTranslator(constraint);

            final EsOrderConstraint esConstraint;
            if (constraint instanceof ConstraintContainer) {
                @SuppressWarnings("unchecked") final ConstraintContainer<OrderConstraint> container = (ConstraintContainer<OrderConstraint>) constraint;

                // create new level for translated constraints
                esConstraintStack.push(new LinkedList<>());

                // set up context for reference attributes ordering
                if (container instanceof ReferenceAttribute) {//slightly different
                    if (referenceSchema != null) {
                        throw new IllegalStateException("There cannot be nested referenceAttribute constraints.");
                    }
                    referenceSchema = super.entitySchema.getReference(((ReferenceAttribute) container).getEntityType());
                }

                for (OrderConstraint innerConstraint : container.getConstraints()) {
                    innerConstraint.accept(this);
                }

                // discard context for reference attributes ordering
                if (container instanceof ReferenceAttribute) {
                    referenceSchema = null;
                }

                esConstraint = translator.apply(constraint, this);

                esConstraintStack.pop();
            } else if (constraint instanceof ConstraintLeaf) {
                esConstraint = translator.apply(constraint, this);
            } else {
                throw new IllegalStateException("Unknown constraint type. Should never happen!");
            }
            if (constraint instanceof OrderBy && esConstraint != null) {
                esOrdering.addAll(esConstraint.getSortBuilders());
            } else if (esConstraint != null) {
                getCurrentLevelConstraints().add(esConstraint);
            }
        }

        public List<SortBuilder<?>> getEsOrdering() {
            if (esOrdering.isEmpty()) {
                return Collections.singletonList(new FieldSortBuilder("primaryKey"));
            } else {
                esOrdering.add(new FieldSortBuilder("primaryKey"));
                return esOrdering;
            }
        }
    }

    public static BoolQueryBuilder enhanceWithTypeConstraint(BoolQueryBuilder esQuery, BoolQueryBuilder priceVisitorEsQuery, Query query) {
        BoolQueryBuilder finalQuery = esQuery;
        Object entityType = query.getEntities().getEntityType();
        if (entityType != null) {

            if (esQuery == null){
                esQuery = Objects.requireNonNullElseGet(priceVisitorEsQuery, QueryBuilders::boolQuery);
            }else {
                if (priceVisitorEsQuery != null){
                    esQuery.must(priceVisitorEsQuery);
                }
            }

            finalQuery = TypeTranslator.enhanceWithType(esQuery, entityType);

        }
        return finalQuery;
    }


}
