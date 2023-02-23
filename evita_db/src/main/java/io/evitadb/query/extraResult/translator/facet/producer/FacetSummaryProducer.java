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

package io.evitadb.query.extraResult.translator.facet.producer;

import com.carrotsearch.hppc.IntSet;
import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.io.extraResult.FacetSummary;
import io.evitadb.api.io.extraResult.FacetSummary.FacetGroupStatistics;
import io.evitadb.api.io.extraResult.FacetSummary.FacetStatistics;
import io.evitadb.api.io.extraResult.FacetSummary.RequestImpact;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.UserFilter;
import io.evitadb.api.query.require.FacetStatisticsDepth;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.facet.FacetEntityTypeIndex;
import io.evitadb.index.facet.FacetIdIndex;
import io.evitadb.index.facet.FacetIndex;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.AndFormula;
import io.evitadb.query.algebra.base.OrFormula;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultProducer;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * {@link FacetSummaryProducer} creates {@link FacetSummary} instance and does the heavy lifting to compute all
 * information necessary. The producer executes following logic:
 *
 * - from all gathered {@link FacetEntityTypeIndex} collect all facets organized into respective groups
 * - it merges all facets from all indexes by {@link OrFormula} so that it has full group-facet-entityId information
 * - each facet statistics is combined by {@link AndFormula} with {@link #filterFormula} that contains the output of the
 * filtered query excluding user-defined parts (e.g. without subtrees in {@link UserFilter} constraint)
 * - the result allows to list all facets that correspond to the result entities that returned as the query response
 *
 * When requested {@link RequestImpact} is computed for each facet that is not already requested and computes
 * potential the difference in the returned entities count should this facet be selected.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class FacetSummaryProducer implements ExtraResultProducer {
	private static final String ERROR_SANITY_CHECK = "Sanity check!";
	/**
	 * Reference to the query context that allows to access entity bodies.
	 */
	@Nonnull private final QueryContext queryContext;
	/**
	 * Contains {@link io.evitadb.api.query.require.FacetSummary#getFacetStatisticsDepth()} information.
	 */
	private final FacetStatisticsDepth facetStatisticsDepth;
	/**
	 * Filter formula produces all entity ids that are going to be returned by current query (including user-defined
	 * filter).
	 */
	private final Formula filterFormula;
	/**
	 * Contains references to all {@link FacetIndex#getFacetingEntities()} that were involved in query resolution.
	 */
	private final List<Map<Serializable, FacetEntityTypeIndex>> facetIndexes;
	/**
	 * Contains index of all requested {@link Facet#getFacetIds()} in the input query grouped by their {@link Facet#getType()}.
	 */
	private final Map<Serializable, IntSet> requestedFacets;

	@Nonnull
	@Override
	public <T extends Serializable> EvitaResponseExtraResult fabricate(@Nonnull List<T> entities) {
		// create facet calculators - in reaction to requested depth level
		final MemoizingFacetCalculator countsCalculator = new MemoizingFacetCalculator(queryContext, filterFormula);
		final ImpactCalculator impactCalculator = facetStatisticsDepth == FacetStatisticsDepth.COUNTS ?
			ImpactCalculator.NO_IMPACT : countsCalculator;
		// fabrication is a little transformation hell
		return new FacetSummary(
			facetIndexes
				.stream()
				// we need Stream<FacetEntityTypeIndex>
				.flatMap(it -> it.values().stream())
				.collect(
					Collectors.groupingBy(
						// group them by Facet#type
						FacetEntityTypeIndex::getEntityType,
						// reduce and transform data from indexes to FacetGroupStatistics
						Collectors.mapping(
							Function.identity(),
							new FacetGroupStatisticsCollector(
								// translates Facet#type to EntitySchema#reference#groupType
								facetEntityType -> ofNullable(queryContext.getSchema().getReferenceOrThrowException(facetEntityType).getGroupType()).orElse(facetEntityType),
								requestedFacets,
								countsCalculator,
								impactCalculator
							)
						)
					)
				)
				.values()
				.stream()
				.flatMap(Collection::stream)
				.collect(Collectors.toList())
		);
	}

	/**
	 * Collector translates data from {@link FacetEntityTypeIndex} to {@link FacetGroupStatistics}.
	 */
	@RequiredArgsConstructor
	private static class FacetGroupStatisticsCollector implements Collector<FacetEntityTypeIndex, HashMap<Integer, EntityTypeGroupAccumulator>, Collection<FacetGroupStatistics>> {
		/**
		 * Translates {@link Facet#getType()} to {@link EntitySchema#getReference(Serializable)} group type.
		 */
		private final Function<Serializable, Serializable> toGroupEntityTypeConvertor;
		/**
		 * Contains for each {@link Facet#getType()} set of requested facets.
		 */
		private final Map<Serializable, IntSet> requestedFacets;
		/**
		 * Facet calculator computes the entity count that relate to each facet.
		 */
		private final FacetCalculator countCalculator;
		/**
		 * Impact calculator computes the potential entity count returned should the facet be selected as well.
		 */
		private final ImpactCalculator impactCalculator;

		/**
		 * Returns TRUE if facet with `facetId` of specified `entityType` was requested by the user.
		 */
		public boolean isRequested(Serializable entityType, int facetId) {
			return ofNullable(requestedFacets.get(entityType))
				.map(it -> it.contains(facetId))
				.orElse(false);
		}

		@Override
		public Supplier<HashMap<Integer, EntityTypeGroupAccumulator>> supplier() {
			return HashMap::new;
		}

		@Override
		public BiConsumer<HashMap<Integer, EntityTypeGroupAccumulator>, FacetEntityTypeIndex> accumulator() {
			return (acc, facetEntityTypeIndex) -> facetEntityTypeIndex
				.getFacetGroupIndexesAsStream()
				.forEach(groupIx -> {
					final Serializable entityType = facetEntityTypeIndex.getEntityType();
					// get or create separate accumulator for the group statistics
					final EntityTypeGroupAccumulator groupAcc = acc.computeIfAbsent(
						groupIx.getGroupId(),
						gId -> new EntityTypeGroupAccumulator(
							entityType, gId, countCalculator, impactCalculator
						)
					);
					// create fct that can resolve whether the facet is requested for this entity type
					final IntFunction<Boolean> isRequestedResolver = facetId -> isRequested(entityType, facetId);
					// now go through all facets in the index and register their statistics
					groupIx.getFacetIdIndexes()
						.values()
						.forEach(facetIx ->
							groupAcc.addStatistics(facetIx, isRequestedResolver)
						);
				});
		}

		@Override
		public BinaryOperator<HashMap<Integer, EntityTypeGroupAccumulator>> combiner() {
			return (left, right) -> {
				// combine two HashMap<Integer, EntityTypeGroupAccumulator> together, right one is fully merged into left
				right.forEach((key, value) -> left.merge(key, value, EntityTypeGroupAccumulator::combine));
				return left;
			};
		}

		@Override
		public Function<HashMap<Integer, EntityTypeGroupAccumulator>, Collection<FacetGroupStatistics>> finisher() {
			return entityAcc ->
				entityAcc
					.values()
					.stream()
					.map(groupAcc -> {
						// collect all facet statistics
						final Map<Integer, FacetStatistics> facetStatistics = groupAcc.getFacetStatistics()
							.entrySet()
							.stream()
							// exclude those that has no results after base formula application
							.filter(it -> it.getValue().hasAnyResults())
							.collect(
								Collectors.toMap(
									Entry::getKey,
									it -> it.getValue().toFacetStatistics()
								)
							);
						// create facet group statistics
						return new FacetGroupStatistics(
							// translate Facet#type to EntitySchema#reference#groupType
							toGroupEntityTypeConvertor.apply(groupAcc.getEntityType()),
							groupAcc.getGroupId(),
							facetStatistics
						);
					}).collect(Collectors.toList());
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Set.of(Characteristics.UNORDERED);
		}
	}

	/**
	 * This mutable accumulator contains statistics for all facets of same `entityType` and `groupId`.
	 */
	@Data
	private static class EntityTypeGroupAccumulator {
		/**
		 * Contains {@link Facet#getType()}.
		 */
		@Nonnull private final Serializable entityType;
		/**
		 * Contains group id of the facets in this accumulator.
		 */
		@Nullable private final Integer groupId;
		/**
		 * Facet calculator computes the entity count that relate to each facet.
		 */
		private final FacetCalculator countCalculator;
		/**
		 * Impact calculator computes the potential entity count returned should the facet be selected as well.
		 */
		private final ImpactCalculator impactCalculator;
		/**
		 * Contains statistic accumulator for each of the facet.
		 */
		private final Map<Integer, FacetAccumulator> facetStatistics = new HashMap<>();

		/**
		 * Registers new {@link FacetAccumulator} statistics in the local state.
		 */
		public void addStatistics(
			@Nonnull FacetIdIndex facetIx,
			@Nonnull IntFunction<Boolean> requiredResolver
		) {
			this.facetStatistics.compute(
				facetIx.getFacetId(),
				(fId, facetAccumulator) -> {
					final FacetAccumulator newAccumulator = new FacetAccumulator(
						entityType,
						fId,
						groupId,
						requiredResolver.apply(fId),
						facetIx.getRecords(),
						countCalculator,
						impactCalculator
					);
					if (facetAccumulator == null) {
						return newAccumulator;
					} else {
						return facetAccumulator.combine(newAccumulator);
					}
				}
			);
		}

		/**
		 * Combines two EntityTypeGroupAccumulator together. It adds everything from the `otherAccumulator` to self
		 * instance and returns self.
		 */
		public EntityTypeGroupAccumulator combine(EntityTypeGroupAccumulator otherAccumulator) {
			Assert.isTrue(entityType.equals(otherAccumulator.entityType), () -> new IllegalStateException(ERROR_SANITY_CHECK));
			Assert.isTrue(Objects.equals(groupId, otherAccumulator.groupId), () -> new IllegalStateException(ERROR_SANITY_CHECK));
			otherAccumulator.getFacetStatistics()
				.forEach((key, value) -> this.facetStatistics.merge(key, value, FacetAccumulator::combine));
			return this;
		}

	}

	/**
	 * This mutable accumulator contains statistics for single facet.
	 */
	@Data
	private static class FacetAccumulator {
		private static final Formula[] EMPTY_INT_FORMULA = new Formula[0];
		/**
		 * Contains entityType - obviously.
		 */
		private final Serializable entityType;
		/**
		 * Contains facetGroupId - obviously.
		 */
		private final Integer facetGroupId;
		/**
		 * Contains facetId - obviously.
		 */
		private final int facetId;
		/**
		 * Contains TRUE if this particular facet was requested by in the input query.
		 */
		private final boolean required;
		/**
		 * Facet calculator computes the entity count that relate to each facet.
		 */
		private final FacetCalculator countCalculator;
		/**
		 * Impact calculator computes the potential entity count returned should the facet be selected as well.
		 */
		private final ImpactCalculator impactCalculator;
		/**
		 * Contains finished result formula so that {@link #getCount()} can be called multiple times without performance
		 * penalty.
		 */
		private Formula resultFormula;
		/**
		 * Contains bitmaps of all entity primary keys that posses this facet. All bitmaps need to be combined with OR
		 * relation in order to get full entity primary key list.
		 */
		private List<Bitmap> facetEntityIds = new LinkedList<>();

		public FacetAccumulator(@Nonnull Serializable entityType, int facetId, @Nullable Integer facetGroupId, boolean required, @Nonnull Bitmap facetEntityIds, @Nonnull FacetCalculator countCalculator, @Nonnull ImpactCalculator impactCalculator) {
			this.entityType = entityType;
			this.facetId = facetId;
			this.facetGroupId = facetGroupId;
			this.required = required;
			this.countCalculator = countCalculator;
			this.impactCalculator = impactCalculator;
			this.facetEntityIds.add(facetEntityIds);
		}

		/**
		 * Produces final result of this accumulator.
		 */
		public FacetStatistics toFacetStatistics() {
			return new FacetStatistics(
				facetId,
				required,
				getCount(),
				impactCalculator.calculateImpact(
					this.entityType, this.facetId, this.facetGroupId, this.required,
					this.facetEntityIds.toArray(new io.evitadb.index.bitmap.Bitmap[0])
				)
			);
		}

		/**
		 * Combines two FacetAccumulator together. It adds everything from the `otherAccumulator` to self
		 * instance and returns self.
		 */
		public FacetAccumulator combine(FacetAccumulator otherAccumulator) {
			Assert.isTrue(facetId == otherAccumulator.facetId, () -> new IllegalStateException(ERROR_SANITY_CHECK));
			Assert.isTrue(required == otherAccumulator.required, () -> new IllegalStateException(ERROR_SANITY_CHECK));
			this.facetEntityIds.addAll(otherAccumulator.getFacetEntityIds());
			return this;
		}

		/**
		 * Returns true if there is at least one entity in the query result that has this facet.
		 */
		public boolean hasAnyResults() {
			return getCount() > 0;
		}

		/**
		 * Returns count of all entities in the query response that has this facet.
		 */
		public int getCount() {
			if (resultFormula == null) {
				// we need to combine all collected facet formulas and then AND them with base formula to get rid
				// of entity primary keys that haven't passed the filter logic
				resultFormula = countCalculator.createCountFormula(
					entityType, facetId, facetGroupId,
					facetEntityIds.toArray(new io.evitadb.index.bitmap.Bitmap[0])
				);
			}
			// this is the most expensive call in this very class
			return resultFormula.compute().size();
		}
	}

}
