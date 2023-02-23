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

package io.evitadb.senesi;

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.client.synthetic.ClientSyntheticTestState.QueryWithExpectedType;
import io.evitadb.senesi.attributeAndHierarchyFiltering.ElasticsearchAttributeAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.attributeAndHierarchyFiltering.InMemoryAttributeAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.attributeAndHierarchyFiltering.SqlAttributeAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.attributeFiltering.ElasticsearchAttributeFilteringSenesiState;
import io.evitadb.senesi.attributeFiltering.InMemoryAttributeFilteringSenesiState;
import io.evitadb.senesi.attributeFiltering.SqlAttributeFilteringSenesiState;
import io.evitadb.senesi.attributeHistogram.ElasticsearchAttributeHistogramSenesiState;
import io.evitadb.senesi.attributeHistogram.InMemoryAttributeHistogramSenesiState;
import io.evitadb.senesi.attributeHistogram.SqlAttributeHistogramSenesiState;
import io.evitadb.senesi.bulkWrite.ElasticsearchBulkWriteSenesiState;
import io.evitadb.senesi.bulkWrite.InMemoryBulkWriteSenesiState;
import io.evitadb.senesi.bulkWrite.SqlBulkWriteSenesiState;
import io.evitadb.senesi.facetAndHierarchyFiltering.ElasticsearchFacetAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.facetAndHierarchyFiltering.InMemoryFacetAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.facetAndHierarchyFiltering.SqlFacetAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.facetAndHierarchyFilteringAndSummarizingCount.ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountSenesiState;
import io.evitadb.senesi.facetAndHierarchyFilteringAndSummarizingCount.InMemoryFacetAndHierarchyFilteringAndSummarizingCountSenesiState;
import io.evitadb.senesi.facetAndHierarchyFilteringAndSummarizingCount.SqlFacetAndHierarchyFilteringAndSummarizingCountSenesiState;
import io.evitadb.senesi.facetAndHierarchyFilteringAndSummarizingImpact.ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactSenesiState;
import io.evitadb.senesi.facetAndHierarchyFilteringAndSummarizingImpact.InMemoryFacetAndHierarchyFilteringAndSummarizingImpactSenesiState;
import io.evitadb.senesi.facetAndHierarchyFilteringAndSummarizingImpact.SqlFacetAndHierarchyFilteringAndSummarizingImpactSenesiState;
import io.evitadb.senesi.facetFiltering.ElasticsearchFacetFilteringSenesiState;
import io.evitadb.senesi.facetFiltering.InMemoryFacetFilteringSenesiState;
import io.evitadb.senesi.facetFiltering.SqlFacetFilteringSenesiState;
import io.evitadb.senesi.facetFilteringAndSummarizingCount.ElasticsearchFacetFilteringAndSummarizingCountSenesiState;
import io.evitadb.senesi.facetFilteringAndSummarizingCount.InMemoryFacetFilteringAndSummarizingCountSenesiState;
import io.evitadb.senesi.facetFilteringAndSummarizingCount.SqlFacetFilteringAndSummarizingCountSenesiState;
import io.evitadb.senesi.hierarchyStatistics.ElasticsearchHierarchyStatisticsComputationSenesiState;
import io.evitadb.senesi.hierarchyStatistics.InMemoryHierarchyStatisticsComputationSenesiState;
import io.evitadb.senesi.hierarchyStatistics.SqlHierarchyStatisticsComputationSenesiState;
import io.evitadb.senesi.parentsComputation.ElasticsearchParentsComputationSenesiState;
import io.evitadb.senesi.parentsComputation.InMemoryParentsComputationSenesiState;
import io.evitadb.senesi.parentsComputation.SqlParentsComputationSenesiState;
import io.evitadb.senesi.priceAndHierarchyFiltering.ElasticsearchPriceAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.priceAndHierarchyFiltering.InMemoryPriceAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.priceAndHierarchyFiltering.SqlPriceAndHierarchyFilteringSenesiState;
import io.evitadb.senesi.priceFiltering.ElasticsearchPriceFilteringSenesiState;
import io.evitadb.senesi.priceFiltering.InMemoryPriceFilteringSenesiState;
import io.evitadb.senesi.priceFiltering.SqlPriceFilteringSenesiState;
import io.evitadb.senesi.priceHistogram.ElasticsearchPriceHistogramSenesiState;
import io.evitadb.senesi.priceHistogram.InMemoryPriceHistogramSenesiState;
import io.evitadb.senesi.priceHistogram.SqlPriceHistogramSenesiState;
import io.evitadb.senesi.randomPageRead.ElasticsearchPageReadSenesiState;
import io.evitadb.senesi.randomPageRead.InMemoryPageReadSenesiState;
import io.evitadb.senesi.randomPageRead.SqlPageReadSenesiState;
import io.evitadb.senesi.randomSingleRead.ElasticsearchSingleReadSenesiState;
import io.evitadb.senesi.randomSingleRead.InMemorySingleReadSenesiState;
import io.evitadb.senesi.randomSingleRead.SqlSingleReadSenesiState;
import io.evitadb.senesi.synthetic.ElasticsearchSyntheticTestSenesiState;
import io.evitadb.senesi.synthetic.InMemorySyntheticTestSenesiState;
import io.evitadb.senesi.synthetic.SqlSyntheticTestSenesiState;
import io.evitadb.senesi.transactionalWrite.ElasticsearchTransactionalWriteSenesiState;
import io.evitadb.senesi.transactionalWrite.InMemoryTransactionalWriteSenesiState;
import io.evitadb.senesi.transactionalWrite.SqlTransactionalWriteSenesiState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmarks contains test that use real anonymized client data from www.senesi.cz web site for performance measurements.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Timeout(time = 1, timeUnit = TimeUnit.HOURS)
public abstract class SenesiBenchmark {

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
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteSenesiState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteSenesiState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Sql(SqlBulkWriteSenesiState state) {
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
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteSenesiState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteSenesiState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteSenesiState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	/*
		RANDOM SINGLE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the transactional
	 * mode and starts to randomly read one entity with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on single entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_InMemory(InMemorySingleReadSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Sql(SqlSingleReadSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		RANDOM PAGE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on page of entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_InMemory(InMemoryPageReadSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Sql(SqlPageReadSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		ATTRIBUTE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Sql(SqlAttributeFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		ATTRIBUTE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and hierarchy placement and sort.
	 * During setup bunch of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
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
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_InMemory(InMemoryPriceFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Sql(SqlPriceFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and hierarchy placement and sort. During
	 * setup bunch of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price and hierarchy placement data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
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
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Sql(SqlPriceHistogramSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_InMemory(InMemoryFacetFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Sql(SqlFacetFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET AND HIERARCHY FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the senesi database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the senesi database, switches it to the transactional mode
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
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
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
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
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
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactSenesiState state, Blackhole blackhole) {
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
	public void parentsComputation_InMemory(InMemoryParentsComputationSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Sql(SqlParentsComputationSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		HIERARCHY STATISTICS COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the senesi database, switches it to the
	 * transactional mode and starts to randomly read page of entities with hierarchy placement data and hierarchy
	 * statistics DTO computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures hierarchy statistics DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationSenesiState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		SYNTHETIC TEST
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the Senesi database, switches it to the
	 * transactional mode and starts to execute queries recorded in production system.
	 *
	 * Test measures real-world traffic on the real-world dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_InMemory(InMemorySyntheticTestSenesiState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_Elasticsearch(ElasticsearchSyntheticTestSenesiState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void syntheticTest_Sql(SqlSyntheticTestSenesiState state, Blackhole blackhole) {
		final QueryWithExpectedType queryWithExpectedType = state.getQueryWithExpectedType();
		blackhole.consume(
			state.getSession().query(queryWithExpectedType.getQuery(), queryWithExpectedType.getExpectedResult())
		);
	}

}
