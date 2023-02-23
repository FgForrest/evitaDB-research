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
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.AbstractAttributeFilterConstraintLeaf;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.query.filter.FilterConstraintTranslator;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;

import javax.annotation.Nonnull;
import java.time.*;
import java.util.Locale;
import java.util.Objects;

import static org.elasticsearch.index.query.QueryBuilders.*;


/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public abstract class AttributeFilterConstraintTranslator<C extends FilterConstraint> implements FilterConstraintTranslator<C> {

    @Override
    public EsConstraint apply(C c, FilterByVisitor filterByVisitor) {
        if (c instanceof AbstractAttributeFilterConstraintLeaf && Objects.equals(((AbstractAttributeFilterConstraintLeaf) c).getAttributeName(), filterByVisitor.getForbiddenAttribute()))
            return null;
        return applyInternally(c, filterByVisitor);
    }

    public abstract EsConstraint applyInternally(C c, FilterByVisitor filterByVisitor);

    protected AttributeSchema getAttributeSchema(@Nonnull FilterByVisitor filterByVisitor, @Nonnull String attributeName) {
        final EntitySchema entitySchema = filterByVisitor.getEntitySchema();
        final ReferenceSchema referenceSchema = filterByVisitor.getReferenceSchema();

        final AttributeSchema attributeSchema;
        if (referenceSchema != null) {
            attributeSchema = referenceSchema.getAttribute(attributeName);
            Assert.notNull(attributeSchema, "Attribute `" + attributeName + "` is not known for reference `" + referenceSchema.getEntityType() + "` of entity `" + entitySchema.getName() + "`!");
        } else {
            attributeSchema = entitySchema.getAttribute(attributeName);
            Assert.notNull(attributeSchema, "Attribute `" + attributeName + "` is not known for entity `" + entitySchema.getName() + "`!");
        }

        return attributeSchema;
    }

    protected BoolQueryBuilder optionallyAddLocale(BoolQueryBuilder query, FilterByVisitor filterByVisitor) {

        Locale locale = filterByVisitor.getLocale();
        if (locale != null) {
            query = query.should(
                            matchQuery(
                                    filterByVisitor.getPathPrefix() + "attribute.locale.keyword",
                                    locale
                            ).operator(Operator.AND)
                    )
                    .should(
                            boolQuery().mustNot(
                                    existsQuery(
                                            filterByVisitor.getPathPrefix() + "attribute.locale"
                                    )
                            )
                    ).minimumShouldMatch(1);
        }
        return query;
    }

    protected BoolQueryBuilder buildDefaultQuery(String rowName, String attributeName, BoolQueryBuilder queryBuilder, FilterByVisitor filterByVisitor) {
        return boolQuery()
                .must(
                        matchQuery(
                                filterByVisitor.getPathPrefix() + "attribute.attributeName",
                                attributeName
                        ).operator(Operator.AND)
                ).filter(
                        nestedQuery(
                                getNestedPath(rowName, filterByVisitor),
                                queryBuilder,
                                ScoreMode.Max
                        )
                );
    }

    /**
     * @param rowName This string may be use for further computation in overriding classes
     */
    protected String getNestedPath(String rowName, @Nonnull FilterByVisitor filterByVisitor) {
        return filterByVisitor.getPathPrefix() + "attribute.value";
    }

    protected EsConstraint nestIntoConstraint(BoolQueryBuilder query, FilterByVisitor filterByVisitor) {
        return new EsConstraint(
                boolQuery()
                        .filter(
                                nestedQuery(filterByVisitor.getPathPrefix() + "attribute",
                                        query,
                                        ScoreMode.Max
                                )
                        )
        );
    }

    protected Object convertObjectAndSerializeValue(Object value, FilterByVisitor filterByVisitor) throws JsonProcessingException {
        value = convertObject(value);
        return value instanceof String ? (String) value : filterByVisitor.getObjectMapper().writeValueAsString(value);
    }

    protected Object convertObject(Object value) {
        if (value instanceof String) {
            try {
                value = LocalDateTime.parse((String) value);
            } catch (Exception ignored) {
                // ignored - dates should be always parsable by LocalDateTime , otherwise, its another value type
            }
        }

        if (value instanceof ZonedDateTime) {
            value = ((ZonedDateTime) value).toEpochSecond();
        } else if (value instanceof LocalDateTime) {
            value = ((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC);
        } else if (value instanceof LocalDate) {
            value = ((LocalDate) value).toEpochDay();
        } else if (value instanceof LocalTime) {
            value = ((LocalTime) value).toSecondOfDay();
        }
        return value;
    }


}
