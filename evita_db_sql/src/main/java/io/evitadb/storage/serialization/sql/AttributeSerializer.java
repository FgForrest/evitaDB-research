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

package io.evitadb.storage.serialization.sql;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.sql.Types;

/**
 * Serializer to serialize {@link AttributeValue}s for SQL insert and update queries.
 *
 * @author Lukáš Hornych 2021
 */
public class AttributeSerializer {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();
    private static final AttributeTypedValueSerializer ATTRIBUTE_TYPED_VALUE_SERIALIZER = AttributeTypedValueSerializer.getInstance();

    /**
     * Serializes {@link AttributeValue} to {@link org.springframework.jdbc.core.namedparam.SqlParameterSource} ready
     * to be inserted.
     *
     * @param insertBuffer insert buffer for new attributes
     * @param attributeValue attribute value to serialize
     * @param attributeSchema schema of inserting attribute
     * @param entityId internal id of owning entity (in case of entity attribute) or id of entity owning reference (in case of reference attribute)
     * @param referenceId internal id of owning reference (in case of reference attribute)
     * @param reference owning reference (in case of reference attribute)
     */
    public void serializeToInsert(@Nonnull SqlParameterSourceInsertBuffer insertBuffer,
                                  @Nonnull AttributeValue attributeValue,
                                  @Nonnull AttributeSchema attributeSchema,
                                  long entityId,
                                  Long referenceId,
                                  ReferenceContract reference) {
        if (attributeValue.isDropped()) {
            return;
        }
        if (attributeValue.getValue() == null) {
            throw new IllegalArgumentException("Cannot serialize attribute with null value.");
        }

        Assert.isTrue(
                (referenceId != null) == (reference != null),
                "Both reference id and reference entity type have to be present or none."
        );

        final MapSqlParameterSource args = new MapSqlParameterSource();

        args.addValue("entity_id", entityId);

        if (referenceId != null) {
            args.addValue("reference_id", referenceId);
            args.addValue("reference_entityType", STRING_TYPED_VALUE_SERIALIZER.serialize(reference.getReferencedEntity().getType()).getSerializedValue());
        }

        final AttributeKey attributeKey = attributeValue.getKey();
        args.addValue("name", attributeKey.getAttributeName());
        args.addValue("version", attributeValue.getVersion());
        args.addValue("sortable", attributeSchema.isSortable());
        args.addValue("uniq", attributeSchema.isUnique());

        if (attributeValue.getKey().getLocale() != null) {
            args.addValue("locale", attributeValue.getKey().getLocale().toString());
        }

        final Serializable[] values;
        if (attributeValue.getValue().getClass().isArray()) {
            values = (Serializable[]) attributeValue.getValue();
        } else {
            values = new Serializable[] { attributeValue.getValue() };
        }

        final AttributeTypedValue serializedValues = ATTRIBUTE_TYPED_VALUE_SERIALIZER.serialize(values, attributeSchema);
        if (serializedValues.getSerializedTargetType().equals(AttributeTypedValue.TargetType.INT_RANGE)) {
            args.addValue(
                    serializedValues.getSerializedTargetType().getColumn(),
                    serializedValues.getSerializedValue(),
                    Types.OTHER,
                    "int8range[]"
            );
        } else {
            args.addValue(serializedValues.getSerializedTargetType().getColumn(), serializedValues.getSerializedValue());
        }

        insertBuffer.add(args);
    }

    /**
     * Serialize {@link AttributeValue} to {@link SqlParameterSource} ready to be updated.
     *
     * @param attributeValue attribute value to serialize
     * @param attributeSchema schema of passed attribute
     * @param attributeId internal id of attribute
     * @return ready source
     */
    public SqlParameterSource serializeToUpdate(@Nonnull AttributeValue attributeValue,
                                                @Nonnull AttributeSchema attributeSchema,
                                                long attributeId) {
        if (attributeValue.isDropped()) {
            return null;
        }
        if (attributeValue.getValue() == null) {
            throw new IllegalArgumentException("Cannot serialize attribute with null value.");
        }

        final Serializable[] values;
        if (attributeValue.getValue().getClass().isArray()) {
            values = (Serializable[]) attributeValue.getValue();
        } else {
            values = new Serializable[] { attributeValue.getValue() };
        }

        final AttributeTypedValue serializedValues = ATTRIBUTE_TYPED_VALUE_SERIALIZER.serialize(values, attributeSchema);

        final MapSqlParameterSource args = new MapSqlParameterSource();

        // updated data
        args.addValue("version", attributeValue.getVersion());
        args.addValue(AttributeTypedValue.STRINGS_COLUMN, serializedValues.getSerializedTargetType().equals(AttributeTypedValue.TargetType.STRING) ? serializedValues.getSerializedValue() : null);
        args.addValue(AttributeTypedValue.INTS_COLUMN, serializedValues.getSerializedTargetType().equals(AttributeTypedValue.TargetType.INT) ? serializedValues.getSerializedValue() : null);
        args.addValue(AttributeTypedValue.INT_RANGES_COLUMN, serializedValues.getSerializedTargetType().equals(AttributeTypedValue.TargetType.INT_RANGE) ? serializedValues.getSerializedValue() : null, Types.OTHER);

        // select query
        args.addValue("attributeId", attributeId);

        return args;
    }
}
