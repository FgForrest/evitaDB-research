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

package io.evitadb.storage.serialization.cache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.cache.payload.CachePayloadHeader;
import io.evitadb.index.GlobalEntityIndex;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.price.PriceListAndCurrencyPriceIndex;
import io.evitadb.index.price.PriceListAndCurrencyPriceSuperIndex;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.priceRecord.CumulatedVirtualPriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.query.algebra.price.filteredPriceRecords.FilteredPriceRecords;
import io.evitadb.query.algebra.price.filteredPriceRecords.LazyEvaluatedEntityPriceRecords;
import io.evitadb.query.algebra.price.filteredPriceRecords.NonResolvedFilteredPriceRecords;
import io.evitadb.query.algebra.price.filteredPriceRecords.ResolvedFilteredPriceRecords;
import io.evitadb.query.algebra.price.termination.PriceEvaluationContext;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Abstract parent of all {@link CachePayloadHeader} serializers. Contains common logic shared among the implementations
 * and also maintains the flags that control writing head / body.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public abstract class AbstractFlattenedFormulaSerializer<T extends CachePayloadHeader> extends Serializer<T> {
	/**
	 * Cached array of all possible values of {@link QueryPriceMode} to avoid memory allocation that is part of values method.
	 */
	private static final QueryPriceMode[] QUERY_PRICE_MODE_VALUES = QueryPriceMode.values();

	/**
	 * Method serializes array of long to Kryo output.
	 */
	protected void writeBitmapIds(@Nonnull Output output, @Nonnull long[] bitmap) {
		final int bitmapIdsLength = bitmap.length;
		output.writeVarInt(bitmapIdsLength, true);
		for (long bitmapId : bitmap) {
			output.writeVarLong(bitmapId, true);
		}
	}

	/**
	 * Method deserializes array of longs from Kryo input.
	 */
	@Nonnull
	protected long[] readBitmapIds(@Nonnull Input input) {
		final int bitmapIdsLength = input.readVarInt(true);
		final long[] bitmapIds = new long[bitmapIdsLength];
		for (int i = 0; i < bitmapIdsLength; i++) {
			bitmapIds[i] = input.readVarLong(true);
		}
		return bitmapIds;
	}

	/**
	 * Method serializes integer bitmap to Kryo output.
	 */
	protected void writeIntegerBitmap(@Nonnull Output output, @Nonnull Bitmap bitmap) {
		final int computeResultLength = bitmap.size();
		output.writeVarInt(computeResultLength, true);
		output.writeInts(bitmap.getArray(), 0, computeResultLength);
	}

	/**
	 * Method deserializes integer bitmap from Kryo input.
	 */
	@Nonnull
	protected Bitmap readIntegerBitmap(@Nonnull Input input) {
		final int computedResultLength = input.readVarInt(true);
		return new BaseBitmap(input.readInts(computedResultLength));
	}

	/**
	 * Method writes {@link FilteredPriceRecords} to the Kryo output. In order to save memory space {@link PriceRecord}
	 * is not serialized completely but only {@link PriceRecordContract#getInternalPriceId()} ()} is stored to the output. Virtual
	 * prices represented by {@link CumulatedVirtualPriceRecord} must be serialized fully because their prices cannot
	 * be reconstructed easily.
	 *
	 * @see #readFilteredPriceRecords(Kryo, Input, Supplier, PriceEvaluationContext) to understand how price ids are reconstructed back
	 */
	protected void writeFilteredPriceRecords(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull FilteredPriceRecords filteredPriceRecords) {
		if (filteredPriceRecords instanceof ResolvedFilteredPriceRecords) {
			writeFilteredPriceRecords(output, (ResolvedFilteredPriceRecords) filteredPriceRecords);
		} else if (filteredPriceRecords instanceof NonResolvedFilteredPriceRecords) {
			writeFilteredPriceRecords(output, ((NonResolvedFilteredPriceRecords) filteredPriceRecords).toResolvedFilteredPriceRecords());
		} else if (filteredPriceRecords instanceof LazyEvaluatedEntityPriceRecords) {
			writeFilteredPriceRecords(kryo, output, (LazyEvaluatedEntityPriceRecords) filteredPriceRecords);
		} else {
			throw new IllegalStateException("Unknown type of FilteredPriceRecords " + filteredPriceRecords.getClass().getName());
		}


	}

	/**
	 * Method deserializes {@link FilteredPriceRecords} from the Kryo input. Price ids for prices that are part of
	 * {@link PriceListAndCurrencyPriceSuperIndex} are reconstructed using `formula` input parameter. The formula contains
	 * references to all price super indexes used for this part of query resolution. Each of this index is asked to provide
	 * full {@link PriceRecord} references for each price id that was deserialized.
	 */
	@Nonnull
	protected FilteredPriceRecords readFilteredPriceRecords(@Nonnull Kryo kryo, @Nonnull Input input, @Nonnull Supplier<GlobalEntityIndex> globalEntityIndexAccessor, @Nonnull PriceEvaluationContext priceEvaluationContext) {
		if (input.readBoolean()) {
			return readNonResolvedFilteredPriceRecords(input, priceEvaluationContext, globalEntityIndexAccessor);
		} else {
			return readLazyEvaluatedEntityPriceRecords(kryo, input, globalEntityIndexAccessor);
		}
	}

	/**
	 * Writes specialized form of {@link ResolvedFilteredPriceRecords} to a Kryo stream.
	 */
	private void writeFilteredPriceRecords(@Nonnull Output output, @Nonnull ResolvedFilteredPriceRecords filteredPriceRecords) {
		// store flag that we have resolved filtered price records
		output.writeBoolean(true);

		final PriceRecordContract[] priceRecords = filteredPriceRecords.getPriceRecords();

		// we need one iteration to create a list of internal price ids of all standard prices
		final int[] ordinaryPriceRecordIds = Arrays.stream(priceRecords)
			.filter(it -> !(it instanceof CumulatedVirtualPriceRecord))
			.mapToInt(PriceRecordContract::getInternalPriceId)
			.toArray();

		// if there are at least one cumulated price we need to write its contents as a whole as there is
		// no other way of reconstructing it
		final int cumulatedPriceRecords = priceRecords.length - ordinaryPriceRecordIds.length;
		output.writeVarInt(cumulatedPriceRecords, true);
		if (cumulatedPriceRecords > 0) {
			int writtenCumulatedRecordCount = 0;
			for (final PriceRecordContract priceRecord : priceRecords) {
				final boolean cumulatedVirtualRecord = priceRecord instanceof CumulatedVirtualPriceRecord;
				if (cumulatedVirtualRecord) {
					final CumulatedVirtualPriceRecord cumulatedPrice = (CumulatedVirtualPriceRecord) priceRecord;
					output.writeVarInt(priceRecord.getEntityPrimaryKey(), false);
					output.writeVarInt(cumulatedPrice.getPrice(), false);
					output.writeByte((byte) cumulatedPrice.getPriceMode().ordinal());
					writtenCumulatedRecordCount++;
				}
				// finish as soon as possible
				if (writtenCumulatedRecordCount == cumulatedPriceRecords) {
					break;
				}
			}
		}

		// now we can write the ids of ordinary prices
		output.writeVarInt(ordinaryPriceRecordIds.length, true);
		output.writeInts(ordinaryPriceRecordIds, 0, ordinaryPriceRecordIds.length, true);
	}

	/**
	 * Reads specialized form of {@link ResolvedFilteredPriceRecords} from a Kryo stream. Because the stream doesn't
	 * contain full bodies of prices that can be looked up in price indexes, the method creates
	 * a {@link NonResolvedFilteredPriceRecords} that can be lazily translated to the {@link ResolvedFilteredPriceRecords}
	 * using data from appropriate price indexes in {@link GlobalEntityIndex}.
	 */
	@Nonnull
	private NonResolvedFilteredPriceRecords readNonResolvedFilteredPriceRecords(@Nonnull Input input, @Nonnull PriceEvaluationContext priceEvaluationContext, @Nonnull Supplier<GlobalEntityIndex> globalEntityIndexAccessor) {

		// now read the prices from the stream
		final int priceRecordCount = input.readVarInt(true);
		final PriceRecordContract[] cumulatedPriceRecords = new PriceRecordContract[priceRecordCount];
		// read all prices
		for (int i = 0; i < priceRecordCount; i++) {
			final int entityPrimaryKey = input.readVarInt(false);
			final int price = input.readVarInt(false);
			final QueryPriceMode queryPriceMode = QUERY_PRICE_MODE_VALUES[input.readByte()];
			cumulatedPriceRecords[i] = new CumulatedVirtualPriceRecord(
				entityPrimaryKey, price, queryPriceMode
			);
		}

		final int ordinaryPriceRecordsCount = input.readVarInt(true);
		final int[] ordinaryPriceRecords = input.readInts(ordinaryPriceRecordsCount, true);

		final GlobalEntityIndex globalEntityIndex = globalEntityIndexAccessor.get();
		final PriceListAndCurrencyPriceIndex[] priceIndexes = Arrays.stream(priceEvaluationContext.getTargetPriceIndexes())
			.map(globalEntityIndex::getPriceIndex)
			.toArray(PriceListAndCurrencyPriceIndex[]::new);

		return new NonResolvedFilteredPriceRecords(cumulatedPriceRecords, new BaseBitmap(ordinaryPriceRecords), priceIndexes);
	}

	/**
	 * Writes specialized form of {@link LazyEvaluatedEntityPriceRecords} to a Kryo stream. The implementation contains
	 * information only of {@link PriceIndexKey} that can be used to retrieve the lowest prices for associated entities.
	 */
	private void writeFilteredPriceRecords(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull LazyEvaluatedEntityPriceRecords filteredPriceRecords) {
		// store flag that we don't have resolved filtered price records
		output.writeBoolean(false);

		final PriceIndexKey[] priceIndexKeys = Arrays.stream(filteredPriceRecords.getPriceIndexes())
			.map(PriceListAndCurrencyPriceIndex::getPriceIndexKey)
			.distinct()
			.toArray(PriceIndexKey[]::new);

		// now serialize the price index keys
		output.writeVarInt(priceIndexKeys.length, true);
		for (PriceIndexKey priceIndexKey : priceIndexKeys) {
			kryo.writeObject(output, priceIndexKey);
		}
	}

	/**
	 * Reads specialized form of {@link LazyEvaluatedEntityPriceRecords} to a Kryo stream. The implementation contains
	 * information only of {@link PriceIndexKey} that can be used to retrieve the lowest prices for associated entities.
	 */
	@Nonnull
	private LazyEvaluatedEntityPriceRecords readLazyEvaluatedEntityPriceRecords(@Nonnull Kryo kryo, @Nonnull Input input, @Nonnull Supplier<GlobalEntityIndex> globalEntityIndexAccessor) {
		// now read the prices from the stream
		final int priceIndexCount = input.readVarInt(true);
		final PriceIndexKey[] priceIndexKeys = new PriceIndexKey[priceIndexCount];
		// read all prices
		for (int i = 0; i < priceIndexCount; i++) {
			priceIndexKeys[i] = kryo.readObject(input, PriceIndexKey.class);
		}

		final GlobalEntityIndex globalEntityIndex = globalEntityIndexAccessor.get();
		return new LazyEvaluatedEntityPriceRecords(
			Arrays.stream(priceIndexKeys)
				.map(globalEntityIndex::getPriceIndex)
				.toArray(PriceListAndCurrencyPriceIndex[]::new)
		);
	}

	/**
	 * Method serializes {@link PriceEvaluationContext} to the Kryo output.
	 */
	protected void writePriceEvaluationContext(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull PriceEvaluationContext priceEvaluationContext) {
		final PriceIndexKey[] priceLists = priceEvaluationContext.getTargetPriceIndexes();
		output.writeVarInt(priceLists.length, true);
		for (PriceIndexKey priceIndexKey : priceLists) {
			kryo.writeObject(output, priceIndexKey);
		}
	}

	/**
	 * Method deserializes {@link PriceEvaluationContext} from the Kryo input.
	 */
	@Nonnull
	protected PriceEvaluationContext readPriceEvaluationContext(@Nonnull Kryo kryo, @Nonnull Input input) {
		final int priceListCount = input.readVarInt(true);
		final PriceIndexKey[] priceIndexKeys = new PriceIndexKey[priceListCount];
		for (int i = 0; i < priceListCount; i++) {
			priceIndexKeys[i] = kryo.readObject(input, PriceIndexKey.class);
		}

		return new PriceEvaluationContext(priceIndexKeys);
	}

}
