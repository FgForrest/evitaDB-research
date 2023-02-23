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

package io.evitadb.api.query;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test verifies {@link Query} object creation, normalization and java equals and hash code contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class QueryTest {

	@Test
	void shouldCreateQueryAndPrettyPrintIt() {
		final Query query = query(
			entities("brand"),
			filterBy(
				and(
					eq("code", "samsung"),
					inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
				)
			),
			orderBy(
				ascending("name")
			),
			require(
				page(1, 5)
			)
		);
		assertEquals(
			"query(\n" +
				"\tentities('brand'),\n" +
				"\tfilterBy(\n" +
				"\t\tand(\n" +
				"\t\t\tequals('code', 'samsung'),\n" +
				"\t\t\tinRange('validity', 2020-01-01T00:00:00+01:00[Europe/Prague])\n" +
				"\t\t)\n" +
				"\t),\n" +
				"\torderBy(\n" +
				"\t\tascending('name')\n" +
				"\t),\n" +
				"\trequire(\n" +
				"\t\tpage(1, 5)\n" +
				"\t)\n" +
				")",
			query.prettyPrint()
		);
	}

	@Test
	void shouldCreateIncompleteQueryAndPrettyPrintIt() {
		final Query query = query(
			entities("brand"),
			require(
				page(1, 5)
			)
		);
		assertEquals(
			"query(\n" +
				"\tentities('brand'),\n" +
				"\trequire(\n" +
				"\t\tpage(1, 5)\n" +
				"\t)\n" +
				")",
			query.prettyPrint()
		);
	}

	@Test
	void shouldCreateQueryAndPrintIt() {
		final Query query = query(
			entities("brand"),
			filterBy(
				and(
					eq("code", "samsung"),
					inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
				)
			),
			orderBy(
				ascending("name")
			),
			require(
				page(1, 5)
			)
		);
		assertEquals(
			"query(entities('brand'),filterBy(and(equals('code','samsung'),inRange('validity',2020-01-01T00:00:00+01:00[Europe/Prague]))),orderBy(ascending('name')),require(page(1,5)))",
			query.toString()
		);
	}

	@Test
	void shouldVerifyEquals() {
		assertEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			),
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			)
		);
	}

	@Test
	void shouldVerifyNotEqualsByValueChange() {
		assertNotEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			),
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "nokia"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			)
		);
	}

	@Test
	void shouldVerifyNotEqualsByEntityChange() {
		assertNotEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			),
			query(
				entities("product"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			)
		);
	}

	@Test
	void shouldVerifyNotEqualsByDifferentStructure() {
		assertNotEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			),
			query(
				entities("product"),
				filterBy(
					or(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					descending("name")
				),
				require(
					page(1, 5)
				)
			)
		);
	}

	@Test
	void shouldVerifyHashCodeEquals() {
		assertEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode(),
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode()
		);
	}

	@Test
	void shouldVerifyHashCodeNotEqualsByValueChange() {
		assertNotEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode(),
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "nokia"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode()
		);
	}

	@Test
	void shouldVerifyHashCodeNotEqualsByEntityChange() {
		assertNotEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode(),
			query(
				entities("product"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode()
		);
	}

	@Test
	void shouldVerifyHashCodeNotEqualsByDifferentStructure() {
		assertNotEquals(
			query(
				entities("brand"),
				filterBy(
					and(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode(),
			query(
				entities("product"),
				filterBy(
					or(
						eq("code", "samsung"),
						inRange("validity", ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
					)
				),
				orderBy(
					descending("name")
				),
				require(
					page(1, 5)
				)
			).hashCode()
		);
	}

	@Test
	void shouldNormalizeQueryByRemovingInvalidConstraints() {
		assertEquals(
			query(
				entities("product")
			),
			query(
				entities("product"),
				filterBy(
					eq("code", null)
				)
			).normalizeQuery()
		);
	}

	@Test
	void shouldNormalizeQueryByFlatteningUnnecessaryConstraintContainers() {
		assertEquals(
			query(
				entities("product"),
				filterBy(
					eq("code", "abc")
				),
				orderBy(
					ascending("name")
				)
			),
			query(
				entities("product"),
				filterBy(
					and(
						eq("code", "abc")
					)
				),
				orderBy(
					ascending("name")
				)
			).normalizeQuery()
		);
	}

	@Test
	void shouldNormalizeQueryByRemovingInvalidAndFlatteningUnnecessaryConstraintContainers() {
		assertEquals(
			query(
				entities("product"),
				orderBy(
					ascending("name")
				)
			),
			query(
				entities("product"),
				filterBy(
					and(eq("code", null))
				),
				orderBy(
					ascending("name")
				)
			).normalizeQuery()
		);
	}

	@Test
	void shouldNormalizeQueryByRemovingInvalidAndFlatteningUnnecessaryConstraintContainersInComplexScenario() {
		assertEquals(
			query(
				entities("product"),
				filterBy(
					isNotNull("valid")
				),
				orderBy(
					ascending("name")
				),
				require(
					page(1, 5)
				)
			),
			query(
				entities("product"),
				filterBy(
					and(
						or(
							eq("code", null),
							isNull(null)
						),
						and(
							isNotNull("valid")
						)
					)
				),
				orderBy(
					ascending("name"),
					descending(null)
				),
				require(
					page(1, 5)
				)
			).normalizeQuery()
		);
	}

	@Test
	void shouldNormalizeQueryByRemovingInvalidAndFlatteningUnnecessaryConstraintContainersInComplexDeepScenario() {
		assertEquals(
			query(
				entities("product"),
				filterBy(
					isNotNull("valid")
				)
			),
			query(
				entities("product"),
				filterBy(
					and(
						or(
							eq("code", null),
							isNull(null),
							null,
							or(
								isTrue(null),
								null
							)
						),
						and(
							isNotNull("valid")
						),
						or(
							and(
								not(null)
							)
						)
					)
				)
			).normalizeQuery()
		);
	}

}
