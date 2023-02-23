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
import io.evitadb.api.serialization.common.HeterogeneousMapSerializer;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.*;

import static io.evitadb.api.serialization.utils.UnmodifiableCollectionsUnwrapper.unwrap;

/**
 * This {@link Serializer} implementation reads/writes {@link io.evitadb.api.schema.EntitySchema} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class EntitySchemaSerializer extends Serializer<EntitySchema> {
	private final KryoSerializationHelper kryoSerializationHelper;
	private final HeterogeneousMapSerializer<Object, Object> heterogeneousSerializer;

	public EntitySchemaSerializer(KryoSerializationHelper kryoSerializationHelper) {
		this.kryoSerializationHelper = kryoSerializationHelper;
		this.heterogeneousSerializer = new HeterogeneousMapSerializer<>(kryoSerializationHelper, LinkedHashMap::new);
	}

	@Override
	public void write(Kryo kryo, Output output, EntitySchema entitySchema) {
		output.writeInt(entitySchema.getVersion());
		kryoSerializationHelper.writeSerializable(kryo, output, entitySchema.getName());
		output.writeBoolean(entitySchema.isWithGeneratedPrimaryKey());
		output.writeBoolean(entitySchema.isWithHierarchy());
		output.writeBoolean(entitySchema.isWithPrice());
		output.writeInt(entitySchema.getIndexedPricePlaces(), true);
		kryo.writeObject(output, unwrap(entitySchema.getLocales()));
		kryo.writeObject(output, unwrap(entitySchema.getAttributes()));
		kryo.writeObject(output, unwrap(entitySchema.getAssociatedData()));
		kryo.writeObject(output, unwrap(entitySchema.getReferences()), heterogeneousSerializer);
		kryo.writeObject(output, unwrap(entitySchema.getEvolutionMode()));
	}

	@Override
	public EntitySchema read(Kryo kryo, Input input, Class<? extends EntitySchema> aClass) {
		final int version = input.readInt();
		final Serializable entityName = kryoSerializationHelper.readSerializable(kryo, input);
		final boolean withGeneratedPrimaryKey = input.readBoolean();
		final boolean withHierarchy = input.readBoolean();
		final boolean withPrice = input.readBoolean();
		final int indexedPricePlaces = input.readInt(true);
		@SuppressWarnings("unchecked") final Set<Locale> locales = kryo.readObject(input, LinkedHashSet.class);
		@SuppressWarnings("unchecked") final Map<String, AttributeSchema> attributeSchema = kryo.readObject(input, LinkedHashMap.class);
		@SuppressWarnings("unchecked") final Map<String, AssociatedDataSchema> associatedDataSchema = kryo.readObject(input, LinkedHashMap.class);
		@SuppressWarnings("unchecked") final Map<Serializable, ReferenceSchema> referenceSchema = kryo.readObject(input, LinkedHashMap.class, heterogeneousSerializer);
		@SuppressWarnings("unchecked") final Set<EvolutionMode> evolutionMode = kryo.readObject(input, EnumSet.class);
		return new EntitySchema(
			version,
			entityName,
			withGeneratedPrimaryKey, withHierarchy,
			withPrice, indexedPricePlaces,
			locales,
			attributeSchema,
			associatedDataSchema,
			referenceSchema,
			evolutionMode
		);
	}

}
