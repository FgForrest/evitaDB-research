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
import io.evitadb.api.io.extraResult.HierarchyStatistics.LevelInfo;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts collection of {@link LevelInfo}s of same collection from {@link ResultSet} as tree of {@link LevelInfo}s.
 *
 * @param <T> this class represents hierarchy entity
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class HierarchyStatisticsExtractor<T extends Serializable> implements ResultSetExtractor<List<LevelInfo<?>>> {

    @Nullable private final EntityRowMapper entityRowMapper;
    @Nonnull private final Class<T> hierarchyEntityTargetClass;

    @Override
    public List<LevelInfo<?>> extractData(ResultSet rs) throws SQLException, DataAccessException {
        // no hierarchy found
        if (!rs.next()) {
            return List.of();
        }

        final List<LevelInfo<?>> statistics = new LinkedList<>();

        int currentLevel = -1;
        final Deque<OrderedLevelInfo<T>> levelInfoStack = new LinkedList<>();
        do {
            final int level = rs.getInt("level");
            final int cardinality = rs.getInt("cardinality");
            final short orderAmongSiblings = rs.getShort("orderAmongSiblings");

            if (level <= currentLevel) {
                // flush lower level infos as well as previous neighbour of current level info
                final int levelDiff = currentLevel - level + 1;
                for (int i = 0; i < levelDiff; i++) {
                    flushCurrentLevelInfoToUpper(statistics, levelInfoStack);
                }
            }

            currentLevel = level;
            // add current hierarchy entity to stack
            levelInfoStack.push(new OrderedLevelInfo<>(extractHierarchyEntity(rs), cardinality, orderAmongSiblings));
        } while (rs.next());

        // flush remaining subtree
        while (!levelInfoStack.isEmpty()) {
            flushCurrentLevelInfoToUpper(statistics, levelInfoStack);
        }

        return Collections.unmodifiableList(statistics);
    }

    /**
     * Flushes current level info to children of parent level info
     *
     * @param statistics final root level statistics
     * @param stack stack of current level infos
     */
    private void flushCurrentLevelInfoToUpper(@Nonnull List<LevelInfo<?>> statistics, @Nonnull Deque<OrderedLevelInfo<T>> stack) {
        final OrderedLevelInfo<T> prevLevelInfo = stack.pop();
        if (stack.isEmpty()) {
            // root level info flush to final statistics collection
            statistics.add(prevLevelInfo.toActualLevelInfo());
        } else {
            stack.peek().getChildren().add(prevLevelInfo);
        }
    }

    /**
     * Extracts hierarchy entity from current result row
     *
     * @param rs result set
     * @return hierarchy entity as integer or entity
     */
    private T extractHierarchyEntity(@Nonnull ResultSet rs) throws SQLException {
        if (hierarchyEntityTargetClass.equals(Integer.class)) {
            //noinspection unchecked
            return (T) (Integer) rs.getInt("primaryKey");
        } else if (SealedEntity.class.isAssignableFrom(hierarchyEntityTargetClass)) {
            //noinspection unchecked
            return (T) entityRowMapper.mapRow(rs, -1);
        } else {
            throw new IllegalArgumentException("Unsupported target hierarchy entity class.");
        }
    }

    /**
     * Ordered temporary version of {@link LevelInfo} to be able to sort infos among siblings.
     */
    @Value
    private static class OrderedLevelInfo<T extends Serializable> {
        T entity;
        int cardinality;
        short order;
        List<OrderedLevelInfo<T>> children = new LinkedList<>();

        public LevelInfo<T> toActualLevelInfo() {
            final List<LevelInfo<T>> actualChildren = children.stream()
                    .sorted(Comparator.comparing(OrderedLevelInfo::getOrder))
                    .map(OrderedLevelInfo::toActualLevelInfo)
                    .collect(Collectors.toUnmodifiableList());
            return new LevelInfo<>(entity, cardinality, actualChildren);
        }
    }
}
