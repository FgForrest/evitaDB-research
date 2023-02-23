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

package io.evitadb.storage.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CurrencySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.common.EnumNameSerializer;
import io.evitadb.api.serialization.common.SerialVersionBasedSerializer;
import io.evitadb.api.serialization.io.SerializationService;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.cache.payload.*;
import io.evitadb.index.GlobalEntityIndex;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.query.algebra.CacheableFormula;
import io.evitadb.query.extraResult.translator.histogram.cache.FlattenedHistogramComputer;
import io.evitadb.storage.serialization.cache.*;
import io.evitadb.storage.serialization.index.PriceIndexKeySerializer;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Currency;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class takes care of (de)serialization {@link CachePayloadHeader} from and to binary format.
 * Currently, simple implementation that keeps single kryo instance with all necessary classes registered. Implementation
 * is not thread safe.
 *
 * Deserializers that work with price needs to have access to the original {@link CacheableFormula} in order to find
 * proper {@link PriceRecord} for serialized price ids. Complete price records are not serialized
 * to save memory.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@NotThreadSafe
public class CachedRecordSerializationService implements SerializationService<CachePayloadHeader> {
	private final Kryo kryo;

	public CachedRecordSerializationService(@Nonnull Supplier<GlobalEntityIndex> globalEntityIndexAccessor) {
		this.kryo = KryoFactory.createKryo(
			new DefaultClassResolver(),
			new CachedRecordKryoConfigurer(globalEntityIndexAccessor)
		);
	}

	@Override
	public void serialize(@Nonnull CachePayloadHeader theObject, @Nonnull Output output) {
		this.kryo.writeClassAndObject(output, theObject);
	}

	@Override
	public CachePayloadHeader deserialize(@Nonnull Input input) {
		return (CachePayloadHeader) this.kryo.readClassAndObject(input);
	}

	/**
	 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
	 * required to (de)serialize {@link io.evitadb.query.algebra.CacheableFormula}.
	 */
	@RequiredArgsConstructor
	public static class CachedRecordKryoConfigurer implements Consumer<Kryo> {
		private final Supplier<GlobalEntityIndex> globalEntityIndexAccessor;

		@Override
		public void accept(Kryo kryo) {
			kryo.register(String.class, new StringSerializer(), 100);
			kryo.register(Currency.class, new SerialVersionBasedSerializer<>(new CurrencySerializer(), Currency.class), 101);
			kryo.register(PriceInnerRecordHandling.class, new EnumNameSerializer<PriceInnerRecordHandling>(DefaultKryoSerializationHelper.INSTANCE), 102);

			kryo.register(FlattenedFormula.class, new SerialVersionBasedSerializer<>(new FlattenedFormulaSerializer(), FlattenedFormula.class), 200);
			kryo.register(FlattenedFormulaWithFilteredOutRecords.class, new SerialVersionBasedSerializer<>(new FlattenedFormulaWithFilteredOutRecordsSerializer(), FlattenedFormulaWithFilteredOutRecords.class), 201);
			kryo.register(FlattenedFormulaWithFilteredPrices.class, new SerialVersionBasedSerializer<>(new FlattenedFormulaWithFilteredPricesSerializer(globalEntityIndexAccessor), FlattenedFormulaWithFilteredPrices.class), 202);
			kryo.register(FlattenedFormulaWithFilteredPricesAndFilteredOutRecords.class, new SerialVersionBasedSerializer<>(new FlattenedFormulaWithFilteredPricesAndFilteredOutRecordsSerializer(globalEntityIndexAccessor), FlattenedFormulaWithFilteredPricesAndFilteredOutRecords.class), 203);
			kryo.register(FlattenedHistogramComputer.class, new SerialVersionBasedSerializer<>(new FlattenedHistogramComputerSerializer(), FlattenedHistogramComputer.class), 204);
			kryo.register(PriceIndexKey.class, new SerialVersionBasedSerializer<>(new PriceIndexKeySerializer(DefaultKryoSerializationHelper.INSTANCE), PriceIndexKey.class), 205);
		}

	}
}
