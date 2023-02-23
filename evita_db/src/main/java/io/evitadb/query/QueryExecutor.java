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

package io.evitadb.query;

import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.base.NotFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula.PrefetchFormulaVisitor;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultPlanningVisitor;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.indexSelection.IndexSelectionResult;
import io.evitadb.query.indexSelection.IndexSelectionVisitor;
import io.evitadb.query.indexSelection.TargetIndexes;
import io.evitadb.query.response.QueryTelemetry;
import io.evitadb.query.response.QueryTelemetry.QueryPhase;
import io.evitadb.query.sort.NoSorter;
import io.evitadb.query.sort.OrderByVisitor;
import io.evitadb.query.sort.Sorter;
import lombok.*;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * {@link QueryExecutor} translates {@link EvitaRequest} to a {@link QueryPlan}. It has to main functions:
 * <p>
 * - to choose the best index(es) to be used in query execution
 * - to construct the {@link QueryPlan} body that consists of a tree of formulas
 * <p>
 * Query executor doesn't really compute the result - only prepares the recipe for computing it. Result is computed
 * after {@link QueryPlan#execute()} is called. Preparation of the {@link QueryPlan} should be really fast and can be
 * called anytime without big performance penalty.
 * <p>
 * Query executor uses <a href="https://en.wikipedia.org/wiki/Visitor_pattern">Visitor</a> pattern to translate tree
 * of {@link FilterConstraint} to a tree of {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryExecutor {

	public static QueryPlan planQuery(@Nonnull QueryContext context) {
		final QueryTelemetry planningStep = context.addStep(QueryPhase.PLANNING);

		// determine the indexes that should be used for filtering
		final IndexSelectionResult indexSelectionResult = selectIndexes(context, planningStep);

		// if we found empty target index, we may quickly return empty result - one key condition is not fulfilled
		if (indexSelectionResult.isEmpty()) {
			planningStep.finish();
			return new QueryPlan(
				context, EmptyFormula.INSTANCE, NoSorter.INSTANCE, Collections.emptyList()
			);
		}

		// create filtering formula and pick the formula with least estimated costs
		// this should be pretty fast - no computation is done yet
		final FormulaWithTargetIndex filterResult = createFilterFormula(
			context, planningStep, indexSelectionResult
		);

		// create sorter
		final Sorter sorter = createSorter(
			context, planningStep, filterResult.getFormula()
		);

		// create EvitaResponseExtraResult producers
		final Collection<ExtraResultProducer> extraResultProducers = createExtraResultProducers(
			context,
			filterResult.getTargetIndexes(),
			planningStep,
			filterResult.getFormula(),
			sorter
		);

		// return query plan
		final QueryPlan queryPlan = new QueryPlan(
			context, filterResult.getFormula(), sorter, extraResultProducers
		);

		planningStep.finish();
		return queryPlan;
	}

	/**
	 * Method analyzes the input query and picks multiple {@link EntityIndex} sets that can be interchangeably used to
	 * construct response to the query. Currently, the logic is quite stupid - it searches the filter for all constraints
	 * within AND relation and when relation or hierarchy constraint is encountered, it adds specific
	 * {@link EntityIndexType#REFERENCED_ENTITY} or {@link EntityIndexType#REFERENCED_HIERARCHY_NODE} that contains
	 * limited subset of the entities related to that placement/relation.
	 */
	private static IndexSelectionResult selectIndexes(
		@Nonnull QueryContext queryContext,
		@Nonnull QueryTelemetry planningStep
	) {
		final QueryTelemetry indexSelection = planningStep.addStep(QueryPhase.PLANNING_INDEX_USAGE);
		final IndexSelectionVisitor indexSelectionVisitor = new IndexSelectionVisitor(queryContext);
		ofNullable(queryContext.getFilterBy()).ifPresent(indexSelectionVisitor::visit);
		indexSelection.finish();
		return new IndexSelectionResult(
			indexSelectionVisitor.getTargetIndexes(),
			indexSelectionVisitor.isTargetIndexQueriedByOtherConstraints()
		);
	}

	/**
	 * Method creates multiple filter formulas for each of the {@link IndexSelectionResult#getTargetIndexes()} using
	 * specialized visitor that goes through input query. Creating formulas is relatively inexpensive - no computation
	 * really happens, only the execution tree is constructed. For each {@link IndexSelectionResult#getTargetIndexes()}
	 * one formula is created. From all of those formulas only single one is selected, the one with least estimated cost.
	 */
	@Nonnull
	private static FormulaWithTargetIndex createFilterFormula(
		@Nonnull QueryContext queryContext,
		@Nonnull QueryTelemetry planningStep,
		@Nonnull IndexSelectionResult indexSelectionResult
	) {
		final QueryTelemetry filterPlan = planningStep.addStep(QueryPhase.PLANNING_FILTER);
		Formula mostOptimalFilterFormula = null;
		TargetIndexes selectedIndex = null;
		PrefetchFormulaVisitor mostOptimalPrefetchVisitor = null;
		for (TargetIndexes targetIndex : indexSelectionResult.getTargetIndexes()) {
			final QueryTelemetry filterAlternative = filterPlan.addStep(QueryPhase.PLANNING_FILTER_ALTERNATIVE);
			final FilterByVisitor filterByVisitor = new FilterByVisitor(
				queryContext,
				indexSelectionResult.getTargetIndexes(),
				targetIndex,
				indexSelectionResult.isTargetIndexQueriedByOtherConstraints()
			);

			final PrefetchFormulaVisitor prefetchFormulaVisitor;
			// we may use prefetch only when using global index
			// in case of narrowed indexes some constraint may be omitted - due the implicit lack of certain entities
			// in such index - this would then must be taken into an account in SelectableFormula which would also
			// limit its performance boost to a large extent
			if (targetIndex.isGlobalIndex()) {
				prefetchFormulaVisitor = new PrefetchFormulaVisitor();
				filterByVisitor.registerFormulaPostProcessorIfNotPresent(prefetchFormulaVisitor);
			} else {
				prefetchFormulaVisitor = null;
			}

			ofNullable(queryContext.getFilterBy()).ifPresent(filterByVisitor::visit);
			final Formula adeptFormula = queryContext.analyse(filterByVisitor.getFormula());
			if (mostOptimalFilterFormula == null || adeptFormula.getEstimatedCost() < mostOptimalFilterFormula.getEstimatedCost()) {
				selectedIndex = targetIndex;
				mostOptimalFilterFormula = adeptFormula;
				mostOptimalPrefetchVisitor = prefetchFormulaVisitor;
			}
			filterAlternative.finish(targetIndex.toStringWithCosts(adeptFormula.getEstimatedCost()));
		}

		filterPlan.finish("Selected index: " + selectedIndex);
		// formula should never be null
		final Formula finalFormula = mostOptimalFilterFormula == null ? EmptyFormula.INSTANCE : mostOptimalFilterFormula;

		// check whether we can prefetch (and is worthwhile to do so) the requested entities
		if (mostOptimalPrefetchVisitor != null) {
			SelectionFormula.prefetchEntities(queryContext, mostOptimalPrefetchVisitor);
		}

		return new FormulaWithTargetIndex(finalFormula, selectedIndex);
	}

	/**
	 * Method creates instance of {@link Sorter} that sorts result of the filtering formula according to input query,
	 * and slices appropriate part of the result to respect limit/offset requirements from the query. No sorting/slicing
	 * is done in this method, only the instance of {@link Sorter} capable of doing it is created and returned.
	 */
	private static Sorter createSorter(
		@Nonnull QueryContext queryContext,
		@Nonnull QueryTelemetry planningStep,
		@Nonnull Formula filterFormula
	) {
		final QueryTelemetry extraResultPlanning = planningStep.addStep(QueryPhase.PLANNING_EXTRA_RESULT_FABRICATION);
		final OrderByVisitor orderByVisitor = new OrderByVisitor(queryContext, filterFormula);
		ofNullable(queryContext.getOrderBy()).ifPresent(orderByVisitor::visit);
		final Sorter sorter = orderByVisitor.getSorter();
		extraResultPlanning.finish();
		return sorter;
	}

	/**
	 * Method creates list of {@link ExtraResultProducer} implementations that fabricate requested extra data structures
	 * that are somehow connected with the processed query taking existing formula and their memoized results into
	 * account (which is a great advantage comparing to computation in multiple requests as needed in other database
	 * solutions).
	 */
	private static Collection<ExtraResultProducer> createExtraResultProducers(
		@Nonnull QueryContext queryContext,
		@Nonnull TargetIndexes indexSetToUse,
		@Nonnull QueryTelemetry planningStep,
		@Nonnull Formula filterFormula,
		@Nonnull Sorter sorter
	) {
		final QueryTelemetry sortPlan = planningStep.addStep(QueryPhase.PLANNING_EXTRA_RESULT_FABRICATION);
		final ExtraResultPlanningVisitor extraResultPlanner = new ExtraResultPlanningVisitor(
			queryContext,
			indexSetToUse,
			filterFormula,
			sorter
		);
		ofNullable(queryContext.getRequire()).ifPresent(extraResultPlanner::visit);
		final Set<ExtraResultProducer> extraResultProducers = extraResultPlanner.getExtraResultProducers();
		sortPlan.finish();
		return extraResultProducers;
	}

	/**
	 * Simple carrier object for {@link #createFilterFormula(QueryContext, QueryTelemetry, IndexSelectionResult)}.
	 */
	@Data
	private static class FormulaWithTargetIndex {
		private final Formula formula;
		private final TargetIndexes targetIndexes;
	}

	/*
		THIS CLASS IS ONLY CONTEMPORARY FAKE CLASS - IT SHOULD NEVER BE USED FOR REAL COMPUTATION!!!
	 */

	/**
	 * This special case of {@link AbstractFormula} is used for negative constraints. These constraint results need to be
	 * compared against certain superset which is the output of the computation on the same level or in the case
	 * of the root constraint the entire superset of the index.
	 */
	@RequiredArgsConstructor
	public static class FutureNotFormula extends AbstractFormula {
		private static final String ERROR_TEMPORARY = "FutureNotFormula is only temporary placeholder!";
		/**
		 * This formula represents the real formula to compute the negated set.
		 */
		@Getter private final Formula innerFormula;

		/**
		 * This method is used to compose the final formula that takes collection of formulas on the current level
		 * of the query and wraps them to the final "not" formula.
		 *
		 * Method produces these results from these example formulas (in case aggregator function produces `and`):
		 *
		 * - [ANY_FORMULA, ANY_FORMULA] -> [ANY_FORMULA, ANY_FORMULA]
		 * - [ANY_FORMULA, FUTURE_NOT_FORMULA] -> not(FUTURE_NOT_FORMULA, ANY_FORMULA)
		 * - [ANY_FORMULA, ANY_FORMULA, FUTURE_NOT_FORMULA, FUTURE_NOT_FORMULA] -> not(and(FUTURE_NOT_FORMULA,FUTURE_NOT_FORMULA), and(ANY_FORMULA, ANY_FORMULA))
		 * - [FUTURE_NOT_FORMULA] -> not(FUTURE_NOT_FORMULA, superSetFormula) ... or exception when not on first level of query
		 * - [FUTURE_NOT_FORMULA, FUTURE_NOT_FORMULA] -> not(and(FUTURE_NOT_FORMULA, FUTURE_NOT_FORMULA), superSetFormula) ... or exception when not on first level of query
		 */
		public static Formula postProcess(@Nonnull Formula[] collectedFormulas, @Nonnull Function<Formula[], Formula> aggregator) {
			return postProcess(collectedFormulas, aggregator, null);
		}

		/**
		 * This method is used to compose the final formula that takes collection of formulas on the current level
		 * of the query and wraps them to the final "not" formula.
		 *
		 * Method produces these results from these example formulas (in case aggregator function produces `and`):
		 *
		 * - [ANY_FORMULA, ANY_FORMULA] -> [ANY_FORMULA, ANY_FORMULA]
		 * - [ANY_FORMULA, FUTURE_NOT_FORMULA] -> not(FUTURE_NOT_FORMULA, ANY_FORMULA)
		 * - [ANY_FORMULA, ANY_FORMULA, FUTURE_NOT_FORMULA, FUTURE_NOT_FORMULA] -> not(and(FUTURE_NOT_FORMULA,FUTURE_NOT_FORMULA), and(ANY_FORMULA, ANY_FORMULA))
		 * - [FUTURE_NOT_FORMULA] -> not(FUTURE_NOT_FORMULA, superSetFormula) ... or exception when not on first level of query
		 * - [FUTURE_NOT_FORMULA, FUTURE_NOT_FORMULA] -> not(and(FUTURE_NOT_FORMULA, FUTURE_NOT_FORMULA), superSetFormula) ... or exception when not on first level of query
		 */
		public static Formula postProcess(@Nonnull Formula[] collectedFormulas, @Nonnull Function<Formula[], Formula> aggregator, @Nullable Supplier<Formula> superSetFormulaSupplier) {
			/* collect all negative formulas */
			final Formula[] notFormulas = Arrays.stream(collectedFormulas)
				.filter(FutureNotFormula.class::isInstance)
				.map(FutureNotFormula.class::cast)
				.map(FutureNotFormula::getInnerFormula)
				.toArray(Formula[]::new);
			/* if there are none - just wrap positive formulas with aggregator function */
			if (notFormulas.length == 0) {
				return aggregator.apply(collectedFormulas);
			} else {
				/* collect all positive formulas */
				final Formula[] otherFormulas = Arrays.stream(collectedFormulas)
					.filter(it -> !(it instanceof FutureNotFormula))
					.toArray(Formula[]::new);
				/* if there are none - i.e. we have only negative formulas */
				if (ArrayUtils.isEmpty(otherFormulas)) {
					/* access superset formula  */
					if (superSetFormulaSupplier != null) {
						final Formula superSetFormula = superSetFormulaSupplier.get();
						/* construct not formula using aggregator function if there are multiple negative formulas */
						return new NotFormula(
							notFormulas.length == 1 ? notFormulas[0] : aggregator.apply(notFormulas),
							superSetFormula
						);
						/* delegate FutureNotFormula to upper level */
					} else {
						return new FutureNotFormula(
							notFormulas.length == 1 ? notFormulas[0] : aggregator.apply(notFormulas)
						);
					}
				} else {
					/* construct not formula using aggregator function if there are multiple negative / positive formulas */
					return new NotFormula(
						notFormulas.length == 1 ? notFormulas[0] : aggregator.apply(notFormulas),
						otherFormulas.length == 1 ? otherFormulas[0] : aggregator.apply(otherFormulas)
					);
				}
			}
		}

		@Nonnull
		@Override
		public Formula getCloneWithInnerFormulas(@Nonnull Formula... innerFormulas) {
			throw new UnsupportedOperationException(ERROR_TEMPORARY);
		}

		@Override
		public long getOperationCost() {
			throw new UnsupportedOperationException(ERROR_TEMPORARY);
		}

		@Nonnull
		@Override
		protected Bitmap computeInternal() {
			throw new UnsupportedOperationException(ERROR_TEMPORARY);
		}

		@Override
		public int getEstimatedCardinality() {
			throw new UnsupportedOperationException(ERROR_TEMPORARY);
		}

		@Override
		protected long includeAdditionalHash(@Nonnull LongHashFunction hashFunction) {
			throw new UnsupportedOperationException(ERROR_TEMPORARY);
		}

		@Override
		protected long getClassId() {
			throw new UnsupportedOperationException(ERROR_TEMPORARY);
		}
	}

}
