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

package io.evitadb.storage.query.filter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
@NoArgsConstructor(access = AccessLevel.NONE)
public class TypeTranslator {

    public static BoolQueryBuilder enhanceWithType(BoolQueryBuilder esQuery,Object entityType){
        return enhanceWithType(esQuery,entityType,"type");
    }
    public static BoolQueryBuilder enhanceWithType(BoolQueryBuilder esQuery,Object entityType,String prefix){

        BoolQueryBuilder typeQuery = boolQuery();
        if (entityType instanceof Enum) {
            Enum<?> enumType = ((Enum<?>) entityType);

            typeQuery = typeQuery
                    .must(
                            matchQuery(prefix+".value.keyword", enumType.name())
                    )
                    .must(
                            matchQuery(prefix+".valueClass", enumType.getClass().getName())
                    );
        } else {

            typeQuery = typeQuery
                    .must(
                            matchQuery(prefix+".value", entityType.toString())
                    );
        }

        return esQuery
                .filter(
                        nestedQuery(prefix, typeQuery, ScoreMode.Max));
    }
}
