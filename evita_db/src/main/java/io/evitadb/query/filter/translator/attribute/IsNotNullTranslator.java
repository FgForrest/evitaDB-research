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

package io.evitadb.query.filter.translator.attribute;

import io.evitadb.api.query.filter.IsNotNull;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.attribute.alternative.AttributeBitmapFilter;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link IsNotNull} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class IsNotNullTranslator implements FilteringConstraintTranslator<IsNotNull> {

	@Override
	@Nonnull
	public Formula translate(@Nonnull IsNotNull filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final String attributeName = filterConstraint.getAttributeName();
		final AttributeSchema attributeDefinition = filterByVisitor.getAttributeSchema(attributeName);

		// if attribute is unique prefer O(1) hash map lookup over histogram
		if (attributeDefinition.isUnique()) {
			return new AttributeFormula(
				attributeName,
				filterByVisitor.applyOnUniqueIndexes(
					attributeDefinition, index -> new ConstantFormula(index.getRecordIds())
				)
			);
		} else {
			return new SelectionFormula(
				filterByVisitor.getQueryContext(),
				new AttributeFormula(
					attributeName,
					filterByVisitor.applyOnFilterIndexes(
						attributeDefinition, FilterIndex::getAllRecordsFormula
					)
				),
				new AttributeBitmapFilter(
					attributeName,
					Objects::nonNull
				)
			);
		}
	}

}
