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
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.parser.EvitaQLLiteral;
import io.evitadb.api.query.parser.EvitaQLLiteralType;
import io.evitadb.api.query.parser.exception.EvitaQLConstraintParsingFailedException;
import io.evitadb.api.query.parser.grammar.EvitaQLBaseVisitor;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.grammar.EvitaQLVisitor;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EvitaQLVisitor} for parsing all filter type constraints
 * ({@link FilterConstraint}).
 * This visitor should not be used directly if not needed instead use generic {@link EvitaQLConstraintVisitor}.
 *
 * @see EvitaQLConstraintVisitor
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public class EvitaQLFilterConstraintVisitor extends EvitaQLBaseVisitor<Constraint<?>> {

    protected final EvitaQLIdentifierVisitor identifierVisitor = new EvitaQLIdentifierVisitor();
    protected final EvitaQLLiteralVisitor comparableTypesLiteralVisitor = EvitaQLLiteralVisitor.withComparableTypesAllowed();
    protected final EvitaQLLiteralVisitor allTypesLiteralVisitor = EvitaQLLiteralVisitor.withAllTypesAllowed();
    protected final EvitaQLLiteralVisitor stringLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.STRING);
    protected final EvitaQLLiteralVisitor intLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.INT);
    protected final EvitaQLLiteralVisitor inRangeLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.INT, EvitaQLLiteralType.FLOAT, EvitaQLLiteralType.ZONED_DATE_TIME);
    protected final EvitaQLLiteralVisitor floatLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.FLOAT);
    protected final EvitaQLLiteralVisitor zonedDateTimeLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.ZONED_DATE_TIME);
    protected final EvitaQLLiteralVisitor localeLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.LOCALE);


    @Override
    public Constraint<?> visitFilterByConstraint(EvitaQLParser.FilterByConstraintContext ctx) {
        if (ctx.args.constraints.size() > 1) {
            throw new EvitaQLConstraintParsingFailedException("Filter constraint \"filterBy\" has to have one child filter constraint.");
        }

        return new FilterBy((FilterConstraint) ctx.args.constraints.get(0).accept(this));
    }

    @Override
    public Constraint<?> visitAndConstraint(EvitaQLParser.AndConstraintContext ctx) {
        return new And(
                ctx.args.constraints
                        .stream()
                        .map(fc -> (FilterConstraint) fc.accept(this))
                        .toArray(FilterConstraint[]::new)
        );
    }

    @Override
    public Constraint<?> visitOrConstraint(EvitaQLParser.OrConstraintContext ctx) {
        return new Or(
                ctx.args.constraints
                        .stream()
                        .map(fc -> (FilterConstraint) fc.accept(this))
                        .toArray(FilterConstraint[]::new)
        );
    }

    @Override
    public Constraint<?> visitNotConstraint(EvitaQLParser.NotConstraintContext ctx) {
        if (ctx.args.constraints.size() > 1) {
            throw new EvitaQLConstraintParsingFailedException("Filter constraint \"not\" has to have one child filter constraint.");
        }

        return new Not((FilterConstraint) ctx.args.constraints.get(0).accept(this));
    }

    @Override
    public Constraint<?> visitUserFilterConstraint(EvitaQLParser.UserFilterConstraintContext ctx) {
        return new UserFilter(
                ctx.args.constraints
                        .stream()
                        .map(fc -> (FilterConstraint) fc.accept(this))
                        .toArray(FilterConstraint[]::new)
        );
    }

    @Override
    public Constraint<?> visitEqualsConstraint(EvitaQLParser.EqualsConstraintContext ctx) {
        return new Equals(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(comparableTypesLiteralVisitor).asSerializableAndComparable()
        );
    }

    @Override
    public Constraint<?> visitGreaterThanConstraint(EvitaQLParser.GreaterThanConstraintContext ctx) {
        return new GreaterThan(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(comparableTypesLiteralVisitor).asSerializableAndComparable()
        );
    }

    @Override
    public Constraint<?> visitGreaterThanEqualsConstraint(EvitaQLParser.GreaterThanEqualsConstraintContext ctx) {
        return new GreaterThanEquals(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(comparableTypesLiteralVisitor).asSerializableAndComparable()
        );
    }

    @Override
    public Constraint<?> visitLessThanConstraint(EvitaQLParser.LessThanConstraintContext ctx) {
        return new LessThan(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(comparableTypesLiteralVisitor).asSerializableAndComparable()
        );
    }

    @Override
    public Constraint<?> visitLessThanEqualsConstraint(EvitaQLParser.LessThanEqualsConstraintContext ctx) {
        return new LessThanEquals(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(comparableTypesLiteralVisitor).asSerializableAndComparable()
        );
    }

    @Override
    public Constraint<?> visitBetweenConstraint(EvitaQLParser.BetweenConstraintContext ctx) {
        return new Between(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.valueFrom
                        .accept(comparableTypesLiteralVisitor)
                        .asSerializableAndComparable(),
                ctx.args.valueTo
                        .accept(comparableTypesLiteralVisitor)
                        .asSerializableAndComparable()
        );
    }

    @Override
    public Constraint<?> visitInSetConstraint(EvitaQLParser.InSetConstraintContext ctx) {
        final List<EvitaQLLiteral> literals = ctx.args.values
                .stream()
                .map(v -> v.accept(comparableTypesLiteralVisitor))
                .collect(Collectors.toList());

        return new InSet(
                ctx.args.name.accept(identifierVisitor),
                literalsToAttributeValues(literals)
        );
    }

    @Override
    public Constraint<?> visitContainsConstraint(EvitaQLParser.ContainsConstraintContext ctx) {
        return new Contains(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(stringLiteralVisitor).getValue()
        );
    }

    @Override
    public Constraint<?> visitStartsWithConstraint(EvitaQLParser.StartsWithConstraintContext ctx) {
        return new StartsWith(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(stringLiteralVisitor).getValue()
        );
    }

    @Override
    public Constraint<?> visitEndsWithConstraint(EvitaQLParser.EndsWithConstraintContext ctx) {
        return new EndsWith(
                ctx.args.name.accept(identifierVisitor),
                ctx.args.value.accept(stringLiteralVisitor).getValue()
        );
    }

    @Override
    public Constraint<?> visitIsTrueConstraint(EvitaQLParser.IsTrueConstraintContext ctx) {
        return new IsTrue(ctx.args.name.accept(identifierVisitor));
    }

    @Override
    public Constraint<?> visitIsFalseConstraint(EvitaQLParser.IsFalseConstraintContext ctx) {
        return new IsFalse(ctx.args.name.accept(identifierVisitor));
    }

    @Override
    public Constraint<?> visitIsNullConstraint(EvitaQLParser.IsNullConstraintContext ctx) {
        return new IsNull(ctx.args.name.accept(identifierVisitor));
    }

    @Override
    public Constraint<?> visitIsNotNullConstraint(EvitaQLParser.IsNotNullConstraintContext ctx) {
        return new IsNotNull(ctx.args.name.accept(identifierVisitor));
    }

    @Override
    public Constraint<?> visitInRangeConstraint(EvitaQLParser.InRangeConstraintContext ctx) {
        final String attributeName = ctx.args.name.accept(identifierVisitor);
        final EvitaQLLiteral attributeValue = ctx.args.value.accept(inRangeLiteralVisitor);

        if (Number.class.isAssignableFrom(attributeValue.getType())) {
            return new InRange(attributeName, attributeValue.asNumber());
        } else if (ZonedDateTime.class.isAssignableFrom(attributeValue.getType())) {
            return new InRange(attributeName, attributeValue.asZonedDateTime());
        } else {
            throw new EvitaQLConstraintParsingFailedException("Filter constraint \"inRange\" only supports number and date time values.");
        }
    }

    @Override
    public Constraint<?> visitPrimaryKeyConstraint(EvitaQLParser.PrimaryKeyConstraintContext ctx) {
        return new PrimaryKey(
                ctx.args.values
                        .stream()
                        .map(v -> v.accept(intLiteralVisitor).asLong().intValue())
                        .toArray(Integer[]::new)
        );
    }

    @Override
    public Constraint<?> visitLanguageConstraint(EvitaQLParser.LanguageConstraintContext ctx) {
        return new Language(
                ctx.args.value
                        .accept(localeLiteralVisitor)
                        .getValue()
        );
    }

    @Override
    public Constraint<?> visitPriceInCurrencyConstraint(EvitaQLParser.PriceInCurrencyConstraintContext ctx) {
        return new PriceInCurrency(
            (String)ctx.args.value
                        .accept(stringLiteralVisitor)
                        .getValue()
        );
    }

    @Override
    public Constraint<?> visitPriceInPriceListsConstraints(EvitaQLParser.PriceInPriceListsConstraintsContext ctx) {
        return new PriceInPriceLists(
                ctx.args.values
                        .stream()
                        .map(v -> v.accept(allTypesLiteralVisitor).getValue())
                        .toArray(Serializable[]::new)
        );
    }

    @Override
    public Constraint<?> visitPriceValidInConstraint(EvitaQLParser.PriceValidInConstraintContext ctx) {
        return new PriceValidIn(
                ctx.args.value
                        .accept(zonedDateTimeLiteralVisitor)
                        .getValue()
        );
    }

    @Override
    public Constraint<?> visitPriceBetweenConstraint(EvitaQLParser.PriceBetweenConstraintContext ctx) {
        final BigDecimal from = ctx.args.valueFrom.accept(floatLiteralVisitor).getValue();
        final BigDecimal to = ctx.args.valueTo.accept(floatLiteralVisitor).getValue();

        return new PriceBetween(from, to);
    }

    @Override
    public Constraint<?> visitFacetConstraint(EvitaQLParser.FacetConstraintContext ctx) {
        return new Facet(
                ctx.args.entityType.accept(allTypesLiteralVisitor).getValue(),
                ctx.args.values
                        .stream()
                        .map(v -> v.accept(intLiteralVisitor).asLong().intValue())
                        .toArray(Integer[]::new)
        );
    }

    @Override
    public Constraint<?> visitReferenceHavingAttributeConstraint(EvitaQLParser.ReferenceHavingAttributeConstraintContext ctx) {
        return new ReferenceHavingAttribute(
                ctx.args.entityType.accept(allTypesLiteralVisitor).getValue(),
                (FilterConstraint) ctx.args.filterConstraint().accept(this)
        );
    }

    @Override
    public Constraint<?> visitWithinHierarchyConstraint(EvitaQLParser.WithinHierarchyConstraintContext ctx) {
        final int primaryKey = ctx.args.primaryKey.accept(intLiteralVisitor).asLong().intValue();
        final HierarchySpecificationFilterConstraint[] constraints = ctx.args.constrains
            .stream()
            .map(c -> (HierarchySpecificationFilterConstraint) c.accept(this))
            .toArray(HierarchySpecificationFilterConstraint[]::new);

        if (ctx.args.entityType == null) {
            return new WithinHierarchy(primaryKey, constraints);
        }
        return new WithinHierarchy(
            ctx.args.entityType.accept(allTypesLiteralVisitor).getValue(),
            primaryKey,
            constraints
        );
    }

    @Override
    public Constraint<?> visitWithinRootHierarchyConstraint(EvitaQLParser.WithinRootHierarchyConstraintContext ctx) {
        final HierarchySpecificationFilterConstraint[] constraints = ctx.args.constrains
            .stream()
            .map(c -> (HierarchySpecificationFilterConstraint) c.accept(this))
            .toArray(HierarchySpecificationFilterConstraint[]::new);

        if (ctx.args.entityType == null) {
            return new WithinRootHierarchy(constraints);
        }
        return new WithinRootHierarchy(
                ctx.args.entityType.accept(allTypesLiteralVisitor).getValue(),
                constraints
        );
    }

    @Override
    public Constraint<?> visitDirectRelationConstraint(EvitaQLParser.DirectRelationConstraintContext ctx) {
        return new DirectRelation();
    }

    @Override
    public Constraint<?> visitExcludingRootConstraint(EvitaQLParser.ExcludingRootConstraintContext ctx) {
        return new ExcludingRoot();
    }

    @Override
    public Constraint<?> visitExcludingConstraint(EvitaQLParser.ExcludingConstraintContext ctx) {
        return new Excluding(
                ctx.args.values
                        .stream()
                        .map(v -> v.accept(intLiteralVisitor).asLong().intValue())
                        .toArray(Integer[]::new)
        );
    }


    private <T extends Serializable & Comparable<?>> T[] literalsToAttributeValues(List<EvitaQLLiteral> literals) {
        final Class<?> attributeValuesType = literals.get(0).getType();
        for (EvitaQLLiteral attributeValue : literals) {
            if (!attributeValuesType.isAssignableFrom(attributeValue.getType())) {
                throw new EvitaQLConstraintParsingFailedException("All attribute values have to have same literal type.");
            }
        }

        //noinspection unchecked
        return literals.stream()
                .map(v -> (T) v.asSerializableAndComparable())
                .collect(Collectors.toList())
                .toArray((T[]) Array.newInstance(attributeValuesType, 0));
    }
}
