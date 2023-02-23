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
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
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
import org.lmdbjava.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.ByteBuffer.allocateDirect;

/**
 * This test reads production data from file system using Kryo.
 *
 * Then creates new LMDB database and writes data using Kryo to the database.
 * Then opens LMDB in read-only mode and reads all data back and compares them with the original.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public class LmdbReadWrite implements TestConstants {
	public static final ReflectionLookup REFLECTION_LOOKUP = new ReflectionLookup(ReflectionCachingBehaviour.CACHE);

	public static void main(String[] args) throws IOException {
		final LmdbReadWrite instance = new LmdbReadWrite();
		instance.write("senesi");
		instance.read("senesi");
	}

	public void write(String catalogName) throws IOException {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final File path = getDataDirectory().resolve(catalogName + "_lmdbi").toFile();
		if (path.exists()) {
			FileUtils.deleteDirectory(path);
		}
		path.mkdirs();

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting reading process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final LmdbEntityWriter lmdbConsumer = new LmdbEntityWriter(path, catalogName);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			lmdbConsumer
		);

		log.info("Persistence done in " + StringUtils.formatPreciseNano(lmdbConsumer.getDuration()) + " (" + StringUtils.formatRequestsPerSec(lmdbConsumer.getRecordCount(), lmdbConsumer.getDuration()) + ")");
	}

	private void storeKey(Dbi<ByteBuffer> db, Txn<ByteBuffer> txn, ByteBuffer key, ByteBuffer value, String keyName, Consumer<ByteBuffer> valueWriter) {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).flip();
		value.clear();
		valueWriter.accept(value);
		value.flip();
		db.put(txn, key, value);
	}

	private void storeKey(Dbi<ByteBuffer> db, Txn<ByteBuffer> txn, ByteBuffer key, ByteBuffer value, String keyName, Integer keyId, Consumer<ByteBuffer> valueWriter) {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).putInt(keyId).flip();
		value.clear();
		valueWriter.accept(value);
		value.flip();

		db.put(txn, key, value);
	}

	public void read(String catalogName) {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final File path = getDataDirectory().resolve(catalogName + "_lmdbi").toFile();
		Assert.isTrue(path.exists(), "Folder " + path.getAbsolutePath() + " doesn't exists!");

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting reading process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final LmdbEntityReader entityReader = new LmdbEntityReader(path, catalogName);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityReader
		);

		log.info("All entities read in " + StringUtils.formatPreciseNano(entityReader.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityReader.getRecordCount(), entityReader.getDuration()) + ")");
	}

	private <T> T getKey(Dbi<ByteBuffer> db, Txn<ByteBuffer> txn, ByteBuffer key, String keyName, Function<ByteBuffer, T> valueReader) {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).flip();
		// Now store it. Dbi.put() internally begins and commits a transaction (Txn).
		final ByteBuffer value = db.get(txn, key);
		return valueReader.apply(value);
	}

	private <T> T getKey(Dbi<ByteBuffer> db, Txn<ByteBuffer> txn, ByteBuffer key, String keyName, Integer keyId, Function<ByteBuffer, T> valueReader) {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).putInt(keyId).flip();
		// Now store it. Dbi.put() internally begins and commits a transaction (Txn).
		final ByteBuffer value = db.get(txn, key);
		return valueReader.apply(value);
	}

	private class LmdbEntityWriter implements EntityConsumer {
		private final int BATCH_SIZE = 1000;
		private final AtomicInteger recCounter = new AtomicInteger();
		private final AtomicLong duration = new AtomicLong();
		private final List<Entity> batch = new ArrayList<>(BATCH_SIZE);
		private final File path;
		private final String catalogName;
		private Env<ByteBuffer> env;
		private Dbi<ByteBuffer> db;
		private ByteBuffer key;
		private ByteBuffer val;
		private EntitySchema schema;
		private Kryo entityKryo;

		public LmdbEntityWriter(File path, String catalogName) {
			this.path = path;
			this.catalogName = catalogName;
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			final long start = System.nanoTime();
			// We always need an Env. An Env owns a physical on-disk storage file. One
			// Env can store many different databases (ie sorted maps).
			this.env = Env.create(ByteBufferProxy.PROXY_OPTIMAL)
				// LMDB also needs to know how large our DB might be. Over-estimating is OK.
				.setMapSize(1_000_000_000L)
				// LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
				.setMaxDbs(1)
				// Now let's open the Env. The same path can be concurrently opened and
				// used in different processes, but do not open the same path twice in
				// the same process at the same time.
				.open(path);

			// We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
			// MDB_CREATE flag causes the DB to be created if it doesn't already exist.
			this.db = env.openDbi(catalogName, DbiFlags.MDB_CREATE);
			this.key = allocateDirect(env.getMaxKeySize());
			this.val = allocateDirect(4_194_304);

			try (final Txn<ByteBuffer> txn = env.txnWrite()) {
				final WritableClassResolver classResolver = header.getClassResolver();
				final Kryo schemaKryo = KryoFactory.createKryo(classResolver, SchemaKryoConfigurer.INSTANCE);
				this.entityKryo = KryoFactory.createKryo(classResolver, new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header));
				storeKey(
					db, txn, key, val, "schema_" + schema.getName(),
					byteBuffer -> schemaKryo.writeObject(new ByteBufferOutput(byteBuffer), schema)
				);
				recCounter.incrementAndGet();
				txn.commit();
			}
			this.duration.addAndGet(System.nanoTime() - start);
			this.schema = schema;
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			batch.add(entity);
			if (batch.size() == BATCH_SIZE) {
				flush();
			}
			return true;
		}

		@Override
		public void close() {
			final long endStart = System.nanoTime();
			flush();
			env.close();
			duration.addAndGet(System.nanoTime() - endStart);
		}

		public int getRecordCount() {
			return recCounter.get();
		}

		public long getDuration() {
			return duration.get();
		}

		private void flush() {
			// We want to store some data, so we will need a direct ByteBuffer.
			// Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
			// Values can be larger.
			final long subStart = System.nanoTime();
			try (final Txn<ByteBuffer> txn = env.txnWrite()) {
				for (Entity entity : batch) {
					storeKey(
						db, txn, key, val, schema.getName().toString(), entity.getPrimaryKey(),
						byteBuffer -> entityKryo.writeObject(new ByteBufferOutput(byteBuffer), entity)
					);
					recCounter.incrementAndGet();
				}
				txn.commit();
			}
			duration.addAndGet(System.nanoTime() - subStart);
			batch.clear();
		}
	}

	private class LmdbEntityReader implements EntityConsumer {
		private final AtomicInteger recCounter = new AtomicInteger();
		private final AtomicLong duration = new AtomicLong();
		private final File path;
		private final String catalogName;
		private Env<ByteBuffer> env;
		private Dbi<ByteBuffer> db;
		private ByteBuffer key;
		private Kryo entityKryo;

		public LmdbEntityReader(File path, String catalogName) {
			this.path = path;
			this.catalogName = catalogName;
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			// We always need an Env. An Env owns a physical on-disk storage file. One
			// Env can store many different databases (ie sorted maps).
			this.env = Env.open(path, 1_000, EnvFlags.MDB_RDONLY_ENV);
			// We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
			// MDB_CREATE flag causes the DB to be created if it doesn't already exist.
			final long start = System.nanoTime();
			this.db = env.openDbi(catalogName, DbiFlags.MDB_CREATE);
			this.key = allocateDirect(env.getMaxKeySize());

			// We want to store some data, so we will need a direct ByteBuffer.
			// Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
			// Values can be larger.
			try (Txn<ByteBuffer> txn = env.txnRead()) {
				final Kryo schemaKryo = KryoFactory.createKryo(header.getClassResolver(), SchemaKryoConfigurer.INSTANCE);
				recCounter.incrementAndGet();
				final EntitySchema readSchema = getKey(
					db, txn, key,
					"schema_" + schema.getName(),
					byteBuffer -> schemaKryo.readObject(new ByteBufferInput(byteBuffer), EntitySchema.class)
				);
				if (!Objects.equals(schema, readSchema)) {
					throw new IllegalStateException("Schema doesn't match after deserialization!");
				}
			}
			duration.addAndGet(System.nanoTime() - start);

			this.entityKryo = KryoFactory.createKryo(header.getClassResolver(), new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header));
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			// We want to store some data, so we will need a direct ByteBuffer.
			// Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
			// Values can be larger.
			try (Txn<ByteBuffer> txn = env.txnRead()) {
				final long entityStart = System.nanoTime();
				final Entity readEntity = getKey(
					db, txn, key,
					schema.getName().toString(), entity.getPrimaryKey(),
					byteBuffer -> entityKryo.readObject(new ByteBufferInput(byteBuffer), Entity.class)
				);
				duration.addAndGet(System.nanoTime() - entityStart);
				recCounter.incrementAndGet();
				if (!Objects.equals(entity, readEntity)) {
					throw new IllegalStateException("Schema doesn't match after deserialization!");
				}
			}
			return true;
		}

		@Override
		public void close() {
			final long endStart = System.nanoTime();
			env.close();
			duration.addAndGet(System.nanoTime() - endStart);
		}

		public int getRecordCount() {
			return recCounter.get();
		}

		public long getDuration() {
			return duration.get();
		}

	}
}
