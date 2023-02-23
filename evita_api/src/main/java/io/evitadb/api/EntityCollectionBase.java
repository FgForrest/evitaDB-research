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

import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.exception.ConcurrentSchemaUpdateException;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.exception.SchemaAlteringException;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Entity collection maintains all entities of same {@link Entity#getType()}. Entity collection could be imagined as single
 * table in RDBMS environment or document type in case of Elastic Search or Mongo DB no sql databases.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class EntityCollectionBase<REQUEST extends EvitaRequestBase> {
	/**
	 * Contains schema of the entity type that is used for formal verification of the data consistency and indexing
	 * prescription.
	 */
	protected final AtomicReference<EntitySchema> schema;

	/**
	 * Creates new collection when schema is not yet known (needs to be loaded from underlying storage).
	 */
	protected EntityCollectionBase() {
		this.schema = new AtomicReference<>();
	}

	/**
	 * Creates new collection with schema setup.
	 */
	protected EntityCollectionBase(EntitySchema entitySchema) {
		this.schema = new AtomicReference<>(entitySchema);
	}

	/**
	 * Returns paginated list of entities that conform to input query. Additional results might be computed as well if
	 * there are controlling {@link RequireConstraint} present in the query.
	 */
	@Nonnull
	public abstract <S extends Serializable, T extends EvitaResponseBase<S>> T getEntities(@Nonnull REQUEST request);

	/**
	 * Method returns entity by its type and primary key in requested form of completeness. This method allows quick
	 * access to the entity contents when primary key is known.
	 */
	@Nullable
	public abstract SealedEntity getEntity(int primaryKey, @Nonnull REQUEST evitaRequest);

	/**
	 * Method returns entity with additionally loaded data specified by requirements in second argument. This method
	 * is particularly useful for implementation of lazy loading when application loads only parts of the entity it
	 * expects to be required for handling common client request and then load additional data if processing requires
	 * more in-depth view of the entity.
	 *
	 * @param evitaRequest - request has no filter / order - only envelopes additional requirements for the loaded entity,
	 *                     so that utility methods in request can be reused
	 */
	public abstract SealedEntity enrichEntity(@Nonnull SealedEntity partiallyLoadedEntity, @Nonnull REQUEST evitaRequest);

	/**
	 * Returns UNIQUE name of the entity collection in the catalog.
	 */
	@Nonnull
	public Serializable getName() {
		return schema.get().getName();
	}

	/**
	 * Creates entity builder for new entity that needs to be inserted to the collection.
	 *
	 * @param primaryKey may be null if {@link EntitySchema#isWithGeneratedPrimaryKey()} is set to true
	 * @return builder instance to be filled up and stored via {@link #upsertEntity(EntityMutation)}
	 */
	@Nonnull
	public EntityBuilder createNewEntity(@Nullable Integer primaryKey) {
		return new InitialEntityBuilder(getSchema(), primaryKey);
	}

	/**
	 * Method inserts to or updates entity in collection according to passed set of mutations.
	 *
	 * @param entityMutation list of mutation snippets that alter or form the entity
	 * @throws InvalidMutationException when mutation cannot be executed - it is throw when there is attempt to insert
	 *                                  twice entity with the same primary key, or execute update that has no sense
	 */
	@Nonnull
	public abstract EntityReference upsertEntity(@Nonnull EntityMutation entityMutation) throws InvalidMutationException;

	/**
	 * Method removes existing entity in collection by its primary key. All entities of other entity types that reference
	 * removed entity in their {@link SealedEntity#getReference(Serializable, int)} still keep the data untouched.
	 *
	 * @return true if entity existed and was removed
	 */
	public abstract boolean deleteEntity(int primaryKey);

	/**
	 * Method removes existing hierarchical entity in collection by its primary key. Method also removes all entities
	 * of the same type that are transitively referencing the removed entity as its parent. All entities of other entity
	 * types that reference removed entities in their {@link SealedEntity#getReference(Serializable, int)} still keep
	 * the data untouched.
	 *
	 * @return number of removed entities
	 * @throws IllegalArgumentException when entity type has not hierarchy support enabled in schema
	 */
	public abstract int deleteEntityAndItsHierarchy(int primaryKey);

	/**
	 * Method removes all entities that match passed query. All entities of other entity types that reference removed
	 * entities in their {@link SealedEntity#getReference(Serializable, int)} still keep the data untouched.
	 *
	 * @return number of deleted entities
	 */
	public abstract int deleteEntities(@Nonnull REQUEST request);

	/**
	 * Method returns true if there is no single entity in the collection.
	 */
	public abstract boolean isEmpty();

	/**
	 * Returns count of all elements in the storage.
	 */
	public abstract int size();

	/**
	 * Returns read-only schema of the entity type that is used for formal verification of the data consistency and indexing
	 * prescription. If you need to alter the schema use {@link #defineSchema()} method.
	 */
	@Nonnull
	public EntitySchema getSchema() {
		return schema.get();
	}

	/**
	 * Method allows to update current schema of the entity type. Use this method if you want to define attribute, reference
	 * or associated data types upfront. If you want to change attribute indexes or entity schema evolution rules.
	 */
	@Nonnull
	public EntitySchemaBuilder defineSchema() {
		return new EntitySchemaBuilder(getSchema(), this::updateSchema);
	}

	/**
	 * Internal method for storing updated schema and verifying optimistic locking.
	 *
	 * @throws SchemaAlteringException when changes were not applied because of an error
	 */
	@Nonnull
	EntitySchema updateSchema(@Nonnull EntitySchema newSchema) throws SchemaAlteringException {
		final EntitySchema currentSchema = getSchema();
		if (newSchema == currentSchema) {
			// there is nothing to update
			return currentSchema;
		}
		Assert.isTrue(
			newSchema.getVersion() - 1 == currentSchema.getVersion(),
			() -> new ConcurrentSchemaUpdateException(
				"Cannot update schema - someone else altered the schema in the meanwhile (current version is " +
					currentSchema.getVersion() + ", yours is " + newSchema.getVersion() + ")."
			)
		);
		final EntitySchema originalSchemaBeforeExchange = this.schema.compareAndExchange(currentSchema, newSchema);
		Assert.isTrue(
			originalSchemaBeforeExchange.getVersion() == currentSchema.getVersion(),
			() -> new ConcurrentSchemaUpdateException(
				"Cannot update schema - someone else altered the schema in the meanwhile (current version is " +
					currentSchema.getVersion() + ", yours is " + newSchema.getVersion() + ")."
			)
		);
		return newSchema;
	}

}
