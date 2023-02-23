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

package io.evitadb.api.schema;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Map;

import static io.evitadb.api.serialization.utils.UnmodifiableCollectionsUnwrapper.unwrap;

/**
 * This {@link Serializer} implementation reads/writes {@link ReferenceSchema} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class ReferenceSchemaSerializer extends Serializer<ReferenceSchema> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, ReferenceSchema referenceSchema) {
		kryoSerializationHelper.writeSerializable(kryo, output, referenceSchema.getEntityType());
		output.writeBoolean(referenceSchema.isEntityTypeRelatesToEntity());
		kryoSerializationHelper.writeOptionalSerializable(kryo, output, referenceSchema.getGroupType());
		output.writeBoolean(referenceSchema.isGroupTypeRelatesToEntity());
		output.writeBoolean(referenceSchema.isIndexed());
		output.writeBoolean(referenceSchema.isFaceted());
		kryo.writeObject(output, unwrap(referenceSchema.getAttributes()));
	}

	@Override
	public ReferenceSchema read(Kryo kryo, Input input, Class<? extends ReferenceSchema> aClass) {
		final Serializable entityType = kryoSerializationHelper.readSerializable(kryo, input);
		final boolean entityTypeRelatesToEntity = input.readBoolean();
		final Serializable groupType = kryoSerializationHelper.readOptionalSerializable(kryo, input);
		final boolean groupTypeRelatesToEntity = input.readBoolean();
		final boolean indexed = input.readBoolean();
		final boolean faceted = input.readBoolean();
		@SuppressWarnings("unchecked") final Map<String, AttributeSchema> attributes = kryo.readObject(input, Map.class);
		return new ReferenceSchema(
			entityType, entityTypeRelatesToEntity,
			groupType, groupTypeRelatesToEntity,
			indexed, faceted, attributes
		);
	}

}
