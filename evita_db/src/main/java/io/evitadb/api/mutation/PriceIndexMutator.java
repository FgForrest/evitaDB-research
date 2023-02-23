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

package io.evitadb.api.mutation;

import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.IndexType;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;
import io.evitadb.index.price.model.internalId.PriceWithInternalIds;
import io.evitadb.storage.model.storageParts.entity.PricesStoragePart;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;

import static io.evitadb.api.utils.NumberUtils.convertToInt;

/**
 * This interface is used to co-locate price mutating routines which are rather procedural and long to avoid excessive
 * amount of code in {@link EntityIndexLocalMutationExecutor}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface PriceIndexMutator {

	/**
	 * Method handles inserting or updating price in index according the changes in passed arguments. Update is executed
	 * as removal of previously stored value and inserting new price information to the indexes again.
	 */
	static void priceUpsert(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex entityIndex,
		int entityPrimaryKey,
		@Nonnull PriceKey priceKey,
		@Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity,
		@Nonnull BigDecimal priceWithoutVat,
		@Nonnull BigDecimal priceWithVat,
		boolean sellable,
		@Nonnull BiFunction<PriceKey, Integer, PriceInternalIdContainer> internalIdSupplier
	) {
		final PricesStoragePart entityFormerPrices = executor.getContainerAccessor().getPriceStorageContainer(entityPrimaryKey);
		final PriceWithInternalIds formerPrice = entityFormerPrices.getPriceByKey(priceKey);
		final PriceInnerRecordHandling innerRecordHandling = entityFormerPrices.getPriceInnerRecordHandling();
		priceUpsert(
			executor, entityIndex, priceKey, innerRecordId, validity,
			priceWithoutVat, priceWithVat,
			sellable,
			formerPrice, innerRecordHandling,
			internalIdSupplier
		);
	}

	/**
	 * Method handles inserting or updating price in index according the changes in passed arguments. Update is executed
	 * as removal of previously stored value and inserting new price information to the indexes again.
	 */
	static void priceUpsert(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex entityIndex,
		@Nonnull PriceKey priceKey,
		@Nullable Integer innerRecordId,
		@Nullable DateTimeRange validity,
		@Nonnull BigDecimal priceWithoutVat,
		@Nonnull BigDecimal priceWithVat, boolean sellable,
		@Nullable PriceWithInternalIds formerPrice,
		@Nonnull PriceInnerRecordHandling innerRecordHandling,
		@Nonnull BiFunction<PriceKey, Integer, PriceInternalIdContainer> internalIdSupplier
	) {
		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.PRICE_INDEX);
		final int indexedPricePlaces = executor.getEntitySchema().getIndexedPricePlaces();
		// remove former price first
		if (formerPrice != null && formerPrice.exists() && formerPrice.isSellable()) {
			entityIndex.priceRemove(
				entityPrimaryKey,
				Objects.requireNonNull(formerPrice.getInternalPriceId()),
				priceKey, innerRecordHandling, formerPrice.getInnerRecordId(),
				formerPrice.getValidity(),
				convertToInt(formerPrice.getPriceWithoutVat(), indexedPricePlaces),
				convertToInt(formerPrice.getPriceWithVat(), indexedPricePlaces)
			);
		}
		// now insert new price
		if (sellable) {
			final PriceInternalIdContainer internalPriceIds = internalIdSupplier.apply(priceKey, innerRecordId);
			final PriceInternalIdContainer priceId = entityIndex.addPrice(
				entityPrimaryKey,
				internalPriceIds.getInternalPriceId(),
				priceKey, innerRecordHandling, innerRecordId,
				validity,
				convertToInt(priceWithoutVat, indexedPricePlaces),
				convertToInt(priceWithVat, indexedPricePlaces)
			);
			executor.getContainerAccessor().registerAssignedPriceId(entityPrimaryKey, priceKey, innerRecordId, priceId);
		}
	}

	/**
	 * Method handles updating price index in the situation when entity price is removed.
	 */
	static void priceRemove(
		@Nonnull EntityIndexLocalMutationExecutor executor, @Nonnull EntityIndex entityIndex,
		@Nonnull PriceKey priceKey
	) {
		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.PRICE_INDEX);
		final PricesStoragePart entityFormerPrices = executor.getContainerAccessor().getPriceStorageContainer(entityPrimaryKey);
		final PriceWithInternalIds formerPrice = entityFormerPrices.getPriceByKey(priceKey);
		final PriceInnerRecordHandling innerRecordHandling = entityFormerPrices.getPriceInnerRecordHandling();

		priceRemove(executor, entityIndex, priceKey, formerPrice, innerRecordHandling);
	}

	/**
	 * Method handles updating price index in the situation when entity price is removed.
	 */
	static void priceRemove(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex entityIndex,
		@Nonnull PriceKey priceKey,
		@Nullable PriceWithInternalIds formerPrice,
		@Nonnull PriceInnerRecordHandling innerRecordHandling) {

		final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.PRICE_INDEX);
		final int indexedPricePlaces = executor.getEntitySchema().getIndexedPricePlaces();

		if (formerPrice != null) {
			if (formerPrice.exists() && formerPrice.isSellable()) {
				entityIndex.priceRemove(
					entityPrimaryKey,
					Objects.requireNonNull(formerPrice.getInternalPriceId()),
					priceKey,
					innerRecordHandling,
					formerPrice.getInnerRecordId(),
					formerPrice.getValidity(),
					convertToInt(formerPrice.getPriceWithoutVat(), indexedPricePlaces),
					convertToInt(formerPrice.getPriceWithVat(), indexedPricePlaces)
				);
			}
		} else {
			throw new IllegalArgumentException("Price " + priceKey + " doesn't exist and cannot be removed!");
		}
	}

	/**
	 * Returns simple function that returns exactly the `price` passed in argument. The function checks whether passed
	 * bi function arguments match the `price` identification and if not exception is thrown.
	 */
	@Nonnull
	static BiFunction<PriceKey, Integer, PriceInternalIdContainer> createPriceProvider(@Nonnull PriceWithInternalIds price) {
		return (priceKey, innerRecordId) -> {
			Assert.isTrue(
				priceKey.equals(price.getPriceKey()) && Objects.equals(innerRecordId, price.getInnerRecordId()),
				() -> new IllegalStateException("Unexpected price call!")
			);
			return price;
		};
	}
}
