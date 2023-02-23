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
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This class runs all tests in {@link KeramikaSoukupBenchmark} in latency mode measurement.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@BenchmarkMode({Mode.AverageTime})
public class KeramikaSoukupLatencyBenchmark extends KeramikaSoukupBenchmark {

	@Override
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteKeramikaSoukupState state) {
		super.bulkInsertThroughput_InMemory(state);
	}

	@Override
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteKeramikaSoukupState state) {
		super.bulkInsertThroughput_Elasticsearch(state);
	}

	@Override
	public void bulkInsertThroughput_Sql(SqlBulkWriteKeramikaSoukupState state) {
		super.bulkInsertThroughput_Sql(state);
	}

	@Override
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteKeramikaSoukupState state) {
		super.transactionalUpsertThroughput_InMemory(state);
	}

	@Override
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteKeramikaSoukupState state) {
		super.transactionalUpsertThroughput_Elasticsearch(state);
	}

	@Override
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteKeramikaSoukupState state) {
		super.transactionalUpsertThroughput_Sql(state);
	}

	@Override
	public void singleEntityRead_InMemory(InMemorySingleReadKeramikaSoukupState state, Blackhole blackhole) {
		super.singleEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadKeramikaSoukupState state, Blackhole blackhole) {
		super.singleEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void singleEntityRead_Sql(SqlSingleReadKeramikaSoukupState state, Blackhole blackhole) {
		super.singleEntityRead_Sql(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_InMemory(InMemoryPageReadKeramikaSoukupState state, Blackhole blackhole) {
		super.paginatedEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadKeramikaSoukupState state, Blackhole blackhole) {
		super.paginatedEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Sql(SqlPageReadKeramikaSoukupState state, Blackhole blackhole) {
		super.paginatedEntityRead_Sql(state, blackhole);
	}

	@Override
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeFiltering_Sql(SqlAttributeFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramKeramikaSoukupState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void priceFiltering_InMemory(InMemoryPriceFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.priceFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.priceFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceFiltering_Sql(SqlPriceFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.priceFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramKeramikaSoukupState state, Blackhole blackhole) {
		super.priceHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramKeramikaSoukupState state, Blackhole blackhole) {
		super.priceHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Sql(SqlPriceHistogramKeramikaSoukupState state, Blackhole blackhole) {
		super.priceHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void facetFiltering_InMemory(InMemoryFacetFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.facetFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.facetFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFiltering_Sql(SqlFacetFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.facetFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactKeramikaSoukupState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Sql(state, blackhole);
	}

	@Override
	public void parentsComputation_InMemory(InMemoryParentsComputationKeramikaSoukupState state, Blackhole blackhole) {
		super.parentsComputation_InMemory(state, blackhole);
	}

	@Override
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationKeramikaSoukupState state, Blackhole blackhole) {
		super.parentsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void parentsComputation_Sql(SqlParentsComputationKeramikaSoukupState state, Blackhole blackhole) {
		super.parentsComputation_Sql(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationKeramikaSoukupState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_InMemory(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationKeramikaSoukupState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationKeramikaSoukupState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Sql(state, blackhole);
	}

	@Override
	public void syntheticTest_InMemory(InMemorySyntheticTestKeramikaSoukupState state, Blackhole blackhole) {
		super.syntheticTest_InMemory(state, blackhole);
	}

	@Override
	public void syntheticTest_Elasticsearch(ElasticsearchSyntheticTestKeramikaSoukupState state, Blackhole blackhole) {
		super.syntheticTest_Elasticsearch(state, blackhole);
	}

	@Override
	public void syntheticTest_Sql(SqlSyntheticTestKeramikaSoukupState state, Blackhole blackhole) {
		super.syntheticTest_Sql(state, blackhole);
	}

}
