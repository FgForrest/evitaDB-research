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

package io.evitadb.api.io.predicate;

import io.evitadb.api.io.EvitaRequestBase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * This test verifies behaviour of {@link AttributeValueSerializablePredicate}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
class AttributeValueSerializablePredicateTest {

	@Test
	void shouldCreateRicherCopyForNoAttributes() {
		final AttributeValueSerializablePredicate noAttributesRequired = new AttributeValueSerializablePredicate(
			Collections.emptySet(), false
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(Collections.emptySet());
		assertNotSame(noAttributesRequired, noAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForNoAttributes() {
		final AttributeValueSerializablePredicate noAttributesRequired = new AttributeValueSerializablePredicate(
			Collections.emptySet(), false
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(false);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(Collections.emptySet());
		assertSame(noAttributesRequired, noAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForNoAttributesWhenAttributesPresent() {
		final AttributeValueSerializablePredicate noAttributesRequired = new AttributeValueSerializablePredicate(
			Collections.emptySet(), true
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(false);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(Collections.emptySet());
		assertSame(noAttributesRequired, noAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldCreateRicherCopyForGlobalAttributes() {
		final AttributeValueSerializablePredicate globalAttributesRequired = new AttributeValueSerializablePredicate(
			Collections.emptySet(), true
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(new HashSet<>(Collections.singletonList(Locale.ENGLISH)));
		assertNotSame(globalAttributesRequired, globalAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForGlobalAttributes() {
		final AttributeValueSerializablePredicate globalAttributesRequired = new AttributeValueSerializablePredicate(
			Collections.emptySet(), true
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(Collections.emptySet());
		assertSame(globalAttributesRequired, globalAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldCreateRicherCopyForGlobalAndLocalizedAttributes() {
		final AttributeValueSerializablePredicate localizedAttributesRequired = new AttributeValueSerializablePredicate(
			new HashSet<>(Collections.singletonList(Locale.ENGLISH)), true
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.CANADA)));
		assertNotSame(localizedAttributesRequired, localizedAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForGlobalAndLocalizedAttributes() {
		final AttributeValueSerializablePredicate localizedAttributesRequired = new AttributeValueSerializablePredicate(
			new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.CANADA)), true
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.CANADA)));
		assertSame(localizedAttributesRequired, localizedAttributesRequired.createRicherCopyWith(evitaRequest));
	}

	@Test
	void shouldNotCreateRicherCopyForGlobalAndLocalizedAttributesSubset() {
		final AttributeValueSerializablePredicate localizedAttributesRequired = new AttributeValueSerializablePredicate(
			new HashSet<>(Arrays.asList(Locale.ENGLISH, Locale.CANADA)), true
		);

		final EvitaRequestBase evitaRequest = Mockito.mock(EvitaRequestBase.class);
		Mockito.when(evitaRequest.isRequiresEntityAttributes()).thenReturn(true);
		Mockito.when(evitaRequest.getRequiredLanguages()).thenReturn(new HashSet<>(Arrays.asList(Locale.ENGLISH)));
		assertSame(localizedAttributesRequired, localizedAttributesRequired.createRicherCopyWith(evitaRequest));
	}

}