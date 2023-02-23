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

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.query.filter.LessThan;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.attribute.alternative.AttributeBitmapFilter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link LessThan} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class LessThanTranslator implements FilteringConstraintTranslator<LessThan> {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	@Nonnull
	public Formula translate(@Nonnull LessThan filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final String attributeName = filterConstraint.getAttributeName();
		final Serializable attributeValue = filterConstraint.getAttributeValue();
		final AttributeSchema attributeDefinition = filterByVisitor.getAttributeSchema(attributeName);
		final Comparable comparableValue = (Comparable) EvitaDataTypes.toTargetType(attributeValue, attributeDefinition.getType());

		return new SelectionFormula(
			filterByVisitor.getQueryContext(),
			new AttributeFormula(
				attributeName,
				filterByVisitor.applyOnFilterIndexes(
					attributeDefinition, index -> index.getRecordsLesserThanFormula(comparableValue)
				)
			),
			new AttributeBitmapFilter(
				attributeName,
				attributeDefinition.getType().isArray() ?
					getArrayPredicate(comparableValue) :
					getPredicate(comparableValue)
			)
		);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	public static Predicate<Object> getPredicate(Comparable<?> comparableValue) {
		return attr -> attr != null && ((Comparable) attr).compareTo(comparableValue) < 0;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	public static Predicate<Object> getArrayPredicate(Comparable<?> comparableValue) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((Comparable) it).compareTo(comparableValue) < 0);
	}

}
