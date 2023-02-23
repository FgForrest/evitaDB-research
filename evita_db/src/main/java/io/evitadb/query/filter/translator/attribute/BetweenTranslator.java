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
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.dataType.Range;
import io.evitadb.api.query.filter.Between;
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
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link Between} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class BetweenTranslator implements FilteringConstraintTranslator<Between> {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	@Nonnull
	public Formula translate(@Nonnull Between filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final String attributeName = filterConstraint.getAttributeName();
		final Serializable from = filterConstraint.getFrom();
		final Serializable to = filterConstraint.getTo();
		final AttributeSchema attributeDefinition = filterByVisitor.getAttributeSchema(attributeName);

		final Class<? extends Serializable> attributeType = attributeDefinition.getPlainType();
		if (Range.class.isAssignableFrom(attributeType)) {
			if (NumberRange.class.isAssignableFrom(attributeType)) {
				if (attributeDefinition.getIndexedDecimalPlaces() > 0) {
					final BigDecimal comparableValueFrom = EvitaDataTypes.toTargetType(from, BigDecimal.class);
					final BigDecimal comparableValueTo = EvitaDataTypes.toTargetType(to, BigDecimal.class);
					final Long comparableFrom = NumberRange.toComparableLong(comparableValueFrom, attributeDefinition.getIndexedDecimalPlaces());
					final Long comparableTo = NumberRange.toComparableLong(comparableValueTo, attributeDefinition.getIndexedDecimalPlaces());
					return new SelectionFormula(
						filterByVisitor.getQueryContext(),
						new AttributeFormula(
							attributeName,
							filterByVisitor.applyOnFilterIndexes(
								attributeDefinition, index -> index.getRecordsOverlappingFormula(comparableFrom, comparableTo)
							)
						),
						new AttributeBitmapFilter(
							attributeName,
							attributeDefinition.getType().isArray() ?
								getNumberRangeArrayPredicate(comparableValueFrom, comparableValueTo) :
								getNumberRangePredicate(comparableValueFrom, comparableValueTo)
						)
					);
				} else {
					final Long comparableValueFrom = EvitaDataTypes.toTargetType(from, Long.class);
					final Long comparableValueTo = EvitaDataTypes.toTargetType(to, Long.class);
					final Long comparableFrom = NumberRange.toComparableLong(comparableValueFrom);
					final Long comparableTo = NumberRange.toComparableLong(comparableValueTo);
					return new SelectionFormula(
						filterByVisitor.getQueryContext(),
						new AttributeFormula(
							attributeName,
							filterByVisitor.applyOnFilterIndexes(
								attributeDefinition, index -> index.getRecordsOverlappingFormula(comparableFrom, comparableTo)
							)
						),
						new AttributeBitmapFilter(
							attributeName,
							attributeDefinition.getType().isArray() ?
								getNumberRangeArrayPredicate(comparableValueFrom, comparableValueTo) :
								getNumberRangePredicate(comparableValueFrom, comparableValueTo)
						)
					);
				}
			} else if (DateTimeRange.class.isAssignableFrom(attributeType)) {
				final ZonedDateTime comparableValueFrom = EvitaDataTypes.toTargetType(from, ZonedDateTime.class);
				final ZonedDateTime comparableValueTo = EvitaDataTypes.toTargetType(to, ZonedDateTime.class);
				final Long comparableFrom = DateTimeRange.toComparableLong(comparableValueFrom);
				final Long comparableTo = DateTimeRange.toComparableLong(comparableValueTo);
				return new SelectionFormula(
					filterByVisitor.getQueryContext(),
					new AttributeFormula(
						attributeName,
						filterByVisitor.applyOnFilterIndexes(
							attributeDefinition, index -> index.getRecordsOverlappingFormula(comparableFrom, comparableTo)
						)
					),
					new AttributeBitmapFilter(
						attributeName,
						attributeDefinition.getType().isArray() ?
							getDateTimeArrayPredicate(comparableValueFrom, comparableValueTo) :
							getDateTimePredicate(comparableValueFrom, comparableValueTo)
					)
				);
			} else {
				throw new IllegalStateException("Unexpected Range type!");
			}
		} else {
			final Comparable comparableFrom = (Comparable) EvitaDataTypes.toTargetType(from, attributeType);
			final Comparable comparableTo = (Comparable) EvitaDataTypes.toTargetType(to, attributeType);
			return new SelectionFormula(
				filterByVisitor.getQueryContext(),
				new AttributeFormula(
					attributeName,
					filterByVisitor.applyOnFilterIndexes(
						attributeDefinition, index -> index.getRecordsBetweenFormula(comparableFrom, comparableTo)
					)
				),
				new AttributeBitmapFilter(
					attributeName,
					attributeDefinition.getType().isArray() ?
						getComparableArrayPredicate(comparableFrom, comparableTo) :
						getComparablePredicate(comparableFrom, comparableTo)
				)
			);
		}
	}

	@Nonnull
	public static Predicate<Object> getDateTimePredicate(ZonedDateTime comparableValueFrom, ZonedDateTime comparableValueTo) {
		return attr -> attr != null && ((DateTimeRange) attr).overlaps(DateTimeRange.between(comparableValueFrom, comparableValueTo));
	}

	@Nonnull
	public static Predicate<Object> getDateTimeArrayPredicate(ZonedDateTime comparableValueFrom, ZonedDateTime comparableValueTo) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((DateTimeRange) it).overlaps(DateTimeRange.between(comparableValueFrom, comparableValueTo)));
	}

	@Nonnull
	public static Predicate<Object> getNumberRangePredicate(Long comparableValueFrom, Long comparableValueTo) {
		return attr -> attr != null && ((NumberRange) attr).overlaps(NumberRange.between(comparableValueFrom, comparableValueTo));
	}

	@Nonnull
	public static Predicate<Object> getNumberRangeArrayPredicate(Long comparableValueFrom, Long comparableValueTo) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((NumberRange) it).overlaps(NumberRange.between(comparableValueFrom, comparableValueTo)));
	}

	@Nonnull
	public static Predicate<Object> getNumberRangePredicate(BigDecimal comparableValueFrom, BigDecimal comparableValueTo) {
		return attr -> attr != null && ((NumberRange) attr).overlaps(NumberRange.between(comparableValueFrom, comparableValueTo));
	}

	@Nonnull
	public static Predicate<Object> getNumberRangeArrayPredicate(BigDecimal comparableValueFrom, BigDecimal comparableValueTo) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((NumberRange) it).overlaps(NumberRange.between(comparableValueFrom, comparableValueTo)));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	public static Predicate<Object> getComparablePredicate(Comparable<?> comparableFrom, Comparable<?> comparableTo) {
		return attr -> attr != null && ((Comparable) attr).compareTo(comparableFrom) >= 0 && ((Comparable) attr).compareTo(comparableTo) <= 0;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Nonnull
	public static Predicate<Object> getComparableArrayPredicate(Comparable<?> comparableFrom, Comparable<?> comparableTo) {
		return attr -> attr != null && Arrays.stream((Object[])attr).anyMatch(it -> ((Comparable) it).compareTo(comparableFrom) >= 0 && ((Comparable) it).compareTo(comparableTo) <= 0);
	}

}
