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

package io.evitadb.storage.query.translate.filter.translator.attribute;

import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.dataType.Range;
import io.evitadb.api.query.filter.Between;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link Between} to generic {@link SqlPart}
 *
 * @author Lukáš Hornych 2021
 */
public class BetweenTranslator extends AttributeFilterConstraintTranslator<Between> {

    @Override
    public SqlPart translate(@Nonnull Between constraint, @Nonnull FilterTranslatingContext ctx) {
        final AttributeSchema attributeSchema = ctx.getAttributeSchema(constraint.getAttributeName());

        if (Range.class.isAssignableFrom(attributeSchema.getPlainType())) {
            // treat between as overlap constraint for ranges

            final Serializable from;
            final Serializable to;
            if (NumberRange.class.isAssignableFrom(attributeSchema.getPlainType())) {
                if (attributeSchema.getIndexedDecimalPlaces() > 0) {
                    from = EvitaDataTypes.toTargetType(constraint.getFrom(), BigDecimal.class);
                    to = EvitaDataTypes.toTargetType(constraint.getTo(), BigDecimal.class);
                } else {
                    from = EvitaDataTypes.toTargetType(constraint.getFrom(), Long.class);
                    to = EvitaDataTypes.toTargetType(constraint.getTo(), Long.class);
                }
            } else if (DateTimeRange.class.isAssignableFrom(attributeSchema.getPlainType())) {
                from = EvitaDataTypes.toTargetType(constraint.getFrom(), ZonedDateTime.class);
                to = EvitaDataTypes.toTargetType(constraint.getTo(), ZonedDateTime.class);
            } else {
                throw new IllegalArgumentException("Unsupported range type for between constraint: " + attributeSchema.getPlainType().getName());
            }

            final AttributeTypedValue serializedFromValue = ctx.getAttributeTypedValueSerializer().serialize(from, attributeSchema);
            final AttributeTypedValue serializedToValue = ctx.getAttributeTypedValueSerializer().serialize(to, attributeSchema);

            return buildValueComparisonSqlPart(
                    ctx,
                    attributeSchema,
                    true,
                    "?::int8range && any",
                    null,
                    new StringBuilder()
                            .append("[")
                            .append(serializedFromValue.getSerializedValue())
                            .append(",")
                            .append(serializedToValue.getSerializedValue())
                            .append("]")
                            .toString()
            );
        } else {
            // treat between as normal between for numbers

            final Serializable from = EvitaDataTypes.toTargetType(constraint.getFrom(), attributeSchema.getPlainType());
            final Serializable to = EvitaDataTypes.toTargetType(constraint.getTo(), attributeSchema.getPlainType());

            final AttributeTypedValue serializedFromValue = ctx.getAttributeTypedValueSerializer().serialize(from, attributeSchema);
            final AttributeTypedValue serializedToValue = ctx.getAttributeTypedValueSerializer().serialize(to, attributeSchema);

            return buildValueComparisonSqlPart(
                    ctx,
                    attributeSchema,
                    false,
                    null,
                    "between ? and ?",
                    serializedFromValue.getSerializedValue(),
                    serializedToValue.getSerializedValue()
            );
        }
    }
}
