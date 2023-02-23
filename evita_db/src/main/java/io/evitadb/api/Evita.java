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

package io.evitadb.api;

import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.configuration.EvitaConfiguration;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.scheduling.Scheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This is main entrance to the Evita DB. All basic methods are specified in the superclass {@link EvitaBase}.
 * Implementations just need to implement access to the {@link Catalog} and work with {@link EvitaSession}.
 *
 * Currently it references simplistic memory implementation - each implementation should go its way.
 *
 * TOBEDONE JNO - ALTER MODEL + SEARCH ALGORITHM TO SUPPORT GROUPING ON ENTITY LEVEL (SEE LATEST RESEARCH IN E-SHOP PROJECT)
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public class Evita extends EvitaBase<EvitaRequest, EvitaCatalogConfiguration, EntityCollection, Catalog, Transaction, EvitaSession> {
	/**
	 * Field contains the global - shared configuration for entire Evita instance.
	 */
	@Getter private final EvitaConfiguration globalConfiguration;
	/**
	 * Field contains reference to the scheduler that maintains shared Evita asynchronous tasks used for maintenance
	 * operations.
	 */
	private final Scheduler scheduler;

	public Evita(@Nonnull EvitaConfiguration globalConfiguration, @Nonnull EvitaCatalogConfiguration... configs) {
		super();
		this.globalConfiguration = globalConfiguration;
		this.scheduler = new Scheduler(globalConfiguration);
		Arrays.stream(configs)
			.map(this::createCatalog)
			.forEach(it -> {
				log.info("Catalog {} fully loaded.", it.getName());
				this.catalogIndexes.put(it.getName(), it);
			});
	}

	@Override
	public void close() {
		super.close();
		this.scheduler.terminate();
	}

	@Override
	protected Catalog createCatalog(@Nonnull EvitaCatalogConfiguration config) {
		return new Catalog(
			config,
			scheduler,
			new ReflectionLookup(
				config.getCacheOptions().getReflection()
			)
		);
	}

	@Override
	protected EvitaSession createReadWriteSession(@Nonnull Catalog catalog, @Nonnull Consumer<Catalog> updatedCatalogCallback) {
		final EvitaSession readWriteSession = new EvitaSession(catalog, updatedCatalogCallback);
		catalog.increaseReadWriteSessionCount();
		readWriteSession.addTerminationCallback(session -> catalog.decreaseReadWriteSessionCount());
		return readWriteSession;
	}

	@Override
	protected EvitaSession createReadOnlySession(@Nonnull Catalog catalog) {
		return new EvitaSession(catalog);
	}

}
