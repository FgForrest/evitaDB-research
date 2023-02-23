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

package io.evitadb.storage.query.filter.attribute;

import io.evitadb.api.query.filter.InRange;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer;
import lombok.SneakyThrows;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class InRangeTranslator extends AttributeFilterConstraintTranslator<InRange> {

    @SneakyThrows
    @Override
    public EsConstraint applyInternally(InRange constraint, FilterByVisitor filterByVisitor) {


        final AttributeSchema attributeSchema = getAttributeSchema(filterByVisitor, constraint.getAttributeName());

        Object value = constraint.getUnknownArgument();
        if (value == null) {
            value = filterByVisitor.getNow().toEpochSecond();
        }
        Object serializedValue = convertObjectAndSerializeValue(value,filterByVisitor);
        Class<? extends Serializable> type = attributeSchema.getType();
        String rowName = EntityJsonSerializer.DescribableValue.getRowByType(type);

        List<RangeQueryBuilder> ranges = new LinkedList<>();

        ranges.add(
                rangeQuery(filterByVisitor.getPathPrefix() + "attribute.value." + rowName + "." + "lte")
                        .gte(serializedValue)
                        .lte(null));
        ranges.add(
                rangeQuery(filterByVisitor.getPathPrefix() + "attribute.value." + rowName + "." + "gte")
                        .gte(null)
                        .lte(serializedValue));
        BoolQueryBuilder boolQuery = boolQuery();
        ranges.forEach(boolQuery::must);

        BoolQueryBuilder query = buildDefaultQuery(
                rowName, constraint.getAttributeName(),
                boolQuery, filterByVisitor);

        optionallyAddLocale(query, filterByVisitor);

        return nestIntoConstraint(query, filterByVisitor);
    }

    @Override
    protected String getNestedPath(String rowName,@Nonnull FilterByVisitor filterByVisitor) {
        return super.getNestedPath(rowName,filterByVisitor)+"."+rowName;
    }
}
