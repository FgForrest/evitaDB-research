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
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.exception.InvalidMutationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies constract of {@link ApplyDeltaAttributeMutation} mutation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ApplyDeltaAttributeMutationTest {

	@Test
	void shouldIncrementVersionByUpdatingAttribute() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (byte)5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (byte)3));
		assertEquals(2L, newValue.getVersion());
	}

	@Test
	void shouldIncrementExistingByteValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (byte)5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey( "a"), (byte)3));
		assertEquals((byte) 8, (byte) newValue.getValue());
	}

	@Test
	void shouldDecrementExistingByteValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (byte)-5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (byte)3));
		assertEquals((byte)-2, (byte) newValue.getValue());
	}

	@Test
	void shouldIncrementExistingShortValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (short)5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (short)3));
		assertEquals((short) 8, (short) newValue.getValue());
	}

	@Test
	void shouldDecrementExistingShortValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (short)-5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (short)3));
		assertEquals((short)-2, (short) newValue.getValue());
	}
	
	@Test
	void shouldIncrementExistingIntValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", 5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), 3));
		assertEquals(8, (int) newValue.getValue());
	}

	@Test
	void shouldDecrementExistingIntValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", -5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), 3));
		assertEquals(-2, (int) newValue.getValue());
	}

	@Test
	void shouldIncrementExistingLongValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (long)5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (long)3));
		assertEquals((long) 8, (long) newValue.getValue());
	}

	@Test
	void shouldDecrementExistingLongValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (long)-5);
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (long)3));
		assertEquals((long)-2, (long) newValue.getValue());
	}

	@Test
	void shouldIncrementExistingBigDecimalValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", new BigDecimal("5.123"));
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), new BigDecimal("3.005")));
		assertEquals(new BigDecimal("8.128"), newValue.getValue());
	}

	@Test
	void shouldDecrementExistingBigDecimalValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", new BigDecimal("-5.123"));
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), new BigDecimal("3.005")));
		assertEquals(new BigDecimal("-2.118"), newValue.getValue());
	}

	@Test
	void shouldPassRangeCheckWithValidValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (byte)5, NumberRange.from(7));
		final AttributeValue newValue = mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (byte)3));
		assertEquals((byte) 8, (byte) newValue.getValue());
	}

	@Test
	void shouldFailRangeCheckWithInvalidValue() {
		final ApplyDeltaAttributeMutation mutation = new ApplyDeltaAttributeMutation("a", (byte)5, NumberRange.from(7));
		assertThrows(InvalidMutationException.class, () -> mutation.mutateLocal(new AttributeValue(new AttributeKey("a"), (byte)1)));
	}

	@Test
	void shouldReturnSameSkipToken() {
		assertEquals(
				new ApplyDeltaAttributeMutation("abc", 5).getSkipToken(),
				new ApplyDeltaAttributeMutation("abc", 6).getSkipToken()
		);
		assertEquals(
				new ApplyDeltaAttributeMutation("abc", Locale.ENGLISH, 5).getSkipToken(),
				new ApplyDeltaAttributeMutation("abc", Locale.ENGLISH, 6).getSkipToken()
		);
	}

	@Test
	void shouldReturnDifferentSkipToken() {
		assertNotEquals(
				new ApplyDeltaAttributeMutation("abc", 5).getSkipToken(),
				new ApplyDeltaAttributeMutation("abe", 6).getSkipToken()
		);
		assertNotEquals(
				new ApplyDeltaAttributeMutation("abc", Locale.ENGLISH, 5).getSkipToken(),
				new ApplyDeltaAttributeMutation("abc", Locale.GERMAN, 6).getSkipToken()
		);
	}

}