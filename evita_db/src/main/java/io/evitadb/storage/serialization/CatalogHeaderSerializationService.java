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

package io.evitadb.storage.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.CatalogState;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.serialization.io.SerializationService;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.CatalogHeader;
import io.evitadb.storage.model.memTable.FileLocation;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.evitadb.api.serialization.io.SerializationService.getClassTypeSafely;
import static java.util.Optional.ofNullable;

/**
 * This class takes care of (de)serialization of dictionary contents from and to binary format.
 * Currently simple implementation that keeps single kryo instance with all necessary classes registered. Implementation
 * is not thread safe.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@ThreadSafe
@RequiredArgsConstructor
public class CatalogHeaderSerializationService implements SerializationService<CatalogHeader> {
	private final Consumer<Kryo> kryoConfigurer;

	@Override
	public void serialize(@Nonnull CatalogHeader header, @Nonnull Output output) {
		final WritableClassResolver classResolver = new WritableClassResolver(KryoFactory.CLASSES_RESERVED_FOR_INTERNAL_USE);
		final Kryo kryo = KryoFactory.createKryo(classResolver);

		for (ClassId registeredClass : header.getRegisteredClasses()) {
			classResolver.registerTypeWithDefaultSerializer(registeredClass);
		}

		for (Serializable entityType : header.getEntityTypes()) {
			final Class<? extends Serializable> entityTypeClass = entityType.getClass();
			if (classResolver.getRegistration(entityTypeClass) == null) {
				classResolver.registerNewEnumType(entityTypeClass);
			}
		}

		ofNullable(kryoConfigurer).ifPresent(it -> it.accept(kryo));

		output.writeString(header.getCatalogName());
		kryo.writeObject(output, header.getCatalogState());
		output.writeVarLong(header.getLastTransactionId(), true);
		// write information about classes first
		serializeClassRegistrations(classResolver.listRecordedClasses(), output);

		output.writeVarInt(header.getEntityTypes().size(), true);
		for (CatalogEntityHeader catalogEntityHeader : header.getEntityTypesIndex().values()) {
			DefaultKryoSerializationHelper.INSTANCE.writeSerializable(kryo, output, catalogEntityHeader.getEntityType());
			output.writeVarInt(catalogEntityHeader.getLastPrimaryKey(), true);
			output.writeVarInt(catalogEntityHeader.getLastEntityIndexPrimaryKey(), true);
			output.writeVarInt(catalogEntityHeader.getRecordCount(), true);
			final FileLocation memTableLocation = catalogEntityHeader.getMemTableLocation();
			output.writeVarLong(memTableLocation.getStartingPosition(), true);
			output.writeVarInt(memTableLocation.getRecordLength(), true);
			serializeClassRegistrations(catalogEntityHeader.getRegisteredClasses(), output);
			serializeKeys(catalogEntityHeader.getIdToKeyIndex(), output, kryo);
			kryo.writeObjectOrNull(output, catalogEntityHeader.getGlobalEntityIndexId(), Integer.class);
			serializeEntityIndexIds(output, catalogEntityHeader);
		}
	}

	@Override
	public CatalogHeader deserialize(@Nonnull Input input) {
		final WritableClassResolver classResolver = new WritableClassResolver(KryoFactory.CLASSES_RESERVED_FOR_INTERNAL_USE);
		final Kryo kryo = KryoFactory.createKryo(
			classResolver,
			kryoConfigurer
		);

		final String catalogName = input.readString();
		final CatalogState catalogState = kryo.readObject(input, CatalogState.class);
		final long lastTransactionId = input.readVarLong(true);
		final List<ClassId> classIds = deserializeClassRegistrations(input);

		for (ClassId classId : classIds) {
			if (classResolver.getRegistration(classId.getType()) == null) {
				classResolver.registerTypeWithDefaultSerializer(classId);
			}
		}

		final int entityTypeCount = input.readVarInt(true);
		final List<CatalogEntityHeader> entityTypeHeaders = new ArrayList<>(entityTypeCount);
		for (int i = 0; i < entityTypeCount; i++) {
			final Serializable entityType = DefaultKryoSerializationHelper.INSTANCE.readSerializable(kryo, input);
			final int lastPrimaryKey = input.readVarInt(true);
			final int lastEntityIndexPrimaryKey = input.readVarInt(true);
			final int entityCount = input.readVarInt(true);
			final FileLocation memTableLocation = new FileLocation(
				input.readVarLong(true),
				input.readVarInt(true)
			);
			final List<ClassId> additionalClassRegistrations = deserializeClassRegistrations(input);
			final List<ClassId> entitySpecificClassRegistrations;
			if (additionalClassRegistrations.isEmpty()) {
				// no specific classes were added
				entitySpecificClassRegistrations = classIds;
			} else {
				entitySpecificClassRegistrations = new ArrayList<>(classIds.size() + additionalClassRegistrations.size());
				entitySpecificClassRegistrations.addAll(classIds);
				entitySpecificClassRegistrations.addAll(additionalClassRegistrations);
			}
			final Map<Integer, Object> keys = deserializeKeys(input, kryo);

			final Integer globalIndexKey = kryo.readObjectOrNull(input, Integer.class);
			final List<Integer> entityIndexIds = deserializeEntityIndexIds(input);

			entityTypeHeaders.add(
				new CatalogEntityHeader(
					entityType, 1L, entityCount, lastPrimaryKey, lastEntityIndexPrimaryKey,
					memTableLocation,
					keys, entitySpecificClassRegistrations,
					globalIndexKey, entityIndexIds
				)
			);
		}
		return new CatalogHeader(catalogName, catalogState, lastTransactionId, classIds, entityTypeHeaders);
	}

	/*
		PRIVATE METHODS
	 */

	private void serializeEntityIndexIds(@Nonnull Output output, @Nonnull CatalogEntityHeader catalogEntityHeader) {
		final int entityIndexCount = catalogEntityHeader.getUsedEntityIndexIds().size();
		output.writeVarInt(entityIndexCount, true);
		output.writeInts(catalogEntityHeader.getUsedEntityIndexIds().stream().mapToInt(it -> it).toArray(), 0, entityIndexCount, true);
	}

	@Nonnull
	private List<Integer> deserializeEntityIndexIds(@Nonnull Input input) {
		final int entityIndexCount = input.readVarInt(true);
		return Arrays.stream(input.readInts(entityIndexCount, true))
			.boxed()
			.collect(Collectors.toList());
	}

	private void serializeClassRegistrations(@Nonnull List<ClassId> classIds, @Nonnull Output output) {
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
	private List<ClassId> deserializeClassRegistrations(@Nonnull Input input) {
		final int registrationCount = input.readVarInt(true);
		final List<ClassId> classIdList = new ArrayList<>(registrationCount);
		for (int i = 0; i < registrationCount; i++) {
			final int id = input.readVarInt(true);
			final boolean isArray = input.readBoolean();
			final String typeName = input.readString();

			final Class<?> type = isArray ?
				Array.newInstance(getClassTypeSafely(typeName), 0).getClass() :
				getClassTypeSafely(typeName);

			classIdList.add(new ClassId(id, type));
		}
		return classIdList;
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
