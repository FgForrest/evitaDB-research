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
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.TypedValue;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Serializer to serialize {@link PriceContract}s for SQL insert and update queries.
 *
 * @author Lukáš Hornych 2021
 */
public class PriceSerializer {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    /**
     * Serializes {@link PriceContract} to {@link org.springframework.jdbc.core.namedparam.SqlParameterSource} ready
     * to be inserted.
     *
     * @param insertBuffer insert buffer for new prices
     * @param price price to serialize
     * @param priceInnerRecordHandling price inner record handling of prices collection of the price
     * @param entityId internal id of owning entity
     */
    public void serializeToInsert(@Nonnull SqlParameterSourceInsertBuffer insertBuffer,
                                  @Nonnull PriceContract price,
                                  @Nonnull PriceInnerRecordHandling priceInnerRecordHandling,
                                  long entityId) {
        if (price.isDropped() || !price.isSellable()) {
            return;
        }

        final PGobject serializedValidity = Optional.ofNullable(price.getValidity())
                .map(validity -> new PGIntRange(validity.getFrom(), validity.getTo()))
                .orElse(null);
        final TypedValue<String> serializedPriceList = STRING_TYPED_VALUE_SERIALIZER.serialize(price.getPriceList());

        final MapSqlParameterSource args = new MapSqlParameterSource();

        args.addValue("entity_id", entityId);
        args.addValue("primaryKey", price.getPriceId());
        args.addValue("version", price.getVersion());
        args.addValue("currency", STRING_TYPED_VALUE_SERIALIZER.serialize(price.getCurrency()).getSerializedValue());
        args.addValue("priceList", serializedPriceList.getSerializedValue());
        args.addValue("priceListDataType", serializedPriceList.getSerializedType());
        args.addValue("validity", serializedValidity);
        args.addValue("innerRecordHandling", STRING_TYPED_VALUE_SERIALIZER.serialize(priceInnerRecordHandling).getSerializedValue());
        args.addValue("innerRecordId", price.getInnerRecordId());
        args.addValue("priceWithoutVat", price.getPriceWithoutVat());
        args.addValue("priceWithVat", price.getPriceWithVat());

        insertBuffer.add(args);
    }

    /**
     * Serialize {@link PriceContract} to {@link org.springframework.jdbc.core.namedparam.SqlParameterSource} ready to be updated.
     *
     * @param price price to serialize
     * @param priceInnerRecordHandling price inner record handling of prices collection of the price
     * @param priceId internal id of price
     * @return ready source
     */
    public SqlParameterSource serializeToUpdate(@Nonnull PriceContract price,
                                                @Nonnull PriceInnerRecordHandling priceInnerRecordHandling,
                                                long priceId) {
        if (price.isDropped() || !price.isSellable()) {
            return null;
        }

        final PGobject serializedValidity = Optional.ofNullable(price.getValidity())
                .map(validity -> new PGIntRange(validity.getFrom(), validity.getTo()))
                .orElse(null);

        final MapSqlParameterSource args = new MapSqlParameterSource();

        // updated data
        args.addValue("version", price.getVersion());
        args.addValue("validity", serializedValidity);
        args.addValue("innerRecordHandling", STRING_TYPED_VALUE_SERIALIZER.serialize(priceInnerRecordHandling).getSerializedValue());
        args.addValue("innerRecordId", price.getInnerRecordId());
        args.addValue("priceWithoutVat", price.getPriceWithoutVat());
        args.addValue("priceWithVat", price.getPriceWithVat());

        // select query
        args.addValue("priceId", priceId);

        return args;
    }
}
