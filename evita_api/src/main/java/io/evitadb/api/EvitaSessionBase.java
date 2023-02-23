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
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.structure.EntityDecorator;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.exception.CollectionNotFoundException;
import io.evitadb.api.exception.InstanceTerminatedException;
import io.evitadb.api.exception.InvalidSchemaMutationException;
import io.evitadb.api.exception.TransactionException;
import io.evitadb.api.exception.TransactionNotSupportedException;
import io.evitadb.api.exception.UnexpectedResultException;
import io.evitadb.api.exception.UnexpectedTransactionStateException;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.UUIDUtil;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.evitadb.api.query.QueryConstraints.entities;
import static io.evitadb.api.query.QueryConstraints.require;
import static java.util.Optional.ofNullable;

/**
 * Creates {@link EvitaSessionBase} for querying or updating the database. All query results in the same session
 * are cached and entities returned in different queries are the same (in the sense of identity entityA == entityB).
 * <p>
 * EvitaSession transactions behave like <a href="https://en.wikipedia.org/wiki/Isolation_(database_systems)#Serializable">Serializable</a>
 * transactions. When no transaction is explicitly opened - each query to Evita behaves as one small transaction. Data
 * updates are not allowed without explicitly opened transaction.
 * <p>
 * Don't forget to {@link #close()} when your work with Evita is finished.
 * EvitaSession is not is not thread safe!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
@EqualsAndHashCode(of = "id")
public abstract class EvitaSessionBase<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase> implements Comparable<EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>, AutoCloseable {
	/**
	 * Signalizes that this session is opened only for read operations.
	 */
	protected final boolean readOnly;
	/**
	 * Contains reference to the callback that needs to be called one catalog contents are changed (i.e. transaction
	 * is committed).
	 */
	protected final Consumer<CATALOG> updatedCatalogCallback;
	private final UUID id = UUIDUtil.randomUUID();
	/**
	 * Shared thread local that allows to access transaction object.
	 */
	private final ThreadLocal<TRANSACTION> transactionAccessor = new ThreadLocal<>();
	/**
	 * List of all calbacks that will be called when session is closed.
	 */
	private final List<EvitaSessionTerminationCallback> terminationCallbacks = new LinkedList<>();
	/**
	 * Contains reference to the catalog to query / update.
	 */
	protected CATALOG catalog;
	/**
	 * Flag that is se to TRUE when Evita. is ready to serve application calls.
	 * Aim of this flag is to refuse any calls after {@link #close()} method has been called.
	 */
	private boolean active = true;

	protected EvitaSessionBase(@Nonnull CATALOG catalog) {
		this.catalog = catalog;
		this.readOnly = true;
		this.updatedCatalogCallback = null;
	}

	protected EvitaSessionBase(@Nonnull CATALOG catalog, @Nonnull Consumer<CATALOG> updatedCatalogCallback) {
		this.catalog = catalog;
		this.readOnly = false;
		this.updatedCatalogCallback = updatedCatalogCallback;
	}

	/**
	 * Returns unique id of the session.
	 *
	 * @return unique ID generated when session is created.
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Returns TRUE if session is active (and can be used).
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Terminates EVE session and releases all used resources. This method renders the session unusable and any further
	 * calls to this session should end up with {@link io.evitadb.api.exception.InstanceTerminatedException}
	 * <p>
	 * This method is idempotent and may be called multiple times. Only first call is really processed and others are
	 * ignored.
	 */
	@Override
	public void close() {
		if (active) {
			// set itself inactive to avoid future calls
			active = false;
			// first close transaction if present
			ofNullable(transactionAccessor.get())
				.ifPresent(TransactionBase::close);
			// then apply termination callbacks
			for (EvitaSessionTerminationCallback terminationCallback : terminationCallbacks) {
				terminationCallback.onTermination(this);
			}
		}
	}

	/**
	 * Switches catalog to the {@link CatalogState#ALIVE} state an terminates the EVE session so that next session is
	 * operating in the new catalog state.
	 * <p>
	 * Session is {@link #close() closed} only when the state transition successfully occurs and this is signalized
	 * by return value.
	 *
	 * @return TRUE if catalog was successfully switched to {@link CatalogState#ALIVE} state
	 * @see CatalogState
	 */
	public boolean goLiveAndClose() {
		Assert.isTrue(!catalog.supportsTransaction(), "Catalog went live already and is currently in transactional mode!");
		if (catalog.goLive()) {
			close();
			return true;
		}
		return false;
	}

	/**
	 * Registers a callback that will be called when this session is being closed. Callback must not throw any exception.
	 */
	public void addTerminationCallback(@Nonnull EvitaSessionTerminationCallback terminationCallback) {
		this.terminationCallbacks.add(terminationCallback);
	}

	/**
	 * Method executes query on {@link CatalogBase} data and returns result. Because result is generic and may contain
	 * different data as its contents (based on input query), additional parameter `expectedType` is passed. This parameter
	 * allows to check whether passed response contains the expected type of data before returning it back to the client.
	 * This should prevent late ClassCastExceptions on the client side.
	 *
	 * @param query        input query
	 * @param expectedType type of object, that is expected to be in response data
	 * @throws UnexpectedResultException   when {@link EvitaResponseBase#getRecordPage()} contains data that are not assignable to `expectedType`
	 * @throws InstanceTerminatedException when session has been already terminated
	 */
	public <S extends Serializable, T extends EvitaResponseBase<S>> T query(@Nonnull Query query, @Nonnull Class<S> expectedType) throws UnexpectedResultException, InstanceTerminatedException {
		assertActive();
		final REQUEST request = createEvitaRequest(
			query.normalizeQuery(
				getFilterConstraintTranslator(),
				getOrderConstraintTranslator(),
				getRequireConstraintTranslator()
			),
			ZonedDateTime.now()
		);
		final Serializable entityType = request.getEntityType();
		final COLLECTION entityCollection = ofNullable(catalog.getCollectionForEntity(entityType))
			.orElseThrow(() -> new CollectionNotFoundException(entityType));
		final T response = getEntities(request, entityCollection);
		if (!response.getRecordData().isEmpty() && !expectedType.isInstance(response.getRecordData().get(0))) {
			throw new UnexpectedResultException(expectedType, response.getRecordData().get(0).getClass());
		}
		return response;
	}

	/**
	 * Method returns entity by its type and primary key in requested form of completeness. This method allows quick
	 * access to the entity contents when primary key is known.
	 */
	@Nullable
	public SealedEntity getEntity(@Nonnull Serializable entityType, int primaryKey, EntityContentRequire... require) {
		assertActive();
		final EntityCollectionBase<REQUEST> entityCollection = ofNullable(catalog.getCollectionForEntity(entityType))
			.orElseThrow(() -> new CollectionNotFoundException(entityType));
		return entityCollection.getEntity(
			primaryKey,
			createEvitaRequest(
				Query.query(
					entities(entityType),
					require(require)
				),
				ZonedDateTime.now()
			)
		);
	}

	/**
	 * Method returns entity with additionally loaded data specified by requirements in second argument. This method
	 * is particularly useful for implementation of lazy loading when application loads only parts of the entity it
	 * expects to be required for handling common client request and then load additional data if processing requires
	 * more in-depth view of the entity.
	 */
	public SealedEntity enrichEntity(@Nonnull SealedEntity partiallyLoadedEntity, EntityContentRequire... require) {
		assertActive();
		final Serializable entityType = partiallyLoadedEntity.getType();
		final EntityCollectionBase<REQUEST> entityCollection = ofNullable(catalog.getCollectionForEntity(entityType))
			.orElseThrow(() -> new CollectionNotFoundException(entityType));
		Assert.isTrue(partiallyLoadedEntity instanceof EntityDecorator, "Expected entity decorator in the input.");
		final EntityDecorator entityDecorator = (EntityDecorator) partiallyLoadedEntity;
		return entityCollection.enrichEntity(
			entityDecorator,
			createEvitaRequest(
				Query.query(
					entities(entityType),
					require(require)
				),
				ZonedDateTime.now()
			)
		);
	}

	/**
	 * Returns builder that can be used to define schema for the entity.
	 * Collection for `entityType` is automatically (and immediately by this method) created if it hasn't existed yet.
	 */
	public EntitySchemaBuilder defineSchema(@Nonnull Serializable entityType) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> {
			final EntityCollectionBase<REQUEST> collection = catalog.getOrCreateCollectionForEntity(entityType);
			return collection.defineSchema();
		});
	}

	/**
	 * Creates new collection that conforms to the passed schema. Schema for passed entity type must not exist. This
	 * method can be used only for bootstrapping new entity collection.
	 *
	 * @throws InvalidSchemaMutationException when schema with passed name already exists
	 */
	public void defineSchema(@Nonnull EntitySchema entitySchema) throws InvalidSchemaMutationException {
		assertActive();
		executeInTransactionIfPossible(SESSION -> {
			catalog.createCollectionForEntity(entitySchema);
			return null;
		});
	}

	/**
	 * Returns schema definition for entity of specified type.
	 */
	@Nullable
	public EntitySchema getEntitySchema(@Nonnull Serializable entityType) {
		assertActive();
		final EntityCollectionBase<REQUEST> collection = catalog.getCollectionForEntity(entityType);
		return ofNullable(collection).map(EntityCollectionBase::getSchema).orElse(null);
	}

	/**
	 * Returns list of all entity types available in this catalog.
	 */
	@Nonnull
	public Set<Serializable> getAllEntityTypes() {
		return catalog.getEntityTypes();
	}

	/**
	 * Deletes entire collection of entities along with its schema. After this operation there will be nothing left
	 * of the data that belong to the specified entity type.
	 *
	 * @param entityType type of the entity which collection should be deleted
	 * @return TRUE if collection was successfully deleted
	 */
	public boolean deleteCollection(@Nonnull Serializable entityType) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> catalog.deleteCollectionOfEntity(entityType));
	}

	/**
	 * Method returns count of all entities stored in the collection of passed entity type.
	 *
	 * @throws IllegalArgumentException when entity collection doesn't exist
	 */
	public int getEntityCollectionSize(@Nonnull Serializable entityType) {
		assertActive();
		return ofNullable(catalog.getCollectionForEntity(entityType))
			.map(EntityCollectionBase::size)
			.orElseThrow(() -> new IllegalArgumentException("Entity collection of type " + entityType + " doesn't exist!"));
	}

	/**
	 * Creates entity builder for new entity that needs to be inserted to the collection.
	 *
	 * @param primaryKey may be null if {@link EntitySchema#isWithGeneratedPrimaryKey()} is set to true
	 * @return builder instance to be filled up and stored via {@link #upsertEntity(EntityBuilder)}
	 */
	public EntityBuilder createNewEntity(@Nonnull Serializable entityType, @Nullable Integer primaryKey) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> {
			final EntityCollectionBase<REQUEST> collection = catalog.getOrCreateCollectionForEntity(entityType);
			return collection.createNewEntity(primaryKey);
		});
	}

	/**
	 * Short-hand method for {@link #upsertEntity(EntityMutation)} that accepts {@link EntityBuilder} that can produce
	 * mutation.
	 *
	 * @param entityBuilder that contains changed entity state
	 */
	public EntityReference upsertEntity(@Nonnull EntityBuilder entityBuilder) {
		return upsertEntity(entityBuilder.toMutation());
	}

	/**
	 * Method inserts to or updates entity in collection according to passed set of mutations.
	 *
	 * @param entityMutation list of mutation snippets that alter or form the entity
	 */
	public EntityReference upsertEntity(@Nonnull EntityMutation entityMutation) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> {
			final COLLECTION collection = catalog.getOrCreateCollectionForEntity(entityMutation.getEntityType());
			return collection.upsertEntity(entityMutation);
		});
	}

	/**
	 * Method removes existing entity in collection by its primary key. All entities of other entity types that reference
	 * removed entity in their {@link SealedEntity#getReference(Serializable, int)} still keep the data untouched.
	 *
	 * @return true if entity existed and was removed
	 */
	public boolean deleteEntity(@Nonnull Serializable entityType, @Nonnull Integer primaryKey) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> {
			final EntityCollectionBase<REQUEST> collection = catalog.getOrCreateCollectionForEntity(entityType);
			return collection.deleteEntity(primaryKey);
		});
	}

	/**
	 * Method removes existing hierarchical entity in collection by its primary key. Method also removes all entities
	 * of the same type that are transitively referencing the removed entity as its parent. All entities of other entity
	 * types that reference removed entities in their {@link SealedEntity#getReference(Serializable, int)} still keep
	 * the data untouched.
	 *
	 * @return number of removed entities
	 * @throws IllegalArgumentException when entity type has not hierarchy support enabled in schema
	 */
	public int deleteEntityAndItsHierarchy(@Nonnull Serializable entityType, @Nonnull Integer primaryKey) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> {
			final EntityCollectionBase<REQUEST> collection = catalog.getOrCreateCollectionForEntity(entityType);
			Assert.isTrue(
				collection.getSchema().isWithHierarchy(),
				"Entity type " + entityType + " doesn't represent a hierarchical entity!"
			);
			return collection.deleteEntityAndItsHierarchy(primaryKey);
		});
	}

	/**
	 * Method removes all entities that match passed query. All entities of other entity types that reference removed
	 * entities in their {@link SealedEntity#getReference(Serializable, int)} still keep the data untouched.
	 *
	 * @return number of deleted entities
	 */
	public int deleteEntities(@Nonnull REQUEST request) {
		assertActive();
		return executeInTransactionIfPossible(SESSION -> {
			final EntityCollectionBase<REQUEST> collection = catalog.getOrCreateCollectionForEntity(request.getEntityType());
			return collection.deleteEntities(request);
		});
	}

	/**
	 * Clears all {@link EvitaSessionBase} caches and forces loading all entities again even if they were already loaded
	 * in this session.
	 *
	 * TOBEDONE JNO - this will be removed
	 */
	public abstract void clearCache();

	/**
	 * Default implementation uses ID for comparing two sessions (and to distinguish one session from another).
	 *
	 * @return 0 if both sessions are the same
	 */
	@Override
	public int compareTo(@Nonnull EvitaSessionBase otherSession) {
		return getId().compareTo(otherSession.getId());
	}

	/**
	 * Opens a new transaction.
	 */
	public void openTransaction() {
		assertTransactionIsNotOpened();
		createAndInitTransaction();
	}

	/**
	 * Terminates opened transaction - either by rollback or commit depending on {@link TransactionBase#isRollbackOnly()}.
	 * This method throws exception only when transaction hasn't been opened.
	 */
	public void closeTransaction() {
		if (transactionAccessor.get() == null) {
			throw new UnexpectedTransactionStateException("No transaction has been opened!");
		}
		final TRANSACTION transaction = transactionAccessor.get();
		destroyTransaction();
		transaction.close();
	}

	/**
	 * Method allows to normalize {@link io.evitadb.api.query.filter.FilterBy} part of the query and exchange certain
	 * constraints with another one. It may also perform special validation logic or whatever else.
	 */
	@Nullable
	protected UnaryOperator<FilterConstraint> getFilterConstraintTranslator() {
		return null;
	}

	/**
	 * Method allows normalizing {@link io.evitadb.api.query.order.OrderBy} part of the query and exchange certain
	 * constraints with another one. It may also perform special validation logic or whatever else.
	 */
	@Nullable
	protected UnaryOperator<OrderConstraint> getOrderConstraintTranslator() {
		return null;
	}

	/**
	 * Method queries and returns entities in passed `entityCollection` using defined `request`. The response object
	 * is derived from the query {@link Query#getRequire()} constraints and might represent simple primary key
	 * identification or fully loaded entities with all data. The result is always "paginated" - contains only
	 * the requested slice of data. Result might also contain additional rich objects
	 * in {@link EvitaResponseBase#getAdditionalResults(Class)} should they are stated in requirements.
	 */
	@Nonnull
	protected <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull REQUEST request, @Nonnull COLLECTION entityCollection) {
		return entityCollection.getEntities(request);
	}

	/**
	 * Creates implementation of {@link EvitaRequestBase} that helps to parse input query.
	 */
	protected abstract REQUEST createEvitaRequest(@Nonnull Query query, @Nonnull ZonedDateTime alignedNow);

	/**
	 * Creates new transaction a wraps it into carrier object.
	 */
	protected abstract TRANSACTION createTransaction();

	/**
	 * If {@link CatalogBase} supports transactions (see {@link CatalogBase#supportsTransaction()}) method executes application
	 * `logic` in current session and commits the transaction at the end. Transaction is automatically roll-backed when
	 * exception is thrown from the `logic` scope. Changes made by the updating logic are visible only within update
	 * function. Other threads outside the logic function work with non-changed data until transaction is committed
	 * to the index.
	 * <p>
	 * When catalog doesn't support transactions application `logic` is immediatelly applied to the index data and logic
	 * operates in a <a href="https://en.wikipedia.org/wiki/Isolation_(database_systems)#Read_uncommitted">read uncommited</a>
	 * mode. Application `logic` can only append new entities in non-transactional mode.
	 */
	<T, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> T execute(Function<SESSION, T> logic) {
		if (catalog.supportsTransaction()) {
			assertTransactionIsNotOpened();
			try (final TRANSACTION tx = createAndInitTransaction()) {
				return applyRollbackable(logic);
			} catch (Exception e) {
				log.error("Failed to finish transaction properly.", e);
				throw new TransactionException("Failed to finish transaction properly: " + e.getMessage(), e);
			} finally {
				destroyTransaction();
			}
		} else {
			//currently, impossible to model in Java
			//noinspection unchecked
			return logic.apply((SESSION) this);
		}
	}

	/**
	 * Method allows to normalize {@link io.evitadb.api.query.require.Require} part of the query and exchange certain
	 * constraints with another one. It may also perform special validation logic or whatever else.
	 */
	@Nullable
	private UnaryOperator<RequireConstraint> getRequireConstraintTranslator() {
		return null;
	}

	/**
	 * Verifies this instance is still active.
	 */
	protected void assertActive() {
		if (!active) {
			throw new InstanceTerminatedException("Evita instance has been already terminated! No calls are accepted since all resources has been released.");
		}
	}

	/**
	 * Verifies this instance is still active.
	 */
	private void assertTransactionIsNotOpened() {
		if (transactionAccessor.get() != null) {
			throw new UnexpectedTransactionStateException("Transaction has been already opened. Evita doesn't support nested transactions!");
		}
	}

	/**
	 * Initializes transaction reference.
	 */
	private TRANSACTION createAndInitTransaction() {
		if (readOnly) {
			throw new TransactionNotSupportedException("Transaction cannot be opened in read only session!");
		}
		if (!catalog.supportsTransaction()) {
			throw new TransactionNotSupportedException("Catalog " + catalog.getName() + " doesn't support transactions yet. Call `goLiveAndClose()` method first!");
		}
		final TRANSACTION tx = createTransaction();
		transactionAccessor.set(tx);
		return tx;
	}

	/**
	 * Destroys transaction reference.
	 */
	private void destroyTransaction() {
		transactionAccessor.remove();
	}

	/**
	 * Executes passed lambda in existing transaction or throws exception.
	 *
	 * @throws UnexpectedTransactionStateException if transaction is not open
	 */
	private <T, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> T executeInTransactionIfPossible(Function<SESSION, T> logic) {
		if (transactionAccessor.get() == null && catalog.supportsTransaction()) {
			throw new UnexpectedTransactionStateException("No transaction has been opened. Cannot alter catalog data!");
		}
		try {
			//currently, impossible to model in Java
			//noinspection unchecked
			return logic.apply((SESSION) this);
		} catch (Throwable ex) {
			ofNullable(transactionAccessor.get())
				.ifPresent(TransactionBase::setRollbackOnly);
			throw ex;
		}
	}

	/**
	 * Executes lambda and sets rollback only flag when anything bad happens.
	 */
	private <T, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>> T applyRollbackable(Function<SESSION, T> logic) {
		try {
			//currently, impossible to model in Java
			//noinspection unchecked
			return logic.apply((SESSION) this);
		} catch (Throwable ex) {
			ofNullable(transactionAccessor.get())
				.ifPresent(TransactionBase::setRollbackOnly);
			throw ex;
		}
	}

}
