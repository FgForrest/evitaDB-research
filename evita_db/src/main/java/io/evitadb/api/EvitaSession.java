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
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityDecorator;
import io.evitadb.api.exception.CollectionNotFoundException;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.GlobalEntityIndex;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import static io.evitadb.api.query.QueryConstraints.entities;
import static io.evitadb.api.query.QueryConstraints.require;
import static java.util.Optional.ofNullable;

/**
 * Session are created by the clients to envelope a "piece of work" with Evita DB. In web environment it's a good idea
 * to have session per request, in batch processing it's recommended to keep session per "record page" or "transaction".
 * There may be multiple {@link Transaction transactions} during single session instance life but there is no support
 * for transactional overlap - there may be at most single transaction open in single session. Session also caches
 * response to the queries - when client reads twice the same entity within the session (in different queries) he/she
 * should obtain the same reference to that entity (i.e. same in the sense of ==). Once transaction is committed cached
 * results are purged.
 *
 * Note for implementors: transactional access doesn't need to be implemented in the early stages
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EvitaSession extends EvitaSessionBase<EvitaRequest, EvitaCatalogConfiguration, EntityCollection, Catalog, Transaction> {
	/**
	 * Contains internal cache of already fetched entities within the same session.
	 * TOBEDONE JNO - this needs to be filled in during read operations and purged by mutations.
	 *
	 * private final Map<Serializable, IntMap<SealedEntity>> fetchedEntities = new HashMap<>();
	 */

	protected EvitaSession(@Nonnull Catalog catalog) {
		super(catalog);
	}

	protected EvitaSession(@Nonnull Catalog catalog, @Nonnull Consumer<Catalog> updatedCatalogCallback) {
		super(catalog, updatedCatalogCallback);
	}

	@Override
	public void clearCache() {
		// no cache in the simplistic implementation
	}

	@Nonnull
	@Override
	protected <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull EvitaRequest evitaRequest, @Nonnull EntityCollection entityCollection) {
		return entityCollection.getEntities(evitaRequest, this);
	}

	@Override
	protected EvitaRequest createEvitaRequest(@Nonnull Query query, @Nonnull ZonedDateTime alignedNow) {
		return new EvitaRequest(query, alignedNow);
	}

	@Override
	protected Transaction createTransaction() {
		Assert.isTrue(!isReadOnly(), "Evita session is read only!");
		return new Transaction(
			catalog,
			updatedCatalog -> {
				this.catalog = updatedCatalog;
				ofNullable(this.updatedCatalogCallback)
					.ifPresent(it -> it.accept(updatedCatalog));
			},
			this::clearCache
		);
	}

	/**
	 * Method returns true if the session is in read-only mode. That means no transaction is opened and no data will
	 * be modified within this session. Read-only sessions allow more aggressive optimizations, such as using cached
	 * results.
	 */
	public boolean isReadOnly() {
		return this.updatedCatalogCallback == null;
	}

	/**
	 * Returns the last {@link Catalog#getLastCommittedTransactionId() transaction id} that has been fully committed
	 * to the catalog.
	 */
	public long getCatalogLastCommittedTransactionId() {
		return catalog.getLastCommittedTransactionId();
	}

	/**
	 * Method returns entity with additionally loaded data specified by requirements in second argument. This method
	 * is particularly useful for implementation of lazy loading when application loads only parts of the entity it
	 * expects to be required for handling common client request and then load additional data if processing requires
	 * more in-depth view of the entity.
	 */
	public SealedEntity enrichOrLimitEntity(@Nonnull SealedEntity partiallyLoadedEntity, EntityContentRequire... require) {
		assertActive();
		final Serializable entityType = partiallyLoadedEntity.getType();
		final EntityCollection entityCollection = ofNullable(catalog.getCollectionForEntity(entityType))
			.orElseThrow(() -> new CollectionNotFoundException(entityType));
		Assert.isTrue(partiallyLoadedEntity instanceof EntityDecorator, "Expected entity decorator in the input.");
		final EntityDecorator entityDecorator = (EntityDecorator) partiallyLoadedEntity;
		final EvitaRequest evitaRequest = createEvitaRequest(
			Query.query(
				entities(entityType),
				require(require)
			),
			ZonedDateTime.now()
		);
		return entityCollection.limitEntity(
			entityCollection.enrichEntity(entityDecorator, evitaRequest),
			evitaRequest
		);
	}

	/**
	 * TOBEDONE JNO - this method needs to be part of the internal API, we need to migrate EvitaSession to interface
	 * and create specific implementation that will have these internal method available
	 *
	 * Method returns {@link GlobalEntityIndex} for entity collection of specified `entityType`.
	 */
	@Nonnull
	public GlobalEntityIndex getGlobalEntityIndexForType(@Nonnull Serializable entityType) {
		final EntityCollection entityCollection = ofNullable(catalog.getCollectionForEntity(entityType))
			.orElseThrow(() -> new CollectionNotFoundException(entityType));
		return entityCollection.getGlobalIndex();
	}

}
