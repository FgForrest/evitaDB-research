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

--------------------------
-- collection structure --
--------------------------

create table ${collectionUid}.t_referenceIndex
(
    reference_id                        bigint not null,
    entity_id                           bigint not null,
    version                             int not null,
    faceted                             boolean not null,

    -- referenced entity metadata
    entityPrimaryKey                    int not null,
    entityType                          varchar(128) not null,
    entityTypeDataType                  varchar(128) not null,

    -- group metadata
    groupPrimaryKey                     int,
    groupType                           varchar(128),
    groupTypeDataType                   varchar(128),

    constraint cnpk_referenceIndex primary key (reference_id),
    constraint cnfk_referenceIndex_entity foreign key (entity_id) references t_entity (entity_id) on delete cascade on update cascade
);

create sequence ${collectionUid}.t_reference_id_seq;

create materialized view ${collectionUid}.t_facetIndex as
(
    select facetsByEntityType.entity_id,
        jsonb_object_agg(
            facetsByEntityType.referencedEntityType,
            facetsByEntityType.facetsByEntityType
        ) as facets
    from (
        select entity.primaryKey                  as primaryKey,
            reference.entityType                  as referencedEntityType,
            entity.entity_id                      as entity_id,
            array_agg(reference.entityPrimaryKey) as facetsByEntityType
        from t_entity entity
        inner join ${collectionUid}.t_referenceIndex reference
            on entity.entity_id = reference.entity_id
                and reference.faceted = true
        where entity.type = '${collectionType}'
        group by entity.primaryKey, reference.entityType, entity.entity_id
        order by entity.primaryKey, reference.entityType, entity.entity_id
    ) as facetsByEntityType
    group by facetsByEntityType.primaryKey, facetsByEntityType.entity_id
    order by facetsByEntityType.entity_id
);

create table ${collectionUid}.t_attributeIndex
(
    attribute_id                        bigserial not null,
    entity_id                           bigint not null,
    reference_id                        bigint,
    reference_entityType                varchar(128),

    name                                varchar(128) not null,
    locale                              char(5),
    version                             int not null,
    sortable                            boolean default false,
    uniq                                boolean default false,

    -- actual values
    stringValues                        varchar(1024)[],
    intValues                           bigint[],
    intRangeValues                      int8range[],

    constraint cnpk_attributeIndex primary key (attribute_id),
    constraint cnfk_attributeIndex_entity foreign key (entity_id) references t_entity (entity_id) on delete cascade on update cascade,
    constraint cnfk_attributeIndex_reference foreign key (reference_id) references ${collectionUid}.t_referenceIndex (reference_id) on delete cascade on update cascade
);

create table ${collectionUid}.t_priceIndex
(
    price_id                            bigserial not null,
    entity_id                           bigint not null,
    primaryKey                          int not null,
    version                             int not null,
    currency                            char(3) not null,
    priceList                           varchar(64) not null,
    priceListDataType                   varchar(128) not null,
    validity                            int8range,
    innerRecordHandling                 varchar(64),
    innerRecordId                       int,
    priceWithoutVAT                     numeric(9, 2) not null,
    priceWithVAT                        numeric(9, 2) not null,

    constraint cnpk_priceIndex primary key (price_id),
    constraint cnfk_priceIndex_entity foreign key (entity_id) references t_entity (entity_id) on delete cascade on update cascade
);
