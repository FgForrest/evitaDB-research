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

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.storage.model.storageParts.EntityStoragePart;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * This container class represents single {@link Entity} and contains all data necessary to fetch other entity data.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@EqualsAndHashCode(exclude = "dirty")
@ToString(exclude = "dirty")
public class EntityBodyStoragePart implements EntityStoragePart {
	private static final long serialVersionUID = 34998825794290379L;
	/**
	 * See {@link Entity#getPrimaryKey()}.
	 */
	@Getter private final int primaryKey;
	/**
	 * See {@link Entity#getVersion()}.
	 */
	private int version;
	/**
	 * See {@link Entity#getHierarchicalPlacement()}.
	 */
	@Getter private HierarchicalPlacementContract hierarchicalPlacement;
	/**
	 * See {@link Entity#getLocales()}.
	 */
	@Getter private Set<Locale> locales;
	/**
	 * See {@link Entity#getAttributeLocales()}.
	 */
	@Getter private Set<Locale> attributeLocales;
	/**
	 * Contains set of all associated data keys that are used in this entity.
	 */
	@Getter private Set<AssociatedDataKey> associatedDataKeys;
	/**
	 * Contains true if anything changed in this container.
	 */
	@Getter private boolean dirty;

	public EntityBodyStoragePart(int primaryKey) {
		this.primaryKey = primaryKey;
		this.locales = new LinkedHashSet<>();
		this.attributeLocales = new LinkedHashSet<>();
		this.associatedDataKeys = new LinkedHashSet<>();
		this.dirty = true;
	}

	public EntityBodyStoragePart(int version, @Nonnull Integer primaryKey, @Nonnull HierarchicalPlacementContract hierarchicalPlacement, @Nonnull Set<Locale> locales, @Nonnull Set<Locale> attributeLocales, @Nonnull Set<AssociatedDataKey> associatedDataKeys) {
		this.version = version;
		this.primaryKey = primaryKey;
		this.hierarchicalPlacement = hierarchicalPlacement;
		this.locales = locales;
		this.attributeLocales = attributeLocales;
		this.associatedDataKeys = associatedDataKeys;
	}

	@Nullable
	@Override
	public Long getUniquePartId() {
		return (long) primaryKey;
	}

	@Nonnull
	@Override
	public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
		return primaryKey;
	}

	/**
	 * Updates hierarchical placement of the entity.
	 */
	public void setHierarchicalPlacement(HierarchicalPlacementContract hierarchicalPlacement) {
		if ((this.hierarchicalPlacement == null && hierarchicalPlacement != null) || (this.hierarchicalPlacement != null && this.hierarchicalPlacement.differsFrom(hierarchicalPlacement))) {
			this.hierarchicalPlacement = hierarchicalPlacement;
			this.dirty = true;
		}
	}

	/**
	 * Updates set of locales that is entity localized to (any of its attributes or associated data).
	 */
	public void setLocales(Set<Locale> locales) {
		if (!this.locales.equals(locales)) {
			this.locales = locales;
			this.dirty = true;
		}
	}

	/**
	 * Updates set of locales of all localized attributes of this entity.
	 */
	public void setAttributeLocales(Set<Locale> attributeLocales) {
		if (!this.attributeLocales.equals(attributeLocales)) {
			this.attributeLocales = attributeLocales;
			this.dirty = true;
		}
	}

	/**
	 * Updates set of associated data keys the entity posses of. This set is crucial for finding all related associated
	 * data of the entity.
	 */
	public void setAssociatedDataKeys(Set<AssociatedDataKey> associatedDataKeys) {
		if (!this.associatedDataKeys.equals(associatedDataKeys)) {
			this.associatedDataKeys = associatedDataKeys;
			this.dirty = true;
		}
	}

	/**
	 * Returns distinct locales of associated data.
	 */
	public Set<Locale> getAssociatedDataLocales() {
		return associatedDataKeys.stream().map(AssociatedDataKey::getLocale).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	/**
	 * Returns version of the entity for storing (incremented by one, if anything changed).
	 */
	public int getVersion() {
		return dirty ? version + 1 : version;
	}

	/**
	 * Method registers new {@link Locale} used in attributes.
	 */
	public void addAttributeLocale(Locale locale) {
		this.attributeLocales.add(locale);
		this.locales.add(locale);
		this.dirty = true;
	}

	/**
	 * Method registers new {@link AssociatedDataKey} referenced by this entity.
	 */
	public void addAssociatedDataKey(AssociatedDataKey associatedDataKey) {
		// if associated data is localized - enrich the set of entity locales
		ofNullable(associatedDataKey.getLocale()).ifPresent(locales::add);
		this.associatedDataKeys.add(associatedDataKey);
		this.dirty = true;
	}
}
