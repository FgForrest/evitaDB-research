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

package io.evitadb.api.dataType.trie;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is serializer that is optimized to store tries in their original tree structure.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class TrieSerializer<T extends Serializable> extends Serializer<Trie<T>> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, Trie<T> trie) {
		kryoSerializationHelper.writeSerializableClass(kryo, output, trie.type);
		kryo.writeObject(output, trie.root);
	}

	@Override
	public Trie<T> read(Kryo kryo, Input input, Class<? extends Trie<T>> type) {
		final Class<T> arrayType = kryoSerializationHelper.readSerializableClass(kryo, input);
		final AtomicReference<TrieNode<T>> rootRef = new AtomicReference<>();
		//noinspection unchecked
		TrieNodeSerializer.deserializeWithArrayType(arrayType, () -> {
			@SuppressWarnings("unchecked")
			final TrieNode<T> rootNode = kryo.readObject(input, TrieNode.class);
			rootRef.set(rootNode);
		});
		return new Trie<>(
			arrayType, rootRef.get()
		);
	}

}
