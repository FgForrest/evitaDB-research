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

package io.evitadb.api.serialization.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link UnmodifiableCollectionsUnwrapper} class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class UnmodifiableCollectionsUnwrapperTest {

	@Test
	void shouldUnwrapUnmodifiableSet() {
		final Set<Object> me = new HashSet<>();
		final Set<Object> unwrappedMe = UnmodifiableCollectionsUnwrapper.unwrap(Collections.unmodifiableSet(me));
		assertSame(me, unwrappedMe);
	}

	@Test
	void shouldUnwrapEmptySet() {
		final Set<Object> unwrappedMe = UnmodifiableCollectionsUnwrapper.unwrap(Collections.unmodifiableSet(Collections.emptySet()));
		assertTrue(unwrappedMe.isEmpty());
		assertFalse(unwrappedMe.getClass().getName().contains("Collections"));
	}

	@Test
	void shouldUnwrapUnmodifiableList() {
		final List<Object> me = new ArrayList<>();
		final List<Object> unwrappedMe = UnmodifiableCollectionsUnwrapper.unwrap(Collections.unmodifiableList(me));
		assertSame(me, unwrappedMe);
	}

	@Test
	void shouldUnwrapEmptyList() {
		final List<Object> unwrappedMe = UnmodifiableCollectionsUnwrapper.unwrap(Collections.unmodifiableList(Collections.emptyList()));
		assertTrue(unwrappedMe.isEmpty());
		assertFalse(unwrappedMe.getClass().getName().contains("Collections"));
	}

	@Test
	void shouldUnwrapUnmodifiableMap() {
		final Map<Object, Object> me = new HashMap<>();
		final Map<Object, Object> unwrappedMe = UnmodifiableCollectionsUnwrapper.unwrap(Collections.unmodifiableMap(me));
		assertSame(me, unwrappedMe);
	}

	@Test
	void shouldUnwrapEmptyMap() {
		final Map<Object, Object> unwrappedMe = UnmodifiableCollectionsUnwrapper.unwrap(Collections.unmodifiableMap(Collections.emptyMap()));
		assertTrue(unwrappedMe.isEmpty());
		assertFalse(unwrappedMe.getClass().getName().contains("Collections"));
	}

}