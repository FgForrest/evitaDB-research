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
import io.evitadb.api.exception.CollectionNotFoundException;
import io.evitadb.api.exception.InvalidSchemaMutationException;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.ofNullable;

/**
 * Catalog is a fragment of Evita database that can be compared to a schema of relational database. Catalog allows
 * handling multiple isolated data collections inside single Evita instance. Catalogs in Evita are isolated one from
 * another and share no single thing. They have separate {@link CatalogConfiguration}, separate data and cannot share
 * {@link EvitaSessionBase}. It means that EvitaSession is bound to its catalog since creation and cannot query
 * different catalog than this one.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class CatalogBase<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>> {
	/**
	 * Contains catalog configuration.
	 */
	private final CONFIGURATION configuration;

	/**
	 * Indicates state in which Catalog operates.
	 *
	 * @see CatalogState
	 */
	private final AtomicReference<CatalogState> state;

	protected CatalogBase(@Nonnull CONFIGURATION configuration) {
		this.configuration = configuration;
		this.state = new AtomicReference<>(CatalogState.WARMING_UP);
	}

	protected CatalogBase(@Nonnull CONFIGURATION configuration, @Nonnull CatalogState state) {
		this.configuration = configuration;
		this.state = new AtomicReference<>(state);
	}

	/**
	 * Returns read-only catalog configuration.
	 */
	@Nonnull
	public CONFIGURATION getConfiguration() {
		return configuration;
	}

	/**
	 * Returns state of this catalog instance.
	 */
	@Nonnull
	public CatalogState getCatalogState() {
		return this.state.get();
	}

	/**
	 * Descendants can use this method to set current catalog state. Method should be called in constructor.
	 */
	protected void setCatalogState(@Nonnull CatalogState catalogState) {
		this.state.set(catalogState);
	}

	/**
	 * Returns name of the CatalogBase instance. Name must be unique across all catalogs inside same {@link EvitaBase}
	 * instance. Name of the catalog must be equal to {@link CatalogConfiguration#getName()}
	 */
	@Nonnull
	public String getName() {
		return configuration.getName();
	}

	/**
	 * Returns true if catalog supports transactions.
	 */
	public boolean supportsTransaction() {
		return state.get() == CatalogState.ALIVE;
	}

	/**
	 * Returns set of all maintained {@link EntityCollectionBase} - i.e. entity types.
	 */
	@Nonnull
	public abstract Set<Serializable> getEntityTypes();

	/**
	 * Returns collection maintaining all entities of same type.
	 *
	 * @param entityType type (name) of the entity
	 */
	@Nullable
	public abstract COLLECTION getCollectionForEntity(@Nonnull Serializable entityType);

	/**
	 * Returns collection maintaining all entities of same type or throws standardized exception.
	 *
	 * @param entityType type (name) of the entity
	 */
	@Nonnull
	public COLLECTION getCollectionForEntityOrThrowException(@Nonnull Serializable entityType) throws CollectionNotFoundException {
		return ofNullable(getCollectionForEntity(entityType))
			.orElseThrow(() -> new CollectionNotFoundException(entityType));
	}

	/**
	 * Returns collection maintaining all entities of same type. If no such collection exists new one is created.
	 *
	 * @param entityType type (name) of the entity
	 */
	@Nonnull
	public abstract COLLECTION getOrCreateCollectionForEntity(@Nonnull Serializable entityType);

	/**
	 * Creates new collection that conforms to the passed schema. Schema for passed entity type must not exist. This
	 * method can be used only for bootstrapping new entity collection.
	 *
	 * @throws InvalidSchemaMutationException when schema with passed name already exists
	 */
	@Nonnull
	public abstract COLLECTION createCollectionForEntity(@Nonnull EntitySchema entitySchema) throws InvalidSchemaMutationException;

	/**
	 * Deletes entire collection of entities along with its schema. After this operation there will be nothing left
	 * of the data that belong to the specified entity type.
	 *
	 * @param entityType type of the entity which collection should be deleted
	 * @return TRUE if collection was successfully deleted
	 */
	public abstract boolean deleteCollectionOfEntity(@Nonnull Serializable entityType);

	/**
	 * Changes state of the catalog from {@link CatalogState#WARMING_UP} to {@link CatalogState#ALIVE}.
	 *
	 * @see CatalogState
	 */
	boolean goLive() {
		Assert.isTrue(state.get() == CatalogState.WARMING_UP, "Catalog has already alive state!");
		flush();
		return state.compareAndSet(CatalogState.WARMING_UP, CatalogState.ALIVE);
	}

	/**
	 * Method allows to immediately flush all information held in memory to the persistent storage.
	 * This method might do nothing particular in transaction ({@link CatalogState#ALIVE}) mode.
	 */
	abstract void flush();

	/**
	 * Terminates catalog instance and frees all claimed resources. Prepares catalog instance to be garbage collected.
	 *
	 * This method is idempotent and may be called multiple times. Only first call is really processed and others are
	 * ignored.
	 */
	abstract void terminate();

}
