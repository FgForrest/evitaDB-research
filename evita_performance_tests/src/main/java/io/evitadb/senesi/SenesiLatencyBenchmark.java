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
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This class runs all tests in {@link SenesiBenchmark} in latency mode measurement.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@BenchmarkMode({Mode.AverageTime})
public class SenesiLatencyBenchmark extends SenesiBenchmark {

	@Override
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteSenesiState state) {
		super.bulkInsertThroughput_InMemory(state);
	}

	@Override
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteSenesiState state) {
		super.bulkInsertThroughput_Elasticsearch(state);
	}

	@Override
	public void bulkInsertThroughput_Sql(SqlBulkWriteSenesiState state) {
		super.bulkInsertThroughput_Sql(state);
	}

	@Override
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteSenesiState state) {
		super.transactionalUpsertThroughput_InMemory(state);
	}

	@Override
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteSenesiState state) {
		super.transactionalUpsertThroughput_Elasticsearch(state);
	}

	@Override
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteSenesiState state) {
		super.transactionalUpsertThroughput_Sql(state);
	}

	@Override
	public void singleEntityRead_InMemory(InMemorySingleReadSenesiState state, Blackhole blackhole) {
		super.singleEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadSenesiState state, Blackhole blackhole) {
		super.singleEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void singleEntityRead_Sql(SqlSingleReadSenesiState state, Blackhole blackhole) {
		super.singleEntityRead_Sql(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_InMemory(InMemoryPageReadSenesiState state, Blackhole blackhole) {
		super.paginatedEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadSenesiState state, Blackhole blackhole) {
		super.paginatedEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Sql(SqlPageReadSenesiState state, Blackhole blackhole) {
		super.paginatedEntityRead_Sql(state, blackhole);
	}

	@Override
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringSenesiState state, Blackhole blackhole) {
		super.attributeFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringSenesiState state, Blackhole blackhole) {
		super.attributeFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeFiltering_Sql(SqlAttributeFilteringSenesiState state, Blackhole blackhole) {
		super.attributeFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramSenesiState state, Blackhole blackhole) {
		super.attributeHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramSenesiState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramSenesiState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void priceFiltering_InMemory(InMemoryPriceFilteringSenesiState state, Blackhole blackhole) {
		super.priceFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringSenesiState state, Blackhole blackhole) {
		super.priceFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceFiltering_Sql(SqlPriceFilteringSenesiState state, Blackhole blackhole) {
		super.priceFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramSenesiState state, Blackhole blackhole) {
		super.priceHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramSenesiState state, Blackhole blackhole) {
		super.priceHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Sql(SqlPriceHistogramSenesiState state, Blackhole blackhole) {
		super.priceHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void facetFiltering_InMemory(InMemoryFacetFilteringSenesiState state, Blackhole blackhole) {
		super.facetFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringSenesiState state, Blackhole blackhole) {
		super.facetFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFiltering_Sql(SqlFacetFilteringSenesiState state, Blackhole blackhole) {
		super.facetFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactSenesiState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Sql(state, blackhole);
	}

	@Override
	public void parentsComputation_InMemory(InMemoryParentsComputationSenesiState state, Blackhole blackhole) {
		super.parentsComputation_InMemory(state, blackhole);
	}

	@Override
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationSenesiState state, Blackhole blackhole) {
		super.parentsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void parentsComputation_Sql(SqlParentsComputationSenesiState state, Blackhole blackhole) {
		super.parentsComputation_Sql(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationSenesiState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_InMemory(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationSenesiState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationSenesiState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Sql(state, blackhole);
	}

	@Override
	public void syntheticTest_InMemory(InMemorySyntheticTestSenesiState state, Blackhole blackhole) {
		super.syntheticTest_InMemory(state, blackhole);
	}

	@Override
	public void syntheticTest_Elasticsearch(ElasticsearchSyntheticTestSenesiState state, Blackhole blackhole) {
		super.syntheticTest_Elasticsearch(state, blackhole);
	}

	@Override
	public void syntheticTest_Sql(SqlSyntheticTestSenesiState state, Blackhole blackhole) {
		super.syntheticTest_Sql(state, blackhole);
	}
}
