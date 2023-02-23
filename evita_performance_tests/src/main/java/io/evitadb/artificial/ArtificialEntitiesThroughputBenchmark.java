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
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This class runs all tests in {@link ArtificialEntitiesBenchmark} in throughput mode measurement.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@BenchmarkMode({Mode.Throughput})
public class ArtificialEntitiesThroughputBenchmark extends ArtificialEntitiesBenchmark {

	@Override
	public void bulkInsertThroughput_InMemory(InMemoryBulkWriteArtificialState state) {
		super.bulkInsertThroughput_InMemory(state);
	}

	@Override
	public void bulkInsertThroughput_Elasticsearch(ElasticsearchBulkWriteArtificialState state) {
		super.bulkInsertThroughput_Elasticsearch(state);
	}

	@Override
	public void bulkInsertThroughput_Sql(SqlBulkWriteArtificialState state) {
		super.bulkInsertThroughput_Sql(state);
	}

	@Override
	public void transactionalUpsertThroughput_InMemory(InMemoryTransactionalWriteArtificialState state) {
		super.transactionalUpsertThroughput_InMemory(state);
	}

	@Override
	public void transactionalUpsertThroughput_Elasticsearch(ElasticsearchTransactionalWriteArtificialState state) {
		super.transactionalUpsertThroughput_Elasticsearch(state);
	}

	@Override
	public void transactionalUpsertThroughput_Sql(SqlTransactionalWriteArtificialState state) {
		super.transactionalUpsertThroughput_Sql(state);
	}

	@Override
	public void singleEntityRead_InMemory(InMemorySingleReadArtificialState state, Blackhole blackhole) {
		super.singleEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void singleEntityRead_Elasticsearch(ElasticsearchSingleReadArtificialState state, Blackhole blackhole) {
		super.singleEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void singleEntityRead_Sql(SqlSingleReadArtificialState state, Blackhole blackhole) {
		super.singleEntityRead_Sql(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_InMemory(InMemoryPageReadArtificialState state, Blackhole blackhole) {
		super.paginatedEntityRead_InMemory(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Elasticsearch(ElasticsearchPageReadArtificialState state, Blackhole blackhole) {
		super.paginatedEntityRead_Elasticsearch(state, blackhole);
	}

	@Override
	public void paginatedEntityRead_Sql(SqlPageReadArtificialState state, Blackhole blackhole) {
		super.paginatedEntityRead_Sql(state, blackhole);
	}

	@Override
	public void attributeFiltering_InMemory(InMemoryAttributeFilteringArtificialState state, Blackhole blackhole) {
		super.attributeFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeFiltering_Elasticsearch(ElasticsearchAttributeFilteringArtificialState state, Blackhole blackhole) {
		super.attributeFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeFiltering_Sql(SqlAttributeFilteringArtificialState state, Blackhole blackhole) {
		super.attributeFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_InMemory(InMemoryAttributeAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Elasticsearch(ElasticsearchAttributeAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeAndHierarchyFiltering_Sql(SqlAttributeAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.attributeAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_InMemory(InMemoryAttributeHistogramArtificialState state, Blackhole blackhole) {
		super.attributeHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Elasticsearch(ElasticsearchAttributeHistogramArtificialState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void attributeHistogramComputation_Sql(SqlAttributeHistogramArtificialState state, Blackhole blackhole) {
		super.attributeHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void priceFiltering_InMemory(InMemoryPriceFilteringArtificialState state, Blackhole blackhole) {
		super.priceFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceFiltering_Elasticsearch(ElasticsearchPriceFilteringArtificialState state, Blackhole blackhole) {
		super.priceFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceFiltering_Sql(SqlPriceFilteringArtificialState state, Blackhole blackhole) {
		super.priceFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_InMemory(InMemoryPriceAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Elasticsearch(ElasticsearchPriceAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceAndHierarchyFiltering_Sql(SqlPriceAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.priceAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_InMemory(InMemoryPriceHistogramArtificialState state, Blackhole blackhole) {
		super.priceHistogramComputation_InMemory(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Elasticsearch(ElasticsearchPriceHistogramArtificialState state, Blackhole blackhole) {
		super.priceHistogramComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void priceHistogramComputation_Sql(SqlPriceHistogramArtificialState state, Blackhole blackhole) {
		super.priceHistogramComputation_Sql(state, blackhole);
	}

	@Override
	public void facetFiltering_InMemory(InMemoryFacetFilteringArtificialState state, Blackhole blackhole) {
		super.facetFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetFiltering_Elasticsearch(ElasticsearchFacetFilteringArtificialState state, Blackhole blackhole) {
		super.facetFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFiltering_Sql(SqlFacetFilteringArtificialState state, Blackhole blackhole) {
		super.facetFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_InMemory(InMemoryFacetAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFiltering_Sql(SqlFacetAndHierarchyFilteringArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFiltering_Sql(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_InMemory(InMemoryFacetFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetFilteringAndSummarizingCount_Sql(SqlFacetFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		super.facetFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingCount_Sql(SqlFacetAndHierarchyFilteringAndSummarizingCountArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingCount_Sql(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_InMemory(InMemoryFacetAndHierarchyFilteringAndSummarizingImpactArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_InMemory(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(ElasticsearchFacetAndHierarchyFilteringAndSummarizingImpactArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Elasticsearch(state, blackhole);
	}

	@Override
	public void facetAndHierarchyFilteringAndSummarizingImpact_Sql(SqlFacetAndHierarchyFilteringAndSummarizingImpactArtificialState state, Blackhole blackhole) {
		super.facetAndHierarchyFilteringAndSummarizingImpact_Sql(state, blackhole);
	}

	@Override
	public void parentsComputation_InMemory(InMemoryParentsComputationArtificialState state, Blackhole blackhole) {
		super.parentsComputation_InMemory(state, blackhole);
	}

	@Override
	public void parentsComputation_Elasticsearch(ElasticsearchParentsComputationArtificialState state, Blackhole blackhole) {
		super.parentsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void parentsComputation_Sql(SqlParentsComputationArtificialState state, Blackhole blackhole) {
		super.parentsComputation_Sql(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_InMemory(InMemoryHierarchyStatisticsComputationArtificialState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_InMemory(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Elasticsearch(ElasticsearchHierarchyStatisticsComputationArtificialState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Elasticsearch(state, blackhole);
	}

	@Override
	public void hierarchyStatisticsComputation_Sql(SqlHierarchyStatisticsComputationArtificialState state, Blackhole blackhole) {
		super.hierarchyStatisticsComputation_Sql(state, blackhole);
	}
}
