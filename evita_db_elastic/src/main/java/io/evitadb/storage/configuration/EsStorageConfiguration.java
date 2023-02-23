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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class EsStorageConfiguration {
    public static final String ES_INDEX_SHARDS_SIZE = "shardSize";
    public static final String ES_HOST_SYSTEM_PROPERTY = "dbhost";
    public static final String ES_PORT_SYSTEM_PROPERTY = "dbport";
    public static final String ES_SCHEME_SYSTEM_PROPERTY = "dbScheme";
    public static final String ES_PASS_SYSTEM_PROPERTY = "dbPass";
    public static final String ES_USER_SYSTEM_PROPERTY = "dbUser";
    public static final String DEFAULT_PASS = "xEu5qtDytAG1zOKPeiv1eWJqA6";
    public static final String DEFAULT_USER = "elastic";
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_SCHEME = "http";
    public static final int DEFAULT_PORT = 9200;


    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PUBLIC)
    private static HttpHost cachedHost;


    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PUBLIC)
    private static Credentials cachedCredentials;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PUBLIC)
    private static Integer cachedShardSize;

    public static Credentials defaultCredentialsSupplier(){
        if (cachedCredentials == null){
            String esPassProperty = System.getProperty(ES_PASS_SYSTEM_PROPERTY);
            String esUserProperty = System.getProperty(ES_USER_SYSTEM_PROPERTY);
            cachedCredentials = new UsernamePasswordCredentials(
                    esUserProperty != null ? esUserProperty : DEFAULT_USER,
                    esPassProperty != null ? esPassProperty : DEFAULT_PASS
            );
        }
        return cachedCredentials;
    }

    public static Integer defaultShardSize(){
        if (cachedShardSize == null){
            String specifiedShardSize = System.getProperty(ES_INDEX_SHARDS_SIZE);
            int shardSize = 1;
            if (specifiedShardSize != null){
                try{
                    shardSize = Integer.parseInt(specifiedShardSize);
                }catch (Exception e){
                    throw new IllegalArgumentException("Cannot parse shard size: " + specifiedShardSize + " due to : " + e.getMessage(),e);
                }
            }
            cachedShardSize = shardSize;
        }
        return cachedShardSize;
    }


    public static HttpHost defaultHostSupplier(){
        if (cachedHost == null){
            String esHostNameProperty = System.getProperty(ES_HOST_SYSTEM_PROPERTY);
            String esPortProperty = System.getProperty(ES_PORT_SYSTEM_PROPERTY);
            String esSchemeProperty = System.getProperty(ES_SCHEME_SYSTEM_PROPERTY);
            cachedHost = new HttpHost(
                    esHostNameProperty != null ? esHostNameProperty : DEFAULT_HOST,
                    esPortProperty != null ? Integer.parseInt(esPortProperty) : DEFAULT_PORT,
                    esSchemeProperty != null ? esSchemeProperty : DEFAULT_SCHEME
            );
        }
        return cachedHost;
    }

}
