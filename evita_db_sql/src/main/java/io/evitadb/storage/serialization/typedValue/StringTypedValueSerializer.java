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
import io.evitadb.storage.serialization.LocaleParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.Assert.notNull;

/**
 * <p>Helps to serialize a generic value between a persistence storage and POJOs/primitives which needs to maintain
 * correct data type.
 * Supports all basic
 * {@link io.evitadb.api.dataType.EvitaDataTypes} (without arrays) and serializes them into a string representation.</p>
 *
 * <p><b>Note:</b> also {@link EnumWrapper} have to be resolved before serialization.</p>
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
public class StringTypedValueSerializer {

    private static final StringTypedValueSerializer INSTANCE = new StringTypedValueSerializer();

    private StringTypedValueSerializer() {}

    public static StringTypedValueSerializer getInstance() {
        return INSTANCE;
    }


    public TypedValue<String> serialize(@Nullable Serializable value) {
        if (value != null) {
            final Class<?> type = value.getClass();
            isTrue(
                    isTypeSupported(type),
                    "Value type \"" + value.getClass().getName() + "\" is not supported in current serializing context."
            );
        }

        if (value == null) {
            return TypedValue.nullValue();
        } else if (value instanceof String) {
            return TypedValue.of((String) value, value.getClass());
        } else if (value instanceof Character) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Byte) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Short) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Integer) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Long) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Boolean) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof DateTimeRange) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof BigDecimal) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof NumberRange) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Multiple) {
            final List<String> serializedMultipleValues = new LinkedList<>();
            for (Serializable multipleValue : ((Multiple) value).getValues()) {

                final TypedValue<String> serializedTypedValue = serialize(multipleValue);
                final StringBuilder serializedMultipleValueBuilder = new StringBuilder();
                if (serializedTypedValue.getType().equals(String.class)) {
                    serializedMultipleValueBuilder
                            .append("\"")
                            .append(serializedTypedValue.getSerializedValue())
                            .append("\"");
                } else {
                    serializedMultipleValueBuilder.append(serializedTypedValue.getSerializedValue());
                }
                serializedMultipleValueBuilder
                        .append(",")
                        .append(serializedTypedValue.getSerializedType());

                serializedMultipleValues.add(serializedMultipleValueBuilder.toString());
            }

            return TypedValue.of(String.join(";", serializedMultipleValues), value.getClass());
        } else if (value instanceof ZonedDateTime) {
            return TypedValue.of(DateTimeFormatter.ISO_ZONED_DATE_TIME.format((TemporalAccessor) value), value.getClass());
        } else if (value instanceof LocalDateTime) {
            return TypedValue.of(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format((TemporalAccessor) value), value.getClass());
        } else if (value instanceof LocalDate) {
            return TypedValue.of(DateTimeFormatter.ISO_LOCAL_DATE.format((TemporalAccessor) value), value.getClass());
        } else if (value instanceof LocalTime) {
            return TypedValue.of(DateTimeFormatter.ISO_LOCAL_TIME.format((TemporalAccessor) value), value.getClass());
        } else if (value instanceof Locale) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Enum) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof EnumWrapper) {
            return TypedValue.of(value.toString(), value.getClass());
        } else if (value instanceof Currency) {
            return TypedValue.of(value.toString(), value.getClass());
        }

        throw new IllegalArgumentException("Class \"" + value.getClass().getName() + "\" is currently not supported by this Evita implementation.");
    }


    @SuppressWarnings("unchecked")
    public <T extends Serializable> T deserialize(@Nonnull TypedValue<String> typedValue) {
        notNull(typedValue, "Cannot deserialize null typed value.");
        if (typedValue.getType() != null) {
            isTrue(
                    isTypeSupported(typedValue.getType()),
                    "Value type \"" + typedValue.getType() + "\" is not supported in current (de)serializing context."
            );
        }

        if (typedValue.getType() == null) {
            return null;
        } else if (typedValue.getType().equals(String.class)) {
            return (T) typedValue.getSerializedValue();
        } else if (typedValue.getType().equals(Byte.class)) {
            return (T) Byte.valueOf(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(Short.class)) {
            return (T) Short.valueOf(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(Integer.class)) {
            return (T) Integer.valueOf(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(Long.class)) {
            return (T) Long.valueOf(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(Boolean.class)) {
            return (T) Boolean.valueOf(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(Character.class)) {
            return (T) Character.valueOf(typedValue.getSerializedValue().charAt(0));
        } else if (typedValue.getType().equals(BigDecimal.class)) {
            return (T) new BigDecimal(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(ZonedDateTime.class)) {
            return (T) ZonedDateTime.parse(typedValue.getSerializedValue(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } else if (typedValue.getType().equals(LocalDateTime.class)) {
            return (T) LocalDateTime.parse(typedValue.getSerializedValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (typedValue.getType().equals(LocalDate.class)) {
            return (T) LocalDate.parse(typedValue.getSerializedValue(), DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (typedValue.getType().equals(LocalTime.class)) {
            return (T) LocalTime.parse(typedValue.getSerializedValue(), DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (typedValue.getType().equals(DateTimeRange.class)) {
            return (T) DateTimeRange.fromString(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(NumberRange.class)) {
            return (T) NumberRange.fromString(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(Multiple.class)) {
            throw new IllegalArgumentException("Deserialization of multiple typed is not currently supported.");
        } else if (typedValue.getType().equals(Locale.class)) {
            return (T) LocaleParser.parse(typedValue.getSerializedValue());
        } else if (typedValue.getType().equals(EnumWrapper.class)) {
            return (T) EnumWrapper.fromString(typedValue.getSerializedValue());
        } else if (typedValue.getType().isEnum()) {
            return (T) Arrays.stream(typedValue.getType().getEnumConstants())
                    .filter(ec -> ec.toString().equals(typedValue.getSerializedValue()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not find enum value for value \"" + typedValue
                            .getSerializedValue() + "\" in enum \"" + typedValue.getType().getName() + "\"."));
        } else if (typedValue.getType().equals(Currency.class)) {
            return (T) Currency.getInstance(typedValue.getSerializedValue());
        }

        throw new IllegalArgumentException("Class \"" + typedValue.getType().getName() + "\" is currently not supported by this Evita implementation.");
    }

    private boolean isTypeSupported(Class<?> type) {
        return EvitaDataTypes.isSupportedType(type) && !type.equals(EnumWrapper.class);
    }
}
