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

package io.evitadb.storage.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.configuration.ElasticClientUtil;
import io.evitadb.storage.configuration.EsStorageConfiguration;
import io.evitadb.storage.configuration.accessor.ClientAccessor;
import io.evitadb.storage.configuration.accessor.OMAccessor;
import io.evitadb.storage.exception.EvitaGetException;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.codec.binary.Base64;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@Data
@CommonsLog
public class SchemaRepository {
    private SchemaRepository() {
    }

    public static final String SCHEMES_POSTFIX = "-schemes";
    private static final Map<Serializable, EntitySchema> schemaMap = new HashMap<>();

    public static void updateSchemaInEs(String indexName, EntitySchema schema) {
        String type = norm(schema.getName());
        ObjectMapper objectMapper = OMAccessor.getSchemaObjectMapper();
        EntitySchema oldOne = schemaMap.get(type);
        if (oldOne == null || oldOne.getVersion() != schema.getVersion()){
            try {
                RestHighLevelClient client = ClientAccessor.getClient();
                ElasticClientUtil.createIndex(client,indexName+SCHEMES_POSTFIX, null,50000, EsStorageConfiguration.defaultShardSize());

                String schemeSource = objectMapper.writeValueAsString(schema);
                IndexRequest indexRequest = new IndexRequest(indexName+SCHEMES_POSTFIX)
                        .id("evita_schema" + type)
                        .source(Collections.singletonMap("sourceAsBytes", Base64.encodeBase64((schemeSource.getBytes(StandardCharsets.UTF_8)))));
                client.index(indexRequest, RequestOptions.DEFAULT);
                schemaMap.put(type, schema);
            } catch (IOException e) {
                throw new EvitaGetException("Scheme not found for type: " + type);
            }
        } // else ignored
    }

    public static EntitySchema getEntitySchema(String indexName, @Nonnull Serializable entityType) throws IOException {
        String type = norm(entityType);
        EntitySchema entitySchema = schemaMap.get(type);
        if (entitySchema == null) {
            GetRequest getRequest = new GetRequest(indexName+SCHEMES_POSTFIX, "evita_schema" + type);
            GetResponse existingSchema = ClientAccessor.getClient().get(getRequest, RequestOptions.DEFAULT);
            if (existingSchema.isExists()) {
                byte[] sourceAsBytes = ((String)existingSchema.getSourceAsMap().get("sourceAsBytes")).getBytes(StandardCharsets.UTF_8);
                schemaMap.put(type, OMAccessor.getSchemaObjectMapper().readValue(new String(Base64.decodeBase64(Base64.decodeBase64((sourceAsBytes)))), new TypeReference<>() {
                }));
                entitySchema = schemaMap.get(type);
            }
        }

        if (entitySchema != null) {
            return entitySchema;
        }

        throw new EvitaGetException("Scheme not found for type: " + type);
    }

    public static void clear(){
        schemaMap.clear();
    }

    private static String norm(Serializable type){
        return type.toString().toLowerCase();
    }

}
