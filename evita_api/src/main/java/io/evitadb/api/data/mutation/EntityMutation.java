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

package io.evitadb.api.data.mutation;

import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Mutations implementing this interface are top-level mutations that group all {@link LocalMutation} that target same
 * entity. This mutation atomically updates single {@link Entity} in Evita DB.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
public interface EntityMutation extends SchemaEvolvingMutation {

	/**
	 * Returns {@link Entity#getType()} of the entity this mutation targets.
	 */
	@Nonnull
	Serializable getEntityType();

	/**
	 * Returns {@link Entity#getPrimaryKey()} of the entity this mutation targets.
	 *
	 * @return may return NULL only in case mutation can setup brand new entity
	 */
	@Nullable
	Integer getEntityPrimaryKey();

	/**
	 * Allows to set primary key generated by Evita DB into the created entity.
	 */
	void setEntityPrimaryKey(@Nonnull Integer primaryKey);

	/**
	 * Returns true if mutation expects there is already existing entity.
	 */
	@Nonnull
	EntityExistence expects();

	/**
	 * Returns entity with applied mutation.
	 */
	@Nonnull
	Entity mutate(@Nullable Entity entity);

	/**
	 * Returns collection of all local mutations that modify this entity.
	 */
	@Nonnull
	Collection<? extends LocalMutation<?, ?>> getLocalMutations();

	/**
	 * Default implementation of the entity builder retrieves all local mutations, filters those that implement
	 * {@link SchemaEvolvingLocalMutation} and calls back their implementation of {@link SchemaEvolvingLocalMutation#verifyOrEvolveSchema(EntitySchema, UnaryOperator)}.
	 *
	 * Because there may be multiple mutations that target the same schema settings, concept of skipToken is introduced
	 * that allows to perform only first verification / evolution of the entity schema and skip others very quickly.
	 *
	 * @param schema to check entity against
	 */
	@Nonnull
	@Override
	default EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		final Set<Serializable> skipList = new HashSet<>();
		EntitySchema resultSchema = schema;
		for (LocalMutation<?, ?> localMutation : getLocalMutations()) {
			if (localMutation instanceof SchemaEvolvingLocalMutation) {
				final SchemaEvolvingLocalMutation<?, ?> evolvingMutation = (SchemaEvolvingLocalMutation<?, ?>) localMutation;
				final Serializable skipToken = evolvingMutation.getSkipToken();
				// grouping token allows us to skip duplicate schema verifications / evolutions
				// there may be several mutations that targets same entity "field"
				// so there is no sense to verify them repeatedly
				if (!skipList.contains(skipToken)) {
					resultSchema = evolvingMutation.verifyOrEvolveSchema(resultSchema, schemaUpdater);
					skipList.add(skipToken);
				}
			}
		}
		return resultSchema;
	}

	/**
	 * Contains set of all possible expected states for the entity.
	 */
	enum EntityExistence {
		MUST_NOT_EXIST,
		MAY_EXIST,
		MUST_EXIST
	}
}