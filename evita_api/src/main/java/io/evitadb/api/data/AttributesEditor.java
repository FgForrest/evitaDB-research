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

import io.evitadb.api.data.mutation.attribute.AttributeMutation;
import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.schema.AttributeSchema;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Contract for classes that allow creating / updating or removing information in {@link Attributes} instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface AttributesEditor<W extends AttributesEditor<W>> extends AttributesContract {

	/**
	 * Removes value associated with the key or null when the attribute is missing.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	W removeAttribute(@Nonnull String attributeName);

	/**
	 * Stores value associated with the key.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	<T extends Serializable & Comparable<?>> W setAttribute(@Nonnull String attributeName, @Nonnull T attributeValue);

	/**
	 * Stores array of values associated with the key.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	<T extends Serializable & Comparable<?>> W setAttribute(@Nonnull String attributeName, @Nonnull T[] attributeValue);

	/**
	 * Removes locale specific value associated with the key or null when the attribute is missing.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	W removeAttribute(@Nonnull String attributeName, @Nonnull Locale locale);

	/**
	 * Stores locale specific value associated with the key.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	<T extends Serializable & Comparable<?>> W setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T attributeValue);

	/**
	 * Stores array of locale specific values associated with the key.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	<T extends Serializable & Comparable<?>> W setAttribute(@Nonnull String attributeName, @Nonnull Locale locale, @Nonnull T[] attributeValue);

	/**
	 * Alters attribute value in a way defined by the passed mutation implementation.
	 * There may never me multiple mutations for the same attribute - if you need to compose mutations you must wrap
	 * them into single one, that is then handed to the builder.
	 * <p>
	 * Remember each setAttribute produces a mutation itself - so you cannot set attribute and mutate it in the same
	 * round. The latter operation would overwrite the previously registered mutation.
	 *
	 * @return self (builder pattern)
	 */
	@Nonnull
	W mutateAttribute(@Nonnull AttributeMutation mutation);

	/**
	 * Interface that simply combines writer and builder contracts together.
	 */
	interface AttributesBuilder extends AttributesEditor<AttributesBuilder>, Builder<Attributes> {

		@Nonnull
		@Override
		Stream<? extends AttributeMutation> buildChangeSet();

		/**
		 * Method creates implicit attribute type for the attribute value that doesn't map to any existing (known) attribute
		 * type of the {@link io.evitadb.api.schema.EntitySchema} schema.
		 */
		default AttributeSchema createImplicitSchema(@Nonnull AttributeValue attributeValue) {
			return new AttributeSchema(
				attributeValue.getKey().getAttributeName(),
				attributeValue.getValue().getClass(),
				attributeValue.getKey().isLocalized()
			);
		}

	}

}
