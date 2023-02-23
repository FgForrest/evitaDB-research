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

package io.evitadb.api.data;

import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.MemoryMeasuringConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Contract for classes that allow reading information about {@link Entity} instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface EntityContract extends ContentComparator<EntityContract>, AttributesContract, AssociatedDataContract, PricesContract, Versioned, Droppable, Serializable {

	/**
	 * Returns type of the entity. This match {@link EntitySchema#getName()}.
	 */
	@Nonnull
	Serializable getType();

	/**
	 * Returns schema of the entity, that fully describes its structure and capabilities. Schema is up-to-date to the
	 * moment entity was fetched from Evita DB.
	 */
	@Nonnull
	EntitySchema getSchema();

	/**
	 * Returns primary key of the entity that is UNIQUE among all other entities of the same type.
	 * Primary key may be null only when entity is created in case Evita DB is responsible for automatically assigning
	 * new primary key. Once entity is stored into Evita DB it MUST have non-null primary key.
	 */
	@Nullable
	Integer getPrimaryKey();

	/**
	 * Returns hierarchy information about the entity. Hierarchy information allows to compose hierarchy tree composed
	 * of entities of the same type. Referenced entity is always entity of the same type. Referenced entity must be
	 * already present in the Evita DB and must also have hierarchy placement set. Root `parentPrimaryKey` (i.e. parent
	 * for top-level hierarchical placements) is null.
	 */
	@Nullable
	HierarchicalPlacementContract getHierarchicalPlacement();

	/**
	 * Returns collection of {@link Reference} of this entity. References represent relations to other Evita DB entities or
	 * external entities in different systems.
	 */
	@Nonnull
	Collection<ReferenceContract> getReferences();

	/**
	 * Returns collection of {@link Reference} to certain type of other entities. References represent relations to
	 * other Evita DB entities or external entities in different systems.
	 */
	@Nonnull
	Collection<ReferenceContract> getReferences(Serializable referencedEntityType);

	/**
	 * Returns single {@link Reference} instance that is referencing passed entity type with certain primary key. References
	 * represent relations to other Evita DB entities or external entities in different systems.
	 */
	@Nullable
	ReferenceContract getReference(Serializable referencedEntityType, int referencedEntityId);

	/**
	 * Returns set of locales this entity has any of localized data in. Although {@link EntitySchema#getLocales()} may
	 * support wider range of the locales, this method returns only those that are used by data of this very entity
	 * instance.
	 */
	@Nonnull
	Set<Locale> getLocales();

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	default int estimateSize() {
		return MemoryMeasuringConstants.OBJECT_HEADER_SIZE +
			// primary key
			MemoryMeasuringConstants.INT_SIZE +
			// version
			MemoryMeasuringConstants.INT_SIZE +
			// reference to the schema
			MemoryMeasuringConstants.REFERENCE_SIZE +
			// dropped
			MemoryMeasuringConstants.BYTE_SIZE +
			// type - we should assume the key is stored in memory only once (should be enum or String)
			MemoryMeasuringConstants.REFERENCE_SIZE +
			// hierarchical placement
			ofNullable(getHierarchicalPlacement()).map(HierarchicalPlacementContract::estimateSize).orElse(0) +
			// locales
			getLocales().stream().mapToInt(it -> MemoryMeasuringConstants.REFERENCE_SIZE).sum() +
			// attributes
			getAttributeValues().stream().mapToInt(AttributeValue::estimateSize).sum() +
			// attributes
			getAssociatedDataValues().stream().mapToInt(AssociatedDataValue::estimateSize).sum() +
			// price inner record handling
			MemoryMeasuringConstants.BYTE_SIZE +
			// prices
			getPrices().stream().mapToInt(PriceContract::estimateSize).sum() +
			// references
			getReferences().stream().mapToInt(ReferenceContract::estimateSize).sum();

	}

	/**
	 * Method returns true if any entity inner data differs from other entity.
	 */
	@Override
	default boolean differsFrom(@Nullable EntityContract otherEntity) {
		if (this == otherEntity) return false;
		if (otherEntity == null) return true;

		if (!Objects.equals(getPrimaryKey(), otherEntity.getPrimaryKey())) return true;
		if (getVersion() != otherEntity.getVersion()) return true;
		if (isDropped() != otherEntity.isDropped()) return true;
		if (!getType().equals(otherEntity.getType())) return true;
		if (getHierarchicalPlacement() != null ? otherEntity.getHierarchicalPlacement() == null || getHierarchicalPlacement().differsFrom(otherEntity.getHierarchicalPlacement()) : otherEntity.getHierarchicalPlacement() != null)
			return true;
		if (AttributesContract.anyAttributeDifferBetween(this, otherEntity)) return true;
		if (AssociatedDataContract.anyAssociatedDataDifferBetween(this, otherEntity)) return true;
		if (getPriceInnerRecordHandling() != otherEntity.getPriceInnerRecordHandling()) return true;
		if (PricesContract.anyPriceDifferBetween(this, otherEntity)) return true;
		if (!getLocales().equals(otherEntity.getLocales())) return true;

		final Collection<ReferenceContract> thisReferences = getReferences();
		final Collection<ReferenceContract> otherReferences = otherEntity.getReferences();
		if (thisReferences.size() != otherReferences.size()) return true;
		for (ReferenceContract thisReference : thisReferences) {
			final EntityReferenceContract<?> thisKey = thisReference.getReferencedEntity();
			final ReferenceContract otherReference = otherEntity.getReference(thisKey.getType(), thisKey.getPrimaryKey());
			if (otherReference == null || thisReference.differsFrom(otherReference)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method prints details about entity as single line. Use in {@link Object#toString()} implementations.
	 */
	default String describe() {
		final Collection<ReferenceContract> references = getReferences();
		final Collection<AttributeValue> attributeValues = getAttributeValues();
		final Collection<AssociatedDataValue> associatedDataValues = getAssociatedDataValues();
		final Collection<PriceContract> prices = getPrices();
		final Set<Locale> locales = getLocales();
		return (isDropped() ? "❌" : "") +
			"Entity " + getType() + " ID=" + getPrimaryKey() +
			ofNullable(getHierarchicalPlacement()).map(it -> ", with " + it).orElse("") +
			(references.isEmpty() ? "" : ", " + references.stream().map(ReferenceContract::toString).collect(Collectors.joining(", "))) +
			(attributeValues.isEmpty() ? "" : ", " + attributeValues.stream().map(AttributeValue::toString).collect(Collectors.joining(", "))) +
			(associatedDataValues.isEmpty() ? "" : ", " + associatedDataValues.stream().map(AssociatedDataValue::toString).collect(Collectors.joining(", "))) +
			(prices.isEmpty() ? "" : ", " + prices.stream().map(Object::toString).collect(Collectors.joining(", "))) +
			(locales.isEmpty() ? "" : ", localized to " + locales.stream().map(Locale::toString).collect(Collectors.joining(", ")));
	}
}
