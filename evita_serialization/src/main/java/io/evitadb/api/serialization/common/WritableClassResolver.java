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
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is meant to be used as default Kryo class resolver. Class resolver is used to translate types to
 * {@link Registration} instances identified by integer id (this is a way for Kryo to limit the storage requirements
 * and also speed up (de)serialization).
 *
 * Evita supports only narrowed set of {@link io.evitadb.api.dataType.EvitaDataTypes}, but supports Enum type, which is
 * by nature "wildcard" one - i.e. enum always represents unknown client enum class. This implementation
 * can cope with this situation and enums get serialized and classes are registered "on the fly". Other unknown classes
 * are reported by {@link IllegalArgumentException}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
@NotThreadSafe
public class WritableClassResolver extends AbstractClassResolver {
	@Getter private int classIdSequence;
	private final AtomicBoolean dirty = new AtomicBoolean();

	public WritableClassResolver(int sequenceSeed, KryoSerializationHelper kryoSerializationHelper) {
		super(kryoSerializationHelper);
		this.classIdSequence = sequenceSeed;
	}

	public WritableClassResolver(int sequenceSeed) {
		super();
		this.classIdSequence = sequenceSeed;
	}

	public WritableClassResolver(List<ClassId> classIds) {
		super();
		Assert.isTrue(!classIds.isEmpty(), "Only non-empty classIds are accepted for this constructor!");
		this.classIdSequence = registerClasses(classIds);
	}

	public WritableClassResolver(List<ClassId> classIds, KryoSerializationHelper kryoSerializationHelper) {
		super(kryoSerializationHelper);
		Assert.isTrue(!classIds.isEmpty(), "Only non-empty classIds are accepted for this constructor!");
		this.classIdSequence = registerClasses(classIds);
	}

	public WritableClassResolver(ReadOnlyClassResolver classResolver) {
		super(classResolver.kryoSerializationHelper);
		int peek = 0;
		for (Registration registration : classResolver.getRegistrations()) {
			this.register(registration);
			if (peek < registration.getId()) {
				peek = registration.getId();
			}
		}
		this.classIdSequence = peek;
	}

	/**
	 * Method returns TRUE if there were any changes in this instance since last reset or creation.
	 * @return
	 */
	public boolean resetDirtyFlag() {
		return dirty.getAndSet(false);
	}

	/**
	 * Registers new enum type with default serializer.
	 * @param type
	 */
	public void registerNewEnumType(Class<?> type) {
		Assert.isTrue(type.isEnum(), "Type " + type + " is not an enum!");
		kryo.register(
			type,
			type.isArray() ? getEnumArraySerializer() : getEnumSerializer(),
			classIdSequence++
		);
		this.dirty.compareAndSet(false, true);
	}

	/**
	 * Registers new array enum type with default serializer.
	 * @param type
	 */
	public void registerNewEnumArrayType(Class<?> type) {
		Assert.isTrue(type.isArray() && type.getComponentType().isEnum(), "Type " + type + " is not an enum array!");
		kryo.register(type, getEnumArraySerializer(), classIdSequence++);
		this.dirty.compareAndSet(false, true);
	}

	/**
	 * Registers new type with default serializer. If there is better serializer found it would get overwritten.
	 *
	 * @param classId
	 */
	public void registerTypeWithDefaultSerializer(ClassId classId) {
		kryo.register(
			classId.getType(),
			classId.getType().isArray() ? getEnumArraySerializer() : getEnumSerializer(),
			classId.getId()
		);
	}

	@Override
	public Registration writeClass(Output output, Class type) {
		try {
			return super.writeClass(output, type);
		} catch (IllegalArgumentException ex) {
			if (type.isEnum()) {
				registerNewEnumType(type);
				return super.writeClass(output, type);
			} else if (type.isArray() && type.getComponentType().isEnum()) {
				registerNewEnumArrayType(type);
				return super.writeClass(output, type);
			} else {
				throw ex;
			}
		}
	}
}
