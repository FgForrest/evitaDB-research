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

import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.dataType.ComplexDataObject;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.exception.InvalidSchemaMutationException;
import io.evitadb.api.exception.SchemaAlteringException;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.query.filter.Language;
import io.evitadb.api.query.filter.PriceInPriceLists;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.require.*;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Optional.ofNullable;

/**
 * Internal {@link EntitySchema} builder returned by {@link EntityCollectionBase} when
 * {@link EntityCollectionBase#defineSchema()} is called.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class EntitySchemaBuilder {
	/**
	 * Defines version of the schema update. Used for optimistic locking.
	 */
	private final int version;
	/**
	 * Contains unique name of the entity type (cannot be changed).
	 */
	private final Serializable name;
	/**
	 * Callback method that allows to propagate changes once builder process is completed.
	 */
	private final UnaryOperator<EntitySchema> schemaUpdater;
	/**
	 * May be set by the client - defines set of allowed schema changes that are performed on the fly.
	 */
	private final EnumSet<EvolutionMode> evolutionMode;
	/**
	 * May be set by the client - may be se to true when Evita should be responsible for generating primary keys.
	 */
	private boolean withGeneratedPrimaryKey;
	/**
	 * May be set by the client - defines whether entity is organized in tree structure.
	 */
	private boolean withHierarchy;
	/**
	 * May be set by the client - defines whether entity maintains related prices.
	 */
	private boolean withPrice;
	/**
	 * May be set by the client - defines set of allowed {@link Locale} (i.e. languages).
	 */
	private final Set<Locale> locales;
	/**
	 * May be set by the client - defines how many franctional digits will be indexed in prices.
	 */
	private int indexedPricePlaces;
	/**
	 * May be set by the client - defines set of allowed {@link ReferenceSchema}.
	 */
	private final Map<Serializable, ReferenceSchema> references;
	/**
	 * May be set by the client - defines set of allowed {@link AttributeSchema}.
	 */
	private final Map<String, AttributeSchema> attributes;
	/**
	 * May be set by the client - defines set of allowed {@link AssociatedDataSchema}.
	 */
	private final Map<String, AssociatedDataSchema> associatedData;

	public EntitySchemaBuilder(EntitySchema baseSchema, UnaryOperator<EntitySchema> schemaUpdater) {
		this.version = baseSchema.getVersion() + 1;
		this.name = baseSchema.getName();
		this.evolutionMode = EnumSet.copyOf(baseSchema.getEvolutionMode());
		this.schemaUpdater = schemaUpdater;
		this.withGeneratedPrimaryKey = baseSchema.isWithGeneratedPrimaryKey();
		this.withHierarchy = baseSchema.isWithHierarchy();
		this.withPrice = baseSchema.isWithPrice();
		this.indexedPricePlaces = baseSchema.getIndexedPricePlaces();
		this.locales = new LinkedHashSet<>(baseSchema.getLocales());
		this.references = new LinkedHashMap<>(baseSchema.getReferences());
		this.attributes = new LinkedHashMap<>(baseSchema.getAttributes());
		this.associatedData = new LinkedHashMap<>(baseSchema.getAssociatedData());
	}

	/**
	 * Sets strict verification mode for entities of this type. All attributes, references, associated data and languages
	 * will be checked against the entity definition (schema) and validation errors will result in upsert refusal.
	 *
	 * This mode is recommended if you want to strictly control schema and define structure up-front.
	 *
	 * @return
	 */
	public EntitySchemaBuilder verifySchemaStrictly() {
		this.evolutionMode.clear();
		return this;
	}

	/**
	 * This is slightly relaxed mode of the schema evolution. All existing attributes, references, associated data and
	 * languages are checked against the entity definition (schema) but adding new attribute, reference, associated data
	 * or language may be allowed with default settings if you specify {@link EvolutionMode} for it.
	 *
	 * @param evolutionMode
	 * @return
	 */
	public EntitySchemaBuilder verifySchemaButAllow(EvolutionMode... evolutionMode) {
		this.evolutionMode.clear();
		this.evolutionMode.addAll(Arrays.asList(evolutionMode));
		return this;
	}

	/**
	 * This is lax mode of the schema evolution. All existing attributes, references, associated data and
	 * languages are checked against the entity definition (schema) but adding new attribute, reference, associated data
	 * or language may is allowed and added to the schema with the default settings.
	 * @return
	 */
	public EntitySchemaBuilder verifySchemaButCreateOnTheFly() {
		this.evolutionMode.addAll(Arrays.asList(EvolutionMode.values()));
		return this;
	}

	/**
	 * Specifies that entities of this type will have primary keys generated by Evita and not provided from outside.
	 * @return
	 */
	public EntitySchemaBuilder withGeneratedPrimaryKey() {
		this.withGeneratedPrimaryKey = true;
		return this;
	}

	/**
	 * Specifies that entities of this type will have primary keys provided from outside.
	 * @return
	 */
	public EntitySchemaBuilder withoutGeneratedPrimaryKey() {
		this.withGeneratedPrimaryKey = false;
		return this;
	}

	/**
	 * Enables hierarchy structure for this type of entity. Entities may have {@link Entity#getHierarchicalPlacement()}
	 * defined on them. That means that entity may refer to single parent entity and may be
	 * referred by multiple child entities. Hierarchy is always composed of entities of same type.
	 * Each entity must be part of at most single hierarchy (tree).
	 *
	 * Hierarchy can limit returned entities by using filtering constraints {@link WithinHierarchy}. It's also used for
	 * computation of extra data - such as {@link Parents}. It can also invert type of returned entities in case requirement
	 * {@link HierarchyStatistics} is used.
	 */
	public EntitySchemaBuilder withHierarchy() {
		this.withHierarchy = true;
		return this;
	}

	/**
	 * Disables hierarchy structure for this type of entity. This is default setting for new entity types.
	 * @return
	 */
	public EntitySchemaBuilder withoutHierarchy() {
		this.withHierarchy = false;
		return this;
	}

	/**
	 * Enables price related data for this type of entity. Entities may have {@link Entity#getPrices()} defined on them.
	 *
	 * Prices are specific to a very few entities, but because correct price computation is very complex in e-commerce
	 * systems and highly affects performance of the entities filtering and sorting, they deserve first class support
	 * in entity model. It is pretty common in B2B systems single product has assigned dozens of prices for the different
	 * customers.
	 *
	 * Specifying prices on entity allows usage of {@link io.evitadb.api.query.filter.PriceValidIn},
	 * {@link io.evitadb.api.query.filter.PriceBetween}, {@link QueryPriceMode}
	 * and {@link PriceInPriceLists} filtering constraints and also {@link io.evitadb.api.query.order.PriceAscending},
	 * {@link io.evitadb.api.query.order.PriceDescending} ordering of the entities. Additional requirements
	 * {@link PriceHistogram}, {@link Prices}
	 * can be used in query as well.
	 *
	 * This method variant expects that prices may have up to two decimal places.
	 */
	public EntitySchemaBuilder withPrice() {
		this.withPrice = true;
		this.indexedPricePlaces = 2;
		return this;
	}

	/**
	 * Enables price related data for this type of entity. Entities may have {@link Entity#getPrices()} defined on them.
	 *
	 * Prices are specific to a very few entities, but because correct price computation is very complex in e-commerce
	 * systems and highly affects performance of the entities filtering and sorting, they deserve first class support
	 * in entity model. It is pretty common in B2B systems single product has assigned dozens of prices for the different
	 * customers.
	 *
	 * Specifying prices on entity allows usage of {@link io.evitadb.api.query.filter.PriceValidIn},
	 * {@link io.evitadb.api.query.filter.PriceBetween}, {@link QueryPriceMode}
	 * and {@link PriceInPriceLists} filtering constraints and also {@link io.evitadb.api.query.order.PriceAscending},
	 * {@link io.evitadb.api.query.order.PriceDescending} ordering of the entities. Additional requirements
	 * {@link PriceHistogram}, {@link Prices}
	 * can be used in query as well.
	 */
	public EntitySchemaBuilder withPrice(int indexedDecimalPlaces) {
		this.withPrice = true;
		this.indexedPricePlaces = indexedDecimalPlaces;
		return this;
	}

	/**
	 * Disables price related data for this type of entity. This is default setting for new entity types.
	 */
	public EntitySchemaBuilder withoutPrice() {
		this.withPrice = false;
		return this;
	}

	/**
	 * Adds specific {@link Locale} to the set of possible locales (languages) that can be used when specifying localized
	 * {@link AttributeSchema} or {@link AssociatedDataSchema}.
	 *
	 * Allows using {@link Language} filtering constraint in query.
	 *
	 * @param locale
	 * @return
	 */
	public EntitySchemaBuilder withLocale(Locale... locale) {
		this.locales.addAll(Arrays.asList(locale));
		return this;
	}

	/**
	 * Removes specific {@link Locale} from the set of possible locales (languages) that can be used when specifying
	 * localized {@link AttributeSchema} or {@link AssociatedDataSchema}.
	 * @param locale
	 * @return
	 */
	public EntitySchemaBuilder withoutLocale(Locale locale) {
		this.locales.remove(locale);
		return this;
	}

	/**
	 * Adds new {@link AttributeSchema} to the set of allowed attributes of the entity or updates existing.
	 *
	 * If you update existing associated data type all data must be specified again, nothing is preserved.
	 *
	 * Entity (global) attributes allows defining set of data that are fetched in bulk along with the entity body.
	 * Attributes may be indexed for fast filtering ({@link AttributeSchema#isFilterable()}) or can be used to sort along
	 * ({@link AttributeSchema#isSortable()}). Attributes are not automatically indexed in order not to waste precious
	 * memory space for data that will never be used in search queries.
	 *
	 * Filtering in attributes is executed by using constraints like {@link io.evitadb.api.query.filter.And},
	 * {@link io.evitadb.api.query.filter.Not}, {@link io.evitadb.api.query.filter.Equals}, {@link io.evitadb.api.query.filter.Contains}
	 * and many others. Sorting can be achieved with {@link io.evitadb.api.query.order.Ascending},
	 * {@link io.evitadb.api.query.order.Descending} or others.
	 *
	 * Attributes are not recommended for bigger data as they are all loaded at once when {@link Attributes}
	 * requirement is used. Large data that are occasionally used store in {@link io.evitadb.api.data.structure.AssociatedData}.
	 *
	 * @param attributeName
	 * @param ofType type of the entity. Must be one of {@link EvitaDataTypes#getSupportedDataTypes()} types
	 * @return
	 */
	public EntitySchemaBuilder withAttribute(@Nonnull String attributeName, @Nonnull Class<? extends Serializable> ofType) {
		return withAttribute(attributeName, ofType, null);
	}

	/**
	 * Adds new {@link AttributeSchema} to the set of allowed attributes of the entity or updates existing.
	 *
	 * If you update existing associated data type all data must be specified again, nothing is preserved.
	 *
	 * Entity (global) attributes allows defining set of data that are fetched in bulk along with the entity body.
	 * Attributes may be indexed for fast filtering ({@link AttributeSchema#isFilterable()}) or can be used to sort along
	 * ({@link AttributeSchema#isSortable()}). Attributes are not automatically indexed in order not to waste precious
	 * memory space for data that will never be used in search queries.
	 *
	 * Filtering in attributes is executed by using constraints like {@link io.evitadb.api.query.filter.And},
	 * {@link io.evitadb.api.query.filter.Not}, {@link io.evitadb.api.query.filter.Equals}, {@link io.evitadb.api.query.filter.Contains}
	 * and many others. Sorting can be achieved with {@link io.evitadb.api.query.order.Ascending},
	 * {@link io.evitadb.api.query.order.Descending} or others.
	 *
	 * Attributes are not recommended for bigger data as they are all loaded at once when {@link Attributes}
	 * requirement is used. Large data that are occasionally used store in {@link io.evitadb.api.data.structure.AssociatedData}.
	 *
	 * @param attributeName
	 * @param ofType type of the entity. Must be one of {@link EvitaDataTypes#getSupportedDataTypes()} types
	 * @param whichIs lambda that allows to specify attributes of the attribute itself
	 * @return
	 */
	public EntitySchemaBuilder withAttribute(
			@Nonnull String attributeName,
			@Nonnull Class<? extends Serializable> ofType,
			@Nullable Consumer<AttributeSchemaBuilder> whichIs
	) {
		final AttributeSchema existingAttribute = this.attributes.get(attributeName);
		final AttributeSchemaBuilder attributeSchemaBuilder =
			ofNullable(existingAttribute)
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
		if (attributeSchema.isSortable()) {
			Assert.isTrue(
				!attributeSchema.getType().isArray(),
				() -> new InvalidSchemaMutationException(
					"Attribute " + attributeName + " is marked as sortable and thus cannot be array of " +
						attributeSchema.getType() + "!"
				)
			);
		}
		if (existingAttribute == null || !existingAttribute.equals(attributeSchema)) {
			this.attributes.put(attributeSchema.getName(), attributeSchema);
		}
		return this;
	}

	/**
	 * Removes specific {@link AttributeSchema} from the set of allowed attributes of the entity.
	 * @param attributeName
	 * @return
	 */
	public EntitySchemaBuilder withoutAttribute(String attributeName) {
		this.attributes.remove(attributeName);
		return this;
	}

	/**
	 * Adds new {@link AssociatedDataSchema} to the set of allowed associated data of the entity or updates existing.
	 *
	 * If you update existing associated data type all data must be specified again, nothing is preserved.
	 *
	 * Associated data carry additional data entries that are never used for filtering / sorting but may be needed to be fetched
	 * along with entity in order to present data to the target consumer (i.e. user / API / bot). Associated data may be stored
	 * in slower storage and may contain wide range of data types - from small ones (i.e. numbers, strings, dates) up to large
	 * binary arrays representing entire files (i.e. pictures, documents).
	 *
	 * The search query must contain specific {@link AssociatedData} requirement in order
	 * associated data are fetched along with the entity. Associated data are stored and fetched separately by their name.
	 *
	 * @param ofType type of the entity. Must be one of {@link io.evitadb.api.dataType.EvitaDataTypes#getSupportedDataTypes()}
	 *                   types or may represent complex type - which is POJO that can be automatically
	 *                   ({@link io.evitadb.api.data.DataObjectConverter}) converted to the set of basic types.
	 * @param dataName
	 * @return
	 */
	public EntitySchemaBuilder withAssociatedData(String dataName, @Nonnull Class<? extends Serializable> ofType) {
		return withAssociatedData(dataName, ofType, null);
	}

	/**
	 * Adds new {@link AssociatedDataSchema} to the set of allowed associated data of the entity or updates existing.
	 *
	 * If you update existing associated data type all data must be specified again, nothing is preserved.
	 *
	 * Associated data carry additional data entries that are never used for filtering / sorting but may be needed to be fetched
	 * along with entity in order to present data to the target consumer (i.e. user / API / bot). Associated data may be stored
	 * in slower storage and may contain wide range of data types - from small ones (i.e. numbers, strings, dates) up to large
	 * binary arrays representing entire files (i.e. pictures, documents).
	 *
	 * The search query must contain specific {@link AssociatedData} requirement in order
	 * associated data are fetched along with the entity. Associated data are stored and fetched separately by their name.
	 *
	 * @param ofType type of the entity. Must be one of {@link io.evitadb.api.dataType.EvitaDataTypes#getSupportedDataTypes()}
	 *                  types or may represent complex type - which is POJO that can be automatically
	 *                  ({@link io.evitadb.api.data.DataObjectConverter}) converted to the set of basic types.
	 * @param dataName
	 * @param whichIs lambda that allows to specify attributes of the attribute itself
	 * @return
	 */
	public EntitySchemaBuilder withAssociatedData(String dataName, @Nonnull Class<? extends Serializable> ofType, Consumer<AssociatedDataSchemaBuilder> whichIs) {
		final AssociatedDataSchema existingAssociatedData = this.associatedData.get(dataName);
		final AssociatedDataSchemaBuilder associatedDataSchemaBuilder = ofNullable(existingAssociatedData)
			.map(it -> {
				Assert.isTrue(
					ofType.equals(it.getType()),
					() -> new InvalidSchemaMutationException(
						"Associated data " + dataName + " has already assigned type " + existingAssociatedData.getType() +
							", cannot change this type to: " + ofType + "!"
					)
				);
				return new AssociatedDataSchemaBuilder(it);
			})
			.orElseGet(() -> new AssociatedDataSchemaBuilder(
				dataName,
				EvitaDataTypes.isSupportedType(ReflectionLookup.getSimpleType(ofType)) ? ofType : ComplexDataObject.class
			));
		ofNullable(whichIs).ifPresent(it -> it.accept(associatedDataSchemaBuilder));
		final AssociatedDataSchema associatedDataSchema = associatedDataSchemaBuilder.build();
		if (existingAssociatedData == null || !existingAssociatedData.equals(associatedDataSchema)) {
			this.associatedData.put(associatedDataSchema.getName(), associatedDataSchema);
		}
		return this;
	}

	/**
	 * Removes specific {@link AssociatedDataSchema} from the set of allowed associated data of the entity.
	 * @param dataName
	 * @return
	 */
	public EntitySchemaBuilder withoutAssociatedData(String dataName) {
		this.associatedData.remove(dataName);
		return this;
	}

	/**
	 * Adds new {@link ReferenceSchema} to the set of allowed references of the entity or updates existing.
	 *
	 * If you update existing reference type - existing {@link ReferenceSchema#getGroupType()} and {@link ReferenceSchema#getAttributes()}
	 * are preserved unless you make changes to them.
	 *
	 * References refer to other entities (of same or different entity type).
	 * Allows entity filtering (but not sorting) of the entities by using {@link io.evitadb.api.query.filter.Facet} constraint
	 * and statistics computation if when {@link FacetStatistics} requirement is used. Reference
	 * is uniquely represented by int positive number (max. 2<sup>63</sup>-1) and {@link Serializable} entity type and can be
	 * part of multiple reference groups, that are also represented by int and {@link Serializable} entity type.
	 *
	 * Reference id in one entity is unique and belongs to single reference group id. Among multiple entities reference may be part
	 * of different reference groups. Referenced entity type may represent type of another Evita entity or may refer
	 * to anything unknown to Evita that posses unique int key and is maintained by external systems (fe. tag assignment,
	 * group assignment, category assignment, stock assignment and so on). Not all these data needs to be present in
	 * Evita.
	 *
	 * References may carry additional key-value data linked to this entity relation (fe. item count present on certain stock).
	 *
	 * @param externalEntityType
	 * @return
	 */
	public EntitySchemaBuilder withReferenceTo(Serializable externalEntityType) {
		return withReferenceTo(externalEntityType, null);
	}

	/**
	 * Adds new {@link ReferenceSchema} to the set of allowed references of the entity or updates existing.
	 *
	 * If you update existing reference type - existing {@link ReferenceSchema#getGroupType()} and {@link ReferenceSchema#getAttributes()}
	 * are preserved unless you make changes to them.
	 *
	 * References refer to other entities (of same or different entity type).
	 * Allows entity filtering (but not sorting) of the entities by using {@link io.evitadb.api.query.filter.Facet} constraint
	 * and statistics computation if when {@link FacetStatistics} requirement is used. Reference
	 * is uniquely represented by int positive number (max. 2<sup>63</sup>-1) and {@link Serializable} entity type and can be
	 * part of multiple reference groups, that are also represented by int and {@link Serializable} entity type.
	 *
	 * Reference id in one entity is unique and belongs to single reference group id. Among multiple entities reference may be part
	 * of different reference groups. Referenced entity type may represent type of another Evita entity or may refer
	 * to anything unknown to Evita that posses unique int key and is maintained by external systems (fe. tag assignment,
	 * group assignment, category assignment, stock assignment and so on). Not all these data needs to be present in
	 * Evita.
	 *
	 * References may carry additional key-value data linked to this entity relation (fe. item count present on certain stock).
	 *
	 * @param externalEntityType
	 * @param whichIs lambda that allows to define reference specifics
	 * @return
	 */
	public EntitySchemaBuilder withReferenceTo(Serializable externalEntityType, Consumer<ReferenceSchemaBuilder> whichIs) {
		final ReferenceSchemaBuilder referenceBuilder = new ReferenceSchemaBuilder(
				externalEntityType,
				false,
				this.references.get(externalEntityType)
		);
		ofNullable(whichIs).ifPresent(it -> it.accept(referenceBuilder));
		final ReferenceSchema referenceSchema = referenceBuilder.build();
		redefineReferenceType(referenceSchema);
		return this;
	}

	/**
	 * Adds new {@link ReferenceSchema} to the set of allowed references of the entity or updates existing.
	 * {@link Reference#getReferencedEntity()} will represent {@link Entity#getPrimaryKey()} of Evita managed entity of this
	 * {@link EntitySchema#getName()}.
	 *
	 * If you update existing reference type - existing {@link ReferenceSchema#getGroupType()} and {@link ReferenceSchema#getAttributes()}
	 * are preserved unless you make changes to them.
	 *
	 * References refer to other entities (of same or different entity type).
	 * Allows entity filtering (but not sorting) of the entities by using {@link io.evitadb.api.query.filter.Facet} constraint
	 * and statistics computation if when {@link FacetStatistics} requirement is used. Reference
	 * is uniquely represented by int positive number (max. 2<sup>63</sup>-1) and {@link Serializable} entity type and can be
	 * part of multiple reference groups, that are also represented by int and {@link Serializable} entity type.
	 *
	 * Reference id in one entity is unique and belongs to single reference group id. Among multiple entities reference may be part
	 * of different reference groups. Referenced entity type may represent type of another Evita entity or may refer
	 * to anything unknown to Evita that posses unique int key and is maintained by external systems (fe. tag assignment,
	 * group assignment, category assignment, stock assignment and so on). Not all these data needs to be present in
	 * Evita.
	 *
	 * References may carry additional key-value data linked to this entity relation (fe. item count present on certain stock).
	 * @param entityType
	 * @return
	 */
	public EntitySchemaBuilder withReferenceToEntity(Serializable entityType) {
		return withReferenceToEntity(entityType, null);
	}

	/**
	 * Adds new {@link ReferenceSchema} to the set of allowed references of the entity or updates existing.
	 * {@link Reference#getReferencedEntity()} will represent {@link Entity#getPrimaryKey()} of Evita managed entity of this
	 * {@link EntitySchema#getName()}.
	 *
	 * If you update existing reference type - existing {@link ReferenceSchema#getGroupType()} and {@link ReferenceSchema#getAttributes()}
	 * are preserved unless you make changes to them.
	 *
	 * References refer to other entities (of same or different entity type).
	 * Allows entity filtering (but not sorting) of the entities by using {@link io.evitadb.api.query.filter.Facet} constraint
	 * and statistics computation if when {@link FacetStatistics} requirement is used. Reference
	 * is uniquely represented by int positive number (max. 2<sup>63</sup>-1) and {@link Serializable} entity type and can be
	 * part of multiple reference groups, that are also represented by int and {@link Serializable} entity type.
	 *
	 * Reference id in one entity is unique and belongs to single reference group id. Among multiple entities reference may be part
	 * of different reference groups. Referenced entity type may represent type of another Evita entity or may refer
	 * to anything unknown to Evita that posses unique int key and is maintained by external systems (fe. tag assignment,
	 * group assignment, category assignment, stock assignment and so on). Not all these data needs to be present in
	 * Evita.
	 *
	 * References may carry additional key-value data linked to this entity relation (fe. item count present on certain stock).
	 * @param entityType
	 * @param whichIs lambda that allows to define reference specifics
	 * @return
	 */
	public EntitySchemaBuilder withReferenceToEntity(Serializable entityType, Consumer<ReferenceSchemaBuilder> whichIs) {
		final ReferenceSchemaBuilder referenceSchemaBuilder = new ReferenceSchemaBuilder(
				entityType,
				true,
				this.references.get(entityType)
		);
		ofNullable(whichIs).ifPresent(it -> it.accept(referenceSchemaBuilder));
		final ReferenceSchema referenceSchema = referenceSchemaBuilder.build();
		redefineReferenceType(referenceSchema);
		return this;
	}

	/**
	 * Removes specific {@link ReferenceSchema} from the set of allowed references of the entity.
	 * @param entityType
	 * @return
	 */
	public EntitySchemaBuilder withoutReferenceTo(Serializable entityType) {
		this.references.remove(entityType);
		return this;
	}

	/**
	 * Method applies all changes made to the entity type schema by this builder. When this method finishes all changes
	 * are successfully altered in Evita DB.
	 *
	 * @throws SchemaAlteringException when changes were not applied because of an error
	 */
	@Nonnull
	public EntitySchema applyChanges() throws SchemaAlteringException {
		return schemaUpdater.apply(
			new EntitySchema(
				version, name,
				withGeneratedPrimaryKey,
				withHierarchy,
				withPrice,
				indexedPricePlaces,
				locales,
				attributes,
						associatedData,
						references,
						EnumSet.copyOf(evolutionMode)
				)
		);
	}

	void redefineReferenceType(ReferenceSchema referenceSchema) {
		final ReferenceSchema existingReference = this.references.get(referenceSchema.getEntityType());
		if (!Objects.equals(existingReference, referenceSchema)) {
			this.references.put(referenceSchema.getEntityType(), referenceSchema);
		}
	}
}
