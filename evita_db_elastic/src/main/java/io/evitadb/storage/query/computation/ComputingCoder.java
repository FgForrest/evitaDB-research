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
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.order.PriceAscending;
import io.evitadb.api.query.order.PriceDescending;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.storage.query.util.FilterMode;
import io.evitadb.storage.query.util.FilterModeUtil;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public abstract class ComputingCoder {

    protected final PriceInPriceLists priceInPriceLists;
    protected final PriceBetween priceBetween;
    protected final PriceInCurrency priceInCurrency;
    protected final PriceValidIn priceValidIn;
    protected final PriceDescending priceDescending;
    protected final PriceAscending priceAscending;
    protected final FilterMode filterMode;

    protected String priceFormat;


    protected ComputingCoder(Query query, QueryPriceMode priceMode, FilterMode filterMode) {
        FilterBy filterBy = query.getFilterBy();
        this.priceInPriceLists = QueryUtils.findFilter(filterBy, PriceInPriceLists.class);
        this.priceBetween = QueryUtils.findFilter(filterBy, PriceBetween.class);
        this.priceInCurrency = QueryUtils.findFilter(filterBy, PriceInCurrency.class);
        this.priceValidIn = QueryUtils.findFilter(filterBy, PriceValidIn.class);
        this.priceDescending = QueryUtils.findOrder(query, PriceDescending.class);
        this.priceAscending = QueryUtils.findOrder(query, PriceAscending.class);
        this.priceFormat = priceMode.equals(QueryPriceMode.WITHOUT_VAT) ? "priceWithoutVat" : "priceWithVat";
        this.filterMode = filterMode;
    }

    public abstract String getCode();

    protected List<Serializable> getPriceLists() {
        if (priceInPriceLists == null) {
            return Collections.emptyList();
        } else {
            return asList(priceInPriceLists.getPriceLists());
        }
    }

    protected String addBasics() {
        StringBuilder sb = new StringBuilder();
        if (priceInCurrency != null) {
            sb.append("&& ( item.currency == '" + priceInCurrency.getCurrency().toString() + "')");
        }
        if (priceValidIn != null) {
            ZonedDateTime theMoment = priceValidIn.getTheMoment();
            long theMomentEpochSec = Objects.requireNonNull(theMoment).toEpochSecond();
            sb.append("&& ( item.validity == null || " +
                    "(item.validity.gte < " + theMomentEpochSec + " " +
                    "&& item.validity.lte > " + theMomentEpochSec + "))");
        }
        return sb.toString();
    }

    protected String constructBase() {
        return " if(finalPrice == 100000000) { finalPrice = item." + priceFormat + "; break;}";
    }


    protected String getBetweenCondition(String itemName, String content) {
        return priceBetween == null || FilterModeUtil.isPriceHistogramComputingMode(filterMode) ?
                content :
                " if( " + itemName + " > " + priceBetween.getFrom() + " && " + itemName + " < " + priceBetween.getTo() + ")" +
                        "{" + content + "} ";

    }
}
