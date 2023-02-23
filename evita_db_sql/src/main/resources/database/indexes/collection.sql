------------------------
-- structural indexes --
------------------------

-- makes sure that there are not multiple sortable attributes with same name (global and localized) across referenced entities with same type within single entity
create unique index ix_attributeIndex_uniqueSortableReferenceAttributes
    on ${collectionUid}.t_attributeIndex (entity_id, reference_entityType, name)
    where sortable = true;

-- makes sure that values of multiple unique attributes with same name (global and localized) across single entity collection are unique
-- because multiple "null" values are not considered equal, there have to be unique index for each value type
create unique index ix_attributeIndex_uniqueAttributeStringValues
    on ${collectionUid}.t_attributeIndex (name, stringValues)
    where uniq = true and stringValues is not null;
create unique index ix_attributeIndex_uniqueAttributeIntValues
    on ${collectionUid}.t_attributeIndex (name, intValues)
    where uniq = true and intValues is not null;
create unique index ix_attributeIndex_uniqueAttributeIntRangeValues
    on ${collectionUid}.t_attributeIndex (name, intRangeValues)
    where uniq = true and intRangeValues is not null;

--------------------------
-- optimization indexes --
--------------------------

-- references
create index ix_referenceIndex_referencedEntity
    on ${collectionUid}.t_referenceIndex (entityType, entityPrimaryKey)
    include (entity_id);
create index ix_referenceIndex_facet
    on ${collectionUid}.t_referenceIndex (faceted)
    where faceted = true;
create index ix_referenceIndex_facetToEntity
    on ${collectionUid}.t_referenceIndex (entity_id, faceted)
    include (entityType, entityPrimaryKey) -- for temp table creation
    where faceted = true;

-- attributes
create index ix_attributeIndex_entityAttributeByKey
    on ${collectionUid}.t_attributeIndex (entity_id, name, locale)
    include (stringValues, intValues, intRangeValues)
    where reference_id is null;
create index ix_attributeIndex_referenceAttributeByKey
    on ${collectionUid}.t_attributeIndex (reference_id, name, locale)
    include (stringValues, intValues, intRangeValues)
    where reference_id is not null;
-- entity attributes
create index ix_attributeIndex_entityAttributeByValue
    on ${collectionUid}.t_attributeIndex (name, locale, stringValues, intValues, intRangeValues)
    include (entity_id)
    where reference_id is null;
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

-- reference attributes
create index ix_attributeIndex_referenceAttributeByValue
    on ${collectionUid}.t_attributeIndex (reference_id, name, locale, stringValues, intValues, intRangeValues)
    include (entity_id)
    where reference_id is not null;

-- prices
create index ix_priceIndex_query
    on ${collectionUid}.t_priceIndex using gin (priceList)
