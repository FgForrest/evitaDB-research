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

package io.evitadb.api.configuration;

import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.cache.CacheAnteroom;
import io.evitadb.cache.CacheEden;
import io.evitadb.query.algebra.Formula;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class is simple DTO object holding all cache-related configuration options of the Evita.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CacheOptions {
	/**
	 * Contains mode for accessing reflection data - CACHE mode is mostly recommended, unless you're running
	 * some kind of tests.
	 */
	private ReflectionCachingBehaviour reflection = ReflectionCachingBehaviour.CACHE;
	/**
	 * Enables global-wide caching of Evita query results. If caching is enabled, costly {@link Formula} computations
	 * may be cached in {@link CacheEden} and next time they are found in the query computation tree immediately
	 * replaced with the already memoized computation result.
	 */
	private boolean enableFormulaCache;

	/*
		THESE SETTINGS ARE VALID ONLY WHEN CACHE IS ENABLED
	 */

	/**
	 * Contains interval in second the {@link CacheAnteroom} is
	 * {@link CacheAnteroom#evaluateAssociatesAsynchronously() re-evaluated} and its contents are either purged or
	 * moved to {@link CacheEden}
	 */
	private int reevaluateCacheEachSeconds = 60;
	/**
	 * Contains limit of maximal entries held in {@link CacheAnteroom}. When this limit is exceeded the anteroom needs
	 * to be re-evaluated and the adepts either cleared or moved to {@link CacheEden}. In other words it's the size
	 * of the {@link CacheAnteroom} buffer.
	 */
	private int anteroomRecordCount;
	/**
	 * Contains minimal threshold of the {@link Formula#getEstimatedCost()} that formula needs to exceed in order to
	 * become a cache adept, that may be potentially moved to {@link CacheEden}.
	 */
	private long minimalComplexityThreshold;
	/**
	 * Contains minimal threshold the {@link Formula} must be encountered in {@link CacheAnteroom} purging period
	 * in order it could be considered to be moved to {@link CacheEden}. This allows us to avoid expensive "unicorn"
	 * formulas (that occurs once a long while) to get cached. Each formula needs to earn the placement in cache.
	 */
	private int minimalUsageThreshold;
	/**
	 * Contains memory limit for the cache in Bytes. Java is very dynamic in object memory sizes, so we only
	 * try to estimate the size of cached data in order to control the cache size within the defined limit.
	 */
	private long cacheSizeInBytes;
	/**
	 * Contains memory limit defined as share of free memory after Evita has started up and loaded its data.
	 */
	private float cacheSizeAsPercentageOfFreeMemory;

}
