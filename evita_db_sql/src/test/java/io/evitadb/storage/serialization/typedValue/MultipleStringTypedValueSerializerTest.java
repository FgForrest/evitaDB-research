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
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.schema.AttributeSchema;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link MultipleStringTypedValueSerializer}.
 *
 * @author Tomáš Pozler, 2021
 */
class MultipleStringTypedValueSerializerTest {

    private final AttributeSchema schema = new AttributeSchema("att", false, true, true, true, Serializable.class, 1);
    private final MultipleStringTypedValueSerializer serializer = MultipleStringTypedValueSerializer.getInstance();

    @Test
    void shouldSerializeValueOfSupportedTypeToExpectedForm() {
        assertSerializedValue("", null);
        assertSerializedValue("\"hello\"", "hello");
        assertSerializedValue("\"c\"", 'c');
        assertSerializedValue("8000000000000000", (byte)0);
        assertSerializedValue("7ffffffffffffff6", (byte)-10);
        assertSerializedValue("800000000000000a", (byte)10);
        assertSerializedValue("8000000000000000", (short)0);
        assertSerializedValue("7ffffffffffffff6", (short)-10);
        assertSerializedValue("800000000000000a", (short)10);
        assertSerializedValue("8000000000000000", 0);
        assertSerializedValue("7ffffffffffffff6", -10);
        assertSerializedValue("800000000000000a", 10);
        assertSerializedValue("0000000000000000", Long.MIN_VALUE);
        assertSerializedValue("ffffffffffffffff", Long.MAX_VALUE);
        assertSerializedValue("8000000000000000", 0L);
        assertSerializedValue("7ffffffffffffff6", -10L);
        assertSerializedValue("800000000000000a", 10L);
        assertSerializedValue("true", true);
        final ZonedDateTime dt1 = ZonedDateTime.ofInstant(LocalDateTime.of(2020, 5, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague"));
        final ZonedDateTime dt2 = ZonedDateTime.ofInstant(LocalDateTime.of(2020, 6, 1, 10, 0), ZoneOffset.of("+02:00"), ZoneId.of("Europe/Prague"));
        final NumberRange nr = NumberRange.between(dt1.toInstant().getEpochSecond(),dt2.toInstant().getEpochSecond());
        assertSerializedValue("[" + nr.getFrom() + "," + nr.getTo() + "]", DateTimeRange.between(dt1, dt2));
        assertSerializedValue("800000000000001f", BigDecimal.valueOf(3.1415926535));
        assertSerializedValue("[-100,100]", NumberRange.between(-100,100));
        assertSerializedValue("[-150,347]", NumberRange.between(BigDecimal.valueOf(-1.5),BigDecimal.valueOf(3.47),2));
        assertSerializedValue("1588320000", dt1);
        assertSerializedValue("1588327200", dt1.toLocalDateTime());
        assertSerializedValue("1588291200", dt1.toLocalDate());
        assertSerializedValue("36000", dt1.toLocalTime());
        assertSerializedValue("cs_CZ", new Locale("cs","CZ"));
        assertSerializedValue("ONE", DummyEnum.ONE);

        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(new DummyPojo("hello"), schema));
    }

    private void assertSerializedValue(Serializable expected, Serializable value) {
        assertEquals(expected, serializer.serialize(value, schema));
    }
}