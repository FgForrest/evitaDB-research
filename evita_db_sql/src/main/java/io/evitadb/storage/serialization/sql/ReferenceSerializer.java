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

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;

import static java.util.Optional.ofNullable;

/**
 * Serializer to serialize {@link Reference}s for SQL insert and update queries.
 *
 * @author Lukáš Hornych 2021
 */
public class ReferenceSerializer {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    /**
     * Serializes {@link Reference} to {@link SqlParameterSourceInsertBuffer} ready to be inserted.
     *
     * @param insertBuffer insert buffer for new references
     * @param reference reference to serialize
     * @param referenceId internal id of reference
     * @param entityId internal id of owning entity
     */
    public void serializeToInsert(@Nonnull SqlParameterSourceInsertBuffer insertBuffer,
                                  @Nonnull ReferenceContract reference,
                                  @Nonnull ReferenceSchema referenceSchema,
                                  long referenceId,
                                  long entityId) {
        if (reference.isDropped()) {
            return;
        }
        if (!referenceSchema.isIndexed() && !referenceSchema.isFaceted()) {
            return;
        }

        final MapSqlParameterSource args = new MapSqlParameterSource();

        args.addValue("reference_id", referenceId);
        args.addValue("entity_id", entityId);
        args.addValue("version", reference.getVersion());

        args.addValue("entityPrimaryKey", reference.getReferencedEntity().getPrimaryKey());
        TypedValue<String> typedValue = STRING_TYPED_VALUE_SERIALIZER.serialize(reference.getReferencedEntity().getType());
        args.addValue("entityType", typedValue.getSerializedValue());
        args.addValue("entityTypeDataType", typedValue.getSerializedType());
        args.addValue("faceted", referenceSchema.isFaceted());

        final ReferenceContract.GroupEntityReference group = reference.getGroup();
        if (group != null) {
            final TypedValue<String> serializedGroupType = STRING_TYPED_VALUE_SERIALIZER.serialize(group.getType());
            args.addValue("groupPrimaryKey", group.getPrimaryKey());
            args.addValue("groupType", serializedGroupType.getSerializedValue());
            args.addValue("groupTypeDataType", serializedGroupType.getSerializedType());
        }


        insertBuffer.add(args);
    }

    /**
     * Serialize {@link ReferenceContract} to {@link SqlParameterSource} ready to be updated.
     *
     * @param reference reference to serialize
     * @param referenceSchema schema of updated reference
     * @param referenceId internal id of reference
     * @return ready source
     */
    public SqlParameterSource serializeToUpdate(@Nonnull ReferenceContract reference,
                                                @Nonnull ReferenceSchema referenceSchema,
                                                long referenceId) {
        if (reference.isDropped() || !referenceSchema.isIndexed()) {
            return null;
        }

        final MapSqlParameterSource args = new MapSqlParameterSource();

        // updated data
        args.addValue("version", reference.getVersion());
        args.addValue("groupPrimaryKey", ofNullable(reference.getGroup()).map(EntityReference::getPrimaryKey).orElse(null));
        args.addValue("groupType", ofNullable(reference.getGroup()).map(g -> STRING_TYPED_VALUE_SERIALIZER.serialize(g.getType()).getSerializedValue()).orElse(null));

        // search query
        args.addValue("referenceId", referenceId);

        return args;
    }
}
