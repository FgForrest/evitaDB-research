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

package io.evitadb.artificial;

import io.evitadb.api.CatalogBase;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.TransactionBase;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.StringUtils;
import io.evitadb.test.Entities;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Optional.of;

/**
 * Base state class for {@link ArtificialEntitiesBenchmark} benchmark.
 * See benchmark description on the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ArtificialFullDatabaseState<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>>
	extends ArtificialState<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION> {
	/**
	 * Number of products stored in the database.
	 */
	public static final int PRODUCT_COUNT = 100_000;

	/**
	 * Method is invoked before each benchmark.
	 * Method creates bunch of brand, categories, price lists and stores that cen be referenced in products.
	 * Method also prepares 100.000 products in the database.
	 */
	@Setup(Level.Trial)
	public void setUp() {
		this.dataGenerator.clear();
		this.generatedEntities.clear();

		final long start = System.nanoTime();
		final String catalogName = getCatalogName();
		final StringBuilder loadInfo = new StringBuilder("Catalog: " + catalogName + "\n");
		if (shouldStartFromScratch() || !isCatalogAvailable(catalogName)) {
			System.out.println("\n\nCreating database from scratch ...");
			// prepare database
			this.evita = createEmptyEvitaInstance(catalogName);
			// create bunch or entities for referencing in products
			this.evita.updateCatalog(
				catalogName,
				session -> {
					this.dataGenerator.generateEntities(
							processAndReturnSchema(this.dataGenerator.getSampleBrandSchema(session)),
							this.randomEntityPicker,
							SEED
						)
						.limit(5)
						.forEach(it -> {
							processEntity(it.toInstance());
							createEntity(session, generatedEntities, it);
						});

					this.dataGenerator.generateEntities(
							processAndReturnSchema(this.dataGenerator.getSampleCategorySchema(session)),
							this.randomEntityPicker,
							SEED
						)
						.limit(10)
						.forEach(it -> {
							processEntity(it.toInstance());
							createEntity(session, generatedEntities, it);
						});

					this.dataGenerator.generateEntities(
							processAndReturnSchema(this.dataGenerator.getSamplePriceListSchema(session)),
							this.randomEntityPicker,
							SEED
						)
						.limit(4)
						.forEach(it -> {
							processEntity(it.toInstance());
							createEntity(session, generatedEntities, it);
						});

					this.dataGenerator.generateEntities(
							processAndReturnSchema(this.dataGenerator.getSampleStoreSchema(session)),
							this.randomEntityPicker,
							SEED
						)
						.limit(12)
						.forEach(it -> {
							processEntity(it.toInstance());
							createEntity(session, generatedEntities, it);
						});

					this.dataGenerator.generateEntities(
							processAndReturnSchema(this.dataGenerator.getSampleParameterSchema(session)),
							this.randomEntityPicker,
							SEED
						)
						.limit(200)
						.forEach(it -> {
							processEntity(it.toInstance());
							createEntity(session, generatedEntities, it);
						});

					this.dataGenerator.generateEntities(
							processAndReturnSchema(this.dataGenerator.getSampleParameterGroupSchema(session)),
							this.randomEntityPicker,
							SEED
						)
						.limit(20)
						.forEach(it -> {
							processEntity(it.toInstance());
							createEntity(session, generatedEntities, it);
						});

					this.productSchema = dataGenerator.getSampleProductSchema(
						session,
						productSchemaBuilder -> productSchemaBuilder.withReferenceToEntity(
							Entities.PARAMETER,
							whichIs -> whichIs.withGroupTypeRelatedToEntity(Entities.PARAMETER_GROUP).faceted()
						)
					);

					processSchema(productSchema);

					this.dataGenerator.generateEntities(
							this.productSchema,
							this.randomEntityPicker,
							SEED
						)
						.limit(PRODUCT_COUNT)
						.forEach(it -> {
							processEntity(it.toInstance());
							final EntityReference createdEntity = session.upsertEntity(it);
							processCreatedEntityReference(createdEntity);
						});

					session.goLiveAndClose();
				}
			);
		} else {
			System.out.println("\n\nReusing existing database ...");
			final long startLoading = System.nanoTime();
			// prepare database
			this.evita = createEvitaInstanceFromExistingData(catalogName);
			System.out.println("Database loaded in " + StringUtils.formatPreciseNano(System.nanoTime() - startLoading));
			final AtomicInteger totalEntityCount = new AtomicInteger();
			final AtomicInteger totalPriceCount = new AtomicInteger();
			final AtomicInteger totalAttributeCount = new AtomicInteger();
			final AtomicInteger totalAssociatedDataCount = new AtomicInteger();
			final AtomicInteger totalReferenceCount = new AtomicInteger();
			// read and process all existing entities
			this.evita.queryCatalog(
				getCatalogName(),
				session -> {
					final long processingStart = System.nanoTime();
					final Serializable[] entityTypes = session.getAllEntityTypes().stream().sorted().toArray(Serializable[]::new);
					for (Serializable entityType : entityTypes) {
						final long entityProcessingStart = System.nanoTime();
						final EntitySchema entitySchema = session.getEntitySchema(entityType);
						Assert.notNull(entitySchema, "Schema for entity `" + entityType + "` was not found!");

						if (Entities.PRODUCT.equals(entitySchema.getName())) {
							this.productSchema = entitySchema;
						}

						processSchema(entitySchema);

						EvitaResponseBase<SealedEntity> response;
						int pageNumber = 1;
						final AtomicInteger entityCount = new AtomicInteger();
						final AtomicInteger priceCount = new AtomicInteger();
						final AtomicInteger attributeCount = new AtomicInteger();
						final AtomicInteger associatedDataCount = new AtomicInteger();
						final AtomicInteger referenceCount = new AtomicInteger();
						do {
							response = session.query(
								Query.query(
									entities(entityType),
									require(
										fullEntityAnd(
											page(pageNumber++, 1000)
										)
									)
								),
								SealedEntity.class
							);
							response
								.getRecordData()
								.forEach(it -> {
									processEntity(it);
									processCreatedEntityReference(new EntityReference(it.getType(), it.getPrimaryKey()));
									entityCount.incrementAndGet();
									priceCount.addAndGet(it.getPrices().size());
									attributeCount.addAndGet(it.getAttributeValues().size());
									associatedDataCount.addAndGet(it.getAssociatedDataValues().size());
									referenceCount.addAndGet(it.getReferences().size());
								});
							totalEntityCount.addAndGet(response.getRecordData().size());
						} while (response.getRecordPage().hasNext());

						totalPriceCount.addAndGet(priceCount.get());
						totalAttributeCount.addAndGet(attributeCount.get());
						totalAssociatedDataCount.addAndGet(associatedDataCount.get());
						totalReferenceCount.addAndGet(referenceCount.get());
						loadInfo.append("Entity `" + entityType + "` fully read and examined in " + StringUtils.formatPreciseNano(System.nanoTime() - entityProcessingStart) + "\n");
						of(entityCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append(        "\t - entity count          : " + it + "\n"));
						of(priceCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append(         "\t - price count           : " + it + "\n"));
						of(attributeCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append(     "\t - attribute count       : " + it + "\n"));
						of(associatedDataCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append("\t - associated data count : " + it + "\n"));
						of(referenceCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append("\t - reference count       : " + it + "\n"));
					}
					final double entitiesPerSec = (double) totalEntityCount.get() / (double) ((System.nanoTime() - processingStart) / 1_000_000_000);
					System.out.println("Entities (" + totalEntityCount.get() + ") processing speed " + entitiesPerSec + " recs/sec.");
					return null;
				}
			);
			loadInfo.append("\nSummary for " + totalEntityCount.get() + " entities:\n");
			of(totalPriceCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append(         "\t - price count           : " + it + "\n"));
			of(totalAttributeCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append(     "\t - attribute count       : " + it + "\n"));
			of(totalAssociatedDataCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append("\t - associated data count : " + it + "\n"));
			of(totalReferenceCount.get()).filter(it -> it > 0).ifPresent(it -> loadInfo.append("\t - reference count       : " + it + "\n"));
		}
		System.out.print("Database loaded in " + StringUtils.formatPreciseNano(System.nanoTime() - start) + "!\n" + loadInfo + "\nWarmup results: ");
	}

	/**
	 * Method closes evita database at the end of trial.
	 */
	@TearDown(Level.Trial)
	public void closeEvita() {
		this.evita.close();
	}

	/**
	 * Returns name of the test catalog.
	 */
	protected String getCatalogName() {
		return TEST_CATALOG;
	}

	/**
	 * Descendants may store reference to the schema if they want.
	 */
	protected void processSchema(EntitySchema schema) {
		// do nothing by default
	}

	/**
	 * Descendants may examine created entity if they want.
	 */
	protected void processEntity(SealedEntity entity) {
		// do nothing by default
	}

	/**
	 * Descendants may examine created entity if they want.
	 */
	protected void processCreatedEntityReference(EntityReference entity) {
		// do nothing by default
	}

	/**
	 * Method processes and returns exactly same schema.
	 */
	private EntitySchema processAndReturnSchema(EntitySchema schema) {
		processSchema(schema);
		return schema;
	}

}
