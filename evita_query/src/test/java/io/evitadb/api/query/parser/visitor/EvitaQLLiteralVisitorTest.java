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
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;

import static io.evitadb.api.dataType.EvitaDataTypes.formatValue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EvitaQLLiteralVisitor}
 *
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
class EvitaQLLiteralVisitorTest extends EvitaQLVisitorTestBase {

    @Test
    void shouldCreateVisitorWithUserSpecifiedDataTypes() {
        final EvitaQLLiteralVisitor visitor1 = EvitaQLLiteralVisitor.withAllowedTypes();
        assertEquals(0, visitor1.allowedLiteralTypes.size());

        final EvitaQLLiteralVisitor visitor2 = EvitaQLLiteralVisitor.withAllowedTypes(EvitaQLLiteralType.INT, EvitaQLLiteralType.STRING);
        assertEquals(2, visitor2.allowedLiteralTypes.size());
        assertTrue(visitor2.allowedLiteralTypes.contains(EvitaQLLiteralType.INT));
        assertTrue(visitor2.allowedLiteralTypes.contains(EvitaQLLiteralType.STRING));
    }

    @Test
    void shouldCreateVisitorWithAllComparableDataTypes() {
        final EvitaQLLiteralVisitor visitor = EvitaQLLiteralVisitor.withComparableTypesAllowed();
        assertEquals(12, visitor.allowedLiteralTypes.size());
    }

    @Test
    void shouldCreateVisitorWithAllDataTypes() {
        final EvitaQLLiteralVisitor visitor = EvitaQLLiteralVisitor.withAllTypesAllowed();
        assertEquals(EvitaQLLiteralType.values().length, visitor.allowedLiteralTypes.size());
    }

    @Test
    void shouldParseStringLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("'hello all'");
        assertEquals(String.class, literal1.getType());
        assertEquals("hello all", literal1.asString());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue("hello all"));
        assertEquals(String.class, literal2.getType());
        assertEquals("hello all", literal2.asString());
    }

    @Test
    void shouldNotParseStringLiteral() {
        assertThrows(RuntimeException.class, () -> parseLiteral("hello all"));
        assertThrows(RuntimeException.class, () -> parseLiteral("\"hello\""));
    }

    @Test
    void shouldParseIntLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("100");
        assertEquals(Long.class, literal1.getType());
        assertEquals(100, literal1.asLong());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(100L));
        assertEquals(Long.class, literal2.getType());
        assertEquals(100, literal2.asLong());
    }

    @Test
    void shouldNotParseIntLiteral() {
        final EvitaQLLiteral literal = parseLiteral("100.0");
        assertNotEquals(Long.class, literal.getType());
    }

    @Test
    void shouldParseFloatLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("100.55");
        assertEquals(BigDecimal.class, literal1.getType());
        assertEquals(BigDecimal.valueOf(100.55), literal1.asBigDecimal());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(BigDecimal.valueOf(100.55)));
        assertEquals(BigDecimal.class, literal2.getType());
        assertEquals(BigDecimal.valueOf(100.55), literal2.asBigDecimal());
    }

    @Test
    void shouldNotParseFloatLiteral() {
        final EvitaQLLiteral literal = parseLiteral("100");
        assertNotEquals(BigDecimal.class, literal.getType());
    }

    @Test
    void shouldParseBooleanLiteral() {
        final EvitaQLLiteral literalTrue1 = parseLiteral("true");
        assertEquals(Boolean.class, literalTrue1.getType());
        assertEquals(true, literalTrue1.asBoolean());

        final EvitaQLLiteral literalFalse1 = parseLiteral("false");
        assertEquals(Boolean.class, literalFalse1.getType());
        assertEquals(false, literalFalse1.asBoolean());

        final EvitaQLLiteral literalTrue2 = parseLiteral(formatValue(true));
        assertEquals(Boolean.class, literalTrue2.getType());
        assertEquals(true, literalTrue2.asBoolean());

        final EvitaQLLiteral literalFalse2 = parseLiteral(formatValue(false));
        assertEquals(Boolean.class, literalFalse2.getType());
        assertEquals(false, literalFalse2.asBoolean());
    }

    @Test
    void shouldNotParseBoolean() {
        assertThrows(RuntimeException.class, () -> parseLiteral("something"));
    }

    @Test
    void shouldParseDateLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("2020-02-08");
        assertEquals(LocalDate.class, literal1.getType());
        assertEquals(LocalDate.of(2020, 2, 8), literal1.asLocalDate());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(LocalDate.of(2020, 2, 8)));
        assertEquals(LocalDate.class, literal2.getType());
        assertEquals(LocalDate.of(2020, 2, 8), literal2.asLocalDate());
    }

    @Test
    void shouldNotParseDateLiteral() {
        final EvitaQLLiteral literal = parseLiteral("2020-02-8");
        assertNotEquals(LocalDate.class, literal.getType());
    }

    @Test
    void shouldParseTimeLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("13:30:55");
        assertEquals(LocalTime.class, literal1.getType());
        assertEquals(LocalTime.of(13, 30, 55), literal1.asLocalTime());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(LocalTime.of(13, 30, 55)));
        assertEquals(LocalTime.class, literal2.getType());
        assertEquals(LocalTime.of(13, 30, 55), literal2.asLocalTime());
    }

    @Test
    void shouldNotParseTimeLiteral() {
        final EvitaQLLiteral literal = parseLiteral("5:30");
        assertNotEquals(LocalTime.class, literal.getType());
    }

    @Test
    void shouldParseDateTimeLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("2020-02-08T13:30:55");
        assertEquals(LocalDateTime.class, literal1.getType());
        assertEquals(
                LocalDateTime.of(2020, 2, 8, 13, 30, 55),
                literal1.asLocalDateTime()
        );

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(LocalDateTime.of(2020, 2, 8, 13, 30, 55)));
        assertEquals(LocalDateTime.class, literal2.getType());
        assertEquals(
                LocalDateTime.of(2020, 2, 8, 13, 30, 55),
                literal2.asLocalDateTime()
        );
    }

    @Test
    void shouldNotParseDateTimeLiteral() {
        final EvitaQLLiteral literal = parseLiteral("2020-Jan-08T13:30:55");
        assertNotEquals(LocalDateTime.class, literal.getType());
    }

    @Test
    void shouldParseZonedDateTimeLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("2020-02-08T13:30:55+01:00[Europe/Prague]");
        assertEquals(ZonedDateTime.class, literal1.getType());
        assertEquals(
                ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                literal1.asZonedDateTime()
        );

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))));
        assertEquals(ZonedDateTime.class, literal2.getType());
        assertEquals(
                ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                literal2.asZonedDateTime()
        );
    }

    @Test
    void shouldNotParseZonedDateTimeLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("2020-02-08T13:30:55+01:00");
        assertNotEquals(ZonedDateTime.class, literal1.getType());

        final EvitaQLLiteral literal2 = parseLiteral("2020-02-08T13:30:55+1:00[Europe/Prague]");
        assertNotEquals(ZonedDateTime.class, literal2.getType());
    }

    @Test
    void shouldParseNumberRangerLiteral() {
        final EvitaQLLiteral literalFull1 = parseLiteral("[102,500]");
        assertEquals(NumberRange.class, literalFull1.getType());
        assertEquals(NumberRange.between(BigDecimal.valueOf(102), BigDecimal.valueOf(500)), literalFull1.asNumberRange());

        final EvitaQLLiteral literalWithoutEnd1 = parseLiteral("[102,]");
        assertEquals(NumberRange.class, literalWithoutEnd1.getType());
        assertEquals(NumberRange.between(BigDecimal.valueOf(102), null), literalWithoutEnd1.asNumberRange());

        final EvitaQLLiteral literalWithoutStart1 = parseLiteral("[,500]");
        assertEquals(NumberRange.class, literalWithoutStart1.getType());
        assertEquals(NumberRange.between(null, BigDecimal.valueOf(500)), literalWithoutStart1.asNumberRange());

        final EvitaQLLiteral literalFull2 = parseLiteral(formatValue(NumberRange.between(102L, 500L)));
        assertEquals(NumberRange.class, literalFull2.getType());
        assertEquals(NumberRange.between(BigDecimal.valueOf(102), BigDecimal.valueOf(500)), literalFull2.asNumberRange());

        final EvitaQLLiteral literalWithoutEnd2 = parseLiteral(formatValue(NumberRange.from(102L)));
        assertEquals(NumberRange.class, literalWithoutEnd2.getType());
        assertEquals(NumberRange.between(BigDecimal.valueOf(102), null), literalWithoutEnd2.asNumberRange());

        final EvitaQLLiteral literalWithoutStart2 = parseLiteral(formatValue(NumberRange.to(500L)));
        assertEquals(NumberRange.class, literalWithoutStart2.getType());
        assertEquals(NumberRange.between(null, BigDecimal.valueOf(500)), literalWithoutStart2.asNumberRange());
    }

    @Test
    void shouldNotParseNumberRangeLiteral() {
        assertThrows(RuntimeException.class, () -> parseLiteral("[858]"));
        assertThrows(RuntimeException.class, () -> parseLiteral("[]"));
    }

    @Test
    void shouldParseZonedDateTimeRangeLiteral() {
        final EvitaQLLiteral literalFull1 = parseLiteral("[2020-02-08T13:30:55+01:00[Europe/Prague],2020-02-09T13:30:55+01:00[Europe/Prague]]");
        assertEquals(DateTimeRange.class, literalFull1.getType());
        assertEquals(
                DateTimeRange.between(
                        ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                        ZonedDateTime.of(2020, 2, 9, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))
                ),
                literalFull1.asDateTimeRange()
        );

        final EvitaQLLiteral literalWithoutEnd1 = parseLiteral("[2020-02-08T13:30:55+01:00[Europe/Prague],]");
        assertEquals(DateTimeRange.class, literalWithoutEnd1.getType());
        assertEquals(
                DateTimeRange.between(
                        ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                        null
                ),
                literalWithoutEnd1.asDateTimeRange()
        );

        final EvitaQLLiteral literalWithoutStart1 = parseLiteral("[,2020-02-09T13:30:55+01:00[Europe/Prague]]");
        assertEquals(DateTimeRange.class, literalWithoutStart1.getType());
        assertEquals(
                DateTimeRange.between(
                        null,
                        ZonedDateTime.of(2020, 2, 9, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))
                ),
                literalWithoutStart1.asDateTimeRange()
        );

        final EvitaQLLiteral literalFull2 = parseLiteral(formatValue(
                DateTimeRange.between(
                        ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                        ZonedDateTime.of(2020, 2, 9, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))
                )
        ));
        assertEquals(DateTimeRange.class, literalFull2.getType());
        assertEquals(
                DateTimeRange.between(
                        ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                        ZonedDateTime.of(2020, 2, 9, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))
                ),
                literalFull2.asDateTimeRange()
        );

        final EvitaQLLiteral literalWithoutEnd2 = parseLiteral(formatValue(
                DateTimeRange.between(
                        ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                        null
                )
        ));
        assertEquals(DateTimeRange.class, literalWithoutEnd2.getType());
        assertEquals(
                DateTimeRange.between(
                        ZonedDateTime.of(2020, 2, 8, 13, 30, 55, 0, ZoneId.of("Europe/Prague")),
                        null
                ),
                literalWithoutEnd2.asDateTimeRange()
        );

        final EvitaQLLiteral literalWithoutStart2 = parseLiteral(formatValue(
                DateTimeRange.between(
                        null,
                        ZonedDateTime.of(2020, 2, 9, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))
                )
        ));
        assertEquals(DateTimeRange.class, literalWithoutStart2.getType());
        assertEquals(
                DateTimeRange.between(
                        null,
                        ZonedDateTime.of(2020, 2, 9, 13, 30, 55, 0, ZoneId.of("Europe/Prague"))
                ),
                literalWithoutStart2.asDateTimeRange()
        );
    }

    @Test
    void shouldNotParseZonedDateTimeRangeLiteral() {
        assertThrows(RuntimeException.class, () -> parseLiteral("[2020-02-08T13:30:55,2020-02-09T13:30:55]"));
        assertThrows(RuntimeException.class, () -> parseLiteral("[2020-02-08T13:30:55+1:00[Europe/Prague]]"));
        assertThrows(RuntimeException.class, () -> parseLiteral("[]"));
    }

    @Test
    void shouldParseEnumLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("WITH_VAT");
        assertEquals(EnumWrapper.class, literal1.getType());
        assertEquals(EnumWrapper.fromString("WITH_VAT"), literal1.asEnumWrapper());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(EnumWrapper.fromString("WITH_VAT")));
        assertEquals(EnumWrapper.class, literal2.getType());
        assertEquals(EnumWrapper.fromString("WITH_VAT"), literal2.asEnumWrapper());
    }

    @Test
    void shouldNotParseEnumLiteral() {
        assertThrows(RuntimeException.class, () -> parseLiteral("withVat"));
        assertThrows(RuntimeException.class, () -> parseLiteral("_WITH-VAT"));
    }

    @Test
    void shouldParseLocaleLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("`cs_CZ`");
        assertEquals(Locale.class, literal1.getType());
        assertEquals(new Locale("cs", "CZ"), literal1.asLocale());

        final EvitaQLLiteral literal2 = parseLiteral("`cs`");
        assertEquals(Locale.class, literal2.getType());
        assertEquals(new Locale("cs"), literal2.asLocale());

        final EvitaQLLiteral literal3 = parseLiteral(formatValue(new Locale("cs")));
        assertEquals(Locale.class, literal3.getType());
        assertEquals(new Locale("cs"), literal3.asLocale());

        final EvitaQLLiteral literal4 = parseLiteral(formatValue(new Locale("cs", "CZ")));
        assertEquals(Locale.class, literal4.getType());
        assertEquals(new Locale("cs", "CZ"), literal4.asLocale());
    }

    @Test
    void shouldNotParseLocaleLiteral() {
        assertThrows(RuntimeException.class, () -> parseLiteral("`123`"));
        assertThrows(RuntimeException.class, () -> parseLiteral("`cs_CZ_CZ`"));
        assertThrows(RuntimeException.class, () -> parseLiteral("`cs-CZ`"));

        final EvitaQLLiteral literal = parseLiteral("'cs_CZ'");
        assertNotEquals(Locale.class, literal.getType());
    }

    @Test
    void shouldParseMultipleLiteral() {
        final EvitaQLLiteral literal1 = parseLiteral("{123,'a',SOME_ENUM}");
        assertEquals(Multiple.class, literal1.getType());
        assertEquals(new Multiple(123L, "a", EnumWrapper.fromString("SOME_ENUM")), literal1.asMultiple());

        final EvitaQLLiteral literal2 = parseLiteral(formatValue(new Multiple(123L, "a", EnumWrapper.fromString("SOME_ENUM"))));
        assertEquals(Multiple.class, literal2.getType());
        assertEquals(new Multiple(123L, "a", EnumWrapper.fromString("SOME_ENUM")), literal2.asMultiple());
    }

    @Test
    void shouldNotParseMultipleLiteral() {
        assertThrows(RuntimeException.class, () -> parseLiteral("{}"));
        assertThrows(RuntimeException.class, () -> parseLiteral("{123}"));
        assertThrows(RuntimeException.class, () -> parseLiteral("{something}"));
        assertThrows(RuntimeException.class, () -> parseLiteral("{123,'a',{'b','c'}}"));

        final EvitaQLLiteral literal = parseLiteral("[123,159]");
        assertNotEquals(Multiple.class, literal.getType());
    }


    /**
     * Using generated EvitaQL parser tries to parse string as grammar rule "literal"
     *
     * @param string string to parse
     * @return parsed literal
     */
    private EvitaQLLiteral parseLiteral(String string) {
        lexer.setInputStream(CharStreams.fromString(string));
        parser.setInputStream(new CommonTokenStream(lexer));

        return parser.literal().accept(EvitaQLLiteralVisitor.withAllTypesAllowed());
    }
}
