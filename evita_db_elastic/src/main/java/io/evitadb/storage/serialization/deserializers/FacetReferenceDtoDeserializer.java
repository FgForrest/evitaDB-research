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

package io.evitadb.storage.serialization.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.evitadb.storage.model.EsEnumWrapper;
import io.evitadb.storage.model.FacetReferenceDto;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

/**
 * No extra information provided, if you see this, code is my very best work,
 * so each method is self-explanatory and description would be useless.
 * If this code is not masterpiece, there wasn't time to write proper code
 * and not even documentation, so so sorry.
 *
 * @author Štěpán Kameník (kamenik@fg.cz), FG Forrest a.s. (c) 2022
 **/
public class FacetReferenceDtoDeserializer extends JsonDeserializer<FacetReferenceDto> {
    @Override
    public FacetReferenceDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        JsonParser parser = ctxt.getParser();
        ObjectCodec codec = parser.getCodec();
        final JsonNode treeNode = codec.readTree(parser);

        Serializable typeValue = codec.readValue(treeNode.get("type").traverse(codec), EsEnumWrapper.class).getEnumValue();
        Set<String> usedInEntities = codec.readValue(treeNode.get("usedInEntities").traverse(codec), new TypeReference<>() {});
        int primaryKey = treeNode.get("primaryKey").asInt();

        JsonNode groupType = treeNode.get("groupType");
        Serializable groupTypeValue = groupType == null ? null : codec.readValue(groupType.traverse(codec), EsEnumWrapper.class).getEnumValue();

        Integer groupPrimaryKey = Optional.ofNullable(treeNode.get("groupPrimaryKey")).map(JsonNode::asInt).orElse(null);

        return new FacetReferenceDto(typeValue, primaryKey, groupTypeValue, groupPrimaryKey,usedInEntities);
    }

}
