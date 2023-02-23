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
import io.evitadb.api.io.extraResult.AttributeHistogram;
import io.evitadb.api.io.extraResult.Histogram;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.cache.CacheSupervisor;
import io.evitadb.index.array.CompositeObjectArray;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.index.histogram.HistogramBucket;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.base.AndFormula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.OrFormula;
import io.evitadb.query.algebra.facet.UserFilterFormula;
import io.evitadb.query.algebra.utils.visitor.FormulaCloner;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.extraResult.translator.histogram.FilterFormulaAttributeOptimizeVisitor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.openhft.hashing.LongHashFunction;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains logic that creates single {@link AttributeHistogram} DTO containing {@link Histogram} for all
 * attributes requested by {@link io.evitadb.api.query.require.AttributeHistogram} require constraint in input query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class AttributeHistogramProducer implements ExtraResultProducer {
	/**
	 * Type of the queried entity - {@link EntitySchema#getName()}.
	 */
	private final Serializable entityType;
	/**
	 * Reference to the query context that allows to access entity bodies.
	 */
	private final QueryContext queryContext;
	/**
	 * Bucket count contains desired count of histogram columns=buckets. Output histogram bucket count must never exceed
	 * this value, but might be optimized to lower count when there are big gaps between columns.
	 */
	private final int bucketCount;
	/**
	 * Contains filtering formula tree that was used to produce results so that computed sub-results can be used for
	 * sorting.
	 */
	@Nonnull private final Formula filterFormula;
	/**
	 * Contains list of requests for attribute histograms. Requests contains all data necessary for histogram computation.
	 */
	private final Map<String, AttributeHistogramRequest> histogramRequests = new HashMap<>();
	/**
	 * Formula supervisor is an entry point to the Evita cache. The idea is that each {@link Formula} can be identified by
	 * its {@link Formula#computeHash(LongHashFunction)} method and when the supervisor identifies that certain formula
	 * is frequently used in query formulas it moves its memoized results to the cache. The non-computed formula
	 * of the same hash will be exchanged in next query that contains it with the cached formula that already contains
	 * memoized result.
	 */
	private final CacheSupervisor cacheSupervisor;

	/**
	 * Method combines arrays of {@link HistogramBucket} (i.e. two-dimensional matrix) together so that in the output
	 * array the buckets are flattened to one-dimensional representation containing only distinct {@link HistogramBucket#getValue()}
	 * in a way that two or more {@link HistogramBucket} sharing same {@link HistogramBucket#getValue()} are combined
	 * into a single bucket.
	 *
	 * The bucket record ids are also filtered to match `filteringFormula` output (i.e. the bucket will not contain a
	 * record that is not part of the `filteringFormula` output). Empty buckets are discarded along the way.
	 */
	static <T extends Comparable<T>> HistogramBucket<T>[] getCombinedAndFilteredBucketArray(
		@Nonnull Formula filteringFormula,
		@Nonnull HistogramBucket<T>[][] histogramBitmaps
	) {
		if (ArrayUtils.isEmpty(histogramBitmaps)) {
			//noinspection unchecked
			return new HistogramBucket[0];
		}

		// prepare filtering bitmap
		final RoaringBitmap filteredRecordIds = RoaringBitmapBackedBitmap.getRoaringBitmap(filteringFormula.compute());
		// prepare output elastic array
		@SuppressWarnings("rawtypes") final CompositeObjectArray<HistogramBucket> finalBuckets = new CompositeObjectArray<>(HistogramBucket.class, false);

		// now create utility arrays that get reused during computation
		if (histogramBitmaps.length > 1) {
			// indexes contain last index visited in each input HistogramBucket array
			final int[] indexes = new int[histogramBitmaps.length];
			// incIndexes contains index in `indexes` array that should be incremented at the end of the loop
			final int[] incIndexes = new int[histogramBitmaps.length];
			// combination pack contains histogram buckets with same value which records should be combined
			@SuppressWarnings("unchecked") final HistogramBucket<T>[] combinationPack = new HistogramBucket[histogramBitmaps.length];

			do {
				// this peek signalizes index of the last position in incIndexes / combinationPack that are filled with data
				int combinationPackPeek = 0;
				T minValue = null;
				for (int i = 0; i < indexes.length; i++) {
					int index = indexes[i];
					if (index > -1) {
						final HistogramBucket<T> examinedBucket = histogramBitmaps[i][index];
						final T histogramValue = examinedBucket.getValue();

						// is the value same as min value found in this iteration?
						final int comparisonResult = minValue == null ? -1 : histogramValue.compareTo(minValue);
						// we found new `minValue` int the loop
						if (comparisonResult < 0) {
							// reset peek variable to zero (start writing from scratch)
							combinationPackPeek = 0;
							// remember min value
							minValue = examinedBucket.getValue();
							// add this bucket as first bucket of combination pack
							combinationPack[combinationPackPeek] = examinedBucket;
							// remember we need to increase index in `indexes` for this bucket array at the end of the loop
							incIndexes[combinationPackPeek] = i;
						} else if (comparisonResult == 0) {
							// we found same value as current min value
							// add this bucket as next bucket of combination pack
							combinationPack[++combinationPackPeek] = examinedBucket;
							// remember we need to increase index in `indexes` for this bucket array at the end of the loop
							incIndexes[combinationPackPeek] = i;
						}
					}
				}
				// if the peek value is zero - we have only one bucket with the distinct value
				if (combinationPackPeek == 0) {
					addBucket(filteredRecordIds, finalBuckets, combinationPack[0]);
					incrementBitmapIndex(histogramBitmaps, indexes, incIndexes[0]);
				} else {
					// if larger than zero we need to combine multiple buckets, but no more than current combination pack peek
					addBucket(filteredRecordIds, finalBuckets, Arrays.copyOfRange(combinationPack, 0, combinationPackPeek + 1));
					incrementBitmapIndex(histogramBitmaps, indexes, Arrays.copyOfRange(incIndexes, 0, combinationPackPeek + 1));
				}
			} while (endNotReached(indexes));
		} else if (histogramBitmaps.length == 1) {
			// go the fast route
			for (HistogramBucket<T> bucket : histogramBitmaps[0]) {
				addBucket(filteredRecordIds, finalBuckets, bucket);
			}
		}

		//noinspection unchecked
		return finalBuckets.toArray();
	}

	/**
	 * End is reached when all indexes contain -1 value.
	 */
	private static boolean endNotReached(int[] indexes) {
		for (int index : indexes) {
			if (index > -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method combines all `theBucket` into a single bucket with shared distinct {@link HistogramBucket#getValue()}.
	 * Record ids are combined by OR relation and then filtered by AND relation with `filteredRecordIds`.
	 */
	@SuppressWarnings("rawtypes")
	private static <T extends Comparable<T>> void addBucket(
		@Nonnull RoaringBitmap filteredRecordIds,
		@Nonnull CompositeObjectArray<HistogramBucket> finalBuckets,
		@Nonnull HistogramBucket<T>[] theBucket
	) {
		final BaseBitmap recordIds = new BaseBitmap(
			RoaringBitmap.and(
				filteredRecordIds,
				RoaringBitmap.or(
					Arrays.stream(theBucket)
						.map(it -> RoaringBitmapBackedBitmap.getRoaringBitmap(it.getRecordIds()))
						.toArray(RoaringBitmap[]::new)
				)
			)
		);
		if (!recordIds.isEmpty()) {
			finalBuckets.add(
				new HistogramBucket<>(
					theBucket[0].getValue(),
					recordIds
				)
			);
		}
	}

	/**
	 * Method filters out record ids of the {@link HistogramBucket} that are not part of `filteredRecordIds` and
	 * produces new bucket with filtered data.
	 */
	@SuppressWarnings("rawtypes")
	private static <T extends Comparable<T>> void addBucket(
		@Nonnull RoaringBitmap filteredRecordIds,
		@Nonnull CompositeObjectArray<HistogramBucket> finalBuckets,
		@Nonnull HistogramBucket<T> theBucket
	) {
		final BaseBitmap recordIds = new BaseBitmap(
			RoaringBitmap.and(
				filteredRecordIds,
				RoaringBitmapBackedBitmap.getRoaringBitmap(theBucket.getRecordIds())
			)
		);
		if (!recordIds.isEmpty()) {
			finalBuckets.add(
				new HistogramBucket<>(
					theBucket.getValue(),
					recordIds
				)
			);
		}
	}

	/**
	 * Method increments indexes in `indexes` by one, if they match index in `bitmapIndexes` array. If the index exceeds
	 * the number of elements in respective `histogramBitmap`, the index is set to -1 which marks end of the stream.
	 */
	private static <T extends Comparable<T>> void incrementBitmapIndex(
		@Nonnull HistogramBucket<T>[][] histogramBitmaps,
		@Nonnull int[] indexes,
		@Nonnull int[] bitmapIndexes
	) {
		for (int bitmapIndex : bitmapIndexes) {
			incrementBitmapIndex(histogramBitmaps, indexes, bitmapIndex);
		}
	}

	/**
	 * Method increment number (index) in `indexes` array on position `bitmapIndex` by one. If the index reaches number
	 * of available records in `histogramBitmap` on `bitmapIndex`, the index is set to -1 which marks end of the stream.
	 */
	private static <T extends Comparable<T>> void incrementBitmapIndex(
		@Nonnull HistogramBucket<T>[][] histogramBitmaps,
		@Nonnull int[] indexes,
		int bitmapIndex
	) {
		if (histogramBitmaps[bitmapIndex].length == indexes[bitmapIndex] + 1) {
			indexes[bitmapIndex] = -1;
		} else {
			indexes[bitmapIndex]++;
		}
	}

	/**
	 * Adds a request for histogram computation passing all data necessary for the computation.
	 * Method doesn't compute the histogram - just registers the requirement to be resolved later
	 * in the {@link ExtraResultProducer#fabricate(List)} )}  method.
	 */
	public void addAttributeHistogramRequest(
		@Nonnull AttributeSchema attributeSchema,
		@Nonnull List<FilterIndex> attributeIndexes,
		@Nullable List<AttributeFormula> attributeFormulas
	) {
		final Set<Formula> formulaSet;
		if (attributeFormulas == null) {
			formulaSet = Collections.emptySet();
		} else {
			formulaSet = Collections.newSetFromMap(new IdentityHashMap<>());
			formulaSet.addAll(attributeFormulas);
		}
		this.histogramRequests.put(
			attributeSchema.getName(),
			new AttributeHistogramRequest(
				attributeSchema,
				attributeIndexes,
				formulaSet
			)
		);
	}

	@Nullable
	@Override
	public <T extends Serializable> EvitaResponseExtraResult fabricate(@Nonnull List<T> entities) {
		// create optimized formula that offers best memoized intermediate results reuse
		final Formula optimizedFormula = FilterFormulaAttributeOptimizeVisitor.optimize(filterFormula, histogramRequests.keySet());
		// create clone of the optimized formula without user filter contents
		final Formula baseFormulaWithoutUserFilter = FormulaCloner.clone(
			optimizedFormula,
			formula -> formula instanceof UserFilterFormula ? null : formula
		);

		// compute attribute histogram
		return new AttributeHistogram(
			// for each histogram request
			histogramRequests.entrySet()
				.stream()
				// check whether it produces any results with mandatory filter, and if not skip its production
				.filter(entry -> hasSenseWithMandatoryFilter(baseFormulaWithoutUserFilter, entry.getValue()))
				.map(entry -> {
					final AttributeHistogramRequest histogramRequest = entry.getValue();
					final HistogramContract optimalHistogram = queryContext.analyse(
						entityType,
						new AttributeHistogramComputer(optimizedFormula, bucketCount, histogramRequest)
					).compute();
					if (optimalHistogram == HistogramContract.EMPTY) {
						return null;
					} else {
						// create histogram DTO for the output
						return new AttributeHistogramWrapper(
							entry.getKey(),
							optimalHistogram
						);
					}
				})
				.filter(Objects::nonNull)
				.collect(
					Collectors.toMap(
						AttributeHistogramWrapper::getAttributeName,
						AttributeHistogramWrapper::getHistogram
					)
				)
		);
	}

	/**
	 * If we combine all records for the attribute in filter indexes with current filtering formula result - is there
	 * at least single entity primary key left?
	 */
	private boolean hasSenseWithMandatoryFilter(
		@Nonnull Formula filteringFormula,
		@Nonnull AttributeHistogramRequest request
	) {
		// collect all records from the filter indexes for this attribute
		final Bitmap[] histogramBitmaps = request
			.getAttributeIndexes()
			.stream()
			.map(FilterIndex::getAllRecords)
			.toArray(Bitmap[]::new);
		// filter out attributes that don't make sense even with mandatory filtering constraints
		final Formula histogramBitmapsFormula;
		if (histogramBitmaps.length == 0) {
			return false;
		} else if (histogramBitmaps.length == 1) {
			histogramBitmapsFormula = new ConstantFormula(histogramBitmaps[0]);
		} else {
			final long[] indexTransactionIds = request.getAttributeIndexes()
				.stream()
				.mapToLong(FilterIndex::getId)
				.toArray();
			histogramBitmapsFormula = new OrFormula(indexTransactionIds, histogramBitmaps);
		}
		return !new AndFormula(
			histogramBitmapsFormula,
			filteringFormula
		)
			.compute()
			.isEmpty();
	}

	/**
	 * DTO that aggregates all data necessary for computing histogram for single attribute.
	 */
	@Data
	public static class AttributeHistogramRequest {
		/**
		 * Refers to attribute schema.
		 */
		@Nonnull private final AttributeSchema attributeSchema;
		/**
		 * Refers to all filter indexes that map entity primary keys and their associated values for this attribute.
		 */
		@Nonnull private final List<FilterIndex> attributeIndexes;
		/**
		 * Contains set of formulas in current filtering query that target this attribute.
		 */
		@Nonnull private final Set<Formula> attributeFormulas;

		/**
		 * Returns name of the attribute.
		 */
		@Nonnull
		public String getAttributeName() {
			return attributeSchema.getName();
		}

		/**
		 * Returns number of maximum decimal places allowed for this attribute.
		 */
		public int getDecimalPlaces() {
			return attributeSchema.getIndexedDecimalPlaces();
		}

	}

	/**
	 * Simple tuple for passing data in stream.
	 */
	@Data
	private static class AttributeHistogramWrapper {
		private final String attributeName;
		private final HistogramContract histogram;
	}

}
