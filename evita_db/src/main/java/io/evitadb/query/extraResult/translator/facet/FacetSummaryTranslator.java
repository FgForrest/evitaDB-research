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

package io.evitadb.query.extraResult.translator.facet;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import io.evitadb.api.query.require.FacetSummary;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.facet.FacetEntityTypeIndex;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.facet.FacetGroupFormula;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder.LookUp;
import io.evitadb.query.extraResult.ExtraResultPlanningVisitor;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.extraResult.translator.RequireConstraintTranslator;
import io.evitadb.query.extraResult.translator.facet.producer.FacetSummaryProducer;
import io.evitadb.query.indexSelection.TargetIndexes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This implementation of {@link RequireConstraintTranslator} converts {@link FacetSummary} to {@link FacetSummaryProducer}.
 * The producer instance has all pointer necessary to compute result. All operations in this translator are relatively
 * cheap comparing to final result computation, that is deferred to {@link ExtraResultProducer#fabricate(List)} method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetSummaryTranslator implements RequireConstraintTranslator<FacetSummary> {

	@Override
	public ExtraResultProducer apply(FacetSummary facetSummary, ExtraResultPlanningVisitor extraResultPlanner) {
		// find user filters that enclose variable user defined part
		final Set<Formula> userFilters = extraResultPlanner.getUserFilteringFormula();
		// find all requested facets
		final Map<Serializable, IntSet> requestedFacets = userFilters
			.stream()
			.flatMap(it -> FormulaFinder.find(it, FacetGroupFormula.class, LookUp.SHALLOW).stream())
			.collect(
				Collectors.groupingBy(
					FacetGroupFormula::getFacetType,
					Collectors.mapping(
						FacetGroupFormula::getFacetIds,
						new IntArrayToIntSetCollector()
					)
				)
			);
		// collect all facet statistics
		final TargetIndexes indexSetToUse = extraResultPlanner.getIndexSetToUse();
		final List<Map<Serializable, FacetEntityTypeIndex>> facetIndexes = indexSetToUse.getIndexes()
			.stream()
			.map(EntityIndex::getFacetingEntities)
			.collect(Collectors.toList());
		// now create the producer instance that has all pointer necessary to compute result
		// all operations above should be relatively cheap comparing to final result computation, that is deferred
		// to FacetSummaryProducer#fabricate method
		return new FacetSummaryProducer(
			extraResultPlanner.getQueryContext(),
			facetSummary.getFacetStatisticsDepth(),
			extraResultPlanner.getFilteringFormula(),
			facetIndexes,
			requestedFacets
		);
	}

	/**
	 * Collector helps to accumulate arrays of possible duplicated integers in {@link IntSet}.
	 */
	private static class IntArrayToIntSetCollector implements Collector<int[], IntHashSet, IntSet> {

		@Override
		public Supplier<IntHashSet> supplier() {
			return IntHashSet::new;
		}

		@Override
		public BiConsumer<IntHashSet, int[]> accumulator() {
			return (acc, recs) -> Arrays.stream(recs).forEach(acc::add);
		}

		@Override
		public BinaryOperator<IntHashSet> combiner() {
			return (left, right) -> {
				left.addAll(right);
				return left;
			};
		}

		@Override
		public Function<IntHashSet, IntSet> finisher() {
			return acc -> acc;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Set.of(
				Characteristics.UNORDERED,
				Characteristics.CONCURRENT
			);
		}
	}
}
