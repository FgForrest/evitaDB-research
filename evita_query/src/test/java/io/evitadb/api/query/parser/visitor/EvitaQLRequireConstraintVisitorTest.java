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

import io.evitadb.api.dataType.EnumWrapper;
import io.evitadb.api.query.Constraint;
import io.evitadb.api.query.require.FacetStatisticsDepth;
import io.evitadb.api.query.require.QueryPriceMode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Locale;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link EvitaQLRequireConstraintVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLRequireConstraintVisitorTest extends EvitaQLVisitorTestBase {

	@Test
	void shouldParseRequireContainerConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("require(entityBody())");
		assertEquals(require(entityBody()), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("require(entityBody(),attributes())");
		assertEquals(require(entityBody(), attributes()), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("require (  entityBody() ,  attributes() )");
		assertEquals(require(entityBody(), attributes()), constraint3);
	}

	@Test
	void shouldNotParseRequireContainerConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("require"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("require()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("require('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("require(equals('a',1))"));
	}

	@Test
	void shouldParsePagingConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("page(10,20)");
		assertEquals(page(10, 20), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("page (  10 ,20 )");
		assertEquals(page(10, 20), constraint2);
	}

	@Test
	void shouldNotParsePagingConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("page"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("page()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("page(1)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("page('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("page(1,'a')"));
	}

	@Test
	void shouldParseStripConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("strip(10,20)");
		assertEquals(strip(10, 20), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("strip(   5  , 30  )");
		assertEquals(strip(5, 30), constraint2);
	}

	@Test
	void shouldNotParseStripConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("strip"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("strip()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("strip(1)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("strip('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("strip(1,'a')"));
	}

	@Test
	void shouldParseEntityBodyConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("entityBody()");
		assertEquals(entityBody(), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("entityBody (  )");
		assertEquals(entityBody(), constraint2);
	}

	@Test
	void shouldNotParseEntityBodyConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("entityBody"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("entityBody('a')"));
	}

	@Test
	void shouldParseAttributesConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("attributes()");
		assertEquals(attributes(), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("attributes (  )");
		assertEquals(attributes(), constraint2);
	}

	@Test
	void shouldNotParseAttributesConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributes"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributes('a')"));
	}

	@Test
	void shouldParsePricesConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("prices()");
		assertEquals(prices(), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("prices(ALL)");
		assertEquals(allPrices(), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("prices (  ALL )");
		assertEquals(allPrices(), constraint3);
	}

	@Test
	void shouldNotParsePricesConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("prices"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("prices('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("prices(AA)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("prices(ALL,1)"));
	}

	@Test
	void shouldParseAssociatedDataConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("associatedData('a')");
		assertEquals(associatedData("a"), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("associatedData('a','b','c')");
		assertEquals(associatedData("a", "b", "c"), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("associatedData (  'a' , 'b'  , 'c' )");
		assertEquals(associatedData("a", "b", "c"), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("associatedData()");
		assertEquals(associatedData(), constraint4);

		final Constraint<?> constraint5 = parseRequireConstraint("associatedData (  )");
		assertEquals(associatedData(), constraint5);
	}

	@Test
	void shouldNotParseAssociatedDataConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("associatedData(1)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("associatedData('a',1)"));
	}

	@Test
	void shouldParseReferencesConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("references('a')");
		assertEquals(references("a"), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("references('a','b','c')");
		assertEquals(references("a", "b", "c"), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("references (  'a' , 'b'  , 'c' )");
		assertEquals(references("a", "b", "c"), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("references()");
		assertEquals(references(), constraint4);

		final Constraint<?> constraint5 = parseRequireConstraint("references (  )");
		assertEquals(references(), constraint5);

		final Constraint<?> constraint6 = parseRequireConstraint("references(BRAND, CATEGORY,'stock')");
		assertEquals(references(EnumWrapper.fromString("BRAND"), EnumWrapper.fromString("CATEGORY"), "stock"), constraint6);
	}

	@Test
	void shouldParseUseOfPriceConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("useOfPrice(WITH_VAT)");
		assertEquals(useOfPrice(QueryPriceMode.WITH_VAT), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("useOfPrice(   WITHOUT_VAT )");
		assertEquals(useOfPrice(QueryPriceMode.WITHOUT_VAT), constraint2);
	}

	@Test
	void shouldNotParseUseOfPriceConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("useOfPrice"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("useOfPrice()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("useOfPrice('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("useOfPrice(WITH_VAT,WITH_VAT)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("useOfPrice(VAT)"));
	}

	@Test
	void shouldParseDataInLanguageConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("dataInLanguage()");
		assertEquals(dataInLanguage(), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("dataInLanguage(`cs`)");
		assertEquals(dataInLanguage(new Locale("cs")), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("dataInLanguage(`cs`,`en_US`)");
		assertEquals(dataInLanguage(new Locale("cs"), new Locale("en", "US")), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("dataInLanguage (   `cs` ,    `en_US` )");
		assertEquals(dataInLanguage(new Locale("cs"), new Locale("en", "US")), constraint4);
	}

	@Test
	void shouldNotParseDataInLanguageConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("dataInLanguage"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("dataInLanguage('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("dataInLanguage(`cs`,2)"));
	}

	@Test
	void shouldParseParentsConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("parents()");
		assertEquals(parents(), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("parents(entityBody())");
		assertEquals(parents(entityBody()), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("parents(entityBody(),attributes())");
		assertEquals(parents(entityBody(), attributes()), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("parents (  entityBody()  ,  attributes()  )");
		assertEquals(parents(entityBody(), attributes()), constraint4);
	}

	@Test
	void shouldNotParseParentsConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parents"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parents('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parents('a',SOME_ENUM_A)"));
	}

	@Test
	void shouldParseParentsOfTypeConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("parentsOfType('a')");
		assertEquals(parentsOfType("a"), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("parentsOfType('a','b',SOME_ENUM_A)");
		assertEquals(parentsOfType(new Serializable[]{"a", "b", EnumWrapper.fromString("SOME_ENUM_A")}), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("parentsOfType('a','b',entityBody(),attributes())");
		assertEquals(parentsOfType(new Serializable[]{"a", "b"}, entityBody(), attributes()), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("parentsOfType (  SOME_ENUM_A ,  SOME_ENUM_B ,  entityBody(),  attributes() )");
		assertEquals(parentsOfType(new Serializable[]{EnumWrapper.fromString("SOME_ENUM_A"), EnumWrapper.fromString("SOME_ENUM_B")}, entityBody(), attributes()), constraint4);
	}

	@Test
	void shouldNotParseParentsOfTypeConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parentsOfType"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parentsOfType()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parentsOfType(entityBody())"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parentsOfType(entityBody(),'a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("parentsOfType('a',entityBody(),'b')"));
	}

	@Test
	void shouldParseFacetSummaryConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("facetSummary()");
		assertEquals(facetSummary(), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("facetSummary (  )");
		assertEquals(facetSummary(), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("facetSummary(COUNTS)");
		assertEquals(facetSummary(FacetStatisticsDepth.COUNTS), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("facetSummary( IMPACT   )");
		assertEquals(facetSummary(FacetStatisticsDepth.IMPACT), constraint4);
	}

	@Test
	void shouldNotParseFacetSummaryConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetSummary"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetSummary('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetSummary(NONE)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetSummary(COUNTS,IMPACT)"));
	}

	@Test
	void shouldParseFacetGroupsConjunctionConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("facetGroupsConjunction('a',1)");
		assertEquals(facetGroupsConjunction("a", 1), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("facetGroupsConjunction(SOME_ENUM,1,5,6)");
		assertEquals(facetGroupsConjunction(EnumWrapper.fromString("SOME_ENUM"), 1, 5, 6), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("facetGroupsConjunction (  SOME_ENUM ,  1 , 5, 6)");
		assertEquals(facetGroupsConjunction(EnumWrapper.fromString("SOME_ENUM"), 1, 5, 6), constraint3);
	}

	@Test
	void shouldNotParseFacetGroupsConjunctionConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsConjunction"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsConjunction()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsConjunction('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsConjunction('a','b','c')"));
	}

	@Test
	void shouldParseFacetGroupsDisjunctionConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("facetGroupsDisjunction('a',1)");
		assertEquals(facetGroupsDisjunction("a", 1), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("facetGroupsDisjunction(SOME_ENUM,1,5,6)");
		assertEquals(facetGroupsDisjunction(EnumWrapper.fromString("SOME_ENUM"), 1, 5, 6), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("facetGroupsDisjunction (  SOME_ENUM ,  1 , 5, 6)");
		assertEquals(facetGroupsDisjunction(EnumWrapper.fromString("SOME_ENUM"), 1, 5, 6), constraint3);
	}

	@Test
	void shouldNotParseFacetGroupsDisjunctionConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsDisjunction"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsDisjunction()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsDisjunction('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsDisjunction('a','b','c')"));
	}

	@Test
	void shouldParseFacetGroupsNegationConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("facetGroupsNegation('a',1)");
		assertEquals(facetGroupsNegation("a", 1), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("facetGroupsNegation(SOME_ENUM,1,5,6)");
		assertEquals(facetGroupsNegation(EnumWrapper.fromString("SOME_ENUM"), 1, 5, 6), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("facetGroupsNegation (  SOME_ENUM ,  1 , 5, 6)");
		assertEquals(facetGroupsNegation(EnumWrapper.fromString("SOME_ENUM"), 1, 5, 6), constraint3);
	}

	@Test
	void shouldNotParseFacetGroupsNegationConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsNegation"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsNegation()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsNegation('a')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("facetGroupsNegation('a','b','c')"));
	}

	@Test
	void shouldParseAttributeHistogramConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("attributeHistogram(20, 'a')");
		assertEquals(attributeHistogram(20, "a"), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("attributeHistogram(20, 'a','b','c')");
		assertEquals(attributeHistogram(20, "a", "b", "c"), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("attributeHistogram ( 20  , 'a' ,  'b' ,'c' )");
		assertEquals(attributeHistogram(20, "a", "b", "c"), constraint3);
	}

	@Test
	void shouldNotParseAttributeHistogramConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributeHistogram"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributeHistogram()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributeHistogram(1)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributeHistogram('a',1)"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("attributeHistogram('a','b')"));
	}

	@Test
	void shouldParsePriceHistogramConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("priceHistogram(20)");
		assertEquals(priceHistogram(20), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("priceHistogram ( 20 )");
		assertEquals(priceHistogram(20), constraint2);
	}

	@Test
	void shouldNotParsePriceHistogramConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("priceHistogram"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("priceHistogram('a')"));
	}

	@Test
	void shouldParseHierarchyStatisticsConstraint() {
		final Constraint<?> constraint1 = parseRequireConstraint("hierarchyStatistics('a')");
		assertEquals(hierarchyStatistics("a"), constraint1);

		final Constraint<?> constraint2 = parseRequireConstraint("hierarchyStatistics ( SOME_ENUM )");
		assertEquals(hierarchyStatistics(EnumWrapper.fromString("SOME_ENUM")), constraint2);

		final Constraint<?> constraint3 = parseRequireConstraint("hierarchyStatistics (SOME_ENUM,attributes())");
		assertEquals(hierarchyStatistics(EnumWrapper.fromString("SOME_ENUM"), attributes()), constraint3);

		final Constraint<?> constraint4 = parseRequireConstraint("hierarchyStatistics (SOME_ENUM,entityBody(),prices())");
		assertEquals(hierarchyStatistics(EnumWrapper.fromString("SOME_ENUM"), entityBody(), prices()), constraint4);

		final Constraint<?> constraint5 = parseRequireConstraint("hierarchyStatistics (  SOME_ENUM  , entityBody(),   prices() )");
		assertEquals(hierarchyStatistics(EnumWrapper.fromString("SOME_ENUM"), entityBody(), prices()), constraint5);
	}

	@Test
	void shouldNotParseHierarchyStatisticsConstraint() {
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("hierarchyStatistics"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("hierarchyStatistics()"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("hierarchyStatistics('a','b')"));
		assertThrows(RuntimeException.class, () -> parseRequireConstraint("hierarchyStatistics('a',useOfPrice(WITH_VAT))"));
	}


	/**
	 * Using generated EvitaQL parser tries to parse string as grammar rule "filterConstraint"
	 *
	 * @param string string to parse
	 * @return parsed constraint
	 */
	private Constraint<?> parseRequireConstraint(String string) {
		lexer.setInputStream(CharStreams.fromString(string));
		parser.setInputStream(new CommonTokenStream(lexer));

		return parser.requireConstraint().accept(new EvitaQLRequireConstraintVisitor());
    }
}
