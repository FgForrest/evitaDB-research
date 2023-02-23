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

import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.SqlEvitaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Extracts data chuck of entites from result set.
 *
 * @param <E> type of entity projection
 * @author Lukáš Hornych 2022
 */
@RequiredArgsConstructor
public class EntitiesExtractor<E extends Serializable> implements ResultSetExtractor<DataChunk<E>> {

    private final EntityRowMapper entityRowMapper;
    private final EntityReferenceRowMapper entityReferenceRowMapper;
    private final SqlEvitaRequest request;

    @Nonnull
    @Override
    public DataChunk<E> extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (!rs.next()) {
            return request.createDataChunk(0, List.of());
        }

        final int totalRecordCount = rs.getInt("totalRecordCount");

        final List<E> results = new LinkedList<>();
        int rowNum = 0;
        do {
            results.add(getEntityRowMapper(request).mapRow(rs, rowNum++));
        } while (rs.next());

        return request.createDataChunk(totalRecordCount, results);
    }

    /**
     * Returns appropriated entity row mapper by request
     */
    private <M extends RowMapper<E>> M getEntityRowMapper(@Nonnull SqlEvitaRequest request) {
        if (request.isRequiresEntityBody()) {
            //noinspection unchecked
            return (M) entityRowMapper;
        }
        //noinspection unchecked
        return (M) entityReferenceRowMapper;
    }
}
