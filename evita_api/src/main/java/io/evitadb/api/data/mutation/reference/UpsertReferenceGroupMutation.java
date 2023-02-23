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
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Optional.ofNullable;

/**
 * This mutation allows to create / update {@link GroupEntityReference} of the {@link Reference}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UpsertReferenceGroupMutation extends ReferenceMutation<EntityReference> implements SchemaEvolvingLocalMutation<ReferenceContract, EntityReference> {
	private static final long serialVersionUID = -8894714389485857588L;
	@Getter private final GroupEntityReference groupReference;

	public UpsertReferenceGroupMutation(EntityReference referenceKey, EntitySchema entitySchema, GroupEntityReference groupReference) {
		super(entitySchema, referenceKey);
		this.groupReference = groupReference;
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return new ReferenceGroupSkipToken(referenceKey.getType(), groupReference.getType());
	}

	@Nonnull
	@Override
	public EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		final ReferenceSchema referenceSchema = schema.getReference(referenceKey.getType());
		Assert.notNull(referenceSchema, "Reference to type `" + referenceKey.getType() + "` was not found!");
		final Serializable existingGroupType = referenceSchema.getGroupType();

		if (existingGroupType == null) {
			Assert.isTrue(
				schema.allows(EvolutionMode.ADDING_REFERENCES),
				() -> new InvalidMutationException(
					"Entity " + schema.getName() + " doesn't support groups for references of type " + referenceKey.getType() +
						", you need to change the schema definition for it first."
				)
			);
			if (referenceSchema.isEntityTypeRelatesToEntity()) {
				return schema.open(schemaUpdater)
					.withReferenceToEntity(
						referenceKey.getType(),
						whichIs -> whichIs.withGroupType(groupReference.getType())
					)
					.applyChanges();
			} else {
				return schema.open(schemaUpdater)
					.withReferenceTo(
						referenceKey.getType(),
						whichIs -> whichIs.withGroupType(groupReference.getType())
					)
					.applyChanges();
			}
		} else {
			Assert.isTrue(
				existingGroupType.equals(groupReference.getType()),
				() -> new InvalidMutationException(
					"Group is already related to entity " + existingGroupType +
						". It is not possible to change it to " + groupReference.getType() + "!"
				)
			);
			return schema;
		}
	}

	@Nonnull
	@Override
	public ReferenceContract mutateLocal(@Nullable ReferenceContract existingReference) {
		Assert.isTrue(
			existingReference != null && existingReference.exists(),
			() -> new InvalidMutationException("Cannot remove reference " + referenceKey + " - reference doesn't exist!")
		);

		final Optional<GroupEntityReference> existingReferenceGroup = ofNullable(existingReference.getGroup());
		return new Reference(
			entitySchema,
			existingReference.getVersion() + 1,
			referenceKey,
			existingReferenceGroup
				.map(it ->
					new GroupEntityReference(
						it.getVersion() + 1,
						groupReference.getType(),
						groupReference.getPrimaryKey(),
						false
					)
				)
				.orElseGet(() ->
					new GroupEntityReference(
						groupReference.getType(),
						groupReference.getPrimaryKey()
					)
				),
			existingReference.getAttributeValues(),
			existingReference.getAttributeLocales(),
			existingReference.isDropped()
		);
	}

	@Override
	public long getPriority() {
		return PRIORITY_UPSERT;
	}

	@Override
	public EntityReference getComparableKey() {
		return referenceKey;
	}

}
