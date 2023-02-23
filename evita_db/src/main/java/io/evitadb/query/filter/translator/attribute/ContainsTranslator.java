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

import io.evitadb.api.query.filter.Contains;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.attribute.alternative.AttributeBitmapFilter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link Contains} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ContainsTranslator implements FilteringConstraintTranslator<Contains> {

	@Nonnull
	@Override
	public Formula translate(@Nonnull Contains filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final String attributeName = filterConstraint.getAttributeName();
		final String textToSearch = filterConstraint.getTextToSearch();
		final AttributeSchema attributeDefinition = filterByVisitor.getAttributeSchema(attributeName);
		Assert.isTrue(
			String.class.equals(attributeDefinition.getType()),
			"StartsWith constraint can be used only on String attributes - " + attributeName + " is " + attributeDefinition.getType() + "!"
		);

		return new SelectionFormula(
			filterByVisitor.getQueryContext(),
			new AttributeFormula(
				attributeName,
				filterByVisitor.applyOnFilterIndexes(
					attributeDefinition, index -> {
						/* TOBEDONE JNO naive and slow - use RadixTree */
						final Formula[] foundRecords = index.getValues()
							.stream()
							.filter(getPredicate(textToSearch))
							.map(index::getRecordsEqualToFormula)
							.toArray(Formula[]::new);
						return ArrayUtils.isEmpty(foundRecords) ?
							EmptyFormula.INSTANCE : FormulaFactory.or(foundRecords);
					}
				)
			),
			new AttributeBitmapFilter(
				attributeName,
				attributeDefinition.getType().isArray() ?
					getArrayPredicate(textToSearch) :
					getPredicate(textToSearch)
			)
		);
	}

	@Nonnull
	public static Predicate<Object> getPredicate(@Nonnull String textToSearch) {
		return attr -> attr != null && ((String) attr).contains(textToSearch);
	}

	@Nonnull
	public static Predicate<Object> getArrayPredicate(String textToSearch) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((String) it).contains(textToSearch));
	}

}
