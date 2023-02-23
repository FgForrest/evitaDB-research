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

package io.evitadb.storage.serialization.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.evitadb.api.dataType.ComplexDataObject;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.storage.model.EsEnumWrapper;
import io.evitadb.storage.model.FacetReferenceDto;
import io.evitadb.storage.pmptt.EsHierarchyStorage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemWithHistory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class EvitaSerializers {

    /**
     * Deserializes {@link ComplexDataObject.EmptyValue}.
     */
    public static class EmptyValueSerializer extends JsonSerializer<ComplexDataObject.EmptyValue> {
        @Override
        public void serialize(ComplexDataObject.EmptyValue value, JsonGenerator gen, SerializerProvider serializers) {
            // do not serialize at all
        }
    }

    public static class EsEnumSerializer<T extends Enum<T>> extends JsonSerializer<Enum<T>> {
        @Override
        public void serialize(Enum<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            new EsEnumWrapperSerializer<T>().serialize(new EsEnumWrapper<>(value), gen, serializers);
        }
    }

    public static class HierarchySerializer extends JsonSerializer<Hierarchy> {
        @Override
        public void serialize(Hierarchy value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            EsHierarchyStorage storage = (EsHierarchyStorage) value.getStorage();

            short sectionSize = value.getSectionSize();
            short levels = value.getLevels();
            String code = value.getCode();

            EsHierarchyStorage newStorage = new EsHierarchyStorage();
            HashMap<String, EsHierarchyStorage.HierarchyWithContents> map = new HashMap<>();
            map.put(code, storage.getHierarchyIndex().get(code));
            newStorage.setHierarchyIndex(map);
            Hierarchy hierarchy = new Hierarchy(code, (short) (levels - 1), (short) (sectionSize - 1));
            hierarchy.setStorage(newStorage);
            gen.writeStartObject();
            Set<HierarchyItem> rootItems = new HashSet<>(hierarchy.getRootItems());
            gen.writeFieldName("rootItems");
            writeItems(rootItems, gen, hierarchy);
            gen.writeObjectField("code", code);
            gen.writeObjectField("levels", levels);
            gen.writeObjectField("sectionSize", sectionSize);
            gen.writeEndObject();
        }
    }

    private static void writeItems(Set<HierarchyItem> rootItems, JsonGenerator gen, Hierarchy hierarchy) throws IOException {
        gen.writeStartArray();
        for (HierarchyItem rootItem : rootItems) {
            gen.writeStartObject();
            gen.writeObjectField("code", rootItem.getCode());
            gen.writeFieldName("items");
            writeItems(new HashSet<>(hierarchy.getChildItems(rootItem.getCode())), gen, hierarchy);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    public static class HierarchyItemWithHistorySerializer extends JsonSerializer<HierarchyItemWithHistory> {
        @Override
        public void serialize(HierarchyItemWithHistory value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("delegate", value.getDelegate());
            gen.writeEndObject();
        }
    }

    public static class EsEnumWrapperSerializer<T extends Enum<T>> extends JsonSerializer<EsEnumWrapper<T>> {
        @Override
        public void serialize(EsEnumWrapper<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("valueClass", value.getEnumClass());
            gen.writeObjectField("value", value.getValue());
            gen.writeEndObject();
        }
    }

    /**
     * Deserializes {@link ComplexDataObject.NullValue}.
     */
    public static class NullValueSerializer extends JsonSerializer<ComplexDataObject.NullValue> {
        @Override
        public void serialize(ComplexDataObject.NullValue value, JsonGenerator gen, SerializerProvider serializers) {
            // do not serialize at all
        }
    }

    public static class EntitySchemaSerializer extends JsonSerializer<EntitySchema> {

        @Override
        public void serialize(EntitySchema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeStartObject();
            gen.writeObjectField("version", value.getVersion());
            gen.writeObjectField("name", new EsEnumWrapper<>(value.getName()));
            gen.writeObjectField("withGeneratedPrimaryKey", value.isWithGeneratedPrimaryKey());
            gen.writeObjectField("withHierarchy", value.isWithHierarchy());
            gen.writeObjectField("withPrice", value.isWithPrice());
            gen.writeObjectField("indexedPricePlaces", value.getIndexedPricePlaces());
            gen.writeObjectField("locales", value.getLocales());
            gen.writeObjectField("attributes", value.getAttributes());
            gen.writeObjectField("associatedData", value.getAssociatedData());
            gen.writeObjectField("references", value.getReferences());
            gen.writeObjectField("evolutionMode", value.getEvolutionMode());
            gen.writeEndObject();
        }
    }

    public static class ReferenceSchemaSerializer extends JsonSerializer<ReferenceSchema> {

        @Override
        public void serialize(ReferenceSchema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeStartObject();

            gen.writeObjectField("entityType", new EsEnumWrapper<>(value.getEntityType()));
            gen.writeObjectField("entityTypeRelatesToEntity", value.isEntityTypeRelatesToEntity());
            gen.writeObjectField("groupType", value.getGroupType());
            gen.writeObjectField("groupTypeRelatesToEntity", value.isGroupTypeRelatesToEntity());
            gen.writeObjectField("indexed", value.isIndexed());
            gen.writeObjectField("faceted", value.isFaceted());
            gen.writeObjectField("attributes", value.getAttributes());

            gen.writeEndObject();


        }
    }

    public static class FacetReferenceDtoSerializer extends JsonSerializer<FacetReferenceDto> {

        @Override
        public void serialize(FacetReferenceDto value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            gen.writeStartObject();
            gen.writeObjectField("type", new EsEnumWrapper<>(value.getType()));
            gen.writeObjectField("primaryKey", value.getPrimaryKey());
            if (value.getGroupType() != null) gen.writeObjectField("groupType", new EsEnumWrapper<>(value.getGroupType()));
            if (value.getGroupPrimaryKey() != null) gen.writeObjectField("groupPrimaryKey", value.getGroupPrimaryKey());
            gen.writeObjectField("simpleType", new EsEnumWrapper<>(value.getType()).getValue());
            gen.writeObjectField("usedInEntities", value.getUsedInEntities());
            gen.writeEndObject();

        }
    }

}
