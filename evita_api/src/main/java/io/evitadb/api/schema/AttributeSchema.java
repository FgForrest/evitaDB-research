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

import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.query.require.AttributeHistogram;
import lombok.Data;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This is the definition object for {@link Attributes} that is stored along with
 * {@link Entity}. Definition objects allow to describe the structure of the entity type so that
 * in any time everyone can consult complete structure of the entity type. Definition object is similar to Java reflection
 * process where you can also at any moment see which fields and methods are available for the class.
 *
 * Entity attributes allows defining set of data that are fetched in bulk along with the entity body.
 * Attributes may be indexed for fast filtering or can be used to sort along. Attributes are not automatically indexed
 * in order not to waste precious memory space for data that will never be used in search queries.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Immutable
@ThreadSafe
@Data
public class AttributeSchema implements Serializable {
	private static final long serialVersionUID = 1340876688998990217L;

	/**
	 * Unique name of the attribute. Case sensitive. Distinguishes one associated data item from another within
	 * single entity instance.
	 */
	private final String name;
	/**
	 * When attribute is unique it is automatically filterable and it is ensured there is exactly one single entity
	 * having certain value of this attribute. {@link AttributeSchema#getType() Type} of the unique attribute must
	 * implement {@link Comparable<?>} interface.
	 *
	 * As an example of unique attribute can be URL - there is no sense in having two entities with same URL and it's
	 * better to have this ensured by the database engine.
	 */
	private final boolean unique;
	/**
	 * When attribute is filterable, it is possible to filter entities by this attribute. Do not mark attribute
	 * as filterable unless you know that you'll search entities by this attribute. Each filterable attribute occupies
	 * (memory/disk) space in the form of index. {@link AttributeSchema#getType() Type} of the filterable attribute must
	 * implement {@link Comparable<?>} interface.
	 *
	 * When attribute is filterable requirement {@link AttributeHistogram}
	 * can be used for this attribute.
	 */
	private final boolean filterable;
	/**
	 * When attribute is sortable, it is possible to sort entities by this attribute. Do not mark attribute
	 * as sortable unless you know that you'll sort entities along this attribute. Each sortable attribute occupies
	 * (memory/disk) space in the form of index. {@link AttributeSchema#getType() Type} of the filterable attribute must
	 * implement {@link Comparable<?>} interface.
	 */
	private final boolean sortable;
	/**
	 * When attribute is localized, it has to be ALWAYS used in connection with specific {@link java.util.Locale}.
	 */
	private final boolean localized;
	/**
	 * Type of the attribute. Must be one of {@link EvitaDataTypes#getSupportedDataTypes()}.
	 */
	private final Class<? extends Serializable> type;
	/**
	 * Determines how many fractional places are important when entities are compared during filtering or sorting. It is
	 * important to know that all values of this attribute will be converted to {@link java.lang.Integer}, so the attribute
	 * number must not ever exceed maximum limits of {@link java.lang.Integer} type when scaling the number by the power
	 * of ten using {@link #getIndexedDecimalPlaces()} as exponent.
	 */
	private final int indexedDecimalPlaces;

	public AttributeSchema(String name, Class<? extends Serializable> type, boolean localized) {
		this.name = name;
		this.type = type;
		this.localized = localized;
		this.unique = false;
		this.filterable = false;
		this.sortable = false;
		this.indexedDecimalPlaces = 0;
	}

	public AttributeSchema(String name, boolean unique, boolean filterable, boolean sortable, boolean localized, Class<? extends Serializable> type) {
		this.name = name;
		this.unique = unique;
		this.filterable = filterable;
		this.sortable = sortable;
		this.localized = localized;
		this.type = type;
		if ((filterable || sortable) && BigDecimal.class.equals(type)) {
			throw new IllegalArgumentException(
					"IndexedDecimalPlaces must be specified for attributes of type BigDecimal (attribute: " + name + ")!"
			);
		}
		this.indexedDecimalPlaces = 0;
	}

	public AttributeSchema(String name, boolean unique, boolean filterable, boolean sortable, boolean localized, Class<? extends Serializable> type, int indexedDecimalPlaces) {
		this.name = name;
		this.unique = unique;
		this.filterable = filterable;
		this.sortable = sortable;
		this.localized = localized;
		this.type = type;
		this.indexedDecimalPlaces = indexedDecimalPlaces;
	}

	/**
	 * Returns attribute type that represents non-array type class. I.e. method just unwraps array types to plain ones.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Serializable> getPlainType() {
		return type.isArray() ? (Class<? extends Serializable>) type.getComponentType() : type;
	}
}
