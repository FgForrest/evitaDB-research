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

package io.evitadb.storage.query.translate.order.translator;

import io.evitadb.api.query.order.AbstractOrderAttributeConstraintLeaf;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.query.translate.order.OrderTranslatingContext;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Ancestor for order constraint translators translating attribute related constraints. It provides common helper methods.
 *
 * @param <CONSTRAINT> translating constraint
 * @author Lukáš Hornych 2021
 */
public abstract class AttributeOrderConstraintTranslator<CONSTRAINT extends AbstractOrderAttributeConstraintLeaf> implements OrderConstraintTranslator<CONSTRAINT> {

    protected abstract String getOrderOperator();

    @Nonnull
    @Override
    public SqlSortExpression translate(@Nonnull CONSTRAINT constraint, @Nonnull OrderTranslatingContext ctx) {
        final AttributeSchema attributeSchema = ctx.getAttributeSchema(constraint.getAttributeName());
        final AttributeTypedValue.TargetType attributeTargetType = AttributeTypedValue.TargetType.from(attributeSchema.getType());

        final String attributeAlias;
        final int containerId;
        final int attributeId = ctx.getCurrentLevelExpressions().size();
        final SqlPart localeSqlPart;
        final List<Object> args = new LinkedList<>();

        if (ctx.getReferenceSchema() != null) {
            containerId = ctx.getReferenceSortCount();

            final Serializable referencedEntityType = ctx.getReferenceSchema().getEntityType();
            args.add(ctx.getStringTypedValueSerializer().serialize(referencedEntityType).getSerializedValue());
        } else {
            containerId = 0;
        }
        attributeAlias = buildAttributeAlias(containerId, attributeId);

        args.add(constraint.getAttributeName());

        localeSqlPart = buildLocaleSqlPart(ctx, containerId, attributeId);
        args.addAll(localeSqlPart.getArgs());

        final StringBuilder joinSqlBuilder = new StringBuilder()
                .append("left outer join ").append(ctx.getCollectionUid()).append(".t_attributeIndex ").append(attributeAlias)
                .append("   on (").append(getAttributeJoinCondition(ctx, containerId, attributeId))
                .append("       and ").append(attributeAlias).append(".name = ?" +
                        "       and ").append(localeSqlPart.getSql())
                .append("   )");

        final StringBuilder sqlBuilder = new StringBuilder()
                .append(attributeAlias)
                .append(".")
                .append(attributeTargetType.getColumn())
                .append(" ").append(getOrderOperator()).append(" nulls last");

        return new SqlSortExpression(joinSqlBuilder, sqlBuilder, args);
    }

    /**
     * Returns appropriate attribute join condition depending on context of constraint
     *
     * @param ctx translating context
     * @param containerId unique id of container in which the constraint is placed, if it is entity attribute it
     *                    should be 0, if it is reference attribute it should be unique id for each ReferenceAttribute container
     * @param attributeId unique id of attribute inside the container
     */
    private StringBuilder getAttributeJoinCondition(@Nonnull OrderTranslatingContext ctx, int containerId, int attributeId) {
        final String attributeAlias = buildAttributeAlias(containerId, attributeId);
        if (ctx.getReferenceSchema() != null) {
            return new StringBuilder()
                    .append(attributeAlias).append(".entity_id = filteredEntity.entity_id and ")
                    .append(attributeAlias).append(".reference_id is not null and ")
                    .append(attributeAlias).append(".reference_entityType = ?");
        } else {
            return new StringBuilder()
                    .append(attributeAlias).append(".entity_id = filteredEntity.entity_id and ")
                    .append(attributeAlias).append(".reference_id is null");
        }
    }

    /**
     * Returns attribute locale sql part
     *
     * @param ctx translating context
     * @param containerId unique id of container in which the constraint is placed, if it is entity attribute it
     *                    should be 0, if it is reference attribute it should be unique id for each ReferenceAttribute container
     * @param attributeId unique id of attribute inside the container
     */
    private SqlPart buildLocaleSqlPart(@Nonnull OrderTranslatingContext ctx, int containerId, int attributeId) {
        final String attributeAlias = buildAttributeAlias(containerId, attributeId);

        final Locale locale = ctx.getLocale();
        if (locale == null) {
            return new SqlPart(attributeAlias + ".locale is null");
        }
        return new SqlPart(
                new StringBuilder()
                        .append("(")
                        .append(attributeAlias).append(".locale is null or ")
                        .append(attributeAlias).append(".locale = ?)"),
                List.of(locale.toString())
        );
    }

    private String buildAttributeAlias(int containerId, int attributeId) {
        return "attribute_" + containerId + "_" + attributeId;
    }
}
