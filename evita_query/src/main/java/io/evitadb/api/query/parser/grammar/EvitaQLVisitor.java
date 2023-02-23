// Generated from EvitaQL.g4 by ANTLR 4.9.2

package io.evitadb.api.query.parser.grammar;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link EvitaQLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface EvitaQLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#root}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoot(EvitaQLParser.RootContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(EvitaQLParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint(EvitaQLParser.ConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code entitiesConstraint}
	 * labeled alternative in {@link EvitaQLParser#headConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntitiesConstraint(EvitaQLParser.EntitiesConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code filterByConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterByConstraint(EvitaQLParser.FilterByConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code andConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndConstraint(EvitaQLParser.AndConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code orConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrConstraint(EvitaQLParser.OrConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotConstraint(EvitaQLParser.NotConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code userFilterConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserFilterConstraint(EvitaQLParser.UserFilterConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code equalsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualsConstraint(EvitaQLParser.EqualsConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code greaterThanConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterThanConstraint(EvitaQLParser.GreaterThanConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code greaterThanEqualsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterThanEqualsConstraint(EvitaQLParser.GreaterThanEqualsConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lessThanConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessThanConstraint(EvitaQLParser.LessThanConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lessThanEqualsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessThanEqualsConstraint(EvitaQLParser.LessThanEqualsConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code betweenConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetweenConstraint(EvitaQLParser.BetweenConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inSetConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInSetConstraint(EvitaQLParser.InSetConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code containsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainsConstraint(EvitaQLParser.ContainsConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code startsWithConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartsWithConstraint(EvitaQLParser.StartsWithConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code endsWithConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndsWithConstraint(EvitaQLParser.EndsWithConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isTrueConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsTrueConstraint(EvitaQLParser.IsTrueConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isFalseConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsFalseConstraint(EvitaQLParser.IsFalseConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isNullConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsNullConstraint(EvitaQLParser.IsNullConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isNotNullConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsNotNullConstraint(EvitaQLParser.IsNotNullConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inRangeConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInRangeConstraint(EvitaQLParser.InRangeConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primaryKeyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryKeyConstraint(EvitaQLParser.PrimaryKeyConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code languageConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLanguageConstraint(EvitaQLParser.LanguageConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceInCurrencyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceInCurrencyConstraint(EvitaQLParser.PriceInCurrencyConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceInPriceListsConstraints}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceInPriceListsConstraints(EvitaQLParser.PriceInPriceListsConstraintsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceValidInConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceValidInConstraint(EvitaQLParser.PriceValidInConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceBetweenConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceBetweenConstraint(EvitaQLParser.PriceBetweenConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code facetConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFacetConstraint(EvitaQLParser.FacetConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code referenceHavingAttributeConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferenceHavingAttributeConstraint(EvitaQLParser.ReferenceHavingAttributeConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code withinHierarchyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithinHierarchyConstraint(EvitaQLParser.WithinHierarchyConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code withinRootHierarchyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithinRootHierarchyConstraint(EvitaQLParser.WithinRootHierarchyConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code directRelationConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectRelationConstraint(EvitaQLParser.DirectRelationConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code excludingRootConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcludingRootConstraint(EvitaQLParser.ExcludingRootConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code excludingConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcludingConstraint(EvitaQLParser.ExcludingConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code orderByConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderByConstraint(EvitaQLParser.OrderByConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ascendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAscendingConstraint(EvitaQLParser.AscendingConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code descendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescendingConstraint(EvitaQLParser.DescendingConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceAscendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceAscendingConstraint(EvitaQLParser.PriceAscendingConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceDescendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceDescendingConstraint(EvitaQLParser.PriceDescendingConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code randomConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRandomConstraint(EvitaQLParser.RandomConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code referenceAttributeConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferenceAttributeConstraint(EvitaQLParser.ReferenceAttributeConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code requireContainerConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRequireContainerConstraint(EvitaQLParser.RequireContainerConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pageConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPageConstraint(EvitaQLParser.PageConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stripConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStripConstraint(EvitaQLParser.StripConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code entityBodyConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityBodyConstraint(EvitaQLParser.EntityBodyConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code attributesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributesConstraint(EvitaQLParser.AttributesConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pricesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPricesConstraint(EvitaQLParser.PricesConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code associatedDataConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssociatedDataConstraint(EvitaQLParser.AssociatedDataConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code referencesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencesConstraint(EvitaQLParser.ReferencesConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code useOfPriceConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseOfPriceConstraint(EvitaQLParser.UseOfPriceConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dataInLanguageConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataInLanguageConstraint(EvitaQLParser.DataInLanguageConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parentsConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentsConstraint(EvitaQLParser.ParentsConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parentsOfTypeConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentsOfTypeConstraint(EvitaQLParser.ParentsOfTypeConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code facetSummaryConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFacetSummaryConstraint(EvitaQLParser.FacetSummaryConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code facetGroupsConjunctionConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFacetGroupsConjunctionConstraint(EvitaQLParser.FacetGroupsConjunctionConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code facetGroupsDisjunctionConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFacetGroupsDisjunctionConstraint(EvitaQLParser.FacetGroupsDisjunctionConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code facetGroupsNegationConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFacetGroupsNegationConstraint(EvitaQLParser.FacetGroupsNegationConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code attributeHistogramConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeHistogramConstraint(EvitaQLParser.AttributeHistogramConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code priceHistogramConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriceHistogramConstraint(EvitaQLParser.PriceHistogramConstraintContext ctx);
	/**
	 * Visit a parse tree produced by the {@code hierarchyStatisticsConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHierarchyStatisticsConstraint(EvitaQLParser.HierarchyStatisticsConstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#constraintListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraintListArgs(EvitaQLParser.ConstraintListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#emptyArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyArgs(EvitaQLParser.EmptyArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#filterConstraintContainerArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterConstraintContainerArgs(EvitaQLParser.FilterConstraintContainerArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#orderConstraintContainerArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderConstraintContainerArgs(EvitaQLParser.OrderConstraintContainerArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#requireConstraintContainerArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRequireConstraintContainerArgs(EvitaQLParser.RequireConstraintContainerArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#nameArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameArgs(EvitaQLParser.NameArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#nameWithValueArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameWithValueArgs(EvitaQLParser.NameWithValueArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#nameWithValueListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameWithValueListArgs(EvitaQLParser.NameWithValueListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#nameWithBetweenValuesArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameWithBetweenValuesArgs(EvitaQLParser.NameWithBetweenValuesArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#valueArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueArgs(EvitaQLParser.ValueArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#valueListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueListArgs(EvitaQLParser.ValueListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#betweenValuesArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetweenValuesArgs(EvitaQLParser.BetweenValuesArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#nameListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameListArgs(EvitaQLParser.NameListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#valueWithNameListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueWithNameListArgs(EvitaQLParser.ValueWithNameListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#referencedTypesArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencedTypesArgs(EvitaQLParser.ReferencedTypesArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#entityTypeArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityTypeArgs(EvitaQLParser.EntityTypeArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#entityTypeListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityTypeListArgs(EvitaQLParser.EntityTypeListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#entityTypeWithValueListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityTypeWithValueListArgs(EvitaQLParser.EntityTypeWithValueListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#entityTypeWithFilterConstraintArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityTypeWithFilterConstraintArgs(EvitaQLParser.EntityTypeWithFilterConstraintArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#entityTypeWithOrderConstraintListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityTypeWithOrderConstraintListArgs(EvitaQLParser.EntityTypeWithOrderConstraintListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#entityTypeWithRequireConstraintListArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityTypeWithRequireConstraintListArgs(EvitaQLParser.EntityTypeWithRequireConstraintListArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#withinHierarchyConstraintArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithinHierarchyConstraintArgs(EvitaQLParser.WithinHierarchyConstraintArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#withinRootHierarchyConstraintArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithinRootHierarchyConstraintArgs(EvitaQLParser.WithinRootHierarchyConstraintArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#pageConstraintArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPageConstraintArgs(EvitaQLParser.PageConstraintArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#stripConstraintArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStripConstraintArgs(EvitaQLParser.StripConstraintArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#parentsOfTypeConstraintArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentsOfTypeConstraintArgs(EvitaQLParser.ParentsOfTypeConstraintArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link EvitaQLParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(EvitaQLParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(EvitaQLParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntLiteral(EvitaQLParser.IntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatLiteral(EvitaQLParser.FloatLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(EvitaQLParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dateLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateLiteral(EvitaQLParser.DateLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimeLiteral(EvitaQLParser.TimeLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dateTimeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateTimeLiteral(EvitaQLParser.DateTimeLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code zonedDateTimeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZonedDateTimeLiteral(EvitaQLParser.ZonedDateTimeLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numberRangeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumberRangeLiteral(EvitaQLParser.NumberRangeLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dateTimeRangeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateTimeRangeLiteral(EvitaQLParser.DateTimeRangeLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code enumLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumLiteral(EvitaQLParser.EnumLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code localeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocaleLiteral(EvitaQLParser.LocaleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multipleLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipleLiteral(EvitaQLParser.MultipleLiteralContext ctx);
}