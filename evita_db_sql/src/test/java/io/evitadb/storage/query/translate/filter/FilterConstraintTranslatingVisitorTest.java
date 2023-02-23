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

package io.evitadb.storage.query.translate.filter;

import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.storage.CatalogContext;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlWithClause;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FilterConstraintTranslatingVisitor}
 *
 * @author Lukáš Hornych 2022
 */
class FilterConstraintTranslatingVisitorTest {

    @Test
    void shouldTranslateBasicFilterConstraint() {
        final FilterBy c = filterBy(
                and(
                    eq("a", 1),
                    isTrue("b")
                )
        );
        final FilterConstraintTranslatingVisitor visitor = createVisitor(c);
        visitor.visit(c);

        final SqlWithClause with = visitor.getFinalSqlWithClause();
        assertEquals(0, with.getWithJoins().length());
        assertEquals(
                "with cte_1 as (" +
                        "select entity_id " +
                        "from c_1.t_attributeIndex attribute " +
                        "where attribute.reference_id is null" +
                        "   and attribute.name = ?" +
                        "   and attribute.locale is null" +
                        "   and ? = any ((attribute.intValues)::bigint[]) " +
                        "), " +
                        "cte_2 as (" +
                        "select entity_id " +
                        "from c_1.t_attributeIndex attribute " +
                        "where attribute.reference_id is null" +
                        "   and attribute.name = ?" +
                        "   and attribute.locale is null" +
                        "   and  attribute.intValues[1] = 1" +
                        ")",
                with.getWith().getSql().toString()
        );
        assertEquals(
                List.of("a", 1L, "b"),
                with.getWith().getArgs()
        );

        final SqlPart where = visitor.getFinalWhereSqlPart();
        assertEquals(
                "(entity.entity_id = any (select entity_id from cte_1) and entity.entity_id = any (select entity_id from cte_2))",
                where.getSql().toString()
        );
        assertEquals(List.of(), where.getArgs());
    }

    @Test
    void shouldTranslateReferenceFilterConstraint() {
        final FilterBy c = filterBy(
                referenceHavingAttribute(
                    "category",
                    and(
                        eq("c", 1),
                        isTrue("d")
                    )
                )
        );
        final FilterConstraintTranslatingVisitor visitor = createVisitor(c);
        visitor.visit(c);

        final SqlWithClause with = visitor.getFinalSqlWithClause();
        assertEquals(0, with.getWithJoins().length());
        assertEquals(
                "with cte_1 as (" +
                        "select reference.entity_id " +
                        "from c_1.t_referenceIndex reference" +
                        "   where reference.entityType = ? and " +
                        "(? = any ((select attribute.intValues" +
                        " from c_1.t_attributeIndex attribute " +
                        "where attribute.reference_id = reference.reference_id" +
                        "   and attribute.locale is null" +
                        "   and attribute.name = ? " +
                        " limit 1)::bigint[]) " +
                        " and  " +
                        "(select attribute.intValues[1]" +
                        " from c_1.t_attributeIndex attribute " +
                        "where attribute.reference_id = reference.reference_id" +
                        "   and attribute.locale is null" +
                        "   and attribute.name = ? " +
                        " limit 1) = 1)" +
                        ")",
                with.getWith().getSql().toString()
        );
        assertEquals(
                List.of("category", 1L, "c", "d"),
                with.getWith().getArgs()
        );

        final SqlPart where = visitor.getFinalWhereSqlPart();
        assertEquals(
                "entity.entity_id = any (select entity_id from cte_1)",
                where.getSql().toString()
        );
        assertEquals(List.of(), where.getArgs());
    }


    private FilterConstraintTranslatingVisitor createVisitor(@Nonnull FilterBy filterBy) {
        return FilterConstraintTranslatingVisitor.withDefaultMode(
                new EntityCollectionContext(
                        mock(CatalogContext.class),
                        "c_1",
                        "product",
                        new AtomicReference<>(
                                new EntitySchemaBuilder(new EntitySchema("product"), schema -> schema)
                                        .withAttribute("a", Integer.class, whichIs -> whichIs.filterable())
                                        .withAttribute("b", Boolean.class, whichIs -> whichIs.filterable())
                                        .withReferenceTo("category", whichIs -> whichIs
                                                .withAttribute("c", Integer.class, thatIs -> thatIs.filterable())
                                                .withAttribute("d", Boolean.class, thatIs -> thatIs.filterable()))
                                        .applyChanges()
                        ),
                        new AtomicBoolean(true),
                        null
                ),
                new SqlEvitaRequest(
                        Query.query(
                                entities("product"),
                                filterBy
                        ),
                        ZonedDateTime.now()
                )
        );
    }
}