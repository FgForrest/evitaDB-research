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

import com.esotericsoftware.kryo.Kryo;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.serialization.common.ReadOnlyClassResolver;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.storage.kryo.VersionedKryo;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.ReadOnlyKeyCompressor;
import io.evitadb.storage.model.ReadWriteKeyCompressor;
import lombok.Data;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This DTO contains all data that needs to be known when {@link io.evitadb.storage.MemTable} is flushed to the disk.
 * It contains pointer to the last MemTable fragment as well as all class with ids that has been registered during
 * MemTable serialization process. These data needs to be stored elsewhere and used for correct MemTable reinitialization.
 *
 * Descriptor is Evita DB agnostic and can be used separately along with MemTable object without any specifics tied
 * to the Evita objects.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class MemTableDescriptor {
	/**
	 * Descriptor version is incremented with each update. Version is not stored on the disk, it serves only to distinguish
	 * whether there is any change made in the header and whether it needs to be persisted on disk.
	 */
	@Getter private final long version;
	/**
	 * Contains location of the last MemTable fragment for this version of the header / collection.
	 */
	@Nullable private final FileLocation fileLocation;
	/**
	 * Implementation of {@link io.evitadb.api.serialization.KeyCompressor} that allows adding new entries during write.
	 */
	@Nonnull private final ReadWriteKeyCompressor writeKeyCompressor;
	/**
	 * Implementation of {@link io.evitadb.api.serialization.KeyCompressor} that allows only reading existing entries.
	 */
	@Nonnull private final KeyCompressor readOnlyKeyCompressor;
	/**
	 * Implementation of {@link com.esotericsoftware.kryo.ClassResolver} that allows adding new entries during write.
	 */
	@Nonnull private final WritableClassResolver writableClassResolver;
	/**
	 * {@link Kryo} instance used for writing data to the output stream.
	 */
	@Nonnull private final Kryo writeKryo;
	/**
	 * Reference to the function that allows creating new {@link VersionedKryo} instances for reading {@link io.evitadb.storage.MemTable}
	 * contents using up-to-date configuration specified in {@link VersionedKryoKeyInputs}.
	 *
	 * This function is passed from outside in constructor.
	 */
	@Nonnull private final Function<VersionedKryoKeyInputs, VersionedKryo> kryoFactory;
	/**
	 * Reference to the function that allows creating new {@link VersionedKryo} instances for reading {@link io.evitadb.storage.MemTable}.
	 * This is internal object that wraps {@link #kryoFactory} and allows only to propagate passed version that reflects
	 * the key changes in {@link VersionedKryoKeyInputs}.
	 */
	@Nonnull private final Function<Long, VersionedKryo> readKryoFactory;

	public MemTableDescriptor(@Nonnull CatalogEntityHeader catalogEntityHeader, @Nonnull Function<VersionedKryoKeyInputs, VersionedKryo> kryoFactory, boolean transactional) {
		this.version = 1L;
		this.fileLocation = catalogEntityHeader.getMemTableLocation();
		this.kryoFactory = kryoFactory;
		// create writable instances
		this.writeKeyCompressor = new ReadWriteKeyCompressor(catalogEntityHeader.getIdToKeyIndex());
		this.readOnlyKeyCompressor = transactional ? new ReadOnlyKeyCompressor(catalogEntityHeader.getIdToKeyIndex()) : this.writeKeyCompressor;
		this.writableClassResolver = catalogEntityHeader.getRegisteredClasses().isEmpty() ?
			new WritableClassResolver(catalogEntityHeader.getLastUsedClassId()) : new WritableClassResolver(catalogEntityHeader.getRegisteredClasses());
		this.writeKryo = kryoFactory.apply(new VersionedKryoKeyInputs(writeKeyCompressor, writableClassResolver, 1));
		// create read only instances
		this.readKryoFactory = updatedVersion -> kryoFactory.apply(
			new VersionedKryoKeyInputs(
				readOnlyKeyCompressor,
				transactional ? new ReadOnlyClassResolver(catalogEntityHeader.getRegisteredClasses()) : this.writableClassResolver,
				updatedVersion
			)
		);
	}

	public MemTableDescriptor(@Nonnull FileLocation fileLocation, @Nonnull MemTableDescriptor memTableDescriptor) {
		this.version = memTableDescriptor.version + 1;
		this.fileLocation = fileLocation;
		this.kryoFactory = memTableDescriptor.kryoFactory;
		// keep all write instances
		this.writeKeyCompressor = memTableDescriptor.writeKeyCompressor;
		this.readOnlyKeyCompressor = new ReadOnlyKeyCompressor(getCompressedKeys());
		this.writableClassResolver = memTableDescriptor.writableClassResolver;
		this.writeKryo = memTableDescriptor.writeKryo;
		// reset read only instances according to current state of write instances
		this.readKryoFactory = updatedVersion -> kryoFactory.apply(
			new VersionedKryoKeyInputs(
				this.readOnlyKeyCompressor,
				new ReadOnlyClassResolver(getRegisteredClassIds()),
				updatedVersion
			)
		);
	}

	/**
	 * Returns true if there were any changes in {@link VersionedKryoKeyInputs} that require purging kryo pools.
	 */
	public boolean resetDirty() {
		return writeKeyCompressor.resetDirtyFlag() | writableClassResolver.resetDirtyFlag();
	}

	/**
	 * Returns actual contents of the {@link io.evitadb.api.serialization.KeyCompressor} used during write.
	 */
	@Nonnull
	public Map<Integer, Object> getCompressedKeys() {
		return writeKeyCompressor.getKeys();
	}

	/**
	 * Returns actual contents of the {@link com.esotericsoftware.kryo.ClassResolver} used during write.
	 */
	@Nonnull
	public List<ClassId> getRegisteredClassIds() {
		return writableClassResolver.listRecordedClasses();
	}
}
