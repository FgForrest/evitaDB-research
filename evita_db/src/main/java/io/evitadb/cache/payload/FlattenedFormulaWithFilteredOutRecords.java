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

package io.evitadb.cache.payload;

import io.evitadb.api.utils.MemoryMeasuringConstants;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.price.termination.PlainPriceTerminationFormulaWithPriceFilter;
import io.evitadb.query.algebra.price.termination.PriceEvaluationContext;
import io.evitadb.query.algebra.price.termination.PriceTerminationFormula;
import io.evitadb.query.algebra.price.termination.PriceWrappingFormula;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Flattened formula represents a memoized form of original formula that contains already computed bitmap of results.
 * This variant of flattened formula keeps computed bitmap of integers and also information about
 * {@link PriceEvaluationContext} and bitmap of entity primary keys that were filtered out by the original formula.
 * Formula is used from {@link PlainPriceTerminationFormulaWithPriceFilter}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class FlattenedFormulaWithFilteredOutRecords extends FlattenedFormula implements PriceTerminationFormula, Formula {
	private static final long serialVersionUID = -1357022866282833762L;
	/**
	 * Records that has been filtered out by the original formula. Copies {@link PriceTerminationFormula#getRecordsFilteredOutByPredicate()}.
	 */
	@Getter @Nonnull private final Bitmap recordsFilteredOutByPredicate;
	/**
	 * Price evaluation context. Copies {@link PriceWrappingFormula#getPriceEvaluationContext()}.
	 */
	@Getter @Nonnull private final PriceEvaluationContext priceEvaluationContext;

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	public static int estimateSize(@Nonnull long[] transactionalIds, @Nonnull Bitmap computationalResult, @Nonnull Bitmap recordsFilteredOutByPredicate, @Nonnull PriceEvaluationContext priceEvaluationContext) {
		return FlattenedFormula.estimateSize(transactionalIds, computationalResult) +
			RoaringBitmapBackedBitmap.getRoaringBitmap(recordsFilteredOutByPredicate).getSizeInBytes() +
			priceEvaluationContext.estimateSize();
	}

	public FlattenedFormulaWithFilteredOutRecords(long formulaHash, long transactionalIdHash, @Nonnull long[] originalBitmapIds, @Nonnull Bitmap memoizedResult, @Nonnull Bitmap recordsFilteredOutByPredicate, @Nonnull PriceEvaluationContext priceEvaluationContext) {
		super(formulaHash, transactionalIdHash, originalBitmapIds, memoizedResult);
		this.recordsFilteredOutByPredicate = recordsFilteredOutByPredicate;
		this.priceEvaluationContext = priceEvaluationContext;
	}

	@Nonnull
	@Override
	public Formula getCloneWithPricePredicateFilteredOutResults() {
		return new FlattenedFormulaWithFilteredOutRecords(
			recordHash, transactionalIdHash, transactionalDataIds, recordsFilteredOutByPredicate,
			recordsFilteredOutByPredicate, priceEvaluationContext
		);
	}

}
