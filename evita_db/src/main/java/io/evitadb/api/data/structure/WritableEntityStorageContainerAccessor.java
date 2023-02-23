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

import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface is an extension to {@link EntityStorageContainerAccessor} that allows accepting and maintaining
 * assigned internal price identificators. For purpose of internal identificators see {@link PriceInternalIdContainer}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface WritableEntityStorageContainerAccessor extends EntityStorageContainerAccessor {

	/**
	 * Registers internal identificators in `priceId` argument to `priceKey` and `innerRecordId` combination inside
	 * the entity with `entityPrimaryKey`.
	 */
	void registerAssignedPriceId(
		int entityPrimaryKey,
		@Nonnull PriceKey priceKey,
		@Nullable Integer innerRecordId,
		@Nonnull PriceInternalIdContainer priceId
	);

	/**
	 * Returns assigned identificators for combination of `priceKey` and `innerRecordId` inside the entity with
	 * `entityPrimaryKey`.
	 */
	@Nonnull
	PriceInternalIdContainer findExistingInternalIds(
		int entityPrimaryKey,
		@Nonnull PriceKey priceKey,
		@Nullable Integer innerRecordId
	);

}
