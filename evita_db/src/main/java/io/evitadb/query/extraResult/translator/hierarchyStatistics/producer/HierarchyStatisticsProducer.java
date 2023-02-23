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

package io.evitadb.query.extraResult.translator.hierarchyStatistics.producer;

import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.io.extraResult.HierarchyStatistics;
import io.evitadb.api.io.extraResult.HierarchyStatistics.LevelInfo;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.query.filter.Language;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.index.hierarchy.HierarchyIndex;
import io.evitadb.index.hierarchy.HierarchyNode;
import io.evitadb.index.hierarchy.HierarchyVisitor;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultProducer;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * {@link HierarchyStatisticsProducer} creates {@link HierarchyStatistics} DTO instance and does the heavy lifting to
 * compute all information necessary. The producer aggregates {@link EntityTypeHierarchyProducer} for each
 * {@link io.evitadb.api.query.require.HierarchyStatistics} requirement and combines them into the single result.
 *
 * Producer uses {@link HierarchyIndex} of the targeted entity to {@link HierarchyIndex#traverseHierarchy(HierarchyVisitor)}
 * finding all entities linked to the queried entity type. It respects {@link Language} and {@link WithinHierarchy}
 * filtering constraints when filtering the entity tree. For each such hierarchical entity node it finds all entity
 * primary keys of the queried entity type connected to it, combines them with entity id array that is produced by
 * the query (only matching ids will remain) and uses this information to fill the {@link LevelInfo#getCardinality()}
 * information. {@link LevelInfo} with zero cardinality are filtered out from the result.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class HierarchyStatisticsProducer implements ExtraResultProducer {
	/**
	 * Reference to the query context that allows to access entity bodies.
	 */
	@Nonnull private final QueryContext queryContext;
	/**
	 * Contains type of the entity that is requested by the query.
	 */
	@Nonnull private final Serializable requestedEntity;
	/**
	 * Contains language specified in {@link io.evitadb.api.io.EvitaRequest}. Language is valid for entire query.
	 */
	@Nullable private final Locale language;
	/**
	 * Contains filtering formula tree that was used to produce results so that computed {@link LevelInfo#getCardinality()}
	 * is reduced according to the input filter.
	 */
	@Nonnull private final Formula filteringFormula;
	/**
	 * Contains set of producer instances, that should build collections of {@link LevelInfo} for each requested
	 * entityTypes. Each producer contains all information necessary.
	 */
	@Nonnull private final List<EntityTypeHierarchyProducer> hierarchyRequests = new LinkedList<>();

	@Nullable
	@Override
	public <T extends Serializable> EvitaResponseExtraResult fabricate(@Nonnull List<T> entities) {
		//noinspection unchecked,rawtypes
		return new HierarchyStatistics(
			hierarchyRequests
				.stream()
				.collect(
					Collectors.toMap(
						EntityTypeHierarchyProducer::getEntityType,
						it -> (List) it.createStatistics(filteringFormula, language)
					)
				)
		);
	}

	/**
	 * Registers new {@link LevelInfo} collection computation request for passed `entityType`.
	 *
	 * @param entityType                    relates to hierarchy {@link io.evitadb.api.schema.EntitySchema}
	 * @param rootNode                      limits the statistics to certain subtree of the hierarchy
	 * @param hierarchyIndex                references hierarchy index
	 * @param hierarchyReferencingEntityPks represents function that produces bitmap of queried entity ids connected
	 *                                      with particular hierarchical entity
	 * @param requirements                  contains set of requirement constraints that can enforce loading full entity
	 *                                      instead of just primary keys
	 */
	public void addHierarchyRequest(
		@Nonnull Serializable entityType,
		@Nullable WithinHierarchy rootNode,
		@Nonnull EntityIndex hierarchyIndex,
		@Nonnull IntFunction<Bitmap> hierarchyReferencingEntityPks,
		@Nonnull EntityContentRequire[] requirements
	) {
		// first create the `entityFetcher` that either returns simple integer primary keys or full entities
		final IntFunction<Serializable> entityFetcher;
		if (ArrayUtils.isEmpty(requirements)) {
			entityFetcher = entityPk -> entityPk;
		} else {
			entityFetcher = entityPk -> this.queryContext.fetchEntity(entityType, entityPk, requirements);
		}
		// and now register the produces for this particular entity
		this.hierarchyRequests.add(
			new EntityTypeHierarchyProducer(
				entityType, entityType.equals(requestedEntity), rootNode, entityFetcher,
				hierarchyIndex, hierarchyReferencingEntityPks, requirements
			)
		);
	}

	/**
	 * Producer class
	 */
	@Data
	private static class EntityTypeHierarchyProducer {
		/**
		 * Relates to hierarchy {@link io.evitadb.api.schema.EntitySchema}
		 */
		@Nonnull @Getter private final Serializable entityType;
		/**
		 * Contains true if hierarchy statistics is generated for the same entity as queried.
		 */
		private final boolean targetsSameEntity;
		/**
		 * Contains {@link WithinHierarchy} filtering constraint if it was part of the query filter.
		 */
		@Nullable private final WithinHierarchy withinHierarchy;
		/**
		 * Contains function that converts hierarchy entity id to the requested data type (either it's left as primary
		 * key or converted to full-fledged entity).
		 */
		@Nonnull private final IntFunction<Serializable> entityFetcher;
		/**
		 * Contains reference to the {@link HierarchyIndex}.
		 */
		@Nonnull private final EntityIndex hierarchyIndex;
		/**
		 * Contains a function that produces bitmap of queried entity ids connected with particular hierarchical entity.
		 */
		@Nonnull private final IntFunction<Bitmap> hierarchyReferencingEntityPks;
		/**
		 * Contains set of requirement constraints that can enforce loading full entity instead of just primary keys.
		 */
		@Nonnull private final EntityContentRequire[] requirements;

		/**
		 * Fabricates single collection of {@link LevelInfo} for requested hierarchical entity type. It respects
		 * the {@link Language} and {@link WithinHierarchy} constraints used in the query. It also uses
		 * `filteringFormula` to limit the reported cardinalities in level info objects.
		 */
		@Nonnull
		public List<LevelInfo<Serializable>> createStatistics(@Nonnull Formula filteringFormula, @Nullable Locale language) {
			// get roaring bitmap of filtering entity ids
			final RoaringBitmap filteredEntityPks = RoaringBitmapBackedBitmap.getRoaringBitmap(filteringFormula.compute());
			final Deque<Accumulator<Serializable>> accumulator = new LinkedList<>();
			// the language predicate is used to filter out entities that doesn't have requested language variant
			final IntPredicate languagePredicate = language == null ?
				value -> true :
				new HierarchyEntityPredicate(hierarchyIndex, language);

			// accumulator is used to gather information about its children gradually
			final Accumulator<Serializable> root = new Accumulator<>(entityType, 0);
			accumulator.add(root);

			if (withinHierarchy == null) {
				// if there is not within hierarchy constraint we start at root nodes
				hierarchyIndex.traverseHierarchy(
					new StatisticsHierarchyVisitor(
						targetsSameEntity, languagePredicate, filteredEntityPks, accumulator,
						hierarchyReferencingEntityPks, entityFetcher
					)
				);
			} else {
				// if root node is set, use different traversal method
				hierarchyIndex.traverseHierarchyFromNode(
					new StatisticsHierarchyVisitor(
						targetsSameEntity, languagePredicate, filteredEntityPks, accumulator,
						hierarchyReferencingEntityPks, entityFetcher
					),
					withinHierarchy.getParentId(),
					withinHierarchy.isExcludingRoot(),
					withinHierarchy.getExcludedChildrenIds()
				);
			}

			return root.getChildren();
		}

		/**
		 * This class allows to test, whether the hierarchical entity has requested language variant.
		 */
		private static class HierarchyEntityPredicate implements IntPredicate {
			/**
			 * Bitmap contains id of all hierarchical entities of the requested language.
			 * Bitmap contains distinct primary ids ordered in ascending form.
			 */
			private final Bitmap recordsInLanguage;

			public HierarchyEntityPredicate(@Nonnull EntityIndex hierarchyIndex, @Nonnull Locale language) {
				this.recordsInLanguage = hierarchyIndex.getRecordsWithLanguageFormula(language).compute();
			}

			@Override
			public boolean test(int value) {
				return recordsInLanguage.contains(value);
			}
		}

		/**
		 * This {@link HierarchyVisitor} implementation is called for each hierarchical entity and cooperates
		 * with {@link Accumulator} to compose a tree of {@link LevelInfo} objects.
		 */
		@RequiredArgsConstructor
		private static class StatisticsHierarchyVisitor implements HierarchyVisitor {
			/**
			 * Contains true if hierarchy statistics is generated for the same entity as queried.
			 */
			private final boolean targetsSameEntity;
			/**
			 * Predicate is used to filter out hierarchical entities that doesn't match the language requirement.
			 */
			@Nonnull private final IntPredicate languagePredicate;
			/**
			 * Contains bitmap of entity primary keys that fulfills the filter of the query.
			 */
			@Nonnull private final RoaringBitmap filteredEntityPks;
			/**
			 * Deque of accumulators allow to compose a tree of results
			 */
			@Nonnull private final Deque<Accumulator<Serializable>> accumulator;
			/**
			 * Contains a function that produces bitmap of queried entity ids connected with particular hierarchical entity.
			 */
			@Nonnull private final IntFunction<Bitmap> hierarchyReferencingEntityPks;
			/**
			 * Function that allows to fetch {@link SealedEntity} for `entityType` + `primaryKey` combination. SealedEntity
			 * is fetched to the depth specified by {@link RequireConstraint[]}.
			 */
			@Nonnull private final IntFunction<Serializable> entityFetcher;

			@Override
			public void visit(@Nonnull HierarchyNode node, @Nonnull Runnable childrenTraverser) {
				final int entityPrimaryKey = node.getEntityPrimaryKey();
				// check whether the hierarchical entity passes the language test
				if (languagePredicate.test(entityPrimaryKey)) {
					// get all queried entity primary keys that refer to this hierarchical node
					final Bitmap allEntitiesReferencingEntity = hierarchyReferencingEntityPks.apply(entityPrimaryKey);
					// now combine them with primary keys that are really returned by the query and compute matching count
					final int matchingCount = RoaringBitmap.and(
						RoaringBitmapBackedBitmap.getRoaringBitmap(allEntitiesReferencingEntity),
						filteredEntityPks
					).getCardinality();

					// now fetch the appropriate form of the hierarchical entity
					final Serializable hierarchyEntity = entityFetcher.apply(entityPrimaryKey);
					// and create element in accumulator that will be filled in
					accumulator.push(new Accumulator<>(hierarchyEntity, matchingCount));
					// traverse subtree - filling up the accumulator on previous row
					childrenTraverser.run();
					// now remove current accumulator from stack
					final Accumulator<Serializable> finalizedAccumulator = accumulator.pop();
					// and if its cardinality is greater than zero (contains at least one queried entity)
					// add it to the result
					if (targetsSameEntity) {
						accumulator.peek().add(finalizedAccumulator.toLevelInfo());
					} else {
						Optional.of(finalizedAccumulator.toLevelInfo())
							.filter(it -> it.getCardinality() > 0)
							.ifPresent(it -> accumulator.peek().add(it));
					}
				}
			}
		}
	}

	/**
	 * Accumulator serves to aggregate information about children before creating immutable statistics result.
	 */
	@RequiredArgsConstructor
	private static class Accumulator<T extends Serializable> {
		/**
		 * The hierarchical entity in proper form.
		 */
		private final T entity;
		/**
		 * The count of queried entities directly referencing this hierarchical entity (respecting current query filter).
		 */
		private final int directEntityCount;
		/**
		 * Mutable container for gradually added children.
		 */
		@Getter private final List<LevelInfo<T>> children = new LinkedList<>();

		/**
		 * Adds information about this hierarchy node children statistics.
		 */
		public void add(LevelInfo<T> nodeAsLevel) {
			this.children.add(nodeAsLevel);
		}

		/**
		 * Converts accumulator data to immutable {@link LevelInfo} DTO.
		 */
		public LevelInfo<T> toLevelInfo() {
			return new LevelInfo<>(
				entity,
				IntStream.concat(
					IntStream.of(directEntityCount),
					children.stream().mapToInt(LevelInfo::getCardinality)
				).sum(),
				children
			);
		}
	}

}
