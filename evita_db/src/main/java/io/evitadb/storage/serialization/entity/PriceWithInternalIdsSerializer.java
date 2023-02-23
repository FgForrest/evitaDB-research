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

package io.evitadb.storage.serialization.entity;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.key.CompressiblePriceKey;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.index.price.model.internalId.PriceWithInternalIds;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * This {@link Serializer} implementation reads/writes {@link PriceWithInternalIds} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class PriceWithInternalIdsSerializer extends Serializer<PriceWithInternalIds> {
	private final KeyCompressor keyCompressor;

	@Override
	public void write(Kryo kryo, Output output, PriceWithInternalIds price) {
		output.writeVarInt(price.getVersion(), true);
		output.writeBoolean(price.isSellable());
		if (price.isSellable()) {
			output.writeInt(price.getInternalPriceId());
		}
		output.writeInt(price.getPriceId());
		output.writeVarInt(keyCompressor.getId(new CompressiblePriceKey(price.getPriceKey())), true);
		if (price.getInnerRecordId() == null) {
			output.writeBoolean(false);
		} else {
			output.writeBoolean(true);
			output.writeInt(price.getInnerRecordId());
		}
		kryo.writeObject(output, price.getPriceWithoutVat());
		kryo.writeObject(output, price.getVat());
		kryo.writeObject(output, price.getPriceWithVat());
		kryo.writeObjectOrNull(output, price.getValidity(), DateTimeRange.class);
		output.writeBoolean(price.isDropped());
	}

	@Override
	public PriceWithInternalIds read(Kryo kryo, Input input, Class<? extends PriceWithInternalIds> type) {
		final int version = input.readVarInt(true);
		final boolean sellable = input.readBoolean();
		final Integer internalPriceId = sellable ? input.readInt() : null;
		final int priceId = input.readInt();
		final CompressiblePriceKey priceKey = keyCompressor.getKeyForId(input.readVarInt(true));
		final boolean innerIdExists = input.readBoolean();
		final Integer innerRecordId;
		if (innerIdExists) {
			innerRecordId = input.readInt();
		} else {
			innerRecordId = null;
		}
		final BigDecimal priceWithoutVat = kryo.readObject(input, BigDecimal.class);
		final BigDecimal vat = kryo.readObject(input, BigDecimal.class);
		final BigDecimal priceWithVat = kryo.readObject(input, BigDecimal.class);
		final DateTimeRange validity = kryo.readObjectOrNull(input, DateTimeRange.class);
		final boolean dropped = input.readBoolean();

		return new PriceWithInternalIds(
			new Price(
				version,
				new PriceKey(priceId, priceKey.getPriceList(), priceKey.getCurrency()),
				innerRecordId,
				priceWithoutVat, vat, priceWithVat,
				validity, sellable, dropped
			),
			internalPriceId
		);
	}

}
