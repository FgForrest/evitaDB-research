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
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link EvitaQLFilterConstraintVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLFilterConstraintVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldParseFilterByConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("filterBy(equals('a',1))");
        assertEquals(filterBy(eq("a", 1L)), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("filterBy ( equals('a',1)  )");
        assertEquals(filterBy(eq("a", 1L)), constraint2);
    }

    @Test
    void shouldNotParseFilterByConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("filterBy"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("filterBy()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("filterBy(entities('a'))"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("filterBy(equals('a',1),equals('b','c'))"));
    }

    @Test
    void shouldParseAndConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("and(equals('a',1))");
        assertEquals(and(eq("a", 1L)), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("and(equals('a',1),equals('b','c'))");
        assertEquals(and(eq("a", 1L), eq("b", "c")), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("and ( equals('a',1), equals('b','c') )");
        assertEquals(and(eq("a", 1L), eq("b", "c")), constraint3);
    }

    @Test
    void shouldNotParseAndConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("and"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("and()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("and(entities('a'))"));
    }

    @Test
    void shouldParseOrConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("or(equals('a',1))");
        assertEquals(or(eq("a", 1L)), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("or(equals('a',1),equals('b','c'))");
        assertEquals(or(eq("a", 1L), eq("b", "c")), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("or ( equals('a',1) , equals('b','c') )");
        assertEquals(or(eq("a", 1L), eq("b", "c")), constraint3);
    }

    @Test
    void shouldNotParseOrConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("or"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("or()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("or(entities('a'))"));
    }

    @Test
    void shouldParseNotConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("not(equals('a',1))");
        assertEquals(not(eq("a", 1L)), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("not ( equals('a',1)  )");
        assertEquals(not(eq("a", 1L)), constraint2);
    }

    @Test
    void shouldNotParseNotConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("not"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("not()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("not(entities('a'))"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("not(equals('a',1),equals('b','c'))"));
    }

     @Test
     void shouldParseUserFilterConstraint() {
         final Constraint<?> constraint1 = parseFilterConstraint("userFilter(equals('a',1))");
         assertEquals(userFilter(eq("a", 1L)), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("userFilter(equals('a',1),equals('b','c'))");
         assertEquals(userFilter(eq("a", 1L), eq("b", "c")), constraint2);

         final Constraint<?> constraint3 = parseFilterConstraint("userFilter ( equals('a',1)  , equals('b','c') )");
         assertEquals(userFilter(eq("a", 1L), eq("b", "c")), constraint3);
     }

    @Test
    void shouldNotParseUserFilterConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("userFilter"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("userFilter()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("userFilter(entities('a'))"));
    }

    @Test
    void shouldParseEqualsConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("equals('a',100)");
        assertEquals(eq("a", 100L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("equals('a','c')");
        assertEquals(eq("a", "c"), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("equals('a',SOME_ENUM)");
        assertEquals(eq("a", EnumWrapper.fromString("SOME_ENUM")), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("equals ( 'a'  , SOME_ENUM )");
        assertEquals(eq("a", EnumWrapper.fromString("SOME_ENUM")), constraint4);
    }

    @Test
    void shouldNotParseEqualsConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("equals"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("equals()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("equals('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("equals(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("equals('a',2,3)"));
    }

    @Test
    void shouldParseGreaterThanConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("greaterThan('a',100)");
        assertEquals(greaterThan("a", 100L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("greaterThan('a','c')");
        assertEquals(greaterThan("a", "c"), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("greaterThan('a',SOME_ENUM)");
        assertEquals(greaterThan("a", EnumWrapper.fromString("SOME_ENUM")), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("greaterThan ( 'a'  , SOME_ENUM )");
        assertEquals(greaterThan("a", EnumWrapper.fromString("SOME_ENUM")), constraint4);
    }

    @Test
    void shouldNotParseGreaterThanConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThan"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThan()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThan('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThan(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThan('a',2,3)"));
    }

    @Test
    void shouldParseGreaterThanEqualsConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("greaterThanEquals('a',100)");
        assertEquals(greaterThanEquals("a", 100L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("greaterThanEquals('a','c')");
        assertEquals(greaterThanEquals("a", "c"), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("greaterThanEquals('a',SOME_ENUM)");
        assertEquals(greaterThanEquals("a", EnumWrapper.fromString("SOME_ENUM")), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("greaterThanEquals ( 'a'  , SOME_ENUM )");
        assertEquals(greaterThanEquals("a", EnumWrapper.fromString("SOME_ENUM")), constraint4);
    }

    @Test
    void shouldNotParseGreaterThanEqualsConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThanEquals"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThanEquals()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThanEquals('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThanEquals(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("greaterThanEquals('a',2,3)"));
    }

    @Test
    void shouldParseLessThanConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("lessThan('a',100)");
        assertEquals(lessThan("a", 100L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("lessThan('a','c')");
        assertEquals(lessThan("a", "c"), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("lessThan('a',SOME_ENUM)");
        assertEquals(lessThan("a", EnumWrapper.fromString("SOME_ENUM")), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("lessThan ( 'a'  , SOME_ENUM )");
        assertEquals(lessThan("a", EnumWrapper.fromString("SOME_ENUM")), constraint4);
    }

    @Test
    void shouldNotParseLessThanConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThan"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThan()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThan('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThan(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThan('a',2,3)"));
    }

    @Test
    void shouldParseLessThanEqualsConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("lessThanEquals('a',100)");
        assertEquals(lessThanEquals("a", 100L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("lessThanEquals('a','c')");
        assertEquals(lessThanEquals("a", "c"), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("lessThanEquals('a',SOME_ENUM)");
        assertEquals(lessThanEquals("a", EnumWrapper.fromString("SOME_ENUM")), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("lessThanEquals ( 'a'  , SOME_ENUM )");
        assertEquals(lessThanEquals("a", EnumWrapper.fromString("SOME_ENUM")), constraint4);
    }

    @Test
    void shouldNotParseLessThanEqualsConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThanEquals"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThanEquals()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThanEquals('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThanEquals(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("lessThanEquals('a',2,3)"));
    }

    @Test
    void shouldParseBetweenConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("between('a',100,150)");
        assertEquals(between("a", 100L, 150L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("between('a',2021-02-15,2021-03-15)");
        assertEquals(between("a", LocalDate.of(2021, 2, 15), LocalDate.of(2021, 3, 15)), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("between ( 'a' ,  2021-02-15,2021-03-15 )");
        assertEquals(between("a", LocalDate.of(2021, 2, 15), LocalDate.of(2021, 3, 15)), constraint3);
    }

    @Test
    void shouldNotParseBetweenConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("between"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("between()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("between('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("between(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("between('a',2,3,3)"));
    }

    @Test
    void shouldParseInSetConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("inSet('a',100,150,200)");
        assertEquals(inSet("a", 100L, 150L, 200L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("inSet('a','aa','bb','cc')");
        assertEquals(inSet("a", "aa", "bb", "cc"), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("inSet('a',SOME_ENUM)");
        assertEquals(inSet("a", EnumWrapper.fromString("SOME_ENUM")), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("inSet ( 'a'  , SOME_ENUM )");
        assertEquals(inSet("a", EnumWrapper.fromString("SOME_ENUM")), constraint4);
    }

    @Test
    void shouldNotParseInSetConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inSet"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inSet()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inSet('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inSet(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inSet('a',2,'b',3)"));
    }

    @Test
    void shouldParseContainsConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("contains('a','text')");
        assertEquals(contains("a", "text"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("contains('a','')");
        assertEquals(contains("a", ""), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("contains ( 'a'  , 'text' )");
        assertEquals(contains("a", "text"), constraint3);
    }

    @Test
    void shouldNotParseContainsConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("contains"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("contains()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("contains('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("contains(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("contains('a',2,'b',3)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("contains('a',2)"));
    }

    @Test
    void shouldParseStartsWithConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("startsWith('a','text')");
        assertEquals(startsWith("a", "text"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("startsWith('a','')");
        assertEquals(startsWith("a", ""), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("startsWith ( 'a'  , 'text' )");
        assertEquals(startsWith("a", "text"), constraint3);
    }

    @Test
    void shouldNotParseStartsWithConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("startsWith"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("startsWith()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("startsWith('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("startsWith(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("startsWith('a',2,'b',3)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("startsWith('a',2)"));
    }

    @Test
    void shouldParseEndsWithConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("endsWith('a','text')");
        assertEquals(endsWith("a", "text"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("endsWith('a','')");
        assertEquals(endsWith("a", ""), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("endsWith ( 'a'  , 'text' )");
        assertEquals(endsWith("a", "text"), constraint3);
    }

    @Test
    void shouldNotParseEndsWithConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("endsWith"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("endsWith()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("endsWith('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("endsWith(1,2)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("endsWith('a',2,'b',3)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("endsWith('a',2)"));
    }

    @Test
    void shouldParseIsTrueConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("isTrue('a')");
        assertEquals(isTrue("a"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("isTrue (  'a' )");
        assertEquals(isTrue("a"), constraint2);
    }

    @Test
    void shouldNotParseIsTrueConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isTrue"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isTrue()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isTrue(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isTrue('a',2)"));
    }

    @Test
    void shouldParseIsFalseConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("isFalse('a')");
        assertEquals(isFalse("a"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("isFalse (  'a' )");
        assertEquals(isFalse("a"), constraint2);
    }

    @Test
    void shouldNotParseIsFalseConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isFalse"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isFalse()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isFalse(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isFalse('a',2)"));
    }

    @Test
    void shouldParseIsNullConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("isNull('a')");
        assertEquals(isNull("a"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("isNull (  'a' )");
        assertEquals(isNull("a"), constraint2);
    }

    @Test
    void shouldNotParseIsNullConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNull"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNull()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNull(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNull('a',2)"));
    }

    @Test
    void shouldParseIsNotNullConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("isNotNull('a')");
        assertEquals(isNotNull("a"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("isNotNull (  'a' )");
        assertEquals(isNotNull("a"), constraint2);
    }

    @Test
    void shouldNotParseIsNotNullConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNotNull"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNotNull()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNotNull(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("isNotNull('a',2)"));
    }

    @Test
    void shouldParseInRangeConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("inRange('a',500)");
        assertEquals(inRange("a", 500L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("inRange('a',2021-02-15T11:00:00+01:00[Europe/Prague])");
        assertEquals(
                inRange("a", ZonedDateTime.of(2021, 2, 15, 11, 0, 0, 0, ZoneId.of("Europe/Prague"))),
                constraint2
        );

        final Constraint<?> constraint3 = parseFilterConstraint("inRange (  'a' ,  500)");
        assertEquals(inRange("a", 500L), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("inRange ( 'a'  , 2021-02-15T11:00:00+01:00[Europe/Prague] )");
        assertEquals(
                inRange("a", ZonedDateTime.of(2021, 2, 15, 11, 0, 0, 0, ZoneId.of("Europe/Prague"))),
                constraint4
        );

        final Constraint<?> constraint5 = parseFilterConstraint("inRange('a',500.5)");
        assertEquals(inRange("a", BigDecimal.valueOf(500.5)), constraint5);
    }

    @Test
    void shouldNotParseInRangeConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange('a','b')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange('a',2021-02-15)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("inRange('a',1,2)"));
    }

    @Test
    void shouldParsePrimaryKeyConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("primaryKey(10)");
        assertEquals(primaryKey(10), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("primaryKey(10,20,50,60)");
        assertEquals(primaryKey(10, 20, 50, 60), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("primaryKey ( 10 ,  20 , 50, 60 )");
        assertEquals(primaryKey(10, 20, 50, 60), constraint3);
    }

    @Test
    void shouldNotParsePrimaryKeyConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("primaryKey"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("primaryKey()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("primaryKey('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("primaryKey('a','b')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("primaryKey(1,'a',2)"));
    }

    @Test
    void shouldParseLanguageConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("language(`cs_CZ`)");
        assertEquals(language(new Locale("cs", "CZ")), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("language(  `cs_CZ` )");
        assertEquals(language(new Locale("cs", "CZ")), constraint2);
    }

    @Test
    void shouldNotParseLanguageConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("language"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("language()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("language('a','b')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("language(1,'a',2)"));
    }

    @Test
    void shouldParsePriceInCurrencyConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("priceInCurrency('CZK')");
        assertEquals(priceInCurrency("CZK"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("priceInCurrency (  'CZK' )");
        assertEquals(priceInCurrency("CZK"), constraint2);
    }

    @Test
    void shouldNotParsePriceInCurrencyConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceInCurrency"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceInCurrency()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceInCurrency('a','b')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceInCurrency(1,'a',2)"));
    }

    @Test
    void shouldParsePriceInPriceListsConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("priceInPriceLists(10)");
        assertEquals(priceInPriceLists(10L), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("priceInPriceLists(10,20,50,60)");
        assertEquals(priceInPriceLists(10L, 20L, 50L, 60L), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("priceInPriceLists ( 10 ,  20 , 50, 60 )");
        assertEquals(priceInPriceLists(10L, 20L, 50L, 60L), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("priceInPriceLists('basic')");
        assertEquals(priceInPriceLists("basic"), constraint4);

        final Constraint<?> constraint5 = parseFilterConstraint("priceInPriceLists('basic','reference')");
        assertEquals(priceInPriceLists("basic", "reference"), constraint5);

        final Constraint<?> constraint6 = parseFilterConstraint("priceInPriceLists ( 'basic' ,  'reference' , 'action' )");
        assertEquals(priceInPriceLists("basic", "reference", "action"), constraint6);
    }

    @Test
    void shouldNotParsePriceInPriceListsConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceInPriceLists"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceInPriceLists()"));
    }

    @Test
    void shouldParsePriceValidInConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("priceValidIn(2021-02-15T11:00:00+01:00[Europe/Prague])");
        assertEquals(
                priceValidIn(ZonedDateTime.of(2021, 2, 15, 11, 0, 0, 0, ZoneId.of("Europe/Prague"))),
                constraint1
        );

        final Constraint<?> constraint2 = parseFilterConstraint("priceValidIn (  2021-02-15T11:00:00+01:00[Europe/Prague] )");
        assertEquals(
                priceValidIn(ZonedDateTime.of(2021, 2, 15, 11, 0, 0, 0, ZoneId.of("Europe/Prague"))),
                constraint2
        );
    }

    @Test
    void shouldNotParsePriceValidInConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceValidIn"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceValidIn()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceValidIn('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceValidIn(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceValidIn(2021-02-15T11:00:00+01:00[Europe/Prague],2021-02-15T11:00:00+01:00[Europe/Prague])"));
    }

    @Test
    void shouldParsePriceBetweenConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("priceBetween(10.0,50.5)");
        assertEquals(priceBetween(BigDecimal.valueOf(10.0), BigDecimal.valueOf(50.5)), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("priceBetween( 10.0  , 50.5  )");
        assertEquals(priceBetween(BigDecimal.valueOf(10.0), BigDecimal.valueOf(50.5)), constraint2);
    }

    @Test
    void shouldNotParsePriceBetweenConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceBetween"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceBetween()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceBetween(10,11)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceBetween('a',1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("priceBetween(10.0,50.0,78)"));
    }

    @Test
    void shouldParseFacetConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("facet('a',10)");
        assertEquals(facet("a", 10), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("facet(SOME_ENUM,10,20,50)");
        assertEquals(facet(EnumWrapper.fromString("SOME_ENUM"), 10, 20, 50), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("facet ( SOME_ENUM  , 10,  20,50 )");
        assertEquals(facet(EnumWrapper.fromString("SOME_ENUM"), 10, 20, 50), constraint3);
    }

    @Test
    void shouldNotParseFacetConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("facet"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("facet()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("facet('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("facet('a','b',5)"));
    }

    @Test
    void shouldParseReferenceHavingAttributeConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("referenceHavingAttribute('a',equals('b',1))");
        assertEquals(
                referenceHavingAttribute("a", eq("b", 1L)),
                constraint1
        );

        final Constraint<?> constraint2 = parseFilterConstraint("referenceHavingAttribute(SOME_ENUM,and(equals('b',1)))");
        assertEquals(
                referenceHavingAttribute(EnumWrapper.fromString("SOME_ENUM"), and(eq("b", 1L))),
                constraint2
        );

        final Constraint<?> constraint3 = parseFilterConstraint("referenceHavingAttribute ( 'a' , and( equals ('b', 1 )) )");
        assertEquals(
                referenceHavingAttribute("a", and(eq("b", 1L))),
                constraint3
        );
    }

    @Test
    void shouldNotParseReferenceHavingAttributeConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("referenceHavingAttribute"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("referenceHavingAttribute()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("referenceHavingAttribute(1)"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("referenceHavingAttribute('a','b')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("referenceHavingAttribute('a',equals('b',1),isTrue('c'))"));
    }

    @Test
    void shouldParseWithinHierarchyConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("withinHierarchy('a',10)");
        assertEquals(withinHierarchy("a", 10), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("withinHierarchy(10)");
        assertEquals(withinHierarchy(10), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("withinHierarchy(SOME_ENUM,10,directRelation())");
        assertEquals(withinHierarchy(EnumWrapper.fromString("SOME_ENUM"), 10, directRelation()), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("withinHierarchy('a',10,directRelation(),excluding(1,3),excludingRoot())");
        assertEquals(
                withinHierarchy("a", 10, directRelation(), excluding(1, 3), excludingRoot()),
                constraint4
        );

        final Constraint<?> constraint5 = parseFilterConstraint("withinHierarchy (  'a' ,10 ,  directRelation() )");
        assertEquals(withinHierarchy("a", 10, directRelation()), constraint5);
    }

    @Test
    void shouldNotParseWithinHierarchyConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinHierarchy"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinHierarchy()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinHierarchy('a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinHierarchy(directRelation())"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinHierarchy(1,'a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinHierarchy(directRelation(),'a')"));
    }

    @Test
    void shouldParseWithinRootHierarchyConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("withinRootHierarchy('a')");
        assertEquals(withinRootHierarchy("a"), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("withinRootHierarchy(directRelation())");
        assertEquals(withinRootHierarchy(directRelation()), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("withinRootHierarchy(directRelation(),excluding(1))");
        assertEquals(withinRootHierarchy(directRelation(), excluding(1)), constraint3);

        final Constraint<?> constraint4 = parseFilterConstraint("withinRootHierarchy(SOME_ENUM)");
        assertEquals(withinRootHierarchy(EnumWrapper.fromString("SOME_ENUM")), constraint4);

        final Constraint<?> constraint5 = parseFilterConstraint("withinRootHierarchy('a',excluding(1,3))");
        assertEquals(
                withinRootHierarchy("a", excluding(1, 3)),
                constraint5
        );

        final Constraint<?> constraint6 = parseFilterConstraint("withinRootHierarchy (  'a' )");
        assertEquals(withinRootHierarchy("a"), constraint6);

        final Constraint<?> constraint7 = parseFilterConstraint("withinRootHierarchy (   directRelation()   ,excluding( 1) )");
        assertEquals(withinRootHierarchy(directRelation(), excluding(1)), constraint7);
    }

    @Test
    void shouldNotParseWithinRootHierarchyConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinRootHierarchy"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinRootHierarchy(1,'a')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("withinRootHierarchy(directRelation(),'a')"));
    }

    @Test
    void shouldParseDirectRelationConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("directRelation()");
        assertEquals(directRelation(), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("directRelation (  )");
        assertEquals(directRelation(), constraint2);
    }

    @Test
    void shouldNotParseDirectRelationConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("directRelation"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("directRelation('a')"));
    }

    @Test
    void shouldParseExcludingRootConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("excludingRoot()");
        assertEquals(excludingRoot(), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("excludingRoot (  )");
        assertEquals(excludingRoot(), constraint2);
    }

    @Test
    void shouldNotParseExcludingRootConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("excludingRoot"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("excludingRoot('a')"));
    }

    @Test
    void shouldParseExcludingConstraint() {
        final Constraint<?> constraint1 = parseFilterConstraint("excluding(1)");
        assertEquals(excluding(1), constraint1);

        final Constraint<?> constraint2 = parseFilterConstraint("excluding(1,5,6)");
        assertEquals(excluding(1, 5, 6), constraint2);

        final Constraint<?> constraint3 = parseFilterConstraint("excluding ( 1 , 6, 2 )");
        assertEquals(excluding(1, 6, 2), constraint3);
    }

    @Test
    void shouldNotParseExcludingConstraint() {
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("excluding"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("excluding()"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("excluding('a','b')"));
        assertThrows(RuntimeException.class, () -> parseFilterConstraint("excluding(1,'a')"));
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "filterConstraint"
     *
     * @param string string to parse
     * @return parsed constraint
     */
    private Constraint<?> parseFilterConstraint(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.filterConstraint().accept(new EvitaQLFilterConstraintVisitor());
    }
}

