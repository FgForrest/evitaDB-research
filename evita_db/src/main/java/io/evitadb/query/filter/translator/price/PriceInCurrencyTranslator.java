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

import io.evitadb.api.query.filter.*;
import io.evitadb.index.price.PriceListAndCurrencyPriceIndex;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.algebra.infra.SkipFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.price.alternative.SellingPriceAvailableBitmapFilter;

import javax.annotation.Nonnull;
import java.util.Currency;
import java.util.List;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link PriceInCurrency} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceInCurrencyTranslator extends AbstractPriceRelatedConstraintTranslator<PriceInCurrency> {

	@Nonnull
	@Override
	public Formula translate(@Nonnull PriceInCurrency priceInCurrency, @Nonnull FilterByVisitor filterByVisitor) {
		// if there are any more specific constraints - skip itself
		//noinspection unchecked
		if (filterByVisitor.isParentConstraint(And.class) && filterByVisitor.isAnySiblingConstraintPresent(PriceInPriceLists.class, PriceValidIn.class, PriceBetween.class)) {
			return SkipFormula.INSTANCE;
		} else {
			final Currency requestedCurrency = priceInCurrency.getCurrency();
			final List<Formula> priceListFormula = createFormula(filterByVisitor, requestedCurrency);
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
	 * Method creates formula for {@link PriceInCurrency} filtering constraint.
	 * Method is reused from {@link PriceBetweenTranslator} that builds upon this translator.
	 */
	@Nonnull
	List<Formula> createFormula(@Nonnull FilterByVisitor filterByVisitor, @Nonnull Currency requestedCurrency) {
		return createPriceListFormula(
			null, requestedCurrency,
			(serializable, currency, innerRecordHandling) ->
				filterByVisitor.applyOnIndexes(
					entityIndex -> FormulaFactory.or(
						entityIndex
							.getPriceListAndCurrencyIndexes()
							.stream()
							.filter(it -> {
								final PriceIndexKey priceIndexKey = it.getPriceIndexKey();
								return innerRecordHandling.equals(priceIndexKey.getRecordHandling()) &&
									currency.equals(priceIndexKey.getCurrency());
							})
							.map(PriceListAndCurrencyPriceIndex::createPriceIndexFormulaWithAllRecords)
							.toArray(Formula[]::new)
					)
				)

		);
	}

}
