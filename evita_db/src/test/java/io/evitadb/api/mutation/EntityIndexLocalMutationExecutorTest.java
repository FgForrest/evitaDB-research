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

package io.evitadb.api.mutation;

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.mutation.EntityMutation.EntityExistence;
import io.evitadb.api.data.mutation.associatedData.RemoveAssociatedDataMutation;
import io.evitadb.api.data.mutation.associatedData.UpsertAssociatedDataMutation;
import io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation;
import io.evitadb.api.data.mutation.attribute.UpsertAttributeMutation;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.storage.model.storageParts.entity.EntityBodyStoragePart;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.evitadb.test.generator.DataGenerator.ATTRIBUTE_NAME;
import static io.evitadb.test.generator.DataGenerator.CZECH_LOCALE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies {@link EntityIndexLocalMutationExecutor} contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class EntityIndexLocalMutationExecutorTest extends AbstractMutatorTestBase {
	public static final AttributeKey LOCALIZED_ATTRIBUTE_KEY = new AttributeKey(ATTRIBUTE_NAME, CZECH_LOCALE);
	public static final AttributeSchema LOCALIZED_ATTRIBUTE_SCHEMA = new AttributeSchema(ATTRIBUTE_NAME, String.class, true);
	public static final AssociatedDataKey LOCALIZED_ASSOCIATED_DATA_KEY = new AssociatedDataKey("texts", CZECH_LOCALE);

	@Override
	protected void alterProductSchema(EntitySchemaBuilder schema) {
		/* DO NOTHING */
	}

	@Test
	void shouldAddLocaleInformationAboutEntityWhenLocalizedAttributeIsUpserted() {
		assertFalse(index.getLanguages().contains(CZECH_LOCALE));

		final UpsertAttributeMutation attrMutation = new UpsertAttributeMutation(LOCALIZED_ATTRIBUTE_KEY, "Žluťoučký kůň");
		executor.applyMutation(attrMutation);

		containerAccessor.getAttributeStorageContainer(1, CZECH_LOCALE)
			.upsertAttribute(LOCALIZED_ATTRIBUTE_KEY, LOCALIZED_ATTRIBUTE_SCHEMA, attrMutation::mutateLocal);

		assertTrue(index.getLanguages().contains(CZECH_LOCALE));
		assertTrue(index.getRecordsWithLanguage(CZECH_LOCALE).contains(1));
	}

	@Test
	void shouldRemoveLocaleInformationAboutEntityWhenLastLocalizedAttributeIsRemoved() {
		shouldAddLocaleInformationAboutEntityWhenLocalizedAttributeIsUpserted();
		assertTrue(index.getLanguages().contains(CZECH_LOCALE));

		executor.applyMutation(
			new RemoveAttributeMutation(LOCALIZED_ATTRIBUTE_KEY)
		);

		assertFalse(index.getLanguages().contains(CZECH_LOCALE));
		assertFalse(index.getRecordsWithLanguage(CZECH_LOCALE).contains(1));
	}

	@Test
	void shouldAddLocaleInformationAboutEntityWhenLocalizedAssociatedDataIsUpserted() {
		assertFalse(index.getLanguages().contains(CZECH_LOCALE));

		executor.applyMutation(
			new UpsertAssociatedDataMutation(LOCALIZED_ASSOCIATED_DATA_KEY, "Žluťoučký kůň")
		);
		final EntityBodyStoragePart entityStorageContainer = containerAccessor.getEntityStorageContainer(
			1, EntityExistence.MUST_EXIST
		);
		entityStorageContainer.setAssociatedDataKeys(Set.of(LOCALIZED_ASSOCIATED_DATA_KEY));

		assertTrue(index.getLanguages().contains(CZECH_LOCALE));
		assertTrue(index.getRecordsWithLanguage(CZECH_LOCALE).contains(1));
	}

	@Test
	void shouldRemoveLocaleInformationAboutEntityWhenLastLocalizedAssociatedDataIsRemoved() {
		shouldAddLocaleInformationAboutEntityWhenLocalizedAssociatedDataIsUpserted();
		assertTrue(index.getLanguages().contains(CZECH_LOCALE));

		executor.applyMutation(
			new RemoveAssociatedDataMutation(LOCALIZED_ASSOCIATED_DATA_KEY)
		);

		assertFalse(index.getLanguages().contains(CZECH_LOCALE));
		assertFalse(index.getRecordsWithLanguage(CZECH_LOCALE).contains(1));
	}

	@Test
	void shouldRemoveLocaleInformationAboutEntityWhenLastLocalizedInformationIsRemovedButNotTooEarly() {
		assertFalse(index.getLanguages().contains(CZECH_LOCALE));

		final UpsertAttributeMutation attrMutation = new UpsertAttributeMutation(LOCALIZED_ATTRIBUTE_KEY, "Žluťoučký kůň");
		final UpsertAssociatedDataMutation assocDataMutation = new UpsertAssociatedDataMutation(LOCALIZED_ASSOCIATED_DATA_KEY, "Žluťoučký kůň");

		executor.applyMutation(attrMutation);
		executor.applyMutation(assocDataMutation);

		final EntityBodyStoragePart entityStorageContainer = containerAccessor.getEntityStorageContainer(
			1, EntityExistence.MUST_EXIST
		);
		entityStorageContainer.setAssociatedDataKeys(Set.of(LOCALIZED_ASSOCIATED_DATA_KEY));
		containerAccessor.getAttributeStorageContainer(1, CZECH_LOCALE)
			.upsertAttribute(LOCALIZED_ATTRIBUTE_KEY, LOCALIZED_ATTRIBUTE_SCHEMA, attrMutation::mutateLocal);

		executor.applyMutation(
			new RemoveAttributeMutation(LOCALIZED_ATTRIBUTE_KEY)
		);

		assertTrue(index.getLanguages().contains(CZECH_LOCALE));
		assertTrue(index.getRecordsWithLanguage(CZECH_LOCALE).contains(1));

		containerAccessor.reset();

		executor.applyMutation(
			new RemoveAssociatedDataMutation(LOCALIZED_ASSOCIATED_DATA_KEY)
		);

		assertFalse(index.getLanguages().contains(CZECH_LOCALE));
		assertFalse(index.getRecordsWithLanguage(CZECH_LOCALE).contains(1));
	}

}