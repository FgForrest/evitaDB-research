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

package io.evitadb.api;

import io.evitadb.api.data.structure.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/***
 *  Holds cached entities that by themselves flushes into DB by {@link #storeFunction}.
 *  Number of cached entities could be changed by {@link #DEFAULT_CACHE_SIZE_IN_BYTES}
 *  and after the warming up state,
 *  {@link #flush(boolean)} should be called to flush remaining entites into DB.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class StoringParameters {
    private static final int DEFAULT_CACHE_SIZE_IN_BYTES = 50 * 1000 * 1000 ;

    /**
     * Storing function, which takes entities from cache and stores them into DB.
     */
    private final BiConsumer<List<StoredEntity>,Boolean> storeFunction;
    /**
     * Serializing function, which serializes entity pojo into json.
     */
    private final Function<Entity,String> serializingFunction;
    /**
     * Simple caching collection.
     */
    @Getter
    private List<StoredEntity> entityCache;
    @Getter
    private Map<String,AttributeWithReference> entityCacheAttrs;

    public StoringParameters(BiConsumer<List<StoredEntity>,Boolean> storeFunction, Function<Entity,String> serializingFunction) {
        this.storeFunction = storeFunction;
        this.serializingFunction = serializingFunction;
        entityCache = new CopyOnWriteArrayList<>();
        entityCacheAttrs = new ConcurrentHashMap<>();
    }

    /**
     * Adds entity into cache, if limit is reached also flushes into DB (could take some sec).
     *
     * @param entity to store
     */
    public void addEntity(Entity entity) {
        check();
        entityCache.add(new StoredEntity(entity,serializingFunction.apply(entity)));
        Map<String, Serializable> uniqueAttrsOfEntity = EsEntityCollection.getUniqueAttrsOfEntity(entity);
        if (uniqueAttrsOfEntity.isEmpty()){
            for (Map.Entry<String, Serializable> entry : uniqueAttrsOfEntity.entrySet()) {

                entityCacheAttrs.put(entry.getKey(),new AttributeWithReference(entity.getType(),entity.getPrimaryKey(),entry.getValue()));
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class AttributeWithReference{
        private Serializable type;
        private Integer pk;
        private Serializable attr;
    }
    /**
     * Flushes cache into DB.
     */
    public void flush(boolean async) {
        storeFunction.accept(entityCache,async);
        entityCache = new CopyOnWriteArrayList<>();
        entityCacheAttrs = new ConcurrentHashMap<>();
    }
    public int count(Serializable entityType) {
        return (int) entityCache.stream().filter(i-> Objects.equals(i.getEntity().getType(), entityType)).count();
    }

    private void check() {
        if (entityCache.stream().mapToInt(i->i.getJson().length()).sum() >= DEFAULT_CACHE_SIZE_IN_BYTES) {
            flush(true);
        }
    }

    @Data
    @AllArgsConstructor
    public static class StoredEntity{
        private Entity entity;
        private String json;
    }

    public void checkByConsumer(Consumer<Map.Entry<String,AttributeWithReference>> predicate){
        entityCacheAttrs
                .entrySet()
                .forEach(predicate::accept);
    }


}
