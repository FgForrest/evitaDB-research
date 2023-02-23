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

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;

import static io.evitadb.api.dataType.NumberRange.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks creation of the {@link NumberRange} data type.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class NumberRangeTest {

	@Test
	void shouldFailToConstructUnreasonableRange() {
		assertThrows(IllegalArgumentException.class, () -> between(null, null));
	}

	@Test
	void shouldConstructBetweenByte() {
		final NumberRange range = between((byte)1, (byte)2);
		assertEquals((byte)1, range.getPreciseFrom());
		assertEquals((byte)2, range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(2L, range.getTo());
		assertEquals("[1,2]", range.toString());
		assertEquals(range, between((byte)1, (byte)2));
		assertNotSame(range, between((byte)1, (byte)2));
		assertNotEquals(range, between((byte)1, (byte)3));
		assertNotEquals(range, between((byte)2, (byte)2));
		assertEquals(range.hashCode(), between((byte)1, (byte)2).hashCode());
		assertNotEquals(range.hashCode(), between((byte)1, (byte)3).hashCode());
	}

	@Test
	void shouldConstructFromByte() {
		final NumberRange range = from((byte)1);
		assertEquals((byte)1, range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1,]", range.toString());
		assertEquals(range, from((byte)1));
		assertNotSame(range, from((byte)1));
		assertNotEquals(range, from((byte)2));
		assertEquals(range.hashCode(), from((byte)1).hashCode());
		assertNotEquals(range.hashCode(), from((byte)2).hashCode());
	}

	@Test
	void shouldConstructToByte() {
		final NumberRange range = to((byte)1);
		assertEquals((byte)1, range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1]", range.toString());
		assertEquals(range, to((byte)1));
		assertNotSame(range, to((byte)1));
		assertNotEquals(range, to((byte)2));
		assertEquals(range.hashCode(), to((byte)1).hashCode());
		assertNotEquals(range.hashCode(), to((byte)2).hashCode());
	}

	@Test
	void shouldConstructBetweenShort() {
		final NumberRange range = between((short)1, (short)2);
		assertEquals((short)1, range.getPreciseFrom());
		assertEquals((short)2, range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(2L, range.getTo());
		assertEquals("[1,2]", range.toString());
		assertEquals(range, between((short)1, (short)2));
		assertNotSame(range, between((short)1, (short)2));
		assertNotEquals(range, between((short)1, (short)3));
		assertNotEquals(range, between((short)2, (short)2));
		assertEquals(range.hashCode(), between((short)1, (short)2).hashCode());
		assertNotEquals(range.hashCode(), between((short)1, (short)3).hashCode());
	}

	@Test
	void shouldConstructFromShort() {
		final NumberRange range = from((short)1);
		assertEquals((short)1, range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1,]", range.toString());
		assertEquals(range, from((short)1));
		assertNotSame(range, from((short)1));
		assertNotEquals(range, from((short)2));
		assertEquals(range.hashCode(), from((short)1).hashCode());
		assertNotEquals(range.hashCode(), from((short)2).hashCode());
	}

	@Test
	void shouldConstructToShort() {
		final NumberRange range = to((short)1);
		assertEquals((short)1, range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1]", range.toString());
		assertEquals(range, to((short)1));
		assertNotSame(range, to((short)1));
		assertNotEquals(range, to((short)2));
		assertEquals(range.hashCode(), to((short)1).hashCode());
		assertNotEquals(range.hashCode(), to((short)2).hashCode());
	}
	
	@Test
	void shouldConstructBetweenInt() {
		final NumberRange range = between(1, 2);
		assertEquals(1, range.getPreciseFrom());
		assertEquals(2, range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(2L, range.getTo());
		assertEquals("[1,2]", range.toString());
		assertEquals(range, between(1, 2));
		assertNotSame(range, between(1, 2));
		assertNotEquals(range, between(1, 3));
		assertNotEquals(range, between(2, 2));
		assertEquals(range.hashCode(), between(1, 2).hashCode());
		assertNotEquals(range.hashCode(), between(1, 3).hashCode());
	}

	@Test
	void shouldConstructFromInt() {
		final NumberRange range = from(1);
		assertEquals(1, range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1,]", range.toString());
		assertEquals(range, from(1));
		assertNotSame(range, from(1));
		assertNotEquals(range, from(2));
		assertEquals(range.hashCode(), from(1).hashCode());
		assertNotEquals(range.hashCode(), from(2).hashCode());
	}

	@Test
	void shouldConstructToInt() {
		final NumberRange range = to(1);
		assertEquals(1, range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1]", range.toString());
		assertEquals(range, to(1));
		assertNotSame(range, to(1));
		assertNotEquals(range, to(2));
		assertEquals(range.hashCode(), to(1).hashCode());
		assertNotEquals(range.hashCode(), to(2).hashCode());
	}

	@Test
	void shouldConstructBetweenLong() {
		final NumberRange range = between((long)1, (long)2);
		assertEquals((long)1, range.getPreciseFrom());
		assertEquals((long)2, range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(2L, range.getTo());
		assertEquals("[1,2]", range.toString());
		assertEquals(range, between((long)1, (long)2));
		assertNotSame(range, between((long)1, (long)2));
		assertNotEquals(range, between((long)1, (long)3));
		assertNotEquals(range, between((long)2, (long)2));
		assertEquals(range.hashCode(), between((long)1, (long)2).hashCode());
		assertNotEquals(range.hashCode(), between((long)1, (long)3).hashCode());
	}

	@Test
	void shouldConstructFromLong() {
		final NumberRange range = from((long)1);
		assertEquals((long)1, range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1,]", range.toString());
		assertEquals(range, from((long)1));
		assertNotSame(range, from((long)1));
		assertNotEquals(range, from((long)2));
		assertEquals(range.hashCode(), from((long)1).hashCode());
		assertNotEquals(range.hashCode(), from((long)2).hashCode());
	}

	@Test
	void shouldConstructToLong() {
		final NumberRange range = to((long)1);
		assertEquals((long)1, range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1]", range.toString());
		assertEquals(range, to((long)1));
		assertNotSame(range, to((long)1));
		assertNotEquals(range, to((long)2));
		assertEquals(range.hashCode(), to((long)1).hashCode());
		assertNotEquals(range.hashCode(), to((long)2).hashCode());
	}

	@Test
	void shouldFailToConstructBetweenFloat() {
		assertThrows(IllegalArgumentException.class, () -> between(1f, 2f));
	}

	@Test
	void shouldFailToConstructFromFloat() {
		assertThrows(IllegalArgumentException.class, () -> from(1f));
	}

	@Test
	void shouldFailToConstructToFloat() {
		assertThrows(IllegalArgumentException.class, () -> to(1f));
	}

	@Test
	void shouldFailToConstructBetweenDouble() {
		assertThrows(IllegalArgumentException.class, () -> between((double)1, (double)2));
	}

	@Test
	void shouldFailToConstructFromDouble() {
		assertThrows(IllegalArgumentException.class, () -> from((double)1));
	}

	@Test
	void shouldFailToConstructToDouble() {
		assertThrows(IllegalArgumentException.class, () -> to((double)1));
	}

	@Test
	void shouldFailToConstructBigDecimalWithFractionalPart() {
		assertThrows(IllegalArgumentException.class, () -> between(toBD("1.123"), toBD("2.1")));
	}

	@Test
	void shouldConstructBetweenBigDecimal() {
		final NumberRange range = between(new BigDecimal(1), new BigDecimal(2));
		assertEquals(new BigDecimal(1), range.getPreciseFrom());
		assertEquals(new BigDecimal(2), range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(2L, range.getTo());
		assertEquals("[1,2]", range.toString());
		assertEquals(range, between(new BigDecimal(1), new BigDecimal(2)));
		assertNotSame(range, between(new BigDecimal(1), new BigDecimal(2)));
		assertNotEquals(range, between(new BigDecimal(1), new BigDecimal(3)));
		assertNotEquals(range, between(new BigDecimal(2), new BigDecimal(2)));
		assertEquals(range.hashCode(), between(new BigDecimal(1), new BigDecimal(2)).hashCode());
		assertNotEquals(range.hashCode(), between(new BigDecimal(1), new BigDecimal(3)).hashCode());
	}

	@Test
	void shouldConstructFromBigDecimal() {
		final NumberRange range = from(new BigDecimal(1));
		assertEquals(new BigDecimal(1), range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1,]", range.toString());
		assertEquals(range, from(new BigDecimal(1)));
		assertNotSame(range, from(new BigDecimal(1)));
		assertNotEquals(range, from(new BigDecimal(2)));
		assertEquals(range.hashCode(), from(new BigDecimal(1)).hashCode());
		assertNotEquals(range.hashCode(), from(new BigDecimal(2)).hashCode());
	}

	@Test
	void shouldConstructToBigDecimal() {
		final NumberRange range = to(new BigDecimal(1));
		assertEquals(new BigDecimal(1), range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1]", range.toString());
		assertEquals(range, to(new BigDecimal(1)));
		assertNotSame(range, to(new BigDecimal(1)));
		assertNotEquals(range, to(new BigDecimal(2)));
		assertEquals(range.hashCode(), to(new BigDecimal(1)).hashCode());
		assertNotEquals(range.hashCode(), to(new BigDecimal(2)).hashCode());
	}

	@Test
	void shouldConstructBetweenBigDecimalWithFractionalPartRounding() {
		final NumberRange range = between(toBD("1.125"), toBD("2.125"), 2);
		assertEquals(toBD("1.125"), range.getPreciseFrom());
		assertEquals(toBD("2.125"), range.getPreciseTo());
		assertEquals(113L, range.getFrom());
		assertEquals(213L, range.getTo());
		assertEquals("[1.125,2.125]", range.toString());
		assertEquals(range, between(toBD("1.125"), toBD("2.125"), 2));
		assertNotSame(range, between(toBD("1.125"), toBD("2.125"), 2));
		assertNotEquals(range, between(toBD("1.125"), toBD("3.125"), 2));
		assertNotEquals(range, between(toBD("2.125"), toBD("2.125"), 2));
		assertEquals(range.hashCode(), between(toBD("1.125"), toBD("2.125"), 2).hashCode());
		assertNotEquals(range.hashCode(), between(toBD("1.125"), toBD("3.125"), 2).hashCode());
	}

	@Test
	void shouldConstructFromBigDecimalFractionalPartRounding() {
		final NumberRange range = from(toBD("1.125"), 2);
		assertEquals(toBD("1.125"), range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(113L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1.125,]", range.toString());
		assertEquals(range, from(toBD("1.125"), 2));
		assertNotSame(range, from(toBD("1.125"), 2));
		assertNotEquals(range, from(toBD("2.125"), 2));
		assertEquals(range.hashCode(), from(toBD("1.125"), 2).hashCode());
		assertNotEquals(range.hashCode(), from(toBD("2.125"), 2).hashCode());
	}

	@Test
	void shouldConstructToBigDecimalFractionalPartRounding() {
		final NumberRange range = to(toBD("1.125"), 2);
		assertEquals(toBD("1.125"), range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(113L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1.125]", range.toString());
		assertEquals(range, to(toBD("1.125"), 2));
		assertNotSame(range, to(toBD("1.125"), 2));
		assertNotEquals(range, to(toBD("2.125"), 2));
		assertEquals(range.hashCode(), to(toBD("1.125"), 2).hashCode());
		assertNotEquals(range.hashCode(), to(toBD("2.125"), 2).hashCode());
	}

	@Test
	void shouldConstructBetweenBigInteger() {
		final NumberRange range = between(new BigInteger("1"), new BigInteger("2"));
		assertEquals(new BigInteger("1"), range.getPreciseFrom());
		assertEquals(new BigInteger("2"), range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(2L, range.getTo());
		assertEquals("[1,2]", range.toString());
		assertEquals(range, between(new BigInteger("1"), new BigInteger("2")));
		assertNotSame(range, between(new BigInteger("1"), new BigInteger("2")));
		assertNotEquals(range, between(new BigInteger("1"), new BigInteger("3")));
		assertNotEquals(range, between(new BigInteger("2"), new BigInteger("2")));
		assertEquals(range.hashCode(), between(new BigInteger("1"), new BigInteger("2")).hashCode());
		assertNotEquals(range.hashCode(), between(new BigInteger("1"), new BigInteger("3")).hashCode());
	}

	@Test
	void shouldConstructFromBigInteger() {
		final NumberRange range = from(new BigInteger("1"));
		assertEquals(new BigInteger("1"), range.getPreciseFrom());
		assertNull(range.getPreciseTo());
		assertEquals(1L, range.getFrom());
		assertEquals(Long.MAX_VALUE, range.getTo());
		assertEquals("[1,]", range.toString());
		assertEquals(range, from(new BigInteger("1")));
		assertNotSame(range, from(new BigInteger("1")));
		assertNotEquals(range, from(new BigInteger("2")));
		assertEquals(range.hashCode(), from(new BigInteger("1")).hashCode());
		assertNotEquals(range.hashCode(), from(new BigInteger("2")).hashCode());
	}

	@Test
	void shouldConstructToBigInteger() {
		final NumberRange range = to(new BigInteger("1"));
		assertEquals(new BigInteger("1"), range.getPreciseTo());
		assertNull(range.getPreciseFrom());
		assertEquals(1L, range.getTo());
		assertEquals(Long.MIN_VALUE, range.getFrom());
		assertEquals("[,1]", range.toString());
		assertEquals(range, to(new BigInteger("1")));
		assertNotSame(range, to(new BigInteger("1")));
		assertNotEquals(range, to(new BigInteger("2")));
		assertEquals(range.hashCode(), to(new BigInteger("1")).hashCode());
		assertNotEquals(range.hashCode(), to(new BigInteger("2")).hashCode());
	}

	@Test
	void shouldCompareRanges() {
		assertTrue(from(1).compareTo(from(2)) < 0);
		assertTrue(from(1).compareTo(from(1)) == 0);
		assertTrue(from(2).compareTo(from(1)) > 0);

		assertTrue(to(1).compareTo(to(2)) < 0);
		assertTrue(to(1).compareTo(to(1)) == 0);
		assertTrue(to(2).compareTo(to(1)) > 0);

		assertTrue(between(1, 2).compareTo(between(2, 2)) < 0);
		assertTrue(between(1, 2).compareTo(between(1, 2)) == 0);
		assertTrue(between(2, 2).compareTo(between(1, 2)) > 0);
		assertTrue(between(1, 2).compareTo(between(1, 3)) < 0);
		assertTrue(between(1, 3).compareTo(between(1, 2)) > 0);
	}

	@Test
	void shouldFormatAndParsefromRangeWithoutError() {
		final NumberRange from = from(toBD("1.123"), 3);
		assertEquals(from, fromString(from.toString()));
	}

	@Test
	void shouldFormatAndParsetoRangeWithoutError() {
		final NumberRange to = to(toBD("1.123"), 3);
		assertEquals(to, fromString(to.toString()));
	}

	@Test
	void shouldFormatAndParseBetweenRangeWithoutError() {
		final NumberRange between = between(toBD("1.123"), toBD("5"), 3);
		assertEquals(between, fromString(between.toString()));
	}

	@Test
	void shouldFailToParseInvalidFormats() {
		assertThrows(DataTypeParseException.class, () -> fromString(""));
		assertThrows(DataTypeParseException.class, () -> fromString("[,]"));
		assertThrows(DataTypeParseException.class, () -> fromString("[a,b]"));
	}

	@Test
	void shouldResolveWithinCorrectly() {
		assertTrue(between(1,5).isWithin(3));
		assertTrue(between(1,5).isWithin(1));
		assertTrue(between(1,5).isWithin(5));
		assertFalse(between(1,5).isWithin(6));
		assertFalse(between(1,5).isWithin(0));
		assertFalse(between(1,5).isWithin(-1));
		assertFalse(between(1,5).isWithin(Integer.MAX_VALUE));
		assertFalse(between(1,5).isWithin(Integer.MIN_VALUE));
	}

	@Test
	void shouldResolveWithinCorrectlyWithBigDecimal() {
		assertTrue(between(toBD(1),toBD(5)).isWithin(toBD(3)));
		assertTrue(between(toBD(1),toBD(5)).isWithin(toBD(1)));
		assertTrue(between(toBD(1),toBD(5)).isWithin(toBD(5)));
		assertFalse(between(toBD(1),toBD(5)).isWithin(toBD(6)));
		assertFalse(between(toBD(1),toBD(5)).isWithin(toBD(0)));
		assertFalse(between(toBD(1),toBD(5)).isWithin(toBD(-1)));
		assertFalse(between(toBD(1),toBD(5)).isWithin(toBD(Integer.MAX_VALUE)));
		assertFalse(between(toBD(1),toBD(5)).isWithin(toBD(Integer.MIN_VALUE)));
	}

	@Test
	void shouldResolveWithinCorrectlyWithBigDecimalAndPrecision() {
		assertTrue(between(toBD("1.123"), toBD("5.123"), 1).isWithin(toBD("3.123"), 1));
		assertTrue(between(toBD("1.123"), toBD("5.123"), 1).isWithin(toBD("1.123"), 1));
		assertTrue(between(toBD("1.123"), toBD("5.123"), 1).isWithin(toBD("5.123"), 1));
		assertFalse(between(toBD("1.123"), toBD("5.123"), 1).isWithin(toBD("6.123"), 1));
		assertFalse(between(toBD("1.123"), toBD("5.123"), 1).isWithin(toBD("0.123"), 1));
		assertFalse(between(toBD("1.123"), toBD("5.123"), 1).isWithin(toBD("-1.123"), 1));
		//this can be true due to rounding and inclusivity
		assertTrue(between(toBD("1.1"), toBD("5.0"), 1).isWithin(toBD("5.00000001"), 1));
	}

	@Test
	void shouldComputeOverlapsCorrectly() {
		assertTrue(between(5,8).overlaps(between(6, 7)));
		assertTrue(between(5,8).overlaps(between(1, 10)));
		assertTrue(between(5,8).overlaps(between(5, 8)));
		assertTrue(between(5,8).overlaps(between(4, 5)));
		assertTrue(between(5,8).overlaps(between(4, 6)));
		assertTrue(between(5,8).overlaps(between(8, 9)));
		assertTrue(between(5,8).overlaps(between(7, 9)));
		assertFalse(between(5,8).overlaps(between(1, 4)));
		assertFalse(between(5,8).overlaps(between(9, 15)));
	}

	@Test
	void shouldComputeOverlapsCorrectlyWithBigDecimal() {
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(6), toBD(7))));
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(1), toBD(10))));
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(5), toBD(8))));
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(4), toBD(5))));
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(4), toBD(6))));
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(8), toBD(9))));
		assertTrue(between(toBD(5),toBD(8)).overlaps(between(toBD(7), toBD(9))));
		assertFalse(between(toBD(5),toBD(8)).overlaps(between(toBD(1), toBD(4))));
		assertFalse(between(toBD(5),toBD(8)).overlaps(between(toBD(9), toBD(15))));
	}

	@Test
	void shouldCorrectlyParseRegex() {
		assertArrayEquals(new String[] {"15.1", "78.9"}, NumberRange.PARSE_FCT.apply("[15.1,78.9]"));
		assertArrayEquals(new String[] {"15.1", null}, NumberRange.PARSE_FCT.apply("[15.1,]"));
		assertArrayEquals(new String[] {null, "78.9"}, NumberRange.PARSE_FCT.apply("[,78.9]"));
		assertArrayEquals(new String[] {"15.1", "78.9"}, NumberRange.PARSE_FCT.apply("[15.1 ,    78.9]"));
		assertArrayEquals(new String[] {"15.1", null}, NumberRange.PARSE_FCT.apply("[15.1 ,  ]"));
		assertArrayEquals(new String[] {null, "78.9"}, NumberRange.PARSE_FCT.apply("[ ,  78.9]"));
	}

	@Test
	void shouldConsolidateRanges() {
		assertArrayEquals(
			new NumberRange[] {
				between(2, 12),
				between(50, 55),
				between(80, 90),
			},
			Range.consolidateRange(
				new NumberRange[] {
					between(80, 90),
					between(51, 55),
					between(5, 12),
					between(2, 6),
					between(50, 51),
				}
			)
		);
	}

	private BigDecimal toBD(int integer) {
		return toBD(String.valueOf(integer));
	}

	@Nonnull
	private BigDecimal toBD(String s) {
		return new BigDecimal(s);
	}

}