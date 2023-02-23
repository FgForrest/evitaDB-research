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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.evitadb.api.data.*;
import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.dataType.*;
import io.evitadb.storage.configuration.accessor.PMPTTAccessor;
import io.evitadb.storage.model.EntityWrapper;
import io.evitadb.storage.model.EsEnumWrapper;
import io.evitadb.storage.utils.StringUtils;
import lombok.Builder;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.oss.pmptt.exception.PivotHierarchyNodeNotFound;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/***
 *  Serializes {@link Entity}
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@CommonsLog
public class EntityJsonSerializer extends JsonSerializer<EntityWrapper> {

    public static final String VERSION = "version";
    public static final String DROPPED = "dropped";
    public static final String LOCALE = "locale";
    public static final String NUMBER = "number";
    public static final String OTHER = "other";
    public static final String VALUE = "value";
    public static final String PRICE_ID = "priceId";
    public static final String MULTIPLE = "multiple";
    public static final String INDEXED = "indexed";
    public static final String PRICE_LIST = "priceList";
    public static final String CURRENCY = "currency";
    public static final String PRIMARY_KEY = "primaryKey";

    @Override
    public void serialize(EntityWrapper value, JsonGenerator jGen, SerializerProvider provider) throws IOException {
        jGen.writeStartObject();

        Entity entity = value.getEntity();
        String indexName = value.getIndexName();

        jGen.writeNumberField(VERSION, entity.getVersion());
        jGen.writeObjectField("type", new EsEnumWrapper<>(entity.getType()));
        jGen.writeObjectField("locales", entity.getLocales());
        jGen.writeBooleanField(DROPPED, entity.isDropped());
        if (entity.getPrimaryKey() != null)
            jGen.writeNumberField(PRIMARY_KEY, entity.getPrimaryKey());


        if (!entity.getReferences().isEmpty())
            writeReferences(jGen, entity.getReferences());

        if (!entity.getAttributeKeys().isEmpty())
            writeAttributes(jGen, entity.getAttributeValues(), null);
        if (!entity.getPrices().isEmpty())
            writePrice(jGen, entity.getPricesVersion(), entity.getPriceInnerRecordHandling(), entity.getPrices());


        jGen.writeObjectField("indexName", indexName);
        writeAssociatedData(jGen,
                entity.getAssociatedDataValues()
                        .stream()
                        .collect(toMap(AssociatedDataContract.AssociatedDataValue::getKey, j -> j)),
                indexName, entity.getType());
        if (entity.getHierarchicalPlacement() != null)
            writeHierarchicalPlacement(jGen, entity.getHierarchicalPlacement());
        writePaths(jGen, entity);
        jGen.writeEndObject();
    }

    @SneakyThrows
    private void writePaths(JsonGenerator jGen, Entity entity) {
        jGen.writeFieldName("paths");
        jGen.writeStartArray();
        Serializable entityType = entity.getType();
        List<Path> paths = getPathsForEntity(entity.getReferences(), entity.getHierarchicalPlacement(), entityType);
        paths.stream()
                .filter(i -> i.getLeftBound() != null)
                .forEach(i -> {
                    try {
                        jGen.writeStartObject();
                        jGen.writeObjectField("rightBound", i.getRightBound());
                        jGen.writeObjectField("leftBound", i.getLeftBound());
                        jGen.writeObjectField("type", new EsEnumWrapper<>(i.getType()));
                        jGen.writeObjectField("ui", i.getUi());
                        jGen.writeEndObject();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                });
        if (paths.isEmpty()) {
            jGen.writeStartObject();
            jGen.writeObjectField("rightBound", 0);
            jGen.writeObjectField("leftBound", 0);
            jGen.writeObjectField("type", new EsEnumWrapper<>(entityType));
            jGen.writeEndObject();
        }
        jGen.writeEndArray();
    }

    private List<Path> getPathsForEntity(Collection<ReferenceContract> references, HierarchicalPlacementContract hierarchicalPlacement, Serializable type) {
        List<Path> paths = new LinkedList<>();
        for (ReferenceContract referenceContract : references) {

            EntityReferenceContract referencedEntity = referenceContract.getReferencedEntity();
            Serializable referencedEntityType = referencedEntity.getType();
            Hierarchy hierarchy = PMPTTAccessor.getHierarchy(referencedEntityType);
            try {
                String ui = StringUtils.getUI(referencedEntityType, referencedEntity.getPrimaryKey());
                HierarchyItem item = hierarchy.getItem(ui);
                paths.add(new Path(item.getLeftBound(), item.getRightBound(), referencedEntityType,ui));
            } catch (PivotHierarchyNodeNotFound ignored) {
                // ignored
            }
        }
        if (hierarchicalPlacement != null) {
            Hierarchy hierarchy = PMPTTAccessor.getHierarchy(type);
            Integer parentPrimaryKey = hierarchicalPlacement.getParentPrimaryKey();
            if (parentPrimaryKey != null) {
                String ui = StringUtils.getUI(type, parentPrimaryKey);
                HierarchyItem item = hierarchy.getItem(ui);
                paths.add(new Path(item.getLeftBound(), item.getRightBound(), type, ui));
            }
        }
        return paths;
    }

    @AllArgsConstructor
    @Data
    private static class Path {
        private Long leftBound;
        private Long rightBound;
        private Serializable type;
        private String ui;
    }

    private void writeAssociatedData(JsonGenerator jGen, Map<AssociatedDataContract.AssociatedDataKey, AssociatedDataContract.AssociatedDataValue> data, String indexName, Serializable type) throws IOException {

        jGen.writeFieldName("associatedData");
        jGen.writeStartObject();
        jGen.writeObjectField("type", new EsEnumWrapper<>(type));
        jGen.writeObjectField("indexName", indexName);
        jGen.writeObjectField("reflectionLookup", null);

        jGen.writeFieldName("associatedDataValues");
        jGen.writeStartArray();
        for (AssociatedDataContract.AssociatedDataValue value : data.values()) {
            jGen.writeObject(value);

        }
        jGen.writeEndArray();
        jGen.writeEndObject();
    }

    private void writePrice(JsonGenerator jGen, int priceVersion, PriceInnerRecordHandling innerRecordHandling, Collection<PriceContract> prices) throws IOException {

        jGen.writeObjectField("priceVersion", priceVersion);
        jGen.writeObjectField("priceInnerRecordHandling", innerRecordHandling);

        jGen.writeFieldName("prices");
        jGen.writeStartArray();
        for (PriceContract price : prices) {
            if ((price.isSellable() && !price.isDropped())) {

                writePrice(jGen,price, innerRecordHandling);
            }
        }

        jGen.writeEndArray();

        jGen.writeFieldName("notSellablePrices");
        jGen.writeStartArray();
        for (PriceContract price : prices) {
            if (!price.isSellable() || price.isDropped()) {
                writePrice(jGen,price, innerRecordHandling);
            }
        }

        jGen.writeEndArray();
    }

    private void writePrice(JsonGenerator jGen, PriceContract price, PriceInnerRecordHandling innerRecordHandling) throws IOException {

        jGen.writeStartObject();

        if (price.getInnerRecordId() != null)
            jGen.writeNumberField("innerRecordId", price.getInnerRecordId());
        Price.PriceKey priceKey = price.getPriceKey();
        jGen.writeObjectField(PRICE_LIST, priceKey.getPriceList());
        jGen.writeObjectField(CURRENCY, priceKey.getCurrency());
        jGen.writeNumberField("priceWithoutVat", price.getPriceWithoutVat());
        jGen.writeNumberField("priceWithVat", price.getPriceWithVat());

        jGen.writeBooleanField(INDEXED, price.isSellable());
        jGen.writeBooleanField(DROPPED, price.isDropped());
        jGen.writeNumberField(VERSION, price.getVersion());
        jGen.writeObjectField(PRICE_ID, priceKey.getPriceId());
        jGen.writeNumberField("vat", price.getVat());
        jGen.writeObjectField("priceInnerRecordHandling", innerRecordHandling.toString());

        DateTimeRange validity = price.getValidity();
        if (validity != null) {
            jGen.writeObjectField("hasValidity", true);
            jGen.writeObjectField("validity", new DescribableValue.Range(validity.getPreciseFrom(), validity.getPreciseTo()));
        } else {
            jGen.writeObjectField("hasValidity", false);
        }

        jGen.writeEndObject();
    }

    private void writeAttributes(JsonGenerator jGen, Collection<AttributesContract.AttributeValue> entity, Serializable referencedEntityType) throws IOException {

        List<String> types = entity.stream().map(i -> i.getKey().getAttributeName()).collect(Collectors.toList());
        if (!types.isEmpty()) {
            jGen.writeObjectField("types", types);
        }
        jGen.writeFieldName("attribute");
        jGen.writeStartArray();
        for (AttributesContract.AttributeValue value : entity) {
            if (!value.isDropped()){
                jGen.writeStartObject();

                AttributesContract.AttributeKey key = value.getKey();
                String attributeName = key.getAttributeName();

                jGen.writeObjectField("attributeName", attributeName);
                jGen.writeObjectField(LOCALE, key.getLocale());

                Serializable serializable = value.getValue();
                jGen.writeObjectField(VALUE, DescribableValue.storeValueIn(serializable, key.getAttributeName(), referencedEntityType));

                jGen.writeObjectField(DROPPED, value.isDropped());
                jGen.writeObjectField(VERSION, value.getVersion());
                if (serializable instanceof Number) {
                    jGen.writeObjectField("numberValue", serializable);
                }
                jGen.writeEndObject();
            }
        }

        jGen.writeEndArray();
    }


    private void writeReferences(JsonGenerator jGen, Collection<ReferenceContract> references) throws IOException {
        jGen.writeFieldName("references");
        jGen.writeStartArray();
        for (ReferenceContract referenceContract : references) {
            if (!referenceContract.isDropped()) {
                jGen.writeStartObject();
                if (referenceContract instanceof Reference) {
                    Reference reference = (Reference) referenceContract;

                    EntityReferenceContract referencedEntity = reference.getReferencedEntity();
                    Serializable referencedEntityType = referencedEntity.getType();

                    jGen.writeObjectField(VERSION, reference.getVersion());
                    jGen.writeObjectField(DROPPED, reference.isDropped());
                    jGen.writeObjectField("sortRefs", reference.getAttributeValues().stream().map(SortReference::new).collect(Collectors.toList()));
                    EsEnumWrapper type = new EsEnumWrapper(referencedEntityType);
                    String typeValue = type.getValue();
                    jGen.writeObjectField("simpleType", typeValue);
                    int pk = referencedEntity.getPrimaryKey();
                    jGen.writeObjectField("referencedEntity", typeValue + "_" + pk);

                    jGen.writeObjectField("referencedEntityType", type);
                    jGen.writeObjectField("referencedEntityPrimaryKey", pk);
                    if (referencedEntity instanceof ReferenceContract.GroupEntityReference) {
                        ReferenceContract.GroupEntityReference groupEntityReference = (ReferenceContract.GroupEntityReference) referencedEntity;
                        jGen.writeObjectField("referencedEntityVersion", groupEntityReference.getVersion());
                        jGen.writeObjectField("referencedEntityDropped", groupEntityReference.isDropped());
                    }

                    ReferenceContract.GroupEntityReference group = reference.getGroup();
                    if (group != null) {
                        jGen.writeObjectField("groupType", new EsEnumWrapper<>(group.getType()));
                        jGen.writeObjectField("groupPrimaryKey", group.getPrimaryKey());
                        jGen.writeObjectField("groupVersion", group.getVersion());
                        jGen.writeObjectField("groupDropped", group.isDropped());
                    }


                    Attributes attributes = reference.getAttributes();
                    Collection<AttributesContract.AttributeValue> attributeValues = attributes.getAttributeValues();
                    writeAttributes(jGen, attributeValues,  referencedEntityType);

                } else {
                    log.error("Unknown class of reference: " + referenceContract.getClass().getSimpleName());
                }
                jGen.writeEndObject();
            }
        }
        jGen.writeEndArray();
    }

    /**
     * This could be changed behalf of the model.
     */
    private void writeHierarchicalPlacement(JsonGenerator jGen, HierarchicalPlacementContract hierarchicalPlacement) throws IOException {
        jGen.writeFieldName("hierarchicalPlacement");
        jGen.writeStartObject();

        jGen.writeNumberField(VERSION, hierarchicalPlacement.getVersion());
        // should be indexed differently
        if (hierarchicalPlacement.getParentPrimaryKey() != null) {
            jGen.writeNumberField("parentPrimaryKey", hierarchicalPlacement.getParentPrimaryKey());
        }
        jGen.writeNumberField("orderAmongSiblings", hierarchicalPlacement.getOrderAmongSiblings());
        jGen.writeBooleanField(DROPPED, hierarchicalPlacement.isDropped());

        jGen.writeEndObject();
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class DescribableValue implements Serializable {

        @JsonCreator
        public DescribableValue() {
            // do not assign anything here
        }

        private Serializable singleValue;
        private Serializable key;
        private Serializable valueWithType;
        private Serializable date;
        private ZoneId zoneId;
        private Boolean booleanValue;
        private Byte byteValue;
        private Serializable number;
        private Serializable other;
        private Range integerRange;
        private Range floatRange;
        private Range longRange;
        private Range doubleRange;
        private Range numberRange;
        private Range dateRange;
        private Locale locale;
        private Serializable[] values;
        private JsonMultiple multiple;

        protected static DescribableValue storeValueIn(@Nullable Serializable value, String key, Serializable pureReferencedEntityType) {
            String valueWithType = pureReferencedEntityType + "_" + key;
            if (value == null || value instanceof ComplexDataObject.NullValue) {
                return DescribableValue.builder().build();
            } else if (value instanceof String || value instanceof Character) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).singleValue(value).build();
            } else if (value instanceof Byte) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).byteValue((Byte) value).build();
            } else if (value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigDecimal) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).number(value).build();
            } else if (value instanceof Boolean) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).booleanValue((Boolean) value).build();
            } else if (value instanceof DateTimeRange) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).dateRange(new Range(((DateTimeRange) value).getPreciseFrom(), ((DateTimeRange) value).getPreciseTo())).build();
            } else if (value instanceof NumberRange) {

                return getNumberRangeValue(value, key, valueWithType);
            } else if (value instanceof Multiple) {
                return DescribableValue
                        .builder().key(key).valueWithType(valueWithType)
                        .multiple(new JsonMultiple((Multiple) value))
                        .build();
            } else if (value.getClass().isArray()) {

                return DescribableValue
                        .builder().key(key).valueWithType(valueWithType)
                        .values(
                                Arrays.stream((Object[]) value)
                                        .map(Serializable.class::cast)
                                        .map(value1 -> storeValueIn(value1, key, pureReferencedEntityType))
                                        .toArray(DescribableValue[]::new))
                        .build();
            } else if (value instanceof ZonedDateTime) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).date(((ZonedDateTime) value).toEpochSecond()).zoneId(((ZonedDateTime) value).getZone()).build();
            } else if (value instanceof LocalDateTime) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).date(((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC)).build();
            } else if (value instanceof LocalDate) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).date(((LocalDate) value).toEpochDay()).build();
            } else if (value instanceof LocalTime) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).date(((LocalTime) value).toSecondOfDay()).build();
            } else if (value instanceof Locale) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).locale((Locale) value).build();
            } else if (value instanceof Enum || value instanceof EnumWrapper || value instanceof Currency) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).other(value).build();
            }

            throw new IllegalArgumentException("Class \"" + value.getClass().getName() + "\" is currently not supported by this Evita implementation.");
        }

        private static DescribableValue getNumberRangeValue(Serializable value, String key, String valueWithType) {

            NumberRange numberRange = (NumberRange) value;
            Number preciseFrom = numberRange.getPreciseFrom();
            Number preciseTo = numberRange.getPreciseTo();

            if (preciseFrom instanceof Integer) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).integerRange(new Range(preciseFrom, preciseTo)).build();
            } else if (preciseFrom instanceof Float) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).floatRange(new Range(preciseFrom, preciseTo)).build();
            } else if (preciseFrom instanceof Long) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).longRange(new Range(preciseFrom, preciseTo)).build();
            } else if (preciseFrom instanceof Double) {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).doubleRange(new Range(preciseFrom, preciseTo)).build();
            } else {
                return DescribableValue.builder().key(key).valueWithType(valueWithType).numberRange(new Range(preciseFrom, preciseTo)).build();
            }
        }

        @SuppressWarnings("unchecked")
        public static <T extends Comparable<? super T> & Serializable> Serializable getValueFrom(JsonNode value, Class<?> type, ObjectCodec codec) throws IOException {

            if (value == null) {
                return ComplexDataObject.NullValue.INSTANCE;

            } else if (type.isAssignableFrom(String.class)) {
                return value.get("singleValue").asText();

            } else if (type.isAssignableFrom(Character.class)) {
                return readValue(Character.class, codec, "singleValue", value);

            } else if (type.isAssignableFrom(Byte.class)) {
                return readValue(Byte.class, codec, "byteValue", value);

            } else if (type.isAssignableFrom(Short.class)) {
                return readValue(Short.class, codec, NUMBER, value);

            } else if (type.isAssignableFrom(Integer.class)) {
                return readValue(Integer.class, codec, NUMBER, value);

            } else if (type.isAssignableFrom(Float.class)) {
                return readValue(Float.class, codec, NUMBER, value);

            } else if (type.isAssignableFrom(Long.class)) {
                return readValue(Long.class, codec, NUMBER, value);

            } else if (type.isAssignableFrom(Boolean.class)) {
                return value.get("booleanValue").asBoolean();

            } else if (type.isAssignableFrom(BigDecimal.class)) {
                return readValue(BigDecimal.class, codec, NUMBER, value);

            } else if (type.isAssignableFrom(DateTimeRange.class)) {
                JsonNode dateRange = value.get("dateRange");
                long dateFrom = dateRange.get("gte").asLong();
                ZoneId dateFromZone = codec.readValue(dateRange.get("dateFromZone").traverse(codec), ZoneId.class);
                long dateTo = dateRange.get("lte").asLong();
                ZoneId dateToZone = codec.readValue(dateRange.get("dateToZone").traverse(codec), ZoneId.class);
                return DateTimeRange.between(
                        ZonedDateTime.ofInstant(Instant.ofEpochSecond(dateFrom), dateFromZone),
                        ZonedDateTime.ofInstant(Instant.ofEpochSecond(dateTo), dateToZone));
            } else if (type.isAssignableFrom(NumberRange.class)) {
                JsonNode integerRange = value.get("integerRange");
                if (integerRange != null) {
                    return NumberRange.between(integerRange.get("gte").asInt(), integerRange.get("lte").asInt());
                }
                JsonNode floatRange = value.get("floatRange");
                if (floatRange != null) {
                    return NumberRange.between(readValue(Float.class, codec, "gte", floatRange), readValue(Float.class, codec, "lte", floatRange));
                }
                JsonNode longRange = value.get("longRange");
                if (longRange != null) {
                    return NumberRange.between(longRange.get("gte").asLong(), longRange.get("lte").asLong());
                }
                JsonNode doubleRange = value.get("doubleRange");
                if (doubleRange != null) {
                    return NumberRange.between(doubleRange.get("gte").asInt(), doubleRange.get("lte").asInt());
                }
                JsonNode numberRange = value.get("numberRange");
                if (numberRange != null) {
                    return NumberRange.between(readValue(Number.class, codec, "gte", numberRange), readValue(Number.class, codec, "gte", numberRange));
                }
            } else if (type.isAssignableFrom(Multiple.class)) {
                JsonNode jsonMultiple = value.get(MULTIPLE);
                Class<?> readValue = readValue(Class.class, codec, "valueClass", jsonMultiple);
                Serializable[] values = (Serializable[]) codec.readValue(jsonMultiple.get(MULTIPLE).get("values").traverse(codec), readValue);
                if (values.length == 2) {
                    return new Multiple((T) values[0], (T) values[1]);
                } else if (values.length == 3) {
                    return new Multiple((T) values[0], (T) values[1], (T) values[2]);
                } else if (values.length == 4) {
                    return new Multiple((T) values[0], (T) values[1], (T) values[2], (T) values[3]);
                }
            } else if (type.isArray()) {
                JsonNode values = value.get("values");
                List<T> typedValues = new ArrayList<>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    typedValues.add((T) getValueFrom(values.get(i), type.getComponentType(), codec));
                }
                return typedValues.toArray(i -> (T[]) Array.newInstance(type.getComponentType(), values.size()));
            } else if (type.isAssignableFrom(ZonedDateTime.class)) {
                return ZonedDateTime.ofInstant(Instant.ofEpochSecond(value.get("date").asLong()), Objects.requireNonNull(readValue(ZoneId.class, codec, "zoneId", value)));
            } else if (type.isAssignableFrom(LocalDateTime.class)) {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(value.get("date").asLong()), TimeZone.getDefault().toZoneId());
            } else if (type.isAssignableFrom(LocalDate.class)) {
                return LocalDate.ofEpochDay(value.get("date").asLong());
            } else if (type.isAssignableFrom(LocalTime.class)) {
                return LocalTime.ofSecondOfDay(value.get("date").asLong());
            } else if (type.isAssignableFrom(Locale.class)) {
                return readValue(Locale.class, codec, LOCALE, value);
            } else if (Enum.class.isAssignableFrom(type) || EnumWrapper.class.isAssignableFrom(type)) {
                return Objects.requireNonNull(readValue(EsEnumWrapper.class, codec, OTHER, value)).getEnumValue();
            } else if (Currency.class.isAssignableFrom(type)) {
                return readValue(Currency.class, codec, OTHER, value);
            }
            throw new IllegalArgumentException(type + " : " + value);
        }

        private static <T> T readValue(Class<T> type, ObjectCodec codec, String nodeName, JsonNode value) throws IOException {
            JsonNode jsonNode = value.get(nodeName);
            if (jsonNode == null) return null;
            return codec.readValue(jsonNode.traverse(codec), type);

        }

        public static String getRowByType(Class<?> type) {
            if (type.isAssignableFrom(String.class)) {
                return "singleValue.keyword";

            } else if (type.isAssignableFrom(Character.class)) {
                return "singleValue.keyword";

            } else if (type.isAssignableFrom(Byte.class)) {
                return "byteValue";

            } else if (type.isAssignableFrom(Short.class)) {
                return NUMBER;

            } else if (type.isAssignableFrom(Integer.class)) {
                return NUMBER;

            } else if (type.isAssignableFrom(Long.class)) {
                return NUMBER;

            } else if (type.isAssignableFrom(Boolean.class)) {
                return "booleanValue";

            } else if (type.isAssignableFrom(BigDecimal.class)) {
                return NUMBER;

            } else if (type.isAssignableFrom(DateTimeRange.class)) {
                return "dateRange";
            } else if (type.isAssignableFrom(NumberRange.class)) {
                return "integerRange";
            } else if (type.isAssignableFrom(Multiple.class)) {
                return MULTIPLE;
            } else if (type.isArray()) {
                return "values." + getRowByType(type.getComponentType());
            } else if (type.isAssignableFrom(ZonedDateTime.class)) {
                return "date";
            } else if (type.isAssignableFrom(LocalDateTime.class)) {
                return "date";
            } else if (type.isAssignableFrom(LocalTime.class)) {
                return "date";
            } else if (type.isAssignableFrom(LocalDate.class)) {
                return "date";
            } else if (type.isAssignableFrom(Locale.class)) {
                return LOCALE;
            } else if (type.isAssignableFrom(Enum.class)) {
                return OTHER;
            } else if (type.isAssignableFrom(EnumWrapper.class)) {
                return OTHER;
            } else if (type.isAssignableFrom(Currency.class)) {
                return OTHER;
            }
            return OTHER;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Range implements Serializable {

            public Range(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
                this.gte = dateFrom.toEpochSecond();
                this.dateFromZone = dateFrom.getZone();
                this.lte = dateTo.toEpochSecond();
                this.dateToZone = dateTo.getZone();
            }

            public Range(Number gte, Number lte) {
                this.gte = gte;
                this.lte = lte;
            }

            private Number gte;
            private Number lte;
            private ZoneId dateFromZone;
            private ZoneId dateToZone;

        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class JsonMultiple implements Serializable {

            public JsonMultiple(Multiple multiple) {
                this.multiple = multiple;
                Serializable[] values = multiple.getValues();
                valueClass = multiple.getValues().getClass();
                if (values.length >= 2) {
                    first = convertValue(values[0]);
                    second = convertValue(values[1]);
                }
                if (values.length >= 3) {
                    third = convertValue(values[2]);
                }
                if (values.length >= 4) {
                    fourth = convertValue(values[3]);
                }
            }

            private Multiple multiple;
            private Class<?> valueClass;
            private Serializable first;
            private Serializable second;
            private Serializable third;
            private Serializable fourth;

            private Serializable convertValue(Serializable object) {
                return object instanceof Number ? StringUtils.leftPad(String.valueOf(object), 8, '0') : object;
            }
        }


    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortReference {
        // type + attrName
        String combinedField;
        Serializable valueToSort;

        public SortReference(AttributesContract.AttributeValue value) {
            this.combinedField = value.getKey().getAttributeName();
            this.valueToSort = StringUtils.leftPad(String.valueOf(convert(value.getValue())), 15, '0');
        }

        private Serializable convert(Serializable value) {
            if (value instanceof ZonedDateTime) {
                value = ((ZonedDateTime) value).toEpochSecond();
            } else if (value instanceof LocalDateTime) {
                value = ((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC);
            } else if (value instanceof LocalDate) {
                value = ((LocalDate) value).toEpochDay();
            } else if (value instanceof LocalTime) {
                value = ((LocalTime) value).toSecondOfDay();
            } else if (value instanceof Enum) {
                value = new EsEnumWrapper<>(value).getValue();
            }
            return value;

        }
    }


}
