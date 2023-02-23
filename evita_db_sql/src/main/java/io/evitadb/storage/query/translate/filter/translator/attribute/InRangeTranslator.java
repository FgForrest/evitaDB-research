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
import io.evitadb.api.query.filter.InRange;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.math.BigDecimal;

import static java.util.Optional.ofNullable;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link InRange} to generic {@link SqlPart}
 *
 * @author Lukáš Hornych 2021
 */
public class InRangeTranslator extends AttributeFilterConstraintTranslator<InRange> {

    @Override
    public SqlPart translate(@Nonnull InRange constraint, @Nonnull FilterTranslatingContext ctx) {
        final AttributeSchema attributeSchema = ctx.getAttributeSchema(constraint.getAttributeName());

        final Serializable targetValue;
        if (NumberRange.class.isAssignableFrom(attributeSchema.getPlainType())) {
            final Number theValue = constraint.getTheValue();
            Assert.notNull(theValue, "Value for number range cannot be null.");
            if (attributeSchema.getIndexedDecimalPlaces() > 0) {
                targetValue = EvitaDataTypes.toTargetType(theValue, BigDecimal.class);
            } else {
                targetValue = EvitaDataTypes.toTargetType(theValue, Long.class);
            }
        } else if (DateTimeRange.class.isAssignableFrom(attributeSchema.getPlainType())) {
            targetValue = ofNullable(constraint.getTheMoment()).orElse(ctx.getNow());
        } else {
            throw new IllegalArgumentException("Unsupported range type for between constraint: " + attributeSchema.getPlainType().getName());
        }
        final AttributeTypedValue serializedValue = ctx.getAttributeTypedValueSerializer().serialize(targetValue, attributeSchema);

        return buildValueComparisonSqlPart(
                ctx,
                attributeSchema,
                true,
                "? <@ any",
                null,
                serializedValue.getSerializedValue()
        );
    }
}
