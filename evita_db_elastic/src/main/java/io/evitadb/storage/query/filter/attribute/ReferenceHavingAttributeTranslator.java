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

package io.evitadb.storage.query.filter.attribute;

import io.evitadb.api.query.filter.ReferenceHavingAttribute;
import io.evitadb.storage.query.EsConstraint;
import io.evitadb.storage.query.EsQueryTranslator.FilterByVisitor;
import io.evitadb.storage.query.filter.FilterConstraintTranslator;
import io.evitadb.storage.query.filter.TypeTranslator;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

/**
 * @author Jiří Bönsch, 2021
 */

/**
 *  No extra information provided - see (selfexplanatory) method signatures.
 *  I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class ReferenceHavingAttributeTranslator implements FilterConstraintTranslator<ReferenceHavingAttribute> {

    public static final String REFERENCES_PREFIX = "references";
    public static final String REFERENCES_RE_PREFIX = REFERENCES_PREFIX + ".referencedEntityType";


    @Override
    public EsConstraint apply(ReferenceHavingAttribute constraint, FilterByVisitor filterByVisitor) {

        final List<EsConstraint> innerConstraints = filterByVisitor.getCurrentLevelConstraints();
        if (innerConstraints.isEmpty()) return null;

        BoolQueryBuilder boolQueryBuilder = TypeTranslator.enhanceWithType(
                innerConstraints.get(0).getQueryBuilder(),
                constraint.getEntityType(),
                REFERENCES_RE_PREFIX);
        return new EsConstraint(
                boolQuery()
                        .filter(
                                nestedQuery(REFERENCES_PREFIX,
                                        boolQueryBuilder,
                                        ScoreMode.Max
                                )
                        )
        );

    }
}

