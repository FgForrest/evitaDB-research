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

package io.evitadb.storage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import io.evitadb.api.CatalogState;
import io.evitadb.api.EntityCollection;
import io.evitadb.api.configuration.EvitaCatalogConfiguration;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityDecorator;
import io.evitadb.api.data.structure.EntityFactory;
import io.evitadb.api.data.structure.EntitySerializationContext;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.exception.InvalidFileNameException;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.predicate.AssociatedDataValueSerializablePredicate;
import io.evitadb.api.io.predicate.AttributeValueSerializablePredicate;
import io.evitadb.api.io.predicate.PriceContractSerializablePredicate;
import io.evitadb.api.io.predicate.ReferenceContractSerializablePredicate;
import io.evitadb.api.mutation.StorageContainerBuffer;
import io.evitadb.api.mutation.StorageContainerBuffer.BufferedChangeSet;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.api.query.require.PriceFetchMode;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.ClassId;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.KryoFactory.CatalogSerializationHeaderKryoConfigurer;
import io.evitadb.api.serialization.KryoFactory.EntityKryoConfigurer;
import io.evitadb.api.serialization.KryoFactory.SchemaKryoConfigurer;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.storage.exception.*;
import io.evitadb.api.utils.*;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.GlobalEntityIndex;
import io.evitadb.index.ReducedEntityIndex;
import io.evitadb.index.attribute.AttributeIndex;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.index.attribute.SortIndex;
import io.evitadb.index.attribute.UniqueIndex;
import io.evitadb.index.facet.FacetIndex;
import io.evitadb.index.hierarchy.HierarchyIndex;
import io.evitadb.index.price.PriceListAndCurrencyPriceRefIndex;
import io.evitadb.index.price.PriceListAndCurrencyPriceSuperIndex;
import io.evitadb.index.price.PriceRefIndex;
import io.evitadb.index.price.PriceSuperIndex;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.storage.kryo.VersionedKryo;
import io.evitadb.storage.kryo.VersionedKryoFactory;
import io.evitadb.storage.model.CatalogEntityHeader;
import io.evitadb.storage.model.CatalogHeader;
import io.evitadb.storage.model.memTable.VersionedKryoKeyInputs;
import io.evitadb.storage.model.storageParts.entity.*;
import io.evitadb.storage.model.storageParts.entity.AssociatedDataStoragePart.EntityAssociatedDataKey;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart.EntityAttributesSetKey;
import io.evitadb.storage.model.storageParts.index.*;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStoragePart.AttributeIndexType;
import io.evitadb.storage.serialization.CatalogHeaderSerializationService;
import io.evitadb.storage.serialization.ExtendedCatalogHeaderConfigurer;
import io.evitadb.storage.serialization.StoragePartConfigurer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.storage.model.storageParts.index.PriceListAndCurrencySuperIndexStoragePart.computeUniquePartId;
import static java.util.Optional.ofNullable;

/**
 * IOService class encapsulates {@link io.evitadb.api.Catalog} serialization to persistent storage and also deserializing
 * the catalog contents back.
 * <p>
 * This class is work in progress and is expected to change dramatically in the future.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Slf4j
@RequiredArgsConstructor
public class IOService {
	public static final String DATA_FILE_SUFFIX = ".dat";
	public static final String HEADER_FILE_NAME = "header" + DATA_FILE_SUFFIX;
	private final ReflectionLookup reflectionLookup;
	private final Pool<CatalogHeaderSerializationService> headerSerializationServicePool = new Pool<>(true, false, 8) {
		@Override
		protected CatalogHeaderSerializationService create() {
			return new CatalogHeaderSerializationService(
				CatalogSerializationHeaderKryoConfigurer.INSTANCE
					.andThen(ExtendedCatalogHeaderConfigurer.INSTANCE)
			);
		}
	};

	/**
	 * Check whether target directory exists and whether it is really directory.
	 */
	public void verifyDirectory(@Nonnull Path storageDirectory, boolean requireEmpty) {
		final File storageDirectoryFile = storageDirectory.toFile();
		if (!storageDirectoryFile.exists()) {
			//noinspection ResultOfMethodCallIgnored
			storageDirectoryFile.mkdirs();
		}
		Assert.isTrue(storageDirectoryFile.exists(), () -> new InvalidStoragePathException("Storage path doesn't exist: " + storageDirectory));
		Assert.isTrue(storageDirectoryFile.isDirectory(), () -> new InvalidStoragePathException("Storage path doesn't represent a directory: " + storageDirectory));
		if (requireEmpty) {
			Assert.isTrue(
				ofNullable(storageDirectoryFile.listFiles()).map(it -> it.length).orElse(0) == 0,
				() -> new DirectoryNotEmptyException(storageDirectory.toString())
			);
		}
	}

	/**
	 * There is a file in target directory which name equals to lowercased entity type.
	 */
	@Nonnull
	public Path getPathForEntityType(@Nonnull Path storageDirectory, @Nonnull Serializable entityType) {
		return storageDirectory
			.resolve(
				StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(
					entityType.toString(), '-', ""
				).toUpperCase(Locale.ROOT)
					+ DATA_FILE_SUFFIX
			);
	}

	/**
	 * Creates {@link Kryo} instance that is usable for deserializing entity instances.
	 */
	public Function<VersionedKryoKeyInputs, VersionedKryo> createTypeKryoInstance(@Nonnull Supplier<EntitySchema> currentSchemaSupplier) {
		return kryoKeyInputs -> VersionedKryoFactory.createKryo(
			kryoKeyInputs.getVersion(),
			kryoKeyInputs.getClassResolver(),
			SchemaKryoConfigurer.INSTANCE
				.andThen(new EntityKryoConfigurer(currentSchemaSupplier, reflectionLookup, kryoKeyInputs.getKeyCompressor()))
				.andThen(new StoragePartConfigurer(DefaultKryoSerializationHelper.INSTANCE, kryoKeyInputs.getKeyCompressor()))
				.andThen(ExtendedCatalogHeaderConfigurer.INSTANCE)
		);
	}

	/**
	 * Deserializes catalog from persistent storage. As of now the catalog is read entirely from scratch from
	 * {@link EvitaCatalogConfiguration#getStorageDirectory()}. Contents of the directory must contain previously
	 * serialized catalog of the identical catalog otherwise exception is thrown.
	 *
	 * @throws UnexpectedCatalogContentsException when directory contains different catalog data or no data at all
	 * @throws UnexpectedIOException              in case of any unknown IOException
	 */
	@Nullable
	public CatalogHeader readHeader(@Nonnull Path catalogDirectory, @Nonnull String expectedCatalogName) {
		final long start = System.nanoTime();
		final CatalogHeaderSerializationService serializationService = headerSerializationServicePool.obtain();
		try {
			verifyDirectory(catalogDirectory, false);
			final File headerFile = catalogDirectory.resolve(HEADER_FILE_NAME).toFile();
			if (!headerFile.exists()) {
				return null;
			}
			final CatalogHeader header;
			try (final FileInputStream is = new FileInputStream(headerFile)) {
				header = serializationService.deserialize(is);
				logStatistics(header, " is being loaded");
			} catch (IOException e) {
				throw new UnexpectedIOException("Failed to store Evita header file! All data are worthless :(", e);
			}
			Assert.isTrue(
				header.getCatalogName().equals(expectedCatalogName),
				() -> new UnexpectedCatalogContentsException(
					"Directory " + catalogDirectory + " contains data of " + header.getCatalogName() +
						" catalog. Cannot load catalog " + expectedCatalogName + " from this directory!"
				)
			);
			return header;
		} finally {
			headerSerializationServicePool.free(serializationService);
			log.info("Catalog loaded in " + StringUtils.formatNano(System.nanoTime() - start));
		}
	}

	/**
	 * Verifies if passed entity type name is valid and unique among other entity types after file name normalization.
	 *
	 * @throws DuplicateFileNameException when multiple entity types translate to same file name
	 * @throws InvalidFileNameException   when entity type contains such characters that normalization creates file with no name
	 */
	public void verifyEntityType(Stream<Serializable> existingEntityTypes, Serializable entityType) throws DuplicateFileNameException, InvalidFileNameException {
		final String normalizedFileName = FileUtils.formatFileName(entityType);
		final Optional<Serializable> conflictingType = existingEntityTypes
			.filter(it -> normalizedFileName.equals(FileUtils.formatFileName(it)))
			.findAny();
		if (conflictingType.isPresent()) {
			throw new DuplicateFileNameException(
				"Catalog already contains entity type " + conflictingType.get() +
					" that translates to the same file name as " + entityType + "! Please choose different entity type."
			);
		}
	}

	/**
	 * Serializes all {@link EntityCollection} of the catalog to the persistent storage. Target directory
	 * {@link EvitaCatalogConfiguration#getStorageDirectory()} is expected to be empty.
	 *
	 * @throws InvalidStoragePathException when path is incorrect (cannot be created)
	 * @throws DirectoryNotEmptyException  when directory already contains some data
	 * @throws UnexpectedIOException       in case of any unknown IOException
	 */
	public void storeHeader(@Nonnull Path catalogDirectory, @Nonnull String catalogName, @Nonnull CatalogState catalogState, long transactionId, @Nonnull List<CatalogEntityHeader> entityHeaders) {
		final long start = System.nanoTime();
		final CatalogHeaderSerializationService catalogHeaderSerializationService = headerSerializationServicePool.obtain();
		try {
			verifyDirectory(catalogDirectory, false);
			final CatalogHeader catalogHeader = new CatalogHeader(
				catalogName,
				catalogState,
				transactionId,
				getClassIds(entityHeaders),
				entityHeaders
			);

			try (final FileOutputStream os = new FileOutputStream(catalogDirectory.resolve(HEADER_FILE_NAME).toFile())) {
				catalogHeaderSerializationService.serialize(catalogHeader, os);
				logStatistics(catalogHeader, "has been written");
			} catch (IOException e) {
				throw new UnexpectedIOException("Failed to store Evita header file! All data are worthless :(", e);
			}
		} finally {
			headerSerializationServicePool.free(catalogHeaderSerializationService);
			log.info("Catalog stored in " + StringUtils.formatNano(System.nanoTime() - start));
		}
	}

	/**
	 * Reads entity from persistent storage by its primary key.
	 * Requirements of type {@link EntityContentRequire} in `evitaRequest` are taken into an account. Passed `memTable`
	 * is used for reading data from underlying data store.
	 */
	public Entity readEntity(int entityPrimaryKey, EvitaRequest evitaRequest, EntitySchema entitySchema, StorageContainerBuffer storageContainerBuffer) {
		// provide passed schema during deserialization from binary form
		return EntitySerializationContext.executeWithSupplier(entitySchema, () -> {
			// fetch the main entity container
			final EntityBodyStoragePart entityStorageContainer = storageContainerBuffer.fetch(
				entityPrimaryKey, EntityBodyStoragePart.class
			);
			if (entityStorageContainer == null) {
				// return null if not found
				return null;
			} else {
				return toEntity(evitaRequest, entitySchema, entityStorageContainer, storageContainerBuffer);
			}
		});
	}

	/**
	 * Converts {@link EntityBodyStoragePart} into the full blown entity.
	 */
	@Nonnull
	public Entity toEntity(EvitaRequest evitaRequest, EntitySchema entitySchema, EntityBodyStoragePart entityStorageContainer, StorageContainerBuffer storageContainerBuffer) {
		final int entityPrimaryKey = entityStorageContainer.getPrimaryKey();
		// load additional containers only when requested
		final ReferencesStoragePart referencesStorageContainer = fetchReferences(
			entityPrimaryKey, null, new ReferenceContractSerializablePredicate(evitaRequest),
			storageContainerBuffer
		);
		final PricesStoragePart priceStorageContainer = fetchPrices(
			entityPrimaryKey, null, new PriceContractSerializablePredicate(evitaRequest),
			storageContainerBuffer
		);

		final List<AttributesStoragePart> attributesStorageContainers = fetchAttributes(
			entityPrimaryKey, null, new AttributeValueSerializablePredicate(evitaRequest),
			storageContainerBuffer, entityStorageContainer.getAttributeLocales()
		);
		final List<AssociatedDataStoragePart> associatedDataStorageContainers = fetchAssociatedData(
			entityPrimaryKey, null, new AssociatedDataValueSerializablePredicate(evitaRequest),
			storageContainerBuffer, entityStorageContainer.getAssociatedDataKeys()
		);

		// and build the entity
		return EntityFactory.createEntityFrom(
			entitySchema,
			reflectionLookup,
			entityStorageContainer,
			attributesStorageContainers,
			associatedDataStorageContainers,
			referencesStorageContainer,
			priceStorageContainer
		);
	}

	/**
	 * Loads additional data to existing entity according to requirements of type {@link EntityContentRequire}
	 * in `evitaRequest`. Passed `memTable` is used for reading data from underlying data store.
	 * Since entity is immutable object - enriched instance is a new instance based on previous entity that contains
	 * additional data.
	 */
	public Entity enrichEntity(
		@Nonnull EntityDecorator entityDecorator,
		@Nonnull AttributeValueSerializablePredicate newAttributePredicate,
		@Nonnull AssociatedDataValueSerializablePredicate newAssociatedDataPredicate,
		@Nonnull ReferenceContractSerializablePredicate newReferenceContractPredicate,
		@Nonnull PriceContractSerializablePredicate newPricePredicate,
		@Nonnull StorageContainerBuffer storageContainerBuffer
	) {
		// provide passed schema during deserialization from binary form
		return EntitySerializationContext.executeWithSupplier(entityDecorator.getSchema(), () -> {
			final int entityPrimaryKey = Objects.requireNonNull(entityDecorator.getPrimaryKey());

			// fetch additional data if requested and not already present
			final ReferencesStoragePart referencesStorageContainer = fetchReferences(
				entityPrimaryKey, entityDecorator.getReferencePredicate(), newReferenceContractPredicate,
				storageContainerBuffer
			);
			final PricesStoragePart priceStorageContainer = fetchPrices(
				entityPrimaryKey, entityDecorator.getPricePredicate(), newPricePredicate,
				storageContainerBuffer
			);

			final List<AttributesStoragePart> attributesStorageContainers = fetchAttributes(
				entityPrimaryKey, entityDecorator.getAttributePredicate(), newAttributePredicate,
				storageContainerBuffer, entityDecorator.getAttributeLocales()
			);
			final List<AssociatedDataStoragePart> associatedDataStorageContainers = fetchAssociatedData(
				entityPrimaryKey, entityDecorator.getAssociatedDataPredicate(), newAssociatedDataPredicate,
				storageContainerBuffer, entityDecorator.getDelegate().getAssociatedDataKeys()
			);

			// if anything was fetched from the persistent storage
			if (referencesStorageContainer != null || priceStorageContainer != null ||
				!attributesStorageContainers.isEmpty() || !associatedDataStorageContainers.isEmpty()) {
				// and build the enriched entity as a new instance
				return EntityFactory.createEntityFrom(
					entityDecorator.getDelegate().getSchema(),
					reflectionLookup,
					entityDecorator.getDelegate(),
					attributesStorageContainers,
					associatedDataStorageContainers,
					referencesStorageContainer,
					priceStorageContainer
				);
			} else {
				// return original entity - nothing has been fetched
				return entityDecorator.getDelegate();
			}
		});
	}

	/**
	 * Method reconstructs entity index from underlying containers.
	 */
	public EntityIndex readEntityIndex(int entityIndexId, @Nonnull MemTable memTable, @Nonnull Supplier<EntitySchema> schemaSupplier, @Nonnull Supplier<PriceSuperIndex> temporalIndexAccessor, @Nonnull Supplier<PriceSuperIndex> superIndexAccessor) {
		final EntityIndexStoragePart entityIndexCnt = memTable.get(entityIndexId, EntityIndexStoragePart.class);
		Assert.isTrue(entityIndexCnt != null, () -> new IllegalStateException("Entity index with PK `" + entityIndexId + "` was unexpectedly not found in the mem table!"));

		final Map<AttributeKey, UniqueIndex> uniqueIndexes = new HashMap<>();
		final Map<AttributeKey, FilterIndex> filterIndexes = new HashMap<>();
		final Map<AttributeKey, SortIndex> sortIndexes = new HashMap<>();
		for (AttributeIndexStorageKey attributeIndexKey : entityIndexCnt.getAttributeIndexes()) {
			switch (attributeIndexKey.getIndexType()) {
				case UNIQUE: {
					fetchUniqueIndex(entityIndexId, memTable, uniqueIndexes, attributeIndexKey);
					break;
				}
				case FILTER: {
					fetchFilterIndex(entityIndexId, memTable, filterIndexes, attributeIndexKey);
					break;
				}
				case SORT: {
					fetchSortIndex(entityIndexId, memTable, sortIndexes, attributeIndexKey);
					break;
				}
				default:
					throw new IllegalStateException("Unknown attribute index type: " + attributeIndexKey.getIndexType());
			}
		}

		final HierarchyIndex hierarchyIndex = fetchHierarchyIndex(entityIndexId, memTable, entityIndexCnt);
		final FacetIndex facetIndex = fetchFacetIndex(entityIndexId, memTable, entityIndexCnt);

		final EntityIndexType entityIndexType = entityIndexCnt.getEntityIndexKey().getType();
		// base on entity index type we either create GlobalEntityIndex or ReducedEntityIndex
		if (entityIndexType == EntityIndexType.GLOBAL) {
			final Map<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> priceIndexes = fetchPriceSuperIndexes(
				entityIndexId, entityIndexCnt.getPriceIndexes(), memTable
			);
			return new GlobalEntityIndex(
				entityIndexCnt.getPrimaryKey(),
				entityIndexCnt.getEntityIndexKey(),
				entityIndexCnt.getVersion(),
				schemaSupplier,
				entityIndexCnt.getEntityIds(),
				entityIndexCnt.getEntitiesIdsByLanguage(),
				new AttributeIndex(
					uniqueIndexes, filterIndexes, sortIndexes
				),
				new PriceSuperIndex(
					Objects.requireNonNull(entityIndexCnt.getInternalPriceIdSequence()),
					priceIndexes
				),
				hierarchyIndex,
				facetIndex
			);
		} else {
			final Map<PriceIndexKey, PriceListAndCurrencyPriceRefIndex> priceIndexes = fetchPriceRefIndexes(
				entityIndexId, entityIndexCnt.getPriceIndexes(), memTable, temporalIndexAccessor
			);
			return new ReducedEntityIndex(
				entityIndexCnt.getPrimaryKey(),
				entityIndexCnt.getEntityIndexKey(),
				entityIndexCnt.getVersion(),
				schemaSupplier,
				entityIndexCnt.getEntityIds(),
				entityIndexCnt.getEntitiesIdsByLanguage(),
				new AttributeIndex(
					uniqueIndexes, filterIndexes, sortIndexes
				),
				new PriceRefIndex(priceIndexes, superIndexAccessor),
				hierarchyIndex,
				facetIndex
			);
		}
	}

	/**
	 * Flushes all trapped memory data to the persistent storage.
	 * This method doesn't take transactional memory into an account but only flushes changes for trapped updates.
	 */
	public void flushTrappedUpdates(BufferedChangeSet bufferedChangeSet, MemTable memTable) {
		// now store all entity trapped updates
		bufferedChangeSet.getTrappedMemTableUpdates()
			.forEach(it -> memTable.put(0L, it));
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * Fetches {@link io.evitadb.index.facet.FacetIndex} from the {@link MemTable} and returns it.
	 */
	@Nonnull
	private FacetIndex fetchFacetIndex(int entityIndexId, @Nonnull MemTable memTable, @Nonnull EntityIndexStoragePart entityIndexCnt) {
		final FacetIndex facetIndex;
		final Set<Serializable> facetIndexes = entityIndexCnt.getFacetIndexes();
		if (facetIndexes.isEmpty()) {
			facetIndex = new FacetIndex();
		} else {
			final List<FacetIndexStoragePart> facetIndexParts = new ArrayList<>(facetIndexes.size());
			for (Serializable referencedEntityType : facetIndexes) {
				final long primaryKey = FacetIndexStoragePart.computeUniquePartId(entityIndexId, referencedEntityType, memTable.getReadOnlyKeyCompressor());
				final FacetIndexStoragePart facetIndexStoragePart = memTable.get(primaryKey, FacetIndexStoragePart.class);
				Assert.isTrue(facetIndexStoragePart != null, () -> new IllegalStateException("Facet index with id " + entityIndexId + " (upid=" + primaryKey + ") and key " + referencedEntityType + " was not found in mem table!"));
				facetIndexParts.add(facetIndexStoragePart);
			}
			facetIndex = new FacetIndex(facetIndexParts);
		}
		return facetIndex;
	}

	/**
	 * Fetches {@link HierarchyIndex} from the {@link MemTable} and returns it.
	 */
	@Nonnull
	private HierarchyIndex fetchHierarchyIndex(int entityIndexId, @Nonnull MemTable memTable, @Nonnull EntityIndexStoragePart entityIndexCnt) {
		final HierarchyIndex hierarchyIndex;
		if (entityIndexCnt.isHierarchyIndex()) {
			final HierarchyIndexStoragePart hierarchyIndexStoragePart = memTable.get(entityIndexId, HierarchyIndexStoragePart.class);
			Assert.isTrue(hierarchyIndexStoragePart != null, () -> new IllegalStateException("Hierarchy index with id " + entityIndexId + " was not found in mem table!"));
			hierarchyIndex = new HierarchyIndex(
				hierarchyIndexStoragePart.getRoots(),
				hierarchyIndexStoragePart.getLevelIndex(),
				hierarchyIndexStoragePart.getItemIndex(),
				hierarchyIndexStoragePart.getOrphans()
			);
		} else {
			hierarchyIndex = new HierarchyIndex();
		}
		return hierarchyIndex;
	}

	/**
	 * Fetches {@link SortIndex} from the {@link MemTable} and puts it into the `sortIndexes` key-value index.
	 */
	private void fetchSortIndex(int entityIndexId, @Nonnull MemTable memTable, @Nonnull Map<AttributeKey, SortIndex> sortIndexes, @Nonnull AttributeIndexStorageKey attributeIndexKey) {
		final long primaryKey = AttributeIndexStoragePart.computeUniquePartId(entityIndexId, AttributeIndexType.SORT, attributeIndexKey.getAttribute(), memTable.getReadOnlyKeyCompressor());
		final SortIndexStoragePart sortIndexCnt = memTable.get(primaryKey, SortIndexStoragePart.class);
		Assert.isTrue(sortIndexCnt != null, () -> new IllegalStateException("Sort index with id " + entityIndexId + " with key " + attributeIndexKey.getAttribute() + " was not found in mem table!"));
		final AttributeKey attributeKey = sortIndexCnt.getAttributeKey();
		sortIndexes.put(
			attributeKey,
			new SortIndex(
				sortIndexCnt.getType(),
				sortIndexCnt.getSortedRecords(),
				sortIndexCnt.getSortedRecordsValues(),
				sortIndexCnt.getValueCardinalities()
			)
		);
	}

	/**
	 * Fetches {@link FilterIndex} from the {@link MemTable} and puts it into the `filterIndexes` key-value index.
	 */
	private void fetchFilterIndex(int entityIndexId, @Nonnull MemTable memTable, @Nonnull Map<AttributeKey, FilterIndex> filterIndexes, @Nonnull AttributeIndexStorageKey attributeIndexKey) {
		final long primaryKey = AttributeIndexStoragePart.computeUniquePartId(entityIndexId, AttributeIndexType.FILTER, attributeIndexKey.getAttribute(), memTable.getReadOnlyKeyCompressor());
		final FilterIndexStoragePart filterIndexCnt = memTable.get(primaryKey, FilterIndexStoragePart.class);
		Assert.isTrue(filterIndexCnt != null, () -> new IllegalStateException("Filter index with id " + entityIndexId + " with key " + attributeIndexKey.getAttribute() + " was not found in mem table!"));
		final AttributeKey attributeKey = filterIndexCnt.getAttributeKey();
		filterIndexes.put(
			attributeKey,
			new FilterIndex(
				filterIndexCnt.getHistogram(),
				filterIndexCnt.getRangeIndex()
			)
		);
	}

	/**
	 * Fetches {@link UniqueIndex} from the {@link MemTable} and puts it into the `uniqueIndexes` key-value index.
	 */
	private void fetchUniqueIndex(int entityIndexId, @Nonnull MemTable memTable, @Nonnull Map<AttributeKey, UniqueIndex> uniqueIndexes, @Nonnull AttributeIndexStorageKey attributeIndexKey) {
		final long primaryKey = AttributeIndexStoragePart.computeUniquePartId(entityIndexId, AttributeIndexType.UNIQUE, attributeIndexKey.getAttribute(), memTable.getReadOnlyKeyCompressor());
		final UniqueIndexStoragePart uniqueIndexCnt = memTable.get(primaryKey, UniqueIndexStoragePart.class);
		Assert.isTrue(uniqueIndexCnt != null, () -> new IllegalStateException("Unique index with id " + entityIndexId + " with key " + attributeIndexKey.getAttribute() + " was not found in mem table!"));
		final AttributeKey attributeKey = uniqueIndexCnt.getAttributeKey();
		uniqueIndexes.put(
			attributeKey,
			new UniqueIndex(
				attributeKey.getAttributeName(), uniqueIndexCnt.getType(),
				uniqueIndexCnt.getUniqueValueToRecordId()
			)
		);
	}

	/**
	 * Fetches {@link PriceListAndCurrencyPriceSuperIndex price indexes} from the {@link MemTable} and returns key-value
	 * index of them.
	 */
	private Map<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> fetchPriceSuperIndexes(int entityIndexId, @Nonnull Set<PriceIndexKey> priceIndexes, @Nonnull MemTable memTable) {
		final Map<PriceIndexKey, PriceListAndCurrencyPriceSuperIndex> priceSuperIndexes = CollectionUtils.createHashMap(priceIndexes.size());
		for (PriceIndexKey priceIndexKey : priceIndexes) {
			final long primaryKey = computeUniquePartId(entityIndexId, priceIndexKey, memTable.getReadOnlyKeyCompressor());
			final PriceListAndCurrencySuperIndexStoragePart priceIndexCnt = memTable.get(primaryKey, PriceListAndCurrencySuperIndexStoragePart.class);
			Assert.isTrue(priceIndexCnt != null, () -> new IllegalStateException("Price index with id " + entityIndexId + " with key " + priceIndexKey + " was not found in mem table!"));
			priceSuperIndexes.put(
				priceIndexKey,
				new PriceListAndCurrencyPriceSuperIndex(
					priceIndexKey,
					priceIndexCnt.getValidityIndex(),
					priceIndexCnt.getPriceRecords()
				)
			);
		}
		return priceSuperIndexes;
	}

	/**
	 * Fetches {@link PriceListAndCurrencyPriceRefIndex price indexes} from the {@link MemTable} and returns key-value
	 * index of them.
	 */
	private Map<PriceIndexKey, PriceListAndCurrencyPriceRefIndex> fetchPriceRefIndexes(int entityIndexId, @Nonnull Set<PriceIndexKey> priceIndexes, @Nonnull MemTable memTable, @Nonnull Supplier<PriceSuperIndex> superIndexAccessor) {
		final Map<PriceIndexKey, PriceListAndCurrencyPriceRefIndex> priceRefIndexes = CollectionUtils.createHashMap(priceIndexes.size());
		for (PriceIndexKey priceIndexKey : priceIndexes) {
			final long primaryKey = computeUniquePartId(entityIndexId, priceIndexKey, memTable.getReadOnlyKeyCompressor());
			final PriceListAndCurrencyRefIndexStoragePart priceIndexCnt = memTable.get(primaryKey, PriceListAndCurrencyRefIndexStoragePart.class);
			Assert.isTrue(
				priceIndexCnt != null,
				() -> new IllegalStateException("Price index with id " + entityIndexId + " with key " + priceIndexKey + " was not found in mem table!")
			);
			priceRefIndexes.put(
				priceIndexKey,
				new PriceListAndCurrencyPriceRefIndex(
					priceIndexKey,
					priceIndexCnt.getValidityIndex(),
					priceIndexCnt.getPriceIds(),
					pik -> superIndexAccessor.get().getPriceIndex(pik)
				)
			);
		}
		return priceRefIndexes;
	}

	/**
	 * Fetches reference container from MemTable if it hasn't been already loaded before.
	 */
	@Nullable
	private ReferencesStoragePart fetchReferences(
		int entityPrimaryKey,
		@Nullable ReferenceContractSerializablePredicate previousReferenceContractPredicate,
		@Nonnull ReferenceContractSerializablePredicate newReferenceContractPredicate,
		@Nonnull StorageContainerBuffer storageContainerBuffer
	) {
		final ReferencesStoragePart referencesStorageContainer;
		if ((previousReferenceContractPredicate == null || !previousReferenceContractPredicate.isRequiresEntityReferences()) && newReferenceContractPredicate.isRequiresEntityReferences()) {
			referencesStorageContainer = storageContainerBuffer.fetch(entityPrimaryKey, ReferencesStoragePart.class);
		} else {
			referencesStorageContainer = null;
		}
		return referencesStorageContainer;
	}

	/**
	 * Fetches prices container from MemTable if it hasn't been already loaded before.
	 */
	@Nullable
	private PricesStoragePart fetchPrices(int entityPrimaryKey, @Nullable PriceContractSerializablePredicate previousPricePredicate, @Nonnull PriceContractSerializablePredicate newPricePredicate, StorageContainerBuffer storageContainerBuffer) {
		final PricesStoragePart priceStorageContainer;
		if ((previousPricePredicate == null || previousPricePredicate.getPriceFetchMode() == PriceFetchMode.NONE) && newPricePredicate.getPriceFetchMode() != PriceFetchMode.NONE) {
			priceStorageContainer = storageContainerBuffer.fetch(entityPrimaryKey, PricesStoragePart.class);
		} else {
			priceStorageContainer = null;
		}
		return priceStorageContainer;
	}

	/**
	 * Fetches attributes container from MemTable if it hasn't been already loaded before.
	 */
	@Nonnull
	private List<AttributesStoragePart> fetchAttributes(
		int entityPrimaryKey,
		@Nullable AttributeValueSerializablePredicate previousAttributePredicate,
		@Nonnull AttributeValueSerializablePredicate newAttributePredicate,
		@Nonnull StorageContainerBuffer storageContainerBuffer,
		@Nonnull Set<Locale> allAvailableLocales
	) {
		final List<AttributesStoragePart> attributesStorageContainers = new LinkedList<>();
		if (newAttributePredicate.isRequiresEntityAttributes()) {
			// we need to load global attributes container (i.e. attributes not linked to any locale)
			final boolean firstRequest = previousAttributePredicate == null || !previousAttributePredicate.isRequiresEntityAttributes();
			if (firstRequest) {
				final EntityAttributesSetKey globalAttributeSetKey = new EntityAttributesSetKey(entityPrimaryKey, null);
				ofNullable(storageContainerBuffer.fetch(globalAttributeSetKey, AttributesStoragePart.class, AttributesStoragePart::computeUniquePartId))
					.ifPresent(attributesStorageContainers::add);
			}
			// go through all alreadyFetchedLocales entity is known to have
			final Set<Locale> previouslyFetchedLanguages = ofNullable(previousAttributePredicate).map(AttributeValueSerializablePredicate::getLanguages).orElse(null);
			final Set<Locale> newlyFetchedLanguages = newAttributePredicate.getLanguages();
			final Predicate<Locale> fetchedPreviously = firstRequest ?
				locale -> false :
				locale -> previouslyFetchedLanguages != null && (previouslyFetchedLanguages.isEmpty() || previouslyFetchedLanguages.contains(locale));
			final Predicate<Locale> fetchedNewly = locale -> newlyFetchedLanguages != null && (newlyFetchedLanguages.isEmpty() || newlyFetchedLanguages.contains(locale));
			allAvailableLocales
				.stream()
				// filter them according to language (if no language is requested, all languages match)
				.filter(it -> !fetchedPreviously.test(it) && fetchedNewly.test(it))
				// now fetch it from the storage
				.map(it -> {
					final EntityAttributesSetKey localeSpecificAttributeSetKey = new EntityAttributesSetKey(entityPrimaryKey, it);
					// there may be no attributes in specified language
					return storageContainerBuffer.fetch(localeSpecificAttributeSetKey, AttributesStoragePart.class, AttributesStoragePart::computeUniquePartId);
				})
				// filter out null values (of non-existent containers)
				.filter(Objects::nonNull)
				// non null values add to output list
				.forEach(attributesStorageContainers::add);
		}
		return attributesStorageContainers;
	}

	/**
	 * Fetches associated data container(s) from MemTable if it hasn't (they haven't) been already loaded before.
	 */
	@Nonnull
	private List<AssociatedDataStoragePart> fetchAssociatedData(
		int entityPrimaryKey,
		@Nullable AssociatedDataValueSerializablePredicate previousAssociatedDataValuePredicate,
		@Nonnull AssociatedDataValueSerializablePredicate newAssociatedDataValuePredicate,
		@Nonnull StorageContainerBuffer storageContainerBuffer,
		@Nonnull Set<AssociatedDataKey> allAssociatedDataKeys
	) {
		// if there is single request for associated data
		if (newAssociatedDataValuePredicate.isRequiresEntityAssociatedData()) {
			final Set<AssociatedDataKey> missingAssociatedDataSet = new HashSet<>(allAssociatedDataKeys.size());
			final Set<Locale> requestedLocales = newAssociatedDataValuePredicate.getLanguages();
			final Set<String> requestedAssociatedDataSet = newAssociatedDataValuePredicate.getAssociatedDataSet();
			final Predicate<AssociatedDataKey> wasNotFetched =
				associatedDataKey -> previousAssociatedDataValuePredicate == null || !previousAssociatedDataValuePredicate.wasFetched(associatedDataKey);
			// construct missing associated data keys
			if (requestedAssociatedDataSet.isEmpty()) {
				// add all not yet loaded keys
				allAssociatedDataKeys
					.stream()
					.filter(associatedDataKey -> !associatedDataKey.isLocalized() || (requestedLocales != null && (requestedLocales.isEmpty() || requestedLocales.contains(associatedDataKey.getLocale()))))
					.filter(wasNotFetched)
					.forEach(missingAssociatedDataSet::add);
			} else {
				for (String associatedDataName : requestedAssociatedDataSet) {
					final AssociatedDataKey globalKey = new AssociatedDataKey(associatedDataName);
					if (allAssociatedDataKeys.contains(globalKey) && wasNotFetched.test(globalKey)) {
						missingAssociatedDataSet.add(globalKey);
					}
					if (requestedLocales != null) {
						for (Locale requestedLocale : requestedLocales) {
							final AssociatedDataKey localizedKey = new AssociatedDataKey(associatedDataName, requestedLocale);
							if (allAssociatedDataKeys.contains(localizedKey) && wasNotFetched.test(localizedKey)) {
								missingAssociatedDataSet.add(localizedKey);
							}
						}
					}
				}
			}

			return missingAssociatedDataSet
				.stream()
				.map(it -> {
					// fetch missing associated data from underlying storage
					final AssociatedDataStoragePart associatedData = storageContainerBuffer.fetch(
						new EntityAssociatedDataKey(entityPrimaryKey, it.getAssociatedDataName(), it.getLocale()),
						AssociatedDataStoragePart.class, AssociatedDataStoragePart::computeUniquePartId
					);
					// since we know all available keys from the entity header there always should be looked up container
					Assert.notNull(associatedData, "Associated data " + it + " was expected in the storage, but none was found!");
					return associatedData;
				})
				.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Generates list of additional classes that needs to be registered in {@link Kryo} so that (de)serialization
	 * stays consistent.
	 */
	private List<ClassId> getClassIds(List<CatalogEntityHeader> entityHeaders) {
		final List<ClassId> classIds = new ArrayList<>(entityHeaders.size());
		final Set<Class<?>> registeredTypes = new HashSet<>(entityHeaders.size());
		int classIdSequence = KryoFactory.CLASSES_RESERVED_FOR_INTERNAL_USE;
		for (CatalogEntityHeader entityHeader : entityHeaders) {
			final Class<? extends Serializable> entityTypeType = entityHeader.getEntityType().getClass();
			if (!EvitaDataTypes.isSupportedType(entityTypeType) && !registeredTypes.contains(entityTypeType)) {
				classIds.add(
					new ClassId(
						classIdSequence++, entityTypeType
					)
				);
				registeredTypes.add(entityTypeType);
			}
		}
		return classIds;
	}

	/**
	 * Logs statistics about the catalog and entities in it to the logger.
	 */
	private void logStatistics(@Nonnull CatalogHeader catalogHeader, @Nonnull String operation) {
		if (log.isInfoEnabled()) {
			final StringBuilder stats = new StringBuilder("Catalog " + catalogHeader.getCatalogName() + " " + operation + ". It contains:");
			for (CatalogEntityHeader catalogEntityHeader : catalogHeader.getEntityTypesIndex().values()) {
				stats.append("\n\t- ")
					.append(catalogEntityHeader.getEntityType())
					.append(" (")
					.append(catalogEntityHeader.getRecordCount())
					.append(")");
			}
			log.info(stats.toString());
		}
	}

}
