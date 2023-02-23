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

package io.evitadb.storage.query.translate.order;

import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import lombok.Data;

import javax.annotation.Nonnull;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Context of translating order query to SQL. It stores data and metadata needed throughout whole translating process
 *
 * @author Lukáš Hornych 2021
 */
@Data
public class OrderTranslatingContext {

    /**
     * Serializer for values which needs to be serialized to string value, such as entity type, for translators.
     */
    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    /**
     * Context of queried collection
     */
    private final EntityCollectionContext collectionCtx;
    /**
     * Reference schema of current reference container context.
     */
    private ReferenceSchema referenceSchema;
    private final Locale locale;

    /**
     * Counter holding count of visited reference containers to be able to correctly index join aliases.
     */
    private int referenceSortCount = 0;
    /**
     * Stack of already translated constraints.
     */
    protected final Deque<List<SqlSortExpression>> sqlSortExpressionsStack = new LinkedList<>();
    /**
     * Final SQL condition after whole query has been visited.
     */
    private SqlSortExpression finalSqlSortExpression = null;


    public String getCollectionUid() {
        return collectionCtx.getUid();
    }

    public EntitySchema getEntitySchema() {
        return collectionCtx.getEntitySchema().get();
    }

    public StringTypedValueSerializer getStringTypedValueSerializer() {
        return STRING_TYPED_VALUE_SERIALIZER;
    }

    /**
     * Returns translated constraints (SQL sort expressions) on current stack level
     */
    @Nonnull
    public List<SqlSortExpression> getCurrentLevelExpressions() {
        final List<SqlSortExpression> currentLevelExpressions = sqlSortExpressionsStack.peek();
        Assert.notNull(currentLevelExpressions, "Missing current level expressions in order constraint translation.");
        return currentLevelExpressions;
    }

    /**
     * Tries to find attribute schema depending on context of constraint. It searches either entity schema or reference schema.
     *
     * @param attributeName name of attribute
     * @return found schema
     */
    @Nonnull
    public AttributeSchema getAttributeSchema(@Nonnull String attributeName) {
        final AttributeSchema attributeSchema;

        if (referenceSchema != null) {
            attributeSchema = referenceSchema.getAttribute(attributeName);
            Assert.notNull(attributeSchema, "Attribute `" + attributeName + "` is not known for reference `" + referenceSchema.getEntityType() + "` of entity `" + getEntitySchema().getName() + "`!");
        } else {
            attributeSchema = getEntitySchema().getAttribute(attributeName);
            Assert.notNull(attributeSchema, "Attribute `" + attributeName + "` is not known for entity `" + getEntitySchema().getName() + "`!");
        }
        Assert.isTrue(attributeSchema.isSortable(), "Attribute " + attributeSchema.getName() + " is not sortable.");

        return attributeSchema;
    }
}
