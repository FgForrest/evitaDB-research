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

package io.evitadb.api.data.mutation.reference;

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * This mutation allows to create {@link Reference} in the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InsertReferenceMutation extends ReferenceMutation<EntityReference> implements SchemaEvolvingLocalMutation<ReferenceContract, EntityReference> {
	private static final long serialVersionUID = 6295749367283283232L;
	@Getter private final ReferenceContract createdReference;

	public InsertReferenceMutation(ReferenceContract createdReference, EntitySchema entitySchema) {
		super(entitySchema, createdReference.getReferencedEntity());
		this.createdReference = createdReference;
	}

	@Override
	public @Nonnull
	Class<ReferenceContract> affects() {
		return ReferenceContract.class;
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return new ReferenceSkipToken(referenceKey.getType());
	}

	@Override
	public @Nonnull
	EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		if (schema.getReference(referenceKey.getType()) == null) {
			Assert.isTrue(
				schema.allows(EvolutionMode.ADDING_REFERENCES),
				() -> new InvalidMutationException(
					"Entity " + schema.getName() + " doesn't support references of type " + referenceKey.getType() +
						", you need to change the schema definition for it first."
				)
			);
			return schema.open(schemaUpdater)
				.withReferenceTo(referenceKey.getType())
				.applyChanges();
		} else {
			return schema;
		}
	}

	@Nonnull
	@Override
	public ReferenceContract mutateLocal(@Nullable ReferenceContract existingReference) {
		if (existingReference == null) {
			return new Reference(
				entitySchema,
				createdReference.getReferencedEntity(),
				createdReference.getGroup(),
				new Attributes(entitySchema)
			);
		} else if (existingReference.isDropped()) {
			return new Reference(
				entitySchema,
				existingReference.getVersion() + 1,
				createdReference.getReferencedEntity(),
				createdReference.getGroup(),
				// attributes are inserted in separate mutation
				Collections.emptyList(),
				Collections.emptySet(),
				false
			);
		} else {
			/* SHOULD NOT EVER HAPPEN */
			throw new InvalidMutationException(
				"This mutation cannot be used for updating reference."
			);
		}
	}

	@Override
	public long getPriority() {
		return PRIORITY_UPSERT;
	}

	@Override
	public EntityReference getComparableKey() {
		return referenceKey;
	}

	/**
	 * This method is used to unify creation process for creating the reference itself and also all its attributes
	 * for which there is special case of the mutation (necessary to handle schema validation / evolution).
	 */
	public Stream<ReferenceMutation<?>> generateMutations() {
		return Stream.concat(
			Stream.of(this),
			createdReference.getAttributeValues()
				.stream()
				.map(x ->
					new ReferenceAttributesUpdateMutation(
						createdReference.getReferencedEntity(),
						entitySchema,
						new UpsertAttributeMutation(x.getKey(), x.getValue())
					)
				)
		);
	}
}
