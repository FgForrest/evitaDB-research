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

import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.api.io.extraResult.Histogram;
import io.evitadb.api.io.extraResult.HistogramContract.Bucket;
import io.evitadb.api.io.extraResult.PriceHistogram;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Extracts {@link java.sql.ResultSet} of computed price histogram buckets to {@link PriceHistogram}.
 *
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class PriceHistogramExtractor implements ResultSetExtractor<PriceHistogram> {

    @Nonnull
    private final SqlEvitaRequest request;

    @Override
    public PriceHistogram extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (!rs.next()) {
            // if there are no buckets there should be no histogram
            return null;
        }

        final int requestedBuckets = request.getRequiresPriceHistogramBuckets();

        final List<Bucket> currentBuckets = new LinkedList<>();

        final BigDecimal histogramMin = rs.getBigDecimal("histogramMin");
        final BigDecimal histogramMax = rs.getBigDecimal("histogramMax");
        final BigDecimal currentBucketSize = histogramMax
                .subtract(histogramMin)
                .divide(BigDecimal.valueOf(requestedBuckets), 10, RoundingMode.HALF_UP);

        boolean lastBucketExists = false;
        do {
            int bucketIndex = rs.getInt("bucketIndex");
            int bucketOccurrences = rs.getInt("bucketOccurrences");

            if (bucketIndex == requestedBuckets) {
                // increment last bucket by 1 to ensure that even the max value is considered because width_bucket function
                // has right bound always exclusive (therefore max value is always omitted but at the same time there is
                // always max value) which is not behaviour what we want in evita buckets
                bucketOccurrences++;
                lastBucketExists = true;
            } else if (bucketIndex > requestedBuckets) {
                // ignore last unbounded bucket with max value in it if there is real last bucket
                // if there is no real last bucket we need to create mock last bucket with max value in it
                // this is caused by function width_bucket which treats right bound of each bucket as exclusive,
                // even the last one (which should be inclusive for evita purposes) and therefore there is additional
                // bucket with only max value in it
                if (lastBucketExists) {
                    break;
                }
                bucketIndex = requestedBuckets; // mock last bucket
            }

            // add bucket from current row to current list
            final BigDecimal bucketThreshold = histogramMin.add(currentBucketSize.multiply(BigDecimal.valueOf(bucketIndex - 1L))).stripTrailingZeros();
            currentBuckets.add(new Bucket(bucketIndex, bucketThreshold, bucketOccurrences));
        } while (rs.next());

        return new PriceHistogram(
            new Histogram(currentBuckets.toArray(new Bucket[0]), histogramMax)
        );
    }
}
