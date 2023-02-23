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

package io.evitadb;

import io.evitadb.api.Evita;
import io.evitadb.api.EvitaBase;
import io.evitadb.api.configuration.CacheOptions;
import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.configuration.EvitaConfiguration;
import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.sequence.SequenceService;
import io.evitadb.test.extension.AbstractDbInstanceParameterResolver;
import org.apache.commons.io.FileUtils;

import java.io.IOException;

/**
 * This is special extension to JUnit platform that:
 *
 * a) spins up new Evita DB instance implementation (i.e. creates reference of {@link EvitaBase} implementation that
 * can be used in tests)
 * b) tears down Evita DB instance after test is finished
 * c) provides dependency injection support for two parameters:
 * - `{@link EvitaBase} evita` parameter - injecting current Evita DB instance to it
 * - `{@link String} catalogName` parameter - injecting current Evita DB catalog name that is expected to be used in test
 *
 * In order this extension works this must be fulfilled:
 *
 * - this class name is referenced in file `/META-INF/services/org.junit.jupiter.api.extension.Extension`
 * - test needs to be run with JVM argument `-Djunit.jupiter.extensions.autodetection.enabled=true`
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DbInstanceParameterResolver extends AbstractDbInstanceParameterResolver<Evita> {

	@Override
	protected Evita createEvita(String catalogName) {
		SequenceService.reset();
		return new Evita(
			new EvitaConfiguration(),
			new EvitaCatalogConfiguration(
				catalogName, STORAGE_PATH,
				new StorageOptions(1),
				new CacheOptions()
			)
		);
	}

	@Override
	protected void destroyEvitaData() throws IOException {
		FileUtils.deleteDirectory(STORAGE_PATH.toFile());
	}

}