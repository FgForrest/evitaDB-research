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
import io.evitadb.api.dataType.ComplexDataObject.EmptyValue;
import io.evitadb.api.dataType.ComplexDataObject.NullValue;
import io.evitadb.api.dataType.trie.Trie;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This {@link Serializer} implementation reads/writes {@link ComplexDataObject} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class ComplexDataObjectSerializer extends Serializer<ComplexDataObject> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, ComplexDataObject complexDataObject) {
		final List<Serializable> keys = complexDataObject.getKeys();
		output.writeVarInt(keys.size(), true);
		for (Serializable key : keys) {
			kryoSerializationHelper.writeSerializable(kryo, output, key);
		}
		kryo.writeObject(output, complexDataObject.properties);
	}

	@Override
	public ComplexDataObject read(Kryo kryo, Input input, Class<? extends ComplexDataObject> type) {
		final int keyCount = input.readVarInt(true);
		final List<Serializable> keys = new ArrayList<>(keyCount);
		for (int i = 0; i < keyCount; i++) {
			keys.add(kryoSerializationHelper.readSerializable(kryo, input));
		}
		@SuppressWarnings("unchecked")
		final Trie<Serializable> properties = kryo.readObject(input, Trie.class);
		return new ComplexDataObject(keys, properties);
	}

	public static class NullValueSerializer extends Serializer<NullValue> {

		@Override
		public void write(Kryo kryo, Output output, NullValue object) {
		}

		@Override
		public NullValue read(Kryo kryo, Input input, Class<? extends NullValue> type) {
			return NullValue.INSTANCE;
		}

	}

	public static class EmptyValueSerializer extends Serializer<EmptyValue> {

		@Override
		public void write(Kryo kryo, Output output, EmptyValue object) {
		}

		@Override
		public EmptyValue read(Kryo kryo, Input input, Class<? extends EmptyValue> type) {
			return EmptyValue.INSTANCE;
		}

	}

}
