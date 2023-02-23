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

package io.evitadb.query.algebra.deferred;

import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.FormulaPostProcessor;
import io.evitadb.query.algebra.FormulaVisitor;
import io.evitadb.query.algebra.base.AndFormula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.facet.UserFilterFormula;
import io.evitadb.query.algebra.price.FilteredPriceRecordAccessor;
import io.evitadb.query.algebra.price.filteredPriceRecords.FilteredPriceRecords;
import io.evitadb.query.algebra.price.filteredPriceRecords.ResolvedFilteredPriceRecords;
import io.evitadb.query.context.QueryContext;
import lombok.Getter;
import net.openhft.hashing.LongHashFunction;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Selection formula is an optimization opportunity that can compute its results in two different ways, and it chooses
 * the one promising better results.
 *
 * 1. standard way of computing results is via {@link #getDelegate()} formula - but it may require quite a lot of computations
 * 2. alternative way of computing results is via {@link #alternative} filter that can operate only when explicit IDs
 * are present in request in conjunction form (AND)
 *
 * For very small set of entities known upfront it's beneficial to fetch their bodies from the datastore and apply
 * filtering on real data instead of operating on large bitmaps present in index. This form of filtering is even better
 * in case the entity is also required in the response. This approach is targeted especially on methods that retrieve
 * the entity with its contents by primary key or unique attribute. In such case entity would be fetched from
 * the underlying data store anyway, so when we prefetch it - we may avoid expensive bitmap joins to check additional
 * constraints of the request.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class SelectionFormula extends AbstractFormula implements FilteredPriceRecordAccessor {
	private static final long CLASS_ID = 3311110127363103780L;
	/**
	 * Contains {@link QueryContext} used with the query.
	 */
	private final QueryContext queryContext;
	/**
	 * Contains the alternative computation based on entity contents filtering.
	 */
	private final EntityToBitmapFilter alternative;

	public SelectionFormula(@Nonnull QueryContext queryContext, @Nonnull Formula delegate, @Nonnull EntityToBitmapFilter alternative) {
		super(delegate);
		this.queryContext = queryContext;
		this.alternative = alternative;
	}

	/**
	 * Method will prefetch the entities identified in {@link PrefetchFormulaVisitor} but only in case the prefetch
	 * is possible and would "pay off". In case the possible prefetching would be more costly than executing the standard
	 * filtering logic, the prefetch is not executed.
	 */
	public static void prefetchEntities(@Nonnull QueryContext queryContext, @Nonnull PrefetchFormulaVisitor prefetchFormulaVisitor) {
		// do we know entity ids to prefetch?
		if (prefetchFormulaVisitor.isPrefetchPossible()) {
			final Bitmap entitiesToPrefetch = prefetchFormulaVisitor.getConjunctiveEntities();
			final EntityContentRequire[] requirements = prefetchFormulaVisitor.getRequirements();
			// does the prefetch pay off?
			if (prefetchFormulaVisitor.getExpectedComputationalCosts() > estimatePrefetchCost(entitiesToPrefetch.size(), requirements)) {
				queryContext.prefetchEntities(entitiesToPrefetch, requirements);
			}
		}
	}

	/**
	 * We've performed benchmark of reading data from disk - using Linux file cache the reading performance was:
	 *
	 * Benchmark                                Mode  Cnt       Score   Error  Units
	 * SenesiThroughputBenchmark.memTableRead  thrpt       140496.829          ops/s
	 *
	 * For two storage parts - this means 280992 reads / sec. When the linux cache would be empty it would require I/O
	 * which may be 40x times slower (source: https://www.quora.com/Is-the-speed-of-SSD-and-RAM-the-same) for 4kB payload
	 * it means that the lowest expectations are 6782 reads / sec.
	 *
	 * Recomputed on 1. mil operations ({@link io.evitadb.spike.FormulaCostMeasurement}) it's cost of 148.
	 */
	private static long estimatePrefetchCost(int prefetchedEntityCount, EntityContentRequire[] requirements) {
		return prefetchedEntityCount * requirements.length * 148L;
	}

	public EntityContentRequire[] getRequirements() {
		return alternative.getRequirements();
	}

	@Override
	protected long getClassId() {
		return CLASS_ID;
	}

	@Override
	protected long includeAdditionalHash(@Nonnull LongHashFunction hashFunction) {
		return 0L;
	}

	@Nonnull
	@Override
	protected Bitmap computeInternal() {
		// if the entities were prefetched we passed the "is it worthwhile" check
		return Optional.ofNullable(queryContext.getPrefetchedEntities())
			.map(it -> alternative.filter(queryContext, it))
			.orElseGet(getDelegate()::compute);
	}

	@Override
	protected long getEstimatedCostInternal() {
		return Optional.ofNullable(queryContext.getPrefetchedEntities())
			.map(it -> alternative.getRequirements().length * 148L)
			.orElseGet(getDelegate()::getEstimatedCost);
	}

	@Override
	protected long getCostInternal() {
		return Optional.ofNullable(queryContext.getPrefetchedEntities())
			.map(it -> alternative.getRequirements().length * 148L)
			.orElseGet(getDelegate()::getCost);
	}

	@Override
	protected long getCostToPerformanceInternal() {
		return Optional.ofNullable(queryContext.getPrefetchedEntities())
			.map(it -> getCost() / Math.max(1, compute().size()))
			.orElseGet(getDelegate()::getCostToPerformanceRatio);
	}

	@Override
	public int getEstimatedCardinality() {
		return Optional.ofNullable(queryContext.getPrefetchedEntities())
			.map(List::size)
			.orElseGet(getDelegate()::getEstimatedCardinality);
	}

	/**
	 * Returns delegate formula that computes the result in a standard way.
	 */
	public Formula getDelegate() {
		return innerFormulas[0];
	}

	@Nonnull
	@Override
	public Formula getCloneWithInnerFormulas(@Nonnull Formula... innerFormulas) {
		Assert.isTrue(innerFormulas.length == 1, () -> new IllegalArgumentException("Exactly one inner formula is expected!"));
		return new SelectionFormula(
			queryContext, innerFormulas[0], alternative
		);
	}

	@Override
	public long getOperationCost() {
		return 1;
	}

	/**
	 * We need to override this method so that sorting logic will communicate with our implementation and doesn't ask
	 * for filtered price records from this formula {@link #getDelegate()} children which would require computation executed
	 * by the {@link #getDelegate()} which we try to avoid by alternative solution.
	 */
	@Nonnull
	@Override
	public FilteredPriceRecords getFilteredPriceRecords() {
		// if the entities were prefetched we passed the "is it worthwhile" check
		return Optional.ofNullable(queryContext.getPrefetchedEntities())
			// ask the alternative solution for filtered price records
			.map(it ->
				alternative instanceof FilteredPriceRecordAccessor ?
					((FilteredPriceRecordAccessor) alternative).getFilteredPriceRecords() :
					new ResolvedFilteredPriceRecords()
			)
			// otherwise collect the filtered records from the delegate
			.orElseGet(() -> FilteredPriceRecords.createFromFormulas(this, this.compute()));
	}

	/**
	 * This formula visitor identifies the entity ids that are accessible within conjunction scope from the formula root
	 * and detects possible {@link SelectionFormula} in the tree. It prepares:
	 *
	 * - {@link #getRequirements()} set that to fetch entity with
	 * - {@link #getConjunctiveEntities()} entity primary keys to fetch
	 * - {@link #getExpectedComputationalCosts()} costs that is estimated to be paid with regular execution
	 */
	public static class PrefetchFormulaVisitor implements FormulaVisitor, FormulaPostProcessor {
		private static final int BITMAP_SIZE_THRESHOLD = 1000;
		/**
		 * Contains set of formulas that are considered conjunctive for purpose of this visitor.
		 */
		private static final Set<Class<? extends Formula>> CONJUNCTIVE_FORMULAS;
		/**
		 * Contains set of requirements collected from all {@link SelectionFormula} in the tree.
		 */
		private final Map<Class<? extends EntityContentRequire>, EntityContentRequire> requirements = new HashMap<>();
		/**
		 * Contains all bitmaps of entity ids found in conjunctive scope of the formula.
		 */
		private final List<Bitmap> conjunctiveEntityIds = new LinkedList<>();
		/**
		 * Contains sum of all collected bitmap cardinalities. If sum is greater than {@link #BITMAP_SIZE_THRESHOLD}
		 * the prefetch will automatically signalize it has no sense.
		 */
		private int estimatedBitmapCardinality = -1;
		/**
		 * Contains aggregated costs that is estimated to be paid with regular execution of the {@link SelectionFormula}.
		 */
		@Getter private long expectedComputationalCosts = 0L;
		/**
		 * Flag that signalizes {@link #visit(Formula)} happens in conjunctive scope.
		 */
		private boolean conjunctiveScope = true;
		/**
		 * Result of {@link FormulaPostProcessor} interface - basically root of the formula. This implementation doesn't
		 * change the input formula tree - just analyzes it.
		 */
		private Formula outputFormula;

		static {
			CONJUNCTIVE_FORMULAS = new HashSet<>();
			CONJUNCTIVE_FORMULAS.add(AndFormula.class);
			CONJUNCTIVE_FORMULAS.add(UserFilterFormula.class);
		}

		/**
		 * We don't alter the input formula - just analyze it.
		 */
		@Nonnull
		@Override
		public Formula getPostProcessedFormula() {
			return outputFormula;
		}

		/**
		 * Returns entity primary keys to fetch.
		 */
		public Bitmap getConjunctiveEntities() {
			return estimatedBitmapCardinality <= BITMAP_SIZE_THRESHOLD ?
				conjunctiveEntityIds.stream()
					.reduce((bitmapA, bitmapB) -> {
						final RoaringBitmap roaringBitmapA = RoaringBitmapBackedBitmap.getRoaringBitmap(bitmapA);
						final RoaringBitmap roaringBitmapB = RoaringBitmapBackedBitmap.getRoaringBitmap(bitmapB);
						return new BaseBitmap(
							RoaringBitmap.and(roaringBitmapA, roaringBitmapB)
						);
					})
					.orElse(EmptyBitmap.INSTANCE) : EmptyBitmap.INSTANCE;
		}

		/**
		 * Returns set of requirements to fetch entities with.
		 */
		public EntityContentRequire[] getRequirements() {
			return requirements.values().toArray(new EntityContentRequire[0]);
		}

		/**
		 * Returns true if there is any entity id known in conjunction scope and at least single {@link SelectionFormula}
		 * is present in the formula tree.
		 */
		public boolean isPrefetchPossible() {
			return !conjunctiveEntityIds.isEmpty() && !requirements.isEmpty() && estimatedBitmapCardinality <= BITMAP_SIZE_THRESHOLD;
		}

		@Override
		public void visit(Formula formula) {
			if (outputFormula == null) {
				this.outputFormula = formula;
			}
			if (formula instanceof SelectionFormula) {
				final SelectionFormula selectionFormula = (SelectionFormula) formula;
				expectedComputationalCosts += selectionFormula.getDelegate().getEstimatedCost();
				for (EntityContentRequire requirement : selectionFormula.getRequirements()) {
					requirements.merge(
						requirement.getClass(), requirement,
						EntityContentRequire::combineWith
					);
				}
			}
			final boolean formerConjunctiveScope = this.conjunctiveScope;
			try {
				if (this.conjunctiveScope && formula instanceof ConstantFormula) {
					final Bitmap bitmap = ((ConstantFormula) formula).getDelegate();
					this.conjunctiveEntityIds.add(bitmap);
					final int bitmapSize = bitmap.size();
					estimatedBitmapCardinality = estimatedBitmapCardinality == -1 || bitmapSize < estimatedBitmapCardinality ?
						bitmapSize : estimatedBitmapCardinality;
				}
				if (!CONJUNCTIVE_FORMULAS.contains(formula.getClass())) {
					this.conjunctiveScope = false;
				}
				for (Formula innerFormula : formula.getInnerFormulas()) {
					innerFormula.accept(this);
				}
			} finally {
				this.conjunctiveScope = formerConjunctiveScope;
			}
		}

	}

}
