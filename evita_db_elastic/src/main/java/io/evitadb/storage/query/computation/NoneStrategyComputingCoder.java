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

package io.evitadb.storage.query.computation;

import io.evitadb.api.query.Query;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.storage.query.util.FilterMode;

import java.io.Serializable;
import java.util.List;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class NoneStrategyComputingCoder extends ComputingCoder {


    public NoneStrategyComputingCoder(Query query, QueryPriceMode priceMode, FilterMode filterMode) {
        super(query, priceMode,filterMode);
    }

    @Override
    public String getCode() {
        StringBuilder priceListCode = new StringBuilder();
        List<Serializable> priceLists = getPriceLists();
        for (Serializable priceList : priceLists) {

            priceListCode
                    .append("for (item in params._source.prices) { if(item.priceList == '")
                            .append(priceList)
                            .append("' ")
                            .append(addBasics())
                        .append(" ){")
                        .append(constructBase())
                        .append("}")
                    .append("}")
                    .append("if(finalPrice != 100000000){")
                        .append(getBetweenCondition("finalPrice", "emit(finalPrice);return;"))
                    .append("}");
        }
        if (priceLists.isEmpty()) {
            priceListCode = new StringBuilder("for (item in params._source.prices) {" +
                    "if( " + addBasics() + "){" + constructBase() +
                    "}}");
        }

        return
                "if(params._source.priceInnerRecordHandling.value == 'NONE'){" +
                        "double finalPrice = 100000000;" +
                        priceListCode +
                        "return;" +
                        "}";
    }


}
