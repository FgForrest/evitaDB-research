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

package io.evitadb.query.extraResult.translator.parents.producer;

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.io.extraResult.Parents;
import io.evitadb.api.io.extraResult.Parents.ParentsByType;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.hierarchy.HierarchyIndex;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultProducer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link ParentsProducer} creates {@link Parents} DTO instance and does the heavy lifting to compute all information
 * necessary. The producer aggregates {@link EntityTypeParentsProducer} for each {@link io.evitadb.api.query.require.Parents}
 * or {@link io.evitadb.api.query.require.ParentsOfType} requirement and combines them into the single result.
 *
 * For each entity returned in {@link DataChunk} (i.e. {@link io.evitadb.api.io.EvitaResponseBase}) it consults
 * {@link HierarchyIndex} and retrieves all parents in respective hierarchy and adds them
 * to the result DTO. If entity bodies are requested it also fetches appropriate {@link SealedEntity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class ParentsProducer implements ExtraResultProducer {
	/**
	 * Reference to the query context that allows to access entity bodies.
	 */
	@Nonnull private final QueryContext queryContext;
	/**
	 * Function that returns array of primary keys of entities of specified `entityType` the entity with `primaryKey`
	 * references.
	 */
	private final BiFunction<Integer, Serializable, Integer[]> referenceFetcher;
	/**
	 * Predicate returning TRUE in case the {@link io.evitadb.api.io.EvitaRequest} contains requirement for {@link DataChunk}
	 * to fetch references of specified `entityType`. I.e. we can relly on {@link SealedEntity#getReferences(Serializable)}
	 * returning data needed for parents computation.
	 */
	private final Predicate<Serializable> referenceFetchedPredicate;
	/**
	 * List of lambdas that each compute part of the {@link Parents} DTO. Each lambda corresponds to single
	 * {@link io.evitadb.api.query.require.Parents} or {@link io.evitadb.api.query.require.ParentsOfType} requirement.
	 */
	private final List<EntityTypeParentsProducer<?>> producersByType = new LinkedList<>();

	public ParentsProducer(
		@Nonnull QueryContext queryContext,
		@Nonnull Predicate<Serializable> referenceFetchedPredicate,
		@Nonnull Serializable entityType,
		boolean includingSelf,
		@Nonnull BiFunction<Integer, Serializable, Integer[]> referenceFetcher,
		@Nonnull EntityIndex globalIndex,
		@Nonnull EntityContentRequire[] requirements
	) {
		this.queryContext = queryContext;
		this.referenceFetcher = referenceFetcher;
		this.referenceFetchedPredicate = referenceFetchedPredicate;
		this.producersByType.add(
			new EntityTypeParentsProducer<>(
				queryContext, entityType,
				referenceFetchedPredicate.test(entityType),
				includingSelf,
				referenceFetcher,
				globalIndex, requirements
			)
		);
	}

	@Nullable
	@Override
	 public <T extends Serializable> EvitaResponseExtraResult fabricate(@Nonnull List<T> entities) {
		return new Parents(
			// call each producer lambda to make up the result
			producersByType
				.stream()
				.collect(
					Collectors.toMap(
						EntityTypeParentsProducer::getEntityType,
						it -> it.compute(entities)
					)
				)
		);
	}

	/**
	 * Registers a lambda that will compute parents for requested `entityType`, loading the results according
	 * to `requirements` array. Lambda will use {@link HierarchyIndex} of passed `globalIndex`.
	 */
	public void addRequestedParents(
		@Nonnull Serializable entityType,
		@Nonnull EntityIndex globalIndex,
		@Nullable EntityContentRequire[] requirements
	) {
		this.producersByType.add(
			new EntityTypeParentsProducer<>(
				queryContext, entityType,
				referenceFetchedPredicate.test(entityType),
				false,
				referenceFetcher,
				globalIndex, requirements
			)
		);
	}

	/**
	 * Registers a lambda that will compute parents for requested `entityType`, loading the results according
	 * to `requirements` array. Lambda will use {@link HierarchyIndex} of passed `globalIndex`. Computed parents will
	 * also include the referenced entity.
	 */
	public void addRequestedParentsIncludingSelf(
		@Nonnull Serializable entityType,
		@Nonnull EntityIndex globalIndex,
		@Nullable EntityContentRequire[] requirements
	) {
		this.producersByType.add(
			new EntityTypeParentsProducer<>(
				queryContext, entityType,
				referenceFetchedPredicate.test(entityType),
				true,
				referenceFetcher,
				globalIndex, requirements
			)
		);
	}

	/**
	 * The class represents a computational lambda for single {@link ParentsByType} sub-object of overall {@link Parents}
	 * container.
	 */
	private static class EntityTypeParentsProducer<T extends Serializable> {
		/**
		 * Contains {@link EntitySchema#getName()} of the referenced entity the parents are computed for.
		 */
		@Getter private final T entityType;
		/**
		 * Reference to the query context that allows to access entity bodies.
		 */
		@Nonnull private final QueryContext queryContext;
		/**
		 * Contains TRUE if {@link #entityType} equals to {@link EvitaRequest#getEntityType()}.
		 */
		private final boolean requestedEntityTypeMatch;
		/**
		 * Contains TRUE in case the {@link io.evitadb.api.io.EvitaRequest} contains requirement for {@link DataChunk}
		 * to fetch references of specified `entityType`. I.e. we can relly on {@link SealedEntity#getReferences(Serializable)}
		 * returning data needed for parents computation.
		 */
		private final boolean entityTypeReferenceFetched;
		/**
		 * Function that returns array of primary keys of entities of specified `entityType` the entity with `primaryKey`
		 * references.
		 */
		private final BiFunction<Integer, Serializable, Integer[]> referenceFetcher;
		/**
		 * Contains internal function that allows to unify and share logic for gathering the output data.
		 */
		private final Collector<Integer, ?, Map<Integer, Serializable[]>> parentEntityCollector;

		public EntityTypeParentsProducer(
			@Nonnull QueryContext queryContext,
			@Nonnull T entityType,
			boolean entityTypeReferenceFetched,
			boolean includingSelf,
			@Nonnull BiFunction<Integer, Serializable, Integer[]> referenceFetcher,
			@Nonnull EntityIndex globalIndex,
			@Nullable EntityContentRequire[] requirements
		) {
			this.queryContext = queryContext;
			this.entityType = entityType;
			this.requestedEntityTypeMatch = Objects.equals(queryContext.getSchema().getName(), entityType);
			this.entityTypeReferenceFetched = entityTypeReferenceFetched;
			this.referenceFetcher = referenceFetcher;
			final boolean requirementsPresent = !ArrayUtils.isEmpty(requirements);
			this.parentEntityCollector = Collectors.toMap(
				Function.identity(),
				refEntityId -> {
					// return parents only or parents including the refEntityId
					final Integer[] parents = includingSelf ?
						globalIndex.listHierarchyNodesFromRootToTheNodeIncludingSelf(refEntityId) :
						globalIndex.listHierarchyNodesFromRootToTheNode(refEntityId);
					// return primary keys only (Integer) or full entities of requested size (SealedEntity)
					return requirementsPresent ?
						Arrays.stream(parents)
							.map(it -> this.queryContext.fetchEntity(entityType, it, requirements))
							.toArray(SealedEntity[]::new) :
						parents;
				}
			);
		}

		/**
		 * Computes {@link ParentsByType} DTO - expected to be called only once.
		 */
		@Nonnull
		public <S extends Serializable> ParentsByType<Serializable> compute(@Nonnull List<S> entities) {
			if (entities.isEmpty()) {
				// if data chunk contains no entities - our result will be empty as well
				return new ParentsByType<>(entityType, Collections.emptyMap());
			} else if (entityTypeReferenceFetched && entities.get(0) instanceof EntityContract) {
				// if data chunk contains EntityContract entities with information of requested referenced entity
				// take advantage of already preloaded data
				return new ParentsByType<>(
					entityType,
					entities
						.stream()
						.map(EntityContract.class::cast)
						.collect(
							Collectors.toMap(
								EntityContract::getPrimaryKey,
								sealedEntity -> {
									// if requested entity match the referenced entity type (for example category parents
									// are requested when querying categories)
									if (requestedEntityTypeMatch) {
										// use only the primary key of the entity in data chunk
										return Stream.of(sealedEntity.getPrimaryKey())
											// and use the shared collector logic
											.collect(parentEntityCollector);
									} else {
										// otherwise, get primary keys of referenced entities of particular type
										return sealedEntity
											.getReferences(entityType)
											.stream()
											.map(refEntity -> refEntity.getReferencedEntity().getPrimaryKey())
											// and use the shared collector logic
											.collect(parentEntityCollector);
									}
								}
							)
						)
				);
			} else if (entities.get(0) instanceof EntityContract) {
				// if data chunk contains EntityContract entities but these lack information of requested referenced
				// entity - use the translator function to fetch requested referenced container from MemTable lazily
				return new ParentsByType<>(
					entityType,
					entities
						.stream()
						.map(EntityContract.class::cast)
						.collect(
							Collectors.toMap(
								EntityContract::getPrimaryKey,
								sealedEntity -> {
									// if requested entity match the referenced entity type (for example category parents
									// are requested when querying categories)
									if (requestedEntityTypeMatch) {
										// use only the primary key of the entity in data chunk
										return Stream.of(sealedEntity.getPrimaryKey())
											// and use the shared collector logic
											.collect(parentEntityCollector);
									} else {
										// otherwise, get primary keys of referenced entities of particular type
										return Arrays.stream(referenceFetcher.apply(sealedEntity.getPrimaryKey(), entityType))
											// and use the shared collector logic
											.collect(parentEntityCollector);
									}
								}
							)
						)
				);
			} else {
				// if data chunk contains only primary keys of entities
				// use the translator function to fetch requested referenced container from MemTable lazily
				return new ParentsByType<>(
					entityType,
					entities
						.stream()
						.map(EntityReference.class::cast)
						.map(EntityReference::getPrimaryKey)
						.collect(
							Collectors.toMap(
								Function.identity(),
								entityPrimaryKey -> {
									// if requested entity match the referenced entity type (for example category parents
									// are requested when querying categories)
									if (requestedEntityTypeMatch) {
										// use only the primary key of the entity in data chunk
										return Stream.of(entityPrimaryKey)
											// and use the shared collector logic
											.collect(parentEntityCollector);
									} else {
										// otherwise, get primary keys of referenced entities of particular type
										return Arrays.stream(referenceFetcher.apply(entityPrimaryKey, entityType))
											// and use the shared collector logic
											.collect(parentEntityCollector);
									}
								}
							)
						)
				);
			}
		}
	}

}
