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

package io.evitadb.api.data.mutation.associatedData;

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Upsert associatedData mutation will either update existing associatedData or create new one.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UpsertAssociatedDataMutation extends AssociatedDataSchemaEvolvingMutation {
	private static final long serialVersionUID = 2106367735845445016L;
	@Nonnull private final Serializable value;

	public UpsertAssociatedDataMutation(@Nonnull AssociatedDataKey associatedDataKey, @Nonnull Serializable value) {
		super(associatedDataKey);
		this.value = value;
	}

	@Override
	public long getPriority() {
		return PRIORITY_UPSERT;
	}

	@Nonnull
	@Override
	public Serializable getAssociatedDataValue() {
		return value;
	}

	@Nonnull
	@Override
	public AssociatedDataValue mutateLocal(@Nullable AssociatedDataValue existingValue) {
		if (existingValue == null) {
			// create new associatedData value
			return new AssociatedDataValue(associatedDataKey, value);
		} else {
			// update associatedData version (we changed it) and return mutated value
			return new AssociatedDataValue(existingValue.getVersion() + 1, associatedDataKey, this.value);
		}
	}
}
