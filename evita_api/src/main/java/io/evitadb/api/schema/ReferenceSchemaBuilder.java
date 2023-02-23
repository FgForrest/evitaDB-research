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

package io.evitadb.api.schema;

import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.AssociatedData;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.exception.InvalidSchemaMutationException;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.require.Attributes;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

/**
 * Internal {@link ReferenceSchema} builder used solely from within {@link EntitySchemaBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceSchemaBuilder {
	/**
	 * Entity type of the reference.
	 */
	private final Serializable entityType;
	/**
	 * Contains true if {@link #entityType} relates to entity maintained by Evita.
	 */
	private final boolean entityTypeRelatesToEntity;
	/**
	 * Contains relation specific attributes of the entity.
	 */
	private final Map<String, AttributeSchema> attributes;
	/**
	 * May be set by the client - specifies entity type of the reference group.
	 */
	private Serializable groupType;
	/**
	 * Contains true if {@link #groupType} relates to entity maintained by Evita.
	 */
	private boolean groupTypeRelatesToEntity;
	/**
	 * May be set by the client - contains true if the index should be created for the references of this entity. When
	 * reference is required to be {@link #faceted()} it must be {@link #indexed()} as well.
	 */
	private boolean indexed;
	/**
	 * May be set by the client - contains true if the statistics data for this reference should be maintained and thus
	 * allowing to get {@link FacetStatistics} for this reference or use {@link Facet} filtering constraint.
	 */
	private boolean faceted;

	ReferenceSchemaBuilder(Serializable entityType, boolean entityTypeRelatesToEntity, ReferenceSchema existingReference) {
		this.entityType = entityType;
		this.entityTypeRelatesToEntity = entityTypeRelatesToEntity;
		this.groupType = ofNullable(existingReference).map(ReferenceSchema::getGroupType).orElse(null);
		this.groupTypeRelatesToEntity = ofNullable(existingReference).map(ReferenceSchema::isGroupTypeRelatesToEntity).orElse(false);
		this.indexed = ofNullable(existingReference).map(ReferenceSchema::isIndexed).orElse(false);
		this.faceted = ofNullable(existingReference).map(ReferenceSchema::isFaceted).orElse(false);
		this.attributes = ofNullable(existingReference).map(it -> new LinkedHashMap<>(it.getAttributes())).orElseGet(LinkedHashMap::new);
	}

	/**
	 * Creates attribute type related to reference. Such attributes are fetched in bulk along with the entity body.
	 * Attributes may be indexed for fast filtering ({@link AttributeSchema#isFilterable()}) or can be used to sort along
	 * ({@link AttributeSchema#isSortable()}. Attributes are not automatically indexed in order not to waste precious
	 * memory space for data that will never be used in search queries.
	 *
	 * Filtering in attributes is executed by using constraints like {@link io.evitadb.api.query.filter.And},
	 * {@link io.evitadb.api.query.filter.Not}, {@link io.evitadb.api.query.filter.Equals}, {@link io.evitadb.api.query.filter.Contains}
	 * and many others. Sorting can be achieved with {@link io.evitadb.api.query.order.Ascending},
	 * {@link io.evitadb.api.query.order.Descending} or others.
	 *
	 * Attributes are not recommended for bigger data as they are all loaded at once when {@link Attributes}
	 * requirement is used. Large data that are occasionally used store in {@link AssociatedData}.
	 *
	 * @param ofType type of the entity. Must be one of {@link io.evitadb.api.dataType.EvitaDataTypes#getSupportedDataTypes()} types
	 */
	public ReferenceSchemaBuilder withAttribute(String attributeName, @Nonnull Class<? extends Serializable> ofType) {
		return withAttribute(attributeName, ofType, null);
	}

	/**
	 * Creates attribute type related to reference. Such attributes are fetched in bulk along with the entity body.
	 * Attributes may be indexed for fast filtering ({@link AttributeSchema#isFilterable()}) or can be used to sort along
	 * ({@link AttributeSchema#isSortable()}. Attributes are not automatically indexed in order not to waste precious
	 * memory space for data that will never be used in search queries.
	 *
	 * Filtering in attributes is executed by using constraints like {@link io.evitadb.api.query.filter.And},
	 * {@link io.evitadb.api.query.filter.Not}, {@link io.evitadb.api.query.filter.Equals}, {@link io.evitadb.api.query.filter.Contains}
	 * and many others. Sorting can be achieved with {@link io.evitadb.api.query.order.Ascending},
	 * {@link io.evitadb.api.query.order.Descending} or others.
	 *
	 * Attributes are not recommended for bigger data as they are all loaded at once when {@link Attributes}
	 * requirement is used. Large data that are occasionally used store in {@link AssociatedData}.
	 *
	 * @param ofType  type of the entity. Must be one of {@link io.evitadb.api.dataType.EvitaDataTypes#getSupportedDataTypes()} types
	 * @param whichIs lambda that allows to specify attributes of the attribute itself
	 */
	public ReferenceSchemaBuilder withAttribute(String attributeName, @Nonnull Class<? extends Serializable> ofType, Consumer<AttributeSchemaBuilder> whichIs) {
		final AttributeSchema existingAttribute = this.attributes.get(attributeName);
		final AttributeSchemaBuilder attributeSchemaBuilder = ofNullable(existingAttribute)
			.map(it -> {
				Assert.isTrue(
					ofType.equals(it.getType()),
					() -> new InvalidSchemaMutationException(
						"Attribute " + attributeName + " has already assigned type " + it.getType() +
							", cannot change this type to: " + ofType + "!"
					)
				);
				return new AttributeSchemaBuilder(it);
			})
			.orElseGet(() -> new AttributeSchemaBuilder(attributeName, ofType));

		ofNullable(whichIs).ifPresent(it -> it.accept(attributeSchemaBuilder));
		final AttributeSchema attributeSchema = attributeSchemaBuilder.build();
		if (existingAttribute == null || !existingAttribute.equals(attributeSchema)) {
			this.attributes.put(attributeSchema.getName(), attributeSchema);
		}
		return this;
	}

	/**
	 * Removes possibly existing attribute type related to reference.
	 */
	public ReferenceSchemaBuilder withoutAttribute(String attributeName) {
		this.attributes.remove(attributeName);
		return this;
	}

	/**
	 * Specifies that reference of this type will be related to external entity not maintained in Evita.
	 */
	public ReferenceSchemaBuilder withGroupType(Serializable groupType) {
		this.groupTypeRelatesToEntity = false;
		this.groupType = groupType;
		return this;
	}

	/**
	 * Specifies that reference of this type will be related to another entity maintained in Evita ({@link Entity#getType()}).
	 */
	public ReferenceSchemaBuilder withGroupTypeRelatedToEntity(Serializable groupType) {
		this.groupTypeRelatesToEntity = true;
		this.groupType = groupType;
		return this;
	}

	/**
	 * Specifies that this reference will not be grouped to a specific groups. This is default setting for the reference.
	 */
	public ReferenceSchemaBuilder withoutGroupType() {
		this.groupTypeRelatesToEntity = false;
		this.groupType = null;
		return this;
	}

	/**
	 * Contains TRUE if the index for this reference should be created and maintained allowing to filter by
	 * {@link io.evitadb.api.query.filter.ReferenceHavingAttribute} filtering constraints. Index is also required
	 * when reference is {@link #faceted()}.
	 *
	 * Do not mark reference as faceted unless you know that you'll need to filter entities by this reference. Each
	 * indexed reference occupies (memory/disk) space in the form of index. When reference is not indexed, the entity
	 * cannot be looked up by reference attributes or relation existence itself, but the data is loaded alongside
	 * other references and is available by calling {@link SealedEntity#getReferences()} method.
	 */
	public ReferenceSchemaBuilder indexed() {
		this.indexed = true;
		return this;
	}

	/**
	 * Makes reference as non-faceted. This means reference information will be available on entity when loaded but
	 * cannot be used in filtering.
	 */
	public ReferenceSchemaBuilder nonIndexed() {
		this.indexed = false;
		return this;
	}

	/**
	 * Makes reference faceted. That means that statistics data for this reference should be maintained and this
	 * allowing to get {@link FacetStatistics} for this reference or use {@link Facet} filtering constraint. When
	 * reference is faceted it is also automatically made {@link #indexed()} as well.
	 *
	 * Do not mark reference as faceted unless you know that you'll need to filter entities by this reference. Each
	 * indexed reference occupies (memory/disk) space in the form of index.
	 */
	public ReferenceSchemaBuilder faceted() {
		this.indexed = true;
		this.faceted = true;
		return this;
	}

	/**
	 * Makes reference as non-faceted. This means reference information will be available on entity when loaded but
	 * cannot be part of the computed facet statistics and filtering by facet constraint.
	 *
	 * @return builder to continue with configuration
	 */
	public ReferenceSchemaBuilder nonFaceted() {
		this.faceted = false;
		return this;
	}

	/**
	 * Creates reference schema instance.
	 */
	public ReferenceSchema build() {
		return new ReferenceSchema(
			entityType, entityTypeRelatesToEntity,
			groupType, groupTypeRelatesToEntity,
			indexed, faceted, attributes
		);
	}

}
