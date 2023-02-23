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

package io.evitadb.check;

import io.evitadb.api.*;
import io.evitadb.api.configuration.CacheOptions;
import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.configuration.EvitaConfiguration;
import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.test.TestFileSupport;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;

/**
 * Concrete implementation of {@link SanityChecker} that targets in-memory implementation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class InMemorySanityChecker extends SanityChecker<EvitaRequest, EvitaCatalogConfiguration, EntityCollection, Catalog, Transaction, EvitaSession>
	implements TestFileSupport {

	public static void main(String[] args) throws FileNotFoundException {
		new InMemorySanityChecker().execute(args);
	}

	@Override
	protected Evita createEvitaInstance(@Nonnull String catalogName) {
		return new Evita(
			new EvitaConfiguration(),
			new EvitaCatalogConfiguration(
				catalogName,
				getTestDirectory().resolve(catalogName),
				new StorageOptions(64),
				new CacheOptions(
					ReflectionCachingBehaviour.CACHE,
					true,
					10,
					100_000,
					100_000,
					2,
					0,
					0.75f
				)
			)
		);
	}
}
