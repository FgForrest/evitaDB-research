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

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.attribute.AttributeSchemaEvolvingMutation;
import io.evitadb.api.data.mutation.reference.ReferenceAttributesUpdateMutation.EntityReferenceWithAttributeKey;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.ExistingAttributesBuilder;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.schema.ReferenceSchemaBuilder;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * This mutation allows to create / update / remove {@link AttributeValue} of the {@link Reference}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceAttributesUpdateMutation extends ReferenceMutation<EntityReferenceWithAttributeKey> implements SchemaEvolvingLocalMutation<ReferenceContract, EntityReferenceWithAttributeKey> {
	private static final long serialVersionUID = -1403540167469945561L;
	@Getter private final AttributeMutation attributeMutation;
	@Getter private final AttributeKey attributeKey;
	private final EntityReferenceWithAttributeKey comparableKey;

	public ReferenceAttributesUpdateMutation(EntityReference referenceKey, EntitySchema entitySchema, AttributeMutation attributeMutation) {
		super(entitySchema, referenceKey);
		this.attributeMutation = attributeMutation;
		this.attributeKey = attributeMutation.getAttributeKey();
		this.comparableKey = new EntityReferenceWithAttributeKey(referenceKey, attributeKey);
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return new ReferenceAttributeSkipToken(referenceKey.getType(), attributeKey);
	}

	@Override
	public @Nonnull
	EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		if (attributeMutation instanceof AttributeSchemaEvolvingMutation) {
			final AttributeSchemaEvolvingMutation schemaValidatingMutation = (AttributeSchemaEvolvingMutation) attributeMutation;
			final ReferenceSchema referenceSchema = schema.getReference(referenceKey.getType());
			Assert.notNull(referenceSchema, "Reference to type `" + referenceKey.getType() + "` was not found!");
			return attributeMutation.verifyOrEvolveSchema(
				schema,
				referenceSchema.getAttribute(attributeKey.getAttributeName()),
				schemaValidatingMutation.getAttributeValue(),
				() -> {
					final EntitySchemaBuilder schemaBuilder = schema.open(schemaUpdater);
					if (attributeKey.isLocalized()) {
						schemaBuilder.withLocale(attributeKey.getLocale());
					}
					final Consumer<ReferenceSchemaBuilder> referenceSchemaUpdater = whichIs -> whichIs.withAttribute(
						attributeKey.getAttributeName(),
						schemaValidatingMutation.getAttributeValue().getClass(),
						thatIs -> thatIs.localized(attributeKey::isLocalized)
					);
					if (referenceSchema.isEntityTypeRelatesToEntity()) {
						return schemaBuilder
							.withReferenceToEntity(referenceKey.getType(), referenceSchemaUpdater)
							.applyChanges();
					} else {
						return schemaBuilder
							.withReferenceTo(referenceKey.getType(), referenceSchemaUpdater)
							.applyChanges();
					}
				}
			);
		} else {
			return schema;
		}
	}

	@Nonnull
	@Override
	public ReferenceContract mutateLocal(@Nullable ReferenceContract existingReference) {
		Assert.isTrue(
			existingReference != null && existingReference.exists(),
			() -> new InvalidMutationException("Cannot update attributes on reference " + referenceKey + " - reference doesn't exist!")
		);
		// this is kind of expensive, let's hope references will not have many attributes on them that frequently change
		return new Reference(
			entitySchema,
			existingReference.getVersion() + 1,
			existingReference.getReferencedEntity(),
			existingReference.getGroup(),
			new ExistingAttributesBuilder(
				entitySchema,
				existingReference.getAttributeValues(),
				existingReference.getAttributeLocales()
			)
				.mutateAttribute(attributeMutation)
				.build(),
			false
		);
	}

	@Override
	public long getPriority() {
		// we need that attribute removals are placed before insert/remove reference itself
		final long priority = attributeMutation.getPriority();
		if (priority >= PRIORITY_REMOVAL) {
			return priority + 1;
		} else {
			return priority - 1;
		}
	}

	@Override
	public EntityReferenceWithAttributeKey getComparableKey() {
		return comparableKey;
	}

	public static class EntityReferenceWithAttributeKey implements Comparable<EntityReferenceWithAttributeKey>, Serializable {
		private static final long serialVersionUID = 773755868610382953L;
		private final EntityReference entityReference;
		private final AttributeKey attributeKey;

		public EntityReferenceWithAttributeKey(@Nonnull EntityReference entityReference, @Nonnull AttributeKey attributeKey) {
			this.entityReference = entityReference;
			this.attributeKey = attributeKey;
		}

		@Override
		public int compareTo(EntityReferenceWithAttributeKey o) {
			final int entityReferenceComparison = entityReference.compareTo(o.entityReference);
			if (entityReferenceComparison == 0) {
				return attributeKey.compareTo(o.attributeKey);
			} else {
				return entityReferenceComparison;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(entityReference, attributeKey);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			EntityReferenceWithAttributeKey that = (EntityReferenceWithAttributeKey) o;
			return Objects.equals(entityReference, that.entityReference) && Objects.equals(attributeKey, that.attributeKey);
		}
	}

}
