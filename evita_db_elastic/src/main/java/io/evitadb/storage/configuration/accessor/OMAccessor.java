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

package io.evitadb.storage.configuration.accessor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.evitadb.api.data.*;
import io.evitadb.api.data.structure.*;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.trie.Trie;
import io.evitadb.api.dataType.trie.TrieNode;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.storage.serialization.EvitaJacksonModule;
import io.evitadb.storage.serialization.mixins.EntityMixin;
import io.evitadb.storage.serialization.mixins.EvitaMixins;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class OMAccessor {
    private static ObjectMapper objectMapper;
    private static ObjectMapper schemaObjectMapper;

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null){
            objectMapper = initOM(JsonInclude.Include.NON_ABSENT);
        }
        return objectMapper;
    }
    public static ObjectMapper getSchemaObjectMapper() {
        if (schemaObjectMapper == null){
            schemaObjectMapper = initOM(JsonInclude.Include.NON_DEFAULT);
        }
        return schemaObjectMapper;
    }

    private static ObjectMapper initOM(JsonInclude.Include include){
        // config mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new EvitaJacksonModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.setDefaultPropertyInclusion(include);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);

        // add mixins
        mapper.addMixIn(Entity.class, EntityMixin.class);
        mapper.addMixIn(EntitySchema.class, EvitaMixins.EntitySchemaMixin.class);
        mapper.addMixIn(AttributeSchema.class, EvitaMixins.AttributeSchemaMixin.class);
        mapper.addMixIn(AssociatedDataSchema.class, EvitaMixins.AssociatedDataSchemaMixin.class);
        mapper.addMixIn(Attributes.class, EvitaMixins.AttributesMixin.class);
        mapper.addMixIn(AttributesContract.AttributeValue.class, EvitaMixins.AttributeValueMixin.class);
        mapper.addMixIn(AttributesContract.AttributeKey.class, EvitaMixins.AttributeKeyMixin.class);
        mapper.addMixIn(AssociatedDataContract.AssociatedDataValue.class, EvitaMixins.AssociatedDataValueMixin.class);
        mapper.addMixIn(AssociatedDataContract.AssociatedDataKey.class, EvitaMixins.AssociatedDataKeyMixin.class);
        mapper.addMixIn(AssociatedData.class, EvitaMixins.AssociatedDataMixin.class);
        mapper.addMixIn(Prices.class, EvitaMixins.PricesMixin.class);
        mapper.addMixIn(Price.class, EvitaMixins.PriceMixin.class);
        mapper.addMixIn(Price.PriceKey.class, EvitaMixins.PriceKeyMixin.class);
        mapper.addMixIn(ReferenceSchema.class, EvitaMixins.ReferenceSchemaMixin.class);
        mapper.addMixIn(Reference.class, EvitaMixins.ReferenceMixin.class);
        mapper.addMixIn(HierarchicalPlacementContract.class, EvitaMixins.HierarchicalPlacementContractMixin.class);
        mapper.addMixIn(ReferenceContract.GroupEntityReference.class, EvitaMixins.GroupEntityReferenceMixin.class);
        mapper.addMixIn(EntityReferenceContract.class, EvitaMixins.EntityReferenceContractMixin.class);
        mapper.addMixIn(EntityReference.class, EvitaMixins.EntityReferenceMixin.class);
        mapper.addMixIn(Trie.class, EvitaMixins.TrieMixin.class);
        mapper.addMixIn(TrieNode.class, EvitaMixins.TrieNodeMixin.class);
        mapper.addMixIn(DateTimeRange.class, EvitaMixins.DateTimeRangeMixin.class);

        return mapper;
    }
}
