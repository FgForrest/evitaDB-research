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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

/**
 * Range type that envelopes {@link java.time.ZonedDateTime} types.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode(of = {"fromToCompare", "toToCompare"}, cacheStrategy = CacheStrategy.LAZY)
public class DateTimeRange implements Range<ZonedDateTime>, Serializable, Comparable<DateTimeRange> {
	private static final long serialVersionUID = 7690351814641934282L;
	private static final Pattern PARSE_PATTERN = Pattern.compile("^" + Pattern.quote(OPEN_CHAR) + "(\\S+?)?\\s*" + Pattern.quote(INTERVAL_JOIN) + "\\s*(\\S+?)?" + Pattern.quote(CLOSE_CHAR) + "$");
	public static final Function<String, String[]> PARSE_FCT = string -> {
		final Matcher matcher = PARSE_PATTERN.matcher(string);
		return matcher.matches() ? new String[]{matcher.group(1), matcher.group(2)} : null;
	};

	private final ZonedDateTime from;
	private final ZonedDateTime to;
	private final long fromToCompare;
	private final long toToCompare;

	private DateTimeRange(@Nullable ZonedDateTime from, @Nullable ZonedDateTime to) {
		assertEitherBoundaryNotNull(from, to);
		assertFromLesserThanTo(from, to);
		this.from = from;
		this.to = to;
		this.fromToCompare = toComparableLong(ofNullable(from).orElseGet(() -> LocalDateTime.MIN.atZone(to.getZone())));
		this.toToCompare = toComparableLong(ofNullable(to).orElseGet(() -> LocalDateTime.MAX.atZone(from.getZone())));
	}

	/**
	 * Converts unknown value to a long that is comparable with {@link DateTimeRange}.
	 */
	public static Long toComparableLong(@Nonnull ZonedDateTime theMoment) {
		return theMoment.toEpochSecond();
	}

	/**
	 * Parses string to {@link DateTimeRange} or throws an exception. String must conform to the format produced
	 * by {@link DateTimeRange#toString()} method.
	 */
	@Nonnull
	public static DateTimeRange fromString(String string) throws DataTypeParseException {
		Assert.isTrue(
			string.startsWith(OPEN_CHAR) && string.endsWith(CLOSE_CHAR),
			() -> new DataTypeParseException("DateTimeRange must start with " + OPEN_CHAR + " and end with " + CLOSE_CHAR + "!")
		);
		final int delimiter = string.indexOf(INTERVAL_JOIN, 1);
		Assert.isTrue(
			delimiter > -1,
			() -> new DataTypeParseException("DateTimeRange must contain " + INTERVAL_JOIN + " to separate since and until dates!")
		);
		final ZonedDateTime since = delimiter == 1 ? null : parseDateTime(string.substring(1, delimiter));
		final ZonedDateTime until = delimiter == string.length() - 2 ? null : parseDateTime(string.substring(delimiter + 1, string.length() - 1));
		if (since == null && until != null) {
			return until(until);
		} else if (since != null && until == null) {
			return since(since);
		} else if (since != null) {
			return between(since, until);
		} else {
			throw new DataTypeParseException("Range has no sense with both limits open to infinity!");
		}
	}

	private static ZonedDateTime parseDateTime(String substring) {
		try {
			return ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(substring));
		} catch (DateTimeException ex) {
			throw new DataTypeParseException("Unable to parse date from string: " + substring);
		}
	}

	/**
	 * Method creates new DateTimeRange instance.
	 */
	public static DateTimeRange between(ZonedDateTime from, ZonedDateTime to) {
		return new DateTimeRange(from, to);
	}

	/**
	 * Method creates new DateTimeRange instance.
	 */
	public static DateTimeRange between(LocalDateTime from, LocalDateTime to, ZoneId zoneId) {
		return new DateTimeRange(from.atZone(zoneId), to.atZone(zoneId));
	}

	/**
	 * Method creates new DateTimeRange instance when only upper range bound is available.
	 */
	public static DateTimeRange until(ZonedDateTime to) {
		return new DateTimeRange(null, to);
	}

	/**
	 * Method creates new DateTimeRange instance when only upper range bound is available.
	 */
	public static DateTimeRange until(LocalDateTime to, ZoneId zoneId) {
		return new DateTimeRange(null, to.atZone(zoneId));
	}

	/**
	 * Method creates new DateTimeRange instance when only lower range bound is available.
	 */
	public static DateTimeRange since(ZonedDateTime from) {
		return new DateTimeRange(from, null);
	}

	/**
	 * Method creates new DateTimeRange instance when only lower range bound is available.
	 */
	public static DateTimeRange since(LocalDateTime from, ZoneId zoneId) {
		return new DateTimeRange(from.atZone(zoneId), null);
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
	public ZonedDateTime getPreciseFrom() {
		return from;
	}

	@Override
	public ZonedDateTime getPreciseTo() {
		return to;
	}

	@Override
	public boolean isWithin(ZonedDateTime valueToCheck) {
		final long comparedValue = DateTimeRange.toComparableLong(valueToCheck);
		return fromToCompare <= comparedValue && comparedValue <= toToCompare;
	}

	@Override
	public Range<ZonedDateTime> cloneWithDifferentBounds(ZonedDateTime from, ZonedDateTime to) {
		return new DateTimeRange(from, to);
	}

	/**
	 * Returns true if passed moment is within the specified range (inclusive).
	 */
	public boolean isValidFor(ZonedDateTime theMoment) {
		final long comparedValue = theMoment.toEpochSecond();
		return fromToCompare <= comparedValue && toToCompare >= comparedValue;
	}

	@Override
	public int compareTo(@Nonnull DateTimeRange o) {
		final int leftBoundCompare = Long.compare(getFrom(), o.getFrom());
		final int rightBoundCompare = Long.compare(getTo(), o.getTo());
		if (leftBoundCompare != 0) {
			return leftBoundCompare;
		} else {
			return rightBoundCompare;
		}
	}

	/**
	 * Formats {@link DateTimeRange} to string.
	 */
	@Override
	public String toString() {
		return OPEN_CHAR + ofNullable(from).map(DateTimeFormatter.ISO_DATE_TIME::format).orElse("") +
			INTERVAL_JOIN + ofNullable(to).map(DateTimeFormatter.ISO_DATE_TIME::format).orElse("") + CLOSE_CHAR;
	}

	private void assertEitherBoundaryNotNull(@Nullable ZonedDateTime from, @Nullable ZonedDateTime to) {
		if (from == null && to == null) {
			throw new IllegalArgumentException("From and to cannot be both null at the same time in DateTimeRange type!");
		}
	}

	private void assertFromLesserThanTo(@Nullable ZonedDateTime from, @Nullable ZonedDateTime to) {
		if (!(from == null || to == null || from.equals(to) || from.isBefore(to))) {
			throw new IllegalArgumentException("From must be before or equals to to!");
		}
	}

}
