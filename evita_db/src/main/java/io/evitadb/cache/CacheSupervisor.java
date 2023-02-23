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
import io.evitadb.cache.dto.CacheRecordAdept;
import io.evitadb.cache.dto.CachedRecord;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.extraResult.CacheableEvitaResponseExtraResultComputer;
import io.evitadb.query.extraResult.EvitaResponseExtraResultComputer;
import io.evitadb.query.response.TransactionalDataRelatedStructure;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Formula supervisor is an entry point to the Evita cache. The idea is that each {@link Formula} can be identified by
 * its {@link Formula#computeHash(LongHashFunction)} method and when the supervisor identifies that certain formula is frequently used
 * in query formulas it moves its memoized results to the cache. The non-computed formula of the same hash will be
 * exchanged in next query that contains it with the cached formula that already contains memoized result.
 *
 * The key aspect of each caching mechanism is invalidation when the cached data become obsolete. We keep all
 * {@link Formula#gatherTransactionalIds() transactional bitmap ids} the formula builds upon along with the cached
 * formula. When the underlying datastore is changed we can track which bitmaps were discarded and mark all cached
 * formulas that refer to any of those discarded bitmaps as obsolete. We update the cached formula with the last
 * transaction id they're valid to (we keep also the transaction id when the formula result was memoized) when they
 * link to the discarded bitmap.
 *
 * If we found formula in cache we compare its transaction id validity span (from-to) with the transaction id of the
 * {@link EvitaSession} the query is executed in. The cached formula result is used only when the transaction id is
 * within the memoized formula validity span. By this mechanism we may still keep obsolete results in a cache for a while
 * providing there are old Evita sessions that heavily work with these. If not the formulas will quickly fade away
 * through "cooling" mechanism and get evicted from the cache.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface CacheSupervisor {

	/**
	 * Central accessor for creating a hash function that returns fast hashing function producing low collision hashes
	 * for passed input. We use <a href="https://github.com/OpenHFT/Zero-Allocation-Hashing">zero-allocation hashing</a>
	 * library and <a href="https://github.com/Cyan4973/xxHash">xxHash</a> function that is believed to fulfill these
	 * requirements.
	 */
	@Nonnull
	static LongHashFunction createHashFunction() {
		return LongHashFunction.xx3();
	}

	/**
	 * Method traverses {@link Formula} tree and for each "expensive" formula it computes hash and checks whether
	 * the formula has cached counterpart. If so the formula is exchanged with the {@link CachedRecord} that
	 * has the result already memoized. The cost (expensiveness) of the formula are based on
	 * {@link TransactionalDataRelatedStructure#getEstimatedCost()} that is the only way how to guess the cost without
	 * really computing the result.
	 */
	@Nonnull
	<T extends Formula> T analyse(
		@Nonnull EvitaSession evitaSession,
		@Nonnull Serializable entityType,
		@Nonnull T filterFormula
	);

	/**
	 * Method examines whether `extraResultComputer` is "expensive" enough and when it is, it computes hash and checks
	 * whether the extraResultComputer has cached counterpart. If so the computer is exchanged with
	 * the {@link CachedRecord} that has the result already memoized. The cost (expensiveness) of the computer are based
	 * on {@link TransactionalDataRelatedStructure#getEstimatedCost()} that is the only way how to guess the cost
	 * without really computing the result.
	 */
	@Nonnull
	<U, T extends CacheableEvitaResponseExtraResultComputer<U>> EvitaResponseExtraResultComputer<U> analyse(
		@Nonnull EvitaSession evitaSession,
		@Nonnull Serializable entityType,
		@Nonnull T extraResultComputer
	);

	/**
	 * Method tries to find particular entity `primaryKey` and `requirements` in the cache so that we can avoid physical
	 * fetch from the persistent datastore.
	 *
	 * If the entity is not found in the cache, it is fetched via `entityFetcher` function and {@link CacheRecordAdept}
	 * is created for it. If there are enough requests targeting this entity, the entity will eventually propagate to
	 * the cache.
	 *
	 * If the entity is found in the cache, it is checked whether it's rich enough via `enricher` function, and then
	 * it's narrowed via `sealer` function (the cached entity may be richer than requested) and returned.
	 */
	@Nullable
	SealedEntity analyse(
		@Nonnull EvitaSession evitaSession,
		int primaryKey,
		@Nonnull Serializable entityType,
		@Nonnull EntityContentRequire[] requirements,
		@Nonnull Supplier<SealedEntity> entityFetcher,
		@Nonnull UnaryOperator<SealedEntity> enricher,
		@Nonnull UnaryOperator<SealedEntity> sealer
	);

	/**
	 * Temporary function for auto-adjusting memory for performance testing.
	 */
	void checkFreeMemory();

}