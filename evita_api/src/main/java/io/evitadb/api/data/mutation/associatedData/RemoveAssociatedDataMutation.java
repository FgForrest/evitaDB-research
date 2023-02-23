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
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Remove associated data mutation will drop existing associatedData - ie.generates new version of the associated data with tombstone
 * on it.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class RemoveAssociatedDataMutation extends AssociatedDataMutation {
	private static final long serialVersionUID = 3777453666285515950L;

	public RemoveAssociatedDataMutation(@Nonnull AssociatedDataKey associatedDataKey) {
		super(associatedDataKey);
	}

	@Nonnull
	@Override
	public AssociatedDataValue mutateLocal(@Nullable AssociatedDataValue existingValue) {
		Assert.isTrue(
			existingValue != null && existingValue.exists(),
			() -> new InvalidMutationException(
				"Cannot remove " + associatedDataKey.getAssociatedDataName() +
					" associated data - it doesn't exist!"
			)
		);
		return new AssociatedDataValue(existingValue.getVersion() + 1, existingValue.getKey(), existingValue.getValue(), true);
	}

	@Override
	public long getPriority() {
		return PRIORITY_REMOVAL;
	}

}
