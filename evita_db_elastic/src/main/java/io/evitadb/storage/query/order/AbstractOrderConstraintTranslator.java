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

package io.evitadb.storage.query.order;

import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.EsOrderConstraint;
import io.evitadb.storage.query.EsQueryTranslator.OrderByVisitor;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.search.sort.*;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public abstract class AbstractOrderConstraintTranslator<C extends OrderConstraint> implements OrderConstraintTranslator<C> {

    public EsOrderConstraint apply(String attributeName, OrderByVisitor orderByVisitor) {
        return applyInternally(attributeName, orderByVisitor);
    }

    protected EsOrderConstraint applyInternally(String attributeName, OrderByVisitor orderByVisitor) {
        final AttributeSchema attributeSchema = getAttributeSchema(orderByVisitor, attributeName);
        String rowName = EntityJsonSerializer.DescribableValue.getRowByType(attributeSchema.getType());
        List<SortBuilder<?>> sortBuilders = new LinkedList<>();
        if (rowName.equals("multiple")) {
            for (int i = 0; i < 4; i++) {
                String curField;
                switch (i) {
                    case 3:
                        curField = "fourth";
                        break;
                    case 2:
                        curField = "third";
                        break;
                    case 1:
                        curField = "second";
                        break;
                    default:
                        curField = "first";
                        break;
                }
                sortBuilders.add(applyInternally(attributeName, rowName + "." + curField, orderByVisitor).order(getSortOrder()).sortMode(getSortMode()));
            }
        }else {
            sortBuilders.add(applyInternally(attributeName,rowName, orderByVisitor).order(getSortOrder()).sortMode(getSortMode()));
        }
        return new EsOrderConstraint(sortBuilders);
    }

    protected abstract SortOrder getSortOrder();
    protected abstract SortMode getSortMode();

    protected FieldSortBuilder applyInternally(String attributeName,String rowName, OrderByVisitor orderByVisitor) {

        return new FieldSortBuilder(
                orderByVisitor.getPathPrefix() + "attribute.value." + rowName)
                .setNestedSort(
                        new NestedSortBuilder(orderByVisitor.getPathPrefix() + "attribute.value")
                                .setFilter(
                                        matchQuery(
                                                orderByVisitor.getPathPrefix() + "attribute.value.key.keyword",
                                                 attributeName
                                        ).operator(Operator.AND)
                                ));
    }

    protected AttributeSchema getAttributeSchema(@Nonnull OrderByVisitor orderByVisitor, @Nonnull String attributeName) {
        final EntitySchema entitySchema = orderByVisitor.getEntitySchema();
        final ReferenceSchema referenceSchema = orderByVisitor.getReferenceSchema();

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

}
