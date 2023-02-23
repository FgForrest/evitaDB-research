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

package io.evitadb.api;

import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.exception.UniqueValueViolationException;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.query.QueryConstraints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static io.evitadb.api.EvitaApiFunctionalTest.LOGO;
import static io.evitadb.api.EvitaApiFunctionalTest.SIEMENS_TITLE;
import static io.evitadb.test.Entities.BRAND;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test verifies entity indexing behaviour and checks whether the implementation prevents to upsert entities with
 * invalid or non-unique data.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita indexing API")
@Tag(FUNCTIONAL_TEST)
public class EvitaIndexingFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {

	public static final String ATTRIBUTE_CODE = "code";

	@DisplayName("Fail to create entity with conflicting unique attribute")
	@Test
	void shouldFailToInsertConflictingUniqueAttributes(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session.defineSchema(BRAND)
					.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
					.applyChanges();
				session.upsertEntity(createBrand(session, 1));
				assertThrows(UniqueValueViolationException.class, () -> session.upsertEntity(createBrand(session, 2)));
			}
		);
	}

	@DisplayName("Allow to reuse unique attribute when changed in original entity")
	@Test
	void shouldAllowToUpdateExistingUniqueAttributeAndReuseItForAnotherEntity(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session.defineSchema(BRAND)
					.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
					.applyChanges();

				session.upsertEntity(createBrand(session, 1));

				// change the unique value
				final SealedEntity theBrand = session.getEntity(BRAND, 1, QueryConstraints.attributes());
				assertNotNull(theBrand);
				session.upsertEntity(theBrand.open().setAttribute(ATTRIBUTE_CODE, "otherCode"));

				// now we can use original code for different entity
				session.upsertEntity(createBrand(session, 2));
			}
		);
	}

	@DisplayName("Allow to reuse unique attribute when original entity is removed")
	@Test
	void shouldAllowToInsertUniqueAttributeWhenOriginalEntityRemoved(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session.defineSchema(BRAND)
					.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
					.applyChanges();

				session.upsertEntity(createBrand(session, 1));

				// change the unique value
				final SealedEntity theBrand = session.getEntity(BRAND, 1, QueryConstraints.attributes());
				assertNotNull(theBrand);
				session.deleteEntity(BRAND, theBrand.getPrimaryKey());

				// now we can use original code for different entity
				session.upsertEntity(createBrand(session, 2));
			}
		);
	}

	/*
		HELPER METHODS
	 */

	private EntityBuilder createBrand(SESSION session, Integer primaryKey) {
		final EntityBuilder newBrand = session.createNewEntity(BRAND, primaryKey);
		newBrand.setAttribute(ATTRIBUTE_CODE, "siemens");
		newBrand.setAttribute("name", Locale.ENGLISH, SIEMENS_TITLE);
		newBrand.setAttribute("logo", LOGO);
		newBrand.setAttribute("productCount", 1);
		return newBrand;
	}

}
