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

package io.evitadb.api.data.mutation.attribute;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.Mutation;
import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Attribute {@link Mutation} allows to execute mutation operations on {@link Attributes} of the {@link Entity}
 * object. Each attribute change is increases {@link AttributeValue#getVersion()} by one, attribute removal only sets
 * tombstone flag on a attribute value and doesn't really remove it. Possible removal will be taken care of during
 * compaction process, leaving attributes in place allows to see last assigned value to the attribute and also consult
 * last version of the attribute.
 *
 * These traits should help to manage concurrent transactional process as updates to the same entity could be executed
 * safely and concurrently as long as attribute modification doesn't overlap. Some mutations may also overcome same
 * attribute concurrent modification if it's safely additive (i.e. incrementation / decrementation and so on).
 *
 * Exact mutations also allows engine implementation to safely update only those indexes that the change really affects
 * and doesn't require additional analysis.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class AttributeMutation implements LocalMutation<AttributeValue, AttributeKey> {
	private static final long serialVersionUID = 8615227519108169551L;
	@Getter @Nonnull protected final AttributeKey attributeKey;

	protected AttributeMutation(@Nonnull AttributeKey attributeKey) {
		this.attributeKey = attributeKey;
	}

	@Override
	public @Nonnull
	Class<AttributeValue> affects() {
		return AttributeValue.class;
	}

	@Override
	public AttributeKey getComparableKey() {
		return attributeKey;
	}

	@Nonnull
	public EntitySchema verifyOrEvolveSchema(
		@Nonnull EntitySchema schema,
		@Nullable AttributeSchema attributeSchema,
		@Nonnull Serializable attributeValue,
		@Nonnull Supplier<EntitySchema> schemaEvolutionApplicator
	) throws InvalidMutationException {
		// when attribute definition is known execute first encounter formal verification
		if (attributeSchema != null) {
			// we need to unwrap array classes - we need to check base class for compatibility with Comparable
			final Class<?> attributeClass;
			if (attributeValue instanceof Object[]) {
				attributeClass = attributeValue.getClass().getComponentType();
			} else {
				attributeClass = attributeValue.getClass();
			}
			Assert.isTrue(
				attributeSchema.getType().isInstance(attributeValue),
				() -> new InvalidMutationException(
					"Invalid type: " + attributeValue.getClass() + "! " +
						"Attribute " + attributeKey.getAttributeName() + " was already stored as type " + attributeSchema.getType() + ". " +
						"All values of attribute " + attributeKey.getAttributeName() + " must respect this data type!"
				)
			);
			if (attributeSchema.isFilterable()) {
				Assert.isTrue(
					Comparable.class.isAssignableFrom(attributeClass),
					() -> new InvalidMutationException(
						"Attribute " + attributeKey.getAttributeName() + " is filterable and needs to implement Comparable interface but it doesn't: " + attributeValue.getClass() + "!"
					)
				);
			}
			if (attributeSchema.isSortable()) {
				Assert.isTrue(
					Comparable.class.isAssignableFrom(attributeClass),
					() -> new InvalidMutationException(
						"Attribute " + attributeKey.getAttributeName() + " is filterable and needs to implement Comparable interface but it doesn't: " + attributeValue.getClass() + "!"
					)
				);
			}
			if (attributeSchema.isLocalized()) {
				Assert.isTrue(
					attributeKey.isLocalized(),
					() -> new InvalidMutationException(
						"Attribute " + attributeKey.getAttributeName() + " was already stored as localized value. " +
							"All values of attribute " + attributeKey.getAttributeName() + " must be localized now " +
							"- use different attribute name for locale independent variant of attribute!!"
					)
				);
				final Locale locale = attributeKey.getLocale();
				if (!schema.getLocales().contains(locale)) {
					if (schema.allows(EvolutionMode.ADDING_LOCALES)) {
						// evolve schema automatically
						return schemaEvolutionApplicator.get();
					} else {
						throw new InvalidMutationException(
							"Attribute " + attributeKey.getAttributeName() + " is localized to " + locale + " which is not allowed by the schema" +
								" (allowed are only: " + schema.getLocales().stream().map(Locale::toString).collect(Collectors.joining(", ")) + "). " +
								"You must first alter entity schema to be able to add this localized attribute to the entity!"
						);
					}
				}
			} else {
				Assert.isTrue(
					!attributeKey.isLocalized(),
					() -> new InvalidMutationException(
						"Attribute " + attributeKey.getAttributeName() + " was not stored as localized value. " +
							"No values of attribute " + attributeKey.getAttributeName() + " can be localized now " +
							"- use different attribute name for localized variant of attribute!"
					)
				);
			}
			return schema;
			// else check whether adding attributes on the fly is allowed
		} else if (schema.allows(EvolutionMode.ADDING_ATTRIBUTES)) {
			// evolve schema automatically
			return schemaEvolutionApplicator.get();
		} else {
			throw new InvalidMutationException(
				"Unknown attribute " + attributeKey.getAttributeName() + "! You must first alter entity schema to be able to add this attribute to the entity!"
			);
		}
	}

}
