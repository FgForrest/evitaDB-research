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

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.query.filter.InSet;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link InSet} to generic {@link SqlPart}
 *
 * @author Lukáš Hornych 2021
 */
public class InSetTranslator extends AttributeFilterConstraintTranslator<InSet> {

    @Override
    public SqlPart translate(@Nonnull InSet constraint, @Nonnull FilterTranslatingContext ctx) {
        final AttributeSchema attributeSchema = ctx.getAttributeSchema(constraint.getAttributeName());
        final Serializable[] values = Arrays.stream(constraint.getSet())
                .map(value -> EvitaDataTypes.toTargetType((Serializable) value, attributeSchema.getPlainType()))
                .toArray(Serializable[]::new);

        final AttributeTypedValue.TargetType attributeTargetType = AttributeTypedValue.TargetType.from(attributeSchema.getType());
        final Object serializedValues = ctx.getAttributeTypedValueSerializer().serialize(values, attributeSchema).getSerializedValue();
        return buildValueComparisonSqlPart(
                ctx,
                attributeSchema,
                true,
                null,
                " && (?)::" + attributeTargetType.getSqlType() + "[]",
                serializedValues
        );
    }
}
