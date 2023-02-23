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

package io.evitadb.query.sort.price.translator;

import io.evitadb.api.query.OrderConstraint;
import io.evitadb.query.algebra.price.FilteredPriceRecordAccessor;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder.LookUp;
import io.evitadb.query.sort.NoSorter;
import io.evitadb.query.sort.OrderByVisitor;
import io.evitadb.query.sort.Sorter;
import io.evitadb.query.sort.translator.OrderingConstraintTranslator;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * AbstractPriceOrderingTranslator is a superclass for {@link PriceAscendingTranslator} and {@link PriceDescendingTranslator}
 * and contains shared logic for these two translators.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
abstract class AbstractPriceOrderingTranslator<T extends OrderConstraint> implements OrderingConstraintTranslator<T> {

	@Override
	public Sorter apply(T random, OrderByVisitor orderByVisitor) {
		// are filtered prices used in the filtering?
		final Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors = FormulaFinder.find(
			orderByVisitor.getFiltering(), FilteredPriceRecordAccessor.class, LookUp.SHALLOW
		);

		final Sorter thisSorter;
		if (!filteredPriceRecordAccessors.isEmpty()) {
			// if so, create filtered prices sorter
			thisSorter = createFilteredPricesSorter(orderByVisitor, filteredPriceRecordAccessors);
		} else {
			// otherwise, we cannot sort the entities by price
			thisSorter = NoSorter.INSTANCE;
		}
		// if there was defined primary sorter, append this sorter after it
		final Sorter lastUsedSorter = orderByVisitor.getLastUsedSorter();
		if (lastUsedSorter == null) {
			return thisSorter;
		} else {
			return lastUsedSorter.andThen(thisSorter);
		}
	}

	@Nonnull
	protected abstract Sorter createFilteredPricesSorter(
		@Nonnull OrderByVisitor orderByVisitor,
		@Nonnull Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors
	);

}
