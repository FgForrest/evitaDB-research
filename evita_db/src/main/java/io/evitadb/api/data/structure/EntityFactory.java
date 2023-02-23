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

package io.evitadb.api.data.structure;

import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.storage.model.storageParts.entity.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * This helper class allows to instantiate {@link Entity} from the set of {@link io.evitadb.storage.model.storageParts.EntityStoragePart}.
 * Entity hides internal contents by using friendly accessors in order to discourage client code from using the internals.
 * On the other hand this fact is the reason why this class must exist and reside in the data.structure package, because
 * these internals are crucial for effectively loading the entity from the storage form.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EntityFactory {

	private EntityFactory() {}

	/**
	 * Creates {@link Entity} contents from the {@link io.evitadb.storage.model.storageParts.EntityStoragePart}.
	 * Most of the parts may be missing in this time and may be gradually added by method:
	 * {@link #createEntityFrom(EntitySchema, ReflectionLookup, Entity, List, List, ReferencesStoragePart, PricesStoragePart)}
	 *
	 * This method is used for initial loading of the entity.
	 *
	 * @param entitySchema
	 * @param reflectionLookup
	 * @param entityStorageContainer
	 * @param attributesStorageContainers
	 * @param associatedDataStorageContainers
	 * @param referencesStorageContainer
	 * @param priceStorageContainer
	 * @return
	 */
	public static Entity createEntityFrom(
		@Nonnull EntitySchema entitySchema,
		@Nonnull ReflectionLookup reflectionLookup,
		@Nonnull EntityBodyStoragePart entityStorageContainer,
		@Nonnull List<AttributesStoragePart> attributesStorageContainers,
		@Nonnull List<AssociatedDataStoragePart> associatedDataStorageContainers,
		@Nullable ReferencesStoragePart referencesStorageContainer,
		@Nullable PricesStoragePart priceStorageContainer
	) {
		final List<AttributeValue> attributeValues = attributesStorageContainers
			.stream()
			.flatMap(it -> Arrays.stream(it.getAttributes()))
			.collect(Collectors.toList());
		return new Entity(
			entityStorageContainer.getVersion(),
			entitySchema,
			entityStorageContainer.getPrimaryKey(),
			entityStorageContainer.getHierarchicalPlacement(),
			// when references storage container is present use it, otherwise init references by empty collection
			ofNullable(referencesStorageContainer)
				.map(ReferencesStoragePart::getReferencesAsCollection)
				.orElse(Collections.emptyList()),
			// always initialize Attributes container
			new Attributes(
				entitySchema,
				// fill all contents of the attributes loaded from storage (may be empty)
				attributeValues,
				entityStorageContainer.getAttributeLocales()
			),
			// always initialize Associated data container
			new AssociatedData(
				entitySchema,
				reflectionLookup,
				entityStorageContainer.getAssociatedDataKeys(),
				// fill all contents of the associated data loaded from storage (may be empty)
				associatedDataStorageContainers
					.stream()
					.map(AssociatedDataStoragePart::getValue)
					.collect(Collectors.toList())
			),
			// when prices container is present - init prices and price inner record handling - otherwise use default config
			ofNullable(priceStorageContainer)
				.map(PricesStoragePart::getAsPrices)
				.orElseGet(() -> new Prices(PriceInnerRecordHandling.NONE)),
			// pass all locales known in the entity container
			entityStorageContainer.getLocales(),
			// loaded entity is never dropped - otherwise it could not have been read
			false
		);
	}

	/**
	 * Creates {@link Entity} contents on basis of partially loaded entity passed in argument and additional set
	 * of the {@link io.evitadb.storage.model.storageParts.EntityStoragePart}.
	 *
	 * This method cannot be used for initial loading of the entity but is targeted for enriching previously loaded one.
	 *
	 * @param entitySchema
	 * @param reflectionLookup
	 * @param entity
	 * @param attributesStorageContainers
	 * @param associatedDataStorageContainers
	 * @param referencesStorageContainer
	 * @param priceStorageContainer
	 * @return
	 */
	public static Entity createEntityFrom(
		@Nonnull EntitySchema entitySchema,
		@Nonnull ReflectionLookup reflectionLookup,
		@Nonnull Entity entity,
		@Nonnull List<AttributesStoragePart> attributesStorageContainers,
		@Nonnull List<AssociatedDataStoragePart> associatedDataStorageContainers,
		@Nullable ReferencesStoragePart referencesStorageContainer,
		@Nullable PricesStoragePart priceStorageContainer
	) {
		final List<AttributeValue> attributeValues = Stream.concat(
			// new attribute containers
			attributesStorageContainers
				.stream()
				.flatMap(it -> Arrays.stream(it.getAttributes())),
			// original attributes from entity contents
			entity.getAttributeValues().stream()
		).collect(Collectors.toList());
		return new Entity(
			entity.getVersion(),
			entitySchema,
			entity.getPrimaryKey(),
			entity.getHierarchicalPlacement(),
			// when references storage container is present use it
			// otherwise use original references from previous entity contents
			ofNullable(referencesStorageContainer)
				.map(ReferencesStoragePart::getReferencesAsCollection)
				.orElse(entity.getReferences()),
			// when no additional attribute containers were loaded
			attributesStorageContainers.isEmpty() ?
				// use original attributes from the entity contents
				entity.attributes :
				// otherwise combine
				new Attributes(
					entitySchema,
					attributeValues,
					attributeValues
						.stream()
						.map(it -> it.getKey().getLocale())
						.filter(Objects::nonNull)
						.collect(Collectors.toSet())
				),
			// when no additional associated data containers were loaded
			associatedDataStorageContainers.isEmpty() ?
				// use original associated data from the entity contents
				entity.associatedData :
				// otherwise combine
				new AssociatedData(
					entitySchema,
					reflectionLookup,
					// copy all associated data keys from the original entity
					// this set contains all complete set associated data keys of this entity
					entity.associatedData.getAssociatedDataKeys(),
					Stream.concat(
						// new associated data containers
						associatedDataStorageContainers.stream().map(AssociatedDataStoragePart::getValue),
						// original associated data from entity contents
						entity.getAssociatedDataValues().stream()
					)
						// attribute values may be null when they were not loaded but are available in the storage
						.filter(Objects::nonNull)
						.collect(Collectors.toList())
				),
			// when prices container is present - init prices and price inner record handling
			// otherwise use original prices from previous entity contents
			ofNullable(priceStorageContainer)
				.map(PricesStoragePart::getAsPrices)
				.orElseGet(() -> entity.prices),
			// pass all locales known in the entity container
			entity.locales,
			// loaded entity is never dropped - otherwise it could not have been read
			false
		);
	}

}
