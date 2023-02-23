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

package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.evitadb.storage.model.EsEnumWrapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class EsMapSerializer extends MapSerializer {
    public EsMapSerializer(MapSerializer src) {
        super(src, src._filterId, src._sortKeys);
    }

    public EsMapSerializer(MapSerializer src, BeanProperty property,
                           JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer,
                           Set<String> ignoredEntries, Set<String> includedEntries) {
        super(src, property, keySerializer, valueSerializer, ignoredEntries, includedEntries);
    }

    public EsMapSerializer(MapSerializer ser, Object filterId, boolean sortKeys) {
        super(ser, filterId, sortKeys);
    }

    @Override
    public MapSerializer withResolved(BeanProperty property, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer, Set<String> ignored, Set<String> included, boolean sortKeys) {
        MapSerializer ser = new EsMapSerializer(this, property, keySerializer, valueSerializer, ignored, included);
        if (sortKeys != ser._sortKeys) {
            ser = new EsMapSerializer(ser, _filterId, sortKeys);
        }
        return ser;

    }

    @Override
    public void serialize(Map value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeFieldName("types");
        gen.writeStartObject();
        for (Object obj : value.entrySet()) {
            Map.Entry<?,?> entry = (Map.Entry<?,?>) obj;
            EsEnumWrapper<?> esEnumWrapper = new EsEnumWrapper<>((Serializable) entry.getKey());
            gen.writeObjectField(esEnumWrapper.getValue(), esEnumWrapper.getEnumClass());
        }
        gen.writeEndObject();

        gen.writeFieldName("mapItself");
        super.serialize(value, gen, serializers);

        gen.writeEndObject();
    }

}
