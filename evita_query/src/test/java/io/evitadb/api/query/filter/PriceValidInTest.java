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

import static io.evitadb.api.query.QueryConstraints.priceValidIn;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link PriceValidIn} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class PriceValidInTest {

	@Test
	void shouldCreateMomentViaFactoryClassWorkAsExpected() {
		final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		final PriceValidIn priceValidIn = priceValidIn(now);
		assertEquals(now, priceValidIn.getTheMoment());
	}
	
	@Test
	void shouldRecognizeApplicability() {
		assertFalse(new PriceValidIn(null).isApplicable());
		assertTrue(priceValidIn(ZonedDateTime.now(ZoneId.systemDefault())).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final PriceValidIn inDateRange = priceValidIn(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
		assertEquals("priceValidIn(2021-01-01T00:00:00+01:00[Europe/Prague])", inDateRange.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		assertNotSame(priceValidIn(now), priceValidIn(now));
		assertEquals(priceValidIn(now), priceValidIn(now));
		assertNotEquals(priceValidIn(now), priceValidIn(now.plusHours(1)));
		assertEquals(priceValidIn(now).hashCode(), priceValidIn(now).hashCode());
		assertNotEquals(priceValidIn(now).hashCode(), priceValidIn(now.plusHours(1)).hashCode());
	}

}