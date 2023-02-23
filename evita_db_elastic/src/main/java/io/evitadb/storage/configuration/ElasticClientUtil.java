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

package io.evitadb.storage.configuration;

import io.evitadb.api.EsCatalog;
import io.evitadb.api.utils.Assert;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.function.Predicate;

import static io.evitadb.storage.configuration.accessor.PMPTTAccessor.HIERARCHY_INDEX;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@CommonsLog
@NoArgsConstructor(access = AccessLevel.NONE)
public class ElasticClientUtil {

    public static void cleanAllEsData(String catalogName) {
        doWithClient(client -> {
            try {
                String indexNameOfCatalog = catalogName == null ? null : EsCatalog.getIndexNameOfCatalog(catalogName);
                IndicesClient indices = client.indices();
                GetIndexResponse response = indices.get(new GetIndexRequest("evita_*"), RequestOptions.DEFAULT);
                for (String index : response.getIndices()) {
                    if (index.startsWith(indexNameOfCatalog == null ? "evita_" : indexNameOfCatalog)) {
                        removeIndexData(client, index);
                    }
                }
                removeHierarchyIndexes(indices,indexNameOfCatalog,client);

            } catch (IOException e) {
                throw new IllegalStateException("Elastic not ready : " + e.getMessage(), e);
            }
            return true;
        });
    }

    private static void removeHierarchyIndexes(IndicesClient indices, String indexNameOfCatalog, RestHighLevelClient client) throws IOException {
        if (indices.exists(new GetIndexRequest(HIERARCHY_INDEX), RequestOptions.DEFAULT)) {

            if (indexNameOfCatalog == null) {
                ElasticClientUtil.removeIndexData(client, HIERARCHY_INDEX);
            } else {
                DeleteRequest request = new DeleteRequest(HIERARCHY_INDEX);
                request.id(indexNameOfCatalog);
                request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                client.delete(request, RequestOptions.DEFAULT);
            }
        }
    }

    public static boolean doWithClient(Predicate<RestHighLevelClient> consumer) {
//		// delete testing data
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, EsStorageConfiguration.defaultCredentialsSupplier());
        RestHighLevelClientBuilder restHighLevelClientBuilder = new RestHighLevelClientBuilder(
                RestClient
                        .builder(EsStorageConfiguration.defaultHostSupplier())
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                        .build());
        restHighLevelClientBuilder.setApiCompatibilityMode(true);
        try (RestHighLevelClient restHighLevelClient = restHighLevelClientBuilder.build()){
            return consumer.test(restHighLevelClient);
        } catch (Exception e) {
            // ignored
            if (!e.getMessage().contains("index_not_found_exception")) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    public static void removeIndexData(RestHighLevelClient client, String index) throws IOException {
        // close all resources here, here we just purge all data

        client.indices().refresh(new RefreshRequest(index), RequestOptions.DEFAULT);

        DeleteByQueryRequest req = new DeleteByQueryRequest(index);
        req.setWaitForActiveShards(ActiveShardCount.ALL);
        req.setQuery(QueryBuilders.matchAllQuery());
        req.setAbortOnVersionConflict(false);
        log.info("Removing index : " + index);
        client.deleteByQuery(req, RequestOptions.DEFAULT);
        client.indices().refresh(new RefreshRequest(index), RequestOptions.DEFAULT);
        client.deleteByQuery(req, RequestOptions.DEFAULT);
    }

    public static void createIndex(RestHighLevelClient client, String indexName, String mapping, int totalFieldsLimit, int numberOfShards) {
        try {
            if (!client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT)) {
                CreateIndexRequest request = new CreateIndexRequest(indexName);
                Settings.Builder settings = Settings.builder()
                        .put("index.number_of_shards", numberOfShards)
                        .put("index.number_of_replicas", 0)
                        .put("index.mapping.total_fields.limit", totalFieldsLimit)
                        .put("index.mapping.nested_objects.limit", 20000)
                        .put("index.max_result_window", Integer.MAX_VALUE);
                request.settings(settings);

                if (mapping != null) {
                    request.mapping(mapping, XContentType.JSON);
                }
                CreateIndexResponse indexResponse = client.indices().create(request, RequestOptions.DEFAULT);
                ElasticClientUtil.indexStatusCheck(client, indexName);
                Assert.isTrue(indexName.equals(indexResponse.index()), "Created index name (" + indexResponse.index() + ") doesn't match origin name (" + indexName + ")");
            }
        } catch (ElasticsearchStatusException e) {
            if (!e.getMessage().contains("resource_already_exists_exception")) {
                throw e;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static boolean isCatalogInitialized(String catalogName) {
        return doWithClient(client -> {
            try {
                CountResponse count = client.count(
                        new CountRequest(new String[]{EsCatalog.getIndexNameOfCatalog(catalogName)},
                                QueryBuilders.boolQuery()
                                        .must(
                                                QueryBuilders.existsQuery("primaryKey")
                                        )
                        ),
                        RequestOptions.DEFAULT);
                return count.getCount() > 0;
            } catch (IOException e) {
                return false;
            }
        });
    }

    public static void indexStatusCheck(RestHighLevelClient client, String index) throws IOException {
        ClusterHealthRequest healthRequest = new ClusterHealthRequest(index);
        healthRequest = healthRequest.waitForYellowStatus();
        healthRequest = healthRequest.waitForActiveShards(1);
        ClusterHealthResponse response = client.cluster().health(healthRequest, RequestOptions.DEFAULT);
        if (response.getStatus() == ClusterHealthStatus.RED) {
            throw new IllegalStateException("Index not ready");
        }
    }

    public static void refreshIndex(RestHighLevelClient client, String indexName) {
        try {
            client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}
