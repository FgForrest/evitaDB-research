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

package io.evitadb.query.algebra.price.termination;

import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.query.algebra.Formula;
import lombok.RequiredArgsConstructor;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * This class extends standard Java {@link Predicate} with custom {@link #toString()} implementation that is set from
 * outside. This text describes the logic hidden in the predicate so that it could be used in generic price formulas
 * {@link Formula#toString()} methods.
 */
@RequiredArgsConstructor
public abstract class PricePredicate implements Predicate<PriceRecordContract> {
	public static final PricePredicate NO_FILTER = new PricePredicate("NO FILTER PREDICATE") {
		@Override
		public boolean test(PriceRecordContract priceRecord) {
			return true;
		}

		@Override
		public long computeHash(@Nonnull LongHashFunction hashFunction) {
			return 0L;
		}
	};

	/**
	 * Contains brief description of what predicate does.
	 */
	private final String description;

	@Override
	public String toString() {
		return description;
	}

	/**
	 * Method is expected to compute unique hash for this particular instance of predicate. This hash needs much better
	 * collision rate than common {@link #hashCode()} and therefore passed hash function is expected to be utilized.
	 * Hash is targeted to be used in cache key.
	 */
	public abstract long computeHash(@Nonnull LongHashFunction hashFunction);
}
