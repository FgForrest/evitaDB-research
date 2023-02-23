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

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests verifies basic properties of {@link Parents} constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ParentsTest {

	@Test
	void shouldCreateViaFactoryClassWorkAsExpected() {
		final Parents parents = parents(entityBody());
		assertArrayEquals(new EntityContentRequire[]{entityBody()}, parents.getRequirements());
	}

	@Test
	void shouldRecognizeApplicability() {
		assertTrue(parents().isApplicable());
		assertTrue(parents(entityBody()).isApplicable());
		assertTrue(parents(entityBody(), prices()).isApplicable());
	}

	@Test
	void shouldToStringReturnExpectedFormat() {
		final Parents parents = parents(entityBody(), prices());
		assertEquals("parents(entityBody(),prices(RESPECTING_FILTER))", parents.toString());
	}

	@Test
	void shouldConformToEqualsAndHashContract() {
		assertNotSame(parents(), parents());
		assertEquals(parents(), parents());
		assertNotEquals(parents(entityBody()), parents(prices()));
		assertNotEquals(parents(entityBody()), parents(entityBody(), prices()));
		assertEquals(parents(entityBody()).hashCode(), parents(entityBody()).hashCode());
		assertNotEquals(parents(entityBody()).hashCode(), parents(prices()).hashCode());
		assertNotEquals(parents().hashCode(), parents(entityBody()).hashCode());
	}

}
