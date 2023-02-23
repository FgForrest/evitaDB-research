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

import io.evitadb.api.configuration.EsEvitaCatalogConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestHighLevelClientBuilder;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class ClientAccessor {

    @Getter
    private static RestHighLevelClient client;


    public static RestHighLevelClient initClient(EsEvitaCatalogConfiguration configuration) {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, configuration.getCredentials());
        RestClientBuilder clientBuilder = RestClient
                .builder(configuration.getHost())
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setMaxConnTotal(Integer.MAX_VALUE)
                                .setMaxConnPerRoute(Integer.MAX_VALUE)
                )
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(30000)
                                .setConnectionRequestTimeout(500000)
                                .setSocketTimeout(500000)
                );
        RestHighLevelClientBuilder restHighLevelClientBuilder = new RestHighLevelClientBuilder(clientBuilder.build());
        restHighLevelClientBuilder.setApiCompatibilityMode(true);
        client = restHighLevelClientBuilder.build();

        return client;
    }
}
