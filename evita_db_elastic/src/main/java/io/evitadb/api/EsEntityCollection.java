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

package io.evitadb.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.data.AttributesContract;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.structure.*;
import io.evitadb.api.exception.ConcurrentSchemaUpdateException;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.exception.SchemaAlteringException;
import io.evitadb.api.exception.UniqueValueViolationException;
import io.evitadb.api.io.*;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.require.*;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.exception.EvitaFacetComputationCheckedException;
import io.evitadb.storage.exception.EvitaGetException;
import io.evitadb.storage.exception.EvitaInsertException;
import io.evitadb.storage.model.EntityWrapper;
import io.evitadb.storage.model.HierarchyReference;
import io.evitadb.storage.pmptt.ParentsComputer;
import io.evitadb.storage.query.EsQueryTranslator;
import io.evitadb.storage.query.filter.TypeTranslator;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.schema.SchemaRepository;
import io.evitadb.storage.statistics.HierarchyStatisticsComputer;
import io.evitadb.storage.statistics.StatisticsComputer;
import io.evitadb.storage.utils.FacetUtil;
import io.evitadb.storage.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.oss.pmptt.exception.PivotHierarchyNodeNotFound;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.xcontent.XContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.storage.configuration.ElasticClientUtil.refreshIndex;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 * @see io.evitadb.api.EntityCollectionBase
 **/
@CommonsLog
public class EsEntityCollection extends EntityCollectionBase<EsEvitaRequest> {
    /**
     * Memory store for entities.
     */
    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;
    /**
     * Contains function that takes care of primary key generation if it's required by this {@link #getSchema()}.
     */
    private Consumer<EntityMutation> primaryKeyHandler;

    private final BooleanSupplier transactionalSupportEnabled;
    private final String indexName;
    private StatisticsComputer statisticsComputer = null;

    public EsEntityCollection(EntitySchema entitySchema, String indexName, RestHighLevelClient client, ObjectMapper objectMapper, BooleanSupplier transactionalSupportEnabled) {

        super(entitySchema);
        this.indexName = indexName;
        this.client = client;
        this.objectMapper = objectMapper;

        SchemaRepository.updateSchemaInEs(indexName, entitySchema);
        this.transactionalSupportEnabled = transactionalSupportEnabled;
    }

    /**
     * Gets entities from DB , in case that whole source is wanted, gets source in second phase.
     *
     * @param evitaRequest request with query.
     * @param <S>          Entity class
     * @param <T>          Containing response class
     * @return paginated list with entities
     */
    @Nonnull
    @Override
    public <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull EsEvitaRequest evitaRequest) {
        return getEntities(evitaRequest, true);
    }

    public <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull EsEvitaRequest evitaRequest, boolean respectPage) {
        // get entities from map by their primary keys

        List<String> primaryKeys;
        final List<EvitaResponseExtraResult> extraResults = new LinkedList<>();
        int totalCount;
        EsSearchResult searchResult;
        try {
            searchResult = searchElastic(evitaRequest, respectPage, FilterMode.DEFAULT);
            primaryKeys = searchResult.getPrimaryKeys();
            totalCount = searchResult.getTotalCount();
            SearchResponse searchResponse = searchResult.getSearchResponse();

            if (totalCount != 0 && searchResponse.getHits().getHits().length == 0) {
                searchResult = searchElastic(evitaRequest, false, FilterMode.DEFAULT);
                primaryKeys = searchResult.getPrimaryKeys();
                totalCount = searchResult.getTotalCount();
            }
        } catch (EvitaFacetComputationCheckedException e) {
            throw e;
        } catch (Exception e) {
            throw new EvitaGetException(e.getMessage(), e);
        }

        if (statisticsComputer == null) {
            statisticsComputer = new StatisticsComputer(client, objectMapper, this, indexName);
        }

        FacetSummary facetSummary = evitaRequest.getFacetSummary();
        if (facetSummary != null) {
            try {
                extraResults.add(statisticsComputer.computeFacetSummary(facetSummary, evitaRequest, getSchema(), searchResult));
            } catch (IOException e) {
                throw new EvitaGetException("Cannot compute facet summary: " + e.getMessage(),e);
            }
        }
        if (evitaRequest.isRequiresAttributeHistogram()) {
            extraResults.add(statisticsComputer.computeAttributeHistogram(evitaRequest));
        }
        if (evitaRequest.isRequiresPriceHistogram()) {
            io.evitadb.api.io.extraResult.PriceHistogram histogram = statisticsComputer.computePriceHistogram(evitaRequest);
            if (histogram != null) extraResults.add(histogram);
        }


        Parents parents = evitaRequest.getRequireParents();
        ParentsOfType parentsOfType = evitaRequest.getRequireParentsOfType();
        ParentsComputer parentsComputer = new ParentsComputer(this);
        if (parents != null) {
            extraResults.add(parentsComputer.getParentsOfEntity(parents, evitaRequest, primaryKeys));
        }
        if (parentsOfType != null && !primaryKeys.isEmpty()) {
            extraResults.add(parentsComputer.getParentsOfTypes(parentsOfType, evitaRequest, primaryKeys));
        }

        HierarchyStatistics hierarchyStatistics = evitaRequest.getRequireHierarchyStatistics();
        HierarchyStatisticsComputer hierarchyStatisticsComputer = new HierarchyStatisticsComputer(this);
        final EntitySchema currentSchema = getSchema();
        if (hierarchyStatistics != null) {
            extraResults.add(
                    hierarchyStatisticsComputer
                            .computeHierarchyStatistics(evitaRequest,client,currentSchema,indexName,objectMapper)
            );
        }

        // if full entity bodies are requested
        if (evitaRequest.isRequiresEntityBody()) {
            // this may produce ClassCast exception if client assigns variable to different result than requests
            final List<Entity> entitiesByPrimaryKey = getEntitiesByPrimaryKey(primaryKeys.stream()
                    .skip(evitaRequest.getFirstRecordOffset(primaryKeys.size()))
                    .limit(evitaRequest.getLimit())
                    .toArray(String[]::new), evitaRequest);
            //noinspection unchecked
            return (T) new EvitaEntityResponse(
                    evitaRequest.getQuery(),
                    evitaRequest.createDataChunk(
                            totalCount,
                            // cut off required page of entities from the full result
                            entitiesByPrimaryKey
                                    .stream()
                                    // if schema changed between storing to inner map and retrieving back, update it
                                    // this won't be necessary in real implementation
                                    .map(it -> it.getSchema() != currentSchema ? EntityHelper.withUpdatedSchema(it, currentSchema) : it)
                                    // decorate entity to an object that hides not requested / dropped data
                                    .map(it ->
                                            Entity.decorate(
                                                    it,
                                                    currentSchema,
                                                    new HierarchicalContractSerializablePredicate(),
                                                    new AttributeValueSerializablePredicate(evitaRequest),
                                                    new AssociatedDataValueSerializablePredicate(evitaRequest),
                                                    new ReferenceContractSerializablePredicate(evitaRequest),
                                                    new PriceContractSerializablePredicate(evitaRequest)
                                            )
                                    )
                                    .collect(Collectors.toList())
                    ),
                    extraResults.toArray(new EvitaResponseExtraResult[0])
            );
        } else {
            // this may produce ClassCast exception if client assigns variable to different result than requests
            // noinspection unchecked
            return (T) new EvitaEntityReferenceResponse(
                    evitaRequest.getQuery(),
                    evitaRequest.createDataChunk(
                            totalCount,
                            // cut off required page of entities from the full result
                            primaryKeys.stream()
                                    .skip(evitaRequest.getFirstRecordOffset(primaryKeys.size()))
                                    .limit(evitaRequest.getLimit())
                                    // returns simple reference to the entity (i.e. primary key and type of the entity)
                                    .map(it -> new EntityReference(currentSchema.getName(), StringUtils.cleanFromType(it, evitaRequest.getEntityType())))
                                    .collect(Collectors.toList())
                    ),
                    extraResults.toArray(new EvitaResponseExtraResult[0])
            );
        }
    }

    @Override
    public SealedEntity enrichEntity(@Nonnull SealedEntity sealedEntity, @Nonnull EsEvitaRequest evitaRequest) {
        final EntityDecorator partiallyLoadedEntity = (EntityDecorator) sealedEntity;
        Assert.notNull(partiallyLoadedEntity.getPrimaryKey(), "Primary key cannot be null!");

        // return decorator that hides information not requested by original query
        final AttributeValueSerializablePredicate newAttributePredicate = partiallyLoadedEntity.getAttributePredicate().createRicherCopyWith(evitaRequest);
        final AssociatedDataValueSerializablePredicate newAssociatedDataPredicate = partiallyLoadedEntity.getAssociatedDataPredicate().createRicherCopyWith(evitaRequest);
        final ReferenceContractSerializablePredicate newReferencePredicate = partiallyLoadedEntity.getReferencePredicate().createRicherCopyWith(evitaRequest);
        final PriceContractSerializablePredicate newPricePredicate = partiallyLoadedEntity.getPricePredicate().createRicherCopyWith(evitaRequest);
        return new EntityDecorator(
                // load all missing data according to current evita request
                getPureEntity(partiallyLoadedEntity.getPrimaryKey(), evitaRequest),
                // use original schema
                partiallyLoadedEntity.getSchema(),
                // show / hide hierarchical placement information
                partiallyLoadedEntity.getHierarchicalPlacementPredicate(),
                // show / hide attributes information
                newAttributePredicate,
                // show / hide associated data information
                newAssociatedDataPredicate,
                // show / hide references information
                newReferencePredicate,
                // show / hide price information
                newPricePredicate
        );
    }

    @Nullable
    @Override
    public SealedEntity getEntity(int primaryKey, @Nonnull EsEvitaRequest evitaRequest) {
        Entity source = getPureEntity(primaryKey, evitaRequest);

        return ofNullable(source)
                .filter(Droppable::exists)
                .map(it -> Entity.decorate(
                        it,
                        getSchema(),
                        new HierarchicalContractSerializablePredicate(),
                        new AttributeValueSerializablePredicate(evitaRequest),
                        new AssociatedDataValueSerializablePredicate(evitaRequest),
                        new ReferenceContractSerializablePredicate(evitaRequest),
                        new PriceContractSerializablePredicate(evitaRequest)
                ))
                .orElse(null);
    }

    public Entity getPureEntity(int primaryKey, @Nonnull EsEvitaRequest evitaRequest) {
        Entity source = null;
        if (!transactionalSupportEnabled.getAsBoolean()) {

            List<StoringParameters.StoredEntity> allEntities =
                    StoringParametersDao.getStoringParameters()
                            .values()
                            .stream()
                            .flatMap(i -> i.getEntityCache().stream())
                            .collect(Collectors.toList());
            source = allEntities
                    .stream()
                    .filter(i -> {
                        Entity entity = i.getEntity();
                        Integer entityPrimaryKey = entity.getPrimaryKey();
                        return (entityPrimaryKey == null || entityPrimaryKey.equals(primaryKey)) && entity.getType().equals(evitaRequest.getEntityType());
                    })
                    .findFirst()
                    .map(StoringParameters.StoredEntity::getEntity)
                    .orElse(null);
        }
        if (source == null) {
            try {
                GetRequest request = new GetRequest(indexName, StringUtils.getUI(evitaRequest.getEntityType(), primaryKey));
                String[] includes = getTypesByRequest(evitaRequest);
                request.fetchSourceContext(new FetchSourceContext(includes.length != 0, includes, new String[0]));
                GetResponse response = client.get(request, RequestOptions.DEFAULT);
                if (response.isExists()) {
                    source = objectMapper.readValue(response.getSourceAsString(), Entity.class);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return source;
    }

    private List<Entity> getEntitiesByPrimaryKey(String[] primaryKeys, EsEvitaRequest evitaRequest) {

        List<Entity> entities = Collections.emptyList();
        if (primaryKeys.length == 0) return entities;
        try {
            MultiGetRequest multiGetRequest = new MultiGetRequest();
            String[] includes = getTypesByRequest(evitaRequest);
            Arrays.stream(primaryKeys)
                    .forEach(i -> {
                        MultiGetRequest.Item item = new MultiGetRequest.Item(indexName, i);
                        item.fetchSourceContext(new FetchSourceContext(includes.length != 0, includes, new String[0]));
                        multiGetRequest.add(item);
                    });
            MultiGetResponse multiGetItemResponses = client.mget(multiGetRequest, RequestOptions.DEFAULT);
            entities = Arrays.stream(multiGetItemResponses.getResponses())
                    .map(MultiGetItemResponse::getResponse)
                    .filter(GetResponse::isExists)
                    .map(i -> {
                        try {
                            return objectMapper.readValue(i.getSourceAsString(), Entity.class);
                        } catch (JsonProcessingException e) {
                            throw new EvitaGetException(e.getMessage(), e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new EvitaGetException(e.getMessage(), e);
        }
        return entities;
    }

    private String[] getTypesByRequest(EsEvitaRequest request) {
        List<String> includes = new LinkedList<>();

        includes.add("primaryKey");
        includes.add("type");
        includes.add("version");
        includes.add("dropped");
        includes.add("locales");
        includes.add("schema");
        includes.add("hierarchicalPlacement");
        includes.add("priceInnerRecordHandling");
        includes.add("priceVersion");
        includes.add("indexName");

        if (request.isRequiresEntityAssociatedData() || QueryUtils.findRequire(request.getQuery(), DataInLanguage.class) != null) {
            includes.add("associatedData");
        }
        if (request.isRequiresEntityAttributes() || QueryUtils.findRequire(request.getQuery(), DataInLanguage.class) != null) {
            includes.add("attribute");
        }
        if (request.isRequiresEntityReferences()) {
            includes.add("references");
        }
        if (!request.getRequiresEntityPrices().equals(PriceFetchMode.NONE)) {
            includes.add("prices");
            includes.add("notSellablePrices");
        }
        return includes.toArray(String[]::new);
    }

    @Nonnull
    @Override
    public EntityReference upsertEntity(@Nonnull EntityMutation entityMutation) {
        Entity source = null;
        String ui = StringUtils.getUI(entityMutation.getEntityType(), entityMutation.getEntityPrimaryKey());
        if (entityMutation.getEntityPrimaryKey() != null) {
            try {
                GetRequest request = new GetRequest(indexName, ui);
                request.fetchSourceContext(new FetchSourceContext(true, new String[0], new String[0]));
                GetResponse response = client.get(request, RequestOptions.DEFAULT);
                if (response.isExists()) {
                    source = objectMapper.readValue(response.getSourceAsString(), Entity.class);
                }
                if (source != null && source.isDropped()) source = null;
            } catch (IOException e) {
                throw new EvitaInsertException(e.getMessage(), e);
            }
        }

        // verify mutation against schema
        // it was already executed when mutation was created, but there are two reasons to do it again
        // - we don't trust clients - in future it may be some external JS application
        // - schema may changed between entity was provided to the client and the moment upsert was called
        final EntitySchema currentSchema = updateSchemaWithRetry(
                s -> entityMutation.verifyOrEvolveSchema(s, this::updateSchema)
        );

        getOrCreatePrimaryKeyHandler(entityMutation.getEntityPrimaryKey() != null)
                .accept(entityMutation);
        final Entity newVersionOfEntity = entityMutation.mutate(source);
        final Entity upToDateEntity = newVersionOfEntity.getSchema() == currentSchema ?
                newVersionOfEntity : EntityHelper.withUpdatedSchema(newVersionOfEntity, currentSchema);


        HierarchyReference hierarchyReference = updateHierarchy(upToDateEntity);
        if (hierarchyReference.isUpdated()) {
            if (hierarchyReference.getExternalId() != null) {
                updateHierarchyEntity(hierarchyReference.getExternalId(), hierarchyReference.getLeftBound(), hierarchyReference.getRightBound());
            }
            PMPTTAccessor.storePMPTT(client, hierarchyReference.getType(), indexName, objectMapper);
        }

        try {


            if (transactionalSupportEnabled.getAsBoolean()) {
                EsCatalog.updateReferencesForEntitiesInSeparateIndex(Collections.singletonList(upToDateEntity), indexName, client, objectMapper);
                checkUniquesByEs(upToDateEntity, ZonedDateTime.now());
                indexEntity(upToDateEntity);

            } else {
                StoringParameters storingParameters = StoringParametersDao.getStoringParameters().get(indexName);
                checkUniquesByMemory(storingParameters, upToDateEntity);
                storingParameters.addEntity(upToDateEntity);
            }
            source = upToDateEntity;


        } catch (IOException e) {
            throw new EvitaInsertException(e.getMessage(), e);
        }

        Assert.notNull(source.getPrimaryKey(), "Primary key of dest entity cannot be null!");
        return new EntityReference(source.getType(), source.getPrimaryKey());
    }

    public void updateHierarchyEntity(String externalId, Long oldLeftBound, Long oldRightBound) {

        List<Entity> sources;
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(
                    boolQuery()
                            .should(
                                    boolQuery()
                                            .filter(
                                                    nestedQuery("paths",
                                                            boolQuery()
                                                                    .must(rangeQuery("paths.leftBound").gte(oldLeftBound))
                                                                    .must(rangeQuery("paths.rightBound").lte(oldRightBound)),
                                                            ScoreMode.Max
                                                    )
                                            )).minimumShouldMatch(1)
            );
            searchSourceBuilder.fetchSource(false);
            searchSourceBuilder.size(10000);
            searchSourceBuilder.from(0);

            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);


            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            sources = getEntitiesByPrimaryKey(
                    Arrays.stream(searchResponse.getHits().getHits())
                            .map(SearchHit::getId)
                            .toArray(String[]::new),
                    new EsEvitaRequest(
                            query(
                                    entities(getSchema().getName()),
                                    new FilterBy(null),
                                    require(fullEntity())
                            ),
                            ZonedDateTime.now()
                    ));
        } catch (Exception e) {
            throw new EvitaGetException(e.getMessage(), e);
        }

        if (!sources.isEmpty()) log.info("Updating entities from category" + externalId);

        Set<Serializable> updatedHierarchyCodes = new HashSet<>();
        for (Entity source : sources) {

            if (source.getPrimaryKey() != null) {
                Assert.notNull(source, "Cannot change hierarchy without entity! ");
                HierarchyReference hierarchyReference = updateHierarchy(source);
                updatedHierarchyCodes.add(hierarchyReference.getType());

                try {

                    if (transactionalSupportEnabled.getAsBoolean()) {
                        indexEntity(source);
                    }

                } catch (IOException e) {
                    throw new EvitaInsertException(e.getMessage(), e);
                }

            }
        }
        updatedHierarchyCodes.forEach(i->PMPTTAccessor.storePMPTT(client, i, indexName, objectMapper));
    }

    private void indexEntity(Entity source) throws IOException {
        String ui = StringUtils.getUI(source.getType(), source.getPrimaryKey());

        IndexRequest indexRequest = new IndexRequest(indexName)
                .id(ui)
                .source(objectMapper.writeValueAsString(new EntityWrapper(source, indexName)), XContentType.JSON);
        client.index(indexRequest, RequestOptions.DEFAULT);

        refreshIndex(client, indexName);
        FacetUtil.getCachedReferences().clear();
    }

    private HierarchyReference updateHierarchy(Entity upToDateEntity) {


        HierarchyReference item = new HierarchyReference();

        // here it cannot be null - see primaryKeyHandler upper
        if (upToDateEntity.getPrimaryKey() != null) {
            String externalId = StringUtils.getUI(upToDateEntity.getType(), upToDateEntity.getPrimaryKey());

            item.setType(upToDateEntity.getType());
            Hierarchy hierarchy = PMPTTAccessor.getHierarchy(upToDateEntity.getType());
            try {
                HierarchyItem hierarchyItem = hierarchy.getItem(externalId);
                item.updateBy(hierarchyItem);
            } catch (PivotHierarchyNodeNotFound e) {
                // ignored - could be created or product
            }

            // categories
            HierarchicalPlacementContract hierarchicalPlacement = upToDateEntity.getHierarchicalPlacement();
            if (hierarchicalPlacement != null) {
                if (hierarchicalPlacement.getParentPrimaryKey() != null) {
                    int parentPrimaryKey = hierarchicalPlacement.getParentPrimaryKey();
                    String parent = StringUtils.getUI(upToDateEntity.getType(), parentPrimaryKey);
                    HierarchyItem hierarchyItem = getFromHierarchy(hierarchy, externalId);
                    if (hierarchyItem == null) {
                        HierarchyItem parentItem = null;
                        try {
                            parentItem = hierarchy.getItem(parent);
                        } catch (PivotHierarchyNodeNotFound e) {
                            // ignored
                        }
                        if (parentItem == null) {
                            hierarchy.createRootItem(parent);
                        }
                        item.setUpdated(true);
                        hierarchy.createItem(externalId, parent);
                    } else {
                        HierarchyItem parentItem = hierarchy.getParentItem(externalId);
                        if (parentItem == null || !Objects.equals(parentItem.getCode(), parent)) {
                            item.setUpdated(true);
                            hierarchy.moveItemToLast(parent);
                        }
                    }
                } else {

                    HierarchyItem hierarchyItem = getFromHierarchy(hierarchy, externalId);
                    if (hierarchyItem == null) {
                        item.setUpdated(true);
                        hierarchy.createRootItem(externalId);
                    } else {
                        if (item.getLevel() != 1) {
                            item.setUpdated(true);
                            hierarchy.removeItem(externalId);
                            hierarchy.createRootItem(externalId);
                        }
                    }
                }

            }
            if (upToDateEntity.isDropped()) {
                item.setUpdated(true);
                hierarchy.removeItem(externalId);
            }
        }
        return item;
    }

    @Nullable
    private HierarchyItem getFromHierarchy(Hierarchy hierarchy, String code) {
        try {
            return hierarchy.getItem(code);
        } catch (PivotHierarchyNodeNotFound ignored) {
            // ignored
        }
        return null;
    }

    private void checkUniquesByMemory(StoringParameters evitaParameters, Entity upToDateEntity) {
        Map<String, Serializable> uniques = getUniqueAttrsOfEntity(upToDateEntity);

        uniques
                .forEach((key, value) -> {

                    StoringParameters.AttributeWithReference attributeWithReference = evitaParameters.getEntityCacheAttrs().get(key);
                    if (attributeWithReference != null){
                        Serializable attr = attributeWithReference.getAttr();
                        Integer pk = attributeWithReference.getPk();
                        boolean existDuplicate = attr.equals(value)
                                && attributeWithReference.getType().equals(upToDateEntity.getType())
                                && pk != null
                                && !pk.equals(upToDateEntity.getPrimaryKey());
                        if (existDuplicate) {
                            throw new UniqueValueViolationException(
                                    key,
                                    attr,
                                    pk,
                                    Optional.of(upToDateEntity).map(Entity::getPrimaryKey).orElse(0));
                        }
                    }
                });
    }

    private void checkUniquesByEs(@Nonnull Entity upToDateEntity, ZonedDateTime now) {
        Map<String, Serializable> uniques = getUniqueAttrsOfEntity(upToDateEntity);
        if (uniques.isEmpty()) return;

        EvitaResponseBase<Serializable> entities = getEntities(new EsEvitaRequest(
                query(
                        entities(upToDateEntity.getType()),
                        filterBy(

                                and(
                                        or(
                                                uniques
                                                        .entrySet()
                                                        .stream()
                                                        .map((Map.Entry<String, Serializable> attributeName) -> eq(attributeName.getKey(), (Serializable & Comparable<?>) attributeName.getValue()))
                                                        .toArray(FilterConstraint[]::new)
                                        ),
                                        not(primaryKey(upToDateEntity.getPrimaryKey()))
                                )
                        ),
                        require(
                                page(1, Integer.MAX_VALUE)
                        )
                ),
                now
        ));
        if (entities.getTotalRecordCount() != 0) {

            entities = getEntities(new EsEvitaRequest(
                    query(
                            entities(upToDateEntity.getType()),
                            filterBy(

                                    and(
                                            or(
                                                    uniques
                                                            .entrySet()
                                                            .stream()
                                                            .map((Map.Entry<String, Serializable> attributeName) -> eq(attributeName.getKey(), (Serializable & Comparable<?>) attributeName.getValue()))
                                                            .toArray(FilterConstraint[]::new)
                                            ),
                                            not(primaryKey(upToDateEntity.getPrimaryKey()))
                                    )
                            ),
                            require(
                                    page(1, Integer.MAX_VALUE),
                                    entityBody(),
                                    attributes()
                            )
                    ),
                    now
            ));
            Map<Integer, List<AttributesContract.AttributeValue>> collect = entities
                    .getRecordData()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    j -> ((EntityDecorator) j).getPrimaryKey(),
                                    i -> ((EntityDecorator) i).getAttributeValues().stream().filter(j -> uniques.entrySet().stream().anyMatch(k -> k.getKey().equals(j.getKey().getAttributeName()) && k.getValue().equals(j.getValue()))).collect(Collectors.toList())));
            for (Map.Entry<Integer, List<AttributesContract.AttributeValue>> integerListEntry : collect.entrySet()) {
                if (!integerListEntry.getValue().isEmpty()) {
                    // throw first
                    AttributesContract.AttributeValue attributeValue = integerListEntry.getValue().get(0);
                    Assert.notNull(upToDateEntity.getPrimaryKey(), "Primary key must be already generated.");
                    throw new UniqueValueViolationException(attributeValue.getKey().getAttributeName(), attributeValue.getValue(), integerListEntry.getKey(), upToDateEntity.getPrimaryKey());
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static Map<String, Serializable> getUniqueAttrsOfEntity(Entity upToDateEntity) {
        return upToDateEntity
                .getSchema()
                .getAttributes()
                .values()
                .stream()
                .filter(AttributeSchema::isUnique)
                .map(AttributeSchema::getName)
                .filter(i -> upToDateEntity.getAttribute(i) != null)
                .collect(toMap(i -> i, upToDateEntity::getAttribute));
    }


    @Override
    public boolean deleteEntity(int primaryKey) {
        try {
            DeleteResponse deleteResponse = client.delete(
                    new DeleteRequest(indexName, StringUtils.getUI(getSchema().getName(), primaryKey)),
                    RequestOptions.DEFAULT);
            refreshIndex(client, indexName);
            FacetUtil.getCachedReferences().clear();
            return deleteResponse.getResult().getOp() == 2;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int deleteEntityAndItsHierarchy(int primaryKey) {
        SealedEntity entity = getEntity(primaryKey,
                new EsEvitaRequest(
                        Query.query(
                                entities(getSchema().getName()),
                                require(entityBody())
                        ),
                        ZonedDateTime.now()
                ));
        AtomicInteger count = new AtomicInteger();
        if (entity != null) {
            Serializable entityType = entity.getType();
            Hierarchy hierarchy = PMPTTAccessor.getHierarchy(entityType);
            String parent = StringUtils.getUI(entityType, primaryKey);
            HierarchyItem hierarchyItem = getFromHierarchy(hierarchy, parent);
            if (hierarchyItem != null) {
                removeChildren(hierarchy, hierarchyItem.getCode(), count);
            }
        }
        return count.get();
    }

    private void removeChildren(Hierarchy hierarchy, String ui, AtomicInteger count) {
        for (HierarchyItem childItem : hierarchy.getChildItems(ui)) {
            removeChildren(hierarchy, childItem.getCode(), count);
        }
        hierarchy.removeItem(ui);
        deleteEntity(Integer.parseInt(ui.substring(ui.indexOf("_") + 1)));
        count.incrementAndGet();
    }

    @Override
    public int deleteEntities(@Nonnull EsEvitaRequest evitaRequest) {
        return (int) Arrays.stream(evitaRequest.getPrimaryKeys())
                .mapToObj(this::deleteEntity)
                .filter(it -> it)
                .count();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        try {
            long size = client.count(new CountRequest(new String[]{indexName}, TypeTranslator.enhanceWithType(boolQuery(), getSchema().getName())), RequestOptions.DEFAULT).getCount();
            if (!transactionalSupportEnabled.getAsBoolean()) {
                size += StoringParametersDao.getStoringParameters().get(indexName).count(getSchema().getName());
            }
            return (int) size;
        } catch (IOException e) {
            throw new EvitaGetException(e.getMessage(), e);
        }
    }

    /**
     * This method will try to update existing schema but allows automatic retrying when there is race condition and
     * some other process updates schema simultaneously but was a little bit faster.
     */
    private EntitySchema updateSchemaWithRetry(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
        int sanityCheck = 0;
        EntitySchema currentSchema = null;

        do {
            try {
                sanityCheck++;
                currentSchema = schemaUpdater.apply(getSchema());
            } catch (ConcurrentSchemaUpdateException ignored) {
                // someone was faster then us - retry with current schema
            }
        } while (currentSchema == null && sanityCheck < 50);


        if (currentSchema == null) {

            try {
                currentSchema = schemaUpdater.apply(getSchema());
            } catch (ConcurrentSchemaUpdateException ignored) {
                // someone was faster then us - retry with current schema
            }

            if (currentSchema == null) {
                log.error("someone was faster then us too many times");
            }
        }
        return currentSchema;
    }

    protected Consumer<EntityMutation> getOrCreatePrimaryKeyHandler(boolean pkInFirstEntityPresent) {
        return ofNullable(primaryKeyHandler)
                .orElseGet(() -> {
                    this.primaryKeyHandler = createPrimaryKeyHandler(pkInFirstEntityPresent);
                    return this.primaryKeyHandler;
                });
    }

    protected Consumer<EntityMutation> createPrimaryKeyHandler(boolean pkInFirstEntityPresent) {
        final EntitySchema currentSchema = this.getSchema();
        if (pkInFirstEntityPresent) {
            return entityMutation ->
                    Assert.isTrue(
                            !currentSchema.isWithGeneratedPrimaryKey(),
                            "Entity of type " + currentSchema.getName() +
                                    " is expected to have primary key automatically generated by Evita!"

                    );
        } else {
            Assert.isTrue(
                    currentSchema.isWithGeneratedPrimaryKey() ||
                            (currentSchema.allows(EvolutionMode.ADAPT_PRIMARY_KEY_GENERATION) &&
                                    isEmpty()),
                    () -> new InvalidMutationException(
                            "Entity of type " + currentSchema.getName() +
                                    " is expected to have primary key provided by external systems!"
                    )
            );
            if (!currentSchema.isWithGeneratedPrimaryKey()) {
                updateSchemaWithRetry(s -> s.open(this::updateSchema).withGeneratedPrimaryKey().applyChanges());
            }
            return entityMutation -> {
                if (entityMutation.getEntityPrimaryKey() == null) {
                    entityMutation.setEntityPrimaryKey(generatePrimaryKey(entityMutation.getEntityType()));
                }
            };
        }
    }

    /**
     * Internal method for storing updated schema and verifying optimistic locking.
     *
     * @throws SchemaAlteringException when changes were not applied because of an error
     */
    @Override
    @Nonnull
    EntitySchema updateSchema(@Nonnull EntitySchema newSchema) throws SchemaAlteringException {
        EntitySchema schema = super.updateSchema(newSchema);
        SchemaRepository.updateSchemaInEs(indexName, schema);
        return schema;
    }

    @Nonnull
    @Override
    public EntitySchema getSchema() {
        try {
            return SchemaRepository.getEntitySchema(indexName, schema.get().getName());
        } catch (IOException e) {
            throw new EvitaGetException("Cannot find scheme for entity, cause : " + e.getMessage(), e);
        }
    }

    protected Integer generatePrimaryKey(Serializable entityType) {
        Queue<Integer> integerQueue = StoringParametersDao
                .getGeneratedPrimaryKeys()
                .computeIfAbsent(indexName, i -> new ConcurrentHashMap<>())
                .computeIfAbsent(entityType, i -> new ConcurrentLinkedDeque<>());
        Integer newPK = integerQueue.poll();
        if (newPK == null) {
            if (transactionalSupportEnabled.getAsBoolean()) {
                try {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("id", "id");
                    IndexRequest indexRequest = new IndexRequest(indexName + "-keys")
                            .id("id" + entityType + EsCatalog.idGeneratorID.get())
                            .source(hashMap);
                    IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                    return (int) indexResponse.getVersion();
                } catch (IOException e) {
                    throw new EvitaInsertException(e.getMessage(), e);
                }
            } else {
                fillTheQueueOfPKs(entityType, integerQueue);
                return integerQueue.poll();
            }
        }
        return newPK;
    }

    private synchronized void fillTheQueueOfPKs(Serializable entityType, Queue<Integer> integerQueue) {
        try {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("id", "id");

            BulkRequest bulkRequest = new BulkRequest();
            for (int i = 0; i < 100; i++) {
                IndexRequest indexRequest = new IndexRequest(indexName + "-keys")
                        .id("id" + entityType + EsCatalog.idGeneratorID.get())
                        .source(hashMap);
                bulkRequest.add(indexRequest);
            }
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            Arrays.stream(bulkResponse.getItems()).map(BulkItemResponse::getVersion).forEach(i -> integerQueue.add(i.intValue()));
        } catch (IOException e) {
            throw new EvitaInsertException(e.getMessage(), e);
        }
    }

    /**
     * Vložit do separátní servicy
     */
    public EsSearchResult searchElastic(EsEvitaRequest evitaRequest, boolean respectPage, FilterMode filterMode) throws
            IOException {
        return searchElastic(evitaRequest, respectPage, filterMode, null);
    }

    public EsSearchResult searchElastic(EsEvitaRequest evitaRequest, boolean respectPage, FilterMode
            filterMode, String forbiddenAttribute) throws IOException {
        long startQuery = System.currentTimeMillis();
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = EsQueryTranslator.translateQuery(
                evitaRequest.getAlignedNow(),
                getSchema(),
                evitaRequest,
                objectMapper,
                respectPage,
                filterMode,
                client,
                forbiddenAttribute,
                indexName);
        sourceBuilder.trackTotalHits(true);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        long took = System.currentTimeMillis() - startQuery;

        if (took > 1000) {
            log.info("Searching entities took " + took);
        }
        return new EsSearchResult(
                Arrays.stream(searchResponse.getHits().getHits())
                        .map(SearchHit::getId)
                        .collect(Collectors.toList()),
                ofNullable(searchResponse.getHits().getTotalHits()).map(i->(int)i.value).orElse(0),
                searchResponse);
    }

    @AllArgsConstructor
    @Data
    public static class EsSearchResult {
        private List<String> primaryKeys;
        private int totalCount;
        private SearchResponse searchResponse;
    }
}
