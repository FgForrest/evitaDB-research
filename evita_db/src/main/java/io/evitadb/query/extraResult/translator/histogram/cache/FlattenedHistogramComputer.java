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

package io.evitadb.query.extraResult.translator.histogram.cache;

import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.utils.MemoryMeasuringConstants;
import io.evitadb.cache.payload.CachePayloadHeader;
import io.evitadb.query.extraResult.EvitaResponseExtraResultComputer;
import io.evitadb.query.response.TransactionalDataRelatedStructure;
import net.openhft.hashing.LongHashFunction;

import javax.annotation.Nonnull;

/**
 * Flattened formula represents a memoized form of original Histogram.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class FlattenedHistogramComputer extends CachePayloadHeader implements TransactionalDataRelatedStructure, EvitaResponseExtraResultComputer<HistogramContract> {
	private static final long serialVersionUID = 4049228240087093145L;
	/**
	 * Contains originally computed histogram.
	 */
	private final HistogramContract histogram;

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	public static int estimateSize(@Nonnull long[] transactionalIds, @Nonnull HistogramContract histogram) {
		return CachePayloadHeader.estimateSize(transactionalIds) +
			histogram.estimateSize();
	}

	public FlattenedHistogramComputer(long recordHash, long transactionalIdHash, @Nonnull long[] transactionalIds, @Nonnull HistogramContract histogram) {
		super(recordHash, transactionalIdHash, transactionalIds);
		this.histogram = histogram;
	}

	@Nonnull
	@Override
	public HistogramContract compute() {
		return histogram;
	}

	@Override
	public long computeHash(@Nonnull LongHashFunction hashFunction) {
		return recordHash;
	}

	@Override
	public long computeTransactionalIdHash(@Nonnull LongHashFunction hashFunction) {
		return transactionalIdHash;
	}

	@Nonnull
	@Override
	public long[] gatherTransactionalIds() {
		return transactionalDataIds;
	}

	@Override
	public long getEstimatedCost() {
		return 0;
	}

	@Override
	public long getCost() {
		return 0;
	}

	@Override
	public long getOperationCost() {
		return 0;
	}

	@Override
	public long getCostToPerformanceRatio() {
		return Long.MAX_VALUE;
	}

}
