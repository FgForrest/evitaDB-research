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
import io.evitadb.api.configuration.CacheOptions;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.extraResult.CacheableEvitaResponseExtraResultComputer;
import io.evitadb.query.extraResult.EvitaResponseExtraResultComputer;
import io.evitadb.scheduling.Scheduler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Optional.ofNullable;

/**
 * This class contains the full logic documented in {@link CacheSupervisor}. It delegates its logic to two additional
 * classes:
 *
 * - {@link CacheAnteroom} the place for all costly formulas that haven't yet been placed in the cache
 * - {@link CacheEden} the place for all already cached formulas
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 * @see CacheSupervisor for more information
 */
public class HeapMemoryCacheSupervisor implements CacheSupervisor {
	private final CacheOptions cacheOptions;
	private final CacheAnteroom cacheAnteroom;
	private final CacheEden cacheEden;

	public HeapMemoryCacheSupervisor(@Nonnull CacheOptions cacheOptions, @Nonnull Scheduler scheduler) {
		this.cacheOptions = cacheOptions;
		this.cacheEden = new CacheEden(
			cacheOptions.getCacheSizeInBytes(),
			cacheOptions.getMinimalUsageThreshold(),
			cacheOptions.getMinimalComplexityThreshold()
		);
		this.cacheAnteroom = new CacheAnteroom(
			cacheOptions.getAnteroomRecordCount(),
			cacheOptions.getMinimalComplexityThreshold(),
			cacheEden, scheduler
		);
		// initialize function that will frequently evaluate contents of the cache, discard unused entries and introduce
		// new ones from the CacheAnteroom
		scheduler.scheduleAtFixedRate(
			this.cacheAnteroom::evaluateAssociatesSynchronously, 0,
			cacheOptions.getReevaluateCacheEachSeconds(),
			TimeUnit.SECONDS
		);
	}

	@Override
	public void checkFreeMemory() {
		final long cacheSizeInBytes;
		if (cacheOptions.getCacheSizeInBytes() == 0 && cacheOptions.getCacheSizeAsPercentageOfFreeMemory() > 0) {
			long allocatedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
			long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
			Assert.isTrue(
				cacheOptions.getCacheSizeAsPercentageOfFreeMemory() < 1.0f,
				() -> new IllegalArgumentException("Cannot exceed available memory!")
			);
			cacheSizeInBytes = (long) ((double) presumableFreeMemory * cacheOptions.getCacheSizeAsPercentageOfFreeMemory());
		} else if (cacheOptions.getCacheSizeInBytes() > 0) {
			cacheSizeInBytes = cacheOptions.getCacheSizeInBytes();
		} else {
			cacheSizeInBytes = 0;
		}
		System.out.println("Using " + StringUtils.formatByteSize(cacheSizeInBytes) + " for cache.");
		cacheEden.updateMaximalByteSize(cacheSizeInBytes);
	}

	@Nonnull
	@Override
	public <T extends Formula> T analyse(@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType, @Nonnull T filterFormula) {
		// we use cache only for Evita read only sessions, write session might already contain client specific modifications
		// that effectively exclude the formula caches from being used
		if (evitaSession.isReadOnly()) {
			//noinspection unchecked
			return (T) FormulaCacheVisitor.analyse(evitaSession, entityType, filterFormula, this.cacheAnteroom);
		} else {
			return filterFormula;
		}
	}

	@Nonnull
	@Override
	public <U, T extends CacheableEvitaResponseExtraResultComputer<U>> EvitaResponseExtraResultComputer<U> analyse(@Nonnull EvitaSession evitaSession, @Nonnull Serializable entityType, @Nonnull T extraResultComputer) {
		// we use cache only for Evita read only sessions, write session might already contain client specific modifications
		// that effectively exclude the formula caches from being used
		if (evitaSession.isReadOnly()) {
			//noinspection unchecked
			return (EvitaResponseExtraResultComputer<U>) this.cacheAnteroom.register(evitaSession, entityType, extraResultComputer);
		} else {
			return extraResultComputer;
		}
	}


	@Nullable
	@Override
	public SealedEntity analyse(@Nonnull EvitaSession evitaSession, int primaryKey, @Nonnull Serializable entityType, @Nonnull EntityContentRequire[] requirements, @Nonnull Supplier<SealedEntity> entityFetcher, @Nonnull UnaryOperator<SealedEntity> enricher, @Nonnull UnaryOperator<SealedEntity> sealer) {
		// we use cache only for Evita read only sessions, write session might already contain client specific modifications
		// that effectively exclude the formula caches from being used
		if (evitaSession.isReadOnly()) {
			return this.cacheAnteroom.register(evitaSession, primaryKey, entityType, requirements, entityFetcher, enricher, sealer);
		} else {
			return ofNullable(entityFetcher.get()).map(sealer).orElse(null);
		}
	}

}
