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
import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.reference.InsertReferenceMutation;
import io.evitadb.api.data.mutation.reference.ReferenceMutation;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Builder that is used to create new {@link Reference} instance.
 * Due to performance reasons (see {@link DirectWriteOrOperationLog} microbenchmark) there is special implementation
 * for the situation when entity is newly created. In this case we know everything is new and we don't need to closely
 * monitor the changes so this can speed things up.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InitialReferenceBuilder implements ReferenceBuilder {
	private static final long serialVersionUID = 2225492596172273289L;

	private final EntitySchema entitySchema;
	private final BiPredicate<Serializable, String> uniqueAttributePredicate;
	@Getter private final EntityReference referencedEntity;
	@Delegate(types = AttributesContract.class)
	private final AttributesBuilder attributesBuilder;
	@Getter private GroupEntityReference group;

	public <T extends BiPredicate<Serializable, String> & Serializable> InitialReferenceBuilder(EntitySchema entitySchema, EntityReference referencedEntity, T uniqueAttributePredicate) {
		this.entitySchema = entitySchema;
		this.referencedEntity = referencedEntity;
		this.uniqueAttributePredicate = uniqueAttributePredicate;
		this.group = null;
		this.attributesBuilder = new InitialAttributesBuilder(entitySchema, true);
	}

	public <T extends BiPredicate<Serializable, String> & Serializable> InitialReferenceBuilder(EntitySchema entitySchema, EntityReference referencedEntity, GroupEntityReference group, T uniqueAttributePredicate) {
		this.entitySchema = entitySchema;
		this.referencedEntity = referencedEntity;
		this.uniqueAttributePredicate = uniqueAttributePredicate;
		this.group = group;
		this.attributesBuilder = new InitialAttributesBuilder(entitySchema);
	}

	@Override
	public boolean isDropped() {
		return false;
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@Nullable
	@Override
	public ReferenceSchema getReferenceSchema() {
		return this.entitySchema.getReference(referencedEntity.getType());
	}

	@Nonnull
	@Override
	public ReferenceBuilder setGroup(@Nonnull Serializable referencedEntity, int primaryKey) {
		this.group = new GroupEntityReference(referencedEntity, primaryKey);
		return this;
	}

	@Nonnull
	@Override
	public ReferenceBuilder removeGroup() {
		this.group = null;
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
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.referencedEntity.getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass());
		verifySortableAttributeUniqueness(referenceSchema, attributeName, uniqueAttributePredicate);
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> ReferenceBuilder setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.referencedEntity.getType());
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
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.referencedEntity.getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass(), locale);
		verifySortableAttributeUniqueness(referenceSchema, attributeName, uniqueAttributePredicate);
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> ReferenceBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.referencedEntity.getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, attributeName, attributeValue.getClass(), locale);
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public ReferenceBuilder mutateAttribute(@Nonnull AttributeMutation mutation) {
		final ReferenceSchema referenceSchema = entitySchema.getReference(this.referencedEntity.getType());
		verifyAttributeIsInSchemaAndTypeMatch(entitySchema, referenceSchema, mutation.getAttributeKey().getAttributeName(), null);
		attributesBuilder.mutateAttribute(mutation);
		return this;
	}

	@Nonnull
	@Override
	public Stream<? extends ReferenceMutation<?>> buildChangeSet() {
		return new InsertReferenceMutation(build(), entitySchema).generateMutations();
	}

	@Nonnull
	@Override
	public Reference build() {
		return new Reference(
			entitySchema,
			1,
			referencedEntity,
			group,
			attributesBuilder.build(),
			false
		);
	}

	static void verifyAttributeIsInSchemaAndTypeMatch(@Nonnull EntitySchema entitySchema, @Nullable ReferenceSchema referenceSchema, @Nonnull String attributeName, @Nullable Class<? extends Serializable> aClass) {
		final AttributeSchema attributeSchema = ofNullable(referenceSchema)
			.map(it -> it.getAttribute(attributeName))
			.orElse(null);
		InitialAttributesBuilder.verifyAttributeIsInSchemaAndTypeMatch(entitySchema, attributeName, aClass, null, attributeSchema);
	}

	static void verifyAttributeIsInSchemaAndTypeMatch(@Nonnull EntitySchema entitySchema, @Nullable ReferenceSchema referenceSchema, @Nonnull String attributeName, @Nullable Class<? extends Serializable> aClass, @Nonnull Locale locale) {
		final AttributeSchema attributeSchema = ofNullable(referenceSchema)
			.map(it -> it.getAttribute(attributeName))
			.orElse(null);
		InitialAttributesBuilder.verifyAttributeIsInSchemaAndTypeMatch(entitySchema, attributeName, aClass, locale, attributeSchema);
	}

	static void verifySortableAttributeUniqueness(@Nullable ReferenceSchema referenceSchema, @Nonnull String attributeName, @Nonnull BiPredicate<Serializable, String> uniqueAttributePredicate) {
		final AttributeSchema attributeSchema = ofNullable(referenceSchema)
			.map(it -> it.getAttribute(attributeName))
			.orElse(null);
		if (attributeSchema != null && attributeSchema.isSortable()) {
			Assert.isTrue(
				!uniqueAttributePredicate.test(referenceSchema.getEntityType(), attributeName),
				() -> new IllegalArgumentException(
					"Attribute " + attributeName + " is sortable and only single reference of type " + referenceSchema.getEntityType() + " may use it!" +
						" In this entity there is already " + attributeName + " present on another reference of this type!"
				)
			);
		}
	}

}
