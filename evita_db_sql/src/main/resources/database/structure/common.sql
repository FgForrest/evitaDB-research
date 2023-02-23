create extension if not exists btree_gin;

create table t_collection
(
    name                                varchar(128) not null unique,
    nameType                            varchar(128) not null,
    uid                                 varchar(16) not null,
    serializationHeader                 bytea,

    constraint cnpk_collection primary key (name)
);

create sequence t_collection_uid_seq;

create table t_schema
(
    entityType                          varchar(128) not null unique,
    detail                              bytea not null,
    serializationHeader                 bytea not null,
    withHierarchy                       boolean not null default false,
    hierarchyLevels                     smallint,
    hierarchySectionSize                smallint,

    constraint cnfk_schema_collection foreign key (entityType) references t_collection (name) on delete cascade on update cascade
);

create table t_catalogAttribute (
    key                               varchar(16) not null,
    value                             varchar(16),

    constraint cnpk_catalogAttribute primary key (key)
);

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

-- initial state
insert into t_catalogAttribute (key, value) values ('state', 'WARMING_UP');

create table t_entity
(
    entity_id                           bigint not null,
    primaryKey                          int not null,
    type                                varchar(128) not null,
    version                             int not null,
    dropped                             boolean default false,
    locales                             char(5)[],

    -- hierarchy metadata
    parentPrimaryKey                    int,
    -- other hierarchy metadata, only in case of hierarchy entity
    leftBound                           bigint,
    rightBound                          bigint,
    level                               bigint,
    numberOfChildren                    smallint,
    orderAmongSiblings                  smallint,
    hierarchyBucket                     smallint,

    -- original entity data
    serializedEntity                    bytea not null,

    constraint cnpk_entity primary key (entity_id),
    constraint cnun_entity_typePrimaryKey unique (type, primaryKey),
    constraint cnfk_entity_collection foreign key (type) references t_collection (name) on delete cascade on update cascade
);

create sequence t_entity_id_seq;

create table t_entityHierarchyPlacement
(
    entity_id                           bigint not null,

    -- target hierarchy entity identification, if represents itself then there will be special keyword for both type and PK
    type                                varchar(128) not null,
    primaryKey                          varchar(32) not null,

    -- placement of target hierarchy entity
    leftBound                           bigint not null,
    rightBound                          bigint not null,
    level                               smallint not null,

    constraint cnpk_entityHierarchyPlacement primary key (entity_id, type, primaryKey)
);
