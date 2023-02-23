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

package io.evitadb.query.sort.price;

import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.price.FilteredPriceRecordAccessor;
import io.evitadb.query.algebra.price.FilteredPriceRecordsLookupResult;
import io.evitadb.query.algebra.price.termination.SumPriceTerminationFormula;
import io.evitadb.query.sort.Sorter;
import io.evitadb.query.sort.utils.SortUtils;
import lombok.Getter;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * This sorter implementation executes sorting by price according to passed {@link SortOrder} and {@link QueryPriceMode}.
 * Unfortunately there is no way how to use presorted datasets because the inner workings of the prices is too complicated
 * (there may be multiple prices for the same entity even in the same price list - for example with different time validity,
 * and correct price matters). The sorter also works with "virtual" prices that get created by {@link SumPriceTerminationFormula}
 * and contain accumulated price for all inner records. This is the second argument why pre-sorted prices are problematic
 * to be used.
 *
 * Sorter outputs set of entity ids sorted by price.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FilteredPricesSorter implements Sorter {
	/**
	 * Comparator sorts {@link PriceRecord} by price with VAT in ascending order.
	 */
	protected static final Comparator<PriceRecordContract> ASC_PRICE_WITH_VAT = Comparator.comparingInt(PriceRecordContract::getPriceWithVat);
	/**
	 * Comparator sorts {@link PriceRecord} by price without VAT in ascending order.
	 */
	protected static final Comparator<PriceRecordContract> ASC_PRICE_WITHOUT_VAT = Comparator.comparingInt(PriceRecordContract::getPriceWithoutVat);
	/**
	 * Comparator sorts {@link PriceRecord} by price with VAT in descending order.
	 */
	protected static final Comparator<PriceRecordContract> DESC_PRICE_WITH_VAT = (o1, o2) -> Integer.compare(o2.getPriceWithVat(), o1.getPriceWithVat());
	/**
	 * Comparator sorts {@link PriceRecord} by price without VAT in descending order.
	 */
	protected static final Comparator<PriceRecordContract> DESC_PRICE_WITHOUT_VAT = (o1, o2) -> Integer.compare(o2.getPriceWithoutVat(), o1.getPriceWithoutVat());
	/**
	 * This sorter instance will be used for sorting entities, that cannot be sorted by this sorter.
	 */
	protected final Sorter unknownRecordIdsSorter;
	/**
	 * This collection contains list of {@link FilteredPriceRecordAccessor} that were used in the filtering constraint
	 * and already posses limited set of {@link PriceRecord} data that can be used for sorting. We want to avoid sorting
	 * excessive large data sets and using already prefiltered records allows it.
	 */
	private final Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors;
	/**
	 * Contains comparator that will be used for sorting price records (initialized in constructor).
	 */
	private final Comparator<PriceRecordContract> priceRecordComparator;
	/**
	 * Contains DTO that holds array of all {@link PriceRecord} that match entity primary keys produced by filtering
	 * formula and also array of entity primary keys that are not linked to any price.
	 */
	@Getter private FilteredPriceRecordsLookupResult priceRecordsLookupResult;

	public FilteredPricesSorter(SortOrder sortOrder, QueryPriceMode queryPriceMode, Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors) {
		this.unknownRecordIdsSorter = null;
		this.priceRecordComparator = getPriceRecordComparator(sortOrder, queryPriceMode);
		this.filteredPriceRecordAccessors = filteredPriceRecordAccessors;
		Assert.isTrue(!filteredPriceRecordAccessors.isEmpty(), "Price translate formulas must not be empty!");
	}

	private FilteredPricesSorter(Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors, Comparator<PriceRecordContract> priceRecordComparator, Sorter unknownRecordIdsSorter) {
		this.unknownRecordIdsSorter = unknownRecordIdsSorter;
		this.filteredPriceRecordAccessors = filteredPriceRecordAccessors;
		this.priceRecordComparator = priceRecordComparator;
	}

	@Nonnull
	@Override
	public Sorter andThen(Sorter sorterForUnknownRecords) {
		return new FilteredPricesSorter(
			filteredPriceRecordAccessors,
			priceRecordComparator,
			sorterForUnknownRecords
		);
	}

	@Nullable
	@Override
	public Sorter getNextSorter() {
		return unknownRecordIdsSorter;
	}

	@Override
	public int[] sortAndSlice(@Nonnull Formula input, int startIndex, int endIndex) {
		// compute entire set of entity pks that needs to be sorted
		final Bitmap computeResult = input.compute();
		final RoaringBitmap computeResultBitmap = RoaringBitmapBackedBitmap.getRoaringBitmap(computeResult);
		// collect price records from the filtering formulas
		priceRecordsLookupResult = new FilteredPriceRecordsCollector(
			computeResultBitmap, filteredPriceRecordAccessors
		).getResult();

		// now sort filtered prices by passed comparator
		final PriceRecordContract[] translatedResult = priceRecordsLookupResult.getPriceRecords();
		Arrays.sort(translatedResult, getPriceRecordComparator());

		// slice the output and cut appropriate page from it
		final int pageSize = endIndex - startIndex;
		final int[] sortedPricesResult = Arrays.stream(translatedResult)
			.skip(startIndex)
			.limit(pageSize)
			.mapToInt(PriceRecordContract::getEntityPrimaryKey)
			.toArray();

		// if the output is not complete, and we have not found entity PKs
		final int[] notFoundEntities = priceRecordsLookupResult.getNotFoundEntities();
		if (sortedPricesResult.length < pageSize && (notFoundEntities == null || notFoundEntities.length > 0)) {
			// combine sorted result with the unknown rest using additional sorter or default own
			return appendSortedUnknownEntityPks(
				computeResult, computeResultBitmap,
				notFoundEntities,
				sortedPricesResult,
				startIndex, endIndex, pageSize
			);
		} else {
			return sortedPricesResult;
		}
	}

	/**
	 * Creates {@link PriceRecord} comparator for sorting according to input `sortOrder` and `queryPriceMode`.
	 */
	private Comparator<PriceRecordContract> getPriceRecordComparator(SortOrder sortOrder, QueryPriceMode queryPriceMode) {
		switch (sortOrder) {
			case ASC:
				return queryPriceMode == QueryPriceMode.WITH_VAT ? ASC_PRICE_WITH_VAT : ASC_PRICE_WITHOUT_VAT;
			case DESC:
				return queryPriceMode == QueryPriceMode.WITH_VAT ? DESC_PRICE_WITH_VAT : DESC_PRICE_WITHOUT_VAT;
			default:
				throw new IllegalStateException("Unknown sort type: " + sortOrder);
		}
	}

	/**
	 * Method fills the missing gap for requested page with unknown entities sorted by {@link #unknownRecordIdsSorter}
	 * or by default in ascending order of PKs.
	 */
	@Nonnull
	private int[] appendSortedUnknownEntityPks(@Nonnull Bitmap computeResult, @Nonnull RoaringBitmap computeResultBitmap, @Nullable int[] notFoundArray, @Nonnull int[] sortedPricesResult, int startIndex, int endIndex, int pageSize) {
		// prepare combined result
		final int[] combinedResult = new int[Math.min(pageSize, computeResult.size())];
		int peak = sortedPricesResult.length;
		// write all entity ids from the price sorted array
		System.arraycopy(sortedPricesResult, 0, combinedResult, 0, peak);

		// compute the rest we need to fill in
		final int recomputedStartIndex = Math.max(0, startIndex - peak);
		final int recomputedEndIndex = Math.max(0, endIndex - peak);
		if (notFoundArray == null) {
			if (unknownRecordIdsSorter != null) {
				// use provided unknown sorter to sort the rest and copty the result to the output
				final int[] missingRecords = unknownRecordIdsSorter.sortAndSlice(
					new ConstantFormula(computeResult),
					recomputedStartIndex, recomputedEndIndex
				);
				System.arraycopy(missingRecords, 0, combinedResult, peak, missingRecords.length);
				peak += missingRecords.length;
			} else {
				// copy the not found ids sorted by PK asc in to the result
				peak = SortUtils.appendNotFoundResult(
					combinedResult, peak, recomputedStartIndex, recomputedEndIndex, computeResultBitmap
				);
			}
		} else {
			if (unknownRecordIdsSorter != null) {
				// use provided unknown sorter to sort the rest and copty the result to the output
				final int[] missingRecords = unknownRecordIdsSorter.sortAndSlice(
					new ConstantFormula(new BaseBitmap(notFoundArray)),
					recomputedStartIndex, recomputedEndIndex
				);
				System.arraycopy(missingRecords, 0, combinedResult, peak, missingRecords.length);
				peak += missingRecords.length;
			} else {
				// copy the not found ids sorted by PK asc in to the result
				peak = SortUtils.appendNotFoundResult(
					combinedResult, peak, recomputedStartIndex, recomputedEndIndex, notFoundArray
				);
			}
		}
		// final result is the combined result, from start to peak
		return Arrays.copyOf(combinedResult, peak);
	}

	private Comparator<PriceRecordContract> getPriceRecordComparator() {
		return priceRecordComparator;
	}

}
