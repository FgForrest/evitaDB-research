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
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.RemoveAssociatedDataMutation;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation;
import io.evitadb.api.data.mutation.entity.RemoveHierarchicalPlacementMutation;
import io.evitadb.api.data.mutation.entity.SetHierarchicalPlacementMutation;
import io.evitadb.api.data.mutation.price.PriceMutation;
import io.evitadb.api.data.mutation.price.RemovePriceMutation;
import io.evitadb.api.data.mutation.reference.*;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.data.structure.SerializablePredicate.ExistsPredicate;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.query.require.PriceFetchMode;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Builder that is used to alter existing {@link Entity}. Entity is immutable object so there is need for another object
 * that would simplify the process of updating its contents. This is why the builder class exists.
 *
 * This builder is suitable for the situation when there already is some entity at place and we need to alter it.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExistingEntityBuilder implements EntityBuilder {
	private static final long serialVersionUID = -1422927537304173188L;
	private final ReferenceUniqueAttributeCheck referenceUniqueAttributeCheck = new ReferenceUniqueAttributeCheck();

	/**
	 * This predicate filters out invalid hierarchy placements.
	 */
	@Getter private final HierarchicalContractSerializablePredicate hierarchicalPlacementPredicate;
	/**
	 * This predicate filters out attributes that were not fetched in query.
	 */
	@Getter private final AttributeValueSerializablePredicate attributePredicate;
	/**
	 * This predicate filters out associated data that were not fetched in query.
	 */
	@Getter private final AssociatedDataValueSerializablePredicate associatedDataPredicate;
	/**
	 * This predicate filters out references that were not fetched in query.
	 */
	@Getter private final ReferenceContractSerializablePredicate referencePredicate;
	/**
	 * This predicate filters out prices that were not fetched in query.
	 */
	@Getter private final PriceContractSerializablePredicate pricePredicate;

	private final Entity baseEntity;
	@Delegate(types = AttributesContract.class)
	private final AttributesBuilder attributesBuilder;
	@Delegate(types = AssociatedDataContract.class)
	private final AssociatedDataBuilder associatedDataBuilder;
	@Delegate(types = PricesContract.class)
	private final PricesBuilder pricesBuilder;
	private final Map<EntityReferenceContract<?>, List<ReferenceMutation<?>>> referenceMutations;
	private final Set<EntityReferenceContract<?>> removedReferences = new HashSet<>();
	private LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract> hierarchyMutation;

	public ExistingEntityBuilder(EntityDecorator baseEntity) {
		this.baseEntity = baseEntity.getDelegate();
		this.attributesBuilder = new ExistingAttributesBuilder(this.baseEntity.attributes, baseEntity.getAttributePredicate());
		this.associatedDataBuilder = new ExistingAssociatedDataBuilder(this.baseEntity.associatedData, baseEntity.getAssociatedDataPredicate());
		this.pricesBuilder = new ExistingPricesBuilder(this.baseEntity.prices, baseEntity.getPricePredicate());
		this.referenceMutations = new HashMap<>();
		this.hierarchicalPlacementPredicate = baseEntity.getHierarchicalPlacementPredicate();
		this.attributePredicate = baseEntity.getAttributePredicate();
		this.associatedDataPredicate = baseEntity.getAssociatedDataPredicate();
		this.pricePredicate = baseEntity.getPricePredicate();
		this.referencePredicate = baseEntity.getReferencePredicate();
	}

	public ExistingEntityBuilder(Entity baseEntity) {
		this.baseEntity = baseEntity;
		this.attributesBuilder = new ExistingAttributesBuilder(this.baseEntity.attributes, ExistsPredicate.instance());
		this.associatedDataBuilder = new ExistingAssociatedDataBuilder(this.baseEntity.associatedData, ExistsPredicate.instance());
		this.pricesBuilder = new ExistingPricesBuilder(this.baseEntity.prices, new PriceContractSerializablePredicate());
		this.referenceMutations = new HashMap<>();
		this.hierarchicalPlacementPredicate = HierarchicalContractSerializablePredicate.DEFAULT_INSTANCE;
		this.attributePredicate = AttributeValueSerializablePredicate.DEFAULT_INSTANCE;
		this.associatedDataPredicate = AssociatedDataValueSerializablePredicate.DEFAULT_INSTANCE;
		this.pricePredicate = PriceContractSerializablePredicate.DEFAULT_INSTANCE;
		this.referencePredicate = ReferenceContractSerializablePredicate.DEFAULT_INSTANCE;
	}

	@Override
	public boolean isDropped() {
		return false;
	}

	@Override
	public int getVersion() {
		return baseEntity.getVersion() + 1;
	}

	@Override
	@Nonnull
	public Serializable getType() {
		return baseEntity.getType();
	}

	@Override
	@Nonnull
	public EntitySchema getSchema() {
		return baseEntity.getSchema();
	}

	@Override
	@Nullable
	public Integer getPrimaryKey() {
		return baseEntity.getPrimaryKey();
	}

	@Override
	public HierarchicalPlacementContract getHierarchicalPlacement() {
		final HierarchicalPlacementContract placement = ofNullable(baseEntity.getHierarchicalPlacement())
			.map(it -> ofNullable(hierarchyMutation).map(hp -> hp.mutateLocal(it)).filter(mutatedIt -> mutatedIt.differsFrom(it)).orElse(it))
			.orElseGet(() -> ofNullable(hierarchyMutation).map(hp -> hp.mutateLocal(null)).orElse(null));
		return ofNullable(placement)
			.filter(hierarchicalPlacementPredicate)
			.orElse(null);
	}

	@Override
	public @Nonnull
	Collection<ReferenceContract> getReferences() {
		return Stream.concat(
				baseEntity.getReferences()
					.stream()
					.filter(Droppable::exists)
					.map(it ->
						ofNullable(this.referenceMutations.get(it.getReferencedEntity()))
							.map(mutations -> evaluateReferenceMutations(it, mutations))
							.filter(mutatedReference -> mutatedReference.differsFrom(it))
							.orElse(it)
					),
				this.referenceMutations
					.entrySet()
					.stream()
					.filter(it -> baseEntity.getReference(it.getKey().getType(), it.getKey().getPrimaryKey()) == null)
					.map(Entry::getValue)
					.map(it -> evaluateReferenceMutations(null, it))
			)
			.filter(referencePredicate)
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences(Serializable referencedEntityType) {
		return getReferences()
			.stream()
			.filter(it -> Objects.equals(referencedEntityType, it.getReferencedEntity().getType()))
			.collect(Collectors.toList());
	}

	@Override
	public @Nullable
	ReferenceContract getReference(Serializable referencedEntityType, int referencedEntityId) {
		final EntityReferenceContract<?> entityReferenceContract = new EntityReference(referencedEntityType, referencedEntityId);
		final ReferenceContract reference = ofNullable(baseEntity.getReference(referencedEntityType, referencedEntityId))
			.map(it -> ofNullable(this.referenceMutations.get(entityReferenceContract))
				.map(mutations -> evaluateReferenceMutations(it, mutations))
				.orElse(it)
			)
			.orElseGet(() ->
				ofNullable(this.referenceMutations.get(entityReferenceContract))
					.map(mutations -> evaluateReferenceMutations(null, mutations))
					.orElse(null)
			);
		return ofNullable(reference)
			.filter(referencePredicate)
			.orElse(null);
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
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(new AttributeKey(attributeName), -1)),
			"Attribute " + attributeName + " was not fetched and cannot be removed. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.removeAttribute(attributeName);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull T attributeValue) {
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(new AttributeKey(attributeName), -1)),
			"Attributes were not fetched and cannot be updated. Please enrich the entity first or load it with attributes. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue) {
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(new AttributeKey(attributeName), -1)),
			"Attributes were not fetched and cannot be updated. Please enrich the entity first or load it with attributes. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(new AttributeKey(attributeName, locale), -1)),
			"Attribute " + attributeName + " in locale " + locale + " was not fetched and cannot be removed. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.removeAttribute(attributeName, locale);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T attributeValue) {
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(new AttributeKey(attributeName, locale), -1)),
			"Attributes in locale " + locale + " were not fetched and cannot be updated. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue) {
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(new AttributeKey(attributeName, locale), -1)),
			"Attributes in locale " + locale + " were not fetched and cannot be updated. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.setAttribute(attributeName, locale, attributeValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder mutateAttribute(@Nonnull AttributeMutation mutation) {
		Assert.isTrue(
			attributePredicate.test(new AttributeValue(mutation.getAttributeKey(), -1)),
			"Attribute " + mutation.getAttributeKey() + " was not fetched and cannot be updated. Please enrich the entity first or load it with attributes."
		);
		attributesBuilder.mutateAttribute(mutation);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAssociatedData(@Nonnull String associatedDataName) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(new AssociatedDataKey(associatedDataName), -1)),
			"Associated data " + associatedDataName + " was not fetched and cannot be removed. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.removeAssociatedData(associatedDataName);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T associatedDataValue) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(new AssociatedDataKey(associatedDataName), -1)),
			"Associated data " + associatedDataName + " was not fetched and cannot be updated. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.setAssociatedData(associatedDataName, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T[] associatedDataValue) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(new AssociatedDataKey(associatedDataName), -1)),
			"Associated data " + associatedDataName + " was not fetched and cannot be updated. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.setAssociatedData(associatedDataName, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(new AssociatedDataKey(associatedDataName, locale), -1)),
			"Associated data " + associatedDataName + " was not fetched and cannot be removed. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.removeAssociatedData(associatedDataName, locale);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T associatedDataValue) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(new AssociatedDataKey(associatedDataName, locale), -1)),
			"Associated data " + associatedDataName + " was not fetched and cannot be updated. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.setAssociatedData(associatedDataName, locale, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T[] associatedDataValue) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(new AssociatedDataKey(associatedDataName, locale), -1)),
			"Associated data " + associatedDataName + " was not fetched and cannot be updated. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.setAssociatedData(associatedDataName, locale, associatedDataValue);
		return this;
	}

	@Nonnull
	@Override
	public EntityBuilder mutateAssociatedData(@Nonnull AssociatedDataMutation mutation) {
		Assert.isTrue(
			associatedDataPredicate.test(new AssociatedDataValue(mutation.getAssociatedDataKey(), -1)),
			"Associated data " + mutation.getAssociatedDataKey() + " was not fetched and cannot be updated. Please enrich the entity first or load it with the associated data."
		);
		associatedDataBuilder.mutateAssociatedData(mutation);
		return this;
	}

	@Override
	public EntityBuilder setHierarchicalPlacement(int orderAmongSiblings) {
		this.hierarchyMutation = new SetHierarchicalPlacementMutation(orderAmongSiblings);
		return this;
	}

	@Override
	public EntityBuilder setHierarchicalPlacement(int parentPrimaryKey, int orderAmongSiblings) {
		this.hierarchyMutation = new SetHierarchicalPlacementMutation(parentPrimaryKey, orderAmongSiblings);
		return this;
	}

	@Override
	public EntityBuilder removeHierarchicalPlacement() {
		Assert.notNull(baseEntity.getHierarchicalPlacement(), "Cannot remove hierarchy placement that doesn't exist!");
		this.hierarchyMutation = new RemoveHierarchicalPlacementMutation();
		return this;
	}

	@Override
	public EntityBuilder setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey) {
		return setReference(referencedEntityType, referencedPrimaryKey, null);
	}

	@Override
	public EntityBuilder setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey, @Nullable Consumer<ReferenceBuilder> whichIs) {
		Assert.isTrue(
			referencePredicate.test(new Reference(getSchema(), new EntityReference(referencedEntityType, referencedPrimaryKey), null)),
			"References were not fetched and cannot be updated. Please enrich the entity first or load it with the references."
		);
		final EntityReference referencedEntity = new EntityReference(referencedEntityType, referencedPrimaryKey);
		final EntitySchema schema = getSchema();
		final Optional<ReferenceContract> existingReference = ofNullable(baseEntity.getReference(referencedEntityType, referencedPrimaryKey));
		final ReferenceBuilder referenceBuilder = existingReference
			.map(it -> (ReferenceBuilder) new ExistingReferenceBuilder(it, schema, referenceUniqueAttributeCheck))
			.filter(referencePredicate)
			.orElseGet(() -> new InitialReferenceBuilder(schema, referencedEntity, referenceUniqueAttributeCheck));
		ofNullable(whichIs).ifPresent(it -> it.accept(referenceBuilder));
		if (existingReference.isPresent()) {
			final ReferenceContract referenceInBaseEntity = ofNullable(this.baseEntity.getReference(referencedEntityType, referencedPrimaryKey))
				.filter(Droppable::exists)
				.orElse(null);
			final List<ReferenceMutation<?>> changeSet = referenceBuilder.buildChangeSet().collect(Collectors.toList());
			if (referenceInBaseEntity == null || (referenceInBaseEntity.exists() && !removedReferences.contains(referencedEntity))) {
				this.referenceMutations.put(
					referencedEntity,
					changeSet
				);
			} else {
				this.referenceMutations.put(
					referencedEntity,
					Stream.concat(
							Stream.concat(
								ofNullable(referenceInBaseEntity.getGroup())
									.stream()
									.filter(Droppable::exists)
									.map(it ->
										new RemoveReferenceGroupMutation(
											referencedEntity, getSchema()
										)
									),
								referenceInBaseEntity
									.getAttributeValues()
									.stream()
									.filter(Droppable::exists)
									.map(it ->
										new ReferenceAttributesUpdateMutation(
											referencedEntity, getSchema(),
											new RemoveAttributeMutation(it.getKey())
										)
									)
							),
							changeSet.stream()
						)
						.collect(Collectors.toList())
				);
			}
		} else {
			this.referenceMutations.put(
				referencedEntity,
				new InsertReferenceMutation(referenceBuilder.build(), schema)
					.generateMutations()
					.collect(Collectors.toList())
			);
		}
		return this;
	}

	@Override
	public EntityBuilder removeReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey) {
		Assert.isTrue(
			referencePredicate.test(new Reference(getSchema(), new EntityReference(referencedEntityType, referencedPrimaryKey), null)),
			"References were not fetched and cannot be updated. Please enrich the entity first or load it with the references."
		);
		final EntityReferenceContract<?> referencedEntity = new EntityReference(referencedEntityType, referencedPrimaryKey);
		Assert.isTrue(getReference(referencedEntityType, referencedPrimaryKey) != null, "There's no reference of type " + referencedEntityType + " and primary key " + referencedPrimaryKey + "!");
		final ReferenceContract existingReference = ofNullable(baseEntity.getReference(referencedEntityType, referencedPrimaryKey))
			.filter(referencePredicate)
			.orElseThrow(() -> new IllegalArgumentException(
					"Reference to " + referencedEntityType + " and primary key " + referencedEntity +
						" is not present on the entity " + baseEntity.getType() + " and id " +
						baseEntity.getPrimaryKey() + "!"
				)
			);
		this.referenceMutations.put(
			referencedEntity,
			Collections.singletonList(
				new RemoveReferenceMutation(
					existingReference.getReferencedEntity(),
					getSchema()
				)
			)
		);
		this.removedReferences.add(referencedEntity);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		Assert.isTrue(
			pricePredicate.test(new Price(new PriceKey(priceId, priceList, currency), 1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, false)),
			"Price " + priceId + ", " + priceList + ", " + currency + " was not fetched and cannot be updated. Please enrich the entity first or load it with the prices."
		);
		pricesBuilder.setPrice(priceId, priceList, currency, priceWithoutVat, vat, priceWithVat, sellable);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		Assert.isTrue(
			pricePredicate.test(new Price(new PriceKey(priceId, priceList, currency), 1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, false)),
			"Price " + priceId + ", " + priceList + ", " + currency + " was not fetched and cannot be updated. Please enrich the entity first or load it with the prices."
		);
		pricesBuilder.setPrice(priceId, priceList, currency, innerRecordId, priceWithoutVat, vat, priceWithVat, sellable);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		Assert.isTrue(
			pricePredicate.test(new Price(new PriceKey(priceId, priceList, currency), 1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, false)),
			"Price " + priceId + ", " + priceList + ", " + currency + " was not fetched and cannot be updated. Please enrich the entity first or load it with the prices."
		);
		pricesBuilder.setPrice(priceId, priceList, currency, priceWithoutVat, vat, priceWithVat, validity, sellable);
		return this;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		Assert.isTrue(
			pricePredicate.test(new Price(new PriceKey(priceId, priceList, currency), 1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, false)),
			"Price " + priceId + ", " + priceList + ", " + currency + " was not fetched and cannot be updated. Please enrich the entity first or load it with the prices."
		);
		pricesBuilder.setPrice(priceId, priceList, currency, innerRecordId, priceWithoutVat, vat, priceWithVat, validity, sellable);
		return this;
	}

	@Override
	public EntityBuilder removePrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		Assert.isTrue(
			pricePredicate.test(new Price(new PriceKey(priceId, priceList, currency), 1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, false)),
			"Price " + priceId + ", " + priceList + ", " + currency + " was not fetched and cannot be updated. Please enrich the entity first or load it with the prices."
		);
		pricesBuilder.removePrice(priceId, priceList, currency);
		return this;
	}

	@Override
	public EntityBuilder setPriceInnerRecordHandling(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		assertPricesFetched(pricePredicate);
		pricesBuilder.setPriceInnerRecordHandling(priceInnerRecordHandling);
		return this;
	}

	@Override
	public EntityBuilder removePriceInnerRecordHandling() {
		assertPricesFetched(pricePredicate);
		pricesBuilder.removePriceInnerRecordHandling();
		return this;
	}

	@Override
	public EntityBuilder removeAllNonTouchedPrices() {
		assertPricesFetched(pricePredicate);
		pricesBuilder.removeAllNonTouchedPrices();
		return this;
	}

	@Override
	public EntityMutation toMutation() {
		return new ExistingEntityMutation(
			baseEntity,
			Stream.of(
					Stream.of(hierarchyMutation),
					attributesBuilder.buildChangeSet(),
					associatedDataBuilder.buildChangeSet(),
					pricesBuilder.buildChangeSet(),
					referenceMutations.values().stream().flatMap(Collection::stream)
				)
				.flatMap(it -> it)
				.filter(Objects::nonNull)
				.sorted()
				.collect(Collectors.toList())
		);
	}

	@Override
	public EntityMutation toRemovalMutation() {
		return new ExistingEntityRemovalMutation(
			baseEntity
		);
	}

	@Override
	public Entity toInstance() {
		return toMutation().mutate(baseEntity);
	}

	private static void assertPricesFetched(PriceContractSerializablePredicate pricePredicate) {
		Assert.isTrue(
			pricePredicate.getPriceFetchMode() == PriceFetchMode.ALL,
			"Prices were not fetched and cannot be updated. Please enrich the entity first or load it with all the prices."
		);
	}

	private ReferenceContract evaluateReferenceMutations(ReferenceContract reference, List<ReferenceMutation<?>> mutations) {
		ReferenceContract mutatedReference = reference;
		for (ReferenceMutation<?> mutation : mutations) {
			mutatedReference = mutation.mutateLocal(mutatedReference);
		}
		return mutatedReference.differsFrom(reference) ? mutatedReference : reference;
	}

	@RequiredArgsConstructor
	private abstract static class AbstractExistingEntityMutation implements EntityMutation {
		private static final long serialVersionUID = 3711045986466019572L;

		@Getter private final Collection<? extends LocalMutation<?, ?>> localMutations;

		void applyPriceChange(EntityDirtyContext dirtyContext, Map<PriceKey, PriceContract> prices, PriceMutation mutation) {
			final PriceContract existingValue = prices.get(mutation.getPriceKey());
			final PriceContract mutatedValue = mutation.mutateLocal(existingValue);
			if (ofNullable(existingValue).map(it -> it.differsFrom(mutatedValue)).orElse(true)) {
				prices.put(mutatedValue.getPriceKey(), mutatedValue);
				dirtyContext.setPricesDirty();
			}
		}

		void applyReferenceChange(EntityDirtyContext dirtyContext, Map<EntityReferenceContract<?>, ReferenceContract> references, Set<Locale> locales, ReferenceMutation<?> mutation) {
			final ReferenceContract existingValue = references.get(mutation.getReferenceKey());
			final ReferenceContract mutatedValue = mutation.mutateLocal(existingValue);
			if (ofNullable(existingValue).map(it -> it.differsFrom(mutatedValue)).orElse(true)) {
				if (mutation instanceof ReferenceAttributesUpdateMutation) {
					ofNullable(((ReferenceAttributesUpdateMutation) mutation).getAttributeKey().getLocale())
						.ifPresent(locales::add);
				}
				references.put(mutatedValue.getReferencedEntity(), mutatedValue);
				dirtyContext.setReferencesDirty();
			}
		}

		void applyAttributeChange(EntityDirtyContext dirtyContext, Map<AttributeKey, AttributeValue> attributes, Set<Locale> locales, AttributeMutation mutation) {
			final AttributeValue existingValue = attributes.get(mutation.getAttributeKey());
			final AttributeValue mutatedValue = mutation.mutateLocal(existingValue);
			if (ofNullable(existingValue).map(it -> it.differsFrom(mutatedValue)).orElse(true)) {
				ofNullable(mutation.getAttributeKey().getLocale()).ifPresent(locales::add);
				attributes.put(mutatedValue.getKey(), mutatedValue);
				dirtyContext.setAttributesDirty();
			}
		}

		void applyAssociatedDataChange(EntityDirtyContext dirtyContext, Set<AssociatedDataKey> associatedDataKeys, Map<AssociatedDataKey, AssociatedDataValue> associatedDataValues, Set<Locale> locales, AssociatedDataMutation mutation) {
			final AssociatedDataValue existingValue = associatedDataValues.get(mutation.getAssociatedDataKey());
			final AssociatedDataValue mutatedValue = mutation.mutateLocal(existingValue);
			if (ofNullable(existingValue).map(it -> it.differsFrom(mutatedValue)).orElse(true)) {
				ofNullable(mutation.getAssociatedDataKey().getLocale()).ifPresent(locales::add);
				associatedDataKeys.add(mutatedValue.getKey());
				associatedDataValues.put(mutatedValue.getKey(), mutatedValue);
				dirtyContext.setAssociatedDataDirty();
			}
		}

		Prices applyPriceHandlingChange(Entity entity, EntityDirtyContext dirtyContext, LocalMutation<Prices, PriceInnerRecordHandling> priceHandlingMutation) {
			final Prices mutatedValue = priceHandlingMutation.mutateLocal(entity.prices);
			if (entity.getPriceInnerRecordHandling() != mutatedValue.getPriceInnerRecordHandling()) {
				dirtyContext.setPricesDirty();
			}
			return mutatedValue;
		}

		HierarchicalPlacementContract applyHierarchyHandlingChange(Entity entity, EntityDirtyContext dirtyContext, LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract> hierarchicalPlacementMutation) {
			final HierarchicalPlacementContract mutatedValue = hierarchicalPlacementMutation.mutateLocal(entity.getHierarchicalPlacement());
			if (ofNullable(entity.getHierarchicalPlacement()).map(it -> it.differsFrom(mutatedValue)).orElse(true)) {
				dirtyContext.setHierarchyPlacementDirty();
			}
			return mutatedValue;
		}

		protected static class EntityDirtyContext {
			@Getter private boolean hierarchyPlacementDirty;
			@Getter private boolean attributesDirty;
			@Getter private boolean associatedDataDirty;
			@Getter private boolean pricesDirty;
			@Getter private boolean referencesDirty;

			public boolean isDirty() {
				return hierarchyPlacementDirty ||
					attributesDirty ||
					associatedDataDirty ||
					pricesDirty ||
					referencesDirty;
			}

			public void setHierarchyPlacementDirty() {
				this.hierarchyPlacementDirty = true;
			}

			public void setAttributesDirty() {
				this.attributesDirty = true;
			}

			public void setAssociatedDataDirty() {
				this.associatedDataDirty = true;
			}

			public void setPricesDirty() {
				this.pricesDirty = true;
			}

			public void setReferencesDirty() {
				this.referencesDirty = true;
			}
		}
	}

	public static class ExistingEntityMutation extends AbstractExistingEntityMutation {
		private static final long serialVersionUID = -2012245398443357781L;

		@Getter private final Serializable entityType;
		@Getter private final Integer entityPrimaryKey;

		public ExistingEntityMutation(Entity baseEntity, Collection<? extends LocalMutation<?, ?>> localMutations) {
			super(localMutations);
			this.entityType = baseEntity.getType();
			this.entityPrimaryKey = baseEntity.getPrimaryKey();
		}

		public ExistingEntityMutation(Entity baseEntity, LocalMutation<?, ?>... localMutations) {
			super(Arrays.asList(localMutations));
			this.entityType = baseEntity.getType();
			this.entityPrimaryKey = baseEntity.getPrimaryKey();
		}

		@Override
		public void setEntityPrimaryKey(@Nonnull Integer primaryKey) {
			// SHOULD NOT EVER HAPPEN
			throw new IllegalStateException("Id is already generated for existing entity!");
		}

		@Nonnull
		@Override
		public EntityExistence expects() {
			return EntityExistence.MUST_EXIST;
		}

		@Nonnull
		@Override
		public Entity mutate(@Nullable Entity entity) {
			Assert.notNull(entity, "This mutation requires existing entity to be altered!");

			final EntityDirtyContext dirtyContext = new EntityDirtyContext();
			Prices prices = entity.prices;
			HierarchicalPlacementContract hierarchicalPlacement = entity.getHierarchicalPlacement();
			final Map<PriceKey, PriceContract> priceIndex = new HashMap<>(entity.prices.getPriceIndex());
			final Map<EntityReferenceContract<?>, ReferenceContract> referenceIndex = new HashMap<>(entity.references);
			final Map<AttributeKey, AttributeValue> attributes = new HashMap<>(entity.attributes.attributeValues);
			final Set<AssociatedDataKey> associatedDataKeys = new HashSet<>(entity.associatedData.getAssociatedDataKeys());
			final Map<AssociatedDataKey, AssociatedDataValue> associatedDataValues = new HashMap<>(entity.associatedData.associatedDataValues);
			final Set<Locale> locales = new HashSet<>(entity.getLocales());

			for (LocalMutation<?, ?> mutation : getLocalMutations()) {
				final Class<?> affectsProperty = mutation.affects();
				if (affectsProperty.equals(Prices.class)) {
					//noinspection unchecked
					prices = applyPriceHandlingChange(entity, dirtyContext, (LocalMutation<Prices, PriceInnerRecordHandling>) mutation);
				} else if (affectsProperty.equals(PriceContract.class)) {
					applyPriceChange(dirtyContext, priceIndex, (PriceMutation) mutation);
				} else if (affectsProperty.equals(HierarchicalPlacementContract.class)) {
					//noinspection unchecked
					hierarchicalPlacement = applyHierarchyHandlingChange(entity, dirtyContext, (LocalMutation<HierarchicalPlacementContract, HierarchicalPlacementContract>) mutation);
				} else if (affectsProperty.equals(ReferenceContract.class)) {
					applyReferenceChange(dirtyContext, referenceIndex, locales, (ReferenceMutation<?>) mutation);
				} else if (affectsProperty.equals(AttributeValue.class)) {
					applyAttributeChange(dirtyContext, attributes, locales, (AttributeMutation) mutation);
				} else if (affectsProperty.equals(AssociatedDataValue.class)) {
					applyAssociatedDataChange(dirtyContext, associatedDataKeys, associatedDataValues, locales, (AssociatedDataMutation) mutation);
				} else {
					// SHOULD NOT EVER HAPPEN
					throw new IllegalStateException("Unknown mutation: " + mutation.getClass());
				}
			}

			if (dirtyContext.isDirty()) {
				return new Entity(
					entity.getVersion() + 1,
					entity.getSchema(),
					entity.getPrimaryKey(),
					hierarchicalPlacement,
					dirtyContext.isReferencesDirty() ?
						referenceIndex.values() : entity.getReferences(),
					dirtyContext.isAttributesDirty() ?
						new Attributes(
							entity.schema,
							attributes.values(),
							attributes
								.keySet()
								.stream()
								.map(AttributeKey::getLocale)
								.filter(Objects::nonNull)
								.collect(Collectors.toSet()), entity.attributes.attributeTypes
						) : entity.attributes,
					dirtyContext.isAssociatedDataDirty() ?
						new AssociatedData(
							entity.schema,
							entity.associatedData.reflectionLookup,
							associatedDataKeys,
							associatedDataValues.values()
						) : entity.associatedData,
					dirtyContext.isPricesDirty() ?
						new Prices(entity.prices.version + 1, priceIndex.values(), prices.getPriceInnerRecordHandling()) :
						entity.prices,
					locales,
					false
				);
			} else {
				// entity has not changed at all!
				return entity;
			}
		}
	}

	public static class ExistingEntityRemovalMutation extends AbstractExistingEntityMutation {
		private static final long serialVersionUID = 5707497055545283888L;

		private final Entity entity;

		public ExistingEntityRemovalMutation(Entity entity) {
			super(
				Stream.of(
						entity.references.values()
							.stream()
							.flatMap(it -> new RemoveReferenceMutation(it.getReferencedEntity(), entity.getSchema()).generateMutations(it)),
						entity.attributes
							.getAttributeKeys()
							.stream()
							.map(RemoveAttributeMutation::new),
						entity.associatedData
							.getAssociatedDataKeys()
							.stream()
							.map(RemoveAssociatedDataMutation::new),
						entity.prices
							.getPrices()
							.stream()
							.map(it -> new RemovePriceMutation(it.getPriceKey()))
					)
					.flatMap(it -> it)
					.filter(Objects::nonNull)
					.collect(Collectors.toList())
			);
			this.entity = entity;
		}

		@Nonnull
		@Override
		public Serializable getEntityType() {
			return entity.getType();
		}

		@Nullable
		@Override
		public Integer getEntityPrimaryKey() {
			return entity.getPrimaryKey();
		}

		@Override
		public void setEntityPrimaryKey(@Nonnull Integer primaryKey) {
			throw new IllegalArgumentException("Updating primary key when entity is being removed is not possible!");
		}

		@Nonnull
		@Override
		public EntityExistence expects() {
			return EntityExistence.MAY_EXIST;
		}

		@Nonnull
		@Override
		public Entity mutate(@Nullable Entity entity) {
			Assert.notNull(entity, "Entity must not be null in order to be removed!");
			if (entity.isDropped()) {
				return entity;
			}

			final EntityDirtyContext dirtyContext = new EntityDirtyContext();
			final Map<PriceKey, PriceContract> priceIndex = new HashMap<>(entity.prices.getPriceIndex());
			final Map<EntityReferenceContract<?>, ReferenceContract> referenceIndex = new HashMap<>(entity.references);
			final Map<AttributeKey, AttributeValue> attributes = new HashMap<>(entity.attributes.attributeValues);
			final Set<AssociatedDataKey> associatedDataKeys = new HashSet<>(entity.associatedData.getAssociatedDataKeys());
			final Map<AssociatedDataKey, AssociatedDataValue> associatedDataValues = new HashMap<>(entity.associatedData.associatedDataValues);
			final Set<Locale> locales = new HashSet<>(entity.getLocales());

			for (LocalMutation<?, ?> mutation : getLocalMutations()) {
				final Class<?> affectsProperty = mutation.affects();
				if (affectsProperty.equals(PriceContract.class)) {
					applyPriceChange(dirtyContext, priceIndex, (PriceMutation) mutation);
				} else if (affectsProperty.equals(ReferenceContract.class)) {
					applyReferenceChange(dirtyContext, referenceIndex, locales, (ReferenceMutation<?>) mutation);
				} else if (affectsProperty.equals(AttributeValue.class)) {
					applyAttributeChange(dirtyContext, attributes, locales, (AttributeMutation) mutation);
				} else if (affectsProperty.equals(AssociatedDataValue.class)) {
					applyAssociatedDataChange(dirtyContext, associatedDataKeys, associatedDataValues, locales, (AssociatedDataMutation) mutation);
				} else {
					// SHOULD NOT EVER HAPPEN
					throw new IllegalStateException("Unknown mutation: " + mutation.getClass());
				}
			}

			return new Entity(
				entity.getVersion() + 1,
				entity.getSchema(),
				entity.getPrimaryKey(),
				entity.getHierarchicalPlacement(),
				dirtyContext.isReferencesDirty() ?
					referenceIndex.values() : entity.getReferences(),
				dirtyContext.isAttributesDirty() ?
					new Attributes(
						entity.schema,
						attributes.values(),
						attributes
							.keySet()
							.stream()
							.map(AttributeKey::getLocale)
							.filter(Objects::nonNull)
							.collect(Collectors.toSet()), entity.attributes.attributeTypes
					) : entity.attributes,
				dirtyContext.isAssociatedDataDirty() ?
					new AssociatedData(
						entity.schema,
						entity.associatedData.reflectionLookup,
						associatedDataKeys,
						associatedDataValues.values()
					) : entity.associatedData,
				dirtyContext.isPricesDirty() ?
					new Prices(entity.prices.version + 1, priceIndex.values(), entity.prices.getPriceInnerRecordHandling()) :
					entity.prices,
				locales,
				true
			);
		}
	}

	private class ReferenceUniqueAttributeCheck implements BiPredicate<Serializable, String>, Serializable {
		private static final long serialVersionUID = 3153392405996708805L;

		@Override
		public boolean test(Serializable entityType, String attributeName) {
			return getReferences()
				.stream()
				.filter(it -> Objects.equals(entityType, it.getReferencedEntity().getType()))
				.anyMatch(it -> ofNullable(it.getAttributeValue(attributeName)).map(Droppable::exists).orElse(false));
		}

	}

}
