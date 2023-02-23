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

package io.evitadb.storage.kryo;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.serialization.KryoFactory.EntityKryoConfigurer;
import io.evitadb.api.serialization.KryoFactory.SchemaKryoConfigurer;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;

import java.util.function.Consumer;

import static io.evitadb.api.serialization.KryoFactory.initializeKryo;

/**
 * This class mimics {@link io.evitadb.api.serialization.KryoFactory} but produces {@link VersionedKryo} instances
 * instead of pure {@link Kryo} ones.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class VersionedKryoFactory {

	private VersionedKryoFactory() {}

	/**
	 * Method creates default VersionedKryo instance ({@link #createKryo(long, ClassResolver)} and created instance hands to passed consumer
	 * implementation. This method should play well with {@link SchemaKryoConfigurer} and {@link EntityKryoConfigurer}
	 * consumers defined by this class.
	 *
	 * @param version
	 * @param classResolver
	 * @param andThen
	 * @return
	 */
	public static VersionedKryo createKryo(long version, ClassResolver classResolver, Consumer<Kryo> andThen) {
		final VersionedKryo kryo = createKryo(version, classResolver);
		andThen.accept(kryo);
		return kryo;
	}

	/**
	 * Method creates default VersionedKryo instance ({@link #createKryo(long, ClassResolver)}) and created instance hands to passed consumer
	 * implementation. This method should play well with {@link SchemaKryoConfigurer} and {@link EntityKryoConfigurer}
	 * consumers defined by this class.
	 *
	 * @param version
	 * @param classResolver
	 * @param andThen
	 * @return
	 */
	public static VersionedKryo createKryo(long version, ClassResolver classResolver, KryoSerializationHelper kryoSerializationHelper, Consumer<Kryo> andThen) {
		final VersionedKryo kryo = initializeKryo(new VersionedKryo(version, classResolver, null), kryoSerializationHelper);
		andThen.accept(kryo);
		return kryo;
	}

	/**
	 * Method creates default VersionedKryo instance with all default serializers registered. This instance of the VersionedKryo
	 * should be able to (de)serialize all {@link EvitaDataTypes#getSupportedDataTypes()} data types.
	 *
	 * @param version
	 * @param classResolver
	 * @return
	 */
	public static VersionedKryo createKryo(long version, ClassResolver classResolver) {
		return initializeKryo(new VersionedKryo(version, classResolver, null), new DefaultKryoSerializationHelper());
	}
	
}
