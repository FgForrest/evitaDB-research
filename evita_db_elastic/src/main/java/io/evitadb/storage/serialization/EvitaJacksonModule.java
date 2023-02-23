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

package io.evitadb.storage.serialization;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.EsMapDeserializer;
import com.fasterxml.jackson.databind.ser.std.EsMapSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.MapType;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.data.HierarchicalPlacementContract;
import io.evitadb.api.data.structure.*;
import io.evitadb.api.dataType.ComplexDataObject;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.storage.model.EntityWrapper;
import io.evitadb.storage.model.EsEnumWrapper;
import io.evitadb.storage.model.FacetReferenceDto;
import io.evitadb.storage.serialization.deserializers.EvitaDeserializers;
import io.evitadb.storage.serialization.deserializers.EvitaDeserializers.*;
import io.evitadb.storage.serialization.deserializers.FacetReferenceDtoDeserializer;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer;
import io.evitadb.storage.serialization.serializers.EvitaSerializers.*;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItemWithHistory;

/**
 * Basic modules which registers serializers
 * and deserializers for Evita implementations.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@CommonsLog
public class EvitaJacksonModule extends SimpleModule {


    @SuppressWarnings({"rawtypes", "unchecked"})
    public EvitaJacksonModule() {
        addSerializer(EntityWrapper.class, new EntityJsonSerializer());

        addKeyDeserializer(
                Price.PriceKey.class,
                new PriceKeyDeserializer());
        addDeserializer(
                EntityReferenceContract.class,
                new EvitaDeserializers.EntityReferenceContractDeserializer());

        addDeserializer(
                Entity.class,
                new EvitaDeserializers.EntityDeserializer());
        addDeserializer(
                Attributes.class,
                new AttributesDeserializer());

        setDeserializerModifier(new BeanDeserializerModifier()
        {
            @Override
            public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {

                if (deserializer instanceof MapDeserializer){
                    return new EsMapDeserializer((MapDeserializer) deserializer);
                }
                return deserializer;
            }
        });
        setSerializerModifier(new BeanSerializerModifier()
        {

            @Override
            public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {

                if (serializer instanceof MapSerializer) {
                    MapSerializer mapSerializer = (MapSerializer) serializer;
                    return new EsMapSerializer(mapSerializer);
                }
                return serializer;
            }
        });

        addSerializer(
                EntitySchema.class,
                new EntitySchemaSerializer());
        addDeserializer(
                EntitySchema.class,
                new EntitySchemaDeserializer());
        addSerializer(
                FacetReferenceDto.class,
                new FacetReferenceDtoSerializer());
        addDeserializer(
                FacetReferenceDto.class,
                new FacetReferenceDtoDeserializer());
        addSerializer(
                ReferenceSchema.class,
                new ReferenceSchemaSerializer());
        addDeserializer(
                ReferenceSchema.class,
                new ReferenceSchemaDeserializer());
        addDeserializer(
                Hierarchy.class,
                new HierarchyDeserializer());
        addDeserializer(
                ReflectionLookup.class,
                new ReflectionLookupDeserializer());
        addDeserializer(
                HierarchicalPlacementContract.class,
                new HierarchicalPlacementContractDeserializer());
        addDeserializer(
                Prices.class,
                new PricesDeserializer());
        addDeserializer(
                AssociatedData.class,
                new AssociatedDataDeserializer());

        addSerializer(
                ComplexDataObject.EmptyValue.class,
                new EmptyValueSerializer());
        addSerializer(
                EsEnumWrapper.class,
                new EsEnumWrapperSerializer());
        addDeserializer(
                EsEnumWrapper.class,
                new EsEnumWrapperDeserializer<>());
        addSerializer(
                Enum.class,
                new EsEnumSerializer());

        addSerializer(
                ComplexDataObject.NullValue.class,
                new NullValueSerializer());

        addSerializer(
                Hierarchy.class,
                new HierarchySerializer());
        addSerializer(
                HierarchyItemWithHistory.class,
                new HierarchyItemWithHistorySerializer());

    }


}
