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

package io.evitadb.query.extraResult.translator.histogram.producer;

import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.io.extraResult.PriceHistogram;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.facet.UserFilterFormula;
import io.evitadb.query.algebra.price.FilteredPriceRecordAccessor;
import io.evitadb.query.algebra.price.FilteredPriceRecordsLookupResult;
import io.evitadb.query.algebra.price.termination.PriceTerminationFormula;
import io.evitadb.query.algebra.utils.visitor.FormulaCloner;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultProducer;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class contains logic that creates single {@link PriceHistogram} DTO requested
 * by {@link io.evitadb.api.query.require.PriceHistogram} require constraint in input query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class PriceHistogramProducer implements ExtraResultProducer {
	/**
	 * Bucket count contains desired count of histogram columns=buckets. Output histogram bucket count must never exceed
	 * this value, but might be optimized to lower count when there are big gaps between columns.
	 */
	private final int bucketCount;
	/**
	 * Reference to the query context that allows to access entity bodies.
	 */
	@Nonnull private final QueryContext queryContext;
	/**
	 * Contains filtering formula tree that was used to produce results so that computed sub-results can be used for
	 * sorting.
	 */
	@Nonnull private final Formula filteringFormula;
	/**
	 * Contains list of all {@link FilteredPriceRecordAccessor} formulas that allow access to the {@link PriceRecord}
	 * used in filtering formula processing.
	 */
	@Nonnull private final Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors;
	/**
	 * Contains existing {@link FilteredPriceRecordsLookupResult} if it was already produced by filtering or sorter logic.
	 * We can reuse already computed data in this producer and save precious ticks.
	 */
	@Nullable private final FilteredPriceRecordsLookupResult priceRecordsLookupResult;

	@Nullable
	@Override
	public <T extends Serializable> EvitaResponseExtraResult fabricate(@Nonnull List<T> entities) {
		// contains flag whether there was at least one formula with price predicate that filtered out some entity pks
		final AtomicBoolean filteredRecordsFound = new AtomicBoolean();
		// create clone of the current formula in a such way that all price termination formulas within user filter
		// that filtered out entity primary keys based on price predicate (price between constraint) produce just
		// the excluded records - this way we can compute remainder to the current filtering result and get all data
		// for price histogram ignoring the price between filtering constraint
		final Formula formulaWithFilteredOutResults = FormulaCloner.clone(
			filteringFormula, (formulaCloner, formula) -> {
				if (formula instanceof PriceTerminationFormula) {
					if (formulaCloner.isWithin(UserFilterFormula.class)) {
						final PriceTerminationFormula priceTerminationFormula = (PriceTerminationFormula) formula;
						final Bitmap filteredOutRecords = priceTerminationFormula.getRecordsFilteredOutByPredicate();
						Assert.isTrue(
							filteredOutRecords != null,
							() -> new IllegalStateException("Compute was not yet called on price termination formula, this is not expected!")
						);
						if (filteredOutRecords.isEmpty()) {
							return EmptyFormula.INSTANCE;
						} else {
							filteredRecordsFound.set(true);
							return priceTerminationFormula.getCloneWithPricePredicateFilteredOutResults();
						}
					} else {
						return formula;
					}
				} else {
					return formula;
				}
			}
		);

		final HistogramContract optimalHistogram = queryContext.analyse(
			new PriceHistogramComputer(
				bucketCount,
				queryContext.getSchema().getIndexedPricePlaces(),
				queryContext.getQueryPriceMode(),
				filteringFormula,
				filteredRecordsFound.get() ? formulaWithFilteredOutResults : null,
				filteredPriceRecordAccessors, priceRecordsLookupResult
			)
		).compute();
		if (optimalHistogram == HistogramContract.EMPTY) {
			return null;
		} else {
			// create histogram DTO for the output
			return new PriceHistogram(optimalHistogram);
		}
	}

}