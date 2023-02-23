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

package io.evitadb.query.algebra.utils.visitor;

import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.FormulaVisitor;
import io.evitadb.query.algebra.base.NotFormula;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * FormulaCloner creates deep duplicate of the original {@link Formula} instance. Cloner reuses all formulas with
 * memoized results and recreates only those that are modified by {@link #mutator}
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FormulaCloner implements FormulaVisitor {
	/**
	 * This map keeps track of already mutated formulas so that they can be reused in cloned formula tree.
	 * This solves the situation that when there are duplicate instances in formulas in source tree, they will be
	 * appropriately imitated (in terms of formula identity) in the output tree.
	 */
	protected final IdentityHashMap<Formula, Formula> formulasProcessed = new IdentityHashMap<>();
	/**
	 * This function is applied to every {@link Formula} visited and may return:
	 *
	 * - original instance (it will keep it along with all memoized results)
	 * - new instance (it will use it and recreate all parent formulas which then lose their memoized results)
	 * - NULL (it will skip it and recreate all parent formulas which then lose their memoized results)
	 */
	protected final BiFunction<FormulaCloner, Formula, Formula> mutator;
	/**
	 * This stack contains list of parents for currently examined formula.
	 */
	protected final Deque<Formula> parents = new LinkedList<>();
	/**
	 * Stacks serves internally to collect the cloned tree of formulas.
	 */
	protected final Deque<Set<Formula>> stack = new LinkedList<>();
	/**
	 * Result set of the clone operation.
	 */
	@Getter private Formula resultClone;

	protected FormulaCloner(@Nonnull UnaryOperator<Formula> mutator) {
		this.mutator = (formulaCloner, formula) -> mutator.apply(formula);
	}

	protected FormulaCloner(@Nonnull BiFunction<FormulaCloner, Formula, Formula> mutator) {
		this.mutator = mutator;
	}

	/**
	 * Preferred way of invoking this visitor. Accepts formula (tree) and produces mutated clone of this formula in
	 * response. The result shares memoized results that can be shared.
	 */
	@Nonnull
	public static Formula clone(@Nonnull Formula formulaToClone, @Nonnull UnaryOperator<Formula> mutator) {
		final FormulaCloner visitor = new FormulaCloner(mutator);
		formulaToClone.accept(visitor);
		return visitor.getResultClone();
	}

	/**
	 * Preferred way of invoking this visitor. Accepts formula (tree) and produces mutated clone of this formula in
	 * response. The result shares memoized results that can be shared.
	 */
	@Nonnull
	public static Formula clone(@Nonnull Formula formulaToClone, @Nonnull BiFunction<FormulaCloner, Formula, Formula> mutator) {
		final FormulaCloner visitor = new FormulaCloner(mutator);
		formulaToClone.accept(visitor);
		return visitor.getResultClone();
	}

	/**
	 * Returns true if there is at least single parent formula that matches passed predicate for currently visited
	 * formula.
	 */
	public boolean isWithin(@Nonnull Predicate<Formula> formulaTester) {
		for (Formula parent : parents) {
			if (formulaTester.test(parent)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if there is at least single parent formula of passed `formulaType` for currently visited formula.
	 */
	public boolean isWithin(@Nonnull Class<? extends Formula> formulaType) {
		return isWithin(formulaType::isInstance);
	}

	@Override
	public void visit(Formula formula) {
		final Formula mutatedFormula = mutator.apply(this, formula);
		final Formula alreadyProcessedFormula = formulasProcessed.get(formula);
		if (alreadyProcessedFormula != null) {
			storeFormula(alreadyProcessedFormula);
		} else {
			final Formula formulaToStore;
			if (mutatedFormula == formula) {
				stack.push(new LinkedHashSet<>());
				parents.push(formula);
				for (Formula innerFormula : formula.getInnerFormulas()) {
					innerFormula.accept(this);
				}
				parents.pop();
				final Set<Formula> updatedChildren = stack.pop();
				final boolean childrenHaveNotChanged = updatedChildren.size() == formula.getInnerFormulas().length &&
					Arrays.stream(formula.getInnerFormulas()).allMatch(updatedChildren::contains);

				if (childrenHaveNotChanged) {
					// use entire formula tree block
					formulaToStore = formula;
				} else if (formula instanceof NotFormula && updatedChildren.size() == 1) {
					formulaToStore = updatedChildren.iterator().next();
				} else {
					// recreate parent formula with new children
					formulaToStore = formula.getCloneWithInnerFormulas(
						updatedChildren.toArray(Formula[]::new)
					);
				}
			} else {
				formulaToStore = mutatedFormula;
			}

			if (formulaToStore != null) {
				formulasProcessed.put(formula, formulaToStore);
				storeFormula(formulaToStore);
			}
		}
	}

	/*
		PRIVATE METHODS
	 */

	private void storeFormula(Formula formula) {
		// store updated formula
		if (stack.isEmpty()) {
			this.resultClone = formula;
		} else {
			stack.peek().add(formula);
		}
	}

}
