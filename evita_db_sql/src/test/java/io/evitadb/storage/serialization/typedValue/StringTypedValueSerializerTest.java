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

package io.evitadb.storage.serialization.typedValue;

import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.EnumWrapper;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.NumberRange;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.Currency;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StringTypedValueSerializer}
 *
 * @author Lukáš Hornych 2021
 */
class StringTypedValueSerializerTest {

    private final StringTypedValueSerializer serializer = StringTypedValueSerializer.getInstance();

    @Test
    void shouldSerializeValueOfSupportedTypeToExpectedForm() {
        assertEquals(TypedValue.nullValue(), serializer.serialize(null));
        assertEquals(TypedValue.of("hello", String.class), serializer.serialize("hello"));
        assertEquals(TypedValue.of("c", Character.class), serializer.serialize('c'));
        assertEquals(TypedValue.of("10", Byte.class), serializer.serialize((byte) 10));
        assertEquals(TypedValue.of("10", Short.class), serializer.serialize((short) 10));
        assertEquals(TypedValue.of("10", Integer.class), serializer.serialize(10));
        assertEquals(TypedValue.of("10", Long.class), serializer.serialize(10L));
        assertEquals(TypedValue.of("true", Boolean.class), serializer.serialize(true));
        assertEquals(
                TypedValue.of("[2020-05-01T10:00:00+02:00[Europe/Prague],2020-06-01T10:00:00+02:00[Europe/Prague]]", DateTimeRange.class),
                serializer.serialize(DateTimeRange.between(
                        ZonedDateTime.ofInstant(LocalDateTime.of(2020, 5, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague")),
                        ZonedDateTime.ofInstant(LocalDateTime.of(2020, 6, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague"))
                ))
        );
        assertEquals(
                TypedValue.of("[2,10.5]", NumberRange.class),
                serializer.serialize(NumberRange.between(BigDecimal.valueOf(2), BigDecimal.valueOf(10.5), 1))
        );
        assertEquals(TypedValue.of("10.5", BigDecimal.class), serializer.serialize(BigDecimal.valueOf(10.5)));
        assertEquals(
                TypedValue.of("\"hello\",java.lang.String;10,java.lang.Integer", Multiple.class),
                serializer.serialize(new Multiple("hello", 10))
        );
        assertEquals(
                TypedValue.of("2020-01-01T10:00:00+01:00[Europe/Prague]", ZonedDateTime.class),
                serializer.serialize(ZonedDateTime.ofInstant(LocalDateTime.of(2020, 1, 1, 10, 0), ZoneOffset.of("+01:00"), ZoneId.of("Europe/Prague")))
        );
        assertEquals(
                TypedValue.of("2020-06-01T10:00:00", LocalDateTime.class),
                serializer.serialize(LocalDateTime.of(2020, 6, 1, 10, 0))
        );
        assertEquals(
                TypedValue.of("2020-06-01", LocalDate.class),
                serializer.serialize(LocalDate.of(2020, 6, 1))
        );
        assertEquals(
                TypedValue.of("10:00:00", LocalTime.class),
                serializer.serialize(LocalTime.of(10, 0))
        );
        assertEquals(TypedValue.of("cs_CZ", Locale.class), serializer.serialize(new Locale("cs", "CZ")));
        assertEquals(TypedValue.of("ONE", DummyEnum.class), serializer.serialize(DummyEnum.ONE));
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(EnumWrapper.fromString("ONE")));
        assertEquals(TypedValue.of("CZK", Currency.class), serializer.serialize(Currency.getInstance("CZK")));
    }


    @Test
    void shouldDeserializeValueOfSupportedTypeToOriginalForm() {
        assertNull(serializer.deserialize(TypedValue.nullValue()));
        assertEquals("hello", serializer.deserialize(TypedValue.of("hello", String.class)));
        assertEquals('c', (char) serializer.deserialize(TypedValue.of("c", Character.class)));
        assertEquals((byte) 10, (byte) serializer.deserialize(TypedValue.of("10", Byte.class)));
        assertEquals((short) 10, (short) serializer.deserialize(TypedValue.of("10", Short.class)));
        assertEquals(10, (int) serializer.deserialize(TypedValue.of("10", Integer.class)));
        assertEquals(10L, (long) serializer.deserialize(TypedValue.of("10", Long.class)));
        assertEquals(true, serializer.deserialize(TypedValue.of("true", Boolean.class)));
        assertEquals(
                DateTimeRange.between(
                        ZonedDateTime.ofInstant(LocalDateTime.of(2020, 5, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague")),
                        ZonedDateTime.ofInstant(LocalDateTime.of(2020, 6, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague"))
                ),
                serializer.deserialize(TypedValue.of("[2020-05-01T10:00:00+02:00[Europe/Prague],2020-06-01T10:00:00+02:00[Europe/Prague]]", DateTimeRange.class))
        );
        assertEquals(BigDecimal.valueOf(10.5), serializer.deserialize(TypedValue.of("10.5", BigDecimal.class)));
        assertEquals(
                NumberRange.between(BigDecimal.valueOf(2), BigDecimal.valueOf(10.5), 1),
                serializer.deserialize(TypedValue.of("[2,10.5]", NumberRange.class))
        );
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(TypedValue.of("\"hello\",java.lang.String;10,java.lang.Integer", Multiple.class)));
        assertEquals(
                ZonedDateTime.ofInstant(LocalDateTime.of(2020, 6, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague")),
                serializer.deserialize(TypedValue.of("2020-06-01T10:00:00+02:00[Europe/Prague]", ZonedDateTime.class))
        );
        assertEquals(
                LocalDateTime.of(2020, 6, 1, 10, 0),
                serializer.deserialize(TypedValue.of("2020-06-01T10:00:00", LocalDateTime.class))
        );
        assertEquals(
                LocalDate.of(2020, 6, 1),
                serializer.deserialize(TypedValue.of("2020-06-01", LocalDate.class))
        );
        assertEquals(
                LocalTime.of(10, 0),
                serializer.deserialize(TypedValue.of("10:00:00", LocalTime.class))
        );
        assertEquals(new Locale("cs", "CZ"), serializer.deserialize(TypedValue.of("cs_CZ", Locale.class)));
        assertEquals(DummyEnum.ONE, serializer.deserialize(TypedValue.of("ONE", DummyEnum.class)));
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(TypedValue.of("ONE", EnumWrapper.class)));
        assertEquals(Currency.getInstance("CZK"), serializer.deserialize(TypedValue.of("CZK", Currency.class)));
    }
}