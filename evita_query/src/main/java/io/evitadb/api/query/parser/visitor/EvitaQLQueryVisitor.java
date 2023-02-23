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

package io.evitadb.api.query.parser.visitor;

import io.evitadb.api.query.*;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.head.Entities;
import io.evitadb.api.query.order.OrderBy;
import io.evitadb.api.query.parser.exception.EvitaQLQueryParsingFailedException;
import io.evitadb.api.query.parser.grammar.EvitaQLBaseVisitor;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.grammar.EvitaQLVisitor;
import io.evitadb.api.query.require.Require;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EvitaQLVisitor} for parsing top level query ({@link Query}).
 * This visitor is meant to be used when you need to parse whole query from string.
 *
 * @see EvitaQLConstraintVisitor
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public class EvitaQLQueryVisitor extends EvitaQLBaseVisitor<Query> {

    protected final EvitaQLConstraintVisitor constraintVisitor = new EvitaQLConstraintVisitor();


    @Override
    public Query visitQuery(EvitaQLParser.QueryContext ctx) {
        final List<Constraint<?>> constraints = ctx.args.constraints
                .stream()
                .map(con -> con.accept(constraintVisitor))
                .collect(Collectors.toList());

        final Entities entitiesConstraint = findEntitiesConstraint(constraints);
        final FilterBy filterByConstraint = findFilterByConstraint(constraints);
        final OrderBy orderByConstraint = findOrderByConstraint(constraints);
        final Require requireConstraint = findRequireConstraint(constraints);

        return Query.query(
                entitiesConstraint,
                filterByConstraint,
                orderByConstraint,
                requireConstraint
        );
    }

    /**
     * Tries to find single {@link Entities} constraint in top level constraints
     *
     * @param topLevelConstraints top level constraints directly from query args
     * @return found {@link Entities}
     * @throws EvitaQLQueryParsingFailedException if no appropriated constraint found
     */
    protected Entities findEntitiesConstraint(List<Constraint<?>> topLevelConstraints) {
        final List<Constraint<?>> headConstraints = topLevelConstraints.stream()
                .filter(HeadConstraint.class::isInstance)
                .collect(Collectors.toList());

        if ((headConstraints.size() != 1) || !(headConstraints.get(0) instanceof Entities)) {
            throw new EvitaQLQueryParsingFailedException("Query can have only one top level head constraint -> \"entities\".");
        }
        return (Entities) headConstraints.get(0);
    }

    /**
     * Tries to find single {@link FilterBy} constraint in top level constraints
     *
     * @param topLevelConstraints top level constraints directly from query args
     * @return {@code null} if not appropriated constraint found or found {@link FilterBy}
     * @throws EvitaQLQueryParsingFailedException if there is more top level filter constraints or found constraint is not appropriated type
     */
    protected FilterBy findFilterByConstraint(List<Constraint<?>> topLevelConstraints) {
        final List<Constraint<?>> filterConstraints = topLevelConstraints.stream()
                .filter(FilterConstraint.class::isInstance)
                .collect(Collectors.toList());

        if (filterConstraints.isEmpty()) {
            return null;
        }
        if ((filterConstraints.size() > 1) || !(filterConstraints.get(0) instanceof FilterBy)) {
            throw new EvitaQLQueryParsingFailedException("Query can have only one top level filter constraint -> \"filterBy\".");
        }
        return (FilterBy) filterConstraints.get(0);
    }

    /**
     * Tries to find single {@link OrderBy} constraint in top level constraints
     *
     * @param topLevelConstraints top level constraints directly from query args
     * @return {@code null} if not appropriated constraint found or found {@link OrderBy}
     * @throws EvitaQLQueryParsingFailedException if there is more top level order constraints or found constraint is not appropriated type
     */
    protected OrderBy findOrderByConstraint(List<Constraint<?>> topLevelConstraints) {
        final List<Constraint<?>> orderConstraints = topLevelConstraints.stream()
                .filter(OrderConstraint.class::isInstance)
                .collect(Collectors.toList());

        if (orderConstraints.isEmpty()) {
            return null;
        }
        if ((orderConstraints.size() > 1) || !(orderConstraints.get(0) instanceof OrderBy)) {
            throw new EvitaQLQueryParsingFailedException("Query can have only one top level order constraint -> \"orderBy\".");
        }
        return (OrderBy) orderConstraints.get(0);
    }

    /**
     * Tries to find single {@link Require} constraint in top level constraints
     *
     * @param topLevelConstraints top level constraints directly from query args
     * @return {@code null} if not appropriated constraint found or found {@link Require}
     * @throws EvitaQLQueryParsingFailedException if there is more top level require constraints or found constraint is not appropriated type
     */
    protected Require findRequireConstraint(List<Constraint<?>> topLevelConstraints) {
        final List<Constraint<?>> requireConstraints = topLevelConstraints.stream()
                .filter(RequireConstraint.class::isInstance)
                .collect(Collectors.toList());

        if (requireConstraints.isEmpty()) {
            return null;
        }
        if ((requireConstraints.size() > 1) || !(requireConstraints.get(0) instanceof Require)) {
            throw new EvitaQLQueryParsingFailedException("Query can have only one top level require constraint -> \"require\".");
        }
        return (Require) requireConstraints.get(0);
    }
}
