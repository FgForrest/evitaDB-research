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

package io.evitadb.cache;

import io.evitadb.api.EvitaSession;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.utils.visitor.FormulaCloner;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * FormulaCacheVisitor extends {@link FormulaCloner} and creates new clone of original "analysed" formula replacing all
 * costly formulas either with cached results, or their copies that communicate with {@link CacheAnteroom} so their
 * usage is being tracked and evaluated.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 * @see CacheAnteroom#register(EvitaSession, Serializable, Formula, FormulaCacheVisitor) for more details
 */
public class FormulaCacheVisitor extends FormulaCloner {

	/**
	 * Preferred way of invoking this visitor. Accepts formula (tree) and produces clone that may contain already
	 * cached results.
	 *
	 * @see CacheAnteroom#register(EvitaSession, Serializable, Formula, FormulaCacheVisitor) for more details
	 */
	@Nonnull
	public static Formula analyse(@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType, @Nonnull Formula formulaToAnalyse, @Nonnull CacheAnteroom cacheAnteroom) {
		final FormulaCacheVisitor visitor = new FormulaCacheVisitor(evitaSession, entityType, cacheAnteroom);
		formulaToAnalyse.accept(visitor);
		return visitor.getResultClone();
	}

	private FormulaCacheVisitor(@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType, @Nonnull CacheAnteroom cacheAnteroom) {
		super((self, formula) -> cacheAnteroom.register(evitaSession, entityType, formula, (FormulaCacheVisitor) self));
	}

	/**
	 * Method is called from {@link CacheAnteroom} to traverse children of the passed formula using this visitor.
	 */
	@Nonnull
	public Formula[] analyseChildren(@Nonnull Formula formula) {
		stack.push(new LinkedHashSet<>());
		parents.push(formula);
		for (Formula innerFormula : formula.getInnerFormulas()) {
			innerFormula.accept(this);
		}
		parents.pop();
		final Set<Formula> updatedChildren = stack.pop();
		return updatedChildren.toArray(Formula[]::new);
	}
}
