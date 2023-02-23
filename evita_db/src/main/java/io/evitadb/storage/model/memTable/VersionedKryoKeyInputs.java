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

package io.evitadb.storage.model.memTable;

import com.esotericsoftware.kryo.ClassResolver;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.storage.MemTable.MemTableKryoPool;
import lombok.Data;

/**
 * Class contains all key instances that are necessary for creating {@link io.evitadb.storage.kryo.VersionedKryo} object.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class VersionedKryoKeyInputs {
	/**
	 * Key compressor holds index ids to keys used in serialized objects.
	 * See {@link KeyCompressor} documentation for more informations.
	 */
	private final KeyCompressor keyCompressor;
	/**
	 * Class resolver holds index of classes and their assigned ids that were used in object serialization.
	 */
	private final ClassResolver classResolver;
	/**
	 * Version holds information used in {@link io.evitadb.storage.kryo.VersionedKryo} instance and this version
	 * server to allow discarding obsolete Kryo instances in {@link MemTableKryoPool#expireAllPreviouslyCreated()}
	 */
	private final long version;
}
