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

package io.evitadb.query.extraResult;

import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.query.*;
import io.evitadb.api.query.require.*;
import io.evitadb.index.EntityIndex;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.facet.UserFilterFormula;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder.LookUp;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.translator.RequireConstraintTranslator;
import io.evitadb.query.extraResult.translator.RequireTranslator;
import io.evitadb.query.extraResult.translator.facet.FacetSummaryTranslator;
import io.evitadb.query.extraResult.translator.hierarchyStatistics.HierarchyStatisticsTranslator;
import io.evitadb.query.extraResult.translator.histogram.AttributeHistogramTranslator;
import io.evitadb.query.extraResult.translator.histogram.PriceHistogramTranslator;
import io.evitadb.query.extraResult.translator.parents.ParentsOfTypeTranslator;
import io.evitadb.query.extraResult.translator.parents.ParentsTranslator;
import io.evitadb.query.indexSelection.TargetIndexes;
import io.evitadb.query.sort.Sorter;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * This {@link ConstraintVisitor} translates tree of {@link RequireConstraint} to a list of {@link ExtraResultProducer}.
 * Visitor represents the "planning" phase for the requirement resolution. The planning should be as light-weight as
 * possible.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExtraResultPlanningVisitor implements ConstraintVisitor<RequireConstraint> {
	private static final Map<Class<? extends RequireConstraint>, RequireConstraintTranslator<? extends RequireConstraint>> TRANSLATORS;

	/* initialize list of all RequireConstraint handlers once for a lifetime */
	static {
		TRANSLATORS = createHashMap(8);
		TRANSLATORS.put(Require.class, new RequireTranslator());
		TRANSLATORS.put(FacetSummary.class, new FacetSummaryTranslator());
		TRANSLATORS.put(Parents.class, new ParentsTranslator());
		TRANSLATORS.put(ParentsOfType.class, new ParentsOfTypeTranslator());
		TRANSLATORS.put(AttributeHistogram.class, new AttributeHistogramTranslator());
		TRANSLATORS.put(PriceHistogram.class, new PriceHistogramTranslator());
		TRANSLATORS.put(HierarchyStatistics.class, new HierarchyStatisticsTranslator());
	}

	/**
	 * Reference to the query context that allows to access entity bodies, indexes, original request and much more.
	 */
	@Delegate @Getter private final QueryContext queryContext;
	/**
	 * This instance contains the {@link EntityIndex} set that is used to resolve passed query filter.
	 */
	@Getter private final TargetIndexes indexSetToUse;
	/**
	 * Contains filtering formula tree that was used to produce results so that computed sub-results can be used for
	 * sorting.
	 */
	@Getter private final Formula filteringFormula;
	/**
	 * Contains prepared sorter implementation that takes output of the {@link #filteringFormula} and sorts the entity
	 * primary keys according to {@link OrderConstraint} in {@link EvitaRequest}.
	 */
	@Getter private final Sorter sorter;
	/**
	 * Contains the list of producers that react to passed requirements.
	 */
	@Getter private final LinkedHashSet<ExtraResultProducer> extraResultProducers = new LinkedHashSet<>();
	/**
	 * Contains set (usually of size == 1 or 0) that contains references to the {@link UserFilterFormula} inside
	 * {@link #filteringFormula}. This is a helper field that allows to reuse result of the formula search multiple
	 * times.
	 */
	private Set<Formula> userFilterFormula;

	public ExtraResultPlanningVisitor(
		@Nonnull QueryContext queryContext,
		@Nonnull TargetIndexes indexSetToUse,
		@Nonnull Formula filteringFormula,
		@Nonnull Sorter sorter
	) {
		this.queryContext = queryContext;
		this.indexSetToUse = indexSetToUse;
		this.filteringFormula = filteringFormula;
		this.sorter = sorter;
	}

	/**
	 * Method finds existing {@link ExtraResultProducer} implementation of particular `producerClass` allowing multiple
	 * translators to reuse (enrich) it.
	 */
	@Nullable
	public <T extends ExtraResultProducer> T findExistingProducer(Class<T> producerClass) {
		for (ExtraResultProducer extraResultProducer : extraResultProducers) {
			if (producerClass.isInstance(extraResultProducer)) {
				//noinspection unchecked
				return (T) extraResultProducer;
			}
		}
		return null;
	}

	/**
	 * Returns set (usually of size == 1 or 0) that contains references to the {@link UserFilterFormula} inside
	 * {@link #filteringFormula}. Result of this method is cached so that additional calls introduce no performance
	 * penalty.
	 */
	@Nonnull
	public Set<Formula> getUserFilteringFormula() {
		if (userFilterFormula == null) {
			userFilterFormula = new HashSet<>(
				FormulaFinder.find(
					getFilteringFormula(), UserFilterFormula.class, LookUp.SHALLOW
				)
			);
		}
		return userFilterFormula;
	}

	/**
	 * Method finds sorter of specified type in the current {@link #sorter} or sorters that are chained in it as secondary
	 * or tertiary sorters.
	 */
	@Nullable
	public <T extends Sorter> T findSorter(@Nonnull Class<T> sorterType) {
		Sorter theSorter = this.sorter;
		while (theSorter != null) {
			if (sorterType.isInstance(theSorter)) {
				//noinspection unchecked
				return (T) theSorter;
			}
			theSorter = theSorter.getNextSorter();
		}
		return null;
	}

	@Override
	public void visit(@Nonnull RequireConstraint constraint) {
		if (constraint instanceof ExtraResultRequireConstraint) {
			@SuppressWarnings("unchecked") final RequireConstraintTranslator<RequireConstraint> translator =
				(RequireConstraintTranslator<RequireConstraint>) TRANSLATORS.get(constraint.getClass());
			isTrue(
				translator != null,
				() -> new IllegalStateException("No translator found for constraint `" + constraint.getClass() + "`!")
			);

			// if constraint is a container constraint
			if (constraint instanceof ConstraintContainer) {
				@SuppressWarnings("unchecked") final ConstraintContainer<RequireConstraint> container = (ConstraintContainer<RequireConstraint>) constraint;
				// process children constraints
				if (!(translator instanceof SelfTraversingTranslator)) {
					for (RequireConstraint subConstraint : container) {
						subConstraint.accept(this);
					}
				} else {
					ofNullable(translator.apply(constraint, this)).ifPresent(extraResultProducers::add);
				}
			} else if (constraint instanceof ConstraintLeaf) {
				// process the leaf constraint
				ofNullable(translator.apply(constraint, this)).ifPresent(extraResultProducers::add);
			} else {
				// sanity check only
				throw new IllegalStateException("Should never happen");
			}
		} else if (constraint instanceof ConstraintContainer) {
			@SuppressWarnings("unchecked") final ConstraintContainer<RequireConstraint> container = (ConstraintContainer<RequireConstraint>) constraint;
			for (RequireConstraint subConstraint : container) {
				subConstraint.accept(this);
			}
		}
	}

}
