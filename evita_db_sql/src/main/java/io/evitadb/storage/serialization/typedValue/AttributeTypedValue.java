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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.postgresql.util.PGobject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * <p>Extensions of {@link TypedValue} with additional values for correct value of {@link io.evitadb.api.data.AttributesContract.AttributeValue}
 * serialization where each type is serialized to different column in persistence storage.</p>
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttributeTypedValue extends TypedValue<Serializable> {

    /**
     * Column for string, char, enum etc. values or as fallback column
     */
    public static final String STRINGS_COLUMN = "stringValues";
    /**
     * Column for any integer values
     */
    public static final String INTS_COLUMN = "intValues";
    /**
     * Column for any number range values
     */
    public static final String INT_RANGES_COLUMN = "intRangeValues";

    /**
     * Represents target database column to which the serialized value should be put / belongs to.
     */
    private final TargetType serializedTargetType;

    private AttributeTypedValue(@Nullable Serializable serializedValue,
                                @Nonnull TargetType serializedTargetType,
                                @Nullable Class<?> type) {
        super(serializedValue, type);
        this.serializedTargetType = serializedTargetType;
    }

    public static AttributeTypedValue of(@Nullable Serializable serializedValue,
                                         @Nonnull TargetType serializedTargetType,
                                         @Nullable Class<?> type) {
        return new AttributeTypedValue(serializedValue, serializedTargetType, type);
    }

    public static AttributeTypedValue nullValue() {
        return new AttributeTypedValue(null, TargetType.STRING, null);
    }


    /**
     * Descriptor of target type of attribute value in database
     */
    @Getter
    @RequiredArgsConstructor
    public enum TargetType {
        /**
         * Column for string, char, enum etc. values or as fallback column
         */
        STRING(String.class, STRINGS_COLUMN, "varchar"),
        /**
         * Column for any integer values
         */
        INT(Long.class, INTS_COLUMN, "bigint"),
        /**
         * Column for any range that can be serialized to comparable int range.
         */
        INT_RANGE(PGobject.class, INT_RANGES_COLUMN, "int8range");

        private final Class<?> clazz;
        private final String column;
        private final String sqlType;

        public static TargetType from(Class<?> originalType) {
            if (originalType.isArray()) {
                return from(originalType.getComponentType());
            } else if (String.class.isAssignableFrom(originalType)) {
                return TargetType.STRING;
            } else if (Character.class.isAssignableFrom(originalType)) {
                return TargetType.STRING;
            } else if (Byte.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (Short.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (Integer.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (Long.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (Boolean.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (BigDecimal.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (DateTimeRange.class.isAssignableFrom(originalType)) {
                return TargetType.INT_RANGE;
            } else if (NumberRange.class.isAssignableFrom(originalType)) {
                return TargetType.INT_RANGE;
            } else if (Multiple.class.isAssignableFrom(originalType)) {
                return TargetType.STRING;
            } else if (ZonedDateTime.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (LocalDateTime.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (LocalDate.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (LocalTime.class.isAssignableFrom(originalType)) {
                return TargetType.INT;
            } else if (Locale.class.isAssignableFrom(originalType)) {
                return TargetType.STRING;
            } else if (Enum.class.isAssignableFrom(originalType)) {
                return TargetType.STRING;
            } else if (EnumWrapper.class.isAssignableFrom(originalType)) {
                return TargetType.STRING;
            }

            throw new IllegalArgumentException("Unsupported type " + originalType.getName() + " for attribute value.");
        }
    }
}
