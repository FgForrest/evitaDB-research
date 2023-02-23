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

import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.Range;
import io.evitadb.api.query.filter.Equals;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.model.EsEnumWrapper;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer.DescribableValue.JsonMultiple;
import lombok.SneakyThrows;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;

import javax.annotation.Nonnull;
import java.io.Serializable;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;


/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class EqualsTranslator extends AttributeFilterConstraintTranslator<Equals> {

    @SneakyThrows
    @Override
    public EsConstraint applyInternally(Equals constraint, FilterByVisitor filterByVisitor) {
        final AttributeSchema attributeSchema = getAttributeSchema(filterByVisitor, constraint.getAttributeName());
        Object value = convertObject(constraint.getAttributeValue());
        String serializedValue = value instanceof String ? (String) value : filterByVisitor.getObjectMapper().writeValueAsString(value);
        Class<? extends Serializable> type = attributeSchema.getType();
        String rowName = EntityJsonSerializer.DescribableValue.getRowByType(type);


        BoolQueryBuilder base = boolQuery();

        if (rowName.contains("Range")) {

            Range<?> range = (Range<?>) value;
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".gte", rowName, convertObject(range.getPreciseFrom()));
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".lte", rowName, convertObject(range.getPreciseTo()));
        } else if (rowName.equals("multiple")) {
            Multiple multiple = (Multiple) value;
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".first", rowName, convertObject(new JsonMultiple(multiple).getFirst()));
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".second", rowName, convertObject(new JsonMultiple(multiple).getSecond()));
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".third", rowName, convertObject(new JsonMultiple(multiple).getThird()));
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".fourth", rowName, convertObject(new JsonMultiple(multiple).getFourth()));
        } else if (Enum.class.isAssignableFrom(type)) {
            EsEnumWrapper<?> enumWrapper = new EsEnumWrapper((Enum) value);
            addMatchQuery(base, filterByVisitor.getPathPrefix(), ".value", rowName, enumWrapper.getValue());
            if (enumWrapper.getEnumClass() != null){
                addMatchQuery(base, filterByVisitor.getPathPrefix(), ".valueClass", rowName, enumWrapper.getEnumClass().getName());
            }
        } else {
            addMatchQuery(base, filterByVisitor.getPathPrefix(), "", rowName, serializedValue);
        }
        BoolQueryBuilder query = buildDefaultQuery(
                rowName,
                constraint.getAttributeName(),
                base,
                filterByVisitor);

        optionallyAddLocale(query, filterByVisitor);

        return nestIntoConstraint(query, filterByVisitor);
    }

    private void addMatchQuery(BoolQueryBuilder base, String pathPrefix, String pathPostfix, String rowName, Object value) {
        if (value == null) return;
        base.must(
                matchQuery(
                        pathPrefix + "attribute.value." + rowName + pathPostfix,
                        value
                ).operator(Operator.AND)
        );
    }

    @Override
    protected String getNestedPath(String rowName, @Nonnull FilterByVisitor filterByVisitor) {
        return rowName.contains("Range") ? super.getNestedPath(rowName, filterByVisitor) + "." + rowName : super.getNestedPath(rowName, filterByVisitor);
    }
}
