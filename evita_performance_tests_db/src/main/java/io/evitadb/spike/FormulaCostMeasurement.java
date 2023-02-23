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

package io.evitadb.spike;

import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.index.histogram.suppliers.HistogramBitmapSupplier;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.query.algebra.base.*;
import io.evitadb.query.algebra.price.innerRecordHandling.PriceHandlingContainerFormula;
import io.evitadb.query.algebra.price.priceIndex.PriceIdContainerFormula;
import io.evitadb.query.algebra.price.termination.*;
import io.evitadb.query.algebra.price.translate.PriceIdToEntityIdTranslateFormula;
import io.evitadb.query.extraResult.translator.histogram.producer.AttributeHistogramComputer;
import io.evitadb.query.extraResult.translator.histogram.producer.PriceHistogramComputer;
import io.evitadb.spike.mock.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Currency;

/**
 * This spike test tries to test how fast are formulas.
 *
 * Results:
 * (COST = 1 = 1mil. ops/s)
 *
 * Benchmark                                                       Mode  Cnt        Score   Error  Units
 * FormulaCostMeasurement.andFormulaInteger                       thrpt    2   116094.151          ops/s
 * FormulaCostMeasurement.attributeHistogramComputer              thrpt    2      301.262          ops/s
 * FormulaCostMeasurement.disentangleFormula                      thrpt    2      446.449          ops/s
 * FormulaCostMeasurement.firstVariantPriceTermination            thrpt    2       54.935          ops/s
 * FormulaCostMeasurement.histogramBitmapSupplier                 thrpt    2     3959.800          ops/s
 * FormulaCostMeasurement.joinFormula                             thrpt    2      390.631          ops/s
 * FormulaCostMeasurement.notFormulaInteger                       thrpt    2   148431.438          ops/s
 * FormulaCostMeasurement.orFormulaInteger                        thrpt    2    80640.199          ops/s
 * FormulaCostMeasurement.plainPriceTermination                   thrpt    2 45566587.048          ops/s
 * FormulaCostMeasurement.plainPriceTerminationWithPriceFilter    thrpt    2      312.185          ops/s
 * FormulaCostMeasurement.priceHistogramComputer                  thrpt    2       88.759          ops/s
 * FormulaCostMeasurement.priceIdContainer                        thrpt    2      483.873          ops/s
 * FormulaCostMeasurement.priceIdToEntityIdTranslate              thrpt    2      283.536          ops/s
 * FormulaCostMeasurement.sumPriceTermination                     thrpt    2       55.321          ops/s
 *
 * Benchmark                                                       Mode  Cnt        Score   Error  Units
 * FormulaCostMeasurement.roaringBitmapWithRandomFar              thrpt    2      374.028          ops/s
 * FormulaCostMeasurement.roaringBitmapWithRandomClose            thrpt    2      903.770          ops/s
 * FormulaCostMeasurement.roaringBitmapWithRandomIntClose         thrpt    2     1580.381          ops/s
 * FormulaCostMeasurement.roaringBitmapWithRandomIntCloseBatch    thrpt    2     3332.862          ops/s
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FormulaCostMeasurement {

	public static void main(String[] args) throws Exception {
		org.openjdk.jmh.Main.main(args);
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void andFormulaInteger(IntegerBitmapState bitmapDataSet, Blackhole blackhole) {
		blackhole.consume(
			new AndFormula(
				new long[]{1L},
				bitmapDataSet.getBitmapA(),
				bitmapDataSet.getBitmapB()
			).compute()
		);
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void orFormulaInteger(IntegerBitmapState bitmapDataSet, Blackhole blackhole) {
		blackhole.consume(
			new OrFormula(
				new long[] {1L},
				bitmapDataSet.getBitmapA(),
				bitmapDataSet.getBitmapB()
			).compute()
		);
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void notFormulaInteger(IntegerBitmapState bitmapDataSet, Blackhole blackhole) {
		blackhole.consume(
			new NotFormula(
				bitmapDataSet.getBitmapA(),
				bitmapDataSet.getBitmapB()
			).compute()
		);
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void joinFormula(IntegerBitmapState bitmapDataSet, Blackhole blackhole) {
		blackhole.consume(
			new JoinFormula(
				1L,
				bitmapDataSet.getBitmapA(),
				bitmapDataSet.getBitmapB()
			).compute()
		);
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void disentangleFormula(IntegerBitmapState bitmapDataSet, Blackhole blackhole) {
		blackhole.consume(
			new DisentangleFormula(
				bitmapDataSet.getBitmapA(),
				bitmapDataSet.getBitmapB()
			).compute()
		);
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void priceIdContainer(PriceIdsWithPriceRecordsRecordState priceDataSet, Blackhole blackhole) {
		final PriceIdContainerFormula testedFormula = new PriceIdContainerFormula(
			priceDataSet.getPriceIndex(),
			priceDataSet.getPriceIdsFormula()
		);
		blackhole.consume(testedFormula.compute());
		blackhole.consume(testedFormula.getFilteredPriceRecords());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void priceIdToEntityIdTranslate(PriceIdsWithPriceRecordsRecordState priceDataSet, Blackhole blackhole) {
		final PriceIdToEntityIdTranslateFormula testedFormula = new PriceIdToEntityIdTranslateFormula(priceDataSet.getPriceIdsFormula());
		blackhole.consume(testedFormula.compute());
		blackhole.consume(testedFormula.getFilteredPriceRecords());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void plainPriceTermination(EntityIdsWithPriceRecordsRecordState priceDataSet, Blackhole blackhole) {
		final PlainPriceTerminationFormula testedFormula = new PlainPriceTerminationFormula(
			new PriceHandlingContainerFormula(
				PriceInnerRecordHandling.NONE,
				priceDataSet.getFormula()
			),
			new PriceEvaluationContext(
				new PriceIndexKey("whatever", Currency.getInstance("CZK"), PriceInnerRecordHandling.NONE)
			)
		);
		blackhole.consume(testedFormula.compute());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void plainPriceTerminationWithPriceFilter(EntityIdsWithPriceRecordsRecordState priceDataSet, Blackhole blackhole) {
		final PlainPriceTerminationFormulaWithPriceFilter testedFormula = new PlainPriceTerminationFormulaWithPriceFilter(
			new PriceHandlingContainerFormula(
				PriceInnerRecordHandling.NONE,
				priceDataSet.getFormula()
			),
			new PriceEvaluationContext(
				new PriceIndexKey("whatever", Currency.getInstance("CZK"), PriceInnerRecordHandling.NONE)
			),
			PricePredicate.NO_FILTER
		);
		blackhole.consume(testedFormula.compute());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void firstVariantPriceTermination(InnerRecordIdsWithPriceRecordsRecordState priceDataSet, Blackhole blackhole) {
		final FirstVariantPriceTerminationFormula testedFormula = new FirstVariantPriceTerminationFormula(
			new PriceHandlingContainerFormula(
				PriceInnerRecordHandling.FIRST_OCCURRENCE,
				priceDataSet.getFormula()
			),
			new PriceEvaluationContext(
				new PriceIndexKey("whatever", Currency.getInstance("CZK"), PriceInnerRecordHandling.NONE)
			),
			QueryPriceMode.WITH_VAT,
			PricePredicate.NO_FILTER
		);
		blackhole.consume(testedFormula.compute());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void sumPriceTermination(InnerRecordIdsWithPriceRecordsRecordState priceDataSet, Blackhole blackhole) {
		final SumPriceTerminationFormula testedFormula = new SumPriceTerminationFormula(
			new PriceHandlingContainerFormula(
				PriceInnerRecordHandling.FIRST_OCCURRENCE,
				priceDataSet.getFormula()
			),
			new PriceEvaluationContext(
				new PriceIndexKey("whatever", Currency.getInstance("CZK"), PriceInnerRecordHandling.NONE)
			),
			QueryPriceMode.WITH_VAT,
			PricePredicate.NO_FILTER
		);
		blackhole.consume(testedFormula.compute());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void histogramBitmapSupplier(BucketsRecordState bucketDataSet, Blackhole blackhole) {
		final HistogramBitmapSupplier<Integer> testedFormula = new HistogramBitmapSupplier<>(
			bucketDataSet.getBuckets()
		);
		blackhole.consume(testedFormula.get());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void attributeHistogramComputer(BucketsRecordState bucketDataSet, Blackhole blackhole) {
		final AttributeHistogramComputer testedFormula = new AttributeHistogramComputer(
			bucketDataSet.getFormula(),
			40,
			bucketDataSet.getRequest()
		);
		blackhole.consume(testedFormula.compute());
	}

	@Benchmark
	@Threads(1)
	@BenchmarkMode({Mode.Throughput})
	public void priceHistogramComputer(PriceBucketRecordState bucketDataSet, Blackhole blackhole) {
		final PriceHistogramComputer testedFormula = new PriceHistogramComputer(
			40, 2, QueryPriceMode.WITH_VAT,
			bucketDataSet.getFormulaA(),
			bucketDataSet.getFormulaB(),
			bucketDataSet.getFilteredPriceRecordAccessors(),
			null
		);
		blackhole.consume(testedFormula.compute());
	}

}