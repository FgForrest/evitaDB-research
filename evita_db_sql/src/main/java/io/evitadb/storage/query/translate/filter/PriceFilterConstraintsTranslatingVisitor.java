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
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.PriceBetween;
import io.evitadb.api.query.filter.PriceInCurrency;
import io.evitadb.api.query.filter.PriceInPriceLists;
import io.evitadb.api.query.filter.PriceValidIn;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.query.require.UseOfPrice;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.translator.price.PriceBetweenTranslator;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Special query visitor for filtering by prices. It prepares special sql sub-query to be joined to main sql query which
 * joins correct prices to entities that can be later filtered by {@link PriceBetween}.
 *
 * This visitor handles conversion for constraints {@link PriceInPriceLists}, {@link PriceValidIn} and {@link PriceInCurrency}
 * because these constraints cannot be easily translated separately inside {@link FilterConstraintTranslatingVisitor}.
 * They need to be converted together into single sql in specific order therefore the {@link FilterConstraintTranslatingVisitor}
 * with it's translators cannot do the job.
 *
 * @author Lukáš Hornych 2021
 */
public class PriceFilterConstraintsTranslatingVisitor {

    /**
     * Serializer for values which needs to be serialized to string value, such as entity type, for translators.
     */
    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    /**
     * Translates price filter constraints (besides {@link PriceBetween}) to single sql sub-query to be joined to main sql
     * query.
     *
     * @param request to gather price constraints data
     * @return sql sub-query
     */
    public SqlPart translate(@Nonnull String collectionUid, @Nonnull SqlEvitaRequest request) {
        if (!request.isRequiresPrices()) {
            return SqlPart.EMPTY;
        }

        // gather constraint data
        final Serializable[] priceLists = request.getRequiresPriceLists();
        final Currency currency = request.getRequiresCurrency();

        // serialize constraint data
        final String[] serializedPriceLists = Arrays.stream(priceLists)
                .map(priceList -> STRING_TYPED_VALUE_SERIALIZER.serialize(priceList).getSerializedValue())
                .toArray(String[]::new);
        final String serializedCurrency = STRING_TYPED_VALUE_SERIALIZER.serialize(currency).getSerializedValue();
        final Long utcValidIn = Optional.ofNullable(request.getRequiresPriceValidIn())
                .map(ZonedDateTime::toEpochSecond)
                .orElse(null);

        // prepare sql parts
        final String priceColumn = getPriceColumn(request);
        final SqlPart validitySqlPart = getValiditySqlPart(utcValidIn);

        // build sub-query for prices
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("inner join (" +
                        "   select price.entity_id, " +
                        "          price.innerRecordHandling, " +
                        "          case when price.innerRecordHandling = 'NONE'             then array[min(price.price)] " +
                        "               when price.innerRecordHandling = 'FIRST_OCCURRENCE' then array_agg(price.price order by price.price) " +
                        "               when price.innerRecordHandling = 'SUM'              then array[sum(price.price)] " +
                        "           end as prices " +
                        "   from (" +
                        "      select distinct on (price.entity_id, price.innerRecordId) price.entity_id as entity_id, " +
                        "                                                                price.innerRecordHandling as innerRecordHandling, " +
                        "                                                                price.").append(priceColumn).append(" as price ")
                .append("      from ").append(collectionUid).append(".t_priceIndex as price " +
                        "      where price.currency = ? ").append(validitySqlPart.getSql()).append(" and price.priceList = any (?::varchar[]) ")
                .append("      order by price.entity_id, price.innerRecordId, array_position(?::varchar[], price.priceList), price.").append(priceColumn)
                .append("   ) price " +
                        "   group by price.entity_id, price.innerRecordHandling " +
                        ") price on price.entity_id = entity.entity_id");

        // prepare args
        final List<Object> args = new LinkedList<>();
        args.add(serializedCurrency);
        args.addAll(validitySqlPart.getArgs());
        args.add(serializedPriceLists);
        args.add(serializedPriceLists);

        return new SqlPart(sqlBuilder, args);
    }

    /**
     * Returns sql select column with correct price to fetch. This column is later used in {@link PriceBetweenTranslator}
     */
    @Nonnull
    private String getPriceColumn(@Nonnull SqlEvitaRequest request) {
        return Optional.ofNullable(QueryUtils.findRequire(request.getQuery(), UseOfPrice.class))
            .map(UseOfPrice::getQueryPriceMode)
            .or(() -> Optional.of(QueryPriceMode.WITH_VAT))
            .map(priceFilterMode -> {
                if (priceFilterMode.equals(QueryPriceMode.WITHOUT_VAT)) {
                    return "priceWithoutVat";
                } else {
                    return "priceWithVat";
                }
            })
            .orElseThrow();
    }

    /**
     * Creates sql part to compare price validity
     */
    @Nonnull
    private SqlPart getValiditySqlPart(@Nullable Long priceValidIn) {
        if (priceValidIn == null) {
            return SqlPart.EMPTY;
        }
        return new SqlPart(
                "and (price.validity @> ?::bigint or price.validity is null)",
                List.of(priceValidIn)
        );
    }
}
