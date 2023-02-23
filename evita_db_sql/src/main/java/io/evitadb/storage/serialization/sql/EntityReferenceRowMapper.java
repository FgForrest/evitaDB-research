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

import io.evitadb.api.data.structure.EntityReference;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Retrieves only reference to entity instead of fetching whole entity.
 *
 * @see EntityRowMapper
 * @author Lukáš Hornych 2022
 */
@RequiredArgsConstructor
public class EntityReferenceRowMapper implements RowMapper<EntityReference> {

    private final Serializable entityType;

    @Override
    public EntityReference mapRow(ResultSet rs, int rowNum) throws SQLException {
        final int primaryKey = rs.getInt("primaryKey");
        return new EntityReference(entityType, primaryKey);
    }
}
