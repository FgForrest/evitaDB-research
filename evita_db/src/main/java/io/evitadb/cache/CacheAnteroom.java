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

package io.evitadb.cache;

import io.evitadb.api.EvitaSession;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.utils.CollectionUtils;
import io.evitadb.cache.dto.CacheRecordAdept;
import io.evitadb.cache.payload.EntityComputationalObjectAdapter;
import io.evitadb.query.algebra.CacheableFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.price.CacheablePriceFormula;
import io.evitadb.query.algebra.price.termination.PriceTerminationFormula;
import io.evitadb.query.extraResult.CacheableEvitaResponseExtraResultComputer;
import io.evitadb.query.extraResult.EvitaResponseExtraResultComputer;
import io.evitadb.query.response.TransactionalDataRelatedStructure;
import io.evitadb.scheduling.Scheduler;
import lombok.extern.slf4j.Slf4j;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Cache anteroom represents a stage before the formulas enter the cache. It collects information about formula usage
 * and their costs and computes the "ROI" value of them. Only the most valuable formulas will make it to the
 * {@link CacheEden} and consume the precious memory for the sake of better system performance. If you see any analogies
 * with the Bible, you're right :).
 *
 * The key entry-points of this class are:
 *
 * - {@link #register(EvitaSession, Serializable, Formula, FormulaCacheVisitor)}
 * - {@link #register(EvitaSession, Serializable, CacheableEvitaResponseExtraResultComputer)}
 * - {@link #evaluateAssociates(boolean)}
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@Slf4j
@ThreadSafe
public class CacheAnteroom {
	/**
	 * Contains limit of maximal entries held in CacheAnteroom. When this limit is exceeded the anteroom needs to be
	 * re-evaluated and the adepts either cleared or moved to {@link CacheEden}. In other words it's the size of the
	 * CacheAnteroom buffer.
	 */
	private final int maxRecordCount;
	/**
	 * Contains minimal threshold of the {@link Formula#getEstimatedCost()} that formula needs to exceed in order to
	 * become a cache adept, that may be potentially moved to {@link CacheEden}.
	 */
	private final long minimalComplexityThreshold;
	/**
	 * Contains reference to the real cache.
	 */
	private final CacheEden cacheEden;
	/**
	 * Contains reference to the asynchronous task executor.
	 */
	private final Scheduler scheduler;
	/**
	 * Contains a hash map that collects adepts for the caching. In other terms the expensive data structures that were
	 * recently computed and might be worth caching. The map is cleared each time {@link #evaluateAssociates(boolean)}
	 * is executed.
	 */
	private final AtomicReference<ConcurrentHashMap<Long, CacheRecordAdept>> cacheAdepts;

	public CacheAnteroom(int maxRecordCount, long minimalComplexityThreshold, @Nonnull CacheEden cacheEden, @Nonnull Scheduler scheduler) {
		this.cacheAdepts = new AtomicReference<>(CollectionUtils.createConcurrentHashMap((int) (maxRecordCount * 1.1)));
		this.cacheEden = cacheEden;
		this.maxRecordCount = maxRecordCount;
		this.minimalComplexityThreshold = minimalComplexityThreshold;
		this.scheduler = scheduler;
	}

	/**
	 * Hands off {@link #cacheAdepts} via. {@link CacheEden#setNextAdeptsToEvaluate(Collection)} for evaluation. It also
	 * triggers immediate evaluation of those adepts in this thread context.
	 *
	 * Beware the evaluation might take a while - it's better considering asynchronous variant of this method
	 * ({@link #evaluateAssociatesAsynchronously()}) that is fast.
	 */
	public void evaluateAssociatesSynchronously() {
		evaluateAssociates(true);
	}

	/**
	 * Hands off {@link #cacheAdepts} via. {@link CacheEden#setNextAdeptsToEvaluate(Collection)} for evaluation. It also
	 * triggers evaluation of those adepts in different thread using {@link #scheduler}. The evaluation will start almost
	 * immediately if there is any thread available in the executor pool.
	 */
	public void evaluateAssociatesAsynchronously() {
		evaluateAssociates(false);
	}

	/**
	 * Method examines `formula`, whether the formula computation seems expensive and whether formula claims that it
	 * supports caching of its results. If so, it's checked for existing cached result in {@link CacheEden} and return
	 * it immediately. If no cached result is found it registers the formula to {@link #cacheAdepts} and returns a clone
	 * that records the detailed information on first formula computation.
	 *
	 * Special treatment is provided for formulas within {@link PriceTerminationFormula} - these formulas even if
	 * cacheable and expensive may hold so large data that are excluded from caching.
	 */
	@Nonnull
	public Formula register(@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType, @Nonnull Formula formula, @Nonnull FormulaCacheVisitor formulaVisitor) {
		if (formula instanceof CacheableFormula &&
			(formula instanceof CacheablePriceFormula || !formulaVisitor.isWithin(PriceTerminationFormula.class))) {
			final CacheableFormula inputFormula = (CacheableFormula) formula;
			if (formula.getEstimatedCost() >= minimalComplexityThreshold) {
				final long formulaHash = computeDataStructureHash(entityType, inputFormula);
				final Formula cachedFormula = cacheEden.getCachedRecord(evitaSession, entityType, inputFormula, Formula.class, formulaHash);
				return cachedFormula != null ?
					cachedFormula :
					recordUsageAndReturnInstrumentedCopyIfNotYetSeen(formulaVisitor, inputFormula, formulaHash);
			} else {
				return formula;
			}
		} else {
			return formula;
		}
	}

	/**
	 * Method examines `computer`, whether the computation seems expensive and whether computer claims that it
	 * supports caching of its results. If so, it's checked for existing cached result in {@link CacheEden} and return
	 * it immediately. If no cached result is found it registers the formula to {@link #cacheAdepts} and returns a clone
	 * that records the detailed information on first computation.
	 */
	@Nonnull
	public <U> EvitaResponseExtraResultComputer<?> register(@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType, @Nonnull CacheableEvitaResponseExtraResultComputer<U> computer) {
		if (computer.getEstimatedCost() > minimalComplexityThreshold) {
			final long recordHash = computeDataStructureHash(entityType, computer);
			final EvitaResponseExtraResultComputer<?> cachedResult = cacheEden.getCachedRecord(
				evitaSession, entityType, computer, EvitaResponseExtraResultComputer.class, recordHash
			);
			return cachedResult == null ?
				recordUsageAndReturnInstrumentedCopyIfNotYetSeen(computer, recordHash) : cachedResult;
		} else {
			return computer;
		}
	}

	/**
	 * Method checks whether the entity wit `entityPrimaryKey` is present in {@link CacheEden} and if so it returns
	 * it immediately applying the `sealer` function that might hide the data that were not requested but are loaded
	 * for the entity object in cache. If no cached result is found it fetches the entity from the persistent data store
	 * and registers the entity request to {@link #cacheAdepts} along with the information about its size. The cache
	 * adept information is then used to evaluate the worthiness of keeping this entity in cache.
	 */
	@Nullable
	public SealedEntity register(@Nonnull EvitaSession evitaSession, int entityPrimaryKey, @Nonnull Serializable entityType, @Nonnull EntityContentRequire[] requirements, @Nonnull Supplier<SealedEntity> entityFetcher, @Nonnull UnaryOperator<SealedEntity> enricher, @Nonnull UnaryOperator<SealedEntity> sealer) {
		final LongHashFunction hashFunction = CacheSupervisor.createHashFunction();
		final long recordHash = hashFunction.hashLongs(
			new long[]{
				entityPrimaryKey,
				hashFunction.hashChars(entityType.toString())
			}
		);
		final EntityComputationalObjectAdapter entityWrapper = new EntityComputationalObjectAdapter(
			entityPrimaryKey, entityFetcher, enricher, requirements.length, minimalComplexityThreshold
		);
		final SealedEntity cachedResult = cacheEden.getCachedRecord(
			evitaSession, entityType,
			entityWrapper,
			SealedEntity.class, recordHash
		);
		if (cachedResult == null) {
			final SealedEntity entity = entityFetcher.get();
			final AtomicBoolean enlarged = new AtomicBoolean(false);
			final ConcurrentHashMap<Long, CacheRecordAdept> currentCacheAdepts = this.cacheAdepts.get();
			final CacheRecordAdept cacheRecordAdept = currentCacheAdepts.computeIfAbsent(
				recordHash, fHash -> {
					enlarged.set(true);
					return new CacheRecordAdept(
						fHash,
						entityWrapper.getCost(),
						entityWrapper.getCostToPerformanceRatio(),
						1,
						entity.estimateSize()
					);
				}
			);
			if (enlarged.get() && currentCacheAdepts.size() > maxRecordCount) {
				CacheAnteroom.this.evaluateAssociatesAsynchronously();
			}
			cacheRecordAdept.used();
			return entity == null ? null : sealer.apply(entity);
		} else {
			return sealer.apply(cachedResult);
		}
	}

	/**
	 * Method returns {@link CacheRecordAdept} for passed `dataStructure`.
	 * The key is the {@link TransactionalDataRelatedStructure#computeHash(LongHashFunction)}.
	 */
	@Nullable
	CacheRecordAdept getCacheAdept(@Nonnull Serializable entityType, @Nonnull TransactionalDataRelatedStructure dataStructure) {
		return cacheAdepts.get().get(computeDataStructureHash(entityType, dataStructure));
	}

	/**
	 * Method computes long hash for passed `dataStructure`.
	 */
	long computeDataStructureHash(@Nonnull Serializable entityType, @Nonnull TransactionalDataRelatedStructure dataStructure) {
		final LongHashFunction hashFunction = CacheSupervisor.createHashFunction();
		return hashFunction.hashLongs(
			new long[]{
				hashFunction.hashChars(entityType.toString()),
				dataStructure.computeHash(hashFunction)
			}
		);
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * Hands off {@link #cacheAdepts} via. {@link CacheEden#setNextAdeptsToEvaluate(Collection)} for evaluation. It also
	 * triggers evaluation of those adepts.
	 */
	private void evaluateAssociates(boolean synchronously) {
		try {
			final ConcurrentHashMap<Long, CacheRecordAdept> currentCacheAdepts = this.cacheAdepts.get();
			if (!currentCacheAdepts.isEmpty()) {
				// create new cache adepts map and move the old one to the cache eden for evaluation
				final ConcurrentHashMap<Long, CacheRecordAdept> adeptsToEvaluate = this.cacheAdepts.getAndSet(
					CollectionUtils.createConcurrentHashMap(currentCacheAdepts.size())
				);
				this.cacheEden.setNextAdeptsToEvaluate(adeptsToEvaluate.values());
			} else {
				// we need to trigger evaluation even if current adepts are empty
				// in order to trigger cooling of currently cached records
				this.cacheEden.setNextAdeptsToEvaluate(Collections.emptyList());
			}
			// evaluate either in this thread or via thread executor
			if (synchronously) {
				this.cacheEden.evaluateAssociates();
			} else {
				scheduler.execute(this.cacheEden::evaluateAssociates);
			}
		} catch (RuntimeException e) {
			// we don't rethrow - it would stop engine, just log error
			log.error("Failed to evaluate cache associates: " + e.getMessage(), e);
		}
	}

	/**
	 * Method will check whether the `inputFormula` is already registered in {@link #cacheAdepts} and if so, it's immediately
	 * returned. If not - new clone is created and {@link #recordDataOnComputationCompletion(long, CacheRecordAdept, int, long, long)}
	 * is introduced to it so that critical information are filled in on first result computation.
	 *
	 * If new cache adept is created it's checked whether the number of adepts exceeds {@link #maxRecordCount} and if
	 * so, the {@link #evaluateAssociatesAsynchronously()} process is executed.
	 */
	@Nonnull
	private Formula recordUsageAndReturnInstrumentedCopyIfNotYetSeen(@Nonnull FormulaCacheVisitor formulaVisitor, @Nonnull CacheableFormula inputFormula, long formulaHash) {
		final AtomicBoolean enlarged = new AtomicBoolean(false);
		final ConcurrentHashMap<Long, CacheRecordAdept> currentCacheAdepts = this.cacheAdepts.get();
		final CacheRecordAdept cacheFormulaAdept = currentCacheAdepts.computeIfAbsent(
			formulaHash, fHash -> {
				enlarged.set(true);
				return new CacheRecordAdept(fHash);
			}
		);
		if (enlarged.get() && currentCacheAdepts.size() > maxRecordCount) {
			CacheAnteroom.this.evaluateAssociatesAsynchronously();
		}
		cacheFormulaAdept.used();
		if (cacheFormulaAdept.isInitialized()) {
			return inputFormula;
		} else {
			return inputFormula.getCloneWithComputationCallback(
				self -> recordDataOnComputationCompletion(
					formulaHash, cacheFormulaAdept,
					self.getSerializableFormulaSizeEstimate(),
					self.getCost(),
					self.getCostToPerformanceRatio()
				),
				formulaVisitor.analyseChildren(inputFormula)
			);
		}
	}

	/**
	 * Method will check whether the `extraResult` is already registered in {@link #cacheAdepts} and if so, it's immediately
	 * returned. If not - new clone is created and {@link #recordDataOnComputationCompletion(long, CacheRecordAdept, int, long, long)}
	 * is introduced to it so that critical information are filled in on first result computation.
	 *
	 * If new cache adept is created it's checked whether the number of adepts exceeds {@link #maxRecordCount} and if
	 * so, the {@link #evaluateAssociatesAsynchronously()} process is executed.
	 */
	@Nonnull
	private CacheableEvitaResponseExtraResultComputer<?> recordUsageAndReturnInstrumentedCopyIfNotYetSeen(@Nonnull CacheableEvitaResponseExtraResultComputer<?> extraResult, long extraResultHash) {
		final AtomicBoolean enlarged = new AtomicBoolean(false);
		final ConcurrentHashMap<Long, CacheRecordAdept> currentCacheAdepts = this.cacheAdepts.get();
		final CacheRecordAdept cacheRecordAdept = currentCacheAdepts.computeIfAbsent(
			extraResultHash, fHash -> {
				enlarged.set(true);
				return new CacheRecordAdept(fHash);
			}
		);
		if (enlarged.get() && currentCacheAdepts.size() > maxRecordCount) {
			CacheAnteroom.this.evaluateAssociatesAsynchronously();
		}
		cacheRecordAdept.used();
		if (cacheRecordAdept.isInitialized()) {
			return extraResult;
		} else {
			return extraResult.getCloneWithComputationCallback(
				self -> recordDataOnComputationCompletion(
					extraResultHash, cacheRecordAdept,
					self.getSerializableResultSizeEstimate(),
					self.getCost(),
					self.getCostToPerformanceRatio()
				)
			);
		}
	}

	/**
	 * This method is used as computational callback for formulas and extra result computers. When the computation result
	 * is available it computes the expected size of the cached record (without really caching it) and computes
	 * the cost to performance ration - i.e. how much memory we'll pay for how big performance gain. This number is
	 * the key aspect for deciding whether this data structure is worth caching.
	 */
	private void recordDataOnComputationCompletion(long formulaHash, @Nonnull CacheRecordAdept cacheFormulaAdept, int estimatedMemorySize, long cost, long costToPerformanceRatio) {
		this.cacheAdepts.get().put(
			formulaHash,
			new CacheRecordAdept(
				formulaHash,
				cost,
				costToPerformanceRatio,
				cacheFormulaAdept.getTimesUsed(),
				CacheRecordAdept.estimateSize(estimatedMemorySize)
			)
		);
	}

}
