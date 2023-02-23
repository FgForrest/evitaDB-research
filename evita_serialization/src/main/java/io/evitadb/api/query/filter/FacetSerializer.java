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
import java.util.Arrays;

/**
 * This {@link Serializer} implementation reads/writes {@link Facet} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class FacetSerializer extends Serializer<Facet> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, Facet object) {
		kryoSerializationHelper.writeSerializable(kryo, output, object.getEntityType());
		final int[] primaryKeys = object.getFacetIds();
		output.writeVarInt(primaryKeys.length, true);
		output.writeInts(primaryKeys, 0, primaryKeys.length);
	}

	@Override
	public Facet read(Kryo kryo, Input input, Class<? extends Facet> type) {
		final Serializable entityType = kryoSerializationHelper.readSerializable(kryo, input);
		final int facetCount = input.readVarInt(true);
		final Integer[] primaryKeys = Arrays.stream(input.readInts(facetCount)).boxed().toArray(Integer[]::new);
		return new Facet(entityType, primaryKeys);
	}

}