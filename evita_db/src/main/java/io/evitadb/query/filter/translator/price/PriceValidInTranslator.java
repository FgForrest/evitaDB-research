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
import io.evitadb.api.query.filter.*;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.algebra.infra.SkipFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.price.alternative.SellingPriceAvailableBitmapFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link PriceValidIn} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceValidInTranslator extends AbstractPriceRelatedConstraintTranslator<PriceValidIn> {
	static final PriceValidInTranslator INSTANCE = new PriceValidInTranslator();

	@Nonnull
	@Override
	public Formula translate(@Nonnull PriceValidIn priceValidIn, @Nonnull FilterByVisitor filterByVisitor) {
		// if there are any more specific constraints - skip itself
		//noinspection unchecked
		if (filterByVisitor.isParentConstraint(And.class) && filterByVisitor.isAnySiblingConstraintPresent(PriceBetween.class)) {
			return SkipFormula.INSTANCE;
		} else {
			final ZonedDateTime theMoment = ofNullable(priceValidIn.getTheMoment()).orElseGet(filterByVisitor::getNow);
			final Serializable[] priceLists = ofNullable(filterByVisitor.findInConjunctionTree(PriceInPriceLists.class))
				.map(PriceInPriceLists::getPriceLists)
				.orElse(null);
			final Currency currency = ofNullable(filterByVisitor.findInConjunctionTree(PriceInCurrency.class))
				.map(PriceInCurrency::getCurrency)
				.orElse(null);

			final List<Formula> priceListFormula = createFormula(filterByVisitor, theMoment, priceLists, currency);
			return new SelectionFormula(
				filterByVisitor.getQueryContext(),
				PriceListCompositionTerminationVisitor.translate(
					priceListFormula,
					filterByVisitor.getQueryPriceMode(), null
				),
				new SellingPriceAvailableBitmapFilter(
					filterByVisitor.getSchema().getIndexedPricePlaces()
				)
			);
		}
	}

	/**
	 * Method creates formula for {@link PriceValidIn} filtering constraint.
	 * Method is reused from {@link PriceBetweenTranslator} that builds upon this translator.
	 */
	@Nonnull
	List<Formula> createFormula(@Nonnull FilterByVisitor filterByVisitor, @Nonnull ZonedDateTime theMoment, @Nullable Serializable[] priceLists, @Nullable Currency currency) {
		final TriFunction<Serializable, Currency, PriceInnerRecordHandling, Formula> priceListFormulaComputer;
		if (currency == null && priceLists == null) {
			// we don't have currency nor price lists - we need to join all records a single OR constraint
			priceListFormulaComputer = (priceList, curr, innerRecordHandling) -> filterByVisitor.applyOnIndexes(
				entityIndex -> FormulaFactory.or(
					entityIndex
						.getPriceListAndCurrencyIndexes()
						.stream()
						.filter(it -> innerRecordHandling.equals(it.getPriceIndexKey().getRecordHandling()))
						.map(it -> it.getIndexedRecordIdsValidInFormula(theMoment))
						.toArray(Formula[]::new)
				)
			);
		} else if (currency == null) {
			// we don't have currency - we need to join records for all currencies in a single OR constraint
			priceListFormulaComputer = (priceList, curr, innerRecordHandling) -> filterByVisitor.applyOnIndexes(
				entityIndex -> FormulaFactory.or(
					entityIndex
						.getPriceIndexesStream(priceList, innerRecordHandling)
						.map(it -> it.getIndexedRecordIdsValidInFormula(theMoment))
						.toArray(Formula[]::new)
				)
			);
		} else if (priceLists == null) {
			// we don't have price lists - we need to join records for all price lists and the same currency in a single OR constraint
			priceListFormulaComputer = (priceList, curr, innerRecordHandling) -> filterByVisitor.applyOnIndexes(
				entityIndex -> FormulaFactory.or(
					entityIndex
						.getPriceIndexesStream(curr, innerRecordHandling)
						.map(it -> it.getIndexedRecordIdsValidInFormula(theMoment))
						.toArray(Formula[]::new)
				)
			);
		} else {
			// this is the easy way - we have both price list name and currency, we may use data from the specialized index
			priceListFormulaComputer = (priceList, curr, innerRecordHandling) -> filterByVisitor.applyOnIndexes(
				entityIndex -> ofNullable(entityIndex.getPriceIndex(priceList, currency, innerRecordHandling))
					.map(it -> (Formula) it.getIndexedRecordIdsValidInFormula(theMoment))
					.orElse(EmptyFormula.INSTANCE)
			);
		}

		return createPriceListFormula(priceLists, currency, priceListFormulaComputer);
	}

}
