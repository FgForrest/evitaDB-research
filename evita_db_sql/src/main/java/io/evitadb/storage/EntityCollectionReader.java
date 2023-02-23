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

package io.evitadb.storage;

import io.evitadb.api.SqlEntityCollection;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.api.io.extraResult.AttributeHistogram;
import io.evitadb.api.io.extraResult.FacetSummary;
import io.evitadb.api.io.extraResult.HierarchyStatistics;
import io.evitadb.api.io.extraResult.Parents;
import io.evitadb.api.io.extraResult.PriceHistogram;
import io.evitadb.api.io.extraResult.*;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.require.*;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlWithClause;
import io.evitadb.storage.query.builder.facet.*;
import io.evitadb.storage.query.translate.ConstraintRemovingVisitor;
import io.evitadb.storage.query.translate.filter.FilterConstraintTranslatingVisitor;
import io.evitadb.storage.query.translate.filter.PriceFilterConstraintsTranslatingVisitor;
import io.evitadb.storage.query.translate.order.OrderConstraintTranslatingVisitor;
import io.evitadb.storage.serialization.sql.*;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.evitadb.api.query.QueryUtils.findFilter;
import static io.evitadb.api.query.QueryUtils.findRequires;
import static io.evitadb.api.utils.Assert.notNull;
import static io.evitadb.storage.query.builder.AttributeHistogramSqlQueryBuilder.buildAttributeHistogramQuery;
import static io.evitadb.storage.query.builder.EntityQueryBuilder.*;
import static io.evitadb.storage.query.builder.HierarchyStatisticsQueryBuilder.buildHierarchyStatisticsQuery;
import static io.evitadb.storage.query.builder.ParentsQueryBuilder.buildParentsQuery;
import static io.evitadb.storage.query.builder.PriceHistogramSqlQueryBuilder.buildPriceHistogramQuery;
import static io.evitadb.storage.query.builder.TempTableBuilder.buildCreateTempTableSql;
import static io.evitadb.storage.query.builder.facet.FacetSqlQueryBuilder.*;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * <p>Reader for {@link SqlEntityCollection}'s SQL persistence storage. Contains all collection read operations</p>

 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
public class EntityCollectionReader {

    /**
     * Temporary table used for storing intermediate results of baseline query to provide way to query different information
     * based on user filter constraint modification on those entities.
     */
    private static final String QUERY_TEMP_TABLE_NAME_PREFIX = "t_entityTemp_";
    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    // repository state and configuration
    private final EntityCollectionContext ctx;
    private final String serializedEntityType;

    // serialization support
    private final EntitySchemaRowMapper entitySchemaRowMapper;
    private final EntityReferenceRowMapper entityReferenceRowMapper;
    private final EntityRowMapper entityRowMapper;
    private final AllPossibleFacetsExtractor allPossibleFacetsExtractor;


    /**
     * Creates new storage handler for entity collection
     *
     * @param entityCollectionContext context including all necessary things to operate collection repository, such as db connection, schema, entityType
     */
    public EntityCollectionReader(@Nonnull EntityCollectionContext entityCollectionContext) {
        notNull(entityCollectionContext, "Database connection is required.");

        this.ctx = entityCollectionContext;

        this.serializedEntityType = ctx.getSerializedEntityType();
        this.entitySchemaRowMapper = new EntitySchemaRowMapper(entityCollectionContext);
        this.entityReferenceRowMapper = new EntityReferenceRowMapper(entityCollectionContext.getEntityType());
        this.entityRowMapper = new EntityRowMapper(entityCollectionContext);
        this.allPossibleFacetsExtractor = new AllPossibleFacetsExtractor();
    }


    /**
     * Tries to find fully loaded entity by its primary key
     *
     * @param primaryKey PK of search entity
     * @return found entity by PK or empty
     */
    public Optional<Entity> findEntityByPrimaryKey(int primaryKey) {
        final SqlPart query = buildFindEntityByPKQuery(serializedEntityType, primaryKey);

        try {
            return ofNullable(ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                    query.getSql().toString(),
                    entityRowMapper,
                    query.getArgs().toArray()
            ));
        } catch (EmptyResultDataAccessException ex) {
            return empty();
        }
    }

    /**
     * Finds entities completely by request. Note: if run in complex content it fetches entities from cached results.
     *
     * @param request request to find and load entities by
     * @return page of entities
     */
    public <E extends Serializable, C extends DataChunk<E>> C findEntitiesByRequest(@Nonnull SqlEvitaRequest request) {
        if (ComplexEntityQueryContextHolder.hasContext()) {
            return findEntitiesByRequestFromTempTable(request);
        } else {
            return findEntitiesByRequestDirectly(request);
        }
    }

    /**
     * Executes {@code executable} inside context allowing to fetch more than list of entities (such as histograms).
     * It does by pre-fetching entities by baseline part of query and caching them for later usage.
     *
     * @param request request
     * @param executable set of different fetch calls
     * @return executable result
     */
    public <T> T fetchComplex(@Nonnull SqlEvitaRequest request, @Nonnull Supplier<T> executable) {
        return ComplexEntityQueryContextHolder.executeWithinContext(() -> {
            final TransactionTemplate transactionTemplate = new TransactionTemplate(ctx.getCatalogCtx().getTransactionManager());
            return transactionTemplate.execute(s -> {
                prefetchEntities(request);
                return executable.get();
            });
        });
    }

    /**
     * Finds entity schema for this instance entity collection type.
     *
     * @return found schema or empty
     */
    public Optional<EntitySchema> findSchema() {
        try {
            return Optional.ofNullable(ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                    "select detail, serializationHeader from t_schema where entityType = ?",
                    entitySchemaRowMapper,
                    serializedEntityType
            ));
        } catch (IncorrectResultSizeDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * @return count of all entities stored in db
     */
    @SuppressWarnings("ConstantConditions")
    public int countStoredEntities() {
        return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select count(*) from t_entity where dropped = false and type = ?",
                Integer.class,
                serializedEntityType
        );
    }

    /**
     * Computes facet summary for passed request on existing intermediate entity results.
     * Note: needs to be run withing {@link ComplexEntityQueryContext} as it uses cached results.
     *
     * @param request request to compute summary for
     * @return computed facet summary
     */
    public FacetSummary computeFacetSummary(@Nonnull SqlEvitaRequest request) {
        Assert.isTrue(ComplexEntityQueryContextHolder.hasContext(), "Cannot compute facet summary without complex context.");

        final Function<FacetComputationContext, FacetSummary> executable = context -> {
            // translate baseline part of query
            final AllPossibleFacets allPossibleFacets = findAllPossibleFacets(request);
            if (allPossibleFacets == null) {
                return new FacetSummary(List.of());
            }
            context.setAllPossibleFacets(allPossibleFacets);

            // if there is facet group negation - invert the facet counts
            if (!context.getNegatedGroups().isEmpty()) {
                context.recalculateFacetCountsInNegatedGroups(countTempTable());
            }

            Map<FacetReference, FacetSummary.RequestImpact> facetImpact = null;
            if (request.getFacetStatisticsDepth() == FacetStatisticsDepth.IMPACT) {
                // create facet conditions from query
                final List<FacetCondition> facetConditions = mapFacetsFromRequestToConditions(request);
                context.setFacetConditions(facetConditions);

                // count user filter count from combined user facet constraints for statistics reference
                final int userFilterCount = countUserFilterInTempTable();
                context.setUserFilterCount(userFilterCount);

                facetImpact = computeFacetImpact(context, allPossibleFacets);
            }

            // build final summary and statistics from prepared data
            return buildFacetSummary(request.getFacetStatisticsDepth(), facetImpact);
        };
        return FacetComputationContextHolder.executeWithinContext(
                ctx,
                findRequires(request.getQuery(), FacetGroupsConjunction.class),
                findRequires(request.getQuery(), FacetGroupsDisjunction.class),
                findRequires(request.getQuery(), FacetGroupsNegation.class),
                request.getQuery().getFilterBy(),
                executable
        );
    }

    /**
     * Computes attribute histograms from cached results in intermediate results table.
     * Note: needs to be run withing {@link ComplexEntityQueryContext} as it uses cached results.
     *
     * @param request request with metadata for computation
     * @return computed attribute histograms
     */
    public AttributeHistogram computeAttributeHistograms(@Nonnull SqlEvitaRequest request) {
        Assert.isTrue(ComplexEntityQueryContextHolder.hasContext(), "Cannot compute attribute histograms without complex context.");

        final Map<String, HistogramContract> histograms = new TreeMap<>();
        for (String attributeName : request.getRequiresAttributeHistogramsNames()) {
            final HistogramContract histogram = computeAttributeHistogram(request, attributeName);
            if (histogram != null) {
                histograms.put(attributeName, histogram);
            }
        }

        return new AttributeHistogram(histograms);
    }

    /**
     * Computes price histogram from cached results in intermediate results table
     * Note: needs to be run withing {@link ComplexEntityQueryContext} as it uses cached results.
     *
     * @param request request with metadata for computation
     * @return price histogram
     */
    public PriceHistogram computePriceHistogram(@Nonnull SqlEvitaRequest request) {
        Assert.isTrue(ComplexEntityQueryContextHolder.hasContext(), "Cannot compute price histogram without complex context.");

        SqlPart userFilterSqlPart = SqlPart.TRUE;

        final UserFilter userFilter = QueryUtils.findFilter(request.getQuery(), UserFilter.class);
        if (userFilter != null) {
            final Predicate<FilterConstraint> forbiddenPriceBetweenPredicate = c -> PriceBetween.class.isAssignableFrom(c.getClass());
            final ConstraintRemovingVisitor<FilterConstraint> removingVisitor = new ConstraintRemovingVisitor<>(forbiddenPriceBetweenPredicate);
            userFilter.accept(removingVisitor);
            final UserFilter filteredUserFilter = (UserFilter) removingVisitor.getModifiedTree();

            final FilterConstraintTranslatingVisitor userFilterTranslator = FilterConstraintTranslatingVisitor.withDefaultMode(ctx, request);
            filteredUserFilter.accept(userFilterTranslator);
            userFilterSqlPart = userFilterTranslator.getFinalWhereSqlPart();
        }

        final SqlPart query = buildPriceHistogramQuery(userFilterSqlPart, request.getRequiresPriceHistogramBuckets());
        return ctx.getCatalogCtx().getJdbcTemplate().query(
                query.getSql().toString(),
                new PriceHistogramExtractor(request),
                query.getArgs().toArray()
        );
    }

    /**
     * Finds parents of all entities in cached results in intermediate results table depending on request
     *
     * @param request request with metadata for finding
     * @return parents
     */
    public Parents findParents(@Nonnull SqlEvitaRequest request) {
        Assert.isTrue(ComplexEntityQueryContextHolder.hasContext(), "Cannot find parents without complex context.");

        final Map<Serializable, Parents.ParentsByType<?>> parents;
        if (request.getRequiredParents() != null) {
            parents = findAllParents(request);
        } else {
            parents = findSpecificParents(request);
        }

        return new Parents(parents);
    }

    /**
     * Computes tree of hierarchy items with cardinalities of their children.
     * Note: needs to be run withing {@link ComplexEntityQueryContext} as it uses cached results.
     *
     * @param request request with metadata for computing
     * @return hierarchy statistics
     */
    public HierarchyStatistics computeHierarchyStatistics(@Nonnull SqlEvitaRequest request) {
        Assert.isTrue(ComplexEntityQueryContextHolder.hasContext(), "Cannot compute hierarchy statistics without complex context.");

        final Map<Serializable, List<HierarchyStatistics.LevelInfo<?>>> hierarchyStatistics = new HashMap<>();

        final ConstraintRemovingVisitor<FilterConstraint> withinHierarchyForStatisticsTransformer = new ConstraintRemovingVisitor<>(
                c -> DirectRelation.class.isAssignableFrom(c.getClass())
        );

        for (io.evitadb.api.query.require.HierarchyStatistics requiredHierarchyStatistics : request.getRequiredHierarchyStatistics()) {
            final Serializable requiredHierarchyType = requiredHierarchyStatistics.getEntityType() != null ? requiredHierarchyStatistics.getEntityType() : request.getEntityType();
            final String serializedRequiredHierarchyType = ctx.getCatalogCtx().getSerializedEntityType(requiredHierarchyType);
            final EntityCollectionContext hierarchyEntityCollectionCtx = ctx.getCatalogCtx()
                    .getCatalog()
                    .getCollectionForEntity(requiredHierarchyType)
                    .getCtx();

            final FilterConstraintTranslatingVisitor withinHierarchyTranslator = FilterConstraintTranslatingVisitor.withBaselineMode(hierarchyEntityCollectionCtx, request);

            // build condition to found hierarchy entities only within specified subtree
            SqlPart whereSqlPart = null;
            SqlWithClause withClause = null;
            boolean inSubtree = false;
            boolean directRelation = false;
            final WithinHierarchy withinHierarchy = findFilter(request.getQuery(), WithinHierarchy.class);
            if (withinHierarchy != null) {
                // remove direct relation flag from original constraint as it would disable option to correctly calculate cardinalities for whole subtrees
                withinHierarchy.accept(withinHierarchyForStatisticsTransformer);
                final WithinHierarchy withinHierarchyForStatistics = (WithinHierarchy) withinHierarchyForStatisticsTransformer.getModifiedTree();

                // translate modified constraint
                withinHierarchyForStatistics.accept(withinHierarchyTranslator);
                whereSqlPart = withinHierarchyTranslator.getFinalWhereSqlPart();
                withClause = withinHierarchyTranslator.getFinalSqlWithClause();

                inSubtree = true;
                directRelation = withinHierarchy.isDirectRelation();
            } else {
                final WithinRootHierarchy withinRootHierarchy = findFilter(request.getQuery(), WithinRootHierarchy.class);
                if (withinRootHierarchy != null) {
                    // remove direct relation flag from original constraint as it would disable option to correctly calculate cardinalities for whole subtrees
                    withinRootHierarchy.accept(withinHierarchyForStatisticsTransformer);
                    final WithinRootHierarchy withinRootHierarchyForStatistics = (WithinRootHierarchy) withinHierarchyForStatisticsTransformer.getModifiedTree();

                    // translate modified constraint
                    withinRootHierarchyForStatistics.accept(withinHierarchyTranslator);
                    whereSqlPart = withinHierarchyTranslator.getFinalWhereSqlPart();
                    withClause = withinHierarchyTranslator.getFinalSqlWithClause();

                    directRelation = withinRootHierarchy.isDirectRelation();
                }
            }

            // decide what richness of entities is required
            final boolean requiresFullEntity = requiredHierarchyStatistics.getConstraints().length != 0;
            final Class<? extends Serializable> hierarchyEntityTargetClass = requiresFullEntity ? SealedEntity.class : Integer.class;
            EntityRowMapper hierarchyEntityRowMapper = null;
            if (requiresFullEntity) {
                hierarchyEntityRowMapper = new EntityRowMapper(hierarchyEntityCollectionCtx);
            }

            // query actual data
            final Serializable filterHierarchyType = request.getWithinHierarchyEntityType() != null ? request.getWithinHierarchyEntityType() : request.getEntityType();
            if (requiredHierarchyType.equals(filterHierarchyType)) {
                final SqlPart hierarchyStatisticsQuery = buildHierarchyStatisticsQuery(
                        request.getLanguage(),
                        withClause,
                        requiresFullEntity,
                        serializedEntityType,
                        serializedRequiredHierarchyType,
                        whereSqlPart,
                        inSubtree,
                        directRelation
                );

                final List<HierarchyStatistics.LevelInfo<?>> hierarchyStatisticsByType = ctx.getCatalogCtx().getJdbcTemplate().query(
                        hierarchyStatisticsQuery.getSql().toString(),
                        new HierarchyStatisticsExtractor<>(hierarchyEntityRowMapper, hierarchyEntityTargetClass),
                        hierarchyStatisticsQuery.getArgs().toArray()
                );
                hierarchyStatistics.put(requiredHierarchyType, hierarchyStatisticsByType);
            } else {
                final SqlPart hierarchyStatisticsQuery = buildHierarchyStatisticsQuery(
                        request.getLanguage(),
                        null,
                        requiresFullEntity,
                        serializedEntityType,
                        serializedRequiredHierarchyType,
                        null,
                        inSubtree,
                        false
                );
                final List<HierarchyStatistics.LevelInfo<?>> hierarchyStatisticsByType = ctx.getCatalogCtx().getJdbcTemplate().query(
                        hierarchyStatisticsQuery.getSql().toString(),
                        new HierarchyStatisticsExtractor<>(hierarchyEntityRowMapper, hierarchyEntityTargetClass),
                        hierarchyStatisticsQuery.getArgs().toArray()
                );
                hierarchyStatistics.put(requiredHierarchyType, hierarchyStatisticsByType);
            }
        }

        return new HierarchyStatistics(hierarchyStatistics);
    }


    /*
     * --- Entity fetching ---
     */

    /**
     * Finds entities completely by request directly from entity table.
     *
     * @param request request to find and load entities by
     * @return page of entities
     */
    private <E extends Serializable, C extends DataChunk<E>> C findEntitiesByRequestDirectly(SqlEvitaRequest request) {
        // translate main price filter constraints
        final PriceFilterConstraintsTranslatingVisitor priceTranslator = new PriceFilterConstraintsTranslatingVisitor();
        final SqlPart prices = priceTranslator.translate(ctx.getUid(), request);

        // translate filter constraints
        final FilterConstraintTranslatingVisitor filterTranslator;
        filterTranslator = FilterConstraintTranslatingVisitor.withDefaultMode(ctx, request);
        ofNullable(request.getQuery().getFilterBy()).ifPresent(filterBy -> filterBy.accept(filterTranslator));

        // translate order constraints
        final OrderConstraintTranslatingVisitor orderTranslator = new OrderConstraintTranslatingVisitor(
                ctx,
                request.getLanguage()
        );
        ofNullable(request.getQuery().getOrderBy()).ifPresent(orderBy -> orderBy.accept(orderTranslator));

        final SqlPart query = buildFindEntitiesByQueryDirectly(
                serializedEntityType,
                request,
                filterTranslator.getFinalSqlWithClause(),
                filterTranslator.getFinalWhereSqlPart(),
                prices,
                orderTranslator.getFinalSqlSortExpression()
        );
        return executeEntitiesQuery(request, query, request.getFirstRecordOffset(), request.getLimit());
    }

    /**
     * Finds entities by baseline part of request and stores them in temp. table to be later queried.
     * It is used when additional results like histograms are needed to save resources.
     *
     * To get main entities call {@link #findEntitiesByRequestFromTempTable(SqlEvitaRequest)}.
     *
     * @param request request to find and load entities by
     */
    private void prefetchEntities(@Nonnull SqlEvitaRequest request) {
        // translate baseline part of query
        final FilterConstraintTranslatingVisitor baselineTranslator = FilterConstraintTranslatingVisitor.withBaselineMode(ctx, request);
        ofNullable(request.getQuery().getFilterBy()).ifPresent(filterBy -> filterBy.accept(baselineTranslator));

        // translate prices part of query
        final PriceFilterConstraintsTranslatingVisitor pricesTranslator = new PriceFilterConstraintsTranslatingVisitor();
        final SqlPart prices = pricesTranslator.translate(ctx.getUid(), request);

        // query baseline entities and store them to temp. table to be able to work with them later
        ComplexEntityQueryContextHolder.getContext().setTempTableName(generateTempTableName());

        final SqlPart createTempTableSql = buildCreateTempTableSql(
                ctx,
                request,
                serializedEntityType,
                baselineTranslator.getFinalSqlWithClause(),
                prices,
                baselineTranslator.getFinalWhereSqlPart()
        );
        ctx.getCatalogCtx().getJdbcTemplate().update(
                createTempTableSql.getSql().toString(),
                createTempTableSql.getArgs().toArray()
        );
    }

    /**
     * Same as {@link #findEntitiesByRequest(SqlEvitaRequest)} but searches through results in temp. table created by
     * {@link #prefetchEntities(SqlEvitaRequest)}
     *
     * @param request request to find and load entities by
     * @return page of entities
     */
    private <E extends Serializable, C extends DataChunk<E>> C findEntitiesByRequestFromTempTable(@Nonnull SqlEvitaRequest request) {
        final FilterConstraintTranslatingVisitor userFilterTranslator = FilterConstraintTranslatingVisitor.withDefaultMode(ctx, request);

        final SqlPart where;
        final SqlWithClause with;
        final UserFilter userFilter = QueryUtils.findFilter(request.getQuery(), UserFilter.class);
        if (userFilter == null) {
            // fetch all entities from temp table
            where = SqlPart.TRUE;
            with = SqlWithClause.EMPTY;
        } else {
            // fetched filtered entities from temp table
            userFilter.accept(userFilterTranslator);
            where = userFilterTranslator.getFinalWhereSqlPart();
            with = userFilterTranslator.getFinalSqlWithClause();
        }

        final OrderConstraintTranslatingVisitor orderTranslator = new OrderConstraintTranslatingVisitor(
                ctx,
                request.getLanguage()
        );
        ofNullable(request.getQuery().getOrderBy()).ifPresent(orderBy -> orderBy.accept(orderTranslator));

        final SqlPart query = buildFindEntitiesByQueryFromTempTable(
                request,
                where,
                with,
                orderTranslator.getFinalSqlSortExpression()
        );
        return executeEntitiesQuery(request, query, request.getFirstRecordOffset(), request.getLimit());
    }

    /**
     * Executes built query for fetching entities. Expects query without limit and offset because it adds it by itself.
     * Also, if first attempt returns no results and offset is not 0, query is executed again with offset 0.
     */
    private <E extends Serializable, C extends DataChunk<E>> C executeEntitiesQuery(@Nonnull SqlEvitaRequest request,
                                                                                    @Nonnull SqlPart query,
                                                                                    int firstRecordOffset,
                                                                                    int recordsLimit) {
        final String builtQuery = query.getSql().toString() + " limit ? offset ?";

        final List<Object> args = new LinkedList<>(query.getArgs());
        args.add(recordsLimit);
        args.add(firstRecordOffset);

        //noinspection unchecked
        C result = (C) ctx.getCatalogCtx().getJdbcTemplate().query(
                builtQuery,
                new EntitiesExtractor<>(entityRowMapper, entityReferenceRowMapper, request),
                args.toArray()
        );
        // return first page of results if no results found
        if (result.isEmpty() && (firstRecordOffset > 0)) {
            result = executeEntitiesQuery(request, query, 0, recordsLimit);
        }
        return result;
    }


    /*
     * --- Facet summary fetching ---
     */

    private AllPossibleFacets findAllPossibleFacets(SqlEvitaRequest request) {
        final FilterConstraintTranslatingVisitor baselineTranslator = FilterConstraintTranslatingVisitor.withBaselineMode(ctx, request);
        ofNullable(request.getQuery().getFilterBy()).ifPresent(filterBy -> filterBy.accept(baselineTranslator));

        final SqlPart findAllPossibleFacetsQuery = buildFindAllPossibleFacetsQuery(ctx.getUid());
        return ctx.getCatalogCtx().getJdbcTemplate().query(
                findAllPossibleFacetsQuery.getSql().toString(),
                allPossibleFacetsExtractor,
                findAllPossibleFacetsQuery.getArgs().toArray()
        );
    }

    private Integer countTempTable() {
        return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select count(*) from " + ComplexEntityQueryContextHolder.getContext().getTempTableName(),
                Integer.class
        );
    }

    private List<FacetCondition> mapFacetsFromRequestToConditions(SqlEvitaRequest request) {
        final List<FacetCondition> facetConditions = new LinkedList<>();

        final FacetComputationContext context = FacetComputationContextHolder.getContext();
        final Map<Serializable, List<Integer>> facets = request.getSelectedFacets();
        for (Map.Entry<Serializable, List<Integer>> facet : facets.entrySet()) {
            final String serializedFacetType = STRING_TYPED_VALUE_SERIALIZER.serialize(facet.getKey()).getSerializedValue();

            final Map<GroupReference, List<Integer>> groupedFacets = facet.getValue().stream()
                    .collect(
                            groupingBy(
                                    facetId -> ofNullable(context.getGroupForFacet(new FacetReference(facet.getKey(), serializedFacetType, facetId, 0)))
                                            .orElse(new GroupReference(facet.getKey(), serializedFacetType, null))
                            )
                    );
            groupedFacets
                    .forEach((group, facetIdList) -> {
                        final int[] facetIds = facetIdList.stream().mapToInt(it -> it).toArray();
                        facetConditions.add(buildSummaryFacetConditionByGroupRelation(group, serializedFacetType, facetIds));
                    });
        }

        return facetConditions;
    }

    private int countUserFilterInTempTable() {
        final FacetComputationContext context = FacetComputationContextHolder.getContext();
        final SqlPart combinedFacetConditions = combineFacetConditions(context.getFacetConditions());
        return ctx.getCatalogCtx().getJdbcTemplate().queryForObject(
                "select count(*) " +
                        "from " + ComplexEntityQueryContextHolder.getContext().getTempTableName() +
                        " where " + combinedFacetConditions.getSql(),
                Integer.class,
                combinedFacetConditions.getArgs().toArray()
        );
    }

    private Map<FacetReference, FacetSummary.RequestImpact> computeFacetImpact(FacetComputationContext context, AllPossibleFacets allPossibleFacets) {
        final List<Object> args = new LinkedList<>();
        final StringBuilder impactQuery = new StringBuilder();
        final Map<FacetReference, FacetSummary.RequestImpact> computedImpacts = new HashMap<>();

        final Set<FacetReference> facetReferences = allPossibleFacets.getFacetToGroupMap().keySet();
        final Iterator<FacetReference> facetReferencesIterator = facetReferences.iterator();
        int i = 0;
        while (facetReferencesIterator.hasNext()) {
            i++;
            final FacetReference facetReference = facetReferencesIterator.next();
            final SqlPart userFilterWithTestFacetCondition = createTestFacetCondition(context.getGroupForFacet(facetReference), facetReference);
            impactQuery.append("select ? as entityType, ? as entityPrimaryKey, count(*) as count, count(*) - ")
                    .append(context.getUserFilterCount()).append(" as impact from ")
                    .append(ComplexEntityQueryContextHolder.getContext().getTempTableName())
                    .append(" where ").append(userFilterWithTestFacetCondition.getSql());
            args.add(facetReference.getSerializedEntityType());
            args.add(facetReference.getFacetId());
            args.addAll(userFilterWithTestFacetCondition.getArgs());

            if (i >= 1500) {
                computedImpacts.putAll(ctx.getCatalogCtx().getJdbcTemplate().query(
                        impactQuery.toString(),
                        new FacetImpactsExtractor(facetReferences),
                        args.toArray()
                ));
                i = 0;
                impactQuery.setLength(0);
                args.clear();
            } else if (facetReferencesIterator.hasNext()) {
                impactQuery.append(" UNION ");
            }
        }

        if (i > 0) {
            computedImpacts.putAll(ctx.getCatalogCtx().getJdbcTemplate().query(
                    impactQuery.toString(),
                    new FacetImpactsExtractor(facetReferences),
                    args.toArray()
            ));
        }

        return computedImpacts;
    }

    private FacetSummary buildFacetSummary(@Nonnull FacetStatisticsDepth depth,
                                           @Nullable Map<FacetReference, FacetSummary.RequestImpact> facetImpact) {
        final FacetComputationContext context = FacetComputationContextHolder.getContext();

        return new FacetSummary(
                context.getAllPossibleFacets().getGroupedAllPossibleFacets()
                        .entrySet()
                        .stream()
                        .map(it ->
                                new FacetSummary.FacetGroupStatistics(
                                        it.getKey().getEntityType(),
                                        it.getKey().getGroupId(),
                                        it.getValue()
                                                .stream()
                                                .map(facet -> {
                                                    FacetSummary.RequestImpact impact = null;
                                                    if (depth == FacetStatisticsDepth.IMPACT) {
                                                        impact = facetImpact.get(facet);
                                                    }

                                                    return new FacetSummary.FacetStatistics(
                                                            facet.getFacetId(),
                                                            context.wasFacetRequested(facet),
                                                            facet.getCount(),
                                                            impact
                                                    );
                                                })
                                                .collect(toList())
                                )
                        )
                        .collect(toList())
        );
    }


    /*
     * --- Histograms fetching ---
     */

    /**
     * Computes single attribute histogram from cached results in intermediate results table
     *
     * @param request request with metadata for computation
     * @param attributeName name of attribute for which to compute histogram
     * @return attribute histogram
     */
    private HistogramContract computeAttributeHistogram(@Nonnull SqlEvitaRequest request, @Nonnull String attributeName) {
        SqlPart userFilterSqlPart = SqlPart.TRUE;
        SqlWithClause userFilterWithClause = SqlWithClause.EMPTY;

        final UserFilter userFilter = QueryUtils.findFilter(request.getQuery(), UserFilter.class);
        if (userFilter != null) {
            final Predicate<FilterConstraint> forbiddenAttributeConstraintPredicate = c ->
                    (c.getArguments().length > 0) && attributeName.equals(c.getArguments()[0]);
            final ConstraintRemovingVisitor<FilterConstraint> removingVisitor = new ConstraintRemovingVisitor<>(forbiddenAttributeConstraintPredicate);
            userFilter.accept(removingVisitor);
            final UserFilter filteredUserFilter = (UserFilter) removingVisitor.getModifiedTree();

            final FilterConstraintTranslatingVisitor userFilterTranslator = FilterConstraintTranslatingVisitor.withDefaultMode(ctx, request);
            filteredUserFilter.accept(userFilterTranslator);
            userFilterSqlPart = userFilterTranslator.getFinalWhereSqlPart();
            userFilterWithClause = userFilterTranslator.getFinalSqlWithClause();
        }

        final SqlPart query = buildAttributeHistogramQuery(
                ctx.getUid(),
                attributeName,
                request.getRequiresAttributeHistogramBuckets(),
                request.getLanguage(),
                userFilterSqlPart,
                userFilterWithClause
        );

        return ctx.getCatalogCtx().getJdbcTemplate().query(
                query.getSql().toString(),
                new AttributeHistogramExtractor(request, ctx.getEntitySchema().get().getAttribute(attributeName)),
                query.getArgs().toArray()
        );
    }


    /*
     * --- Parents fetching ---
     */

    /**
     * Finds all parents of all entities in cached results in intermediate results table
     *
     * @param request request with metadata for finding
     * @return parents
     */
    private Map<Serializable, Parents.ParentsByType<?>> findAllParents(@Nonnull SqlEvitaRequest request) {
        final io.evitadb.api.query.require.Parents requiredParents = request.getRequiredParents();

        final boolean requiresFullEntity = requiredParents.getRequirements().length != 0;
        final Class<? extends Serializable> parentTargetClass = requiresFullEntity ? SealedEntity.class : Integer.class;

        return ctx.getCatalogCtx().getNpJdbcTemplate().query(
                buildParentsQuery(requiresFullEntity, null).getSql().toString(),
                new ParentsByTypeExtractor<>(ctx, parentTargetClass)
        );
    }

    /**
     * Finds parents of only specified entity types of all entities in cached results in intermediate results table
     *
     * @param request request with metadata for finding
     * @return parents
     */
    private Map<Serializable, Parents.ParentsByType<?>> findSpecificParents(@Nonnull SqlEvitaRequest request) {
        final Map<Serializable, Parents.ParentsByType<?>> parents = new HashMap<>();

        for (ParentsOfType requiredParentsOfType : request.getRequiredParentsOfType()) {
            final boolean requiresFullEntity = requiredParentsOfType.getRequirements().length != 0;
            final Class<? extends Serializable> parentTargetClass = requiresFullEntity ? SealedEntity.class : Integer.class;

            final SqlPart parentsQuery = buildParentsQuery(
                    requiresFullEntity,
                    Arrays.stream(requiredParentsOfType.getEntityTypes())
                            .map(type -> ctx.getCatalogCtx().getSerializedEntityType(type))
                            .toArray(String[]::new)
            );
            final Map<Serializable, Parents.ParentsByType<?>> parentsForCurrentTypes = ctx.getCatalogCtx().getJdbcTemplate().query(
                    parentsQuery.getSql().toString(),
                    new ParentsByTypeExtractor<>(ctx, parentTargetClass),
                    parentsQuery.getArgs().toArray()
            );
            parents.putAll(parentsForCurrentTypes);
        }

        return parents;
    }

    /**
     * Creates unique name of temporary table for storing intermediate entity results
     */
    private String generateTempTableName() {
        final String tableUuid = UUID.randomUUID().toString().replace("-", "_");
        return QUERY_TEMP_TABLE_NAME_PREFIX + tableUuid;
    }
}
