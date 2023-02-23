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

import io.evitadb.api.query.parser.grammar.EvitaQLLexer;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.junit.jupiter.api.BeforeAll;

/**
 * Tests base for testing EvitaQL visitor implementations with pre-configured lexer and parse with disabled error recovery.
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public abstract class EvitaQLVisitorTestBase {

    static final EvitaQLLexer lexer = new EvitaQLLexer(null);
    static final EvitaQLParser parser = new EvitaQLParser(null);

    @BeforeAll
    static void setupAll() {
        parser.setErrorHandler(new BailErrorStrategy());
    }
}
