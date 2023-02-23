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

package io.evitadb.integration;

import io.evitadb.api.*;
import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.structure.CopyExistingEntityBuilder;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.test.TestConstants;
import io.evitadb.test.annotation.CatalogName;
import io.evitadb.test.snapshot.EntityConsumer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;

/**
 * This tests imports real production data from the Senesi.cz e-commerce site and verifies data contents after it has
 * been inserted into Evita DB implementation by requesting it back using primary key.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Tag(TestConstants.INTEGRATION_TEST)
@CatalogName("senesi")
@DisplayName("Senesi production set")
@Slf4j
public class SenesiSnapshotImportTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> extends AbstractImportTest<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION, EVITA> {

	@Nonnull
	@Override
	protected EntitySchema getSchema(EVITA evita, String catalogName, Serializable entityType) {
		return evita.queryCatalog(
			catalogName,
			evitaSession -> evitaSession.getEntitySchema(entityType)
		);
	}

	@Override
	protected int getEntityCount(EVITA evita, String catalogName, Serializable entityType) {
		return evita.queryCatalog(catalogName, evitaSession -> evitaSession.getEntityCollectionSize(entityType));
	}

	@Nonnull
	@Override
	protected EntityContract getEntity(EVITA evita, String catalogName, Serializable entityType, Integer primaryKey) {
		return evita.queryCatalog(
			catalogName,
			evitaSession -> {
				final EvitaResponseBase<EntityContract> entities = evitaSession.query(
					query(
						entities(entityType),
						filterBy(
							primaryKey(primaryKey)
						),
						require(
							fullEntity()
						)
					),
					EntityContract.class
				);
				return entities.getTotalRecordCount() == 1 ? entities.getRecordData().get(0) : null;
			}
		);
	}

	@Nonnull
	protected EntityConsumer getEntityCollectionContentsConsumer(EVITA evita, String catalogName) {
		return new EvitaEntityWriter(evita, catalogName);
	}

	private class EvitaEntityWriter implements EntityConsumer {
		private final int BATCH_SIZE = 1000;
		private final EVITA evita;
		private final String catalogName;
		private final List<Entity> entities = new ArrayList<>(BATCH_SIZE);
		private Exception encountered;

		public EvitaEntityWriter(EVITA evita, String catalogName) {
			this.evita = evita;
			this.catalogName = catalogName;
		}

		@Override
		public void setup(MutableCatalogEntityHeader header, EntitySchema schema) {
			evita.updateCatalog(catalogName, evitaSession -> {
				evitaSession.defineSchema(schema);
			});
		}

		@Override
		public boolean accept(EntitySchema schema, Entity entity) {
			entities.add(entity);
			if (entities.size() == BATCH_SIZE) {
				flush();
			}
			return true;
		}

		@Override
		public void close() {
			if (encountered == null) {
				flush();
			}
		}

		private void flush() {
			evita.updateCatalog(catalogName, evitaSession -> {
				for (Entity entity : entities) {
					try {
						evitaSession.upsertEntity(new CopyExistingEntityBuilder(entity));
					} catch (Exception ex) {
						log.error("Failed to upsert entity " + entity.getType() + " " + entity.getPrimaryKey(), ex);
						encountered = ex;
						throw ex;
					}
				}
				entities.clear();
			});
		}
	}
}
