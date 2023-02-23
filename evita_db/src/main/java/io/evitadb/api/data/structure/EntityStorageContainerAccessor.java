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

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.storage.model.storageParts.entity.*;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * This interface contains method that allows accessing storage container objects in current index version. All accesses
 * are cached so that multiple calls of the same method performs only single I/O access.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface EntityStorageContainerAccessor {

	/**
	 * Fetches entity container.
	 */
	@Nonnull
	EntityBodyStoragePart getEntityStorageContainer(int entityPrimaryKey, EntityExistence expects);

	/**
	 * Fetches global attributes container.
	 */
	@Nonnull
	AttributesStoragePart getAttributeStorageContainer(int entityPrimaryKey);

	/**
	 * Fetches localized attributes container.
	 */
	@Nonnull
	AttributesStoragePart getAttributeStorageContainer(int entityPrimaryKey, @Nonnull Locale locale);

	/**
	 * Fetches associated data container.
	 */
	@Nonnull
	AssociatedDataStoragePart getAssociatedDataStorageContainer(int entityPrimaryKey, @Nonnull AssociatedDataKey key);

	/**
	 * Fetches reference container.
	 */
	@Nonnull
	ReferencesStoragePart getReferencesStorageContainer(int entityPrimaryKey);

	/**
	 * Fetches prices container.
	 */
	@Nonnull
	PricesStoragePart getPriceStorageContainer(int entityPrimaryKey);

}
