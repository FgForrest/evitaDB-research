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

import io.evitadb.api.exception.InconvertibleDataTypeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

import static io.evitadb.api.dataType.EvitaDataTypes.formatValue;
import static io.evitadb.api.dataType.EvitaDataTypes.getWrappingPrimitiveClass;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test verifies {@link EvitaDataTypes} contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EvitaDataTypesTest {

	@Test
	void shouldFormatByte() {
		assertEquals("5", formatValue((byte)5));
	}

	@Test
	void shouldFormatShort() {
		assertEquals("5", formatValue((short)5));
	}

	@Test
	void shouldFormatInt() {
		assertEquals("5", formatValue(5));
	}

	@Test
	void shouldFormatLong() {
		assertEquals("5", formatValue((long)5));
	}

	@Test
	void shouldFormatChar() {
		assertEquals("'5'", formatValue('5'));
	}

	@Test
	void shouldFormatBoolean() {
		assertEquals("true", formatValue(true));
	}

	@Test
	void shouldFormatString() {
		assertEquals("'5'", formatValue("5"));
	}

	@Test
	void shouldFormatBigDecimal() {
		assertEquals("1.123", formatValue(new BigDecimal("1.123")));
	}

	@Test
	void shouldFormatEnum() {
		assertEquals("A", formatValue(SomeEnum.A));
	}

	@Test
	void shouldFormatEnumWrapper() {
		assertEquals("A", formatValue(EnumWrapper.fromString("A")));
	}

	@Test
	void shouldFormatLocale() {
		final Locale czechLocale = new Locale("cs", "CZ");
		assertEquals("`cs_CZ`", formatValue(czechLocale));
	}

	@Test
	void shouldFormatZonedDateTime() {
		final ZonedDateTime dateTime = ZonedDateTime.of(2021, 1, 30, 14, 45, 16, 0, ZoneId.of("Europe/Prague"));
		assertEquals("2021-01-30T14:45:16+01:00[Europe/Prague]", formatValue(dateTime));
	}

	@Test
	void shouldFormatLocalDateTime() {
		final LocalDateTime dateTime = LocalDateTime.of(2021, 1, 30, 14, 45, 16, 0);
		assertEquals("2021-01-30T14:45:16", formatValue(dateTime));
	}

	@Test
	void shouldFormatLocalDate() {
		final LocalDate date = LocalDate.of(2021, 1, 30);
		assertEquals("2021-01-30", formatValue(date));
	}

	@Test
	void shouldFormatLocalTime() {
		final LocalTime time = LocalTime.of(14, 45, 16, 0);
		assertEquals("14:45:16", formatValue(time));
	}

	@Test
	void shouldFormatNumberRange() {
		final NumberRange fromRange = NumberRange.from(new BigDecimal("45.45"), 2);
		assertEquals("[45.45,]", formatValue(fromRange));

		final NumberRange toRange = NumberRange.to(new BigDecimal("45.45"), 2);
		assertEquals("[,45.45]", formatValue(toRange));

		final NumberRange betweenRange = NumberRange.between(new BigDecimal("45.45"), new BigDecimal("89.5"), 2);
		assertEquals("[45.45,89.5]", formatValue(betweenRange));
	}

	@Test
	void shouldFormatDateTimeRange() {
		final ZonedDateTime dateTimeA = ZonedDateTime.of(2021, 1, 30, 14, 45, 16, 0, ZoneId.of("Europe/Prague"));
		final ZonedDateTime dateTimeB = ZonedDateTime.of(2022, 1, 30, 14, 45, 16, 0, ZoneId.of("Europe/Prague"));

		final DateTimeRange since = DateTimeRange.since(dateTimeA);
		assertEquals("[2021-01-30T14:45:16+01:00[Europe/Prague],]", formatValue(since));

		final DateTimeRange toRange = DateTimeRange.until(dateTimeA);
		assertEquals("[,2021-01-30T14:45:16+01:00[Europe/Prague]]", formatValue(toRange));

		final DateTimeRange betweenRange = DateTimeRange.between(dateTimeA, dateTimeB);
		assertEquals("[2021-01-30T14:45:16+01:00[Europe/Prague],2022-01-30T14:45:16+01:00[Europe/Prague]]", formatValue(betweenRange));
	}

	@Test
	void shouldFormatMultiple() {
		final Multiple multiple = new Multiple(1, 2);
		assertEquals("{1,2}", formatValue(multiple));
	}

	@Test
	void shouldReturnWrapperClass() {
		assertEquals(Integer.class, getWrappingPrimitiveClass(int.class));
		assertEquals(Character.class, getWrappingPrimitiveClass(char.class));
	}

	@Test
	void shouldConvertToByte() {
		assertEquals((byte)8, EvitaDataTypes.toTargetType(((byte)8), Byte.class));
		assertEquals((byte)8, EvitaDataTypes.toTargetType(((short)8), Byte.class));
		assertEquals((byte)8, EvitaDataTypes.toTargetType(((int)8), Byte.class));
		assertEquals((byte)8, EvitaDataTypes.toTargetType(((long)8), Byte.class));
		assertEquals((byte)8, EvitaDataTypes.toTargetType(new BigDecimal("8"), Byte.class));
		assertEquals((byte)8, EvitaDataTypes.toTargetType("8", Byte.class));
	}

	@Test
	void shouldFailToConvertToByte() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), Byte.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), Byte.class));
	}

	@Test
	void shouldConvertToShort() {
		assertEquals((short)8, EvitaDataTypes.toTargetType(((byte)8), Short.class));
		assertEquals((short)8, EvitaDataTypes.toTargetType(((short)8), Short.class));
		assertEquals((short)8, EvitaDataTypes.toTargetType(((int)8), Short.class));
		assertEquals((short)8, EvitaDataTypes.toTargetType(((long)8), Short.class));
		assertEquals((short)8, EvitaDataTypes.toTargetType(new BigDecimal("8"), Short.class));
		assertEquals((short)8, EvitaDataTypes.toTargetType("8", Short.class));
	}

	@Test
	void shouldFailToConvertToShort() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), Short.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), Short.class));
	}

	@Test
	void shouldConvertToInt() {
		assertEquals((int)8, EvitaDataTypes.toTargetType(((byte)8), Integer.class));
		assertEquals((int)8, EvitaDataTypes.toTargetType(((short)8), Integer.class));
		assertEquals((int)8, EvitaDataTypes.toTargetType(((int)8), Integer.class));
		assertEquals((int)8, EvitaDataTypes.toTargetType(((long)8), Integer.class));
		assertEquals((int)8, EvitaDataTypes.toTargetType(new BigDecimal("8"), Integer.class));
		assertEquals((int)8, EvitaDataTypes.toTargetType("8", Integer.class));
	}

	@Test
	void shouldFailToConvertToInt() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), Integer.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), Integer.class));
	}

	@Test
	void shouldConvertToLong() {
		assertEquals((long)8, EvitaDataTypes.toTargetType(((byte)8), Long.class));
		assertEquals((long)8, EvitaDataTypes.toTargetType(((short)8), Long.class));
		assertEquals((long)8, EvitaDataTypes.toTargetType(((int)8), Long.class));
		assertEquals((long)8, EvitaDataTypes.toTargetType(((long)8), Long.class));
		assertEquals((long)8, EvitaDataTypes.toTargetType(new BigDecimal("8"), Long.class));
		assertEquals((long)8, EvitaDataTypes.toTargetType("8", Long.class));
	}

	@Test
	void shouldFailToConvertToLong() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), Long.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), Long.class));
	}

	@Test
	void shouldConvertToBigDecimal() {
		assertEquals(new BigDecimal("8"), EvitaDataTypes.toTargetType(((byte)8), BigDecimal.class));
		assertEquals(new BigDecimal("8"), EvitaDataTypes.toTargetType(((short)8), BigDecimal.class));
		assertEquals(new BigDecimal("8"), EvitaDataTypes.toTargetType(((int)8), BigDecimal.class));
		assertEquals(new BigDecimal("8"), EvitaDataTypes.toTargetType(((long)8), BigDecimal.class));
		assertEquals(new BigDecimal("8"), EvitaDataTypes.toTargetType(new BigDecimal("8"), BigDecimal.class));
		assertEquals(new BigDecimal("8"), EvitaDataTypes.toTargetType("8", BigDecimal.class));
	}

	@Test
	void shouldFailToConvertToBigDecimal() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), BigDecimal.class));
	}

	@Test
	void shouldConvertToBoolean() {
		assertTrue(EvitaDataTypes.toTargetType(((byte)1), Boolean.class));
		assertTrue(EvitaDataTypes.toTargetType(true, Boolean.class));
		assertTrue(EvitaDataTypes.toTargetType("true", Boolean.class));
		assertFalse(EvitaDataTypes.toTargetType(((byte)0), Boolean.class));
		assertFalse(EvitaDataTypes.toTargetType("false", Boolean.class));
	}

	@Test
	void shouldConvertToChar() {
		assertEquals((char)8, EvitaDataTypes.toTargetType(((byte)8), Character.class));
		assertEquals((char)8, EvitaDataTypes.toTargetType(((short)8), Character.class));
		assertEquals((char)8, EvitaDataTypes.toTargetType(((char)8), Character.class));
		assertEquals((char)8, EvitaDataTypes.toTargetType(((long)8), Character.class));
		assertEquals((char)8, EvitaDataTypes.toTargetType(new BigDecimal("8"), Character.class));
		assertEquals((char)56, EvitaDataTypes.toTargetType("8", Character.class));
		assertEquals((char)65, EvitaDataTypes.toTargetType("A", Character.class));
	}

	@Test
	void shouldFailToConvertToChar() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), Character.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), Character.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", Character.class));
	}

	@Test
	void shouldConvertToZonedDateTime() {
		final ZonedDateTime theDate = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(theDate, EvitaDataTypes.toTargetType(theDate, ZonedDateTime.class));
		assertEquals(theDate, EvitaDataTypes.toTargetType(theDate.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZonedDateTime.class));
		assertEquals(theDate, EvitaDataTypes.toTargetType(theDate.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE), ZonedDateTime.class));
	}

	@Test
	void shouldFailToConvertToZonedDateTime() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), ZonedDateTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), ZonedDateTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", ZonedDateTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(LocalTime.of(11, 45), ZonedDateTime.class));
	}

	@Test
	void shouldConvertToLocalDateTime() {
		final ZonedDateTime theDate = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final LocalDateTime theLocalDate = LocalDateTime.of(2021, 1, 1, 0, 0, 0, 0);
		assertEquals(theLocalDate, EvitaDataTypes.toTargetType(theDate, LocalDateTime.class));
		assertEquals(theLocalDate, EvitaDataTypes.toTargetType(theDate.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), LocalDateTime.class));
		assertEquals(theLocalDate, EvitaDataTypes.toTargetType(theDate.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE), LocalDateTime.class));
	}

	@Test
	void shouldFailToConvertToLocalDateTime() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), LocalDateTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), LocalDateTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", LocalDateTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(LocalTime.of(11, 45), ZonedDateTime.class));
	}

	@Test
	void shouldConvertToLocalDate() {
		final ZonedDateTime theDate = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final LocalDate theLocalDate = LocalDate.of(2021, 1, 1);
		assertEquals(theLocalDate, EvitaDataTypes.toTargetType(theDate, LocalDate.class));
		assertEquals(theLocalDate, EvitaDataTypes.toTargetType(theDate.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), LocalDate.class));
		assertEquals(theLocalDate, EvitaDataTypes.toTargetType(theDate.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.class));
	}

	@Test
	void shouldFailToConvertToLocalDate() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), LocalDate.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), LocalDate.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", LocalDate.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(LocalTime.of(11, 45), ZonedDateTime.class));
	}

	@Test
	void shouldConvertToLocalTime() {
		final ZonedDateTime theDate = ZonedDateTime.of(2021, 1, 1, 11, 45, 51, 0, ZoneId.systemDefault());
		final LocalTime theLocalTime = LocalTime.of(11, 45, 51);
		assertEquals(theLocalTime, EvitaDataTypes.toTargetType(theDate, LocalTime.class));
		assertEquals(theLocalTime, EvitaDataTypes.toTargetType(theDate.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME), LocalTime.class));
	}

	@Test
	void shouldFailToConvertToLocalTime() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), LocalTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), LocalTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", LocalTime.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(LocalTime.of(11, 45), ZonedDateTime.class));
	}

	@Test
	void shouldConvertToDateTimeRange() {
		final ZonedDateTime theDate = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		assertEquals(DateTimeRange.since(theDate), EvitaDataTypes.toTargetType(DateTimeRange.since(theDate), DateTimeRange.class));
		assertEquals(DateTimeRange.since(theDate), EvitaDataTypes.toTargetType("[" + theDate.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ",]", DateTimeRange.class));
		assertEquals(DateTimeRange.since(theDate), EvitaDataTypes.toTargetType("[" + theDate.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + ",]", DateTimeRange.class));
	}


	@Test
	void shouldFailToConvertToDateTimeRange() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), DateTimeRange.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), DateTimeRange.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", DateTimeRange.class));
	}

	@Test
	void shouldConvertToNumberRange() {
		assertEquals(NumberRange.from(8), EvitaDataTypes.toTargetType(NumberRange.from(8), NumberRange.class));
		assertEquals(NumberRange.from(8), EvitaDataTypes.toTargetType("[8,]", NumberRange.class));
		assertEquals(NumberRange.to(new BigDecimal("8.78"), 2), EvitaDataTypes.toTargetType("[,8.78]", NumberRange.class, 2));
	}

	@Test
	void shouldFailToConvertToNumberRange() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), NumberRange.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), NumberRange.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("AB", NumberRange.class));
	}

	@Test
	void shouldConvertToLocale() {
		assertEquals(Locale.ENGLISH, EvitaDataTypes.toTargetType(Locale.ENGLISH, Locale.class));
		assertEquals(Locale.ENGLISH, EvitaDataTypes.toTargetType("en", Locale.class));
	}

	@Test
	void shouldFailToConvertToLocale() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType("WHATEVER", Locale.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), Locale.class));
	}

	@Test
	void shouldConvertToEnum() {
		assertEquals(SomeEnum.A, EvitaDataTypes.toTargetType(SomeEnum.A, SomeEnum.class));
		assertEquals(SomeEnum.A, EvitaDataTypes.toTargetType("A", SomeEnum.class));
	}

	@Test
	void shouldFailToConvertToEnum() {
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(Currency.getInstance("CZK"), SomeEnum.class));
		assertThrows(InconvertibleDataTypeException.class, () -> EvitaDataTypes.toTargetType(new BigDecimal("8.78"), SomeEnum.class));
	}

	public enum SomeEnum {
		A, B
	}

}
