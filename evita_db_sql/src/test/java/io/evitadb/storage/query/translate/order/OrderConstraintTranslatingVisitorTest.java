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

package io.evitadb.storage.query.translate.order;

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.storage.CatalogContext;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlSortExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OrderConstraintTranslatingVisitor}
 *
 * @author Lukáš Hornych 2022
 */
class OrderConstraintTranslatingVisitorTest {

    private OrderConstraintTranslatingVisitor visitor;

    @BeforeEach
    void setup() {
        visitor = new OrderConstraintTranslatingVisitor(
                new EntityCollectionContext(
                        mock(CatalogContext.class),
                        "c_1",
                        "product",
                        new AtomicReference<>(
                                new EntitySchemaBuilder(new EntitySchema("product"), schema -> schema)
                                        .withAttribute("a", Integer.class, whichIs -> whichIs.sortable())
                                        .withAttribute("b", String.class, whichIs -> whichIs.sortable())
                                        .withReferenceTo("category", whichIs -> whichIs
                                                .withAttribute("c", Integer.class, thatIs -> thatIs.sortable())
                                                .withAttribute("d", String.class, thatIs -> thatIs.sortable()))
                                        .applyChanges()
                        ),
                        new AtomicBoolean(true),
                        null
                ),
                Locale.ENGLISH
        );
    }

    @Test
    void shouldTranslateBasicOrderConstraints() {
        visitor.visit(orderBy(
                ascending("a"),
                descending("b")
        ));

        final SqlSortExpression sql = visitor.getFinalSqlSortExpression();
        assertEquals(
                "left outer join c_1.t_attributeIndex attribute_0_0" +
                        "   on (attribute_0_0.entity_id = filteredEntity.entity_id and attribute_0_0.reference_id is null" +
                        "       and attribute_0_0.name = ?" +
                        "       and (attribute_0_0.locale is null or attribute_0_0.locale = ?)" +
                        "   ) " +
                        "left outer join c_1.t_attributeIndex attribute_0_1" +
                        "   on (attribute_0_1.entity_id = filteredEntity.entity_id and attribute_0_1.reference_id is null" +
                        "       and attribute_0_1.name = ?" +
                        "       and (attribute_0_1.locale is null or attribute_0_1.locale = ?)" +
                        "   ) ",
                sql.getJoinSql().toString()
        );
        assertEquals(
                List.of("a", "en", "b", "en"),
                sql.getArgs()
        );
        assertEquals(
                "attribute_0_0.intValues asc nulls last, attribute_0_1.stringValues desc nulls last, filteredEntity.primaryKey asc nulls last",
                sql.getSql().toString()
        );
    }

    @Test
    void shouldTranslateReferenceOrderConstraints() {
        visitor.visit(orderBy(
                referenceAttribute(
                    "category",
                    ascending("c"),
                    descending("d")
                )
        ));

        final SqlSortExpression sql = visitor.getFinalSqlSortExpression();
        assertEquals(
                "left outer join c_1.t_attributeIndex attribute_1_0" +
                        "   on (attribute_1_0.entity_id = filteredEntity.entity_id and attribute_1_0.reference_id is not null and attribute_1_0.reference_entityType = ?" +
                        "       and attribute_1_0.name = ?" +
                        "       and (attribute_1_0.locale is null or attribute_1_0.locale = ?)" +
                        "   ) " +
                        "left outer join c_1.t_attributeIndex attribute_1_1" +
                        "   on (attribute_1_1.entity_id = filteredEntity.entity_id and attribute_1_1.reference_id is not null and attribute_1_1.reference_entityType = ?" +
                        "       and attribute_1_1.name = ?" +
                        "       and (attribute_1_1.locale is null or attribute_1_1.locale = ?)" +
                        "   ) ",
                sql.getJoinSql().toString()
        );
        assertEquals(
                List.of("category", "c", "en", "category", "d", "en"),
                sql.getArgs()
        );
        assertEquals(
                "attribute_1_0.intValues asc nulls last, attribute_1_1.stringValues desc nulls last, filteredEntity.primaryKey asc nulls last",
                sql.getSql().toString()
        );
    }
}