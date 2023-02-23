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
import io.evitadb.api.query.filter.UserFilter;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.utils.FormulaFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

/**
 * This implementation contains the heavy part of {@link FacetCalculator} interface implementation. It computes how many
 * entities posses the specified facet respecting current {@link EvitaRequest} filtering constraint except contents
 * of the {@link UserFilter}. It means that it respects all mandatory filtering constraints which gets enriched by
 * additional constraint that represents single facet. The result of the query represents the number of products having
 * such facet.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
public class FacetFormulaGenerator extends AbstractFacetFormulaGenerator {

	public FacetFormulaGenerator(
		@Nonnull UnaryOperator<Serializable> entityToGroupTypeTranslator,
		@Nonnull BiPredicate<Serializable, Integer> isFacetGroupConjunction,
		@Nonnull BiPredicate<Serializable, Integer> isFacetGroupDisjunction,
		@Nonnull BiPredicate<Serializable, Integer> isFacetGroupNegation
	) {
		super(entityToGroupTypeTranslator, isFacetGroupConjunction, isFacetGroupDisjunction, isFacetGroupNegation);
	}

	@Override
	protected boolean shouldIncludeChildren(boolean isUserFilter) {
		// this implementation skips former contents of the user filter formula
		// (it just adds single new facet formula for the computation
		return !isUserFilter;
	}

	@Override
	protected Formula getResult(@Nonnull Formula baseFormula) {
		// if the output is same as input, it means the input didn't contain UserFilterFormula
		if (result == baseFormula) {
			// so we need to change it here adding new facet group formula
			return FormulaFactory.and(
				baseFormula,
				createNewFacetGroupFormula()
			);
		} else {
			// output changed - just propagate it
			return result;
		}
	}

}
