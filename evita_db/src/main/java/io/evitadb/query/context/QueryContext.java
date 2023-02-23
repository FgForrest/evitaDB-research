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

package io.evitadb.query.context;

import io.evitadb.api.EntityCollection;
import io.evitadb.api.EvitaSession;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.api.data.structure.EntityStorageContainerAccessor;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.query.*;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.cache.CacheSupervisor;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.GlobalEntityIndex;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.extraResult.CacheableEvitaResponseExtraResultComputer;
import io.evitadb.query.extraResult.EvitaResponseExtraResultComputer;
import io.evitadb.query.response.QueryTelemetry;
import io.evitadb.query.response.QueryTelemetry.QueryPhase;
import io.evitadb.query.sort.Sorter;
import io.evitadb.storage.model.storageParts.entity.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * Query context aggregates references to all the instances that are required to process the {@link EvitaRequest}.
 * The object serves as single "go to" object while preparing or executing {@link io.evitadb.query.QueryPlan}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class QueryContext {
	private static final EntityIndexKey GLOBAL_INDEX_KEY = new EntityIndexKey(EntityIndexType.GLOBAL);

	/**
	 * Contains reference to the entity collection that is targeted by {@link #evitaRequest}.
	 */
	@Nonnull private final EntityCollection entityCollection;
	/**
	 * Allows accessing entity {@link io.evitadb.storage.model.storageParts.StoragePart} directly.
	 */
	@Nonnull private final EntityStorageContainerAccessor entityStorageContainerAccessor;
	/**
	 * Contains reference to the enveloping {@link EvitaSession} within which the {@link #evitaRequest} is executed.
	 */
	@Nonnull private final EvitaSession evitaSession;
	/**
	 * Contains input in {@link EvitaRequest}.
	 */
	@Getter
	@Nonnull private final EvitaRequest evitaRequest;
	/**
	 * Contains {@link QueryTelemetry} information that measures the costs of each {@link #evitaRequest} processing
	 * phases.
	 */
	@Getter
	@Nonnull private final QueryTelemetry telemetry;
	/**
	 * Accessor to all other entity collections.
	 */
	@Nonnull private final Function<Serializable, EntityCollection> externalEntityCollectionAccessor;
	/**
	 * Collection of search indexes prepared to handle queries.
	 */
	@Nonnull private final Map<EntityIndexKey, EntityIndex> indexes;
	/**
	 * Formula supervisor is an entry point to the Evita cache. The idea is that each {@link Formula} can be identified
	 * by its {@link Formula#computeHash(LongHashFunction)} method and when the supervisor identifies that certain
	 * formula is frequently used in query formulas it moves its memoized results to the cache. The non-computed formula
	 * of the same hash will be exchanged in next query that contains it with the cached formula that already contains
	 * memoized result.
	 */
	@Getter
	@Nonnull private final CacheSupervisor cacheSupervisor;
	/**
	 * Contains list of prefetched entities if they were considered worthwhile to prefetch -
	 * see {@link io.evitadb.query.algebra.deferred.SelectionFormula} for more information.
	 */
	@Getter
	private List<SealedEntity> prefetchedEntities;

	/**
	 * Method loads requested entity contents by specifying its primary key.
	 */
	@Nullable
	public SealedEntity fetchEntity(int entityPrimaryKey) {
		return entityCollection.getEntity(entityPrimaryKey, evitaRequest);
	}

	/**
	 * Method loads requested entity contents by specifying its primary key.
	 */
	@Nullable
	public SealedEntity fetchEntity(int entityPrimaryKey, @Nonnull EntityContentRequire... requireConstraints) {
		final EvitaRequest fetchRequest = fabricateFetchRequest(getSchema().getName(), requireConstraints);
		return entityCollection.getEntity(entityPrimaryKey, fetchRequest);
	}

	/**
	 * Method loads entity contents by specifying its type and primary key. Fetching logic respect language from
	 * the original {@link EvitaRequest}
	 */
	@Nullable
	public SealedEntity fetchEntity(@Nonnull Serializable entityType, int entityPrimaryKey, @Nonnull EntityContentRequire... requireConstraints) {
		if (entityType.equals(getSchema().getName())) {
			return fetchEntity(entityPrimaryKey, requireConstraints);
		} else {
			final EvitaRequest fetchRequest = fabricateFetchRequest(entityType, requireConstraints);
			return externalEntityCollectionAccessor.apply(entityType).getEntity(entityPrimaryKey, fetchRequest);
		}
	}

	/**
	 * Method loads requested entity contents by specifying its primary key.
	 */
	@Nonnull
	public List<SealedEntity> fetchEntities(int[] entityPrimaryKeys, @Nonnull EntityContentRequire... requireConstraints) {
		final ArrayList<SealedEntity> result = new ArrayList<>(entityPrimaryKeys.length);
		EvitaRequest fetchRequest = null;
		for (int entityPrimaryKey : entityPrimaryKeys) {
			fetchRequest = ofNullable(fetchRequest).orElseGet(() -> fabricateFetchRequest(getSchema().getName(), requireConstraints));
			final SealedEntity fetchedEntity = entityCollection.getEntity(entityPrimaryKey, fetchRequest);
			result.add(fetchedEntity);
		}
		return result;
	}

	/**
	 * Returns {@link EntityIndex} of external entity type by its key and entity type.
	 */
	@Nullable
	public EntityIndex getEntityIndex(@Nonnull Serializable entityType, @Nonnull EntityIndexKey entityIndexKey) {
		return externalEntityCollectionAccessor.apply(entityType).getIndexByKeyIfExists(entityIndexKey);
	}

	/**
	 * Returns {@link EntityIndex} by its key.
	 */
	@Nullable
	public EntityIndex getEntityIndex(@Nonnull EntityIndexKey entityIndexKey) {
		return indexes.get(entityIndexKey);
	}

	/**
	 * Shorthand for {@link QueryTelemetry#addStep(QueryPhase, Serializable...)}
	 */
	@Nonnull
	public QueryTelemetry addStep(QueryPhase phase) {
		return telemetry.addStep(phase);
	}

	/**
	 * Shorthand for {@link EvitaRequest#getQuery()} and {@link Query#getFilterBy()}.
	 */
	@Nullable
	public FilterConstraint getFilterBy() {
		return evitaRequest.getQuery().getFilterBy();
	}

	/**
	 * Shorthand for {@link EvitaRequest#getQuery()} and {@link Query#getOrderBy()} ()}.
	 */
	@Nullable
	public OrderConstraint getOrderBy() {
		return evitaRequest.getQuery().getOrderBy();
	}

	/**
	 * Shorthand for {@link EvitaRequest#getQuery()} and {@link Query#getRequire()} ()}.
	 */
	@Nullable
	public RequireConstraint getRequire() {
		return evitaRequest.getQuery().getRequire();
	}

	/**
	 * Returns language specified in {@link EvitaRequest}. Language is valid for entire query.
	 */
	@Nullable
	public Locale getLanguage() {
		return evitaRequest.getLanguage();
	}

	/**
	 * Returns query price mode specified in {@link EvitaRequest}. Query price mode is valid for entire query.
	 */
	@Nonnull
	public QueryPriceMode getQueryPriceMode() {
		return evitaRequest.getQueryPriceMode();
	}

	/**
	 * Returns entity schema.
	 */
	@Nonnull
	public EntitySchema getSchema() {
		return entityCollection.getSchema();
	}

	/**
	 * Returns entity schema by its type.
	 */
	@Nullable
	public EntitySchema getSchema(Serializable entityType) {
		return externalEntityCollectionAccessor.apply(entityType).getSchema();
	}

	/**
	 * Returns {@link EntityIndex} by its key and entity type.
	 */
	@Nonnull
	public GlobalEntityIndex getGlobalEntityIndex() {
		return ofNullable(getEntityIndex(GLOBAL_INDEX_KEY))
			.map(GlobalEntityIndex.class::cast)
			.orElseThrow(() -> new IllegalStateException("Global index of entity " + getSchema().getName() + " unexpectedly not found!"));
	}

	/**
	 * Returns {@link EntityIndex} by its key and entity type.
	 */
	@Nonnull
	public GlobalEntityIndex getGlobalEntityIndex(Serializable entityType) {
		return ofNullable(getEntityIndex(entityType, GLOBAL_INDEX_KEY))
			.map(GlobalEntityIndex.class::cast)
			.orElseThrow(() -> new IllegalStateException("Global index of entity " + entityType + " unexpectedly not found!"));
	}

	/**
	 * Analyzes the input formula for cacheable / cached formulas and replaces them with appropriate counterparts (only
	 * if cache is enabled).
	 */
	@Nonnull
	public Formula analyse(@Nonnull Formula formula) {
		return cacheSupervisor.analyse(evitaSession, evitaRequest.getEntityType(), formula);
	}

	/**
	 * Analyzes the input formula for cacheable / cached formulas and replaces them with appropriate counterparts (only
	 * if cache is enabled).
	 */
	@Nonnull
	public <U, T extends CacheableEvitaResponseExtraResultComputer<U>> EvitaResponseExtraResultComputer<U> analyse(@Nonnull T computer) {
		return cacheSupervisor.analyse(evitaSession, evitaRequest.getEntityType(), computer);
	}

	/**
	 * Analyzes the input formula for cacheable / cached formulas and replaces them with appropriate counterparts (only
	 * if cache is enabled).
	 */
	@Nonnull
	public <U, T extends CacheableEvitaResponseExtraResultComputer<U>> EvitaResponseExtraResultComputer<U> analyse(@Nonnull Serializable entityType, @Nonnull T computer) {
		return cacheSupervisor.analyse(evitaSession, entityType, computer);
	}

	/**
	 * Creates slice of entity primary keys that respect filtering constraint, specified sorting and is sliced according
	 * to requested offset and limit.
	 */
	@Nonnull
	public DataChunk<Integer> createDataChunk(int totalRecordCount, @Nonnull Formula filteringFormula, @Nonnull Sorter sorter) {
		final int firstRecordOffset = evitaRequest.getFirstRecordOffset(totalRecordCount);
		return evitaRequest.createDataChunk(
			totalRecordCount,
			Arrays.stream(sorter.sortAndSlice(filteringFormula, firstRecordOffset, firstRecordOffset + evitaRequest.getLimit()))
				.boxed()
				.collect(Collectors.toList())
		);
	}

	/**
	 * Shorthand for {@link EntityStorageContainerAccessor#getEntityStorageContainer(int, EntityExistence)}.
	 */
	@Nonnull
	public EntityBodyStoragePart getEntityStorageContainer(int entityPrimaryKey, EntityExistence expects) {
		return entityStorageContainerAccessor.getEntityStorageContainer(entityPrimaryKey, expects);
	}

	/**
	 * Shorthand for {@link EntityStorageContainerAccessor#getAttributeStorageContainer(int)}.
	 */
	@Nonnull
	public AttributesStoragePart getAttributeStorageContainer(int entityPrimaryKey) {
		return entityStorageContainerAccessor.getAttributeStorageContainer(entityPrimaryKey);
	}

	/**
	 * Shorthand for {@link EntityStorageContainerAccessor#getAttributeStorageContainer(int, Locale)}.
	 */
	@Nonnull
	public AttributesStoragePart getAttributeStorageContainer(int entityPrimaryKey, @Nonnull Locale locale) {
		return entityStorageContainerAccessor.getAttributeStorageContainer(entityPrimaryKey, locale);
	}

	/**
	 * Shorthand for {@link EntityStorageContainerAccessor#getAssociatedDataStorageContainer(int, AssociatedDataKey)}.
	 */
	@Nonnull
	public AssociatedDataStoragePart getAssociatedDataStorageContainer(int entityPrimaryKey, @Nonnull AssociatedDataKey key) {
		return entityStorageContainerAccessor.getAssociatedDataStorageContainer(entityPrimaryKey, key);
	}

	/**
	 * Shorthand for {@link EntityStorageContainerAccessor#getReferencesStorageContainer(int)}.
	 */
	@Nonnull
	public ReferencesStoragePart getReferencesStorageContainer(int entityPrimaryKey) {
		return entityStorageContainerAccessor.getReferencesStorageContainer(entityPrimaryKey);
	}

	/**
	 * Shorthand for {@link EntityStorageContainerAccessor#getPriceStorageContainer(int)}.
	 */
	@Nonnull
	public PricesStoragePart getPriceStorageContainer(int entityPrimaryKey) {
		return entityStorageContainerAccessor.getPriceStorageContainer(entityPrimaryKey);
	}

	/**
	 * Method will prefetch all entities mentioned in `entitiesToPrefetch` and loads them with the scope of `requirements`.
	 * The entities will reveal only the scope to the `requirements` - no less, no more data.
	 */
	public void prefetchEntities(@Nonnull Bitmap entitiesToPrefetch, @Nonnull EntityContentRequire[] requirements) {
		this.prefetchedEntities = fetchEntities(entitiesToPrefetch.getArray(), requirements);
	}

	/*
		PRIVATE METHODS
	 */

	@Nonnull
	private EvitaRequest fabricateFetchRequest(@Nonnull Serializable entityType, @Nonnull EntityContentRequire... requireConstraints) {
		return new EvitaRequest(
			Query.query(
				entities(entityType),
				filterBy(
					and(
						ofNullable(evitaRequest.getLanguage()).map(QueryConstraints::language).orElse(null),
						ofNullable(evitaRequest.getRequiresCurrency()).map(QueryConstraints::priceInCurrency).orElse(null),
						of(evitaRequest.getRequiresPriceLists()).filter(it -> !ArrayUtils.isEmpty(it)).map(QueryConstraints::priceInPriceLists).orElse(null),
						ofNullable(evitaRequest.getRequiresPriceValidIn()).map(QueryConstraints::priceValidIn).orElse(null)
					)
				),
				require(requireConstraints)
			),
			evitaRequest.getAlignedNow()
		);
	}

}
