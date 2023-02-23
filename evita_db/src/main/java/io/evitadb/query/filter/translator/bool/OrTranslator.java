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

package io.evitadb.query.filter.translator.bool;

import io.evitadb.api.query.filter.Or;
import io.evitadb.query.QueryExecutor.FutureNotFormula;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.base.OrFormula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;

import javax.annotation.Nonnull;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link Or} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class OrTranslator implements FilteringConstraintTranslator<Or> {

	@Nonnull
	@Override
	public Formula translate(@Nonnull Or filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		return FutureNotFormula.postProcess(
			filterByVisitor.getCollectedIntegerFormulasOnCurrentLevel(),
			formulas -> {
				if (formulas.length == 0) {
					return EmptyFormula.INSTANCE;
				} else if (formulas.length == 1) {
					return formulas[0];
				} else {
					return new OrFormula(formulas);
				}
			}
		);
	}

}
