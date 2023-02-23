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

package io.evitadb.storage.query.translate.filter;

import io.evitadb.api.query.require.FacetGroupsConjunction;
import io.evitadb.api.query.require.FacetGroupsDisjunction;
import io.evitadb.api.query.require.FacetGroupsNegation;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlWithClause;
import io.evitadb.storage.query.translate.filter.FilterConstraintTranslatingVisitor.Mode;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValueSerializer;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Context of translating filter query to SQL. It stores data and metadata needed throughout whole translating process
 *
 * @author Lukáš Hornych 2021
 */
@Data
public class FilterTranslatingContext {

    /**
     * Serializer for constraint values for translators.
     */
    private static final AttributeTypedValueSerializer ATTRIBUTE_TYPED_VALUE_SERIALIZER = AttributeTypedValueSerializer.getInstance();
    /**
     * Serializer for values which needs to be serialized to string value, such as entity type, for translators.
     */
    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    /**
     * Context of queried collection
     */
    private final EntityCollectionContext collectionCtx;
    /**
     * In which mode the visitor and translators will be operating
     */
    private final Mode mode;
    /**
     * Reference schema of current reference container context.
     */
    private ReferenceSchema referenceSchema;
    private final Locale locale;
    private final ZonedDateTime now;
    private final QueryPriceMode priceMode;
    private final List<FacetGroupsConjunction> facetGroupsConjunction;
    private final List<FacetGroupsDisjunction> facetGroupsDisjunction;
    private final List<FacetGroupsNegation> facetGroupsNegation;

    /**
     * Stack of already translated constraints.
     */
    protected final Deque<List<SqlPart>> sqlPartsStack = new LinkedList<>();
    /**
     * Final where SQL part after whole query has been visited.
     */
    private SqlPart finalWhereSqlPart = null;
    /**
     * Last used With CTE id used for naming ctes.
     */
    private int lastWithCteId = 0;
    /**
     * CTEs for "with" clause
     */
    private final Map<SqlWithCteKey, SqlPart> withCtes = new HashMap<>();

    public String getCollectionUid() {
        return collectionCtx.getUid();
    }

    public EntitySchema getEntitySchema() {
        return collectionCtx.getEntitySchema().get();
    }

    public AttributeTypedValueSerializer getAttributeTypedValueSerializer() {
        return ATTRIBUTE_TYPED_VALUE_SERIALIZER;
    }

    public StringTypedValueSerializer getStringTypedValueSerializer() {
        return STRING_TYPED_VALUE_SERIALIZER;
    }

    /**
     * Adds CTE for with clause under auto generated alias.
     *
     * @param subquery subquery of cte
     * @param autoJoin if this cte should be automatically joined in main query (cte have to return only single row)
     * @return generated alias
     */
    public String addWithCte(SqlPart subquery, boolean autoJoin) {
        final String alias = generateWithCteAlias();

        final SqlWithCteKey key = new SqlWithCteKey(alias, autoJoin);
        Assert.isTrue(!withCtes.containsKey(key), "There is already cte with alias " + alias);
        withCtes.put(key, subquery);

        return alias;
    }

    /**
     * Adds CTE for with clause under specified alias to reference later.
     *
     * @param alias alias of cte
     * @param subquery subquery of cte
     * @param autoJoin if this cte should be automatically joined in main query (cte have to return only single row)
     */
    public void addWithCte(String alias, SqlPart subquery, boolean autoJoin) {
        final SqlWithCteKey key = new SqlWithCteKey(alias, autoJoin);
        Assert.isTrue(!withCtes.containsKey(key), "There is already cte with alias " + alias);
        withCtes.put(key, subquery);
    }

    /**
     * Returns translated constraints (SQL parts) on current stack level
     */
    @Nonnull
    public List<SqlPart> getCurrentLevelConditions() {
        return sqlPartsStack.peek();
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
        Assert.isTrue(attributeSchema.isFilterable() || attributeSchema.isUnique(), "Attribute " + attributeSchema.getName() + " is not filterable nor unique.");

        return attributeSchema;
    }

    /**
     * Returns final with clause with all added CTEs and joins.
     */
    public SqlWithClause buildFinalWithClause() {
        if(withCtes.isEmpty()) {
            return SqlWithClause.EMPTY;
        }

        // create with clause
        final List<Object> combinedArgs = new LinkedList<>();

        final StringBuilder withBuilder = new StringBuilder("with ");
        final Iterator<Map.Entry<SqlWithCteKey, SqlPart>> withCteIterator = withCtes.entrySet().iterator();
        while (withCteIterator.hasNext()) {
            final Map.Entry<SqlWithCteKey, SqlPart> cte = withCteIterator.next();

            combinedArgs.addAll(cte.getValue().getArgs());

            withBuilder
                    .append(cte.getKey().getAlias())
                    .append(" as (")
                    .append(cte.getValue().getSql())
                    .append(")");

            if (withCteIterator.hasNext()) {
                withBuilder.append(", ");
            }
        }
        final SqlPart with = new SqlPart(withBuilder, combinedArgs);

        // create joins for with
        final StringBuilder withJoinsBuilder = new StringBuilder();
        withCtes.keySet().stream()
            .filter(SqlWithCteKey::isAutoJoin)
                .map(SqlWithCteKey::getAlias)
                // can only join properly cte with single row
                .forEach(alias -> withJoinsBuilder
                        .append(" join ")
                        .append(alias)
                        .append(" on true ")
                );

        return new SqlWithClause(with, withJoinsBuilder);
    }

    /**
     * Generates unique alias for With CTE
     */
    private String generateWithCteAlias() {
        return "cte_" + ++lastWithCteId;
    }


    /**
     * With clause CTE descriptor
     */
    @Data
    @EqualsAndHashCode(exclude = "autoJoin")
    private static class SqlWithCteKey {
        private final String alias;
        private final boolean autoJoin;
    }
}
