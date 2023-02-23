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
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import io.evitadb.api.data.*;
import io.evitadb.api.data.structure.*;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.schema.*;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.storage.exception.EvitaGetException;
import io.evitadb.storage.model.EsEnumWrapper;
import io.evitadb.storage.pmptt.EsHierarchyStorage;
import io.evitadb.storage.schema.SchemaRepository;
import io.evitadb.storage.serialization.serializers.EntityJsonSerializer;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.evitadb.storage.serialization.serializers.EntityJsonSerializer.*;
import static java.util.Optional.ofNullable;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@CommonsLog
public class EvitaDeserializers {
    public static final String INDEX_NAME_ATTR = "indexName";

    private EvitaDeserializers() {
    }
// DESERIALIZATION

    /**
     * Deserializes {@link HierarchicalPlacement}.
     */
    public static class HierarchicalPlacementContractDeserializer extends JsonDeserializer<HierarchicalPlacementContract> {
        @Override
        public HierarchicalPlacementContract deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            final JsonNode treeNode = parser.getCodec().readTree(parser);
            JsonNode parentPrimaryKeyNode = treeNode.get("parentPrimaryKey");
            Integer parentPrimaryKey = parentPrimaryKeyNode == null ? null : parentPrimaryKeyNode.asInt();
            return new HierarchicalPlacement(
                    treeNode.get(VERSION).asInt(),
                    parentPrimaryKey,
                    treeNode.get("orderAmongSiblings").asInt(),
                    treeNode.get(DROPPED).asBoolean()
            );
        }
    }

    public static class EsEnumWrapperDeserializer<T extends Enum<T>> extends JsonDeserializer<EsEnumWrapper<T>> {
        @Override
        public EsEnumWrapper<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);
            if (treeNode instanceof NullNode || treeNode == null || treeNode.isEmpty()) {
                return null;
            }
            if (treeNode.isTextual()) {
                return new EsEnumWrapper<>(treeNode.asText());
            }
            Class<T> enumClass = codec.readValue(treeNode.get("valueClass").traverse(codec), new TypeReference<>() {
            });
            String value = treeNode.get(VALUE).asText();
            return new EsEnumWrapper<>(enumClass, value);
        }
    }


    /**
     * Deserializes {@link ReflectionLookup} - should not return anything.
     */
    public static class ReflectionLookupDeserializer extends JsonDeserializer<ReflectionLookup> {
        @Override
        public ReflectionLookup deserialize(JsonParser p, DeserializationContext ctxt) {
            return null;
        }
    }

    /**
     * Deserializes {@link EntityReference}
     */
    public static class PriceKeyDeserializer extends KeyDeserializer {
        @Override
        public Price.PriceKey deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            JsonParser parser = ctxt.getParser();
            final JsonNode treeNode = parser.getCodec().readTree(parser);
            //Use the string key here to return a real map key object
            return new Price.PriceKey(treeNode.get(PRICE_ID).asInt(), treeNode.get(PRICE_LIST).asText(), Currency.getInstance(treeNode.get(CURRENCY).asText()));
        }
    }

    /**
     * Deserializes {@link EntityReference}
     */
    public static class EntityReferenceContractDeserializer extends JsonDeserializer<EntityReferenceContract> {

        @Override
        public EntityReferenceContract deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);
            if (treeNode != null) {
                JsonNode version = treeNode.get(VERSION);
                JsonNode typeNode = treeNode.get("type");
                Serializable type = codec.readValue(typeNode.traverse(codec), EsEnumWrapper.class).getEnumValue();
                if (version != null) {
                    //Use the string key here to return a real map key object
                    return new ReferenceContract.GroupEntityReference(
                            version.asInt(),
                            type,
                            treeNode.get(PRIMARY_KEY).asInt(),
                            treeNode.get(DROPPED).asBoolean());
                } else {
                    //Use the string key here to return a real map key object
                    return new EntityReference(type, treeNode.get(PRIMARY_KEY).asInt());
                }
            }
            return null;
        }
    }

    /**
     * Deserializes {@link Price}
     */
    public static class PricesDeserializer extends JsonDeserializer<Prices> {
        @Override
        public Prices deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);

            List<PriceContract> prices = new LinkedList<>();
            for (JsonNode i : treeNode.get("prices")) {
                PriceContract priceContract = new Price(
                        i.get(VERSION).asInt(),
                        new Price.PriceKey(i.get(PRICE_ID).asInt(), i.get(PRICE_LIST).asText(), Currency.getInstance(i.get(CURRENCY).asText())),
                        ofNullable(i.get("innerRecordId")).map(JsonNode::asInt).orElse(null),
                        new BigDecimal(i.get("priceWithoutVat").asText()),
                        new BigDecimal(i.get("vat").asText()),
                        new BigDecimal(i.get("priceWithVat").asText()),
                        parseValidity(i.get("validity"), codec),
//                        Optional.ofNullable(i.get("validity")).map(JsonNode::asText).filter(k -> !k.equals("") && !k.equals("null")).map(DateTimeRange::fromString).orElse(null),
                        i.get(INDEXED).asBoolean(),
                        i.get(DROPPED).asBoolean()
                );
                prices.add(priceContract);
            }
            PriceInnerRecordHandling priceInnerRecordHandling = (PriceInnerRecordHandling) codec.readValue(treeNode.get("priceInnerRecordHandling").traverse(codec), EsEnumWrapper.class).getEnumValue();
            return new Prices(treeNode.get(VERSION).asInt(), prices, priceInnerRecordHandling);
        }
    }

    private static DateTimeRange parseValidity(JsonNode validity, ObjectCodec codec) throws IOException {
        if (validity == null || validity instanceof NullNode) {
            return null;
        }

        long dateFrom = validity.get("gte").asLong();
        ZoneId dateFromZone = codec.readValue(validity.get("dateFromZone").traverse(codec), ZoneId.class);
        long dateTo = validity.get("lte").asLong();
        ZoneId dateToZone = codec.readValue(validity.get("dateToZone").traverse(codec), ZoneId.class);
        return DateTimeRange.between(
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(dateFrom), dateFromZone),
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(dateTo), dateToZone));
    }

    public static class AssociatedDataDeserializer extends JsonDeserializer<AssociatedData> {
        @Override
        public AssociatedData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);

            Serializable typeValue = codec.readValue(treeNode.get("type").traverse(codec), EsEnumWrapper.class).getEnumValue();
            String indexName = treeNode.get(INDEX_NAME_ATTR).asText();
            EntitySchema entitySchema = SchemaRepository.getEntitySchema(indexName, typeValue);

            JsonNode dataValues = treeNode.get("associatedDataValues");
            List<AssociatedDataContract.AssociatedDataValue> associatedData = new ArrayList<>(dataValues.size());
            for (JsonNode next : dataValues) {
                AssociatedDataContract.AssociatedDataKey key = codec.readValue(next.get("key").traverse(codec), AssociatedDataContract.AssociatedDataKey.class);
                Class<? extends Serializable> type = entitySchema.getAssociatedData(key.getAssociatedDataName()).getType();
                Serializable value = codec.readValue(next.get(VALUE).traverse(codec), type);
                associatedData.add(
                        new AssociatedDataContract.AssociatedDataValue(
                                next.get(VERSION).asInt(),
                                key,
                                value,
                                next.get(DROPPED).asBoolean())
                );

            }
            //Use the string key here to return a real map key object
            return new AssociatedData(
                    entitySchema,
                    new ReflectionLookup(ReflectionCachingBehaviour.CACHE),
                    associatedData);

        }
    }


    @SuppressWarnings("rawtypes")
    public static class EntityDeserializer extends JsonDeserializer<Entity> {
        @Override
        public Entity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);

            int version = treeNode.get(VERSION).asInt();
            int primaryKey = treeNode.get(PRIMARY_KEY).asInt();
            boolean dropped = treeNode.get(DROPPED).asBoolean();
            Serializable type = codec.readValue(treeNode.get("type").traverse(codec), EsEnumWrapper.class).getEnumValue();
            String indexName = treeNode.get(INDEX_NAME_ATTR).asText();
            EntitySchema schema = SchemaRepository.getEntitySchema(indexName, type);


            JsonNode hierarchicalPlacementNode = treeNode.get("hierarchicalPlacement");
            HierarchicalPlacementContract hierarchicalPlacement = hierarchicalPlacementNode == null ?
                    null :
                    codec.readValue(hierarchicalPlacementNode.traverse(codec), HierarchicalPlacementContract.class);
            List<ReferenceContract> references = readReferences(parser, treeNode.get("references"), schema);
            JsonNode attributesNode = treeNode.get("attribute");
            Attributes attributes = attributesNode == null ? new Attributes(schema) : readAttributes(attributesNode, codec, null, schema);
            JsonNode associatedDataNode = treeNode.get("associatedData");
            AssociatedData associatedData = associatedDataNode == null ?
                    new AssociatedData(schema, new ReflectionLookup(ReflectionCachingBehaviour.CACHE)) :
                    codec.readValue(associatedDataNode.traverse(codec), AssociatedData.class);
            JsonNode pricesNode = treeNode.get("prices");
            JsonNode notSellablePricesNode = treeNode.get("notSellablePrices");
            Prices prices = pricesNode == null && notSellablePricesNode == null ? new Prices(PriceInnerRecordHandling.NONE) : readPrices(parser, pricesNode,notSellablePricesNode, treeNode);
            JsonNode localesNode = treeNode.get("locales");
            Set<Locale> locales = localesNode == null ? Collections.emptySet() : codec.readValue(localesNode.traverse(codec), new TypeReference<>() {
            });


            return EntityHelper.newEntity(
                    new EntityHelper.DataKeeper(version, schema, primaryKey, hierarchicalPlacement, references, attributes, associatedData, prices, locales, dropped)
            );
        }

        public List<ReferenceContract> readReferences(JsonParser parser, JsonNode referencesNode, EntitySchema schema) throws IOException {
            if (referencesNode == null) return Collections.emptyList();
            List<ReferenceContract> references = new ArrayList<>(referencesNode.size());
            ObjectCodec codec = parser.getCodec();
            for (JsonNode jsonNode : referencesNode) {

                EntityReference referencedEntity;
                EsEnumWrapper referencedEntityType = codec.readValue(jsonNode.get("referencedEntityType").traverse(codec), EsEnumWrapper.class);
                int referencedEntityPrimaryKey = jsonNode.get("referencedEntityPrimaryKey").asInt();
                JsonNode referencedEntityDroppedNode = jsonNode.get("referencedEntityDropped");
                if (referencedEntityDroppedNode != null){
                    int referencedEntityVersion = jsonNode.get("referencedEntityVersion").asInt();
                    boolean referencedEntityDropped = referencedEntityDroppedNode.asBoolean();
                    referencedEntity = new ReferenceContract.GroupEntityReference(referencedEntityVersion,referencedEntityType.getEnumValue(),referencedEntityPrimaryKey,referencedEntityDropped);
                }else {
                    referencedEntity = new EntityReference(referencedEntityType.getEnumValue(),referencedEntityPrimaryKey);
                }

                Reference reference = new Reference(
                        schema,
                        jsonNode.get(VERSION).asInt(),
                        referencedEntity,
                        readGroup(jsonNode, codec),
                        readAttributes(jsonNode.get("attribute"), codec, referencedEntity.getType(), schema),
                        jsonNode.get(DROPPED).asBoolean()
                );

                references.add(reference);
            }
            return references;
        }

        private ReferenceContract.GroupEntityReference readGroup(JsonNode group, ObjectCodec codec) throws IOException {
            JsonNode groupType = group.get("groupType");
            if (groupType == null) return null;
            Serializable type = codec.readValue(groupType.traverse(codec), EsEnumWrapper.class).getEnumValue();
            return new ReferenceContract.GroupEntityReference(
                    group.get("groupVersion").asInt(),
                    type,
                    group.get("groupPrimaryKey").asInt(),
                    group.get("groupDropped").asBoolean()
            );
        }

        public Prices readPrices(JsonParser parser, JsonNode pricesNode, JsonNode notSellablePricesNode, JsonNode entityNode) throws IOException {
            ObjectCodec codec = parser.getCodec();

            List<PriceContract> pricesList = new LinkedList<>();
            addPrice(pricesNode,codec,pricesList);
            addPrice(notSellablePricesNode,codec,pricesList);

            PriceInnerRecordHandling priceInnerRecordHandling = (PriceInnerRecordHandling) codec.readValue(entityNode.get("priceInnerRecordHandling").traverse(codec), EsEnumWrapper.class).getEnumValue();
            return new Prices(entityNode.get("priceVersion").asInt(), pricesList, priceInnerRecordHandling);
        }

        private void addPrice(JsonNode pricesNode, ObjectCodec codec,List<PriceContract> pricesList) throws IOException {

            if (pricesNode != null){
                for (JsonNode i : pricesNode) {
                    PriceContract priceContract = new Price(
                            i.get(VERSION).asInt(),
                            new Price.PriceKey(i.get(PRICE_ID).asInt(), i.get(PRICE_LIST).asText(), Currency.getInstance(i.get(CURRENCY).asText())),
                            ofNullable(i.get("innerRecordId")).map(JsonNode::asInt).orElse(null),
                            new BigDecimal(i.get("priceWithoutVat").asText()),
                            new BigDecimal(i.get("vat").asText()),
                            new BigDecimal(i.get("priceWithVat").asText()),
                            parseValidity(i.get("validity"), codec),
//                        Optional.ofNullable(i.get("validity")).map(JsonNode::asText).filter(k -> !k.equals("") && !k.equals("null")).map(DateTimeRange::fromString).orElse(null),
                            i.get(INDEXED).asBoolean(),
                            i.get(DROPPED).asBoolean()
                    );
                    pricesList.add(priceContract);
                }
            }
        }

    }

    public static class AttributesDeserializer extends JsonDeserializer<Attributes> {
        @Override
        public Attributes deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            JsonNode treeNode = codec.readTree(parser);

            Serializable type = codec.readValue(treeNode.get("type").traverse(codec), EsEnumWrapper.class).getEnumValue();
            String indexName = treeNode.get(INDEX_NAME_ATTR).asText();
            EntitySchema schema = SchemaRepository.getEntitySchema(indexName, type);
            return readAttributes(treeNode, codec, null, schema);
        }
    }


    public static Attributes readAttributes(JsonNode attributeValuesNode, ObjectCodec codec, Serializable type, EntitySchema schema) throws IOException {

        List<AttributesContract.AttributeValue> attributeValues = new ArrayList<>(attributeValuesNode.size());
        Map<String, AttributeSchema> attributeTypes = null;
        if (type != null) {
            ReferenceSchema reference = schema.getReference(type);
            if (reference != null) {
                attributeTypes = reference.getAttributes();
            } else {
                log.info("Reference is null for type :" + type);
            }
        }
        if (attributeTypes == null) {
            attributeTypes = schema.getAttributes();
        }

        for (JsonNode valueNode : attributeValuesNode) {
            String attributeName = valueNode.get("attributeName").asText();
            Locale locale = codec.readValue(valueNode.get("locale").traverse(codec), Locale.class);
            AttributesContract.AttributeKey attributeKey = new AttributesContract.AttributeKey(attributeName, locale);

            AttributeSchema attributeSchema = attributeTypes.get(attributeKey.getAttributeName());
            JsonNode value = valueNode.get(VALUE);
            if (attributeSchema != null) {
                Class<? extends Serializable> attributeSchemaType = attributeSchema.getType();
                Serializable valueFrom = EntityJsonSerializer.DescribableValue.getValueFrom(value, attributeSchemaType, codec);
                AttributesContract.AttributeValue attributeValue = new AttributesContract.AttributeValue(
                        valueNode.get(VERSION).asInt(),
                        attributeKey,
                        Objects.requireNonNull(valueFrom),
                        valueNode.get(DROPPED).asBoolean()
                );
                attributeValues.add(attributeValue);
            }

        }

        return new Attributes(schema, attributeValues, attributeValues.stream().map(i -> i.getKey().getLocale()).collect(Collectors.toSet()), attributeTypes);

    }


    public static class EntitySchemaDeserializer extends JsonDeserializer<EntitySchema> {
        @SneakyThrows
        @Override
        public EntitySchema deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);

            return SchemaInitializer.getSchema(
                    treeNode.get(VERSION).asInt(),
                    codec.readValue(treeNode.get("name").traverse(codec), EsEnumWrapper.class).getEnumValue(),
                    treeNode.get("withGeneratedPrimaryKey").asBoolean(),
                    treeNode.get("withHierarchy").asBoolean(),
                    treeNode.get("withPrice").asBoolean(),
                    treeNode.get("indexedPricePlaces").asInt(),
                    codec.readValue(treeNode.get("locales").traverse(codec), new TypeReference<>() {
                    }),
                    codec.readValue(treeNode.get("attributes").traverse(codec), new TypeReference<>() {
                    }),
                    codec.readValue(treeNode.get("associatedData").traverse(codec), new TypeReference<>() {
                    }),
                    codec.readValue(treeNode.get("references").traverse(codec), new TypeReference<>() {
                    }),
                    readEvolutionMode(treeNode.get("evolutionMode"), codec)
            );
        }

        public Set<EvolutionMode> readEvolutionMode(JsonNode evolutionMode, ObjectCodec codec) throws IOException {
            Set<EvolutionMode> evolutionModes = new HashSet<>();
            for (JsonNode jsonNode : evolutionMode) {
                evolutionModes.add((EvolutionMode) codec.readValue(jsonNode.traverse(codec), EsEnumWrapper.class).getEnumValue());
            }
            return evolutionModes;
        }
    }

    public static class HierarchyDeserializer extends JsonDeserializer<Hierarchy> {
        @SneakyThrows
        @Override
        public Hierarchy deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonParser parser = ctxt.getParser();
            ObjectCodec codec = parser.getCodec();
            final JsonNode treeNode = codec.readTree(parser);

            String code = treeNode.get("code").asText();
            Hierarchy hierarchy = new Hierarchy(code,
                    (short) (treeNode.get("levels").asInt() - 1),
                    (short) (treeNode.get("sectionSize").asInt() - 1));
            new EsHierarchyStorage().createHierarchy(hierarchy);
            for (JsonNode rootItem : treeNode.get("rootItems")) {
                HierarchyItem hierarchyItem = hierarchy.createRootItem(rootItem.get("code").asText());
                readChildren(rootItem, hierarchyItem, hierarchy);
            }
            return hierarchy;

        }

        private void readChildren(JsonNode rootItem, HierarchyItem parenItem, Hierarchy hierarchy) {
            for (JsonNode item : rootItem.get("items")) {
                HierarchyItem hierarchyItem = hierarchy.createItem(item.get("code").asText(), parenItem.getCode());
                readChildren(item, hierarchyItem, hierarchy);
            }

        }

    }

    @SuppressWarnings("rawtypes")
    public static class ReferenceSchemaDeserializer extends JsonDeserializer<ReferenceSchema> {
        @SneakyThrows
        @Override
        public ReferenceSchema deserialize(JsonParser p, DeserializationContext ctxt) {

            ObjectCodec codec = ctxt.getParser().getCodec();
            final JsonNode treeNode = codec.readTree(p);

            JsonNode attributes = treeNode.get("attributes");
            return SchemaInitializer.getReferenceSchema(
                    getAsEnum(treeNode.get("entityType"), codec),
                    ofNullable(treeNode.get("entityTypeRelatesToEntity")).map(JsonNode::asBoolean).orElse(false),
                    getAsEnum(treeNode.get("groupType"),codec),
                    ofNullable(treeNode.get("groupTypeRelatesToEntity")).map(JsonNode::asBoolean).orElse(false),
                    ofNullable(treeNode.get(INDEXED)).map(JsonNode::asBoolean).orElse(false),
                    ofNullable(treeNode.get("faceted")).map(JsonNode::asBoolean).orElse(false),
                    ofNullable(attributes).map(i -> {
                        try {
                            return codec.<Map<String, AttributeSchema>>readValue(i.traverse(codec), new TypeReference<>() {});
                        } catch (IOException e) {
                            throw new EvitaGetException("Cannot convert enum: "+ i.asText());
                        }
                    }).orElse(Collections.emptyMap())

            );

        }

        private Serializable getAsEnum(JsonNode node, ObjectCodec codec) {
            return ofNullable(node)
                    .map(i -> {
                        try {
                            EsEnumWrapper esEnumWrapper = codec.readValue(i.traverse(codec), EsEnumWrapper.class);
                            return esEnumWrapper == null ? null : esEnumWrapper.getEnumValue();
                        } catch (IOException e) {
                            throw new EvitaGetException("Cannot convert enum: "+ node.asText(),e);
                        }
                    })
                    .orElse(null);
        }


    }


}
