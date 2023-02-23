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

import io.evitadb.api.query.filter.ReferenceHavingAttribute;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} which converts constraint {@link ReferenceHavingAttribute} to generic {@link SqlPart}
 *
 * @author Jiří Bönsch, 2021
 * @author Lukáš Hornych 2022
 */
public class ReferenceHavingAttributeTranslator extends AttributeFilterConstraintTranslator<ReferenceHavingAttribute> {

    @Override
    public SqlPart translate(@Nonnull ReferenceHavingAttribute constraint, @Nonnull FilterTranslatingContext ctx) {
        final String serializedReferenceType = ctx.getStringTypedValueSerializer().serialize(constraint.getEntityType()).getSerializedValue();
        final List<SqlPart> innerConditions = ctx.getCurrentLevelConditions();

        final StringBuilder foundEntitiesSqlBuilder = new StringBuilder()
                .append("select reference.entity_id " +
                        "from ").append(ctx.getCollectionUid()).append(".t_referenceIndex reference" +
                        "   where reference.entityType = ? and ")
                .append(innerConditions.get(0).getSql());

        final List<Object> foundEntitiesArgs = new LinkedList<>();
        foundEntitiesArgs.add(serializedReferenceType);
        foundEntitiesArgs.addAll(innerConditions.get(0).getArgs());

        final String foundEntitiesCteAlias = ctx.addWithCte(new SqlPart(foundEntitiesSqlBuilder, foundEntitiesArgs), false);

        return new SqlPart("entity.entity_id = any (select entity_id from " + foundEntitiesCteAlias + ")");
    }
}

