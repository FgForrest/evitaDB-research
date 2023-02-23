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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.KryoFactory.EntityKryoConfigurer;
import io.evitadb.api.serialization.KryoFactory.SchemaKryoConfigurer;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.test.TestConstants;
import io.evitadb.test.snapshot.EntityConsumer;
import io.evitadb.test.snapshot.GenericSerializedCatalogReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This test reads production data from file system using Kryo.
 * <p>
 * Then creates new file database and writes data using Kryo to the database in a single pass.
 * Then opens the file in read-only mode and reads all data back and compares them with the original.
 * This is the base line test that shows the fastest way the data can be serialized and deserialized from disk.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public class BaseLineReadWrite implements TestConstants {
	public static final ReflectionLookup REFLECTION_LOOKUP = new ReflectionLookup(ReflectionCachingBehaviour.CACHE);

	public static void main(String[] args) throws IOException {
		final BaseLineReadWrite instance = new BaseLineReadWrite();
		instance.write("senesi");
		instance.read("senesi");
	}

	public void write(String catalogName) throws IOException {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final Path pathToDirectory = getDataDirectory().resolve(catalogName + "_baseline/");
		final File directory = pathToDirectory.toFile();
		if (directory.exists()) {
			FileUtils.deleteDirectory(directory);
		}
		directory.mkdirs();

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting writing process ...");
		final BaselineEntityWriter entityWriter = new BaselineEntityWriter(pathToDirectory);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityWriter
		);

		log.info("Persistence done in " + StringUtils.formatPreciseNano(entityWriter.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityWriter.getRecordCount(), entityWriter.getDuration()) + ")");
	}

	public void read(String catalogName) throws IOException {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final Path targetFile = getDataDirectory().resolve(catalogName + "_baseline/");
		Assert.isTrue(targetFile.toFile().exists(), "Folder " + targetFile + " doesn't exists!");

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting reading process ...");
		final BaselineEntityReader entityReader = new BaselineEntityReader(targetFile);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityReader
		);

		log.info("All entities read in " + StringUtils.formatPreciseNano(entityReader.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityReader.getRecordCount(), entityReader.getDuration()) + ")");
	}

	private static class BaselineEntityWriter implements EntityConsumer {
		private final AtomicLong duration = new AtomicLong();
		private final AtomicInteger recCounter = new AtomicInteger();
		private final Path targetFile;
		private Output output;
		private Kryo entityKryo;

		public BaselineEntityWriter(Path targetFile) {
			this.targetFile = targetFile;
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			try {
				this.output = new Output(new FileOutputStream(targetFile.resolve(schema.getName() + ".kryo").toFile()), 65_536);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}

			final WritableClassResolver classResolver = header.getClassResolver();
			final Kryo schemaKryo = KryoFactory.createKryo(classResolver, SchemaKryoConfigurer.INSTANCE);
			this.entityKryo = KryoFactory.createKryo(classResolver, new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header));

			final long schemaStart = System.nanoTime();
			schemaKryo.writeObject(output, schema);
			duration.addAndGet(System.nanoTime() - schemaStart);
			recCounter.incrementAndGet();
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			final long entityStart = System.nanoTime();
			entityKryo.writeObject(output, entity);
			duration.addAndGet(System.nanoTime() - entityStart);
			recCounter.incrementAndGet();
			return true;
		}

		@Override
		public void close() {
			final long closeStart = System.nanoTime();
			output.close();
			duration.addAndGet(System.nanoTime() - closeStart);
		}

		public long getDuration() {
			return duration.get();
		}

		public int getRecordCount() {
			return recCounter.get();
		}
	}

	private static class BaselineEntityReader implements EntityConsumer {
		private final AtomicLong duration = new AtomicLong();
		private final AtomicInteger recCounter = new AtomicInteger();
		private final Path targetFile;
		private Input input;
		private Kryo entityKryo;

		public BaselineEntityReader(Path targetFile) {
			this.targetFile = targetFile;
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			try {
				this.input = new Input(new FileInputStream(targetFile.resolve(schema.getName() + ".kryo").toFile()), 65_536);
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}

			final WritableClassResolver classResolver = header.getClassResolver();
			final Kryo schemaKryo = KryoFactory.createKryo(classResolver, SchemaKryoConfigurer.INSTANCE);
			this.entityKryo = KryoFactory.createKryo(classResolver, new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header));

			final long schemaStart = System.nanoTime();
			final EntitySchema readSchema = schemaKryo.readObject(input, EntitySchema.class);
			duration.addAndGet(System.nanoTime() - schemaStart);
			recCounter.incrementAndGet();
			if (!Objects.equals(schema, readSchema)) {
				throw new IllegalStateException("Schema doesn't match after deserialization!");
			}
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			final long entityStart = System.nanoTime();
			final Entity readEntity = entityKryo.readObject(input, Entity.class);
			duration.addAndGet(System.nanoTime() - entityStart);
			recCounter.incrementAndGet();
			if (!Objects.equals(entity, readEntity)) {
				throw new IllegalStateException("Schema doesn't match after deserialization!");
			}
			return true;
		}

		@Override
		public void close() {
			final long closeStart = System.nanoTime();
			input.close();
			duration.addAndGet(System.nanoTime() - closeStart);
		}

		public long getDuration() {
			return duration.get();
		}

		public int getRecordCount() {
			return recCounter.get();
		}
	}
}
