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

import com.carrotsearch.hppc.IntLongHashMap;
import io.evitadb.api.EvitaSession;
import io.evitadb.api.configuration.CacheOptions;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.cache.dto.CacheRecordAdept;
import io.evitadb.cache.dto.CachedRecord;
import io.evitadb.cache.payload.CachePayloadHeader;
import io.evitadb.cache.payload.EntityComputationalObjectAdapter;
import io.evitadb.index.array.CompositeLongArray;
import io.evitadb.query.algebra.CacheableFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.extraResult.CacheableEvitaResponseExtraResultComputer;
import io.evitadb.query.response.TransactionalDataRelatedStructure;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

/**
 * {@link CacheEden} represents the Evita cache core. It contains {@link #theCache} holding all cached formulas, tracks
 * formula usage and tracks their changes resulting in cache invalidation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@Slf4j
@ThreadSafe
public class CacheEden {
	/**
	 * Threshold that controls how many iterations {@link #evaluateAssociates()} may remain any of {@link CachedRecord}
	 * unused until it is evicted from the cache.
	 */
	public static final int COOL_ENOUGH = 3;
	/**
	 * Contains maximal size of payload that will be acceptable by this cache. Too large records will never make it
	 * to the cache.
	 */
	static final int MAX_BUFFER_SIZE = 1_048_576;
	/**
	 * Represents the core of this class - the cache.
	 */
	private final ConcurrentHashMap<Long, CachedRecord> theCache;
	/**
	 * Contains {@link CacheOptions#getMinimalUsageThreshold()} limit for this cache.
	 */
	private final int minimalUsageThreshold;
	/**
	 * Contains {@link CacheOptions#getMinimalComplexityThreshold()} limit for this cache.
	 */
	private final long minimalComplexityThreshold;
	/**
	 * Contains {@link CacheOptions#getCacheSizeInBytes()} limit for this cache.
	 */
	private final AtomicLong maximalByteSize = new AtomicLong();
	/**
	 * Represents counter of hits of this cache (i.e. how many times requested formula was found in cache).
	 */
	private final AtomicLong hits = new AtomicLong();
	/**
	 * Represents counter of misses of this cache (i.e. how many times requested formula was NOT found in cache).
	 */
	private final AtomicLong misses = new AtomicLong();
	/**
	 * Represents counter of entity enrichments of this cache.
	 */
	private final AtomicLong enrichments = new AtomicLong();
	/**
	 * Lock used to synchronize {@link #evaluateAssociates()} that is expected to be called in synchronized block.
	 */
	private final ReentrantLock lock = new ReentrantLock();
	/**
	 * Contains precise count of records stored in the cache.
	 */
	private final AtomicInteger cacheSize = new AtomicInteger();
	/**
	 * Contains estimate of current cache size in Bytes.
	 */
	private final AtomicLong usedByteSize = new AtomicLong();
	/**
	 * Contains reference to collection of adepts that were collected by {@link CacheAnteroom} between current and
	 * previous  {@link #evaluateAssociates()} method call.
	 */
	private final AtomicReference<Collection<CacheRecordAdept>> nextAdeptsToEvaluate = new AtomicReference<>();

	public CacheEden(long maximalByteSize, int minimalUsageThreshold, long minimalComplexityThreshold) {
		// let's assume single record will occupy 1KB
		this.theCache = new ConcurrentHashMap<>(Math.toIntExact(Math.min(maximalByteSize / 10_000L, Integer.MAX_VALUE)));
		this.maximalByteSize.set(maximalByteSize);
		this.minimalUsageThreshold = minimalUsageThreshold;
		this.minimalComplexityThreshold = minimalComplexityThreshold;
	}

	/**
	 * Redefines the cache size.
	 */
	public void updateMaximalByteSize(long maximalByteSize) {
		this.maximalByteSize.set(maximalByteSize);
	}

	/**
	 * Returns the {@link Formula} with memoized cached result for passed `computationalObject` and its `recordHash` providing
	 * that the cached formula has the same has of used transactional ids as the current computational object. There is small
	 * chance, that returned formula hasn't yet contain serialized computed result. When the formula enters the eden
	 * its empty and when used for the first time the result is memoized. In such case, original `computationalObject` is cloned
	 * to the form, that once result is computed updates its cached counterpart.
	 */
	@Nullable
	public <T extends TransactionalDataRelatedStructure, S> S getCachedRecord(
		@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType,
		@Nonnull T computationalObject, @Nonnull Class<S> expectedClass,
		long recordHash
	) {
		final CachedRecord cachedRecord = theCache.get(recordHash);
		final LongHashFunction hashFunction = CacheSupervisor.createHashFunction();
		if (cachedRecord != null) {
			if (cachedRecord.isInitialized()) {
				// check whether cached formula is valid for current transaction id
				if (cachedRecord.getTransactionalIdHash() == computationalObject.computeTransactionalIdHash(hashFunction)) {
					// track hit
					hits.incrementAndGet();
					if (computationalObject instanceof EntityComputationalObjectAdapter) {
						final EntityComputationalObjectAdapter entityWrapper = (EntityComputationalObjectAdapter) computationalObject;
						return enrichCachedEntityIfNecessary(recordHash, cachedRecord, entityWrapper);
					} else {
						// return payload
						return cachedRecord.getPayload(expectedClass);
					}
				} else {
					// track - miss, formula found but not valid for current input formula regarding used transactional data
					misses.incrementAndGet();
					return null;
				}
			} else {
				// formula found but not yet initialized
				misses.incrementAndGet();
				// set up initialization lambda to cloned input computational object
				if (computationalObject instanceof CacheableFormula) {
					final CacheableFormula inputFormula = (CacheableFormula) computationalObject;
					return alterToResultRecordingFormula(recordHash, cachedRecord, hashFunction, inputFormula);
				} else if (computationalObject instanceof CacheableEvitaResponseExtraResultComputer<?>) {
					final CacheableEvitaResponseExtraResultComputer<?> inputComputer = (CacheableEvitaResponseExtraResultComputer<?>) computationalObject;
					return alterToResultRecordingComputer(recordHash, cachedRecord, hashFunction, inputComputer);
				} else if (computationalObject instanceof EntityComputationalObjectAdapter) {
					final EntityComputationalObjectAdapter entityWrapper = (EntityComputationalObjectAdapter) computationalObject;
					return fetchAndCacheEntity(recordHash, cachedRecord, hashFunction, entityWrapper);
				} else {
					throw new IllegalStateException("Unexpected object in cache `" + computationalObject.getClass() + "`!");
				}
			}
		}
		// formula not found record miss
		misses.incrementAndGet();
		return null;
	}

	/**
	 * Returns estimate of current cache size in Bytes.
	 */
	public long getByteSizeUsedByCache() {
		return usedByteSize.get();
	}

	/**
	 * Returns precise count of records stored in the cache.
	 */
	public int getCacheRecordCount() {
		return cacheSize.get();
	}

	/**
	 * Stores collection of {@link CacheRecordAdept} that are required to be evaluated by {@link #evaluateAssociates()}.
	 * This method can be actually called multiple times within single {@link #evaluateAssociates()} interval, if there
	 * is pressure on the evitaDB. In such case intermediate calls are ignored and their contents are garbage collected
	 * without even being analyzed and only last call will be processed.
	 */
	public void setNextAdeptsToEvaluate(@Nonnull Collection<CacheRecordAdept> adepts) {
		final Collection<CacheRecordAdept> alreadyWaitingAdepts = this.nextAdeptsToEvaluate.getAndSet(adepts);
		if (alreadyWaitingAdepts != null) {
			// log excessive pressure on the cache
			log.warn("Evita cache refresh doesn't keep up with cache adepts ingress!");
		}
	}

	/**
	 * The second most important method of this class beside {@link #getCachedRecord(EvitaSession, Serializable, TransactionalDataRelatedStructure, Class, long)}.
	 * This method evaluates all {@link CacheRecordAdept} recorded by {@link #setNextAdeptsToEvaluate(Collection)}
	 * along with currently held {@link CachedRecord} records. It sorts them all by their value (performance ratio)
	 * and registers the most precious of them into the cache. It also evicts currently cached records if they become
	 * cool enough.
	 */
	public void evaluateAssociates() {
		try {
			// this method is allowed to run in one thread only
			if (lock.tryLock() || lock.tryLock(1, TimeUnit.SECONDS)) {
				try {
					final long maximalAllowedMemorySize = maximalByteSize.get();
					// retrieve adepts to evaluate
					final Collection<CacheRecordAdept> adepts = nextAdeptsToEvaluate.getAndSet(null);
					if (adepts != null) {
						// copy all adepts into a new array that will be sorted
						final EvaluationCacheFormulaAdeptSource evaluationSource = mergeAdeptsWithExistingEntriesForEvaluation(adepts);
						final CacheRecordAdept[] evaluation = evaluationSource.getEvaluation();
						// now sort all entries by: initialized first, cooling attribute second and then space to performance ration
						Arrays.sort(evaluation, 0, evaluationSource.getPeek(), new CacheRecordAdeptComparator(minimalUsageThreshold, evaluation.length));
						// now find the delimiter for entries that will be newly accepted to cache
						int threshold = -1; // index of the last entry that will be accepted to the new cache
						long occupiedMemorySize = 0; // contains memory size in bytes going to be occupied by flattened formula entries
						for (int i = 0; i < evaluationSource.getPeek(); i++) {
							final CacheRecordAdept adept = evaluation[i];
							final int adeptSizeInBytes = CachedRecord.computeSizeInBytes(adept);
							// increase counters for bitmaps and occupied memory size
							occupiedMemorySize += adeptSizeInBytes;
							// if the expected memory consumption is greater than allowed
							if (occupiedMemorySize > maximalAllowedMemorySize) {
								// stop iterating - we found our threshold, last counter increase must be reverted
								// currently examined item will not be part of the cache
								occupiedMemorySize -= adeptSizeInBytes;
								break;
							}
							threshold = i;
						}

						// we need first to free the memory to avoid peek
						// evict all cold cached formulas
						final OfLong expiredItemsIt = evaluationSource.getExpiredItems().iterator();
						while (expiredItemsIt.hasNext()) {
							final long expiredFormulaHash = expiredItemsIt.next();
							theCache.remove(expiredFormulaHash);
						}
						// evict all cached formulas after the found threshold
						for (int i = threshold + 1; i < evaluationSource.getPeek(); i++) {
							final CacheRecordAdept adept = evaluation[i];
							if (adept instanceof CachedRecord) {
								theCache.remove(adept.getRecordHash());
							}
						}

						// cache all non-cached formulas before the found threshold
						// now we can allocate new memory
						for (int i = 0; i <= threshold; i++) {
							final CacheRecordAdept adept = evaluation[i];
							// cache all adepts
							if (!(adept instanceof CachedRecord)) {
								// init the cached formula, final reference will be initialized with first additional request
								theCache.put(adept.getRecordHash(), adept.toCachedRecord());
							}
						}

						// TOBEDONE JNO - replace this with metrics
						System.out.println(
							"Cache re-evaluation: " +
								"count " + theCache.size() +
								", size " + StringUtils.formatByteSize(occupiedMemorySize) +
								", hits " + this.hits.get() +
								", misses " + this.misses.get() +
								", enrichments " + this.enrichments.get() +
								", ratio " + ((float) this.hits.get() / (float) (this.misses.get() + this.hits.get())) * 100f + "%" +
								", average complexity: " + ((float) Arrays.stream(evaluation).limit(evaluationSource.getPeek()).mapToLong(it -> it.getSpaceToPerformanceRatio(minimalUsageThreshold)).sum() / (float) evaluationSource.getPeek()) +
								", adept count: " + evaluationSource.getPeek()

						);

						// finally, set occupied memory size according to expectations
						this.usedByteSize.set(occupiedMemorySize);
						this.cacheSize.set(theCache.size());
						this.hits.set(0);
						this.misses.set(0);
						this.enrichments.set(0);
					}
				} finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			log.warn("Failed to acquire lock for cache re-evaluation!");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Combines collection of {@link CacheRecordAdept} with entire contents of {@link #theCache} - i.e. already
	 * {@link CachedRecord} into the single object for price evaluation. During the process the {@link CachedRecord}
	 * that hasn't been used for a long time (has cooled off) are marked for discarding.
	 */
	@Nonnull
	private EvaluationCacheFormulaAdeptSource mergeAdeptsWithExistingEntriesForEvaluation(@Nonnull Collection<CacheRecordAdept> adepts) {
		final CacheRecordAdept[] evaluation = new CacheRecordAdept[adepts.size() + cacheSize.get()];
		final CompositeLongArray expiredItems = new CompositeLongArray();
		int index = 0;
		// first fill in all waiting adepts
		final Iterator<CacheRecordAdept> adeptIt = adepts.iterator();
		while (adeptIt.hasNext() && index < evaluation.length) {
			final CacheRecordAdept adept = adeptIt.next();
			if (CachedRecord.computeSizeInBytes(adept) < MAX_BUFFER_SIZE && adept.getCost() >= minimalComplexityThreshold) {
				evaluation[index++] = adept;
			}
		}
		// next fill in all existing entries in cache - we need re-evaluate even them
		final Iterator<CachedRecord> cacheIt = theCache.values().iterator();
		while (cacheIt.hasNext() && index < evaluation.length) {
			final CachedRecord cachedFormula = cacheIt.next();
			if (cachedFormula.reset() <= COOL_ENOUGH) {
				evaluation[index++] = cachedFormula;
			} else {
				expiredItems.add(cachedFormula.getRecordHash());
			}
		}
		return new EvaluationCacheFormulaAdeptSource(evaluation, index, expiredItems);
	}

	/**
	 * Method will replace the `inputFormula` with a clone that allows capturing the computational result and store it
	 * to the eden cache for future requests.
	 */
	@Nonnull
	private <S> S alterToResultRecordingFormula(long recordHash, @Nonnull CachedRecord cachedRecord, @Nonnull LongHashFunction hashFunction, @Nonnull CacheableFormula inputFormula) {
		// otherwise, clone input formula and add logic, that will store the computed result to the cache
		//noinspection unchecked
		return (S) inputFormula.getCloneWithComputationCallback(
			cacheableFormula -> {
				final CachePayloadHeader payload = inputFormula.toSerializableFormula(recordHash, hashFunction);
				theCache.put(
					recordHash,
					new CachedRecord(
						cachedRecord.getRecordHash(),
						cachedRecord.getCost(),
						cachedRecord.getCostToPerformanceRatio(),
						cachedRecord.getTimesUsed(),
						cachedRecord.getSizeInBytes(),
						inputFormula.computeTransactionalIdHash(hashFunction),
						payload
					)
				);
			},
			inputFormula.getInnerFormulas()
		);
	}

	/**
	 * Method will replace the `inputComputer` with a clone that allows capturing the computational result and store it
	 * to the eden cache for future requests.
	 */
	@Nonnull
	private <S> S alterToResultRecordingComputer(long recordHash, @Nonnull CachedRecord cachedRecord, @Nonnull LongHashFunction hashFunction, @Nonnull CacheableEvitaResponseExtraResultComputer<?> inputComputer) {
		// otherwise, clone input formula and add logic, that will store the computed result to the cache
		//noinspection unchecked
		return (S) inputComputer.getCloneWithComputationCallback(
			cacheableFormula -> {
				final CachePayloadHeader payload = inputComputer.toSerializableResult(recordHash, hashFunction);
				theCache.put(
					recordHash,
					new CachedRecord(
						cachedRecord.getRecordHash(),
						cachedRecord.getCost(),
						cachedRecord.getCostToPerformanceRatio(),
						cachedRecord.getTimesUsed(),
						cachedRecord.getSizeInBytes(),
						inputComputer.computeTransactionalIdHash(hashFunction),
						payload
					)
				);
			}
		);
	}

	/**
	 * Method will fetch entity from the datastore and stores it to the eden cache for future requests.
	 */
	@Nullable
	private <S> S fetchAndCacheEntity(long recordHash, @Nonnull CachedRecord cachedRecord, @Nonnull LongHashFunction hashFunction, @Nonnull EntityComputationalObjectAdapter entityWrapper) {
		final SealedEntity payload = entityWrapper.fetchEntity();
		if (payload != null && payload.exists()) {
			theCache.put(
				recordHash,
				new CachedRecord(
					cachedRecord.getRecordHash(),
					cachedRecord.getCost(),
					cachedRecord.getCostToPerformanceRatio(),
					cachedRecord.getTimesUsed(),
					cachedRecord.getSizeInBytes(),
					entityWrapper.computeTransactionalIdHash(hashFunction),
					payload
				)
			);
		}
		//noinspection unchecked
		return (S) payload;
	}

	/**
	 * Method will check whether the cached entity is rich enough to satisfy the input query and if not, the entity is
	 * lazily enriched of additional data and the cached object is replaced with this richer entity for future use.
	 */
	private <S> S enrichCachedEntityIfNecessary(long recordHash, @Nonnull CachedRecord cachedRecord, @Nonnull EntityComputationalObjectAdapter entityWrapper) {
		final SealedEntity cachedEntity = cachedRecord.getPayload(SealedEntity.class);
		final SealedEntity enrichedEntity = entityWrapper.enrichEntity(cachedEntity);
		if (enrichedEntity != cachedEntity) {
			theCache.put(
				recordHash,
				new CachedRecord(
					cachedRecord.getRecordHash(),
					cachedRecord.getCost(),
					cachedRecord.getCostToPerformanceRatio(),
					cachedRecord.getTimesUsed(),
					cachedRecord.getSizeInBytes(),
					cachedRecord.getTransactionalIdHash(),
					enrichedEntity
				)
			);
			enrichments.incrementAndGet();
			//noinspection unchecked
			return (S) enrichedEntity;
		} else {
			//noinspection unchecked
			return (S) cachedEntity;
		}
	}

	/**
	 * DTO that collects all adepts for price evaluation and storing into the cache along with identification of those
	 * that are marked for eviction.
	 */
	@Data
	private static class EvaluationCacheFormulaAdeptSource {
		private final CacheRecordAdept[] evaluation;
		private final int peek;
		private final CompositeLongArray expiredItems;
	}

	/**
	 * Comparator that memoizes previously computed space to performance ratio results. If the results would not be
	 * memoized it may change during sort and cause contract violation for Java array sort.
	 */
	@SuppressWarnings("ComparatorNotSerializable")
	private static class CacheRecordAdeptComparator implements Comparator<CacheRecordAdept> {
		private final int minimalUsageThreshold;
		private final IntLongHashMap memoizationCache;

		public CacheRecordAdeptComparator(int minimalUsageThreshold, int adeptCount) {
			this.minimalUsageThreshold = minimalUsageThreshold;
			this.memoizationCache = new IntLongHashMap(adeptCount);
		}

		@Override
		public int compare(CacheRecordAdept o1, CacheRecordAdept o2) {
			final long spaceToPerformanceRatio2 = computeIfAbsent(memoizationCache, System.identityHashCode(o2), () -> o2.getSpaceToPerformanceRatio(minimalUsageThreshold));
			final long spaceToPerformanceRatio1 = computeIfAbsent(memoizationCache, System.identityHashCode(o1), () -> o1.getSpaceToPerformanceRatio(minimalUsageThreshold));
			return Long.compare(spaceToPerformanceRatio2, spaceToPerformanceRatio1);
		}

		private long computeIfAbsent(IntLongHashMap memoizedValues, int identityHashCode, LongSupplier ratioComputer) {
			final long value = memoizedValues.getOrDefault(identityHashCode, -1L);
			if (value >= 0) {
				return value;
			} else {
				final long performance = ratioComputer.getAsLong();
				memoizedValues.put(identityHashCode, performance);
				return performance;
			}
		}
	}
}
