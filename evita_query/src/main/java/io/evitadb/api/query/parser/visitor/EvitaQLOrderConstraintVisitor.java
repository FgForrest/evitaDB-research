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

import io.evitadb.api.query.Constraint;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.order.*;
import io.evitadb.api.query.parser.grammar.EvitaQLBaseVisitor;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.grammar.EvitaQLVisitor;

/**
 * Implementation of {@link EvitaQLVisitor} for parsing all order type constraints
 * ({@link OrderConstraint}).
 * This visitor should not be used directly if not needed instead use generic {@link EvitaQLConstraintVisitor}.
 *
 * @see EvitaQLConstraintVisitor
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public class EvitaQLOrderConstraintVisitor extends EvitaQLBaseVisitor<Constraint<?>> {

    protected final EvitaQLIdentifierVisitor identifierVisitor = new EvitaQLIdentifierVisitor();
    protected final EvitaQLLiteralVisitor allTypesLiteralVisitor = EvitaQLLiteralVisitor.withAllTypesAllowed();


    @Override
    public Constraint<?> visitOrderByConstraint(EvitaQLParser.OrderByConstraintContext ctx) {
        return new OrderBy(
                ctx.args.constraints
                        .stream()
                        .map(oc -> (OrderConstraint) oc.accept(this))
                        .toArray(OrderConstraint[]::new)
        );
    }

    @Override
    public Constraint<?> visitAscendingConstraint(EvitaQLParser.AscendingConstraintContext ctx) {
        return new Ascending(
                ctx.args.name.accept(identifierVisitor)
        );
    }

    @Override
    public Constraint<?> visitDescendingConstraint(EvitaQLParser.DescendingConstraintContext ctx) {
        return new Descending(
                ctx.args.name.accept(identifierVisitor)
        );
    }

    @Override
    public Constraint<?> visitPriceAscendingConstraint(EvitaQLParser.PriceAscendingConstraintContext ctx) {
        return new PriceAscending();
    }

    @Override
    public Constraint<?> visitPriceDescendingConstraint(EvitaQLParser.PriceDescendingConstraintContext ctx) {
        return new PriceDescending();
    }

    @Override
    public Constraint<?> visitRandomConstraint(EvitaQLParser.RandomConstraintContext ctx) {
        return new Random();
    }

    @Override
    public Constraint<?> visitReferenceAttributeConstraint(EvitaQLParser.ReferenceAttributeConstraintContext ctx) {
        return new ReferenceAttribute(
                ctx.args.entityType.accept(allTypesLiteralVisitor).getValue(),
                ctx.args.constrains
                        .stream()
                        .map(c -> (OrderConstraint) c.accept(this))
                        .toArray(OrderConstraint[]::new)
        );
    }
}
