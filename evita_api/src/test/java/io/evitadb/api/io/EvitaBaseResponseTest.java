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

package io.evitadb.api.io;

import io.evitadb.api.dataType.PaginatedList;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.entities;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link EvitaBaseResponseTest} class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EvitaBaseResponseTest {

	@Test
	void shouldPassExtraDataInResponse() {
		final EvitaResponseBase<String> response = new EvitaResponseBase<>(
				query(entities("brand")), PaginatedList.<String>emptyList()
		) {};

		response.addAdditionalResults(new MockEvitaResponseExtraResult("a"));
		assertNotNull(response.getAdditionalResults(MockEvitaResponseExtraResult.class));
		assertEquals("a", response.getAdditionalResults(MockEvitaResponseExtraResult.class).getData());
		assertNull(response.getAdditionalResults(EvitaResponseExtraResult.class));
	}

	@Data
	private static class MockEvitaResponseExtraResult implements EvitaResponseExtraResult {
		private static final long serialVersionUID = 133944519712518780L;
		private final String data;
	}
}