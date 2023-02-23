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

package io.evitadb.artificial;

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.artificial.attributeAndHierarchyFiltering.ElasticsearchAttributeAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.attributeAndHierarchyFiltering.InMemoryAttributeAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.attributeAndHierarchyFiltering.SqlAttributeAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.attributeFiltering.ElasticsearchAttributeFilteringArtificialState;
import io.evitadb.artificial.attributeFiltering.InMemoryAttributeFilteringArtificialState;
import io.evitadb.artificial.attributeFiltering.SqlAttributeFilteringArtificialState;
import io.evitadb.artificial.attributeHistogram.ElasticsearchAttributeHistogramArtificialState;
import io.evitadb.artificial.attributeHistogram.InMemoryAttributeHistogramArtificialState;
import io.evitadb.artificial.attributeHistogram.SqlAttributeHistogramArtificialState;
import io.evitadb.artificial.bulkWrite.ElasticsearchBulkWriteArtificialState;
import io.evitadb.artificial.bulkWrite.InMemoryBulkWriteArtificialState;
import io.evitadb.artificial.bulkWrite.SqlBulkWriteArtificialState;
import io.evitadb.artificial.facetAndHierarchyFiltering.ElasticsearchFacetAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.facetAndHierarchyFiltering.InMemoryFacetAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.facetAndHierarchyFiltering.SqlFacetAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.facetAndHierarchyFilteringAndSummarizingCount.ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountArtificialState;
import io.evitadb.artificial.facetAndHierarchyFilteringAndSummarizingCount.InMemoryFacetAndHierarchyFilteringAndSummarizingCountArtificialState;
import io.evitadb.artificial.facetAndHierarchyFilteringAndSummarizingCount.SqlFacetAndHierarchyFilteringAndSummarizingCountArtificialState;
import io.evitadb.artificial.facetAndHierarchyFilteringAndSummarizingImpact.ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactArtificialState;
import io.evitadb.artificial.facetAndHierarchyFilteringAndSummarizingImpact.InMemoryFacetAndHierarchyFilteringAndSummarizingImpactArtificialState;
import io.evitadb.artificial.facetAndHierarchyFilteringAndSummarizingImpact.SqlFacetAndHierarchyFilteringAndSummarizingImpactArtificialState;
import io.evitadb.artificial.facetFiltering.ElasticsearchFacetFilteringArtificialState;
import io.evitadb.artificial.facetFiltering.InMemoryFacetFilteringArtificialState;
import io.evitadb.artificial.facetFiltering.SqlFacetFilteringArtificialState;
import io.evitadb.artificial.facetFilteringAndSummarizingCount.ElasticsearchFacetFilteringAndSummarizingCountArtificialState;
import io.evitadb.artificial.facetFilteringAndSummarizingCount.InMemoryFacetFilteringAndSummarizingCountArtificialState;
import io.evitadb.artificial.facetFilteringAndSummarizingCount.SqlFacetFilteringAndSummarizingCountArtificialState;
import io.evitadb.artificial.hierarchyStatistics.ElasticsearchHierarchyStatisticsComputationArtificialState;
import io.evitadb.artificial.hierarchyStatistics.InMemoryHierarchyStatisticsComputationArtificialState;
import io.evitadb.artificial.hierarchyStatistics.SqlHierarchyStatisticsComputationArtificialState;
import io.evitadb.artificial.parentsComputation.ElasticsearchParentsComputationArtificialState;
import io.evitadb.artificial.parentsComputation.InMemoryParentsComputationArtificialState;
import io.evitadb.artificial.parentsComputation.SqlParentsComputationArtificialState;
import io.evitadb.artificial.priceAndHierarchyFiltering.ElasticsearchPriceAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.priceAndHierarchyFiltering.InMemoryPriceAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.priceAndHierarchyFiltering.SqlPriceAndHierarchyFilteringArtificialState;
import io.evitadb.artificial.priceFiltering.ElasticsearchPriceFilteringArtificialState;
import io.evitadb.artificial.priceFiltering.InMemoryPriceFilteringArtificialState;
import io.evitadb.artificial.priceFiltering.SqlPriceFilteringArtificialState;
import io.evitadb.artificial.priceHistogram.ElasticsearchPriceHistogramArtificialState;
import io.evitadb.artificial.priceHistogram.InMemoryPriceHistogramArtificialState;
import io.evitadb.artificial.priceHistogram.SqlPriceHistogramArtificialState;
import io.evitadb.artificial.randomPageRead.ElasticsearchPageReadArtificialState;
import io.evitadb.artificial.randomPageRead.InMemoryPageReadArtificialState;
import io.evitadb.artificial.randomPageRead.SqlPageReadArtificialState;
import io.evitadb.artificial.randomSingleRead.ElasticsearchSingleReadArtificialState;
import io.evitadb.artificial.randomSingleRead.InMemorySingleReadArtificialState;
import io.evitadb.artificial.randomSingleRead.SqlSingleReadArtificialState;
import io.evitadb.artificial.transactionalWrite.ElasticsearchTransactionalWriteArtificialState;
import io.evitadb.artificial.transactionalWrite.InMemoryTransactionalWriteArtificialState;
import io.evitadb.artificial.transactionalWrite.SqlTransactionalWriteArtificialState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmarks contains test that use "artificial" (i.e. random, non-real) data for performance measurements.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Timeout(time = 1, timeUnit = TimeUnit.HOURS)
public abstract class ArtificialEntitiesBenchmark {

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
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteArtificialState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteArtificialState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void bulkInsertThroughput_Sql(SqlBulkWriteArtificialState state) {
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
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteArtificialState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteArtificialState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(1)
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteArtificialState state) {
		state.getSession().upsertEntity(state.getProduct());
	}

	/*
		RANDOM SINGLE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there a one hundred thousands products, switches it to the transactional mode
	 * and starts to randomly read one entity with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on single entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_InMemory(InMemorySingleReadArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void singleEntityRead_Sql(SqlSingleReadArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		RANDOM PAGE ENTITY READ
	 */

	/**
	 * This test spins an empty DB inserts there a one hundred thousands products, switches it to the transactional mode
	 * and starts to randomly read page of entities with different requirements. During setup bunch of brands, categories,
	 * price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures random read on page of entity data.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_InMemory(InMemoryPageReadArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void paginatedEntityRead_Sql(SqlPageReadArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityContract.class)
		);
	}

	/*
		ATTRIBUTE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeFiltering_Sql(SqlAttributeFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		ATTRIBUTE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with simple multiple attribute filter and sort as well as hierarchy
	 * filter that targets single category (optionally with subtree). During setup bunch of brands, categories, price
	 * lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by various attributes and hierarchy placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		ATTRIBUTE HISTOGRAM COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized attribute filter and hierarchy placement data and attribute
	 * histogram computation. During setup bunch of brands, categories, attribute lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures attribute histogram DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_InMemory(InMemoryPriceFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceFiltering_Sql(SqlPriceFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE AND HIERARCHY FILTERING / SORTING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price and hierarchy filter and sort. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering and ordering by price and hierarchy placement data in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PRICE HISTOGRAM COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized price filter and hierarchy placement data and price
	 * histogram computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures price histogram DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void priceHistogramComputation_Sql(SqlPriceHistogramArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_InMemory(InMemoryFacetFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFiltering_Sql(SqlFacetFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET AND HIERARCHY FILTERING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
	 * and starts to randomly read page of entities with randomized facet filter. During setup bunch
	 * of brands, categories, price lists and stores are created so that they could be referenced in products.
	 *
	 * Test measures filtering by facet references and hierarchical placement in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
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
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING IN HIERARCHY
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
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
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		FACET FILTERING AND SUMMARIZING IMPACT IN HIERARCHY
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the transactional mode
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
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		PARENTS COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the
	 * transactional mode and starts to randomly read page of entities with hierarchy placement data and parents DTO
	 * computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures parents DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_InMemory(InMemoryParentsComputationArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void parentsComputation_Sql(SqlParentsComputationArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	/*
		HIERARCHY STATISTICS COMPUTATION
	 */

	/**
	 * This test spins an empty DB inserts there full contents of the artificial database, switches it to the
	 * transactional mode and starts to randomly read page of entities with hierarchy placement data and hierarchy
	 * statistics DTO computation. During setup bunch of brands, categories, price lists and stores are created so that
	 * they could be referenced in products.
	 *
	 * Test measures hierarchy statistics DTO computation in the dataset.
	 */
	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

	@Benchmark
	@Measurement(time = 1, timeUnit = TimeUnit.MINUTES)
	@Threads(Threads.MAX)
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationArtificialState state, Blackhole blackhole) {
		blackhole.consume(
			state.getSession().query(state.getQuery(), EntityReferenceContract.class)
		);
	}

}
