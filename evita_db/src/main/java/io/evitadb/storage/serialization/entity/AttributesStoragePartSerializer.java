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
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

import static java.util.Optional.ofNullable;

/**
 * This {@link Serializer} implementation reads/writes {@link AttributesStoragePart} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class AttributesStoragePartSerializer extends Serializer<AttributesStoragePart> {
	private final KeyCompressor keyCompressor;

	@Override
	public void write(Kryo kryo, Output output, AttributesStoragePart object) {
		final long uniqueId = ofNullable(object.getUniquePartId()).orElseGet(() -> object.computeUniquePartIdAndSet(keyCompressor));
		output.writeLong(uniqueId);
		output.writeInt(object.getEntityPrimaryKey());
		kryo.writeObjectOrNull(output, object.getLocale(), Locale.class);

		final AttributeValue[] attributes = object.getAttributes();
		output.writeVarInt(attributes.length, true);
		for (AttributeValue attribute : attributes) {
			kryo.writeObject(output, attribute);
		}
	}

	@Override
	public AttributesStoragePart read(Kryo kryo, Input input, Class<? extends AttributesStoragePart> type) {
		final long uniquePartId = input.readLong();
		final int entityPrimaryKey = input.readInt();
		final Locale locale = kryo.readObjectOrNull(input, Locale.class);

		final int attributeCount = input.readVarInt(true);
		final AttributeValue[] attributes = new AttributeValue[attributeCount];
		for (int i = 0; i < attributeCount; i++) {
			attributes[i] = kryo.readObject(input, AttributeValue.class);
		}

		return new AttributesStoragePart(
			uniquePartId, entityPrimaryKey, locale, attributes
		);
	}

}