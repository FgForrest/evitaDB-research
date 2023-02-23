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

import io.evitadb.storage.EntityCollectionDescriptor;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Extractor for {@link EntityCollectionDescriptor}
 *
 * @author Lukáš Hornych 2022
 */
public class EntityCollectionDescriptorRowMapper implements RowMapper<EntityCollectionDescriptor> {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    @Override
    public EntityCollectionDescriptor mapRow(ResultSet rs, int rowNum) throws SQLException {
        final TypedValue<String> serializedEntityType = TypedValue.of(
                rs.getString("name"),
                rs.getString("nameType")
        );
        return new EntityCollectionDescriptor(
                STRING_TYPED_VALUE_SERIALIZER.deserialize(serializedEntityType),
                rs.getString("uid"),
                rs.getBytes("serializationHeader")
        );
    }
}
