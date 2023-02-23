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

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

/**
 * This configuration is expected to contain main configuration options for Evita DB implementation. Ie. target directorys
 * ports, passwords, tokens ans so forth.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
@RequiredArgsConstructor
public class EvitaCatalogConfiguration implements CatalogConfiguration {
	/**
	 * Name of the catalog (must be unique among other catalogs).
	 */
	private final String name;
	/**
	 * Directory on local disk where Evita files are stored.
	 * By default, temporary directory is used - but it is highly recommended setting your own directory if you
	 * don't want to lose the data.
	 */
	private Path storageDirectory = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "evita");
	/**
	 * Cache options contain settings crucial for Evita caching and cache invalidation.
	 */
	private CacheOptions cacheOptions;
	/**
	 * This field contains all options related to underlying key-value store.
	 */
	private StorageOptions storageOptions;

	public EvitaCatalogConfiguration(@Nonnull String name, @Nonnull Path storageDirectory, @Nonnull StorageOptions storageOptions, @Nonnull CacheOptions cacheOptions) {
		this.name = name;
		this.storageDirectory = storageDirectory;
		this.storageOptions = storageOptions;
		this.cacheOptions = cacheOptions;
	}

}
