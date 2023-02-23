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

package io.evitadb.storage.configuration.accessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.configuration.ElasticClientUtil;
import io.evitadb.storage.exception.EvitaInsertException;
import io.evitadb.storage.pmptt.EsHierarchyStorage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.oss.pmptt.PMPTT;
import one.edee.oss.pmptt.model.Hierarchy;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.io.Serializable;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
@CommonsLog
public class PMPTTAccessor {

    public static final String HIERARCHY_INDEX = "-hierarchy_index";
    private static PMPTT pmptt;
    private static EsHierarchyStorage hierarchyStorage = new EsHierarchyStorage();
    private static String indexName;

    public static Hierarchy getHierarchy(Serializable type) {
        Assert.notNull(pmptt, "You must initialize PMPTT first!!");
        String hierarchyCode = indexName + "_" + type;
        Hierarchy hierarchy = hierarchyStorage.getHierarchy(hierarchyCode);
        if (hierarchy == null) {

            PMPTTAccessor.initPMPTT(ClientAccessor.getClient(), indexName, OMAccessor.getObjectMapper());
            Hierarchy newHierarchy = hierarchyStorage.getHierarchy(hierarchyCode);
            if (newHierarchy == null) {
                newHierarchy = new Hierarchy(hierarchyCode, (short) 9, (short) 30);
                hierarchyStorage.createHierarchy(newHierarchy);
            }
            return newHierarchy;
        } else {
            return hierarchy;
        }
    }

    public static PMPTT initPMPTT() {
        if (pmptt == null) {

            hierarchyStorage = new EsHierarchyStorage();
            pmptt = new PMPTT(hierarchyStorage);
        }
        return pmptt;
    }

    public static void destroyPMPTT() {
        pmptt.removeHierarchy(indexName);
    }

    public static void initPMPTT(RestHighLevelClient client, String iName, ObjectMapper mapper) {
        indexName = iName;
        try {
            IndicesClient indices = client.indices();
            String hierarchyIndexName = indexName + HIERARCHY_INDEX;
            if (!indices.exists(new GetIndexRequest(hierarchyIndexName), RequestOptions.DEFAULT)) {
                createHierarchyIndex(client, indices);
            }

            ElasticClientUtil.indexStatusCheck(client, hierarchyIndexName);

            SearchRequest searchRequest = new SearchRequest(hierarchyIndexName);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQuery().must(matchAllQuery()));
            searchSourceBuilder.fetchSource(true);
            searchSourceBuilder.size(10000);
            searchSourceBuilder.from(0);
            searchRequest.source(searchSourceBuilder);

            SearchResponse existingHierarchy = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] hits = existingHierarchy.getHits().getHits();
            if (hits.length != 0) {
                for (SearchHit hit : hits) {
                    Hierarchy hierarchy = mapper.readValue(hit.getSourceAsString(), Hierarchy.class);

                    EsHierarchyStorage newHS = (EsHierarchyStorage) hierarchy.getStorage();
                    if (pmptt == null) {
                        hierarchyStorage = newHS;
                        pmptt = new PMPTT(hierarchyStorage);
                    } else {
                        hierarchyStorage.getHierarchyIndex().put(hierarchy.getCode(), newHS.getHierarchyIndex().get(hierarchy.getCode()));
                    }
                }
            } else {
                initPMPTT();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

    public static void storePMPTT(RestHighLevelClient client, Serializable type, String indexName, ObjectMapper mapper) {

        Hierarchy hierarchy = getHierarchy(type);

        String hierarchyIndexName = indexName + HIERARCHY_INDEX;
        IndexRequest indexRequest;
        try {
            String source = mapper.writeValueAsString(hierarchy);
            IndicesClient indices = client.indices();
            if (!indices.exists(new GetIndexRequest(hierarchyIndexName), RequestOptions.DEFAULT)) {
                createHierarchyIndex(client, indices);
            }
            ElasticClientUtil.indexStatusCheck(client, hierarchyIndexName);
            indexRequest = new IndexRequest(hierarchyIndexName)
                    .id(indexName + "_" + type)
                    .source(source, XContentType.JSON);
            client.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new EvitaInsertException("Cannot store pmptt!", e);
        }

    }

    private static void createHierarchyIndex(RestHighLevelClient client, IndicesClient indices) throws IOException {
        try {

            String hierarchyIndexName = indexName + HIERARCHY_INDEX;
            CreateIndexRequest request = new CreateIndexRequest(hierarchyIndexName);
            request.settings(Settings.builder()
                    .put("index.number_of_shards", 10)
                    .put("index.number_of_replicas", 0)
                    .put("index.mapping.total_fields.limit", 7000)
                    .put("index.mapping.nested_objects.limit", 20000)
            );
            request.mapping(
                    "{\"properties\":{\"rootItems\":{\"type\":\"object\",\"enabled\":false}}}",
                    XContentType.JSON);
            indices.create(request, RequestOptions.DEFAULT);
            ElasticClientUtil.indexStatusCheck(client, hierarchyIndexName);
            indices.refresh(new RefreshRequest(hierarchyIndexName), RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (e.getMessage().contains("resource_already_exists_exception")) {
                //ignored
                return;
            }
            throw e;
        }
    }

}
