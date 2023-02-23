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

import io.evitadb.api.io.extraResult.Histogram;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.index.histogram.HistogramBucket;
import io.evitadb.index.histogram.HistogramSubSet;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.algebra.utils.visitor.FormulaCloner;
import io.evitadb.query.extraResult.CacheableEvitaResponseExtraResultComputer;
import io.evitadb.query.extraResult.translator.histogram.cache.FlattenedHistogramComputer;
import io.evitadb.query.extraResult.translator.histogram.producer.AttributeHistogramProducer.AttributeHistogramRequest;
import io.evitadb.query.response.TransactionalDataRelatedStructure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.LongStream;

import static java.util.Optional.ofNullable;

/**
 * DTO that aggregates all data necessary for computing histogram for single attribute.
 */
@RequiredArgsConstructor
public class AttributeHistogramComputer implements CacheableEvitaResponseExtraResultComputer<HistogramContract> {
	/**
	 * Contains reference to the lambda that needs to be executed THE FIRST time the histogram produced by this computer
	 * instance is really computed (and memoized).
	 */
	private final Consumer<CacheableEvitaResponseExtraResultComputer<HistogramContract>> onComputationCallback;
	/**
	 * Contains filtering formula tree that was used to produce results so that computed sub-results can be used for
	 * sorting.
	 */
	@Nonnull private final Formula filterFormula;
	/**
	 * Bucket count contains desired count of histogram columns=buckets. Output histogram bucket count must never exceed
	 * this value, but might be optimized to lower count when there are big gaps between columns.
	 */
	private final int bucketCount;
	/**
	 * Contains original {@link AttributeHistogramRequest} that was collected during query examination.
	 */
	@Nonnull @Getter private final AttributeHistogramRequest request;
	/**
	 * Contains memoized value of {@link #computeHash(LongHashFunction)} method.
	 */
	private Long memoizedHash;
	/**
	 * Contains memoized value of {@link #gatherTransactionalIds()} method.
	 */
	private long[] memoizedTransactionalIds;
	/**
	 * Contains memoized value of {@link #gatherTransactionalIds()} computed hash.
	 */
	private Long memoizedTransactionalIdHash;
	/**
	 * Contains bucket array that contains only entity primary keys that match the {@link #filterFormula}. The array
	 * is initialized during {@link #compute()} method and result is memoized, so it's ensured it's computed only once.
	 */
	private HistogramBucket<?>[] memoizedNarrowedBuckets;
	/**
	 * Contains result - computed histogram. The value is initialized during {@link #compute()} method, and it is
	 * memoized, so it's ensured it's computed only once.
	 */
	private HistogramContract memoizedResult;

	public AttributeHistogramComputer(@Nonnull Formula filterFormula, int bucketCount, @Nonnull AttributeHistogramRequest request) {
		this.filterFormula = filterFormula;
		this.bucketCount = bucketCount;
		this.request = request;
		this.onComputationCallback = null;
	}

	@Override
	public FlattenedHistogramComputer toSerializableResult(long extraResultHash, @Nonnull LongHashFunction hashFunction) {
		return new FlattenedHistogramComputer(
			extraResultHash,
			computeHash(hashFunction),
			Arrays.stream(gatherTransactionalIds())
				.distinct()
				.sorted()
				.toArray(),
			Objects.requireNonNull(compute())
		);
	}

	@Override
	public int getSerializableResultSizeEstimate() {
		return FlattenedHistogramComputer.estimateSize(
			gatherTransactionalIds(),
			compute()
		);
	}

	@Nonnull
	@Override
	public CacheableEvitaResponseExtraResultComputer<HistogramContract> getCloneWithComputationCallback(@Nonnull Consumer<CacheableEvitaResponseExtraResultComputer<HistogramContract>> selfOperator) {
		return new AttributeHistogramComputer(selfOperator, filterFormula, bucketCount, request);
	}

	/**
	 * Method creates instance of {@link HistogramDataCruncher} that computes optimal histogram for the attribute.
	 */
	@Nullable
	private static <T extends Comparable<T>> HistogramDataCruncher<T> createHistogramDataCruncher(
		@Nonnull AttributeHistogramComputer histogramComputer,
		int bucketCount, HistogramBucket<T>[] buckets
	) {
		if (ArrayUtils.isEmpty(buckets)) {
			return null;
		} else {
			// first create converter that converts unknown Number attribute to the integer
			final AttributeHistogramRequest attributeHistogramRequest = histogramComputer.getRequest();
			final ToIntFunction<T> converter = createNumberToIntegerConverter(attributeHistogramRequest);
			final int decimalPlaces = attributeHistogramRequest.getDecimalPlaces();
			//noinspection unchecked
			return (HistogramDataCruncher<T>) HistogramDataCruncher.createOptimalHistogram(
				bucketCount,
				decimalPlaces,
				// combine all together - we want to have single bucket for single distinct value
				buckets,
				// value in the bucket represents the distinct value
				bucket -> converter.applyAsInt(bucket.getValue()),
				// number of records in the bucket represents the weight of it
				bucket -> bucket.getRecordIds().size(),
				// conversion method from / to BigDecimal that use histogramRequest#decimalPlaces for the conversion
				value -> decimalPlaces == 0 ? new BigDecimal(value) : new BigDecimal(value).stripTrailingZeros().scaleByPowerOfTen(-1 * decimalPlaces),
				value -> decimalPlaces == 0 ? value.intValueExact() : value.stripTrailingZeros().scaleByPowerOfTen(decimalPlaces).intValueExact()
			);
		}
	}

	private <T extends Comparable<T>> HistogramBucket<T>[] computeNarrowedHistogramBuckets(@Nonnull AttributeHistogramComputer histogramComputer, @Nonnull Formula filterFormula) {
		if (this.memoizedNarrowedBuckets == null) {
			// create formula clone without formula targeting current attribute
			final Formula optimizedFormula = FormulaCloner.clone(
				filterFormula, theFormula -> {
					if (theFormula instanceof SelectionFormula) {
						return shouldBeExcluded(((SelectionFormula)theFormula).getDelegate()) ? null : theFormula;
					} else {
						return shouldBeExcluded(theFormula) ? null : theFormula;
					}
				}
			);

			// now collect all INDEX histogram subsets that will be used for the computation
			@SuppressWarnings({"unchecked", "rawtypes"}) final HistogramBucket[][] attributeIndexes = histogramComputer
				.getAttributeIndexes()
				.stream()
				.map(it -> (HistogramSubSet<T>) it.getHistogramOfAllRecords())
				.map(HistogramSubSet::getHistogramBuckets)
				.toArray(HistogramBucket[][]::new);

			//noinspection unchecked
			this.memoizedNarrowedBuckets = AttributeHistogramProducer.getCombinedAndFilteredBucketArray(
				optimizedFormula, attributeIndexes
			);
		}
		//noinspection unchecked
		return (HistogramBucket<T>[]) this.memoizedNarrowedBuckets;
	}

	/**
	 * Method creates lambda that converts any {@link Number} value to an int value. The number overflows are checked
	 * in this method an any data precision loss is reported.
	 */
	@Nonnull
	private static <T extends Comparable<T>> ToIntFunction<T> createNumberToIntegerConverter(@Nonnull AttributeHistogramRequest histogramRequest) {
		final ToIntFunction<T> converter;
		if (Byte.class.isAssignableFrom(histogramRequest.getAttributeSchema().getType())) {
			converter = value -> (int) ((Byte) value);
		} else if (Short.class.isAssignableFrom(histogramRequest.getAttributeSchema().getType())) {
			converter = value -> (int) ((Short) value);
		} else if (Integer.class.isAssignableFrom(histogramRequest.getAttributeSchema().getType())) {
			converter = value -> (int) ((Integer) value);
		} else if (Long.class.isAssignableFrom(histogramRequest.getAttributeSchema().getType())) {
			converter = value -> {
				final int converted = ((Long) value).intValue();
				if ((Long) value != (long) converted) {
					throw new ArithmeticException("int overflow: " + value);
				}
				return converted;
			};
		} else if (BigDecimal.class.isAssignableFrom(histogramRequest.getAttributeSchema().getType())) {
			converter = value -> ((BigDecimal) value).stripTrailingZeros().scaleByPowerOfTen(histogramRequest.getDecimalPlaces()).intValueExact();
		} else {
			throw new IllegalStateException(
				"Unsupported histogram number type: " + histogramRequest.getAttributeSchema().getType() +
					", supported are byte, short, int. Number types long and BigDecimal are allowed as long as their " +
					"fit into an integer range!"
			);
		}
		return converter;
	}

	@Nonnull
	public List<FilterIndex> getAttributeIndexes() {
		return request.getAttributeIndexes();
	}

	@Override
	public long computeHash(@Nonnull LongHashFunction hashFunction) {
		if (this.memoizedHash == null) {
			this.memoizedHash = hashFunction.hashLongs(
				LongStream.concat(
					LongStream.of(
						bucketCount,
						filterFormula.computeHash(hashFunction)
					),
					LongStream.of(
						request.getAttributeIndexes()
							.stream()
							.mapToLong(FilterIndex::getId)
							.sorted()
							.toArray()
					)
				).toArray()
			);
		}
		return this.memoizedHash;
	}

	@Override
	public long computeTransactionalIdHash(@Nonnull LongHashFunction hashFunction) {
		if (this.memoizedTransactionalIdHash == null) {
			this.memoizedTransactionalIdHash = hashFunction.hashLongs(
				Arrays.stream(gatherTransactionalIds())
					.distinct()
					.sorted()
					.toArray()
			);
		}
		return this.memoizedTransactionalIdHash;
	}

	@Nonnull
	@Override
	public long[] gatherTransactionalIds() {
		if (this.memoizedTransactionalIds == null) {
			this.memoizedTransactionalIds = LongStream.concat(
					LongStream.of(filterFormula.gatherTransactionalIds()),
					request.getAttributeIndexes()
						.stream()
						.mapToLong(FilterIndex::getId)
				)
				.toArray();
		}
		return this.memoizedTransactionalIds;
	}

	@Override
	public long getEstimatedCost() {
		return filterFormula.getEstimatedCost() +
			getAttributeIndexes()
				.stream()
				.map(FilterIndex::getAllRecordsFormula)
				.mapToLong(TransactionalDataRelatedStructure::getEstimatedCost)
				.sum() * getOperationCost();
	}

	@Override
	public long getCost() {
		return filterFormula.getCost() +
			Arrays.stream(computeNarrowedHistogramBuckets(this, filterFormula))
				.mapToInt(it -> it.getRecordIds().size())
				.sum() * getOperationCost();
	}

	@Override
	public long getOperationCost() {
		return 3320;
	}

	@Override
	public long getCostToPerformanceRatio() {
		return getCost() / bucketCount;
	}


	@Nonnull
	@Override
	public HistogramContract compute() {
		if (memoizedResult == null) {
			// create cruncher that will compute the histogram
			@SuppressWarnings("rawtypes")
			final HistogramBucket[] histogramBuckets = computeNarrowedHistogramBuckets(
				this, filterFormula
			);
			@SuppressWarnings("unchecked")
			final HistogramDataCruncher<?> optimalHistogram = createHistogramDataCruncher(
				this, bucketCount, histogramBuckets
			);

			if (optimalHistogram != null) {
				memoizedResult = new Histogram(
					optimalHistogram.getHistogram(),
					optimalHistogram.getMaxValue()
				);
			} else {
				memoizedResult = HistogramContract.EMPTY;
			}

			ofNullable(onComputationCallback).ifPresent(it -> it.accept(this));
		}
		return memoizedResult;
	}

	/**
	 * Returns true if passed `formula` represents the formula targeting this attribute.
	 */
	private boolean shouldBeExcluded(@Nonnull Formula formula) {
		return request.getAttributeFormulas().contains(formula);
	}

}
