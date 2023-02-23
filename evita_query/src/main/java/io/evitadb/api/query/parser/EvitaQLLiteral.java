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

package io.evitadb.api.query.parser;

import io.evitadb.api.dataType.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Represents EvitaQL literal. It is wrapper for parsed values of supported data types. All parsed values must be at least
 * of type {@link Serializable}.
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 * @see EvitaDataTypes
 */
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
@ToString
public class EvitaQLLiteral {

    /**
     * Concrete value of parsed literal in target data type.
     */
    private final Serializable value;

    /**
     * Target data type of parsed value
     */
    @Getter
    private final Class<?> type;

    public EvitaQLLiteral(Serializable value) {
        this.value = value;
        this.type = value.getClass();
    }

    public <T extends Serializable> T getValue() {
        //noinspection unchecked
        return (T) value;
    }

    public <T extends Serializable & Comparable<?>> T asSerializableAndComparable() {
        //noinspection unchecked
        return (T) value;
    }

    public Comparable<?> asComparable() {
        return (Comparable<?>) value;
    }

    public <T extends Comparable<? super T> & Serializable> T asComparableAndSerializable() {
        //noinspection unchecked
        return (T) value;
    }

    public String asString() {
        return (String) value;
    }

    public Number asNumber() {
        return (Number) value;
    }

    public Long asLong() {
        return (Long) value;
    }

    public Boolean asBoolean() {
        return (Boolean) value;
    }

    public BigDecimal asBigDecimal() {
        return (BigDecimal) value;
    }

    public ZonedDateTime asZonedDateTime() {
        return (ZonedDateTime) value;
    }

    public LocalDateTime asLocalDateTime() {
        return (LocalDateTime) value;
    }

    public LocalDate asLocalDate() {
        return (LocalDate) value;
    }

    public LocalTime asLocalTime() {
        return (LocalTime) value;
    }

    public DateTimeRange asDateTimeRange() {
        return (DateTimeRange) value;
    }

    public NumberRange asNumberRange() {
        return (NumberRange) value;
    }

    public EnumWrapper asEnumWrapper() {
        return (EnumWrapper) value;
    }

    public Locale asLocale() {
        return (Locale) value;
    }

    public Multiple asMultiple() {
        return (Multiple) value;
    }
}
