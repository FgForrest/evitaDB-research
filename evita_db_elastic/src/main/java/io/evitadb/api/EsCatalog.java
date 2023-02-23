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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.configuration.EsEvitaCatalogConfiguration;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.StoringParametersDao;
import io.evitadb.api.io.EsEvitaRequest;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.configuration.ElasticClientUtil;
import io.evitadb.storage.configuration.EsStorageConfiguration;
import io.evitadb.storage.configuration.accessor.ClientAccessor;
import io.evitadb.storage.configuration.accessor.OMAccessor;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.exception.EvitaEsException;
import io.evitadb.storage.exception.EvitaGetException;
import io.evitadb.storage.exception.EvitaInsertException;
import io.evitadb.storage.model.EntityWrapper;
import io.evitadb.storage.model.FacetReferenceDto;
import io.evitadb.storage.schema.SchemaRepository;
import io.evitadb.storage.utils.FacetUtil;
import io.evitadb.storage.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.xcontent.XContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Timestamp: 2022-10-02
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 * @see CatalogBase
 **/
@CommonsLog
public class EsCatalog extends CatalogBase<EsEvitaRequest, EsEvitaCatalogConfiguration, EsEntityCollection> {

    /**
     * Memory store for catalogs.
     */
    @Getter(AccessLevel.PACKAGE)
    @Setter
    private RestHighLevelClient client;

    @Getter
    @Setter
    private ObjectMapper objectMapper;

    private final Map<String, EsEntityCollection> collectionMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> indexExistsMap = new ConcurrentHashMap<>();
    public static final AtomicReference<String> idGeneratorID = new AtomicReference<>(null);

    public EsCatalog(EsEvitaCatalogConfiguration configuration) {
        super(configuration);
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, configuration.getCredentials());
        client = ClientAccessor.initClient(configuration);
        createIndex();
        this.objectMapper = OMAccessor.getObjectMapper();
        String indexName = getIndexName();
        PMPTTAccessor.initPMPTT(client, indexName, objectMapper);

        StoringParameters evitaParameters = new StoringParameters(
                (entities, async) -> {
                    if (entities.isEmpty()) return;
                    BulkRequest bulkRequest = new BulkRequest();

                    entities.forEach(i -> {
                        String ui = StringUtils.getUI(i.getEntity().getType(), i.getEntity().getPrimaryKey());
                        IndexRequest indexRequest = new IndexRequest(indexName)
                                .id(ui)
                                .source(i.getJson(), XContentType.JSON);
                        bulkRequest.add(indexRequest);
                    });


                    if (bulkRequest.estimatedSizeInBytes() > 20000000) {
                        log.warn("Storing " + RamUsageEstimator.humanReadableUnits(bulkRequest.estimatedSizeInBytes()) + " in bulk req.");
                    }
                    createIndex();
                    BulkResponse bulk;
                    try {
                        updateReferencesForEntitiesInSeparateIndex(
                                entities.stream().map(StoringParameters.StoredEntity::getEntity).collect(Collectors.toList()),
                                indexName,
                                client,
                                objectMapper);

                        bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    } catch (IOException e) {
                        throw new EvitaInsertException(e.getMessage(), e);
                    }
                    if (bulk.hasFailures()) {
                        log.error(bulk.buildFailureMessage());
                    }

                },
                i -> {
                    try {
                        return objectMapper.writeValueAsString(new EntityWrapper(i, indexName));
                    } catch (IOException e) {
                        throw new EvitaInsertException(e.getMessage(), e);
                    }
                });
        StoringParametersDao.getStoringParameters().put(indexName, evitaParameters);
    }

    public static void updateReferencesForEntitiesInSeparateIndex(List<Entity> entities, String indexName, RestHighLevelClient client, ObjectMapper mapper) throws IOException {

        String referenceIndex = indexName + "-reference";
        Map<FacetReferenceDto, Set<String>> facetReferences = new HashMap<>();
        for (Entity entity : entities) {

            for (ReferenceContract reference : entity.getReferences()) {
                if (!reference.isDropped()) {
                    FacetReferenceDto facetReferenceDto = new FacetReferenceDto(reference);

                    Set<String> existingSet = facetReferences.getOrDefault(facetReferenceDto, new HashSet<>());
                    existingSet.add(StringUtils.getUI(entity.getType(), entity.getPrimaryKey()));
                    facetReferences.put(facetReferenceDto, existingSet);
                }
            }
        }

        for (Map.Entry<FacetReferenceDto, Set<String>> facetReferenceDtoEntry : facetReferences.entrySet()) {

            FacetReferenceDto storedSource = null;

            FacetReferenceDto facetReferenceDto = facetReferenceDtoEntry.getKey();
            GetRequest request = new GetRequest(referenceIndex, facetReferenceDto.getReferenceId());
            request.fetchSourceContext(new FetchSourceContext(true, new String[0], new String[0]));
            GetResponse response = client.get(request, RequestOptions.DEFAULT);
            if (response.isExists()) {
                storedSource = mapper.readValue(response.getSourceAsString(), FacetReferenceDto.class);
            }
            if (storedSource == null) storedSource = facetReferenceDto;

            storedSource.getUsedInEntities().addAll(facetReferenceDtoEntry.getValue());

            FacetUtil.getCachedReferences().put(StringUtils.getUI(storedSource.getType(), storedSource.getPrimaryKey()), storedSource);
            client.index(
                    new IndexRequest(referenceIndex)
                            .id(facetReferenceDto.getReferenceId())
                            .source(mapper.writeValueAsString(storedSource), XContentType.JSON), RequestOptions.DEFAULT);

        }

    }


    @Override
    public Set<Serializable> getEntityTypes() {
        try {
            SearchRequest searchRequest = new SearchRequest(getIndexName());
            searchRequest
                    .source()
                    .size(0)
                    .aggregation(
                            AggregationBuilders
                                    .nested("entityTypes", "type")
                                    .subAggregation(
                                            AggregationBuilders
                                                    .terms("types")
                                                    .size(1000)
                                                    .field("type.value.keyword"))

                    );

            final SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            ParsedNested parsedNested = response.getAggregations().get("entityTypes");
            ParsedStringTerms types = parsedNested.getAggregations().get("types");

            return types.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new EvitaGetException(e.getMessage(), e);
        }

    }

    @Nullable
    @Override
    public EsEntityCollection getCollectionForEntity(@Nonnull Serializable entityType) {
        return collectionMap.computeIfAbsent(String.valueOf(entityType), i -> {
            EntitySchema entitySchema;
            try {
                entitySchema = SchemaRepository.getEntitySchema(getIndexName(), entityType);
            } catch (IOException e) {
                log.error("Cannot deserialize scheme!" + entityType + " " + e.getMessage(), e);
                entitySchema = new EntitySchema(entityType);
            }
            return new EsEntityCollection(entitySchema, getIndexName(), client, objectMapper, this::supportsTransaction);
        });
    }

    @Nonnull
    @Override
    public EsEntityCollection getOrCreateCollectionForEntity(@Nonnull Serializable entityType) {
        EntitySchema entitySchema = new EntitySchema(entityType);
        return createCollectionForEntity(entitySchema);
    }

    @Nonnull
    @Override
    public EsEntityCollection createCollectionForEntity(@Nonnull EntitySchema entitySchema) {
        Serializable entityType = entitySchema.getName();
        createIndex();
        if (idGeneratorID.get() == null) {

            try {
                String keyIndex = getIndexName() + "-keys";
                GetRequest getRequest = new GetRequest(keyIndex, "idGen");
                GetResponse existingGenerator = client.get(getRequest, RequestOptions.DEFAULT);
                if (existingGenerator.isExists()) {
                    idGeneratorID.set((String) existingGenerator.getSourceAsMap().get("id"));
                } else {
                    idGeneratorID.set(UUID.randomUUID().toString());
                    IndexRequest indexRequest = new IndexRequest(keyIndex)
                            .id("idGen")
                            .source("{\"id\":\"" + idGeneratorID.get() + "\"}", XContentType.JSON);
                    client.index(indexRequest, RequestOptions.DEFAULT);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

        }

        return collectionMap.computeIfAbsent(String.valueOf(entityType), i -> new EsEntityCollection(entitySchema, getIndexName(), client, objectMapper, this::supportsTransaction));
    }

    @SneakyThrows
    private void createIndex() {
        String indexName = getIndexName();
        createIndex(
                indexName,
                i ->
                {
                    try {
                        ElasticClientUtil.createIndex(
                                client,
                                indexName,
                                IOUtils.toString(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("mapping.json")), StandardCharsets.UTF_8),
                                7000,
                                EsStorageConfiguration.defaultShardSize());
                    } catch (IOException e) {
                        throw new EvitaInsertException("Cannot create index " + e.getMessage(), e);
                    }
                }
        );
        createIndex(
                indexName + "-reference",
                i ->
                {
                    try {
                        ElasticClientUtil.createIndex(
                                client,
                                i,
                                IOUtils.toString(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("referenceMapping.json")), StandardCharsets.UTF_8),
                                200,
                                EsStorageConfiguration.defaultShardSize());
                    } catch (IOException e) {
                        throw new EvitaInsertException("Cannot create index " + e.getMessage(), e);
                    }
                }
        );
        createIndex(
                indexName + "-keys",
                i -> ElasticClientUtil.createIndex(client, i, null, 200, 1)
        );
    }

    private void createIndex(String indexName, Consumer<String> consumer) {
        if (Boolean.FALSE.equals(indexExistsMap.getOrDefault(indexName, false))) {
            consumer.accept(indexName);
            indexExistsMap.put(indexName, true);
        }
    }

    public String getIndexName() {
        return getIndexNameOfCatalog(getName());
    }

    public static String getIndexNameOfCatalog(String catalog) {
        return "evita_" + catalog.toLowerCase();
    }

    @Override
    public boolean deleteCollectionOfEntity(@Nonnull Serializable entityType) {
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest(getIndexName());
            request.setQuery(new TermQueryBuilder("entityType", entityType));
            request.setBatchSize(Integer.MAX_VALUE);
            request.setMaxDocs(Integer.MAX_VALUE);
            request.setTimeout(TimeValue.timeValueMinutes(2));
            request.setRefresh(true);
            BulkByScrollResponse bulkByScrollResponse = client.deleteByQuery(request, RequestOptions.DEFAULT);
            return bulkByScrollResponse.getBulkFailures().isEmpty();
        } catch (IOException e) {
            throw new EvitaInsertException(e.getMessage(), e);
        }
    }

    @SneakyThrows
    @Override
    void flush() {

        String indexName = getIndexName();
        StoringParametersDao.getStoringParameters().get(indexName).flush(false);
        ElasticClientUtil.refreshIndex(client, indexName);
        FacetUtil.getCachedReferences().clear();
        try {
            ElasticClientUtil.indexStatusCheck(client, indexName);
            StoringParametersDao.getGeneratedPrimaryKeys().remove(indexName);
        } catch (IOException e) {
            throw new EvitaEsException(e.getMessage(), e);
        }
    }

    @SneakyThrows
    @Override
    protected void terminate() {
        collectionMap.clear();
        idGeneratorID.set(null);
        SchemaRepository.clear();
        PMPTTAccessor.destroyPMPTT();
        ClientAccessor.getClient().close();
    }

}
