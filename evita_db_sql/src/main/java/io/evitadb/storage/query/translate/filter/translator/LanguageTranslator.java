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

package io.evitadb.storage.query.translate.filter.translator;

import io.evitadb.api.query.filter.Language;
import io.evitadb.storage.query.SqlPart;
import io.evitadb.storage.query.translate.filter.FilterTranslatingContext;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation of {@link FilterConstraintTranslator} for {@link Language} constraint.
 * This implementation generates SQL to filter out entities that does not have any data with specified language,
 * although language should be also taken into account in other filter constraint where it makes sense
 * (e.g. attributes have languages and querying changes with specified language).
 *
 * @author Lukáš Hornych 2021
 */
public class LanguageTranslator implements FilterConstraintTranslator<Language> {

    @Override
    public SqlPart translate(@Nonnull Language constraint, @Nonnull FilterTranslatingContext ctx) {
        return new SqlPart("? = any (entity.locales)", List.of(constraint.getLanguage().toString()));
    }
}
