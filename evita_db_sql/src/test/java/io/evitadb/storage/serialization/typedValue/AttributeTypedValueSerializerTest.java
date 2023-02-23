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
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.serialization.sql.PGIntRange;
import io.evitadb.storage.serialization.sql.PGIntRangeArray;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;

import static io.evitadb.storage.serialization.typedValue.AttributeTypedValue.TargetType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AttributeTypedValueSerializer}.
 *
 * @author Lukáš Hornych 2021
 */
class AttributeTypedValueSerializerTest {

    private final AttributeSchema schema = new AttributeSchema("att", false, true, true, true, Serializable.class, 1);
    private final AttributeTypedValueSerializer serializer = AttributeTypedValueSerializer.getInstance();

    @Test
    void shouldSerializeSingleValue() {
        assertSerializedValue(AttributeTypedValue.of(null, STRING, null), (Serializable) null);
        assertSerializedValue(AttributeTypedValue.of("hello", STRING, String.class), "hello");
        assertSerializedValue(AttributeTypedValue.of("c", STRING, Character.class), 'c');
        assertSerializedValue(AttributeTypedValue.of(10L, INT, Byte.class), (byte) 10);
        assertSerializedValue(AttributeTypedValue.of(10L, INT, Short.class), (short) 10);
        assertSerializedValue(AttributeTypedValue.of(10L, INT, Integer.class), 10);
        assertSerializedValue(AttributeTypedValue.of(10L, INT, Long.class), 10L);
        assertSerializedValue(AttributeTypedValue.of(1L, INT, Boolean.class), true);
        assertSerializedValue(AttributeTypedValue.of(0L, INT, Boolean.class), false);
        assertSerializedValue(
                AttributeTypedValue.of(new PGIntRange(1588320000L, 1590998400L), INT_RANGE, DateTimeRange.class),
                DateTimeRange.between(
                        ZonedDateTime.ofInstant(LocalDateTime.of(2020, 5, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague")),
                        ZonedDateTime.ofInstant(LocalDateTime.of(2020, 6, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague"))
                )
        );
        assertSerializedValue(
                AttributeTypedValue.of(new PGIntRange(-31557014135600264L, 31556889832777199L), INT_RANGE, DateTimeRange.class),
                DateTimeRange.between(LocalDateTime.MIN, LocalDateTime.MAX, ZoneId.systemDefault())
        );
        assertSerializedValue(
                AttributeTypedValue.of(105L, INT, BigDecimal.class),
                BigDecimal.valueOf(10.5)
        );
        assertSerializedValue(
                AttributeTypedValue.of(new PGIntRange(100L, 155L), INT_RANGE, NumberRange.class),
                NumberRange.between(new BigDecimal("10"), new BigDecimal("15.5"), 1)
        );
        assertSerializedValue(
                AttributeTypedValue.of("\"hello\";800000000000000a", STRING, Multiple.class),
                new Multiple("hello", 10)
        );
        assertSerializedValue(
                AttributeTypedValue.of(1577869200L, INT, ZonedDateTime.class),
                ZonedDateTime.ofInstant(LocalDateTime.of(2020, 1, 1, 10, 0), ZoneOffset.of("+01:00"), ZoneId.of("Europe/Prague"))
        );
        assertSerializedValue(AttributeTypedValue.of(1591005600L, INT, LocalDateTime.class), LocalDateTime.of(2020, 6, 1, 10, 0));
        assertSerializedValue(AttributeTypedValue.of(1590969600L, INT, LocalDate.class), LocalDate.of(2020, 6, 1));
        assertSerializedValue(AttributeTypedValue.of(36000L, INT, LocalTime.class), LocalTime.of(10, 0));
        assertSerializedValue(AttributeTypedValue.of("cs_CZ", STRING, Locale.class), new Locale("cs", "CZ"));
        assertSerializedValue(AttributeTypedValue.of("ONE", STRING, DummyEnum.class), DummyEnum.ONE);
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(EnumWrapper.fromString("ONE"), schema));
    }

    @Test
    void shouldSerializeArrayOfValues() {
        final AttributeSchema schema = new AttributeSchema("att", false, true, true, true, Serializable.class, 1);

        assertThrows(IllegalArgumentException.class, () -> serializer.serialize((Serializable[]) null, schema));
        assertSerializedValues(
                AttributeTypedValue.of(new String[] { "hello", "hi" }, STRING, String.class),
                new String[] { "hello", "hi" }
        );
        assertSerializedValues(
                AttributeTypedValue.of(new Long[] { 36000L, 39600L }, INT, LocalTime.class),
                new LocalTime[] {
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                }
        );
        assertSerializedValues(
                AttributeTypedValue.of(new PGIntRangeArray(new PGIntRange(100L, 155L), new PGIntRange(22L, 31L)), INT_RANGE, NumberRange.class),
                new NumberRange[] {
                        NumberRange.between(new BigDecimal("10"), new BigDecimal("15.5"), 1),
                        NumberRange.between(new BigDecimal("2.2"), new BigDecimal("3.1"), 1)
                }
        );
    }


    private void assertSerializedValue(AttributeTypedValue expected, Serializable value) {
        assertEquals(expected, serializer.serialize(value, schema));
    }

    private void assertSerializedValues(AttributeTypedValue expected, Serializable[] values) {
        final AttributeTypedValue actual = serializer.serialize(values, schema);
        assertEquals(expected.getSerializedTargetType(), actual.getSerializedTargetType());
        assertEquals(expected.getType(), actual.getType());

        final Class<?> type = expected.getType();
        if (String.class.isAssignableFrom(type)) {
            assertArrayEquals((String[]) expected.getSerializedValue(), (String[]) actual.getSerializedValue());
        } else if (LocalTime.class.isAssignableFrom(type)) {
            assertArrayEquals((Long[]) expected.getSerializedValue(), (Long[]) actual.getSerializedValue());
        } else if (NumberRange.class.isAssignableFrom(type)) {
            assertEquals(((PGIntRangeArray) expected.getSerializedValue()).getValue(), ((PGIntRangeArray) actual.getSerializedValue()).getValue());
        } else {
            throw new RuntimeException("Unexpected serialized type.");
        }
    }
}