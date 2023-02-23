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

package io.evitadb.generators;

import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.key.CompressiblePriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.HierarchySpecificationFilterConstraint;
import io.evitadb.api.query.require.FacetStatisticsDepth;
import io.evitadb.api.query.visitor.FinderVisitor;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Optional.ofNullable;

/**
 * This interface contains methods that allow generation of random filtering constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface RandomQueryGenerator {
	HierarchySpecificationFilterConstraint[] EMPTY_HSFC_ARRAY = new HierarchySpecificationFilterConstraint[0];

	/**
	 * Returns random locale from the set of available locales.
	 */
	private static Locale getRandomExistingLocale(EntitySchema schema, Random random) {
		return schema
			.getLocales()
			.stream()
			.skip(random.nextInt(schema.getLocales().size()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No locales found!"));
	}

	/**
	 * Returns random element from the set.
	 */
	@Nonnull
	private static <T> T pickRandom(Random random, Set<T> theSet) {
		Assert.isTrue(theSet.size() >= 1, "There are no values to choose from!");
		final int index = theSet.size() == 1 ? 0 : random.nextInt(theSet.size() - 1) + 1;
		final Iterator<T> it = theSet.iterator();
		for (int i = 0; i < index; i++) {
			it.next();
		}
		return it.next();
	}

	@Nonnull
	private static AttributeStatistics pickRandom(Random random, Map<String, AttributeStatistics> filterableAttributes) {
		final int index = random.nextInt(filterableAttributes.size() - 1) + 1;
		final Iterator<AttributeStatistics> it = filterableAttributes.values().iterator();
		for (int i = 0; i < index; i++) {
			it.next();
		}
		return it.next();
	}

	private static ZonedDateTime getRandomZonedDateTimeBetween(Random random, Statistics statistics) {
		final DateTimeRange min = (DateTimeRange) statistics.getMinimalValue();
		final DateTimeRange max = (DateTimeRange) statistics.getMaximalValue();
		final long from = min.getPreciseFrom() == null ? min.getTo() : min.getFrom();
		final long to = max.getPreciseTo() == null ? max.getFrom() : max.getTo();
		final long inBetween = Math.round(from + random.nextDouble() * (to - from));
		final Instant instant = Instant.ofEpochMilli(inBetween);
		return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	/**
	 * Method gathers statistics about faceted reference ids in processed dataset. This information is necessary to
	 * generate random query using {@link #generateRandomFacetQuery(Random, EntitySchema, Map)} method.
	 */
	default void updateFacetStatistics(SealedEntity entity, Map<Serializable, Set<Integer>> facetedReferences, Map<Serializable, Map<Integer, Integer>> facetGroupsIndex) {
		entity.getReferences()
			.stream()
			.filter(it -> facetedReferences.containsKey(it.getReferencedEntity().getType()))
			.filter(it -> it.getReferenceSchema().isFaceted())
			.forEach(it -> {
				facetedReferences.get(it.getReferencedEntity().getType()).add(it.getReferencedEntity().getPrimaryKey());
				if (it.getGroup() != null) {
					facetGroupsIndex.get(it.getReferencedEntity().getType())
						.put(it.getReferencedEntity().getPrimaryKey(), it.getGroup().getPrimaryKey());
				}
			});
	}

	/**
	 * Method gathers statistics about attribute values in processed dataset. This information is necessary to generate
	 * random query using {@link #generateRandomAttributeQuery(Random, EntitySchema, Map, Set)} method.
	 */
	default void updateAttributeStatistics(SealedEntity entity, Random random, Map<String, AttributeStatistics> filterableAttributes) {
		for (Entry<String, AttributeStatistics> entry : filterableAttributes.entrySet()) {
			final String attributeName = entry.getKey();
			final AttributeStatistics statistics = entry.getValue();
			if (statistics.isLocalized()) {
				for (Locale locale : entity.getAttributeLocales()) {
					final AttributeValue localizedAttributeValue = entity.getAttributeValue(attributeName, locale);
					if (localizedAttributeValue != null) {
						if (localizedAttributeValue.getValue() instanceof Object[]) {
							final Serializable[] valueArray = (Serializable[]) localizedAttributeValue.getValue();
							for (Serializable valueItem : valueArray) {
								statistics.updateValue(valueItem, locale, random);
							}
						} else {
							statistics.updateValue(localizedAttributeValue.getValue(), locale, random);
						}
					}
				}
			} else {
				final AttributeValue attributeValue = entity.getAttributeValue(attributeName);
				if (attributeValue != null) {
					if (attributeValue.getValue() instanceof Object[]) {
						final Serializable[] valueArray = (Serializable[]) attributeValue.getValue();
						for (Serializable valueItem : valueArray) {
							statistics.updateValue(valueItem, random);
						}
					} else {
						statistics.updateValue(attributeValue.getValue(), random);
					}
				}
			}
		}
	}

	/**
	 * Method gathers statistics about price values in processed dataset. This information is necessary to generate
	 * random query using {@link #generateRandomPriceQuery(Random, EntitySchema, GlobalPriceStatistics)} method.
	 */
	default void updatePriceStatistics(SealedEntity entity, Random random, GlobalPriceStatistics priceStats) {
		for (PriceContract price : entity.getPrices()) {
			priceStats.updateValue(price, random);
		}
	}

	/**
	 * Creates randomized query for passed entity schema based on passed statistics about `filteringAttributes` and
	 * `sortableAttributes`.
	 */
	default Query generateRandomAttributeQuery(Random random, EntitySchema schema, Map<String, AttributeStatistics> filterableAttributes, Set<String> sortableAttributes) {
		final Locale randomExistingLocale = getRandomExistingLocale(schema, random);

		return Query.query(
			entities(schema.getName()),
			filterBy(
				and(
					createRandomAttributeFilterBy(random, randomExistingLocale, filterableAttributes),
					language(randomExistingLocale)
				)
			),
			orderBy(
				createRandomAttributeOrderBy(random, sortableAttributes)
			),
			require(
				page(random.nextInt(5) + 1, 20)
			)
		);
	}

	/**
	 * Method generates random attribute histogram requirement and adds it to the passed `existingQuery`.
	 */
	default Query generateRandomAttributeHistogramQuery(Query existingQuery, Random random, Set<String> numericFilterableAttributes) {
		Assert.isTrue(numericFilterableAttributes.size() >= 1, "There are no numeric attributes!");
		final int histogramCount = numericFilterableAttributes.size() == 1 ? 1 : 1 + random.nextInt(numericFilterableAttributes.size() - 1);
		final String[] attributes = new String[histogramCount];
		final Set<String> alreadySelected = new HashSet<>(histogramCount);
		for (int i = 0; i < histogramCount; i++) {
			String attribute = pickRandom(random, numericFilterableAttributes);
			while (alreadySelected.contains(attribute)) {
				attribute = pickRandom(random, numericFilterableAttributes);
			}
			attributes[i] = attribute;
			alreadySelected.add(attribute);
		}

		return Query.query(
			existingQuery.getEntities(),
			existingQuery.getFilterBy(),
			require(
				ArrayUtils.mergeArrays(
					existingQuery.getRequire().getConstraints(),
					new RequireConstraint[]{
						attributeHistogram(10 + random.nextInt(20), attributes)
					}
				)
			),
			existingQuery.getOrderBy()
		);
	}

	/**
	 * Creates randomized query for passed entity schema based on passed statistics about `filteringAttributes` and
	 * `sortableAttributes`.
	 */
	default Query generateRandomPriceQuery(Random random, EntitySchema schema, GlobalPriceStatistics priceStats) {
		final Locale randomExistingLocale = getRandomExistingLocale(schema, random);

		return Query.query(
			entities(schema.getName()),
			filterBy(
				and(
					createRandomPriceFilterBy(random, priceStats, schema.getIndexedPricePlaces()),
					language(randomExistingLocale)
				)
			),
			orderBy(
				createRandomPriceOrderBy(random)
			),
			require(
				page(random.nextInt(5) + 1, 20)
			)
		);
	}

	/**
	 * Method generates random hierarchy filtering constraint and adds it to the passed `existingQuery`. While new
	 * constraint is created `categoryIds` collection is used to retrieve random category id specification.
	 */
	default Query generateRandomHierarchyQuery(Query existingQuery, Random random, List<Integer> categoryIds, Serializable hierarchyEntityType) {
		final FilterConstraint hierarchyConstraint;
		final List<HierarchySpecificationFilterConstraint> specification = new ArrayList<>();
		final int rndKey = Math.abs(random.nextInt()) + 1;
		final Integer[] excludedIds;
		if (rndKey % 5 == 0) {
			excludedIds = new Integer[5];
			for (int i = 0; i < 5; i++) {
				excludedIds[i] = categoryIds.get(Math.abs(rndKey * (i + 1)) % (categoryIds.size()));
			}
			specification.add(excluding(excludedIds));
		} else {
			excludedIds = null;
		}
		if (rndKey % 3 == 0) {
			specification.add(excludingRoot());
		} else if (rndKey % 7 == 0 && excludedIds == null) {
			specification.add(directRelation());
		}
		final int parentId = categoryIds.get(rndKey % categoryIds.size());
		hierarchyConstraint = withinHierarchy(hierarchyEntityType, parentId, specification.toArray(EMPTY_HSFC_ARRAY));

		return Query.query(
			existingQuery.getEntities(),
			filterBy(
				and(
					ArrayUtils.mergeArrays(
						new FilterConstraint[]{hierarchyConstraint},
						existingQuery.getFilterBy().getConstraints()
					)
				)
			),
			existingQuery.getRequire(),
			existingQuery.getOrderBy()
		);
	}

	/**
	 * Method generates random price histogram requirement and adds it to the passed `existingQuery`.
	 */
	default Query generateRandomPriceHistogramQuery(Query existingQuery, Random random) {
		return Query.query(
			existingQuery.getEntities(),
			existingQuery.getFilterBy(),
			require(
				ArrayUtils.mergeArrays(
					existingQuery.getRequire().getConstraints(),
					new RequireConstraint[]{
						priceHistogram(10 + random.nextInt(20))
					}
				)
			),
			existingQuery.getOrderBy()
		);
	}

	/**
	 * Creates randomized query for passed entity schema based on passed statistics about faceted `references`.
	 */
	default Query generateRandomFacetQuery(Random random, EntitySchema schema, Map<Serializable, Set<Integer>> facetedReferences) {
		final int facetCount = random.nextInt(5) + 1;
		final Map<Serializable, Set<Integer>> selectedFacets = new HashMap<>();
		for (int i = 0; i < facetCount; i++) {
			final Serializable randomFacetType = getRandomItem(random, facetedReferences.keySet());
			final Integer randomFacetId = getRandomItem(random, facetedReferences.get(randomFacetType));
			selectedFacets.computeIfAbsent(randomFacetType, fType -> new HashSet<>()).add(randomFacetId);
		}
		return Query.query(
			entities(schema.getName()),
			filterBy(
				userFilter(
					selectedFacets.entrySet()
						.stream()
						.map(it -> facet(it.getKey(), it.getValue().toArray(new Integer[0])))
						.toArray(FilterConstraint[]::new)
				)
			),
			require(
				page(random.nextInt(5) + 1, 20)
			)
		);
	}

	/**
	 * Creates randomized query requiring {@link io.evitadb.api.io.extraResult.Parents} computation for passed entity
	 * schema based on passed set.
	 */
	default Query generateRandomParentSummaryQuery(Random random, EntitySchema schema, Set<Serializable> referencedHierarchyEntities, int maxProductId) {
		final Integer[] requestedPks = new Integer[20];
		int firstPk = random.nextInt(maxProductId / 2);
		for (int i = 0; i < requestedPks.length; i++) {
			requestedPks[i] = firstPk;
			firstPk += random.nextInt(maxProductId / 40);
		}
		return Query.query(
			entities(schema.getName()),
			filterBy(
				primaryKey(requestedPks)
			),
			require(
				page(1, 20),
				parentsOfType(
					getRandomItems(random, referencedHierarchyEntities).toArray(Serializable[]::new)
				)
			)
		);
	}

	/**
	 * Creates randomized query requiring {@link io.evitadb.api.io.extraResult.HierarchyStatistics} computation for
	 * passed entity schema based on passed set.
	 */
	default Query generateRandomParentSummaryQuery(Random random, EntitySchema schema, Set<Serializable> referencedHierarchyEntities) {
		return Query.query(
			entities(schema.getName()),
			filterBy(
				language(getRandomExistingLocale(schema, random))
			),
			require(
				page(1, 20),
				hierarchyStatistics(pickRandom(random, referencedHierarchyEntities))
			)
		);
	}

	/**
	 * Updates randomized query adding a request to facet summary computation juggling inter facet relations.
	 */
	default Query generateRandomFacetSummaryQuery(Query existingQuery, Random random, EntitySchema schema, FacetStatisticsDepth depth, Map<Serializable, Map<Integer, Integer>> facetGroupsIndex) {
		final List<FilterConstraint> facetFilters = FinderVisitor.findConstraints(existingQuery.getFilterBy(), Facet.class::isInstance);
		final List<RequireConstraint> requireConstraints = new LinkedList<>();
		for (FilterConstraint facetFilter : facetFilters) {
			final Facet facetConstraint = (Facet) facetFilter;
			final int dice = random.nextInt(4);
			final Map<Integer, Integer> entityTypeGroups = facetGroupsIndex.get(facetConstraint.getEntityType());
			final Set<Integer> groupIds = Arrays.stream(facetConstraint.getFacetIds())
				.mapToObj(entityTypeGroups::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
			if (!groupIds.isEmpty()) {
				if (dice == 1) {
					requireConstraints.add(
						facetGroupsConjunction(facetConstraint.getEntityType(), getRandomItem(random, groupIds))
					);
				} else if (dice == 2) {
					requireConstraints.add(
						facetGroupsDisjunction(facetConstraint.getEntityType(), getRandomItem(random, groupIds))
					);
				} else if (dice == 3) {
					requireConstraints.add(
						facetGroupsNegation(facetConstraint.getEntityType(), getRandomItem(random, groupIds))
					);
				}
			}
		}
		return Query.query(
			entities(schema.getName()),
			existingQuery.getFilterBy(),
			existingQuery.getOrderBy(),
			require(
				ArrayUtils.mergeArrays(
					new RequireConstraint[]{
						page(random.nextInt(5) + 1, 20),
						facetSummary(depth)
					},
					requireConstraints.toArray(RequireConstraint[]::new)
				)

			)
		);
	}

	/**
	 * Returns random item from passed collection.
	 */
	default <T> T getRandomItem(Random random, Collection<T> collection) {
		final int position = random.nextInt(collection.size());
		final Iterator<T> it = collection.iterator();
		int i = 0;
		while (it.hasNext()) {
			final T next = it.next();
			if (i++ == position) {
				return next;
			}
		}
		throw new IllegalStateException("Should not ever happen!");
	}

	/**
	 * Returns random items from passed collection.
	 */
	default <T> Set<T> getRandomItems(Random random, Collection<T> collection) {
		final Set<T> result = new LinkedHashSet<>();
		for (T next : collection) {
			if (random.nextBoolean()) {
				result.add(next);
			}
		}
		if (result.isEmpty()) {
			result.add(collection.iterator().next());
		}
		return result;
	}

	/**
	 * Creates randomized filter constraint that targets existing attribute. It picks random attribute from the
	 * `filterableAttributes` and creates one of the following constraints based on the attribute type:
	 *
	 * - isNull
	 * - isNotNull
	 * - eq
	 * - inSet
	 * - isTrue
	 * - isFalse
	 * - greaterThan
	 * - lesserThan
	 * - inRange
	 * - between
	 */
	@Nonnull
	default FilterConstraint createRandomAttributeFilterBy(Random random, Locale locale, Map<String, AttributeStatistics> filterableAttributes) {
		final AttributeStatistics attribute = pickRandom(random, filterableAttributes);
		if (String.class.equals(attribute.getType())) {
			final Statistics statistics = attribute.getStatistics(locale);
			switch (random.nextInt(3)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return statistics == null ? isNotNull(attribute.getName()) : eq(attribute.getName(), statistics.getSomeValue(random));
				case 3:
					return statistics == null ? isNotNull(attribute.getName()) : inSet(attribute.getName(), statistics.getSomeValue(random), statistics.getSomeValue(random));
				default:
					return isNull(attribute.getName());
			}
		} else if (Boolean.class.equals(attribute.getType())) {
			switch (random.nextInt(3)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return isTrue(attribute.getName());
				case 3:
					return isFalse(attribute.getName());
				default:
					return isNull(attribute.getName());
			}
		} else if (Long.class.equals(attribute.getType()) && attribute.isArray()) {
			final Statistics statistics = attribute.getStatistics(locale);
			switch (random.nextInt(5)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return statistics == null ? isNotNull(attribute.getName()) : eq(attribute.getName(), statistics.getSomeValue(random));
				case 3:
					return statistics == null ? isNotNull(attribute.getName()) : inSet(attribute.getName(), statistics.getSomeValue(random), statistics.getSomeValue(random));
				case 4: {
					if (statistics == null) {
						return isNotNull(attribute.getName());
					}
					final Long first = statistics.getSomeValue(random);
					final Long second = statistics.getSomeValue(random);
					return between(attribute.getName(), first < second ? first : second, first < second ? second : first);
				}
				default:
					return isNull(attribute.getName());
			}
		} else if (Long.class.equals(attribute.getType())) {
			final Statistics statistics = attribute.getStatistics(locale);
			switch (random.nextInt(7)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return statistics == null ? isNotNull(attribute.getName()) : eq(attribute.getName(), statistics.getSomeValue(random));
				case 3:
					return statistics == null ? isNotNull(attribute.getName()) : inSet(attribute.getName(), statistics.getSomeValue(random), statistics.getSomeValue(random));
				case 4:
					return statistics == null ? isNotNull(attribute.getName()) : greaterThan(attribute.getName(), statistics.getSomeValue(random));
				case 5:
					return statistics == null ? isNotNull(attribute.getName()) : lessThan(attribute.getName(), statistics.getSomeValue(random));
				case 6: {
					if (statistics == null) {
						return isNotNull(attribute.getName());
					}
					final Long first = statistics.getSomeValue(random);
					final Long second = statistics.getSomeValue(random);
					return between(attribute.getName(), first < second ? first : second, first < second ? second : first);
				}
				default:
					return isNull(attribute.getName());
			}
		} else if (BigDecimal.class.equals(attribute.getType()) && attribute.isArray()) {
			final Statistics statistics = attribute.getStatistics(locale);
			switch (random.nextInt(5)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return statistics == null ? isNotNull(attribute.getName()) : eq(attribute.getName(), statistics.getSomeValue(random));
				case 3:
					return statistics == null ? isNotNull(attribute.getName()) : inSet(attribute.getName(), statistics.getSomeValue(random), statistics.getSomeValue(random));
				case 4: {
					if (statistics == null) {
						return isNotNull(attribute.getName());
					}
					final BigDecimal first = statistics.getSomeValue(random);
					final BigDecimal second = statistics.getSomeValue(random);
					return between(attribute.getName(), first.compareTo(second) < 0 ? first : second, first.compareTo(second) < 0 ? second : first);
				}
				default:
					return isNull(attribute.getName());
			}
		} else if (BigDecimal.class.equals(attribute.getType())) {
			final Statistics statistics = attribute.getStatistics(locale);
			switch (random.nextInt(7)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return statistics == null ? isNotNull(attribute.getName()) : eq(attribute.getName(), statistics.getSomeValue(random));
				case 3:
					return statistics == null ? isNotNull(attribute.getName()) : inSet(attribute.getName(), statistics.getSomeValue(random), statistics.getSomeValue(random));
				case 4:
					return statistics == null ? isNotNull(attribute.getName()) : greaterThan(attribute.getName(), statistics.getSomeValue(random));
				case 5:
					return statistics == null ? isNotNull(attribute.getName()) : lessThan(attribute.getName(), statistics.getSomeValue(random));
				case 6: {
					if (statistics == null) {
						return isNotNull(attribute.getName());
					}
					final BigDecimal first = statistics.getSomeValue(random);
					final BigDecimal second = statistics.getSomeValue(random);
					return between(attribute.getName(), first.compareTo(second) < 0 ? first : second, first.compareTo(second) < 0 ? second : first);
				}
				default:
					return isNull(attribute.getName());
			}
		} else if (DateTimeRange.class.equals(attribute.getType())) {
			final Statistics statistics = attribute.getStatistics(locale);
			switch (random.nextInt(3)) {
				case 0:
					return isNull(attribute.getName());
				case 1:
					return isNotNull(attribute.getName());
				case 2:
					return statistics == null ? isNotNull(attribute.getName()) : inRange(attribute.getName(), getRandomZonedDateTimeBetween(random, statistics));
				case 3: {
					if (statistics == null) {
						return isNotNull(attribute.getName());
					}
					final ZonedDateTime first = getRandomZonedDateTimeBetween(random, statistics);
					final ZonedDateTime second = getRandomZonedDateTimeBetween(random, statistics);
					return between(attribute.getName(), first.isBefore(second) ? first : second, first.isBefore(second) ? second : first);
				}
				default:
					return isNull(attribute.getName());
			}
		} else {
			return isNotNull(attribute.getName());
		}
	}

	/**
	 * Creates randomized filter by constraint for prices. Filter by always contain filter for:
	 *
	 * - random currency
	 * - the biggest price list + random number of additional price lists in that currency
	 * - random validity constraint
	 *
	 * In 40% of cases also `priceBetween` constraint that further limits the output results.
	 */
	@Nonnull
	default FilterConstraint createRandomPriceFilterBy(Random random, GlobalPriceStatistics priceStatistics, int decimalPlaces) {
		final int queryType = random.nextInt(10);
		final Currency currency = priceStatistics.pickRandomCurrency(random);
		final Serializable biggestPriceList = priceStatistics.getBiggestPriceListFor(currency);
		final Serializable[] additionalPriceLists = priceStatistics.pickRandomPriceLists(random, random.nextInt(6), currency, biggestPriceList);
		final Serializable[] priceLists = ArrayUtils.mergeArrays(additionalPriceLists, new Serializable[]{biggestPriceList});
		final ZonedDateTime validIn = priceStatistics.pickRandomDateTimeFor(currency, priceLists, random);
		if (queryType < 4) {
			final BigDecimal from = priceStatistics.pickRandomValue(currency, priceLists, random, decimalPlaces);
			final BigDecimal to = priceStatistics.pickRandomValue(currency, priceLists, random, decimalPlaces);
			// query prices with price between
			final int fromLesserThanTo = from.compareTo(to);
			//noinspection ConstantConditions
			return and(
				priceInCurrency(currency),
				priceInPriceLists(priceLists),
				priceValidIn(validIn),
				priceBetween(fromLesserThanTo < 0 ? from : to, fromLesserThanTo < 0 ? to : from)
			);
		} else {
			// query prices with currency, price lists and validity
			//noinspection ConstantConditions
			return and(
				priceInCurrency(currency),
				priceInPriceLists(priceLists),
				priceValidIn(validIn)
			);
		}
	}

	/**
	 * Creates randomized order constraint that targets existing attribute. It picks random attribute from the
	 * `sortableAttributes` and creates ascending or descending order.
	 */
	@Nonnull
	default OrderConstraint createRandomAttributeOrderBy(Random random, Set<String> sortableAttributes) {
		final OrderConstraint randomOrderBy;
		if (random.nextBoolean()) {
			randomOrderBy = ascending(pickRandom(random, sortableAttributes));
		} else {
			randomOrderBy = descending(pickRandom(random, sortableAttributes));
		}
		return randomOrderBy;
	}

	/**
	 * Creates randomized order constraint that targets prices. In 33% it returns ordering by price asc, 33% desc and
	 * for 33% of cases no ordering.
	 */
	@Nullable
	default OrderConstraint createRandomPriceOrderBy(Random random) {
		final int selectedType = random.nextInt(3);
		switch (selectedType) {
			case 0:
				return priceAscending();
			case 1:
				return priceDescending();
			default:
				return null;
		}
	}

	/**
	 * This class contains statistical information about attribute data that is necessary to create valid queries.
	 * This DTO is locale / global attribute ambiguous - it aggregates all locale specific or global attributes.
	 */
	class AttributeStatistics {
		/**
		 * Holds name of the attribute.
		 */
		@Getter private final String name;
		/**
		 * Holds type of the attribute.
		 */
		@Getter private final Class<? extends Serializable> type;
		/**
		 * Holds true if type represents array.
		 */
		@Getter private final boolean array;
		/**
		 * Holds true if attribute is locale specific.
		 */
		@Getter private final boolean localized;
		/**
		 * Holds global statistics (only if attribute is not localized).
		 */
		private Statistics global;
		/**
		 * Holds local specific statistics (only if attribute is localized).
		 */
		private Map<Locale, Statistics> localeSpecific;

		public AttributeStatistics(AttributeSchema attributeSchema) {
			this.name = attributeSchema.getName();
			final Class<? extends Serializable> theType = attributeSchema.getType();
			//noinspection unchecked
			this.type = theType.isArray() ? (Class<? extends Serializable>) theType.getComponentType() : theType;
			this.array = theType.isArray();
			this.localized = attributeSchema.isLocalized();
		}

		/**
		 * Records a value encountered in the dataset.
		 */
		public void updateValue(Serializable value, Random random) {
			Assert.isTrue(!this.localized, "Attribute is localized by schema!");
			if (global == null) {
				global = new Statistics(value);
			} else {
				global.update(value, random);
			}
		}

		/**
		 * Records a value encountered in the dataset.
		 */
		public void updateValue(Serializable localizedValue, Locale locale, Random random) {
			Assert.isTrue(this.localized, "Attribute is not localized by schema!");
			if (localeSpecific == null) {
				localeSpecific = new HashMap<>();
			}
			final Statistics statistics = localeSpecific.get(locale);
			if (statistics == null) {
				localeSpecific.put(locale, new Statistics(localizedValue));
			} else {
				statistics.update(localizedValue, random);
			}
		}

		/**
		 * Returns statistics for the passed locale or global statistics if there are no locale specific statistics
		 * found.
		 */
		public Statistics getStatistics(Locale locale) {
			return ofNullable(localeSpecific).map(it -> it.get(locale)).orElse(global);
		}

	}

	/**
	 * This class contains statistical information about price data that is necessary to create valid queries.
	 */
	class GlobalPriceStatistics {
		private final Map<CompressiblePriceKey, PriceStatistics> priceAndCurrencyStats = new HashMap<>();
		@Getter private final Map<Currency, Set<Serializable>> priceLists = new HashMap<>();
		@Getter private final Set<Currency> currencies = new HashSet<>();

		/**
		 * Indexes new price.
		 */
		public void updateValue(PriceContract value, Random random) {
			if (value.isSellable()) {
				final CompressiblePriceKey key = new CompressiblePriceKey(value.getPriceKey());
				final PriceStatistics priceStatistics = priceAndCurrencyStats.computeIfAbsent(key, PriceStatistics::new);
				priceStatistics.updateValue(value, random);
				currencies.add(key.getCurrency());
				final Set<Serializable> priceLists = this.priceLists.computeIfAbsent(key.getCurrency(), currency -> new HashSet<>());
				priceLists.add(key.getPriceList());
			}
		}

		/**
		 * Returns price statistics for passed priceList and currency combination.
		 */
		public PriceStatistics getPriceStats(CompressiblePriceKey key) {
			return priceAndCurrencyStats.get(key);
		}

		/**
		 * Selects random currency from all available currencies.
		 */
		public Currency pickRandomCurrency(Random random) {
			final int index = random.nextInt(currencies.size());
			final Iterator<Currency> it = currencies.iterator();
			int i = -1;
			while (++i < index) {
				it.next();
			}
			return it.next();
		}

		/**
		 * Returns price list with passed `currency` that has the most prices in it. This price list is probably
		 * "the basic" price list that contains prices for all items.
		 */
		public Serializable getBiggestPriceListFor(Currency currency) {
			final Set<Serializable> priceLists = this.priceLists.get(currency);
			Serializable biggestOne = null;
			int biggestCount = 0;
			for (Serializable priceList : priceLists) {
				final PriceStatistics priceStatistics = priceAndCurrencyStats.get(new CompressiblePriceKey(priceList, currency));
				if (priceStatistics.getCount() > biggestCount) {
					biggestOne = priceList;
					biggestCount = priceStatistics.getCount();
				}
			}
			return biggestOne;
		}

		/**
		 * Selects set of random price lists except of specified one. Price lists are selected only for passed currency.
		 */
		public Serializable[] pickRandomPriceLists(Random random, int count, Currency currency, Serializable except) {
			final Set<Serializable> priceListsAvailable = priceLists.get(currency);
			final Set<Serializable> pickedPriceLists = new HashSet<>();
			final int requestedCount = Math.min(count, priceListsAvailable.size() - 1);
			do {
				final int index = random.nextInt(priceListsAvailable.size());
				final Iterator<Serializable> it = priceListsAvailable.iterator();
				int i = -1;
				while (++i < index) {
					it.next();
				}
				final Serializable pickedPriceList = it.next();
				if (!except.equals(pickedPriceList)) {
					pickedPriceLists.add(pickedPriceList);
				}
			} while (pickedPriceLists.size() < requestedCount);

			final Serializable[] priceLists = pickedPriceLists.toArray(new Serializable[0]);
			ArrayUtils.shuffleArray(random, priceLists);
			return priceLists;
		}

		/**
		 * Returns random date time that belongs to the validity intervals of passed currency and price lists combinations.
		 */
		public ZonedDateTime pickRandomDateTimeFor(Currency currency, Serializable[] priceLists, Random random) {
			ZonedDateTime randomValue = null;
			for (Serializable priceList : priceLists) {
				final CompressiblePriceKey key = new CompressiblePriceKey(priceList, currency);
				final PriceStatistics statistics = priceAndCurrencyStats.get(key);
				final Statistics validityStatistics = ofNullable(statistics).map(PriceStatistics::getValidityStatistics).orElse(null);
				if (validityStatistics != null) {
					final LinkedList<Comparable> randomValues = validityStatistics.getRandomValues();
					final int index = random.nextInt(randomValues.size());
					final Iterator<Comparable> it = randomValues.iterator();
					int i = -1;
					while (++i < index) {
						it.next();
					}
					final DateTimeRange range = (DateTimeRange) it.next();
					randomValue = random.nextBoolean() ? range.getPreciseFrom() : range.getPreciseTo();
				}
			}
			if (randomValue == null || randomValue.getYear() > 2090 || randomValue.getYear() < 1950) {
				return ZonedDateTime.now();
			} else {
				return randomValue;
			}
		}

		/**
		 * Returns random price with VAT belongs to the price spans of passed currency and price lists combinations.
		 */
		public BigDecimal pickRandomValue(Currency currency, Serializable[] priceLists, Random random, int decimalPlaces) {
			BigDecimal min = null;
			BigDecimal max = null;
			for (Serializable priceList : priceLists) {
				final CompressiblePriceKey key = new CompressiblePriceKey(priceList, currency);
				final PriceStatistics statistics = priceAndCurrencyStats.get(key);
				final Statistics priceWithVatStats = ofNullable(statistics).map(PriceStatistics::getPriceWithVatStatistics).orElse(null);
				if (priceWithVatStats != null) {
					final BigDecimal priceListMinPrice = (BigDecimal) priceWithVatStats.getMinimalValue();
					if (min == null || min.compareTo(priceListMinPrice) > 0) {
						min = priceListMinPrice;
					}
					final BigDecimal priceListMaxPrice = (BigDecimal) priceWithVatStats.getMaximalValue();
					if (max == null || max.compareTo(priceListMaxPrice) < 0) {
						max = priceListMaxPrice;
					}
				}
			}
			if (min == null && max == null) {
				return null;
			} else {
				final BigDecimal diff = max.subtract(min);
				return min.add(diff.multiply(BigDecimal.valueOf(random.nextFloat())))
					.setScale(decimalPlaces, RoundingMode.HALF_UP);
			}
		}
	}

	/**
	 * This class contains statistical information about price data that is necessary to create valid queries.
	 * This DTO is specific to the price list / currency combination.
	 */
	@RequiredArgsConstructor
	class PriceStatistics {
		/**
		 * Holds identification of the price list and currency combination.
		 */
		@Getter private final CompressiblePriceKey key;
		/**
		 * Holds statistics for the price.
		 */
		private Statistics priceWithoutVatStatistics;
		/**
		 * Holds statistics for the price.
		 */
		private Statistics priceWithVatStatistics;
		/**
		 * Holds statistics for the price.
		 */
		private Statistics validityStatistics;

		/**
		 * Records a value encountered in the dataset.
		 */
		public void updateValue(PriceContract value, Random random) {
			if (priceWithoutVatStatistics == null) {
				priceWithoutVatStatistics = new Statistics(value.getPriceWithoutVat());
				priceWithVatStatistics = new Statistics(value.getPriceWithVat());
			} else {
				priceWithoutVatStatistics.update(value.getPriceWithoutVat(), random);
				priceWithVatStatistics.update(value.getPriceWithVat(), random);
			}
			if (value.getValidity() != null) {
				if (validityStatistics == null) {
					validityStatistics = new Statistics(value.getValidity());
				} else {
					validityStatistics.update(value.getValidity(), random);
				}
			}
		}

		/**
		 * Returns statistics for the prices without VAT.
		 * found.
		 */
		@Nullable
		public Statistics getPriceWithoutVatStatistics() {
			return priceWithoutVatStatistics;
		}

		/**
		 * Returns statistics for the prices without VAT.
		 * found.
		 */
		@Nullable
		public Statistics getPriceWithVatStatistics() {
			return priceWithVatStatistics;
		}

		/**
		 * Returns statistics for datetime validity of the prices.
		 * found.
		 */
		@Nullable
		public Statistics getValidityStatistics() {
			return validityStatistics;
		}

		/**
		 * Returns count of prices in this price list and currency combination.
		 */
		public int getCount() {
			return priceWithoutVatStatistics.getCount();
		}
	}

	/**
	 * This class contains statistical information about attribute data that is necessary to create valid queries.
	 * This class is locale specific.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Data
	class Statistics {
		/**
		 * Holds information about minimal value encountered for this attribute.
		 */
		private Comparable minimalValue;
		/**
		 * Holds information about maximal value encountered for this attribute.
		 */
		private Comparable maximalValue;
		/**
		 * Holds information about count of all attributes of this name in the dataset.
		 */
		private int count;
		/**
		 * Contains random 50 values of this attribute from the actual dataset.
		 * Values can be used for equality or threshold filtering.
		 */
		private LinkedList<Comparable> randomValues = new LinkedList<>();

		public Statistics(Serializable value) {
			this.minimalValue = (Comparable) value;
			this.maximalValue = (Comparable) value;
			this.count = 1;
			this.randomValues.add((Comparable) value);
		}

		/**
		 * Records a value encountered in the dataset.
		 */
		public void update(Serializable value, Random random) {
			if (minimalValue.compareTo(value) > 0) {
				this.minimalValue = (Comparable) value;
			}
			if (maximalValue.compareTo(value) < 0) {
				this.maximalValue = (Comparable) value;
			}
			this.count++;
			if (random.nextInt(5) == 0) {
				randomValues.add((Comparable) value);
				if (randomValues.size() > 50) {
					randomValues.removeFirst();
				}
			}
		}

		/**
		 * Returns random value that is present in the dataset.
		 */
		public <T extends Comparable<?> & Serializable> T getSomeValue(Random random) {
			return (T) randomValues.get(random.nextInt(randomValues.size()));
		}
	}

}
