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

import java.util.Locale;

import static io.evitadb.api.query.QueryConstraints.language;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Language} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class LanguageTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Language language = language(Locale.ENGLISH);
		assertEquals(Locale.ENGLISH, language.getLanguage());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(language(Locale.ENGLISH).isApplicable());
		assertFalse(new Language(null).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Language language = language(Locale.ENGLISH);
		assertEquals("language(`en`)", language.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(language(Locale.ENGLISH), language(Locale.ENGLISH));
		assertEquals(language(Locale.ENGLISH), language(Locale.ENGLISH));
		assertNotEquals(language(Locale.ENGLISH), language(Locale.FRANCE));
		assertNotEquals(language(Locale.ENGLISH), new Language(null));
		assertEquals(language(Locale.ENGLISH).hashCode(), language(Locale.ENGLISH).hashCode());
		assertNotEquals(language(Locale.ENGLISH).hashCode(), language(Locale.FRANCE).hashCode());
		assertNotEquals(language(Locale.ENGLISH).hashCode(), new Language(null).hashCode());
	}

}
