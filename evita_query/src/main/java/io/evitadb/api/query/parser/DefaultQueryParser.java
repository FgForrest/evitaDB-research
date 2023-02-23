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

package io.evitadb.api.query.parser;

import io.evitadb.api.query.Constraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.QueryParser;
import io.evitadb.api.query.parser.grammar.EvitaQLLexer;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.visitor.EvitaQLConstraintVisitor;
import io.evitadb.api.query.parser.visitor.EvitaQLLiteralVisitor;
import io.evitadb.api.query.parser.visitor.EvitaQLQueryVisitor;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.Serializable;

/**
 * Implementation of {@link QueryParser} using ANTLR4 parser and lexer.
 *
 * <b>Note: </b>the generated ANTLR4 parser is set to not to recover from syntax errors using {@link BailErrorStrategy}
 * so an exception is immediately thrown.
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public class DefaultQueryParser implements QueryParser {
    private static final DefaultQueryParser INSTANCE = new DefaultQueryParser();

    private final EvitaQLQueryVisitor queryVisitor = new EvitaQLQueryVisitor();
    private final EvitaQLConstraintVisitor constraintVisitor = new EvitaQLConstraintVisitor();
    private final EvitaQLLiteralVisitor literalVisitor = EvitaQLLiteralVisitor.withAllTypesAllowed();

    /**
     * @return thread safe instance of this class
     */
    public static DefaultQueryParser getInstance() {
        return INSTANCE;
    }


    @Override
    public Query parseQuery(String query) {
        final EvitaQLParser parser = createAndSetupParser(query);
        return parser.root().query().accept(queryVisitor);
    }

    @Override
    public Constraint<?> parseConstraint(String constraint) {
        final EvitaQLParser parser = createAndSetupParser(constraint);
        return parser.root().constraint().accept(constraintVisitor);
    }

    @Override
    public <T extends Serializable> T parseLiteral(String literal) {
        final EvitaQLParser parser = createAndSetupParser(literal);
        return parser.root().literal().accept(literalVisitor).getValue();
    }


    /**
     * Returns new preconfigured Evita QL parser with preconfigured lexer to string that is being parsed
     *
     * @param stringToParse
     */
    protected EvitaQLParser createAndSetupParser(String stringToParse) {
        final EvitaQLLexer lexer = new EvitaQLLexer(CharStreams.fromString(stringToParse));

        final EvitaQLParser parser = new EvitaQLParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());

        return parser;
    }
}

