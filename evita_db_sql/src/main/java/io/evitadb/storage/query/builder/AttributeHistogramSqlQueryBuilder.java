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

package io.evitadb.storage.query.builder;

import io.evitadb.storage.ComplexEntityQueryContextHolder;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlWithClause;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Sql query builder for fetching attribute histogram for single attribute
 *
 * @author Lukáš Hornych 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttributeHistogramSqlQueryBuilder {

    public static SqlPart buildAttributeHistogramQuery(@Nonnull String collectionUid,
                                                       @Nonnull String attributeName,
                                                       int buckets,
                                                       @Nullable Locale language,
                                                       @Nonnull SqlPart userFilter,
                                                       @Nonnull SqlWithClause userFilterWithClause) {
        final String localeSqlCondition;
        final List<Object> args = new LinkedList<>();
        args.addAll(userFilterWithClause.getWith().getArgs());
        args.add(attributeName);

        if (language == null) {
            localeSqlCondition = "is null";
        } else {
            localeSqlCondition = "= ?";
            args.add(language.toString());
        }

        args.addAll(userFilter.getArgs());
        args.add(buckets);

        final StringBuilder sqlBuilder = new StringBuilder();
        if (userFilterWithClause.getWith().getSql().length() == 0) {
            sqlBuilder
                    .append("with ");
        } else {
            sqlBuilder
                    .append(userFilterWithClause.getWith().getSql())
                    .append(", ");
        }
        sqlBuilder
                // find only relevant attributes (according to user filter and attribute key of wanted histogram)
                .append("relevantAttribute as (" +
                        "   select attribute.intValues[1] as value")
                .append("   from ").append(ComplexEntityQueryContextHolder.getContext().getTempTableName()).append(" entity ").append(userFilterWithClause.getWithJoins())
                .append("   join ").append(collectionUid).append(".t_attributeIndex attribute " +
                        "       on entity.entity_id = attribute.entity_id" +
                        "           and attribute.reference_id is null" +
                        "           and attribute.name = ?" +
                        "           and attribute.locale ").append(localeSqlCondition)
                .append("   where ").append(userFilter.getSql())
                .append(// find out metadata of relevant attributes
                        "), attributeMetadata as (" +
                        "   select min(relevantAttribute.value) as min, " +
                        "       max(relevantAttribute.value) as max " +
                        "   from relevantAttribute" +
                        ")" +
                        // compute buckets
                        "select metadata.min    as histogramMin, " +
                        "       metadata.max    as histogramMax, " +
                        "       width_bucket(" +
                        "           attribute.value, " +
                        "           metadata.min, " +
                        "           case " +
                        "               when metadata.max = metadata.min then metadata.max + 1 " + // precaution when min and max is same
                        "               else metadata.max " +
                        "           end, " +
                        "           ?" +
                        "       )               as bucketIndex, " +
                        "       count(*)        as bucketOccurrences " +
                        "from relevantAttribute attribute, attributeMetadata metadata " +
                        "group by histogramMin, histogramMax, bucketIndex " +
                        "order by histogramMin, histogramMax, bucketIndex");

        return new SqlPart(sqlBuilder, args);
    }
}
