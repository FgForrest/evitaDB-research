// Generated from EvitaQL.g4 by ANTLR 4.9.2

package io.evitadb.api.query.parser.grammar;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link EvitaQLParser}.
 */
public interface EvitaQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(EvitaQLParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(EvitaQLParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(EvitaQLParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(EvitaQLParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#constraint}.
	 * @param ctx the parse tree
	 */
	void enterConstraint(EvitaQLParser.ConstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#constraint}.
	 * @param ctx the parse tree
	 */
	void exitConstraint(EvitaQLParser.ConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code entitiesConstraint}
	 * labeled alternative in {@link EvitaQLParser#headConstraint}.
	 * @param ctx the parse tree
	 */
	void enterEntitiesConstraint(EvitaQLParser.EntitiesConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code entitiesConstraint}
	 * labeled alternative in {@link EvitaQLParser#headConstraint}.
	 * @param ctx the parse tree
	 */
	void exitEntitiesConstraint(EvitaQLParser.EntitiesConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code filterByConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFilterByConstraint(EvitaQLParser.FilterByConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code filterByConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFilterByConstraint(EvitaQLParser.FilterByConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code andConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterAndConstraint(EvitaQLParser.AndConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code andConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitAndConstraint(EvitaQLParser.AndConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code orConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterOrConstraint(EvitaQLParser.OrConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code orConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitOrConstraint(EvitaQLParser.OrConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterNotConstraint(EvitaQLParser.NotConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitNotConstraint(EvitaQLParser.NotConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userFilterConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterUserFilterConstraint(EvitaQLParser.UserFilterConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userFilterConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitUserFilterConstraint(EvitaQLParser.UserFilterConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code equalsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterEqualsConstraint(EvitaQLParser.EqualsConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code equalsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitEqualsConstraint(EvitaQLParser.EqualsConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code greaterThanConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterGreaterThanConstraint(EvitaQLParser.GreaterThanConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code greaterThanConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitGreaterThanConstraint(EvitaQLParser.GreaterThanConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code greaterThanEqualsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterGreaterThanEqualsConstraint(EvitaQLParser.GreaterThanEqualsConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code greaterThanEqualsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitGreaterThanEqualsConstraint(EvitaQLParser.GreaterThanEqualsConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lessThanConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterLessThanConstraint(EvitaQLParser.LessThanConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lessThanConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitLessThanConstraint(EvitaQLParser.LessThanConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lessThanEqualsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterLessThanEqualsConstraint(EvitaQLParser.LessThanEqualsConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lessThanEqualsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitLessThanEqualsConstraint(EvitaQLParser.LessThanEqualsConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code betweenConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterBetweenConstraint(EvitaQLParser.BetweenConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code betweenConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitBetweenConstraint(EvitaQLParser.BetweenConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inSetConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterInSetConstraint(EvitaQLParser.InSetConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inSetConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitInSetConstraint(EvitaQLParser.InSetConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code containsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterContainsConstraint(EvitaQLParser.ContainsConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code containsConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitContainsConstraint(EvitaQLParser.ContainsConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code startsWithConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterStartsWithConstraint(EvitaQLParser.StartsWithConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code startsWithConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitStartsWithConstraint(EvitaQLParser.StartsWithConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code endsWithConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterEndsWithConstraint(EvitaQLParser.EndsWithConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code endsWithConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitEndsWithConstraint(EvitaQLParser.EndsWithConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isTrueConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterIsTrueConstraint(EvitaQLParser.IsTrueConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isTrueConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitIsTrueConstraint(EvitaQLParser.IsTrueConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isFalseConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterIsFalseConstraint(EvitaQLParser.IsFalseConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isFalseConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitIsFalseConstraint(EvitaQLParser.IsFalseConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNullConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterIsNullConstraint(EvitaQLParser.IsNullConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNullConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitIsNullConstraint(EvitaQLParser.IsNullConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNotNullConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterIsNotNullConstraint(EvitaQLParser.IsNotNullConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNotNullConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitIsNotNullConstraint(EvitaQLParser.IsNotNullConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inRangeConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterInRangeConstraint(EvitaQLParser.InRangeConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inRangeConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitInRangeConstraint(EvitaQLParser.InRangeConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primaryKeyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryKeyConstraint(EvitaQLParser.PrimaryKeyConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primaryKeyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryKeyConstraint(EvitaQLParser.PrimaryKeyConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code languageConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterLanguageConstraint(EvitaQLParser.LanguageConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code languageConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitLanguageConstraint(EvitaQLParser.LanguageConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceInCurrencyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceInCurrencyConstraint(EvitaQLParser.PriceInCurrencyConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceInCurrencyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceInCurrencyConstraint(EvitaQLParser.PriceInCurrencyConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceInPriceListsConstraints}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceInPriceListsConstraints(EvitaQLParser.PriceInPriceListsConstraintsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceInPriceListsConstraints}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceInPriceListsConstraints(EvitaQLParser.PriceInPriceListsConstraintsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceValidInConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceValidInConstraint(EvitaQLParser.PriceValidInConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceValidInConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceValidInConstraint(EvitaQLParser.PriceValidInConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceBetweenConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceBetweenConstraint(EvitaQLParser.PriceBetweenConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceBetweenConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceBetweenConstraint(EvitaQLParser.PriceBetweenConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code facetConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFacetConstraint(EvitaQLParser.FacetConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code facetConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFacetConstraint(EvitaQLParser.FacetConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code referenceHavingAttributeConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterReferenceHavingAttributeConstraint(EvitaQLParser.ReferenceHavingAttributeConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code referenceHavingAttributeConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitReferenceHavingAttributeConstraint(EvitaQLParser.ReferenceHavingAttributeConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code withinHierarchyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterWithinHierarchyConstraint(EvitaQLParser.WithinHierarchyConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code withinHierarchyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitWithinHierarchyConstraint(EvitaQLParser.WithinHierarchyConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code withinRootHierarchyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterWithinRootHierarchyConstraint(EvitaQLParser.WithinRootHierarchyConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code withinRootHierarchyConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitWithinRootHierarchyConstraint(EvitaQLParser.WithinRootHierarchyConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code directRelationConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterDirectRelationConstraint(EvitaQLParser.DirectRelationConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code directRelationConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitDirectRelationConstraint(EvitaQLParser.DirectRelationConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code excludingRootConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterExcludingRootConstraint(EvitaQLParser.ExcludingRootConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code excludingRootConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitExcludingRootConstraint(EvitaQLParser.ExcludingRootConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code excludingConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void enterExcludingConstraint(EvitaQLParser.ExcludingConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code excludingConstraint}
	 * labeled alternative in {@link EvitaQLParser#filterConstraint}.
	 * @param ctx the parse tree
	 */
	void exitExcludingConstraint(EvitaQLParser.ExcludingConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code orderByConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterOrderByConstraint(EvitaQLParser.OrderByConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code orderByConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitOrderByConstraint(EvitaQLParser.OrderByConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ascendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterAscendingConstraint(EvitaQLParser.AscendingConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ascendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitAscendingConstraint(EvitaQLParser.AscendingConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code descendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterDescendingConstraint(EvitaQLParser.DescendingConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code descendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitDescendingConstraint(EvitaQLParser.DescendingConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceAscendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceAscendingConstraint(EvitaQLParser.PriceAscendingConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceAscendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceAscendingConstraint(EvitaQLParser.PriceAscendingConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceDescendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceDescendingConstraint(EvitaQLParser.PriceDescendingConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceDescendingConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceDescendingConstraint(EvitaQLParser.PriceDescendingConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code randomConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterRandomConstraint(EvitaQLParser.RandomConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code randomConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitRandomConstraint(EvitaQLParser.RandomConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code referenceAttributeConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void enterReferenceAttributeConstraint(EvitaQLParser.ReferenceAttributeConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code referenceAttributeConstraint}
	 * labeled alternative in {@link EvitaQLParser#orderConstraint}.
	 * @param ctx the parse tree
	 */
	void exitReferenceAttributeConstraint(EvitaQLParser.ReferenceAttributeConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code requireContainerConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterRequireContainerConstraint(EvitaQLParser.RequireContainerConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code requireContainerConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitRequireContainerConstraint(EvitaQLParser.RequireContainerConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pageConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPageConstraint(EvitaQLParser.PageConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pageConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPageConstraint(EvitaQLParser.PageConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stripConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterStripConstraint(EvitaQLParser.StripConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stripConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitStripConstraint(EvitaQLParser.StripConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code entityBodyConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterEntityBodyConstraint(EvitaQLParser.EntityBodyConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code entityBodyConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitEntityBodyConstraint(EvitaQLParser.EntityBodyConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code attributesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterAttributesConstraint(EvitaQLParser.AttributesConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code attributesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitAttributesConstraint(EvitaQLParser.AttributesConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pricesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPricesConstraint(EvitaQLParser.PricesConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pricesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPricesConstraint(EvitaQLParser.PricesConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code associatedDataConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterAssociatedDataConstraint(EvitaQLParser.AssociatedDataConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code associatedDataConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitAssociatedDataConstraint(EvitaQLParser.AssociatedDataConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code referencesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterReferencesConstraint(EvitaQLParser.ReferencesConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code referencesConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitReferencesConstraint(EvitaQLParser.ReferencesConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code useOfPriceConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterUseOfPriceConstraint(EvitaQLParser.UseOfPriceConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code useOfPriceConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitUseOfPriceConstraint(EvitaQLParser.UseOfPriceConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dataInLanguageConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterDataInLanguageConstraint(EvitaQLParser.DataInLanguageConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dataInLanguageConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitDataInLanguageConstraint(EvitaQLParser.DataInLanguageConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parentsConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterParentsConstraint(EvitaQLParser.ParentsConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parentsConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitParentsConstraint(EvitaQLParser.ParentsConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parentsOfTypeConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterParentsOfTypeConstraint(EvitaQLParser.ParentsOfTypeConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parentsOfTypeConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitParentsOfTypeConstraint(EvitaQLParser.ParentsOfTypeConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code facetSummaryConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFacetSummaryConstraint(EvitaQLParser.FacetSummaryConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code facetSummaryConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFacetSummaryConstraint(EvitaQLParser.FacetSummaryConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code facetGroupsConjunctionConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFacetGroupsConjunctionConstraint(EvitaQLParser.FacetGroupsConjunctionConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code facetGroupsConjunctionConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFacetGroupsConjunctionConstraint(EvitaQLParser.FacetGroupsConjunctionConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code facetGroupsDisjunctionConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFacetGroupsDisjunctionConstraint(EvitaQLParser.FacetGroupsDisjunctionConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code facetGroupsDisjunctionConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFacetGroupsDisjunctionConstraint(EvitaQLParser.FacetGroupsDisjunctionConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code facetGroupsNegationConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFacetGroupsNegationConstraint(EvitaQLParser.FacetGroupsNegationConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code facetGroupsNegationConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFacetGroupsNegationConstraint(EvitaQLParser.FacetGroupsNegationConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code attributeHistogramConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterAttributeHistogramConstraint(EvitaQLParser.AttributeHistogramConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code attributeHistogramConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitAttributeHistogramConstraint(EvitaQLParser.AttributeHistogramConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priceHistogramConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPriceHistogramConstraint(EvitaQLParser.PriceHistogramConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priceHistogramConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPriceHistogramConstraint(EvitaQLParser.PriceHistogramConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hierarchyStatisticsConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void enterHierarchyStatisticsConstraint(EvitaQLParser.HierarchyStatisticsConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hierarchyStatisticsConstraint}
	 * labeled alternative in {@link EvitaQLParser#requireConstraint}.
	 * @param ctx the parse tree
	 */
	void exitHierarchyStatisticsConstraint(EvitaQLParser.HierarchyStatisticsConstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#constraintListArgs}.
	 * @param ctx the parse tree
	 */
	void enterConstraintListArgs(EvitaQLParser.ConstraintListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#constraintListArgs}.
	 * @param ctx the parse tree
	 */
	void exitConstraintListArgs(EvitaQLParser.ConstraintListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#emptyArgs}.
	 * @param ctx the parse tree
	 */
	void enterEmptyArgs(EvitaQLParser.EmptyArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#emptyArgs}.
	 * @param ctx the parse tree
	 */
	void exitEmptyArgs(EvitaQLParser.EmptyArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#filterConstraintContainerArgs}.
	 * @param ctx the parse tree
	 */
	void enterFilterConstraintContainerArgs(EvitaQLParser.FilterConstraintContainerArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#filterConstraintContainerArgs}.
	 * @param ctx the parse tree
	 */
	void exitFilterConstraintContainerArgs(EvitaQLParser.FilterConstraintContainerArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#orderConstraintContainerArgs}.
	 * @param ctx the parse tree
	 */
	void enterOrderConstraintContainerArgs(EvitaQLParser.OrderConstraintContainerArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#orderConstraintContainerArgs}.
	 * @param ctx the parse tree
	 */
	void exitOrderConstraintContainerArgs(EvitaQLParser.OrderConstraintContainerArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#requireConstraintContainerArgs}.
	 * @param ctx the parse tree
	 */
	void enterRequireConstraintContainerArgs(EvitaQLParser.RequireConstraintContainerArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#requireConstraintContainerArgs}.
	 * @param ctx the parse tree
	 */
	void exitRequireConstraintContainerArgs(EvitaQLParser.RequireConstraintContainerArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#nameArgs}.
	 * @param ctx the parse tree
	 */
	void enterNameArgs(EvitaQLParser.NameArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#nameArgs}.
	 * @param ctx the parse tree
	 */
	void exitNameArgs(EvitaQLParser.NameArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#nameWithValueArgs}.
	 * @param ctx the parse tree
	 */
	void enterNameWithValueArgs(EvitaQLParser.NameWithValueArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#nameWithValueArgs}.
	 * @param ctx the parse tree
	 */
	void exitNameWithValueArgs(EvitaQLParser.NameWithValueArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#nameWithValueListArgs}.
	 * @param ctx the parse tree
	 */
	void enterNameWithValueListArgs(EvitaQLParser.NameWithValueListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#nameWithValueListArgs}.
	 * @param ctx the parse tree
	 */
	void exitNameWithValueListArgs(EvitaQLParser.NameWithValueListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#nameWithBetweenValuesArgs}.
	 * @param ctx the parse tree
	 */
	void enterNameWithBetweenValuesArgs(EvitaQLParser.NameWithBetweenValuesArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#nameWithBetweenValuesArgs}.
	 * @param ctx the parse tree
	 */
	void exitNameWithBetweenValuesArgs(EvitaQLParser.NameWithBetweenValuesArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#valueArgs}.
	 * @param ctx the parse tree
	 */
	void enterValueArgs(EvitaQLParser.ValueArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#valueArgs}.
	 * @param ctx the parse tree
	 */
	void exitValueArgs(EvitaQLParser.ValueArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#valueListArgs}.
	 * @param ctx the parse tree
	 */
	void enterValueListArgs(EvitaQLParser.ValueListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#valueListArgs}.
	 * @param ctx the parse tree
	 */
	void exitValueListArgs(EvitaQLParser.ValueListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#betweenValuesArgs}.
	 * @param ctx the parse tree
	 */
	void enterBetweenValuesArgs(EvitaQLParser.BetweenValuesArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#betweenValuesArgs}.
	 * @param ctx the parse tree
	 */
	void exitBetweenValuesArgs(EvitaQLParser.BetweenValuesArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#nameListArgs}.
	 * @param ctx the parse tree
	 */
	void enterNameListArgs(EvitaQLParser.NameListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#nameListArgs}.
	 * @param ctx the parse tree
	 */
	void exitNameListArgs(EvitaQLParser.NameListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#valueWithNameListArgs}.
	 * @param ctx the parse tree
	 */
	void enterValueWithNameListArgs(EvitaQLParser.ValueWithNameListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#valueWithNameListArgs}.
	 * @param ctx the parse tree
	 */
	void exitValueWithNameListArgs(EvitaQLParser.ValueWithNameListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#referencedTypesArgs}.
	 * @param ctx the parse tree
	 */
	void enterReferencedTypesArgs(EvitaQLParser.ReferencedTypesArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#referencedTypesArgs}.
	 * @param ctx the parse tree
	 */
	void exitReferencedTypesArgs(EvitaQLParser.ReferencedTypesArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#entityTypeArgs}.
	 * @param ctx the parse tree
	 */
	void enterEntityTypeArgs(EvitaQLParser.EntityTypeArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#entityTypeArgs}.
	 * @param ctx the parse tree
	 */
	void exitEntityTypeArgs(EvitaQLParser.EntityTypeArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#entityTypeListArgs}.
	 * @param ctx the parse tree
	 */
	void enterEntityTypeListArgs(EvitaQLParser.EntityTypeListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#entityTypeListArgs}.
	 * @param ctx the parse tree
	 */
	void exitEntityTypeListArgs(EvitaQLParser.EntityTypeListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#entityTypeWithValueListArgs}.
	 * @param ctx the parse tree
	 */
	void enterEntityTypeWithValueListArgs(EvitaQLParser.EntityTypeWithValueListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#entityTypeWithValueListArgs}.
	 * @param ctx the parse tree
	 */
	void exitEntityTypeWithValueListArgs(EvitaQLParser.EntityTypeWithValueListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#entityTypeWithFilterConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void enterEntityTypeWithFilterConstraintArgs(EvitaQLParser.EntityTypeWithFilterConstraintArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#entityTypeWithFilterConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void exitEntityTypeWithFilterConstraintArgs(EvitaQLParser.EntityTypeWithFilterConstraintArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#entityTypeWithOrderConstraintListArgs}.
	 * @param ctx the parse tree
	 */
	void enterEntityTypeWithOrderConstraintListArgs(EvitaQLParser.EntityTypeWithOrderConstraintListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#entityTypeWithOrderConstraintListArgs}.
	 * @param ctx the parse tree
	 */
	void exitEntityTypeWithOrderConstraintListArgs(EvitaQLParser.EntityTypeWithOrderConstraintListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#entityTypeWithRequireConstraintListArgs}.
	 * @param ctx the parse tree
	 */
	void enterEntityTypeWithRequireConstraintListArgs(EvitaQLParser.EntityTypeWithRequireConstraintListArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#entityTypeWithRequireConstraintListArgs}.
	 * @param ctx the parse tree
	 */
	void exitEntityTypeWithRequireConstraintListArgs(EvitaQLParser.EntityTypeWithRequireConstraintListArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#withinHierarchyConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void enterWithinHierarchyConstraintArgs(EvitaQLParser.WithinHierarchyConstraintArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#withinHierarchyConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void exitWithinHierarchyConstraintArgs(EvitaQLParser.WithinHierarchyConstraintArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#withinRootHierarchyConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void enterWithinRootHierarchyConstraintArgs(EvitaQLParser.WithinRootHierarchyConstraintArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#withinRootHierarchyConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void exitWithinRootHierarchyConstraintArgs(EvitaQLParser.WithinRootHierarchyConstraintArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#pageConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void enterPageConstraintArgs(EvitaQLParser.PageConstraintArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#pageConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void exitPageConstraintArgs(EvitaQLParser.PageConstraintArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#stripConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void enterStripConstraintArgs(EvitaQLParser.StripConstraintArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#stripConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void exitStripConstraintArgs(EvitaQLParser.StripConstraintArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#parentsOfTypeConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void enterParentsOfTypeConstraintArgs(EvitaQLParser.ParentsOfTypeConstraintArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#parentsOfTypeConstraintArgs}.
	 * @param ctx the parse tree
	 */
	void exitParentsOfTypeConstraintArgs(EvitaQLParser.ParentsOfTypeConstraintArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link EvitaQLParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(EvitaQLParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link EvitaQLParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(EvitaQLParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(EvitaQLParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(EvitaQLParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterIntLiteral(EvitaQLParser.IntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitIntLiteral(EvitaQLParser.IntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(EvitaQLParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(EvitaQLParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(EvitaQLParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(EvitaQLParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dateLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterDateLiteral(EvitaQLParser.DateLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dateLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitDateLiteral(EvitaQLParser.DateLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterTimeLiteral(EvitaQLParser.TimeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitTimeLiteral(EvitaQLParser.TimeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dateTimeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterDateTimeLiteral(EvitaQLParser.DateTimeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dateTimeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitDateTimeLiteral(EvitaQLParser.DateTimeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code zonedDateTimeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterZonedDateTimeLiteral(EvitaQLParser.ZonedDateTimeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code zonedDateTimeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitZonedDateTimeLiteral(EvitaQLParser.ZonedDateTimeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numberRangeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterNumberRangeLiteral(EvitaQLParser.NumberRangeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numberRangeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitNumberRangeLiteral(EvitaQLParser.NumberRangeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dateTimeRangeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterDateTimeRangeLiteral(EvitaQLParser.DateTimeRangeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dateTimeRangeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitDateTimeRangeLiteral(EvitaQLParser.DateTimeRangeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code enumLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterEnumLiteral(EvitaQLParser.EnumLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code enumLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitEnumLiteral(EvitaQLParser.EnumLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code localeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLocaleLiteral(EvitaQLParser.LocaleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code localeLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLocaleLiteral(EvitaQLParser.LocaleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multipleLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterMultipleLiteral(EvitaQLParser.MultipleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multipleLiteral}
	 * labeled alternative in {@link EvitaQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitMultipleLiteral(EvitaQLParser.MultipleLiteralContext ctx);
}