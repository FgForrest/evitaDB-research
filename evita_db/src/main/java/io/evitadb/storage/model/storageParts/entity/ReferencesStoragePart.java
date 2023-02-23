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

package io.evitadb.storage.model.storageParts.entity;

import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ArrayUtils.InsertionPosition;
import io.evitadb.storage.model.storageParts.EntityStoragePart;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * This container class represents collection of {@link Reference} of single {@link Entity}. Contains all references
 * along with grouping information and related attributes (localized and non-localized as well).
 *
 * Although query allows fetching references only of certain type, all references including all their attributes
 * are stored in single storage container because the data are expected to be small.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@ToString(of = "entityPrimaryKey")
public class ReferencesStoragePart implements EntityStoragePart {
	private static final long serialVersionUID = -4113353795728768940L;
	private static final ReferenceContract[] EMPTY_REFERENCES = new ReferenceContract[0];

	/**
	 * Id used for lookups in {@link io.evitadb.storage.MemTable} for this particular container.
	 */
	@Getter private final int entityPrimaryKey;
	/**
	 * See {@link Entity#getReferences()}. References are sorted in ascending order according to {@link EntityReference} comparator.
	 */
	@Getter private ReferenceContract[] references = EMPTY_REFERENCES;
	/**
	 * Contains true if anything changed in this container.
	 */
	@Getter private boolean dirty;

	public ReferencesStoragePart(int entityPrimaryKey) {
		this.entityPrimaryKey = entityPrimaryKey;
	}

	public ReferencesStoragePart(int entityPrimaryKey, ReferenceContract[] references) {
		this.entityPrimaryKey = entityPrimaryKey;
		this.references = references;
	}

	@Nullable
	@Override
	public Long getUniquePartId() {
		return (long) entityPrimaryKey;
	}

	@Override
	public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
		return entityPrimaryKey;
	}

	/**
	 * Adds new or replaces existing reference of the entity.
	 */
	public void replaceOrAddReference(@Nonnull EntityReference referenceKey, UnaryOperator<ReferenceContract> mutator) {
		final InsertionPosition insertionPosition = ArrayUtils.computeInsertPositionOfObjInOrderedArray(
			this.references, referenceKey,
			(examinedReference, rk) -> examinedReference.getReferencedEntity().compareTo(rk)
		);
		final int position = insertionPosition.getPosition();
		if (insertionPosition.isAlreadyPresent()) {
			final ReferenceContract reference = mutator.apply(this.references[position]);
			if (this.references[position].differsFrom(reference)) {
				this.references[position] = reference;
				dirty = true;
			}
		} else {
			this.references = ArrayUtils.insertRecordIntoArray(mutator.apply(null), this.references, position);
			dirty = true;
		}
	}

	/**
	 * Returns reference array as collection so it can be easily used in {@link Entity}.
	 */
	public Collection<ReferenceContract> getReferencesAsCollection() {
		return Arrays.stream(references).collect(Collectors.toList());
	}

	/**
	 * Returns array of primary keys of all referenced entities of particular `referencedEntityType`.
	 */
	public Integer[] getReferencedIds(@Nonnull Serializable referencedEntityType) {
		return Arrays.stream(references)
			.filter(it -> Objects.equals(referencedEntityType, it.getReferencedEntity().getType()))
			.map(it -> it.getReferencedEntity().getPrimaryKey())
			.toArray(Integer[]::new);
	}

	/**
	 * Finds reference to target entity specified by `referenceKey` in current container or throws exception.
	 *
	 * @throws IllegalStateException when reference is not found
	 */
	public ReferenceContract findReferenceOrThrowException(EntityReferenceContract<?> referenceKey) {
		return Arrays
			.stream(references)
			.filter(it -> it.getReferencedEntity().equals(referenceKey))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Reference " + referenceKey + " for entity `" + entityPrimaryKey + "` was not found!"));
	}
}
