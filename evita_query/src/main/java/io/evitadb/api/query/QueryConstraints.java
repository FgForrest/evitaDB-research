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

package io.evitadb.api.query;

import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.head.Entities;
import io.evitadb.api.query.order.Random;
import io.evitadb.api.query.order.*;
import io.evitadb.api.query.require.*;
import io.evitadb.api.utils.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory class for creating {@link Constraint} instances.
 * This factory class is handy so that developer doesn't need to remember all possible constraint variants and could
 * easily construct queries similar to textual format of the EQL.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface QueryConstraints {

	/*
		HEADING
	 */

	/**
	 * Each query must specify collection. This mandatory {@link Serializable} constraint controls what collection
	 * the query will be applied on.
	 *
	 * Sample of the header is:
	 *
	 * ```
	 * collection('category')
	 * ```
	 */
	@Nonnull
	static Entities entities(@Nonnull Serializable entityType) {
		return new Entities(entityType);
	}

	/*
		FILTERING
	 */

	/**
	 * This `filterBy` is container for filtering constraints. It is mandatory container when any filtering is to be used.
	 * This container allows only one children container with the filtering condition.
	 *
	 * Example:
	 *
	 * ```
	 * filterBy(
	 * and(
	 * isNotNull('code'),
	 * or(
	 * equals('code', 'ABCD'),
	 * startsWith('title', 'Knife')
	 * )
	 * )
	 * )
	 * ```
	 */
	@Nullable
	static FilterBy filterBy(@Nullable FilterConstraint constraint) {
		return constraint == null ? null : new FilterBy(constraint);
	}

	/**
	 * This `and` is container constraint that contains two or more inner constraints which output is combined by
	 * <a href="https://en.wikipedia.org/wiki/Logical_conjunction">logical AND</a>.
	 *
	 * Example:
	 *
	 * ```
	 * and(
	 * isTrue('visible'),
	 * validInTime(2020-07-30T07:28:13+00:00)
	 * )
	 * ```
	 */
	@Nullable
	static And and(@Nullable FilterConstraint... constraints) {
		if (constraints == null) {
			return null;
		}
		final FilterConstraint[] args = Arrays.stream(constraints).filter(Objects::nonNull).toArray(FilterConstraint[]::new);
		return args.length == 0 ? null : new And(args);
	}

	/**
	 * This `or` is container constraint that contains two or more inner constraints which output is combined by
	 * <a href="https://en.wikipedia.org/wiki/Logical_disjunction">logical OR</a>.
	 *
	 * Example:
	 *
	 * ```
	 * or(
	 * isTrue('visible'),
	 * greaterThan('price', 20)
	 * )
	 * ```
	 */
	@Nullable
	static Or or(@Nullable FilterConstraint... constraints) {
		if (constraints == null) {
			return null;
		}
		final FilterConstraint[] args = Arrays.stream(constraints).filter(Objects::nonNull).toArray(FilterConstraint[]::new);
		return args.length == 0 ? null : new Or(args);
	}

	/**
	 * This `not` is container constraint that contains single inner constraint which output is negated. Behaves as
	 * <a href="https://en.wikipedia.org/wiki/Negation">logical NOT</a>.
	 *
	 * Example:
	 *
	 * ```
	 * not(
	 * primaryKey(1,2,3)
	 * )
	 * ```
	 */
	@Nullable
	static Not not(@Nullable FilterConstraint constraint) {
		return constraint == null ? null : new Not(constraint);
	}

	/**
	 * This `referenceAttribute` container is filtering constraint that filters returned entities by their reference
	 * attributes that are examined whether they fulfill all the inner conditions.
	 *
	 * Example:
	 *
	 * ```
	 * referenceHavingAttribute(
	 * 'CATEGORY',
	 * eq('code', 'KITCHENWARE')
	 * )
	 * ```
	 *
	 * or
	 *
	 * ```
	 * referenceHavingAttribute(
	 * 'CATEGORY',
	 * and(
	 * isTrue('visible'),
	 *
	 * )
	 * )
	 * ```
	 */
	@Nullable
	static ReferenceHavingAttribute referenceHavingAttribute(@Nonnull Serializable entityType, @Nullable FilterConstraint constraint) {
		return constraint == null ? null : new ReferenceHavingAttribute(entityType, constraint);
	}

	/**
	 * This `userFilter` is a container constraint that could contain any constraints
	 * except [priceInPriceLists](#price-in-price-lists),
	 * [language](#language), [priceInCurrency](#price-in-currency), [priceValidInTime](#price-valid-in-time),
	 * [with hierarchy](#within-hierarchy).
	 *
	 * These constraints should react to the settings defined by the end user and must be isolated from the base filter so
	 * that [facetSummary](#facet-summary) logic can distinguish base filtering constraint for a facet summary computation.
	 * Facet summary must define so-called baseline count - i.e. count of the entities that match system constraints but no
	 * optional constraints defined by the user has been applied yet on them. This baseline is also used
	 * for [facet statistics](#facet-statistics) computation.
	 *
	 * This constraint might be used even without [facetSummary](#facet-summary) - when the result facet counts are not
	 * required but still we want the facets use for filtering.
	 *
	 * Only single `userFilter` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * userFilter(
	 * greaterThanEq('memory', 8),
	 * priceBetween(150.25, 220.0),
	 * facet('parameter', 4, 15)
	 * )
	 * ```
	 *
	 * Even more complex queries are supported (although it is hard to make up some real life example for such):
	 *
	 * ```
	 * filterBy(
	 * and(
	 * or(
	 * referenceHavingAttribute('CATEGORY', eq(code, 'abc')),
	 * referenceHavingAttribute('STOCK', eq(market, 'asia')),
	 * ),
	 * eq(visibility, true),
	 * userFilter(
	 * or(
	 * and(
	 * greaterThanEq('memory', 8),
	 * priceBetween(150.25, 220.0)
	 * ),
	 * and(
	 * greaterThanEq('memory', 16),
	 * priceBetween(800.0, 1600.0)
	 * ),
	 * ),
	 * facet('parameter', 4, 15)
	 * )
	 * )
	 * ),
	 * require(
	 * facetGroupDisjunction('parameterType', 4),
	 * negatedFacets('parameterType', 8),
	 * )
	 *
	 * ```
	 *
	 * User filter envelopes the part of the query that is affected by user selection and that is optional. All constraints
	 * outside user filter are considered mandatory and must never be altered by [facet summary](#facet-summary) computational
	 * logic.
	 *
	 * Base count of the facets are computed for query having `userFilter` container contents stripped off. The "what-if"
	 * counts requested by [impact argument](#facet-summary) are computed from the query including `userFilter` creating
	 * multiple sub-queries checking the result for each additional facet selection.
	 *
	 * [Facet](#facet) filtering constraints must be direct children of the `userFilter` container. Their relationship is by
	 * default as follows: facets of the same type within same group are combined by conjunction (OR), facets of different
	 * types / groups are combined by disjunction (AND). This default behaviour can be controlled exactly by using any of
	 * following require constraints:
	 *
	 * - [facet groups conjunction](#facet-groups-conjunction) - changes relationship between facets in the same group
	 * - [facet groups disjunction](#facet-groups-disjunction) - changes relationship between facet groups
	 *
	 * All constraints placed directly inside `userFilter` are combined with by conjunction (AND). Other than `facet` filtering
	 * constraints (as seen in example) may represent user conditions in non-faceted inputs, such as interval inputs.
	 *
	 * ***Note:** this constraint might be a subject to change and affects advanced searching queries such as exclusion facet
	 * groups (i.e. facet in group are not represented as multi-select/checkboxes but as exlusive select/radio) or conditional
	 * filters (which can be used to apply a certain filter only if it would produce non-empty result, this is good for
	 * "sticky" filters).*
	 */
	@Nullable
	static UserFilter userFilter(@Nullable FilterConstraint... constraints) {
		if (constraints == null) {
			return null;
		}
		final FilterConstraint[] args = Arrays.stream(constraints).filter(Objects::nonNull).toArray(FilterConstraint[]::new);
		return args.length == 0 ? null : new UserFilter(args);
	}

	/**
	 * This `between` is constraint that compares value of the attribute with name passed in first argument with the value passed
	 * in the second argument and value passed in third argument. First argument must be {@link String},
	 * second and third argument may be any of {@link Comparable} type.
	 * Type of the attribute value and second argument must be convertible one to another otherwise `between` function
	 * returns false.
	 *
	 * Function returns true if value in a filterable attribute of such a name is greater than or equal to value in second argument
	 * and lesser than or equal to value in third argument.
	 *
	 * Example:
	 *
	 * ```
	 * between('age', 20, 25)
	 * ```
	 */
	@Nonnull
	static <T extends Serializable & Comparable<?>> Between between(@Nonnull String attributeName, @Nonnull  T from, @Nonnull T to) {
		return new Between(attributeName, from, to);
	}

	/**
	 * This `contains` is constraint that searches value of the attribute with name passed in first argument for presence of the
	 * {@link String} value passed in the second argument.
	 *
	 * Function returns true if attribute value contains secondary argument (starting with any position). Function is case
	 * sensitive and comparison is executed using `UTF-8` encoding (Java native).
	 *
	 * Example:
	 *
	 * ```
	 * contains('code', 'eve')
	 * ```
	 */
	@Nullable
	static Contains contains(@Nonnull String attributeName, @Nullable String textToSearch) {
		return textToSearch == null ? null : new Contains(attributeName, textToSearch);
	}

	/**
	 * This `startsWith` is constraint that searches value of the attribute with name passed in first argument for presence of the
	 * {@link String} value passed in the second argument.
	 *
	 * Function returns true if attribute value contains secondary argument (from first position). InSet other words attribute
	 * value starts with string passed in second argument. Function is case sensitive and comparison is executed using `UTF-8`
	 * encoding (Java native).
	 *
	 * Example:
	 *
	 * ```
	 * startsWith('code', 'vid')
	 * ```
	 */
	@Nullable
	static StartsWith startsWith(@Nonnull String attributeName, @Nullable String textToSearch) {
		return textToSearch == null ? null : new StartsWith(attributeName, textToSearch);
	}

	/**
	 * This `endsWith` is constraint that searches value of the attribute with name passed in first argument for presence of the
	 * {@link String} value passed in the second argument.
	 *
	 * Function returns true if attribute value contains secondary argument (using reverse lookup from last position).
	 * InSet other words attribute value ends with string passed in second argument. Function is case sensitive and comparison
	 * is executed using `UTF-8` encoding (Java native).
	 *
	 * Example:
	 *
	 * ```
	 * endsWith('code', 'ida')
	 * ```
	 */
	@Nullable
	static EndsWith endsWith(@Nonnull String attributeName, @Nullable String textToSearch) {
		return textToSearch == null ? null : new EndsWith(attributeName, textToSearch);
	}

	/**
	 * This `equals` is constraint that compares value of the attribute with name passed in first argument with the value passed
	 * in the second argument. First argument must be {@link String}, second argument may be any of {@link Comparable} type.
	 * Type of the attribute value and second argument must be convertible one to another otherwise `equals` function
	 * returns false.
	 *
	 * Function returns true if both values are equal.
	 *
	 * Example:
	 *
	 * ```
	 * equals('code', 'abc')
	 * ```
	 */
	@Nullable
	static <T extends Serializable & Comparable<?>> Equals eq(@Nonnull String attributeName, @Nullable T attributeValue) {
		return attributeValue == null ? null : new Equals(attributeName, attributeValue);
	}

	/**
	 * This `lessThan` is constraint that compares value of the attribute with name passed in first argument with the value passed
	 * in the second argument. First argument must be {@link String},
	 * second argument may be any of {@link Comparable} type.
	 * Type of the attribute value and second argument must be convertible one to another otherwise `lessThan` function
	 * returns false.
	 *
	 * Function returns true if value in a filterable attribute of such a name is greater than value in second argument.
	 *
	 * Example:
	 *
	 * ```
	 * lessThan('age', 20)
	 * ```
	 */
	@Nullable
	static <T extends Serializable & Comparable<?>> LessThan lessThan(@Nonnull String attributeName, @Nullable T attributeValue) {
		return attributeValue == null ? null : new LessThan(attributeName, attributeValue);
	}

	/**
	 * This `lessThanEquals` is constraint that compares value of the attribute with name passed in first argument with the value passed
	 * in the second argument. First argument must be {@link String},
	 * second argument may be any of {@link Comparable} type.
	 * Type of the attribute value and second argument must be convertible one to another otherwise `lessThanEquals` function
	 * returns false.
	 *
	 * Function returns true if value in a filterable attribute of such a name is lesser than value in second argument or
	 * equal.
	 *
	 * Example:
	 *
	 * ```
	 * lessThanEquals('age', 20)
	 * ```
	 */
	@Nullable
	static <T extends Serializable & Comparable<?>> LessThanEquals lessThanEquals(@Nonnull String attributeName, @Nullable T attributeValue) {
		return attributeValue == null ? null : new LessThanEquals(attributeName, attributeValue);
	}

	/**
	 * This `greaterThan` is constraint that compares value of the attribute with name passed in first argument with the value passed
	 * in the second argument. First argument must be {@link String},
	 * second argument may be any of {@link Comparable} type.
	 * Type of the attribute value and second argument must be convertible one to another otherwise `greaterThan` function
	 * returns false.
	 *
	 * Function returns true if value in a filterable attribute of such a name is greater than value in second argument.
	 *
	 * Example:
	 *
	 * ```
	 * greaterThan('age', 20)
	 * ```
	 */
	@Nullable
	static <T extends Serializable & Comparable<?>> GreaterThan greaterThan(@Nonnull String attributeName, @Nullable T attributeValue) {
		return attributeValue == null ? null : new GreaterThan(attributeName, attributeValue);
	}

	/**
	 * This `greaterThanEquals` is constraint that compares value of the attribute with name passed in first argument with the value passed
	 * in the second argument. First argument must be {@link String},
	 * second argument may be any of {@link Comparable} type.
	 * Type of the attribute value and second argument must be convertible one to another otherwise `greaterThanEquals` function
	 * returns false.
	 *
	 * Function returns true if value in a filterable attribute of such a name is greater than value in second argument or
	 * equal.
	 *
	 * Example:
	 *
	 * ```
	 * greaterThanEquals('age', 20)
	 * ```
	 */
	@Nullable
	static <T extends Serializable & Comparable<?>> GreaterThanEquals greaterThanEquals(@Nonnull String attributeName, @Nullable T attributeValue) {
		return attributeValue == null ? null : new GreaterThanEquals(attributeName, attributeValue);
	}

	/**
	 * This `priceInPriceLists` is constraint accepts one or more [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
	 * arguments that represents primary keys of price lists.
	 *
	 * Function returns true if entity has at least one price in any of specified price lists. This function is also affected by
	 * [priceInCurrency](#price-in-currency) function that limits the examined prices as well. The order of the price lists
	 * passed in the argument is crucial, because it defines the priority of the price lists. Let's have a product with
	 * following prices:
	 *
	 * | priceList       | currency | priceWithVat |
	 * |-----------------|----------|--------------|
	 * | basic           | EUR      | 999.99       |
	 * | registered_user | EUR      | 979.00       |
	 * | b2c_discount    | EUR      | 929.00       |
	 * | b2b_discount    | EUR      | 869.00       |
	 *
	 * If query contains:
	 *
	 * `and(
	 * priceInCurrency('EUR'),
	 * priceInPriceLists('basic', 'b2b_discount'),
	 * priceBetween(800.0, 900.0)
	 * )`
	 *
	 * The product will not be found - because query engine will use first defined price for the price lists in defined order.
	 * It's in our case the price `999.99`, which is not in the defined price interval 800 € - 900 €. If the price lists in
	 * arguments gets switched to `priceInPriceLists('b2b_discount', 'basic')`, the product will be returned, because the first
	 * price is now from `b2b_discount` price list - 869 € and this price is within defined interval.
	 *
	 * This constraint affect also the prices accessible in returned entities. By default, (unless [prices](#prices) requirement
	 * has ALL mode used), returned entities will contain only prices from specified price lists. In other words if entity has
	 * two prices - one from price list `1` and second from price list `2` and `priceInPriceLists(1)` is used in the query
	 * returned entity would have only first price fetched along with it.
	 *
	 * The non-sellable prices are not taken into an account in the search - for example if the product has only non-sellable
	 * price it will never be returned when {@link PriceInPriceLists} constraint or any other price constraint is used in the
	 * query. Non-sellable prices behaves like they don't exist. These non-sellable prices still remain accessible for reading
	 * on fetched entity in case the product is found by sellable price satisfying the filter. If you have specific price list
	 * reserved for non-sellable prices you may still use it in {@link PriceInPriceLists} constraint. It won't affect the set
	 * of returned entities, but it will ensure you can access those non-sellable prices on entities even when
	 * {@link PriceFetchMode#RESPECTING_FILTER} is used in {@link Prices} requirement is used.
	 *
	 * Only single `priceInPriceLists` constraint can be used in the query. Constraint must be defined when other price related
	 * constraints are used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * priceInPriceLists(1, 5, 6)
	 * ```
	 */
	@Nullable
	static PriceInPriceLists priceInPriceLists(@Nullable Serializable... priceList) {
		if (priceList == null) {
			return null;
		}
		final Serializable[] args = Arrays.stream(priceList).filter(Objects::nonNull).toArray(Serializable[]::new);
		return args.length == 0 ? null : new PriceInPriceLists(args);
	}

	/**
	 * This `priceInCurrency` is constraint accepts single {@link String}
	 * argument that represents [currency](https://en.wikipedia.org/wiki/ISO_4217) in ISO 4217 code.
	 *
	 * Function returns true if entity has at least one price with specified currency. This function is also affected by
	 * {@link PriceInPriceLists} function that limits the examined prices as well. When this constraint
	 * is used in the query returned entities will contain only prices matching specified locale. In other words if entity has
	 * two prices: USD and CZK and `priceInCurrency('CZK')` is used in query returned entity would have only Czech crown prices
	 * fetched along with it.
	 *
	 * Only single `priceInCurrency` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * priceInCurrency('USD')
	 * ```
	 */
	@Nullable
	static PriceInCurrency priceInCurrency(@Nullable String currency) {
		return currency == null ? null : new PriceInCurrency(currency);
	}

	/**
	 * This `priceInCurrency` is constraint accepts single {@link String}
	 * argument that represents [currency](https://en.wikipedia.org/wiki/ISO_4217) in ISO 4217 code.
	 *
	 * Function returns true if entity has at least one price with specified currency. This function is also affected by
	 * {@link PriceInPriceLists} function that limits the examined prices as well. When this constraint
	 * is used in the query returned entities will contain only prices matching specified locale. In other words if entity has
	 * two prices: USD and CZK and `priceInCurrency('CZK')` is used in query returned entity would have only Czech crown prices
	 * fetched along with it.
	 *
	 * Only single `priceInCurrency` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * priceInCurrency('USD')
	 * ```
	 */
	@Nullable
	static PriceInCurrency priceInCurrency(@Nullable Currency currency) {
		return currency == null ? null : new PriceInCurrency(currency);
	}

	/**
	 * This `withinHierarchy` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
	 * entity type in first argument, primary key of [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
	 * type of entity with [hierarchical placement](../model/entity_model.md#hierarchical-placement) in second argument. There
	 * are also optional third and fourth arguments - see optional arguments {@link DirectRelation}, {@link ExcludingRoot}
	 * and {@link Excluding}.
	 *
	 * Constraint can also have only one numeric argument representing primary key of [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
	 * the very same entity type in case this entity has [hierarchical placement](../model/entity_model.md#hierarchical-placement)
	 * defined. This format of the constraint may be used for example for returning category sub-tree (where we want to return
	 * category entities and also constraint them by their own hierarchy placement).
	 *
	 * Function returns true if entity has at least one [reference](../model/entity_model.md#references) that relates to specified entity
	 * type and entity either directly or relates to any other entity of the same type with [hierarchical placement](../model/entity_model.md#hierarchical-placement)
	 * subordinate to the directly related entity placement (in other words is present in its sub-tree).
	 *
	 * Let's have following hierarchical tree of categories (primary keys are in brackets):
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 * - big (4)
	 * - small (5)
	 * - Plasma (6)
	 * - Fridges (7)
	 *
	 * When constraint `withinHierarchy('category', 1)` is used in a query targeting product entities only products that
	 * relates directly to categories: `TV`, `Crt`, `LCD`, `big`, `small` and `Plasma` will be returned. Products in `Fridges`
	 * will be omitted because they are not in a sub-tree of `TV` hierarchy.
	 *
	 * Only single `withinHierarchy` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * withinHierarchy('category', 4)
	 * ```
	 *
	 * If you want to constraint the entity that you're querying on you can also omit entity type specification. See example:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinHierarchy(5)
	 * )
	 * )
	 * ```
	 *
	 * This query will return all categories that belong to the sub-tree of category with primary key equal to 5.
	 *
	 * If you want to list all entities from the root level you need to use different constraint - `withinRootHierarchy` that
	 * has the same notation but doesn't specify the id of the root level entity:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinRootHierarchy()
	 * )
	 * )
	 * ```
	 *
	 * This query will return all categories within `CATEGORY` entity.
	 *
	 * You may use this constraint to list entities that refers to the hierarchical entities:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinRootHierarchy('CATEGORY')
	 * )
	 * )
	 * ```
	 *
	 * This query returns all products that are attached to any category. Although, this query doesn't make much sense it starts
	 * to be useful when combined with additional inner constraints described in following paragraphs.
	 *
	 * You can use additional sub constraints in `withinHierarchy` constraint: {@link DirectRelation}, {@link ExcludingRoot}
	 * and {@link Excluding}
	 */
	@Nullable
	static WithinHierarchy withinHierarchy(@Nullable Integer ofParent, @Nonnull HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		return ofParent == null ? null : new WithinHierarchy(ofParent, fineGrainedConstraints);
	}

	/**
	 * This `withinHierarchy` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
	 * entity type in first argument, primary key of [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
	 * type of entity with [hierarchical placement](../model/entity_model.md#hierarchical-placement) in second argument. There
	 * are also optional third and fourth arguments - see optional arguments {@link DirectRelation}, {@link ExcludingRoot}
	 * and {@link Excluding}.
	 *
	 * Constraint can also have only one numeric argument representing primary key of [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
	 * the very same entity type in case this entity has [hierarchical placement](../model/entity_model.md#hierarchical-placement)
	 * defined. This format of the constraint may be used for example for returning category sub-tree (where we want to return
	 * category entities and also constraint them by their own hierarchy placement).
	 *
	 * Function returns true if entity has at least one [reference](../model/entity_model.md#references) that relates to specified entity
	 * type and entity either directly or relates to any other entity of the same type with [hierarchical placement](../model/entity_model.md#hierarchical-placement)
	 * subordinate to the directly related entity placement (in other words is present in its sub-tree).
	 *
	 * Let's have following hierarchical tree of categories (primary keys are in brackets):
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 * - big (4)
	 * - small (5)
	 * - Plasma (6)
	 * - Fridges (7)
	 *
	 * When constraint `withinHierarchy('category', 1)` is used in a query targeting product entities only products that
	 * relates directly to categories: `TV`, `Crt`, `LCD`, `big`, `small` and `Plasma` will be returned. Products in `Fridges`
	 * will be omitted because they are not in a sub-tree of `TV` hierarchy.
	 *
	 * Only single `withinHierarchy` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * withinHierarchy('category', 4)
	 * ```
	 *
	 * If you want to constraint the entity that you're querying on you can also omit entity type specification. See example:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinHierarchy(5)
	 * )
	 * )
	 * ```
	 *
	 * This query will return all categories that belong to the sub-tree of category with primary key equal to 5.
	 *
	 * If you want to list all entities from the root level you need to use different constraint - `withinRootHierarchy` that
	 * has the same notation but doesn't specify the id of the root level entity:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinRootHierarchy()
	 * )
	 * )
	 * ```
	 *
	 * This query will return all categories within `CATEGORY` entity.
	 *
	 * You may use this constraint to list entities that refers to the hierarchical entities:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinRootHierarchy('CATEGORY')
	 * )
	 * )
	 * ```
	 *
	 * This query returns all products that are attached to any category. Although, this query doesn't make much sense it starts
	 * to be useful when combined with additional inner constraints described in following paragraphs.
	 *
	 * You can use additional sub constraints in `withinHierarchy` constraint: {@link DirectRelation}, {@link ExcludingRoot}
	 * and {@link Excluding}
	 */
	@Nullable
	static WithinHierarchy withinHierarchy(@Nonnull Serializable entityType, @Nullable Integer ofParent, @Nonnull HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		return ofParent == null ? null : new WithinHierarchy(entityType, ofParent, fineGrainedConstraints);
	}

	/**
	 * This `withinRootHierarchy` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
	 * entity type in first argument. There are also optional argument - see {@link Excluding}.
	 *
	 * Function returns true if entity has at least one [reference](../model/entity_model.md#references) that relates to specified entity
	 * type and entity either directly or relates to any other entity of the same type with [hierarchical placement](../model/entity_model.md#hierarchical-placement)
	 * subordinate to the directly related entity placement (in other words is present in its sub-tree).
	 *
	 * Let's have following hierarchical tree of categories (primary keys are in brackets):
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 * - big (4)
	 * - small (5)
	 * - Plasma (6)
	 * - Fridges (7)
	 *
	 * When constraint `withinRootHierarchy('category')` is used in a query targeting product entities all products that
	 * relates to any of categories will be returned.
	 *
	 * Only single `withinRootHierarchy` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * withinRootHierarchy('category')
	 * ```
	 *
	 * If you want to constraint the entity that you're querying on you can also omit entity type specification. See example:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinRootHierarchy()
	 * )
	 * )
	 * ```
	 *
	 * This query will return all categories within `CATEGORY` entity.
	 *
	 * You may use this constraint to list entities that refers to the hierarchical entities:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinRootHierarchy('CATEGORY')
	 * )
	 * )
	 * ```
	 *
	 * This query returns all products that are attached to any category.
	 */
	@Nonnull
	static WithinRootHierarchy withinRootHierarchy(@Nonnull HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		return new WithinRootHierarchy(fineGrainedConstraints);
	}

	/**
	 * This `withinRootHierarchy` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
	 * entity type in first argument. There are also optional argument - see {@link Excluding}.
	 *
	 * Function returns true if entity has at least one [reference](../model/entity_model.md#references) that relates to specified entity
	 * type and entity either directly or relates to any other entity of the same type with [hierarchical placement](../model/entity_model.md#hierarchical-placement)
	 * subordinate to the directly related entity placement (in other words is present in its sub-tree).
	 *
	 * Let's have following hierarchical tree of categories (primary keys are in brackets):
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 * - big (4)
	 * - small (5)
	 * - Plasma (6)
	 * - Fridges (7)
	 *
	 * When constraint `withinRootHierarchy('category')` is used in a query targeting product entities all products that
	 * relates to any of categories will be returned.
	 *
	 * Only single `withinRootHierarchy` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * withinRootHierarchy('category')
	 * ```
	 *
	 * If you want to constraint the entity that you're querying on you can also omit entity type specification. See example:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinRootHierarchy()
	 * )
	 * )
	 * ```
	 *
	 * This query will return all categories within `CATEGORY` entity.
	 *
	 * You may use this constraint to list entities that refers to the hierarchical entities:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinRootHierarchy('CATEGORY')
	 * )
	 * )
	 * ```
	 *
	 * This query returns all products that are attached to any category.
	 */
	@Nonnull
	static WithinRootHierarchy withinRootHierarchy(@Nonnull Serializable entityType, @Nonnull HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		return new WithinRootHierarchy(entityType, fineGrainedConstraints);
	}

	/**
	 * If you use `excludingRoot` sub-constraint in `withinHierarchy` parent, you can specify one or more
	 * [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html) primary keys of the underlying
	 * entities which hierarchical subtree should be excluded from examination.
	 *
	 * Exclusion arguments allows excluding certain parts of the hierarchy tree from examination. This feature is used in
	 * environments where certain sub-trees can be made "invisible" and should not be accessible to users, although they are
	 * still part of the database.
	 *
	 * Let's have following hierarchical tree of categories (primary keys are in brackets):
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 * - big (4)
	 * - small (5)
	 * - Plasma (6)
	 * - Fridges (7)
	 *
	 * When constraint `withinHierarchy('category', 1, excluding(3))` is used in a query targeting product entities,
	 * only products that relate directly to categories: `TV`, `Crt` and `Plasma` will be returned. Products in `Fridges` will
	 * be omitted because they are not in a sub-tree of `TV` hierarchy and products in `LCD` sub-tree will be omitted because
	 * they're part of the excluded sub-trees.
	 */
	@Nullable
	static Excluding excluding(@Nullable Integer... excludeChildTree) {
		if (excludeChildTree == null) {
			return null;
		}
		final Integer[] args = Arrays.stream(excludeChildTree).filter(Objects::nonNull).toArray(Integer[]::new);
		return args.length == 0 ? null : new Excluding(args);
	}

	/**
	 * This constraint can be used only as sub constraint of `withinHierarchy` or `withinRootHierarchy`.
	 * If you use `directRelation` sub-constraint fetching products related to category - only products that are directly
	 * related to that category will be returned in the response.
	 *
	 * Let's have the following category tree:
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 * - AMOLED (4)
	 *
	 * These categories are related by following products:
	 *
	 * - TV (1):
	 * - Product Philips 32"
	 * - Product Samsung 24"
	 * - Crt (2):
	 * - Product Ilyiama 15"
	 * - Product Panasonic 17"
	 * - LCD (3):
	 * - Product BenQ 32"
	 * - Product LG 28"
	 * - AMOLED (4):
	 * - Product Samsung 32"
	 *
	 * When using this query:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinHierarchy('CATEGORY', 1)
	 * )
	 * )
	 * ```
	 *
	 * All products will be returned.
	 *
	 * When this query is used:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinHierarchy('CATEGORY', 1, directRelation())
	 * )
	 * )
	 * ```
	 *
	 * Only products directly related to TV category will be returned - i.e.: Philips 32" and Samsung 24". Products related
	 * to sub-categories of TV category will be omitted.
	 *
	 * You can also use this hint to browse the hierarchy of the entity itself - to fetch subcategories of category.
	 * If you use this query:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinHierarchy(1)
	 * )
	 * )
	 * ```
	 *
	 * All categories under the category subtree of `TV (1)` will be listed (this means categories `TV`, `Crt`, `LCD`, `AMOLED`).
	 * If you use this query:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinHierarchy(1, directRelation())
	 * )
	 * )
	 * ```
	 *
	 * Only direct sub-categories of category `TV (1)` will be listed (this means categories `Crt` and `LCD`).
	 * You can also use this hint with constraint `withinRootHierarchy`:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinRootHierarchy()
	 * )
	 * )
	 * ```
	 *
	 * All categories in entire tree will be listed.
	 *
	 * When using this query:
	 *
	 * ```
	 * query(
	 * entities('CATEGORY'),
	 * filterBy(
	 * withinHierarchy(directRelation())
	 * )
	 * )
	 * ```
	 *
	 * Which would return only category `TV (1)`.
	 *
	 * As you can see {@link ExcludingRoot} and {@link DirectRelation} are mutually exclusive.
	 */
	@Nonnull
	static DirectRelation directRelation() {
		return new DirectRelation();
	}

	/**
	 * If you use `excludingRoot` sub-constraint in `withinHierarchy` parent, response will contain only children of the
	 * entity specified in `withinHierarchy` or entities related to those children entities - if the `withinHierarchy` targes
	 * different entity types.
	 *
	 * Let's have following category tree:
	 *
	 * - TV (1)
	 * - Crt (2)
	 * - LCD (3)
	 *
	 * These categories are related by following products:
	 *
	 * - TV (1):
	 * - Product Philips 32"
	 * - Product Samsung 24"
	 * - Crt (2):
	 * - Product Ilyiama 15"
	 * - Product Panasonic 17"
	 * - LCD (3):
	 * - Product BenQ 32"
	 * - Product LG 28"
	 *
	 * When using this query:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinHierarchy('CATEGORY', 1)
	 * )
	 * )
	 * ```
	 *
	 * All products will be returned.
	 * When this query is used:
	 *
	 * ```
	 * query(
	 * entities('PRODUCT'),
	 * filterBy(
	 * withinHierarchy('CATEGORY', 1, excludingRoot())
	 * )
	 * )
	 * ```
	 *
	 * Only products related to sub-categories of the TV category will be returned - i.e.: Ilyiama 15", Panasonic 17" and
	 * BenQ 32", LG 28". The products related directly to TV category will not be returned.
	 *
	 * As you can see {@link ExcludingRoot} and {@link DirectRelation} are mutually exclusive.
	 */
	@Nonnull
	static ExcludingRoot excludingRoot() {
		return new ExcludingRoot();
	}

	/**
	 * This `language` is constraint accepts single {@link Locale} argument.
	 *
	 * Function returns true if entity has at least one localized attribute or associated data that  targets specified locale.
	 *
	 * If require part of the query doesn't contain {@link io.evitadb.api.query.require.DataInLanguage} requirement that
	 * would specify the requested data localization, this filtering constraint implicitly sets requirement to the passed
	 * language argument. In other words if entity has two localizations: `en-US` and `cs-CZ` and `language('cs-CZ')` is
	 * used in query, returned entity would have only Czech localization of attributes and associated data fetched along
	 * with it (and also attributes that are locale agnostic).
	 *
	 * If query contains no language constraint filtering logic is applied only on "global" (i.e. language agnostic)
	 * attributes.
	 *
	 * Only single `language` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * language('en-US')
	 * ```
	 */
	@Nullable
	static Language language(@Nullable Locale locale) {
		return locale == null ? null : new Language(locale);
	}

	/**
	 * This `inRange` is constraint that compares value of the attribute with name passed in first argument with the date
	 * and time passed in the second argument. First argument must be {@link String},
	 * second argument must be {@link ZonedDateTime} type.
	 * Type of the attribute value must implement [Range](classes/range_interface.md) interface.
	 *
	 * Function returns true if second argument is greater than or equal to range start (from), and is lesser than
	 * or equal to range end (to).
	 *
	 * Example:
	 *
	 * ```
	 * inRange('valid', 2020-07-30T20:37:50+00:00)
	 * ```
	 */
	@Nullable
	static InRange inRange(@Nonnull String attributeName, @Nullable ZonedDateTime atTheMoment) {
		return atTheMoment == null ? null : new InRange(attributeName, atTheMoment);
	}

	/**
	 * This `inRange` is constraint that compares value of the attribute with name passed in first argument with
	 * the number passed in the second argument. First argument must be {@link String},
	 * second argument must be {@link Number} type.
	 * Type of the attribute value must implement [Range](classes/range_interface.md) interface.
	 *
	 * Function returns true if second argument is greater than or equal to range start (from), and is lesser than
	 * or equal to range end (to).
	 *
	 * Example:
	 *
	 * ```
	 * inRange('age', 18)
	 * ```
	 */
	@Nullable
	static InRange inRange(@Nonnull String attributeName, @Nullable Number theValue) {
		return theValue == null ? null : new InRange(attributeName, theValue);
	}

	/**
	 * This `inRange` is constraint that compares value of the attribute with name passed in first argument with current
	 * date and time.
	 * Type of the attribute value must implement [Range](classes/range_interface.md) interface.
	 *
	 * Function returns true if second argument is greater than or equal to range start (from), and is lesser than
	 * or equal to range end (to).
	 *
	 * Example:
	 *
	 * ```
	 * inRange('valid')
	 * ```
	 */
	@Nonnull
	static InRange inRangeNow(@Nonnull String attributeName) {
		return new InRange(attributeName);
	}

	/**
	 * This `inSet` is constraint that compares value of the attribute with name passed in first argument with all the values passed
	 * in the second, third and additional arguments. First argument must be {@link String},
	 * additional arguments may be any of {@link Comparable} type.
	 * Type of the attribute value and additional arguments must be convertible one to another otherwise `in` function
	 * skips value comparison and ultimately returns false.
	 *
	 * Function returns true if attribute value is equal to at least one of additional values.
	 *
	 * Example:
	 *
	 * ```
	 * inSet('level', 1, 2, 3)
	 * ```
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	static <T extends Serializable & Comparable<?>> InSet inSet(@Nonnull String attributeName, @Nullable T... set) {
		if (set == null) {
			return null;
		}
		final List<T> args = Arrays.stream(set).filter(Objects::nonNull).collect(Collectors.toList());
		if (args.isEmpty()) {
			return null;
		} else if (args.size() == set.length) {
			return new InSet(attributeName, set);
		} else {
			final T[] limitedSet = (T[]) Array.newInstance(set.getClass().getComponentType(), args.size());
			for (int i = 0; i < args.size(); i++) {
				limitedSet[i] = args.get(i);
			}
			return new InSet(attributeName, limitedSet);
		}
	}

	/**
	 * This `isFalse` is constraint that compares value of the attribute with name passed in first argument with boolean FALSE value.
	 * First argument must be {@link String}.
	 * Type of the attribute value must be convertible to Boolean otherwise `isFalse` function returns false.
	 *
	 * Function returns true if attribute value equals to {@link Boolean#FALSE}.
	 *
	 * Example:
	 *
	 * ```
	 * isFalse('visible')
	 * ```
	 */
	@Nonnull
	static IsFalse isFalse(@Nonnull String attributeName) {
		return new IsFalse(attributeName);
	}

	/**
	 * This `isTrue` is constraint that compares value of the attribute with name passed in first argument with boolean TRUE value.
	 * First argument must be {@link String}.
	 * Type of the attribute value must be convertible to Boolean otherwise `isTrue` function returns false.
	 *
	 * Function returns true if attribute value equals to {@link Boolean#TRUE}.
	 *
	 * Example:
	 *
	 * ```
	 * isTrue('visible')
	 * ```
	 */
	@Nonnull
	static IsTrue isTrue(@Nonnull String attributeName) {
		return new IsTrue(attributeName);
	}

	/**
	 * This `isNull` is constraint that checks existence of value of the attribute with name passed in first argument.
	 * First argument must be {@link String}.
	 * Attribute must not exist in order `isNull` function returns true.
	 *
	 * Function returns true if attribute doesn't exist.
	 *
	 * Example:
	 *
	 * ```
	 * isNull('visible')
	 * ```
	 */
	@Nonnull
	static IsNull isNull(@Nonnull String attributeName) {
		return new IsNull(attributeName);
	}

	/**
	 * This `isNotNull` is constraint that checks existence of value of the attribute with name passed in first argument.
	 * First argument must be {@link String}.
	 * Attribute must exist in order `isNotNull` function returns true.
	 *
	 * Function returns true if attribute exists.
	 *
	 * Example:
	 *
	 * ```
	 * isNotNull('visible')
	 * ```
	 */
	@Nonnull
	static IsNotNull isNotNull(@Nonnull String attributeName) {
		return new IsNotNull(attributeName);
	}

	/**
	 * This `priceBetween` constraint accepts two {@link BigDecimal} arguments that represents lower and higher price
	 * bounds (inclusive).
	 *
	 * Function returns true if entity has sellable price in most prioritized price list according to {@link PriceInPriceLists}
	 * constraint greater than or equal to passed lower bound and lesser than or equal to passed higher bound. This function
	 * is also affected by other price related constraints such as {@link PriceInCurrency} functions that limits the examined
	 * prices as well.
	 *
	 * Most prioritized price term relates to [price computation algorithm](price_computation.md) described in special article.
	 *
	 * By default, price with VAT is used for filtering, you can change this by using {@link UseOfPrice} require constraint.
	 * Non-sellable prices doesn't participate in the filtering at all.
	 *
	 * Only single `priceBetween` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * priceBetween(150.25, 220.0)
	 * ```
	 */
	@Nonnull
	static PriceBetween priceBetween(@Nonnull BigDecimal from, @Nonnull BigDecimal to) {
		return new PriceBetween(from, to);
	}

	/**
	 * This `priceValidIn` is constraint accepts single {@link ZonedDateTime}
	 * argument that represents the moment in time for which entity price must be valid.
	 *
	 * Function returns true if entity has at least one price which validity start (valid from) is lesser or equal to passed
	 * date and time and validity end (valid to) is greater or equal to passed date and time. This function is also affected by
	 * {@link PriceInCurrency} and {@link PriceInPriceLists} functions that limits the examined prices as well.
	 * When this constraint is used in the query returned entities will contain only prices which validity settings match
	 * specified date and time.
	 *
	 * Only single `priceValidIn` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * priceValidIn(2020-07-30T20:37:50+00:00)
	 * ```
	 */
	@Nullable
	static PriceValidIn priceValidIn(@Nullable ZonedDateTime theMoment) {
		return theMoment == null ? null : new PriceValidIn(theMoment);
	}

	/**
	 * This `priceValidIn` is constraint uses current date and time for price validity examination.
	 *
	 * Function returns true if entity has at least one price which validity start (valid from) is lesser or equal to passed
	 * date and time and validity end (valid to) is greater or equal to passed date and time. This function is also affected by
	 * {@link PriceInCurrency} and {@link PriceInPriceLists} functions that limits the examined prices as well.
	 * When this constraint is used in the query returned entities will contain only prices which validity settings match
	 * specified date and time.
	 *
	 * Only single `priceValidIn` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * priceValidIn()
	 * ```
	 */
	@Nonnull
	static PriceValidIn priceValidNow() {
		return new PriceValidIn();
	}

	/**
	 * This `facet` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
	 * entity type in first argument and one or more
	 * additional [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
	 * arguments that represents [facets](../model/entity_model.md#facets) that entity is required to have in order to match
	 * this constraint.
	 *
	 * Function returns true if entity has a facet for specified entity type and matches passed primary keys in additional
	 * arguments. By matching we mean, that entity has to have any of its facet (with particular type) primary keys equal to at
	 * least one primary key specified in additional arguments.
	 *
	 * Example:
	 *
	 * ```
	 * query(
	 * entities('product'),
	 * filterBy(
	 * userFilter(
	 * facet('category', 4, 5),
	 * facet('group', 7, 13)
	 * )
	 * )
	 * )
	 * ```
	 *
	 * Constraint may be used only in [user filter](#user-filter) container. By default, facets of the same type within same
	 * group are combined by conjunction (OR), facets of different types / groups are combined by disjunction (AND). This
	 * default behaviour can be controlled exactly by using any of following require constraints:
	 *
	 * - [facet groups conjunction](#facet-groups-conjunction) - changes relationship between facets in the same group
	 * - [facet groups disjunction](#facet-groups-disjunction) - changes relationship between facet groups
	 *
	 * ***Note:** you may ask why facet relation is specified by [require](#require) and not directly part of
	 * the [filter](#filter)
	 * body. The reason is simple - facet relation in certain group is usually specified system-wide and doesn't change in time
	 * frequently. This means that it could be easily cached and passing this information in an extra require simplifies query
	 * construction process.*
	 *
	 * *Another reason is that we need to know relationships among facet groups even for types/groups that hasn't yet been
	 * selected by the user in order to be able to compute [facet summary](#facet-summary) output.*
	 */
	@Nullable
	static <T extends Serializable> Facet facet(@Nonnull T entityType, @Nullable Integer... facetId) {
		if (facetId == null) {
			return null;
		}
		final Integer[] args = Arrays.stream(facetId).filter(Objects::nonNull).toArray(Integer[]::new);
		return args.length == 0 ? null : new Facet(entityType, args);
	}

	/**
	 * This `primaryKey` is constraint that accepts set of {@link Integer}
	 * that represents primary keys of the entities that should be returned.
	 *
	 * Function returns true if entity primary key is part of the passed set of integers.
	 * This form of entity lookup function is the fastest one.
	 *
	 * Only single `primaryKey` constraint can be used in the query.
	 *
	 * Example:
	 *
	 * ```
	 * primaryKey(1, 2, 3)
	 * ```
	 */
	@Nullable
	static PrimaryKey primaryKey(@Nullable Integer... primaryKey) {
		if (primaryKey == null) {
			return null;
		}
		final Integer[] args = Arrays.stream(primaryKey).filter(Objects::nonNull).toArray(Integer[]::new);
		return args.length == 0 ? null : new PrimaryKey(args);
	}

	/*
		ORDERING
	 */

	/**
	 * This `orderBy` is container for ordering that contains two or more inner ordering functions which output is combined. Ordering
	 * process is as follows:
	 *
	 * - first ordering evaluated, entities missing requested attribute value are excluded to intermediate bucket
	 * - next ordering is evaluated using entities present in an intermediate bucket, entities missing requested attribute
	 * are excluded to new intermediate bucket
	 * - second step is repeated until all orderings are processed
	 * - content of the last intermediate bucket is appended to the result ordered by the primary key in ascending order
	 *
	 * Entities with same (equal) values must not be subject to secondary ordering rules and may be sorted randomly within
	 * the scope of entities with the same value (this is subject to change, currently this behaviour differs from the one
	 * used by relational databases - but might be more performant).
	 *
	 * Example:
	 *
	 * ```
	 * orderBy(
	 * ascending('code'),
	 * ascending('create'),
	 * priceDescending()
	 * )
	 * ```
	 */
	@Nullable
	static OrderBy orderBy(@Nullable OrderConstraint... constraints) {
		if (constraints == null) {
			return null;
		}
		final OrderConstraint[] args = Arrays.stream(constraints).filter(Objects::nonNull).toArray(OrderConstraint[]::new);
		return args.length == 0 ? null : new OrderBy(args);
	}

	/**
	 * This `referenceAttribute` container is ordering that sorts returned entities by reference attributes. Ordering is
	 * specified by inner constraints. Price related orderings cannot be used here, because references don't posses of prices.
	 *
	 * Example:
	 *
	 * ```
	 * referenceAttribute(
	 * 'CATEGORY',
	 * ascending('categoryPriority')
	 * )
	 * ```
	 *
	 * or
	 *
	 * ```
	 * referenceAttribute(
	 * 'CATEGORY',
	 * ascending('categoryPriority'),
	 * descending('stockPriority')
	 * )
	 * ```
	 */
	@Nullable
	static ReferenceAttribute referenceAttribute(@Nonnull Serializable entityType, @Nullable OrderConstraint... constraints) {
		if (constraints == null) {
			return null;
		}
		final OrderConstraint[] args = Arrays.stream(constraints).filter(Objects::nonNull).toArray(OrderConstraint[]::new);
		return args.length == 0 ? null : new ReferenceAttribute(entityType, args);
	}

	/**
	 * This `ascending` is ordering that sorts returned entities by values in attribute with name passed in the first argument
	 * in ascending order. Argument must be of {@link String} type.
	 * Ordering is executed by natural order of the {@link Comparable}
	 * type.
	 *
	 * Example:
	 *
	 * ```
	 * ascending('age')
	 * ```
	 */
	@Nonnull
	static Ascending ascending(@Nonnull String attributeName) {
		return new Ascending(attributeName);
	}

	/**
	 * This `descending` is ordering that sorts returned entities by values in attribute with name passed in the first argument
	 * in descending order. Argument must be of {@link String} type.
	 * Ordering is executed by reversed natural order of the {@link Comparable}
	 * type.
	 *
	 * Example:
	 *
	 * ```
	 * descending('age')
	 * ```
	 */
	@Nonnull
	static Descending descending(@Nonnull String attributeName) {
		return new Descending(attributeName);
	}

	/**
	 * This `priceAscending` is ordering that sorts returned entities by most priority price in ascending order.
	 * Most priority price relates to [price computation algorithm](price_computation.md) described in special article.
	 *
	 * Example:
	 *
	 * ```
	 * priceAscending()
	 * ```
	 */
	@Nonnull
	static PriceAscending priceAscending() {
		return new PriceAscending();
	}

	/**
	 * This `priceDescending` is ordering that sorts returned entities by most priority price in descending order.
	 * Most priority price relates to [price computation algorithm](price_computation.md) described in special article.
	 *
	 * Example:
	 *
	 * ```
	 * priceDescending()
	 * ```
	 */
	@Nonnull
	static PriceDescending priceDescending() {
		return new PriceDescending();
	}

	/**
	 * This `random` is ordering that sorts returned entities in random order.
	 *
	 * Example:
	 *
	 * ```
	 * random()
	 * ```
	 */
	@Nonnull
	static Random random() {
		return new Random();
	}

	/*
		requirement
	 */

	/**
	 * This `requirement` is container for additonal requirements. It contains two or more inner requirement functions.
	 *
	 * Example:
	 *
	 * ```
	 * require(
	 * page(1, 2),
	 * entityBody()
	 * )
	 * ```
	 */
	@Nullable
	static Require require(@Nullable RequireConstraint... constraints) {
		if (constraints == null) {
			return null;
		}
		final RequireConstraint[] args = Arrays.stream(constraints).filter(Objects::nonNull).toArray(RequireConstraint[]::new);
		return args.length == 0 ? null : new Require(args);
	}

	/**
	 * This `attributeHistogram` requirement usage triggers computing and adding an object to the result index. It has single
	 * argument that states the number of histogram buckets (columns) that can be safely visualized to the user. Usually
	 * there is fixed size area dedicated to the histogram visualisation and there is no sense to return histogram with
	 * so many buckets (columns) that wouldn't be possible to render. For example - if there is 200px size for the histogram
	 * and we want to dedicate 10px for one column, it's wise to ask for 20 buckets.
	 *
	 * It accepts one or more {@link String} arguments as second, third (and so on) argument that specify filterable attribute
	 * name for which [histograms](https://en.wikipedia.org/wiki/Histogram) should be computed. Attribute must contain only
	 * numeric values in order to compute histogram data.
	 *
	 * When this requirement is used an additional object {@link java.util.Map} is
	 * stored to result. Key of this map is {@link String} of attribute
	 * name and value is the [Histogram](classes/histogram.md).
	 *
	 * Example:
	 *
	 * ```
	 * attributeHistogram(20, 'width', 'height')
	 * ```
	 */
	@Nonnull
	static AttributeHistogram attributeHistogram(int requestedBucketCount, @Nonnull String... attributeName) {
		return new AttributeHistogram(requestedBucketCount, attributeName);
	}

	/**
	 * This `priceHistogram` requirement usage triggers computing and adding an object to the result index. It has single
	 * argument that states the number of histogram buckets (columns) that can be safely visualized to the user. Usually
	 * there is fixed size area dedicated to the histogram visualisation and there is no sense to return histogram with
	 * so many buckets (columns) that wouldn't be possible to render. For example - if there is 200px size for the histogram
	 * and we want to dedicate 10px for one column, it's wise to ask for 20 buckets.
	 *
	 * When this requirement is used an additional object {@link PriceHistogram} is stored to the result. Histogram
	 * contains statistics on price layout in the query result.
	 *
	 * Example:
	 *
	 * ```
	 * priceHistogram(20)
	 * ```
	 */
	@Nonnull
	static PriceHistogram priceHistogram(int requestedBucketCount) {
		return new PriceHistogram(requestedBucketCount);
	}

	/**
	 * This `facetGroupsConjunction` require allows specifying inter-facet relation inside facet groups of certain primary ids.
	 * First mandatory argument specifies entity type of the facet group, secondary argument allows to define one more facet
	 * group ids which inner facets should be considered conjunctive.
	 *
	 * This require constraint changes default behaviour stating that all facets inside same facet group are combined by OR
	 * relation (eg. disjunction). Constraint has sense only when [facet](#facet) constraint is part of the query.
	 *
	 * Example:
	 *
	 * <pre>
	 * query(
	 *    entities('product'),
	 *    filterBy(
	 *       userFilter(
	 *          facet('group', 1, 2),
	 *          facet('parameterType', 11, 12, 22)
	 *       )
	 *    ),
	 *    require(
	 *       facetGroupsConjunction('parameterType', 1, 8, 15)
	 *    )
	 * )
	 * </pre>
	 *
	 * This statement means, that facets in `parameterType` groups `1`, `8`, `15` will be joined with boolean AND relation when
	 * selected.
	 *
	 * Let's have this facet/group situation:
	 *
	 * Color `parameterType` (group id: 1):
	 *
	 * - blue (facet id: 11)
	 * - red (facet id: 12)
	 *
	 * Size `parameterType` (group id: 2):
	 *
	 * - small (facet id: 21)
	 * - large (facet id: 22)
	 *
	 * Flags `tag` (group id: 3):
	 *
	 * - action products (facet id: 31)
	 * - new products (facet id: 32)
	 *
	 * When user selects facets: blue (11), red (12) by default relation would be: get all entities that have facet blue(11) OR
	 * facet red(12). If require `facetGroupsConjunction('parameterType', 1)` is passed in the query filtering condition will
	 * be composed as: blue(11) AND red(12)
	 */
	@Nullable
	static FacetGroupsConjunction facetGroupsConjunction(@Nonnull Serializable entityType, @Nullable Integer... facetGroups) {
		if (facetGroups == null) {
			return null;
		}
		final Integer[] args = Arrays.stream(facetGroups).filter(Objects::nonNull).toArray(Integer[]::new);
		return args.length == 0 ? null : new FacetGroupsConjunction(entityType, args);
	}

	/**
	 * This `facetGroupsDisjunction` require constraint allows specifying facet relation among different facet groups of certain
	 * primary ids. First mandatory argument specifies entity type of the facet group, secondary argument allows to define one
	 * more facet group ids that should be considered disjunctive.
	 *
	 * This require constraint changes default behaviour stating that facets between two different facet groups are combined by
	 * AND relation and changes it to the disjunction relation instead.
	 *
	 * Example:
	 *
	 * <pre>
	 * query(
	 *    entities('product'),
	 *    filterBy(
	 *       userFilter(
	 *          facet('group', 1, 2),
	 *          facet('parameterType', 11, 12, 22)
	 *       )
	 *    ),
	 *    require(
	 *       facetGroupsDisjunction('parameterType', 1, 2)
	 *    )
	 * )
	 * </pre>
	 *
	 * This statement means, that facets in `parameterType` facet groups `1`, `2` will be joined with the rest of the query by
	 * boolean OR relation when selected.
	 *
	 * Let's have this facet/group situation:
	 *
	 * Color `parameterType` (group id: 1):
	 *
	 * - blue (facet id: 11)
	 * - red (facet id: 12)
	 *
	 * Size `parameterType` (group id: 2):
	 *
	 * - small (facet id: 21)
	 * - large (facet id: 22)
	 *
	 * Flags `tag` (group id: 3):
	 *
	 * - action products (facet id: 31)
	 * - new products (facet id: 32)
	 *
	 * When user selects facets: blue (11), large (22), new products (31) - the default meaning would be: get all entities that
	 * have facet blue as well as facet large and action products tag (AND). If require `facetGroupsDisjunction('tag', 3)`
	 * is passed in the query, filtering condition will be composed as: (`blue(11)` AND `large(22)`) OR `new products(31)`
	 */
	@Nullable
	static FacetGroupsDisjunction facetGroupsDisjunction(@Nonnull Serializable entityType, @Nullable Integer... facetGroups) {
		if (facetGroups == null) {
			return null;
		}
		final Integer[] args = Arrays.stream(facetGroups).filter(Objects::nonNull).toArray(Integer[]::new);
		return args.length == 0 ? null : new FacetGroupsDisjunction(entityType, args);
	}

	/**
	 * This `facetGroupsNegation` requirement allows specifying facet relation inside facet groups of certain primary ids. Negative facet
	 * groups results in omitting all entities that have requested facet in query result. First mandatory argument specifies
	 * entity type of the facet group, secondary argument allows to define one more facet group ids that should be considered
	 * negative.
	 *
	 * Example:
	 *
	 * ```
	 * facetGroupsNegation('parameterType', 1, 8, 15)
	 * ```
	 *
	 * This statement means, that facets in 'parameterType' groups `1`, `8`, `15` will be joined with boolean AND NOT relation
	 * when selected.
	 */
	@Nullable
	static FacetGroupsNegation facetGroupsNegation(@Nonnull Serializable entityType, @Nullable Integer... facetGroups) {
		if (facetGroups == null) {
			return null;
		}
		final Integer[] args = Arrays.stream(facetGroups).filter(Objects::nonNull).toArray(Integer[]::new);
		return args.length == 0 ? null : new FacetGroupsNegation(entityType, args);
	}

	/**
	 * This `hierarchyStatistics` require constraint triggers computing the statistics for referenced hierarchical entities and adds
	 * an object to the result index. It has at least one {@link Serializable}
	 * argument that specifies type of hierarchical entity that this entity relates to. Additional arguments allow passing
	 * requirements for fetching the referenced entity contents so that there are no other requests to the Evita DB necessary
	 * and all data are fetched in single query.
	 *
	 * When this require constraint is used an additional object is stored to result index:
	 *
	 * - **HierarchyStatistics**
	 * this object is organized in the tree structure that reflects the hierarchy of the entities of desired type that are
	 * referenced by entities returned by primary query, for each tree entity there is a number that represents the count of
	 * currently queried entities that relates to that referenced hierarchical entity and match the query filter - either
	 * directly or to some subordinate entity of this hierarchical entity
	 *
	 * Example:
	 *
	 * <pre>
	 * hierarchyStatistics('category')
	 * hierarchyStatistics('category', entityBody(), attributes())
	 * </pre>
	 *
	 * This require constraint is usually used when hierarchical menu rendering is needed. For example when we need to render
	 * menu for entire e-commerce site, but we want to take excluded subtrees into an account and also reflect the filtering
	 * conditions that may filter out dozens of products (and thus leading to empty categories) we can invoke following query:
	 *
	 * <pre>
	 * query(
	 *     entities('PRODUCT'),
	 *     filterBy(
	 *         and(
	 *             eq('visible', true),
	 *             inRange('valid', 2020-07-30T20:37:50+00:00),
	 *             priceInCurrency('USD'),
	 *             priceValidIn(2020-07-30T20:37:50+00:00),
	 *             priceInPriceLists('vip', 'standard'),
	 *             withinRootHierarchy('CATEGORY', excluding(3, 7))
	 *         )
	 *     ),
	 *     require(
	 *         page(1, 20),
	 *         hierarchyStatistics('CATEGORY', entityBody(), attributes())
	 *     )
	 * )
	 * </pre>
	 *
	 * This query would return first page with 20 products (omitting hundreds of others on additional pages) but also returns a
	 * HierarchyStatistics in additional data. This object may contain following structure:
	 *
	 * <pre>
	 * Electronics -> 1789
	 *     TV -> 126
	 *         LED -> 90
	 *         CRT -> 36
	 *     Washing machines -> 190
	 *         Slim -> 40
	 *         Standard -> 40
	 *         With drier -> 23
	 *         Top filling -> 42
	 *         Smart -> 45
	 *     Cell phones -> 350
	 *     Audio / Video -> 230
	 *     Printers -> 80
	 * </pre>
	 *
	 * The tree will contain category entities loaded with `attributes` instead the names you see in the example. The number
	 * after the arrow represents the count of the products that are referencing this category (either directly or some of its
	 * children). You can see there are only categories that are valid for the passed query - excluded category subtree will
	 * not be part of the category listing (query filters out all products with excluded category tree) and there is also no
	 * category that happens to be empty (e.g. contains no products or only products that don't match the filter constraint).
	 */
	@Nonnull
	static HierarchyStatistics hierarchyStatistics(@Nonnull Serializable entityType, @Nonnull EntityContentRequire... requirements) {
		if (entityType instanceof EntityContentRequire) {
			return new HierarchyStatistics(null, ArrayUtils.mergeArrays(new EntityContentRequire[]{(EntityContentRequire) entityType}, requirements));
		} else {
			return new HierarchyStatistics(entityType, requirements);
		}
	}

	/**
	 * This `entity` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
	 * this requirement is used result contains [entity bodies](entity_model.md) except `attributes`, `associated data` and `prices` that could
	 * become big. These type of data can be fetched either lazily or by specifying additional requirements in the query.
	 *
	 * Example:
	 *
	 * ```
	 * entityBody()
	 * ```
	 */
	@Nonnull
	static EntityBody entityBody() {
		return new EntityBody();
	}

	/**
	 * This `attributes` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
	 * this requirement is used result contains [entity bodies](entity_model.md) except `associated data` that could
	 * become big. These type of data can be fetched either lazily or by specifying additional requirements in the query.
	 *
	 * This requirement implicitly triggers {@link EntityBody} requirement because attributes cannot be returned without entity.
	 * [Localized interface](classes/localized_interface.md) attributes are returned according to {@link Language}
	 * constraint.
	 *
	 * Example:
	 *
	 * ```
	 * attributes()
	 * ```
	 */
	@Nonnull
	static Attributes attributes() {
		return new Attributes();
	}

	/**
	 * This `associatedData` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
	 * this requirement is used result contains [entity bodies](entity_model.md) along with associated data with names specified in
	 * one or more arguments of this requirement.
	 *
	 * This requirement implicitly triggers {@link EntityBody} requirement because attributes cannot be returned without entity.
	 * [Localized interface](classes/localized_interface.md) associated data is returned according to {@link Language}
	 * constraint. requirement might be combined with {@link Attributes} requirement.
	 *
	 * Example:
	 *
	 * ```
	 * associatedData('description', 'gallery-3d')
	 * ```
	 */
	@Nonnull
	static AssociatedData associatedData(@Nonnull String... associatedDataName) {
		return new AssociatedData(associatedDataName);
	}

	/**
	 * This `dataInLanguage` constraint is require constraint that accepts zero or more {@link Locale} arguments. When this
	 * require constraint is used, result contains [entity attributes and associated data](../model/entity_model.md)
	 * localized in required languages as well as global ones. If constraint contains no argument, data localized to all
	 * languages are returned. If constraint is not present in the query, only global attributes and associated data are
	 * returned.
	 *
	 * **Note:** if {@link io.evitadb.api.query.filter.Language}is used in the filter part of the query and `dataInLanguage`
	 * require constraint is missing, the system implicitly uses `dataInLanguage` matching the language in filter constraint.
	 *
	 * Only single `language` constraint can be used in the query.
	 *
	 * Example that fetches only global and `en-US` localized attributes and associated data (considering there are multiple
	 * language localizations):
	 *
	 * ```
	 * dataInLanguage('en-US')
	 * ```
	 *
	 * Example that fetches all available global and localized data:
	 *
	 * ```
	 * dataInLanguage()
	 * ```
	 */
	@Nullable
	static DataInLanguage dataInLanguage(@Nullable Locale... locale) {
		if (locale == null) {
			return null;
		}
		final Locale[] args = Arrays.stream(locale).filter(Objects::nonNull).toArray(Locale[]::new);
		return new DataInLanguage(args);
	}

	/**
	 * This `references` requirement changes default behaviour of the query engine returning only entity primary keys in the result.
	 * When this requirement is used result contains [entity bodies](entity_model.md) along with references with to entites
	 * or external objects specified in one or more arguments of this requirement.
	 *
	 * This requirement implicitly triggers {@link EntityBody} requirement because references cannot be returned without entity.
	 *
	 * Example:
	 *
	 * ```
	 * references(CATEGORY, 'stocks')
	 * ```
	 */
	@Nullable
	static References references(@Nullable Serializable... referencedEntityType) {
		if (referencedEntityType == null) {
			return null;
		}
		final Serializable[] args = Arrays.stream(referencedEntityType).filter(Objects::nonNull).toArray(Serializable[]::new);
		return new References(args);
	}

	/**
	 * This `parents` require constraint can be used only
	 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and target the entity type that is
	 * requested in the query. Constraint may have also inner require constraints that define how rich returned information
	 * should be (by default only primary keys are returned, but full entities might be returned as well).
	 *
	 * When this require constraint is used an additional object is stored to result index. This data structure contains
	 * information about referenced entity paths for each entity in the response.
	 *
	 * Example for returning parents of the same type as was queried (e.g. parent categories of filtered category):
	 *
	 * ```
	 * parents()
	 * ```
	 *
	 * Additional data structure by default returns only primary keys of those entities, but it can also provide full parent
	 * entities when this form of constraint is used:
	 *
	 * ```
	 * parents()
	 * parents(entityBody())
	 * ```
	 */
	@Nullable
	static Parents parents(@Nullable EntityContentRequire... requirements) {
		if (requirements == null) {
			return null;
		}
		final EntityContentRequire[] args = Arrays.stream(requirements).filter(Objects::nonNull).toArray(EntityContentRequire[]::new);
		return new Parents(args);
	}

	/**
	 * This `parentsOfType` require constraint can be used only
	 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and can have zero, one or more
	 * [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html) arguments that specifies type of
	 * hierarchical entity that this entity relates to. If argument is omitted, entity type of queried entity is used.
	 * Constraint may have also inner require constraints that define how rich returned information should be (by default only
	 * primary keys are returned, but full entities might be returned as well).
	 *
	 * When this require constraint is used an additional object is stored to result index. This data structure contains
	 * information about referenced entity paths for each entity in the response.
	 *
	 * Example for returning parents of the category when entity type `product` is queried:
	 *
	 * ```
	 * parentsOfType('category')
	 * parentsOfType('category','brand')
	 * ```
	 *
	 * Additional data structure by default returns only primary keys of those entities, but it can also provide full parent
	 * entities when this form of constraint is used:
	 *
	 * ```
	 * parentsOfType('category', entityBody())
	 * parentsOfType('category', 'brand', entityBody())
	 * ```
	 */
	@Nullable
	static ParentsOfType parentsOfType(@Nullable Serializable entityType) {
		return entityType == null ? null : new ParentsOfType(entityType);
	}

	/**
	 * This `parents` require constraint can be used only
	 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and can zero, one or more
	 * {@link Serializable} arguments that specifies type of hierarchical entity that this entity relates to.
	 * If argument is omitted, entity type of queried entity is used.
	 *
	 * When this require constraint is used an additional object is stored to result index. This DTO contains information about
	 * referenced entity paths for each entity in the response.
	 *
	 * Example:
	 *
	 * ```
	 * parents()
	 * parents('category')
	 * parents('category', 'brand')
	 * ```
	 */
	@Nullable
	static ParentsOfType parentsOfType(@Nullable Serializable... entityTypes) {
		if (entityTypes == null) {
			return null;
		}
		final Serializable[] args = Arrays.stream(entityTypes).filter(Objects::nonNull).toArray(Serializable[]::new);
		return args.length == 0 ? null : new ParentsOfType(args);
	}

	/**
	 * This `parents` require constraint can be used only
	 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and can zero, one or more
	 * {@link Serializable} arguments that specifies type of hierarchical entity that this entity relates to.
	 * If argument is omitted, entity type of queried entity is used.
	 *
	 * When this require constraint is used an additional object is stored to result index. This DTO contains information about
	 * referenced entity paths for each entity in the response.
	 *
	 * Example:
	 *
	 * ```
	 * parents()
	 * parents('category')
	 * parents('category', 'brand', entityBody())
	 * ```
	 */
	@Nonnull
	static ParentsOfType parentsOfType(@Nonnull Serializable entityType, @Nullable EntityContentRequire... requirements) {
		return new ParentsOfType(new Serializable[]{entityType}, requirements);
	}

	/**
	 * This `parents` require constraint can be used only
	 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and can zero, one or more
	 * {@link Serializable} arguments that specifies type of hierarchical entity that this entity relates to.
	 * If argument is omitted, entity type of queried entity is used.
	 *
	 * When this require constraint is used an additional object is stored to result index. This DTO contains information about
	 * referenced entity paths for each entity in the response.
	 *
	 * Example:
	 *
	 * ```
	 * parents()
	 * parents('category')
	 * parents('category', 'brand', entityBody())
	 * ```
	 */
	@Nullable
	static ParentsOfType parentsOfType(@Nullable Serializable[] entityType, @Nullable EntityContentRequire... requirements) {
		return entityType == null || ArrayUtils.isEmpty(entityType) ? null : new ParentsOfType(entityType, requirements);
	}

	/**
	 * This `prices` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
	 * this requirement is used result contains [entity prices](entity_model.md).
	 *
	 * This requirement implicitly triggers {@link EntityBody} requirement because prices cannot be returned without entity.
	 * When price constraints are used returned prices are filtered according to them by default. This behaviour might be
	 * changed however.
	 *
	 * Accepts single {@link PriceFetchMode} parameter. When {@link PriceFetchMode#ALL} all prices of the entity are returned
	 * regardless of the input query constraints otherwise prices are filtered by those constraints. Default is {@link PriceFetchMode#RESPECTING_FILTER}.
	 *
	 * Example:
	 *
	 * ```
	 * prices() // defaults to respecting filter
	 * prices(RESPECTING_FILTER)
	 * prices(ALL)
	 * prices(NONE)
	 * ```
	 */
	@Nonnull
	static Prices prices() {
		return new Prices(PriceFetchMode.RESPECTING_FILTER);
	}

	/**
	 * This `prices` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
	 * this requirement is used result contains [entity prices](entity_model.md).
	 *
	 * This requirement implicitly triggers {@link EntityBody} requirement because prices cannot be returned without entity.
	 * When price constraints are used returned prices are filtered according to them by default. This behaviour might be
	 * changed however.
	 *
	 * Accepts single {@link PriceFetchMode} parameter. When {@link PriceFetchMode#ALL} all prices of the entity are returned
	 * regardless of the input query constraints otherwise prices are filtered by those constraints. Default is {@link PriceFetchMode#RESPECTING_FILTER}.
	 *
	 * Example:
	 *
	 * ```
	 * prices(ALL)
	 * ```
	 */
	@Nonnull
	static Prices allPrices() {
		return new Prices(PriceFetchMode.ALL);
	}

	/**
	 * This `useOfPrice` require constraint can be used to control the form of prices that will be used for computation in
	 * {@link io.evitadb.api.query.filter.PriceBetween} filtering, and {@link io.evitadb.api.query.order.PriceAscending},
	 * {@link io.evitadb.api.query.order.PriceDescending} ordering. Also {@link PriceHistogram} is sensitive to this setting.
	 * <p>
	 * By default, end customer form of price (e.g. price with VAT) is used in all above-mentioned constraints. This could
	 * be changed by using this requirement constraint. It has single argument that can have one of the following values:
	 * <p>
	 * - WITH_VAT
	 * - WITHOUT_VAT
	 * <p>
	 * Example:
	 * <p>
	 * ```
	 * useOfPrice(WITH_VAT)
	 * ```
	 */
	@Nonnull
	static UseOfPrice useOfPrice(@Nonnull QueryPriceMode priceMode) {
		return new UseOfPrice(priceMode);
	}

	/**
	 * This `page` constraint controls count of the entities in the query output. It allows specifying 2 arguments in following order:
	 *
	 * - **[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) pageNumber**: number of the page of
	 * results that are expected to be returned, starts with 1, must be greater than zero (mandatory)
	 * - **[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) pageSize**: number of entities on
	 * a single page, must be greater than zero (mandatory)
	 *
	 * Example - return first page with 24 items:
	 *
	 * ```
	 * page(1, 24)
	 * ```
	 */
	@Nonnull
	static Page page(int pageNumber, int pageSize) {
		return new Page(pageNumber, pageSize);
	}

	/**
	 * This `strip` constraint controls count of the entities in the query output. It allows specifying 2 arguments in following order:
	 *
	 * - **[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) offset**: number of the items that
	 * should be omitted in the result, must be greater than or equals to zero (mandatory)
	 * - **[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) limit**: number of entities on
	 * that should be returned, must be greater than zero (mandatory)
	 *
	 * Example - return 24 records from index 52:
	 *
	 * ```
	 * strip(52, 24)
	 * ```
	 */
	@Nonnull
	static Strip strip(int offset, int limit) {
		return new Strip(offset, limit);
	}

	/**
	 * This `facetSummary` requirement usage triggers computing and adding an object to the result index. It cooperates
	 * with {@link WithinHierarchy} filtering constraint that must be used in the query as well. The object is quite
	 * complex but allows rendering entire facet listing to e-commerce users. It contains information about all facets
	 * present in current hierarchy view along with count of requested entities that have those facets assigned.
	 *
	 * Facet summary respects current query filtering constraints excluding the conditions inside {@link UserFilter}
	 * container constraint.
	 *
	 * When this requirement is used an additional object {@link io.evitadb.api.io.extraResult.FacetSummary} is stored to result.
	 *
	 * Optinally accepts single enuma argument:
	 *
	 * - COUNT: only counts of facets will be computed
	 * - IMPACT: counts and selection impact for non-selected facets will be computed
	 *
	 * Example:
	 *
	 * ```
	 * facetSummary()
	 * facetSummary(COUNT) //same as previous row - default
	 * facetSummary(IMPACT)
	 * ```
	 */
	@Nonnull
	static FacetSummary facetSummary() {
		return new FacetSummary();
	}

	/**
	 * This `facetSummary` requirement usage triggers computing and adding an object to the result index. The object is
	 * quite complex but allows rendering entire facet listing to e-commerce users. It contains information about all
	 * facets present in current hierarchy view along with count of requested entities that have those facets assigned.
	 *
	 * Facet summary respects current query filtering constraints excluding the conditions inside {@link UserFilter}
	 * container constraint.
	 *
	 * When this requirement is used an additional object {@link io.evitadb.api.io.extraResult.FacetSummary} is stored to result.
	 *
	 * Optionally accepts single enum argument:
	 *
	 * - COUNT: only counts of facets will be computed
	 * - IMPACT: counts and selection impact for non-selected facets will be computed
	 *
	 * Example:
	 *
	 * ```
	 * facetSummary()
	 * facetSummary(COUNT) //same as previous row - default
	 * facetSummary(IMPACT)
	 * ```
	 */
	@Nonnull
	static FacetSummary facetSummary(@Nullable FacetStatisticsDepth statisticsDepth) {
		return statisticsDepth == null ? new FacetSummary(FacetStatisticsDepth.COUNTS) : new FacetSummary(statisticsDepth);
	}

	/**
	 * This method returns array of all requirements that are necessary to load full content of the entity including
	 * all language specific attributes, all prices, all references and all associated data.
	 */
	@Nonnull
	static EntityContentRequire[] fullEntity() {
		return new EntityContentRequire[]{
			entityBody(), attributes(), associatedData(), allPrices(), references(), dataInLanguage()
		};
	}

	/**
	 * This method returns array of all requirements that are necessary to load full content of the entity including
	 * all language specific attributes, all prices, all references and all associated data.
	 */
	@Nonnull
	static RequireConstraint[] fullEntityAnd(@Nullable RequireConstraint... combineWith) {
		if (ArrayUtils.isEmpty(combineWith)) {
			return new RequireConstraint[]{
				entityBody(), attributes(), associatedData(), allPrices(), references(), dataInLanguage()
			};
		} else {
			final RequireConstraint[] combinedResult = new RequireConstraint[6 + combineWith.length];
			combinedResult[0] = entityBody();
			combinedResult[1] = attributes();
			combinedResult[2] = associatedData();
			combinedResult[3] = allPrices();
			combinedResult[4] = references();
			combinedResult[5] = dataInLanguage();
			System.arraycopy(combineWith, 0, combinedResult, 6, combineWith.length);
			return combinedResult;
		}
	}

}
