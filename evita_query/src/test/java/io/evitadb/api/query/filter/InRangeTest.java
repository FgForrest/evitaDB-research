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

package io.evitadb.api.query.filter;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static io.evitadb.api.query.QueryConstraints.inRange;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link InRange} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class InRangeTest {

	@Test
	void shouldCreateMomentViaFactoryClassWorkAsExpected() {
		final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		final InRange inRange = inRange("validity", now);
		assertEquals("validity", inRange.getAttributeName());
		assertEquals(now, inRange.getTheMoment());
		assertNull(inRange.getTheValue());
	}

	@Test
	void shouldCreateNumberViaFactoryClassWorkAsExpected() {
		final InRange inRange = inRange("age", 19);
		assertEquals("age", inRange.getAttributeName());
		assertEquals(19, inRange.getTheValue());
		assertNull(inRange.getTheMoment());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new InRange(null, (Number) null).isApplicable());
		assertTrue(new InRange("validity", (Number) null).isApplicable());
		assertTrue(inRange("validity", ZonedDateTime.now(ZoneId.systemDefault())).isApplicable());
		assertTrue(inRange("age", 19).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final InRange inDateRange = inRange("validity", ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
		assertEquals("inRange('validity',2021-01-01T00:00:00+01:00[Europe/Prague])", inDateRange.toString());

		final InRange inNumberRange = inRange("age", 19);
		assertEquals("inRange('age',19)", inNumberRange.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(inRange("age", 19), inRange("age", 19));
		assertEquals(inRange("age", 19), inRange("age", 19));
		assertNotEquals(inRange("age", 19), inRange("age", 16));
		assertNotEquals(inRange("age", 19), inRange("validity", ZonedDateTime.now(ZoneId.systemDefault())));
		assertEquals(inRange("age", 19).hashCode(), inRange("age", 19).hashCode());
		assertNotEquals(inRange("age", 19).hashCode(), inRange("age", 6).hashCode());
		assertNotEquals(inRange("age", 19).hashCode(), inRange("whatever", 19).hashCode());
	}

}