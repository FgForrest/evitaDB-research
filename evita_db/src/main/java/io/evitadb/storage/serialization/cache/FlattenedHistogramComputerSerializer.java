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
import io.evitadb.api.io.extraResult.Histogram;
import io.evitadb.api.io.extraResult.HistogramContract;
import io.evitadb.api.io.extraResult.HistogramContract.Bucket;
import io.evitadb.cache.payload.FlattenedFormula;
import io.evitadb.query.extraResult.translator.histogram.cache.FlattenedHistogramComputer;

import java.math.BigDecimal;

/**
 * This {@link Serializer} implementation reads/writes {@link FlattenedFormula} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class FlattenedHistogramComputerSerializer extends AbstractFlattenedFormulaSerializer<FlattenedHistogramComputer> {

	@Override
	public void write(Kryo kryo, Output output, FlattenedHistogramComputer object) {
		output.writeLong(object.getRecordHash());
		output.writeLong(object.getTransactionalIdHash());
		writeBitmapIds(output, object.getTransactionalDataIds());

		final HistogramContract histogram = object.compute();
		kryo.writeObject(output, histogram.getMax());
		final Bucket[] buckets = histogram.getBuckets();
		output.writeVarInt(buckets.length, true);
		for (Bucket bucket : buckets) {
			output.writeVarInt(bucket.getIndex(), true);
			output.writeVarInt(bucket.getOccurrences(), true);
			kryo.writeObject(output, bucket.getThreshold());
		}
	}

	@Override
	public FlattenedHistogramComputer read(Kryo kryo, Input input, Class<? extends FlattenedHistogramComputer> type) {
		final long originalHash = input.readLong();
		final long transactionalIdHash = input.readLong();
		final long[] bitmapIds = readBitmapIds(input);
		final BigDecimal max = kryo.readObject(input, BigDecimal.class);
		final int bucketCount = input.readVarInt(true);
		final Bucket[] buckets = new Bucket[bucketCount];
		for(int i = 0; i < bucketCount; i++) {
			final int index = input.readVarInt(true);
			final int occurrences = input.readVarInt(true);
			final BigDecimal threshold = kryo.readObject(input, BigDecimal.class);
			buckets[i] = new Bucket(index, threshold, occurrences);
		}

		return new FlattenedHistogramComputer(
			originalHash, transactionalIdHash, bitmapIds,
			new Histogram(buckets, max)
		);
	}

}
