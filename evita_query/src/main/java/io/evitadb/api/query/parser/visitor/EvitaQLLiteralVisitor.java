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

import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.EnumWrapper;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.query.parser.EvitaQLLiteral;
import io.evitadb.api.query.parser.EvitaQLLiteralType;
import io.evitadb.api.query.parser.exception.EvitaQLLiteralParsingFailedException;
import io.evitadb.api.query.parser.grammar.EvitaQLBaseVisitor;
import io.evitadb.api.query.parser.grammar.EvitaQLParser;
import io.evitadb.api.query.parser.grammar.EvitaQLParser.LiteralContext;
import io.evitadb.api.query.parser.grammar.EvitaQLVisitor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * <p>Implementation of {@link EvitaQLVisitor} for parsing all literal rule types. It produces wrapper {@link EvitaQLLiteral}
 * for all parsed values.</p>
 *
 * <p>When creating new instance one can specify which data types are allowed and which not. This allows specifying
 * which data types are allowed in current context (e.g. in some constraint's parameters only integers are allowed).</p>
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public class EvitaQLLiteralVisitor extends EvitaQLBaseVisitor<EvitaQLLiteral> {

    protected static final EvitaQLLiteralVisitor literalVisitorForMultiple = EvitaQLLiteralVisitor.withAllowedTypes(
            EvitaQLLiteralType.STRING,
            EvitaQLLiteralType.INT,
            EvitaQLLiteralType.FLOAT,
            EvitaQLLiteralType.BOOLEAN,
            EvitaQLLiteralType.DATE,
            EvitaQLLiteralType.TIME,
            EvitaQLLiteralType.DATE_TIME,
            EvitaQLLiteralType.ZONED_DATE_TIME,
            EvitaQLLiteralType.NUMBER_RANGE,
            EvitaQLLiteralType.DATE_TIME_RANGE,
            EvitaQLLiteralType.ENUM
    );


    /**
     * List of {@link EvitaQLLiteralType} which are allowed to be parsed.
     */
    protected final List<EvitaQLLiteralType> allowedLiteralTypes;


    /**
     * Creates literal visitor with custom array of literal types from {@link EvitaQLLiteralType} allowed for parsing.
     */
    private EvitaQLLiteralVisitor(EvitaQLLiteralType... allowedLiteralTypes) {
        if (allowedLiteralTypes == null) {
            this.allowedLiteralTypes = List.of();
            return;
        }

        this.allowedLiteralTypes = List.of(allowedLiteralTypes);
    }

    /**
     * Creates literal visitor with all of {@link EvitaQLLiteralType} allowed for parsing.
     */
    public static EvitaQLLiteralVisitor withAllTypesAllowed() {
        return new EvitaQLLiteralVisitor(EvitaQLLiteralType.values());
    }

    /**
     * Creates literal visitor with all comparable types from {@link EvitaQLLiteralType} allowed for parsing.
     */
    public static EvitaQLLiteralVisitor withComparableTypesAllowed() {
        return new EvitaQLLiteralVisitor(
                EvitaQLLiteralType.STRING,
                EvitaQLLiteralType.INT,
                EvitaQLLiteralType.FLOAT,
                EvitaQLLiteralType.BOOLEAN,
                EvitaQLLiteralType.DATE,
                EvitaQLLiteralType.TIME,
                EvitaQLLiteralType.DATE_TIME,
                EvitaQLLiteralType.ZONED_DATE_TIME,
                EvitaQLLiteralType.NUMBER_RANGE,
                EvitaQLLiteralType.DATE_TIME_RANGE,
                EvitaQLLiteralType.ENUM,
                EvitaQLLiteralType.MULTIPLE
        );
    }

    /**
     * Creates literal visitor with custom array of literal types from {@link EvitaQLLiteralType} allowed for parsing.
     */
    public static EvitaQLLiteralVisitor withAllowedTypes(EvitaQLLiteralType... allowedLiterals) {
        return new EvitaQLLiteralVisitor(allowedLiterals);
    }


    @Override
    public EvitaQLLiteral visitIntLiteral(EvitaQLParser.IntLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.INT, ctx);
        return new EvitaQLLiteral(
                Long.valueOf(ctx.getText())
        );
    }

    @Override
    public EvitaQLLiteral visitStringLiteral(EvitaQLParser.StringLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.STRING, ctx);
        return new EvitaQLLiteral(
                ctx.getText().substring(1, ctx.getText().length() - 1)
        );
    }

    @Override
    public EvitaQLLiteral visitFloatLiteral(EvitaQLParser.FloatLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.FLOAT, ctx);
        return new EvitaQLLiteral(
                new BigDecimal(ctx.getText())
        );
    }

    @Override
    public EvitaQLLiteral visitBooleanLiteral(EvitaQLParser.BooleanLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.BOOLEAN, ctx);
        return new EvitaQLLiteral(
                Boolean.parseBoolean(ctx.getText())
        );
    }

    @Override
    public EvitaQLLiteral visitDateLiteral(EvitaQLParser.DateLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.DATE, ctx);
        return new EvitaQLLiteral(
                LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(ctx.getText()))
        );
    }

    @Override
    public EvitaQLLiteral visitTimeLiteral(EvitaQLParser.TimeLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.TIME, ctx);
        return new EvitaQLLiteral(
                LocalTime.from(DateTimeFormatter.ISO_LOCAL_TIME.parse(ctx.getText()))
        );
    }

    @Override
    public EvitaQLLiteral visitDateTimeLiteral(EvitaQLParser.DateTimeLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.DATE_TIME, ctx);
        return new EvitaQLLiteral(
                LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(ctx.getText()))
        );
    }

    @Override
    public EvitaQLLiteral visitZonedDateTimeLiteral(EvitaQLParser.ZonedDateTimeLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.ZONED_DATE_TIME, ctx);
        return new EvitaQLLiteral(
                ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(ctx.getText()))
        );
    }

    @Override
    public EvitaQLLiteral visitEnumLiteral(EvitaQLParser.EnumLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.ENUM, ctx);
        return new EvitaQLLiteral(
                EnumWrapper.fromString(ctx.getText())
        );
    }

    @Override
    public EvitaQLLiteral visitNumberRangeLiteral(EvitaQLParser.NumberRangeLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.NUMBER_RANGE, ctx);
        return new EvitaQLLiteral(
                NumberRange.fromString(ctx.getText())
        );
    }

    @Override
    public EvitaQLLiteral visitDateTimeRangeLiteral(EvitaQLParser.DateTimeRangeLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.DATE_TIME_RANGE, ctx);
        return new EvitaQLLiteral(
                DateTimeRange.fromString(ctx.getText())
        );
    }

    @Override
    public EvitaQLLiteral visitLocaleLiteral(EvitaQLParser.LocaleLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.LOCALE, ctx);

        final int langDelimiter = ctx.getText().indexOf('_', 1);
        return new EvitaQLLiteral(langDelimiter > -1 ?
                new Locale(
                        ctx.getText().substring(1, langDelimiter),
                        ctx.getText().substring(langDelimiter + 1, ctx.getText().length() - 1)
                ) :
                new Locale(
                        ctx.getText().substring(1, ctx.getText().length() - 1)
                )
        );
    }

    @Override
    public EvitaQLLiteral visitMultipleLiteral(EvitaQLParser.MultipleLiteralContext ctx) {
        checkIfLiteralTypeIsAllowed(EvitaQLLiteralType.MULTIPLE, ctx);
        final List<LiteralContext> values = ctx.values;
        final Multiple multiple;
        if (values.size() == 2) {
            multiple = new Multiple(
                values.get(0).accept(literalVisitorForMultiple).asComparableAndSerializable(),
                values.get(1).accept(literalVisitorForMultiple).asComparableAndSerializable()
            );
        } else if (values.size() == 3) {
            multiple = new Multiple(
                values.get(0).accept(literalVisitorForMultiple).asComparableAndSerializable(),
                values.get(1).accept(literalVisitorForMultiple).asComparableAndSerializable(),
                values.get(2).accept(literalVisitorForMultiple).asComparableAndSerializable()
            );
        } else if (values.size() == 4) {
            multiple = new Multiple(
                values.get(0).accept(literalVisitorForMultiple).asComparableAndSerializable(),
                values.get(1).accept(literalVisitorForMultiple).asComparableAndSerializable(),
                values.get(2).accept(literalVisitorForMultiple).asComparableAndSerializable(),
                values.get(3).accept(literalVisitorForMultiple).asComparableAndSerializable()
            );
        } else {
            throw new IllegalArgumentException(
                "Currently only 2 to 4 arguments are allowed in Multiple data type."
            );
        }
        return new EvitaQLLiteral(multiple);
    }

    /**
     * Checks if literal type being parsed is desired by caller.
     * @param literalType target type of literal being parsed
     * @param ctx context of literal being parsed
     */
    protected void checkIfLiteralTypeIsAllowed(EvitaQLLiteralType literalType, EvitaQLParser.LiteralContext ctx) {
        if ((allowedLiteralTypes == null) || allowedLiteralTypes.stream().anyMatch(type -> type.equals(literalType))) {
            return;
        }

        throw new EvitaQLLiteralParsingFailedException(
                String.format(
                        "Literal type \"%s\" is not allowed in \"%s\".",
                        literalType.toString(),
                        ctx.getParent().getParent().getText()
                )
        );
    }
}
