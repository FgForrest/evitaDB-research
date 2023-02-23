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

package io.evitadb.query.filter.translator.price.alternative;

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityDecorator;
import io.evitadb.api.function.TriFunction;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.query.require.PriceFetchMode;
import io.evitadb.api.query.require.Prices;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.utils.NumberUtils;
import io.evitadb.index.array.CompositeObjectArray;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.price.model.priceRecord.CumulatedVirtualPriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.query.algebra.deferred.EntityToBitmapFilter;
import io.evitadb.query.algebra.price.FilteredPriceRecordAccessor;
import io.evitadb.query.algebra.price.filteredPriceRecords.FilteredPriceRecords;
import io.evitadb.query.algebra.price.filteredPriceRecords.FilteredPriceRecords.SortingForm;
import io.evitadb.query.algebra.price.filteredPriceRecords.ResolvedFilteredPriceRecords;
import io.evitadb.query.context.QueryContext;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Implementation of {@link EntityToBitmapFilter} that verifies that the entity has the "selling price" available.
 * The proper "selling price" is derived from the {@link QueryContext} automatically by specifying
 * {@link PriceFetchMode#RESPECTING_FILTER} which in turn retrieves all basic constraints such as price list / currency
 * from the {@link io.evitadb.api.io.EvitaRequest}. Only the price between must be handled locally.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class SellingPriceAvailableBitmapFilter implements EntityToBitmapFilter, FilteredPriceRecordAccessor {
	private static final EntityContentRequire[] ENTITY_CONTENT_REQUIRES = {
		new Prices(PriceFetchMode.RESPECTING_FILTER)
	};

	/**
	 * Internal function that converts {@link PriceContract} from the entity to the {@link PriceRecordContract} that is
	 * used in filtration logic.
	 */
	private final TriFunction<Integer, QueryPriceMode, PriceContract, PriceRecordContract> converter;
	/**
	 * Contains the predicate that must be fulfilled in order selling price is accepted by the filter.
	 */
	private final Predicate<PriceContract> filter;
	/**
	 * Contains array of price records that links to the price ids produced by {@link #filter(QueryContext, List)}
	 * method. This object is available once the {@link #filter(QueryContext, List)} method has been called.
	 */
	private FilteredPriceRecords filteredPriceRecords;

	public SellingPriceAvailableBitmapFilter(int indexedPricePlaces, @Nonnull Predicate<PriceContract> filter) {
		this.converter = (entityPrimaryKey, priceQueryMode, priceContract) -> new CumulatedVirtualPriceRecord(
			entityPrimaryKey,
			priceQueryMode == QueryPriceMode.WITH_VAT ?
				NumberUtils.convertToInt(priceContract.getPriceWithVat(), indexedPricePlaces) :
				NumberUtils.convertToInt(priceContract.getPriceWithoutVat(), indexedPricePlaces),
			priceQueryMode
		);
		this.filter = filter;
	}

	public SellingPriceAvailableBitmapFilter(int indexedPricePlaces) {
		this(indexedPricePlaces, priceContract -> true);
	}

	@Nonnull
	@Override
	public EntityContentRequire[] getRequirements() {
		return ENTITY_CONTENT_REQUIRES;
	}

	@Nonnull
	@Override
	public FilteredPriceRecords getFilteredPriceRecords() {
		return filteredPriceRecords;
	}

	@Nonnull
	@Override
	public Bitmap filter(@Nonnull QueryContext queryContext, @Nonnull List<SealedEntity> entities) {
		final CompositeObjectArray<PriceRecordContract> theFilteredPriceRecords = new CompositeObjectArray<>(PriceRecordContract.class);
		final QueryPriceMode queryPriceMode = queryContext.getQueryPriceMode();
		final BaseBitmap result = new BaseBitmap();
		// iterate over all entities
		for (SealedEntity entity : entities) {
			final EntityDecorator entityDecorator = (EntityDecorator) entity;
			final Integer primaryKey = entity.getPrimaryKey();
			if (entityDecorator.isContextAvailable()) {
				// check whether they have valid selling price (applying filter on price lists and currency)
				Optional.ofNullable(entityDecorator.getSellingPrice(filter))
					// and if there is still selling price add it to the output result
					.ifPresent(it -> {
						theFilteredPriceRecords.add(converter.apply(primaryKey, queryPriceMode, it));
						result.add(primaryKey);
					});
			} else {
				if (entity.getAllSellingPrices().stream().anyMatch(filter)) {
					result.add(primaryKey);
				}
			}

		}
		// memoize valid selling prices for sorting purposes
		this.filteredPriceRecords = new ResolvedFilteredPriceRecords(theFilteredPriceRecords.toArray(), SortingForm.NOT_SORTED);
		// return entity ids having selling prices
		return result;
	}

}
