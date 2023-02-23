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

package io.evitadb.storage.serialization.mixins;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataValue;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.*;
import io.evitadb.api.data.structure.AssociatedData;
import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.trie.TrieNode;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.storage.serialization.deserializers.EvitaDeserializers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

/***
 *  Mixins for selecting creator constructors of entities.
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@SuppressWarnings("*")
public interface EvitaMixins {

    /**
     * @see PriceKey
     */
    public abstract static class PriceKeyMixin {
        PriceKeyMixin(@JsonProperty("priceId") Serializable priceId, @JsonProperty("priceList") Serializable priceList, @JsonProperty("currency") Currency currency) {

        }
    }

    /**
     * @see EntitySchema
     */
    public abstract static class EntitySchemaMixin {
        EntitySchemaMixin(@JsonProperty("name") Serializable name) {
        }
    }

    /**
     * @see AttributeSchema
     */
    public abstract static class AttributeSchemaMixin {
        AttributeSchemaMixin(@JsonProperty("name") String name, @JsonProperty("type") Class<? extends Serializable> type, @JsonProperty("localized") boolean localized) {
        }
    }

    /**
     * @see AssociatedDataSchema
     */
    public abstract static class AssociatedDataSchemaMixin {
        AssociatedDataSchemaMixin(@JsonProperty("name") String name, @JsonProperty("type") Class<? extends Serializable> type, @JsonProperty("localized") boolean localized) {
        }
    }

    /**
     * @see Attributes
     */
    public abstract static class AttributesMixin {
        AttributesMixin(
                @JsonProperty("entitySchema") EntitySchema entitySchema,
                @JsonProperty("attribute") Collection<AttributeValue> attributeValues,
                @JsonProperty("attributeTypes") Map<String, AttributeSchema> attributeTypes
        ) {
        }

        @JsonIgnore
        EntitySchema entitySchema;

    }

    /**
     * @see AttributeValue
     */
    public abstract static class AttributeValueMixin {
        AttributeValueMixin(@JsonProperty("attributeKey") AttributeKey attributeKey, @JsonProperty("value") Serializable value) {

        }
    }

    /**
     * @see AssociatedDataValue
     */
    public abstract static class AssociatedDataValueMixin {

        AssociatedDataValueMixin(@JsonProperty("version") int version, @JsonProperty("key") AssociatedDataKey key, @JsonProperty("value") Serializable value, @JsonProperty("dropped") boolean dropped) {
        }
    }


    /**
     * @see AssociatedDataKey
     */
    public abstract static class AssociatedDataKeyMixin {
        AssociatedDataKeyMixin(@JsonProperty("associatedDataName") String associatedDataName, @JsonProperty("locale") Locale locale) {
        }
    }


    /**
     * @see AttributeKey
     */
    public abstract static class AttributeKeyMixin {
        AttributeKeyMixin(@Nonnull @JsonProperty("attributeName") String attributeName, @Nonnull @JsonProperty("locale") Locale locale) {
        }

        @JsonIgnore
        public abstract boolean isLocalized();

    }


    /**
     * @see AssociatedData
     */
    public abstract static class AssociatedDataMixin {
        AssociatedDataMixin(
                @JsonProperty("entitySchema") EntitySchema entitySchema,
                @JsonProperty("reflectionLookup") ReflectionLookup reflectionLookup,
                @JsonProperty("associatedDataValues") Collection<AssociatedDataValue> associatedDataValues
        ) {
        }

        @JsonIgnore
        EntitySchema entitySchema;

        @JsonIgnore
        transient ReflectionLookup reflectionLookup;
    }


    /**
     * @see Prices
     */
    public abstract static class PricesMixin {
        PricesMixin(@JsonProperty("version") int version, @JsonProperty("prices") Collection<PriceContract> prices, @JsonProperty("priceInnerRecordHandling") PriceInnerRecordHandling priceInnerRecordHandling) {
        }
    }

    /**
     * @see Price
     */
    public abstract static class PriceMixin {
        PriceMixin(@JsonProperty("version") int version, @JsonProperty("priceKey") @Nonnull PriceKey priceKey, @JsonProperty("innerRecordId") @Nullable Integer innerRecordId, @JsonProperty("priceWithoutVat") @Nonnull BigDecimal priceWithoutVat, @JsonProperty("vat") @Nonnull BigDecimal vat, @JsonProperty("priceWithVat") @Nonnull BigDecimal priceWithVat, @JsonProperty("validity") @Nullable DateTimeRange validity, @JsonProperty("priority") long priority, @JsonProperty("indexed") boolean indexed, @JsonProperty("dropped") boolean dropped) {
        }
    }

    /**
     * @see ReferenceSchema
     */
    public abstract static class ReferenceSchemaMixin {
        ReferenceSchemaMixin(@JsonProperty("entityType") Serializable entityType, @JsonProperty("entityTypeRelatesToEntity") boolean entityTypeRelatesToEntity, @JsonProperty("groupType") Serializable groupType, @JsonProperty("groupTypeRelatesToEntity") boolean groupTypeRelatesToEntity, @JsonProperty("faceted") boolean faceted, @JsonProperty("hierarchyIndex") boolean hierarchyIndex) {
        }
    }

    /**
     * @see ReferenceMixin
     */
    public abstract static class ReferenceMixin {

        protected ReferenceMixin(@JsonProperty("entitySchema") EntitySchema entitySchema, @JsonProperty("version") int version, @JsonProperty("referencedEntity") EntityReferenceContract referencedEntity, @JsonProperty("group") ReferenceContract.GroupEntityReference group, @JsonProperty("dropped") boolean dropped) {
        }

        @JsonIgnore
        EntitySchema entitySchema;

    }

    /**
     * @see HierarchicalPlacementContract
     */
    public abstract static class HierarchicalPlacementContractMixin {

        HierarchicalPlacementContractMixin(@JsonProperty("version") int version, @JsonProperty("parentPrimaryKey") int parentPrimaryKey, @JsonProperty("orderAmongSiblings") int orderAmongSiblings, @JsonProperty("dropped") boolean dropped) {
        }
    }

    /**
     * @see HierarchicalPlacementContract
     */
    public abstract static class GroupEntityReferenceMixin {

        GroupEntityReferenceMixin(@JsonProperty("version") int version, @JsonProperty("referencedEntity") Serializable referencedEntity, @JsonProperty("primaryKey") int primaryKey, @JsonProperty("dropped") boolean dropped) {

        }
    }

    /**
     * @see HierarchicalPlacementContract
     */
    @JsonDeserialize(using = EvitaDeserializers.EntityReferenceContractDeserializer.class)
    public abstract static class EntityReferenceContractMixin {

    }

    public abstract static class EntityReferenceMixin {
        protected EntityReferenceMixin(@JsonProperty("type") Serializable type, @JsonProperty("primaryKey") int primaryKey) {
        }
    }


    public abstract static class TrieMixin {

        protected TrieMixin(@JsonProperty("type") Class<?> type) {
        }

        @JsonIgnore
        private IntFunction<?> arrayFactory;
    }

    public abstract static class TrieNodeMixin<T extends Serializable> {
        protected TrieNodeMixin(@JsonProperty("charIndex") int[] charIndex, @JsonProperty("children") TrieNode<T>[] children, @JsonProperty("edgeLabel") String[] edgeLabel, @JsonProperty("values") T[] values) {
        }
    }

    public abstract static class DateTimeRangeMixin {
        protected DateTimeRangeMixin(@JsonProperty("from") ZonedDateTime from, @JsonProperty("to") ZonedDateTime to) {
        }
    }

}
