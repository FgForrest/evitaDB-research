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
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.query.require.Attributes;
import io.evitadb.api.utils.Assert;
import lombok.Data;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * This is the definition object for {@link Reference} that is stored along with
 * {@link Entity}. Definition objects allow to describe the structure of the entity type so that
 * in any time everyone can consult complete structure of the entity type. Definition object is similar to Java reflection
 * process where you can also at any moment see which fields and methods are available for the class.
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
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
@Data
public class ReferenceSchema implements Serializable {
	private static final long serialVersionUID = 2018566260261489037L;

	/**
	 * Reference to {@link Entity#getType()} of the referenced entity. Might be also anything {@link Serializable}
	 * that identifies type some external resource not maintained by Evita.
	 */
	private final Serializable entityType;
	/**
	 * Contains TRUE if {@link #getEntityType()} refers to any existing {@link EntitySchema#getName()} that is maintained
	 * by Evita.
	 */
	private final boolean entityTypeRelatesToEntity;
	/**
	 * Reference to {@link Entity#getType()} of the referenced entity. Might be also anything {@link Serializable}
	 * that identifies type some external resource not maintained by Evita.
	 */
	private final Serializable groupType;
	/**
	 * Contains TRUE if {@link #getGroupType()} ()} refers to any existing {@link EntitySchema#getName()} that is maintained
	 * by Evita.
	 */
	private final boolean groupTypeRelatesToEntity;
	/**
	 * Contains TRUE if the index for this reference should be created and maintained allowing to filter by
	 * {@link io.evitadb.api.query.filter.ReferenceHavingAttribute} filtering constraints. Index is also required
	 * when reference is {@link #isFaceted() faceted}.
	 *
	 * Do not mark reference as faceted unless you know that you'll need to filter entities by this reference. Each
	 * indexed reference occupies (memory/disk) space in the form of index. When reference is not indexed, the entity
	 * cannot be looked up by reference attributes or relation existence itself, but the data is loaded alongside
	 * other references and is available by calling {@link SealedEntity#getReferences()} method.
	 */
	private final boolean indexed;
	/**
	 * Contains TRUE if the statistics data for this reference should be maintained and this allowing to get
	 * {@link FacetStatistics} for this reference or use {@link io.evitadb.api.query.filter.Facet}
	 * filtering constraint.
	 *
	 * Do not mark reference as faceted unless you want it among {@link FacetStatistics}. Each faceted reference
	 * occupies (memory/disk) space in the form of index.
	 *
	 * Reference that was marked as faceted is called Facet.
	 */
	private final boolean faceted;
	/**
	 * Attributes related to reference allows defining set of data that are fetched in bulk along with the entity body.
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
	 */
	private final Map<String, AttributeSchema> attributes;

	public ReferenceSchema(Serializable entityType, boolean entityTypeRelatesToEntity, Serializable groupType, boolean groupTypeRelatesToEntity, boolean indexed, boolean faceted) {
		this.entityType = entityType;
		this.entityTypeRelatesToEntity = entityTypeRelatesToEntity;
		this.groupType = groupType;
		this.groupTypeRelatesToEntity = groupTypeRelatesToEntity;
		this.indexed = indexed;
		this.faceted = faceted;
		if (this.faceted) {
			Assert.isTrue(this.indexed, "When reference is marked as faceted, it needs also to be indexed.");
		}
		//we need to wrap even empty map to the unmodifiable wrapper in order to unify type for Kryo serialization
		//noinspection RedundantUnmodifiable
		this.attributes = Collections.unmodifiableMap(Collections.emptyMap());
	}

	ReferenceSchema(Serializable entityType, boolean entityTypeRelatesToEntity, Serializable groupType, boolean groupTypeRelatesToEntity, boolean indexed, boolean faceted, Map<String, AttributeSchema> attributes) {
		this.entityType = entityType;
		this.entityTypeRelatesToEntity = entityTypeRelatesToEntity;
		this.groupType = groupType;
		this.groupTypeRelatesToEntity = groupTypeRelatesToEntity;
		this.indexed = indexed;
		this.faceted = faceted;
		if (this.faceted) {
			Assert.isTrue(this.indexed, "When reference is marked as faceted, it needs also to be indexed.");
		}
		this.attributes = Collections.unmodifiableMap(attributes);
	}

	/**
	 * Returns attribute definition by its unique name.
	 */
	public AttributeSchema getAttribute(String attributeName) {
		return attributes.get(attributeName);
	}

}
