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

import io.evitadb.api.data.*;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.Mutation;
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.UpsertAssociatedDataMutation;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.data.mutation.entity.SetHierarchicalPlacementMutation;
import io.evitadb.api.data.mutation.price.PriceInnerRecordHandlingMutation;
import io.evitadb.api.data.mutation.price.UpsertPriceMutation;
import io.evitadb.api.data.mutation.reference.InsertReferenceMutation;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Builder that is used to create the entity.
 * Due to performance reasons (see {@link DirectWriteOrOperationLog} microbenchmark) there is special implementation
 * for the situation when entity is newly created. In this case we know everything is new and we don't need to closely
 * monitor the changes so this can speed things up.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InitialEntityBuilder implements EntityBuilder {
	private static final long serialVersionUID = -3674623071115207036L;
	private final ReferenceUniqueAttributeCheck referenceUniqueAttributeCheck = new ReferenceUniqueAttributeCheck();

	private final Serializable type;
	private final EntitySchema schema;
	private final Integer primaryKey;
	@Delegate(types = AttributesContract.class)
	private final AttributesBuilder attributesBuilder;
	@Delegate(types = AssociatedDataContract.class)
	private final AssociatedDataBuilder associatedDataBuilder;
	@Delegate(types = PricesContract.class)
	private final PricesBuilder pricesBuilder;
	private final Map<EntityReferenceContract<?>, ReferenceContract> references;
	private HierarchicalPlacement hierarchicalPlacement;

	public InitialEntityBuilder(@Nonnull Serializable type) {
		this.type = type;
		this.schema = new EntitySchema(type);
		this.primaryKey = null;
		this.attributesBuilder = new InitialAttributesBuilder(schema);
		this.associatedDataBuilder = new InitialAssociatedDataBuilder(schema);
		this.pricesBuilder = new InitialPricesBuilder();
		this.references = new HashMap<>();
	}

	public InitialEntityBuilder(@Nonnull EntitySchema schema) {
		this.type = schema.getName();
		this.schema = schema;
		this.primaryKey = null;
		this.attributesBuilder = new InitialAttributesBuilder(schema);
		this.associatedDataBuilder = new InitialAssociatedDataBuilder(schema);
		this.pricesBuilder = new InitialPricesBuilder();
		this.references = new HashMap<>();
	}

	public InitialEntityBuilder(@Nonnull Serializable type, @Nullable Integer primaryKey) {
		this.type = type;
		this.primaryKey = primaryKey;
		this.schema = new EntitySchema(type);
		this.attributesBuilder = new InitialAttributesBuilder(schema);
		this.associatedDataBuilder = new InitialAssociatedDataBuilder(schema);
		this.pricesBuilder = new InitialPricesBuilder();
		this.references = new HashMap<>();
	}

	public InitialEntityBuilder(@Nonnull EntitySchema schema, @Nullable Integer primaryKey) {
		this.type = schema.getName();
		this.schema = schema;
		this.primaryKey = primaryKey;
		this.attributesBuilder = new InitialAttributesBuilder(schema);
		this.associatedDataBuilder = new InitialAssociatedDataBuilder(schema);
		this.pricesBuilder = new InitialPricesBuilder();
		this.references = new HashMap<>();
	}

	@Override
	public boolean isDropped() {
		return false;
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@Override
	@Nonnull
	public Serializable getType() {
		return type;
	}

	@Override
	@Nonnull
	public EntitySchema getSchema() {
		return schema;
	}

	@Override
	@Nullable
	public Integer getPrimaryKey() {
		return primaryKey;
	}

	@Override
	public HierarchicalPlacement getHierarchicalPlacement() {
		return hierarchicalPlacement;
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences() {
		return references.values();
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences(Serializable referencedEntityType) {
		return references
			.values()
			.stream()
			.filter(it -> Objects.equals(referencedEntityType, it.getReferencedEntity().getType()))
			.collect(Collectors.toList());
	}

	@Nullable
	@Override
	public ReferenceContract getReference(Serializable referencedEntityType, int referencedEntityId) {
		return references.get(new EntityReference(referencedEntityType, referencedEntityId));
	}

	@Nonnull
	public Set<Locale> getLocales() {
		return Stream.concat(
				attributesBuilder.getAttributeLocales().stream(),
				associatedDataBuilder.getAssociatedDataLocales().stream()
			)
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public EntityBuilder removeAttribute(@Nonnull String attributeName) {
		attributesBuilder.removeAttribute(attributeName);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull T attributeValue) {
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue) {
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		attributesBuilder.removeAttribute(attributeName, locale);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T attributeValue) {
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue) {
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder mutateAttribute(@Nonnull AttributeMutation mutation) {
		attributesBuilder.mutateAttribute(mutation);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAssociatedData(@Nonnull String associatedDataName) {
		associatedDataBuilder.removeAssociatedData(associatedDataName);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T associatedDataValue) {
		associatedDataBuilder.setAssociatedData(associatedDataName, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T[] associatedDataValue) {
		associatedDataBuilder.setAssociatedData(associatedDataName, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		associatedDataBuilder.removeAssociatedData(associatedDataName, locale);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T associatedDataValue) {
		associatedDataBuilder.setAssociatedData(associatedDataName, locale, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T[] associatedDataValue) {
		associatedDataBuilder.setAssociatedData(associatedDataName, locale, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder mutateAssociatedData(@Nonnull AssociatedDataMutation mutation) {
		associatedDataBuilder.mutateAssociatedData(mutation);
		return this;
	}

	@Override
	public EntityBuilder setHierarchicalPlacement(int orderAmongSiblings) {
		this.hierarchicalPlacement = new HierarchicalPlacement(orderAmongSiblings);
		return this;
	}

	@Override
	public EntityBuilder setHierarchicalPlacement(int parentPrimaryKey, int orderAmongSiblings) {
		this.hierarchicalPlacement = new HierarchicalPlacement(parentPrimaryKey, orderAmongSiblings);
		return this;
	}

	@Override
	public EntityBuilder removeHierarchicalPlacement() {
		this.hierarchicalPlacement = null;
		return this;
	}

	@Override
	public EntityBuilder setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey) {
		return setReference(referencedEntityType, referencedPrimaryKey, null);
	}

	@Override
	public EntityBuilder setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey, @Nullable Consumer<ReferenceBuilder> whichIs) {
		final EntityReference referencedEntity = new EntityReference(referencedEntityType, referencedPrimaryKey);
		final InitialReferenceBuilder builder = new InitialReferenceBuilder(this.schema, referencedEntity, referenceUniqueAttributeCheck);
		ofNullable(whichIs).ifPresent(it -> it.accept(builder));
		final Reference reference = builder.build();
		references.put(reference.getReferencedEntity(), reference);
		return this;
	}

	@Override
	public EntityBuilder removeReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey) {
		this.references.remove(new EntityReference(referencedEntityType, referencedPrimaryKey));
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		pricesBuilder.setPrice(priceId, priceList, currency, priceWithoutVat, vat, priceWithVat, sellable);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		pricesBuilder.setPrice(priceId, priceList, currency, innerRecordId, priceWithoutVat, vat, priceWithVat, sellable);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, DateTimeRange validity, boolean sellable) {
		pricesBuilder.setPrice(priceId, priceList, currency, priceWithoutVat, vat, priceWithVat, validity, sellable);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		pricesBuilder.setPrice(priceId, priceList, currency, innerRecordId, priceWithoutVat, vat, priceWithVat, validity, sellable);
		return this;
	}

	@Override
	public EntityBuilder removePrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		pricesBuilder.removePrice(priceId, priceList, currency);
		return this;
	}

	@Override
	public EntityBuilder setPriceInnerRecordHandling(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		pricesBuilder.setPriceInnerRecordHandling(priceInnerRecordHandling);
		return this;
	}

	@Override
	public EntityBuilder removePriceInnerRecordHandling() {
		pricesBuilder.removePriceInnerRecordHandling();
		return this;
	}

	@Override
	public EntityBuilder removeAllNonTouchedPrices() {
		pricesBuilder.removeAllNonTouchedPrices();
		return this;
	}

	@Override
	public EntityMutation toMutation() {
		return new InitEntityMutation(
			schema,
			getPrimaryKey(),
			getType(),
			getHierarchicalPlacement(),
			attributesBuilder.build(),
			associatedDataBuilder.build(),
			pricesBuilder.build(),
			references.values(),
			getAttributeLocales()
		);
	}

	@Override
	public EntityMutation toRemovalMutation() {
		throw new IllegalArgumentException("Cannot remove entity that hasn't yet been created!");
	}

	@Override
	public Entity toInstance() {
		return toMutation().mutate(null);
	}

	static class InitEntityMutation implements Mutation, EntityMutation {
		private static final long serialVersionUID = -3370279011181397739L;
		private final EntitySchema entitySchema;
		private final Serializable entityType;
		private final HierarchicalPlacementContract hierarchicalPlacement;
		private final Attributes attributes;
		private final AssociatedData associatedData;
		private final Prices prices;
		private final Collection<ReferenceContract> references;
		private final Set<Locale> locales;
		@Getter private final Collection<? extends LocalMutation<?, ?>> localMutations;
		private Integer entityPrimaryKey;

		public InitEntityMutation(EntitySchema entitySchema, Integer entityPrimaryKey, Serializable entityType, HierarchicalPlacementContract hierarchicalPlacement, Attributes attributes, AssociatedData associatedData, Prices prices, Collection<ReferenceContract> references, Set<Locale> locales) {
			this.entityPrimaryKey = entityPrimaryKey;
			this.entitySchema = entitySchema;
			this.entityType = entityType;
			this.hierarchicalPlacement = hierarchicalPlacement;
			this.attributes = attributes;
			this.associatedData = associatedData;
			this.prices = prices;
			this.references = references;
			this.locales = locales;
			this.localMutations = Stream.of(
					ofNullable(hierarchicalPlacement)
						.map(it -> ofNullable(it.getParentPrimaryKey())
							.map(ppk -> new SetHierarchicalPlacementMutation(ppk, it.getOrderAmongSiblings()))
							.orElseGet(() -> new SetHierarchicalPlacementMutation(it.getOrderAmongSiblings()))
						)
						.stream(),
					references
						.stream()
						.flatMap(it -> new InsertReferenceMutation(it, entitySchema).generateMutations()),
					attributes
						.getAttributeValues()
						.stream()
						.map(it -> new UpsertAttributeMutation(it.getKey(), it.getValue())),
					associatedData
						.getAssociatedDataValues()
						.stream()
						.map(it -> new UpsertAssociatedDataMutation(it.getKey(), it.getValue())),
					Stream.of(
						new PriceInnerRecordHandlingMutation(prices.getPriceInnerRecordHandling())
					),
					prices
						.getPrices()
						.stream()
						.map(it -> new UpsertPriceMutation(it.getPriceKey(), it))
				)
				.flatMap(it -> it)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}

		@Nonnull
		@Override
		public Serializable getEntityType() {
			return this.entityType;
		}

		@Nullable
		@Override
		public Integer getEntityPrimaryKey() {
			return this.entityPrimaryKey;
		}

		@Override
		public void setEntityPrimaryKey(@Nonnull Integer primaryKey) {
			Assert.isTrue(
				this.entityPrimaryKey == null,
				() -> new IllegalStateException("Id is already generated for existing entity!")
			);
			this.entityPrimaryKey = primaryKey;
		}

		@Nonnull
		@Override
		public EntityExistence expects() {
			return EntityExistence.MUST_NOT_EXIST;
		}

		@Nonnull
		@Override
		public Entity mutate(@Nullable Entity entity) {
			Assert.isTrue(
				entity == null,
				() -> new InvalidMutationException(
					"There is already entity " + entity.getType() + " with primary key " +
						entity.getPrimaryKey() + " present! Please fetch this entity and perform update " +
						"operation on top of it."
				)
			);
			return new Entity(
				1,
				entitySchema,
				entityPrimaryKey,
				hierarchicalPlacement,
				references,
				attributes,
				associatedData,
				prices,
				locales,
				false
			);
		}

	}

	private class ReferenceUniqueAttributeCheck implements BiPredicate<Serializable, String>, Serializable {
		private static final long serialVersionUID = -1720123020048815327L;

		@Override
		public boolean test(Serializable entityType, String attributeName) {
			return getReferences()
				.stream()
				.filter(it -> Objects.equals(entityType, it.getReferencedEntity().getType()))
				.anyMatch(it -> it.getAttribute(attributeName) != null);
		}

	}

}
