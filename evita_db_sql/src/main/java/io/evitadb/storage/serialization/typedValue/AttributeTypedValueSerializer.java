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
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.serialization.sql.PGIntRange;
import io.evitadb.storage.serialization.sql.PGIntRangeArray;
import org.postgresql.util.PGobject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.*;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.Assert.isTrue;

/**
 * <p>Serializer for better querying of {@link io.evitadb.api.data.AttributesContract.AttributeValue}.</p>
 *
 * <p>The generic value have to serialized to db specific data type (and column) which is appropriate for querying.</p>
 *
 * <p>It supports only officially supported data types (and arrays of them) by Evita ({@link EvitaDataTypes}) which
 * makes sense in context of attribute values</p>
 *
 * <p><b>Note: </b> {@link EnumWrapper} have to be resolved before serialization and arrays of values have to be handled
 * outside of this serializer.</p>
 *
 * @author Lukáš Hornych 2021
 * @author Jiří Bonch, 2021
 * @author Tomáš Pozler, 2021
 */
public class AttributeTypedValueSerializer {

    private static final AttributeTypedValueSerializer INSTANCE = new AttributeTypedValueSerializer();

    private final MultipleStringTypedValueSerializer multipleStringTypedValueSerializer = MultipleStringTypedValueSerializer.getInstance();

    private AttributeTypedValueSerializer() {}

    public static AttributeTypedValueSerializer getInstance() {
        return INSTANCE;
    }


    /**
     * Serialize array of original values to queryable array of values
     *
     * @param schema schema of original values
     * @param values original values
     * @return queryable values
     */
    public <V extends Serializable, SV extends Serializable> AttributeTypedValue serialize(@Nonnull V[] values, @Nonnull AttributeSchema schema) {
        Assert.notNull(values, "Array of attribute values cannot be null.");

        final Class<?> componentType = values.length > 0 ? values[0].getClass() : String.class;
        isTrue(
                EvitaDataTypes.isSupportedType(componentType) && !componentType.equals(EnumWrapper.class),
                "Value type \"" + componentType.getName() + "\" is not supported in current serializing context."
        );

        final AttributeTypedValue.TargetType targetType = AttributeTypedValue.TargetType.from(componentType);

        //noinspection unchecked
        final SV[] serializedValues = Arrays.stream(values)
                .map(value -> (SV) serializeValue(value, schema))
                .toArray(length -> (SV[]) Array.newInstance(targetType.getClazz(), length));

        if (targetType.equals(AttributeTypedValue.TargetType.INT_RANGE)) {
            return AttributeTypedValue.of(new PGIntRangeArray((PGobject[]) serializedValues), targetType, componentType);
        }
        return AttributeTypedValue.of(serializedValues, targetType, componentType);
    }

    /**
     * Serialize single original value to queryable value
     *
     * @param value original value
     * @param schema schema of original value
     * @return queryable value
     */
    public AttributeTypedValue serialize(@Nullable Serializable value, @Nonnull AttributeSchema schema) {
        if (value == null) {
            return AttributeTypedValue.nullValue();
        }

        final Class<?> type = value.getClass();
        isTrue(
                EvitaDataTypes.isSupportedType(type) && !type.equals(EnumWrapper.class),
                "Value type \"" + type.getName() + "\" is not supported in current serializing context."
        );

        final AttributeTypedValue.TargetType targetType = AttributeTypedValue.TargetType.from(type);
        return AttributeTypedValue.of(serializeValue(value, schema), targetType, type);
    }

    private Serializable serializeValue(@Nullable Serializable value, @Nonnull AttributeSchema schema) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return value.toString();
        } else if (value instanceof Character) {
            return value.toString();
        } else if (value instanceof Byte) {
            return (long) (byte) value;
        } else if (value instanceof Short) {
            return (long) (short) value;
        } else if (value instanceof Integer) {
            return (long) (int) value;
        } else if (value instanceof Long) {
            return value;
        } else if (value instanceof Boolean) {
            return ((boolean) value) ? 1L : 0L;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).scaleByPowerOfTen(schema.getIndexedDecimalPlaces()).longValue();
        } else if (value instanceof DateTimeRange) {
            final DateTimeRange dateTimeRange = (DateTimeRange) value;
            return new PGIntRange(dateTimeRange.getFrom(), dateTimeRange.getTo());
        } else if (value instanceof NumberRange) {
            final NumberRange numberRange = (NumberRange) value;
            return new PGIntRange(numberRange.getFrom(), numberRange.getTo());
        } else if (value instanceof Multiple) {
            return Arrays.stream(((Multiple) value).getValues())
                    .map(v -> multipleStringTypedValueSerializer.serialize(v, schema))
                    .collect(Collectors.joining(";"));
        } else if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toEpochSecond();
        } else if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC);
        } else if (value instanceof LocalDate) {
            return (LocalDateTime.of((LocalDate) value, LocalTime.of(0, 0))).toEpochSecond(ZoneOffset.UTC);
        } else if (value instanceof LocalTime) {
            return (LocalDateTime.of(LocalDate.of(1970, 1, 1), (LocalTime) value)).toEpochSecond(ZoneOffset.UTC);
        } else if (value instanceof Locale) {
            return value.toString();
        } else if (value instanceof Enum) {
            return value.toString();
        } else if (value instanceof Currency) {
            return value.toString();
        }

        throw new IllegalArgumentException("Class \"" + value.getClass()
                .getName() + "\" is currently not supported by this Evita implementation.");
    }
}

