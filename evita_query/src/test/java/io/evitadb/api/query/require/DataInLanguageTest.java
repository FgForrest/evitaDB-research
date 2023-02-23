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

package io.evitadb.api.query.require;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static io.evitadb.api.query.QueryConstraints.dataInLanguage;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link DataInLanguage} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class DataInLanguageTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final DataInLanguage dataInLanguage = dataInLanguage(Locale.ENGLISH, Locale.FRENCH);
		assertArrayEquals(new Locale[]{Locale.ENGLISH, Locale.FRENCH}, dataInLanguage.getLanguages());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(dataInLanguage().isApplicable());
		assertTrue(dataInLanguage(Locale.ENGLISH).isApplicable());
		assertTrue(dataInLanguage(Locale.ENGLISH, Locale.GERMAN).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final DataInLanguage dataInLanguage = dataInLanguage(Locale.ENGLISH, Locale.FRENCH);
		assertEquals("dataInLanguage(`en`,`fr`)", dataInLanguage.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(dataInLanguage(Locale.ENGLISH, Locale.FRENCH), dataInLanguage(Locale.ENGLISH, Locale.FRENCH));
		assertEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH), dataInLanguage(Locale.ENGLISH, Locale.FRENCH));
		assertNotEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH), dataInLanguage(Locale.ENGLISH, Locale.GERMAN));
		assertNotEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH), dataInLanguage(Locale.ENGLISH));
		assertEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH).hashCode(), dataInLanguage(Locale.ENGLISH, Locale.FRENCH).hashCode());
		assertNotEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH).hashCode(), dataInLanguage(Locale.ENGLISH, Locale.GERMAN).hashCode());
		assertNotEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH).hashCode(), dataInLanguage(Locale.ENGLISH).hashCode());
	}

	@Test
	void shouldCombineWithAnotherConstraint() {
		assertEquals(dataInLanguage(), dataInLanguage().combineWith(dataInLanguage(Locale.ENGLISH)));
		assertEquals(dataInLanguage(), dataInLanguage(Locale.ENGLISH).combineWith(dataInLanguage()));
		assertEquals(dataInLanguage(Locale.ENGLISH), dataInLanguage(Locale.ENGLISH).combineWith(dataInLanguage(Locale.ENGLISH)));
		assertEquals(dataInLanguage(Locale.ENGLISH, Locale.FRENCH), dataInLanguage(Locale.ENGLISH).combineWith(dataInLanguage(Locale.FRENCH)));
	}

}