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

package io.evitadb.storage.query.builder;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Sql builder for creating select column list for fetching whole entity
 *
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class EntityColumnListBuilder {

    private final String alias;
    private final List<String> columns = new ArrayList<>(9);

    public EntityColumnListBuilder serializedEntity() {
        return serializedEntity(true);
    }

    public EntityColumnListBuilder serializedEntity(boolean condition) {
        return addColumn(condition, alias + ".serializedEntity");
    }

    public EntityColumnListBuilder entityId() {
        return entityId(true);
    }

    public EntityColumnListBuilder entityId(boolean condition) {
        return addColumn(condition, alias + ".entity_id");
    }

    public EntityColumnListBuilder primaryKey() {
        return primaryKey(true);
    }

    public EntityColumnListBuilder primaryKey(boolean condition) {
        return addColumn(condition, alias + ".primaryKey");
    }

    public EntityColumnListBuilder type() {
        return type(true);
    }

    public EntityColumnListBuilder type(boolean condition) {
        return addColumn(condition, alias + ".type");
    }

    public EntityColumnListBuilder parentPrimaryKey() {
        return parentPrimaryKey(true);
    }

    public EntityColumnListBuilder parentPrimaryKey(boolean condition) {
        return addColumn(condition, alias + ".parentPrimaryKey");
    }

    public EntityColumnListBuilder hierarchyPlacement() {
        return hierarchyPlacement(true);
    }

    public EntityColumnListBuilder hierarchyPlacement(boolean condition) {
        addColumn(condition, alias + ".leftBound");
        addColumn(condition, alias + ".rightBound");
        return addColumn(condition, alias + ".level");
    }

    public EntityColumnListBuilder orderAmongSiblings() {
        return orderAmongSiblings(true);
    }

    public EntityColumnListBuilder orderAmongSiblings(boolean condition) {
        return addColumn(condition, alias + ".orderAmongSiblings");
    }

    public EntityColumnListBuilder facets() {
        return facets(true);
    }

    public EntityColumnListBuilder facets(boolean condition) {
        return addColumn(condition, "coalesce(facets, '{}'::jsonb) as facets");
    }

    public EntityColumnListBuilder prices() {
        return prices(true);
    }

    public EntityColumnListBuilder prices(boolean condition) {
        return addColumn(condition, "prices");
    }

    public EntityColumnListBuilder count() {
        return count(true);
    }

    public EntityColumnListBuilder count(boolean condition) {
        return addColumn(condition, "count(*) over() as totalRecordCount");
    }

    public EntityColumnListBuilder all() {
        return all(true);
    }

    public EntityColumnListBuilder all(boolean condition) {
        return addColumn(condition, alias + ".*");
    }

    public String build() {
        final String columnsList = String.join(",", columns);
        columns.clear();
        return columnsList;
    }

    private EntityColumnListBuilder addColumn(boolean condition, String columnExpression) {
        if (condition) {
            columns.add(columnExpression);
        }
        return this;
    }
}
