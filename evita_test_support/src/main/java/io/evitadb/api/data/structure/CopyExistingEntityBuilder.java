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
import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.structure.ExistingEntityBuilder.ExistingEntityRemovalMutation;
import io.evitadb.api.data.structure.InitialEntityBuilder.InitEntityMutation;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * This implementation is used only when existing Evita entity from different database is required to be inserted into
 * another Evita database.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class CopyExistingEntityBuilder implements EntityBuilder {
	private static final UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION = new UnsupportedOperationException("This builder is read-only!");
	private static final long serialVersionUID = 2726056906368377183L;

	private final Entity externalEntity;
	@Getter @Setter private Integer overriddenPrimaryKey;
	@Delegate(types = AttributesContract.class)
	private final AttributesBuilder attributesBuilder;
	@Delegate(types = AssociatedDataContract.class)
	private final AssociatedDataBuilder associatedDataBuilder;
	@Delegate(types = PricesContract.class)
	private final PricesBuilder pricesBuilder;

	public CopyExistingEntityBuilder(Entity externalEntity, Integer overriddenPrimaryKey) {
		this(externalEntity);
		this.overriddenPrimaryKey = overriddenPrimaryKey;
	}

	public CopyExistingEntityBuilder(Entity externalEntity) {
		this.externalEntity = externalEntity;
		this.attributesBuilder = new ExistingAttributesBuilder(externalEntity.attributes);
		this.associatedDataBuilder = new ExistingAssociatedDataBuilder(externalEntity.associatedData);
		this.pricesBuilder = new ExistingPricesBuilder(externalEntity.prices);
	}

	@Nonnull
	@Override
	public EntityBuilder removeAssociatedData(@Nonnull String associatedDataName) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T associatedDataValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull T[] associatedDataValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T associatedDataValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable> EntityBuilder setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T[] associatedDataValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public EntityBuilder mutateAssociatedData(@Nonnull AssociatedDataMutation mutation) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAttribute(@Nonnull String attributeName) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull T attributeValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public EntityBuilder removeAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T attributeValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public <T extends Serializable & Comparable<?>> EntityBuilder setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public EntityBuilder mutateAttribute(@Nonnull AttributeMutation mutation) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public boolean isDropped() {
		return false;
	}

	@Nonnull
	@Override
	public Serializable getType() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public EntitySchema getSchema() {
		return externalEntity.getSchema();
	}

	@Nullable
	@Override
	public Integer getPrimaryKey() {
		return ofNullable(overriddenPrimaryKey).orElse(externalEntity.getPrimaryKey());
	}

	@Nullable
	@Override
	public HierarchicalPlacementContract getHierarchicalPlacement() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences(Serializable referencedEntityType) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nullable
	@Override
	public ReferenceContract getReference(Serializable referencedEntityType, int referencedEntityId) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Nonnull
	@Override
	public Set<Locale> getLocales() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setHierarchicalPlacement(int orderAmongSiblings) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setHierarchicalPlacement(int parentPrimaryKey, int orderAmongSiblings) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder removeHierarchicalPlacement() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey, @Nullable Consumer<ReferenceBuilder> whichIs) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder removeReference(@Nonnull Serializable referencedEntityType, int referencedPrimaryKey) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, boolean sellable) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, DateTimeRange validity, boolean sellable) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency, @Nullable Integer innerRecordId, @Nonnull BigDecimal priceWithoutVat, @Nonnull BigDecimal vat, @Nonnull BigDecimal priceWithVat, @Nullable DateTimeRange validity, boolean sellable) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder removePrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder setPriceInnerRecordHandling(@Nonnull PriceInnerRecordHandling priceInnerRecordHandling) {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder removePriceInnerRecordHandling() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public EntityBuilder removeAllNonTouchedPrices() {
		throw UNSUPPORTED_OPERATION_EXCEPTION;
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@Override
	public EntityMutation toMutation() {
		Assert.notNull(externalEntity.associatedData, "Associated data must be fetched in order to copy the entity!");
		return new InitEntityMutation(
			externalEntity.schema,
			ofNullable(overriddenPrimaryKey).orElse(externalEntity.primaryKey),
			externalEntity.type,
			externalEntity.hierarchicalPlacement,
			new Attributes(
				externalEntity.schema,
				externalEntity.attributes
					.getAttributeValues()
					.stream()
					.map(it -> {
						final AttributeSchema attributeSchema = externalEntity.schema.getAttribute(it.getKey().getAttributeName());
						if (attributeSchema.isUnique()) {
							Assert.isTrue(
								String.class.isAssignableFrom(attributeSchema.getType()),
								"Currently, only String unique attributes can be altered!"
							);
							return new AttributeValue(it.getKey(), ofNullable(overriddenPrimaryKey).map(i->it.getValue() + "_" + i).orElse(String.valueOf(it.getValue())) );
						} else {
							return it;
						}
					})
					.collect(Collectors.toList()),
				externalEntity.attributes.getAttributeLocales(),
				externalEntity.attributes.attributeTypes
			),
			externalEntity.associatedData,
			externalEntity.prices,
			externalEntity.references.values(),
			externalEntity.locales
		);
	}

	@Override
	public EntityMutation toRemovalMutation() {
		return new ExistingEntityRemovalMutation(externalEntity);
	}

	@Override
	public Entity toInstance() {
		return externalEntity;
	}
}
