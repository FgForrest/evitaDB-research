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

package io.evitadb.storage.serialization.index;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.index.price.model.PriceIndexKey;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Currency;

/**
 * This {@link Serializer} implementation reads/writes {@link PriceIndexKey} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class PriceIndexKeySerializer extends Serializer<PriceIndexKey> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, PriceIndexKey price) {
		kryoSerializationHelper.writeSerializable(kryo, output, price.getPriceList());
		kryo.writeObject(output, price.getCurrency());
		kryo.writeObject(output, price.getRecordHandling());
	}

	@Override
	public PriceIndexKey read(Kryo kryo, Input input, Class<? extends PriceIndexKey> type) {
		final Serializable priceList = kryoSerializationHelper.readSerializable(kryo, input);
		final Currency currency = kryo.readObject(input, Currency.class);
		final PriceInnerRecordHandling innerRecordHandling = kryo.readObject(input, PriceInnerRecordHandling.class);
		return new PriceIndexKey(
			priceList, currency, innerRecordHandling
		);
	}

}
