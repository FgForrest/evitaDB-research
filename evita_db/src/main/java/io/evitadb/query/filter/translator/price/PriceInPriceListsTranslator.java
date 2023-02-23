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
import io.evitadb.index.price.PriceListAndCurrencyPriceIndex;
import io.evitadb.index.price.model.PriceIndexKey;
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
import java.util.Currency;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link PriceInPriceLists} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceInPriceListsTranslator extends AbstractPriceRelatedConstraintTranslator<PriceInPriceLists> {
	static final PriceInPriceListsTranslator INSTANCE = new PriceInPriceListsTranslator();

	@Nonnull
	@Override
	public Formula translate(@Nonnull PriceInPriceLists priceInPriceLists, @Nonnull FilterByVisitor filterByVisitor) {
		// if there are any more specific constraints - skip itself
		//noinspection unchecked
		if (filterByVisitor.isParentConstraint(And.class) && filterByVisitor.isAnySiblingConstraintPresent(PriceValidIn.class, PriceBetween.class)) {
			return SkipFormula.INSTANCE;
		} else {
			final Serializable[] priceLists = priceInPriceLists.getPriceLists();
			final Currency currency = ofNullable(filterByVisitor.findInConjunctionTree(PriceInCurrency.class))
				.map(PriceInCurrency::getCurrency)
				.orElse(null);

			final List<Formula> priceListFormula = createFormula(filterByVisitor, priceLists, currency);
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
	 * Method creates formula for {@link PriceInPriceLists} filtering constraint.
	 * Method is reused from {@link PriceBetweenTranslator} that builds upon this translator.
	 */
	@Nonnull
	List<Formula> createFormula(@Nonnull FilterByVisitor filterByVisitor, @Nonnull Serializable[] priceLists, @Nullable Currency currency) {
		final TriFunction<Serializable, Currency, PriceInnerRecordHandling, Formula> priceListFormulaComputer;
		if (currency == null) {
			// we don't have currency - we need to join records for all currencies in a single OR constraint
			priceListFormulaComputer = (priceList, curr, innerRecordHandling) -> filterByVisitor.applyOnIndexes(
				entityIndex -> FormulaFactory.or(
					entityIndex
						.getPriceListAndCurrencyIndexes()
						.stream()
						.filter(it -> {
							final PriceIndexKey priceIndexKey = it.getPriceIndexKey();
							return priceList.equals(priceIndexKey.getPriceList()) &&
								innerRecordHandling.equals(priceIndexKey.getRecordHandling());
						})
						.map(PriceListAndCurrencyPriceIndex::createPriceIndexFormulaWithAllRecords)
						.toArray(Formula[]::new)
				)
			);
		} else {
			// this is the easy way - we have both price list name and currency, we may use data from the specialized index
			priceListFormulaComputer = (priceList, curr, innerRecordHandling) -> filterByVisitor.applyOnIndexes(
				entityIndex -> ofNullable(entityIndex.getPriceIndex(priceList, currency, innerRecordHandling))
					.map(PriceListAndCurrencyPriceIndex::createPriceIndexFormulaWithAllRecords)
					.orElse(EmptyFormula.INSTANCE)
			);
		}

		return createPriceListFormula(priceLists, currency, priceListFormulaComputer);
	}

}
