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

/**
 * Lists all supported data types by EvitaQL parser. Each type corresponds to certain Java type.
 * Mainly used for specifying allowed types when parsing literals.
 * All types must be supported by Evita itself (check {@link io.evitadb.api.dataType.EvitaDataTypes}).
 *
 * @see io.evitadb.api.query.parser.visitor.EvitaQLLiteralVisitor
 * @author Lukáš Hornych, FG Forrest a.s. (c) 2021
 */
public enum EvitaQLLiteralType {

    /**
     * Represents string value. Converts to {@link String}.
     */
    STRING,
    /**
     * Represents integer value. Converts to {@link Long}.
     */
    INT,
    /**
     * Represents float value. Converts to {@link java.math.BigDecimal}.
     */
    FLOAT,
    /**
     * Represents boolean value. Converts to {@link Boolean}.
     */
    BOOLEAN,
    /**
     * Represents date value without time part. Converts to {@link java.time.LocalDate}.
     */
    DATE,
    /**
     * Represents time part without date. Converts to {@link java.time.LocalTime}.
     */
    TIME,
    /**
     * Represents whole datetime value. Converts to {@link java.time.LocalDateTime}.
     */
    DATE_TIME,
    /**
     * Represents whole datetime value with zone. Converts to {@link java.time.ZonedDateTime}.
     */
    ZONED_DATE_TIME,
    /**
     * Represents range of numbers. Converts to {@link io.evitadb.api.dataType.NumberRange}.
     */
    NUMBER_RANGE,
    /**
     * Represents range of zoned datetimes. Converts to {@link io.evitadb.api.dataType.DateTimeRange}.
     */
    DATE_TIME_RANGE,
    /**
     * Represents value of some enum. Converts to {@link io.evitadb.api.dataType.EnumWrapper}. Concrete {@link Enum} is
     * resolved later by specifying concrete {@link Enum}.
     */
    ENUM,
    /**
     * Represents locale. Converts to {@link java.util.Locale}.
     */
    LOCALE,
    /**
     * Represents multiple data type. Converts to {@link io.evitadb.api.dataType.Multiple}
     */
    MULTIPLE
}
