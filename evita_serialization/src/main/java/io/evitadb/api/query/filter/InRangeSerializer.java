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

package io.evitadb.api.query.filter;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * This {@link Serializer} implementation reads/writes {@link InRange} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class InRangeSerializer extends Serializer<InRange> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, InRange object) {
		output.writeString(object.getAttributeName());
		kryoSerializationHelper.writeSerializable(kryo, output, object.getUnknownArgument());
	}

	@Override
	public InRange read(Kryo kryo, Input input, Class<? extends InRange> type) {
		final String attributeName = input.readString();
		final Serializable theValue = kryoSerializationHelper.readSerializable(kryo, input);
		if (theValue instanceof Number) {
			return new InRange(attributeName, (Number) theValue);
		} else if (theValue instanceof ZonedDateTime) {
			return new InRange(attributeName, (ZonedDateTime) theValue);
		} else {
			throw new IllegalArgumentException("Unsupported filter value: " + theValue);
		}
	}

}