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

package io.evitadb.query.extraResult.translator.facet.producer;

import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.extraResult.FacetSummary.RequestImpact;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.translator.facet.FilterFormulaFacetOptimizeVisitor;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Single implementation of both interfaces {@link FacetCalculator} and {@link ImpactCalculator}. The class computes
 * facet counts of the entities returned for current {@link EvitaRequest}. The implementation tries to memoize all
 * possible intermediate calculations to save machine ticks.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
public class MemoizingFacetCalculator implements FacetCalculator, ImpactCalculator {
	/**
	 * Contains filtering formula that was used to compute result of the {@link EvitaRequest}.
	 * The formula is converted to more optimal form that produces the same result but allows to memoize more
	 * intermediate calculations in the formula tree.
	 *
	 * @see FilterFormulaFacetOptimizeVisitor for more information
	 */
	private final Formula baseFormula;
	/**
	 * Contains "no-impact" result for all facets that are already selected in {@link EvitaRequest}.
	 */
	private final RequestImpact base;
	/**
	 * Contains instance of {@link FacetFormulaGenerator} that is reused for all calls. Visitors instances are usually
	 * created for single use and then thrown away but here we expect a lot of repeated computations for facets and
	 * reusing the same instance saves a little work for GC.
	 */
	private final FacetFormulaGenerator facetFormulaGenerator;
	/**
	 * Contains instance of {@link ImpactFormulaGenerator} that is reused for all calls. Visitors instances are usually
	 * created for single use and then thrown away but here we expect a lot of repeated computations for facets and
	 * reusing the same instance saves a little work for GC.
	 */
	private final ImpactFormulaGenerator impactFormulaGenerator;

	public MemoizingFacetCalculator(@Nonnull QueryContext queryContext, @Nonnull Formula baseFormula) {
		// first optimize formula to a form that utilizes memoization the most while adding new facet filters
		final Formula optimizedFormula = FilterFormulaFacetOptimizeVisitor.optimize(baseFormula);
		// now replace common parts of the formula with cached counterparts
		this.baseFormula = queryContext.analyse(optimizedFormula);
		this.base = new RequestImpact(0, baseFormula.compute().size());
		final MemoizingFacetToGroupTypeTranslator entityToGroupTypeTranslator = new MemoizingFacetToGroupTypeTranslator(queryContext.getSchema());
		final EvitaRequest evitaRequest = queryContext.getEvitaRequest();
		this.facetFormulaGenerator = new FacetFormulaGenerator(
			entityToGroupTypeTranslator,
			evitaRequest::isFacetGroupConjunction,
			evitaRequest::isFacetGroupDisjunction,
			evitaRequest::isFacetGroupNegation
		);
		this.impactFormulaGenerator = new ImpactFormulaGenerator(
			entityToGroupTypeTranslator,
			evitaRequest::isFacetGroupConjunction,
			evitaRequest::isFacetGroupDisjunction,
			evitaRequest::isFacetGroupNegation
		);
	}

	@Nullable
	@Override
	public RequestImpact calculateImpact(@Nonnull Serializable entityType, int facetId, @Nullable Integer facetGroupId, boolean required, @Nonnull Bitmap[] facetEntityIds) {
		if (required) {
			// facet is already selected in request - return "no impact" result quickly
			return base;
		} else {
			// create formula that would capture the requested facet selected
			final Formula hypotheticalFormula = impactFormulaGenerator.generateFormula(baseFormula, entityType, facetGroupId, facetId, facetEntityIds);
			// compute the hypothetical result
			final int hypotheticalCount = hypotheticalFormula.compute().size();
			// and return computed impact
			return new RequestImpact(
				hypotheticalCount - base.getMatchCount(),
				hypotheticalCount
			);
		}
	}

	@Nonnull
	@Override
	public Formula createCountFormula(@Nonnull Serializable entityType, int facetId, @Nullable Integer facetGroupId, @Nonnull Bitmap[] facetEntityIds) {
		// create formula that would capture all mandatory filtering constraints plus this single facet selected
		return facetFormulaGenerator.generateFormula(baseFormula, entityType, facetGroupId, facetId, facetEntityIds);
	}

	/**
	 * This helper class allows to translate facet {@link ReferenceSchema#getEntityType()} to
	 * {@link ReferenceSchema#getGroupType()}. This is necessary because the require constraints refer to the group
	 * primary keys and not the facet primary keys to specify what boolean relation should be used for the facets
	 * (of the same group).
	 *
	 * The class reuses last result if the same facet type translation is requested. There is strong assumption that
	 * the translation will ask for the same facet type in a row.
	 */
	@RequiredArgsConstructor
	private static class MemoizingFacetToGroupTypeTranslator implements UnaryOperator<Serializable> {
		/**
		 * Reference to the requested entity schema.
		 */
		private final EntitySchema schema;
		/**
		 * Remembered last facet type.
		 */
		private Serializable lastFacetType;
		/**
		 * Remembered last returned result to be quickly returned if the apply targets the same type
		 * as {@link #lastFacetType}.
		 */
		private Serializable lastResult;

		@Override
		public Serializable apply(Serializable entityType) {
			if (!Objects.equals(entityType, lastFacetType)) {
				lastFacetType = entityType;
				lastResult = schema.getReferenceOrThrowException(entityType).getGroupType();
			}
			return lastResult;
		}
	}
}
