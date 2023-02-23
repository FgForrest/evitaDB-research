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

import io.evitadb.api.io.SqlEvitaRequest;
import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.require.FacetGroupsConjunction;
import io.evitadb.api.query.require.FacetGroupsDisjunction;
import io.evitadb.api.query.require.FacetGroupsNegation;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.SqlWithClause;
import io.evitadb.storage.query.translate.filter.translator.FilterByTranslator;
import io.evitadb.storage.query.translate.filter.translator.FilterConstraintTranslator;
import io.evitadb.storage.query.translate.filter.translator.LanguageTranslator;
import io.evitadb.storage.query.translate.filter.translator.PrimaryKeyTranslator;
import io.evitadb.storage.query.translate.filter.translator.attribute.*;
import io.evitadb.storage.query.translate.filter.translator.facet.FacetTranslator;
import io.evitadb.storage.query.translate.filter.translator.facet.UserFilterTranslator;
import io.evitadb.storage.query.translate.filter.translator.hierarchy.*;
import io.evitadb.storage.query.translate.filter.translator.price.PriceBetweenTranslator;
import io.evitadb.storage.query.translate.filter.translator.price.PriceInCurrencyTranslator;
import io.evitadb.storage.query.translate.filter.translator.price.PriceInPriceListsTranslator;
import io.evitadb.storage.query.translate.filter.translator.price.PriceValidInTranslator;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;

import static io.evitadb.api.query.QueryUtils.findRequires;
import static java.util.Optional.ofNullable;

/**
 * Implementation of {@link ConstraintVisitor} for translating filter constraints to {@link SqlPart}s.
 * Root of constraints tree can be any filter constraint and final output is single {@link SqlPart}
 * containing whole translated tree.
 *
 * @author Lukáš Hornych 2021
 */
@Getter
public class FilterConstraintTranslatingVisitor implements ConstraintVisitor<FilterConstraint> {

    /**
     * Available constraint translators.
     */
    private static final Map<Class<? extends FilterConstraint>, FilterConstraintTranslator<? extends FilterConstraint>> CONSTRAINT_TRANSLATORS = new HashMap<>();
    static {
        CONSTRAINT_TRANSLATORS.put(FilterBy.class, new FilterByTranslator());
        CONSTRAINT_TRANSLATORS.put(PrimaryKey.class, new PrimaryKeyTranslator());
        CONSTRAINT_TRANSLATORS.put(Language.class, new LanguageTranslator());

        // attribute
        CONSTRAINT_TRANSLATORS.put(And.class, new AndTranslator());
        CONSTRAINT_TRANSLATORS.put(Or.class, new OrTranslator());
        CONSTRAINT_TRANSLATORS.put(Not.class, new NotTranslator());
        CONSTRAINT_TRANSLATORS.put(Equals.class, new EqualsTranslator());
        CONSTRAINT_TRANSLATORS.put(GreaterThan.class, new GreaterThanTranslator());
        CONSTRAINT_TRANSLATORS.put(GreaterThanEquals.class, new GreaterThanEqualsTranslator());
        CONSTRAINT_TRANSLATORS.put(LessThan.class, new LessThanTranslator());
        CONSTRAINT_TRANSLATORS.put(LessThanEquals.class, new LessThanEqualsTranslator());
        CONSTRAINT_TRANSLATORS.put(Between.class, new BetweenTranslator());
        CONSTRAINT_TRANSLATORS.put(InSet.class, new InSetTranslator());
        CONSTRAINT_TRANSLATORS.put(Contains.class, new ContainsTranslator());
        CONSTRAINT_TRANSLATORS.put(StartsWith.class, new StartsWithTranslator());
        CONSTRAINT_TRANSLATORS.put(EndsWith.class, new EndsWithTranslator());
        CONSTRAINT_TRANSLATORS.put(IsTrue.class, new IsTrueTranslator());
        CONSTRAINT_TRANSLATORS.put(IsFalse.class, new IsFalseTranslator());
        CONSTRAINT_TRANSLATORS.put(IsNull.class, new IsNullTranslator());
        CONSTRAINT_TRANSLATORS.put(IsNotNull.class, new IsNotNullTranslator());
        CONSTRAINT_TRANSLATORS.put(InRange.class, new InRangeTranslator());
        CONSTRAINT_TRANSLATORS.put(ReferenceHavingAttribute.class, new ReferenceHavingAttributeTranslator());

        // price
        CONSTRAINT_TRANSLATORS.put(PriceInCurrency.class, new PriceInCurrencyTranslator());
        CONSTRAINT_TRANSLATORS.put(PriceInPriceLists.class, new PriceInPriceListsTranslator());
        CONSTRAINT_TRANSLATORS.put(PriceValidIn.class, new PriceValidInTranslator());
        CONSTRAINT_TRANSLATORS.put(PriceBetween.class, new PriceBetweenTranslator());

        // hierarchy
        CONSTRAINT_TRANSLATORS.put(WithinHierarchy.class, new WithinHierarchyTranslator());
        CONSTRAINT_TRANSLATORS.put(WithinRootHierarchy.class, new WithinRootHierarchyTranslator());
        CONSTRAINT_TRANSLATORS.put(Excluding.class, new ExcludingTranslator());
        CONSTRAINT_TRANSLATORS.put(ExcludingRoot.class, new ExcludingRootTranslator());
        CONSTRAINT_TRANSLATORS.put(DirectRelation.class, new DirectRelationTranslator());

        // facet
        CONSTRAINT_TRANSLATORS.put(Facet.class, new FacetTranslator());
        CONSTRAINT_TRANSLATORS.put(UserFilter.class, new UserFilterTranslator());
    }

    /**
     * Context of translation. Used by individual translators.
     */
    private final FilterTranslatingContext ctx;

    /**
     * Creates new translating visitor for translating filter constraints. Used only internally.
     *
     * @param collectionContext queried collection's context
     * @param mode in which mode it will be operating
     * @param locale Locale of query.
     * @param now Datetime of query request.
     * @param priceMode price mode of query request.
     * @param facetGroupsConjunction Custom inter facet groups logic from query
     * @param facetGroupsDisjunction Custom inter facet groups logic from query
     * @param facetGroupsNegation List of negated facet groups
     */
    private FilterConstraintTranslatingVisitor(@Nonnull EntityCollectionContext collectionContext,
                                               @Nonnull Mode mode,
                                               @Nullable Locale locale,
                                               @Nonnull ZonedDateTime now,
                                               @Nullable QueryPriceMode priceMode,
                                               @Nullable List<FacetGroupsConjunction> facetGroupsConjunction,
                                               @Nullable List<FacetGroupsDisjunction> facetGroupsDisjunction,
                                               @Nullable List<FacetGroupsNegation> facetGroupsNegation) {
        this.ctx = new FilterTranslatingContext(
                collectionContext,
                mode,
                locale,
                now,
                priceMode,
                facetGroupsConjunction,
                facetGroupsDisjunction,
                facetGroupsNegation
        );
    }

    /**
     * Creates new translating visitor translating whole query
     *
     * @param collectionContext queried collection's context
     * @param request request to gather additional metadata
     */
    public static FilterConstraintTranslatingVisitor withDefaultMode(@Nonnull EntityCollectionContext collectionContext,
                                                                     @Nonnull SqlEvitaRequest request) {
        return new FilterConstraintTranslatingVisitor(
                collectionContext,
                Mode.DEFAULT,
                request.getLanguage(),
                request.getAlignedNow(),
                request.getRequiredPriceMode(),
                findRequires(request.getQuery(), FacetGroupsConjunction.class),
                findRequires(request.getQuery(), FacetGroupsDisjunction.class),
                findRequires(request.getQuery(), FacetGroupsNegation.class)
        );
    }

    /**
     * Creates new translating visitor translating only baseline part of query.
     *
     * @param collectionContext context of queried collection
     * @param request to gather additional metadata
     */
    public static FilterConstraintTranslatingVisitor withBaselineMode(@Nonnull EntityCollectionContext collectionContext,
                                                                      @Nonnull SqlEvitaRequest request) {
        return new FilterConstraintTranslatingVisitor(
                collectionContext,
                Mode.BASELINE,
                request.getLanguage(),
                request.getAlignedNow(),
                request.getRequiredPriceMode(),
                null,
                null,
                null
        );
    }

    @Override
    public void visit(@Nonnull FilterConstraint constraint) {
        final FilterConstraintTranslator<FilterConstraint> translator = getConstraintTranslator(constraint);

        final SqlPart sqlPart;

        if (constraint instanceof ConstraintContainer) {
            @SuppressWarnings("unchecked") final ConstraintContainer<FilterConstraint> container = (ConstraintContainer<FilterConstraint>) constraint;

            // create new level for translated constraints
            ctx.getSqlPartsStack().push(new LinkedList<>());

            // set up context for reference attributes filtering
            if (container instanceof ReferenceHavingAttribute) {
                if (ctx.getReferenceSchema() != null) {
                    throw new IllegalStateException("There cannot be nested referenceHavingAttribute constraints.");
                }
                final Serializable referencedType = ((ReferenceHavingAttribute) container).getEntityType();
                final ReferenceSchema referenceSchema = ctx.getEntitySchema().getReference(referencedType);
                if (referenceSchema == null) {
                    throw new IllegalArgumentException("No schema for referenced type \"" + referencedType + "\" found.");
                }
                ctx.setReferenceSchema(referenceSchema);
            }

            // translate inner constraints
            for (FilterConstraint innerConstraint : container.getConstraints()) {
                innerConstraint.accept(this);
            }

            // discard context for reference attributes filtering
            if (container instanceof ReferenceHavingAttribute) {
                ctx.setReferenceSchema(null);
            }

            sqlPart = translator.translate(constraint, ctx);

            ctx.getSqlPartsStack().pop();
        } else if (constraint instanceof ConstraintLeaf) {
            sqlPart = translator.translate(constraint, ctx);
        } else {
            throw new IllegalStateException("Unknown constraint type. Should never happen!");
        }

        if (sqlPart == null) {
            return;
        }

        if (ctx.getSqlPartsStack().isEmpty()) {
            ctx.setFinalWhereSqlPart(sqlPart);
        } else {
            ctx.getCurrentLevelConditions().add(sqlPart);
        }
    }

    /**
     * Returns final SQL part. Should be called only after whole constraints tree has been visited otherwise
     * it returns empty condition placeholder.
     */
    @Nonnull
    public SqlPart getFinalWhereSqlPart() {
        return ofNullable(ctx.getFinalWhereSqlPart()).orElse(SqlPart.TRUE);
    }

    @Nonnull
    public SqlWithClause getFinalSqlWithClause() {
        return ctx.buildFinalWithClause();
    }

    /**
     * Finds appropriate translator for specified constraint
     *
     * @param constraint constraint to translate
     * @return found translator
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private <C extends FilterConstraint> FilterConstraintTranslator<C> getConstraintTranslator(C constraint) {
        return (FilterConstraintTranslator<C>) ofNullable(CONSTRAINT_TRANSLATORS
                .get(constraint.getClass()))
                .orElseThrow(() -> new IllegalStateException("Could not find translator for constraint \"" + constraint.getName() + "\"."));
    }

    /**
     * Specifies how the visitor will treat passed constraint tree
     */
    public enum Mode {
        /**
         * Translates whole filter constraint tree
         */
        DEFAULT,

        /**
         * Translates whole filter constraint tree without {@link UserFilter} sub-tree
         */
        BASELINE
    }
}
