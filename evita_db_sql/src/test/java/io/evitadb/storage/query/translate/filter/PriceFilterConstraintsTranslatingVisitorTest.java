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
import io.evitadb.storage.query.SqlPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PriceFilterConstraintsTranslatingVisitor}
 *
 * @author Lukáš Hornych 2022
 */
class PriceFilterConstraintsTranslatingVisitorTest {

    private PriceFilterConstraintsTranslatingVisitor visitor;

    @BeforeEach
    void setup() {
        visitor = new PriceFilterConstraintsTranslatingVisitor();
    }

    @Test
    void shouldNotProduceAnySql() {
        assertTrue(translate(filterBy(and(eq("a", 1)))).isEmpty());
        assertTrue(translate(filterBy(and(priceInPriceLists()))).isEmpty());
    }

    @Test
    void shouldProduceSql() {
        final SqlPart sql1 = translate(filterBy(
                and(
                    priceInPriceLists("basic"),
                    priceInCurrency("CZK")
                )
        ));
        assertFalse(sql1.isEmpty());
        assertFalse(sql1.getArgs().isEmpty());

        final SqlPart sql2 = translate(filterBy(
                and(
                    priceInCurrency("CZK"),
                    priceBetween(BigDecimal.valueOf(10L), BigDecimal.valueOf(20L))
                )
        ));
        assertFalse(sql2.isEmpty());
        assertFalse(sql2.getArgs().isEmpty());

        final SqlPart sql3 = translate(filterBy(
                and(
                    priceValidIn(ZonedDateTime.now()),
                    priceInCurrency("CZK")
                )
        ));
        assertFalse(sql3.isEmpty());
        assertFalse(sql3.getArgs().isEmpty());

        final SqlPart sql4 = translate(filterBy(
                and(
                    priceInPriceLists("basic"),
                    priceValidIn(ZonedDateTime.now()),
                    priceInCurrency("CZK"),
                    priceBetween(BigDecimal.valueOf(10), BigDecimal.valueOf(20))
                )
        ));
        assertFalse(sql4.isEmpty());
        assertFalse(sql4.getArgs().isEmpty());
    }

    private SqlPart translate(@Nonnull FilterBy filterBy) {
        return visitor.translate(
                "c_1",
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