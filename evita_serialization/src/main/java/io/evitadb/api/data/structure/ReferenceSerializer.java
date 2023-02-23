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

package io.evitadb.api.data.structure;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.api.utils.Assert;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * This {@link Serializer} implementation reads/writes {@link Reference} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class ReferenceSerializer extends Serializer<Reference> {
	private final KryoSerializationHelper kryoSerializationHelper;

	@Override
	public void write(Kryo kryo, Output output, Reference reference) {
		output.writeVarInt(reference.getVersion(), true);
		final EntityReferenceContract<?> referencedEntity = reference.getReferencedEntity();
		kryoSerializationHelper.writeSerializable(kryo, output, referencedEntity.getType());
		output.writeInt(referencedEntity.getPrimaryKey());
		output.writeBoolean(reference.isDropped());
		final GroupEntityReference group = reference.getGroup();
		final boolean groupExists = group != null;
		output.writeBoolean(groupExists);
		if (groupExists) {
			kryoSerializationHelper.writeSerializable(kryo, output, group.getType());
			output.writeVarInt(group.getVersion(), true);
			output.writeInt(group.getPrimaryKey());
			output.writeBoolean(group.isDropped());
		}
		kryo.writeObject(output, reference.getAttributes());
	}

	@Override
	public Reference read(Kryo kryo, Input input, Class<? extends Reference> type) {
		final int version = input.readVarInt(true);
		final Serializable entityType = kryoSerializationHelper.readSerializable(kryo, input);
		final int entityPrimaryKey = input.readInt();
		final boolean dropped = input.readBoolean();
		final boolean groupExists = input.readBoolean();
		final GroupEntityReference group;
		if (groupExists) {
			final Serializable groupType = kryoSerializationHelper.readSerializable(kryo, input);
			final int groupVersion = input.readVarInt(true);
			final int groupPrimaryKey = input.readInt();
			final boolean groupDropped = input.readBoolean();
			group = new GroupEntityReference(groupVersion, groupType, groupPrimaryKey, groupDropped);
		} else {
			group = null;
		}
		final EntitySchema schema = EntitySerializationContext.getEntitySchema();
		final ReferenceSchema reference = schema.getReference(entityType);
		Assert.isTrue(reference != null, "Reference schema for `" + entityType + "` not found!");

		final Attributes attributes = AttributesSerializer.executeWithSupplier(
			reference.getAttributes(),
			() -> kryo.readObject(input, Attributes.class)
		);

		return new Reference(
			schema, version,
			new EntityReference(entityType, entityPrimaryKey),
			group, attributes, dropped
		);
	}

}
