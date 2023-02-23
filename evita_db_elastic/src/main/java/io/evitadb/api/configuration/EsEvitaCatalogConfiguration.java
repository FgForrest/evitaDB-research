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

package io.evitadb.api.configuration;

import io.evitadb.storage.configuration.EsStorageConfiguration;
import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@Data
public class EsEvitaCatalogConfiguration implements CatalogConfiguration {
	private final String name;
	private final HttpHost host;
	private final Credentials credentials;

	public EsEvitaCatalogConfiguration(String name) {
		this.name = name;
		this.host = EsStorageConfiguration.defaultHostSupplier();
		this.credentials = EsStorageConfiguration.defaultCredentialsSupplier();
	}

	public EsEvitaCatalogConfiguration(String name, HttpHost host, Credentials credentials) {
		this.name = name;
		this.host = host;
		this.credentials = credentials;
		EsStorageConfiguration.setCachedCredentials(credentials);
		EsStorageConfiguration.setCachedHost(host);
	}
}
