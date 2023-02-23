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

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PriceSerializer}
 *
 * @author Lukáš Hornych 2022
 */
class PriceSerializerTest {

    private static final String PRICE_LIST_BASIC = "basic";
    private static final BigDecimal PRICE_WITH_VAT = BigDecimal.valueOf(121);
    private static final BigDecimal PRICE_WITHOUT_VAT = BigDecimal.valueOf(100L);
    private static final BigDecimal VAT = BigDecimal.valueOf(21);

    private SqlParameterSourceInsertBuffer insertBuffer;
    private PriceSerializer priceSerializer;

    @BeforeEach
    void setup() {
        insertBuffer = new SqlParameterSourceInsertBuffer();
        priceSerializer = new PriceSerializer();
    }

    @Test
    void shouldSerializePriceToInsert() {
        // ignore dropped price
        insertPrice(null, false, true);
        assertTrue(insertBuffer.isEmpty());

        // ignore non-sellable price
        insertPrice(null, false, false);
        assertTrue(insertBuffer.isEmpty());

        insertPrice(null, true, false);
        assertInsertedPrice(null);

        insertBuffer.reset();

        insertPrice(DateTimeRange.between(LocalDateTime.of(2022, 1, 1, 10, 0), LocalDateTime.of(2022, 2, 1, 10, 0), ZoneId.of("UTC")), true, false);
        assertInsertedPrice(new PGIntRange(1641031200L, 1643709600L));
    }

    @Test
    void shouldSerializePriceToUpdate() {
        // ignore non-sellable value
        assertNull(updatePrice(null, false, false));
        // ignore dropped value
        assertNull(updatePrice(null, true, true));

        final SqlParameterSource price1 = updatePrice(null, true, false);
        assertUpdatedPrice(price1, null);

        final SqlParameterSource price2 = updatePrice(DateTimeRange.between(LocalDateTime.of(2022, 1, 1, 10, 0), LocalDateTime.of(2022, 2, 1, 10, 0), ZoneId.of("UTC")), true, false);
        assertUpdatedPrice(price2, new PGIntRange(1641031200L, 1643709600L));
    }


    private PriceContract createPrice(int version, @Nullable DateTimeRange validity, boolean sellable, boolean dropped) {
        return new Price(
                version,
                new PriceKey(1, PRICE_LIST_BASIC, Currency.getInstance("CZK")),
                1,
                PRICE_WITHOUT_VAT,
                VAT,
                PRICE_WITH_VAT,
                validity,
                sellable,
                dropped
        );
    }

    private void insertPrice(@Nullable DateTimeRange validity, boolean sellable, boolean dropped) {
        priceSerializer.serializeToInsert(
                insertBuffer,
                createPrice(1, validity, sellable, dropped),
                PriceInnerRecordHandling.NONE,
                1
        );
    }

    private SqlParameterSource updatePrice(@Nullable DateTimeRange validity, boolean sellable, boolean dropped) {
        return priceSerializer.serializeToUpdate(
                createPrice(2, validity, sellable, dropped),
                PriceInnerRecordHandling.NONE,
                1
        );
    }

    private void assertInsertedPrice(@Nullable PGIntRange validity) {
        final Map<String, Object> price = ((MapSqlParameterSource) insertBuffer.toArray()[0]).getValues();

        assertEquals(11, price.size());
        assertEquals(1L, price.get("entity_id"));
        assertEquals(1, price.get("primaryKey"));
        assertEquals(1, price.get("version"));
        assertEquals("CZK", price.get("currency"));
        assertEquals(PRICE_LIST_BASIC, price.get("priceList"));
        assertEquals("java.lang.String", price.get("priceListDataType"));
        assertEquals(validity, price.get("validity"));
        assertEquals("NONE", price.get("innerRecordHandling"));
        assertEquals(1, price.get("innerRecordId"));
        assertEquals(BigDecimal.valueOf(100), price.get("priceWithoutVat"));
        assertEquals(BigDecimal.valueOf(121), price.get("priceWithVat"));
    }

    private void assertUpdatedPrice(SqlParameterSource priceSource, @Nullable PGIntRange validity) {
        final Map<String, Object> price = ((MapSqlParameterSource) priceSource).getValues();

        assertEquals(7, price.size());
        assertEquals(1L, price.get("priceId"));
        assertEquals(2, price.get("version"));
        assertEquals(validity, price.get("validity"));
        assertEquals("NONE", price.get("innerRecordHandling"));
        assertEquals(1, price.get("innerRecordId"));
        assertEquals(BigDecimal.valueOf(100), price.get("priceWithoutVat"));
        assertEquals(BigDecimal.valueOf(121), price.get("priceWithVat"));
    }

}