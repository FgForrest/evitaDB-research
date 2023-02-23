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

package io.evitadb.api.serialization.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.dataType.EvitaDataTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This class contains various utility methods connected with (de)serialization through Kryo library.
 * Default implementation simply serializes original classes and serializes objects along with those classes.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DefaultKryoSerializationHelper implements KryoSerializationHelper {
	public static final DefaultKryoSerializationHelper INSTANCE = new DefaultKryoSerializationHelper();

	@Override
	public void writeSerializable(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull Serializable serializable) {
		kryo.writeClassAndObject(output, serializable);
	}

	@Override
	public void writeObject(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull Serializable serializable) {
		kryo.writeObject(output, serializable);
	}

	@Override
	public void writeOptionalSerializable(@Nonnull Kryo kryo, @Nonnull Output output, @Nullable Serializable serializable) {
		final Class<? extends Serializable> type = serializable != null ? serializable.getClass() : Serializable.class;
		kryo.writeClass(output, type);
		kryo.writeObjectOrNull(output, serializable, type);
	}

	@Override
	public Serializable readSerializable(@Nonnull Kryo kryo, @Nonnull Input input) {
		final Class<? extends Serializable> serializableClass = readSerializableClass(kryo, input);
		return kryo.readObject(input, serializableClass);
	}

	@Override
	public <T> T readObject(@Nonnull Kryo kryo, @Nonnull Input input, @Nonnull Class<T> type) {
		return kryo.readObject(input, type);
	}

	@Override
	public Serializable readOptionalSerializable(@Nonnull Kryo kryo, @Nonnull Input input) {
		final Class<? extends Serializable> serializableClass = readSerializableClass(kryo, input);
		return kryo.readObjectOrNull(input, serializableClass);
	}

	@Override
	public <T extends Serializable> void writeSerializableClass(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull Class<T> type) {
		kryo.writeClass(output, type);
	}

	@Override
	public <T extends Serializable> Class<T> readSerializableClass(@Nonnull Kryo kryo, @Nonnull Input input) {
		final Registration registration = kryo.readClass(input);
		@SuppressWarnings("unchecked") final Class<T> type = registration.getType();
		return type.isPrimitive() ? EvitaDataTypes.getWrappingPrimitiveClass(type) : type;
	}
}
