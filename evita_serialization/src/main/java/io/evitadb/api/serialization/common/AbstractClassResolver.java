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

import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract base class for {@link ReadOnlyClassResolver} and {@link WritableClassResolver}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
abstract class AbstractClassResolver extends DefaultClassResolver {
	protected final KryoSerializationHelper kryoSerializationHelper;

	protected AbstractClassResolver() {
		this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
	}

	protected AbstractClassResolver(KryoSerializationHelper kryoSerializationHelper) {
		this.kryoSerializationHelper = kryoSerializationHelper;
	}

	/**
	 * Registers classes passed in argument to the internal representation of Kryo's {@link com.esotericsoftware.kryo.ClassResolver}.
	 * Each class is bootstraped with {@link EnumNameSerializer}, but {@link io.evitadb.api.serialization.KryoFactory}
	 * should overwrite known classes with its own Serializers.
	 *
	 * @param classIds
	 * @return
	 */
	protected int registerClasses(List<ClassId> classIds) {
		int peekId = 0;
		for (ClassId classId : classIds) {
			this.register(
				new Registration(
					classId.getType(),
					classId.getType().isArray() ? getEnumArraySerializer() : getEnumSerializer(),
					classId.getId()
				)
			);
		}
		return peekId;
	}

	/**
	 * Provides access to all currently registered classes along with their unique Kryo ids and serializer instances.
	 * @return
	 */
	public Iterable<Registration> getRegistrations() {
		return idToRegistration.values();
	}

	/**
	 * This method will create list of all classes currently registered in Kryo instance. Result is created dynamically
	 * and takes some effort so it is advised to call this method with caution and is expected to be called only once
	 * at the end of the serialization process.
	 *
	 * @return list of {@link ClassId} that allows to recreate Kryo instance to the state that will be able to deserialize
	 * serialized contents in consistent way (id and class names must match)
	 */
	public List<ClassId> listRecordedClasses() {
		final List<ClassId> classDictionary = new LinkedList<>();
		for (Registration registration : idToRegistration.values()) {
			if (!registration.getType().isPrimitive()) {
				classDictionary.add(new ClassId(registration));
			}
		}
		return classDictionary;
	}

	/**
	 * Only single unknown type is allowed in EvitaDB and it is {@link Enum} type. Standard {@link EvitaDataTypes#getSupportedDataTypes()}
	 * have already registered serializers by {@link io.evitadb.api.serialization.KryoFactory} and unknown classes are
	 * converted to {@link io.evitadb.api.dataType.ComplexDataObject} object that has its serializer present as well.
	 *
	 * @param <T>
	 * @return
	 */
	@Nonnull
	protected <T extends Enum<T>> Serializer<T> getEnumSerializer() {
		return new EnumNameSerializer<>(kryoSerializationHelper);
	}

	/**
	 * Only single unknown type is allowed in EvitaDB and it is {@link Enum} type. Standard {@link EvitaDataTypes#getSupportedDataTypes()}
	 * have already registered serializers by {@link io.evitadb.api.serialization.KryoFactory} and unknown classes are
	 * converted to {@link io.evitadb.api.dataType.ComplexDataObject} object that has its serializer present as well.
	 *
	 * This method returns serializer that is able to (de)serialize array of enums.
	 *
	 * @param <T>
	 * @return
	 */
	@Nonnull
	protected <T extends Enum<T>> EnumArrayNameSerializer<T> getEnumArraySerializer() {
		return new EnumArrayNameSerializer<>(kryoSerializationHelper);
	}

}
