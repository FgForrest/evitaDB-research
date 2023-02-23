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

package io.evitadb.storage.model.storageParts.entity;

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price.PriceIdFirstPriceKeyComparator;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ArrayUtils.InsertionPosition;
import io.evitadb.index.price.model.internalId.MinimalPriceInternalIdContainer;
import io.evitadb.index.price.model.internalId.PriceInternalIdContainer;
import io.evitadb.index.price.model.internalId.PriceWithInternalIds;
import io.evitadb.storage.model.storageParts.EntityStoragePart;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * This container class represents {@link Prices} of single {@link Entity}. Contains {@link PriceInnerRecordHandling}
 * information and all {@link PriceContract prices} connected to the entity.
 *
 * Although query allows to fetch prices valid only in certain moment / price list / currency, all prices are stored
 * in single storage container because the data are expected to be small.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@ToString(of = {"entityPrimaryKey", "priceInnerRecordHandling"})
public class PricesStoragePart implements EntityStoragePart {
	private static final long serialVersionUID = 3489626529648601062L;
	private static final PriceWithInternalIds[] EMPTY_PRICES = new PriceWithInternalIds[0];

	/**
	 * Id used for lookups in {@link io.evitadb.storage.MemTable} for this particular container.
	 */
	@Getter private final int entityPrimaryKey;
	/**
	 * See {@link Prices#getVersion()}.
	 */
	private final int version;
	/**
	 * See {@link Prices#getPriceInnerRecordHandling()}.
	 */
	@Getter private PriceInnerRecordHandling priceInnerRecordHandling = PriceInnerRecordHandling.NONE;
	/**
	 * See {@link Prices#getPrices()}. Prices are sorted in ascending order according to {@link PriceKey} comparator.
	 */
	@Getter private PriceWithInternalIds[] prices = EMPTY_PRICES;
	/**
	 * Contains true if anything changed in this container.
	 */
	@Getter private boolean dirty;

	public PricesStoragePart(int entityPrimaryKey) {
		this.entityPrimaryKey = entityPrimaryKey;
		this.version = 0;
	}

	public PricesStoragePart(int entityPrimaryKey, int version, @Nonnull PriceInnerRecordHandling priceInnerRecordHandling, @Nonnull PriceWithInternalIds[] prices) {
		this.entityPrimaryKey = entityPrimaryKey;
		this.version = version;
		this.priceInnerRecordHandling = priceInnerRecordHandling;
		this.prices = prices;
	}

	@Nullable
	@Override
	public Long getUniquePartId() {
		return (long) entityPrimaryKey;
	}

	@Override
	public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
		return entityPrimaryKey;
	}

	/**
	 * Returns inner data wrapped to {@link Prices} object that can be wired to {@link Entity}.
	 */
	public Prices getAsPrices() {
		return new Prices(
			version, Arrays.stream(prices).collect(Collectors.toList()), priceInnerRecordHandling
		);
	}

	/**
	 * Sets {@link PriceInnerRecordHandling} strategy for this entity.
	 */
	public void setPriceInnerRecordHandling(PriceInnerRecordHandling priceInnerRecordHandling) {
		if (this.priceInnerRecordHandling != priceInnerRecordHandling) {
			this.priceInnerRecordHandling = priceInnerRecordHandling;
			this.dirty = true;
		}
	}

	/**
	 * Adds new or replaces existing price of the entity.
	 */
	public void replaceOrAddPrice(
		@Nonnull PriceKey priceKey,
		UnaryOperator<PriceContract> mutator,
		@Nonnull ToIntFunction<PriceKey> internalPriceIdResolver
	) {
		final InsertionPosition insertionPosition = ArrayUtils.computeInsertPositionOfObjInOrderedArray(
			this.prices, priceKey,
			(examinedPrice, pk) -> PriceIdFirstPriceKeyComparator.INSTANCE.compare(examinedPrice.getPriceKey(), pk)
		);
		final int position = insertionPosition.getPosition();
		if (insertionPosition.isAlreadyPresent()) {
			final PriceWithInternalIds existingContract = this.prices[position];
			final PriceContract updatedPriceContract = mutator.apply(existingContract);
			if (this.prices[position].differsFrom(updatedPriceContract)) {
				this.prices[position] = new PriceWithInternalIds(
					updatedPriceContract,
					updatedPriceContract.isSellable() ?
						requireNonNull(
							ofNullable(existingContract.getInternalPriceId())
								.orElseGet(() -> internalPriceIdResolver.applyAsInt(priceKey))
						) : null
				);
				this.dirty = true;
			}
		} else {
			final PriceContract newPrice = mutator.apply(null);
			final Integer internalPriceId;
			if (newPrice.isSellable()) {
				internalPriceId = internalPriceIdResolver.applyAsInt(priceKey);
			} else {
				internalPriceId = null;
			}
			this.prices = ArrayUtils.insertRecordIntoArray(
				new PriceWithInternalIds(newPrice, internalPriceId),
				this.prices,
				position
			);
			this.dirty = true;
		}
	}

	/**
	 * Returns a price by its key, NULL if not present.
	 */
	@Nullable
	public PriceWithInternalIds getPriceByKey(@Nonnull PriceKey priceKey) {
		final int index = ArrayUtils.binarySearch(
			this.prices, priceKey,
			(examinedPrice, pk) -> PriceIdFirstPriceKeyComparator.INSTANCE.compare(examinedPrice.getPriceKey(), pk)
		);
		return index >= 0 ? this.prices[index] : null;
	}

	/**
	 * Finds already assigned internal price identificator for combination of `priceKey`. If no id is found the returned
	 * {@link PriceInternalIdContainer} contains nulls in its field.
	 */
	@Nonnull
	public PriceInternalIdContainer findExistingInternalIds(@Nonnull PriceKey priceKey) {
		Integer internalPriceId = null;
		for (PriceWithInternalIds price : prices) {
			if (Objects.equals(priceKey, price.getPriceKey())) {
				internalPriceId = price.getInternalPriceId();
				break;
			}
		}
		return new MinimalPriceInternalIdContainer(internalPriceId);
	}

	/**
	 * Returns version of the entity for storing (incremented by one, if anything changed).
	 */
	public int getVersion() {
		return dirty ? version + 1 : version;
	}

}
