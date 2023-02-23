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

import io.evitadb.api.query.filter.Not;
import io.evitadb.api.utils.Assert;
import io.evitadb.query.QueryExecutor.FutureNotFormula;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;

import javax.annotation.Nonnull;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link Not} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class NotTranslator implements FilteringConstraintTranslator<Not> {

	@Override
	@Nonnull
	public Formula translate(@Nonnull Not filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final Formula[] collectedFormulas = filterByVisitor.getCollectedIntegerFormulasOnCurrentLevel();
		Assert.isTrue(collectedFormulas.length == 1, () -> new IllegalStateException("Not constraint is allowed to have only single argument: `" + filterConstraint + "`"));
		return new FutureNotFormula(collectedFormulas[0]);
	}

}
