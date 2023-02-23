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

package io.evitadb.spike;

import io.evitadb.api.configuration.StorageOptions;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KryoFactory.EntityKryoConfigurer;
import io.evitadb.api.serialization.KryoFactory.SchemaKryoConfigurer;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.storage.MemTable;
import io.evitadb.storage.ObservableOutputKeeper;
import io.evitadb.storage.kryo.VersionedKryo;
import io.evitadb.storage.kryo.VersionedKryoFactory;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.memTable.FileLocation;
import io.evitadb.storage.model.memTable.MemTableDescriptor;
import io.evitadb.storage.model.memTable.VersionedKryoKeyInputs;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import io.evitadb.storage.model.storageParts.schema.EntitySchemaContainer;
import io.evitadb.test.TestConstants;
import io.evitadb.test.snapshot.EntityConsumer;
import io.evitadb.test.snapshot.GenericSerializedCatalogReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * This test reads production data from file system using Kryo.
 *
 * Then creates new Evita internal storage database and writes data using Kryo to the database.
 * Then opens Evita storage from scratch and reads all data back and compares them with the original.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public class EvitaStorageReadWrite implements TestConstants {
	public static final ReflectionLookup REFLECTION_LOOKUP = new ReflectionLookup(ReflectionCachingBehaviour.CACHE);

	public static void main(String[] args) throws IOException {
		final EvitaStorageReadWrite instance = new EvitaStorageReadWrite();
		final Map<Serializable, FileLocation> memTableLocations = instance.write("senesi");
		instance.read("senesi", memTableLocations);
	}

	public Map<Serializable, FileLocation> write(String catalogName) throws IOException {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final Path path = getDataDirectory().resolve(catalogName + "_evita");
		final File directoryFile = path.toFile();
		if (directoryFile.exists()) {
			FileUtils.deleteDirectory(directoryFile);
		}
		directoryFile.mkdirs();

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting writing process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final EvitaEntityWriteConsumer entityWriter = new EvitaEntityWriteConsumer(path);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityWriter
		);

		log.info("Persistence done in " + StringUtils.formatPreciseNano(entityWriter.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityWriter.getRecordCount(), entityWriter.getDuration()) + ")");
		return entityWriter.memTableLocationIndex;
	}

	public void read(String catalogName, Map<Serializable, FileLocation> memTableLocation) throws IOException {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final Path path = getDataDirectory().resolve(catalogName + "_evita");
		Assert.isTrue(path.toFile().exists(), "Folder " + path.toFile().getAbsolutePath() + " doesn't exists!");

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting reading process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final EvitaEntityReadConsumer entityReader = new EvitaEntityReadConsumer(path, memTableLocation);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityReader
		);

		log.info("All entities read in " + StringUtils.formatPreciseNano(entityReader.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityReader.getRecordCount(), entityReader.getDuration()) + ")");
	}

	@Nonnull
	private StorageOptions createStorageOptions() {
		return new StorageOptions(
			1, 5, 262_144, 1, false, false, false
		);
	}

	@Nonnull
	private Function<VersionedKryoKeyInputs, VersionedKryo> createEntityKryo(MutableCatalogEntityHeader header, EntitySchema schema) {
		return keyInputs -> VersionedKryoFactory.createKryo(
			keyInputs.getVersion(),
			keyInputs.getClassResolver(),
			SchemaKryoConfigurer.INSTANCE.andThen(
				new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header)
			)
		);
	}

	private class EvitaEntityWriteConsumer implements EntityConsumer {
		private final Path path;
		private final AtomicInteger recCounter;
		@Getter private final Map<Serializable, FileLocation> memTableLocationIndex;
		private final AtomicLong duration;
		private EntitySchema schema;
		private MemTable memTable;
		private ObservableOutputKeeper outputKeeper;

		public EvitaEntityWriteConsumer(Path path) {
			this.path = path;
			this.recCounter = new AtomicInteger();
			this.duration = new AtomicLong();
			this.memTableLocationIndex = new HashMap<>();
		}

		public long getDuration() {
			return duration.get();
		}

		public int getRecordCount() {
			return recCounter.get();
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			this.schema = schema;
			final long subStart = System.nanoTime();
			final StorageOptions storageOptions = createStorageOptions();
			this.outputKeeper = new ObservableOutputKeeper(storageOptions);
			this.outputKeeper.prepare();

			this.memTable = new MemTable(
				path.resolve(schema.getName() + ".kryo"),
				new MemTableDescriptor(
					new CatalogEntityHeader(
						header.getEntityType(),
						1,
						header.getRecordCount(),
						0,
						0,
						null,
						header.getIdToKeyIndex(),
						header.listRecordedClasses(),
						null,
						Collections.emptyList()
					),
					createEntityKryo(header, schema),
					false
				),
				storageOptions,
				outputKeeper
			);

			memTable.put(0L, new EntitySchemaContainer(schema));
			duration.addAndGet(System.nanoTime() - subStart);
			recCounter.incrementAndGet();
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			final long subStart = System.nanoTime();
			// BEWARE - THIS NO LONGER STORES ENTIRE ENTITY!
			final EntityBodyStoragePart cnt = new EntityBodyStoragePart(
				entity.getVersion(), entity.getPrimaryKey(), entity.getHierarchicalPlacement(), entity.getLocales(),
				entity.getAttributeLocales(), entity.getAssociatedDataKeys()
			);
			memTable.put(0L, cnt);
			duration.addAndGet(System.nanoTime() - subStart);
			recCounter.incrementAndGet();
			return true;
		}

		@Override
		public void close() {
			final long subStart = System.nanoTime();
			memTableLocationIndex.put(schema.getName(), memTable.close());
			outputKeeper.free();
			duration.addAndGet(System.nanoTime() - subStart);
		}
	}

	private class EvitaEntityReadConsumer implements EntityConsumer {
		private final Path path;
		private final Map<Serializable, FileLocation> memTableLocation;
		private final AtomicLong duration;
		private final AtomicInteger recCounter;
		private MemTable memTable;
		private ObservableOutputKeeper outputKeeper;

		public EvitaEntityReadConsumer(Path path, Map<Serializable, FileLocation> memTableLocation) {
			this.path = path;
			this.memTableLocation = memTableLocation;
			this.duration = new AtomicLong();
			this.recCounter = new AtomicInteger();
		}

		public long getDuration() {
			return duration.get();
		}

		public int getRecordCount() {
			return recCounter.get();
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			final long memTableStart = System.nanoTime();
			final StorageOptions storageOptions = createStorageOptions();

			this.outputKeeper = new ObservableOutputKeeper(storageOptions);
			this.outputKeeper.prepare();

			this.memTable = new MemTable(
				path.resolve(schema.getName() + ".kryo"),
				new MemTableDescriptor(
					new CatalogEntityHeader(
						header.getEntityType(),
						1,
						header.getRecordCount(),
						1,
						1,
						memTableLocation.get(schema.getName()),
						header.getIdToKeyIndex(),
						header.listRecordedClasses(),
						null,
						Collections.emptyList()
					),
					createEntityKryo(header, schema),
					false
				),
				storageOptions,
				outputKeeper
			);
			this.duration.addAndGet(System.nanoTime() - memTableStart);

			final long schemaStart = System.nanoTime();
			final EntitySchema readSchema = memTable.get(1, EntitySchema.class);
			if (schema.differsFrom(readSchema)) {
				throw new IllegalStateException("Schema doesn't match after deserialization!");
			}
			this.recCounter.incrementAndGet();
			this.duration.addAndGet(System.nanoTime() - schemaStart);
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			final long entityStart = System.nanoTime();
			final Entity readEntity = memTable.get(entity.getPrimaryKey(), Entity.class);
			this.duration.addAndGet(System.nanoTime() - entityStart);
			this.recCounter.incrementAndGet();
			if (entity.differsFrom(readEntity)) {
				throw new IllegalStateException("Schema doesn't match after deserialization!");
			}
			return true;
		}

		@Override
		public void close() {
			final long endStart = System.nanoTime();
			this.memTable.close();
			this.outputKeeper.free();
			this.duration.addAndGet(System.nanoTime() - endStart);
		}
	}
}
