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
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.MemoryMeasuringConstants;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Contract for classes that allow reading information about references in {@link Entity} instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface ReferenceContract extends AttributesContract, Droppable, Comparable<ReferenceContract>, ContentComparator<ReferenceContract> {

	/**
	 * Returns reference to the entity facet points to. Reference is composed of entity type and primary key
	 * of the referenced group entity. Referenced entity may or may not be Evita entity.
	 */
	@Nonnull
	EntityReference getReferencedEntity();

	/**
	 * Returns reference group. Group is composed of entity type and primary key of the referenced group entity.
	 * Group may or may not be Evita entity.
	 */
	@Nullable
	GroupEntityReference getGroup();

	/**
	 * Returns schema that describes this type of reference.
	 * NULL can be returned in case schema hasn't yet known the reference type, but will be automatically created
	 * if {@link io.evitadb.api.schema.EvolutionMode#ADDING_REFERENCES} is allowed.
	 */
	@Nullable
	ReferenceSchema getReferenceSchema();

	/**
	 * Referenced entity is a business key - we can compare according it.
	 */
	@Override
	default int compareTo(ReferenceContract o) {
		return getReferencedEntity().compareTo(o.getReferencedEntity());
	}

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	default int estimateSize() {
		return MemoryMeasuringConstants.OBJECT_HEADER_SIZE
			// version
			+ MemoryMeasuringConstants.INT_SIZE +
			// dropped
			+ MemoryMeasuringConstants.BYTE_SIZE +
			// referenced entity
			+ MemoryMeasuringConstants.REFERENCE_SIZE + getReferencedEntity().estimateSize() +
			// group
			+ MemoryMeasuringConstants.REFERENCE_SIZE + ofNullable(getGroup()).stream().mapToInt(EntityReference::estimateSize).sum() +
			// schema
			+ MemoryMeasuringConstants.REFERENCE_SIZE;
	}

	/**
	 * Returns true if reference differs by any "business" related data from other reference.
	 */
	@Override
	default boolean differsFrom(@Nullable ReferenceContract otherReference) {
		if (otherReference == null) return true;
		if (!Objects.equals(getReferencedEntity(), otherReference.getReferencedEntity())) return true;
		if (ofNullable(getGroup()).map(it -> it.differsFrom(otherReference.getGroup())).orElseGet(() -> otherReference.getGroup() != null))
			return true;
		if (isDropped() != otherReference.isDropped()) return true;
		return AttributesContract.anyAttributeDifferBetween(this, otherReference);
	}

	/**
	 * This class envelopes reference to the reference group. It adds support for versioning and tombstone on top of basic
	 * {@link EntityReference} structure.
	 */
	@EqualsAndHashCode(callSuper = true, of = "version")
	class GroupEntityReference extends EntityReference implements Droppable, Serializable, ContentComparator<GroupEntityReference> {
		private static final long serialVersionUID = -890552951231848828L;

		/**
		 * Contains version of this object and gets increased with any entity update. Allows to execute
		 * optimistic locking i.e. avoiding parallel modifications.
		 */
		@Getter private final int version;
		/**
		 * Contains TRUE if reference group reference was dropped - i.e. removed. Such reference is not removed (unless
		 * tidying process does it), but are lying in reference with tombstone flag. Dropped reference
		 * can be overwritten by a new value continuing with the versioning where it was stopped for the last time.
		 */
		@Getter private final boolean dropped;

		public GroupEntityReference(@Nonnull Serializable referencedEntity, int primaryKey) {
			super(referencedEntity, primaryKey);
			this.version = 1;
			this.dropped = false;
		}

		public GroupEntityReference(int version, @Nonnull Serializable referencedEntity, int primaryKey, boolean dropped) {
			super(referencedEntity, primaryKey);
			this.version = version;
			this.dropped = dropped;
		}

		/**
		 * Returns true if reference group differs by any "business" related data from other reference group.
		 */
		@Override
		public boolean differsFrom(@Nullable GroupEntityReference otherReferenceGroup) {
			if (otherReferenceGroup == null) {
				return true;
			}
			if (!Objects.equals(super.getType(), otherReferenceGroup.getType())) {
				return true;
			}
			if (!Objects.equals(super.getPrimaryKey(), otherReferenceGroup.getPrimaryKey())) {
				return true;
			}
			return isDropped() != otherReferenceGroup.isDropped();
		}

		@Override
		public int estimateSize() {
			//version
			return MemoryMeasuringConstants.INT_SIZE +
				// dropped
				MemoryMeasuringConstants.BYTE_SIZE +
				// parent data
				super.estimateSize();
		}

		@Override
		public String toString() {
			return (dropped ? "❌" : "") +
				getType() + " with key " + getPrimaryKey();
		}
	}

}
