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

import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.exception.ConcurrentInitializationException;
import io.evitadb.api.exception.InstanceTerminatedException;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.utils.Assert;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Evita is a specialized database with easy-to-use API for e-commerce systems. Purpose of this research is creating fast
 * and scalable engine that handles all complex tasks that e-commerce systems has to deal with on daily basis. Evita should
 * operate as a fast secondary lookup / search index used by application frontends. We aim for order of magnitude better
 * latency (10x faster or better) for common e-commerce tasks than other solutions based on SQL or NoSQL databases on the
 * same hardware specification. Evita should not be used for storing and handling primary data, and we don't aim for ACID
 * properties nor data corruption guarantees. Evita "index" must be treated as something that could be dropped any time and
 * built up from scratch easily again.
 * <p>
 * This class represents main entrance to the Evita DB contents.
 * Implementation is thread safe.
 * <p>
 * Research note: this abstract class is expected to be extended by each implementation team.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public abstract class EvitaBase<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> implements AutoCloseable {
	/**
	 * Index of {@link CatalogBase} that holds data specific to the catalog.
	 *
	 * @see CatalogBase
	 */
	final Map<String, CATALOG> catalogIndexes;
	/**
	 * Keeps information about currently active sessions.
	 */
	final Set<SESSION> activeSessions = new ConcurrentSkipListSet<>();
	/**
	 * Keeps information about count of currently active sessions. Counter is used to safely control single session
	 * limits in parallel execution.
	 */
	final AtomicInteger activeSessionsCounter = new AtomicInteger();
	/**
	 * Flag that is se to TRUE when Evita. is ready to serve application calls.
	 * Aim of this flag is to refuse any calls after {@link #close()} method has been called.
	 */
	private boolean active;

	/**
	 * Creates new instance of E.V.E. with catalog indexes according passed configuration.
	 */
	@SafeVarargs
	protected EvitaBase(CONFIGURATION... configs) {
		catalogIndexes = new ConcurrentHashMap<>(
			Arrays.stream(configs)
				.map(this::createCatalog)
				.peek(catalog -> log.info("Catalog {} fully loaded.", catalog.getName()))
				.collect(Collectors.toMap(CatalogBase::getName, Function.identity()))
		);
		active = true;
	}

	/**
	 * Terminates Evita instance, releases all resources, locks and cleans memory.
	 * This method is idempotent and may be called multiple times. Only first call is really processed and others are
	 * ignored.
	 */
	@Override
	public void close() {
		assertActive();
		active = false;
		final Iterator<SESSION> sessionIt = activeSessions.iterator();
		while (sessionIt.hasNext()) {
			final SESSION activeSession = sessionIt.next();
			activeSession.close();
			activeSessionsCounter.decrementAndGet();
			sessionIt.remove();
			log.info("There is still active session {} - terminating.", activeSession.getId());
		}
		final Iterator<CATALOG> it = catalogIndexes.values().iterator();
		while (it.hasNext()) {
			final CatalogBase<REQUEST, CONFIGURATION, COLLECTION> index = it.next();
			index.terminate();
			it.remove();
			log.info("Catalog {} successfully terminated.", index.getName());
		}
	}

	/**
	 * Creates {@link EvitaSessionBase} for querying the database. All query results in the same session
	 * are cached and entities returned in different queries are the same (in the sense of identity entityA == entityB).
	 * <p>
	 * Don't forget to {@link #close()} or {@link #terminateSession(EvitaSessionBase)} when your work with Evita is finished.
	 * EvitaSession is not is not thread safe!
	 *
	 * @param catalogName - unique name of the catalog, refers to {@link CatalogBase#getName()}
	 * @return new instance of EvitaSession
	 * @see #close()
	 */
	@Nonnull
	public SESSION createReadOnlySession(@Nonnull String catalogName) {
		return createSession(catalogName, (catalog, catalogConsumer) -> this.createReadOnlySession(catalog));
	}

	/**
	 * Creates {@link EvitaSessionBase} for querying and altering the database. All query results in the same session
	 * are cached and entities returned in different queries are the same (in the sense of identity entityA == entityB).
	 * <p>
	 * Don't forget to {@link #close()} or {@link #terminateSession(EvitaSessionBase)} when your work with Evita is finished.
	 * EvitaSession is not is not thread safe!
	 *
	 * @param catalogName - unique name of the catalog, refers to {@link CatalogBase#getName()}
	 * @return new instance of EvitaSession
	 * @see #close()
	 */
	@Nonnull
	public SESSION createReadWriteSession(@Nonnull String catalogName) {
		return createSession(catalogName, this::createReadWriteSession);
	}

	/**
	 * Terminates existing {@link EvitaSessionBase}. When this method is called no additional calls to this EvitaSession
	 * is accepted and all will terminate with {@link io.evitadb.api.exception.InstanceTerminatedException}.
	 */
	public void terminateSession(@Nonnull SESSION session) {
		assertActive();
		session.close();
	}

	/**
	 * Executes querying logic in the newly created Evita session. Session is safely closed at the end of this method
	 * and result is returned.
	 * <p>
	 * Query logic is intended to be read-only. For read-write logic use {@link #updateCatalog(String, Function)} or
	 * open a transaction manually in the logic itself.
	 */
	public <T> T queryCatalog(String catalogName, Function<SESSION, T> queryLogic) {
		assertActive();
		try (final SESSION session = createSession(catalogName, (catalog, catalogConsumer) -> this.createReadOnlySession(catalog))) {
			return queryLogic.apply(session);
		}
	}

	/**
	 * Executes catalog read-write logic in the newly Evita session. When logic finishes without exception, changes are
	 * committed to the index, otherwise changes are roll-backed and no data is affected. Changes made by the updating
	 * logic are visible only within update function. Other threads outside the logic function work with non-changed
	 * data until transaction is committed to the index.
	 * <p>
	 * Current version limitation:
	 * Only single updater can execute in parallel (i.e. updates are expected to be invoked by single thread in serial way).
	 *
	 * @param updater application logic that reads and writes data
	 */
	public <T> T updateCatalog(String catalogName, Function<SESSION, T> updater) {
		assertActive();
		try (final SESSION session = createSession(catalogName, this::createReadWriteSession)) {
			return session.execute(updater);
		}
	}

	/**
	 * Overloaded method {@link #updateCatalog(String, Function)} that returns no result.
	 *
	 * @see #updateCatalog(String, Function)
	 */
	public void updateCatalog(String catalogName, Consumer<SESSION> updater) {
		updateCatalog(
			catalogName,
			evitaSession -> {
				updater.accept(evitaSession);
				return null;
			}
		);
	}

	/**
	 * Creates new catalog instance according to passed configuration.
	 *
	 * @param config configuration for the catalog instance
	 * @return new instance of {@link CatalogBase} implementation
	 */
	protected abstract CATALOG createCatalog(@Nonnull CONFIGURATION config);

	/**
	 * Creates new instance of {@link EvitaSessionBase} that can read and write data to passed catalog instance.
	 *
	 * @param catalog                current version of {@link CatalogBase} database
	 * @param updatedCatalogCallback a callback function that accepts new instance with updated catalog contents and update {@link EvitaBase} contents
	 */
	protected abstract SESSION createReadWriteSession(@Nonnull CATALOG catalog, @Nonnull Consumer<CATALOG> updatedCatalogCallback);

	/**
	 * Creates new instance of {@link EvitaSessionBase} that can only read data from passed catalog instance.
	 *
	 * @param catalog current version of {@link CatalogBase} database
	 */
	protected abstract SESSION createReadOnlySession(@Nonnull CATALOG catalog);

	/**
	 * Creates new session in passed catalog and performs infrastructural checks necessary for opening the session.
	 */
	@Nonnull
	private SESSION createSession(@Nonnull String catalogName, @Nonnull BiFunction<CATALOG, Consumer<CATALOG>, SESSION> sessionOpener) {
		assertActive();
		final CATALOG catalogInstance = getCatalogInstance(catalogName);
		if (catalogInstance.supportsTransaction()) {
			activeSessionsCounter.incrementAndGet();
		} else if (!activeSessionsCounter.compareAndSet(0, 1)) {
			throw new ConcurrentInitializationException("Cannot create more than single parallel session in warming up state!");
		}
		final SESSION session = sessionOpener.apply(catalogInstance, this::replaceCatalogReference);
		activeSessions.add(session);
		session.addTerminationCallback(it -> {
			activeSessionsCounter.decrementAndGet();
			activeSessions.remove(it);
		});
		return session;
	}

	/**
	 * Replaces current catalog reference with updated one. Catalogs
	 * @param catalog
	 */
	private void replaceCatalogReference(@Nonnull CATALOG catalog) {
		Assert.notNull(catalog, "Sanity check.");
		// replace catalog only when reference/pointer differs
		if (this.catalogIndexes.get(catalog.getName()) != catalog) {
			// catalog indexes are ConcurrentHashMap - we can do it safely here
			this.catalogIndexes.put(catalog.getName(), catalog);
		}
	}

	/**
	 * Returns catalog instance for passed catalog name or throws exception.
	 *
	 * @throws IllegalArgumentException when no catalog of such name is found
	 */
	@Nonnull
	private CATALOG getCatalogInstance(String catalog) throws IllegalArgumentException {
		return ofNullable(catalogIndexes.get(catalog))
			.orElseThrow(() -> new IllegalArgumentException("Catalog " + catalog + " is not known to Evita!"));
	}

	/**
	 * Verifies this instance is still active.
	 */
	private void assertActive() {
		if (!active) {
			throw new InstanceTerminatedException("Evita instance has been already terminated! No calls are accepted since all resources has been released.");
		}
	}

}
