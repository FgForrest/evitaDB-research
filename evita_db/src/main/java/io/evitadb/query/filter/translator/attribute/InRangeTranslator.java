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

import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.query.filter.InRange;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.deferred.SelectionFormula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.attribute.alternative.AttributeBitmapFilter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link InRange} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InRangeTranslator implements FilteringConstraintTranslator<InRange> {

	@Override
	@Nonnull
	public Formula translate(@Nonnull InRange filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final String attributeName = filterConstraint.getAttributeName();
		final AttributeSchema attributeDefinition = filterByVisitor.getAttributeSchema(attributeName);

		final Class<? extends Serializable> attributePlainType = attributeDefinition.getPlainType();
		final long comparableValue;
		final Predicate<Object> predicate;
		if (NumberRange.class.isAssignableFrom(attributePlainType)) {
			final Number theValue = filterConstraint.getTheValue();
			Assert.notNull(theValue, "Argument of InRange must not be null.");
			if (theValue instanceof BigDecimal) {
				comparableValue = NumberRange.toComparableLong((BigDecimal) theValue, attributeDefinition.getIndexedDecimalPlaces());
				predicate = attributeDefinition.getType().isArray() ?
					getNumberRangeArrayPredicate((BigDecimal)theValue) :
					getNumberRangePredicate((BigDecimal)theValue);
			} else {
				comparableValue = NumberRange.toComparableLong(theValue);
				predicate = attributeDefinition.getType().isArray() ?
					getNumberRangeArrayPredicate(theValue) :
					getNumberRangePredicate(theValue);
			}
		} else if (DateTimeRange.class.isAssignableFrom(attributePlainType)) {
			final ZonedDateTime theMoment = ofNullable(filterConstraint.getTheMoment()).orElseGet(filterByVisitor::getNow);
			comparableValue = DateTimeRange.toComparableLong(theMoment);
			predicate = attributeDefinition.getType().isArray() ?
				getDateTimeRangeArrayPredicate(theMoment) :
				getDateTimeRangePredicate(theMoment);
		} else {
			throw new IllegalArgumentException("Range types accepts only Number or DateTime types - type " + filterConstraint.getUnknownArgument() + " cannot be used!");
		}

		return new SelectionFormula(
			filterByVisitor.getQueryContext(),
			new AttributeFormula(
				attributeName,
				filterByVisitor.applyOnFilterIndexes(
					attributeDefinition, index -> index.getRecordsValidInFormula(comparableValue)
				)
			),
			new AttributeBitmapFilter(
				attributeName,
				predicate
			)
		);
	}

	@Nonnull
	public static Predicate<Object> getDateTimeRangePredicate(ZonedDateTime theMoment) {
		return attr -> attr != null && ((DateTimeRange) attr).isValidFor(theMoment);
	}

	@Nonnull
	public static Predicate<Object> getDateTimeRangeArrayPredicate(ZonedDateTime theMoment) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((DateTimeRange) it).isValidFor(theMoment));
	}

	@Nonnull
	public static Predicate<Object> getNumberRangePredicate(BigDecimal theValue) {
		return attr -> attr != null && ((NumberRange) attr).isWithin(theValue);
	}

	@Nonnull
	public static Predicate<Object> getNumberRangeArrayPredicate(BigDecimal theValue) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((NumberRange) it).isWithin(theValue));
	}

	@Nonnull
	public static Predicate<Object> getNumberRangePredicate(Number theValue) {
		return attr -> attr != null && ((NumberRange) attr).isWithin(theValue);
	}

	@Nonnull
	public static Predicate<Object> getNumberRangeArrayPredicate(Number theValue) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((NumberRange) it).isWithin(theValue));
	}

}
