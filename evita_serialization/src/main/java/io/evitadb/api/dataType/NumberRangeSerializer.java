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

package io.evitadb.api.dataType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

/**
 * This {@link Serializer} implementation reads/writes {@link NumberRange} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class NumberRangeSerializer extends Serializer<NumberRange> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, NumberRange numberRange) {
		kryoSerializationHelper.writeOptionalSerializable(kryo, output, numberRange.getPreciseFrom());
		kryoSerializationHelper.writeOptionalSerializable(kryo, output, numberRange.getPreciseTo());
		kryo.writeObjectOrNull(output, numberRange.getRetainedDecimalPlaces(), Integer.class);
		output.writeLong(numberRange.getFrom());
		output.writeLong(numberRange.getTo());
	}

	@Override
	public NumberRange read(Kryo kryo, Input input, Class<? extends NumberRange> type) {
		final Number preciseFrom = (Number) kryoSerializationHelper.readOptionalSerializable(kryo, input);
		final Number preciseTo = (Number) kryoSerializationHelper.readOptionalSerializable(kryo, input);
		final Integer retainedDecimalPlaces = kryo.readObjectOrNull(input, Integer.class);
		final long from = input.readLong();
		final long to = input.readLong();
		return new NumberRange(preciseFrom, preciseTo, retainedDecimalPlaces, from, to);
	}

}
