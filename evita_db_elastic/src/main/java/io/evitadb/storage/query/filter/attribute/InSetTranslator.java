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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.evitadb.api.query.filter.InSet;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.exception.EvitaInsertException;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class InSetTranslator extends AttributeFilterConstraintTranslator<InSet> {

    @Override
    public EsConstraint applyInternally(InSet constraint, FilterByVisitor filterByVisitor) {

        final AttributeSchema attributeSchema = getAttributeSchema(filterByVisitor, constraint.getAttributeName());
        Comparable<?>[] set = constraint.getSet();
        List<String> serializedValue = Arrays
                .stream(set)
                .map(i -> {
                        try {
                            return i instanceof String ? (String) i : filterByVisitor.getObjectMapper().writeValueAsString(i);
                        } catch (JsonProcessingException e) {
                            throw new EvitaInsertException("Cannot convert value: "+ i,e);
                        }
                    })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        String rowName = EntityJsonSerializer.DescribableValue.getRowByType(attributeSchema.getType());

        BoolQueryBuilder query = buildDefaultQuery(
                rowName, constraint.getAttributeName(),
                    boolQuery().must(
                            termsQuery(
                                    filterByVisitor.getPathPrefix() + "attribute.value." + rowName,
                                    serializedValue
                            )
                    ),
                filterByVisitor);

        optionallyAddLocale(query, filterByVisitor);

        return nestIntoConstraint(query, filterByVisitor);
    }
}
