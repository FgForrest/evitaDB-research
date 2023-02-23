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

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.serialization.typedValue.AttributeTypedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Ancestor for filter constraint translators translating attribute related constraints. It provides common helper methods.
 *
 * @param <CONSTRAINT> translating constraint
 * @author Lukáš Hornych 2021
 */
public abstract class AttributeFilterConstraintTranslator<CONSTRAINT extends FilterConstraint> implements FilterConstraintTranslator<CONSTRAINT> {

    protected static final String ENTITY_ATTRIBUTE_JOIN_CONDITION = "attribute.entity_id = entity.entity_id";
    protected static final String REFERENCE_ATTRIBUTE_JOIN_CONDITION = "attribute.reference_id = reference.reference_id";

    protected static final String NO_LOCALE_CONDITION = "attribute.locale is null";
    protected static final String SPECIFIED_LOCALE_CONDITION = "(" + NO_LOCALE_CONDITION + " or attribute.locale = ?)";

    /**
     * Returns appropriate attribute join condition depending on context of constraint
     */
    protected String getAttributeJoinCondition(@Nonnull FilterTranslatingContext ctx) {
        if (ctx.getReferenceSchema() != null) {
            return REFERENCE_ATTRIBUTE_JOIN_CONDITION;
        } else {
            return ENTITY_ATTRIBUTE_JOIN_CONDITION;
        }
    }

    /**
     * Returns attribute locale sql part
     */
    protected SqlPart buildLocaleSqlPart(@Nonnull FilterTranslatingContext ctx) {
        final Locale locale = ctx.getLocale();
        if (locale == null) {
            return new SqlPart(NO_LOCALE_CONDITION);
        }
        return new SqlPart(SPECIFIED_LOCALE_CONDITION, List.of(locale.toString()));
    }

    /**
     * <p>
     * Builds default SQL for comparing if entity has particular attribute. For constraints inside {@link io.evitadb.api.query.filter.ReferenceHavingAttribute}
     * it uses ad-hoc attribute comparing ({@link #buildDirectValueComparisonSqlPart(FilterTranslatingContext, AttributeSchema, boolean, String, String, Object...)})
     * and for other constraint it utilizes caching using CTEs ({@link #buildFindSatisfactoryEntitiesForValueCte(FilterTranslatingContext, AttributeSchema, boolean, String, String, Object...)}).
     * </p>
     *
     * <h3>Single/array value</h3>
     * <p>PostgreSQL distinguishes comparing single value and array value therefore it needs to be specified what we
     * are comparing to adjust built expression.</p>
     *
     * <h3>Prefix/suffix</h3>
     * <p>In PostgreSQL some values are compared in this format:
     * <pre>
     *     dbValue *operator* myValue
     * </pre>
     * and some values are compared in inverted format:
     * <pre>
     *     myValue *operator* dbValue
     * </pre>
     * depending on type of comparison.
     * This method supports both formats and automatically prepares the {@code dbValue}. You need to pick one of the formats
     * and provide either prefix (myValue *operator*) or suffix (*operator* myValue).
     * </p>
     *
     * @param ctx translating context
     * @param attributeSchema schema of comparing attribute
     * @param comparingArray if target constraint comparing value as array
     * @param valueComparisonPrefix prefix of expression, used when operator with comparing value is before attribute value query
     * @param valueComparisonSuffix suffix of expression, used when operator with comparing values is after attribute value query
     * @param valueComparisonArgs additional arguments used in either prefix or suffix
     * @return built final sql part used in main query
     */
    protected SqlPart buildValueComparisonSqlPart(@Nonnull FilterTranslatingContext ctx,
                                                  @Nonnull AttributeSchema attributeSchema,
                                                  boolean comparingArray,
                                                  @Nullable String valueComparisonPrefix,
                                                  @Nullable String valueComparisonSuffix,
                                                  Object... valueComparisonArgs) {
        // if filtering reference attribute, fetch attribute ad-hoc because caching of found entities is handled
        // by referenceHavingAttribute translator
        if (ctx.getReferenceSchema() != null) {
            return buildDirectValueComparisonSqlPart(
                    ctx,
                    attributeSchema,
                    comparingArray,
                    valueComparisonPrefix,
                    valueComparisonSuffix,
                    valueComparisonArgs
            );
        }

        final String findSatisfactoryEntitiesForValueCteAlias = buildFindSatisfactoryEntitiesForValueCte(
                ctx,
                attributeSchema,
                comparingArray,
                valueComparisonPrefix,
                valueComparisonSuffix,
                valueComparisonArgs
        );
        return buildEntityMatchingFoundSatisfactoryEntitiesSqlPart(findSatisfactoryEntitiesForValueCteAlias);
    }

    /**
     * <p>Builds SQL part for ad-hoc comparing attribute values, i.e. find particular attribute value by name and owner
     * entity and compare it directly.</p>
     *
     * <h3>Single/array value</h3>
     * <p>PostgreSQL distinguishes comparing single value and array value therefore it needs to be specified what we
     * are comparing to adjust built expression.</p>
     *
     * <h3>Prefix/suffix</h3>
     * <p>In PostgreSQL some values are compared in this format:
     * <pre>
     *     dbValue *operator* myValue
     * </pre>
     * and some values are compared in inverted format:
     * <pre>
     *     myValue *operator* dbValue
     * </pre>
     * depending on type of comparison.
     * This method supports both formats and automatically prepares the {@code dbValue}. You need to pick one of the formats
     * and provide either prefix (myValue *operator*) or suffix (*operator* myValue).
     * </p>
     *
     * @param ctx translating context
     * @param attributeSchema schema of comparing attribute
     * @param comparingArray if target constraint comparing value as array
     * @param valueComparisonPrefix prefix of expression, used when operator with comparing value is before attribute value query
     * @param valueComparisonSuffix suffix of expression, used when operator with comparing values is after attribute value query
     * @param valueComparisonArgs additional arguments used in either prefix or suffix
     * @return built final sql part with attribute value comparing expression
     */
    protected SqlPart buildDirectValueComparisonSqlPart(@Nonnull FilterTranslatingContext ctx,
                                                        @Nonnull AttributeSchema attributeSchema,
                                                        boolean comparingArray,
                                                        @Nullable String valueComparisonPrefix,
                                                        @Nullable String valueComparisonSuffix,
                                                        Object... valueComparisonArgs) {
        Assert.isTrue(
                (valueComparisonPrefix == null) != (valueComparisonSuffix == null),
                "Either expression prefix or suffix must be specified. Not both nor neither."
        );

        final StringBuilder sqlBuilder = new StringBuilder();

        final AttributeTypedValue.TargetType attributeTargetType = AttributeTypedValue.TargetType.from(attributeSchema.getType());
        final SqlPart localeSqlPart = buildLocaleSqlPart(ctx);

        // append prefix
        if (valueComparisonPrefix != null) {
            sqlBuilder.append(valueComparisonPrefix);
        }

        // build attribute value query
        sqlBuilder.append(" (");
        buildFetchValueQuery(sqlBuilder, ctx, comparingArray, attributeTargetType, localeSqlPart);
        sqlBuilder.append(") ");

        // append suffix
        if (valueComparisonSuffix != null) {
            sqlBuilder.append(valueComparisonSuffix);
        }

        // build expression args
        final List<Object> args = new LinkedList<>();
        args.add(attributeSchema.getName());
        args.addAll(localeSqlPart.getArgs());
        if (valueComparisonPrefix != null) {
            args.addAll(0, Arrays.asList(valueComparisonArgs));
        } else {
            args.addAll(Arrays.asList(valueComparisonArgs));
        }

        return new SqlPart(sqlBuilder, args);
    }

    /**
     * Build SQL part for querying attribute value to compare against certain constraint
     *
     * @param sqlBuilder sql builder into which the query will be written
     * @param ctx translating context
     * @param comparingArray if target constraint comparing value as array
     * @param attributeTargetType type of attribute value
     * @param localeSqlPart built locale sql part
     */
    private void buildFetchValueQuery(@Nonnull StringBuilder sqlBuilder,
                                      @Nonnull FilterTranslatingContext ctx,
                                      boolean comparingArray,
                                      @Nonnull AttributeTypedValue.TargetType attributeTargetType,
                                      @Nonnull SqlPart localeSqlPart) {
        // wrap base attribute value query with type cast if array
        if (comparingArray) {
            sqlBuilder.append("(");
        }

        // build base attribute value query
        sqlBuilder
                .append("select attribute.")
                .append(attributeTargetType.getColumn());
        if (!comparingArray) {
            sqlBuilder.append("[1]");
        }
        sqlBuilder.append(" from ").append(ctx.getCollectionUid()).append(".t_attributeIndex attribute " +
                        "where ").append(getAttributeJoinCondition(ctx))
                .append("   and ").append(localeSqlPart.getSql())
                .append("   and attribute.name = ? " +
                        " limit 1");

        // wrap base attribute value query with type cast if array
        if (comparingArray) {
            sqlBuilder.append(")::").append(attributeTargetType.getSqlType()).append("[]");
        }
    }

    /**
     * <p>Builds SQL CTE for cached comparing attribute values. The CTE is automatically added to list of CTEs
     * which is later added as with clause to main query. Such CTE contains internal ids of all entities that
     * have compared attribute. This can be used to lookup if particular entity is such list in main query
     * (see {@link #buildEntityMatchingFoundSatisfactoryEntitiesSqlPart(String)}).</p>
     *
     * <h3>Single/array value</h3>
     * <p>PostgreSQL distinguishes comparing single value and array value therefore it needs to be specified what we
     * are comparing to adjust built expression.</p>
     *
     * <h3>Prefix/suffix</h3>
     * <p>In PostgreSQL some values are compared in this format:
     * <pre>
     *     dbValue *operator* myValue
     * </pre>
     * and some values are compared in inverted format:
     * <pre>
     *     myValue *operator* dbValue
     * </pre>
     * depending on type of comparison.
     * This method supports both formats and automatically prepares the {@code dbValue}. You need to pick one of the formats
     * and provide either prefix (myValue *operator*) or suffix (*operator* myValue).
     * </p>
     *
     * @param ctx translating context
     * @param attributeSchema schema of comparing attribute
     * @param comparingArray if target constraint comparing value as array
     * @param valueComparisonPrefix prefix of expression, used when operator with comparing value is before attribute value query
     * @param valueComparisonSuffix suffix of expression, used when operator with comparing values is after attribute value query
     * @param valueComparisonArgs additional arguments used in either prefix or suffix
     * @return alias of prepared cte
     */
    protected String buildFindSatisfactoryEntitiesForValueCte(@Nonnull FilterTranslatingContext ctx,
                                                              @Nonnull AttributeSchema attributeSchema,
                                                              boolean comparingArray,
                                                              @Nullable String valueComparisonPrefix,
                                                              @Nullable String valueComparisonSuffix,
                                                              Object... valueComparisonArgs) {

        final AttributeTypedValue.TargetType attributeTargetType = AttributeTypedValue.TargetType.from(attributeSchema.getType());
        final SqlPart localeSqlPart = buildLocaleSqlPart(ctx);

        final StringBuilder findEntitiesSqlBuilder = new StringBuilder()
                .append("select entity_id " +
                        "from ").append(ctx.getCollectionUid()).append(".t_attributeIndex attribute " +
                        "where attribute.reference_id is null" +
                        "   and attribute.name = ?" +
                        "   and ").append(localeSqlPart.getSql())
                .append("   and ");
        if (valueComparisonPrefix != null) {
            findEntitiesSqlBuilder
                    .append(valueComparisonPrefix);
        }
        if (!comparingArray) {
            findEntitiesSqlBuilder
                    .append(" attribute.").append(attributeTargetType.getColumn()).append("[1] ");
        } else {
            findEntitiesSqlBuilder
                    .append(" ((attribute.").append(attributeTargetType.getColumn()).append(")::").append(attributeTargetType.getSqlType()).append("[]) ");
        }
        if (valueComparisonSuffix != null) {
            findEntitiesSqlBuilder
                    .append(valueComparisonSuffix);
        }

        final List<Object> findEntitiesArgs = new LinkedList<>();
        findEntitiesArgs.add(attributeSchema.getName());
        findEntitiesArgs.addAll(localeSqlPart.getArgs());
        findEntitiesArgs.addAll(Arrays.asList(valueComparisonArgs));

        return ctx.addWithCte(new SqlPart(findEntitiesSqlBuilder, findEntitiesArgs), false);
    }

    /**
     * Builds SQL part to lookup if current queried entity is in list CTE prepared by {@link #buildFindSatisfactoryEntitiesForValueCte(FilterTranslatingContext, AttributeSchema, boolean, String, String, Object...)}
     *
     * @param findEntitiesCteAlias alias of prepared CTE
     * @return sql part
     */
    protected SqlPart buildEntityMatchingFoundSatisfactoryEntitiesSqlPart(@Nonnull String findEntitiesCteAlias) {
        return new SqlPart("entity.entity_id = any (select entity_id from " + findEntitiesCteAlias + ")");
    }
}
