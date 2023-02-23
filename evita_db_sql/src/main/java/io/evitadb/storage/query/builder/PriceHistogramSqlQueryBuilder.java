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

import io.evitadb.storage.ComplexEntityQueryContextHolder;
import io.evitadb.storage.query.SqlPart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Sql query builder for querying price histograms
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PriceHistogramSqlQueryBuilder {

    public static SqlPart buildPriceHistogramQuery(@Nonnull SqlPart userFilter, int buckets) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append(// find only relevant prices (according to user filter)
                        "with relevantPrice as (" +
                        "    select entity.prices[1] as minPrice" +
                        "    from ").append(ComplexEntityQueryContextHolder.getContext().getTempTableName()).append(" entity")
                .append("    where ").append(userFilter.getSql())
                .append(// find out metadata of relevant prices
                        "), priceMetadata as (" +
                        "    select min(price.minPrice) as min," +
                        "           max(price.minPrice) as max" +
                        "    from relevantPrice price" +
                        ")" +
                        // compute buckets
                        "select metadata.min    as histogramMin," +
                        "       metadata.max    as histogramMax," +
                        "       width_bucket(" +
                        "           price.minPrice, " +
                        "           metadata.min, " +
                        "           case " +
                        "               when metadata.max = metadata.min then metadata.max + 1 " + // precaution when min and max is same
                        "               else metadata.max " +
                        "           end, " +
                        "           ?" +
                        "       )               as bucketIndex," +
                        "       count(*)        as bucketOccurrences " +
                        "from relevantPrice price, priceMetadata metadata " +
                        "group by histogramMin, histogramMax, bucketIndex " +
                        "order by histogramMin, histogramMax, bucketIndex;");

        final List<Object> args = new LinkedList<>(userFilter.getArgs());
        args.add(buckets);

        return new SqlPart(sqlBuilder, args);
    }
}
