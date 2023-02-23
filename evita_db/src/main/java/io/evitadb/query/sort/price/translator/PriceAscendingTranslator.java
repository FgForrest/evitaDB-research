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

import io.evitadb.api.query.order.PriceAscending;
import io.evitadb.query.algebra.price.FilteredPriceRecordAccessor;
import io.evitadb.query.sort.OrderByVisitor;
import io.evitadb.query.sort.Sorter;
import io.evitadb.query.sort.price.FilteredPricesSorter;
import io.evitadb.query.sort.price.SortOrder;
import io.evitadb.query.sort.translator.OrderingConstraintTranslator;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * This implementation of {@link OrderingConstraintTranslator} converts {@link PriceAscending} to {@link Sorter}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceAscendingTranslator extends AbstractPriceOrderingTranslator<PriceAscending> {

	@Nonnull
	protected FilteredPricesSorter createFilteredPricesSorter(@Nonnull OrderByVisitor orderByVisitor, @Nonnull Collection<FilteredPriceRecordAccessor> filteredPriceRecordAccessors) {
		return new FilteredPricesSorter(
			SortOrder.ASC,
			orderByVisitor.getQueryPriceMode(),
			filteredPriceRecordAccessors
		);
	}

}
