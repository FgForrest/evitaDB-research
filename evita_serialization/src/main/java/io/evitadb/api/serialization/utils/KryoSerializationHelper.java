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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This interface defines set of methods that work with unknown {@link Serializable} classes and objects. Although the interface
 * has single implementation, there is another one, that is used in production data exporter that strips out classes
 * that are project specific and replaces them with the String. This is necessary so that these production data can
 * be used in integration tests of Evita project where specific project classes are not available on classpath.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface KryoSerializationHelper {

	/**
	 * Writes Serializable object (i.e. some implementation of the serializable object) along with the class
	 * to the Kryo output.
	 * <p>
	 * Serializable object should be read by {@link #readSerializable(Kryo, Input)} method.
	 */
	void writeObject(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull Serializable serializable);

	/**
	 * Writes Serializable object (i.e. some implementation of the serializable object) along with the class
	 * to the Kryo output.
	 * <p>
	 * Serializable object should be read by {@link #readSerializable(Kryo, Input)} method.
	 */
	void writeSerializable(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull Serializable serializable);

	/**
	 * Writes Serializable object (i.e. some implementation of the serializable object) to the Kryo output.
	 * When Serializable object is null only Serializable type is written as a class type, followed by null value.
	 * <p>
	 * Serializable object should be read by {@link #readOptionalSerializable(Kryo, Input)} method.
	 */
	void writeOptionalSerializable(@Nonnull Kryo kryo, @Nonnull Output output, @Nullable Serializable serializable);

	/**
	 * Reads concrete Serializable implementation from the input stream.
	 */
	<T> T readObject(@Nonnull Kryo kryo, @Nonnull Input input, @Nonnull Class<T> type);

	/**
	 * Reads concrete Serializable implementation from the input stream.
	 */
	Serializable readSerializable(@Nonnull Kryo kryo, @Nonnull Input input);

	/**
	 * Reads concrete Serializable implementation from the stream or NULL if there is none Serializable value found.
	 */
	Serializable readOptionalSerializable(@Nonnull Kryo kryo, @Nonnull Input input);

	/**
	 * Writes class of the serializable object.
	 * @param kryo
	 * @param output
	 * @param type
	 * @param <T>
	 */
	<T extends Serializable> void writeSerializableClass(@Nonnull Kryo kryo, @Nonnull Output output, @Nonnull Class<T> type);

	/**
	 * This method solves the problem, that Kryo signalizes basic Java classes as primitive classes even on places
	 * where originally wrapper class was serialized to the output stream. This method ensures that for primitive
	 * types the wrapper type will be returned.
	 */
	<T extends Serializable> Class<T> readSerializableClass(@Nonnull Kryo kryo, @Nonnull Input input);
}
