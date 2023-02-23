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

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.storage.serialization.kryo.EntityKryoManager;
import io.evitadb.storage.serialization.kryo.EntitySchemaKryoManager;
import lombok.Getter;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds all data needed throughout entity collection instance.
 */
@Getter
public class EntityCollectionContext {

    /**
     * Context of parent catalog
     */
    private final CatalogContext catalogCtx;
    private final String uid;
    private final Serializable entityType;
    private final AtomicReference<EntitySchema> entitySchema;
    /**
     * Flag stating if this collection is in transactional mode (not bulk mode).
     */
    private final AtomicBoolean transactionMode;
    /**
     * Kryo manager for efficient serialization of entities
     */
    private final EntityKryoManager entityKryoManager;
    /**
     * Kryo manager for entity schema serialization
     */
    private final EntitySchemaKryoManager entitySchemaKryoManager;

    public EntityCollectionContext(@Nonnull CatalogContext catalogCtx,
                                   @Nonnull String uid,
                                   @Nonnull Serializable entityType,
                                   @Nonnull AtomicReference<EntitySchema> entitySchema,
                                   @Nonnull AtomicBoolean transactionMode,
                                   @Nullable byte[] serializationHeader) {
        this.catalogCtx = catalogCtx;
        this.uid = uid;
        this.entityType = entityType;
        this.entitySchema = entitySchema;
        this.transactionMode = transactionMode;
        this.entityKryoManager = new EntityKryoManager(entitySchema, serializationHeader);
        this.entitySchemaKryoManager = new EntitySchemaKryoManager();
    }

    public String getSerializedEntityType() {
        return catalogCtx.getSerializedEntityType(entityType);
    }
}
