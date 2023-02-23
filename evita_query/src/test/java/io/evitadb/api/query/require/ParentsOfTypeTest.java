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

import java.io.Serializable;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Parents} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ParentsOfTypeTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final ParentsOfType parents1 = parentsOfType("brand");
		assertArrayEquals(new Serializable[]{"brand"}, parents1.getEntityTypes());

		final ParentsOfType parents2 = parentsOfType("brand", "category");
		assertArrayEquals(new Serializable[]{"brand", "category"}, parents2.getEntityTypes());

		final ParentsOfType parents3 = parentsOfType("brand", entityBody(), attributes());
		assertArrayEquals(new Serializable[]{"brand"}, parents3.getEntityTypes());
		assertArrayEquals(new EntityContentRequire[]{entityBody(), attributes()}, parents3.getConstraints());

		final ParentsOfType parents4 = parentsOfType(new Serializable[]{"brand", "category"}, entityBody(), attributes());
		assertArrayEquals(new Serializable[]{"brand", "category"}, parents4.getEntityTypes());
		assertArrayEquals(new EntityContentRequire[]{entityBody(), attributes()}, parents4.getConstraints());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(parentsOfType("brand").isApplicable());
		assertTrue(parentsOfType(new Serializable[]{"brand", "category"}).isApplicable());
		assertTrue(parentsOfType(new Serializable[]{"brand", "category"}, new EntityContentRequire[]{entityBody()}).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final ParentsOfType parents = parentsOfType(new Serializable[]{"brand", "category"});
		assertEquals("parentsOfType('brand','category')", parents.toString());

		final ParentsOfType parentsWithRequirements = parentsOfType(new Serializable[]{"brand", "category"}, entityBody(), prices());
		assertEquals("parentsOfType('brand','category',entityBody(),prices(RESPECTING_FILTER))", parentsWithRequirements.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(parentsOfType("brand"), parentsOfType("brand"));
		assertEquals(parentsOfType(new Serializable[]{"brand", "category"}), parentsOfType(new Serializable[]{"brand", "category"}));
		assertNotEquals(parentsOfType("brand"), parentsOfType("category"));
		assertNotEquals(parentsOfType("brand"), parentsOfType(new Serializable[]{"priceList", "category"}));
		assertEquals(parentsOfType("brand").hashCode(), parentsOfType("brand").hashCode());
		assertNotEquals(parentsOfType("brand").hashCode(), parentsOfType("category").hashCode());
		assertNotEquals(parentsOfType("brand").hashCode(), parentsOfType(new Serializable[]{"brand", "category"}).hashCode());
	}

}
