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

package io.evitadb.index.histogram;

import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.BiFunction;

/**
 * Histogram subset is a slice of the original histogram that references all key data. Slices can be combined together,
 * provide useful statistical information such as min/max or can output all record ids in the entire subset.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class HistogramSubSet<T extends Comparable<T>> {
	private final long indexTransactionId;
	@Getter private final HistogramBucket<T>[] histogramBuckets;
	private final BiFunction<Long, HistogramBucket<T>[], Formula> aggregationLambda;
	private Formula memoizedResult;

	/**
	 * Returns record ids of all buckets in this histogram subset as single bitmap (ordered distinct array).
	 * For aggregation of record ids of different buckets {@link #aggregationLambda} is used. Result of this call
	 * is memoized so that additional calls are cheap and returns already computed result.
	 */
	public Bitmap getRecordIds() {
		return getFormula().compute();
	}

	/**
	 * Returns formula for computing record ids of all buckets in this histogram subset as single bitmap (ordered
	 * distinct array). For aggregation of record ids of different buckets {@link #aggregationLambda} is used.
	 * Result of this call is memoized so that additional calls are cheap and returns already computed result.
	 */
	public Formula getFormula() {
		if (memoizedResult == null) {
			this.memoizedResult = histogramBuckets.length == 0 ?
				EmptyFormula.INSTANCE : aggregationLambda.apply(indexTransactionId, histogramBuckets);
		}
		return memoizedResult;
	}

	/**
	 * Returns true if this histogram subset contains no buckets / no record ids.
	 */
	public boolean isEmpty() {
		return ArrayUtils.isEmpty(histogramBuckets);
	}

	/**
	 * Returns minimal {@link HistogramBucket#getValue()} of buckets in this histogram subset.
	 */
	public Comparable<?> getMinimalValue() {
		return isEmpty() ? null : histogramBuckets[0].getValue();
	}

	/**
	 * Returns maximal {@link HistogramBucket#getValue()} of buckets in this histogram subset.
	 */
	public Comparable<?> getMaximalValue() {
		return isEmpty() ? null : histogramBuckets[histogramBuckets.length - 1].getValue();
	}
}
