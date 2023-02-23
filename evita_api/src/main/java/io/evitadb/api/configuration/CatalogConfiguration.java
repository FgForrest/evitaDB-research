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

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EvitaBase;

/**
 * Catalog configuration contains all configuration options available for {@link CatalogBase} tuning.
 * Because we start with multiple "prototype" implementations, this configuration is empty interface placeholder that
 * will be replaced by configuration specific to each prototype.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface CatalogConfiguration {

	/**
	 * Returns name of the {@link CatalogBase} instance this configuration refers to. Name must be unique across all catalogs
	 * inside same {@link EvitaBase} instance.
	 */
	String getName();

}
