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

package io.evitadb.api.data.structure;

import io.evitadb.api.data.AttributesContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.reference.ReferenceAttributesUpdateMutation;
import io.evitadb.api.data.mutation.reference.ReferenceMutation;
import io.evitadb.api.data.mutation.reference.RemoveReferenceGroupMutation;
import io.evitadb.api.data.mutation.reference.UpsertReferenceGroupMutation;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static io.evitadb.api.data.structure.InitialReferenceBuilder.verifyAttributeIsInSchemaAndTypeMatch;
import static io.evitadb.api.data.structure.InitialReferenceBuilder.verifySortableAttributeUniqueness;
import static java.util.Optional.ofNullable;

/**
 * Builder that is used to alter existing {@link Reference}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExistingReferenceBuilder implements ReferenceBuilder, Serializable {
	private static final long serialVersionUID = 4611697377656713570L;

	private final BiPredicate<Serializable, String> uniqueAttributePredicate;
	@Getter private final ReferenceContract baseReference;
	@Getter private final EntitySchema entitySchema;
	@Delegate(types = AttributesContract.class)
	private final ExistingAttributesBuilder attributesBuilder;
	private ReferenceMutation<EntityReference> referenceGroupMutation;

	public <T extends BiPredicate<Serializable, String> & Serializable> ExistingReferenceBuilder(ReferenceContract baseReference, EntitySchema entitySchema, T uniqueAttributePredicate) {
		this.baseReference = baseReference;
		this.entitySchema = entitySchema;
		this.uniqueAttributePredicate = uniqueAttributePredicate;
		this.attributesBuilder = new ExistingAttributesBuilder(
			entitySchema,
			baseReference.getAttributeValues(), baseReference.getAttributeLocales(),
			true
		);
	}

	@Override
	public boolean isDropped() {
		return baseReference.isDropped();
	}

	@Override
	public int getVersion() {
		return baseReference.getVersion();
	}

	@Nonnull
	@Override
	public EntityReference getReferencedEntity() {
		return baseReference.getReferencedEntity();
	}

	@Nullable
	@Override
	public GroupEntityReference getGroup() {
		final GroupEntityReference group = ofNullable(baseReference.getGroup())
			.map(it -> ofNullable(referenceGroupMutation).map(fgm -> fgm.mutateLocal(baseReference).getGroup()).orElse(it))
			.orElseGet(() -> ofNullable(referenceGroupMutation).map(fgm -> fgm.mutateLocal(baseReference).getGroup()).orElse(null));
		return ofNullable(group)
			.filter(GroupEntityReference::exists)
			.orElse(null);
	}

	@Nullable
	@Override
	public ReferenceSchema getReferenceSchema() {
		return this.entitySchema.getReference(baseReference.getReferencedEntity().getType());
	}

	@Nonnull
	@Override
	public ReferenceBuilder setGroup(@Nonnull Serializable referencedEntity, int primaryKey) {
		this.referenceGroupMutation = new UpsertReferenceGroupMutation(
			baseReference.getReferencedEntity(),
			entitySchema,
			new GroupEntityReference(referencedEntity, primaryKey)
		);
		return this;
	}

	@Nonnull
	@Override
	public ReferenceBuilder removeGroup() {
		this.referenceGroupMutation = new RemoveReferenceGroupMutation(baseReference.getReferencedEntity(), entitySchema);
		return this;
	}

	@Nonnull
	@Override
	public ReferenceBuilder removeAttribute(@Nonnull String attributeName) {
		attributesBuilder.removeAttribute(attributeName);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> ReferenceBuilder setAttribute(@Nonnull String attributeName, @Nonnull T attributeValue) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.getReferencedEntity().getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass());
		verifySortableAttributeUniqueness(referenceSchema, attributeName, uniqueAttributePredicate);
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> ReferenceBuilder setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.getReferencedEntity().getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass());
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public ReferenceBuilder removeAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		attributesBuilder.removeAttribute(attributeName, locale);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> ReferenceBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T attributeValue) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.getReferencedEntity().getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass());
		verifySortableAttributeUniqueness(referenceSchema, attributeName, uniqueAttributePredicate);
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> ReferenceBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.getReferencedEntity().getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass());
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public ReferenceBuilder mutateAttribute(@Nonnull AttributeMutation mutation) {
		attributesBuilder.mutateAttribute(mutation);
		return this;
	}

	@Nonnull
	@Override
	public Stream<? extends ReferenceMutation<?>> buildChangeSet() {
		return Stream.concat(
				Stream.of(referenceGroupMutation),
				attributesBuilder
					.buildChangeSet()
					.map(it -> new ReferenceAttributesUpdateMutation(baseReference.getReferencedEntity(), entitySchema, it))
			)
			.filter(Objects::nonNull);
	}

	@Nonnull
	@Override
	public ReferenceContract build() {
		final GroupEntityReference newGroup = getGroup();
		final Attributes newAttributes = attributesBuilder.build();
		final boolean groupDiffers = ofNullable(baseReference.getGroup())
			.map(it -> it.differsFrom(newGroup))
			.orElseGet(() -> newGroup != null);

		if (groupDiffers || attributesBuilder.isThereAnyChangeInMutations()) {
			return new Reference(
				entitySchema,
				getVersion() + 1,
				getReferencedEntity(),
				newGroup,
				newAttributes,
				false
			);
		} else {
			return baseReference;
		}
	}
}
