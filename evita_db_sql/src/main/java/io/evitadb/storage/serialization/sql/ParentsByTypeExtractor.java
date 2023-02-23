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

import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.extraResult.Parents.ParentsByType;
import io.evitadb.storage.EntityCollectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Extracts {@link ResultSet} of found entity parents to map of {@link ParentsByType} associated with that type.
 *
 * @param <T> this class represents parent entity
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class ParentsByTypeExtractor<T extends Serializable> implements ResultSetExtractor<Map<Serializable, ParentsByType<?>>> {

    @Nonnull private final EntityCollectionContext collectionCtx;
    @Nonnull private final Class<T> parentTargetClass;

    @Override
    @Nonnull
    public Map<Serializable, ParentsByType<?>> extractData(ResultSet rs) throws SQLException, DataAccessException {
        // no cached entities present
        if (!rs.next()) {
            return new HashMap<>();
        }

        final Map<Serializable, EntityRowMapper> parentsEntityRowMappers = new HashMap<>();

        final Map<Serializable, ParentsByType<?>> parentsByType = new HashMap<>();
        Serializable currentParentsType = null;

        Map<Integer, Map<Integer, T[]>> parentsByEntity = new HashMap<>();

        Integer currentEntityPK = null;
        Integer currentReferencedEntityPK = null;

        List<T> currentParents = new LinkedList<>();
        do {
            final Serializable parentsType = collectionCtx.getCatalogCtx().getOriginalEntityType(rs.getString("parentsType"));
            final int entityPK = rs.getInt("entityPrimaryKey");
            final int referencedEntityPK = rs.getInt("referencedEntityPrimaryKey");

            // we are at the beginning of result set, we need to initialize everything
            if (currentEntityPK == null) {
                currentParentsType = parentsType;

                currentEntityPK = entityPK;
                currentReferencedEntityPK = referencedEntityPK;

                parentsByEntity.put(currentEntityPK, new HashMap<>());

            // parents type changed
            } else {
                // if any change, flush current parents
                if (!currentParentsType.equals(parentsType) || !currentEntityPK.equals(entityPK) || !currentReferencedEntityPK.equals(referencedEntityPK)) {
                    // flush only current referenced entity
                    //noinspection unchecked
                    parentsByEntity.get(currentEntityPK).put(currentReferencedEntityPK, currentParents.toArray(value -> (T[]) Array.newInstance(parentTargetClass, 0)));
                    currentParents = new LinkedList<>();
                    currentReferencedEntityPK = referencedEntityPK;
                }

                // parents type changed
                if (!currentParentsType.equals(parentsType)) {
                    // flush all parents for current type
                    parentsByType.put(currentParentsType, new ParentsByType<>(currentParentsType, parentsByEntity));

                    // reset currents for new parents type
                    currentParentsType = parentsType;
                    currentEntityPK = entityPK;
                    parentsByEntity = new HashMap<>();
                    parentsByEntity.put(currentEntityPK, new HashMap<>());

                // entity changed
                } else if (!currentEntityPK.equals(entityPK)) {
                    // reset currents for new entity
                    currentEntityPK = entityPK;
                    parentsByEntity.put(currentEntityPK, new HashMap<>());
                }
            }

            // extract parent
            currentParents.add(extractParent(rs, parentsEntityRowMappers.computeIfAbsent(parentsType, type -> new EntityRowMapper(collectionCtx.getCatalogCtx().getCatalog().getCollectionForEntity(type).getCtx()))));
        } while (rs.next());
        // flush remaining opened parents
        //noinspection unchecked
        parentsByEntity.get(currentEntityPK).put(currentReferencedEntityPK, currentParents.toArray(value -> (T[]) Array.newInstance(parentTargetClass, 0)));
        // flush remaining opened parents for current parents type
        parentsByType.put(currentParentsType, new ParentsByType<>(currentParentsType, parentsByEntity));

        return Collections.unmodifiableMap(parentsByType);
    }

    /**
     * Extracts parent from current result row
     *
     * @param rs result set
     * @return parent as integer or entity
     */
    private T extractParent(@Nonnull ResultSet rs, @Nonnull EntityRowMapper entityRowMapper) throws SQLException {
        if (parentTargetClass.equals(Integer.class)) {
            //noinspection unchecked
            return (T) (Integer) rs.getInt("primaryKey");
        } else if (SealedEntity.class.isAssignableFrom(parentTargetClass)) {
            //noinspection unchecked
            return (T) entityRowMapper.mapRow(rs, -1);
        } else {
            throw new IllegalArgumentException("Unsupported target parent class.");
        }
    }
}
