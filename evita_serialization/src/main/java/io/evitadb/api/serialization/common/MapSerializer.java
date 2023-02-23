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

package io.evitadb.api.serialization.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntFunction;

/**
 * This serializer implementation allows to store any implementation of the {@link Map} interface. This serializer
 * implementation expects that Map will contain no NULL values or keys and also all keys will be homogenous and all
 * values will be homogenous as well.
 *
 * By homogenous I mean that all objects must be instances of the very same class. Opposite to this is
 * {@link HeterogeneousMapSerializer} that allows to store keys and values that implements common superclass but can
 * be instances of different classes.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class MapSerializer<K extends Serializable, V extends Serializable> extends Serializer<Map<K, V>> {
	private final KryoSerializationHelper kryoSerializationHelper;
	public final IntFunction<Map<K, V>> setFactory;

	@Override
	public void write(Kryo kryo, Output output, Map<K, V> object) {
		output.writeVarInt(object.size(), true);
		boolean classWritten = false;
		for (Entry<K, V> entry : object.entrySet()) {
			if (!classWritten) {
				kryoSerializationHelper.writeSerializableClass(kryo, output, entry.getKey().getClass());
				kryoSerializationHelper.writeSerializableClass(kryo, output, entry.getValue().getClass());
				classWritten = true;
			}
			kryoSerializationHelper.writeObject(kryo, output, entry.getKey());
			kryoSerializationHelper.writeObject(kryo, output, entry.getValue());
		}
	}

	@Override
	public Map<K, V> read(Kryo kryo, Input input, Class<? extends Map<K, V>> type) {
		final int itemCount = input.readVarInt(true);
		final Map<K, V> targetSet = setFactory.apply(itemCount);
		Class<K> keyClass = null;
		Class<V> valueClass = null;
		for (int i = 0; i < itemCount; i++) {
			if (keyClass == null) {
				keyClass = kryoSerializationHelper.readSerializableClass(kryo, input);
				valueClass = kryoSerializationHelper.readSerializableClass(kryo, input);
			}
			targetSet.put(
					kryoSerializationHelper.readObject(kryo, input, keyClass),
					kryoSerializationHelper.readObject(kryo, input, valueClass)
			);
		}
		return targetSet;
	}

}
