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

package io.evitadb.storage.query;

import io.evitadb.api.query.Query;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.filter.PriceBetween;
import io.evitadb.api.query.order.PriceAscending;
import io.evitadb.api.query.order.PriceDescending;
import io.evitadb.api.query.require.PriceHistogram;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.storage.query.computation.FOAndGenericStrategyComputingCoder;
import io.evitadb.storage.query.computation.NoneStrategyComputingCoder;
import io.evitadb.storage.query.computation.SumStrategyComputingCoder;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.query.util.FilterModeUtil;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class PriceFiller {

    private final PriceBetween priceBetween;
    private final PriceDescending priceDescending;
    private final PriceAscending priceAscending;
    private final Query query;
    private final QueryPriceMode priceMode;
    private final boolean requiredhistogram;
    private final FilterMode filterMode;


    public PriceFiller(Query query, QueryPriceMode priceMode,FilterMode filterMode) {
        FilterBy filterBy = query.getFilterBy();
        this.priceBetween = QueryUtils.findFilter(filterBy, PriceBetween.class);
        this.priceDescending = QueryUtils.findOrder(query, PriceDescending.class);
        this.priceAscending = QueryUtils.findOrder(query, PriceAscending.class);
        this.query = query;
        this.priceMode = priceMode;
        this.requiredhistogram = QueryUtils.findRequire(query, PriceHistogram.class) != null;
        this.filterMode = filterMode;
    }

    public void apply(SearchSourceBuilder searchSourceBuilder) {

        if (priceBetween == null && priceAscending == null && priceDescending == null && !requiredhistogram) return;

        Map<String, Object> runtimeMappings = new HashMap<>();

        Map<String, Object> fieldProperties = new HashMap<>();
        fieldProperties.put("type", "double");
        fieldProperties.put("script", constructCode());

        runtimeMappings.put("finalPrice", fieldProperties);
        searchSourceBuilder.runtimeMappings(runtimeMappings);
        if (priceBetween != null && !FilterModeUtil.isPriceHistogramComputingMode(filterMode)){
            searchSourceBuilder.postFilter(
                    boolQuery()
                            .must(
                                    rangeQuery("finalPrice")
                                            .gte(0)
                                            .lte(100000000)
                            ));
        }
    }

    private String constructCode(){
        //        refactor
        return new NoneStrategyComputingCoder(query, priceMode,filterMode).getCode() +
                new FOAndGenericStrategyComputingCoder(query, priceMode,filterMode).getCode() +
                new SumStrategyComputingCoder(query, priceMode,filterMode).getCode();

    }

}
