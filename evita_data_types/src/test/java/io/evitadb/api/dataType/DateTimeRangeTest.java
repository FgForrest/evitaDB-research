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
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static io.evitadb.api.dataType.DateTimeRange.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks creation of the {@link DateTimeRange} data type.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class DateTimeRangeTest {

	@Test
	void shouldFailToConstructUnreasonableRange() {
		assertThrows(IllegalArgumentException.class, () -> between(null, null));
	}

	@Test
	void shouldConstructBetweenZonedDateTime() {
		final DateTimeRange range = between(getZonedDateTime(1), getZonedDateTime(2));
		assertEquals(getZonedDateTime(1), range.getPreciseFrom());
		assertEquals(getZonedDateTime(2), range.getPreciseTo());
		assertEquals(1609514565L, range.getFrom());
		assertEquals(1609600965L, range.getTo());
		assertEquals("[2021-01-01T12:22:45-03:00[America/Sao_Paulo],2021-01-02T12:22:45-03:00[America/Sao_Paulo]]", range.toString());
		assertEquals(range, between(getZonedDateTime(1), getZonedDateTime(2)));
		assertNotSame(range, between(getZonedDateTime(1), getZonedDateTime(2)));
		assertNotEquals(range, between(getZonedDateTime(1), getZonedDateTime(3)));
		assertNotEquals(range, between(getZonedDateTime(2), getZonedDateTime(2)));
		assertEquals(range.hashCode(), between(getZonedDateTime(1), getZonedDateTime(2)).hashCode());
		assertNotEquals(range.hashCode(), between(getZonedDateTime(1), getZonedDateTime(3)).hashCode());
	}

	@Test
	void shouldConstructFromZonedDateTime() {
		final DateTimeRange range = since(getZonedDateTime(1));
		assertEquals(getZonedDateTime(1), range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1609514565L, range.getFrom());
		assertEquals(31556889832791599L, range.getTo());
		assertEquals("[2021-01-01T12:22:45-03:00[America/Sao_Paulo],]", range.toString());
		assertEquals(range, since(getZonedDateTime(1)));
		assertNotSame(range, since(getZonedDateTime(1)));
		assertNotEquals(range, since(getZonedDateTime(2)));
		assertEquals(range.hashCode(), since(getZonedDateTime(1)).hashCode());
		assertNotEquals(range.hashCode(), since(getZonedDateTime(2)).hashCode());
	}

	@Test
	void shouldConstructToZonedDateTime() {
		final DateTimeRange range = until(getZonedDateTime(1));
		assertEquals(getZonedDateTime(1), range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1609514565L, range.getTo());
		assertEquals(-31557014135585612L, range.getFrom());
		assertEquals("[,2021-01-01T12:22:45-03:00[America/Sao_Paulo]]", range.toString());
		assertEquals(range, until(getZonedDateTime(1)));
		assertNotSame(range, until(getZonedDateTime(1)));
		assertNotEquals(range, until(getZonedDateTime(2)));
		assertEquals(range.hashCode(), until(getZonedDateTime(1)).hashCode());
		assertNotEquals(range.hashCode(), until(getZonedDateTime(2)).hashCode());
	}

	@Test
	void shouldConstructBetweenLocalDateTime() {
		final DateTimeRange range = between(getLocalDateTime(1), getLocalDateTime(2), getZoneId());
		assertEquals(getLocalDateTime(1).atZone(getZoneId()), range.getPreciseFrom());
		assertEquals(getLocalDateTime(2).atZone(getZoneId()), range.getPreciseTo());
		assertEquals(1609514565L, range.getFrom());
		assertEquals(1609600965L, range.getTo());
		assertEquals("[2021-01-01T12:22:45-03:00[America/Sao_Paulo],2021-01-02T12:22:45-03:00[America/Sao_Paulo]]", range.toString());
		assertEquals(range, between(getLocalDateTime(1), getLocalDateTime(2), getZoneId()));
		assertNotSame(range, between(getLocalDateTime(1), getLocalDateTime(2), getZoneId()));
		assertNotEquals(range, between(getLocalDateTime(1), getLocalDateTime(3), getZoneId()));
		assertNotEquals(range, between(getLocalDateTime(2), getLocalDateTime(2), getZoneId()));
		assertEquals(range.hashCode(), between(getLocalDateTime(1), getLocalDateTime(2), getZoneId()).hashCode());
		assertNotEquals(range.hashCode(), between(getLocalDateTime(1), getLocalDateTime(3), getZoneId()).hashCode());
	}

	@Test
	void shouldConstructFromLocalDateTime() {
		final DateTimeRange range = since(getLocalDateTime(1), getZoneId());
		assertEquals(getLocalDateTime(1).atZone(getZoneId()), range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1609514565L, range.getFrom());
		assertEquals(31556889832791599L, range.getTo());
		assertEquals("[2021-01-01T12:22:45-03:00[America/Sao_Paulo],]", range.toString());
		assertEquals(range, since(getLocalDateTime(1), getZoneId()));
		assertNotSame(range, since(getLocalDateTime(1), getZoneId()));
		assertNotEquals(range, since(getLocalDateTime(2), getZoneId()));
		assertEquals(range.hashCode(), since(getLocalDateTime(1), getZoneId()).hashCode());
		assertNotEquals(range.hashCode(), since(getLocalDateTime(2), getZoneId()).hashCode());
	}

	@Test
	void shouldConstructToLocalDateTime() {
		final DateTimeRange range = until(getLocalDateTime(1), getZoneId());
		assertEquals(getLocalDateTime(1).atZone(getZoneId()), range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1609514565L, range.getTo());
		assertEquals(-31557014135585612L, range.getFrom());
		assertEquals("[,2021-01-01T12:22:45-03:00[America/Sao_Paulo]]", range.toString());
		assertEquals(range, until(getLocalDateTime(1), getZoneId()));
		assertNotSame(range, until(getLocalDateTime(1), getZoneId()));
		assertNotEquals(range, until(getLocalDateTime(2), getZoneId()));
		assertEquals(range.hashCode(), until(getLocalDateTime(1), getZoneId()).hashCode());
		assertNotEquals(range.hashCode(), until(getLocalDateTime(2), getZoneId()).hashCode());
	}

	@Test
	void shouldCompareRanges() {
		assertTrue(since(getZonedDateTime(1)).compareTo(since(getZonedDateTime(2))) < 0);
		assertTrue(since(getZonedDateTime(1)).compareTo(since(getZonedDateTime(1))) == 0);
		assertTrue(since(getZonedDateTime(2)).compareTo(since(getZonedDateTime(1))) > 0);

		assertTrue(until(getZonedDateTime(1)).compareTo(until(getZonedDateTime(2))) < 0);
		assertTrue(until(getZonedDateTime(1)).compareTo(until(getZonedDateTime(1))) == 0);
		assertTrue(until(getZonedDateTime(2)).compareTo(until(getZonedDateTime(1))) > 0);

		assertTrue(between(getZonedDateTime(1), getZonedDateTime(2)).compareTo(between(getZonedDateTime(2), getZonedDateTime(2))) < 0);
		assertTrue(between(getZonedDateTime(1), getZonedDateTime(2)).compareTo(between(getZonedDateTime(1), getZonedDateTime(2))) == 0);
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(2)).compareTo(between(getZonedDateTime(1), getZonedDateTime(2))) > 0);
		assertTrue(between(getZonedDateTime(1), getZonedDateTime(2)).compareTo(between(getZonedDateTime(1), getZonedDateTime(3))) < 0);
		assertTrue(between(getZonedDateTime(1), getZonedDateTime(3)).compareTo(between(getZonedDateTime(1), getZonedDateTime(2))) > 0);
	}

	@Test
	void shouldFormatAndParseSinceRangeWithoutError() {
		final DateTimeRange since = since(getZonedDateTime(1));
		assertEquals(since, DateTimeRange.fromString(since.toString()));
	}

	@Test
	void shouldFormatAndParseUntilRangeWithoutError() {
		final DateTimeRange until = until(getZonedDateTime(1));
		assertEquals(until, DateTimeRange.fromString(until.toString()));
	}

	@Test
	void shouldFormatAndParseBetweenRangeWithoutError() {
		final DateTimeRange between = between(getZonedDateTime(1), getZonedDateTime(5));
		assertEquals(between, DateTimeRange.fromString(between.toString()));
	}

	@Test
	void shouldFailToParseInvalidFormats() {
		assertThrows(DataTypeParseException.class, () -> DateTimeRange.fromString(""));
		assertThrows(DataTypeParseException.class, () -> DateTimeRange.fromString("[,]"));
		assertThrows(DataTypeParseException.class, () -> DateTimeRange.fromString("[a,b]"));
		assertThrows(DataTypeParseException.class, () -> DateTimeRange.fromString("[2021-01-01T12:22:45,2021-01-05T12:22:45]"));
	}

	@Test
	void shouldParseIncompleteFormat() {
		assertNotNull(DateTimeRange.fromString("[2021-01-01T12:22:45-03:00,2021-01-05T12:22:45-03:00]"));
	}

	@Test
	void shouldBeValidIn() {
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).isValidFor(getZonedDateTime(4)));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).isValidFor(getZonedDateTime(2)));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).isValidFor(getZonedDateTime(6)));
		assertFalse(between(getZonedDateTime(2), getZonedDateTime(6)).isValidFor(getZonedDateTime(1)));
		assertFalse(between(getZonedDateTime(2), getZonedDateTime(6)).isValidFor(getZonedDateTime(30)));
	}

	@Test
	void shouldComputeOverlapsCorrectly() {
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(3), getZonedDateTime(4))));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(2), getZonedDateTime(6))));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(1), getZonedDateTime(2))));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(1), getZonedDateTime(3))));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(6), getZonedDateTime(8))));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(5), getZonedDateTime(8))));
		assertTrue(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(1), getZonedDateTime(10))));
		assertFalse(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(1), getZonedDateTime(1))));
		assertFalse(between(getZonedDateTime(2), getZonedDateTime(6)).overlaps(between(getZonedDateTime(7), getZonedDateTime(10))));
	}

	@Test
	void shouldCorrectlyParseRegex() {
		assertArrayEquals(new String[] {"2021-01-01T12:22:45-03:00[America/Sao_Paulo]", "2021-01-02T12:22:45-03:00[America/Sao_Paulo]"}, DateTimeRange.PARSE_FCT.apply("[2021-01-01T12:22:45-03:00[America/Sao_Paulo],2021-01-02T12:22:45-03:00[America/Sao_Paulo]]"));
		assertArrayEquals(new String[] {"2021-01-01T12:22:45-03:00", "2021-01-05T12:22:45-03:00"}, DateTimeRange.PARSE_FCT.apply("[2021-01-01T12:22:45-03:00,2021-01-05T12:22:45-03:00]"));
		assertArrayEquals(new String[] {null, "2021-01-02T12:22:45-03:00[America/Sao_Paulo]"}, DateTimeRange.PARSE_FCT.apply("[,2021-01-02T12:22:45-03:00[America/Sao_Paulo]]"));
		assertArrayEquals(new String[] {"2021-01-01T12:22:45-03:00[America/Sao_Paulo]", null}, DateTimeRange.PARSE_FCT.apply("[2021-01-01T12:22:45-03:00[America/Sao_Paulo],]"));
	}

	@Test
	void shouldConsolidateOverlappingRanges() {
		assertArrayEquals(
			new DateTimeRange[] {
				between(getZonedDateTime(1), getZonedDateTime(9)),
				between(getZonedDateTime(25), getZonedDateTime(31)),
			},
			Range.consolidateRange(
				new DateTimeRange[] {
					between(getZonedDateTime(1), getZonedDateTime(5)),
					between(getZonedDateTime(25), getZonedDateTime(31)),
					between(getZonedDateTime(5), getZonedDateTime(6)),
					between(getZonedDateTime(3), getZonedDateTime(9)),
				}
			)
		);
	}

	private ZonedDateTime getZonedDateTime(int day) {
		return ZonedDateTime.of(2021, 1, day, 12, 22, 45, 0, getZoneId());
	}

	private LocalDateTime getLocalDateTime(int day) {
		return LocalDateTime.of(2021, 1, day, 12, 22, 45, 0);
	}

	private ZoneId getZoneId() {
		return ZoneId.of("America/Sao_Paulo");
	}

}