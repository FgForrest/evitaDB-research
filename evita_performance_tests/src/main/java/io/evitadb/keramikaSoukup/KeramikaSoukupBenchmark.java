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

package io.evitadb.keramikaSoukup;

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.client.synthetic.ClientSyntheticTestState.QueryWithExpectedType;
import io.evitadb.keramikaSoukup.attributeAndHierarchyFiltering.ElasticsearchAttributeAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeAndHierarchyFiltering.InMemoryAttributeAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeAndHierarchyFiltering.SqlAttributeAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeFiltering.ElasticsearchAttributeFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeFiltering.InMemoryAttributeFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeFiltering.SqlAttributeFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeHistogram.ElasticsearchAttributeHistogramKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeHistogram.InMemoryAttributeHistogramKeramikaSoukupState;
import io.evitadb.keramikaSoukup.attributeHistogram.SqlAttributeHistogramKeramikaSoukupState;
import io.evitadb.keramikaSoukup.bulkWrite.ElasticsearchBulkWriteKeramikaSoukupState;
import io.evitadb.keramikaSoukup.bulkWrite.InMemoryBulkWriteKeramikaSoukupState;
import io.evitadb.keramikaSoukup.bulkWrite.SqlBulkWriteKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFiltering.ElasticsearchFacetAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFiltering.InMemoryFacetAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFiltering.SqlFacetAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFilteringAndSummarizingCount.ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFilteringAndSummarizingCount.InMemoryFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFilteringAndSummarizingCount.SqlFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFilteringAndSummarizingImpact.ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFilteringAndSummarizingImpact.InMemoryFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetAndHierarchyFilteringAndSummarizingImpact.SqlFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetFiltering.ElasticsearchFacetFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetFiltering.InMemoryFacetFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetFiltering.SqlFacetFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetFilteringAndSummarizingCount.ElasticsearchFacetFilteringAndSummarizingCountKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetFilteringAndSummarizingCount.InMemoryFacetFilteringAndSummarizingCountKeramikaSoukupState;
import io.evitadb.keramikaSoukup.facetFilteringAndSummarizingCount.SqlFacetFilteringAndSummarizingCountKeramikaSoukupState;
import io.evitadb.keramikaSoukup.hierarchyStatistics.ElasticsearchHierarchyStatisticsComputationKeramikaSoukupState;
import io.evitadb.keramikaSoukup.hierarchyStatistics.InMemoryHierarchyStatisticsComputationKeramikaSoukupState;
import io.evitadb.keramikaSoukup.hierarchyStatistics.SqlHierarchyStatisticsComputationKeramikaSoukupState;
import io.evitadb.keramikaSoukup.parentsComputation.ElasticsearchParentsComputationKeramikaSoukupState;
import io.evitadb.keramikaSoukup.parentsComputation.InMemoryParentsComputationKeramikaSoukupState;
import io.evitadb.keramikaSoukup.parentsComputation.SqlParentsComputationKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceAndHierarchyFiltering.ElasticsearchPriceAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceAndHierarchyFiltering.InMemoryPriceAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceAndHierarchyFiltering.SqlPriceAndHierarchyFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceFiltering.ElasticsearchPriceFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceFiltering.InMemoryPriceFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceFiltering.SqlPriceFilteringKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceHistogram.ElasticsearchPriceHistogramKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceHistogram.InMemoryPriceHistogramKeramikaSoukupState;
import io.evitadb.keramikaSoukup.priceHistogram.SqlPriceHistogramKeramikaSoukupState;
import io.evitadb.keramikaSoukup.randomPageRead.ElasticsearchPageReadKeramikaSoukupState;
import io.evitadb.keramikaSoukup.randomPageRead.InMemoryPageReadKeramikaSoukupState;
import io.evitadb.keramikaSoukup.randomPageRead.SqlPageReadKeramikaSoukupState;
import io.evitadb.keramikaSoukup.randomSingleRead.ElasticsearchSingleReadKeramikaSoukupState;
import io.evitadb.keramikaSoukup.randomSingleRead.InMemorySingleReadKeramikaSoukupState;
import io.evitadb.keramikaSoukup.randomSingleRead.SqlSingleReadKeramikaSoukupState;
import io.evitadb.keramikaSoukup.synthetic.ElasticsearchSyntheticTestKeramikaSoukupState;
import io.evitadb.keramikaSoukup.synthetic.InMemorySyntheticTestKeramikaSoukupState;
import io.evitadb.keramikaSoukup.synthetic.SqlSyntheticTestKeramikaSoukupState;
import io.evitadb.keramikaSoukup.transactionalWrite.ElasticsearchTransactionalWriteKeramikaSoukupState;
import io.evitadb.keramikaSoukup.transactionalWrite.InMemoryTransactionalWriteKeramikaSoukupState;
import io.evitadb.keramikaSoukup.transactionalWrite.SqlTransactionalWriteKeramikaSoukupState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmarks contains test that use real anonymized client data from
 * <a href="https://www.keramikasoukup.cz">Keramika Soukup</a> web site for performance measurements.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class KeramikaSoukupBenchmark {

	/*
		BULK INSERT
	*/

	/**
	 * This test spins an empty DB and starts inserting new products into it. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures bulk write speed on random data.
	 * Each iteration starts with empty data
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteKeramikaSoukupState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteKeramikaSoukupState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Sql(SqlBulkWriteKeramikaSoukupState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	/*
		TRANSACTIONAL UPSERT
	 */

	/**
	 * This test spins an empty DB inserts there a few thousands products, switches it to the transactional mode
	 * and starts to insert or update existing products in/to it. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures transactional write / overwrite speed on random data.
	 * Each iteration starts with database that already contains few thousands records.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteKeramikaSoukupState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteKeramikaSoukupState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteKeramikaSoukupState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	/*
		RANDOM SINGLE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional
	 * mode and starts to randomly read one entity with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on single entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_InMemory(InMemorySingleReadKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Sql(SqlSingleReadKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		RANDOM PAGE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on page of entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_InMemory(InMemoryPageReadKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Sql(SqlPageReadKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		ATTRIBUTE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Sql(SqlAttributeFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		ATTRIBUTE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and hierarchy placement and sort.
	 * During setup bunch of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		ATTRIBUTE HISTOGRAM COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the client database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized attribute filter and hierarchy placement data and attribute
	 * histogram computation. During setup bunch of brands, categories, attribute lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures attribute histogram DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_InMemory(InMemoryPriceFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Sql(SqlPriceFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and hierarchy placement and sort. During
	 * setup bunch of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price and hierarchy placement data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE HISTOGRAM COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the client database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and hierarchy placement data and price
	 * histogram computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures price histogram DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Sql(SqlPriceHistogramKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_InMemory(InMemoryFacetFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Sql(SqlFacetFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET AND HIERARCHY FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter and facet summary (count) computation.
	 * During setup bunch of brands, categories, price lists and stores are created so that they could be referenced
	 * in products.
	 *
	 * Test measures filtering by facet references and computing summary for the rest in the dataset. It also randomizes
	 * the relation among the facet groups of the facets.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING IN HIERARCHY
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the client database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter  and hierarchy placement data and facet
	 * summary (count) computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures filtering by facet references and hierarchy placement data and computing summary for the rest
	 * in the dataset. It also randomizes the relation among the facet groups of the facets.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING IMPACT IN HIERARCHY
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the client database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter  and hierarchy placement data and facet
	 * summary (impact) computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures filtering by facet references and hierarchy placement data and computing summary for the rest
	 * in the dataset. It also randomizes the relation among the facet groups of the facets.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PARENTS COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the client database, switches it to the
	 * transactional mode and starts to randomly read page of entities with hierarchy placement data and parents DTO
	 * computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures parents DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_InMemory(InMemoryParentsComputationKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Sql(SqlParentsComputationKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		HIERARCHY STATISTICS COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the
	 * transactional mode and starts to randomly read page of entities with hierarchy placement data and hierarchy
	 * statistics DTO computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures hierarchy statistics DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationKeramikaSoukupState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		SYNTHETIC TEST
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the KeramikaSoukup database, switches it to the
	 * transactional mode and starts to execute queries recorded in production system.
	 *
	 * Test measures real-world traffic on the real-world dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_InMemory(InMemorySyntheticTestKeramikaSoukupState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_Elasticsearch(ElasticsearchSyntheticTestKeramikaSoukupState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_Sql(SqlSyntheticTestKeramikaSoukupState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

}
