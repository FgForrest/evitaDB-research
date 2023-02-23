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
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.query.parser.EvitaQLLiteralType;
import io.evitadb.api.query.parser.grammar.EvitaQLBaseVisitor;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.grammar.EvitaQLParser.HierarchyStatisticsConstraintContext;
import io.evitadb.api.query.parser.grammar.EvitaQLVisitor;
import io.evitadb.api.query.require.*;

import java.io.Serializable;
import java.util.Locale;

/**
 * Implementation of {@link EvitaQLVisitor} for parsing all require type constraints
 * ({@link RequireConstraint}).
 * This visitor should not be used directly if not needed instead use generic {@link EvitaQLConstraintVisitor}.
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 * @see EvitaQLConstraintVisitor
 */
public class EvitaQLRequireConstraintVisitor extends EvitaQLBaseVisitor<Constraint<?>> {

	protected final EvitaQLLiteralVisitor allTypesLiteralVisitor = EvitaQLLiteralVisitor.withAllTypesAllowed();
	protected final EvitaQLLiteralVisitor enumLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.ENUM);
	protected final EvitaQLIdentifierVisitor identifierVisitor = new EvitaQLIdentifierVisitor();
	protected final EvitaQLLiteralVisitor intLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.INT);
	protected final EvitaQLLiteralVisitor literalVisitorWithComparableTypesAllowed = EvitaQLLiteralVisitor.withComparableTypesAllowed();
	protected final EvitaQLLiteralVisitor localeLiteralVisitor = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.LOCALE);


	@Override
	public Constraint<?> visitRequireContainerConstraint(EvitaQLParser.RequireContainerConstraintContext ctx) {
		return new Require(
			ctx.args.constraints
				.stream()
				.map(hc -> (RequireConstraint) hc.accept(this))
				.toArray(RequireConstraint[]::new)
		);
	}

	@Override
	public Constraint<?> visitPageConstraint(EvitaQLParser.PageConstraintContext ctx) {
		final int pageNumber = ctx.args.pageNumber.accept(intLiteralVisitor).asLong().intValue();
		final int pageSize = ctx.args.pageSize.accept(intLiteralVisitor).asLong().intValue();

		return new Page(pageNumber, pageSize);
	}

	@Override
	public Constraint<?> visitStripConstraint(EvitaQLParser.StripConstraintContext ctx) {
		final int offset = ctx.args.offset.accept(intLiteralVisitor).asLong().intValue();
		final int limit = ctx.args.limit.accept(intLiteralVisitor).asLong().intValue();

		return new Strip(offset, limit);
	}

	@Override
	public Constraint<?> visitEntityBodyConstraint(EvitaQLParser.EntityBodyConstraintContext ctx) {
		return new EntityBody();
	}

	@Override
	public Constraint<?> visitAttributesConstraint(EvitaQLParser.AttributesConstraintContext ctx) {
		return new Attributes();
	}

	@Override
	public Constraint<?> visitPricesConstraint(EvitaQLParser.PricesConstraintContext ctx) {
		if (ctx.args == null) {
			return new Prices();
		}
		return new Prices(
			ctx.args.value
				.accept(enumLiteralVisitor)
				.asEnumWrapper()
				.toEnum(PriceFetchMode.class)
		);
	}

	@Override
	public Constraint<?> visitAssociatedDataConstraint(EvitaQLParser.AssociatedDataConstraintContext ctx) {
		if (ctx.args == null) {
			return new AssociatedData();
		} else {
			return new AssociatedData(
				ctx.args.names
					.stream()
					.map(n -> n.accept(identifierVisitor))
					.toArray(String[]::new)
			);
		}
	}

	@Override
	public Constraint<?> visitReferencesConstraint(EvitaQLParser.ReferencesConstraintContext ctx) {
		if (ctx.args == null) {
			return new References();
		} else {
			return new References(
				ctx.args.referencedTypes
					.stream()
					.map(n -> n.accept(allTypesLiteralVisitor).getValue())
					.toArray(Serializable[]::new)
			);
		}
	}

	@Override
	public Constraint<?> visitUseOfPriceConstraint(EvitaQLParser.UseOfPriceConstraintContext ctx) {
		return new UseOfPrice(
			ctx.args.value
				.accept(enumLiteralVisitor)
				.asEnumWrapper()
				.toEnum(QueryPriceMode.class)
		);
	}

	@Override
	public Constraint<?> visitDataInLanguageConstraint(EvitaQLParser.DataInLanguageConstraintContext ctx) {
		if (ctx.args == null) {
			return new DataInLanguage();
		} else {
			return new DataInLanguage(
				ctx.args.values
					.stream()
					.map(v -> v.accept(localeLiteralVisitor).asLocale())
					.toArray(Locale[]::new)
			);
		}
	}

	@Override
	public Constraint<?> visitParentsConstraint(EvitaQLParser.ParentsConstraintContext ctx) {
		if (ctx.args == null) {
			return new Parents();
		}
		return new Parents(
			ctx.args.constraints
				.stream()
				.map(n -> (EntityContentRequire) n.accept(this))
				.toArray(EntityContentRequire[]::new)
		);
	}

	@Override
	public Constraint<?> visitParentsOfTypeConstraint(EvitaQLParser.ParentsOfTypeConstraintContext ctx) {
		return new ParentsOfType(
			ctx.args.entityTypes
				.stream()
				.map(n -> n.accept(allTypesLiteralVisitor).getValue())
				.toArray(Serializable[]::new),
			ctx.args.requireConstraints
				.stream()
				.map(n -> (EntityContentRequire) n.accept(this))
				.toArray(EntityContentRequire[]::new)
		);
	}

	@Override
	public Constraint<?> visitFacetSummaryConstraint(EvitaQLParser.FacetSummaryConstraintContext ctx) {
		if (ctx.args == null) {
			return new FacetSummary();
		}
		return new FacetSummary(
			ctx.args.value
				.accept(enumLiteralVisitor)
				.asEnumWrapper()
				.toEnum(FacetStatisticsDepth.class)
		);
	}

	@Override
	public Constraint<?> visitFacetGroupsConjunctionConstraint(EvitaQLParser.FacetGroupsConjunctionConstraintContext ctx) {
		return new FacetGroupsConjunction(
			ctx.args.entityType.accept(literalVisitorWithComparableTypesAllowed).getValue(),
			ctx.args.values
				.stream()
				.map(v -> v.accept(intLiteralVisitor).asLong().intValue())
				.toArray(Integer[]::new)
		);
	}

	@Override
	public Constraint<?> visitFacetGroupsDisjunctionConstraint(EvitaQLParser.FacetGroupsDisjunctionConstraintContext ctx) {
		return new FacetGroupsDisjunction(
			ctx.args.entityType.accept(literalVisitorWithComparableTypesAllowed).getValue(),
			ctx.args.values
				.stream()
				.map(v -> v.accept(intLiteralVisitor).asLong().intValue())
				.toArray(Integer[]::new)
		);
	}

	@Override
	public Constraint<?> visitFacetGroupsNegationConstraint(EvitaQLParser.FacetGroupsNegationConstraintContext ctx) {
		return new FacetGroupsNegation(
			ctx.args.entityType.accept(literalVisitorWithComparableTypesAllowed).getValue(),
			ctx.args.values
				.stream()
				.map(v -> v.accept(intLiteralVisitor).asLong().intValue())
				.toArray(Integer[]::new)
		);
	}

	@Override
	public Constraint<?> visitAttributeHistogramConstraint(EvitaQLParser.AttributeHistogramConstraintContext ctx) {
		return new AttributeHistogram(
			ctx.args.value.accept(intLiteralVisitor).asLong().intValue(),
			ctx.args.names
				.stream()
				.map(n -> n.accept(identifierVisitor))
				.toArray(String[]::new)
		);
	}

	@Override
	public Constraint<?> visitPriceHistogramConstraint(EvitaQLParser.PriceHistogramConstraintContext ctx) {
		return new PriceHistogram(ctx.args.value.accept(intLiteralVisitor).asLong().intValue());
	}

	@Override
	public Constraint<?> visitHierarchyStatisticsConstraint(HierarchyStatisticsConstraintContext ctx) {
		return new HierarchyStatistics(
			ctx.args.entityType.accept(allTypesLiteralVisitor).getValue(),
			ctx.args.constrains
				.stream()
				.map(c -> (EntityContentRequire) c.accept(this))
				.toArray(EntityContentRequire[]::new)
		);
	}
}
