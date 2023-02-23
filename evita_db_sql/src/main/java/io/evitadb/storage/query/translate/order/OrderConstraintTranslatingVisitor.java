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

import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.order.*;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.query.SqlSortExpression;
import io.evitadb.storage.query.translate.order.translator.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Implementation of {@link ConstraintVisitor} for translating order constraints to {@link SqlSortExpression}s.
 * Root of constraints tree must be {@link FilterBy} constraint and final output is single {@link SqlSortExpression}
 * containing whole translated tree.
 *
 * @author Lukáš Hornych 2021
 */
public class OrderConstraintTranslatingVisitor implements ConstraintVisitor<OrderConstraint> {

    /**
     * Available constraint translators.
     */
    private static final Map<Class<? extends OrderConstraint>, OrderConstraintTranslator<? extends OrderConstraint>> CONSTRAINT_TRANSLATORS = new HashMap<>();
    static {
        CONSTRAINT_TRANSLATORS.put(OrderBy.class, new OrderByTranslator());
        CONSTRAINT_TRANSLATORS.put(Ascending.class, new AscendingTranslator());
        CONSTRAINT_TRANSLATORS.put(Descending.class, new DescendingTranslator());
        CONSTRAINT_TRANSLATORS.put(Random.class, new RandomTranslator());
        CONSTRAINT_TRANSLATORS.put(ReferenceAttribute.class, new ReferenceAttributeTranslator());
        CONSTRAINT_TRANSLATORS.put(PriceAscending.class, new PriceAscendingTranslator());
        CONSTRAINT_TRANSLATORS.put(PriceDescending.class, new PriceDescendingTranslator());
    }

    private final OrderTranslatingContext ctx;

    /**
     * Creates new translating visitor for translating order constraints
     *
     * @param collectionContext context of queried collection
     * @param locale Locale of query.
     */
    public OrderConstraintTranslatingVisitor(@Nonnull EntityCollectionContext collectionContext, @Nullable Locale locale) {
        this.ctx = new OrderTranslatingContext(collectionContext, locale);
    }

    @Override
    public void visit(@Nonnull OrderConstraint constraint) {
        final OrderConstraintTranslator<OrderConstraint> translator = getConstraintTranslator(constraint);

        final SqlSortExpression sqlSortExpression;
        if (constraint instanceof ConstraintContainer) {
            @SuppressWarnings("unchecked") final ConstraintContainer<OrderConstraint> container = (ConstraintContainer<OrderConstraint>) constraint;

            // create new level for translated constraints
            ctx.getSqlSortExpressionsStack().push(new LinkedList<>());

            // set up context for reference attributes ordering
            if (container instanceof ReferenceAttribute) {
                if (ctx.getReferenceSchema() != null) {
                    throw new IllegalStateException("There cannot be nested referenceAttribute constraints.");
                }
                ctx.setReferenceSchema(ctx.getEntitySchema().getReference(((ReferenceAttribute) container).getEntityType()));
                ctx.setReferenceSortCount(ctx.getReferenceSortCount() + 1);
            }

            for (OrderConstraint innerConstraint : container.getConstraints()) {
                innerConstraint.accept(this);
            }

            // discard context for reference attributes ordering
            if (container instanceof ReferenceAttribute) {
                ctx.setReferenceSchema(null);
            }

            sqlSortExpression = translator.translate(constraint, ctx);

            ctx.getSqlSortExpressionsStack().pop();
        } else if (constraint instanceof ConstraintLeaf) {
            sqlSortExpression = translator.translate(constraint, ctx);
        } else {
            throw new IllegalStateException("Unknown constraint type. Should never happen!");
        }

        if (constraint instanceof OrderBy) {
            ctx.setFinalSqlSortExpression(new SqlSortExpression(sqlSortExpression.getJoinSql(), sqlSortExpression.getSql(), sqlSortExpression.getArgs()));
        } else {
            ctx.getCurrentLevelExpressions().add(sqlSortExpression);
        }
    }

    /**
     * Returns final SQL sort expression. Should be called only after whole constraints tree has been visited otherwise
     * it returns empty expression placeholder.
     */
    @Nonnull
    public SqlSortExpression getFinalSqlSortExpression() {
        return ofNullable(ctx.getFinalSqlSortExpression()).orElse(SqlSortExpression.EMPTY);
    }

    /**
     * Finds appropriate translator for specified constraint
     *
     * @param constraint constraint to translate
     * @return found translator
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private <C extends OrderConstraint> OrderConstraintTranslator<C> getConstraintTranslator(C constraint) {
        return (OrderConstraintTranslator<C>) ofNullable(CONSTRAINT_TRANSLATORS
                .get(constraint.getClass()))
                .orElseThrow(() -> new IllegalStateException("Could not find translator for constraint \"" + constraint.getName() + "\"."));
    }
}
