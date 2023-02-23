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

package io.evitadb.spike.mock;

import com.carrotsearch.hppc.IntObjectHashMap;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.price.PriceListAndCurrencyPriceIndex;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.price.priceIndex.PriceIdContainerFormula;
import io.evitadb.storage.model.storageParts.StoragePart;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * Mock PriceListAndCurrencyPriceIndex implementation to be used in perf. tests.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class MockPriceListAndCurrencyPriceIndex implements PriceListAndCurrencyPriceIndex {
	private static final long serialVersionUID = -1343396298549809991L;
	private final transient IntObjectHashMap<int[]> priceIdsIndex;
	private int[] priceRecordIds;
	private PriceRecordContract[] priceRecords;

	public MockPriceListAndCurrencyPriceIndex(int entityCount) {
		this.priceIdsIndex = new IntObjectHashMap<>(entityCount);
		this.priceRecordIds = new int[0];
	}

	public void recordPrice(PriceRecordContract price) {
		this.priceRecords = this.priceRecords == null ?
			new PriceRecordContract[]{price} : ArrayUtils.insertRecordIntoOrderedArray(price, this.priceRecords, PriceRecord.PRICE_RECORD_COMPARATOR);
		this.priceRecordIds = ArrayUtils.insertIntIntoOrderedArray(price.getInnerRecordId(), this.priceRecordIds);

		final int entityId = price.getEntityPrimaryKey();
		final int[] existingPriceIds = priceIdsIndex.get(entityId);
		priceIdsIndex.put(
			entityId,
			existingPriceIds == null ?
				new int[]{price.getInternalPriceId()} :
				ArrayUtils.insertIntIntoOrderedArray(price.getInternalPriceId(), existingPriceIds)
		);
	}

	@Override
	public void resetDirty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearTransactionalMemory() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public PriceIndexKey getPriceIndexKey() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Bitmap getIndexedPriceEntityIds() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Formula getIndexedPriceEntityIdsFormula() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public PriceIdContainerFormula getIndexedRecordIdsValidInFormula(ZonedDateTime theMoment) {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	public int[] getInternalPriceIdsForEntity(int entityId) {
		return priceIdsIndex.get(entityId);
	}

	@Nullable
	@Override
	public PriceRecord[] getLowestPriceRecordsForEntity(int entityId) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public PriceRecordContract[] getPriceRecords() {
		return priceRecords;
	}

	@Nonnull
	@Override
	public int[] getIndexedPriceIds() {
		return priceRecordIds;
	}

	@Nonnull
	@Override
	public Formula createPriceIndexFormulaWithAllRecords() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	public StoragePart createStoragePart(int entityIndexPrimaryKey) {
		throw new UnsupportedOperationException();
	}

}
