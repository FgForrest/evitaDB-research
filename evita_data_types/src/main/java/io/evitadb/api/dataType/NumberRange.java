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

package io.evitadb.api.dataType;

import io.evitadb.api.exception.DataTypeParseException;
import io.evitadb.api.utils.Assert;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

/**
 * Range type that envelopes {@link Number} types.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode(of = {"fromToCompare", "toToCompare"}, cacheStrategy = CacheStrategy.LAZY)
public class NumberRange implements Range<Number>, Serializable, Comparable<NumberRange> {
	private static final long serialVersionUID = 7690351814641934282L;
	private static final Pattern PARSE_PATTERN = Pattern.compile("^" + Pattern.quote(OPEN_CHAR) + "([\\d\\\\.]+?)?\\s*" + Pattern.quote(INTERVAL_JOIN) + "\\s*([\\d\\\\.]+?)?" + Pattern.quote(CLOSE_CHAR) + "$");
	public static final Function<String, String[]> PARSE_FCT = string -> {
		final Matcher matcher = PARSE_PATTERN.matcher(string);
		return matcher.matches() ? new String[]{matcher.group(1), matcher.group(2)} : null;
	};

	private final Number from;
	private final Number to;
	@Getter private final Integer retainedDecimalPlaces;
	private final long fromToCompare;
	private final long toToCompare;

	NumberRange(Number from, Number to, Integer retainedDecimalPlaces, long fromToCompare, long toToCompare) {
		this.from = from;
		this.to = to;
		this.retainedDecimalPlaces = retainedDecimalPlaces;
		this.fromToCompare = fromToCompare;
		this.toToCompare = toToCompare;
	}

	private NumberRange(@Nullable Number from, @Nullable Number to) {
		assertEitherBoundaryNotNull(from, to);
		assertFromLesserThanTo(from, to);
		assertNotFloatingPointType(from, "from");
		assertNotFloatingPointType(to, "to");
		this.from = from;
		this.to = to;
		this.retainedDecimalPlaces = null;
		this.fromToCompare = toComparableLong(from, Long.MIN_VALUE);
		this.toToCompare = toComparableLong(to, Long.MAX_VALUE);
	}

	private NumberRange(@Nullable BigDecimal from, @Nullable BigDecimal to) {
		assertEitherBoundaryNotNull(from, to);
		assertFromLesserThanTo(from, to);
		assertNoDecimalPlaces(from, "from");
		assertNoDecimalPlaces(to, "to");
		this.from = from;
		this.to = to;
		this.retainedDecimalPlaces = null;
		this.fromToCompare = toComparableLong(from, Long.MIN_VALUE);
		this.toToCompare = toComparableLong(to, Long.MAX_VALUE);
	}

	private NumberRange(@Nullable BigDecimal from, @Nullable BigDecimal to, int retainedDecimalPlaces) {
		assertEitherBoundaryNotNull(from, to);
		assertFromLesserThanTo(from, to);
		this.from = from;
		this.to = to;
		this.retainedDecimalPlaces = retainedDecimalPlaces;
		this.fromToCompare = toComparableLong(from, retainedDecimalPlaces, Long.MIN_VALUE);
		this.toToCompare = toComparableLong(to, retainedDecimalPlaces, Long.MAX_VALUE);
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link NumberRange}.
	 */
	public static Long toComparableLong(@Nonnull Number theValue) {
		return theValue.longValue();
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link NumberRange}.
	 */
	private static Long toComparableLong(@Nullable Number theValue, long nullValue) {
		return toComparableLong(ofNullable(theValue).orElse(nullValue));
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link NumberRange} using zero retained decimal places.
	 */
	public static Long toComparableLong(@Nonnull BigDecimal theValue) {
		return theValue.longValueExact();
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link NumberRange} using zero retained decimal places.
	 */
	private static Long toComparableLong(@Nullable BigDecimal theValue, long nullValue) {
		return toComparableLong(ofNullable(theValue).map(BigDecimal::longValueExact).orElse(nullValue));
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link NumberRange} using retained decimal places.
	 */
	public static Long toComparableLong(@Nonnull BigDecimal theValue, int retainedDecimalPlaces) {
		return theValue.setScale(retainedDecimalPlaces, RoundingMode.HALF_UP)
			.scaleByPowerOfTen(retainedDecimalPlaces)
			.longValueExact();
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link NumberRange} using retained decimal places.
	 */
	private static Long toComparableLong(@Nullable BigDecimal theValue, int retainedDecimalPlaces, long nullValue) {
		return ofNullable(theValue)
			.map(it -> toComparableLong(theValue, retainedDecimalPlaces))
			.orElse(nullValue);
	}

	/**
	 * Parses string to {@link NumberRange} or throws an exception. String must conform to the format produced
	 * by {@link NumberRange#toString()} method. Parsed Number range always uses {@link BigDecimal} for numbers.
	 */
	@Nonnull
	public static NumberRange fromString(String string) throws DataTypeParseException {
		Assert.isTrue(
			string.startsWith(OPEN_CHAR) && string.endsWith(CLOSE_CHAR),
			() -> new DataTypeParseException("NumberRange must start with " + OPEN_CHAR + " and end with " + CLOSE_CHAR + "!")
		);
		final int delimiter = string.indexOf(INTERVAL_JOIN, 1);
		Assert.isTrue(
			delimiter > -1,
			() -> new DataTypeParseException("NumberRange must contain " + INTERVAL_JOIN + " to separate from and to dates!")
		);
		final BigDecimal from = delimiter == 1 ? null : parseBigDecimal(string.substring(1, delimiter));
		final BigDecimal to = delimiter == string.length() - 2 ? null : parseBigDecimal(string.substring(delimiter + 1, string.length() - 1));
		if (from == null && to != null) {
			return to(to, to.scale());
		} else if (from != null && to == null) {
			return from(from, from.scale());
		} else if (from != null) {
			return between(from, to, Math.max(from.scale(), to.scale()));
		} else {
			throw new DataTypeParseException("Range has no sense with both limits open to infinity!");
		}
	}

	private static BigDecimal parseBigDecimal(String toBeNumber) {
		try {
			return new BigDecimal(toBeNumber);
		} catch (NumberFormatException ex) {
			throw new DataTypeParseException("String " + toBeNumber + " is not a number!");
		}
	}

	/**
	 * Method creates new NumberRange instance.
	 */
	public static NumberRange between(Number from, Number to) {
		return new NumberRange(from, to);
	}

	/**
	 * Method creates new NumberRange instance when only upper range bound is available.
	 */
	public static NumberRange to(Number to) {
		return new NumberRange(null, to);
	}

	/**
	 * Method creates new NumberRange instance when only lower range bound is available.
	 */
	public static NumberRange from(Number from) {
		return new NumberRange(from, null);
	}

	/**
	 * Method creates new BigDecimalRange instance.
	 */
	public static NumberRange between(BigDecimal from, BigDecimal to) {
		return new NumberRange(from, to);
	}

	/**
	 * Method creates new BigDecimalRange instance when only lower range bound is available.
	 */
	public static NumberRange from(BigDecimal from) {
		return new NumberRange(from, null);
	}

	/**
	 * Method creates new BigDecimalRange instance when only upper range bound is available.
	 */
	public static NumberRange to(BigDecimal to) {
		return new NumberRange(null, to);
	}

	/**
	 * Method creates new BigDecimalRange instance.
	 *
	 * @param retainedDecimalPlaces defines how many fractional places will be kept for comparison
	 */
	public static NumberRange between(BigDecimal from, BigDecimal to, int retainedDecimalPlaces) {
		return new NumberRange(from, to, retainedDecimalPlaces);
	}

	/**
	 * Method creates new BigDecimalRange instance when only lower range bound is available.
	 *
	 * @param retainedDecimalPlaces defines how many fractional places will be kept for comparison
	 */
	public static NumberRange from(BigDecimal from, int retainedDecimalPlaces) {
		return new NumberRange(from, null, retainedDecimalPlaces);
	}

	/**
	 * Method creates new BigDecimalRange instance when only upper range bound is available.
	 *
	 * @param retainedDecimalPlaces defines how many fractional places will be kept for comparison
	 */
	public static NumberRange to(BigDecimal to, int retainedDecimalPlaces) {
		return new NumberRange(null, to, retainedDecimalPlaces);
	}

	@Override
	public long getFrom() {
		return fromToCompare;
	}

	@Override
	public long getTo() {
		return toToCompare;
	}

	@Override
	public Number getPreciseFrom() {
		return from;
	}

	@Override
	public Number getPreciseTo() {
		return to;
	}

	@Override
	public Range<Number> cloneWithDifferentBounds(Number from, Number to) {
		if (this.from instanceof BigDecimal) {
			if (this.retainedDecimalPlaces != null) {
				return new NumberRange((BigDecimal) from, (BigDecimal) to, this.retainedDecimalPlaces);
			} else {
				return new NumberRange((BigDecimal) from, (BigDecimal) to);
			}
		} else {
			return new NumberRange(from, to);
		}
	}

	@Override
	public int compareTo(@Nonnull NumberRange o) {
		final int leftBoundCompare = Long.compare(getFrom(), o.getFrom());
		final int rightBoundCompare = Long.compare(getTo(), o.getTo());
		if (leftBoundCompare != 0) {
			return leftBoundCompare;
		} else {
			return rightBoundCompare;
		}
	}

	@Override
	public String toString() {
		return OPEN_CHAR + ofNullable(from).map(Object::toString).orElse("") +
			INTERVAL_JOIN + ofNullable(to).map(Object::toString).orElse("") + CLOSE_CHAR;
	}

	/**
	 * Returns TRUE when value to check is withing the current number range (inclusive).
	 */
	@Override
	public boolean isWithin(Number valueToCheck) {
		Assert.notNull(valueToCheck, "Cannot resolve within range with NULL value!");
		final long valueToCompare = toComparableLong(valueToCheck, 0L);
		return fromToCompare <= valueToCompare && valueToCompare <= toToCompare;
	}

	/**
	 * Returns TRUE when value to check is withing the current number range (inclusive).
	 */
	public boolean isWithin(BigDecimal valueToCheck) {
		checkNotNullValueToCheck(valueToCheck);
		final long valueToCompare = toComparableLong(valueToCheck, 0L);
		return fromToCompare <= valueToCompare && valueToCompare <= toToCompare;
	}

	/**
	 * Returns TRUE when value to check is withing the current number range (inclusive).
	 *
	 * @param retainedDecimalPlaces should represent same number of decimal places as the range itself
	 */
	public boolean isWithin(BigDecimal valueToCheck, int retainedDecimalPlaces) {
		checkNotNullValueToCheck(valueToCheck);
		final Long valueToCompare = toComparableLong(valueToCheck, retainedDecimalPlaces, 0L);
		return fromToCompare <= valueToCompare && valueToCompare <= toToCompare;
	}

	private void assertNotFloatingPointType(@Nullable Number from, final String argName) {
		if (from instanceof Float || from instanceof Double) {
			throw new IllegalArgumentException("For " + argName + " number with floating point use BigDecimal that keeps the precision!");
		}
	}

	private void assertEitherBoundaryNotNull(@Nullable Number from, @Nullable Number to) {
		if (from == null && to == null) {
			throw new IllegalArgumentException("From and to cannot be both null at the same time in NumberRange type!");
		}
	}

	private void assertFromLesserThanTo(@Nullable Number from, @Nullable Number to) {
		//noinspection unchecked,rawtypes
		if (!(from == null || to == null || ((Comparable) from).compareTo(to) <= 0)) {
			throw new IllegalArgumentException("From must be before or equals to to!");
		}
	}

	private void assertNoDecimalPlaces(@Nullable BigDecimal from, final String argName) {
		if (from != null && from.stripTrailingZeros().scale() > 0) {
			throw new IllegalArgumentException(
				"You cannot pass BigDecimal with fractional part to number range " + argName + " unless you provide " +
					"information how many fractional digits are important for comparison."
			);
		}
	}

	private static void checkNotNullValueToCheck(BigDecimal valueToCheck) {
		Assert.notNull(valueToCheck, "Cannot resolve within range with NULL value!");
	}

}
