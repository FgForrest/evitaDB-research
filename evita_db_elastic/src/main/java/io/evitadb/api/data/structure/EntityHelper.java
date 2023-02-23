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

import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.schema.EntitySchema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Util class that allows es implementation to create new {@link Entity} from existing one and new scheme or from pure data.
 * Also can be used for other transformation functions that will help us work with Evita model.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class EntityHelper {

    /**
     * Creates new entity from old one and new (other) scheme.
     */
    public static Entity withUpdatedSchema(Entity entity, EntitySchema currentSchema) {
        return new Entity(
                entity.version,
                currentSchema,
                entity.primaryKey,
                entity.hierarchicalPlacement,
                entity.references.values(),
                entity.attributes,
                entity.associatedData,
                entity.prices,
                entity.locales,
                entity.isDropped()
        );
    }

    /**
     * Creates new entity from data (parameters).
     */
    public static Entity newEntity(DataKeeper dk) {
        return new Entity(
                dk.getVersion(),
                dk.getSchema(),
                dk.getPrimaryKey(),
                dk.getHierarchicalPlacement(),
                dk.getReferences(),
                dk.getAttributes(),
                dk.getAssociatedData(),
                dk.getPrices(),
                dk.getLocales(),
                dk.isDropped()
        );
    }
    @AllArgsConstructor
    @Data
    public  static class DataKeeper{
        private int version;
        private EntitySchema schema;
        private int primaryKey;
        private HierarchicalPlacementContract hierarchicalPlacement;
        private Collection<ReferenceContract> references;
        private Attributes attributes;
        private AssociatedData associatedData;
        private Prices prices;
        private Set<Locale> locales;
        private boolean dropped;
    }
}
