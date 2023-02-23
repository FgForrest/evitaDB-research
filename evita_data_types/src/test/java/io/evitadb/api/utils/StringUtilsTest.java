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

package io.evitadb.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class verifies behaviour of {@link StringUtils}
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class StringUtilsTest {

	@Test
	void shouldUncapitalizeString() {
		assertEquals("abc", StringUtils.uncapitalize("Abc"));
		assertEquals("aBC", StringUtils.uncapitalize("ABC"));
		assertEquals("abc", StringUtils.uncapitalize("abc"));
	}

	@Test
	void shouldFormatNano() {
		assertEquals("106751d 23h 47m 16s", StringUtils.formatNano(Long.MAX_VALUE));
		assertEquals("14s", StringUtils.formatNano(14587877547L));
		assertEquals("0.000000001s", StringUtils.formatNano(1L));
	}

	@Test
	void shouldFormatPreciseNano() {
		assertEquals("106751d 23h 47m 16.854775807s", StringUtils.formatPreciseNano(Long.MAX_VALUE));
		assertEquals("14.587877547s", StringUtils.formatPreciseNano(14587877547L));
	}

	@Test
	void shouldRemoveDiacritics() {
		assertEquals("Prilis zlutoucky kun skakal pres zive ploticky a behal po poli. @#$%^&*()escrzyaieESCRZYAIE",
			StringUtils.removeDiacritics("Příliš žluťoučký kůň skákal přes živé plotíčky a běhal po poli. @#$%^&*()ěščřžýáíéĚŠČŘŽÝÁÍÉ"));
	}

	@Test
	void shouldRemoveDiacriticsForFileName() {
		assertEquals("Prilis-zlutoucky-kun-skakal-pres-zive-ploticky-a-behal-po-poli.-escrzyaieESCRZYAIE",
			StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept("Příliš žluťoučký kůň skákal přes živé plotíčky a běhal po poli. @#$%^&*()ěščřžýáíéĚŠČŘŽÝÁÍÉ", '-', "."));
		assertEquals("Toto-je-priklad-divneho-nazvu-souboru-atd.-kdovi-co-nesmysly-.-txt",
			StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept("Toto je : příklad / divného, názvu souboru & atd. @# + kdoví co, nesmysly     {}|<>/! . txt", '-', "."));
	}

	@Test
	void shouldRemoveDiacriticsForFileNameStripStartAndEnd() {
		assertEquals("Prilis-zlutoucky-kun-skakal-pres-zive-ploticky-a-behal-po-poli.-escrzyaieESCRZYAIE",
			StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept("Příliš žluťoučký kůň skákal přes živé plotíčky a běhal po poli. @#$%^&*()ěščřžýáíéĚŠČŘŽÝÁÍÉ", '-', "."));
		assertEquals("Toto-je-priklad-divneho-nazvu-souboru-atd.-kdovi-co-nesmysly-.-txt",
			StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept("****Toto je : příklad / divného, názvu souboru & atd. @# + kdoví co, nesmysly     {}|<>/! . txt***", '-', "."));
	}

	@Test
	void shouldRemoveDiacriticsAndAllWeirdCharacters() {
		assertEquals("Prilis-zlutoucky-kun-skakal-pres-zive-ploticky-a-behal-po-poli-escrzyaieESCRZYAIE",
			StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept("Příliš žluťoučký kůň skákal přes živé plotíčky a běhal po poli. @#$%^&*()ěščřžýáíéĚŠČŘŽÝÁÍÉ", '-', ""));
		assertEquals("Toto-je-priklad-divneho-nazvu-souboru-atd-kdovi-co-nesmysly-txt",
			StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept("****Toto je : příklad / divného, názvu souboru & atd. @# + kdoví co, nesmysly     {}|<>/! . txt***", '-', ""));
	}

	@Test
	void shouldFormatRequestsPerSec() {
		assertEquals("114461.18 reqs/s", StringUtils.formatRequestsPerSec(457897, 4_000_456_879L));
	}

	@Test
	void shouldFormatByteSize() {
		assertEquals("1.04 GB", StringUtils.formatByteSize(1_120_000_000));
		assertEquals("1.15 GB", StringUtils.formatByteSize(1_240_000_000));
		assertEquals("1.07 MB", StringUtils.formatByteSize(1_120_000));
		assertEquals("1.18 MB", StringUtils.formatByteSize(1_240_000));
		assertEquals("1 KB", StringUtils.formatByteSize(1_240));
		assertEquals("240 B", StringUtils.formatByteSize(240));
		assertEquals("371.83 MB", StringUtils.formatByteSize(389_888_429));
	}

	@Test
	void shouldFormatCountInt() {
		assertEquals("1.12 bil.", StringUtils.formatCount(1_120_000_000));
		assertEquals("1.12 mil.", StringUtils.formatCount(1_120_000));
		assertEquals("1.24 thousands", StringUtils.formatCount(1_240));
		assertEquals("240", StringUtils.formatCount(240));
		assertEquals("389.89 mil.", StringUtils.formatCount(389_888_429));
	}

	@Test
	void shouldFormatCountLong() {
		assertEquals("1.12 bil.", StringUtils.formatCount(1_120_000_000L));
		assertEquals("1.12 mil.", StringUtils.formatCount(1_120_000L));
		assertEquals("1.24 thousands", StringUtils.formatCount(1_240L));
		assertEquals("240", StringUtils.formatCount(240L));
		assertEquals("389.89 mil.", StringUtils.formatCount(389_888_429L));
	}

}