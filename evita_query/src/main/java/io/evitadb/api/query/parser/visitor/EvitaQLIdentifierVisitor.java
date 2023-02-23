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

package io.evitadb.api.query.parser.visitor;

import io.evitadb.api.query.parser.grammar.EvitaQLBaseVisitor;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.grammar.EvitaQLVisitor;

/**
 * <p>Implementation of {@link EvitaQLVisitor} for parsing identifiers. All identifiers should be written as EvitaQL
 * strings.</p>
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public class EvitaQLIdentifierVisitor extends EvitaQLBaseVisitor<String> {

    @Override
    public String visitIdentifier(EvitaQLParser.IdentifierContext ctx) {
        return ctx.getText().substring(1, ctx.getText().length() - 1);
    }
}
