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

package io.evitadb.api.schema;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class SchemaInitializer {

    public static EntitySchema getSchema(int version, Serializable name, boolean withGeneratedPrimaryKey, boolean withHierarchy, boolean withPrice, int indexedPricePlaces, Set<Locale> locales, Map<String, AttributeSchema> attributes, Map<String, AssociatedDataSchema> associatedData, Map<Serializable, ReferenceSchema> references, Set<EvolutionMode> evolutionMode) {
        return new EntitySchema(version, name, withGeneratedPrimaryKey, withHierarchy, withPrice, indexedPricePlaces, locales, attributes, associatedData, references, evolutionMode);
    }
    public static ReferenceSchema getReferenceSchema(Serializable entityType, boolean entityTypeRelatesToEntity, Serializable groupType, boolean groupTypeRelatesToEntity, boolean indexed, boolean faceted, Map<String, AttributeSchema> attributes) {
        return new ReferenceSchema(entityType,entityTypeRelatesToEntity,groupType,groupTypeRelatesToEntity,indexed,faceted,attributes);
    }
}
