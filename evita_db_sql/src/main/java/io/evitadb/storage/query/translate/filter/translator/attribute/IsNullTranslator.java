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

package io.evitadb.storage.query.translate.filter.translator.attribute;

import io.evitadb.api.query.filter.IsNull;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link IsNull} to generic {@link SqlPart}
 *
 * @author Lukáš Hornych 2021
 */
public class IsNullTranslator extends AttributeFilterConstraintTranslator<IsNull> {

    @Override
    public SqlPart translate(@Nonnull IsNull constraint, @Nonnull FilterTranslatingContext ctx) {
        final AttributeSchema attributeSchema = ctx.getAttributeSchema(constraint.getAttributeName());

        // if filtering reference attribute, fetch attribute ad-hoc because caching of found entities is handled
        // by referenceHavingAttribute translator
        if (ctx.getReferenceSchema() != null) {
            return buildDirectValueComparisonSqlPart(
                    ctx,
                    attributeSchema,
                    true,
                    null,
                    "is null"
            );
        }

        // if using caching using cte, we need to find entities that have some value and then in main query
        // found entities that do not match that list, because if attribute "is null" than there is no record of that
        // attribute in database
        final String serializedEntityType = ctx.getStringTypedValueSerializer().serialize(ctx.getCollectionCtx().getEntityType()).getSerializedValue();
        final AttributeTypedValue.TargetType attributeTargetType = AttributeTypedValue.TargetType.from(attributeSchema.getType());
        final SqlPart localeSqlPart = buildLocaleSqlPart(ctx);

        final StringBuilder findEntitiesSqlBuilder = new StringBuilder()
                .append("select entity.entity_id " +
                        "from t_entity entity " +
                        "left join ").append(ctx.getCollectionUid()).append(".t_attributeIndex attribute " +
                        "   on attribute.entity_id = entity.entity_id" +
                        "       and attribute.reference_id is null " +
                        "       and attribute.name = ? " +
                        "       and ").append(localeSqlPart.getSql()).append(" " +
                        "where entity.dropped = false" +
                        "   and entity.type = ?" +
                        "   and attribute.").append(attributeTargetType.getColumn()).append(" is null");

        final List<Object> findEntitiesArgs = new LinkedList<>();
        findEntitiesArgs.add(attributeSchema.getName());
        findEntitiesArgs.addAll(localeSqlPart.getArgs());
        findEntitiesArgs.add(serializedEntityType);

        final String cteAlias = ctx.addWithCte(new SqlPart(findEntitiesSqlBuilder, findEntitiesArgs), false);
        return new SqlPart("entity.entity_id = any (select entity_id from " + cteAlias + ")");
    }
}
