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

package io.evitadb.storage.query.filter.price;

import io.evitadb.api.query.filter.PriceValidIn;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;

import java.time.ZonedDateTime;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class PriceValidInTranslator extends AbstractPriceTranslator<PriceValidIn> {

    @SneakyThrows
    @Override
    public EsConstraint applyInternally(PriceValidIn constraint, FilterByVisitor filterByVisitor) {
        ZonedDateTime theMoment = constraint.getTheMoment();
        if (theMoment == null) return new EsConstraint(boolQuery());

        long theMomentEpochSec = theMoment.toEpochSecond();
        return new EsConstraint(
                boolQuery()
                        .should(
                                boolQuery()
                                        .filter(
                                                nestedQuery("prices.validity",
                                                        boolQuery()
                                                                .must(
                                                                        rangeQuery("prices.validity.lte")
                                                                                .gte(theMomentEpochSec)
                                                                                .lte(null))
                                                                .must(
                                                                        rangeQuery("prices.validity.gte")
                                                                                .gte(null)
                                                                                .lte(theMomentEpochSec)
                                                                ),
                                                        ScoreMode.Max
                                                )
                                        )
                        )
                        .should(
                                boolQuery()
                                        .must(
                                                matchQuery(
                                                        "prices.hasValidity",
                                                        false
                                                )
                                        )
                        ).minimumShouldMatch(1));
    }

}
