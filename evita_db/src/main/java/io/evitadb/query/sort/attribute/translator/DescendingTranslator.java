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

package io.evitadb.query.sort.attribute.translator;

import io.evitadb.api.query.order.Descending;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.attribute.SortIndex;
import io.evitadb.query.sort.OrderByVisitor;
import io.evitadb.query.sort.Sorter;
import io.evitadb.query.sort.attribute.PreSortedRecordsSorter;
import io.evitadb.query.sort.translator.OrderingConstraintTranslator;

/**
 * This implementation of {@link OrderingConstraintTranslator} converts {@link Descending} to {@link Sorter}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DescendingTranslator implements OrderingConstraintTranslator<Descending> {

	@Override
	public Sorter apply(Descending ascending, OrderByVisitor orderByVisitor) {
		final String attributeName = ascending.getAttributeName();
		// verify schema
		orderByVisitor.getAttributeSchema(attributeName);

		final EntityIndex targetIndex = orderByVisitor.getIndexForSort();
		final SortIndex sortIndex = targetIndex.getSortIndex(attributeName, orderByVisitor.getLanguage());
		final Sorter lastUsedSorter = orderByVisitor.getLastUsedSorter();
		if (sortIndex != null) {
			if (lastUsedSorter == null) {
				return new PreSortedRecordsSorter(
					sortIndex.getDescendingOrderRecordsSupplier()
				);
			} else {
				return lastUsedSorter.andThen(
					new PreSortedRecordsSorter(
						sortIndex.getDescendingOrderRecordsSupplier()
					)
				);
			}
		} else {
			return lastUsedSorter;
		}
	}

}
