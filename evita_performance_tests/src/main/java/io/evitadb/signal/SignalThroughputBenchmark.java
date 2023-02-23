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
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This class runs all tests in {@link SignalBenchmark} in throughput mode measurement.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@BenchmarkMode({Mode.Throughput})
public class SignalThroughputBenchmark extends SignalBenchmark {

	@Override
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteSignalState state) {
		super.bulkInsertThroughput_InMemory(state);
	}

	@Override
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteSignalState state) {
		super.bulkInsertThroughput_Elasticsearch(state);
	}

	@Override
	public void bulkInsertThroughput_Sql(SqlBulkWriteSignalState state) {
		super.bulkInsertThroughput_Sql(state);
	}

	@Override
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteSignalState state) {
		super.transactionalUpsertThroughput_InMemory(state);
	}

	@Override
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteSignalState state) {
		super.transactionalUpsertThroughput_Elasticsearch(state);
	}

	@Override
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteSignalState state) {
		super.transactionalUpsertThroughput_Sql(state);
	}

	@Override
	public void singleEntityRead_InMemory(InMemorySingleReadSignalState state, Blackhole blackhole) {
		super.singleEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadSignalState state, Blackhole blackhole) {
		super.singleEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void singleEntityRead_Sql(SqlSingleReadSignalState state, Blackhole blackhole) {
		super.singleEntityRead_Sql(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_InMemory(InMemoryPageReadSignalState state, Blackhole blackhole) {
		super.paginatedEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadSignalState state, Blackhole blackhole) {
		super.paginatedEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Sql(SqlPageReadSignalState state, Blackhole blackhole) {
		super.paginatedEntityRead_Sql(state, blackhole);
	}

	@Override
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringSignalState state, Blackhole blackhole) {
		super.attributeFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringSignalState state, Blackhole blackhole) {
		super.attributeFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeFiltering_Sql(SqlAttributeFilteringSignalState state, Blackhole blackhole) {
		super.attributeFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramSignalState state, Blackhole blackhole) {
		super.attributeHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramSignalState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramSignalState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void priceFiltering_InMemory(InMemoryPriceFilteringSignalState state, Blackhole blackhole) {
		super.priceFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringSignalState state, Blackhole blackhole) {
		super.priceFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceFiltering_Sql(SqlPriceFilteringSignalState state, Blackhole blackhole) {
		super.priceFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramSignalState state, Blackhole blackhole) {
		super.priceHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramSignalState state, Blackhole blackhole) {
		super.priceHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Sql(SqlPriceHistogramSignalState state, Blackhole blackhole) {
		super.priceHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void facetFiltering_InMemory(InMemoryFacetFilteringSignalState state, Blackhole blackhole) {
		super.facetFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringSignalState state, Blackhole blackhole) {
		super.facetFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFiltering_Sql(SqlFacetFilteringSignalState state, Blackhole blackhole) {
		super.facetFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactSignalState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Sql(state, blackhole);
	}

	@Override
	public void parentsComputation_InMemory(InMemoryParentsComputationSignalState state, Blackhole blackhole) {
		super.parentsComputation_InMemory(state, blackhole);
	}

	@Override
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationSignalState state, Blackhole blackhole) {
		super.parentsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void parentsComputation_Sql(SqlParentsComputationSignalState state, Blackhole blackhole) {
		super.parentsComputation_Sql(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationSignalState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_InMemory(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationSignalState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationSignalState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Sql(state, blackhole);
	}

	@Override
	public void syntheticTest_InMemory(InMemorySyntheticTestSignalState state, Blackhole blackhole) {
		super.syntheticTest_InMemory(state, blackhole);
	}

	@Override
	public void syntheticTest_Elasticsearch(ElasticsearchSyntheticTestSignalState state, Blackhole blackhole) {
		super.syntheticTest_Elasticsearch(state, blackhole);
	}

	@Override
	public void syntheticTest_Sql(SqlSyntheticTestSignalState state, Blackhole blackhole) {
		super.syntheticTest_Sql(state, blackhole);
	}
}
