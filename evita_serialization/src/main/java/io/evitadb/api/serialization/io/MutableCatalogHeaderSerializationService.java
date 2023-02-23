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

package io.evitadb.api.serialization.io;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.KryoFactory.CatalogSerializationHeaderKryoConfigurer;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.serialization.MutableCatalogHeader;
import io.evitadb.api.serialization.common.DefaultSerializer;
import io.evitadb.api.serialization.common.EnumNameSerializer;
import io.evitadb.api.serialization.common.ReadOnlyClassResolver;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.serialization.exception.IncompatibleClassExchangeException;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.api.utils.Assert;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

import static io.evitadb.api.serialization.io.SerializationService.getClassTypeSafely;
import static java.util.Optional.ofNullable;

/**
 * This class takes care of (de)serialization of dictionary contents from and to binary format.
 * Currently simple implementation that keeps single kryo instance with all necessary classes registered. Implementation
 * is not thread safe.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
@NotThreadSafe
public class MutableCatalogHeaderSerializationService implements SerializationService<MutableCatalogHeader> {
	private final KryoSerializationHelper kryoSerializationHelper;

	public MutableCatalogHeaderSerializationService() {
		this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
	}

	@Override
	public void serialize(@Nonnull MutableCatalogHeader header, @Nonnull Output output) {
		final WritableClassResolver classResolver = header.getClassResolver();
		final Kryo kryo = KryoFactory.createKryo(classResolver, CatalogSerializationHeaderKryoConfigurer.INSTANCE);

		for (Serializable entityType : header.getEntityTypesIndex().keySet()) {
			final Class<? extends Serializable> entityTypeClass = entityType.getClass();
			if (classResolver.getRegistration(entityTypeClass) == null) {
				classResolver.register(new Registration(entityTypeClass, new EnumNameSerializer<>(kryoSerializationHelper), kryo.getNextRegistrationId()));
			}
		}

		output.writeString(header.getCatalogName());
		// write information about classes first
		serializeClassRegistrations(output, header.listRecordedClasses());

		output.writeVarInt(header.getEntityTypes().size(), true);
		for (MutableCatalogEntityHeader entityHeader : header.getEntityTypesIndex().values()) {
			kryoSerializationHelper.writeSerializable(kryo, output, entityHeader.getEntityType());
			output.writeVarInt(entityHeader.getRecordCount(), true);
			serializeClassRegistrations(output, entityHeader.listRecordedClasses());
			serializeKeys(entityHeader.getIdToKeyIndex(), output, kryo);
		}
	}
	
	@Override
	public MutableCatalogHeader deserialize(@Nonnull Input input) {
		final String catalogName = input.readString();
		final ReadOnlyClassResolver classResolver = deserializeClassRegistrations(input);
		final Kryo kryo = KryoFactory.createKryo(classResolver, CatalogSerializationHeaderKryoConfigurer.INSTANCE);

		final int entityTypeCount = input.readVarInt(true);
		final List<MutableCatalogEntityHeader> entityTypeHeaders = new ArrayList<>(entityTypeCount);
		for (int i = 0; i < entityTypeCount; i++) {
			final Serializable entityType = kryoSerializationHelper.readSerializable(kryo, input);
			final int entityCount = input.readVarInt(true);
			final ReadOnlyClassResolver entityClassResolver = deserializeClassRegistrations(input);
			final Map<Integer, Object> keys = deserializeKeys(input, kryo);
			entityTypeHeaders.add(new MutableCatalogEntityHeader(entityType, new WritableClassResolver(entityClassResolver), entityCount, keys));
		}
		return new MutableCatalogHeader(catalogName, new WritableClassResolver(classResolver), entityTypeHeaders);
	}

	private void serializeClassRegistrations(@Nonnull Output output, List<ClassId> classIds) {
		output.writeVarInt(classIds.size(), true);
		for (ClassId recordedClass : classIds) {
			final Class<?> type = recordedClass.getType();
			final int classId = recordedClass.getId();
			final boolean isArray = type.isArray();
			final String typeName = isArray ? type.getComponentType().getName() : type.getName();

			output.writeVarInt(classId, true);
			output.writeBoolean(isArray);
			output.writeString(typeName);
		}
	}

	@Nonnull
	private ReadOnlyClassResolver deserializeClassRegistrations(@Nonnull Input input) {
		final ReadOnlyClassResolver classResolver = new ReadOnlyClassResolver(Collections.emptyList(), kryoSerializationHelper);
		final int registrationCount = input.readVarInt(true);
		for (int i = 0; i < registrationCount; i++) {
			final int id = input.readVarInt(true);
			final boolean isArray = input.readBoolean();
			final String typeName = input.readString();

			final Class<?> type = isArray ?
				Array.newInstance(getClassTypeSafely(typeName), 0).getClass() :
				getClassTypeSafely(typeName);
			final Optional<Registration> existingRegistration = ofNullable(classResolver.getRegistration(id));
			if (existingRegistration.isPresent()) {
				Assert.isTrue(
					existingRegistration.get().getType().equals(type),
					() -> new IncompatibleClassExchangeException(
						"Id " + id + " is already occupied by " + existingRegistration.get().getType() +
							" and cannot be set to " + type + " as necessary!"
					)
				);
			} else {
				// occupy id with default serializer (specific serializers will be clarified later)
				classResolver.register(
					new Registration(type, new DefaultSerializer<>(kryoSerializationHelper), id)
				);
			}
		}
		return classResolver;
	}

	private void serializeKeys(@Nonnull Map<Integer, Object> keys, Output output, Kryo kryo) {
		output.writeVarInt(keys.size(), true);
		for (Entry<Integer, Object> entry : keys.entrySet()) {
			output.writeVarInt(entry.getKey(), true);
			kryo.writeClassAndObject(output, entry.getValue());
		}
	}

	private Map<Integer, Object> deserializeKeys(Input input, Kryo kryo) {
		final Map<Integer, Object> keys = new HashMap<>();
		final int keyCount = input.readVarInt(true);
		for (int i = 1; i <= keyCount; i++) {
			keys.put(
				input.readVarInt(true),
				kryo.readClassAndObject(input)
			);
		}
		return keys;
	}

}
