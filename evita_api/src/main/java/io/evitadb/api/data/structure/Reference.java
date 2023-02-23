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

import io.evitadb.api.data.AttributesContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.ReferenceEditor.ReferenceBuilder;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
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
 * Class is immutable on purpose - we want to support caching the entities in a shared cache and accessed by many threads.
 * For altering the contents use {@link ReferenceBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
@Data
@EqualsAndHashCode(of = {"version", "referencedEntity"})
public class Reference implements ReferenceContract {
	private static final long serialVersionUID = -2624502273901281240L;

	/**
	 * Entity schema definition.
	 */
	private final EntitySchema entitySchema;
	/**
	 * Contains version of this object and gets increased with any entity update. Allows to execute
	 * optimistic locking i.e. avoiding parallel modifications.
	 */
	private final int version;
	/**
	 * Reference to the Evita {@link Entity} or any external entity not maintained by Evita.
	 */
	private final EntityReference referencedEntity;
	/**
	 * Reference to the Evita {@link Entity} or any external entity not maintained by Evita.
	 * Facet group aggregates facets of the same type - for example by color, size, brand or whatever else.
	 */
	private final GroupEntityReference group;
	/**
	 * Properties valid only for this relation. Can be used to carry information about order (i.e. order of the entity
	 * "product" in certain "category" entity, same "product" may have entirely different order in relation to different
	 * "category").
	 */
	@Delegate(types = AttributesContract.class)
	private final Attributes attributes;
	/**
	 * Contains TRUE if facet reference was dropped - i.e. removed. Facets are not removed (unless tidying process
	 * does it), but are lying among other facets with tombstone flag. Dropped facets can be overwritten by
	 * a new value continuing with the versioning where it was stopped for the last time.
	 */
	@Getter private final boolean dropped;

	public Reference(@Nonnull EntitySchema entitySchema, @Nonnull EntityReference referencedEntity, @Nullable GroupEntityReference group) {
		this.version = 1;
		this.entitySchema = entitySchema;
		this.referencedEntity = referencedEntity;
		this.group = group;
		this.attributes = new Attributes(entitySchema);
		this.dropped = false;
	}

	public Reference(@Nonnull EntitySchema entitySchema, int version, @Nonnull EntityReference referencedEntity, @Nullable GroupEntityReference group, boolean dropped) {
		this.entitySchema = entitySchema;
		this.version = version;
		this.referencedEntity = referencedEntity;
		this.group = group;
		this.attributes = new Attributes(entitySchema);
		this.dropped = dropped;
	}

	public Reference(@Nonnull EntitySchema entitySchema, int version, @Nonnull EntityReference referencedEntity, @Nullable GroupEntityReference group, @Nonnull Attributes attributes, boolean dropped) {
		this.entitySchema = entitySchema;
		this.version = version;
		this.referencedEntity = referencedEntity;
		this.group = group;
		this.attributes = attributes;
		this.dropped = dropped;
	}

	public Reference(@Nonnull EntitySchema entitySchema, int version, @Nonnull EntityReference referencedEntity, @Nullable GroupEntityReference group, @Nonnull Collection<AttributeValue> attributes, @Nonnull Set<Locale> attributeLocales, boolean dropped) {
		this.entitySchema = entitySchema;
		this.version = version;
		this.referencedEntity = referencedEntity;
		this.group = group;
		this.attributes = new Attributes(entitySchema, attributes, attributeLocales);
		this.dropped = dropped;
	}

	public Reference(@Nonnull EntitySchema entitySchema, @Nonnull EntityReference referencedEntity, @Nullable GroupEntityReference group, @Nonnull Attributes attributes) {
		this.entitySchema = entitySchema;
		this.version = 1;
		this.referencedEntity = referencedEntity;
		this.group = group;
		this.attributes = attributes;
		this.dropped = false;
	}

	@Nullable
	@Override
	public ReferenceSchema getReferenceSchema() {
		return this.entitySchema.getReference(referencedEntity.getType());
	}

	@Override
	public String toString() {
		return (dropped ? "❌" : "") +
			"References " + referencedEntity +
			(group == null ? "" : " in " + group) +
			", attrs: " + attributes;
	}

}
