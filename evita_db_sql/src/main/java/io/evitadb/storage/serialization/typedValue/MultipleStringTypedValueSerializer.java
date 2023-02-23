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

import io.evitadb.api.dataType.*;
import io.evitadb.api.schema.AttributeSchema;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;

import static io.evitadb.api.utils.Assert.isTrue;

/**
 * <p>Helps to serialize a generic value between a persistence storage and POJOs/primitives which needs to maintain
 *  * correct data type. Used for serializing inner values of {@link Multiple} into a
 * string representation usable for serialized {@link Multiple}.</p>
 *
 * <p><b>Note:</b> also {@link EnumWrapper} have to be resolved before serialization.</p>
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
public class MultipleStringTypedValueSerializer {

    private static final MultipleStringTypedValueSerializer INSTANCE = new MultipleStringTypedValueSerializer();

    private MultipleStringTypedValueSerializer() {}

    public static MultipleStringTypedValueSerializer getInstance() {
        return INSTANCE;
    }

    public String serialize(@Nullable Serializable value, @Nonnull AttributeSchema schema) {
        if (value != null) {
            final Class<?> type = value.getClass();
            isTrue(
                    EvitaDataTypes.isSupportedType(type) && !type.equals(EnumWrapper.class) && !type.equals(Multiple.class),
                    "Value type \"" + value.getClass().getName() + "\" is not supported in current serializing context."
            );
        }

        if (value == null) {
            return "";
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Character) {
            return serialize(String.valueOf((char) value), schema);
        } else if (value instanceof BigDecimal) {
            return serialize(((BigDecimal) value).scaleByPowerOfTen(schema.getIndexedDecimalPlaces()).longValue(), schema);
        } else if (value instanceof Number) {
            final long unsignedNumber = ((Number) value).longValue() + Long.MIN_VALUE; // create unsigned long

            final StringBuilder hexString = new StringBuilder(Long.toUnsignedString(unsignedNumber,16));
            while (hexString.length() < 16) {
                // pad zeroes
                hexString.insert(0, "0");
            }

            return hexString.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof DateTimeRange) {
            final DateTimeRange dateTimeRange = (DateTimeRange) value;

            final long serializedFrom;
            if (dateTimeRange.getPreciseFrom().toLocalDateTime().equals(LocalDateTime.MIN)) {
                serializedFrom = Long.MIN_VALUE;
            } else {
                serializedFrom = dateTimeRange.getFrom();
            }

            final long serializedTo;
            if (dateTimeRange.getPreciseTo().toLocalDateTime().equals(LocalDateTime.MAX)) {
                serializedTo = Long.MAX_VALUE - 1;
            } else {
                serializedTo = dateTimeRange.getTo();
            }

            return serialize(NumberRange.between(serializedFrom, serializedTo), schema);
        } else if (value instanceof NumberRange) {
            final NumberRange range = (NumberRange) value;
            return "[" + range.getFrom() + "," + range.getTo() + "]";
        } else if (value instanceof ZonedDateTime) {
            return String.valueOf(((ZonedDateTime) value).toEpochSecond());
        } else if (value instanceof LocalDateTime) {
            return String.valueOf(((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC));
        } else if (value instanceof LocalDate) {
            return String.valueOf((LocalDateTime.of((LocalDate) value, LocalTime.of(0, 0))).toEpochSecond(ZoneOffset.UTC));
        } else if (value instanceof LocalTime) {
            return String.valueOf((LocalDateTime.of(LocalDate.of(1970, 1, 1), (LocalTime) value)).toEpochSecond(ZoneOffset.UTC));
        } else if (value instanceof Locale) {
            return value.toString();
        } else if (value instanceof Enum) {
            return value.toString();
        }

        throw new IllegalArgumentException("Class \"" + value.getClass().getName() + "\" is currently not supported by this Evita implementation.");
    }
}
