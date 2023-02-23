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

package io.evitadb.test.snapshot;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.KryoFactory.EntityKryoConfigurer;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.serialization.MutableCatalogHeader;
import io.evitadb.api.serialization.io.MutableCatalogHeaderSerializationService;
import io.evitadb.api.serialization.io.SchemaSerializationService;
import io.evitadb.api.storage.exception.InvalidStoragePathException;
import io.evitadb.api.storage.exception.UnexpectedCatalogContentsException;
import io.evitadb.api.storage.exception.UnexpectedIOException;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.api.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static io.evitadb.api.utils.FileUtils.formatFileName;

/**
 * This is utility class that can read contents of the production catalog from the target directory. Catalog is stored
 * in implementation agnostic form.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
public class GenericSerializedCatalogReader {
	private final ReflectionLookup reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.CACHE);
	/**
	 * This predicate is used to filter entity types that should be read from the serialized storage contents.
	 */
	private final Predicate<Serializable> entityTypePredicate;

	public GenericSerializedCatalogReader() {
		// process all entities
		this.entityTypePredicate = entityType -> true;
	}

	public GenericSerializedCatalogReader(Predicate<Serializable> entityTypePredicate) {
		this.entityTypePredicate = entityTypePredicate;
	}

	/**
	 * Deserializes catalog from persistent storage. As of now the catalog is read entirely from scratch from
	 * `storageDirectory`. Contents of the directory must contain previously serialized catalog of the identical
	 * catalog otherwise exception is thrown.
	 *
	 * @param catalogName      - name of the catalog, must conform to the name of serialized catalog
	 * @param storageDirectory - directory with serialized catalog contents
	 * @param entityConsumer   - lambda that stores entities into the target implementation
	 * @throws UnexpectedCatalogContentsException when directory contains different catalog data or no data at all
	 * @throws UnexpectedIOException              in case of any unknown IOException
	 */
	public void read(@Nonnull String catalogName, @Nonnull Path storageDirectory, @Nonnull EntityConsumer entityConsumer) {
		final long start = System.nanoTime();
		try {
			verifyMainDirectory(storageDirectory);
			final MutableCatalogHeader header = readHeader(storageDirectory);
			Assert.isTrue(
				header.getCatalogName().equals(catalogName),
				() -> new UnexpectedCatalogContentsException(
					"Directory " + storageDirectory + " contains data of " + header.getCatalogName() +
						" catalog. Cannot load catalog " + catalogName + " from this directory!"
				)
			);
			final Map<Serializable, EntitySchema> schemaIndex = new HashMap<>();
			for (Serializable entityType : header.getEntityTypesIndex().keySet()) {
				if (entityTypePredicate.test(entityType)) {
					final MutableCatalogEntityHeader entityTypeHeader = header.getEntityTypeHeader(entityType);
					final EntitySchema schema = readEntitySchema(
						entityTypeHeader, entityType, storageDirectory,
						new TimeMeasuringEntityConsumer(entityConsumer, entityTypeHeader)
					);
					schemaIndex.put(entityType, schema);
				}
			}
			for (Serializable entityType : header.getEntityTypesIndex().keySet()) {
				if (entityTypePredicate.test(entityType)) {
					final MutableCatalogEntityHeader entityTypeHeader = header.getEntityTypeHeader(entityType);
					final EntitySchema entitySchema = schemaIndex.get(entityType);
					Assert.notNull(entitySchema, "Schema is expected to be loaded already!");
					readEntityCollection(
						entityTypeHeader, entitySchema, storageDirectory,
						new TimeMeasuringEntityConsumer(entityConsumer, entityTypeHeader)
					);
				}
			}
		} finally {
			log.info("Catalog processed in " + StringUtils.formatNano(System.nanoTime() - start));
		}
	}

	/**
	 * Deserializes single entity from persistent storage and returns lazy iterator that reads single entity
	 * per {@link Iterator#next()} call. As of now the catalog is read entirely from scratch from
	 * `storageDirectory`. Contents of the directory must contain previously serialized catalog of the identical
	 * catalog otherwise exception is thrown.
	 *
	 * @param catalogName      - name of the catalog, must conform to the name of serialized catalog
	 * @param storageDirectory - directory with serialized catalog contents
	 * @throws UnexpectedCatalogContentsException when directory contains different catalog data or no data at all
	 * @throws UnexpectedIOException              in case of any unknown IOException
	 */
	public Iterator<Entity> read(@Nonnull String catalogName, @Nonnull Path storageDirectory, @Nonnull Serializable entityType) {
		final long start = System.nanoTime();
		try {
			verifyMainDirectory(storageDirectory);
			final MutableCatalogHeader header = readHeader(storageDirectory);
			Assert.isTrue(
				header.getCatalogName().equals(catalogName),
				() -> new UnexpectedCatalogContentsException(
					"Directory " + storageDirectory + " contains data of " + header.getCatalogName() +
						" catalog. Cannot load catalog " + catalogName + " from this directory!"
				)
			);
			final MutableCatalogEntityHeader entityTypeHeader = header.getEntityTypeHeader(entityType);
			final SchemaSerializationService schemaSerializationService = new SchemaSerializationService(entityTypeHeader);
			final EntitySchema schema;
			try (final ByteBufferInput input = new ByteBufferInput(new FileInputStream(storageDirectory.resolve(formatFileName(entityType) + "_schema.kryo").toFile()))) {
				schema = schemaSerializationService.deserialize(input);
			} catch (IOException e) {
				throw new UnexpectedIOException("Failed to load schema of " + entityType + " collection!", e);
			}
			try {
				final ByteBufferInput input = new ByteBufferInput(new FileInputStream(storageDirectory.resolve(formatFileName(entityType) + ".kryo").toFile()));
				final Kryo kryo = KryoFactory.createKryo(entityTypeHeader.getClassResolver(), new EntityKryoConfigurer(() -> schema, reflectionLookup, entityTypeHeader));
				final int entityCount = input.readInt();
				return new EntityIterator(entityCount, kryo, input);
			} catch (IOException e) {
				throw new UnexpectedIOException("Failed to load " + entityType + " collection!", e);
			}
		} finally {
			log.info("Catalog processed in " + StringUtils.formatNano(System.nanoTime() - start));
		}
	}

	/**
	 * Reads catalog header that contains data required to successfully deserialize contents of entities in the catalog.
	 * It contains list and ids of used classes, shared dictionary of keys used in attributes and associated data and
	 * other important things. Catalog header must be read first in order to have information for initialization of
	 * Kryo instance that is used to read entities and schema.
	 */
	private MutableCatalogHeader readHeader(@Nonnull Path storageDirectory) {
		final MutableCatalogHeaderSerializationService headerSerializationService = new MutableCatalogHeaderSerializationService();
		final File headerFile = storageDirectory.resolve("header.kryo").toFile();
		Assert.isTrue(
			headerFile.exists(),
			() -> new UnexpectedCatalogContentsException("Directory " + storageDirectory + " contains no file named header.kryo!")
		);
		try (final FileInputStream is = new FileInputStream(headerFile)) {
			final MutableCatalogHeader header = headerSerializationService.deserialize(is);
			logStatistics(header, " is being loaded");
			return header;
		} catch (IOException e) {
			throw new UnexpectedIOException("Failed to store Evita header file! All data are worthless :(", e);
		}
	}

	/**
	 * Reads entity schema that describes particular entity.
	 */
	private EntitySchema readEntitySchema(@Nonnull MutableCatalogEntityHeader entityTypeHeader, @Nonnull Serializable entityType, @Nonnull Path storageDirectory, EntityConsumer entityConsumer) {
		final SchemaSerializationService schemaSerializationService = new SchemaSerializationService(entityTypeHeader);
		final EntitySchema schema;
		try (final ByteBufferInput input = new ByteBufferInput(new FileInputStream(storageDirectory.resolve(formatFileName(entityType) + "_schema.kryo").toFile()))) {
			schema = schemaSerializationService.deserialize(input);
			entityConsumer.setup(entityTypeHeader, schema);
		} catch (IOException e) {
			throw new UnexpectedIOException("Failed to load schema of " + entityType + " collection!", e);
		}
		return schema;
	}

	/**
	 * Reads collection of entities. Schema is expected to be already initialized.
	 */
	private int readEntityCollection(@Nonnull MutableCatalogEntityHeader entityTypeHeader, @Nonnull EntitySchema schema, @Nonnull Path storageDirectory, @Nonnull EntityConsumer entityConsumer) {
		final Serializable entityType = schema.getName();
		try (final ByteBufferInput input = new ByteBufferInput(new FileInputStream(storageDirectory.resolve(formatFileName(entityType) + ".kryo").toFile()))) {
			final Kryo kryo = KryoFactory.createKryo(entityTypeHeader.getClassResolver(), new EntityKryoConfigurer(() -> schema, reflectionLookup, entityTypeHeader));
			final int entityCount = input.readInt();
			for (int i = 0; i < entityCount; i++) {
				final Entity entity = kryo.readObject(input, Entity.class);
				if (!entityConsumer.accept(schema, entity)) {
					break;
				}
			}

			return entityCount;
		} catch (IOException e) {
			throw new UnexpectedIOException("Failed to load " + entityType + " collection!", e);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to import " + entityType + " collection!", e);
		} finally {
			entityConsumer.close();
		}
	}

	/**
	 * Logs statistics about the catalog and entities in it to the logger.
	 */
	private void logStatistics(@Nonnull MutableCatalogHeader catalogHeader, @Nonnull String operation) {
		if (log.isInfoEnabled()) {
			final StringBuilder stats = new StringBuilder("Catalog " + catalogHeader.getCatalogName() + " " + operation + ". It contains:");
			for (MutableCatalogEntityHeader catalogEntityHeader : catalogHeader.getEntityTypesIndex().values()) {
				stats.append("\n\t- ")
					.append(catalogEntityHeader.getEntityType())
					.append(" (")
					.append(catalogEntityHeader.getRecordCount())
					.append(")");
			}
			log.info(stats.toString());
		}
	}

	/**
	 * Check whether target directory exists and whether it is really directory.
	 */
	private void verifyMainDirectory(Path storageDirectory) {
		final File storageDirectoryFile = storageDirectory.toFile();
		if (!storageDirectoryFile.exists()) {
			//noinspection ResultOfMethodCallIgnored
			storageDirectoryFile.mkdirs();
		}
		Assert.isTrue(storageDirectoryFile.exists(), () -> new InvalidStoragePathException("Storage path doesn't exist: " + storageDirectory));
		Assert.isTrue(storageDirectoryFile.isDirectory(), () -> new InvalidStoragePathException("Storage path doesn't represent a directory: " + storageDirectory));
	}

	/**
	 * This wrapper implementation just recalls the delegate but records the time spent in each method.
	 */
	@RequiredArgsConstructor
	private static class TimeMeasuringEntityConsumer implements EntityConsumer {
		private final EntityConsumer delegate;
		private final MutableCatalogEntityHeader entityTypeHeader;
		private final AtomicLong duration = new AtomicLong();
		private final AtomicLong lastCheckpointDuration = new AtomicLong();
		private final AtomicInteger count = new AtomicInteger();

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			final long collectionWritingStart = System.nanoTime();
			delegate.setup(entityTypeHeader, schema);
			duration.addAndGet(System.nanoTime() - collectionWritingStart);
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			final long collectionWritingStart = System.nanoTime();
			final boolean result = delegate.accept(schema, entity);
			duration.addAndGet(System.nanoTime() - collectionWritingStart);
			final int i = count.incrementAndGet();
			if (i % 5000 == 0) {
				final long checkpointDuration = duration.get() - lastCheckpointDuration.get();
				final long recsSec = Math.round((double) 5000 / ((double) checkpointDuration / (double) 1_000_000_000));
				log.info("Processed " + i + " records of " + entityTypeHeader.getEntityType() + " (" + recsSec + " recs/sec)" + " ...");
				lastCheckpointDuration.set(duration.get());
			}
			return result;
		}

		@Override
		public void close() {
			final long collectionWritingStart = System.nanoTime();
			duration.addAndGet(System.nanoTime() - collectionWritingStart);

			final String durationSec = StringUtils.formatNano(duration.get());
			final long recsSec = Math.round((double) count.get() / ((double) duration.get() / (double) 1_000_000_000));
			log.info("Processed " + entityTypeHeader.getEntityType() + " entity collection (" + count.get() + " recs.) in " + durationSec + " (" + recsSec + " recs/sec).");
		}
	}

	public static class EntityIterator implements Iterator<Entity>, AutoCloseable {
		private final int entityCount;
		private final Kryo kryo;
		private final ByteBufferInput input;
		private int counter;

		public EntityIterator(int entityCount, Kryo kryo, ByteBufferInput input) {
			this.entityCount = entityCount;
			this.kryo = kryo;
			this.input = input;
		}

		@Override
		public boolean hasNext() {
			return counter < entityCount;
		}

		@Override
		public Entity next() {
			counter++;
			return kryo.readObject(input, Entity.class);
		}

		@Override
		public void close() throws Exception {
			input.close();
		}
	}
}
