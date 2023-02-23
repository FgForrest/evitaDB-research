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

package io.evitadb.signal;

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.client.synthetic.ClientSyntheticTestState.QueryWithExpectedType;
import io.evitadb.signal.attributeAndHierarchyFiltering.ElasticsearchAttributeAndHierarchyFilteringSignalState;
import io.evitadb.signal.attributeAndHierarchyFiltering.InMemoryAttributeAndHierarchyFilteringSignalState;
import io.evitadb.signal.attributeAndHierarchyFiltering.SqlAttributeAndHierarchyFilteringSignalState;
import io.evitadb.signal.attributeFiltering.ElasticsearchAttributeFilteringSignalState;
import io.evitadb.signal.attributeFiltering.InMemoryAttributeFilteringSignalState;
import io.evitadb.signal.attributeFiltering.SqlAttributeFilteringSignalState;
import io.evitadb.signal.attributeHistogram.ElasticsearchAttributeHistogramSignalState;
import io.evitadb.signal.attributeHistogram.InMemoryAttributeHistogramSignalState;
import io.evitadb.signal.attributeHistogram.SqlAttributeHistogramSignalState;
import io.evitadb.signal.bulkWrite.ElasticsearchBulkWriteSignalState;
import io.evitadb.signal.bulkWrite.InMemoryBulkWriteSignalState;
import io.evitadb.signal.bulkWrite.SqlBulkWriteSignalState;
import io.evitadb.signal.facetAndHierarchyFiltering.ElasticsearchFacetAndHierarchyFilteringSignalState;
import io.evitadb.signal.facetAndHierarchyFiltering.InMemoryFacetAndHierarchyFilteringSignalState;
import io.evitadb.signal.facetAndHierarchyFiltering.SqlFacetAndHierarchyFilteringSignalState;
import io.evitadb.signal.facetAndHierarchyFilteringAndSummarizingCount.ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountSignalState;
import io.evitadb.signal.facetAndHierarchyFilteringAndSummarizingCount.InMemoryFacetAndHierarchyFilteringAndSummarizingCountSignalState;
import io.evitadb.signal.facetAndHierarchyFilteringAndSummarizingCount.SqlFacetAndHierarchyFilteringAndSummarizingCountSignalState;
import io.evitadb.signal.facetAndHierarchyFilteringAndSummarizingImpact.ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactSignalState;
import io.evitadb.signal.facetAndHierarchyFilteringAndSummarizingImpact.InMemoryFacetAndHierarchyFilteringAndSummarizingImpactSignalState;
import io.evitadb.signal.facetAndHierarchyFilteringAndSummarizingImpact.SqlFacetAndHierarchyFilteringAndSummarizingImpactSignalState;
import io.evitadb.signal.facetFiltering.ElasticsearchFacetFilteringSignalState;
import io.evitadb.signal.facetFiltering.InMemoryFacetFilteringSignalState;
import io.evitadb.signal.facetFiltering.SqlFacetFilteringSignalState;
import io.evitadb.signal.facetFilteringAndSummarizingCount.ElasticsearchFacetFilteringAndSummarizingCountSignalState;
import io.evitadb.signal.facetFilteringAndSummarizingCount.InMemoryFacetFilteringAndSummarizingCountSignalState;
import io.evitadb.signal.facetFilteringAndSummarizingCount.SqlFacetFilteringAndSummarizingCountSignalState;
import io.evitadb.signal.hierarchyStatistics.ElasticsearchHierarchyStatisticsComputationSignalState;
import io.evitadb.signal.hierarchyStatistics.InMemoryHierarchyStatisticsComputationSignalState;
import io.evitadb.signal.hierarchyStatistics.SqlHierarchyStatisticsComputationSignalState;
import io.evitadb.signal.parentsComputation.ElasticsearchParentsComputationSignalState;
import io.evitadb.signal.parentsComputation.InMemoryParentsComputationSignalState;
import io.evitadb.signal.parentsComputation.SqlParentsComputationSignalState;
import io.evitadb.signal.priceAndHierarchyFiltering.ElasticsearchPriceAndHierarchyFilteringSignalState;
import io.evitadb.signal.priceAndHierarchyFiltering.InMemoryPriceAndHierarchyFilteringSignalState;
import io.evitadb.signal.priceAndHierarchyFiltering.SqlPriceAndHierarchyFilteringSignalState;
import io.evitadb.signal.priceFiltering.ElasticsearchPriceFilteringSignalState;
import io.evitadb.signal.priceFiltering.InMemoryPriceFilteringSignalState;
import io.evitadb.signal.priceFiltering.SqlPriceFilteringSignalState;
import io.evitadb.signal.priceHistogram.ElasticsearchPriceHistogramSignalState;
import io.evitadb.signal.priceHistogram.InMemoryPriceHistogramSignalState;
import io.evitadb.signal.priceHistogram.SqlPriceHistogramSignalState;
import io.evitadb.signal.randomPageRead.ElasticsearchPageReadSignalState;
import io.evitadb.signal.randomPageRead.InMemoryPageReadSignalState;
import io.evitadb.signal.randomPageRead.SqlPageReadSignalState;
import io.evitadb.signal.randomSingleRead.ElasticsearchSingleReadSignalState;
import io.evitadb.signal.randomSingleRead.InMemorySingleReadSignalState;
import io.evitadb.signal.randomSingleRead.SqlSingleReadSignalState;
import io.evitadb.signal.synthetic.ElasticsearchSyntheticTestSignalState;
import io.evitadb.signal.synthetic.InMemorySyntheticTestSignalState;
import io.evitadb.signal.synthetic.SqlSyntheticTestSignalState;
import io.evitadb.signal.transactionalWrite.ElasticsearchTransactionalWriteSignalState;
import io.evitadb.signal.transactionalWrite.InMemoryTransactionalWriteSignalState;
import io.evitadb.signal.transactionalWrite.SqlTransactionalWriteSignalState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmarks contains test that use real anonymized client data from www.Signal.cz web site for performance measurements.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class SignalBenchmark {

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
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteSignalState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteSignalState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Sql(SqlBulkWriteSignalState state) {
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
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteSignalState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteSignalState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteSignalState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	/*
		RANDOM SINGLE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional
	 * mode and starts to randomly read one entity with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on single entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_InMemory(InMemorySingleReadSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Sql(SqlSingleReadSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		RANDOM PAGE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on page of entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_InMemory(InMemoryPageReadSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Sql(SqlPageReadSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		ATTRIBUTE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Sql(SqlAttributeFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		ATTRIBUTE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and hierarchy placement and sort.
	 * During setup bunch of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringSignalState state, Blackhole blackhole) {
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
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_InMemory(InMemoryPriceFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Sql(SqlPriceFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and hierarchy placement and sort. During
	 * setup bunch of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price and hierarchy placement data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringSignalState state, Blackhole blackhole) {
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
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Sql(SqlPriceHistogramSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_InMemory(InMemoryFacetFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Sql(SqlFacetFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET AND HIERARCHY FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the transactional mode
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
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
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
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
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
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactSignalState state, Blackhole blackhole) {
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
	public void parentsComputation_InMemory(InMemoryParentsComputationSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Sql(SqlParentsComputationSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		HIERARCHY STATISTICS COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the
	 * transactional mode and starts to randomly read page of entities with hierarchy placement data and hierarchy
	 * statistics DTO computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures hierarchy statistics DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationSignalState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}
	
	/*
		SYNTHETIC TEST
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Signal database, switches it to the
	 * transactional mode and starts to execute queries recorded in production system.
	 *
	 * Test measures real-world traffic on the real-world dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_InMemory(InMemorySyntheticTestSignalState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_Elasticsearch(ElasticsearchSyntheticTestSignalState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_Sql(SqlSyntheticTestSignalState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

}
