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
import org.rocksdb.*;

import javax.annotation.Nonnull;
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
 * <p>
 * Then creates new RocksDB database and writes data using Kryo to the database.
 * Then opens RocksDB in read-only mode and reads all data back and compares them with the original.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public class RocksDbReadWrite implements TestConstants {
	public static final ReflectionLookup REFLECTION_LOOKUP = new ReflectionLookup(ReflectionCachingBehaviour.CACHE);

	public static void main(String[] args) throws IOException, RocksDBException {
		final RocksDbReadWrite instance = new RocksDbReadWrite();
		instance.write("senesi");
		instance.read("senesi");
	}

	public void write(String catalogName) throws IOException, RocksDBException {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final File path = getDataDirectory().resolve(catalogName + "_rocksdb").toFile();
		if (path.exists()) {
			FileUtils.deleteDirectory(path);
		}
		path.mkdirs();

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting writing process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final RocksDbEntityWriter entityWriter = new RocksDbEntityWriter(path);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityWriter
		);

		log.info("Persistence done in " + StringUtils.formatPreciseNano(entityWriter.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityWriter.getRecordCount(), entityWriter.getDuration()) + ")");
	}

	private void storeKey(Transaction transaction, ByteBuffer key, ByteBuffer value, String keyName, Consumer<ByteBuffer> valueWriter) throws RocksDBException {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).flip();
		value.clear();
		valueWriter.accept(value);
		value.flip();
		transaction.put(toByteArray(key), toByteArray(value));
	}

	private void storeKey(Transaction transaction, ByteBuffer key, ByteBuffer value, String keyName, Integer keyId, Consumer<ByteBuffer> valueWriter) throws RocksDBException {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).putInt(keyId).flip();
		value.clear();
		valueWriter.accept(value);
		value.flip();

		transaction.put(toByteArray(key), toByteArray(value));
	}

	@Nonnull
	private byte[] toByteArray(ByteBuffer byteBuffer) {
		final int keySize = byteBuffer.remaining();
		final byte[] keyArray = new byte[keySize];
		byteBuffer.get(keyArray);
		return keyArray;
	}

	public void read(String catalogName) {
		// We need a storage directory first.
		// The path cannot be on a remote file system.
		final File path = getDataDirectory().resolve(catalogName + "_rocksdb").toFile();
		Assert.isTrue(path.exists(), "Folder " + path.getAbsolutePath() + " doesn't exists!");

		final GenericSerializedCatalogReader reader = new GenericSerializedCatalogReader();
		log.info("Starting reading process ...");
		// read serialized data from the directory and insert them to Evita DB implementation
		final RocksDbEntityReader entityReader = new RocksDbEntityReader(path);
		reader.read(
			catalogName, getDataDirectory().resolve(catalogName),
			entityReader
		);

		log.info("All entities read in " + StringUtils.formatPreciseNano(entityReader.getDuration()) + " (" + StringUtils.formatRequestsPerSec(entityReader.getRecordCount(), entityReader.getDuration()) + ")");
	}

	public Options createRocksDbOptions(boolean createIfMissing) {
		return new Options().setCreateIfMissing(createIfMissing).setCompressionOptions(new CompressionOptions().setEnabled(false));
	}

	private <T> T getKey(RocksDB rocksDB, ByteBuffer key, String keyName, Function<byte[], T> valueReader) throws RocksDBException {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).flip();
		final byte[] value = rocksDB.get(toByteArray(key));
		return valueReader.apply(value);
	}

	private <T> T getKey(RocksDB rocksDB, ByteBuffer key, String keyName, Integer keyId, Function<byte[], T> valueReader) throws RocksDBException {
		key.clear();
		key.put(keyName.getBytes(StandardCharsets.UTF_8)).putInt(keyId).flip();
		final byte[] value = rocksDB.get(toByteArray(key));
		return valueReader.apply(value);
	}

	private class RocksDbEntityWriter implements EntityConsumer {
		private final int BATCH_SIZE = 1000;
		private final AtomicLong duration = new AtomicLong();
		private final AtomicInteger recCounter = new AtomicInteger();
		private final File path;
		private final ByteBuffer key;
		private final ByteBuffer val;
		private final List<Entity> batch = new ArrayList<>(BATCH_SIZE);
		private TransactionDB txnDb;
		private Kryo entityKryo;
		private EntitySchema schema;

		public RocksDbEntityWriter(File path) {
			this.path = path;
			this.key = allocateDirect(1_024);
			this.val = allocateDirect(4_194_304);
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			final long start = System.nanoTime();
			try (final Options options = createRocksDbOptions(true)) {
				final TransactionDBOptions txnDbOptions = new TransactionDBOptions();
				this.txnDb = TransactionDB.open(options, txnDbOptions, path.getAbsolutePath());

				final WritableClassResolver classResolver = header.getClassResolver();
				final Kryo schemaKryo = KryoFactory.createKryo(classResolver, SchemaKryoConfigurer.INSTANCE);
				this.entityKryo = KryoFactory.createKryo(classResolver, new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header));
				this.schema = schema;

				try (final Transaction transaction = txnDb.beginTransaction(new WriteOptions())) {
					storeKey(
						transaction, key, val, "schema_" + schema.getName(),
						byteBuffer -> schemaKryo.writeObject(new ByteBufferOutput(byteBuffer), schema)
					);
					transaction.commit();
				}
				recCounter.incrementAndGet();
			} catch (RocksDBException e) {
				throw new IllegalStateException(e);
			}

			duration.addAndGet(System.nanoTime() - start);
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			batch.add(entity);
			if (batch.size() == BATCH_SIZE) {
				flush();
			}
			return true;
		}

		private void flush() {
			final long entityStart = System.nanoTime();
			try (final Transaction transaction = txnDb.beginTransaction(new WriteOptions())) {
				for (Entity entity : batch) {
					storeKey(
						transaction, key, val, schema.getName().toString(), entity.getPrimaryKey(),
						byteBuffer -> entityKryo.writeObject(new ByteBufferOutput(byteBuffer), entity)
					);
					recCounter.incrementAndGet();
				}
				transaction.commit();
			} catch (RocksDBException e) {
				throw new IllegalStateException(e);
			}
			duration.addAndGet(System.nanoTime() - entityStart);
		}

		@Override
		public void close() {
			final long endStart = System.nanoTime();
			try {
				flush();
				txnDb.syncWal();
				txnDb.closeE();
			} catch (RocksDBException e) {
				throw new IllegalStateException(e);
			}
			duration.addAndGet(System.nanoTime() - endStart);
		}

		public long getDuration() {
			return duration.get();
		}

		public int getRecordCount() {
			return recCounter.get();
		}
	}

	private class RocksDbEntityReader implements EntityConsumer {
		private final File path;
		private final AtomicLong duration = new AtomicLong();
		private final AtomicInteger recCounter = new AtomicInteger();
		private final ByteBuffer key;
		private RocksDB rocksDB;
		private Kryo entityKryo;

		public RocksDbEntityReader(File path) {
			this.path = path;
			this.key = allocateDirect(1_024);
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			final long start = System.nanoTime();
			try (final Options options = createRocksDbOptions(false)) {
			     this.rocksDB = TransactionDB.openReadOnly(options, path.getAbsolutePath());

				final WritableClassResolver classResolver = header.getClassResolver();
				final Kryo schemaKryo = KryoFactory.createKryo(classResolver, SchemaKryoConfigurer.INSTANCE);
				this.entityKryo = KryoFactory.createKryo(classResolver, new EntityKryoConfigurer(() -> schema, REFLECTION_LOOKUP, header));
				final long schemaStart = System.nanoTime();
				final EntitySchema readSchema = getKey(
					rocksDB, key,
					"schema_" + schema.getName(),
					byteBuffer -> schemaKryo.readObject(new ByteBufferInput(byteBuffer), EntitySchema.class)
				);
				duration.addAndGet(System.nanoTime() - schemaStart);
				recCounter.incrementAndGet();
				if (!Objects.equals(schema, readSchema)) {
					throw new IllegalStateException("Schema doesn't match after deserialization!");
				}
			} catch (RocksDBException e) {
				throw new IllegalStateException(e);
			}
			duration.addAndGet(System.nanoTime() - start);
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			final long entityStart = System.nanoTime();
			try {
				final Entity readEntity = getKey(
					rocksDB, key,
					schema.getName().toString(), entity.getPrimaryKey(),
					byteBuffer -> entityKryo.readObject(new ByteBufferInput(byteBuffer), Entity.class)
				);
				recCounter.incrementAndGet();
				if (!Objects.equals(entity, readEntity)) {
					throw new IllegalStateException("Schema doesn't match after deserialization!");
				}
			} catch (RocksDBException e) {
				throw new IllegalStateException(e);
			}
			duration.addAndGet(System.nanoTime() - entityStart);
			return true;
		}

		@Override
		public void close() {
			final long endStart = System.nanoTime();
			try {
				rocksDB.closeE();
			} catch (RocksDBException e) {
				throw new IllegalStateException(e);
			}
			duration.addAndGet(System.nanoTime() - endStart);
		}

		public long getDuration() {
			return duration.get();
		}

		public int getRecordCount() {
			return recCounter.get();
		}
	}
}
