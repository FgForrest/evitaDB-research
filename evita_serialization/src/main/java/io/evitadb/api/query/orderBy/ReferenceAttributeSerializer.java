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

package io.evitadb.api.query.orderBy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.order.ReferenceAttribute;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

/**
 * This {@link Serializer} implementation reads/writes {@link ReferenceAttribute} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class ReferenceAttributeSerializer extends Serializer<ReferenceAttribute> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, ReferenceAttribute object) {
		kryoSerializationHelper.writeSerializable(kryo, output, object.getEntityType());
		final OrderConstraint[] children = object.getConstraints();
		output.writeVarInt(children.length, true);
		for (OrderConstraint child : children) {
			kryo.writeClassAndObject(output, child);
		}
	}

	@Override
	public ReferenceAttribute read(Kryo kryo, Input input, Class<? extends ReferenceAttribute> type) {
		final String attributeName = input.readString();
		final OrderConstraint[] children = new OrderConstraint[input.readVarInt(true)];
		for (int i = 0; i < children.length; i++) {
			children[i] = (OrderConstraint) kryo.readClassAndObject(input);
		}
		return new ReferenceAttribute(attributeName, children);
	}

}
