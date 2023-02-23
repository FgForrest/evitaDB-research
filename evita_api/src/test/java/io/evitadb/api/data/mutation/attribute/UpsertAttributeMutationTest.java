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

package io.evitadb.api.data.mutation.attribute;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * This test verifies contract of {@link UpsertAttributeMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class UpsertAttributeMutationTest {

	@Test
	void shouldCreateNewAttribute() {
		final UpsertAttributeMutation mutation = new UpsertAttributeMutation(new AttributeKey("a"), (byte)5);
		final AttributeValue newValue = mutation.mutateLocal(null);
		assertEquals((byte)5, newValue.getValue());
		assertEquals(1L, newValue.getVersion());
	}

	@Test
	void shouldIncrementVersionByUpdatingAttribute() {
		final UpsertAttributeMutation mutation = new UpsertAttributeMutation(new AttributeKey("a"), (byte)5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (byte)3));
		assertEquals((byte) 5, newValue.getValue());
		assertEquals(2L, newValue.getVersion());
	}

	@Test
	void shouldReturnSameSkipToken() {
		assertEquals(
				new UpsertAttributeMutation(new AttributeKey("abc"), "B").getSkipToken(),
				new UpsertAttributeMutation(new AttributeKey("abc"), "C").getSkipToken()
		);
		assertEquals(
				new UpsertAttributeMutation(new AttributeKey("abc", Locale.ENGLISH), "B").getSkipToken(),
				new UpsertAttributeMutation(new AttributeKey("abc", Locale.ENGLISH), "C").getSkipToken()
		);
	}

	@Test
	void shouldReturnDifferentSkipToken() {
		assertNotEquals(
				new UpsertAttributeMutation(new AttributeKey("abc"), "B").getSkipToken(),
				new UpsertAttributeMutation(new AttributeKey("abe"), "C").getSkipToken()
		);
		assertNotEquals(
				new UpsertAttributeMutation(new AttributeKey("abc", Locale.ENGLISH), "B").getSkipToken(),
				new UpsertAttributeMutation(new AttributeKey("abc", Locale.GERMAN), "C").getSkipToken()
		);
	}

}