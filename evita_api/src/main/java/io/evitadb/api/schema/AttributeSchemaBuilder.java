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

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;

import java.io.Serializable;
import java.util.function.BooleanSupplier;

/**
 * Internal {@link AttributeSchema} builder used solely from within {@link EntitySchemaBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class AttributeSchemaBuilder {
	/**
	 * Name of the attribute.
	 */
	private final String name;
	/**
	 * May be set by the client - makes attribute filterable.
	 */
	private boolean filterable;
	/**
	 * May be set by the client - makes attribute unique.
	 */
	private boolean unique;
	/**
	 * May be set by the client - makes attribute sortable.
	 */
	private boolean sortable;
	/**
	 * May be set by the client - makes attribute localized.
	 */
	private boolean localized;
	/**
	 * May be set by the client - defines number of decimal places that will be used for creating search index.
	 */
	private int indexedDecimalPlaces;
	/**
	 * Must be set by client - defines value type of the attribute.
	 */
	private final Class<? extends Serializable> ofType;

	AttributeSchemaBuilder(AttributeSchema existingSchema) {
		this.name = existingSchema.getName();
		this.filterable = existingSchema.isFilterable();
		this.unique = existingSchema.isUnique();
		this.sortable = existingSchema.isSortable();
		this.localized = existingSchema.isLocalized();
		this.indexedDecimalPlaces = existingSchema.getIndexedDecimalPlaces();
		this.ofType = existingSchema.getType();
	}

	AttributeSchemaBuilder(String name, Class<? extends Serializable> ofType) {
		this.name = name;
		this.filterable = false;
		this.unique = false;
		this.sortable = false;
		this.localized = false;
		this.indexedDecimalPlaces = 0;
		this.ofType = ofType;

		Assert.isTrue(
			EvitaDataTypes.isSupportedTypeOrItsArray(ofType),
			"Data type " + ofType.getName() + " is not supported."
		);
	}

	/**
	 * When attribute is filterable, it is possible to filter entities by this attribute. Do not mark attribute
	 * as filterable unless you know that you'll search entities by this attribute. Each filterable attribute occupies
	 * (memory/disk) space in the form of index. {@link AttributeSchema#getType() Type} of the filterable attribute must
	 * implement {@link Comparable<?>} interface.
	 *
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder filterable() {
		this.filterable = true;
		return this;
	}

	/**
	 * When attribute is filterable, it is possible to filter entities by this attribute. Do not mark attribute
	 * as filterable unless you know that you'll search entities by this attribute. Each filterable attribute occupies
	 * (memory/disk) space in the form of index. {@link AttributeSchema#getType() Type} of the filterable attribute must
	 * implement {@link Comparable<?>} interface.
	 *
	 * @param decider returns true when attribute should be filtered
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder filterable(BooleanSupplier decider) {
		this.filterable = decider.getAsBoolean();
		return this;
	}

	/**
	 * When attribute is unique it is automatically filterable and it is ensured there is exactly one single entity
	 * having certain value of this attribute. {@link AttributeSchema#getType() Type} of the unique attribute must
	 * implement {@link Comparable<?>} interface.
	 * <p>
	 * As an example of unique attribute can be URL - there is no sense in having two entities with same URL and it's
	 * better to have this ensured by the database engine.
	 *
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder unique() {
		this.unique = true;
		return this;
	}

	/**
	 * When attribute is unique it is automatically filterable and it is ensured there is exactly one single entity
	 * having certain value of this attribute. {@link AttributeSchema#getType() Type} of the unique attribute must
	 * implement {@link Comparable<?>} interface.
	 * <p>
	 * As an example of unique attribute can be URL - there is no sense in having two entities with same URL and it's
	 * better to have this ensured by the database engine.
	 *
	 * @param decider returns true when attribute should be unique
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder unique(BooleanSupplier decider) {
		this.unique = decider.getAsBoolean();
		return this;
	}

	/**
	 * When attribute is sortable, it is possible to sort entities by this attribute. Do not mark attribute
	 * as sortable unless you know that you'll sort entities along this attribute. Each sortable attribute occupies
	 * (memory/disk) space in the form of index. {@link AttributeSchema#getType() Type} of the filterable attribute must
	 * implement {@link Comparable<?>} interface.
	 *
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder sortable() {
		this.sortable = true;
		return this;
	}

	/**
	 * When attribute is sortable, it is possible to sort entities by this attribute. Do not mark attribute
	 * as sortable unless you know that you'll sort entities along this attribute. Each sortable attribute occupies
	 * (memory/disk) space in the form of index. {@link AttributeSchema#getType() Type} of the filterable attribute must
	 * implement {@link Comparable<?>} interface.
	 *
	 * @param decider returns true when attribute should be sortable
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder sortable(BooleanSupplier decider) {
		this.sortable = decider.getAsBoolean();
		return this;
	}

	/**
	 * Localized attribute has to be ALWAYS used in connection with specific {@link java.util.Locale}. In other
	 * words - it cannot be stored unless associated locale is also provided.
	 *
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder localized() {
		this.localized = true;
		return this;
	}

	/**
	 * Localized attribute has to be ALWAYS used in connection with specific {@link java.util.Locale}. In other
	 * words - it cannot be stored unless associated locale is also provided.
	 *
	 * @param decider returns true when attribute should be localized
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder localized(BooleanSupplier decider) {
		this.localized = decider.getAsBoolean();
		return this;
	}

	/**
	 * Determines how many fractional places are important when entities are compared during filtering or sorting. It is
	 * important to know that all values of this attribute will be converted to {@link java.lang.Integer}, so the attribute
	 * number must not ever exceed maximum limits of {@link java.lang.Integer} type when scaling the number by the power
	 * of ten using `indexDecimalPlaces` as exponent.
	 *
	 * @return builder to continue with configuration
	 */
	public AttributeSchemaBuilder indexDecimalPlaces(int indexedDecimalPlaces) {
		this.indexedDecimalPlaces = indexedDecimalPlaces;
		return this;
	}

	/**
	 * Creates attribute schema instance.
	 */
	public AttributeSchema build() {
		Assert.isTrue(
			(!filterable && !sortable && !unique) ||
				Comparable.class.isAssignableFrom(ReflectionLookup.getSimpleType(ofType)),
			"Data type `" + ofType.getName() + "` must implement Comparable in order to be usable for indexing!"
		);
		Assert.isTrue(
			!(filterable && unique),
			"Attribute `" + name + "` cannot be both unique and filterable. Unique attributes are implicitly filterable!"
		);
		Assert.isTrue(
			!(sortable && ofType.isArray()),
			"Attribute `" + name + "` is sortable but also an array. Arrays cannot be handled by sorting algorithm!"
		);

		return new AttributeSchema(
			name, unique, filterable, sortable, localized, ofType, indexedDecimalPlaces
		);
	}

}
