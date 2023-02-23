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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import io.evitadb.storage.model.EsEnumWrapper;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.*;

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class EsMapDeserializer extends MapDeserializer {
    public EsMapDeserializer(MapDeserializer deserializer) {
        super(deserializer);
    }

    public EsMapDeserializer(EsMapDeserializer deserializer, KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser, TypeDeserializer valueTypeDeser, NullValueProvider nuller, Set<String> ignorable, Set<String> includable) {
        super(deserializer, keyDeser, valueDeser, valueTypeDeser, nuller, ignorable, includable);
    }

    @Override
    protected MapDeserializer withResolved(KeyDeserializer keyDeser, TypeDeserializer valueTypeDeser, JsonDeserializer<?> valueDeser, NullValueProvider nuller, Set<String> ignorable, Set<String> includable) {

        if ((_keyDeserializer == keyDeser) && (_valueDeserializer == valueDeser)
                && (_valueTypeDeserializer == valueTypeDeser) && (_nullProvider == nuller)
                && (_ignorableProperties == ignorable) && (_includableProperties == includable)) {
            return this;
        }

        return new EsMapDeserializer(this,
                keyDeser, (JsonDeserializer<Object>) valueDeser, valueTypeDeser,
                nuller, ignorable, includable);
    }

    @SneakyThrows
    @Override
    public Map deserialize(JsonParser p, DeserializationContext ctxt)  {

        Map<Serializable,Serializable> typesMap = new LinkedHashMap<>();
        JsonParser parser = ctxt.getParser();
        ObjectCodec codec = parser.getCodec();
        final JsonNode treeNode = codec.readTree(p);
        JsonNode typesNode = treeNode.get("types");
        Map<Serializable,Object> finalMap = new HashMap<>();
        JsonNode mapNode;
        if (typesNode != null){

            for (Iterator<String> it = typesNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                String className = typesNode.get(key).asText();
                Class<?> classVar = className == null || className.equals("null")
                        ? null
                        : Class.forName(className);
                EsEnumWrapper<?> esEnumWrapper = classVar == null ? new EsEnumWrapper<>(key) : new EsEnumWrapper(classVar, key);
                typesMap.put(key, esEnumWrapper.getEnumValue());
            }
            mapNode = treeNode.get("mapItself");
        }else {
            mapNode = treeNode;
        }

        for (Iterator<String> it = mapNode.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            JsonParser jsonParser = mapNode.get(key).traverse(codec);
            jsonParser.nextToken();
            Object deserializedValue = _valueDeserializer.deserialize(jsonParser, ctxt);
            Serializable typedKey = typesMap.get(key);
            finalMap.put(
                    typedKey == null ?  key : typedKey,
                    deserializedValue
            );
        }
        return finalMap;

    }


}
