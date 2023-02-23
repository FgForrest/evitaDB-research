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

import io.evitadb.api.data.mutation.associatedData.AssociatedDataMutation;
import io.evitadb.api.data.structure.AssociatedData;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.schema.AssociatedDataSchema;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Internal {@link io.evitadb.api.data.structure.AssociatedData} builder used solely from within {@link InitialEntityBuilder}.
 *
 * Due to performance reasons (see {@link DirectWriteOrOperationLog} microbenchmark) there is special implementation
 * for the situation when entity is newly created. In this case we know everything is new, and we don't need to closely
 * monitor the changes so this can speed things up.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface AssociatedDataEditor<W extends AssociatedDataEditor<W>> extends AssociatedDataContract {

	/**
	 * Removes value associated with the key or null when the associatedData is missing.
	 * @param associatedDataName
	 * @return self (builder pattern)
	 */
	@Nonnull
	W removeAssociatedData(@Nonnull String associatedDataName);

	/**
	 * Stores value associated with the key.
	 * @param associatedDataName
	 * @param associatedDataValue
	 * @param <T>
	 * @return self (builder pattern)
	 */
	@Nonnull
	<T extends Serializable> W setAssociatedData(@Nonnull String associatedDataName, @Nonnull T associatedDataValue);

	/**
	 * Stores array of values associated with the key.
	 * @param associatedDataName
	 * @param associatedDataValue
	 * @param <T>
	 * @return self (builder pattern)
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	<T extends Serializable> W setAssociatedData(@Nonnull String associatedDataName, @Nonnull T[] associatedDataValue);

	/**
	 * Removes locale specific value associated with the key or null when the associatedData is missing.
	 * @param associatedDataName
	 * @return self (builder pattern)
	 */
	@Nonnull
	W removeAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale);

	/**
	 * Stores locale specific value associated with the key.
	 * @param associatedDataName
	 * @param locale
	 * @param associatedDataValue
	 * @param <T>
	 * @return self (builder pattern)
	 */
	@Nonnull
	<T extends Serializable> W setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T associatedDataValue);

	/**
	 * Stores array of locale specific values associated with the key.
	 * @param associatedDataName
	 * @param locale
	 * @param associatedDataValue
	 * @param <T>
	 * @return self (builder pattern)
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	<T extends Serializable> W setAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, @Nonnull T[] associatedDataValue);

	/**
	 * Alters associatedData value in a way defined by the passed mutation implementation.
	 * There may never me multiple mutations for the same associatedData - if you need to compose mutations you must wrap
	 * them into single one, that is then handed to the builder.
	 *
	 * Remember each setAssociatedData produces a mutation itself - so you cannot set associatedData and mutate it in the same
	 * round. The latter operation would overwrite the previously registered mutation.
	 *
	 * @param mutation
	 * @return self (builder pattern)
	 */
	@Nonnull
	W mutateAssociatedData(@Nonnull AssociatedDataMutation mutation);

	/**
	 * Interface that simply combines writer and builder contracts together.
	 */
	interface AssociatedDataBuilder extends AssociatedDataEditor<AssociatedDataBuilder>, Builder<AssociatedData> {

		@Nonnull
		@Override
		Stream<? extends AssociatedDataMutation> buildChangeSet();

		/**
		 * Method creates implicit associatedData type for the associatedData value that doesn't map to any existing
		 * (known) associatedData type of the {@link io.evitadb.api.schema.EntitySchema} schema.
		 * @param associatedDataValue
		 * @return
		 */
		default AssociatedDataSchema createImplicitSchema(AssociatedDataValue associatedDataValue) {
			return new AssociatedDataSchema(
					associatedDataValue.getKey().getAssociatedDataName(),
					associatedDataValue.getValue().getClass(),
					associatedDataValue.getKey().isLocalized()
			);
		}

	}

}
