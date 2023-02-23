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

package io.evitadb.query.filter.translator.price;

import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.function.TriFunction;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.index.array.CompositeObjectArray;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.price.innerRecordHandling.PriceHandlingContainerFormula;
import io.evitadb.query.algebra.price.priceIndex.PriceIdContainerFormula;
import io.evitadb.query.algebra.price.priceIndex.PriceIndexProvidingFormula;
import io.evitadb.query.algebra.price.priceIndex.PriceListCombinationFormula;
import io.evitadb.query.algebra.price.termination.PriceEvaluationContext;
import io.evitadb.query.algebra.price.translate.PriceIdToEntityIdTranslateFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder.LookUp;
import io.evitadb.query.algebra.utils.visitor.FormulaLocator;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Abstract superclass for price related {@link FilteringConstraintTranslator} implementations that unifies the shared
 * logic together.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
abstract class AbstractPriceRelatedConstraintTranslator<T extends FilterConstraint> implements FilteringConstraintTranslator<T> {

	/**
	 * Method creates formula structure where second, third and additional formulas subtract the result of the previous
	 * formula results in the chain and then get combined with them by OR constraint the aim is there to create distinct
	 * result where for each record only its price from most prioritized price list is taken into an account.
	 */
	@Nonnull
	protected List<Formula> createPriceListFormula(@Nullable Serializable[] priceLists, @Nullable Currency currency, @Nonnull TriFunction<Serializable, Currency, PriceInnerRecordHandling, Formula> priceListFormulaComputer) {
		final List<Formula> formulas = new ArrayList<>(PriceInnerRecordHandling.values().length);
		for (PriceInnerRecordHandling innerRecordHandling : PriceInnerRecordHandling.values()) {
			final CompositeObjectArray<Formula> priceListFormulas = new CompositeObjectArray<>(Formula.class);
			if (priceLists == null) {
				final Formula initialFormula = priceListFormulaComputer.apply(null, currency, innerRecordHandling);
				if (!(initialFormula instanceof EmptyFormula)) {
					priceListFormulas.add(translateFormula(initialFormula));
				}
			} else {
				Serializable firstFormulaPriceList = null;
				Formula lastFormula = null;
				for (Serializable priceList : priceLists) {
					final Formula priceListFormula = priceListFormulaComputer.apply(priceList, currency, innerRecordHandling);
					if (!(priceListFormula instanceof EmptyFormula)) {
						final Formula translatedFormula = translateFormula(priceListFormula);

						if (lastFormula == null) {
							firstFormulaPriceList = priceList;
							lastFormula = translatedFormula;
						} else {
							final PriceEvaluationContext priceContext = new PriceEvaluationContext(
								FormulaFinder.find(
									translatedFormula, PriceIndexProvidingFormula.class, LookUp.SHALLOW
								)
									.stream()
									.map(it -> it.getPriceIndex().getPriceIndexKey())
									.distinct()
									.toArray(PriceIndexKey[]::new)
							);
							lastFormula = new PriceListCombinationFormula(
								lastFormula instanceof PriceListCombinationFormula ?
									((PriceListCombinationFormula)lastFormula).getCombinedPriceListNames() : firstFormulaPriceList, priceList,
								priceContext, lastFormula, translatedFormula
							);
						}
						priceListFormulas.add(lastFormula);
					}
				}
			}
			if (!priceListFormulas.isEmpty()) {
				formulas.add(
					new PriceHandlingContainerFormula(
						innerRecordHandling,
						FormulaFactory.or(priceListFormulas.toArray())
					)
				);
			}
		}

		return formulas;
	}

	@Nonnull
	private Formula translateFormula(Formula initialFormula) {
		final Formula translatedFormula;
		if (FormulaLocator.contains(initialFormula, PriceIdContainerFormula.class)) {
			translatedFormula = new PriceIdToEntityIdTranslateFormula(initialFormula);
		} else {
			translatedFormula = initialFormula;
		}
		return translatedFormula;
	}

}
